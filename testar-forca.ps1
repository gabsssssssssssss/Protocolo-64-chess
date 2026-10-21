param(
    [ValidateRange(1,30000)][int]$Milissegundos = 1000,
    [ValidateRange(1,6)][int]$Aberturas = 3,
    [ValidateRange(5,400)][int]$LimiteMeiosLances = 200
)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
New-Item -ItemType Directory -Force -Path build | Out-Null
javac -encoding UTF-8 -cp 'vendor/*' -d build src/*.java
if ($LASTEXITCODE -ne 0) { throw 'Falha de compilação.' }
java -cp 'build;vendor/*' PositionMatchRunner $Milissegundos $Aberturas $LimiteMeiosLances
if ($LASTEXITCODE -ne 0) { throw 'Teste interrompido. Confira os PGNs e relatórios já salvos.' }
