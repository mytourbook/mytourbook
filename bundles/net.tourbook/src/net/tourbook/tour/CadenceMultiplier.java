package net.tourbook.tour;

import net.tourbook.Messages;

public enum CadenceMultiplier {

   NONE(0), RPM(1), SPM(2), INVALID(255);

   protected int value;
   private String label; // This field offers NLS support

   private CadenceMultiplier(final int value) {
      this.value = value;

      switch (value) {
      case 0:
         label = Messages.App_Cadence_None;
         break;
      case 1:
         label = Messages.App_Cadence_Rpm;
         break;
      case 2:
         label = Messages.App_Cadence_Spm;
         break;
      case 255:
         label = Messages.App_Cadence_Invalid;
         break;
      }

   }

   public static CadenceMultiplier getByValue(final int value) {

      for (final CadenceMultiplier type : CadenceMultiplier.values()) {
         if (value == type.value) {
            return type;
         }
      }

      return CadenceMultiplier.INVALID;
   }

   public float getMultiplier() {
      return value;
   }

   public String getNlsLabel() {
      return label;
   }
}

