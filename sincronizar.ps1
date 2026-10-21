param(
    [Parameter(Mandatory=$true)]
    [ValidateNotNullOrEmpty()]
    [string]$Mensagem
)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($Mensagem)) { throw 'Descreva a alteração para o commit.' }
git remote get-url origin | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'O GitHub ainda não foi configurado neste projeto.' }
$projectBranch = git branch --show-current
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($projectBranch)) { throw 'Selecione uma branch antes de sincronizar.' }
git add --all
if ($LASTEXITCODE -ne 0) { throw 'Falha ao preparar os arquivos para commit.' }
git diff --cached --quiet
$changeStatus = $LASTEXITCODE
if ($changeStatus -eq 1) {
    git commit -m $Mensagem
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar o commit. O envio não foi executado.' }
} elseif ($changeStatus -ne 0) {
    throw 'Não foi possível verificar as alterações.'
} else {
    Write-Host 'Sem alterações novas; enviando commits locais pendentes.'
}
git push --set-upstream origin $projectBranch
if ($LASTEXITCODE -ne 0) { throw 'O commit está salvo localmente, mas o envio falhou. Confira a conexão e o login do GitHub e execute novamente.' }
Write-Host 'Histórico sincronizado com o GitHub.'
