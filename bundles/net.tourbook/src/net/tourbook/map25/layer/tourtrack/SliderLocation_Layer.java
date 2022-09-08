/*
 * Original: org.oscim.layers.LocationLayer
 */
package net.tourbook.map25.layer.tourtrack;

import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.renderer.LocationRenderer;

import org.oscim.backend.CanvasAdapter;
import org.oscim.core.GeoPoint;
import org.oscim.core.MercatorProjection;
import org.oscim.layers.Layer;
import org.oscim.map.Map;

public class SliderLocation_Layer extends Layer {

   private final LocationRenderer _locationRenderer;

	public SliderLocation_Layer(final Map map) {

		this(map, CanvasAdapter.getScale());
	}

	public SliderLocation_Layer(final Map map, final float scale) {

		super(map);

      mRenderer = _locationRenderer = new LocationRenderer(mMap, this, scale);
	}

	public void onModifyConfig() {

		final Map25TrackConfig activeTourTrackConfig = Map25ConfigManager.getActiveTourTrackConfig();

		setEnabled(activeTourTrackConfig.isShowSliderLocation);

      _locationRenderer.updateConfig();
	}

	@Override
   public void setEnabled(final boolean isEnabled) {

      if (isEnabled == isEnabled()) {
			return;
		}

      super.setEnabled(isEnabled);

      if (!isEnabled) {
         _locationRenderer.animate(false);
		}
	}

	public void setPosition(final GeoPoint leftGeoPoint, final GeoPoint rightGeoPoint) {

		final double radius = 10 / MercatorProjection.groundResolutionWithScale(leftGeoPoint.getLatitude(), 1);

      _locationRenderer.setLocation(

				MercatorProjection.longitudeToX(leftGeoPoint.getLongitude()),
				MercatorProjection.latitudeToY(leftGeoPoint.getLatitude()),

				MercatorProjection.longitudeToX(rightGeoPoint.getLongitude()),
				MercatorProjection.latitudeToY(rightGeoPoint.getLatitude()),

				radius);

	}
}
