# speedometer HUD

Speedometer HUD is a relative simple project than navigation HUD.
The initial complete idea is navigation HUD, but I think the project can split to two parts:

1. Arduino
2. Android

or

1. CanBUS
2. GPS

So I go simple part first: Arduino (CanBUS), it's speedometer HUD.

Speedometer HUD had two module:
1. CanBUS parser
2. GarminHUD controller

Fortunately, GarminHUD has been hacked:

https://hackaday.com/2014/03/30/controlling-the-garmin-hud-with-bluetooth/

https://github.com/gabonator/Work-in-progress/tree/master/GarminHud

So my remain job is parsing CanBUS, and calling the library to control GarminHUD, done!
