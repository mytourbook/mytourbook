package net.tourbook.device.garmin.fit.listeners;

import com.garmin.fit.DateTime;
import com.garmin.fit.LapMesg;
import com.garmin.fit.LapMesgListener;

import java.util.Date;

import net.tourbook.data.TourMarker;
import net.tourbook.device.garmin.fit.FitContext;

/**
 * A {@link TourMarker} is set for each lap.
 */
public class Lap_MesgListenerImpl extends AbstractMesgListener implements LapMesgListener {

   private int _lapCounter;

   public Lap_MesgListenerImpl(final FitContext context) {
      super(context);
   }

   @Override
   public void onMesg(final LapMesg lapMesg) {

      context.getContextData().setupLap_Marker_10_Initialize();

      setMarker(lapMesg);

      context.getContextData().setupLap_Marker_20_Finalize();
   }

   private void setMarker(final LapMesg lapMesg) {

      final Integer messageIndex = getLapMessageIndex(lapMesg);
      final TourMarker tourMarker = getTourMarker();

      tourMarker.setLabel(messageIndex == null //
            ? Integer.toString(++_lapCounter)
            : messageIndex.toString());

      float lapDistance = -1;
      final Float totalDistance = lapMesg.getTotalDistance();
      if (totalDistance != null) {

         lapDistance = context.getLapDistance();
         lapDistance += totalDistance;

         context.setLapDistance(lapDistance);
         tourMarker.setDistance(lapDistance);
      }

      /*
       * Set lap time, later the time slice position (serie index) will be set.
       */
      final DateTime garminTime = lapMesg.getTimestamp();
      if (garminTime != null) {

         final Date javaTime = garminTime.getDate();

         tourMarker.setDeviceLapTime(javaTime.getTime());

      } else {

         final Float totalElapsedTime = lapMesg.getTotalElapsedTime();
         if (totalElapsedTime != null) {

            int lapTime = -1;

            lapTime = context.getLapTime();
//          lapTime += Math.round(totalElapsedTime);
            lapTime += totalElapsedTime;

            context.setLapTime(lapTime);
//
//         // the correct absolute time will be set later
            tourMarker.setTime(lapTime, Long.MIN_VALUE);
         }
      }
   }
}
