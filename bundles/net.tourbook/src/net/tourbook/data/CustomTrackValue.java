package net.tourbook.data;

import java.io.Serializable;

public class CustomTrackValue implements Serializable {
   /**
    *
    */
   private static final long serialVersionUID = 1533142327708082208L;

   public String Id    = "";             //Reference to CustomTrackDefinition
   public float  Value = Float.MIN_VALUE;
}
