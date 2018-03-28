# GoogleMap Navigation HUD

When using Garmin HUD, you will not just want to use it as speedometer, so Navigation HUD is more important implement.
But it's complexity is growing up, because we need communication between arduino and android.

## Getting Started
We talk about the concept of Navigation HUD later, let's get it on working first!

### Prerequisites
1. A Garmin HUD for sure.
2. An Android phone.
3. A bluetooth module names HC-05 
https://www.amazon.com/LeaningTech-HC-05-Module-Pass-Through-Communication/dp/B00INWZRNC
4. An arduino pro micro with 3.3V 8MHz 
https://www.sparkfun.com/products/12587
5. An USB-Host shield mini 
https://www.circuitsathome.com/usb-host-shield-hardware-manual/

### Bluetooth Side
1. 

### Arduino Side
1. Combine arduino pro micro & USB-Host shield mini together.
2. Load sketch to arduino pro micro by USB micro


### Android Side
1. Enable Developer Options on android
2. Enable USB Debugging Mode on android
3. Use adb push a jar to android
4. Link android with another USB micro wire from USB-Host shield mini.
5. Open google map and set to navigation, 
