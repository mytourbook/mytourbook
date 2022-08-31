/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016-2021 devemux86
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.tourbook.map25.renderer;

import static org.oscim.backend.GLAdapter.gl;
import static org.oscim.renderer.MapRenderer.COORD_SCALE;

import gnu.trove.list.array.TFloatArrayList;

import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.layer.tourtrack.Map25TrackConfig;
import net.tourbook.map25.layer.tourtrack.TourLayer;

import org.oscim.backend.GL;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapPosition;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.theme.styles.LineStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Note:
 * <p>
 * Coordinates must be in range +/- (Short.MAX_VALUE / COORD_SCALE) if using GL.SHORT
 * <p>
 * The maximum resolution for coordinates is 0.25 as points will be converted
 * to fixed point values.
 */
public class LineBucketMT extends RenderBucketMT {

   static final Logger        log              = LoggerFactory.getLogger(LineBucketMT.class);

   /**
    * Scale factor mapping extrusion vector to short values
    */
   public static final float  DIR_SCALE        = 2048;

   /**
    * Maximal resolution
    */
   public static final float  MIN_DIST         = 1 / 8f;

   /**
    * Not quite right.. need to go back so that additional
    * bevel vertices are at least MIN_DIST apart
    */
   private static final float MIN_BEVEL        = MIN_DIST * 4;

   /**
    * Mask for packing last two bits of extrusion vector with texture
    * coordinates, .... 1111 1100
    */
   private static final int   DIR_MASK         = 0xFFFFFFFC;

   /**
    * Lines referenced by this outline layer
    */
   public LineBucketMT        outlines;

   public LineStyle           lineStyle;
   public int                 lineColorMode;

   public float               testValue;

   public float               scale            = 1;

   private boolean            _isCapRounded;
   private float              _minimumDistance = MIN_DIST;
   private float              _minimumBevel    = MIN_BEVEL;

   private float              _heightOffset;

   private int                tmin             = Integer.MIN_VALUE, tmax = Integer.MAX_VALUE;

   private static class DirectionArrowsShader extends GLShaderMT {

      int shader_a_pos,
            shader_attrib_ColorCoord,
            shader_u_mvp,

            shader_uni_ArrowColors,
            shader_uni_OutlineWidth

//          shader_u_width

      ;

      DirectionArrowsShader(final String shaderFile) {

         if (createMT(shaderFile) == false) {
            return;
         }

// SET_FORMATTING_OFF

         shader_u_mvp                  = getUniform("u_mvp");                 //$NON-NLS-1$
         shader_a_pos                  = getAttrib("a_pos");                  //$NON-NLS-1$
         shader_attrib_ColorCoord      = getAttrib("attrib_ColorCoord");       //$NON-NLS-1$

//       shader_u_width                = getUniform("u_width");               //$NON-NLS-1$
         shader_uni_ArrowColors        = getUniform("uni_ArrowColors");       //$NON-NLS-1$
         shader_uni_OutlineWidth       = getUniform("uni_OutlineWidth");      //$NON-NLS-1$

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

         if (createMT(shaderFile) == false) {
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

      @Override
      public boolean useProgram() {

         if (super.useProgram()) {

            GLState.enableVertexArrays(shader_a_pos, GLState.DISABLED);

            return true;
         }

         return false;
      }
   }

   public static final class Renderer {

      /*
       * TODO:
       * http://http.developer.nvidia.com/GPUGems2/gpugems2_chapter22.html
       */

      /**
       * Factor to normalize extrusion vector and scale to coord scale
       */
      private static final float           COORD_SCALE_BY_DIR_SCALE = COORD_SCALE / DIR_SCALE;

      private static final int             CAP_THIN                 = 0;
      private static final int             CAP_BUTT                 = 1;
      private static final int             CAP_ROUND                = 2;

      private static final int             SHADER_PROJECTED         = 0;
      private static final int             SHADER_FLAT              = 1;

      private static int                   _textureID;

      private static DirectionArrowsShader _directionArrowShader;
      private static LineShader[]          _lineShaders             = { null, null };

      /**
       * Performs OpenGL drawing commands of the renderBucket(s)
       *
       * @param renderBucket
       * @param viewport
       * @param vp2mpScale
       *           Viewport scale 2 map scale: it is between 1...2
       * @param renderBucketsAll
       * @return
       */
      public static RenderBucketMT draw(RenderBucketMT renderBucket,
                                        final GLViewport viewport,
                                        final float vp2mpScale,
                                        final RenderBucketsAllMT renderBucketsAll) {

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

         /*
          * Somehow we loose the texture after an indefinite time, when label/symbol textures are
          * used.
          * Debugging gl on Desktop is most fun imaginable, so for now:
          */
         if (!GLAdapter.GDX_DESKTOP_QUIRKS) {
            GLState.bindTex2D(_textureID);
         }

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

         gl.vertexAttribPointer(

               shader_a_pos, //                    index of the vertex attribute that is to be modified
               4, //                               number of components per vertex attribute, must be 1, 2, 3, or 4
               GL.SHORT, //                        data type of each component in the array
               false, //                           values should be normalized
               0, //                               offset in bytes between the beginning of consecutive vertex attributes
               renderBucketsAll.offset[LINE] //    offset in bytes of the first component in the vertex attribute array
         );

         /*
          * Set vertex color
          */
         gl.bindBuffer(GL.ARRAY_BUFFER, renderBucketsAll.vertexColor_BufferId);
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

         for (; renderBucket != null && renderBucket.type == LINE; renderBucket = renderBucket.next) {

            final LineBucketMT lineBucket = (LineBucketMT) renderBucket;
            final LineStyle lineStyle = lineBucket.lineStyle.current();

            final float scale = lineBucket.scale;

            final boolean isPaintOutline = trackConfig.isShowOutline;
            final float outlineWidth = trackConfig.outlineWidth;
            final float outlineBrightnessRaw = trackConfig.outlineBrighness; // -1.0 ... 1.0
            final float outlineBrightness = outlineBrightnessRaw + 1; // 0...2

            gl.uniform1i(shader_uColorMode, lineBucket.lineColorMode);

            if (lineStyle.heightOffset != lineBucket._heightOffset) {
               lineBucket._heightOffset = lineStyle.heightOffset;
            }

            if (lineBucket._heightOffset != heightOffset) {

               heightOffset = lineBucket._heightOffset;

//             final double lineHeight = (heightOffset / groundResolution) / scale;
               final double lineHeight = heightOffset * vp2mpScale;

               gl.uniform1f(shader_u_height, (float) lineHeight);
            }

            if (lineStyle.fadeScale < mapPosition.zoomLevel) {

               GLUtils.setColor(shader_u_color, lineStyle.color, 1);

            } else if (lineStyle.fadeScale > mapPosition.zoomLevel) {

               continue;

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
                  width = scale * lineStyle.width / variableScale;
               }

               // add outline width
               if (lineStyle.fixed) {
                  width += outlineWidth / vp2mpScale;
               } else {
                  width += scale * outlineWidth / variableScale;
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
               if (lineBucket._isCapRounded) {
                  if (capMode != CAP_ROUND) {
                     capMode = CAP_ROUND;
                     gl.uniform1i(shader_u_mode, capMode);
                  }
               } else if (capMode != CAP_BUTT) {
                  capMode = CAP_BUTT;
                  gl.uniform1i(shader_u_mode, capMode);
               }

               gl.drawArrays(GL.TRIANGLE_STRIP, lineBucket.vertexOffset, lineBucket.numVertices);
            }

            /*
             * Draw core line over the outline
             */

            // invert scaling of extrusion vectors so that line width stays the same.
            if (lineStyle.fixed) {
               width = Math.max(lineStyle.width, 1) / vp2mpScale;
            } else {
               width = scale * lineStyle.width / variableScale;
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
            if (scale < 1.0) {
               if (capMode != CAP_THIN) {
                  capMode = CAP_THIN;
                  gl.uniform1i(shader_u_mode, capMode);
               }
            } else if (lineBucket._isCapRounded) {
               if (capMode != CAP_ROUND) {
                  capMode = CAP_ROUND;
                  gl.uniform1i(shader_u_mode, capMode);
               }
            } else if (capMode != CAP_BUTT) {
               capMode = CAP_BUTT;
               gl.uniform1i(shader_u_mode, capMode);
            }

            gl.drawArrays(GL.TRIANGLE_STRIP, lineBucket.vertexOffset, lineBucket.numVertices);
         }

         if (trackConfig.isShowDirectionArrow) {
            draw_DirectionArrows(viewport, renderBucketsAll, vp2mpScale);
         }

         return renderBucket;
      }

      private static void draw_DirectionArrows(final GLViewport viewport,
                                               final RenderBucketsAllMT allRenderBuckets,
                                               final float vp2mpScale) {

         final Map25TrackConfig trackConfig = Map25ConfigManager.getActiveTourTrackConfig();

// SET_FORMATTING_OFF

         final DirectionArrowsShader shader        = _directionArrowShader;

         final int shader_a_pos                    = shader.shader_a_pos;
         final int shader_attrib_ColorCoord        = shader.shader_attrib_ColorCoord;
         final int shader_u_mvp                    = shader.shader_u_mvp;
//       final int shader_u_width                  = shader.shader_u_width;
         final int shader_uni_ArrowColors          = shader.shader_uni_ArrowColors;
         final int shader_uni_OutlineWidth         = shader.shader_uni_OutlineWidth;

// SET_FORMATTING_ON

         shader.useProgram();

         GLState.blend(true);
         GLState.test(true, false);
         gl.depthMask(true);

         // set mvp matrix into the shader
         viewport.mvp.setAsUniform(shader_u_mvp);

         gl.bindBuffer(GL.ARRAY_BUFFER, allRenderBuckets.dirArrows_BufferId);
         gl.enableVertexAttribArray(shader_a_pos);
         gl.vertexAttribPointer(

               shader_a_pos, //           index of the vertex attribute that is to be modified
               4, //                      number of components per vertex attribute, must be 1, 2, 3, or 4
               GL.SHORT, //               data type of each component in the array
               false, //                  values should be normalized
               0, //                      offset in bytes between the beginning of consecutive vertex attributes
               0 //                       offset in bytes of the first component in the vertex attribute array
         );

         gl.bindBuffer(GL.ARRAY_BUFFER, allRenderBuckets.dirArrows_ColorCoords_BufferId);
         gl.enableVertexAttribArray(shader_attrib_ColorCoord);
         gl.vertexAttribPointer(

               shader_attrib_ColorCoord, //    index of the vertex attribute that is to be modified
               3, //                      number of components per vertex attribute, must be 1, 2, 3, or 4
               GL.SHORT, //               data type of each component in the array
               false, //                  values should be normalized
               0, //                      offset in bytes between the beginning of consecutive vertex attributes
               0 //                       offset in bytes of the first component in the vertex attribute array
         );

         final int numDirArrowShorts = allRenderBuckets.numShortsForDirectionArrows;

         /*
          * Draw direction arrows
          */

//       final float width = 10 / vp2mpScale;
//
//       gl.uniform1f(shader_u_width, width * COORD_SCALE_BY_DIR_SCALE);

         // arrow colors
         final float arrowColors[] = trackConfig.getArrowColors();
         gl.uniform4fv(shader_uni_ArrowColors, arrowColors.length / 4, arrowColors, 0);

         // outline width's
         gl.uniform2f(shader_uni_OutlineWidth,
               trackConfig.arrowWing_OutlineWidth / 200f,
               trackConfig.arrowFin_OutlineWidth / 200f);

         gl.drawArrays(GL.TRIANGLES, 0, numDirArrowShorts);

         gl.depthMask(false);

         GLUtils.checkGlError(Renderer.class.getName());
      }

      static boolean init() {

// SET_FORMATTING_OFF

         _lineShaders[SHADER_PROJECTED]   = new LineShader("line_aa_proj");       //$NON-NLS-1$
         _lineShaders[SHADER_FLAT]        = new LineShader("line_aa");            //$NON-NLS-1$

         _directionArrowShader            = new DirectionArrowsShader("directionArrows");    //$NON-NLS-1$

// SET_FORMATTING_ON

         /*
          * create lookup table as texture for 'length(0..1,0..1)'
          * using mirrored wrap mode for 'length(-1..1,-1..1)'
          */
         final byte[] pixel = new byte[128 * 128];

         for (int x = 0; x < 128; x++) {
            final float xx = x * x;
            for (int y = 0; y < 128; y++) {
               final float yy = y * y;
               int color = (int) (Math.sqrt(xx + yy) * 2);
               if (color > 255) {
                  color = 255;
               }
               pixel[x + y * 128] = (byte) color;
            }
         }

         _textureID = GLUtils.loadTexture(pixel,
               128,
               128,
               GL.ALPHA,
               GL.NEAREST,
               GL.NEAREST,
               GL.MIRRORED_REPEAT,
               GL.MIRRORED_REPEAT);

         return true;
      }
   }

   LineBucketMT(final byte type, final boolean indexed, final boolean quads) {
      super(type, indexed, quads);
   }

   public LineBucketMT(final int layer) {

      super(RenderBucketMT.LINE, false, false);
      this.level = layer;
   }

   private void addDirArrowPosition(final short p2Xscaled,
                                    final short p2Yscaled,
                                    final short arrowZ,
                                    final short arrowPartWing,

                                    final int colorCoord1,
                                    final int colorCoord2,
                                    final int colorCoord3) {

      directionArrow_XYZPositions.add(p2Xscaled);
      directionArrow_XYZPositions.add(p2Yscaled);
      directionArrow_XYZPositions.add(arrowZ);
      directionArrow_XYZPositions.add(arrowPartWing);

      colorCoords.add((short) colorCoord1);
      colorCoords.add((short) colorCoord2);
      colorCoords.add((short) colorCoord3);
   }

   /**
    * This is called from {@link TourLayer#_simpleWorker}
    *
    * @param pixelPoints
    *           -2048 ... 2048
    * @param numPoints
    * @param isCapClosed
    * @param pixelPointColors
    *           One {@link #pixelPointColors} has two {@link #pixelPoints}
    */
   public void addLine(final float[] pixelPoints,
                       final int numPoints,
                       final boolean isCapClosed,
                       final int[] pixelPointColors) {

      if (numPoints >= 4) {
         addLine(pixelPoints, null, numPoints, isCapClosed, pixelPointColors);
      }
   }

   /**
    * @param pixelPoints
    *           -2048 ... 2048
    * @param index
    * @param numPoints
    * @param isCapClosed
    * @param pixelPointColors
    */
   void addLine(final float[] pixelPoints,
                final int[] index,
                final int numPoints,
                final boolean isCapClosed,
                final int[] pixelPointColors) {

      // test minimum distance
//      _minimumDistance = testValue * 2.0f;

      boolean isCapRounded = false;
      boolean isCapSquared = false;

      if (lineStyle.cap == Cap.ROUND) {
         isCapRounded = true;
      } else if (lineStyle.cap == Cap.SQUARE) {
         isCapSquared = true;
      }

      /*
       * Note: just a hack to save some vertices, when there are
       * more than 200 lines per type. FIXME make optional!
       */
      if (isCapRounded && index != null) {
         int cnt = 0;
         for (int i = 0, n = index.length; i < n; i++, cnt++) {
            if (index[i] < 0) {
               break;
            }
            if (cnt > 400) {
               isCapRounded = false;
               break;
            }
         }
      }
      _isCapRounded = isCapRounded;

      int numIndices;
      int numLinePoints = 0;

      if (index == null) {
         numIndices = 1;
         if (numPoints > 0) {
            numLinePoints = numPoints;
         } else {
            numLinePoints = pixelPoints.length;
         }
      } else {
         numIndices = index.length;
      }

      for (int indexIndex = 0, pos = 0; indexIndex < numIndices; indexIndex++) {

         if (index != null) {
            numLinePoints = index[indexIndex];
         }

         /* check end-marker in indices */
         if (numLinePoints < 0) {
            break;
         }

         final int startIndex = pos;
         pos += numLinePoints;

         /* need at least two points */
         if (numLinePoints < 4) {
            continue;
         }

         /* start and end point are equal */
         if (numLinePoints == 4 &&
               pixelPoints[startIndex] == pixelPoints[startIndex + 2] &&
               pixelPoints[startIndex + 1] == pixelPoints[startIndex + 3]) {

            continue;
         }

         /* avoid simple 180 degree angles */
         if (numLinePoints == 6 &&
               pixelPoints[startIndex] == pixelPoints[startIndex + 4] &&
               pixelPoints[startIndex + 1] == pixelPoints[startIndex + 5]) {

            numLinePoints -= 2;
         }

         addLine_ToVertices(

               vertexItems,

               pixelPoints,
               pixelPointColors,
               startIndex,
               numLinePoints,
               isCapRounded,
               isCapSquared,
               isCapClosed);
      }
   }

   public void addLine(final GeometryBuffer geom) {

//      if (geom.isPoly()) {
//
//         addLine(geom.points, geom.index, -1, true);
//
//      } else if (geom.isLine()) {
//
//         addLine(geom.points, geom.index, -1, false);
//
//      } else {
//
//         log.debug("geometry must be LINE or POLYGON");
//      }
   }

   /**
    * @param vertices
    * @param pixelPoints
    *           -2048 ... 2048
    * @param pixelPointColors
    * @param startIndex
    * @param numLinePoints
    * @param isRounded
    * @param isSquared
    * @param isClosed
    */
   private void addLine_ToVertices(final VertexDataMT vertices,
                                   final float[] pixelPoints,
                                   final int[] pixelPointColors,
                                   final int startIndex,
                                   final int numLinePoints,
                                   final boolean isRounded,
                                   final boolean isSquared,
                                   final boolean isClosed) {

      float ux, uy;
      float unit1X, unit1Y;
      float unit2X, unit2Y;
      float curX, curY;
      float nextX, nextY;
      double unitDistance;
      int pixelColor;

      /*
       * amount of vertices used
       * + 2 for drawing triangle-strip
       * + 4 for round caps
       * + 2 for closing polygons
       */
      numVertices += numLinePoints
            + (isRounded ? 6 : 2)
            + (isClosed ? 2 : 0);

      int pointIndex = startIndex;
      int pointIndexColor = startIndex / 2;

      curX = pixelPoints[pointIndex++];
      curY = pixelPoints[pointIndex++];
      pixelColor = pixelPointColors[pointIndexColor++];

      nextX = pixelPoints[pointIndex++];
      nextY = pixelPoints[pointIndex++];
      pixelColor = pixelPointColors[pointIndexColor++];

      // unit vector to next node
      unit1X = nextX - curX;
      unit1Y = nextY - curY;
      unitDistance = (float) Math.sqrt(unit1X * unit1X + unit1Y * unit1Y);
      unit1X /= unitDistance;
      unit1Y /= unitDistance;

      // perpendicular on the first segment
      ux = -unit1Y;
      uy = unit1X;

      int ddx, ddy;

      // vertex point coordinate
      short ox = (short) (curX * COORD_SCALE);
      short oy = (short) (curY * COORD_SCALE);

      // vertex extrusion vector, last two bit encode texture coord
      short dx, dy;

      // when the endpoint is outside the tile region omit round caps
      boolean isOutside = (curX < tmin || curX > tmax || curY < tmin || curY > tmax);

      if (isRounded && isOutside == false) {

         ddx = (int) ((ux - unit1X) * DIR_SCALE);
         ddy = (int) ((uy - unit1Y) * DIR_SCALE);
         dx = (short) (0 | ddx & DIR_MASK);
         dy = (short) (2 | ddy & DIR_MASK);

         vertices.add(ox, oy, dx, dy, pixelColor);
         vertices.add(ox, oy, dx, dy, pixelColor);

         ddx = (int) (-(ux + unit1X) * DIR_SCALE);
         ddy = (int) (-(uy + unit1Y) * DIR_SCALE);

         vertices.add(ox, oy, (short) (2 | ddx & DIR_MASK), (short) (2 | ddy & DIR_MASK), pixelColor);

         // start of line
         ddx = (int) (ux * DIR_SCALE);
         ddy = (int) (uy * DIR_SCALE);

         vertices.add(ox, oy, (short) (0 | ddx & DIR_MASK), (short) (1 | ddy & DIR_MASK), pixelColor);
         vertices.add(ox, oy, (short) (2 | -ddx & DIR_MASK), (short) (1 | -ddy & DIR_MASK), pixelColor);

      } else {

         /*
          * Outside means line is probably clipped
          * TODO should align ending with tile boundary
          * for now, just extend the line a little
          */

         float tx = unit1X;
         float ty = unit1Y;

         if (isRounded == false && isSquared == false) {

            tx = 0;
            ty = 0;

         } else if (isRounded) {

            tx *= 0.5;
            ty *= 0.5;
         }

         if (isRounded) {
            numVertices -= 2;
         }

         // add first vertex twice
         ddx = (int) ((ux - tx) * DIR_SCALE);
         ddy = (int) ((uy - ty) * DIR_SCALE);
         dx = (short) (0 | ddx & DIR_MASK);
         dy = (short) (1 | ddy & DIR_MASK);

         vertices.add(ox, oy, dx, dy, pixelColor);
         vertices.add(ox, oy, dx, dy, pixelColor);

         ddx = (int) (-(ux + tx) * DIR_SCALE);
         ddy = (int) (-(uy + ty) * DIR_SCALE);

         vertices.add(ox, oy, (short) (2 | ddx & DIR_MASK), (short) (1 | ddy & DIR_MASK), pixelColor);
      }

      curX = nextX;
      curY = nextY;

      // unit vector pointing back to previous node
      unit1X *= -1;
      unit1Y *= -1;

      for (final int endIndex = startIndex + numLinePoints;;) {

         if (pointIndex < endIndex) {

            nextX = pixelPoints[pointIndex++];
            nextY = pixelPoints[pointIndex++];

            pixelColor = pixelPointColors[pointIndexColor++];

         } else if (isClosed && pointIndex < endIndex + 2) {

            // close the loop -> the next point is back to the startpoint
            // (Original comment) add startpoint == endpoint

            nextX = pixelPoints[startIndex];
            nextY = pixelPoints[startIndex + 1];

            pointIndex += 2;
            pixelColor = pixelPointColors[pointIndexColor++];

         } else {

            break;
         }

         // unit vector pointing forward to next node
         unit2X = nextX - curX;
         unit2Y = nextY - curY;
         unitDistance = Math.sqrt(unit2X * unit2X + unit2Y * unit2Y);

         // skip too short segments
         if (unitDistance < _minimumDistance) {

            numVertices -= 2;

            continue;
         }

         unit2X /= unitDistance;
         unit2Y /= unitDistance;

         final double dotp = unit2X * unit1X + unit2Y * unit1Y;

         //log.debug("acos " + dotp);
         if (dotp > 0.65) {

            // add bevel join to avoid miter going to infinity
            numVertices += 2;

            // dotp = FastMath.clamp(dotp, -1, 1);
            // double cos = Math.acos(dotp);
            // log.debug("cos " + Math.toDegrees(cos));
            // log.debug("back " + (mMinDist * 2 / Math.sin(cos + Math.PI / 2)));

            float px, py;
            if (dotp > 0.999) {

               // 360 degree angle, set points aside
               ux = unit1X + unit2X;
               uy = unit1Y + unit2Y;

               unitDistance = unit2X * uy - unit2Y * ux;

               if (unitDistance < 0.1 && unitDistance > -0.1) {

                  // almost straight
                  ux = -unit2Y;
                  uy = unit2X;

               } else {

                  ux /= unitDistance;
                  uy /= unitDistance;
               }

               //log.debug("aside " + a + " " + ux + " " + uy);
               px = curX - ux * _minimumBevel;
               py = curY - uy * _minimumBevel;
               curX = curX + ux * _minimumBevel;
               curY = curY + uy * _minimumBevel;

            } else {

               //log.debug("back");

               // go back by min dist
               px = curX + unit1X * _minimumBevel;
               py = curY + unit1Y * _minimumBevel;

               // go forward by min dist
               curX = curX + unit2X * _minimumBevel;
               curY = curY + unit2Y * _minimumBevel;
            }

            // unit vector pointing forward to next node
            unit2X = curX - px;
            unit2Y = curY - py;
            unitDistance = Math.sqrt(unit2X * unit2X + unit2Y * unit2Y);
            unit2X /= unitDistance;
            unit2Y /= unitDistance;

            addVertex(vertices, px, py, unit1X, unit1Y, unit2X, unit2Y, pixelColor);

            // flip unit vector to point back
            unit1X = -unit2X;
            unit1Y = -unit2Y;

            // unit vector pointing forward to next node
            unit2X = nextX - curX;
            unit2Y = nextY - curY;
            unitDistance = Math.sqrt(unit2X * unit2X + unit2Y * unit2Y);
            unit2X /= unitDistance;
            unit2Y /= unitDistance;
         }

         addVertex(vertices, curX, curY, unit1X, unit1Y, unit2X, unit2Y, pixelColor);

         curX = nextX;
         curY = nextY;

         // flip vector to point back
         unit1X = -unit2X;
         unit1Y = -unit2Y;
      }

      ux = unit1Y;
      uy = -unit1X;

      isOutside = curX < tmin || curX > tmax || curY < tmin || curY > tmax;

      ox = (short) (curX * COORD_SCALE);
      oy = (short) (curY * COORD_SCALE);

      if (isRounded && isOutside == false) {

         // inside

         ddx = (int) (ux * DIR_SCALE);
         ddy = (int) (uy * DIR_SCALE);

         vertices.add(ox, oy, (short) (0 | ddx & DIR_MASK), (short) (1 | ddy & DIR_MASK), pixelColor);
         vertices.add(ox, oy, (short) (2 | -ddx & DIR_MASK), (short) (1 | -ddy & DIR_MASK), pixelColor);

         // for rounded line edges
         ddx = (int) ((ux - unit1X) * DIR_SCALE);
         ddy = (int) ((uy - unit1Y) * DIR_SCALE);

         vertices.add(ox, oy, (short) (0 | ddx & DIR_MASK), (short) (0 | ddy & DIR_MASK), pixelColor);

         // last vertex
         ddx = (int) (-(ux + unit1X) * DIR_SCALE);
         ddy = (int) (-(uy + unit1Y) * DIR_SCALE);
         dx = (short) (2 | ddx & DIR_MASK);
         dy = (short) (0 | ddy & DIR_MASK);

      } else {

         if (isRounded == false && isSquared == false) {

            unit1X = 0;
            unit1Y = 0;

         } else if (isRounded) {

            unit1X *= 0.5;
            unit1Y *= 0.5;
         }

         if (isRounded) {
            numVertices -= 2;
         }

         ddx = (int) ((ux - unit1X) * DIR_SCALE);
         ddy = (int) ((uy - unit1Y) * DIR_SCALE);

         vertices.add(ox, oy, (short) (0 | ddx & DIR_MASK), (short) (1 | ddy & DIR_MASK), pixelColor);

         // last vertex
         ddx = (int) (-(ux + unit1X) * DIR_SCALE);
         ddy = (int) (-(uy + unit1Y) * DIR_SCALE);
         dx = (short) (2 | ddx & DIR_MASK);
         dy = (short) (1 | ddy & DIR_MASK);
      }

      // add last vertex twice
      vertices.add(ox, oy, dx, dy, pixelColor);
      vertices.add(ox, oy, dx, dy, pixelColor);
   }

   public void addOutline(final LineBucketMT link) {

      for (LineBucketMT l = outlines; l != null; l = l.outlines) {
         if (link == l) {
            return;
         }
      }

      link.outlines = outlines;
      outlines = link;
   }

   /**
    * Adds 2 vertices
    *
    * @param vertexData
    * @param x
    * @param y
    * @param unitNextX
    * @param unitNextY
    * @param unitPrevX
    * @param unitPrevY
    * @param pixelColor
    */
   private void addVertex(final VertexDataMT vertexData,
                          final float x,
                          final float y,
                          final float unitNextX,
                          final float unitNextY,
                          final float unitPrevX,
                          final float unitPrevY,
                          final int pixelColor) {

      float ux = unitNextX + unitPrevX;
      float uy = unitNextY + unitPrevY;

      // vPrev times perpendicular of sum(unitNext, unitPrev)
      final double a = uy * unitPrevX - ux * unitPrevY;

      if (a < 0.01 && a > -0.01) {
         ux = -unitPrevY;
         uy = unitPrevX;
      } else {
         ux /= a;
         uy /= a;
      }

      final short ox = (short) (x * COORD_SCALE);
      final short oy = (short) (y * COORD_SCALE);

      final int ddx = (int) (ux * DIR_SCALE);
      final int ddy = (int) (uy * DIR_SCALE);

// SET_FORMATTING_OFF

      vertexData.add(ox, oy, (short) (0 |  ddx & DIR_MASK), (short) (1 |  ddy & DIR_MASK), pixelColor);
      vertexData.add(ox, oy, (short) (2 | -ddx & DIR_MASK), (short) (1 | -ddy & DIR_MASK), pixelColor);

// SET_FORMATTING_ON
   }

   private void createArrow_MiddleFin(final short p2X_scaled,
                                      final short p2Y_scaled,
                                      final short pBackX_scaled,
                                      final short pBackY_scaled,
                                      final short pOnLineX_scaled,
                                      final short pOnLineY_scaled,
                                      final short arrowZ,
                                      final short finTopZ,
                                      final short arrowPart_Fin) {
// SET_FORMATTING_OFF

      // fin: middle
      addDirArrowPosition(p2X_scaled,        p2Y_scaled,       arrowZ,     arrowPart_Fin, 1, 1, 0);
      addDirArrowPosition(pOnLineX_scaled,   pOnLineY_scaled,  finTopZ,    arrowPart_Fin, 0, 1, 0);
      addDirArrowPosition(pBackX_scaled,     pBackY_scaled,    arrowZ,     arrowPart_Fin, 0, 0, 1);

// SET_FORMATTING_ON
   }

   private void createArrow_OuterFins(final short p2X_scaled,
                                      final short p2Y_scaled,
                                      final short pLeftX_scaled,
                                      final short pLeftY_scaled,
                                      final short pRight_Xscaled,
                                      final short pRightY_scaled,
                                      final short arrowZ,
                                      final short finBottomZ,
                                      final short arrowPart_Fin) {
// SET_FORMATTING_OFF

      // fin: left
      addDirArrowPosition(p2X_scaled,       p2Y_scaled,     arrowZ,     arrowPart_Fin, 1, 0, 0);
      addDirArrowPosition(pLeftX_scaled,    pLeftY_scaled,  arrowZ,     arrowPart_Fin, 0, 1, 0);
      addDirArrowPosition(pLeftX_scaled,    pLeftY_scaled,  finBottomZ, arrowPart_Fin, 0, 0, 1);

      // fin: right
      addDirArrowPosition(p2X_scaled,       p2Y_scaled,     arrowZ,     arrowPart_Fin, 1, 0, 0);
      addDirArrowPosition(pRight_Xscaled,   pRightY_scaled, arrowZ,     arrowPart_Fin, 0, 1, 0);
      addDirArrowPosition(pRight_Xscaled,   pRightY_scaled, finBottomZ, arrowPart_Fin, 0, 0, 1);

// SET_FORMATTING_ON
   }

   private void createArrow_Wings(final short p2X_scaled,
                                  final short p2Y_scaled,
                                  final short pLeftX_scaled,
                                  final short pLeftY_scaled,
                                  final short pRight_Xscaled,
                                  final short pRightY_scaled,
                                  final short pBackX_scaled,
                                  final short pBackY_scaled,
                                  final short arrowZ,
                                  final short arrowPart_Wing) {
// SET_FORMATTING_OFF

      // wing: left
      addDirArrowPosition(p2X_scaled,       p2Y_scaled,     arrowZ,     arrowPart_Wing, 1, 0, 0);
      addDirArrowPosition(pBackX_scaled,    pBackY_scaled,  arrowZ,     arrowPart_Wing, 0, 1, 1);
      addDirArrowPosition(pLeftX_scaled,    pLeftY_scaled,  arrowZ,     arrowPart_Wing, 0, 0, 1);

      // wing: right
      addDirArrowPosition(p2X_scaled,       p2Y_scaled,     arrowZ,     arrowPart_Wing, 1, 0, 0);
      addDirArrowPosition(pBackX_scaled,    pBackY_scaled,  arrowZ,     arrowPart_Wing, 0, 1, 1);
      addDirArrowPosition(pRight_Xscaled,   pRightY_scaled, arrowZ,     arrowPart_Wing, 0, 0, 1);

// SET_FORMATTING_ON
   }

   /**
    * @param allDirectionArrowPixel_Raw
    *           Contains the x/y pixel positions for the direction arrows
    */
   public void createDirectionArrowVertices(final TFloatArrayList allDirectionArrowPixel_Raw) {

      directionArrow_XYZPositions.clearQuick();
      colorCoords.clearQuick();

      // at least 2 positions are needed
      if (allDirectionArrowPixel_Raw.size() < 4) {
         return;
      }

      final Map25TrackConfig trackConfig = Map25ConfigManager.getActiveTourTrackConfig();

      final float[] allDirectionArrowPixel = allDirectionArrowPixel_Raw.toArray();

// SET_FORMATTING_OFF

      final float configArrowScale           = trackConfig.arrow_Scale / 10f;
      final int   configArrowLength          = (int) (configArrowScale * trackConfig.arrow_Length);
      final int   configArrowLengthCenter    = (int) (configArrowScale * trackConfig.arrow_LengthCenter);
      final int   configArrowWidth           = (int) (configArrowScale * trackConfig.arrow_Width);
      final int   configArrowHeight          = (int) (configArrowScale * trackConfig.arrow_Height);

      final short arrowZ      = (short) trackConfig.arrow_VerticalOffset;
      final short finTopZ     = (short) (arrowZ + configArrowHeight);
      final short finBottomZ  = (short) (arrowZ - configArrowHeight);

// SET_FORMATTING_ON

      int pixelIndex = 0;

      float p1X = allDirectionArrowPixel[pixelIndex++];
      float p1Y = allDirectionArrowPixel[pixelIndex++];

      for (; pixelIndex < allDirectionArrowPixel.length;) {

         final float p2X = allDirectionArrowPixel[pixelIndex++];
         final float p2Y = allDirectionArrowPixel[pixelIndex++];

         // get direction/unit vector: dir = (P2-P1)/|P2-P1|
         final float diffX = p2X - p1X;
         final float diffY = p2Y - p1Y;

         // distance between P1 and P2
         final double p12Distance = Math.sqrt(diffX * diffX + diffY * diffY);

         final double p12UnitX = diffX / p12Distance;
         final double p12UnitY = diffY / p12Distance;

         // get perpendicular vector for the arrow head
         final double unitPerpendX = p12UnitY;
         final double unitPerpendY = -p12UnitX;

         final double arrowLength = Math.min(configArrowLength, p12Distance);
         final double arrowLengthMiddle = Math.min(configArrowLengthCenter, p12Distance);
         final double arrowWidth2 = configArrowWidth / 2; // half arrow width

         // point on line between P1 and P2
         final double pOnLineX = p2X - (arrowLength * p12UnitX);
         final double pOnLineY = p2Y - (arrowLength * p12UnitY);
         final double pBackX = p2X - (arrowLengthMiddle * p12UnitX);
         final double pBackY = p2Y - (arrowLengthMiddle * p12UnitY);

         final double vFinX = unitPerpendX * arrowWidth2;
         final double vFinY = unitPerpendY * arrowWidth2;

         // set arrow points which are left/right of the line
         final float pLeftX = (float) (pOnLineX + vFinX);
         final float pLeftY = (float) (pOnLineY + vFinY);

         final float pRightX = (float) (pOnLineX - vFinX);
         final float pRightY = (float) (pOnLineY - vFinY);

         /**
          * <code>
          *
          * WING
          *
          *                 Pleft
          *                     #---\
          *                      -   ---\
          *                     . -      ---\
          *                        -         ---\
          *                     .   -            ---\
          *                          -               ---\
          *                     .     -                  ---\
          *                            -                     ---\
          *    P1  #------------#-------#------------------------# P2
          *                 PonLine    - Pback               ---/
          *                     .     -                  ---/
          *                          -               ---/
          *                     .   -            ---/
          *                        -         ---/
          *                     . -      ---/
          *                      -   ---/
          *                     #---/
          *                 Pright
          *
          * FIN
          *
          *                 ZfinTop
          *                     #---\
          *                          ---\
          *                     .        ---\
          *                                  ---\
          *                     .                ---\
          *                                          ---\
          *                     .                        ---\
          *                                                  ---\
          *    P1  #------------#--------------------------------# P2
          *                 PonLine                          ---/
          *                     .                        ---/
          *                                          ---/
          *                     .                ---/
          *                                  ---/
          *                     .        ---/
          *                          ---/
          *                     #---/
          *                 ZfinBottom
          * </code>
          */

         /*
          * Set arrow positions
          */

// SET_FORMATTING_OFF

         final short arrowPart_Wing    = 0;
         final short arrowPart_Fin     = 1;

         final short p2X_scaled        = (short) (p2X       * COORD_SCALE);
         final short p2Y_scaled        = (short) (p2Y       * COORD_SCALE);
         final short pLeftX_scaled     = (short) (pLeftX    * COORD_SCALE);
         final short pLeftY_scaled     = (short) (pLeftY    * COORD_SCALE);
         final short pRight_Xscaled    = (short) (pRightX   * COORD_SCALE);
         final short pRightY_scaled    = (short) (pRightY   * COORD_SCALE);
         final short pBackX_scaled     = (short) (pBackX    * COORD_SCALE);
         final short pBackY_scaled     = (short) (pBackY    * COORD_SCALE);
         final short pOnLineX_scaled   = (short) (pOnLineX  * COORD_SCALE);
         final short pOnLineY_scaled   = (short) (pOnLineY  * COORD_SCALE);


         /**
          * !!! VERY IMPORTANT !!! <p>
          *
          * THE ORDER, TO FIX Z-FIGHTING
          */

         switch (trackConfig.arrow_Design) {

         case WINGS_WITH_MIDDLE_FIN:

            createArrow_Wings(      p2X_scaled,       p2Y_scaled,
                                    pLeftX_scaled,    pLeftY_scaled,
                                    pRight_Xscaled,   pRightY_scaled,
                                    pBackX_scaled,    pBackY_scaled,
                                    arrowZ,
                                    arrowPart_Wing);

            createArrow_MiddleFin(  p2X_scaled,       p2Y_scaled,
                                    pBackX_scaled,    pBackY_scaled,
                                    pOnLineX_scaled,  pOnLineY_scaled,
                                    arrowZ,
                                    finTopZ,
                                    arrowPart_Fin);

            break;

         case WINGS_WITH_OUTER_FINS:

            createArrow_Wings(      p2X_scaled,       p2Y_scaled,
                                    pLeftX_scaled,    pLeftY_scaled,
                                    pRight_Xscaled,   pRightY_scaled,
                                    pBackX_scaled,    pBackY_scaled,
                                    arrowZ,
                                    arrowPart_Wing);

            createArrow_OuterFins(  p2X_scaled,       p2Y_scaled,
                                    pLeftX_scaled,    pLeftY_scaled,
                                    pRight_Xscaled,   pRightY_scaled,
                                    arrowZ,
                                    finBottomZ,
                                    arrowPart_Fin);

            break;

         case MIDDLE_FIN:

            createArrow_MiddleFin(  p2X_scaled,       p2Y_scaled,
                                    pBackX_scaled,    pBackY_scaled,
                                    pOnLineX_scaled,  pOnLineY_scaled,
                                    arrowZ,
                                    finTopZ,
                                    arrowPart_Fin);
            break;

         case OUTER_FINS:

            createArrow_OuterFins(  p2X_scaled,       p2Y_scaled,
                                    pLeftX_scaled,    pLeftY_scaled,
                                    pRight_Xscaled,   pRightY_scaled,
                                    arrowZ,
                                    finBottomZ,
                                    arrowPart_Fin);

            break;

         case WINGS:
         default:
            createArrow_Wings(      p2X_scaled,       p2Y_scaled,
                                    pLeftX_scaled,    pLeftY_scaled,
                                    pRight_Xscaled,   pRightY_scaled,
                                    pBackX_scaled,    pBackY_scaled,
                                    arrowZ,
                                    arrowPart_Wing);
            break;
         }

// SET_FORMATTING_ON

         // setup next position
         p1X = p2X;
         p1Y = p2Y;
      }
   }

   /**
    * Default is MIN_DIST * 4 = 1/8 * 4.
    */
   public void setBevelDistance(final float minBevel) {
      _minimumBevel = minBevel;
   }

   /**
    * For point reduction by minimal distance. Default is 1/8.
    */
   public void setDropDistance(final float minDist) {
      _minimumDistance = minDist;
   }

   public void setExtents(final int min, final int max) {
      tmin = min;
      tmax = max;

   }
}
