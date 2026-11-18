# Building RAG Agents with LLMs (NVIDIA Deep Learning Institute)

My completed work for the NVIDIA DLI course **Building RAG Agents with Large Language Models**
(certificate, November 2024). The course poses each step as an exercise. These scripts are my solutions,
reorganised into four stages that build on each other, from calling an LLM endpoint to a full RAG pipeline
with automatic evaluation.

All models run on NVIDIA AI Foundation endpoints (NIM) through `langchain-nvidia-ai-endpoints`.

| Stage | Script | What it shows |
|---|---|---|
| [01 LLM endpoints and LCEL](01-llm-endpoints-and-lcel/) | `nim_chat_basics.py` | Querying NIM chat models: streaming, sampling parameters, system roles, model switching, multi-turn prompts |
| | `lcel_runnables_and_gradio.py` | LangChain Expression Language: runnables, zero-shot classification, multi-component chains with dict routing, a streaming Gradio chat UI, LangServe remote runnables |
| [02 Stateful chains and summarization](02-stateful-chains-and-summarization/) | `stateful_flight_assistant.py` | A dialogue agent that keeps a structured knowledge base (Pydantic) across turns and calls a lookup function once enough information is collected |
| | `running_document_summary.py` | Document loading and chunking, then a running summary that extracts structured knowledge chunk by chunk (`RExtract`, `RSummarizer`) |
| [03 Embeddings and vector stores](03-embeddings-and-vector-stores/) | `embedding_similarity.py` | NVIDIA embeddings (`nv-embed-v1`) and cosine-similarity analysis between queries and document chunks |
| | `arxiv_vector_store.py` | Loads seven arXiv papers (Transformer, BERT, RAG and others), chunks them, builds and merges FAISS stores, and saves the index |
| [04 RAG pipeline](04-rag-pipeline/) | `rag_over_arxiv_papers.py` | A document-chat RAG chain over the paper index, with long-context reordering, plus an **LLM-as-judge evaluation**: synthetic question-answer pairs, RAG answers, and a preference score |

## Run

```bash
pip install langchain langchain-community langchain-nvidia-ai-endpoints langchain-unstructured \
            faiss-cpu gradio rich arxiv pymupdf scikit-learn matplotlib
export NVIDIA_API_KEY=nvapi-...        # from build.nvidia.com; never commit it
```

Run the scripts from this folder. Stage 04 uses the index that stage 03 creates:

```bash
python 03-embeddings-and-vector-stores/arxiv_vector_store.py   # writes docstore_index.tgz
python 04-rag-pipeline/rag_over_arxiv_papers.py
```

## Other NVIDIA DLI certificates

- Generative AI with Diffusion Models (April 2025)
- Building LLM Applications with Prompt Engineering (October 2025)
- Disaster Risk Monitoring Using Satellite Imagery (March 2024)
