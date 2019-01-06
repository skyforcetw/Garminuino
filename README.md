# Garminuino
This  is a project use GarminHUD as speedometer HUD, or a GoogleMap navigation HUD.

### 2018-11-05
Auto connect to last binded bluetooth device(not tested yet).

### 2018-10-30
Add english/korean language support, thanks for cloddin's help.
We have pre-release now, please download and try it from https://github.com/skyforcetw/Garminuino/releases

### 2018-10-01
All GoogleMaps_HUD app source code is uploaded!

### 2018-09-07 _VERY IMPORTANT UPDATE!_
A [Vega-HUD](https://visualgoal.com.tw/%E9%A6%96%E9%A0%81/) feature enlightened me a directed way to get turn-by-turn infomation.
I can get turn-by-turn from google map's notification(android)!
It's a big change for Garminuino, because it's mean I don't need arduino anymore!
So navigation part of Garminuino turn into a pure android's app(apk) now!

Hence the information below is not necessary anymore, but I still keep it for commemorative.
The app's development is near the end, I need somebody help me to do close testing.
If you are interesing for it, please mail me: skyforce@gmail.com , with tile "Garminuino Test"


![Initial concept of Garminuino](https://trello-attachments.s3.amazonaws.com/5604cb6e078e570dfc9c7404/1794x1080/accfe9e4f1f1d10e8bb62d7630130425/sketch-1443154690685.jpg "Initial concept of Garminuino")
This is the inital concept of Garminuino, but the implment is slight difference to the concept. For example, Garmin Hud is not linked by BLE, so BLE moduble is needless.

## [Speedometer HUD](https://github.com/skyforcetw/Garminuino/tree/master/speedometer%20HUD)

[![IMAGE ALT TEXT HERE](https://i.ytimg.com/vi/P0d8nm3kuxs/hqdefault.jpg?sqp=-oaymwEZCPYBEIoBSFXyq4qpAwsIARUAAIhCGAFwAQ==&rs=AOn4CLAh96qD5deX_DeYAHk9CHNptn97JQ)](https://www.youtube.com/watch?v=P0d8nm3kuxsE)

(click image can link to Youtube film)

A OBD/CanBus parser by Arduino, and a speed info transmitter by bluetooth(to GarminHUD) .

## [GoogleMaps HUD App](https://github.com/skyforcetw/Garminuino/tree/master/GoogleMaps_HUD)
A pure android app, for project navi-info of google map navigation.
Works just like garmin streetpilot with Garmin HUD.

## [navigation HUD](https://github.com/skyforcetw/Garminuino/tree/master/navigation%20HUD)      (deprecated now!)

[![IMAGE ALT TEXT HERE](https://i.ytimg.com/vi/VWV_F9V6yoA/hqdefault.jpg?sqp=-oaymwEZCPYBEIoBSFXyq4qpAwsIARUAAIhCGAFwAQ==&rs=AOn4CLDerjxVyOMK8V3hm9DaY-8zb3a1DQ)](https://www.youtube.com/watch?v=VWV_F9V6yoA)

(click image can link to Youtube film)

Besides parts of Speedometer HUD, a USB Host Shield with a Android mobile phone can fetch Google Navigation info, and transmit info by bluetooth to GarminHUD, too.

## ToDo List
1. Add auto-connection function.
2. Auto lanunch google map when HUD connected.
3. Add GPS speed showing.
4. Comment source-code.

## Authors

* **skyforce Shen** - *Initial work* - [github](https://github.com/skyforcetw) skyforce@gmail.com

## Contributor
* **Gabriel Valky** / **Frank Huebenthal** - *garmin hud protocol research* - [github](https://github.com/gabonator/Work-in-progress/tree/master/GarminHud) 

* **Niklas04** - help improve parsing & add German language support
[github](https://github.com/Niklas04) 
