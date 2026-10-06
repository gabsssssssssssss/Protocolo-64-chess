param([int]$Segundos = 30, [switch]$Assistir)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
New-Item -ItemType Directory -Force -Path build | Out-Null
javac -encoding UTF-8 -cp 'vendor/*' -d build src/*.java
if ($LASTEXITCODE -ne 0) { throw 'Falha de compilação.' }
if ($Assistir) { java -cp 'build;vendor/*' MatchRunner $Segundos --watch }
else { java -cp 'build;vendor/*' MatchRunner $Segundos }
if ($LASTEXITCODE -ne 0) { throw 'Falha na comparação.' }
