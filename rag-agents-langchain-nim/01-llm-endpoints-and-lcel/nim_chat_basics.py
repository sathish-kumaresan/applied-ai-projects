# nim_chat_basics.py

from langchain_nvidia_ai_endpoints import ChatNVIDIA
import os

# NVIDIA API key is read from the environment
if not os.getenv("NVIDIA_API_KEY"):
    raise SystemExit("Set the NVIDIA_API_KEY environment variable first (see README).")

# Initialize the base model
llm = ChatNVIDIA(model="mistralai/mixtral-8x7b-instruct-v0.1")


def demo_basic_query():
    """Step 1: Basic query to check connection"""
    response = llm.invoke("How do you say 'hello' in Swedish?")
    print("Basic Query Response:", response.content)


def demo_streaming_response():
    """Step 2: Streaming response for a long output"""
    print("Streaming response for a detailed query:")
    for token in llm.stream("Describe the process of photosynthesis in detail."):
        print(token.content, end="")
    print("\n")


def demo_model_parameters():
    """Step 3: Experiment with model parameters"""
    response = llm.invoke(
        "Tell me a creative story about a journey to Mars.",
        temperature=0.9,
        max_tokens=150
    )
    print("Creative Story:", response.content)


def demo_role_based_query():
    """Step 4: Use role-based query for context"""
    input_text = (
        "System: You are a friendly AI assistant.\n"
        "User: What advice do you have for learning Python as a beginner?"
    )
    response = llm.invoke(input_text)
    print("Role-Based Advice:", response.content)


def list_available_models():
    """Step 5: Retrieve and print all available models"""
    available_models = ChatNVIDIA.get_available_models()
    print("Available Models:")
    for model_card in available_models:
        print(f" - {model_card.id}")


def demo_alternate_model():
    """Step 6: Try a specific alternate model if available"""
    # Replace with an available model ID from list_available_models()
    another_llm = ChatNVIDIA(model="mistralai/mistral-7b-instruct-v0.3")
    response = another_llm.invoke("What is the capital of Sweden?")
    print("Capital of Sweden:", response.content)


def demo_sequential_prompts():
    """Step 7: Try contextual or sequential prompts"""
    response1 = llm.invoke("What are black holes?")
    print("Response 1:", response1.content)

    response2 = llm.invoke("Can you explain how they form?")
    print("Follow-Up Response:", response2.content)


# Run all demos
if __name__ == "__main__":
    print("Running NVIDIA LLM demos...\n")
    demo_basic_query()
    print("\n" + "-"*50)
    demo_streaming_response()
    print("\n" + "-"*50)
    demo_model_parameters()
    print("\n" + "-"*50)
    demo_role_based_query()
    print("\n" + "-"*50)
    list_available_models()
    print("\n" + "-"*50)
    demo_alternate_model()
    print("\n" + "-"*50)
    demo_sequential_prompts()
    print("\n" + "-"*50)

