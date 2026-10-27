$ErrorActionPreference = 'Stop'
$newsHubRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$newsHubEnv = Join-Path $newsHubRoot 'backend\.local\native.env'
if (-not (Test-Path -LiteralPath $newsHubEnv)) { throw 'Run scripts/start-backend.ps1 first.' }
Push-Location $newsHubRoot
try {
    & node ('--env-file=' + $newsHubEnv) --test --test-concurrency=1 'backend/tests/*.test.mjs'
    if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed' }
} finally { Pop-Location }
