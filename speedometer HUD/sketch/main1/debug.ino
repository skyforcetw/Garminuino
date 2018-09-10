
void debug(String msg) {
#ifdef USE_SD_LOGGER
  bool SD_logged = true;
  if (SD_ready) {
    File log_file = SD.open(F("log/debug.txt"), FILE_WRITE);
    if (log_file) {
      unsigned long now = millis();
      log_file.print(String(now));
      log_file.print(": ");
      log_file.println(msg);
      log_file.flush();
      log_file.close();
    } else {
      Serial.println(F("SD logger failed!"));
    }
  } else {
    //    Serial.println(F("SD not ready!"));
    SD_logged = false;
  }
#else
  const bool SD_logged = true;
#endif

#ifdef USE_LOGGER
  if (SD_logged) {
    Serial.println(msg);
  } else {
    Serial.print(msg);
    Serial.println("*");
  }
#endif
}


