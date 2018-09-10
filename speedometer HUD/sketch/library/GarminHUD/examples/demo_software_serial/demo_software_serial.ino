#include <GarminHUD.h>
#include <SoftwareSerial.h>

SoftwareSerial softSerial(A4, A5);
GarminHUD hud;

void setup() {
  Serial.begin(115200);
  softSerial.begin(115200);
  // put your setup code here, to run once:
  hud.Init(&softSerial);
}

void loop() {
  // put your main code here, to run repeatedly:
  hud.SetSpeedWarning(99, 130);
  delay(7000);
  hud.SetSpeedWarning(100, 130);
  delay(7000);
}
