$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (!(Test-Path -LiteralPath '.env')) { Copy-Item -LiteralPath '.env.example' -Destination '.env' }
foreach ($line in Get-Content -LiteralPath '.env') {
    if ($line -match '^([A-Z_][A-Z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}
$dbPort = if ($env:DB_PORT) { $env:DB_PORT } else { '5433' }
$dbName = if ($env:DB_NAME) { $env:DB_NAME } else { 'yalla_livri' }
$env:DB_URL = "jdbc:postgresql://localhost:${dbPort}/${dbName}"
& .\mvnw.cmd spring-boot:run
exit $LASTEXITCODE
