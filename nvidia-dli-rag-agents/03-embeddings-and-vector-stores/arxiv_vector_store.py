import os
import json
import tarfile
from langchain_community.document_loaders import ArxivLoader
from langchain_community.document_transformers import LongContextReorder
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import FAISS
from langchain_nvidia_ai_endpoints import NVIDIAEmbeddings, ChatNVIDIA
from faiss import IndexFlatL2
from langchain_community.docstore.in_memory import InMemoryDocstore
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_core.runnables import RunnableLambda

# NVIDIA API key is read from the environment
if not os.getenv("NVIDIA_API_KEY"):
    raise SystemExit("Set the NVIDIA_API_KEY environment variable first (see README).")

embedder = NVIDIAEmbeddings(model="nvidia/nv-embed-v1", truncate="END")
instruct_llm = ChatNVIDIA(model="mistralai/mixtral-8x7b-instruct-v0.1")

text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=1000, chunk_overlap=100,
    separators=["\n\n", "\n", ".", ";", ",", " "],
)

print("Loading Documents...")
docs = [
    ArxivLoader(query="1706.03762").load(),
    ArxivLoader(query="1810.04805").load(),
    ArxivLoader(query="2005.11401").load(),
    ArxivLoader(query="2205.00445").load(),
    ArxivLoader(query="2310.06825").load(),
    ArxivLoader(query="2306.05685").load(),
    ArxivLoader(query="2411.09269").load(),
]

for doc in docs:
    content = doc[0].page_content
    if "References" in content:
        doc[0].page_content = content.split("References")[0]

print("Processing Documents...")
docs_chunks = [text_splitter.split_documents(doc) for doc in docs]
docs_chunks = [[c for c in dchunks if len(c.page_content) > 200] for dchunks in docs_chunks]

doc_string = "Available Documents:"
doc_metadata = []
for chunks in docs_chunks:
    metadata = getattr(chunks[0], 'metadata', {})
    doc_string += "\n - " + metadata.get('Title', "Unknown Title")
    doc_metadata += [str(metadata)]

extra_chunks = [doc_string] + doc_metadata

vecstores = [FAISS.from_texts(extra_chunks, embedder)]
vecstores += [FAISS.from_documents(doc_chunks, embedder) for doc_chunks in docs_chunks]

embed_dims = len(embedder.embed_query("test"))
def default_FAISS():
    return FAISS(
        embedding_function=embedder,
        index=IndexFlatL2(embed_dims),
        docstore=InMemoryDocstore(),
        index_to_docstore_id={},
        normalize_L2=False
    )

def aggregate_vstores(vectorstores):
    agg_vstore = default_FAISS()
    for vstore in vectorstores:
        agg_vstore.merge_from(vstore)
    return agg_vstore

docstore = aggregate_vstores(vecstores)
print(f"Constructed aggregate docstore with {len(docstore.docstore._dict)} chunks")

def docs2str(docs):
    return "\n".join([doc.page_content for doc in docs])

long_reorder = LongContextReorder(max_tokens=4096)
retrieval_chain = (
    RunnableLambda(lambda x: {"history": docs2str(docstore.similarity_search(x["input"]))})
)

def chat_gen(message, history=[]):
    buffer = ""
    retrieval = retrieval_chain.invoke({"input": message})
    response = instruct_llm.invoke(retrieval["history"])
    buffer += response.content
    print(f"Q: {message}\nA: {response.content}")
    return buffer

demo_question = "Tell me about RAG!"
chat_gen(demo_question)

docstore.save_local("docstore_index")

print("Compressing index...")
with tarfile.open("docstore_index.tgz", "w:gz") as tar:
    tar.add("docstore_index", arcname=os.path.basename("docstore_index"))

if os.path.exists("docstore_index"):
    print("Removing old index directory...")
    os.system("rm -rf docstore_index")

print("Extracting compressed index...")
with tarfile.open("docstore_index.tgz", "r:gz") as tar:
    tar.extractall()

print("Loading extracted index into FAISS...")
new_db = FAISS.load_local("docstore_index", embedder, allow_dangerous_deserialization=True)
docs = new_db.similarity_search("Testing the index")
if docs:
    print(docs[0].page_content[:1000])

if os.path.exists("docstore_index"):
    os.system("rm -rf docstore_index")

