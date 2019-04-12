# GoogleMaps HUD
Garminuino Android App: GoogleMaps HUD, parses the navigation information from Google Maps App and displays it on the Garmin HUD

## Instruction
Simple instruction for the app. [Please read here.](./INSTRUCTION.md)

## Supported Arrows
The app just can recongnized the arrow we supported. [Please read here.](./SUPPORTED_ARROWS.md)  
If find some arrow recognize failed, please call a new issue, and provide the arrow image in any form, let us add to supported list.

## How it works
The app reads the notitification of Google Maps, and parse it. Further it establish a connection to the Garmin HUD and sends the processed data.

## Features
* Showing turn-by-turn-symbol on HUD
* Showing distance to turn on HUD (mile-Support not fully tested)
* Showing time to arrive on HUD
* Showing speed on HUD (km/h and mph)
* Autoconnect to HUD (after first manual connection)

## Permissions
The app will need Notification access to read the data of Google Maps Notification.

To show the speed on the HUD the app will need access to your location.

## Supported languages
For parsing the navigation informations the app will need a few keywords. The following languages are supported:
* English
* Chinese (Traditional Han)
* Korean
* German

## For Developers
If you want to modifiy code and compile apk, you need sign your app.
Store keystore.properties and gmaps_hud.jks at GoogleMaps_HUD directory.
Please reference detail at https://developer.android.com/studio/publish/app-signing .
