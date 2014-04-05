/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.util.OGLStackHandler;
import gov.nasa.worldwind.util.OGLUtil;

import java.awt.Color;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import net.tourbook.map3.layer.tourtrack.TourTrackConfig;
import net.tourbook.map3.layer.tourtrack.TourTrackConfigManager;
import net.tourbook.map3.view.Map3View;

public class TrackPointAnnotation extends GlobeAnnotation {

	public float	trackAltitude;
	public LatLon	latLon;

	public TrackPointAnnotation(final String text, final Position position) {
		super(text, position);
	}

	@Override
	protected void doRenderNow(final DrawContext dc) {

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

//		drawLine(dc, annotationPoint);
	}

	private void drawLine(final DrawContext dc, final Vec4 annotationPoint) {

		if (dc.isPickingMode()) {
			return;
		}

		final GL2 gl = dc.getGL().getGL2();
		final OGLStackHandler ogsh = new OGLStackHandler();

		try {

			/*
			 * Current modelview is identity, load default modelview
			 */

			final double[] matrixArray = new double[16];

			final View view = dc.getView();

			ogsh.pushAttrib(gl, GL2.GL_VIEWPORT_BIT | GL2.GL_ENABLE_BIT | GL2.GL_TRANSFORM_BIT);

			// set default projection matrix
			final Matrix projectionMatrix = view.getProjectionMatrix();
			projectionMatrix.toArray(matrixArray, 0, false);

			ogsh.pushProjection(gl);
			gl.glLoadMatrixd(matrixArray, 0);

			// set default model-view matrix
			final Matrix modelviewMatrix = view.getModelviewMatrix();
			modelviewMatrix.toArray(matrixArray, 0, false);

			ogsh.pushModelview(gl);
			gl.glLoadMatrixd(matrixArray, 0);

			gl.glEnable(GL.GL_BLEND);
			OGLUtil.applyBlending(gl, false);

			drawLine_Line(dc, annotationPoint);

		} finally {

			ogsh.pop(gl);
		}
	}

	private void drawLine_Line(final DrawContext dc, final Vec4 annotationPoint) {

		// Compute a terrain point if needed.
		Vec4 terrainPoint = null;
		if (this.altitudeMode != WorldWind.CLAMP_TO_GROUND) {
			terrainPoint = dc.computeTerrainPoint(position.getLatitude(), position.getLongitude(), 0);
		}
		if (terrainPoint == null) {
			return;
		}

		final GL2 gl = dc.getGL().getGL2();

		if ((!dc.isDeepPickingEnabled())) {
			gl.glEnable(GL.GL_DEPTH_TEST);
		}
		gl.glDepthFunc(GL.GL_LEQUAL);
//		gl.glDepthFunc(GL.GL_GREATER); // draw the part that is behind an intersecting surface
		gl.glDepthMask(true);
//		gl.glDepthMask(false);
		gl.glDepthRange(0.0, 1.0);

		try {

			dc.getView().pushReferenceCenter(dc, annotationPoint); // draw relative to the place point

//
// !!! THIS CAUSES A stack overflow1283 because there are only 4 available stack entries !!!
//
//
//			// Pull the arrow triangles forward just a bit to ensure they show over the terrain.
//			dc.pushProjectionOffest(0.95);

			final Color color = this.getAttributes().getTextColor();

			gl.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) 0xff);

			gl.glLineWidth(1.5f);
			gl.glHint(GL.GL_LINE_SMOOTH_HINT, Polyline.ANTIALIAS_FASTEST);
			gl.glEnable(GL.GL_LINE_SMOOTH);

//			System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//					+ ("\t" + dc.getFrameTimeStamp()));
//			GLLogger.logDepth(dc, getClass().getSimpleName());
			// TODO remove SYSTEM.OUT.PRINTLN

			gl.glBegin(GL2.GL_LINE_STRIP);
			{
				gl.glVertex3d(Vec4.ZERO.x, Vec4.ZERO.y, Vec4.ZERO.z);
				gl.glVertex3d(//
						terrainPoint.x - annotationPoint.x,
						terrainPoint.y - annotationPoint.y,
						terrainPoint.z - annotationPoint.z);
			}
			gl.glEnd();

//			GLLogger.logDepth(dc, "trackpt");

		} finally {

//			dc.popProjectionOffest();

			dc.getView().popReferenceCenter(dc);
		}
	}

	/**
	 * @param dc
	 * @return Return slider position or <code>null</code> when it's not fully initialized.
	 */
	public Position setSliderPosition(final DrawContext dc) {

		if (latLon == null) {
			// is not fully initialized, this can happen
			return null;
		}

		final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

		float sliderElevation = 0;

		switch (config.altitudeMode) {
		case WorldWind.ABSOLUTE:

			sliderElevation = trackAltitude;

			if (config.isAltitudeOffset) {

				// append offset
				sliderElevation += Map3View.getAltitudeOffset(dc.getView().getEyePosition());
			}

			break;

		case WorldWind.RELATIVE_TO_GROUND:

			sliderElevation = trackAltitude;
			break;

		default:

			// WorldWind.CLAMP_TO_GROUND -> y position = 0

			break;
		}

		final Position sliderPosition = new Position(latLon, sliderElevation);

		setPosition(sliderPosition);

		return sliderPosition;
	}
}
