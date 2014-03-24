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
package net.tourbook.opengl;

import gov.nasa.worldwind.render.DrawContext;

import java.nio.FloatBuffer;
import java.text.DecimalFormat;

import javax.media.opengl.GL2;

import net.tourbook.common.UI;

public class GLTools {

	private static DecimalFormat	_logFormat	= new DecimalFormat();

	static {
		_logFormat.applyPattern("#.######");
		_logFormat.setPositivePrefix(" ");
	}

// Dump depth settings
//
//			final ByteBuffer writeMask = ByteBuffer.allocate(2);
//			gl.glGetBooleanv(GL.GL_DEPTH_WRITEMASK, writeMask);
//
//			final FloatBuffer depthRange = FloatBuffer.allocate(10);
//			gl.glGetFloatv(GL.GL_DEPTH_RANGE, depthRange);
//
//			final IntBuffer depthFunc = IntBuffer.allocate(1);
//			gl.glGetIntegerv(GL.GL_DEPTH_FUNC, depthFunc);
//
//			System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//					+ ("\tGL_DEPTH_TEST: " + gl.glIsEnabled(GL.GL_DEPTH_TEST))
//					+ ("\tRange: " + depthRange.get(0) + "\t" + depthRange.get(1))
//					+ ("\tGL_DEPTH_WRITEMASK: " + writeMask.get(0))
//					+ ("\tGL_DEPTH_FUNC: " + depthFunc.get(0))
////
//					);
//			// TODO remove SYSTEM.OUT.PRINTLN

	private static String dumpMatrix(final FloatBuffer buffer) {

		final String dump = String.format("\n"
		//
				+ "%-12s %-12s %-12s %-12s\n"
				+ "%-12s %-12s %-12s %-12s\n"
				+ "%-12s %-12s %-12s %-12s\n"
				+ "%-12s %-12s %-12s %-12s\n",
		//
				_logFormat.format(buffer.get(0)),
				_logFormat.format(buffer.get(4)),
				_logFormat.format(buffer.get(8)),
				_logFormat.format(buffer.get(12)),
				//
				_logFormat.format(buffer.get(1)),
				_logFormat.format(buffer.get(5)),
				_logFormat.format(buffer.get(6)),
				_logFormat.format(buffer.get(13)),
				//
				_logFormat.format(buffer.get(2)),
				_logFormat.format(buffer.get(6)),
				_logFormat.format(buffer.get(10)),
				_logFormat.format(buffer.get(14)),
				//
				_logFormat.format(buffer.get(3)),
				_logFormat.format(buffer.get(7)),
				_logFormat.format(buffer.get(11)),
				_logFormat.format(buffer.get(15))
		//
				);

		return dump;
	}

	private static String dumpMatrix_MVP(final FloatBuffer bufferMV, final FloatBuffer bufferP) {

		final String dump = String.format("\n%-52s\t\t\t%-52s\n"
		//
				+ "%-12s %-12s %-12s %-12s\t\t\t%-12s %-12s %-12s %-12s\n"
				+ "%-12s %-12s %-12s %-12s\t\t\t%-12s %-12s %-12s %-12s\n"
				+ "%-12s %-12s %-12s %-12s\t\t\t%-12s %-12s %-12s %-12s\n"
				+ "%-12s %-12s %-12s %-12s\t\t\t%-12s %-12s %-12s %-12s\n",
		//
				"GL_MODELVIEW_MATRIX",
				"GL_PROJECTION_MATRIX",
				//
				_logFormat.format(bufferMV.get(0)),
				_logFormat.format(bufferMV.get(4)),
				_logFormat.format(bufferMV.get(8)),
				_logFormat.format(bufferMV.get(12)),
				//
				_logFormat.format(bufferP.get(0)),
				_logFormat.format(bufferP.get(4)),
				_logFormat.format(bufferP.get(8)),
				_logFormat.format(bufferP.get(12)),
				//
				_logFormat.format(bufferMV.get(1)),
				_logFormat.format(bufferMV.get(5)),
				_logFormat.format(bufferMV.get(6)),
				_logFormat.format(bufferMV.get(13)),
				//
				_logFormat.format(bufferP.get(1)),
				_logFormat.format(bufferP.get(5)),
				_logFormat.format(bufferP.get(6)),
				_logFormat.format(bufferP.get(13)),
				//
				_logFormat.format(bufferMV.get(2)),
				_logFormat.format(bufferMV.get(6)),
				_logFormat.format(bufferMV.get(10)),
				_logFormat.format(bufferMV.get(14)),
				//
				_logFormat.format(bufferP.get(2)),
				_logFormat.format(bufferP.get(6)),
				_logFormat.format(bufferP.get(10)),
				_logFormat.format(bufferP.get(14)),
				//
				_logFormat.format(bufferMV.get(3)),
				_logFormat.format(bufferMV.get(7)),
				_logFormat.format(bufferMV.get(11)),
				_logFormat.format(bufferMV.get(15)),
				//
				_logFormat.format(bufferP.get(3)),
				_logFormat.format(bufferP.get(7)),
				_logFormat.format(bufferP.get(11)),
				_logFormat.format(bufferP.get(15))
		//
				);

		return dump;
	}

	/**
	 * Dumps the current modelview and perspective.
	 */
	public static void dumpModelViewPerspective(final DrawContext dc) {

		final GL2 gl = dc.getGL().getGL2();

		final FloatBuffer modelview = FloatBuffer.allocate(16);
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, modelview);

		final FloatBuffer perspective = FloatBuffer.allocate(16);
		gl.glGetFloatv(GL2.GL_PROJECTION_MATRIX, perspective);

		System.out.println((UI.timeStampNano() + " [" + GLTools.class.getSimpleName() + "] ")
				+ dumpMatrix_MVP(modelview, perspective));

	}
}
