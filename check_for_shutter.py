import requests
import time

# Base URL for the API
endpoint = "http://10.0.0.1:10000/sony/camera"

# Track the last camera status to detect changes
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

def poll_events():
    global last_camera_status

    payload = {
        "method": "getEvent",
        "params": [False],  # Poll for events
        "id": 1,
        "version": "1.0"
    }

    print("Listening for shutter events...")
    while True:
        try:
            response = requests.post(endpoint, json=payload)
            if response.status_code == 200:
                events = response.json()

                # Extract and print URLs from events
                for event in events.get("result", []):
                    urls = extract_urls(event)
                    for url in urls:
                        print(f"URL found: {url}")

                # Check for cameraStatus changes (optional, can be removed if unnecessary)
                for event in events.get("result", []):
                    if isinstance(event, dict):
                        if event.get("type") == "cameraStatus":
                            current_status = event.get("cameraStatus")

                            # Detect status changes
                            if current_status != last_camera_status:
                                if current_status == "StillCapturing":
                                    print("Shutter pressed! Capturing...")
                                elif current_status == "IDLE" and last_camera_status == "StillCapturing":
                                    print("Capture completed.")
                                last_camera_status = current_status

        except Exception as e:
            print(f"Error polling events: {e}")

        # Poll every 0.5 seconds
        time.sleep(0.5)

if __name__ == "__main__":
    poll_events()

