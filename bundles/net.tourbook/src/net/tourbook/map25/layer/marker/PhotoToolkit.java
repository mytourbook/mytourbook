package net.tourbook.map25.layer.marker;

import java.util.ArrayList;
import java.util.List;

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
   
}
