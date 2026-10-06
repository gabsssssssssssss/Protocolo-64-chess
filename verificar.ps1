$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
New-Item -ItemType Directory -Force -Path build | Out-Null
javac -encoding UTF-8 -cp 'vendor/*' -d build src/*.java
if ($LASTEXITCODE -ne 0) { throw 'Falha de compilação.' }
java -cp 'build;vendor/*' Verification --engine
if ($LASTEXITCODE -ne 0) { throw 'Falha de verificação.' }
java -cp 'build;vendor/*' ChessChallenge --ui-test
if ($LASTEXITCODE -ne 0) { throw 'Falha de verificação da interface.' }
java -cp 'build;vendor/*' PgnVerification
if ($LASTEXITCODE -ne 0) { throw 'Falha de verificação PGN.' }
java -cp 'build;vendor/*' BoardViewVerification
if ($LASTEXITCODE -ne 0) { throw 'Falha de verificação do tabuleiro.' }
