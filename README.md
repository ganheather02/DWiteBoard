# DWiteBoard

This Android application is created for the MIT Launch competition by Jeffrey Lin. It offers the ability for multiple users to draw on the same "whiteboard". The application also enables users to upload a picture and have users draw on the picture.

## What's here

The drawing and color-picking portions of this application are largely adapted from Google's
ApiDemo application, which was shipped with a previous version of the Android SDK's
[sample code](https://developer.android.com/samples/).

The program is a fork of Firebase's sample [Android Drawing code] (https://github.com/firebase/AndroidDrawing). The original license is included in the project. 


## Technical Details

The program's picture uploading function compresses the original picture into a PNG file. Then the PNG file is encoded to Base64 and pushed to Firebase. When the phones detect that there is a picture uploaded to Firebase, it downloads the Base64 picture and decodes it back to a Bitmap. 

## More About the Author

Jeffrey Lin is currently a freshman based in the San Francisco Bay Area. He has started programming in sixth grade. He is fluent in Java programming and experienced with Linux. His main passion is robotics programming and he has participated in three years of First Tech Challenge and won numerous awards. To check out more information about him visit [here] (https://www.linkedin.com/in/jeffreytech).

## More about Firebase on Android

You can do lots more with Firebase on Android. Check out the Android
[Quickstart guide](https://www.firebase.com/docs/java-quickstart.html) to learn more.


