/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016-2021 devemux86
 * Copyright 2017 Longri
 * Copyright 2018 Gustl22
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
package net.tourbook.map25.renderer;

import static org.oscim.backend.GLAdapter.gl;
import static org.oscim.renderer.MapRenderer.COORD_SCALE;
import static org.oscim.renderer.MapRenderer.MAX_INDICES;
import static org.oscim.renderer.MapRenderer.bindQuadIndicesVBO;
import static org.oscim.renderer.bucket.RenderBucket.TEXLINE;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import org.oscim.backend.GL;
import org.oscim.core.GeometryBuffer;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.bucket.LineBucket;
import org.oscim.theme.styles.LineStyle;
import org.oscim.utils.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RenderElement for textured or stippled lines
 * <p>
 * Interleave two segment quads in one block to be able to use
 * vertices twice. pos0 and pos1 use the same vertex array where
 * pos1 has an offset of one vertex. The vertex shader will use
 * pos0 when the vertexId is even, pos1 when the Id is odd.
 * <p>
 * As there is no gl_VertexId in gles 2.0 an additional 'flip'
 * array is used. Depending on 'flip' extrusion is inverted.
 * <p/>
 * Indices and flip buffers can be static.
 * <p>
 *
 * <pre>
 * First pass: using even vertex array positions (used vertices are in braces)
 *
 * vertex id   0  1  2  3  4  5  6  7
 * pos0     x (0) 1 (2) 3 (4) 5 (6) 7 x
 * pos1     x (0) 1 (2) 3 (4) 5 (6) 7 x
 * flip        0  1  0  1  0  1  0  1
 *
 * Second pass: using odd vertex array positions
 *
 * vertex id   0  1  2  3  4  5  6  7
 * pos0      x 0 (1) 2 (3) 4 (5) 6 (7) x
 * pos1      x 0 (1) 2 (3) 4 (5) 6 (7) x
 * flip        0  1  0  1  0  1  0  1
 * </pre>
 * <p>
 * Vertex layout (here: 1 unit == 1 short):
 * <p>
 * [2 unit] position,<br>
 * [2 unit] extrusion,<br>
 * [1 unit] line length<br>
 * [1 unit] unused<br>
 * <p>
 * Indices, for two blocks:
 * 0, 1, 2,
 * 2, 1, 3,
 * 4, 5, 6,
 * 6, 5, 7,
 * <p>
 * <b>BIG NOTE:</b> The renderer assumes to be able to offset vertex array position
 * so that in the first pass 'pos1' offset will be < 0 if no data precedes<br>
 * - in our case there is always the polygon fill array at start<br>
 * - see addLine hack otherwise.
 */
public final class LineTexBucketMT extends LineBucketMT {

   static final Logger log         = LoggerFactory.getLogger(LineTexBucketMT.class);

   public int          evenQuads;
   public int          oddQuads;

   private boolean     evenSegment = true;
   private boolean     mTexRepeat  = true;

   public static final class Renderer {

      private static Shader shader;

      /* factor to normalize extrusion vector and scale to coord scale */
      private static final float COORD_SCALE_BY_DIR_SCALE = COORD_SCALE / LineBucket.DIR_SCALE;

      private static int         mVertexFlipID;

      /* posX, posY, extrX, extrY, length, unused */
      private static final int STRIDE = 6 * RenderBucketsAllMT.SHORT_BYTES;

      //static TextureItem tex;

      /* offset for line length, unused; skip first 4 units */
      private static final int LEN_OFFSET = 4 * RenderBucketsAllMT.SHORT_BYTES;

      public static RenderBucketMT draw(RenderBucketMT renderBucket,
                                        final GLViewport viewport,
                                        final float div,
                                        final RenderBucketsAllMT buckets) {

         GLState.blend(true);
         shader.useProgram();

         GLState.enableVertexArrays(GLState.DISABLED, GLState.DISABLED);

         final int shader_aLen0 = shader.aLen0;
         final int shader_aLen1 = shader.aLen1;
         final int shader_aPos0 = shader.aPos0;
         final int shader_aPos1 = shader.aPos1;
         final int shader_aFlip = shader.aFlip;

         gl.enableVertexAttribArray(shader_aPos0);
         gl.enableVertexAttribArray(shader_aPos1);
         gl.enableVertexAttribArray(shader_aLen0);
         gl.enableVertexAttribArray(shader_aLen1);
         gl.enableVertexAttribArray(shader_aFlip);

         viewport.mvp.setAsUniform(shader.uMVP);

         bindQuadIndicesVBO();

         GLState.bindVertexBuffer(mVertexFlipID);
         gl.vertexAttribPointer(shader.aFlip,
               1,
               GL.BYTE,
               false,
               0,
               0);

         buckets.vbo_BufferObject.bind();

         final float mapPositionScale = (float) viewport.pos.getZoomScale();
         final float s = mapPositionScale / div;

         for (; renderBucket != null && renderBucket.type == TEXLINE; renderBucket = renderBucket.next) {

            final LineTexBucketMT lineTexBucket = (LineTexBucketMT) renderBucket;
            final LineStyle line = lineTexBucket.line.current();

            gl.uniform1i(shader.uMode, line.dashArray != null ? 2 : (line.texture != null ? 1 : 0));

            if (line.texture != null) {
               line.texture.bind();
            }

            GLUtils.setColor(shader.uColor, line.stippleColor, 1);
            GLUtils.setColor(shader.uBgColor, line.color, 1);

            float pScale;
            if (s >= 1) {
               pScale = line.stipple * s;
               final float cnt = pScale / line.stipple;
               pScale = line.stipple / (cnt + 1);
            } else {
               pScale = line.stipple / s;
               final float cnt = pScale / line.stipple;
               pScale = line.stipple * cnt;
            }

            //log.debug("pScale {} {}", pScale, s);

            gl.uniform1f(shader.uPatternScale, COORD_SCALE * pScale);

            gl.uniform1f(shader.uPatternWidth, line.stippleWidth);

            /* keep line width fixed */
            gl.uniform1f(shader.uWidth, (lineTexBucket.scale * line.width) / s * COORD_SCALE_BY_DIR_SCALE);

            /* add offset vertex */
            final int vOffset = -STRIDE;

            // TODO interleave 1. and 2. pass to improve vertex cache usage?
            /* first pass */
            int allIndices = (lineTexBucket.evenQuads * 6);
            for (int i = 0; i < allIndices; i += MAX_INDICES) {

               int numIndices = allIndices - i;
               if (numIndices > MAX_INDICES) {
                  numIndices = MAX_INDICES;
               }

               /* i * (24 units per block / 6) * unit bytes) */
               final int add = (renderBucket.vertexOffset + i * 4 * RenderBucketsAllMT.SHORT_BYTES) + vOffset;

               gl.vertexAttribPointer(
                     shader_aPos0,
                     4,
                     GL.SHORT,
                     false,
                     STRIDE,
                     add + STRIDE);

               gl.vertexAttribPointer(
                     shader_aLen0,
                     2,
                     GL.SHORT,
                     false,
                     STRIDE,
                     add + STRIDE + LEN_OFFSET);

               gl.vertexAttribPointer(
                     shader_aPos1,
                     4,
                     GL.SHORT,
                     false,
                     STRIDE,
                     add);

               gl.vertexAttribPointer(
                     shader_aLen1,
                     2,
                     GL.SHORT,
                     false,
                     STRIDE,
                     add + LEN_OFFSET);

               gl.drawElements(GL.TRIANGLES, numIndices, GL.UNSIGNED_SHORT, 0);
            }

            /* second pass */
            allIndices = (lineTexBucket.oddQuads * 6);

            for (int i = 0; i < allIndices; i += MAX_INDICES) {

               int numIndices = allIndices - i;
               if (numIndices > MAX_INDICES) {
                  numIndices = MAX_INDICES;
               }

               /* i * (24 units per block / 6) * unit bytes) */
               final int add = (renderBucket.vertexOffset + i * 4 * RenderBucketsAllMT.SHORT_BYTES) + vOffset;

               gl.vertexAttribPointer(shader_aPos0,
                     4,
                     GL.SHORT,
                     false,
                     STRIDE,
                     add + 2 * STRIDE);

               gl.vertexAttribPointer(shader_aLen0,
                     2,
                     GL.SHORT,
                     false,
                     STRIDE,
                     add + 2 * STRIDE + LEN_OFFSET);

               gl.vertexAttribPointer(shader_aPos1,
                     4,
                     GL.SHORT,
                     false,
                     STRIDE,
                     add + STRIDE);

               gl.vertexAttribPointer(shader_aLen1,
                     2,
                     GL.SHORT,
                     false,
                     STRIDE,
                     add + STRIDE + LEN_OFFSET);

               gl.drawElements(GL.TRIANGLES, numIndices, GL.UNSIGNED_SHORT, 0);
            }
         }

         gl.disableVertexAttribArray(shader_aPos0);
         gl.disableVertexAttribArray(shader_aPos1);
         gl.disableVertexAttribArray(shader_aLen0);
         gl.disableVertexAttribArray(shader_aLen1);
         gl.disableVertexAttribArray(shader_aFlip);

         return renderBucket;
      }

      public static void init() {

         shader = new Shader("linetex_layer_tex"); //$NON-NLS-1$
//       shader = new Shader("linetex_layer");

         final int[] vboIds = GLUtils.glGenBuffers(1);
         mVertexFlipID = vboIds[0];

         /* bytes: 0, 1, 0, 1, 0, ... */
         final byte[] flip = new byte[MapRenderer.MAX_QUADS * 4];
         for (int i = 0; i < flip.length; i++) {
            flip[i] = (byte) (i % 2);
         }

         final ByteBuffer buf = ByteBuffer.allocateDirect(flip.length).order(ByteOrder.nativeOrder());
         buf.put(flip);
         buf.flip();

         final ShortBuffer sbuf = buf.asShortBuffer();

         //GL.bindBuffer(GL20.ARRAY_BUFFER, mVertexFlipID);
         GLState.bindVertexBuffer(mVertexFlipID);
         gl.bufferData(GL.ARRAY_BUFFER, flip.length, sbuf, GL.STATIC_DRAW);
         GLState.bindVertexBuffer(GLState.UNBIND);

         //    mTexID = new int[10];
         //    byte[] stipple = new byte[40];
         //    stipple[0] = 32;
         //    stipple[1] = 32;
         //    mTexID[0] = loadStippleTexture(stipple);

         //tex = new TextureItem(CanvasAdapter.getBitmapAsset("patterns/arrow.png"));
         //tex.mipmap = true;
      }

      public static int loadStippleTexture(final byte[] stipple) {

         int sum = 0;
         for (final byte flip : stipple) {
            sum += flip;
         }

         final byte[] pixel = new byte[sum];

         boolean on = true;
         int pos = 0;
         for (final byte flip : stipple) {
            final float max = flip;

            for (int s = 0; s < flip; s++) {
               float alpha = Math.abs(s / (max - 1) - 0.5f);
               if (on) {
                  alpha = 255 * (1 - alpha);
               } else {
                  alpha = 255 * alpha;
               }

               pixel[pos + s] = FastMath.clampToByte((int) alpha);
            }
            on = !on;
            pos += flip;
         }

         return GLUtils.loadTexture(pixel,
               sum,
               1,
               GL.ALPHA,
               GL.LINEAR,
               GL.LINEAR,
               GL.REPEAT,
               GL.REPEAT);
      }
   }

   static class Shader extends GLShaderMT {

      int uMVP, uColor, uWidth, uBgColor, uMode;
      int uPatternWidth, uPatternScale;
      int aPos0, aPos1, aLen0, aLen1, aFlip;

      Shader(final String shaderFile) {

         if (!createMT(shaderFile)) {
            return;
         }

         uMVP = getUniform("u_mvp"); //$NON-NLS-1$

         uColor = getUniform("u_color"); //$NON-NLS-1$
         uWidth = getUniform("u_width"); //$NON-NLS-1$
         uBgColor = getUniform("u_bgcolor"); //$NON-NLS-1$
         uMode = getUniform("u_mode"); //$NON-NLS-1$

         uPatternWidth = getUniform("u_pwidth"); //$NON-NLS-1$
         uPatternScale = getUniform("u_pscale"); //$NON-NLS-1$

         aPos0 = getAttrib("a_pos0"); // posX, posY, extrX, extrY //$NON-NLS-1$
         aPos1 = getAttrib("a_pos1"); //$NON-NLS-1$
         aLen0 = getAttrib("a_len0"); // line length, unused //$NON-NLS-1$
         aLen1 = getAttrib("a_len1"); //$NON-NLS-1$
         aFlip = getAttrib("a_flip"); //$NON-NLS-1$
      }
   }

   LineTexBucketMT(final int level) {

      super(TEXLINE, false, true);

      this.level = level;
      this.evenSegment = true;
   }

   @Override
   void addLine(final float[] pixelPoints,
                final int[] index,
                final int numPoints,
                final boolean isCapClosed,
                final int[] pixelPointColors) {

      if (vertexItems.empty()) {
         /*
          * The additional end vertex to make sure not to read outside
          * allocated memory
          */
         numVertices = 1;
      }
      final VertexDataMT vi = vertexItems;

      /* reset offset to last written position */
      if (!evenSegment) {
         vi.seek(-12);
      }

      int n;
      int length = 0;

      if (index == null) {
         n = 1;
         length = numPoints;
      } else {
         n = index.length;
      }

      for (int indexIndex = 0, pointIndex = 0; indexIndex < n; indexIndex++) {

         if (index != null) {
            length = index[indexIndex];
         }

         /* check end-marker in indices */
         if (length < 0) {
            break;
         }

         /* need at least two points */
         if (length < 4) {
            pointIndex += length;
            continue;
         }

         final int end = pointIndex + length;
         float x = pixelPoints[pointIndex++] * COORD_SCALE;
         float y = pixelPoints[pointIndex++] * COORD_SCALE;

         /* randomize a bit (must be within range of +/- Short.MAX_VALUE) */
         float lineLength = line.randomOffset ? (x * x + y * y) % 80 : 0;

         while (pointIndex < end) {
            final float nx = pixelPoints[pointIndex++] * COORD_SCALE;
            final float ny = pixelPoints[pointIndex++] * COORD_SCALE;

            /* Calculate triangle corners for the given width */
            final float vx = nx - x;
            final float vy = ny - y;

            //    /* normalize vector */
            final double dist = Math.sqrt(vx * vx + vy * vy);
            //    vx /= dist;
            //    vy /= dist;

            /* normalized perpendicular to line segment */
            final short dx = (short) ((-vy / dist) * DIR_SCALE);
            final short dy = (short) ((vx / dist) * DIR_SCALE);

            if (lineLength + dist > Short.MAX_VALUE) {
               lineLength = Short.MIN_VALUE; // reset lineLength (would cause minimal shift)
            }

            if (dist > (Short.MAX_VALUE - Short.MIN_VALUE)) {

               // In rare cases sloping lines are larger than max range of short:
               // sqrt(x² + y²) > short range. So need to split them in 2 parts.
               // Alternatively can set max clip value to:
               // (Short.MAX_VALUE / Math.sqrt(2)) / MapRenderer.COORD_SCALE
               final float ix = (x + (vx / 2));
               final float iy = (y + (vy / 2));

               addShortVertex(vi,
                     (short) x,
                     (short) y,
                     (short) ix,
                     (short) iy,
                     dx,
                     dy,
                     (short) lineLength,
                     (int) (dist / 2));

               addShortVertex(vi,
                     (short) ix,
                     (short) iy,
                     (short) nx,
                     (short) ny,
                     dx,
                     dy,
                     (short) lineLength,
                     (int) (dist / 2));

            } else {

               addShortVertex(vi,
                     (short) x,
                     (short) y,
                     (short) nx,
                     (short) ny,
                     dx,
                     dy,
                     (short) lineLength,
                     (int) dist);

               if (mTexRepeat) {
                  lineLength += dist;
               }
            }
            x = nx;
            y = ny;
         }
      }

      /* advance offset to last written position */
      if (!evenSegment) {
         vi.seek(12);
      }
   }

   @Override
   public void addLine(final GeometryBuffer geom) {

      addLine(geom.points, geom.index, -1, false, null);
   }

   private void addShortVertex(final VertexDataMT vi,
                               final short x,
                               final short y,
                               final short nx,
                               final short ny,
                               final short dx,
                               final short dy,
                               final short lineLength,
                               final int dist) {

      vi.add(x, y, dx, dy, lineLength, (short) 0);

      vi.seek(6);
      vi.add(nx, ny, dx, dy, (short) (lineLength + dist), (short) 0);

      if (evenSegment) {

         /* go to second segment */
         vi.seek(-12);
         evenSegment = false;

         /* vertex 0 and 2 were added */
         numVertices += 3;
         evenQuads++;

      } else {

         /* go to next block */
         evenSegment = true;

         /* vertex 1 and 3 were added */
         numVertices += 1;
         oddQuads++;
      }
   }

   @Override
   protected void clear() {
      evenSegment = true;
      evenQuads = 0;
      oddQuads = 0;
      super.clear();
   }

   @Override
   protected void compile(final ShortBuffer vboData, final ShortBuffer iboData, final ByteBuffer colorBuffer) {

      compileVertexItems(vboData, null);

      /* add additional vertex for interleaving, see TexLineLayer. */
      vboData.position(vboData.position() + 6);
   }

   public void setTexRepeat(final boolean texRepeat) {
      mTexRepeat = texRepeat;
   }
}
