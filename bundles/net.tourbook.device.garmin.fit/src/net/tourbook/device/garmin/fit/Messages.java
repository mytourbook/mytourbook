package net.tourbook.device.garmin.fit;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

   private static final String BUNDLE_NAME = "net.tourbook.device.garmin.fit.messages"; //$NON-NLS-1$

   public static String        Import_Error_TourMarkerLabel_ExceededTimeSlice;

   public static String        PrefPage_Fit_Checkbox_IgnoreLastMarker;
   public static String        PrefPage_Fit_Checkbox_IgnoreSpeedValues;
   public static String        PrefPage_Fit_Checkbox_ReplaceTimeSlice;
   public static String        PrefPage_Fit_Checkbox_FitImportTourType;
   public static String        PrefPage_Fit_Group_AdjustTemperature;
   public static String        PrefPage_Fit_Group_IgnoreLastMarker;
   public static String        PrefPage_Fit_Group_Power;
   public static String        PrefPage_Fit_Group_ReplaceTimeSlice;
   public static String        PrefPage_Fit_Group_Speed;
   public static String        PrefPage_Fit_Group_TourType;
   public static String        PrefPage_Fit_Label_AdjustTemperature;
   public static String        PrefPage_Fit_Label_AdjustTemperature_Info;
   public static String        PrefPage_Fit_Label_IgnoreLastMarker_Info;
   public static String        PrefPage_Fit_Label_IgnoreLastMarker_TimeSlices;
   public static String        PrefPage_Fit_Label_IgnoreSpeedValues_Info;
   public static String        PrefPage_Fit_Label_Preferred_Power_Data_Source;
   public static String        PrefPage_Fit_Combo_Power_Data_Source_Stryd;
   public static String        PrefPage_Fit_Combo_Power_Data_Source_Garmin_RD_Pod;
   public static String        PrefPage_Fit_Label_ReplaceTimeSlice_Duration;
   public static String        PrefPage_Fit_Label_ReplaceTimeSlice_Info;
   public static String        PrefPage_Fit_Label_FitImportTourType_Info;
   public static String        PrefPage_Fit_Radio_ProfileNameFromSession;
   public static String        PrefPage_Fit_Radio_TourTypeFromSport;
   public static String        PrefPage_Fit_Radio_TourTypeFromProfile;
   public static String        PrefPage_Fit_Radio_TourTypeFromProfileElseSport;
   public static String        PrefPage_Fit_Radio_TourTypeFromSportAndProfile;

   static {
      // initialize resource bundle
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {}
}
