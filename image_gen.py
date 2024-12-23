from openai import OpenAI
from dotenv import load_dotenv
import os

load_dotenv()
api_key = os.getenv("OPENAI_API_KEY")

client = OpenAI(api_key=api_key)

response = client.images.generate(
    model="dall-e-3",
    prompt="In a photorealistic style: " + "The image depicts a serene landscape featuring a wooden pathway or boardwalk that leads through a grassy area. The scene includes lush green grass on either side of the path, with trees and shrubs in the background. The sky is bright with a few clouds, suggesting a pleasant and clear day. This setting evokes a sense of tranquility and nature.",
    size="1024x1024",
    quality="standard",
    n=1,
)

print(response.data[0].url)