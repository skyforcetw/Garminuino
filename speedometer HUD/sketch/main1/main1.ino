#include <SPI.h>
#include <GarminHUD.h>
#include <SoftwareSerial.h>
#include "Timer.h"        //http://github.com/JChristensen/Timer

#define ELM_TIMEOUT 3000  //must define before include GearELM326.h
//#define USE_DUMMY_ELM327

#ifdef USE_DUMMY_ELM327
#include <DummyELM327.h>
#else
#include <GearELM327.h>
#endif
//#include "car.h"



//==================================================================
// function define
//==================================================================
#define USE_SPEED_FIX
#define SPEED_FIX_FACTOR 1.027421907087402
//#define USE_MP3
//#define USE_PHONE_CTRL
//==================================================================
// debug option
//#define USE_LOGGER
//#define USE_SD_LOGGER
//#define IGNORE_BT_LINK
//==================================================================

// option include
#ifdef USE_SD_LOGGER
#include <SD.h>
#endif

#ifdef USE_MP3
#include "DFRobotDFPlayerMini.h"
//==================================================================
// mp3 define
//==================================================================
#define MP3_DING_DONG_100 1
#define MP3_DING_DONG_110 2
#define MP3_PHONE_CTRL 3 //Safety_Net
#define MP3_PHONE_CTRL2 4 //Sophomore_Makeout
//1 for 86
//2 for shutter
//3 for safety
//4 for Sophomore
#endif

#ifdef USE_PHONE_CTRL
#include <Bluetooth_HC05.h>
#endif

//==================================================================
// pin define
//==================================================================
//total 18 pin, include tx/rx
//Cactus Micro Rev2給esp8266吃掉7個, 其中6個是pro micro原本的, 因此只剩下18-6=12個可用

//garmin/hud, pcb全接上(除了reset沒有拉出來)
#define GARMIN_LINKED      6 //HC-05 status
#define GARMIN_BT_RX        8
#define GARMIN_BT_TX        7 //use only for hc-05 setting
#define GARMIN_BT_KEY     A0
#define GARMIN_BT_RESET   9 //use only for hc-05 setting, low active


//ELM327/OBD, pcb沒有全接上, 要全接上, Phone Ctrl就靠他了
#define ELM327_LINKED   A3 //HC-05 status
#define ELM327_BT_RX     D0 //Serial1/D0 ==> A1
#define ELM327_BT_TX     D1 //Serial1/D1 ==> A2
#define ELM327_BT_KEY   10
#define ELM327_BT_RESET 11

#define MP3_RX          4
#define MP3_TX          5
#define SD_CS           3
//==================================================================

#define HUD_UART_BPS 38400
#define HUD_UART_AT_MODE_BPS 38400
#define OBD_UART_BPS 38400
#define SEC *1000

//==================================================================
// constant values
//==================================================================
//old experiment: limit of garmin hud is 7, set 6 for tolerance.
//new exp: 20Hz is ok!
#define MAX_HUD_REFRESH_FPS 8
#define HUD_REFRESH_TIME 1000/MAX_HUD_REFRESH_FPS
#define HUD_READY_TIME 5000
#define CHECK_BT_INTERVAL 1000

#define CTRL_MODE_ACTIVE 1000*30 //after this ms, phone ctrl mode active

#define HIGHWAY1_OVER_SPEED 108
#define HIGHWAY2_OVER_SPEED 118
#define HIGHWAY_OVER_SPEED_VOLUME 20
//==================================================================

//==================================================================
// error code
//==================================================================
#define ERR_HUD_NOT_READY   9999
#define ERR_ELM_GARBAGE     1111
#define ERR_ELM_NOT_SUCCESS 0000
//==================================================================

/*
   bt device address
   OBD +BIND:2016:9:125061
   (2nd hand) HUD/GarminHUD +BIND: 10c6:fc:431419
   (new)          HUD/GarminHUD +BIND: 10c6:fc:65a11a
*/

GarminHUD hud;
SoftwareSerial hud_serial(GARMIN_BT_TX, GARMIN_BT_RX); // RX, TX
ELM327 elm(&Serial1);

#ifdef USE_MP3
SoftwareSerial mp3Serial(MP3_RX, MP3_TX); // RX, TX
DFRobotDFPlayerMini dfPlayer;
//Car car;
#endif

Timer t;
int obd_event = -1;
int one_sec_event = -1;
unsigned long last_time_tcp_request = 0;
unsigned long last_time_command_request = 0;
bool hud_connected = false;
bool obd_connected = false;
bool SD_ready = false;
unsigned long hud_connected_time = 0;
unsigned long hud_unconnected_time = 0;
bool phone_ctrl_mode = false;

byte highway1_over_speed = HIGHWAY1_OVER_SPEED;
byte highway2_over_speed = HIGHWAY2_OVER_SPEED;
byte highway_over_speed_volume = HIGHWAY_OVER_SPEED_VOLUME;


#ifdef USE_PHONE_CTRL
//setting hc05 for garmin hud
void set_HC05_mode(bool master_mode) {
  hud_serial.listen();
  Bluetooth_HC05 hc05(&hud_serial);
  hc05.begin(HUD_UART_AT_MODE_BPS, GARMIN_BT_RESET, GARMIN_BT_KEY, HC05_MODE_COMMAND);
  /* Wait until HC-05 starts */
  delay(700);
  /* Allow HC-05 to initiate connections */
  hc05.initSerialPortProfile();
  hc05.setRole(master_mode ? HC05_ROLE_MASTER : HC05_ROLE_SLAVE);
  hc05.begin(HUD_UART_BPS, GARMIN_BT_RESET, GARMIN_BT_KEY, HC05_MODE_DATA);
  delay(700);
  hc05.softReset();
}
#endif

void check_bluetooth_connected() {
  bool org_hud_connected = hud_connected;
  hud_connected = digitalRead(GARMIN_LINKED);
  obd_connected = digitalRead(ELM327_LINKED);

  if (org_hud_connected != hud_connected) {
    hud_connected_time = hud_connected ? millis() : 0;
    debug(F("hud_connected changed"));
  }

  if (!hud_connected) {
    hud_unconnected_time = millis();
    debug(F("hud_connected failed"));
  }
  if (!obd_connected) {
    debug(F("obd_connected failed"));
  }

#ifdef USE_PHONE_CTRL
  if (!phone_ctrl_mode && hud_unconnected_time > CTRL_MODE_ACTIVE) {
    debug(F("phone control mode active"));
    phone_ctrl_mode = true;
    set_HC05_mode(false); //into slave mode
#ifdef USE_MP3
    dfPlayer.play(MP3_PHONE_CTRL2);
#endif
  }
#endif
}

void setup()
{
#ifdef USE_SD_LOGGER
  if (! (SD_ready = SD.begin(SD_CS))) {
    debug(F("SD Card failed, or not present"));
    // don't do anything more:
  } else {
    debug(F("SD card initialized."));
  }
#endif

  pinMode(GARMIN_LINKED, INPUT);
  pinMode(GARMIN_BT_KEY, OUTPUT);

  /* HC-05 郵票封裝
        PIO11 / 10 / 9 /8 ....
     天                         .
     線                         .
        TX/RX/CTS/RTS.


    1、PIO8 连接LED，指示模块工作状态，模块上电后闪烁，不同的状态闪烁间隔不同。
    2、PIO9 连接LED，指示模块连接成功，蓝牙串口匹配连接成功后，LED 长亮。
    3、PIO11 模块状态切换脚，高电平-->AT 命令响应工作状态，低电平或悬空-->蓝牙常规工作状态。
  */
  //KEY: PIO11 模块状态切换脚，高电平-->AT 命令响应工作状态，低电平或悬空-->蓝牙常规工作状态。
  //EN: 接到LDO開關, High->供電, Low->斷電. KEY邏輯與EN剛好相反
  digitalWrite(GARMIN_BT_KEY, HIGH);

  // Initialise serial port
  Serial.begin(9600);
  Serial1.begin(OBD_UART_BPS); //obd
  delay(3000);
  debug(F("Serial initialized."));


#ifdef USE_PHONE_CTRL
  set_HC05_mode(true);
#endif


#ifdef USE_MP3
  mp3Serial.begin(9600);
  dfPlayer.begin(mp3Serial);
  dfPlayer.volume(highway_over_speed_volume);  //Set volume value (0~30).
  debug(F("MP3 initialized."));
#endif

  if ( ELM_SUCCESS == elm.begin()) {
    debug(F("ELM327 initialized."));
  } else {
    debug(F("ELM327 initialized failed!"));
  }

  t.every(CHECK_BT_INTERVAL, check_bluetooth_connected);

  obd_event = t.every(  HUD_REFRESH_TIME, obd_hud_refresh);
  one_sec_event = t.every(  1 SEC, one_second_update);

  hud_serial.begin(HUD_UART_BPS); //hud
  hud.Init(&hud_serial);
  debug(F("HUD initialized."));
  hud.SetDistance(0, GarminHUDInterface::None);
}

void loop()
{
  t.update();
}




