param([string]$PostgresBin = 'C:\Program Files\PostgreSQL\18\bin')
$ErrorActionPreference = 'Stop'
$newsHubRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$newsHubBackend = Join-Path $newsHubRoot 'backend'
$newsHubLocal = Join-Path $newsHubBackend '.local'
$newsHubServer = Join-Path $newsHubBackend 'src\server.mjs'
$newsHubPidFile = Join-Path $newsHubLocal 'server.pid'

if (Test-Path -LiteralPath $newsHubPidFile) {
    $newsHubSavedPid = [int](Get-Content -LiteralPath $newsHubPidFile -Raw).Trim()
    $newsHubProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$newsHubSavedPid"
    if ($newsHubProcess) {
        if (-not $newsHubProcess.CommandLine -or -not $newsHubProcess.CommandLine.Contains($newsHubServer)) { throw 'PID belongs to another program; refusing to stop it.' }
        Stop-Process -Id $newsHubSavedPid
    }
}
$newsHubData = [IO.Path]::GetFullPath((Join-Path $newsHubLocal 'postgres'))
if (-not $newsHubData.StartsWith($newsHubBackend + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Invalid local database path' }
if (Test-Path -LiteralPath (Join-Path $newsHubData 'PG_VERSION')) {
    $newsHubPgCtl = Join-Path $PostgresBin 'pg_ctl.exe'
    & $newsHubPgCtl status -D $newsHubData
    if ($LASTEXITCODE -eq 0) {
        & $newsHubPgCtl stop -D $newsHubData -m fast -w
        if ($LASTEXITCODE -ne 0) { throw 'Could not stop the isolated database' }
    }
}
Write-Output 'Stopped the local NewsHub backend/database. Data is preserved.'
