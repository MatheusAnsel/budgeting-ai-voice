# Budgeting AI Voice

![Java 25](https://img.shields.io/badge/Java-25-007396)
![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F)
![Spring AI 2.0.0-M4](https://img.shields.io/badge/Spring%20AI-2.0.0--M4-6DB33F)
![MySQL](https://img.shields.io/badge/MySQL-9-4479A1)

API de controle de gastos com Spring Boot e Spring AI. Desafio do módulo Spring AI do Bootcamp Itaú Java com IA (DIO).

## O que o projeto faz

Registra e consulta transações financeiras por duas portas de entrada:

- **REST**: criar uma transação e listar transações por categoria.
- **Voz**: o usuário envia um áudio ("gastei 80 reais no mercado", "quanto gastei no mercado?"), a aplicação entende o pedido, executa a operação no banco e devolve a resposta falada em MP3.

Os dois caminhos usam os mesmos casos de uso da camada `application`, então a IA não acessa o banco por fora das regras de negócio.

## Fluxo

`POST /transactions/ai`:

1. O cliente envia um arquivo de áudio (multipart, campo `file`).
2. O `TranscriptionModel` (OpenAI Whisper, `whisper-1`) transcreve o áudio para texto em português.
3. O `ChatClient` (`gpt-4o-mini`) recebe o texto com o prompt de sistema (`prompts/system-message.st`) e decide qual tool chamar:
   - `persist-transaction`: registra uma transação;
   - `list-transactions-by-category`: lista as transações de uma categoria;
   - `sum-transactions-by-category`: soma o total gasto em uma categoria.
4. O caso de uso correspondente lê ou grava no MySQL.
5. O `TextToSpeechModel` (`gpt-4o-mini-tts`) converte a resposta final em áudio MP3, devolvido no corpo da resposta.

Camadas:

- `domain`: entidade `Transaction`, `Category`, `TransactionId` e o contrato `TransactionRepository`.
- `application`: casos de uso, expostos também como tools (`@Tool`) para o modelo.
- `infrastructure`: controller HTTP, tratamento de erros e adaptador JPA.

## Tecnologias

- Java 25
- Spring Boot 4.0.5 (Web, Data JPA, Docker Compose support)
- Spring AI 2.0.0-M4 (OpenAI: chat, transcrição e text-to-speech)
- MySQL 9.6 via Docker Compose
- Lombok
- JUnit 5, Mockito e AssertJ
- Gradle 9.4.1 (wrapper)

## Como executar

Pré-requisitos:

- JDK 25
- Docker (o Spring Boot sobe o MySQL do `compose.yml` automaticamente no `bootRun`)
- Uma chave da OpenAI na variável de ambiente `OPENAI_API_KEY`

```bash
export OPENAI_API_KEY="sua_chave_aqui"
```

```bash
./gradlew bootRun
```

A aplicação sobe em `http://localhost:8080`. O MySQL fica exposto na porta `3307`.

## Como testar

Testes automatizados (não precisam de chave; os testes de integração com a OpenAI são pulados quando `OPENAI_API_KEY` não está definida):

```bash
./gradlew test
```

Criar uma transação (valor em centavos):

```bash
curl -X POST localhost:8080/transactions -H "Content-Type: application/json" -d '{"description":"Mercado","category":"GROCERIES","amount":8000}'
```

Listar por categoria:

```bash
curl localhost:8080/transactions/GROCERIES
```

Enviar um comando de voz e salvar a resposta em áudio:

```bash
curl -X POST localhost:8080/transactions/ai -F "file=@src/test/resources/audio/recording-1.m4a" --output resposta.mp3
```

Categorias aceitas: `GROCERIES`, `PHARMA`, `AUTO`, `RESTAURANT`, `OTHER`.

## Melhorias implementadas

### 1. Correção do bug de centavos

O valor é armazenado em centavos (`long`), mas o `TransactionOutput` devolvia esse número como se fosse reais.

Antes:

```java
BigDecimal.valueOf(transaction.getAmount()).setScale(2, RoundingMode.HALF_UP)
```

```json
{"description":"Mercado","category":"GROCERIES","amount":8000.0}
```

Depois:

```java
BigDecimal.valueOf(amountInCents).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
```

```json
{"description":"Mercado","category":"GROCERIES","amount":80.0}
```

O erro também afetava a resposta falada, porque o modelo recebia 8000 como valor em reais.

### 2. Validação no domínio

- O construtor de `Transaction` lança `IllegalArgumentException` para descrição vazia, `amount <= 0` ou categoria nula. A regra fica no domínio e vale tanto para o REST quanto para as tools da IA.
- `ApiExceptionHandler` (`@RestControllerAdvice`) converte essa exceção em `ProblemDetail` (RFC 9457) com status 400.
- `POST /transactions/ai` retorna 400 quando o arquivo enviado está vazio, sem chamar a OpenAI.

Exemplo:

```json
{"title":"Requisição inválida","status":400,"detail":"A categoria da transação é obrigatória","instance":"/transactions"}
```

### 3. Novas categorias

Adicionadas `RESTAURANT` e `OTHER`. O prompt de sistema passou a instruir o modelo a usar `OTHER` quando nada se encaixa, a sempre informar valores em centavos às tools e a usar a tool de soma para totais.

### 4. Tool de soma por categoria

`SumTransactionsByCategoryUseCase`, exposto como `@Tool(name = "sum-transactions-by-category")`, devolve o total da categoria em reais (`BigDecimal`). Antes, para responder "quanto gastei no mercado?", o modelo listava as transações e somava sozinho, o que é sujeito a erro de cálculo. Agora a soma é feita em código, com aritmética inteira sobre centavos.

### 5. Testes que rodam sem chave

- `TransactionTest`: regras de validação do domínio.
- `TransactionOutputTest`: conversão de centavos (8000 vira 80.00).
- `SumTransactionsByCategoryUseCaseTest`: soma com repositório mockado (8000 + 4050 = 120.50).
- `BudgetingApplicationTests` anotado com `@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")`, assim como os testes de integração já existentes.

## Limitações conhecidas

- **Spring AI em milestone**: a versão 2.0.0-M4 não é GA; APIs e propriedades podem mudar até a versão final.
- **Transação sem data**: não há data na transação, então não é possível filtrar ou somar por período (por exemplo, "quanto gastei este mês").
- **`/ai` sem limite de tamanho ou formato**: o endpoint aceita qualquer arquivo não vazio, sem validar tipo de mídia nem tamanho além do limite padrão de multipart do Spring. Um arquivo inválido só falha na chamada à OpenAI.
- **Erros da OpenAI**: falhas de rede ou de autenticação com a OpenAI não têm tratamento específico; o cliente pode esperar as retentativas do Spring AI e receber um 500.
- **Valores em `double` na resposta HTTP**: `TransactionResponse` expõe o valor como `double`. Funciona para exibição, mas não é o tipo ideal para dinheiro.

## Próximos passos

- Adicionar data à transação e uma tool de soma por período, para responder perguntas como "quanto gastei este mês?".
- Validar tipo de mídia e tamanho do arquivo no `/ai` antes de chamar a OpenAI.
- Tratar falhas da OpenAI com uma resposta de erro clara (`ProblemDetail` 502 ou 503) em vez de 500.
- Trocar o `double` da resposta HTTP por `BigDecimal`.
- Atualizar para a versão GA do Spring AI quando for lançada.

## O que aprendi

- **Tool calling com Spring AI**: anotar casos de uso com `@Tool` e registrá-los no `ChatClient` permite que o modelo escolha a operação certa sem que a IA acesse o banco diretamente. A regra de negócio continua na camada `application`, e o modelo só decide qual ferramenta chamar e com quais parâmetros.
- **Pipeline de áudio**: o fluxo voz -> texto -> modelo -> texto -> voz é a composição de três modelos diferentes (`TranscriptionModel`, `ChatClient` e `TextToSpeechModel`). Cada etapa tem configuração própria (idioma, prompt de transcrição, voz, velocidade), e um erro em uma delas se propaga para a resposta final.
- **Dinheiro em centavos**: guardar valores como inteiros em centavos evita erro de arredondamento de ponto flutuante, mas exige converter na borda e deixar claro para o modelo em que unidade cada ferramenta trabalha. O bug do `TransactionOutput` mostrou que uma conversão esquecida passa despercebida até alguém olhar o número com atenção.
- **Não deixar o modelo fazer contas**: LLMs podem errar aritmética. Expor uma tool de soma e instruir o modelo a usá-la torna o total determinístico e testável.
- **Invariantes no domínio**: validar no construtor de `Transaction` garante a regra em todos os caminhos de entrada (REST e IA) de uma vez. Com `@RestControllerAdvice` e `ProblemDetail`, o erro chega ao cliente como 400 padronizado em vez de 500.
- **Testar sem depender de API externa**: com Mockito e testes unitários de domínio e casos de uso, a maior parte da lógica é verificada sem chave e sem rede. Os testes de integração com a OpenAI ficam condicionados à variável `OPENAI_API_KEY` com `@EnabledIfEnvironmentVariable`.

## Créditos

Projeto baseado no módulo `05-spring-ai` do curso de Spring Boot da DIO, ministrado pelo expert Poiani.

Repositório original: https://github.com/thpoiani/dio-spring-boot-learning-track
