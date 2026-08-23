Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'

$RepositoryRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$TempRoot = Join-Path ([IO.Path]::GetTempPath()) ("configurator-windows-test-{0}" -f [Guid]::NewGuid())
$PackageRoot = Join-Path $TempRoot 'Папка с пробелами\Configurator'
$FakeBin = Join-Path $TempRoot 'fake-bin'
$FakeDockerLog = Join-Path $TempRoot 'docker.log'
$ServerProcess = $null

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw "WindowsScripts.Tests: $Message" }
}

function Invoke-Operation([string[]]$Arguments) {
    Set-Content -LiteralPath $FakeDockerLog -Value '' -Encoding ASCII
    $scriptPath = Join-Path $PackageRoot 'scripts\configurator.ps1'
    $operationArguments = ($Arguments + @('-NonInteractive', '-NoOpen') | ForEach-Object {
        '"{0}"' -f ([string]$_).Replace('"', '\"')
    }) -join ' '
    $argumentLine = '-NoLogo -NoProfile -ExecutionPolicy Bypass -File "{0}" {1}' -f $scriptPath, $operationArguments
    $process = Start-Process -FilePath 'powershell.exe' -ArgumentList $argumentLine -Wait -PassThru -NoNewWindow
    return $process.ExitCode
}

try {
    New-Item -ItemType Directory -Force -Path (Join-Path $PackageRoot 'scripts'), (Join-Path $PackageRoot 'backups'),
        (Join-Path $PackageRoot 'logs'), $FakeBin | Out-Null
    Copy-Item (Join-Path $RepositoryRoot 'delivery\common\compose.yaml') (Join-Path $PackageRoot 'compose.yaml')
    Copy-Item (Join-Path $RepositoryRoot 'delivery\common\configurator.env') (Join-Path $PackageRoot 'configurator.env')
    Copy-Item (Join-Path $RepositoryRoot 'delivery\windows\scripts\configurator.ps1') (Join-Path $PackageRoot 'scripts\configurator.ps1')

    @'
@echo off
echo %*>>"%FAKE_DOCKER_LOG%"
if "%1"=="info" (
  if "%FAKE_DAEMON_DOWN%"=="1" exit /b 1
  exit /b 0
)
if "%1"=="inspect" (
  echo sha256:fake-image
  exit /b 0
)
if "%1"=="compose" if "%2"=="version" (
  if "%FAKE_COMPOSE_DOWN%"=="1" exit /b 1
  exit /b 0
)
echo %* | findstr /C:" ps " >nul
if not errorlevel 1 (
  echo container-fake
  exit /b 0
)
echo %* | findstr /C:" pull app gateway" >nul
if not errorlevel 1 (
  if "%FAKE_FAIL_PULL%"=="1" exit /b 1
  exit /b 0
)
echo %* | findstr /C:" pg_dump " >nul
if not errorlevel 1 (
  if not exist "%CONFIGURATOR_MAINTENANCE_DIR%" mkdir "%CONFIGURATOR_MAINTENANCE_DIR%"
  echo fake database dump>"%CONFIGURATOR_MAINTENANCE_DIR%\database.dump"
  exit /b 0
)
echo %* | findstr /C:" mirror " | findstr /C:" /backup/minio" >nul
if not errorlevel 1 (
  if not exist "%CONFIGURATOR_MAINTENANCE_DIR%\minio" mkdir "%CONFIGURATOR_MAINTENANCE_DIR%\minio"
  echo fake image bytes>"%CONFIGURATOR_MAINTENANCE_DIR%\minio\object.bin"
  exit /b 0
)
exit /b 0
'@ | Set-Content -LiteralPath (Join-Path $FakeBin 'docker.cmd') -Encoding ASCII

    @'
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 8080)
$listener.Start()
try {
    while ($true) {
        $client = $listener.AcceptTcpClient()
        try {
            $stream = $client.GetStream()
            $reader = New-Object IO.StreamReader($stream, [Text.Encoding]::ASCII, $false, 1024, $true)
            while (($line = $reader.ReadLine()) -ne '') {
                if ($null -eq $line) { break }
            }
            $body = '{"status":"UP"}'
            $bodyBytes = [Text.Encoding]::UTF8.GetBytes($body)
            $headers = "HTTP/1.1 200 OK`r`nContent-Type: application/json`r`nContent-Length: $($bodyBytes.Length)`r`nConnection: close`r`n`r`n"
            $headerBytes = [Text.Encoding]::ASCII.GetBytes($headers)
            $stream.Write($headerBytes, 0, $headerBytes.Length)
            $stream.Write($bodyBytes, 0, $bodyBytes.Length)
        }
        finally { $client.Close() }
    }
}
finally { $listener.Stop() }
'@ | Set-Content -LiteralPath (Join-Path $TempRoot 'http-server.ps1') -Encoding UTF8

    $env:PATH = "$FakeBin;$env:PATH"
    $env:FAKE_DOCKER_LOG = $FakeDockerLog
    $env:FAKE_DAEMON_DOWN = '0'
    $env:FAKE_COMPOSE_DOWN = '0'
    $env:FAKE_FAIL_PULL = '0'
    $env:CONFIGURATOR_DOCKER_WAIT_SECONDS = '0'
    $env:CONFIGURATOR_READINESS_WAIT_SECONDS = '4'
    $env:PROCESSOR_ARCHITECTURE = 'AMD64'

    $serverPath = Join-Path $TempRoot 'http-server.ps1'
    $ServerProcess = Start-Process -FilePath 'powershell.exe' `
        -ArgumentList ('-NoLogo -NoProfile -ExecutionPolicy Bypass -File "{0}"' -f $serverPath) `
        -PassThru -WindowStyle Hidden
    $serverReady = $false
    for ($attempt = 0; $attempt -lt 20; $attempt++) {
        try {
            $null = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/healthz' -UseBasicParsing -TimeoutSec 1
            $serverReady = $true
            break
        }
        catch { Start-Sleep -Milliseconds 100 }
    }
    Assert-True $serverReady 'HTTP test server did not start'

    Assert-True ((Invoke-Operation @('start')) -eq 0) 'start failed'
    Assert-True ((Get-Content $FakeDockerLog -Raw).Contains('up -d --remove-orphans')) 'start did not run compose up'

    Assert-True ((Invoke-Operation @('stop')) -eq 0) 'stop failed'
    Assert-True ((Get-Content $FakeDockerLog -Raw).Contains('stop gateway app minio postgres')) 'stop command is incorrect'

    Assert-True ((Invoke-Operation @('backup')) -eq 0) 'backup failed'
    $backup = Get-ChildItem (Join-Path $PackageRoot 'backups') -Directory |
        Where-Object { -not $_.Name.EndsWith('.partial') } | Select-Object -First 1
    Assert-True ($null -ne $backup) 'backup directory is missing'
    Assert-True (Test-Path (Join-Path $backup.FullName 'database.dump')) 'database dump is missing'
    Assert-True (Test-Path (Join-Path $backup.FullName 'minio\object.bin')) 'MinIO object is missing'
    Assert-True (Test-Path (Join-Path $backup.FullName 'manifest.properties')) 'manifest is missing'
    Assert-True (Test-Path (Join-Path $backup.FullName 'SHA256SUMS')) 'checksums are missing'

    Assert-True ((Invoke-Operation @('restore', '-Yes', '-Backup', $backup.FullName)) -eq 0) 'restore failed'
    Assert-True ((Get-Content $FakeDockerLog -Raw).Contains('pg_restore --exit-on-error')) 'pg_restore was not invoked'
    Assert-True ((Get-Content $FakeDockerLog -Raw).Contains('mirror --overwrite --remove')) 'MinIO replacement was not invoked'

    $env:FAKE_FAIL_PULL = '1'
    Assert-True ((Invoke-Operation @('update')) -eq 60) 'failed update returned an unexpected exit code'
    $updateLog = Get-Content $FakeDockerLog -Raw
    Assert-True ($updateLog.Contains('pull app gateway')) 'update did not pull app/gateway'
    Assert-True ($updateLog.Contains('stop gateway app')) 'failed update did not stop app/gateway'
    $env:FAKE_FAIL_PULL = '0'

    New-Item -ItemType Directory -Path (Join-Path $PackageRoot '.configurator-operation.lock') | Out-Null
    Assert-True ((Invoke-Operation @('stop')) -eq 80) 'lock contention returned an unexpected exit code'
    Remove-Item (Join-Path $PackageRoot '.configurator-operation.lock') -Force

    $env:FAKE_COMPOSE_DOWN = '1'
    Assert-True ((Invoke-Operation @('start')) -eq 20) 'missing Compose returned an unexpected exit code'

    $partial = @(Get-ChildItem (Join-Path $PackageRoot 'backups') -Directory -Filter '*.partial')
    Assert-True ($partial.Count -eq 0) 'partial backup directory leaked'
    $credentialLeak = $false
    foreach ($logFile in Get-ChildItem (Join-Path $PackageRoot 'logs') -File) {
        if (Select-String -LiteralPath $logFile.FullName -SimpleMatch 'configurator-local-preview' -Quiet) {
            $credentialLeak = $true
            break
        }
    }
    Assert-True (-not $credentialLeak) 'local credentials leaked into operation logs'

    Write-Host 'WindowsScripts.Tests: OK'
}
finally {
    if ($ServerProcess -and -not $ServerProcess.HasExited) { Stop-Process -Id $ServerProcess.Id -Force }
    Remove-Item -LiteralPath $TempRoot -Recurse -Force -ErrorAction SilentlyContinue
}
