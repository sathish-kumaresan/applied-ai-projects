# lcel_runnables_and_gradio.py

import os
from functools import partial
from operator import itemgetter
import gradio as gr
from langchain.schema.runnable import RunnableLambda, RunnablePassthrough
from langchain_nvidia_ai_endpoints import ChatNVIDIA
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate

# NVIDIA API key is read from the environment
if not os.getenv("NVIDIA_API_KEY"):
    raise SystemExit("Set the NVIDIA_API_KEY environment variable first (see README).")

# Initialize Chat Model
chat_llm = ChatNVIDIA(model="meta/llama3-8b-instruct")

# Define the rhyme_chain at the top for reuse
prompt = ChatPromptTemplate.from_messages([
    ("system", "Only respond in rhymes"),
    ("user", "{input}")
])
rhyme_chain = prompt | chat_llm | StrOutputParser()


# Define print_and_return function
def print_and_return(x, preface=""):
    print(f"{preface}{x}")
    return x


# Demo 1: Basic Runnable Examples
def demo_runnables():
    print("Running basic Runnable examples...")

    identity = RunnableLambda(lambda x: x)
    rprint0 = RunnableLambda(print_and_return)
    rprint1 = RunnableLambda(partial(print_and_return, preface="1: "))
    chain1 = identity | rprint0
    chain1.invoke("Hello World!")

    output = (
        chain1
        | rprint1
        | RunnableLambda(partial(print_and_return, preface="2: "))
    ).invoke("Welcome Home!")

    print("Output:", output)
    print("-" * 50)


# Demo 2: Simple Chat Pipeline with Rhyme
def demo_simple_chat_pipeline():
    print("Running Simple Chat Pipeline with rhyme constraint...")
    response = rhyme_chain.invoke({"input": "Tell me about birds!"})
    print(response)
    print("-" * 50)


# Demo 3: Gradio Interface for Chat Model
def run_gradio_interface():
    print("Launching Gradio Interface...")

    def rhyme_chat(message, history):
        return rhyme_chain.invoke({"input": message})

    # Launch Gradio interface
    gr.ChatInterface(rhyme_chat).launch()


# Demo 4: Zero-Shot Classification Chain
def demo_zero_shot_classification():
    print("Running Zero-Shot Classification example...")

    sys_msg = (
        "Choose the most likely topic classification given the sentence as context."
        " Only one word, no explanation.\n[Options : {options}]"
    )
    zsc_prompt = ChatPromptTemplate.from_messages([
        ("system", sys_msg),
        ("user", "[[{input}]]")
    ])
    zsc_chain = zsc_prompt | chat_llm | StrOutputParser()

    def zsc_call(input, options=["car", "boat", "airplane", "bike"]):
        return zsc_chain.invoke({"input": input, "options": options}).split()[0]

    print(zsc_call("Should I take the next exit, or keep going to the next one?"))
    print(zsc_call("I get seasick, so I think I'll pass on the trip"))
    print(zsc_call("I'm scared of heights, so flying probably isn't for me"))
    print("-" * 50)


# Demo 5: Multi-Component Chains
def demo_multi_component_chain():
    print("Running Multi-Component Chain example...")

    def make_dictionary(v, key):
        return {key: v} if not isinstance(v, dict) else v

    def RInput(key='input'):
        return RunnableLambda(partial(make_dictionary, key=key))

    def ROutput(key='output'):
        return RunnableLambda(partial(make_dictionary, key=key))

    def RPrint(preface=""):
        return RunnableLambda(partial(print_and_return, preface=preface))

    up_and_down = (
        RPrint("A: ")
        | RInput()
        | RPrint("B: ")
        | itemgetter("input")
        | RPrint("C: ")
        | {
            'word1': lambda x: x.split()[0],
            'word2': lambda x: x.split()[1],
            'words': lambda x: x,
        }
        | RPrint("D: ")
        | itemgetter("word1")
        | RPrint("E: ")
        | RunnableLambda(lambda x: x.upper())
        | RPrint("F: ")
        | ROutput()
    )
    print(up_and_down.invoke("Hello World"))
    print("-" * 50)


# Demo 6: Rhyme Re-themer Chatbot Exercise
def rhyme_chat2_stream(message, history, rhyme_chain, return_buffer=True):
    print("Running Rhyme Re-themer Chatbot Exercise...")
    buffer = "Here's a poem about your topic:\n\n"
    for token in rhyme_chain.stream({"input": message}):
        buffer += token
        yield buffer if return_buffer else token

    passage = "\n\nLet’s switch themes. What new topic would you like?"
    buffer += passage
    yield buffer if return_buffer else passage

    # TODO: Implement follow-up rhyme generation based on the new theme


def run_fake_streaming_gradio():
    history = [[None, "Let me help you make a poem! What would you like for me to write?"]]
    def queue_fake_streaming_gradio(chat_stream, max_questions=3):
        for _ in range(max_questions):
            message = input("\n[ Human ]: ")
            print("\n[ Agent ]: ")
            for token in chat_stream(message, history, rhyme_chain, return_buffer=False):
                print(token, end='')
            print("\n")

    queue_fake_streaming_gradio(rhyme_chat2_stream)
    print("-" * 50)


# Demo 7: LangServe RemoteRunnable (for LangServe environment)
def demo_langserve_runnable():
    from langserve import RemoteRunnable

    print("Calling LangServe RemoteRunnable...")
    llm = RemoteRunnable("http://0.0.0.0:9012/basic_chat/") | StrOutputParser()
    for token in llm.stream("Hello World! How is it going?"):
        print(token, end='')
    print("-" * 50)


# Run all demos
if __name__ == "__main__":
    print("Running LangChain demos...\n")
    demo_runnables()
    demo_simple_chat_pipeline()
    demo_zero_shot_classification()
    demo_multi_component_chain()
    # Uncomment the below line to launch the Gradio interface interactively
    # run_gradio_interface()
    run_fake_streaming_gradio()
    # Uncomment the below line to run the LangServe demo if LangServe is set up
    # demo_langserve_runnable()

