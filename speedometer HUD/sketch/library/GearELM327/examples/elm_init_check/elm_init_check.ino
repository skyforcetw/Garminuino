/* Copyright 2011 David Irvine

   This file is part of Loguino

   Loguino is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   Loguino is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Loguino.  If not, see <http://www.gnu.org/licenses/>.

   $Rev$
   $Author$
   $Date$

*/

#include <Arduino.h>
#define ELM_TIMEOUT 9000
#define ELM_BAUD_RATE 9600
#define ELM_PORT obdSerial

#include <SoftwareSerial.h>
#include <GearELM327.h>
SoftwareSerial obdSerial(A2, A3); // RX, TX

void printStatus(byte status) {
  switch (status)
  {
    case ELM_TIMEOUT:
      Serial.println(F("ELM_TIMEOUT"));
      break;
    case ELM_SUCCESS:
      Serial.println(F("ELM_SUCCESS"));
      break;
    case ELM_NO_RESPONSE:
      Serial.println(F("ELM_NO_RESPONSE"));
      break;
    case ELM_BUFFER_OVERFLOW:
      Serial.println(F("ELM_BUFFER_OVERFLOW"));
      break;
    case ELM_GARBAGE:
      Serial.println(F("ELM_GARBAGE"));
      break;
    case ELM_UNABLE_TO_CONNECT:
      Serial.println(F("ELM_UNABLE_TO_CONNECT"));
      break;
    case ELM_NO_DATA:
      Serial.println(F("ELM_NO_DATA"));
      break;
    default:
      Serial.print(F("UNKNOWN: "));
      Serial.println(status);
  }
}



void setup() {

  float f;
  bool b;
  byte by;
  int i;
  unsigned int ui;

  byte status;
  String s;

  obdSerial.begin(115200);
  ELM327 Elm(obdSerial);

  Serial.begin(115200);
  Serial.print("ELM327 Begin: ");

  printStatus(Elm.begin());

  Serial.print("Elm Voltage: ");
  status = Elm.getVoltage(f);
  if (status == ELM_SUCCESS)
  {
    Serial.println ("Pass");
    Serial.print(" Value: ");
    Serial.println(f);
  } else {
    printStatus(status);
  }

  Serial.print("Elm Ignition: ");
  status = Elm.getIgnMon(b);
  if (status  == ELM_SUCCESS)
  {
    Serial.println ("Pass");
    Serial.print(" Value: ");
    Serial.println(b);
  } else {
    printStatus(status);
  }

  Serial.print("Elm Version: ");
  status = Elm.getVersion(s) ;
  if (status == ELM_SUCCESS)
  {
    Serial.println ("Pass");
    Serial.print(" Value: ");
    Serial.println(s);
  } else {
    printStatus(status);
  }




}
void loop() {}


