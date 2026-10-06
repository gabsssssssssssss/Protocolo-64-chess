$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
New-Item -ItemType Directory -Force -Path build | Out-Null
javac -encoding UTF-8 -cp 'vendor/*' -d build src/*.java
if ($LASTEXITCODE -ne 0) { throw 'Falha de compilação.' }
java -cp 'build;vendor/*' PgnViewer datasets/exemplo.pgn
