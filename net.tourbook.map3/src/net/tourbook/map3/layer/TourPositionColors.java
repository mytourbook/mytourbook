package net.tourbook.map3.layer;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Path;

import java.awt.Color;

class TourPositionColors implements Path.PositionColors {

	private final ColorCacheAWT	_colorCache	= new ColorCacheAWT();

	public Color getColor(final Position position, final int ordinal) {

		// Color the positions based on their altitude.

		final double altitude = position.getAltitude();

		return altitude < 800 ? Color.GREEN : altitude < 1000 ? Color.BLUE : Color.RED;
	}
}
