$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (-not (Test-Path -LiteralPath 'vendor/chesslib-1.3.7.jar')) { throw 'Execute preparar.ps1 primeiro.' }
New-Item -ItemType Directory -Force -Path build | Out-Null
javac -encoding UTF-8 -cp 'vendor/*' -d build src/*.java
if ($LASTEXITCODE -ne 0) { throw 'Falha de compilação.' }
java -cp 'build;vendor/*' TerminalGame
if ($LASTEXITCODE -ne 0) { throw 'Falha no modo de campeonato.' }
