import requests
import time

# Base URL for the API
endpoint = "http://10.0.0.1:10000/sony/camera"

# Track the last camera status to detect changes
last_camera_status = None

def poll_events():
    global last_camera_status

    payload = {
        "method": "getEvent",
        "params": [False],  # Set to True if you want initial settings too
        "id": 1,
        "version": "1.0"
    }

    while True:
        try:
            response = requests.post(endpoint, json=payload)
            if response.status_code == 200:
                events = response.json()

                # Iterate over the "result" field safely
                for event in events.get("result", []):
                    if isinstance(event, dict):  # Process dictionaries only
                        # Check for cameraStatus changes
                        if event.get("type") == "cameraStatus":
                            current_status = event.get("cameraStatus")

                            # Detect status changes
                            if current_status != last_camera_status:
                                if current_status == "StillCapturing":
                                    print("Shutter pressed! Capturing...")
                                elif current_status == "IDLE" and last_camera_status == "StillCapturing":
                                    print("Capture completed.")
                                last_camera_status = current_status

                        # Check for takePicture URL
                        if event.get("type") == "takePicture" and event.get("takePictureUrl"):
                            picture_url = event["takePictureUrl"][0]
                            print(f"Picture available at: {picture_url}")

            else:
                print(f"Error: {response.status_code} - {response.text}")

        except Exception as e:
            print(f"Error polling events: {e}")

        # Poll every 0.1 seconds
        time.sleep(0.1)

if __name__ == "__main__":
    print("Listening for shutter events...")
    poll_events()
