# Garminuino
This  is a project use GarminHUD as speedometer HUD, or a GoogleMap navigation HUD.

### 2018-10-01 _UPDATE!_
All GoogleMaps_HUD app source code is uploaded!

### 2018-09-07 _VERY IMPORTANT UPDATE!_
A Vega-HUD feature enlightened me a directed way to get turn-by-turn infomation.
I can get turn-by-turn from google map's notification(android)!
It's a big change for Garminuino, because it's mean I don't need arduino anymore!
So navigation part of Garminuino turn into a pure android's app(apk) now!

Hence the information below is not necessary anymore, but I still keep it for commemorative.
The app's development is near the end, I need somebody help me to do close testing.
If you are interesing for it, please mail me: skyforce@gmail.com , with tile "Garminuino Test"



![Initial concept of Garminuino](https://trello-attachments.s3.amazonaws.com/5604cb6e078e570dfc9c7404/1794x1080/accfe9e4f1f1d10e8bb62d7630130425/sketch-1443154690685.jpg "Initial concept of Garminuino")
This is the inital concept of Garminuino, but the implment is slight difference to the concept. For example, Garmin Hud is not linked by BLE, so BLE moduble is needless.

![Garminuino block diagram](https://lh3.googleusercontent.com/oXDtDAJLvEJBJ1kAnYHDXJx_3mx6ZalRhZjGc31cwYZ-Qh7aO6kJr3kl5VZUqlsWCHJmWCKbS-wEOZdxjcPB_7tZzMo-gBHcogHR5FFoi-lXqr9Bjd9ymNqrq_dPk6cLGhtv1PSDqPOmDe2Qn5pcDMpjSJpEJikr6Dw-7UnYuxoPz9S9b3n-gFwwzWgmnE3Ocxtc0z5llRsbRpIZpnNpLvGL6ibz1Y8o0pjnZPUCtJwpQpIMKl0JLBh3V2TaXZpaQCImMvkHpPbvA1WVpn8-zKw-q_H9qJ6QSvSANJh5rxb4_T_Ef7W3zr04CBn16doAHuKqc3Z4FmUwhmS7XxpNmx19y_MUw45iXW9y9y3ffyoKXGwfj7GoG8chM4e-a6MkJ4mVGeG32LMPhSj73OEclXtyaUz3-diOmwGkmDjxQHU_sIiJNqJqaJ4Lzcs9x5dOpelmM9azZCANsy1v2epK9MCbhRhGrSrMmQVotfa7X1mlWHLPq_fkVJ5XcceE6NDwl5MqFklf5Js2dS1h7EebnFX4s-MGqpZLwTUuee_lUfdo9JNB31oBglJJwRPIepT0VXKskmVtOp11XYE6NIkFJ47vKCkP4pIkA7XsEMcLyphSnYV3A2V2Z-AlkWYm_zIzBq0j2Z1icsvUYAtRMONan-y3ldamf2RADQ=w1231-h691-no)
This is block diagram for now implement.

## [Speedometer HUD](https://github.com/skyforcetw/Garminuino/tree/master/speedometer%20HUD)

[![IMAGE ALT TEXT HERE](https://i.ytimg.com/vi/P0d8nm3kuxs/hqdefault.jpg?sqp=-oaymwEZCPYBEIoBSFXyq4qpAwsIARUAAIhCGAFwAQ==&rs=AOn4CLAh96qD5deX_DeYAHk9CHNptn97JQ)](https://www.youtube.com/watch?v=P0d8nm3kuxsE)

(click image can link to Youtube film)

A OBD/CanBus parser by Arduino, and a speed info transmitter by bluetooth(to GarminHUD) .

## [GoogleMaps HUD App](https://github.com/skyforcetw/Garminuino/tree/master/GoogleMaps_HUD)
A pure android app, for project navi-info of google map navigation.

## [GoogleMap navigation HUD](https://github.com/skyforcetw/Garminuino/tree/master/navigation%20HUD)      (deprecated now!)

[![IMAGE ALT TEXT HERE](https://i.ytimg.com/vi/VWV_F9V6yoA/hqdefault.jpg?sqp=-oaymwEZCPYBEIoBSFXyq4qpAwsIARUAAIhCGAFwAQ==&rs=AOn4CLDerjxVyOMK8V3hm9DaY-8zb3a1DQ)](https://www.youtube.com/watch?v=VWV_F9V6yoA)

(click image can link to Youtube film)

Besides parts of Speedometer HUD, a USB Host Shield with a Android mobile phone can fetch Google Navigation info, and transmit info by bluetooth to GarminHUD, too.

## Authors

* **skyforce Shen** - *Initial work* - [github](https://github.com/skyforcetw)
