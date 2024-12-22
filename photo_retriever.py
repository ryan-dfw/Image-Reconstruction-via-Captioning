import requests
import time

# Base URL for the API
endpoint = "http://10.0.0.1:10000/sony/camera"

def poll_events():
    print("Listening for manual shutter events...")
    while True:
        payload = {
            "method": "getEvent",
            "params": [False],  # Poll for events
            "id": 1,
            "version": "1.0"
        }

        try:
            response = requests.post(endpoint, json=payload)
            if response.status_code == 200:
                events = response.json()

                # Iterate over "result" to find the "takePicture" event
                for event in events.get("result", []):
                    if isinstance(event, dict) and event.get("type") == "takePicture":
                        picture_urls = event.get("takePictureUrl", [])
                        for url in picture_urls:
                            print(f"New picture taken! URL: {url}")
                            download_image(url)

            else:
                print(f"Error: {response.status_code} - {response.text}")

        except Exception as e:
            print(f"Error polling events: {e}")

        # Poll every 0.5 seconds
        time.sleep(0.5)

def download_image(url):
    response = requests.get(url, stream=True)
    if response.status_code == 200:
        filename = url.split("/")[-1].split("?")[0]  # Extract filename from URL
        with open(filename, "wb") as file:
            for chunk in response.iter_content(1024):
                file.write(chunk)
        print(f"Image saved as {filename}")
    else:
        print(f"Failed to download image: {response.status_code}")

if __name__ == "__main__":
    poll_events()
