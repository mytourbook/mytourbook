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
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.util.OGLStackHandler;
import gov.nasa.worldwind.util.OGLTextRenderer;
import gov.nasa.worldwind.util.OGLUtil;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.util.concurrent.atomic.AtomicLong;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import net.tourbook.common.UI;
import net.tourbook.map3.view.Map3View;

import com.jogamp.opengl.util.awt.TextRenderer;

public class MarkerPlacemark extends PointPlacemark {

	String						markerText;
	double						absoluteAltitude;
	private static AtomicLong	_counterDraw	= new AtomicLong();
	private static AtomicLong	_counterMake	= new AtomicLong();

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
	 * Draw this placemark as an ordered renderable. If in picking mode, add it to the picked object
	 * list of specified {@link PickSupport}. The <code>PickSupport</code> may not be the one
	 * associated with this instance. During batch picking the <code>PickSupport</code> of the
	 * instance initiating the batch picking is used so that all shapes rendered in batch are added
	 * to the same pick list.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @param pickCandidates
	 *            a pick support holding the picked object list to add this shape to.
	 */
	@Override
	protected void doDrawOrderedRenderable(final DrawContext dc, final PickSupport pickCandidates) {

		if (this.isDrawLine(dc)) {
			this.drawLine(dc, pickCandidates);
		}

		if (this.activeTexture == null) {
			if (this.isDrawPoint(dc)) {
				this.drawPoint(dc, pickCandidates);
			}
			return;
		}

		final GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

		final OGLStackHandler osh = new OGLStackHandler();
		try {
			if (dc.isPickingMode()) {
				// Set up to replace the non-transparent texture colors with the single pick color.
				gl.glEnable(GL.GL_TEXTURE_2D);
				gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_COMBINE);
				gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_SRC0_RGB, GL2.GL_PREVIOUS);
				gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_RGB, GL2.GL_REPLACE);

				final Color pickColor = dc.getUniquePickColor();
				pickCandidates.addPickableObject(this.createPickedObject(dc, pickColor));
				gl.glColor3ub((byte) pickColor.getRed(), (byte) pickColor.getGreen(), (byte) pickColor.getBlue());
			} else {
				gl.glEnable(GL.GL_TEXTURE_2D);
				Color color = this.getActiveAttributes().getImageColor();
				if (color == null) {
					color = Color.WHITE;
				}
				gl.glColor4ub(
						(byte) color.getRed(),
						(byte) color.getGreen(),
						(byte) color.getBlue(),
						(byte) color.getAlpha());
			}

			// The image is drawn using a parallel projection.
			osh.pushProjectionIdentity(gl);
			gl.glOrtho(0d, dc.getView().getViewport().width, 0d, dc.getView().getViewport().height, -1d, 1d);

			// Apply the depth buffer but don't change it (for screen-space shapes).
			if ((!dc.isDeepPickingEnabled())) {
				gl.glEnable(GL.GL_DEPTH_TEST);
			}
//			gl.glDepthMask(false);
			gl.glDepthMask(true);

			// Suppress any fully transparent image pixels.
			gl.glEnable(GL2.GL_ALPHA_TEST);
			gl.glAlphaFunc(GL2.GL_GREATER, 0.001f);

			// Adjust depth of image to bring it slightly forward
			double depth = screenPoint.z - (8d * 0.00048875809d);
			depth = depth < 0d ? 0d : (depth > 1d ? 1d : depth);
			gl.glDepthFunc(GL.GL_LESS);
			gl.glDepthRange(depth, depth);

			// The image is drawn using a translated and scaled unit quad.
			// Translate to screen point and adjust to align hot spot.
			osh.pushModelviewIdentity(gl);
			gl.glTranslated(screenPoint.x + this.dx, screenPoint.y + this.dy, 0);

			// Compute the scale
			double xscale;
			final Double scale = this.getActiveAttributes().getScale();
			if (scale != null) {
				xscale = scale * this.activeTexture.getWidth(dc);
			} else {
				xscale = this.activeTexture.getWidth(dc);
			}

			double yscale;
			if (scale != null) {
				yscale = scale * this.activeTexture.getHeight(dc);
			} else {
				yscale = this.activeTexture.getHeight(dc);
			}

			Double heading = getActiveAttributes().getHeading();
			final Double pitch = getActiveAttributes().getPitch();

			// Adjust heading to be relative to globe or screen
			if (heading != null) {
				if (AVKey.RELATIVE_TO_GLOBE.equals(this.getActiveAttributes().getHeadingReference())) {
					heading = dc.getView().getHeading().degrees - heading;
				} else {
					heading = -heading;
				}
			}

			// Apply the heading and pitch if specified.
			if (heading != null || pitch != null) {
				gl.glTranslated(xscale / 2, yscale / 2, 0);
				if (pitch != null) {
					gl.glRotated(pitch, 1, 0, 0);
				}
				if (heading != null) {
					gl.glRotated(heading, 0, 0, 1);
				}
				gl.glTranslated(-xscale / 2, -yscale / 2, 0);
			}

			// Scale the unit quad
			gl.glScaled(xscale, yscale, 1);

			if (this.activeTexture.bind(dc)) {
				dc.drawUnitQuad(activeTexture.getTexCoords());
			}

			gl.glDepthRange(0, 1); // reset depth range to the OGL default
//
//            gl.glDisable(GL.GL_TEXTURE_2D);
//            dc.drawUnitQuadOutline(); // for debugging label placement

			if (this.mustDrawLabel() && !dc.isPickingMode()) {
				this.drawLabel(dc);
			}
		} finally {
			if (dc.isPickingMode()) {
				gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, OGLUtil.DEFAULT_TEX_ENV_MODE);
				gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_SRC0_RGB, OGLUtil.DEFAULT_SRC0_RGB);
				gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_RGB, OGLUtil.DEFAULT_COMBINE_RGB);
			}

			gl.glDisable(GL.GL_TEXTURE_2D);
			osh.pop(gl);
		}
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
//
//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tdrawLabel()")
//				+ ("\tframe: " + dc.getFrameTimeStamp())
//				+ ("\tisPickingMode: " + dc.isPickingMode())
//				+ ("\tisOrderedRenderingMode: " + dc.isOrderedRenderingMode())
//		//
//				);
//		// TODO remove SYSTEM.OUT.PRINTLN

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
//		gl.glDisable(GL.GL_DEPTH_TEST);
//		gl.glDepthMask(false);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LEQUAL);
		gl.glDepthMask(true);

		final TextRenderer textRenderer = OGLTextRenderer.getOrCreateTextRenderer(dc.getTextRendererCache(), font);
		try {

			textRenderer.begin3DRendering();

			// draw background shade
			textRenderer.setColor(backgroundColor);
			textRenderer.draw3D(this.labelText, x + 1, y - 1, 0, 1);

			// draw foreground color
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

			// Pull the arrow triangles forward just a bit to ensure they show over the terrain.
//			dc.pushProjectionOffest(0.99);
			dc.pushProjectionOffest(1.02);

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

			dc.popProjectionOffest();

			dc.getView().popReferenceCenter(dc);
		}
	}

	@Override
	protected void drawOrderedRenderable(final DrawContext dc) {

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tdraw... " + _counterDraw.incrementAndGet())
//				+ ("\tframe: " + dc.getFrameTimeStamp())
//				+ ("\tisPickingMode: " + dc.isPickingMode())
//				+ ("\tisOrderedRenderingMode: " + dc.isOrderedRenderingMode())
//		//
//				);
//		// TODO remove SYSTEM.OUT.PRINTLN

		super.drawOrderedRenderable(dc);
	}

	@Override
	protected void makeOrderedRenderable(final DrawContext dc) {

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tmake... " + _counterMake++)
//				+ ("\tframe: " + dc.getFrameTimeStamp())
//				+ ("\tisPickingMode: " + dc.isPickingMode())
//				+ ("\tisOrderedRenderingMode: " + dc.isOrderedRenderingMode())
//		//
//				);
//		// TODO remove SYSTEM.OUT.PRINTLN

		super.makeOrderedRenderable(dc);
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
