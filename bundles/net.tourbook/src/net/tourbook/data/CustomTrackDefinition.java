package net.tourbook.data;

import java.io.Serializable;

public class CustomTrackDefinition implements Serializable {
   /**
    *
    */
   private static final long serialVersionUID = -4880414084528375769L;
   //Custom Track Definition
   private String            _Name;
   private String            _Id;
   private String            _Unit;

   public String getId() {
      return _Id;
   }

   public String getName() {
      return _Name;
   }

   public String getUnit() {
      return _Unit;
   }

   public void setId(final String id) {
      _Id = id;
   }

   public void setName(final String name) {
      _Name = name;
   }

   public void setUnit(final String unit) {
      _Unit = unit;
   }
}