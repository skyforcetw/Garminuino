/**
* Garmin HUD
* Frank Huebenthal 2015
* Arduino modify by skyforce Shen 2016
*/

/***********************************************************************/
/*            Class HUDInterface                                       */
/*                                                                     */
/* Controlling Garmin Head Up Display                                  */
/* Pure virtual class. Method SendPacket needs to be overridden        */
/* within an inheriting class by a function with the same signature.   */
/* This method does the actual sending of the bytes to the HUD.        */
/* Sending can be done by simple write  to a (virtual) COM port        */
/* assigned to the Bluetooth SSP device or by direct access to the     */
/* Bluetooth stack or using an external Serial Bluetooth Module like   */
/* the HC-05.                                                          */
/*                                                                     */
/* This class is based on the code of gabonator, see                   */
/* https://github.com/gabonator/Work-in-progress/tree/master/GarminHud */
/*                                                                     */
/* Frank Huebenthal 2015, huebenthal (at) uxis.de                      */
/***********************************************************************/

#ifndef GARMIN_HUD_H
#define GARMIN_HUD_H

#include <HardwareSerial.h>
#include <SoftwareSerial.h>

class GarminHUDInterface {
protected:
	
	char Digit(int n);
	virtual int SendPacket(const char* pBuf, int nLen) = 0;
	boolean sendResult = false;
	
public:
	boolean getSendResult();
	void SendHud(char* pBuf, int nLen);
	void SendHud2(char* pBuf, int nLen);
	typedef enum {
		SharpRight = 0x02,
		Right = 0x04,
		EasyRight = 0x08,
		Straight = 0x10,
		EasyLeft = 0x20,
		Left = 0x40,
		SharpLeft = 0x80,
		LeftDown = 0x81,
		RightDown = 0x82,
		AsDirection = 0x00
	} eOutAngle;
	
	typedef enum {
		Off = 0x00,
		Lane = 0x01,
		LongerLane = 0x02,
		LeftRoundabout = 0x04,
		RightRoundabout = 0x08,
		ArrowOnly = 0x80
	} eOutType;
	
	typedef enum {
		None = 0,
		Metres = 1,
		Kilometres = 3,
		Miles = 5
	} eUnits;
	
	typedef enum {
		DotsRight = 0x01,
		OuterRight = 0x02,
		MiddleRight = 0x04,
		InnerRight = 0x08,
		InnerLeft = 0x10,
		MiddleLeft = 0x20,
		OuterLeft = 0x40,
		DotsLeft = 0x80
	} eLane;
	
	GarminHUDInterface();
	virtual ~GarminHUDInterface();
	//bFlag 旗子
	//bTraffic 塞車
	//bColon 冒號
	//bH true: 沒h, false: 有h ?????
	void SetTime(int nH, int nM, bool bFlag = false, bool bTraffic = false, bool bColon = true, bool bH = false);
	void ClearTime();
	
	void SetDistance(int nDist, eUnits unit, bool bDecimal = false, bool bLeadingZero = false);
	void ClearDistance() ;
	void SetAlphabet(char a, char b, char c, char d);
	
	void SetDirection(eOutAngle nDir, eOutType nType = Lane, eOutAngle nRoundaboutOut = AsDirection );
	void SetLanes(char nArrow, char nOutline);
	
	//bSpeeding : 三角驚嘆號
	//bIcon : 像時速表的符號
	void SetSpeedWarning(int nSpeed, int nLimit, bool bSpeeding = false, bool bIcon = false, bool bSlash = true);
	void SetSpeedWarning(int nLimit, bool bSpeeding = false, bool bIcon = false, bool bSlash = true);
	void SetSpeed(int nSpeed, bool bSpeeding=false, bool bIcon=false, bool bSlash=false);
	void SetWarning(bool bSpeeding = false, bool bIcon = false, bool bSlash = false);
	void SetGear(int gear);
	void ShowbIcon(bool show);
	
	void ShowCameraIcon();
	void SetCameraIcon(bool visible);
	 
	void ShowGpsLabel();
	void SetGpsLabel(bool visible);
	
	void SetAutoBrightness();
	void SetBrightness(int brightness);
	
	void Clear();
};

class GarminHUD: public GarminHUDInterface{
	protected:
#ifdef TEENSYDUINO
	HardwareSerial _serial;
#else
	HardwareSerial* _serial;
#endif
	SoftwareSerial* softserial;
	bool inSoftSerial = false;

	virtual int SendPacket(const char* pBuf, int nLen);
public:
	GarminHUD() : GarminHUDInterface()  {};
	
#ifdef TEENSYDUINO	
	bool Init(HardwareSerial serial);
#else
	bool Init(HardwareSerial* serial);
#endif
	bool Init(SoftwareSerial* serial);
	virtual ~GarminHUD();
	bool isInSoftSerial() {
		return inSoftSerial;
	}
};

#endif
