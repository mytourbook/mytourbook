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

import com.jogamp.opengl.util.awt.TextRenderer;

public class MTPointPlacemark extends PointPlacemark {

	public MTPointPlacemark(final Position position) {
		super(position);
	}

	/**
	 * Draws the placemark's label if a label is specified.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	@Override
	protected void drawLabel(final DrawContext dc) {

		if (this.labelText == null) {
			return;
		}

		Color color = this.getActiveAttributes().getLabelColor();
		// Use the default color if the active attributes do not specify one.
		if (color == null) {
//			color = PointPlacemarkAttributes.DEFAULT_LABEL_COLOR;
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
		if ((!dc.isDeepPickingEnabled())) {
			gl.glEnable(GL.GL_DEPTH_TEST);
		}
		gl.glDepthFunc(GL.GL_LEQUAL);
		gl.glDepthMask(true);

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
			gl.glVertex3d(Vec4.ZERO.x, Vec4.ZERO.y, Vec4.ZERO.z);
			gl.glVertex3d(
					this.terrainPoint.x - this.placePoint.x,
					this.terrainPoint.y - this.placePoint.y,
					this.terrainPoint.z - this.placePoint.z);
			gl.glEnd();
		} finally {
			dc.getView().popReferenceCenter(dc);
		}
	}
}
