/* Copyright 2011 David Irvine
*
* This file is part of Loguino
*
* Loguino is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Loguino is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.

* You should have received a copy of the GNU General Public License
* along with Loguino.  If not, see <http://www.gnu.org/licenses/>.
*
* $Rev$
* $Author$
* $Date$

*/


#include "GearELM327.h"


ELM327::ELM327(SoftwareSerial& _softserial) {
	softserial=&_softserial;
	bySoftSerial=true;
}

ELM327::ELM327(HardwareSerial* serial) {
	_serial = serial;
	bySoftSerial=false;
}

ELM327::ELM327() {
	_serial = &Serial1;
	bySoftSerial=false;
}

byte ELM327::begin(){
	return begin(true,true);
}

byte ELM327::begin(boolean ATE0,boolean ATL0){
	char data[40];
	// byte status;
	byte status=runCommand("ATZ",data,40);

	if(ATE0) {
		status=runCommand("AT E0",data,40);
	}

	if(ATL0) {
		status=runCommand("AT L0",data,40);
	}

	return runCommand("AT SP 0",data,40);
}

#ifndef ELM_GEAR_ONLY
byte ELM327::engineLoad(byte &load){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","04",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	load=values[0]*100/255;
	return ELM_SUCCESS;
}

byte ELM327::coolantTemperature(int &temp){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","05",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	temp=values[0]-40;
	return ELM_SUCCESS;
}

byte ELM327::getFuelTrim(const char *pid, int &percent){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41",pid,values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	percent=(values[0] - 128) * 100/128;
	return ELM_SUCCESS;
}

byte ELM327::fuelTrimBank1ShortTerm(int &percent){
	return getFuelTrim("06",percent);
}
byte ELM327::fuelTrimBank1LongTerm(int &percent){
	return getFuelTrim("07",percent);
}
byte ELM327::fuelTrimBank2ShortTerm(int &percent){
	return getFuelTrim("08",percent);
}
byte ELM327::fuelTrimBank2LongTerm(int &percent){
	return getFuelTrim("09",percent);
}

byte ELM327::fuelPressure(int &pressure){
	byte status;
	byte values[1]={0};
	//char mode[]="01";
	//char chkMode[]="41";
	//char pid[]="0A";
	status=getBytes("01","41","0A",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	pressure=values[0]*3;
	return ELM_SUCCESS;
}

byte ELM327::intakeManifoldAbsolutePressure(byte &pressure){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","0B",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	pressure=values[0];
	return ELM_SUCCESS;
}
#endif

byte ELM327::engineRPM(int &rpm){
	byte status;
	byte values[2];
	status=getBytes("01","41","0C",values,2);
	if (status != ELM_SUCCESS){
		return status;
	}
	unsigned int value0=values[0];
	unsigned int value1=values[1];
	rpm=((value0*256)+value1)/4;
	//rpm=((values[0]*256)+values[1])/4;

	return ELM_SUCCESS;
}

byte ELM327::vehicleSpeed(byte &speed){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","0D",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	speed=values[0];
	return ELM_SUCCESS;
}


#ifndef ELM_GEAR_ONLY
byte ELM327::timingAdvance(int &advance){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","0E",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	advance=values[0]/2-64;
	return ELM_SUCCESS;
}

byte ELM327::intakeAirTemperature(int &temperature){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","0F",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	temperature=values[0]-40;
	return ELM_SUCCESS;
}

byte ELM327::MAFAirFlowRate(int &rate){
	byte status;
	byte values[2];
	status=getBytes("01","41","10",values,2);
	if (status != ELM_SUCCESS){
		return status;
	}
	rate = ((256 * values[0]) + values[1])/100 ;
	return ELM_SUCCESS;
}

byte ELM327::MAFAirFlowRate(float &rate){
	byte status;
	byte values[2];
	status=getBytes("01","41","10",values,2);
	if (status != ELM_SUCCESS){
		return status;
	}
	unsigned int value0=values[0];
	unsigned int value1=values[1];
	rate = ((256 * value0) + value1)/100. ;  
	//rate = ((256 * values[0]) + values[1])/100. ;
	return ELM_SUCCESS;
}

byte ELM327::throttlePosition(byte &position){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","11",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	position=(values[0]*100)/255;
	return ELM_SUCCESS;
}

byte ELM327::o2SensorBank1Sensor1(byte &voltage, byte &trim){
	return o2sensorRead("14", voltage, trim);
}
byte ELM327::o2SensorBank1Sensor2(byte &voltage, byte &trim){
	return o2sensorRead("15", voltage, trim);
}
byte ELM327::o2SensorBank1Sensor3(byte &voltage, byte &trim){
	return o2sensorRead("16", voltage, trim);
}
byte ELM327::o2SensorBank1Sensor4(byte &voltage, byte &trim){
	return o2sensorRead("17", voltage, trim);
}
byte ELM327::o2SensorBank2Sensor1(byte &voltage, byte &trim){
	return o2sensorRead("18", voltage, trim);
}
byte ELM327::o2SensorBank2Sensor2(byte &voltage, byte &trim){
	return o2sensorRead("19", voltage, trim);
}
byte ELM327::o2SensorBank2Sensor3(byte &voltage, byte &trim){
	return o2sensorRead("1A", voltage, trim);
}
byte ELM327::o2SensorBank2Sensor4(byte &voltage, byte &trim){
	return o2sensorRead("1B", voltage, trim);
}

byte ELM327::o2sensorRead(const char *bank, byte &voltage, byte &trim){
	byte status;
	byte values[2];
	status=getBytes("01","41",bank,values,2);
	if (status != ELM_SUCCESS){
		return status;
	}
	voltage = values[0] / 200;
	trim=( values[1] * 100 ) / 128;
	return ELM_SUCCESS;
}


byte ELM327::auxiliaryInputStatus(bool &auxStatus){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","1E",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	auxStatus=getBit(values[0], 1);
	return ELM_SUCCESS;
}

byte ELM327::engineRunTime(unsigned int &runTime){
	byte status;
	byte values[2];
	status=getBytes("01","41","1F",values,2);
	if (status != ELM_SUCCESS){
		return status;
	}
	runTime=(values[0]*256)+values[1];
	return ELM_SUCCESS;
}

byte ELM327::distanceMIL(unsigned int &distance){
	byte status;
	byte values[2];
	status=getBytes("01","41","21",values,2);
	if (status != ELM_SUCCESS){
		return status;
	}
	distance=(values[0]*256)+values[1];
	return ELM_SUCCESS;
}

byte ELM327::relativeFuelRailPressure(unsigned int &pressure){
	byte status;
	byte values[2];
	status=getBytes("01","41","22",values,2);
	if (status != ELM_SUCCESS){
		return status;
	}
	pressure=((values[0]*256)+values[1])*0.079;
	return ELM_SUCCESS;
}

byte ELM327::absoluteFuelRailPressure(unsigned int &pressure){
	byte status;
	byte values[2];
	status=getBytes("01","41","23",values,2);
	if (status != ELM_SUCCESS){
		return status;
	}
	pressure=((values[0]*256)+values[1])*10;
	return ELM_SUCCESS;
}


byte ELM327::o2S1WRVoltage(unsigned int &equivRatio, unsigned int &voltage){
	return o2WRVoltage("24", equivRatio, voltage);
}
byte ELM327::o2S2WRVoltage(unsigned int &equivRatio, unsigned int &voltage){
	return o2WRVoltage("25", equivRatio, voltage);
}
byte ELM327::o2S3WRVoltage(unsigned int &equivRatio, unsigned int &voltage){
	return o2WRVoltage("26", equivRatio, voltage);
}
byte ELM327::o2S4WRVoltage(unsigned int &equivRatio, unsigned int &voltage){
	return o2WRVoltage("27", equivRatio, voltage);
}
byte ELM327::o2S5WRVoltage(unsigned int &equivRatio, unsigned int &voltage){
	return o2WRVoltage("28", equivRatio, voltage);
}
byte ELM327::o2S6WRVoltage(unsigned int &equivRatio, unsigned int &voltage){
	return o2WRVoltage("29", equivRatio, voltage);
}
byte ELM327::o2S7WRVoltage(unsigned int &equivRatio, unsigned int &voltage){
	return o2WRVoltage("2A", equivRatio, voltage);
}
byte ELM327::o2S8WRVoltage(unsigned int &equivRatio, unsigned int &voltage){
	return o2WRVoltage("2B", equivRatio, voltage);
}
byte ELM327::o2WRVoltage(const char *sensor, unsigned int &equivRatio, unsigned int &voltage){
	byte status;
	byte values[4];
	status=getBytes("01","41",sensor,values,4);
	if (status != ELM_SUCCESS){
		return status;
	}
	equivRatio=((values[0] * 256)+ values[1])*2/65535; // or ((A*256)+B)/32768
	voltage=((values[2]*256)+values[3])*8/65535;// or ((C*256)+D)/8192;
	return ELM_SUCCESS;
}

byte ELM327::commandedEGR(byte &egr){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","2C",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	egr=(100 * values[0])/255;
	return ELM_SUCCESS;
}

byte ELM327::EGRError(int &error){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","2D",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	error =(values[0]-128)*100/128;
	return ELM_SUCCESS;
}

byte ELM327::commandedEvaporativePurge(byte &purge){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","2E",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	purge= (100 * values[0])/255;
	return ELM_SUCCESS;
}

byte ELM327::fuelLevel(byte &level){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","2F",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	level= (100 * values[0])/255;
	return ELM_SUCCESS;
}

byte ELM327::warmUpsSinceLastCleared(byte &warmUps){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","30",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	warmUps=values[0];
	return ELM_SUCCESS;
}

byte ELM327::distanceSinceLastCleared(unsigned int &distance){
	byte status;
	byte values[2];
	status=getBytes("01","41","31",values,2);
	if (status != ELM_SUCCESS){
		return status;
	}
	distance=(values[0]*256) + values[1];
	return ELM_SUCCESS;
}

byte ELM327::evapPressure(int &pressure){
	byte status;
	byte values[2];
	status=getBytes("01","41","32",values,2);
	if (status != ELM_SUCCESS){
		return status;
	}
	pressure=(256*(values[0]-128) + values[1])/4;
	return ELM_SUCCESS;
}

byte ELM327::barometricPressure(byte  &pressure){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","33",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	pressure=values[0];
	return ELM_SUCCESS;
}


byte ELM327::o2WRCurrent(const char *sensor, unsigned int &equivRatio, int &current){
	byte status;
	byte values[4];
	status=getBytes("01","41",sensor,values,4);
	if (status != ELM_SUCCESS){
		return status;
	}
	equivRatio=((values[0] * 256)+ values[1])*2/32768; 
	current=((values[2]*256)+values[3])*8/265-128;
	return ELM_SUCCESS;
}

byte ELM327::o2S1WRCurrent(unsigned int &equivRatio, int &current){
	return o2WRCurrent("34",equivRatio,current);
}
byte ELM327::o2S2WRCurrent(unsigned int &equivRatio, int &current){
	return o2WRCurrent("35",equivRatio,current);
}
byte ELM327::o2S3WRCurrent(unsigned int &equivRatio, int &current){
	return o2WRCurrent("36",equivRatio,current);
}
byte ELM327::o2S4WRCurrent(unsigned int &equivRatio, int &current){
	return o2WRCurrent("37",equivRatio,current);
}
byte ELM327::o2S5WRCurrent(unsigned int &equivRatio, int &current){
	return o2WRCurrent("38",equivRatio,current);
}
byte ELM327::o2S6WRCurrent(unsigned int &equivRatio, int &current){
	return o2WRCurrent("39",equivRatio,current);
}
byte ELM327::o2S7WRCurrent(unsigned int &equivRatio, int &current){
	return o2WRCurrent("3A",equivRatio,current);
}
byte ELM327::o2S8WRCurrent(unsigned int &equivRatio, int &current){
	return o2WRCurrent("3B",equivRatio,current);
}

byte ELM327::catalystTemperatureBank1Sensor1(int &temperature)
{
	return catTemperature("3C", temperature);
}
byte ELM327::catalystTemperatureBank2Sensor1(int &temperature)
{
	return catTemperature("3D", temperature);
}
byte ELM327::catalystTemperatureBank1Sensor2(int &temperature)
{
	return catTemperature("3E", temperature);
}
byte ELM327::catalystTemperatureBank2Sensor2(int &temperature)
{
	return catTemperature("3F", temperature);
}

byte ELM327::catTemperature(const char *sensor, int &temperature){
	byte status;
	byte values[2];
	status=getBytes("01","41",sensor,values,2);
	if (status != ELM_SUCCESS){
		return status;
	}
	temperature=((values[0]*256)+values[1])/10 - 40;
	return ELM_SUCCESS;
}

//byte ELM327::controlModuleVoltage(unsigned int &voltage){
byte ELM327::controlModuleVoltage(float &voltage){
	byte status;
	byte values[2];
	status=getBytes("01","41","42",values,2);
	if (status != ELM_SUCCESS){
		return status;
	}
	voltage=((values[0]*256)+values[1])/1000.;
	return ELM_SUCCESS;
}

byte ELM327::absoluteLoadValue(unsigned int &load){
	byte status;
	byte values[2];
	status=getBytes("01","41","43",values,2);
	if (status != ELM_SUCCESS){
		return status;
	}
	load=((values[0]*256)+values[1])*100/255;
	return ELM_SUCCESS;
}

byte ELM327::commandEquivalenceRatio(float &ratio){
	byte status;
	byte values[2];
	status=getBytes("01","41","44",values,2);
	if (status != ELM_SUCCESS){
		return status;
	}
	ratio=((values[0]*256)+values[1])/32768;
	return ELM_SUCCESS;
}


byte ELM327::relativeThrottlePosition(byte &position){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","45",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	position=(100*values[0])/255;
	return ELM_SUCCESS;
}

byte ELM327::ambientAirTemperature(int &temperature){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","46",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	temperature=values[0]-40;
	return ELM_SUCCESS;
}

byte ELM327::absoluteThrottlePositionB(byte &position){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","47",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	position=(100*values[0])/255;
	return ELM_SUCCESS;
}
byte ELM327::absoluteThrottlePositionC(byte &position){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","48",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	position=(100*values[0])/255;
	return ELM_SUCCESS;
}
byte ELM327::acceleratorPedalPositionD(byte &position){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","49",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	position=(100*values[0])/255;
	return ELM_SUCCESS;
}
byte ELM327::acceleratorPedalPositionE(byte &position){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","4A",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	position=(100*values[0])/255;
	return ELM_SUCCESS;
}
byte ELM327::acceleratorPedalPositionF(byte &position){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","4B",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	position=(100*values[0])/255;
	return ELM_SUCCESS;
}
byte ELM327::commandedThrottleActuator(byte &position){
	byte status;
	byte values[1]={0};
	status=getBytes("01","41","4C",values,1);
	if (status != ELM_SUCCESS){
		return status;
	}
	position=(100*values[0])/255;
	return ELM_SUCCESS;
}

byte ELM327::fuelSystemStatus(int &systemstatus){
	byte status;
	byte values[2];
	status=getBytes("01","41","03",values,2);
	if (status != ELM_SUCCESS){
		return status;
	}
	systemstatus=(values[0]<<8)+values[1];
	return ELM_SUCCESS;
}
#endif



#ifdef ELM327_DEBUG
#include <stdarg.h>
void p(char *fmt, ... ){
	char tmp[128]; // resulting string limited to 128 chars
	va_list args;
	va_start (args, fmt );
	vsnprintf(tmp, 128, fmt, args);
	va_end (args);
	Serial.print(tmp);
}
#endif


byte ELM327::getBytes( const char *mode, const char *chkMode, const char *pid, byte *values, unsigned int numValues){
	char data[64];
	byte status;
	char hexVal[]="0x00";
	char cmd[6];
	cmd[0]=mode[0];
	cmd[1]=mode[1];
	cmd[2]=' ';
	cmd[3]=pid[0];
	cmd[4]=pid[1];
	cmd[5]='\0';

	status=runCommand(cmd,data,64);
#ifdef ELM327_DEBUG
	p("cmd/data: %s/%s\n",cmd,data);
#endif
#ifdef ELM327_DEBUG_STRING
	debugString1 = "cmd/data: "+String(cmd)+"/"+String(data);
#endif

	if ( status != ELM_SUCCESS )
	{
		return status;
	}
	
	// Check the mode returned was the one we sent
	if ( data[0]!=chkMode[0] 
			|| data[1]!=chkMode[1]
			|| data[3]!=pid[0]
			|| data[4]!=pid[1] ){
		
		#ifdef ELM327_DEBUG
		// p("data: %s\n",data);
		p("A:%c(%d) %c(%d) %c(%d) %c(%d) {%d}\n", data[0],data[0],data[1],data[1],data[3],data[3],data[4],data[4],strlen(data));
		p("B:%c(%d) %c(%d) %c(%d) %c(%d)\n", chkMode[0],chkMode[0],chkMode[1],chkMode[1],pid[0],pid[0],pid[1],pid[1]);   
		#endif 
#ifdef ELM327_DEBUG_STRING
#define S(c) String((char)c)
		debugString2 = S(data[0])+" "+S(data[1])+" "+S(data[3])+" "+S(data[4])+S(strlen(data))+" : ";
		debugString2 +=  S(chkMode[0])+" "+ S(chkMode[1])+" "+ S(pid[0])+" "+ S(pid[1])+"\n";
#undef S			
#endif		
		return ELM_GARBAGE;
	}
	
	// For each byte expected, package it up
	// int i=0;
	for (size_t i=0; i<numValues; i++){
		hexVal[2]=data[6+(3*i)];
		hexVal[3]=data[7+(3*i)];
		values[i]=strtol(hexVal,NULL,16);
	}
	return ELM_SUCCESS;
}

byte ELM327::getBytes_( const char *mode, const char *chkMode, const char *pid, byte *values, unsigned int numValues){
	char data[64];
	byte status;
	char hexVal[]="0x00";
	
	size_t pidlength=strlen(pid);
	
	//char cmd[6];
	char cmd[4+pidlength];
	cmd[0]=mode[0];
	cmd[1]=mode[1];
	cmd[2]=' ';
	//cmd[3]=pid[0];
	//cmd[4]=pid[1];
	//cmd[5]='\0';
	
	for(size_t x=0;x<pidlength;x++) {
		cmd[3+x]=pid[x];
	}
	cmd[3+pidlength]='\0';

	status=runCommand(cmd,data,64);
	//p("cmd/data: %s/ %s\n",cmd,data);

	if ( status != ELM_SUCCESS )
	{
		return status;
	};
	
	boolean pidok=true;
	for(size_t x=0;x<pidlength;x++) {
		pidok = pidok && (data[3+x] == pid[x]);
	}
	
	// Check the mode returned was the one we sent
	if ( data[0]!=chkMode[0] 
			or data[1]!=chkMode[1]
			or !pidok ){
		//p("%s\n",data);
		//p("A:%c(%d) %c(%d) %c(%d) %c(%d)\n", data[0],data[0],data[1],data[1],data[3],data[3],data[4],data[4]);
		//p("B:%c(%d) %c(%d) %c(%d) %c(%d)\n", chkMode[0],chkMode[0],chkMode[1],chkMode[1],pid[0],pid[0],pid[1],pid[1]);    
		return ELM_GARBAGE;
	}
	
	// For each byte expected, package it up
	// int i=0;
	for (unsigned int i=0; i<numValues; i++){
		hexVal[2]=data[6+(3*i)];
		hexVal[3]=data[7+(3*i)];
		values[i]=strtol(hexVal,NULL,16);
	}
	return ELM_SUCCESS;
}



byte ELM327::runCommand(const char *cmd, char *data, unsigned int dataLength)
{	
	// byte cmdLength;
	
	// Flush any leftover data from the last command.
	
	// Send the specified command to the controller.
	flush();
	if(bySoftSerial) {
		softserial->print(cmd);
		softserial->print('\r');
	} else {
		_serial->print(cmd);
		_serial->print('\r');
	}

	
	unsigned long timeOut;
	unsigned int counter;
	bool found;
	
	// Start reading the data right away and don't stop 
	// until either the requested number of bytes has 
	// been read or the timeout is reached, or the >
	// has been returned.
	//
	counter=0;
	//data[counter]='\0';
	timeOut=millis()+ELM_TIMEOUT;
	found=false;
	
	while (!found && counter<( dataLength ) && millis()<timeOut)
	{
		bool available = bySoftSerial ? softserial->available() : _serial->available();
		if ( available ){
			data[counter]=bySoftSerial ? softserial->read() : _serial->read();

			if( 0==counter && 13==data[counter]) {
				continue;
			}

			if (  data[counter] == '>' ){
				found=true;
				data[counter]='\0';
			}else if(data[counter] == 0x0d || data[counter] == 0x0a) {
				
			}else 
			{
				++counter;
			}
		}
	}
	// #ifdef ELM327_DEBUG
	//   p("%s : %s .",cmd,data);
	// #endif    
	// If there is still data pending to be read, raise OVERFLOW error.
	if (!found  && counter>=dataLength)
	{
		// Send a character, this should cancel any operation on the elm device
		// so that it doesnt spuriously inject a response during the next 
		// command
		delay(300);
		return ELM_BUFFER_OVERFLOW;
	}



	// If not found, and there is still buffer space, then raise no response error.
	if (!found && counter<dataLength){
		// Send a character, this should cancel any operation on the elm device
		// so that it doesnt spuriously inject a response during the next 
		// command
		//Serial.print("XXXXXXXXX\r\r\r");
		delay(300);
		return ELM_NO_RESPONSE;
	}



	char *match;
	match=strstr(data,"UNABLE TO CONNECT");
	if (match != NULL){
		return ELM_UNABLE_TO_CONNECT;
	}
	match=strstr(data,"NO DATA");
	if (match != NULL){
		return ELM_NO_DATA;
	}

	if (strncmp(data,"SEARCHING...",12)==0)
	{
		// Remove searching...
		byte i=12;
		while (data[i]!='\0'){
			data[i-12]=data[i];
			i++;
		}
		data[i]='\0';
	}

	// Otherwise return success.
	return ELM_SUCCESS;
}

#ifndef ELM_GEAR_ONLY
byte ELM327::getVersion(String &rev)
{
	char data[20];
	byte status;
	char cmd[]="ATI";
	status=runCommand(cmd,data,20);
	rev=String(data);
	return status;
}

byte ELM327::getIgnMon(bool &powered){
	char data[20];
	byte status;
	char cmd[]= "ATIGN";
	status=runCommand(cmd,data,20);
	if (cmd[1] == 'N' ){
		powered=true;
	}
	else{
		powered=false;
	}
	return status;
}
#endif

byte ELM327::getVoltage(float &voltage){
	char data[20];
	byte status;
	char cmd[]="ATRV";
	status=runCommand(cmd,data,20);
	if (status==ELM_SUCCESS){
		voltage=atof(data);
	}
	return status;
}



void ELM327::flush(){
	if( bySoftSerial ) {
		while (  softserial->read()  >= 0)
		; // do nothing
	}
	else {
		_serial->flush();
	}

}

/** returns the value of the specified bit p in byte b
*/
bool ELM327::getBit(byte b, byte p)
{
	b<<=(7-p);
	if (b>=127){
		return true;
	}
	return false;
}

String ELM327::debugStatus(byte status) {
	switch (status)
	{
		// case ELM_TIMEOUT:
		// Serial.println("ELM_TIMEOUT");
		// break;
	case ELM_SUCCESS:
		return F("ELM_SUCCESS");
		break;
	case ELM_NO_RESPONSE:
		return F("ELM_NO_RESPONSE");
		// break;
	case ELM_BUFFER_OVERFLOW:
		return F("ELM_BUFFER_OVERFLOW");
		// break;
	case ELM_GARBAGE:
		return F("ELM_GARBAGE");
		// break;
	case ELM_UNABLE_TO_CONNECT:
		return F("ELM_UNABLE_TO_CONNECT");
		// break;
	case ELM_NO_DATA:
		return F("ELM_NO_DATA");
		// break;
	default:
		return  "UNKNOWN: " +String(status)+"\n";

	}
}






