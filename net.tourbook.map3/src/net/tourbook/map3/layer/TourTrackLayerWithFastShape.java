/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.layer;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.map3.view.Messages;
import au.gov.ga.worldwind.common.render.fastshape.FastShape;

/**
 */
public class TourTrackLayerWithFastShape extends RenderableLayer {

	public static final String			MAP3_LAYER_ID	= "TourTrackLayer"; //$NON-NLS-1$

	private final TourPositionColors	_tourColors;

	private ArrayList<TourData>			_allTours;

	private class PositionWithTourAlti extends Position {

		float	tourAlti;

		public PositionWithTourAlti(final LatLon arg0, final double arg1) {
			super(arg0, arg1);
		}

		public PositionWithTourAlti(final LatLon fromDegrees, final int mapAlti, final float tourAlti) {

			super(fromDegrees, mapAlti);

			this.tourAlti = tourAlti;
		}

	}

	public TourTrackLayerWithFastShape() {

		_tourColors = new TourPositionColors();

//		addPropertyChangeListener(this);
	}

	/**
	 * Populate the color buffer with colours based on earthquake depth.
	 * <p/>
	 * Blue (shallow) -> Red (deep)
	 */
	private void generateTourColors(final FloatBuffer colorBuffer, final List<Position> positions) {

		double minElevation = Double.MAX_VALUE;
		double maxElevation = -Double.MAX_VALUE;

		for (final Position position : positions) {
			minElevation = Math.min(minElevation, position.elevation);
			maxElevation = Math.max(maxElevation, position.elevation);
		}

		for (final Position position : positions) {

//			final double percent = (position.elevation - minElevation) / (maxElevation - minElevation);
//			final Color color = new HSLColor((float) (240d * percent), 100f, 50f).getRGB();

			float tourAlti = 0;

			if (position instanceof PositionWithTourAlti) {
				final PositionWithTourAlti tourColorPosition = (PositionWithTourAlti) position;
				tourAlti = tourColorPosition.tourAlti;
			}

			final Color color = _tourColors.getColor(tourAlti);

			colorBuffer.put(color.getRed() / 255f).put(color.getGreen() / 255f).put(color.getBlue() / 255f);
		}
	}

	@Override
	public String getName() {
		return Messages.TourTrack_Layer_Name;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {

		System.out.println(UI.timeStampNano() + " \t" + evt);
		// TODO remove SYSTEM.OUT
	}

	private void showTours() {
		//		final long start = System.currentTimeMillis();

		removeAllRenderables();

		_tourColors.updateColors(_allTours);

		for (final TourData oneTour : _allTours) {

			final double[] latSerie = oneTour.latitudeSerie;
			final double[] allLon = oneTour.longitudeSerie;
			final float[] allAlti = oneTour.altitudeSerie;

			if (latSerie == null) {
				continue;
			}

			/*
			 * create positions for all slices
			 */
			final ArrayList<Position> positions = new ArrayList<Position>();

			for (int serieIndex = 0; serieIndex < latSerie.length; serieIndex++) {

				final double lat = latSerie[serieIndex];
				final double lon = allLon[serieIndex];

				float tourAltitude = 0;

				if (allAlti != null) {
					tourAltitude = allAlti[serieIndex] + 1;
				}

				int terrainAltitude = 10;
				terrainAltitude = (int) tourAltitude + 10;

				positions.add(new PositionWithTourAlti(LatLon.fromDegrees(lat, lon), terrainAltitude, tourAltitude));
			}

			/*
			 * create one path for each tour
			 */

			final FloatBuffer colorBuffer = FloatBuffer.allocate(positions.size() * 3);
			generateTourColors(colorBuffer, positions);

			final FastShape shape = new FastShape(positions, GL.GL_POINTS);
			//			final FastShape shape = new FastShape(positions, GL.GL_LINES);
			//			final FastShape shape = new FastShape(positions, GL.GL_POLYGON);
			//			final FastShape shape = new FastShape(positions, GL.GL_TRIANGLE_STRIP);
			//			final FastShape shape = new FastShape(positions, GL.GL_TRIANGLES);

			shape.setColorBuffer(colorBuffer.array());
			shape.setColorBufferElementSize(3);

			shape.setLineWidth(8.0);
			shape.setPointSize(10.0);

//			shape.setFollowTerrain(true);
			shape.setFollowTerrainUpdateFrequency(1000);

			//			shape.setWireframe(true);
			//			shape.setForceSortedPrimitives(true);

			addRenderable(shape);
		}

		//		System.out.println(UI.timeStampNano() + " showTour\t" + (System.currentTimeMillis() - start) + " ms");
		//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	public void showTours(final ArrayList<TourData> allTours) {

		_allTours = allTours;

		showTours();
	}

	public void updateColors() {

		showTours();
	}
}
