# Package `ca.yorku.eecs3311.studyagent.integration`

AI / Integration layer: provider-independent LLM access (Adapter pattern), embeddings and vector search, document parsing and chunking, notifications.

> Stage 1 is the design stage, so this folder contains no implementation yet. The classes listed below come from the Stage 1 class model (`docs/diagrams/class-model.iuml`) and will be implemented here in Stage 2.

| Class | Kind | Notes |
|---|---|---|
| `LLMClient` | interface | «interface» «Target» |
| `GeminiLLMAdapter` | class | «Adapter» |
| `OpenAILLMAdapter` | class | «Adapter» |
| `GeminiSdkClient` | class | «Adaptee, external» |
| `OpenAISdkClient` | class | «Adaptee, external» |
| `LLMRequest` | class |  |
| `LLMResponse` | class |  |
| `EmbeddingService` | interface | «interface» |
| `VectorStore` | interface | «interface» |
| `RetrievalService` | class |  |
| `RetrievedChunk` | class |  |
| `DocumentParser` | interface | «interface» |
| `PdfParser` | class |  |
| `DocxParser` | class |  |
| `PlainTextParser` | class |  |
| `ParsedDocument` | class |  |
| `Chunker` | class |  |
| `NotificationGateway` | interface | «interface» |
| `DesktopNotificationGateway` | class |  |

Planned sub-packages: `integration.llm`, `integration.retrieval`, `integration.parsing`, `integration.notification`.
