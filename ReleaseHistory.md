## GoogleMap navigation HUD App Release History ##


### [2021-08-25 v0.8.9](https://github.com/skyforcetw/Garminuino/files/7044189/gmaps_hud-release_v0.8.9.zip)
Add support for google maps go navigation.

### [2021-08-20 v0.8.6](https://github.com/skyforcetw/Garminuino/files/7016037/gmaps_hud-release_v0.8.6.zip)
Fix Arrow v2 at android 6.0. 

### [2020-09-29 v0.8.3](https://github.com/skyforcetw/Garminuino/releases/download/0.8.3/gmaps_hud-release_v0.8.3.apk)
Change Arrow SAD calculation.

### [2020-09-29 v0.8.2](https://github.com/skyforcetw/Garminuino/releases/download/0.8.2/gmaps_hud-release_v0.8.2.apk)
Fix arrivals arrow.

### [2020-09-26 v0.8.1](https://github.com/skyforcetw/Garminuino/releases/download/v0.8.1/gmaps_hud-release_v0.8.1.apk)
Add support for BMW HUD , thanks for intervigilium's contribution.

### [2020-07-04 v0.7.0](https://github.com/skyforcetw/Garminuino/releases/download/0.7.0/gmaps_hud-release_v0.7.0.apk)
Fix some bug for BT auto-connection.

### [2020-03-02 v0.6.4](https://github.com/skyforcetw/Garminuino/releases/download/0.6.4/gmaps_hud-release_v0.6.4.apk)
Fix time parsing error when XX:00.

### [2020-01-20 v0.6.1](https://github.com/skyforcetw/Garminuino/releases/download/0.6.1/gmaps_hud-release_v0.6.1.apk)
Use more robust arrow recognize + Fix lane detect no function bug.

### [2019-11-11 v0.5.4](https://github.com/skyforcetw/Garminuino/releases/download/0.5.4/gmaps_hud-release_v0.5.4.apk)
Fix some bugs for traffic detection

### [2019-10-23 v0.5.2](https://github.com/skyforcetw/Garminuino/releases/download/0.5.2/gmaps_hud-release_v0.5.2.apk)
Support Arrow Style v2 / Traffic alert speed is adjustable

Arrow v1 style/Arrow v2 style:

![v1](https://github.com/skyforcetw/Garminuino/blob/master/GoogleMap_Arrow_Recognize/PatternRecognize/workdir/Google_Arrow2/Right.png)![v2](https://github.com/skyforcetw/Garminuino/blob/master/GoogleMap_Arrow_Recognize/PatternRecognize/workdir/Google_Arrow3%20-%20remove%20alpha/Right.png)

[New instruction for v0.5.2.](./GoogleMaps_HUD/INSTRUCTIONv052.md)

### [2019-09-26 v0.4.9](https://github.com/skyforcetw/Garminuino/releases/download/0.4.9/gmaps_hud-release_v0.4.9.apk)
Polish Support & New Dark Mode.

### [2019-08-23 v0.4.7](https://github.com/skyforcetw/Garminuino/releases/download/0.4.7/gmaps_hud-release_v0.4.7.apk)
Simple controls in Notification.

**NOTE** If your android is below Android 8.0(Oreo), please download [this one](https://github.com/skyforcetw/Garminuino/releases/download/0.4.7/gmaps_hud-release_v0.4.7.android6.0.apk).


### [2019-08-11 v0.4.6](https://github.com/skyforcetw/Garminuino/releases/download/0.4.6/gmaps_hud-release_v0.4.6.apk)
Lane detection mechanism revision.

### 2019-08-11 v0.4.4
Lane detection mechanism.(had some bug, need fix)

### [2019-08-06 v0.4.3](https://github.com/skyforcetw/Garminuino/releases/download/0.4.3/gmaps_hud-release_v0.4.3.apk)
1. Add traffic detect function
2. Add reset bt function, test it when your bt connection is unable.

### [2019-03-15 v0.4.0s](https://github.com/skyforcetw/Garminuino/releases/download/0.4.0s/gmaps_hud-release.apk)
Auto/Manual Brightness Setting for Garmin HUD.

### 2019-02-19 v0.3.1
Add arrow recognize for roundabout.  
According to country, arrows of roundabout  can classification to clockwise or counter-clockwise.  
Left-sided drive / Right-hand traffic  country, like USA, roundabout arrows are counter-clockwise.  
Right-sided drive/ Left-hand traffic  country, like United Kingdom, roundabout arrows are clockwise.  

### 2019-02-11 v0.2.0
New layout of app.
Add some status indicator for qucik debug.

### 2019-02-06
It's necessary to figure out auto coneection is working well or not, so I removed the naevtamarkus' BT Library first.
I also re-arrange some layout of app, for convient to use.

### 2019-01-15 v0.1.6
Thanks for Niklas04's help, Fix speedmeter, autoconnect.

### 2019-01-07
Thanks for Niklas04's help to improve parsing & add German language support.

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
