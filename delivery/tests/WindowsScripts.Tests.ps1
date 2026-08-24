Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'

$RepositoryRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$TempRoot = Join-Path ([IO.Path]::GetTempPath()) ("configurator-windows-test-{0}" -f [Guid]::NewGuid())
$PackageRoot = Join-Path $TempRoot 'Папка с пробелами\Configurator'
$FakeBin = Join-Path $TempRoot 'fake-bin'
$FakeDockerLog = Join-Path $TempRoot 'docker.log'
$FakePullFailureMarker = "$FakeDockerLog.fail-pull"
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
setlocal EnableExtensions DisableDelayedExpansion
echo %*>>"%FAKE_DOCKER_LOG%"
if "%~1"=="info" goto handle_info
if "%~1"=="inspect" goto handle_inspect
if "%~1"=="compose" if "%~2"=="version" goto handle_compose_version

set "FAKE_HAS_PS=0"
set "FAKE_HAS_PULL=0"
set "FAKE_HAS_PG_DUMP=0"
set "FAKE_HAS_MIRROR=0"
set "FAKE_HAS_BACKUP_MINIO=0"
:scan_arguments
if "%~1"=="" goto arguments_scanned
if /I "%~1"=="ps" set "FAKE_HAS_PS=1"
if /I "%~1"=="pull" set "FAKE_HAS_PULL=1"
if /I "%~1"=="pg_dump" set "FAKE_HAS_PG_DUMP=1"
if /I "%~1"=="mirror" set "FAKE_HAS_MIRROR=1"
if /I "%~1"=="/backup/minio" set "FAKE_HAS_BACKUP_MINIO=1"
shift
goto scan_arguments

:arguments_scanned
if "%FAKE_HAS_PS%"=="1" goto handle_ps
if "%FAKE_HAS_PULL%"=="1" goto handle_pull
if "%FAKE_HAS_PG_DUMP%"=="1" goto handle_pg_dump
if "%FAKE_HAS_MIRROR%"=="1" if "%FAKE_HAS_BACKUP_MINIO%"=="1" goto handle_backup_mirror
exit /b 0

:handle_info
if "%FAKE_DAEMON_DOWN%"=="1" goto docker_failure
exit /b 0

:handle_inspect
echo sha256:fake-image
exit /b 0

:handle_compose_version
if "%FAKE_COMPOSE_DOWN%"=="1" goto docker_failure
exit /b 0

:handle_ps
echo container-fake
exit /b 0

:handle_pull
if exist "%FAKE_DOCKER_LOG%.fail-pull" goto pull_failure
exit /b 0

:pull_failure
echo __fake_failure__ pull>>"%FAKE_DOCKER_LOG%"
goto docker_failure

:handle_pg_dump
if not exist "%CONFIGURATOR_MAINTENANCE_DIR%" mkdir "%CONFIGURATOR_MAINTENANCE_DIR%"
echo fake database dump>"%CONFIGURATOR_MAINTENANCE_DIR%\database.dump"
exit /b 0

:handle_backup_mirror
if not exist "%CONFIGURATOR_MAINTENANCE_DIR%\minio" mkdir "%CONFIGURATOR_MAINTENANCE_DIR%\minio"
echo fake image bytes>"%CONFIGURATOR_MAINTENANCE_DIR%\minio\object.bin"
exit /b 0

:docker_failure
exit /b 1
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

    Set-Content -LiteralPath $FakePullFailureMarker -Value '' -Encoding ASCII
    try { $updateExitCode = Invoke-Operation @('update') }
    finally { Remove-Item -LiteralPath $FakePullFailureMarker -Force -ErrorAction SilentlyContinue }
    $updateLog = Get-Content $FakeDockerLog -Raw
    Assert-True ($updateExitCode -eq 60) `
        "failed update returned $updateExitCode instead of 60. Docker log: $updateLog"
    Assert-True ($updateLog.Contains('__fake_failure__ pull')) 'fake Docker did not inject the pull failure'
    Assert-True ($updateLog.Contains('pull app gateway')) 'update did not pull app/gateway'
    Assert-True ($updateLog.Contains('stop gateway app')) 'failed update did not stop app/gateway'
    $preUpdateBackups = @(Get-ChildItem (Join-Path $PackageRoot 'backups') -Directory -Filter 'pre-update-*')
    Assert-True ($preUpdateBackups.Count -gt 0) 'failed update did not retain its pre-update backup'
    $updateOperationLog = Get-ChildItem (Join-Path $PackageRoot 'logs') -File -Filter 'update-*.log' |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    Assert-True ($null -ne $updateOperationLog) 'update operation log is missing'
    Assert-True (-not (Select-String -LiteralPath $updateOperationLog.FullName -SimpleMatch 'Update завершён.' -Quiet)) `
        'failed update emitted the success message'

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
    Remove-Item -LiteralPath $FakePullFailureMarker -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $TempRoot -Recurse -Force -ErrorAction SilentlyContinue
}
