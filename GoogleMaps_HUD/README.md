# GoogleMaps HUD
Garminuino Android App: GoogleMaps HUD, parses the navigation information from Google Maps App and displays it on the Garmin HUD

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

# Supported languages
For parsing the navigation informations the app will need a few keywords. The following languages are supported:
* English
* Chinese (Traditional Han)
* Korean
* German
