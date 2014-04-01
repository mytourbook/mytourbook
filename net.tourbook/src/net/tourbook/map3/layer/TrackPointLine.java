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
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;

import java.awt.Color;
import java.awt.Point;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

public class TrackPointLine implements OrderedRenderable {

	private boolean		_isVisible;

	private Position	_sliderPosition;
	private Color		_lineColor;

	private double		_eyeDistance;

	private Vec4		_placePoint;
	private Vec4		_terrainPoint;

	@Override
	public double getDistanceFromEye() {
		return _eyeDistance;
	}

	void makeOrderedRenderable(final DrawContext dc, final Position sliderPosition, final Color lineColor) {

		if (_isVisible == false) {
			return;
		}

		_sliderPosition = sliderPosition;
		_lineColor = lineColor;

		// Convert the cube's geographic position to a position in Cartesian coordinates.
		_placePoint = dc.getGlobe().computePointFromPosition(_sliderPosition);

		// Compute the distance from the eye to the cube's position.
		_eyeDistance = dc.getView().getEyePoint().distanceTo3(_placePoint);

		_terrainPoint = dc.computeTerrainPoint(_sliderPosition.getLatitude(), _sliderPosition.getLongitude(), 0);

		dc.addOrderedRenderable(this);
	}

	@Override
	public void pick(final DrawContext dc, final Point pickPoint) {

	}

	@Override
	public void render(final DrawContext dc) {

		final GL2 gl = dc.getGL().getGL2();

		gl.glEnable(GL.GL_DEPTH_TEST);
//		gl.glDepthFunc(GL.GL_LEQUAL);
		gl.glDepthMask(true);
//		gl.glDepthMask(false);
//		gl.glDepthRange(0.0, 1.0);

		try {

			dc.getView().pushReferenceCenter(dc, _placePoint); // draw relative to the place point

			// Pull the arrow triangles forward just a bit to ensure they show over the terrain.
			dc.pushProjectionOffest(0.995);

//			final Color color = this.getAttributes().getTextColor();
//			final Color color = Color.RED;
			final Color color = _lineColor;

			gl.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) 0xff);

			gl.glLineWidth(1.0f);
//			gl.glEnable(GL.GL_LINE_SMOOTH);
//			gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
//			gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_DONT_CARE);

			gl.glBegin(GL2.GL_LINE_STRIP);
			{
				gl.glVertex3d(Vec4.ZERO.x, Vec4.ZERO.y, Vec4.ZERO.z);
				gl.glVertex3d(//
						_terrainPoint.x - _placePoint.x,
						_terrainPoint.y - _placePoint.y,
						_terrainPoint.z - _placePoint.z);
			}
			gl.glEnd();

		} finally {

			dc.popProjectionOffest();

			dc.getView().popReferenceCenter(dc);
		}
	}

	void setVisible(final boolean isVisible) {

		_isVisible = isVisible;
	}

}
