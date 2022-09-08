/*
 * Copyright 2012-2014 Hannes Janetzek
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
import static org.oscim.renderer.bucket.RenderBucket.LINE;

import gnu.trove.list.array.TShortArrayList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import org.oscim.backend.GL;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile.TileData;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.styles.LineStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is primarily intended for rendering the vector elements of a
 * MapTile. It can be used for other purposes as well but some optimizations
 * (and limitations) probably wont make sense in different contexts.
 */
public class TourTrack_AllBuckets extends TileData {

   static final Logger log = LoggerFactory.getLogger(TourTrack_AllBuckets.class);

   /* Count of units needed for one vertex */
   public static final int[] VERTEX_CNT  =

         {
               4,                            // LINE_VERTEX
               6,                            // TEXLINE_VERTEX
               2,                            // POLY_VERTEX
               2,                            // MESH_VERTEX
               4,                            // EXTRUSION_VERTEX
               2,                            // HAIRLINE_VERTEX
               6,                            // SYMBOL
               6,                            // BITMAP
               2,                            // CIRCLE
         };

   /**
    * Number of bytes for a <b>short</b> value
    */
   public static final int   SHORT_BYTES = 2;

// public static final int INT_BYTES = 4;

   /**
    * Number of vertices to fill a tile (represented by a quad).
    */
   public static final int TILE_FILL_VERTICES = 4;

   private static short[]  fillShortCoords;

   static {

      final short s = (short) (Tile.SIZE * COORD_SCALE);

      fillShortCoords = new short[] { 0, s, s, s, 0, 0, s, 0 };
   }

   private TourTrack_Bucket _firstChainedBucket;

   /**
    * VBO holds all vertex data to draw lines and polygons after compilation.
    * <p>
    * Layout:
    * - 16 bytes fill coordinates:
    * {@link #TILE_FILL_VERTICES} * {@link #SHORT_BYTES} * coordsPerVertex
    * - n bytes polygon vertices
    * - m bytes lines vertices
    * ...
    */
   public BufferObject      vbo_BufferObject;

   /**
    * OpenGL id for the vertex colors
    */

   /**
    * To not need to switch VertexAttribPointer positions all the time:
    * <p>
    * <li>1. polygons are packed in VBO at offset 0</li>
    * <li>2. lines afterwards at lineOffset</li>
    * <li>3. other buckets keep their byte offset in offset</li>
    */
   public int[]             offset                         = { 0, 0 };

   private TourTrack_Bucket _currentBucket;

   int                      vertexColor_BufferId           = Integer.MIN_VALUE;
   int                      dirArrows_BufferId             = Integer.MIN_VALUE;
   int                      dirArrows_ColorCoords_BufferId = Integer.MIN_VALUE;

   private ByteBuffer       _vertexColor_Buffer;
   private int              _vertexColor_BufferSize;

   /**
    * Number of {@link Short}'s used for the direction arrows
    */
   int                      numShortsForDirectionArrows;

   /**
    * Number of {@link Short}'s used for the color coordinates
    */
   private int              numShortsForColorCoords;

   public TourTrack_AllBuckets() {}

   public static void initRenderer() {

      TourTrack_Shader.init();
   }

   /**
    * add the LineBucket for a level with a given Line style. Levels are
    * ordered from bottom (0) to top
    */
   public TourTrack_Bucket addLineBucket(final int level, final LineStyle style) {

      final TourTrack_Bucket lineBucket = getBucket(level, LINE);

      if (lineBucket == null) {
         return null;
      }

      // FIXME l.scale = style.width;
      lineBucket.scale = 1;
      lineBucket.lineStyle = style;

      return lineBucket;
   }

   /**
    * Binds vbo and ibo
    */
   public void bind() {

      if (vbo_BufferObject != null) {
         vbo_BufferObject.bind();
      }
   }

   /**
    * cleanup only when buckets are not used by tile or bucket anymore!
    */
   public void clear() {

      /* NB: set null calls clear() on each bucket! */
      set(null);
      _currentBucket = null;

      vbo_BufferObject = BufferObject.release(vbo_BufferObject);
   }

   /**
    * cleanup only when buckets are not used by tile or bucket anymore!
    */
   public void clearBuckets() {

      /* NB: set null calls clear() on each bucket! */
      for (TourTrack_Bucket l = _firstChainedBucket; l != null; l = l.next) {
         l.clear();
      }

      _currentBucket = null;
   }

   /**
    * Compile different types of buckets in one {@link #vbo_BufferObject VBO}.
    *
    * @param addFill
    *           Fill tile (add {@link #TILE_FILL_VERTICES 4} vertices).
    * @return true if compilation succeeded.
    */
   public boolean compile() {

      int vboSize = getVboSize();
      if (vboSize <= 0) {

         vbo_BufferObject = BufferObject.release(vbo_BufferObject);

         return false;
      }

      vboSize += TILE_FILL_VERTICES * 2;

      final int vertexColor_Size = vboSize;
      numShortsForDirectionArrows = getNumberOfShortsForDirectionArrows();
      numShortsForColorCoords = getNumberOfShortsForColorCoords();

      final ShortBuffer vertices_ShortBuffer = MapRenderer.getShortBuffer(vboSize);
      final ShortBuffer directionArrow_ShortBuffer = MapRenderer.getShortBuffer(numShortsForDirectionArrows);
      final ShortBuffer colorCoords_ShortBuffer = MapRenderer.getShortBuffer(numShortsForColorCoords);
      final ByteBuffer colorBuffer = getBuffer_Color(vertexColor_Size);

      vertices_ShortBuffer.put(fillShortCoords, 0, TILE_FILL_VERTICES * 2);

      /*
       * Compile lines
       */
      offset[LINE] = vertices_ShortBuffer.position() * SHORT_BYTES;

      {
         _firstChainedBucket.compile(vertices_ShortBuffer, colorBuffer);
         _firstChainedBucket.vertexOffset = 0;

      }

      if (vboSize != vertices_ShortBuffer.position()) {

         log.debug("wrong vertex buffer size: " //$NON-NLS-1$
               + " new size: " + vboSize //$NON-NLS-1$
               + " buffer pos: " + vertices_ShortBuffer.position() //$NON-NLS-1$
               + " buffer limit: " + vertices_ShortBuffer.limit() //$NON-NLS-1$
               + " buffer fill: " + vertices_ShortBuffer.remaining()); //$NON-NLS-1$

         return false;
      }

      /*
       * Direction arrows
       */

      // set direction arrow shorts into the buffer
      final TShortArrayList directionArrowVertices = _firstChainedBucket.directionArrow_XYZPositions;
      final int numBucketShorts = directionArrowVertices.size();
      directionArrow_ShortBuffer.put(directionArrowVertices.toArray(), 0, numBucketShorts);

      if (dirArrows_BufferId == Integer.MIN_VALUE) {

         // create buffer id
         dirArrows_BufferId = gl.genBuffer();
      }
      gl.bindBuffer(GL.ARRAY_BUFFER, dirArrows_BufferId);
      gl.bufferData(GL.ARRAY_BUFFER, numShortsForDirectionArrows * SHORT_BYTES, directionArrow_ShortBuffer.flip(), GL.STATIC_DRAW);

      // append color coord shorts into the buffer
      final TShortArrayList colorCoordsVertices = _firstChainedBucket.colorCoords;
      final int numColorBucketShorts = colorCoordsVertices.size();
      colorCoords_ShortBuffer.put(colorCoordsVertices.toArray(), 0, numColorBucketShorts);

      if (dirArrows_ColorCoords_BufferId == Integer.MIN_VALUE) {

         // create buffer id
         dirArrows_ColorCoords_BufferId = gl.genBuffer();
      }
      gl.bindBuffer(GL.ARRAY_BUFFER, dirArrows_ColorCoords_BufferId);
      gl.bufferData(GL.ARRAY_BUFFER, numShortsForColorCoords * SHORT_BYTES, colorCoords_ShortBuffer.flip(), GL.STATIC_DRAW);

      /**
       * Load vertex color into the GPU
       * <p>
       * VERY IMPORTANT
       * <p>
       * BUFFER MUST BE BINDED BEFORE VBO/IBO otherwise the map is mostly covered with the vertex
       * color !!!
       */
      if (vertexColor_BufferId == Integer.MIN_VALUE) {

         // create buffer id
         vertexColor_BufferId = gl.genBuffer();
      }

      gl.bindBuffer(GL.ARRAY_BUFFER, vertexColor_BufferId);
      gl.bufferData(GL.ARRAY_BUFFER, vertexColor_Size, _vertexColor_Buffer.flip(), GL.STATIC_DRAW);

      /*
       * VBO
       */
      if (vbo_BufferObject == null) {
         vbo_BufferObject = BufferObject.get(GL.ARRAY_BUFFER, vboSize);
      }

      // Set VBO data to READ mode
      // - gl.bindBuffer()
      // - gl.bufferData()
      vbo_BufferObject.loadBufferData(vertices_ShortBuffer.flip(), vboSize * SHORT_BYTES);

      return true;
   }

   @Override
   protected void dispose() {
      clear();
   }

   /**
    * @return internal linked list of RenderBucket items
    */
   public TourTrack_Bucket get() {
      return _firstChainedBucket;
   }

   private TourTrack_Bucket getBucket(final int level, final int type) {

      TourTrack_Bucket typedBucket = null;

      if (_currentBucket != null && _currentBucket.verticalOrder == level) {

         typedBucket = _currentBucket;

         if (typedBucket.bucketType != type) {
            log.error("BUG wrong bucket {} {} on level {}", typedBucket.bucketType, type, level); //$NON-NLS-1$
            throw new IllegalArgumentException();
         }

         return typedBucket;
      }

      TourTrack_Bucket chainedBucked = _firstChainedBucket;
      if (chainedBucked == null || chainedBucked.verticalOrder > level) {
         /* insert new bucket at start */
         chainedBucked = null;
      } else {
         if (_currentBucket != null && level > _currentBucket.verticalOrder) {
            chainedBucked = _currentBucket;
         }

         while (true) {

            /* found bucket */
            if (chainedBucked.verticalOrder == level) {
               typedBucket = chainedBucked;
               break;
            }

            /* insert bucket between current and next bucket */
            if (chainedBucked.next == null || chainedBucked.next.verticalOrder > level) {
               break;
            }

            chainedBucked = chainedBucked.next;
         }
      }

      if (typedBucket == null) {

         // add a new RenderElement
         if (type == LINE) {

            typedBucket = new TourTrack_Bucket(level);
         }

         if (typedBucket == null) {
            throw new IllegalArgumentException();
         }

         if (chainedBucked == null) {

            /** insert at start */
            typedBucket.next = _firstChainedBucket;
            _firstChainedBucket = typedBucket;

         } else {

            typedBucket.next = chainedBucked.next;
            chainedBucked.next = typedBucket;
         }
      }

      /* check if found buckets matches requested type */
      if (typedBucket.bucketType != type) {
         log.error("BUG wrong bucket {} {} on level {}", typedBucket.bucketType, type, level); //$NON-NLS-1$
         throw new IllegalArgumentException();
      }

      _currentBucket = typedBucket;

      return typedBucket;
   }

   private ByteBuffer getBuffer_Color(final int requestedColorSize) {

      final int bufferBlockSize = 2048;
      final int numBufferBlocks = requestedColorSize / bufferBlockSize;
      final int roundedBufferSize = (numBufferBlocks + 1) * bufferBlockSize;

      if (_vertexColor_Buffer == null || _vertexColor_BufferSize < roundedBufferSize) {

         _vertexColor_Buffer = ByteBuffer
               .allocateDirect(roundedBufferSize)
               .order(ByteOrder.nativeOrder());

         _vertexColor_BufferSize = roundedBufferSize;

      } else {

         // IMPORTANT: reset position to 0 to prevent BufferOverflowException

         _vertexColor_Buffer.clear();
      }

      return _vertexColor_Buffer;
   }

   /**
    * Get or add the LineBucket for a level. Levels are ordered from
    * bottom (0) to top
    */
   public TourTrack_Bucket getLineBucket(final int level) {
      return getBucket(level, LINE);
   }

   /**
    * @return Returns number of {@link Short}'s for the color coordinates
    */
   private int getNumberOfShortsForColorCoords() {

      return _firstChainedBucket == null

            ? 0
            : _firstChainedBucket.colorCoords.size();
   }

   /**
    * @return Returns number of {@link Short}'s for the direction arrows
    */
   private int getNumberOfShortsForDirectionArrows() {

      return _firstChainedBucket == null

            ? 0
            : _firstChainedBucket.directionArrow_XYZPositions.size();
   }

   private int getVboSize() {

      return _firstChainedBucket == null
            ? 0
            : _firstChainedBucket.numVertices * VERTEX_CNT[_firstChainedBucket.bucketType];
   }

   /**
    * Set new bucket items and clear previous.
    */
   public void set(final TourTrack_Bucket newBuckets) {

      for (TourTrack_Bucket previousBucket = _firstChainedBucket; previousBucket != null; previousBucket = previousBucket.next) {
         previousBucket.clear();
      }

      _firstChainedBucket = newBuckets;
   }

}
