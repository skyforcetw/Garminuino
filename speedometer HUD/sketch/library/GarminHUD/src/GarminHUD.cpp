/**
* Garmin HUD
* Frank Huebenthal 2015
* Arduino modify by skyforce Shen 2016
*/


#if ARDUINO>=100
#include <Arduino.h> // Arduino 1.0
#else
#include <Wprogram.h> // Arduino 0022
#endif

#include "GarminHUD.h"

//===========================================================================================
// GarminHUDInterface
//===========================================================================================
	
	
char GarminHUDInterface::Digit(int n)
{
	n = n % 10;
	if (n == 0)
	return (char)10;
	return (char)n;
}

boolean GarminHUDInterface::getSendResult() {
	return sendResult;
}




void GarminHUDInterface::SendHud(char* pBuf, int nLen)
{
	char sendBuf[255], len = 0;
	unsigned int nCrc = 0xeb + nLen + nLen;

	sendBuf[len++] = 0x10;
	sendBuf[len++] = 0x7b;
	sendBuf[len++] = nLen + 6;
	if (nLen == 0xa)
	sendBuf[len++] = 0x10;
	sendBuf[len++] = nLen;
	sendBuf[len++] = 0x00;	
	sendBuf[len++] = 0x00;
	sendBuf[len++] = 0x00;
	sendBuf[len++] = 0x55;
	sendBuf[len++] = 0x15;
	for (int i = 0; i < nLen; i++)
	{
		nCrc += pBuf[i];
		sendBuf[len++] = pBuf[i];
		if (pBuf[i] == 0x10) //Escape LF
		sendBuf[len++] = 0x10;
	}

	sendBuf[len++] = (-(int)nCrc) & 0xff;
	sendBuf[len++] = 0x10;
	sendBuf[len++] = 0x03;
	
	SendPacket(sendBuf, len);
}

void GarminHUDInterface::SendHud2(char* pBuf, int nLen)
{
	//int nLen = pBuf.length;

	char sendBuf[255];
	char len = 0;
	int stuffing_count = 0;

	sendBuf[len++] = 0x10;
	sendBuf[len++] = 0x7b;
	sendBuf[len++] = (char) (nLen + 6);
	if (nLen == 0xa) {
		sendBuf[len++] = 0x10;
		stuffing_count++;
	}
	sendBuf[len++] = (char) nLen;
	sendBuf[len++] = 0x00;
	sendBuf[len++] = 0x00;
	sendBuf[len++] = 0x00;
	sendBuf[len++] = 0x55;
	sendBuf[len++] = 0x15;

	for (int i = 0; i < nLen; i++) {
		//nCrc += pBuf[i];
		sendBuf[len++] = pBuf[i];
		if (pBuf[i] == 0x10) { //Escape LF
			sendBuf[len++] = 0x10;
			stuffing_count++;
		}
	}

	int nCrc = 0;
	for (int i = 1; i < len; i++) {
		nCrc += sendBuf[i];
	}
	nCrc -= stuffing_count * 0x10;


	sendBuf[len++] = (char) ((-(int) nCrc) & 0xff);
	sendBuf[len++] = 0x10;
	sendBuf[len++] = 0x03;

	sendResult = SendPacket(sendBuf, len);
}

GarminHUDInterface::GarminHUDInterface()
{
}


GarminHUDInterface::~GarminHUDInterface()
{
}

void GarminHUDInterface::SetTime(int nH, int nM, bool bFlag, bool bTraffic, bool bColon, bool bH)
{
	char arr[] = {(char) 0x05,
		bTraffic ? (char) 0xff : (char) 0x00,
		Digit(nH / 10), Digit(nH), bColon ? (char) 0xff : (char) 0x00,
		Digit(nM / 10), Digit(nM), bH ? (char) 0xff : (char) 0x00,
		bFlag ? (char) 0xff : (char) 0x00};	
	
	// unsigned char arr[] = {(unsigned char) 0x05,
	// (unsigned char)(bTraffic ? 0xff : 0x00),
	// Digit(nH / 10), Digit(nH), 
	// (unsigned char)(bColon ? 0xff : 0x00),
	// Digit(nM / 10), Digit(nM), 
	// (unsigned char)(bH ? 0xff : 0x00),
	// (unsigned char)(bFlag ? 0x00 : 0xff) };

	SendHud(arr, sizeof(arr));
}

void GarminHUDInterface::ClearTime() {
	char arr[] = {( char) 0x05,
		0x00 ,
		0, 0 , 
		0x00 ,
		0, 0 , 
		0x00 ,
		// 0x00 
	};
	SendHud(arr, sizeof(arr));
}



void GarminHUDInterface::SetDistance(int nDist, eUnits unit, bool bDecimal, bool bLeadingZero)
{
	char arr[] = {
		( char) 0x03,
		Digit(nDist / 1000), 
		Digit(nDist / 100), 
		Digit(nDist / 10), 
		( char)(bDecimal ? 0xff : 0x00), 
		Digit(nDist), 
		( char)unit };
	
	if (!bLeadingZero)
	{
		if (arr[1] == 0xa)
		{
			arr[1] = 0;
			if (arr[2] == 0xa)
			{
				arr[2] = 0;
				if (arr[3] == 0xa)
				{
					arr[3] = 0;
				}
			}
		}
	}

	SendHud(arr, sizeof(arr));
}

void GarminHUDInterface::ClearDistance() {
	char arr[] = {
		( char) 0x03,
		0, 
		0, 
		0, 
		0x00, 
		0, 
		( char)None };
	SendHud(arr, sizeof(arr));
}

void GarminHUDInterface::SetAlphabet(char a, char b, char c, char d) {
	eUnits unit = None;
	boolean bDecimal = false, bLeadingZero = false;


	char arr[] = {(char) 0x03, a, b, c,
		bDecimal ? (char) 0xff : (char) 0x00, d, (char) unit};
	if (!bLeadingZero) {
		if (arr[1] == 0xa) {
			arr[1] = 0;
			if (arr[2] == 0xa) {
				arr[2] = 0;
				if (arr[3] == 0xa) {
					arr[3] = 0;
				}
			}
		}
	}
	SendHud(arr, sizeof(arr));
}

void GarminHUDInterface::SetDirection(eOutAngle nDir, eOutType nType, eOutAngle nRoundaboutOut )
{
	char arr[] = {
		(char) 0x01,
		(char) ((nDir == LeftDown) ? 0x10 : ((nDir == RightDown) ? 0x20 : nType)), 
		(char)((nType == RightRoundabout || nType == LeftRoundabout) ? ((nRoundaboutOut == AsDirection) ? nDir : nRoundaboutOut) : 0x00), 
		(char)((nDir == LeftDown || nDir == RightDown) ? 0x00 : nDir)  	};
	SendHud(arr, sizeof(arr));
}

void GarminHUDInterface::SetLanes(char nArrow, char nOutline)
{
	char arr[] = { (char)0x02, nOutline, nArrow };
	SendHud(arr, sizeof(arr));
}

void GarminHUDInterface::SetSpeedWarning(int nSpeed, int nLimit, bool bSpeeding, bool bIcon, bool bSlash)
{
	char arr[] = { 
		(char)0x06,
		
		(char)((nSpeed / 100) % 10), 
		Digit(nSpeed / 10), 
		Digit(nSpeed), 
		
		(char)(bSlash ? 0xff : 0x00),
		
		(char) ((nLimit / 100) % 10), 
		Digit(nLimit / 10), 
		Digit(nLimit), 
		
		(char)(bSpeeding ? 0xff : 0x00), 
		(char)(bIcon ? 0xff : 0x00) };

	SendHud(arr, sizeof(arr));
}


void GarminHUDInterface::ShowCameraIcon()
{
	char arr[] = { 0x04, 0x01 };
	SendHud(arr, sizeof(arr));
}

void GarminHUDInterface::SetCameraIcon(bool visible) 
{
	char arr[] = { 0x04, visible };
	SendHud(arr, sizeof(arr));
}




void GarminHUDInterface::SetSpeedWarning(int nLimit, bool bSpeeding, bool bIcon, bool bSlash)
{
	char arr[] = { 
		(char)0x06,
		
		(char)0, 
		(char)0, 
		(char)0, 
		
		(char)(bSlash ? 0xff : 0x00),
		
		(char) ((nLimit / 100) % 10), 
		Digit(nLimit / 10), 
		Digit(nLimit), 
		
		(char)(bSpeeding ? 0xff : 0x00), 
		(char)(bIcon ? 0xff : 0x00) };

	SendHud(arr, sizeof(arr));
}


void GarminHUDInterface::SetSpeed(int nSpeed, bool bSpeeding, bool bIcon, bool bSlash)
{
	char arr[] = { 
		(char)0x06,
		
		(char)((nSpeed / 100) % 10), 
		Digit(nSpeed / 10), 
		Digit(nSpeed), 
		
		(char)(bSlash ? 0xff : 0x00),
		
		(char) 0, 
		(char) 0, 
		(char) 0, 
		
		(char)(bSpeeding ? 0xff : 0x00), 
		(char)(bIcon ? 0xff : 0x00) };

	SendHud(arr, sizeof(arr));
}

void GarminHUDInterface::SetWarning(bool bSpeeding, bool bIcon, bool bSlash)
{
	char arr[] = { 
		(char)0x06,
		
		(char)0, 
		(char)0, 
		(char)0, 
		
		(char)(bSlash ? 0xff : 0x00),
		
		(char)0, 
		(char)0, 
		(char)0, 
		
		(char)(bSpeeding ? 0xff : 0x00), 
		(char)(bIcon ? 0xff : 0x00) };

	SendHud(arr, sizeof(arr));
}

void GarminHUDInterface::SetGear(int gear) {
	bool bSpeeding= false;
	bool bIcon= false;
	bool bSlash= false;
	
	char arr[] = { 
		(char)0x06,
		
		//speed
		(char)0,
		(char)0,
		(char)0, 
		
		(char)( bSlash ? 0xff : 0x00),
		
		//limit
		(char) ((gear / 100) % 10), 
		Digit(gear / 10), 
		Digit(gear), 
		
		(char)( bSpeeding ? 0xff : 0x00), 
		(char)(bIcon ? 0xff : 0x00) };

	SendHud(arr, sizeof(arr));
}

void GarminHUDInterface::ShowbIcon(bool show) {
	bool bSpeeding= false;
	bool bSlash= false;
	
	char arr[] = { 
		(char)0x06,
		0,0,0, 
		(char)(bSlash ? 0xff : 0x00),
		0,0,0, 
		(char)(bSpeeding ? 0xff : 0x00), 
		(char)(show ? 0xff : 0x00) };

	SendHud(arr, sizeof(arr));
}

void GarminHUDInterface::ShowGpsLabel()
{
	char arr[] = { (char)0x07, (char)0x01 };
	SendHud(arr, sizeof(arr));
}

void GarminHUDInterface::SetGpsLabel(bool visible)
{
	char arr[] = { (char)0x07, visible };
	SendHud(arr, sizeof(arr));
}

void GarminHUDInterface::SetAutoBrightness()
{
	char command_auto_brightness[] = { 0x10,0x7B,0x0E,0x08,0x00,0x00,0x00,0x56,0x15,0x02,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x02,0x10,0x03 };
	SendPacket(command_auto_brightness, sizeof(command_auto_brightness));
}

void GarminHUDInterface::SetBrightness(int brightness) 
{
	char sendBuf[8];
	int len = 0;
	int stuffing_count = 0;

	sendBuf[len++] = 0x10;
	sendBuf[len++] = 0x0f;
	sendBuf[len++] = 0x02;
	sendBuf[len++] = (char) brightness;
	sendBuf[len++] = 0x00;

	int nCrc = 0;
	for (int i = 1; i < len; i++) {
		nCrc += sendBuf[i];
	}
	nCrc -= stuffing_count * 0x10;


	sendBuf[len++] = (char) ((-(int) nCrc) & 0xff);
	sendBuf[len++] = 0x10;
	sendBuf[len++] = 0x03;
	SendHud(sendBuf,sizeof(sendBuf));
}

void GarminHUDInterface::Clear() {
	SetCameraIcon(false);
	SetGpsLabel(false);
}

//===========================================================================================
// GarminHUD
//===========================================================================================
	
#ifdef TEENSYDUINO	
bool GarminHUD::Init(HardwareSerial serial)
#else
bool GarminHUD::Init(HardwareSerial* serial)
#endif
{
	_serial = serial;
}

bool GarminHUD::Init(SoftwareSerial* serial)
{
	softserial = serial;
	inSoftSerial = true;
	// Serial.println("soft");
}

GarminHUD::~GarminHUD()
{

}

int GarminHUD::SendPacket(const char* pBuf, int nLen)
{
	size_t size = 0;
	if(inSoftSerial) {
		size = softserial->write(pBuf, nLen);
		// softserial->flush();
		// Serial.println("soft");
		// Serial.write(pBuf, nLen);
	}else {
#ifdef TEENSYDUINO	
		size = _serial.write(pBuf, nLen);
#else
		size = _serial->write(pBuf, nLen);
		// size = Serial.write(pBuf, nLen);
		
#endif
	}
	return size;
}
//===========================================================================================