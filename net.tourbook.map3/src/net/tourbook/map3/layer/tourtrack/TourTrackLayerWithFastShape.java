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
package net.tourbook.map3.layer.tourtrack;

import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;

/**
 */
public class TourTrackLayerWithFastShape extends AbstractLayer {

//	public static final String			MAP3_LAYER_ID	= "TourTrackLayer";			//$NON-NLS-1$
//
//	private final TourPositionColors	_tourColors;
//
//	private ArrayList<TourData>			_allTours;
//	private ArrayList<FastShape>		_allShapes		= new ArrayList<FastShape>();
//
//	private class PositionWithTourAlti extends Position {
//
//		float	tourAlti;
//
//		public PositionWithTourAlti(final LatLon arg0, final double arg1) {
//			super(arg0, arg1);
//		}
//
//		public PositionWithTourAlti(final LatLon fromDegrees, final int mapAlti, final float tourAlti) {
//
//			super(fromDegrees, mapAlti);
//
//			this.tourAlti = tourAlti;
//		}
//
//	}
//
//	public TourTrackLayerWithFastShape() {
//
//		_tourColors = new TourPositionColors();
//
////		addPropertyChangeListener(this);
//	}
//
//	@Override
//	protected void doPick(final DrawContext dc, final Point point) {
//		doRender(dc);
//	}

	@Override
	protected void doRender(final DrawContext dc) {

//		if (!isEnabled() || _allShapes.size() == 0) {
//			return;
//		}
//
//		// prevent ConcurrentModificationException
//		final FastShape[] allShapes = _allShapes.toArray(new FastShape[_allShapes.size()]);
//
//		//draw each shape
//		for (final FastShape fastShape : allShapes) {
//
//			if (fastShape != null) {
//
//				if (dc.isPickingMode()) {
//					fastShape.pick(dc, dc.getPickPoint());
//				} else {
//					fastShape.render(dc);
//				}
//			}
//		}
	}

//	/**
//	 * Populate the color buffer with colours based on earthquake depth.
//	 * <p/>
//	 * Blue (shallow) -> Red (deep)
//	 */
//	private void generateTourColors(final FloatBuffer colorBuffer, final List<Position> positions) {
//
//		double minElevation = Double.MAX_VALUE;
//		double maxElevation = -Double.MAX_VALUE;
//
//		for (final Position position : positions) {
//			minElevation = Math.min(minElevation, position.elevation);
//			maxElevation = Math.max(maxElevation, position.elevation);
//		}
//
//		for (final Position position : positions) {
//
//			float tourAlti = 0;
//
//			if (position instanceof PositionWithTourAlti) {
//				final PositionWithTourAlti tourColorPosition = (PositionWithTourAlti) position;
//				tourAlti = tourColorPosition.tourAlti;
//			}
//
//			final Color color = _tourColors.getColor(tourAlti);
//
//			colorBuffer.put(color.getRed() / 255f).put(color.getGreen() / 255f).put(color.getBlue() / 255f);
//		}
//	}
//
//	@Override
//	public String getName() {
//		return Messages.TourTrack_Layer_Name;
//	}
//
//	@Override
//	public void propertyChange(final PropertyChangeEvent evt) {
//
//		System.out.println(UI.timeStampNano() + " \t" + evt);
//		// TODO remove SYSTEM.OUT
//	}
//
//	private void showTours() {
////
////		//		final long start = System.currentTimeMillis();
////
////		removeAllRenderables();
////		_allShapes.clear();
////
////		_tourColors.updateColors(_allTours);
////
////		for (final TourData oneTour : _allTours) {
////
////			final double[] latSerie = oneTour.latitudeSerie;
////			final double[] allLon = oneTour.longitudeSerie;
////			final float[] allAlti = oneTour.altitudeSerie;
////
////			if (latSerie == null) {
////				continue;
////			}
////
////			/*
////			 * create positions for all slices
////			 */
////			final ArrayList<Position> positions = new ArrayList<Position>();
////
////			for (int serieIndex = 0; serieIndex < latSerie.length; serieIndex++) {
////
////				final double lat = latSerie[serieIndex];
////				final double lon = allLon[serieIndex];
////
////				float tourAltitude = 0;
////
////				if (allAlti != null) {
////					tourAltitude = allAlti[serieIndex] + 1;
////				}
////
////				final int terrainAltitude = 2;
//////				terrainAltitude = (int) tourAltitude + 10;
////
////				positions.add(new PositionWithTourAlti(LatLon.fromDegrees(lat, lon), terrainAltitude, tourAltitude));
////			}
////
////			/*
////			 * create one path for each tour
////			 */
////
////			final FloatBuffer colorBuffer = FloatBuffer.allocate(positions.size() * 3);
////			generateTourColors(colorBuffer, positions);
////
////			final FastShape shape = new FastShape(positions, GL.GL_POINTS);
////			//			final FastShape shape = new FastShape(positions, GL.GL_LINES);
////			//			final FastShape shape = new FastShape(positions, GL.GL_POLYGON);
////			//			final FastShape shape = new FastShape(positions, GL.GL_TRIANGLE_STRIP);
////			//			final FastShape shape = new FastShape(positions, GL.GL_TRIANGLES);
////
////			shape.setColorBuffer(colorBuffer.array());
////			shape.setColorBufferElementSize(3);
////
////			shape.setLineWidth(8.0);
////			shape.setPointSize(10.0);
////
////			shape.setFollowTerrain(true);
////			shape.setFollowTerrainUpdateFrequency(1000);
////
////			// activate picking ???
////			shape.setUseOrderedRendering(true);
////
////			_allShapes.add(shape);
////		}
////
////		addRenderables(_allShapes);
////
////		firePropertyChange(AVKey.LAYER, null, this);
////
////		//		System.out.println(UI.timeStampNano() + " showTour\t" + (System.currentTimeMillis() - start) + " ms");
////		//		// TODO remove SYSTEM.OUT.PRINTLN
//	}
//
//	public void showTours(final ArrayList<TourData> allTours) {
//
//		_allTours = allTours;
//
//		showTours();
//	}
//
//	public void updateColors() {
//
//		showTours();
//	}
}
