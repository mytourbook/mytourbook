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

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.util.OGLTextRenderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import net.tourbook.common.UI;
import net.tourbook.map3.view.Map3View;

import com.jogamp.opengl.util.awt.TextRenderer;

public class MarkerPlacemark extends PointPlacemark {

	String	markerText;
	double	absoluteAltitude;

	public MarkerPlacemark(final Position position) {

		super(position);

		this.absoluteAltitude = position.elevation;
	}

	private void checkGLError(final GL2 gl) {

		int error;

		while ((error = gl.glGetError()) != GL.GL_NO_ERROR) {

			System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
					+ ("\tGL error: " + error));
			// TODO remove SYSTEM.OUT.PRINTLN
		}
	}

	/**
	 * Recognize altitude offset when height is computed.
	 * <p>
	 * ------------
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	protected void computePlacemarkPoints(final DrawContext dc) {

		this.placePoint = null;
		this.terrainPoint = null;
		this.screenPoint = null;

		final Position pos = this.getPosition();
		if (pos == null) {
			return;
		}

		if (this.altitudeMode == WorldWind.CLAMP_TO_GROUND) {

			this.placePoint = dc.computeTerrainPoint(pos.getLatitude(), pos.getLongitude(), 0);

		} else if (this.altitudeMode == WorldWind.RELATIVE_TO_GROUND) {

			this.placePoint = dc.computeTerrainPoint(pos.getLatitude(), pos.getLongitude(), pos.getAltitude());

		} else {

			// ABSOLUTE

			final double height = pos.getElevation()
					* (this.isApplyVerticalExaggeration() ? dc.getVerticalExaggeration() : 1);

			// get altitude offset
			final double altitudeOffset = Map3View.getAltitudeOffset(dc.getView().getEyePosition()) * 0.4;

			this.placePoint = dc.getGlobe().computePointFromPosition(
					pos.getLatitude(),
					pos.getLongitude(),
					height + altitudeOffset);
		}

		if (this.placePoint == null) {
			return;
		}

		// Compute a terrain point if needed.
		if (this.isLineEnabled() && this.altitudeMode != WorldWind.CLAMP_TO_GROUND) {
			this.terrainPoint = dc.computeTerrainPoint(pos.getLatitude(), pos.getLongitude(), 0);
		}

		// Compute the placemark point's screen location.
		this.screenPoint = dc.getView().project(this.placePoint);
		this.eyeDistance = this.placePoint.distanceTo3(dc.getView().getEyePoint());
	}

	/**
	 * Use depth mask, otherwise the label is drawn behind the curtain.
	 * <p>
	 * ------------
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	protected void drawLabel(final DrawContext dc) {

		if (this.labelText == null) {
			return;
		}

		Color color = this.getActiveAttributes().getLabelColor();
		// Use the default color if the active attributes do not specify one.
		if (color == null) {
			color = Color.WHITE;
		}

		// If the label color's alpha component is 0 or less, then the label is completely transparent. Exit
		// immediately; the label does not need to be rendered.
		if (color.getAlpha() <= 0) {
			return;
		}

		// Apply the label color's alpha component to the background color. This causes both the label foreground and
		// background to blend evenly into the frame. If the alpha component is 255 we just use the pre-defined constant
		// for BLACK to avoid creating a new background color every frame.
		final Color backgroundColor = (color.getAlpha() < 255 ? new Color(0, 0, 0, color.getAlpha()) : Color.BLACK);

		Font font = this.getActiveAttributes().getLabelFont();
		if (font == null) {
			font = PointPlacemarkAttributes.DEFAULT_LABEL_FONT;
		}

		float x = (float) (this.screenPoint.x + this.dx);
		float y = (float) (this.screenPoint.y + this.dy);

		final Double imageScale = this.getActiveAttributes().getScale();
		Offset os = this.getActiveAttributes().getLabelOffset();
		if (os == null) {
			os = DEFAULT_LABEL_OFFSET_IF_UNSPECIFIED;
		}

		final double w = this.activeTexture != null ? this.activeTexture.getWidth(dc) : 1;
		final double h = this.activeTexture != null ? this.activeTexture.getHeight(dc) : 1;
		final Point.Double offset = os.computeOffset(w, h, imageScale, imageScale);
		x += offset.x;
		y += offset.y;

		final GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		final Double labelScale = this.getActiveAttributes().getLabelScale();
		if (labelScale != null) {
			gl.glTranslatef(x, y, 0); // Assumes matrix mode is MODELVIEW
			gl.glScaled(labelScale, labelScale, 1);
			gl.glTranslatef(-x, -y, 0);
		}

		// Do not depth buffer the label. (Placemarks beyond the horizon are culled above.)
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glDepthMask(false);
//		gl.glEnable(GL.GL_DEPTH_TEST);
//		gl.glDepthFunc(GL.GL_LEQUAL);
//		gl.glDepthMask(true);
//		gl.glDepthMask(false);

		final TextRenderer textRenderer = OGLTextRenderer.getOrCreateTextRenderer(dc.getTextRendererCache(), font);
		try {
			textRenderer.begin3DRendering();
			textRenderer.setColor(backgroundColor);
			textRenderer.draw3D(this.labelText, x + 1, y - 1, 0, 1);
			textRenderer.setColor(color);
			textRenderer.draw3D(this.labelText, x, y, 0, 1);
		} finally {
			textRenderer.end3DRendering();
		}
	}

	/**
	 * Draws the placemark's line.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @param pickCandidates
	 *            the pick support object to use when adding this as a pick candidate.
	 */
	@Override
	protected void drawLine(final DrawContext dc, final PickSupport pickCandidates) {
		final GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

		if ((!dc.isDeepPickingEnabled())) {
			gl.glEnable(GL.GL_DEPTH_TEST);
		}
		gl.glDepthFunc(GL.GL_LEQUAL);
		gl.glDepthMask(true);

		try {

			dc.getView().pushReferenceCenter(dc, this.placePoint); // draw relative to the place point

			this.setLineWidth(dc);
			this.setLineColor(dc, pickCandidates);

			gl.glBegin(GL2.GL_LINE_STRIP);
			{
				gl.glVertex3d(Vec4.ZERO.x, Vec4.ZERO.y, Vec4.ZERO.z);
				gl.glVertex3d(
						this.terrainPoint.x - this.placePoint.x,
						this.terrainPoint.y - this.placePoint.y,
						this.terrainPoint.z - this.placePoint.z);
			}
			gl.glEnd();

		} finally {

			dc.getView().popReferenceCenter(dc);
		}
	}

	private void setDepthFunc(final DrawContext dc, final Vec4 screenPoint) {

		final GL gl = dc.getGL();

		final Position eyePos = dc.getView().getEyePosition();
		if (eyePos == null) {
			gl.glDepthFunc(GL.GL_ALWAYS);
			return;
		}

		final double altitude = eyePos.getElevation();
		final double maxAltitude = dc.getGlobe().getMaxElevation() * dc.getVerticalExaggeration();

		if (altitude < maxAltitude) {

			double depth = screenPoint.z - (8d * 0.00048875809d);
			depth = depth < 0d ? 0d : (depth > 1d ? 1d : depth);

			gl.glDepthFunc(GL.GL_LESS);
			gl.glDepthRange(depth, depth);

		} else if (screenPoint.z >= 1d) {

			gl.glDepthFunc(GL.GL_EQUAL);
			gl.glDepthRange(1d, 1d);

		} else {

			gl.glDepthFunc(GL.GL_ALWAYS);
		}
	}

	public void setMarkerText(final String markerText) {

		this.markerText = markerText;
	}

}
