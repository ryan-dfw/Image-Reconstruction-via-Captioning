import requests

# Replace with the actual endpoint from the XML file
endpoint = "http://10.0.0.1:10000/sony/camera"

# Request the available API list
payload = {
    "method": "getAvailableApiList",
    "params": [],
    "id": 1,
    "version": "1.0"
}

response = requests.post(endpoint, json=payload)

if response.status_code == 200:
    print("Available APIs:")
    print(response.json())
else:
    print("Failed to connect:", response.status_code, response.text)
