import os
from pydantic import BaseModel, Field
from functools import partial
from typing import List
from rich import print as pprint
from tkinter import Tk
from tkinter.filedialog import askopenfilename
from langchain_core.prompts import ChatPromptTemplate
from langchain_unstructured import UnstructuredLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_core.runnables import RunnableLambda, RunnableMap
from langchain_core.output_parsers import StrOutputParser, PydanticOutputParser
from langchain_nvidia_ai_endpoints import ChatNVIDIA

# NVIDIA API key is read from the environment
if not os.getenv("NVIDIA_API_KEY"):
    raise SystemExit("Set the NVIDIA_API_KEY environment variable first (see README).")

# Define DocumentSummaryBase for summarization state
class DocumentSummaryBase(BaseModel):
    running_summary: str = Field(
        "", description="Running description of the document. Do not override; only update!"
    )
    main_ideas: List[str] = Field(
        [], description="Most important information from the document (max 3)"
    )
    loose_ends: List[str] = Field(
        [], description="Open questions that would be good to incorporate into summary"
    )

# Set up summary prompt with the correct variables
summary_prompt = ChatPromptTemplate.from_template(
    "You are generating a running summary of the document. Make it readable by a technical user."
    " After this, the old knowledge base will be replaced by the new one. Make sure a reader can still understand everything."
    " Keep it short, but as dense and useful as possible! The information should flow from chunk to (loose ends or main ideas) to running_summary."
    " The updated knowledge base keeps all of the information from running_summary here: {info_base}."
    "\n\n{format_instructions}. Follow the format precisely, including quotations and commas"
    "\n\nWithout losing any of the info, update the knowledge base with the following: {input}"
)

# Define RExtract function with the appropriate inputs
def RExtract(pydantic_class, llm, prompt):
    parser = PydanticOutputParser(pydantic_object=pydantic_class)
    instruct_merge = RunnableMap(
        {"format_instructions": lambda x: parser.get_format_instructions(),
         "info_base": lambda x: x['info_base'],
         "input": lambda x: x['input']}
    )
    chain = instruct_merge | prompt | llm | parser
    return chain

# Define RSummarizer with correct invocation of RExtract
def RSummarizer(knowledge, llm, prompt, verbose=False, max_iterations=10):
    def summarize_docs(docs):
        parse_chain = RExtract(knowledge.__class__, llm, prompt)
        state = knowledge

        total_docs = len(docs)
        docs_per_iteration = max(1, total_docs // max_iterations)  # Ensure at least 1 chunk per iteration
        chunk_index = 0

        for i in range(max_iterations):
            if chunk_index >= total_docs:
                print("Reached maximum iterations.")
                break

            # Process the current set of chunks for this iteration
            iteration_docs = docs[chunk_index:chunk_index + docs_per_iteration]
            try:
                input_data = {
                    "info_base": state.json(),
                    "input": " ".join([doc.page_content for doc in iteration_docs]),
                }
                updated_state = parse_chain.invoke(input_data)
                state = updated_state
                chunk_index += docs_per_iteration  # Move to next set of chunks
                if verbose:
                    print(f"Processed iteration {i+1}")
                    pprint(state.dict())
            except Exception as e:
                print(f"Error on iteration {i+1}: {e}")
                print(f"Partial state: {state.dict()}")
                continue
        return state
    return RunnableLambda(summarize_docs)

# Test functions
def demo_document_loading():
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
        print("Document loading test passed.")
        return documents
    except Exception as e:
        print(f"Document loading test failed: {e}")
        return None

def demo_document_chunking(documents):
    try:
        text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=1200, chunk_overlap=100
        )
        docs_split = text_splitter.split_documents(documents)
        assert len(docs_split) > 0, "No chunks created."
        print(f"Document chunking test passed. Number of chunks: {len(docs_split)}")
        return docs_split
    except Exception as e:
        print(f"Document chunking test failed: {e}")
        return None

def demo_summarization(docs_split):
    try:
        instruct_model = ChatNVIDIA(
            model="mistralai/mixtral-8x22b-instruct-v0.1"
        ).bind(max_tokens=4096)
        instruct_llm = instruct_model | StrOutputParser()
        summarizer = RSummarizer(
            DocumentSummaryBase(), instruct_llm, summary_prompt, verbose=True, max_iterations=10
        )
        summary = summarizer.invoke(docs_split)
        assert summary, "Summarization output is empty."
        print("Summarization test passed.")
        pprint(summary.dict())
    except Exception as e:
        print(f"Summarization test failed: {e}")

# Running all tests
if __name__ == "__main__":
    print("Running document summarization tests...\n")
    documents = demo_document_loading()
    if documents:
        docs_split = demo_document_chunking(documents)
        if docs_split:
            demo_summarization(docs_split)

