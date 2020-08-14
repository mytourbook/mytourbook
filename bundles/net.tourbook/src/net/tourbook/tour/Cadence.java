package net.tourbook.tour;

import net.tourbook.Messages;

public enum Cadence {

   NONE(0), RPM(1), SPM(2), INVALID(255);

   protected int value;
   private String label;

   private Cadence(final int value) {
      this.value = value;

      switch (value) {
      case 0:
         label = Messages.Enum_Cadence_None;
         break;
      case 1:
         label = Messages.Enum_Cadence_Rpm;
         break;
      case 2:
         label = Messages.Enum_Cadence_Spm;
         break;
      case 255:
         label = Messages.Enum_Cadence_Invalid;
         break;
      }

   }

   public static Cadence getByValue(final int value) {

      for (final Cadence type : Cadence.values()) {
         if (value == type.value) {
            return type;
         }
      }

      return Cadence.INVALID;
   }

   public String getLabel() {
      return label;
   }

   public float getMultiplier() {
      return value;
   }

   public int getValue() {
      return value;
   }
}

