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
import org.oscim.layers.Layer;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.utils.FastMath;

public class HexagonRenderer extends BucketRenderer {

   private static final char NL = UI.NEW_LINE;

   private int               _shaderProgram;

   private int               _shader_a_pos_VertexPosition;
   private int               _shader_u_mvp_MatrixPosition;
   private int               _shader_u_color_ColorPosition;
   private int               _shader_u_center_CenterPosition;

   private boolean           _isShaderSetup;

   private BufferObject      _vbo;

   private float             _cellScale;

   private Layer             _layer;

   private int               _oldX;
   private int               _oldY;
   private int               _oldZ;

   public HexagonRenderer(final Layer openGLTestLayer) {

      super();

      _layer = openGLTestLayer;
   }

   @Override
   protected void compile() {

      final float[] vertices = new float[12];

      for (int i = 0; i < 6; i++) {
         vertices[i * 2 + 0] = (float) Math.cos(Math.PI * 2 * i / 6) * _cellScale;
         vertices[i * 2 + 1] = (float) Math.sin(Math.PI * 2 * i / 6) * _cellScale;
      }

      final FloatBuffer buf = MapRenderer.getFloatBuffer(12);
      buf.put(vertices);

      _vbo = BufferObject.get(GL.ARRAY_BUFFER, 0);
      _vbo.loadBufferData(buf.flip(), 12 * 4);

      setReady(true);
   }

   private boolean init() {

      _cellScale = 10 * COORD_SCALE;

      final String VERTEXT_SHADER = "" //$NON-NLS-1$

            + "#ifdef GLES                                                    " + NL //$NON-NLS-1$
            + "precision mediump float;                                       " + NL //$NON-NLS-1$
            + "#endif                                                         " + NL //$NON-NLS-1$

            + "uniform mat4 u_mvp;                                            " + NL //$NON-NLS-1$
            + "uniform vec2 u_center;                                         " + NL //$NON-NLS-1$
            + "attribute vec2 a_pos;                                          " + NL //$NON-NLS-1$

            + "void main()                                                    " + NL //$NON-NLS-1$
            + "{                                                              " + NL //$NON-NLS-1$
            + "   gl_Position = u_mvp * vec4(u_center + a_pos, 0.0, 1.0);     " + NL //$NON-NLS-1$
            + "}                                                              " + NL //$NON-NLS-1$

      ;

      final String FRAGMENT_SHADER = "" //$NON-NLS-1$

            + "#ifdef GLES                                                    " + NL //$NON-NLS-1$
            + "precision mediump float;                                       " + NL //$NON-NLS-1$
            + "#endif                                                         " + NL //$NON-NLS-1$

            + "varying float alpha;                                           " + NL //$NON-NLS-1$
            + "uniform vec4 u_color;                                          " + NL //$NON-NLS-1$

            + "void main()                                                    " + NL //$NON-NLS-1$
            + "{                                                              " + NL //$NON-NLS-1$
            + "  gl_FragColor = u_color;                                      " + NL //$NON-NLS-1$
            + "}                                                              " + NL //$NON-NLS-1$

      ;

      // Load the vertex/fragment shaders
      final int programObject = GLShader.createProgram(VERTEXT_SHADER, FRAGMENT_SHADER);

      if (programObject == 0) {
         return false;
      }

      // Handle for vertex position in shader
      _shader_a_pos_VertexPosition = gl.getAttribLocation(programObject, "a_pos"); //$NON-NLS-1$
      _shader_u_mvp_MatrixPosition = gl.getUniformLocation(programObject, "u_mvp"); //$NON-NLS-1$
      _shader_u_color_ColorPosition = gl.getUniformLocation(programObject, "u_color"); //$NON-NLS-1$
      _shader_u_center_CenterPosition = gl.getUniformLocation(programObject, "u_center"); //$NON-NLS-1$

      // Store the program object
      _shaderProgram = programObject;

      return true;
   }

   @Override
   public void render(final GLViewport viewport) {

//      System.out.println((System.currentTimeMillis() + " net.tourbook.map25.HexagonRenderer.render(GLViewport) " + mMapPosition));
//      // TODO remove SYSTEM.OUT.PRINTLN

      // Use the program object
      GLState.useProgram(_shaderProgram);

      GLState.blend(true);
      GLState.test(false, false);

      // bind VBO data
      _vbo.bind();

      // set VBO vertex layout
      gl.vertexAttribPointer(_shader_a_pos_VertexPosition, 2, GL.FLOAT, false, 0, 0);

      GLState.enableVertexArrays(_shader_a_pos_VertexPosition, GLState.DISABLED);

      /* apply view and projection matrices */
      // set mvp (tmp) matrix relative to mMapPosition
      // i.e. fixed on the map
      setMatrix(viewport);
      viewport.mvp.setAsUniform(_shader_u_mvp_MatrixPosition);

      /*
       * Draw hexagon cells
       */
      final int offset_x = 4;
      final int offset_y = 16;

      final float h = (float) (Math.sqrt(3) / 2);

      for (int y = -offset_y; y < offset_y; y++) {
         for (int x = -offset_x; x < offset_x; x++) {

            final float xx = x * 2 + (y % 2 == 0 ? 1 : 0);
            final float yy = y * h + h / 2;

            final float xPos = xx * (_cellScale * 1.5f);
            final float yPos = yy * _cellScale;
            gl.uniform2f(_shader_u_center_CenterPosition, xPos, yPos);

            //float alpha = 1 + (float) Math.log10(FastMath.clamp(
            //        (float) Math.sqrt(xx * xx + yy * yy) / offset_y, 0.0f, 1.0f)) * 2;

            // less opaque in the center
            float alpha = (float) Math.sqrt(xx * xx + yy * yy) / offset_y;
            alpha = 0.7f;

            final float fy = (float) (y + offset_y) / (offset_y * 2);
            final float fx = (float) (x + offset_x) / (offset_x * 2);
            final float fz = FastMath.clamp(
                  (float) (x < 0 || y < 0 ? 1 - Math.sqrt(fx * fx + fy
                        * fy)
                        : 0),
                  0,
                  1);

            final int color = 0xff << 24
                  | (int) (0xff * fy) << 16
                  | (int) (0xff * fx) << 8
                  | (int) (0xff * fz);

            GLUtils.setColor(_shader_u_color_ColorPosition, color, alpha);

            gl.drawArrays(GL.TRIANGLE_FAN, 0, 6);
         }
      }

      /*
       * Draw cell border
       */
      GLUtils.setColor(_shader_u_color_ColorPosition, Color.DKGRAY, 0.3f);

      for (int y = -offset_y; y < offset_y; y++) {
         for (int x = -offset_x; x < offset_x; x++) {

            final float xx = x * 2 + (y % 2 == 0 ? 1 : 0);
            final float yy = y * h + h / 2;

            gl.uniform2f(_shader_u_center_CenterPosition, xx * (_cellScale * 1.5f), yy * _cellScale);
            gl.drawArrays(GL.LINE_LOOP, 0, 6);
         }
      }

      GLUtils.checkGlError(getClass().getName() + ": render() end"); //$NON-NLS-1$
   }

   @Override
   public void update(final GLViewport viewport) {

      if (_layer.isEnabled() == false) {
         return;
      }

      if (_isShaderSetup == false) {

         if (init() == false) {
            return;
         }

         _isShaderSetup = true;

         compile();

      }

      mMapPosition.copy(viewport.pos);

//      final MapPosition currentMapPosition = viewport.pos;
//
//      /*
//       * Scale coordinates relative to current 'zoom-level' to get the position as the nearest
//       * tile coordinate
//       */
//      final int currentZ = 1 << currentMapPosition.zoomLevel;
//      final int currentX = (int) (currentMapPosition.x * currentZ);
//      final int currentY = (int) (currentMapPosition.y * currentZ);
//
//      // update buckets when map moved by at least one tile
//      if (currentX == _oldX && currentY == _oldY && currentZ == _oldZ) {
//         return;
//      }
//
//      _oldX = currentX;
//      _oldY = currentY;
//      _oldZ = currentZ;
//
//      /*
//       * Overwrite map position in this renderer
//       */
//      mMapPosition.copy(currentMapPosition);
//      mMapPosition.x = (double) currentX / currentZ;
//      mMapPosition.y = (double) currentY / currentZ;
//      mMapPosition.scale = currentZ;

   }

}
