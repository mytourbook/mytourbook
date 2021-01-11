package net.tourbook.data;

import java.io.Serializable;

public class CustomTrackDefinition implements Serializable {
   /**
    *
    */
   private static final long serialVersionUID = -4880414084528375769L;
   //Custom Track Definition

   public static final String DEFAULT_CUSTOM_TRACK_NAME = "default";            //$NON-NLS-1$

   private String            _name;
   private String            _id;
   private String            _unit;

   public String getId() {
      return _id;
   }

   public String getName() {
      return _name;
   }

   public String getUnit() {
      return _unit;
   }

   public void setId(final String id) {
      _id = id;
   }

   public void setName(final String name) {
      _name = name;
   }

   public void setUnit(final String unit) {
      _unit = unit;

   }
}