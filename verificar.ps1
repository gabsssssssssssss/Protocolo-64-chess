$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
New-Item -ItemType Directory -Force -Path build | Out-Null
javac -encoding UTF-8 -cp 'vendor/*' -d build src/*.java
if ($LASTEXITCODE -ne 0) { throw 'Falha de compilação.' }
java -cp 'build;vendor/*' Verification --engine
if ($LASTEXITCODE -ne 0) { throw 'Falha de verificação.' }
java -cp 'build;vendor/*' SessionVerification
if ($LASTEXITCODE -ne 0) { throw 'Falha de verificação do salvamento.' }
java -cp 'build;vendor/*' TimeManagerVerification
if ($LASTEXITCODE -ne 0) { throw 'Falha de verificação da gestão própria.' }
java -cp 'build;vendor/*' ChessChallenge --ui-test
if ($LASTEXITCODE -ne 0) { throw 'Falha de verificação da interface.' }
java -cp 'build;vendor/*' PgnVerification
if ($LASTEXITCODE -ne 0) { throw 'Falha de verificação PGN.' }
java -cp 'build;vendor/*' BoardViewVerification
if ($LASTEXITCODE -ne 0) { throw 'Falha de verificação do tabuleiro.' }
java -cp 'build;vendor/*' AnalysisVerification
if ($LASTEXITCODE -ne 0) { throw 'Falha de verificação da análise.' }
java -cp 'build;vendor/*' TerminalVerification
if ($LASTEXITCODE -ne 0) { throw 'Falha de verificação do modo terminal.' }
java -cp 'build;vendor/*' OpeningVerification
if ($LASTEXITCODE -ne 0) { throw 'Falha de verificação das estatísticas de abertura.' }
