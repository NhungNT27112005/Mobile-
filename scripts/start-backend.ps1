param(
    [string]$PostgresBin = 'C:\Program Files\PostgreSQL\18\bin',
    [ValidateRange(1024,65535)][int]$DatabasePort = 55432,
    [ValidateRange(1024,65535)][int]$ApiPort = 8000
)
$ErrorActionPreference = 'Stop'
$newsHubRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$newsHubBackend = Join-Path $newsHubRoot 'backend'
$newsHubLocal = Join-Path $newsHubBackend '.local'
$newsHubServer = Join-Path $newsHubBackend 'src\server.mjs'
$newsHubPidFile = Join-Path $newsHubLocal 'server.pid'
$newsHubNode = (Get-Command node -ErrorAction Stop).Source
$newsHubPgCtl = Join-Path $PostgresBin 'pg_ctl.exe'
$newsHubData = [IO.Path]::GetFullPath((Join-Path $newsHubLocal 'postgres'))
if (-not $newsHubData.StartsWith($newsHubBackend + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Invalid local database path' }
if (-not (Test-Path -LiteralPath $newsHubPgCtl)) { throw 'PostgreSQL tools not found. Supply -PostgresBin or use Docker.' }

if (Test-Path -LiteralPath $newsHubPidFile) {
    $newsHubSavedPid = [int](Get-Content -LiteralPath $newsHubPidFile -Raw).Trim()
    $newsHubExisting = Get-CimInstance Win32_Process -Filter "ProcessId=$newsHubSavedPid"
    if ($newsHubExisting -and $newsHubExisting.CommandLine -and $newsHubExisting.CommandLine.Contains($newsHubServer)) {
        $newsHubHealth = Invoke-RestMethod -Uri "http://127.0.0.1:$ApiPort/health" -TimeoutSec 5
        if ($newsHubHealth.service -eq 'newshub-api') {
            Write-Output "NewsHub is already running at http://localhost:$ApiPort"
            return
        }
        throw 'Existing NewsHub process is not healthy. Stop it before restarting.'
    }
}
if (Get-NetTCPConnection -State Listen -LocalPort $ApiPort -ErrorAction SilentlyContinue) { throw "Port $ApiPort is already in use." }

if (-not (Test-Path -LiteralPath (Join-Path $newsHubBackend 'node_modules'))) {
    Push-Location $newsHubBackend
    try {
        & npm.cmd ci --ignore-scripts
        if ($LASTEXITCODE -ne 0) { throw 'npm ci failed' }
    } finally { Pop-Location }
}
& $newsHubNode (Join-Path $PSScriptRoot 'native-db.mjs') configure $DatabasePort $ApiPort
if ($LASTEXITCODE -ne 0) { throw 'Local configuration failed' }

if (-not (Test-Path -LiteralPath (Join-Path $newsHubData 'PG_VERSION'))) {
    if (Get-NetTCPConnection -State Listen -LocalPort $DatabasePort -ErrorAction SilentlyContinue) { throw "Database port $DatabasePort is already in use." }
    $newsHubInitArgs = @(
        '-D', ('"' + $newsHubData + '"'),
        '-U', 'newshub', '--auth=scram-sha-256', '--encoding=UTF8',
        '--locale-provider=builtin', '--builtin-locale=C.UTF-8',
        ('--pwfile="' + (Join-Path $newsHubLocal 'postgres-password.txt') + '"')
    )
    $newsHubInit = Start-Process -FilePath (Join-Path $PostgresBin 'initdb.exe') -ArgumentList $newsHubInitArgs -Wait -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $newsHubLocal 'initdb.log') -RedirectStandardError (Join-Path $newsHubLocal 'initdb-error.log')
    if ($newsHubInit.ExitCode -ne 0) { throw 'Isolated PostgreSQL initialization failed; see backend/.local/initdb-error.log' }
}
& $newsHubPgCtl status -D $newsHubData
if ($LASTEXITCODE -ne 0) {
    if (Get-NetTCPConnection -State Listen -LocalPort $DatabasePort -ErrorAction SilentlyContinue) { throw "Database port $DatabasePort is occupied by another server." }
    & $newsHubPgCtl start -D $newsHubData -l (Join-Path $newsHubLocal 'postgres.log') -o "-p $DatabasePort -h 127.0.0.1 -c max_connections=20 -c shared_buffers=32MB" -w
    if ($LASTEXITCODE -ne 0) { throw 'Could not start the isolated PostgreSQL server' }
}
& $newsHubNode (Join-Path $PSScriptRoot 'native-db.mjs') create-databases
if ($LASTEXITCODE -ne 0) { throw 'Database creation failed' }

$newsHubArgs = @(
    ('--env-file="' + (Join-Path $newsHubLocal 'native.env') + '"'),
    ('"' + $newsHubServer + '"')
)
$newsHubProcess = Start-Process -FilePath $newsHubNode -ArgumentList $newsHubArgs -WorkingDirectory $newsHubRoot -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $newsHubLocal 'server.log') -RedirectStandardError (Join-Path $newsHubLocal 'server-error.log')
Set-Content -LiteralPath $newsHubPidFile -Value $newsHubProcess.Id
for ($newsHubAttempt = 0; $newsHubAttempt -lt 30; $newsHubAttempt++) {
    try {
        $newsHubHealth = Invoke-RestMethod -Uri "http://127.0.0.1:$ApiPort/health" -TimeoutSec 2
        if ($newsHubHealth.service -eq 'newshub-api') {
            Write-Output "NewsHub backend is ready: http://localhost:$ApiPort/health"
            return
        }
    } catch {}
    if ($newsHubProcess.HasExited) { throw 'Backend exited; see backend/.local/server-error.log' }
    Start-Sleep -Milliseconds 500
}
throw 'Backend did not become ready; see backend/.local/server-error.log'
