package net.tourbook.data;

import java.io.Serializable;

public class CustomTrackDefinition implements Serializable {
   /**
    *
    */
   private static final long serialVersionUID = -4880414084528375769L;
   //Custom Track Definition
   private String Name;
   private String Id;
   private String Unit;

   public String getId() {
      return Id;
   }

   public String getName() {
      return Name;
   }

   public String getUnit() {
      return Unit;
   }

   public void setId(final String id) {
      Id = id;
   }

   public void setName(final String name) {
      Name = name;
   }

   public void setUnit(final String unit) {
      Unit = unit;
   }
}