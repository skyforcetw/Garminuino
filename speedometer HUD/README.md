# speedometer HUD

Speedometer HUD is a relative simple project than navigation HUD.
The initial complete idea is navigation HUD, but I think the project can split to two parts:

1. Arduino (CanBus)
2. Android (GPS)

The 1st and simple part: Arduino (CanBUS), it's speedometer HUD.

The 2nd part need use arduino to communicate to any android phone, fetch android's GPS speed.

Combine 1st & 2nd part, we can correct CanBus speed error by GPS, but the benefit is low, so I stop developing 2nd part.

Speedometer HUD had two module:
1. CanBUS parser
2. GarminHUD controller

Fortunately, GarminHUD has been hacked:

https://hackaday.com/2014/03/30/controlling-the-garmin-hud-with-bluetooth/

https://github.com/gabonator/Work-in-progress/tree/master/GarminHud

So my remain job is parsing CanBUS, and calling the library to control GarminHUD, done!
