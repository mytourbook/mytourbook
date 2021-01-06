package net.tourbook.data;

import java.io.Serializable;

public class CustomTrackValue implements Serializable {
   /**
    *
    */
   private static final long serialVersionUID = 1533142327708082208L;

   public String             id               = net.tourbook.common.UI.EMPTY_STRING; //Reference to CustomTrackDefinition
   public float              value            = Float.MIN_VALUE;

}
