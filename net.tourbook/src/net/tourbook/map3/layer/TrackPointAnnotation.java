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

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.util.OGLStackHandler;

import java.awt.Color;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import net.tourbook.map3.layer.tourtrack.TourTrackConfig;
import net.tourbook.map3.layer.tourtrack.TourTrackConfigManager;
import net.tourbook.map3.view.Map3View;
import net.tourbook.opengl.GLTools;

public class TrackPointAnnotation extends GlobeAnnotation {

	public float	trackAltitude;
	public LatLon	latLon;

	public TrackPointAnnotation(final String text, final Position position) {
		super(text, position);
	}

	@Override
	protected void doRenderNow(final DrawContext dc) {

		GLTools.dumpModelViewPerspective(dc);

		if (dc.isPickingMode() && this.getPickSupport() == null) {
			return;
		}

		final Vec4 annotationPoint = this.getAnnotationDrawPoint(dc);
		if (annotationPoint == null) {
			return;
		}

		if (dc.getView().getFrustumInModelCoordinates().getNear().distanceTo(annotationPoint) < 0) {
			return;
		}

		final Vec4 screenAnnotationPoint = dc.getView().project(annotationPoint);
		if (screenAnnotationPoint == null) {
			return;
		}

		final java.awt.Dimension size = this.getPreferredSize(dc);
		final Position pos = dc.getGlobe().computePositionFromPoint(annotationPoint);

		// Scale and opacity depending on distance from eye
		final double[] scaleAndOpacity = computeDistanceScaleAndOpacity(dc, annotationPoint, size);

		this.setDepthFunc(dc, screenAnnotationPoint);

		this.drawTopLevelAnnotation(
				dc,
				(int) screenAnnotationPoint.x,
				(int) screenAnnotationPoint.y,
				size.width,
				size.height,
				scaleAndOpacity[0],
				scaleAndOpacity[1],
				pos);

		drawLine(dc, screenAnnotationPoint);
	}

	private void drawLine(final DrawContext dc, final Vec4 screenPlacePoint) {

		// Compute a terrain point if needed.
		Vec4 terrainPoint = null;
		if (this.altitudeMode != WorldWind.CLAMP_TO_GROUND) {
			terrainPoint = dc.computeTerrainPoint(position.getLatitude(), position.getLongitude(), 0);
		}
		if (terrainPoint == null) {
			return;
		}

		final Vec4 screenTerrainPoint = dc.getView().project(terrainPoint);
		if (screenTerrainPoint == null) {
			return;
		}

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tscreenPlacePoint: " + screenPlacePoint)
//				+ ("\tscreenTerrainPoint: " + screenTerrainPoint));
//		// TODO remove SYSTEM.OUT.PRINTLN

		final OGLStackHandler stackHandler = new OGLStackHandler();
		this.beginDraw(dc, stackHandler);

		try {
//			this.applyScreenTransform(dc, x, y, width, height, scale);
//			this.draw(dc, width, height, opacity, pickPosition);
		} finally {
			this.endDraw(dc, stackHandler);
		}
	}

	/**
	 * Draws the placemark's line.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @param placePoint
	 * @param terrainPoint
	 * @param pickCandidates
	 *            the pick support object to use when adding this as a pick candidate.
	 */
	private void drawLine_OLD(final DrawContext dc, final Vec4 placePoint, final Vec4 terrainPoint) {

		final GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

//		// Do not depth buffer the label. (Placemarks beyond the horizon are culled above.)
//		gl.glDisable(GL.GL_DEPTH_TEST);
//		gl.glDepthMask(false);

//		gl.glDepthFunc(GL.GL_ALWAYS);
//		gl.glDisable(GL.GL_DEPTH_TEST);

		if ((!dc.isDeepPickingEnabled())) {
			gl.glEnable(GL.GL_DEPTH_TEST);
		}
		gl.glDepthFunc(GL.GL_LEQUAL);
//		gl.glDepthFunc(GL.GL_ALWAYS);
		gl.glDepthMask(true);

		try {

//			System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//					+ ("\tTrack line:\t" + placePoint + "\t" + terrainPoint.subtract3(placePoint)));
//			// TODO remove SYSTEM.OUT.PRINTLN

			dc.getView().pushReferenceCenter(dc, placePoint); // draw relative to the place point

			// Pull the arrow triangles forward just a bit to ensure they show over the terrain.
			dc.pushProjectionOffest(0.95);

			this.setLineWidth(dc);
			this.setLineColor(dc);

			gl.glBegin(GL2.GL_LINE_STRIP);
			{
				gl.glVertex3d(Vec4.ZERO.x, Vec4.ZERO.y, Vec4.ZERO.z);
				gl.glVertex3d(//
						terrainPoint.x - placePoint.x,
						terrainPoint.y - placePoint.y,
						terrainPoint.z - placePoint.z);
			}
			gl.glEnd();

		} finally {

			dc.popProjectionOffest();

			dc.getView().popReferenceCenter(dc);
		}
	}

	/**
	 * Sets the color of the placemark's line during rendering.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	protected void setLineColor(final DrawContext dc) {

		final GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

		if (!dc.isPickingMode()) {

//			Color color = this.getActiveAttributes().getLineColor();
			final Color color = Color.GREEN;

//			if (color == null) {
//				color = PointPlacemarkAttributes.DEFAULT_LINE_COLOR;
//			}

			gl.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(),
//					(byte) color.getAlpha()
					(byte) 0x80);
//		} else {
//
//			final Color pickColor = dc.getUniquePickColor();
//			pickCandidates.addPickableObject(pickColor.getRGB(), this, this.getPosition());
//			gl.glColor3ub((byte) pickColor.getRed(), (byte) pickColor.getGreen(), (byte) pickColor.getBlue());
		}
	}

	/**
	 * Sets the width of the placemark's line during rendering.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	protected void setLineWidth(final DrawContext dc) {

//		final Double lineWidth = this.getActiveAttributes().getLineWidth();
		final Double lineWidth = 5.0;

		if (lineWidth != null) {
			final GL gl = dc.getGL();

			if (dc.isPickingMode()) {
//				gl.glLineWidth(lineWidth.floatValue() + this.getLinePickWidth());
				gl.glLineWidth(lineWidth.floatValue() + 5);
			} else {
				gl.glLineWidth(lineWidth.floatValue());
			}

			if (!dc.isPickingMode()) {
//				gl.glHint(GL.GL_LINE_SMOOTH_HINT, this.getActiveAttributes().getAntiAliasHint());
				gl.glHint(GL.GL_LINE_SMOOTH_HINT, Polyline.ANTIALIAS_FASTEST);
				gl.glEnable(GL.GL_LINE_SMOOTH);
			}
		}
	}

	public void setSliderPosition(final DrawContext dc) {

		if (latLon == null) {
			// is not fully initialized, this can happen
			return;
		}

		final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

		float sliderYPosition = 0;

		switch (config.altitudeMode) {
		case WorldWind.ABSOLUTE:

			sliderYPosition = trackAltitude;

			if (config.isAltitudeOffset) {

				// append offset
				sliderYPosition += Map3View.getAltitudeOffset(dc.getView().getEyePosition());
			}

			break;

		case WorldWind.RELATIVE_TO_GROUND:

			sliderYPosition = trackAltitude;
			break;

		default:

			// WorldWind.CLAMP_TO_GROUND -> y position = 0

			break;
		}

		setPosition(new Position(latLon, sliderYPosition));
	}
}
