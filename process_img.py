from openai import OpenAI
import requests


def caption(client: OpenAI, prompt: str, url: str) -> str:
    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": prompt},
                    {
                        "type": "image_url",
                        "image_url": {
                            "url": url
                        },
                    },
                ],
            }
        ],
        max_tokens=300,
    )
    return response.choices[0].message.content

def host_image(path: str):
    with open(path, "rb") as f:
        response = requests.post("https://transfer.sh", files={"file": f})

    if response.status_code == 200:
        print("Image URL:", response.text.strip())
    else:
        print("Failed to upload:", response.status_code, response.text)