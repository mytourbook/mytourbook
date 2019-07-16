package net.tourbook.map25.layer.marker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.ClusterMarkerRenderer;
//import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerRendererFactory;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.marker.MarkerSymbol.HotspotPlace;

import net.tourbook.common.color.ColorUtil;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.map.bookmark.MapBookmark;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.layer.marker.MarkerToolkit.MarkerShape;

public class PhotoToolkit {


   public PhotoToolkit() {
      System.out.println("** PhotoToolkit constructor");
   }

   public final void loadConfig () {
      System.out.println("** PhotoToolkit loadConfig: creating photolayer ");
   }
   
   public List<MapMarker> createMapMarkers(final ArrayList<TourData> allTourData) {

      final List<MapMarker> allMarkerItems = new ArrayList<>();

      for (final TourData tourData : allTourData) {

         final Set<TourMarker> tourMarkerList = tourData.getTourMarkers();

         if (tourMarkerList.size() == 0) {
            continue;
         }

         // check if geo position is available
         final double[] latitudeSerie = tourData.latitudeSerie;
         final double[] longitudeSerie = tourData.longitudeSerie;
         if (latitudeSerie == null || longitudeSerie == null) {
            continue;
         }

         for (final TourMarker tourMarker : tourMarkerList) {

            // skip marker when hidden or not set
            if (tourMarker.isMarkerVisible() == false || tourMarker.getLabel().length() == 0) {
               continue;
            }

            final int serieIndex = tourMarker.getSerieIndex();

            /*
             * check bounds because when a tour is split, it can happen that the marker serie index
             * is out of scope
             */
            if (serieIndex >= latitudeSerie.length) {
               continue;
            }

            /*
             * draw tour marker
             */

            final double latitude = latitudeSerie[serieIndex];
            final double longitude = longitudeSerie[serieIndex];

            final MapMarker item = new MapMarker(
                  tourMarker.getLabel(),
                  tourMarker.getDescription(),
                  new GeoPoint(latitude, longitude));

            allMarkerItems.add(item);
         }
      }

      return allMarkerItems;
   }
   
}
