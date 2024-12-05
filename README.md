# On-device Training

This repository implements on-device training of a TensorFlow Lite models on Android.

## Overview

This project implelements an Android application that allows users to train TensorFlow Lite models in their own device and collect metrics related to the local training. 
The idea of this repository is to collect metrics mainly related to the energy cost of running a model with a certain training configuration in a device.
The app covers data loading, batching, training loop execution, and saving training results and sharing to Firebase or to any other platform via JSON.

## Features

* Loads training data from preprocessed buffers.
* Sets up a TensorFlow Lite Interpreter, using NNAPI for hardware acceleration if available.
* Divides data into batches for efficient training.
* Runs a training loop for a specified number of epochs.
* Calculates and displays training time and energy consumption.
* Saves training results to history.
* Sends results to Firebase instance if available.

## Getting Started

1. **Clone the repository**

2. **Import into Android Studio:**
   Open Android Studio and import the project from the cloned directory.

3. **Build and run:**
   Build the project and run it on an Android device.

## Dependencies

This project uses the following dependencies:

* TensorFlow Lite: For model inference and training.
* Android Support Libraries: For UI elements and other Android components.
* Firebase (if applicable): For cloud services integration.

## Usage

For using this application, it is expected that the user has already created binary versions of the TensorFlow Lite model as well as the dataset.
For this, a support repository was created: In [flower-simulation](https://github.com/PFG-Federated-Learning/flower-simulation), related code and documentation
can be found on how to transform your own models and datasets into binary files.
These files should then be uploaded to a source that makes the model accessible via URL, such as a github repository or a google drive.

## Changing Firebase Integration

**To change the Firebase integration in this project to use your own, follow these steps:**

1. **Create a new Firebase project:**
   If you don't already have one, create a new Firebase project in the Firebase console.

2. **Download the `google-services.json` file:**
   Download the `google-services.json` file from your Firebase project settings and place it in the `app` directory of your Android project.

3. **Update dependencies:**
   Make sure you have the necessary Firebase dependencies in your `build.gradle` file.

4. **Modify code:**
   Update your code to use the new Firebase project configuration. This might involve changing the way you initialize Firebase services or access data.

5. **Rebuild and run:**
   Rebuild your project and run it to ensure the changes have been applied correctly.

**Note:** If you are not using Firebase in your project, you can remove the Firebase dependencies and related code.

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues.

## License

This project is licensed under the [MIT License](LICENSE).
