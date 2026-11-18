import os
from tkinter import Tk
from tkinter.filedialog import askopenfilename
import numpy as np
import matplotlib.pyplot as plt
from sklearn.metrics.pairwise import cosine_similarity
from langchain_unstructured import UnstructuredLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_nvidia_ai_endpoints import NVIDIAEmbeddings

# NVIDIA API key is read from the environment
if not os.getenv("NVIDIA_API_KEY"):
    raise SystemExit("Set the NVIDIA_API_KEY environment variable first (see README).")

# Initialize the embedding model
embedder = NVIDIAEmbeddings(model="nvidia/nv-embed-v1")

# Function to plot similarity matrices
def plot_cross_similarity_matrix(emb1, emb2):
    cross_similarity_matrix = cosine_similarity(np.array(emb1), np.array(emb2))
    plt.imshow(cross_similarity_matrix, cmap="Greens", interpolation="nearest")
    plt.colorbar()
    plt.gca().invert_yaxis()
    plt.title("Cross-Similarity Matrix")
    plt.grid(True)

# Test functions
def load_document():
    print("Select a file to upload (e.g., PDF or text file).")
    Tk().withdraw()  # Close root window
    file_path = askopenfilename()
    if not file_path:
        print("No file selected.")
        return None

    try:
        loader = UnstructuredLoader(file_path)
        documents = loader.load()
        assert len(documents) > 0, "No documents loaded."
        print("Document loading succeeded.")
        return documents
    except Exception as e:
        print(f"Document loading failed: {e}")
        return None

def chunk_document(documents):
    try:
        text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=1200, chunk_overlap=100
        )
        docs_split = text_splitter.split_documents(documents)
        assert len(docs_split) > 0, "No chunks created."
        print(f"Document chunking succeeded. Number of chunks: {len(docs_split)}")
        return docs_split
    except Exception as e:
        print(f"Document chunking failed: {e}")
        return None

def generate_embeddings(docs_split):
    try:
        embeddings = [
            embedder._embed([chunk.page_content], model_type="query")[0]
            for chunk in docs_split
        ]
        assert len(embeddings) > 0, "No embeddings generated."
        print("Embedding generation succeeded.")
        print(f"Generated {len(embeddings)} embeddings, each with dimension {len(embeddings[0])}.")
        return embeddings
    except Exception as e:
        print(f"Embedding generation failed: {e}")
        return None

def visualize_similarity(embeddings):
    try:
        # Visualize cross-similarity matrix
        plt.figure(figsize=(8, 6))
        plot_cross_similarity_matrix(embeddings, embeddings)
        plt.xlabel("Chunk Embeddings")
        plt.ylabel("Chunk Embeddings")
        plt.show()
        print("Similarity visualization succeeded.")
    except Exception as e:
        print(f"Similarity visualization failed: {e}")

def demo_with_hardcoded_queries():
    print("Testing with hardcoded queries...")
    queries = [
        "What's the weather like in Rocky Mountains?",
        "What kinds of food is Italy known for?",
        "What's my name? I bet you don't remember...",
        "What's the point of life anyways?",
        "The point of life is to have fun :D"
    ]

    longer_docs = [
        f"This is a longer document related to the query: {query}" for query in queries
    ]
    print("Number of documents in longer_docs:", len(longer_docs))

    valid_docs = [doc for doc in longer_docs if doc.strip()]
    if len(valid_docs) > 0:
        longer_docs_cut = [doc[:2048] for doc in valid_docs]
        print(f"Number of valid documents: {len(longer_docs_cut)}")
    else:
        print("No valid documents to process.")
        longer_docs_cut = []

    if len(longer_docs_cut) > 0:
        embeddings = []
        for doc in longer_docs_cut:
            try:
                embeddings.append(embedder._embed([doc], model_type="query")[0])
            except Exception as e:
                print(f"Failed to generate embeddings for document: {doc[:100]} - Error: {e}")
        print("Embeddings shape:", np.array(embeddings).shape)
        if len(embeddings) > 0:
            visualize_similarity(embeddings)
    else:
        print("No valid documents for embedding.")

# Running tests
if __name__ == "__main__":
    print("Running document embedding tests...\n")
    documents = load_document()
    if documents:
        docs_split = chunk_document(documents)
        if docs_split:
            embeddings = generate_embeddings(docs_split)
            if embeddings:
                visualize_similarity(embeddings)
    else:
        print("No document loaded. Falling back to hardcoded queries...")
        demo_with_hardcoded_queries()

