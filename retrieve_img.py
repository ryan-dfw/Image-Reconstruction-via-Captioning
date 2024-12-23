import os

import requests
import time

endpoint = "http://10.0.0.1:10000/sony/camera"

last_camera_status = None

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
    global last_camera_status

    payload = {
        "method": "getEvent",
        "params": [False],
        "id": 1,
        "version": "1.0"
    }

    print("Listening for shutter events...")
    while True:
        try:
            response = requests.post(endpoint, json=payload)
            if response.status_code == 200:
                events = response.json()

                for event in events.get("result", []):
                    urls = extract_urls(event)
                    for url in urls:
                        print(f"URL found: {url}")
                        save_image(url)

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
            filename = os.path.join(save_directory, os.path.basename(url.split('?')[0]) or "downloaded_image.jpg")

            with open(filename, 'wb') as image_file:
                image_file.write(response.content)

            print(f"Image saved as: {filename}")
        else:
            print(f"Failed to download image. Status code: {response.status_code}")
    except Exception as e:
        print(f"Error saving image: {e}")


if __name__ == "__main__":
    get_photo_from_camera()