/*
 * Copyright 2016 devemux86
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.tourbook.map25;

import static org.oscim.backend.GLAdapter.gl;
import static org.oscim.renderer.MapRenderer.COORD_SCALE;

import java.nio.FloatBuffer;

import net.tourbook.common.UI;

import org.oscim.backend.GL;
import org.oscim.backend.canvas.Color;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.utils.FastMath;

public class HexagonRenderer extends BucketRenderer {

   private static final char   NL         = UI.NEW_LINE;

   private static final String VERTEXT_SHADER  = ""

         + "#ifdef GLES                                                    " + NL
         + "precision mediump float;                                       " + NL
         + "#endif                                                         " + NL
         + "uniform mat4 u_mvp;                                            " + NL
         + "uniform vec2 u_center;                                         " + NL
         + "attribute vec2 a_pos;                                          " + NL
         + "void main()                                                    " + NL
         + "{                                                              " + NL
         + "   gl_Position = u_mvp * vec4(u_center + a_pos, 0.0, 1.0);     " + NL
         + "}                                                              " + NL

   ;

   private static final String FRAGMENT_SHADER = ""

         + "#ifdef GLES                                                    " + NL
         + "precision mediump float;                                       " + NL
         + "#endif                                                         " + NL
         + "varying float alpha;                                           " + NL
         + "uniform vec4 u_color;                                          " + NL
         + "void main()                                                    " + NL
         + "{                                                              " + NL
         + "  gl_FragColor = u_color;                                      " + NL
         + "}                                                              " + NL

   ;

   private int                 _programObject;

   private int                 _glHandle_VertexPosition;
   private int                 _glHandle_MatrixPosition;

   private int                 _glHandle_ColorPosition;
   private int                 _glHandle_CenterPosition;

   //private FloatBuffer mVertices;
   private boolean      mInitialized;
   private BufferObject mVBO;

   int                  mZoom      = -1;

   float                mCellScale = 30 * COORD_SCALE;

   @Override
   protected void compile() {

      final float[] vertices = new float[12];

      for (int i = 0; i < 6; i++) {
         vertices[i * 2 + 0] = (float) Math.cos(Math.PI * 2 * i / 6) * mCellScale;
         vertices[i * 2 + 1] = (float) Math.sin(Math.PI * 2 * i / 6) * mCellScale;
      }

      final FloatBuffer buf = MapRenderer.getFloatBuffer(12);
      buf.put(vertices);

      mVBO = BufferObject.get(GL.ARRAY_BUFFER, 0);
      mVBO.loadBufferData(buf.flip(), 12 * 4);

      setReady(true);
   }

   private boolean init() {

      // Load the vertex/fragment shaders
      final int programObject = GLShader.createProgram(VERTEXT_SHADER, FRAGMENT_SHADER);

      if (programObject == 0) {
         return false;
      }

      // Handle for vertex position in shader
      _glHandle_VertexPosition = gl.getAttribLocation(programObject, "a_pos");
      _glHandle_MatrixPosition = gl.getUniformLocation(programObject, "u_mvp");
      _glHandle_ColorPosition = gl.getUniformLocation(programObject, "u_color");
      _glHandle_CenterPosition = gl.getUniformLocation(programObject, "u_center");

      // Store the program object
      _programObject = programObject;

      return true;
   }

   @Override
   public void render(final GLViewport viewport) {

      // Use the program object
      GLState.useProgram(_programObject);

      GLState.blend(true);
      GLState.test(false, false);

      // bind VBO data
      mVBO.bind();

      // set VBO vertex layout
      gl.vertexAttribPointer(_glHandle_VertexPosition, 2, GL.FLOAT, false, 0, 0);

      GLState.enableVertexArrays(_glHandle_VertexPosition, GLState.DISABLED);

      /* apply view and projection matrices */
      // set mvp (tmp) matrix relative to mMapPosition
      // i.e. fixed on the map
      setMatrix(viewport);
      viewport.mvp.setAsUniform(_glHandle_MatrixPosition);

      final int offset_x = 4;
      final int offset_y = 16;

      final float h = (float) (Math.sqrt(3) / 2);

      for (int y = -offset_y; y < offset_y; y++) {
         for (int x = -offset_x; x < offset_x; x++) {

            final float xx = x * 2 + (y % 2 == 0 ? 1 : 0);
            final float yy = y * h + h / 2;

            gl.uniform2f(_glHandle_CenterPosition, xx * (mCellScale * 1.5f), yy * mCellScale);

            //float alpha = 1 + (float) Math.log10(FastMath.clamp(
            //        (float) Math.sqrt(xx * xx + yy * yy) / offset_y, 0.0f, 1.0f)) * 2;

            final float alpha = (float) Math.sqrt(xx * xx + yy * yy) / offset_y;

            final float fy = (float) (y + offset_y) / (offset_y * 2);
            final float fx = (float) (x + offset_x) / (offset_x * 2);
            final float fz = FastMath.clamp(
                  (float) (x < 0 || y < 0 ? 1 - Math.sqrt(fx * fx + fy
                        * fy)
                        : 0),
                  0,
                  1);

            final int c = 0xff << 24
                  | (int) (0xff * fy) << 16
                  | (int) (0xff * fx) << 8
                  | (int) (0xff * fz);

            GLUtils.setColor(_glHandle_ColorPosition, c, alpha);

            gl.drawArrays(GL.TRIANGLE_FAN, 0, 6);
         }
      }

      GLUtils.setColor(_glHandle_ColorPosition, Color.DKGRAY, 0.3f);

      for (int y = -offset_y; y < offset_y; y++) {
         for (int x = -offset_x; x < offset_x; x++) {
            final float xx = x * 2 + (y % 2 == 0 ? 1 : 0);
            final float yy = y * h + h / 2;

            gl.uniform2f(_glHandle_CenterPosition, xx * (mCellScale * 1.5f), yy * mCellScale);
            gl.drawArrays(GL.LINE_LOOP, 0, 6);
         }
      }

      GLUtils.checkGlError(getClass().getName() + ": render() end");
   }

   @Override
   public void update(final GLViewport v) {
      if (!mInitialized) {
         if (!init()) {
            return;
         }
         mInitialized = true;

         compile();
         mMapPosition.copy(v.pos);
      }

      //if (mZoom != v.pos.zoomLevel) {
      //    mMapPosition.copy(v.pos);
      //    mZoom = v.pos.zoomLevel;
      //}
   }

}
