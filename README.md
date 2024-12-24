# Image Reconstruction via Captioning

## Overview

This program utilizes Sony's Camera Remote API to interact with any compatible camera,
intercepting captured images, processing them through a pipeline of image-to-text
and text-to-image models, generating a reconstructed image.
The result is an image conceptually related to the original, interpreted through the lens
of two language-based AI models.

## Features

-   **Sony Camera Integration**  
    Utilizes Sony's Camera Remote API to interact with compatible cameras over WiFi Direct, downloading JPEG images from a private URL.
-   **Image Captioning**  
    Uses OpenAI's API to perform Image Captioning with `gpt-4o-mini`
-   **Image Creation**  
    Delivers the resulting string to `dall-e-3` over API, generating a reconstructed image based on the provided caption.

## Installation

1. Clone the repository:

```bash
git clone https://github.com/ryan-dfw/Image-Reconstruction-via-Captioning.git
cd Image-Reconstruction-via-Captioning
```

2. Install the required Python dependencies:

```bash
pip install -r requirements.txt
```

## Usage

> This program requires the having of a Sony Camera; kindly have one.

1. Find your camera's SSID & password - in the case of most Sony Alphas, this is obtainable by navigating in the menu under networking. For cameras without a screen, such as the QX-10, the SSID & PW are printed somewhere such as inside the door of the battery compartment.
2. Connect via Wi-Fi to your camera.
3. Run the script with `python3 main.py`.
4. Take a photo when prompted.
5. When the script requests, change your computer's connection from the camera to the internet at large.
6. Wait just a little bit longer
7. Enjoy your image; share it with everyone on TikTok

## Results

<div style="display: flex; justify-content: center; align-items: center; align=center">
  <img src="img/sample_in.jpg" height=250px alt="a poorly taken photo of a pet dog" style="margin-right: 10px;">
  <img src="img/sample_out.png" height=250px alt="a generated image of a pet dog">
</div>

**Caption used to generate image:**

> ...
