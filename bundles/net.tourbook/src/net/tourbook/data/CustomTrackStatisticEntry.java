package net.tourbook.data;

import java.io.Serializable;

public class CustomTrackStatisticEntry implements Serializable {
   /**
    *
    */
   private static final long serialVersionUID = 8627742750216111261L;
   public float value_Max = Float.MIN_VALUE;
   public float value_Min = Float.MIN_VALUE;
   public float value_Avg = Float.MIN_VALUE;
}
