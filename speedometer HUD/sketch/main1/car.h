#ifndef CAR_H //有問題??
#define CAR_H

//============================================================================
// Car define
//============================================================================

#define TIRE_ROUND 209.846f/100.0 // 215/55/R17
static const float GEAR_RATIO[] = {
  16.700868, //虛擬檔位, 沒有作用, 判斷時直接略過
  16.700868,
  10.080864,
  6.793047,
  4.973877,
  3.794952,
  3.068877,
  2.565465
};
#define MAX_GEAR  (7+1)
//============================================================================

class Car {
  private:
    float gear_rpm[MAX_GEAR];
    float min_delta ;
    byte min_index ;
    
  public:
    byte get_gear(int rpm, byte speed) {
      for (int x = 0; x < MAX_GEAR; x++) {
        gear_rpm[x] = speed * GEAR_RATIO[x] / TIRE_ROUND * 1000 / 60;
      }
      min_delta = 3.4028235E+38;
      min_index = 0;
      for (byte x = 1; x < MAX_GEAR; x++) {
        float delta = abs( gear_rpm[x] - rpm);
        if (delta < min_delta) {
          min_delta = delta;
          min_index = x;
        }
      }

      return min_index;
    }

};

#endif

