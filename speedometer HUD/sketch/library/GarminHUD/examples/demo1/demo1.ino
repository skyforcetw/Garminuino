#include <GarminHUD.h>

GarminHUD hud;
void setup() {
  Serial1.begin(115200);
  // put your setup code here, to run once:
  hud.Init(Serial1);
}

void loop() {
  // put your main code here, to run repeatedly:
  hud.SetSpeedWarning(99, 130);
  delay(7000);
  hud.SetSpeedWarning(100, 130);
  delay(7000);
}
