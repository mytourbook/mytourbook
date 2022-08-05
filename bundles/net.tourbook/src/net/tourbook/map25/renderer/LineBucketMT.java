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
    * mask for packing last two bits of extrusion vector with texture
    * coordinates
    */
   private static final int   DIR_MASK         = 0xFFFFFFFC;

   /**
    * Lines referenced by this outline layer
    */
   public LineBucketMT        outlines;

   public LineStyle           line;

   public boolean             isShowOutline;
   public int                 lineColorMode;
   public float               outlineBrightness;
   public float               outlineWidth;

   public float               testValue;

   public float               scale            = 1;

   private boolean            _isCapRounded;
   private float              _minimumDistance = MIN_DIST;
   private float              _minimumBevel    = MIN_BEVEL;

   private float              _heightOffset;

   private int                tmin             = Integer.MIN_VALUE, tmax = Integer.MAX_VALUE;

   public static final class Renderer {

      /*
       * TODO:
       * http://http.developer.nvidia.com/GPUGems2/gpugems2_chapter22.html
       */

      /**
       * Factor to normalize extrusion vector and scale to coord scale
       */
      private static final float COORD_SCALE_BY_DIR_SCALE = COORD_SCALE / DIR_SCALE;

      private static final int   CAP_THIN                 = 0;
      private static final int   CAP_BUTT                 = 1;
      private static final int   CAP_ROUND                = 2;

      private static final int   SHADER_PROJECTED         = 0;
      private static final int   SHADER_FLAT              = 1;

      private static int         _textureID;
      private static Shader[]    _shaders                 = { null, null };

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

         final Shader shader = _shaders[shaderMode];

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
         gl.bindBuffer(GL.ARRAY_BUFFER, renderBucketsAll.vertexColorId);
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
            final LineStyle lineStyle = lineBucket.line.current();

            final float scale = lineBucket.scale;

            final boolean isPaintOutline = lineBucket.isShowOutline;
            final float outlineWidth = lineBucket.outlineWidth;
            final float outlineBrightnessRaw = lineBucket.outlineBrightness; // -1.0 ... 1.0
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

         gl.disableVertexAttribArray(shader_aVertexColor);

         return renderBucket;
      }

      static boolean init() {

         _shaders[SHADER_PROJECTED] = new Shader("line_aa_proj"); //$NON-NLS-1$
         _shaders[SHADER_FLAT] = new Shader("line_aa"); //$NON-NLS-1$

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

   private static class Shader extends GLShaderMT {

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

      Shader(final String shaderFile) {

         if (createMT(shaderFile) == false) {
            return;
         }

         shader_a_pos = getAttrib("a_pos"); //$NON-NLS-1$
         shader_aVertexColor = getAttrib("aVertexColor"); //$NON-NLS-1$

         shader_u_mvp = getUniform("u_mvp"); //$NON-NLS-1$

         shader_u_fade = getUniform("u_fade"); //$NON-NLS-1$
         shader_u_color = getUniform("u_color"); //$NON-NLS-1$
         shader_u_mode = getUniform("u_mode"); //$NON-NLS-1$

         shader_u_width = getUniform("u_width"); //$NON-NLS-1$
         shader_u_height = getUniform("u_height"); //$NON-NLS-1$

         shader_uColorMode = getUniform("uColorMode"); //$NON-NLS-1$
         shader_uOutlineBrightness = getUniform("uOutlineBrightness"); //$NON-NLS-1$
         shader_uVertexColorAlpha = getUniform("uVertexColorAlpha"); //$NON-NLS-1$
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

   LineBucketMT(final byte type, final boolean indexed, final boolean quads) {
      super(type, indexed, quads);
   }

   public LineBucketMT(final int layer) {

      super(RenderBucketMT.LINE, false, false);
      this.level = layer;
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

      if (line.cap == Cap.ROUND) {
         isCapRounded = true;
      } else if (line.cap == Cap.SQUARE) {
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
      float vPrevX, vPrevY;
      float vNextX, vNextY;
      float curX, curY;
      float nextX, nextY;
      double xyDistance;
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
      vPrevX = nextX - curX;
      vPrevY = nextY - curY;
      xyDistance = (float) Math.sqrt(vPrevX * vPrevX + vPrevY * vPrevY);
      vPrevX /= xyDistance;
      vPrevY /= xyDistance;

      // perpendicular on the first segment
      ux = -vPrevY;
      uy = vPrevX;

      int ddx, ddy;

      // vertex point coordinate
      short ox = (short) (curX * COORD_SCALE);
      short oy = (short) (curY * COORD_SCALE);

      // vertex extrusion vector, last two bit encode texture coord
      short dx, dy;

      // when the endpoint is outside the tile region omit round caps
      boolean isOutside = (curX < tmin || curX > tmax || curY < tmin || curY > tmax);

      if (isRounded && isOutside == false) {

         ddx = (int) ((ux - vPrevX) * DIR_SCALE);
         ddy = (int) ((uy - vPrevY) * DIR_SCALE);
         dx = (short) (0 | ddx & DIR_MASK);
         dy = (short) (2 | ddy & DIR_MASK);

         vertices.add(ox, oy, dx, dy, pixelColor);
         vertices.add(ox, oy, dx, dy, pixelColor);

         ddx = (int) (-(ux + vPrevX) * DIR_SCALE);
         ddy = (int) (-(uy + vPrevY) * DIR_SCALE);

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

         float tx = vPrevX;
         float ty = vPrevY;

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
      vPrevX *= -1;
      vPrevY *= -1;

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
         vNextX = nextX - curX;
         vNextY = nextY - curY;
         xyDistance = Math.sqrt(vNextX * vNextX + vNextY * vNextY);

         // skip too short segments
         if (xyDistance < _minimumDistance) {

            numVertices -= 2;

            continue;
         }

         vNextX /= xyDistance;
         vNextY /= xyDistance;

         final double dotp = (vNextX * vPrevX + vNextY * vPrevY);

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
               ux = vPrevX + vNextX;
               uy = vPrevY + vNextY;

               xyDistance = vNextX * uy - vNextY * ux;

               if (xyDistance < 0.1 && xyDistance > -0.1) {

                  // almost straight
                  ux = -vNextY;
                  uy = vNextX;

               } else {

                  ux /= xyDistance;
                  uy /= xyDistance;
               }

               //log.debug("aside " + a + " " + ux + " " + uy);
               px = curX - ux * _minimumBevel;
               py = curY - uy * _minimumBevel;
               curX = curX + ux * _minimumBevel;
               curY = curY + uy * _minimumBevel;

            } else {

               //log.debug("back");

               // go back by min dist
               px = curX + vPrevX * _minimumBevel;
               py = curY + vPrevY * _minimumBevel;

               // go forward by min dist
               curX = curX + vNextX * _minimumBevel;
               curY = curY + vNextY * _minimumBevel;
            }

            // unit vector pointing forward to next node
            vNextX = curX - px;
            vNextY = curY - py;
            xyDistance = Math.sqrt(vNextX * vNextX + vNextY * vNextY);
            vNextX /= xyDistance;
            vNextY /= xyDistance;

            addVertex(vertices, px, py, vPrevX, vPrevY, vNextX, vNextY, pixelColor);

            // flip unit vector to point back
            vPrevX = -vNextX;
            vPrevY = -vNextY;

            // unit vector pointing forward to next node
            vNextX = nextX - curX;
            vNextY = nextY - curY;
            xyDistance = Math.sqrt(vNextX * vNextX + vNextY * vNextY);
            vNextX /= xyDistance;
            vNextY /= xyDistance;
         }

         addVertex(vertices, curX, curY, vPrevX, vPrevY, vNextX, vNextY, pixelColor);

         curX = nextX;
         curY = nextY;

         // flip vector to point back
         vPrevX = -vNextX;
         vPrevY = -vNextY;
      }

      ux = vPrevY;
      uy = -vPrevX;

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
         ddx = (int) ((ux - vPrevX) * DIR_SCALE);
         ddy = (int) ((uy - vPrevY) * DIR_SCALE);

         vertices.add(ox, oy, (short) (0 | ddx & DIR_MASK), (short) (0 | ddy & DIR_MASK), pixelColor);

         // last vertex
         ddx = (int) (-(ux + vPrevX) * DIR_SCALE);
         ddy = (int) (-(uy + vPrevY) * DIR_SCALE);
         dx = (short) (2 | ddx & DIR_MASK);
         dy = (short) (0 | ddy & DIR_MASK);

      } else {

         if (isRounded == false && isSquared == false) {

            vPrevX = 0;
            vPrevY = 0;

         } else if (isRounded) {

            vPrevX *= 0.5;
            vPrevY *= 0.5;
         }

         if (isRounded) {
            numVertices -= 2;
         }

         ddx = (int) ((ux - vPrevX) * DIR_SCALE);
         ddy = (int) ((uy - vPrevY) * DIR_SCALE);

         vertices.add(ox, oy, (short) (0 | ddx & DIR_MASK), (short) (1 | ddy & DIR_MASK), pixelColor);

         // last vertex
         ddx = (int) (-(ux + vPrevX) * DIR_SCALE);
         ddy = (int) (-(uy + vPrevY) * DIR_SCALE);
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
    * @param vNextX
    * @param vNextY
    * @param vPrevX
    * @param vPrevY
    * @param pixelColor
    */
   private void addVertex(final VertexDataMT vertexData,
                          final float x,
                          final float y,
                          final float vNextX,
                          final float vNextY,
                          final float vPrevX,
                          final float vPrevY,
                          final int pixelColor) {

      float ux = vNextX + vPrevX;
      float uy = vNextY + vPrevY;

      // vPrev times perpendicular of sum(vNext, vPrev)
      final double a = uy * vPrevX - ux * vPrevY;

      if (a < 0.01 && a > -0.01) {
         ux = -vPrevY;
         uy = vPrevX;
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
