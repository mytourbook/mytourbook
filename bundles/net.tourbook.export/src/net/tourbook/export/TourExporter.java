/*******************************************************************************
 * Copyright (C) 2020, 2021 Frédéric Bard
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourWayPoint;
import net.tourbook.database.TourDatabase;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.tools.generic.MathTool;
import org.dinopolis.gpstool.gpsinput.GPSRoute;
import org.dinopolis.gpstool.gpsinput.GPSTrack;
import org.dinopolis.gpstool.gpsinput.GPSTrackpoint;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrack;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrackpointAdapter;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrackpointD304;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.osgi.framework.Version;

public class TourExporter {
   /*
    * Velocity (VC) context values
    */
   private static final String            VC_HAS_TOUR_MARKERS        = "hasTourMarkers";                                   //$NON-NLS-1$
   private static final String            VC_HAS_TRACKS              = "hasTracks";                                        //$NON-NLS-1$
   private static final String            VC_HAS_WAY_POINTS          = "hasWayPoints";                                     //$NON-NLS-1$
   private static final String            VC_IS_EXPORT_ALL_TOUR_DATA = "isExportAllTourData";                              //$NON-NLS-1$

   private static final String            VC_TOUR_MARKERS            = "tourMarkers";                                      //$NON-NLS-1$
   private static final String            VC_TRACKS                  = "tracks";                                           //$NON-NLS-1$
   private static final String            VC_WAY_POINTS              = "wayPoints";                                        //$NON-NLS-1$
   private static final String            VC_LAP                     = "lap";                                              //$NON-NLS-1$
   private static final String            VC_TOUR_DATA               = "tourData";                                         //$NON-NLS-1$

   /**
    * This is a special parameter to force elevation values from the device and not from the lat/lon
    * + srtm data
    * <a href="http://strava.github.io/api/v3/uploads/">http://strava.github.io/api/v3/uploads/</a>.
    *
    * @since 15.6
    */
   private static final String            STRAVA_WITH_BAROMETER      = " with barometer";                                  //$NON-NLS-1$

   private static final String            ZERO                       = "0";                                                //$NON-NLS-1$

   private static final DecimalFormat     _nf1                       = (DecimalFormat) NumberFormat.getInstance(Locale.US);

   private static final DecimalFormat     _nf3                       = (DecimalFormat) NumberFormat.getInstance(Locale.US);
   private static final DecimalFormat     _nf8                       = (DecimalFormat) NumberFormat.getInstance(Locale.US);
   private static final DateTimeFormatter _dtIso                     = ISODateTimeFormat.dateTimeNoMillis();

   private static final SimpleDateFormat  _dateFormat                = new SimpleDateFormat();

   static {

      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
      _nf1.setGroupingUsed(false);

      _nf3.setMinimumFractionDigits(1);
      _nf3.setMaximumFractionDigits(3);
      _nf3.setGroupingUsed(false);

      _nf8.setMinimumFractionDigits(1);
      _nf8.setMaximumFractionDigits(8);
      _nf8.setGroupingUsed(false);

      _dateFormat.applyPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"); //$NON-NLS-1$
      _dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
   }

   private String       _activityType;
   /**
    * The speed is in meters/second
    */
   private float        _camouflageSpeed;
   private String       _courseName;
   private final String _formatTemplate;
   private boolean      _isCamouflageSpeed;
   private boolean      _isCourse;
   private boolean      _isExportAllTourData;
   private boolean      _isExportSurfingWaves;
   private boolean      _isExportWithBarometer;
   private boolean      _isRange;
   private TourData     _tourData;
   private int          _tourEndIndex;
   private int          _tourStartIndex;
   private boolean      _useAbsoluteDistance;
   private boolean      _useActivityType;
   private boolean      _useDescription;

   private boolean      _isGPX;
   private boolean      _isTCX;

   public enum ExportType {
      GPX, TCX
   }

   public TourExporter(final String formatTemplate) {

      _formatTemplate = formatTemplate;

      _isGPX = formatTemplate.toLowerCase().contains("gpx"); //$NON-NLS-1$
      _isTCX = formatTemplate.toLowerCase().contains("tcx"); //$NON-NLS-1$

      // .tcx files always contain absolute distances
      if (_isTCX) {
         setUseAbsoluteDistance(true);
      }
   }

   public TourExporter(final String formatTemplate,
                       final boolean isCamouflageSpeed,
                       final float camouflageSpeed,
                       final boolean isRange,
                       final int tourStartIndex,
                       final int tourEndIndex,
                       final boolean isExportWithBarometer,
                       final boolean useActivityType,
                       final String activityType,
                       final boolean useDescription,
                       final boolean isExportSurfingWaves,
                       final boolean isExportAllTourData,
                       final boolean isCourse,
                       final String courseName) {

      this(formatTemplate);

      setUseDescription(useDescription);
      setIsExportWithBarometer(isExportWithBarometer);
      setIsCamouflageSpeed(isCamouflageSpeed);
      setCamouflageSpeed(camouflageSpeed);
      _camouflageSpeed = camouflageSpeed;
      _isRange = isRange;
      _tourStartIndex = tourStartIndex;
      _tourEndIndex = tourEndIndex;
      setUseActivityType(useActivityType);
      setActivityType(activityType);

      //For GPX
      setIsExportSurfingWaves(isExportSurfingWaves);
      setIsExportAllTourData(isExportAllTourData);

      //For TCX
      setIsCourse(isCourse);
      setCourseName(courseName);
   }

   public boolean doExport_10_Tour(final List<GarminTrack> tracks,
                                   final List<TourWayPoint> wayPoints,
                                   final List<TourMarker> tourMarkers,
                                   final GarminLap lap,
                                   final String exportFileName) throws IOException {

      final File exportFile = new File(exportFileName);
      final VelocityContext vc = new VelocityContext();

      // math tool to convert float into double
      vc.put("math", new MathTool());//$NON-NLS-1$

      if (_isGPX) {

         vc.put(VC_IS_EXPORT_ALL_TOUR_DATA, _isExportAllTourData && _tourData != null);

      } else if (_isTCX) {

         vc.put("iscourses", _isCourse); //$NON-NLS-1$
         vc.put("coursename", _courseName); //$NON-NLS-1$
      }

      vc.put(VC_LAP, lap);
      vc.put(VC_TRACKS, tracks);
      vc.put(VC_WAY_POINTS, wayPoints);
      vc.put(VC_TOUR_MARKERS, tourMarkers);
      vc.put(VC_TOUR_DATA, _tourData);

      vc.put(VC_HAS_TOUR_MARKERS, Boolean.valueOf(tourMarkers.size() > 0));
      vc.put(VC_HAS_TRACKS, Boolean.valueOf(tracks.size() > 0));
      vc.put(VC_HAS_WAY_POINTS, Boolean.valueOf(wayPoints.size() > 0));

      vc.put("dateformat", _dateFormat); //$NON-NLS-1$
      vc.put("dtIso", _dtIso); //$NON-NLS-1$
      vc.put("nf1", _nf1); //$NON-NLS-1$
      vc.put("nf3", _nf3); //$NON-NLS-1$
      vc.put("nf8", _nf8); //$NON-NLS-1$

      doExport_20_TourValues(vc);

      try (final FileOutputStream fileOutputStream = new FileOutputStream(exportFile);
            final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
            final Writer exportWriter = new BufferedWriter(outputStreamWriter);
            final Reader templateReader = new InputStreamReader(TourExporter.class.getClassLoader().getResourceAsStream(_formatTemplate))) {

         Velocity.evaluate(vc, exportWriter, "MyTourbook", templateReader); //$NON-NLS-1$

      } catch (final Exception e) {
         StatusUtil.showStatus(e);
         return false;
      }

      return true;
   }

   /**
    * Adds some values to the velocity context (e.g. date, ...).
    *
    * @param vcContext
    *           the velocity context holding all the data
    */
   private void doExport_20_TourValues(final VelocityContext vcContext) {

      /*
       * Current time, date
       */
      final Calendar now = Calendar.getInstance();
      final Date creationDate = now.getTime();
      vcContext.put("creation_date", creationDate); //$NON-NLS-1$

      doExport_21_Creator(vcContext);
      doExport_22_MinMax_LatLon(vcContext);
      doExport_24_MinMax_Other(vcContext, creationDate);
   }

   private void doExport_21_Creator(final VelocityContext vcContext) {

      final Version version = Activator.getDefault().getVersion();

      /*
       * Version
       */
      String pluginMajorVersion = ZERO;
      String pluginMinorVersion = ZERO;
      String pluginMicroVersion = ZERO;
      String pluginQualifierVersion = ZERO;

      if (version != null) {
         pluginMajorVersion = Integer.toString(version.getMajor());
         pluginMinorVersion = Integer.toString(version.getMinor());
         pluginMicroVersion = Integer.toString(version.getMicro());
         final String versionQualifier = version.getQualifier();
         if (StringUtils.isNumeric(versionQualifier)) {
            pluginQualifierVersion = versionQualifier;
         }
      }

      vcContext.put("pluginMajorVersion", pluginMajorVersion); //$NON-NLS-1$
      vcContext.put("pluginMinorVersion", pluginMinorVersion); //$NON-NLS-1$
      vcContext.put("pluginMicroVersion", pluginMicroVersion); //$NON-NLS-1$
      vcContext.put("pluginQualifierVersion", pluginQualifierVersion); //$NON-NLS-1$

      /*
       * Creator
       */
      String creatorText = UI.EMPTY_STRING;
      if (version != null) {
         creatorText = String.format("MyTourbook %d.%d.%d.%s - http://mytourbook.sourceforge.net", //$NON-NLS-1$
               version.getMajor(),
               version.getMinor(),
               version.getMicro(),
               version.getQualifier());
      }

      if (_isExportWithBarometer) {
         creatorText += STRAVA_WITH_BAROMETER;
      }

      vcContext.put("creator", creatorText); //$NON-NLS-1$
   }

   /**
    * Calculate min/max values for latitude/longitude.
    */
   private void doExport_22_MinMax_LatLon(final VelocityContext vcContext) {

      /*
       * Extent of waypoint, routes and tracks:
       */
      double min_latitude = 90.0;
      double min_longitude = 180.0;
      double max_latitude = -90.0;
      double max_longitude = -180.0;

      final List<?> routes = (List<?>) vcContext.get("routes"); //$NON-NLS-1$
      if (routes != null) {
         final Iterator<?> route_iterator = routes.iterator();
         while (route_iterator.hasNext()) {
            final GPSRoute route = (GPSRoute) route_iterator.next();
            min_longitude = route.getMinLongitude();
            max_longitude = route.getMaxLongitude();
            min_latitude = route.getMinLatitude();
            max_latitude = route.getMaxLatitude();
         }
      }

      final List<?> wayPoints = (List<?>) vcContext.get(VC_WAY_POINTS);
      if (wayPoints != null) {
         final Iterator<?> waypoint_iterator = wayPoints.iterator();
         while (waypoint_iterator.hasNext()) {
            final TourWayPoint waypoint = (TourWayPoint) waypoint_iterator.next();
            min_longitude = Math.min(min_longitude, waypoint.getLongitude());
            max_longitude = Math.max(max_longitude, waypoint.getLongitude());
            min_latitude = Math.min(min_latitude, waypoint.getLatitude());
            max_latitude = Math.max(max_latitude, waypoint.getLatitude());
         }
      }

      final List<?> tourMarkers = (List<?>) vcContext.get(VC_TOUR_MARKERS);
      if (tourMarkers != null) {

         for (final Object element : tourMarkers) {
            if (element instanceof TourMarker) {

               final TourMarker tourMarker = (TourMarker) element;

               final double longitude = tourMarker.getLongitude();
               final double latitude = tourMarker.getLatitude();

               if (longitude != TourDatabase.DEFAULT_DOUBLE) {

                  min_longitude = Math.min(min_longitude, longitude);
                  max_longitude = Math.max(max_longitude, longitude);
                  min_latitude = Math.min(min_latitude, latitude);
                  max_latitude = Math.max(max_latitude, latitude);
               }
            }
         }
      }

      final List<?> tracks = (List<?>) vcContext.get(VC_TRACKS);
      if (tracks != null) {
         final Iterator<?> track_iterator = tracks.iterator();
         while (track_iterator.hasNext()) {
            final GPSTrack track = (GPSTrack) track_iterator.next();
            min_longitude = Math.min(min_longitude, track.getMinLongitude());
            max_longitude = Math.max(max_longitude, track.getMaxLongitude());
            min_latitude = Math.min(min_latitude, track.getMinLatitude());
            max_latitude = Math.max(max_latitude, track.getMaxLatitude());
         }
      }

      vcContext.put("min_latitude", Double.valueOf(min_latitude)); //$NON-NLS-1$
      vcContext.put("min_longitude", Double.valueOf(min_longitude)); //$NON-NLS-1$
      vcContext.put("max_latitude", Double.valueOf(max_latitude)); //$NON-NLS-1$
      vcContext.put("max_longitude", Double.valueOf(max_longitude)); //$NON-NLS-1$
   }

   /**
    * Min/max time, heart, cadence and other values.
    */
   private void doExport_24_MinMax_Other(final VelocityContext vcContext, final Date creationDate) {

      Date starttime = null;
      Date endtime = null;
      int heartNum = 0;
      long heartSum = 0;
      int cadNum = 0;
      long cadSum = 0;
      short maximumheartrate = 0;
      double totaldistance = 0;

      final List<?> tracks = (List<?>) vcContext.get(VC_TRACKS);

      for (final Object name : tracks) {

         final GPSTrack track = (GPSTrack) name;
         for (final Object waypoint : track.getWaypoints()) {

            final GPSTrackpoint wp = (GPSTrackpoint) waypoint;

            // starttime, totaltime
            if (wp.getDate() != null) {
               if (starttime == null) {
                  starttime = wp.getDate();
               }
               endtime = wp.getDate();
            }

            if (wp instanceof GarminTrackpointAdapter) {

               final GarminTrackpointAdapter gta = (GarminTrackpointAdapter) wp;

               // average heartrate, maximum heartrate
               if (gta.hasValidHeartrate()) {
                  heartSum += gta.getHeartrate();
                  heartNum++;
                  if (gta.getHeartrate() > maximumheartrate) {
                     maximumheartrate = gta.getHeartrate();
                  }
               }

               // average cadence
               if (gta.hasValidCadence()) {
                  cadSum += gta.getCadence();
                  cadNum++;
               }

               // total distance
               if (gta.hasValidDistance()) {
                  totaldistance = gta.getDistance();
               }
            }
         }
      }

      if (_useActivityType) {
         vcContext.put("activityType", _activityType); //$NON-NLS-1$
      }

      if (starttime != null) {
         vcContext.put("starttime", starttime); //$NON-NLS-1$
      } else {
         vcContext.put("starttime", creationDate); //$NON-NLS-1$
      }

      if ((starttime != null) && (endtime != null)) {
         vcContext.put("totaltime", ((double) endtime.getTime() - starttime.getTime()) / 1000); //$NON-NLS-1$
      } else {
         vcContext.put("totaltime", (double) 0); //$NON-NLS-1$
      }

      vcContext.put("totaldistance", totaldistance); //$NON-NLS-1$

      if (maximumheartrate != 0) {
         vcContext.put("maximumheartrate", maximumheartrate); //$NON-NLS-1$
      }

      if (heartNum != 0) {
         vcContext.put("averageheartrate", heartSum / heartNum); //$NON-NLS-1$
      }

      if (cadNum != 0) {
         vcContext.put("averagecadence", cadSum / cadNum); //$NON-NLS-1$
      }
   }

   public GarminLap doExport_50_Lap() {

      final GarminLap lap = new GarminLap();

      /*
       * Calories
       */
      lap.setCalories(_tourData.getCalories() / 1000);

      /*
       * Description
       */
      if (_useDescription) {
         final String notes = _tourData.getTourDescription();
         if ((notes != null) && (notes.length() > 0)) {
            lap.setNotes(notes);
         }
      }

      return lap;
   }

   /**
    * @param tourData
    * @param trackDateTime
    * @param _mergedDistance2
    * @param _mergedTime2
    * @return Returns a track or <code>null</code> when tour data cannot be exported.
    */
   public GarminTrack doExport_60_TrackPoints(final ZonedDateTime trackDateTime, final ZonedDateTime[] mergedTime, final int[] mergedDistance) {

      final int[] timeSerie = _tourData.timeSerie;

      if (timeSerie == null) {
         return null;
      }

      final float[] altitudeSerie = _tourData.altitudeSerie;
      final float[] cadenceSerie = _tourData.getCadenceSerie();
      final float[] distanceSerie = _tourData.distanceSerie;
      final long[] gearSerie = _tourData.gearSerie;
      final double[] latitudeSerie = _tourData.latitudeSerie;
      final double[] longitudeSerie = _tourData.longitudeSerie;
      final float[] pulseSerie = _tourData.pulseSerie;
      final float[] temperatureSerie = _tourData.temperatureSerie;
      final float[] powerSerie = _tourData.getPowerSerie();
      final float[] speedSerie = _tourData.getSpeedSerieMetric();

      final boolean isAltitude = (altitudeSerie != null) && (altitudeSerie.length > 0);
      final boolean isCadence = (cadenceSerie != null) && (cadenceSerie.length > 0);
      final boolean isDistance = (distanceSerie != null) && (distanceSerie.length > 0);
      final boolean isGear = (gearSerie != null) && (gearSerie.length > 0);
      final boolean isPulse = (pulseSerie != null) && (pulseSerie.length > 0);
      final boolean isTemperature = (temperatureSerie != null) && (temperatureSerie.length > 0);
      final boolean isPower = (powerSerie != null) && (powerSerie.length > 0);
      final boolean isSpeed = (speedSerie != null) && (speedSerie.length > 0);

      int prevTime = -1;
      ZonedDateTime lastTrackDateTime = null;

      // default is to use all trackpoints
      int startIndex = 0;
      int endIndex = timeSerie.length - 1;

      // adjust start/end when a part is exported
      if (_isRange) {
         startIndex = _tourStartIndex;
         endIndex = _tourEndIndex;
      }

      /*
       * Manage surfing waves
       */
      boolean[] visibleDataPointSerie = null;
      boolean isPrevDataPointVisible = false;
      if (_isExportSurfingWaves && _tourData.isVisiblePointsSaved_ForSurfing()) {

         visibleDataPointSerie = _tourData.visibleDataPointSerie;
         isPrevDataPointVisible = visibleDataPointSerie[0];
      }
      final boolean isExportSurfingWaves = visibleDataPointSerie != null;

      final GarminTrack track = new GarminTrack();

      /*
       * Track title/description
       */
      if (_useDescription) {

         final String tourTitle = _tourData.getTourTitle();
         if (tourTitle.length() > 0) {
            track.setIdentification(tourTitle);
         }

         final StringBuilder sbTourDescription = new StringBuilder();

         // add description from the tour
         sbTourDescription.append(_tourData.getTourDescription());

         // append surfing parameters to the description
         if (isExportSurfingWaves) {

            if (sbTourDescription.length() > 0) {
               sbTourDescription.append(UI.NEW_LINE2);
            }

            sbTourDescription.append(String.format(

                  Messages.Dialog_Export_Description_SurfingWaves,

                  // min start/stop speed
                  _tourData.getSurfing_MinSpeed_StartStop(),
                  UI.UNIT_LABEL_SPEED,

                  // min surfing speed
                  _tourData.getSurfing_MinSpeed_Surfing(),
                  UI.UNIT_LABEL_SPEED,

                  // min time duration
                  _tourData.getSurfing_MinTimeDuration(),
                  Messages.App_Unit_Seconds_Small,

                  // min distance
                  _tourData.isSurfing_IsMinDistance() ? _tourData.getSurfing_MinDistance() : 0,
                  UI.UNIT_LABEL_DISTANCE_M_OR_YD));
         }

         if (sbTourDescription.length() > 0) {
            track.setComment(sbTourDescription.toString());
         }
      }

      /*
       * loop: trackpoints
       */
      for (int serieIndex = startIndex; serieIndex <= endIndex; serieIndex++) {

         final GarminTrackpointD304 tp304 = new GarminTrackpointD304();
         final GarminTrackpointAdapterExtended tpExt = new GarminTrackpointAdapterExtended(tp304);

         /*
          * Manage surfing
          */
         boolean isCreateTrackSegment = false;
         if (isExportSurfingWaves) {

            // export only surfing events

            final boolean isDataPointVisible = visibleDataPointSerie[serieIndex];

            if (isDataPointVisible && isDataPointVisible != isPrevDataPointVisible) {

               // visibility has changed -> show track

               isCreateTrackSegment = true;

            } else if (!isDataPointVisible) {

               // hide time slices until they are visible again

               isPrevDataPointVisible = false;

               continue;
            }

            isPrevDataPointVisible = isDataPointVisible;
         }

         // mark as a new track to create the <trkseg>...</trkseg> tags
         if (serieIndex == startIndex || isCreateTrackSegment) {
            tpExt.setNewTrack(true);
         }

         if (isAltitude) {
            tpExt.setAltitude(altitudeSerie[serieIndex]);
         }

         // I don't know if this is according to the rules to have a gpx/tcx without lat/lon
         if (latitudeSerie != null && longitudeSerie != null) {
            tpExt.setLatitude(latitudeSerie[serieIndex]);
            tpExt.setLongitude(longitudeSerie[serieIndex]);
         }

         float distance = 0;
         if (isDistance) {

            if (_useAbsoluteDistance) {

               distance = distanceSerie[serieIndex];

            } else if (distanceSerie != null && serieIndex > startIndex) {
               // skip first distance difference
               distance = distanceSerie[serieIndex] - distanceSerie[serieIndex - 1];
            }
         }

         int relativeTime;
         if (_isCamouflageSpeed && isDistance) {

            // camouflage speed

            relativeTime = (int) (distance / _camouflageSpeed);

         } else {

            // keep recorded speed

            relativeTime = timeSerie[serieIndex];
         }

         if (isDistance) {
            tpExt.setDistance(distance + mergedDistance[0]);
         }

         if (isCadence) {
            tp304.setCadence((short) cadenceSerie[serieIndex]);
         }

         if (isPulse) {
            tp304.setHeartrate((short) pulseSerie[serieIndex]);
         }

         if (isTemperature) {
            tpExt.setTemperature(temperatureSerie[serieIndex]);
         }

         if (isGear) {
            tpExt.setGear(gearSerie[serieIndex]);
         }

         if (isPower) {
            tpExt.setPower((short) Math.round(powerSerie[serieIndex]));
         }

         if (isSpeed) {
            final double speedValue = Math.round(speedSerie[serieIndex] * 10.0) / 10.0;
            tpExt.setSpeed(speedValue);
         }

         // ignore trackpoints which have the same time
         if (relativeTime != prevTime) {

            lastTrackDateTime = trackDateTime.plusSeconds(relativeTime);

            tpExt.setDate(Date.from(lastTrackDateTime.toInstant()));

            track.addWaypoint(tpExt);
         }

         prevTime = relativeTime;
      }

      /*
       * Keep values for the next merged tour
       */
      if (isDistance && _useAbsoluteDistance) {

         final float distanceDiff = distanceSerie[endIndex] - distanceSerie[startIndex];
         mergedDistance[0] += distanceDiff;
      }

      mergedTime[0] = lastTrackDateTime;

      return track;
   }

   public void doExport_70_WayPoints(final List<TourWayPoint> exportedWayPoints,
                                     final List<TourMarker> exportedTourMarkers,
                                     final ZonedDateTime tourStartTime) {

      final int[] timeSerie = _tourData.timeSerie;
      final float[] altitudeSerie = _tourData.altitudeSerie;
      final double[] latitudeSerie = _tourData.latitudeSerie;
      final double[] longitudeSerie = _tourData.longitudeSerie;
      final float[] distanceSerie = _tourData.distanceSerie;

      final Set<TourMarker> tourMarkers = _tourData.getTourMarkers();
      final Set<TourWayPoint> tourWayPoints = _tourData.getTourWayPoints();

      // ensure required dataseries are available
      if (timeSerie == null || latitudeSerie == null || longitudeSerie == null) {
         return;
      }

      final boolean isAltitude = altitudeSerie != null;
      final boolean isDistance = (distanceSerie != null) && (distanceSerie.length > 0);

      // default is to use all trackpoints
      int startIndex = 0;
      int endIndex = timeSerie.length - 1;
      boolean isRange = false;

      // adjust start/end when a part is exported
      if (_isRange) {
         startIndex = _tourStartIndex;
         endIndex = _tourEndIndex;
         isRange = true;
      }

      /*
       * Create exported tour marker
       */
      for (final TourMarker tourMarker : tourMarkers) {

         final int serieIndex = tourMarker.getSerieIndex();

         // skip markers when they are not in the defined range
         if (isRange && (serieIndex < startIndex) || (serieIndex > endIndex)) {
            continue;
         }

         /*
          * get distance
          */
         float distance = 0;
         if (isDistance) {

            if (_useAbsoluteDistance) {

               distance = distanceSerie[serieIndex];

            } else if (distanceSerie != null && serieIndex > startIndex) {
               // skip first distance difference
               distance = distanceSerie[serieIndex] - distanceSerie[serieIndex - 1];
            }
         }

         /*
          * get time
          */
         int relativeTime;
         if (_isCamouflageSpeed && isDistance) {

            // camouflage speed

            relativeTime = (int) (distance / _camouflageSpeed);

         } else {

            // keep recorded speed

            relativeTime = timeSerie[serieIndex];
         }

         final ZonedDateTime zonedMarkerTime = tourStartTime.plusSeconds(relativeTime);
         final long absoluteMarkerTime = zonedMarkerTime.toInstant().toEpochMilli();

         /*
          * Setup exported tour marker
          */
         final TourMarker exportedTourMarker = tourMarker.clone();

         exportedTourMarker.setTime(relativeTime, absoluteMarkerTime);
         exportedTourMarker.setLatitude(latitudeSerie[serieIndex]);
         exportedTourMarker.setLongitude(longitudeSerie[serieIndex]);

         if (isAltitude) {
            exportedTourMarker.setAltitude(altitudeSerie[serieIndex]);
         }

         if (isDistance) {
            exportedTourMarker.setDistance(distance);
         }

         exportedTourMarkers.add(exportedTourMarker);
      }

      for (final TourWayPoint twp : tourWayPoints) {

         final TourWayPoint wayPoint = new TourWayPoint();

         wayPoint.setTime(twp.getTime());
         wayPoint.setLatitude(twp.getLatitude());
         wayPoint.setLongitude(twp.getLongitude());

         wayPoint.setName(twp.getName());

         // <desc>...</desc>
         final String comment = twp.getComment();
         final String description = twp.getDescription();
         final String descText = description != null ? description : comment;
         if (descText != null) {
            wayPoint.setComment(descText);
         }

         wayPoint.setAltitude(twp.getAltitude());

         wayPoint.setUrlAddress(twp.getUrlAddress());
         wayPoint.setUrlText(twp.getUrlText());

         exportedWayPoints.add(wayPoint);
      }
   }

   public boolean export(final String exportFileName) {

      final ArrayList<GarminTrack> tracks = new ArrayList<>();
      final ArrayList<TourWayPoint> wayPoints = new ArrayList<>();
      final ArrayList<TourMarker> tourMarkers = new ArrayList<>();

      final ZonedDateTime trackStartTime = _tourData.getTourStartTime();

      final GarminLap tourLap = doExport_50_Lap();

      final GarminTrack track = doExport_60_TrackPoints(trackStartTime, new ZonedDateTime[1], new int[1]);
      if (track != null) {
         tracks.add(track);
      }

      doExport_70_WayPoints(wayPoints, tourMarkers, trackStartTime);

      try {
         return doExport_10_Tour(tracks, wayPoints, tourMarkers, tourLap, exportFileName);
      } catch (final IOException e) {
         StatusUtil.log(e);
      }
      return false;
   }

   public void setActivityType(final String activityType) {
      _activityType = activityType;
   }

   public void setCamouflageSpeed(final float camouflageSpeed) {
      _camouflageSpeed = camouflageSpeed;
   }

   public void setCourseName(final String courseName) {
      _courseName = courseName;
   }

   public void setIsCamouflageSpeed(final boolean isCamouflageSpeed) {
      _isCamouflageSpeed = isCamouflageSpeed;
   }

   public void setIsCourse(final boolean isCourse) {
      _isCourse = isCourse;
   }

   public void setIsExportAllTourData(final boolean isExportAllTourData) {
      _isExportAllTourData = isExportAllTourData;
   }

   public void setIsExportSurfingWaves(final boolean isExportSurfingWaves) {
      _isExportSurfingWaves = isExportSurfingWaves;
   }

   public void setIsExportWithBarometer(final boolean isExportWithBarometer) {
      _isExportWithBarometer = isExportWithBarometer;
   }

   public void setUseAbsoluteDistance(final boolean useAbsoluteDistance) {
      _useAbsoluteDistance = useAbsoluteDistance;
   }

   public void setUseActivityType(final boolean useActivityType) {
      _useActivityType = useActivityType;
   }

   public void setUseDescription(final boolean useDescription) {
      _useDescription = useDescription;
   }

   public TourExporter useTourData(final TourData tourData) {

      _tourData = tourData;

      return this;
   }
}
