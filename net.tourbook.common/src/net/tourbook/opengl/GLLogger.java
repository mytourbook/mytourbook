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

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.text.DecimalFormat;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import net.tourbook.common.UI;

public class GLLogger {

	private static DecimalFormat	_logFormat	= new DecimalFormat();

	static {
		_logFormat.applyPattern("#.######");
		_logFormat.setPositivePrefix(" ");
	}

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

	private static String getGLErrors(final DrawContext dc) {

		final GL gl = dc.getGL();

		String errors = UI.EMPTY_STRING;

		for (int glError = gl.glGetError(); glError != GL.GL_NO_ERROR; glError = gl.glGetError()) {
			errors = dc.getGLU().gluErrorString(glError) + glError + "\t";
		}

		return errors;
	}

	/**
	 * Log depth settings.
	 * 
	 * @param dc
	 * @param logId
	 */
	public static void logDepth(final DrawContext dc, final String logId) {

		final GL gl = dc.getGL();

		final ByteBuffer writeMask = ByteBuffer.allocate(2);
		gl.glGetBooleanv(GL.GL_DEPTH_WRITEMASK, writeMask);

		final FloatBuffer depthRange = FloatBuffer.allocate(10);
		gl.glGetFloatv(GL.GL_DEPTH_RANGE, depthRange);

		final IntBuffer depthFunc = IntBuffer.allocate(1);
		gl.glGetIntegerv(GL.GL_DEPTH_FUNC, depthFunc);

		System.out.println((UI.timeStampNano() + " [" + logId + "] ")
				+ ("\tGL_DEPTH_TEST: " + gl.glIsEnabled(GL.GL_DEPTH_TEST))
				+ ("\tGL_DEPTH_WRITEMASK: " + writeMask.get(0))
				+ ("\tGL_DEPTH_FUNC: " + depthFunc.get(0))
				+ ("\tRange: " + _logFormat.format(depthRange.get(0)) + "\t" + _logFormat.format(depthRange.get(1)))
		//
				);
	}

	/**
	 * Called to check for openGL errors. This method includes a "round-trip" between the
	 * application and renderer, which is slow. Therefore, this method is excluded from the "normal"
	 * render pass. It is here as a matter of convenience to developers, and is not part of the API.
	 * 
	 * @param dc
	 *            the relevant <code>DrawContext</code>
	 * @param logId
	 */
	public static void logGLErrors(final DrawContext dc, final String logId) {

		// This is a copy from gov.nasa.worldwind.AbstractSceneController.checkGLErrors(DrawContext)

		final GL gl = dc.getGL();

		boolean isError = false;

		for (int err = gl.glGetError(); err != GL.GL_NO_ERROR; err = gl.glGetError()) {

			isError = true;

			String msg = dc.getGLU().gluErrorString(err);
			msg += err;

			System.out.println((UI.timeStampNano() + " [" + logId + "]\t") + msg);
		}

		if (isError == false) {
			System.out.println((UI.timeStampNano() + " [" + logId + "]\tNo OpenGL error") + ("\t"));
		}
	}

	/**
	 * Dumps the current modelview and perspective.
	 */
	public static void logModelViewPerspective(final DrawContext dc) {

		final GL2 gl = dc.getGL().getGL2();

		final FloatBuffer modelview = FloatBuffer.allocate(16);
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, modelview);

		final FloatBuffer perspective = FloatBuffer.allocate(16);
		gl.glGetFloatv(GL2.GL_PROJECTION_MATRIX, perspective);

		System.out.println((UI.timeStampNano() + " [" + GLLogger.class.getSimpleName() + "]\t")
				+ dumpMatrix_MVP(modelview, perspective));
	}

	/**
	 * Dumps the current modelview and perspective.
	 */
	public static void logStack(final DrawContext dc, final String logId) {

		final GL2 gl = dc.getGL().getGL2();

		final IntBuffer depthModel = IntBuffer.allocate(1);
		final IntBuffer depthPerspective = IntBuffer.allocate(1);

		gl.glGetIntegerv(GL2.GL_MODELVIEW_STACK_DEPTH, depthModel);
		gl.glGetIntegerv(GL2.GL_PROJECTION_STACK_DEPTH, depthPerspective);

		System.out.println((UI.timeStampNano() + " [" + logId + "]\t")
				+ ("GL_MODELVIEW_STACK_DEPTH:\t" + depthModel.get(0))
				+ "\t\t"
				+ ("GL_PROJECTION_STACK_DEPTH:\t" + depthPerspective.get(0))
				+ "\t"
				+ getGLErrors(dc)
		//
				);
	}
}
