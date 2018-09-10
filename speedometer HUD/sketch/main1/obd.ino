
static byte _110kph_overspeed_time_high_th = 30;
static byte _110kph_overspeed_time_low_th = 20;
static byte _110kph_speed_high_th = highway2_over_speed;
static byte _110kph_speed_low_th = highway1_over_speed;

static int hud_refresh_times = 0;

void one_second_update() {
  hud_refresh_times = 0;
}

bool can_update_hud() {
  return hud_refresh_times <= MAX_HUD_REFRESH_FPS;
}

static bool _110_kph_mode_changed = false;
bool judge_110_kph_mode(byte speed,  const unsigned long now ) {

  /* 1. 起始在 not 110 mode
     2. 超速 持續 high th, 就跳到 110 mode
     3. 超速 持續 low th, 就跳回 not 110 mode
  */

  static bool last_110kph_mode = false;
  static bool _110kph_mode = false;
  //110kph mode detect
  static unsigned long overspeed_begin_time = 0;
  static unsigned long underspeed_begin_time = 0;
  const bool overspeed_low_th = speed > _110kph_speed_low_th;

  overspeed_begin_time = overspeed_low_th ? (0 == overspeed_begin_time) ? now : overspeed_begin_time : 0;
  underspeed_begin_time = !overspeed_low_th ? (0 == underspeed_begin_time) ? now : underspeed_begin_time : 0;
  const unsigned long overspeed_continue_time = now - overspeed_begin_time;
  const unsigned long underspeed_continue_time = now - underspeed_begin_time;

  if (!_110kph_mode && overspeed_low_th //不在110, 且一高超速
      &&  overspeed_continue_time > (_110kph_overspeed_time_high_th SEC ) ) {
    _110kph_mode = true;
    debug("overspeed_continue_time: " + String(overspeed_continue_time));
  }

  if (_110kph_mode && !overspeed_low_th
      && underspeed_continue_time >  (_110kph_overspeed_time_low_th SEC)) {
    _110kph_mode = false;
    debug("underspeed_continue_time: " + String(underspeed_continue_time));
  }

  if (last_110kph_mode != _110kph_mode) {
    debug("_110kph_mode changed, _110kph_mode: " + String(_110kph_mode));
    _110_kph_mode_changed = true;
  }

  last_110kph_mode = _110kph_mode;
  return _110kph_mode;
}

static byte elm_failed_count = 0;

void obd_hud_refresh() {
  byte speed;
#ifdef USE_DUMMY_ELM327
  byte status =  elm.vehicleSpeed(speed);
#else
  byte status = obd_connected ? elm.vehicleSpeed(speed) : ELM_UNABLE_TO_CONNECT;
#endif

  static bool _1st_elm_test_run = false;
  if (!_1st_elm_test_run && obd_connected) {
    status = elm.vehicleSpeed(speed);
    _1st_elm_test_run = false;
  }


  if (ELM_SUCCESS == status)  {
#ifdef USE_SPEED_FIX
    speed = (byte) round(speed * SPEED_FIX_FACTOR );
#endif
    process_elm_success2(speed);
  } else {
    process_elm_failed(status);
  }
}

void process_elm_success2(byte speed) {
  const unsigned long now = millis();
  const bool _110kph_mode = judge_110_kph_mode(speed, now);
  const byte speedlimit = _110kph_mode ? _110kph_speed_high_th : _110kph_speed_low_th;
  //mode 0:no over, 1:over low_th, 2:over high_th
  const byte overspeed_mode =  speed <= _110kph_speed_low_th ? 0 : speed <= _110kph_speed_high_th ? 1 : 2;
  static byte last_overspeed_mode = -1;
  static bool cancel_overspeed_warning = false;
  static bool double_cancel_overspeed_warning = false;

#ifdef IGNORE_BT_LINK
  const bool hud_ready = true;
#else
  //give some buffer time for hud can ready to recv data
  const bool hud_ready = hud_connected && 0 != hud_connected_time && ((now - hud_connected_time) >= HUD_READY_TIME);
#endif

  //  const bool overspeed = 0 != overspeed_mode &&   do_warning_overspeed;
  const bool overspeed = _110kph_mode ? overspeed_mode > 1 : overspeed_mode > 0;

  if ( overspeed ) {
    //超速超速超速超速超速超速超速超速超速超速超速超速超速
    const bool do_warning_overspeed =   last_overspeed_mode != overspeed_mode;  //超速且有變化
    static bool update_hud = true;

    if (do_warning_overspeed || false == update_hud) {
      const bool alarm_in_110kph = _110kph_mode || (2 == overspeed_mode);

#ifdef USE_MP3
      dfPlayer.volume(highway_over_speed_volume); //Set volume value (0~30).
      dfPlayer.loop(alarm_in_110kph ? MP3_DING_DONG_110 : MP3_DING_DONG_100);  //Loop the first mp3
#endif

      if (hud_ready && can_update_hud() ) {
        hud.SetSpeedWarning(alarm_in_110kph ? 110 : 100, true, false, false);
        hud_refresh_times++;
        update_hud = true;
      } else {
        update_hud = false;
      }
    }

    //超速超速超速超速超速超速超速超速超速超速超速超速超速
  } else {

    //沒超速沒超速沒超速沒超速沒超速沒超速沒超速沒超速沒超速
    if ( last_overspeed_mode != overspeed_mode || _110_kph_mode_changed) {
      _110_kph_mode_changed = false;
      cancel_overspeed_warning = true;
    }

    static bool update_hud = true;
    if (cancel_overspeed_warning || double_cancel_overspeed_warning || false == update_hud) {
#ifdef USE_MP3
      dfPlayer.pause();  //pause the mp3
#endif
      if (hud_ready &&  can_update_hud()) {
        hud.SetWarning();
        hud_refresh_times++;
        update_hud = true;
      } else {
        update_hud = false;
      }

      if ( double_cancel_overspeed_warning = true) {
        double_cancel_overspeed_warning = false;
      }
      if (cancel_overspeed_warning) {
        cancel_overspeed_warning = false;
        double_cancel_overspeed_warning = true;
      }
    }
    //沒超速沒超速沒超速沒超速沒超速沒超速沒超速沒超速沒超速
  }

  if (hud_ready  && can_update_hud() ) {
    hud.SetDistance(speed, GarminHUDInterface::None);
    hud_refresh_times++;
  }
  last_overspeed_mode = overspeed_mode;
}

void process_elm_failed( byte status ) {
  debug("elm.debugStatus: " + elm.debugStatus(status));

#ifdef USE_MP3
  dfPlayer.pause();  //pause the mp3
#endif

  if (ELM_GARBAGE == status) {
    elm_failed_count++;
    elm.flush();
    elm.begin();

    if (elm_failed_count > 2) {
      hud.SetDistance(ERR_ELM_GARBAGE, GarminHUDInterface::None, false, true);
    }

    debug(F("ELM_GARBAGE == status, delay one refresh time"));
    delay(HUD_REFRESH_TIME);
  } else {
    hud.SetDistance(ERR_ELM_NOT_SUCCESS, GarminHUDInterface::None, false, true);
    //      debug(F("ELM_SUCCESS != elm's status"));
    debug(F("ELM_SUCCESS != status, delay 1000ms"));
    delay(1000);
  }
}

