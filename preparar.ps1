$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
Set-Location -LiteralPath $PSScriptRoot
New-Item -ItemType Directory -Force -Path vendor | Out-Null
Invoke-WebRequest -Uri 'https://jitpack.io/com/github/bhlangonijr/chesslib/1.3.7/chesslib-1.3.7.jar' -OutFile 'vendor/chesslib-1.3.7.jar'
Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/commons/commons-lang3/3.18.0/commons-lang3-3.18.0.jar' -OutFile 'vendor/commons-lang3-3.18.0.jar'
Invoke-WebRequest -Uri 'https://github.com/official-stockfish/Stockfish/releases/download/sf_19/stockfish-windows-x86-64-universal.zip' -OutFile 'vendor/stockfish.zip'
Expand-Archive -LiteralPath 'vendor/stockfish.zip' -DestinationPath 'vendor/engine' -Force
$expected = @{
    'vendor/stockfish.zip' = '3C8BF1F9EA66A09350A40DF4F632288285AC206D99F33AB5842C408FC30B48A7'
    'vendor/chesslib-1.3.7.jar' = 'A5AC968A30BC91BEE4A19D24981C40815D5140BDE518524FEF2A3679F965AD31'
    'vendor/commons-lang3-3.18.0.jar' = '4EEEAE8D20C078ABB64B015EC158ADD383AC581571CDDC45C68F0C9AE0230720'
}
foreach ($file in $expected.Keys) {
    if ((Get-FileHash -Algorithm SHA256 -LiteralPath $file).Hash -ne $expected[$file]) { throw "Hash inesperado: $file" }
}
Write-Host 'Preparação concluída. Execute iniciar.bat. As partidas funcionam offline.'
