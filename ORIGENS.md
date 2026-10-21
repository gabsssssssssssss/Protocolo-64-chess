# Bases e dependências

## Stockfish

- Repositório: https://github.com/official-stockfish/Stockfish
- Release/tag: `sf_19` (Stockfish 19)
- Commit: `edb0d9db6731067ec50ce619ff372b463bc4dd5d`
- Pacote: https://github.com/official-stockfish/Stockfish/releases/download/sf_19/stockfish-windows-x86-64-universal.zip
- SHA-256 do ZIP: `3C8BF1F9EA66A09350A40DF4F632288285AC206D99F33AB5842C408FC30B48A7`
- Licença: GPLv3, texto em `LICENSE` e no pacote original.
- Nenhuma alteração no código C++. A contribuição decisória proposta está no orçamento de cálculo por posição em `TimeManager.allocateForPosition`, chamado pelo terminal e pela interface. A implementação foi elaborada com assistência do Codex e exige revisão e domínio técnico pela equipe; ver `DEFESA-TECNICA.md`. A aceitação da gestão externa e das condições de autoria deve ser confirmada com a organização.
- `TacticalSearch.mateInOne` implementa uma seleção externa exata de mate em um lance, usando regras da chesslib. Foi elaborada com assistência do Codex e não corresponde a uma modificação da busca interna do Stockfish.

## chesslib

- Repositório: https://github.com/bhlangonijr/chesslib
- Versão: `1.3.7`, Apache-2.0.
- JAR: https://jitpack.io/com/github/bhlangonijr/chesslib/1.3.7/chesslib-1.3.7.jar
- SHA-256: `A5AC968A30BC91BEE4A19D24981C40815D5140BDE518524FEF2A3679F965AD31`
- Utilizada para regras e geração de lances; não escolhe a jogada do motor.

## Apache Commons Lang

- Projeto: https://commons.apache.org/proper/commons-lang/
- Versão: `3.18.0`, Apache-2.0, dependência da chesslib.
- JAR: https://repo.maven.apache.org/maven2/org/apache/commons/commons-lang3/3.18.0/commons-lang3-3.18.0.jar
- SHA-256: `4EEEAE8D20C078ABB64B015EC158ADD383AC581571CDDC45C68F0C9AE0230720`

Os arquivos de terceiros são baixados para `vendor/`, excluído do Git. Preserve o pacote original do Stockfish e os avisos META-INF dos JARs ao distribuir. Antes do credenciamento, confirme com a banca a aceitação do componente externo e declare essas bases.
