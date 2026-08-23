param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet('start', 'stop', 'update', 'backup', 'restore')]
    [string]$Operation,
    [switch]$NonInteractive,
    [switch]$NoOpen,
    [switch]$Yes,
    [string]$Backup
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'

$ExitPrerequisite = 10
$ExitDocker = 20
$ExitConflict = 30
$ExitBackup = 40
$ExitRestore = 50
$ExitUpdate = 60
$ExitReadiness = 70
$ExitLocked = 80

$PackageRoot = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $PackageRoot 'compose.yaml'
$EnvFile = Join-Path $PackageRoot 'configurator.env'
$BackupsDirectory = Join-Path $PackageRoot 'backups'
$LogsDirectory = Join-Path $PackageRoot 'logs'
$LockDirectory = Join-Path $PackageRoot '.configurator-operation.lock'
$DockerWaitSeconds = if ($env:CONFIGURATOR_DOCKER_WAIT_SECONDS) { [int]$env:CONFIGURATOR_DOCKER_WAIT_SECONDS } else { 180 }
$ReadinessWaitSeconds = if ($env:CONFIGURATOR_READINESS_WAIT_SECONDS) { [int]$env:CONFIGURATOR_READINESS_WAIT_SECONDS } else { 180 }
$script:LockAcquired = $false
$script:LastBackupDirectory = $null
$script:LogFile = $null

function Get-UtcTimestamp {
    return [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')
}

function Write-Utf8NoBom([string]$Path, [string[]]$Lines) {
    $encoding = New-Object System.Text.UTF8Encoding($false)
    [IO.File]::WriteAllLines($Path, $Lines, $encoding)
}

function Initialize-Log {
    New-Item -ItemType Directory -Force -Path $LogsDirectory | Out-Null
    $script:LogFile = Join-Path $LogsDirectory ("{0}-{1}.log" -f $Operation, (Get-UtcTimestamp))
    New-Item -ItemType File -Force -Path $script:LogFile | Out-Null
}

function Write-OperationLog([string]$Message) {
    Write-Host $Message
    if ($script:LogFile) {
        $line = "{0} {1}" -f [DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ssZ'), $Message
        Add-Content -LiteralPath $script:LogFile -Value $line -Encoding UTF8
    }
}

function Stop-WithError([int]$Code, [string]$Message) {
    Write-OperationLog "ОШИБКА: $Message"
    if ($script:LogFile) {
        Write-OperationLog "Диагностика: $script:LogFile"
    }
    exit $Code
}

function Add-CommandOutput($Output) {
    if ($null -eq $Output -or -not $script:LogFile) { return }
    foreach ($line in @($Output)) {
        Add-Content -LiteralPath $script:LogFile -Value ([string]$line) -Encoding UTF8
    }
}

function Invoke-Docker([string[]]$Arguments, [switch]$AllowFailure) {
    $output = & docker @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    Add-CommandOutput $output
    if ($exitCode -ne 0 -and -not $AllowFailure) {
        throw "docker exited with code $exitCode"
    }
    return @($output)
}

function Get-ComposeArguments([string[]]$Arguments) {
    return @('compose', '--project-directory', $PackageRoot, '--env-file', $EnvFile, '-f', $ComposeFile) + $Arguments
}

function Invoke-Compose([string[]]$Arguments, [switch]$AllowFailure) {
    return Invoke-Docker -Arguments (Get-ComposeArguments $Arguments) -AllowFailure:$AllowFailure
}

function Acquire-Lock {
    try {
        New-Item -ItemType Directory -Path $LockDirectory -ErrorAction Stop | Out-Null
        $script:LockAcquired = $true
    }
    catch {
        Stop-WithError $ExitLocked 'Другая операция Configurator уже выполняется. Дождитесь её завершения.'
    }
}

function Release-Lock {
    if ($script:LockAcquired) {
        Remove-Item -LiteralPath $LockDirectory -Force -ErrorAction SilentlyContinue
        $script:LockAcquired = $false
    }
}

function Test-DockerDaemon {
    & docker info *> $null
    return $LASTEXITCODE -eq 0
}

function Start-DockerDesktopIfPossible {
    $candidates = @(
        (Join-Path $env:LOCALAPPDATA 'Programs\DockerDesktop\Docker Desktop.exe'),
        (Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe')
    )
    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) {
            Write-OperationLog 'Docker Desktop не запущен. Пытаюсь открыть его…'
            Start-Process -FilePath $candidate | Out-Null
            return
        }
    }
}

function Wait-DockerDaemon {
    $elapsed = 0
    while ($elapsed -lt $DockerWaitSeconds) {
        if (Test-DockerDaemon) { return $true }
        Start-Sleep -Seconds 2
        $elapsed += 2
    }
    return $false
}

function Validate-Package {
    if (-not (Test-Path -LiteralPath $ComposeFile -PathType Leaf)) {
        Stop-WithError $ExitPrerequisite 'Не найден compose.yaml. Распакуйте архив полностью.'
    }
    if (-not (Test-Path -LiteralPath $EnvFile -PathType Leaf)) {
        Stop-WithError $ExitPrerequisite 'Не найден configurator.env. Распакуйте архив полностью.'
    }
    New-Item -ItemType Directory -Force -Path $BackupsDirectory, $LogsDirectory | Out-Null
}

function Validate-Platform {
    $architecture = $env:PROCESSOR_ARCHITECTURE
    if ($architecture -notin @('AMD64', 'x86_64')) {
        Stop-WithError $ExitPrerequisite "Неподдерживаемая архитектура Windows: $architecture. Требуется x86-64."
    }
}

function Validate-Docker {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Stop-WithError $ExitDocker 'Docker CLI не найден. Установите Docker Desktop.'
    }
    & docker compose version *> $null
    if ($LASTEXITCODE -ne 0) {
        Stop-WithError $ExitDocker 'Docker Compose v2 недоступен. Обновите Docker Desktop.'
    }
    if (-not (Test-DockerDaemon)) {
        Start-DockerDesktopIfPossible
        if (-not (Wait-DockerDaemon)) {
            Stop-WithError $ExitDocker 'Docker Desktop не готов. Запустите его, примите лицензию и повторите.'
        }
    }
}

function Validate-Compose {
    try { $null = Invoke-Compose @('config', '--quiet') }
    catch { Stop-WithError $ExitConflict 'Некорректная конфигурация пакета.' }
}

function Test-ServiceRunning([string]$Service) {
    try {
        $result = Invoke-Compose @('ps', '--status', 'running', '--quiet', $Service)
        return -not [string]::IsNullOrWhiteSpace(($result -join ''))
    }
    catch { return $false }
}

function Test-Port8080 {
    if (Test-ServiceRunning 'gateway') { return $false }
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $async = $client.BeginConnect('127.0.0.1', 8080, $null, $null)
        if ($async.AsyncWaitHandle.WaitOne(300) -and $client.Connected) { return $true }
        return $false
    }
    catch { return $false }
    finally { $client.Close() }
}

function Test-Url([string]$Url) {
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 400
    }
    catch { return $false }
}

function Wait-Url([string]$Url) {
    $elapsed = 0
    while ($elapsed -lt $ReadinessWaitSeconds) {
        if (Test-Url $Url) { return $true }
        Start-Sleep -Seconds 2
        $elapsed += 2
    }
    return $false
}

function Wait-Application {
    if (-not (Wait-Url 'http://127.0.0.1:8080/healthz')) { return $false }
    return Wait-Url 'http://127.0.0.1:8080/api/v3/api-docs'
}

function Write-SafeDiagnostics {
    $null = Invoke-Compose @('ps') -AllowFailure
    $null = Invoke-Compose @('logs', '--no-color', '--tail', '80', 'app', 'gateway') -AllowFailure
}

function Read-EnvironmentValue([string]$Key) {
    foreach ($line in Get-Content -LiteralPath $EnvFile) {
        if ($line.StartsWith("$Key=")) { return $line.Substring($Key.Length + 1) }
    }
    Stop-WithError $ExitPrerequisite "В configurator.env отсутствует $Key."
}

function Get-ResolvedImageId([string]$Service) {
    try {
        $container = (Invoke-Compose @('ps', '--all', '--quiet', $Service) | Select-Object -First 1)
        if ([string]::IsNullOrWhiteSpace([string]$container)) { return 'unknown' }
        $result = Invoke-Docker @('inspect', '--format', '{{.Image}}', [string]$container)
        return ([string]($result | Select-Object -First 1)).Trim()
    }
    catch { return 'unknown' }
}

function New-Checksums([string]$Directory) {
    $checksumFile = Join-Path $Directory 'SHA256SUMS'
    $lines = New-Object System.Collections.Generic.List[string]
    $files = Get-ChildItem -LiteralPath $Directory -Recurse -File |
        Where-Object { $_.FullName -ne $checksumFile } |
        Sort-Object FullName
    foreach ($file in $files) {
        $relative = $file.FullName.Substring($Directory.Length).TrimStart('\', '/').Replace('\', '/')
        $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $file.FullName).Hash.ToLowerInvariant()
        $lines.Add("$hash  $relative")
    }
    Write-Utf8NoBom $checksumFile $lines.ToArray()
}

function Test-Checksums([string]$Directory) {
    $checksumFile = Join-Path $Directory 'SHA256SUMS'
    if (-not (Test-Path -LiteralPath $checksumFile -PathType Leaf)) { return $false }
    foreach ($line in Get-Content -LiteralPath $checksumFile) {
        if ($line -notmatch '^([0-9a-fA-F]{64})  (.+)$') { return $false }
        $expected = $Matches[1].ToLowerInvariant()
        $relative = $Matches[2]
        if ([IO.Path]::IsPathRooted($relative) -or $relative -match '(^|/)\.\.(/|$)') { return $false }
        $file = Join-Path $Directory ($relative.Replace('/', [IO.Path]::DirectorySeparatorChar))
        if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { return $false }
        $actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $file).Hash.ToLowerInvariant()
        if ($actual -ne $expected) { return $false }
    }
    return $true
}

function Invoke-WithMaintenanceDirectory([string]$Directory, [string[]]$Arguments) {
    $previous = $env:CONFIGURATOR_MAINTENANCE_DIR
    try {
        $env:CONFIGURATOR_MAINTENANCE_DIR = $Directory
        return Invoke-Compose $Arguments
    }
    finally {
        if ($null -eq $previous) { Remove-Item Env:CONFIGURATOR_MAINTENANCE_DIR -ErrorAction SilentlyContinue }
        else { $env:CONFIGURATOR_MAINTENANCE_DIR = $previous }
    }
}

function Restore-ServiceState($State) {
    if ($State.App) { $null = Invoke-Compose @('up', '-d', 'app') }
    else { $null = Invoke-Compose @('stop', 'app') -AllowFailure }
    if ($State.Gateway) { $null = Invoke-Compose @('up', '-d', 'gateway') }
    else { $null = Invoke-Compose @('stop', 'gateway') -AllowFailure }
    if (-not $State.Minio -and -not $State.App -and -not $State.Gateway) {
        $null = Invoke-Compose @('stop', 'minio') -AllowFailure
    }
    if (-not $State.Postgres -and -not $State.App -and -not $State.Gateway) {
        $null = Invoke-Compose @('stop', 'postgres') -AllowFailure
    }
}

function New-FullBackup([string]$Prefix = 'backup') {
    $state = @{
        App = Test-ServiceRunning 'app'
        Gateway = Test-ServiceRunning 'gateway'
        Postgres = Test-ServiceRunning 'postgres'
        Minio = Test-ServiceRunning 'minio'
    }
    $name = Get-UtcTimestamp
    if ($Prefix -ne 'backup') { $name = "$Prefix-$name" }
    $finalDirectory = Join-Path $BackupsDirectory $name
    $suffix = 1
    while ((Test-Path -LiteralPath $finalDirectory) -or (Test-Path -LiteralPath "$finalDirectory.partial")) {
        $finalDirectory = Join-Path $BackupsDirectory ("$name-$suffix")
        $suffix++
    }
    $partialDirectory = "$finalDirectory.partial"
    New-Item -ItemType Directory -Force -Path (Join-Path $partialDirectory 'minio') | Out-Null

    try {
        Write-OperationLog 'Подготавливаю PostgreSQL и MinIO для backup…'
        $null = Invoke-Compose @('up', '-d', '--wait', '--wait-timeout', [string]$ReadinessWaitSeconds, 'postgres', 'minio')
        $null = Invoke-Compose @('stop', 'gateway', 'app') -AllowFailure

        $database = Read-EnvironmentValue 'CONFIGURATOR_DB_NAME'
        $bucket = Read-EnvironmentValue 'CONFIGURATOR_MINIO_BUCKET'
        $packageVersion = Read-EnvironmentValue 'CONFIGURATOR_PACKAGE_VERSION'
        $channel = Read-EnvironmentValue 'CONFIGURATOR_CHANNEL'

        Write-OperationLog 'Сохраняю базу данных…'
        $null = Invoke-WithMaintenanceDirectory $partialDirectory @(
            'run', '--rm', '--no-deps', 'postgres-maintenance', 'pg_dump', '--format=custom', '--no-owner',
            '--no-privileges', '--file=/backup/database.dump', $database
        )
        $null = Invoke-WithMaintenanceDirectory $partialDirectory @(
            'run', '--rm', '--no-deps', 'postgres-maintenance', 'pg_restore', '--list', '/backup/database.dump'
        )

        Write-OperationLog 'Сохраняю изображения…'
        $null = Invoke-WithMaintenanceDirectory $partialDirectory @(
            'run', '--rm', '--no-deps', 'minio-maintenance', 'mb', '--ignore-existing', "configurator/$bucket"
        )
        $null = Invoke-WithMaintenanceDirectory $partialDirectory @(
            'run', '--rm', '--no-deps', 'minio-maintenance', 'mirror', '--overwrite', "configurator/$bucket", '/backup/minio'
        )

        $manifest = @(
            'formatVersion=1',
            "createdAt=$([DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ssZ'))",
            "packageVersion=$packageVersion",
            "channel=$channel",
            'composeProject=configurator',
            "appImage=$(Get-ResolvedImageId 'app')",
            "gatewayImage=$(Get-ResolvedImageId 'gateway')",
            'databaseArtifact=database.dump',
            'minioArtifact=minio'
        )
        Write-Utf8NoBom (Join-Path $partialDirectory 'manifest.properties') $manifest
        New-Checksums $partialDirectory
        if (-not (Test-Checksums $partialDirectory)) { throw 'checksum verification failed' }

        Restore-ServiceState $state
        if ($state.App -and $state.Gateway -and -not (Wait-Application)) {
            Write-SafeDiagnostics
            throw 'service state readiness failed'
        }
        Move-Item -LiteralPath $partialDirectory -Destination $finalDirectory
        $script:LastBackupDirectory = $finalDirectory
        Write-OperationLog "Backup создан: $finalDirectory"
        return $finalDirectory
    }
    catch {
        Remove-Item -LiteralPath $partialDirectory -Recurse -Force -ErrorAction SilentlyContinue
        try { Restore-ServiceState $state } catch { }
        throw
    }
}

function Test-Backup([string]$Directory) {
    if (-not (Test-Path -LiteralPath $Directory -PathType Container)) { return $false }
    $item = Get-Item -LiteralPath $Directory
    if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) { return $false }
    if ($item.Name.EndsWith('.partial')) { return $false }
    foreach ($required in @('database.dump', 'manifest.properties', 'SHA256SUMS')) {
        if (-not (Test-Path -LiteralPath (Join-Path $Directory $required) -PathType Leaf)) { return $false }
    }
    if (-not (Test-Path -LiteralPath (Join-Path $Directory 'minio') -PathType Container)) { return $false }
    if (-not (Select-String -LiteralPath (Join-Path $Directory 'manifest.properties') -SimpleMatch 'formatVersion=1' -Quiet)) {
        return $false
    }
    return Test-Checksums $Directory
}

function Select-BackupDirectory {
    if ($Backup) {
        $candidate = if ([IO.Path]::IsPathRooted($Backup)) { $Backup } else { Join-Path $PackageRoot $Backup }
        return (Resolve-Path -LiteralPath $candidate).Path
    }
    $items = @(Get-ChildItem -LiteralPath $BackupsDirectory -Directory |
        Where-Object { -not $_.Name.EndsWith('.partial') } |
        Sort-Object Name -Descending)
    if ($items.Count -eq 0) { throw 'backups not found' }
    if ($NonInteractive) { return $items[0].FullName }
    Write-OperationLog 'Доступные backups:'
    for ($index = 0; $index -lt $items.Count; $index++) {
        Write-Host ("  {0}. {1}" -f ($index + 1), $items[$index].Name)
    }
    $selection = Read-Host 'Введите номер backup'
    $number = 0
    if (-not [int]::TryParse($selection, [ref]$number) -or $number -lt 1 -or $number -gt $items.Count) {
        throw 'invalid backup selection'
    }
    return $items[$number - 1].FullName
}

function Confirm-Restore {
    if ($Yes) { return $true }
    if ($NonInteractive) { return $false }
    Write-OperationLog 'Restore заменит текущую базу данных и изображения. Сначала будет создан страховочный backup.'
    return (Read-Host 'Для продолжения введите RESTORE') -ceq 'RESTORE'
}

function Invoke-Restore([string]$SelectedBackup) {
    Write-OperationLog 'Создаю страховочный backup перед Restore…'
    $safetyBackup = New-FullBackup 'pre-restore'
    $database = Read-EnvironmentValue 'CONFIGURATOR_DB_NAME'
    $bucket = Read-EnvironmentValue 'CONFIGURATOR_MINIO_BUCKET'
    try {
        $null = Invoke-Compose @('up', '-d', '--wait', '--wait-timeout', [string]$ReadinessWaitSeconds, 'postgres', 'minio')
        $null = Invoke-Compose @('stop', 'gateway', 'app') -AllowFailure
        Write-OperationLog 'Восстанавливаю базу данных…'
        $null = Invoke-WithMaintenanceDirectory $SelectedBackup @(
            'run', '--rm', '--no-deps', 'postgres-maintenance', 'dropdb', '--maintenance-db=postgres',
            '--if-exists', '--force', $database
        )
        $null = Invoke-WithMaintenanceDirectory $SelectedBackup @(
            'run', '--rm', '--no-deps', 'postgres-maintenance', 'createdb', '--maintenance-db=postgres',
            '--template=template0', $database
        )
        $null = Invoke-WithMaintenanceDirectory $SelectedBackup @(
            'run', '--rm', '--no-deps', 'postgres-maintenance', 'pg_restore', '--exit-on-error', '--no-owner',
            '--no-privileges', "--dbname=$database", '/backup/database.dump'
        )
        Write-OperationLog 'Восстанавливаю изображения…'
        $null = Invoke-WithMaintenanceDirectory $SelectedBackup @(
            'run', '--rm', '--no-deps', 'minio-maintenance', 'mb', '--ignore-existing', "configurator/$bucket"
        )
        $null = Invoke-WithMaintenanceDirectory $SelectedBackup @(
            'run', '--rm', '--no-deps', 'minio-maintenance', 'mirror', '--overwrite', '--remove',
            '/backup/minio', "configurator/$bucket"
        )
        $null = Invoke-Compose @('up', '-d', '--remove-orphans')
        if (-not (Wait-Application)) { throw 'readiness failed' }
        $script:LastBackupDirectory = $safetyBackup
    }
    catch {
        $null = Invoke-Compose @('stop', 'gateway', 'app') -AllowFailure
        $script:LastBackupDirectory = $safetyBackup
        throw
    }
}

function Start-Configurator {
    if (Test-Port8080) { Stop-WithError $ExitConflict 'Порт 8080 занят другим приложением.' }
    Write-OperationLog 'Запускаю Configurator…'
    try { $null = Invoke-Compose @('up', '-d', '--remove-orphans') }
    catch { Stop-WithError $ExitConflict 'Не удалось запустить контейнеры. Проверьте порт 8080 и Docker Desktop.' }
    if (-not (Wait-Application)) {
        Write-SafeDiagnostics
        Stop-WithError $ExitReadiness 'Приложение не стало готово за отведённое время.'
    }
    Write-OperationLog 'Configurator готов: http://127.0.0.1:8080'
    if (-not $NoOpen) { Start-Process 'http://127.0.0.1:8080' | Out-Null }
}

function Stop-Configurator {
    Write-OperationLog 'Останавливаю Configurator без удаления данных…'
    try { $null = Invoke-Compose @('stop', 'gateway', 'app', 'minio', 'postgres') }
    catch { Stop-WithError $ExitDocker 'Не удалось остановить контейнеры.' }
    Write-OperationLog 'Configurator остановлен. Данные и backups сохранены.'
}

function Backup-Configurator {
    Write-OperationLog 'Создаю полный backup…'
    try { $null = New-FullBackup 'backup' }
    catch { Stop-WithError $ExitBackup 'Backup не создан. Текущее состояние сервисов восстановлено.' }
}

function Restore-Configurator {
    try { $selected = Select-BackupDirectory }
    catch { Stop-WithError $ExitRestore 'Не удалось выбрать backup.' }
    if (-not (Test-Backup $selected)) {
        Stop-WithError $ExitRestore 'Backup повреждён, неполон или имеет неподдерживаемый формат.'
    }
    if (-not (Confirm-Restore)) { Stop-WithError $ExitRestore 'Restore отменён: подтверждение не получено.' }
    Write-OperationLog "Начинаю Restore из $selected…"
    try { Invoke-Restore $selected }
    catch {
        Stop-WithError $ExitRestore "Restore завершился ошибкой. App/gateway остановлены. Страховочный backup: $script:LastBackupDirectory"
    }
    Write-OperationLog "Restore завершён. Страховочный backup: $script:LastBackupDirectory"
}

function Update-Configurator {
    Write-OperationLog 'Перед Update создаю обязательный backup…'
    try { $updateBackup = New-FullBackup 'pre-update' }
    catch { Stop-WithError $ExitUpdate 'Update отменён: обязательный backup не создан.' }
    Write-OperationLog 'Загружаю новые preview-образы…'
    try { $null = Invoke-Compose @('pull', 'app', 'gateway') }
    catch {
        $null = Invoke-Compose @('stop', 'gateway', 'app') -AllowFailure
        Stop-WithError $ExitUpdate "Не удалось загрузить preview-образы. App/gateway остановлены. Backup: $updateBackup"
    }
    Write-OperationLog 'Запускаю обновлённый Configurator…'
    try {
        $null = Invoke-Compose @('up', '-d', '--remove-orphans')
        if (-not (Wait-Application)) { throw 'readiness failed' }
    }
    catch {
        Write-SafeDiagnostics
        $null = Invoke-Compose @('stop', 'gateway', 'app') -AllowFailure
        Stop-WithError $ExitUpdate "Update не прошёл readiness. App/gateway остановлены; автоматический rollback запрещён. Backup: $updateBackup"
    }
    Write-OperationLog "Update завершён. Backup перед обновлением: $updateBackup"
}

Initialize-Log
try {
    Validate-Package
    Validate-Platform
    Acquire-Lock
    Validate-Docker
    Validate-Compose
    switch ($Operation) {
        'start' { Start-Configurator }
        'stop' { Stop-Configurator }
        'backup' { Backup-Configurator }
        'restore' { Restore-Configurator }
        'update' { Update-Configurator }
    }
}
finally {
    Release-Lock
}

exit 0
