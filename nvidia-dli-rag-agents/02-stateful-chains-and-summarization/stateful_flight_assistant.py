# state_chain_test.py

import os
from pydantic import BaseModel, Field  # Updated import from pydantic directly
from langchain_core.prompts import ChatPromptTemplate
from langchain.schema.runnable import RunnableConfig  # Use RunnableConfig if RunnableAssign is unavailable
from functools import partial

from langchain_core.runnables import RunnableLambda
from langchain_nvidia_ai_endpoints import ChatNVIDIA
from langchain_core.output_parsers import StrOutputParser

# Setup Environment for NVIDIA API
if not os.getenv("NVIDIA_API_KEY"):
    raise SystemExit("Set the NVIDIA_API_KEY environment variable first (see README).")

# Define KnowledgeBase model with fields
class KnowledgeBase(BaseModel):
    first_name: str = Field('unknown', description="User's first name")
    last_name: str = Field('unknown', description="User's last name")
    confirmation: int = Field(-1, description="Flight confirmation number")
    discussion_summary: str = Field("", description="Summary of discussion")
    open_problems: list = Field([], description="Unresolved user queries")
    current_goals: list = Field([], description="Goals to address")

# Define the function to simulate a database query
def get_flight_info(data):
    """
    This function simulates a retrieval from a flight database.
    """
    sample_data = {
        "Jane|Doe|12345": {
            "first_name": "Jane",
            "last_name": "Doe",
            "confirmation": 12345,
            "departure": "San Jose",
            "destination": "New Orleans",
            "departure_time": "12:30 PM",
            "arrival_time": "9:30 PM",
            "flight_day": "tomorrow"
        }
    }
    key = f"{data.get('first_name')}|{data.get('last_name')}|{data.get('confirmation')}"
    if key in sample_data:
        flight = sample_data[key]
        return (
            f"{flight['first_name']} {flight['last_name']}'s flight from {flight['departure']} to {flight['destination']} "
            f"departs at {flight['departure_time']} {flight['flight_day']} and lands at {flight['arrival_time']}."
        )
    return "Flight information could not be found."

# Define prompt templates for different states
external_prompt = ChatPromptTemplate.from_template(
    "SkyFlow chatbot: Here's the current knowledge base: {know_base}. Retrieval result: {context}. "
    "If info is missing, ask user for details. Do not ask unnecessary questions."
)

# Create the chat model
chat_llm = ChatNVIDIA(model="meta/llama3-70b-instruct") | StrOutputParser()

# Define helper functions to facilitate testing
def generate_basic_response(input_text, context="", know_base={}):
    response = external_prompt | chat_llm
    return response.invoke({'input': input_text, 'context': context, 'know_base': know_base})

# Main test cases

def demo_basic_knowledge_base():
    print("Testing Basic KnowledgeBase Creation")
    kb = KnowledgeBase(first_name="Jane", last_name="Doe", confirmation=12345)
    assert kb.first_name == "Jane", "First name does not match expected value"
    assert kb.last_name == "Doe", "Last name does not match expected value"
    assert kb.confirmation == 12345, "Confirmation number does not match expected value"

def demo_flight_info_retrieval():
    print("Testing Flight Info Retrieval")
    demo_data = {"first_name": "Jane", "last_name": "Doe", "confirmation": 12345}
    flight_info = get_flight_info(demo_data)
    print("Flight Info Retrieved:", flight_info)  # Debugging line to inspect the result
    assert "San Jose to New Orleans" in flight_info, "Flight details did not match expected output."

def demo_basic_response_generation():
    print("Testing Basic Response Generation")
    response = generate_basic_response("What is my flight info?", "Flight departs at 12:30 PM tomorrow.")
    print("Generated Response:", response)  # Debugging line to inspect the result
    assert "departs at 12:30 PM" in response, "Generated response did not match expected output."

def demo_chatbot_response_with_missing_info():
    print("Testing ChatBot with Missing Info")
    incomplete_kb = KnowledgeBase(first_name="John", last_name="Smith")
    response = generate_basic_response("Can you tell me my flight info?", get_flight_info(incomplete_kb.dict()), incomplete_kb.dict())
    print("Response:", response)  # Print the response for inspection
    # Check if the response requests additional information
    assert any(
        phrase in response for phrase in ["could not be found", "provide me with your flight number", "help me better assist you"]
    ), "Expected bot to request more info."

def demo_flight_info_with_knowledge_update():
    print("Testing Flight Info with Knowledge Update")
    kb = KnowledgeBase(first_name="Jane", last_name="Doe", confirmation=12345)
    flight_info = get_flight_info(kb.dict())  # Retrieve flight info based on the knowledge base
    print("Flight Info:", flight_info)  # Debugging line to inspect the result
    response = generate_basic_response("My flight confirmation is 12345", flight_info, kb.dict())
    print("Generated Response:", response)  # Debugging line to inspect the result
    assert "departs at" in response, "Response should confirm departure time."

if __name__ == "__main__":
    print("Running tests...\n")
    demo_basic_knowledge_base()
    demo_flight_info_retrieval()
    demo_basic_response_generation()
    demo_chatbot_response_with_missing_info()
    demo_flight_info_with_knowledge_update()
    print("\nAll tests completed.")

