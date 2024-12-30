# Image Reconstruction via Captioning

## Overview

This program utilizes Sony's Camera Remote API to interact with any compatible camera, intercepting captured images, processing them through a pipeline of image-to-text and text-to-image models, generating a reconstructed image. The result is an image conceptually related to the original, interpreted through the lens of two language-based AI models.

## Features

-   **Sony Camera Integration**  
    Utilizes Sony's Camera Remote API to interact with compatible cameras over WiFi Direct, downloading JPEG images from a private URL.
-   **Image Captioning**  
    Uses OpenAI's API to perform Image Captioning with `gpt-4o-mini`
-   **Image Creation**  
    Delivers the resulting string to `dall-e-3` over API, generating a reconstructed image based on the provided caption.
-  **Cross-Platform Compatibility**  
    Efficiently runs as a Python script on computers for lightweight performance, with a dedicated Android app providing enhanced mobility and automatic network management for seamless user experience.

## Results

<div style="display: flex; justify-content: center; align-items: center; align=center">
  <img src="img/sample_in.jpg" height=250px alt="a poorly taken photo of a pet dog" style="margin-right: 10px;">
  <img src="img/sample_out.png" height=250px alt="a generated image of a pet dog">
</div>

**Caption used to generate image:**

> The image features a dog, likely a Boston Terrier, resting on a blanket. The dog has a distinctive black and white coat, with a white patch on its face that contrasts with the darker fur. Its ears are large and upright, adding to its alert appearance. The dog's expression is curious and slightly inquisitive, gazing toward the camera with its big, round eyes. The eyes are dark and expressive, conveying a sense of personality. Its mouth is closed, which gives it a calm demeanor.In terms of pose, the dog is lying down, with one foreleg visible, slightly extended. The body language suggests relaxation, yet the attentive position of the ears indicates that it is aware of its surroundings. The background features a plain wall, while the dog is resting on a colorful blanket with a soft, patterned design featuring blue and green elements. The image has a warm, cozy feel, emphasizing the intimate setting. The focus appears somewhat soft, but the subject (the dog) remains the central point of interest.

## Python Applet

### Installation

1. Clone the repository:

```bash
git clone https://github.com/ryan-dfw/Image-Reconstruction-via-Captioning.git
cd Image-Reconstruction-via-Captioning
```

2. Install the required Python dependencies:

```bash
pip install -r requirements.txt
```

3. Create a `.env` file at the base of the project directory. The contents of the file should be as follows, where the value of OPENAI_API_KEY is a secret key obtained from OpenAI.

```txt
OPENAI_API_KEY="INSERT KEY HERE"
```

## Usage

> This program requires having a camera compatible with Sony's Camera Remote API.

1. Find your camera's SSID & password - in the case of most Sony Alphas, this is obtainable by navigating in the menu under networking. For cameras without a screen, such as the QX-10, the SSID & PW are printed somewhere such as inside the door of the battery compartment.
2. Power on the camera.
3. On your laptop, select the camera's Wi-Fi network to establish a connection.
4. Run the script with `python3 main.py`.
5. Take a photo when prompted.
6. When the script requests, change your computer's connection from the camera to the internet at large.
7. Wait just a little bit longer.
8. Enjoy your image; share it with everyone on TikTok!

## Android App

### Installation

1. Clone the repository:

```bash
git clone https://github.com/ryan-dfw/Image-Reconstruction-via-Captioning.git
cd Image-Reconstruction-via-Captioning
```

2. Open the package in Android Studio. In MainActivity.java, `Ctrl-F` for `"KEYGOESHERE"` and replace with your OpenAI API KEY. In the near future this will be replaced with a less splapdash solution.

3. In Android Studio, select your phone as a run destination and run the app to install.

## Usage

> This app requires having a camera compatible with Sony's Camera Remote API.

1. Find your camera's SSID & password - in the case of most Sony Alphas, this is obtainable by navigating in the menu under networking. For cameras without a screen, such as the QX-10, the SSID & PW are printed somewhere such as inside the door of the battery compartment.
2. Power on the camera.
3. On your phone, select the camera's Wi-Fi network to establish a connection.
4. Run the app.
5. Take a photo.
6. Simply be patient - it takes a bit, but the Android app handles all the networks itself!
7. Enjoy your image! You may want to screenshot it, as we've not implemented a better solution just yet.

Image Reconstruction via Captioning © 2024 by Ryan McKevitt & Eric McKevitt is licensed under CC BY-NC-SA 4.0 
