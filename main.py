import base64
import os
import requests
import time
from openai import OpenAI
from dotenv import load_dotenv

endpoint = "http://10.0.0.1:10000/sony/camera"

def extract_urls(data):
    """
    Recursively extracts all URLs (strings containing 'http') from the data.
    Returns a list of URLs.
    """
    urls = []
    if isinstance(data, str):
        if "http" in data:
            urls.append(data)
    elif isinstance(data, list):
        for item in data:
            urls.extend(extract_urls(item))
    elif isinstance(data, dict):
        for value in data.values():
            urls.extend(extract_urls(value))
    return urls

def get_photo_from_camera():
    """
    Polls the camera for shutter events and returns the first URL found.
    """
    payload = {
        "method": "getEvent",
        "params": [False],
        "id": 1,
        "version": "1.0"
    }

    print("ready for photography...")
    while True:
        try:
            response = requests.post(endpoint, json=payload)
            if response.status_code == 200:
                events = response.json()

                for event in events.get("result", []):
                    urls = extract_urls(event)
                    if urls:
                        print(f"URL found: {urls[0]}")
                        return urls[0]

        except Exception as e:
            print(f"Error polling events: {e}")

        time.sleep(0.5)

def save_image(url: str):
    """
    Downloads the image from the given URL and saves it to a file.
    """
    try:
        response = requests.get(url, stream=True)
        if response.status_code == 200:
            save_directory = "img"
            os.makedirs(save_directory, exist_ok=True)
            filename = os.path.join(save_directory, os.path.basename(url.split('?')[0]) or "downloaded_image.jpg")

            with open(filename, 'wb') as image_file:
                image_file.write(response.content)

            print(f"Image saved as: {filename}")
            return filename
        else:
            print(f"Failed to download image. Status code: {response.status_code}")
    except Exception as e:
        print(f"Error saving image: {e}")


def send_to_openai(path: str):
    client = OpenAI(api_key = os.getenv("OPENAI_API_KEY"))
    with open(path, "rb") as image_file:
        encoded_img = base64.b64encode(image_file.read()).decode("utf-8")
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "text",
                            "text": "Describe the image in detail.",
                        },
                        {
                            "type": "image_url",
                            "image_url": {"url": f"data:image/jpeg;base64,{encoded_img}"},
                        },
                    ],
                }
            ],
        )
    print(f"{response.choices[0].message.content}")
    return response.choices[0].message.content


def send_to_dalle(caption):
    client = OpenAI(api_key = os.getenv("OPENAI_API_KEY"))
    response = client.images.generate(
        model="dall-e-3",
        prompt="In a photorealistic style: " + caption,
        size="1024x1024",
        quality="standard",
        n=1,
    )
    print(response.data[0].url)
    return response.data[0].url


if __name__ == "__main__":
    load_dotenv()
    url = get_photo_from_camera()
    if url:
        path = save_image(url)
        input("Switch the wifi and press a key to proceed.")
        print(f"proceeding with image: {path}")
        caption = send_to_openai(path)
        image = send_to_dalle(caption)
