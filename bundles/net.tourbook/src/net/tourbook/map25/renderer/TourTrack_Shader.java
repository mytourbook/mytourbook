/*******************************************************************************
 * Copyright (C) 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.renderer;

import static org.oscim.backend.GLAdapter.gl;
import static org.oscim.renderer.MapRenderer.COORD_SCALE;

import net.tourbook.common.UI;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.layer.tourtrack.Map25TrackConfig;

import org.oscim.backend.GL;
import org.oscim.core.MapPosition;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.theme.styles.LineStyle;

public final class TourTrack_Shader {

   /**
    * Factor to normalize extrusion vector and scale to coord scale
    */
   private static final float           COORD_SCALE_BY_DIR_SCALE = COORD_SCALE / TourTrack_Bucket.DIR_SCALE;

   private static final int             CAP_THIN                 = 0;
   private static final int             CAP_BUTT                 = 1;
   private static final int             CAP_ROUND                = 2;

   private static final int             SHADER_PROJECTED         = 0;
   private static final int             SHADER_FLAT              = 1;

   private static DirectionArrowsShader _directionArrowShader;
   private static LineShader[]          _lineShaders             = { null, null };

   static int                           bufferId_Vertices;
   static int                           bufferId_VerticesColor;
   static int                           bufferId_DirArrows;
   static int                           bufferId_DirArrows_ColorCoords;
   static int                           bufferId_DirArrows_ArrowIndices;

   static long                          dirArrowAnimation_StartTime;
   private static float                 _dirArrowAnimation_CurrentArrowIndex;
   private static long                  _dirArrowAnimation_LastUpdateTime;

   private static class DirectionArrowsShader extends GLShaderMT {

      /**
       * Location for a shader variable
       */
      int shader_a_pos,
            shader_attrib_ColorCoord,
            shader_attrib_ArrowIndices,

            shader_u_mvp,

            shader_uni_ArrowColors,
            shader_uni_GlowArrowIndex,
            shader_uni_IsAnimate,
            shader_uni_OutlineWidth,
            shader_uni_Vp2MpScale

//          shader_u_width

      ;

      DirectionArrowsShader(final String shaderFile) {

         if (loadShader(shaderFile, "#version 330" + UI.NEW_LINE) == false) {
            return;
         }

   // SET_FORMATTING_OFF

            shader_u_mvp                  = getUniform("u_mvp");                 //$NON-NLS-1$
            shader_a_pos                  = getAttrib("a_pos");                  //$NON-NLS-1$
            shader_attrib_ColorCoord      = getAttrib("attrib_ColorCoord");      //$NON-NLS-1$
            shader_attrib_ArrowIndices    = getAttrib("attrib_ArrowIndices");    //$NON-NLS-1$

//          shader_u_width                = getUniform("u_width");               //$NON-NLS-1$
            shader_uni_ArrowColors        = getUniform("uni_ArrowColors");       //$NON-NLS-1$
            shader_uni_OutlineWidth       = getUniform("uni_OutlineWidth");      //$NON-NLS-1$
            shader_uni_Vp2MpScale         = getUniform("uni_Vp2MpScale");        //$NON-NLS-1$

            shader_uni_GlowArrowIndex     = getUniform("uni_GlowArrowIndex");    //$NON-NLS-1$
            shader_uni_IsAnimate          = getUniform("uni_IsAnimate");         //$NON-NLS-1$

   // SET_FORMATTING_ON
      }
   }

   private static class LineShader extends GLShaderMT {

      int shader_a_pos,
            shader_aVertexColor,

            shader_u_mvp,

            shader_u_fade,
            shader_u_color,
            shader_u_mode,

            shader_u_width,
            shader_u_height,

            shader_uColorMode,
            shader_uOutlineBrightness,
            shader_uVertexColorAlpha

      ;

      LineShader(final String shaderFile) {

         if (loadShader(shaderFile, null) == false) {
            return;
         }

   // SET_FORMATTING_OFF

            shader_a_pos               = getAttrib("a_pos"); //$NON-NLS-1$
            shader_aVertexColor        = getAttrib("aVertexColor"); //$NON-NLS-1$

            shader_u_mvp               = getUniform("u_mvp"); //$NON-NLS-1$

            shader_u_fade              = getUniform("u_fade"); //$NON-NLS-1$
            shader_u_color             = getUniform("u_color"); //$NON-NLS-1$
            shader_u_mode              = getUniform("u_mode"); //$NON-NLS-1$

            shader_u_width             = getUniform("u_width"); //$NON-NLS-1$
            shader_u_height            = getUniform("u_height"); //$NON-NLS-1$

            shader_uColorMode          = getUniform("uColorMode"); //$NON-NLS-1$
            shader_uOutlineBrightness  = getUniform("uOutlineBrightness"); //$NON-NLS-1$
            shader_uVertexColorAlpha   = getUniform("uVertexColorAlpha"); //$NON-NLS-1$

   // SET_FORMATTING_ON
      }
   }

   private static float getGlowArrowIndex(final TourTrack_BucketManager bucketManager) {

      final Map25TrackConfig trackConfig = Map25ConfigManager.getActiveTourTrackConfig();

      // how many arrows are moved in one second
      final int arrowsPerSecond = trackConfig.arrow_ArrowsPerSecond;

      final long currentTimeMS = System.currentTimeMillis();

      // update sequence in one second
      final float secondsForOneArrowSec = 1f / arrowsPerSecond;
      final long secondsForOneArrowMS = (long) (secondsForOneArrowSec * 1000);

      // ensure there not more arrows per second are displayed
      final long nextUpdateTimeMS = _dirArrowAnimation_LastUpdateTime + secondsForOneArrowMS;
      final long timeDiffMS = nextUpdateTimeMS - currentTimeMS;
      if (timeDiffMS > 0) {
         return _dirArrowAnimation_CurrentArrowIndex;
      }

      // the last arrow is not displayed -> -1
      final float maxDirArrows = Math.max(0, bucketManager.numDirectionArrows - 1);
      final float maxLoopTime = maxDirArrows / arrowsPerSecond;

      final float timeDiffSinceFirstRun = (float) ((currentTimeMS - dirArrowAnimation_StartTime) / 1000.0);
      final float currentTimeIndex = timeDiffSinceFirstRun % maxLoopTime;

      float glowArrowIndex = Math.round(currentTimeIndex * arrowsPerSecond);

      // ensure to not jump back to the start when the end is not yet reached
      if (glowArrowIndex < maxDirArrows) {
         glowArrowIndex = _dirArrowAnimation_CurrentArrowIndex + 1;
      }

      // ensure to move not more than one arrow
      if (glowArrowIndex > _dirArrowAnimation_CurrentArrowIndex + 1) {
         glowArrowIndex = _dirArrowAnimation_CurrentArrowIndex + 1;
      }

      // ensure bounds
      if (_dirArrowAnimation_CurrentArrowIndex > maxDirArrows - 1) {
         glowArrowIndex = 0;
      }

      _dirArrowAnimation_CurrentArrowIndex = glowArrowIndex;
      _dirArrowAnimation_LastUpdateTime = currentTimeMS;

      return glowArrowIndex;
   }

   /**
    * Performs OpenGL drawing commands of the renderBucket(s)
    *
    * @param trackBucket
    * @param viewport
    * @param vp2mpScale
    *           Viewport scale 2 map scale: it is between 1...2
    * @param bucketManager
    * @return
    */
   public static void paint(final TourTrack_Bucket trackBucket,
                            final GLViewport viewport,
                            final float vp2mpScale,
                            final TourTrack_BucketManager bucketManager) {

//    _dirArrowFrameBuffer.updateViewport(viewport, 0.5f);

      final Map25TrackConfig trackConfig = Map25ConfigManager.getActiveTourTrackConfig();

      // fix alpha blending
      gl.blendFunc(GL.SRC_ALPHA, GL.ONE_MINUS_SRC_ALPHA);
      {
         paint_10_Track(trackBucket, viewport, vp2mpScale);

         if (trackConfig.isShowDirectionArrow) {
            paint_20_DirectionArrows(viewport, bucketManager, vp2mpScale);
         }
      }
      gl.blendFunc(GL.ONE, GL.ONE_MINUS_SRC_ALPHA); // reset to map default
   }

   /**
    * Performs OpenGL drawing commands of the renderBucket(s)
    *
    * @param inoutRenderBucket
    *           In/out render bucked
    * @param viewport
    * @param vp2mpScale
    *           Viewport scale 2 map scale: it is between 1...2
    * @param renderBucketsAll
    * @return
    */
   private static void paint_10_Track(final TourTrack_Bucket trackBucket,
                                      final GLViewport viewport,
                                      final float vp2mpScale) {

      final Map25TrackConfig trackConfig = Map25ConfigManager.getActiveTourTrackConfig();

      final MapPosition mapPosition = viewport.pos;

      /*
       * Simple line shader does not take forward shortening into
       * account. only used when tilt is 0.
       */
      final int shaderMode = mapPosition.tilt < 1

            // 1 == not projected
            ? SHADER_FLAT

            // 0 == projected
            : SHADER_PROJECTED;

//       shaderMode = shaderMode;
//       shaderMode = SHADER_FLAT;

      final LineShader shader = _lineShaders[shaderMode];

      // is calling GL.enableVertexAttribArray() for shader_a_pos
      shader.useProgram();

      GLState.blend(true);

      final int shader_a_pos = shader.shader_a_pos;
      final int shader_aVertexColor = shader.shader_aVertexColor;

      final int shader_u_fade = shader.shader_u_fade;
      final int shader_u_mode = shader.shader_u_mode;
      final int shader_u_color = shader.shader_u_color;
      final int shader_u_width = shader.shader_u_width;
      final int shader_u_height = shader.shader_u_height;

      final int shader_uColorMode = shader.shader_uColorMode;
      final int shader_uOutlineBrightness = shader.shader_uOutlineBrightness;
      final int shader_uVertexColorAlpha = shader.shader_uVertexColorAlpha;

      gl.bindBuffer(GL.ARRAY_BUFFER, bufferId_Vertices);
      gl.enableVertexAttribArray(shader_a_pos);
      gl.vertexAttribPointer(

            shader_a_pos, //           index of the vertex attribute that is to be modified
            4, //                      number of components per vertex attribute, must be 1, 2, 3, or 4
            GL.SHORT, //               data type of each component in the array
            false, //                  values should be normalized
            0, //                      offset in bytes between the beginning of consecutive vertex attributes
            0 //                       offset in bytes of the first component in the vertex attribute array
      );

      /*
       * Set vertex color
       */
      gl.bindBuffer(GL.ARRAY_BUFFER, bufferId_VerticesColor);
      gl.enableVertexAttribArray(shader_aVertexColor);
      gl.vertexAttribPointer(

            shader_aVertexColor, //    index of the vertex attribute that is to be modified
            4, //                      number of components per vertex attribute, must be 1, 2, 3, or 4
            GL.UNSIGNED_BYTE, //       data type of each component in the array
            false, //                  values should be normalized
            0, //                      offset in bytes between the beginning of consecutive vertex attributes
            0 //                       offset in bytes of the first component in the vertex attribute array
      );

      // set matrix
      viewport.mvp.setAsUniform(shader.shader_u_mvp);

//       final double groundResolution = MercatorProjection.groundResolution(mapPosition);

      /**
       * Line scale factor for non fixed lines:
       * <p>
       * Within a zoom-level, lines would be scaled by the factor 2 by view-matrix.
       * Though lines should only scale by sqrt(2). This is achieved
       * by inverting scaling of extrusion vector with: width/sqrt(scale).
       */
      final double variableScale = Math.sqrt(vp2mpScale);

      /*
       * Scale factor to map one pixel on tile to one pixel on screen:
       * used with orthographic projection, (shader mode == 1)
       */
      final double pixel = (shaderMode == SHADER_PROJECTED)
            ? 0.0001
            : 1.5 / vp2mpScale;

      gl.uniform1f(shader_u_fade, (float) pixel);

      int capMode = CAP_THIN;
      gl.uniform1i(shader_u_mode, capMode);

      boolean isBlur = false;
      double width;

      float heightOffset = 0;
      gl.uniform1f(shader_u_height, heightOffset);

      final LineStyle lineStyle = trackBucket.lineStyle.current();

      final boolean isPaintOutline = trackConfig.isShowOutline;
      final float outlineWidth = trackConfig.outlineWidth;
      final float outlineBrightnessRaw = trackConfig.outlineBrighness; // -1.0 ... 1.0
      final float outlineBrightness = outlineBrightnessRaw + 1; // 0...2

      gl.uniform1i(shader_uColorMode, trackBucket.lineColorMode);

      if (lineStyle.heightOffset != trackBucket._heightOffset) {
         trackBucket._heightOffset = lineStyle.heightOffset;
      }

      if (trackBucket._heightOffset != heightOffset) {

         heightOffset = trackBucket._heightOffset;

//          final double lineHeight = (heightOffset / groundResolution) / scale;
         final double lineHeight = heightOffset * vp2mpScale;

         gl.uniform1f(shader_u_height, (float) lineHeight);
      }

      if (lineStyle.fadeScale < mapPosition.zoomLevel) {

         GLUtils.setColor(shader_u_color, lineStyle.color, 1);

      } else if (lineStyle.fadeScale > mapPosition.zoomLevel) {

         return;

      } else {

         final float alpha = (float) (vp2mpScale > 1.2 ? vp2mpScale : 1.2) - 1;
         GLUtils.setColor(shader_u_color, lineStyle.color, alpha);
      }

      // set common alpha for the vertex color
      final float vertexAlpha = ((lineStyle.color >>> 24) & 0xff) / 255f;
      gl.uniform1f(shader_uVertexColorAlpha, vertexAlpha);

      if (shaderMode == SHADER_PROJECTED && isBlur && lineStyle.blur == 0) {
         gl.uniform1f(shader_u_fade, (float) pixel);
         isBlur = false;
      }

      /*
       * First draw the outline which is afterwards overwritten partly by the core line
       */
      if (isPaintOutline) {

         // core width
         if (lineStyle.fixed) {
            width = Math.max(lineStyle.width, 1) / vp2mpScale;
         } else {
            width = lineStyle.width / variableScale;
         }

         // add outline width
         if (lineStyle.fixed) {
            width += outlineWidth / vp2mpScale;
         } else {
            width += outlineWidth / variableScale;
         }

         gl.uniform1f(shader_u_width, (float) (width * COORD_SCALE_BY_DIR_SCALE));

         // outline brighness
         gl.uniform1f(shader_uOutlineBrightness, outlineBrightness);

         // line-edge fade
         if (lineStyle.blur > 0) {
            gl.uniform1f(shader_u_fade, lineStyle.blur);
            isBlur = true;
         } else if (shaderMode == SHADER_FLAT) {
            gl.uniform1f(shader_u_fade, (float) (pixel / width));
         }

         // cap mode
         if (trackBucket._isCapRounded) {
            if (capMode != CAP_ROUND) {
               capMode = CAP_ROUND;
               gl.uniform1i(shader_u_mode, capMode);
            }
         } else if (capMode != CAP_BUTT) {
            capMode = CAP_BUTT;
            gl.uniform1i(shader_u_mode, capMode);
         }

         gl.drawArrays(GL.TRIANGLE_STRIP, 0, trackBucket.numVertices);
      }

      /*
       * Draw core line over the outline
       */

      // invert scaling of extrusion vectors so that line width stays the same.
      if (lineStyle.fixed) {
         width = Math.max(lineStyle.width, 1) / vp2mpScale;
      } else {
         width = lineStyle.width / variableScale;
      }

      // disable outline brighness/darkness, this value is multiplied with the color
      gl.uniform1f(shader_uOutlineBrightness, 1.0f);

      // factor to increase line width relative to scale
      gl.uniform1f(shader_u_width, (float) (width * COORD_SCALE_BY_DIR_SCALE));

      // line-edge fade
      if (lineStyle.blur > 0) {
         gl.uniform1f(shader_u_fade, lineStyle.blur);
         isBlur = true;
      } else if (shaderMode == SHADER_FLAT) {
         gl.uniform1f(shader_u_fade, (float) (pixel / width));
      }

      // cap mode
      if (trackBucket._isCapRounded) {
         if (capMode != CAP_ROUND) {
            capMode = CAP_ROUND;
            gl.uniform1i(shader_u_mode, capMode);
         }
      } else if (capMode != CAP_BUTT) {
         capMode = CAP_BUTT;
         gl.uniform1i(shader_u_mode, capMode);
      }

//       GLState.test(true, false);
//       gl.depthMask(true);
//       {
//          gl.drawArrays(GL.TRIANGLE_STRIP, lineBucket.vertexOffset, lineBucket.numVertices);
//       }
//       gl.depthMask(false);

      gl.drawArrays(GL.TRIANGLE_STRIP, 0, trackBucket.numVertices);
   }

   private static void paint_20_DirectionArrows(final GLViewport viewport,
                                                final TourTrack_BucketManager bucketManager,
                                                final float vp2mpScale) {

// SET_FORMATTING_OFF

      final DirectionArrowsShader shader     = _directionArrowShader;

      final int shader_a_pos                 = shader.shader_a_pos;
      final int shader_attrib_ColorCoord     = shader.shader_attrib_ColorCoord;
      final int shader_attrib_ArrowIndices   = shader.shader_attrib_ArrowIndices;

//    final int shader_u_width               = shader.shader_u_width;

// SET_FORMATTING_ON

      shader.useProgram();

      final Map25TrackConfig trackConfig = Map25ConfigManager.getActiveTourTrackConfig();

      // set mvp matrix into the shader
      viewport.mvp.setAsUniform(shader.shader_u_mvp);

      // vertices position
      gl.bindBuffer(GL.ARRAY_BUFFER, bufferId_DirArrows);
      gl.enableVertexAttribArray(shader_a_pos);
      gl.vertexAttribPointer(

            shader_a_pos, //                 index of the vertex attribute that is to be modified
            4, //                            number of components per vertex attribute, must be 1, 2, 3, or 4
            GL.SHORT, //                     data type of each component in the array
            false, //                        values should be normalized
            0, //                            offset in bytes between the beginning of consecutive vertex attributes
            0 //                             offset in bytes of the first component in the vertex attribute array
      );

      // direction arrow color
      gl.bindBuffer(GL.ARRAY_BUFFER, bufferId_DirArrows_ColorCoords);
      gl.enableVertexAttribArray(shader_attrib_ColorCoord);
      gl.vertexAttribPointer(

            shader_attrib_ColorCoord, //     index of the vertex attribute that is to be modified
            3, //                            number of components per vertex attribute, must be 1, 2, 3, or 4
            GL.SHORT, //                     data type of each component in the array
            false, //                        values should be normalized
            0, //                            offset in bytes between the beginning of consecutive vertex attributes
            0 //                             offset in bytes of the first component in the vertex attribute array
      );

      // arrow index
      gl.bindBuffer(GL.ARRAY_BUFFER, bufferId_DirArrows_ArrowIndices);
      gl.enableVertexAttribArray(shader_attrib_ArrowIndices);
      gl.vertexAttribPointer(

            shader_attrib_ArrowIndices, //   index of the vertex attribute that is to be modified
            1, //                            number of components per vertex attribute, must be 1, 2, 3, or 4
            GL.FLOAT, //                     data type of each component in the array
            false, //                        values should be normalized
            0, //                            offset in bytes between the beginning of consecutive vertex attributes
            0 //                             offset in bytes of the first component in the vertex attribute array
      );

//    final float width = 10 / vp2mpScale;
//
//    gl.uniform1f(shader_u_width, width * COORD_SCALE_BY_DIR_SCALE);

      // arrow colors
      final float arrowColors[] = trackConfig.getArrowColors();
      gl.uniform4fv(shader.shader_uni_ArrowColors, arrowColors.length / 4, arrowColors, 0);

      // outline width's
      gl.uniform2f(shader.shader_uni_OutlineWidth,
            trackConfig.arrowWing_OutlineWidth / 200f,
            trackConfig.arrowFin_OutlineWidth / 200f);

      // viewport to map scale: 1.0...2.0
      gl.uniform1f(shader.shader_uni_Vp2MpScale, vp2mpScale);

      // glowing
      gl.uniform1f(shader.shader_uni_GlowArrowIndex, getGlowArrowIndex(bucketManager));
      gl.uniform1f(shader.shader_uni_IsAnimate, trackConfig.arrow_IsAnimate ? 1 : 0);

      /*
       * Draw direction arrows
       */
      GLState.test(true, false);
      gl.depthMask(true);
      {
         gl.drawArrays(GL.TRIANGLES, 0, bucketManager.numDirectionArrowVertices);
      }
      gl.depthMask(false);

      GLUtils.checkGlError(TourTrack_Shader.class.getName());
   }

   public static boolean setupShader() {

// SET_FORMATTING_OFF

      _lineShaders[SHADER_PROJECTED]   = new LineShader("line_aa_proj");       //$NON-NLS-1$
      _lineShaders[SHADER_FLAT]        = new LineShader("line_aa");            //$NON-NLS-1$

      _directionArrowShader            = new DirectionArrowsShader("directionArrows");    //$NON-NLS-1$

      // create buffer id's
      bufferId_DirArrows               = gl.genBuffer();
      bufferId_DirArrows_ColorCoords   = gl.genBuffer();
      bufferId_DirArrows_ArrowIndices  = gl.genBuffer();
      bufferId_Vertices                = gl.genBuffer();
      bufferId_VerticesColor           = gl.genBuffer();

// SET_FORMATTING_ON

//    _dirArrowFrameBuffer = new FrameBuffer();

      return true;
   }
}
