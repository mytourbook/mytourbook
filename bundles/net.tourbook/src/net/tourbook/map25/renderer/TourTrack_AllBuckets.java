/**
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

import gnu.trove.list.array.TShortArrayList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import org.oscim.backend.GL;
import org.oscim.layers.tile.MapTile.TileData;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.MapRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is primarily intended for rendering the vector elements of a
 * MapTile. It can be used for other purposes as well but some optimizations
 * (and limitations) probably wont make sense in different contexts.
 */
public class TourTrack_AllBuckets extends TileData {

   private static final Logger log                            = LoggerFactory.getLogger(TourTrack_AllBuckets.class);

   /**
    * Number of bytes for a <b>short</b> value
    */
   private static final int    SHORT_BYTES                    = 2;

   private TourTrack_Bucket    _trackBucket;
   private TourTrack_Bucket    _anotherTrackBucket;

   /**
    * VBO holds all vertex data to draw lines and polygons after compilation.
    * <p>
    * Layout:
    * <ul>
    * <li>16 bytes fill coordinates:
    * {@link #TILE_FILL_VERTICES} * {@link #SHORT_BYTES} * coordsPerVertex
    * <li>n bytes polygon vertices
    * <li>m bytes lines vertices
    * ...
    */
   public BufferObject         vbo_BufferObject;

   int                         vertexColor_BufferId           = Integer.MIN_VALUE;
   int                         dirArrows_BufferId             = Integer.MIN_VALUE;
   int                         dirArrows_ColorCoords_BufferId = Integer.MIN_VALUE;

   private ByteBuffer          _vertexColor_Buffer;
   private int                 _vertexColor_BufferSize;

   /**
    * Number of {@link Short}'s used for the direction arrows
    */
   int                         numShortsForDirectionArrows;

   /**
    * Number of {@link Short}'s used for the color coordinates
    */
   private int                 numShortsForColorCoords;

   public TourTrack_AllBuckets() {}

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

      // NB: set null calls clear() on each bucket!
      set(null);
      _anotherTrackBucket = null;

      vbo_BufferObject = BufferObject.release(vbo_BufferObject);
   }

   /**
    * Compile different types of buckets in one {@link #vbo_BufferObject VBO}.
    *
    * @param addFill
    *           Fill tile (add {@link #TILE_FILL_VERTICES 4} vertices).
    * @return true if compilation succeeded.
    */
   public boolean compile() {

      final int vboSize = getNumberOfVbo();
      if (vboSize <= 0) {

         vbo_BufferObject = BufferObject.release(vbo_BufferObject);

         return false;
      }

      final int vertexColor_Size = vboSize;
      numShortsForDirectionArrows = getNumberOfShortsForDirectionArrows();
      numShortsForColorCoords = getNumberOfShortsForColorCoords();

      final ShortBuffer vertices_ShortBuffer = MapRenderer.getShortBuffer(vboSize);
      final ShortBuffer directionArrow_ShortBuffer = MapRenderer.getShortBuffer(numShortsForDirectionArrows);
      final ShortBuffer colorCoords_ShortBuffer = MapRenderer.getShortBuffer(numShortsForColorCoords);
      final ByteBuffer colorBuffer = getBuffer_Color(vertexColor_Size);

      /*
       * Compile lines
       */

      _trackBucket.compile(vertices_ShortBuffer, colorBuffer);
      _trackBucket.vertexOffset = 0;

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
      {
         // set direction arrow shorts into the buffer
         final TShortArrayList directionArrowVertices = _trackBucket.directionArrow_XYZPositions;
         final int numBucketShorts = directionArrowVertices.size();
         directionArrow_ShortBuffer.put(directionArrowVertices.toArray(), 0, numBucketShorts);

         if (dirArrows_BufferId == Integer.MIN_VALUE) {

            // create buffer id
            dirArrows_BufferId = gl.genBuffer();
         }
         gl.bindBuffer(GL.ARRAY_BUFFER, dirArrows_BufferId);
         gl.bufferData(GL.ARRAY_BUFFER, numShortsForDirectionArrows * SHORT_BYTES, directionArrow_ShortBuffer.flip(), GL.STATIC_DRAW);

         // append color coord shorts into the buffer
         final TShortArrayList colorCoordsVertices = _trackBucket.colorCoords;
         final int numColorBucketShorts = colorCoordsVertices.size();
         colorCoords_ShortBuffer.put(colorCoordsVertices.toArray(), 0, numColorBucketShorts);

         if (dirArrows_ColorCoords_BufferId == Integer.MIN_VALUE) {

            // create buffer id
            dirArrows_ColorCoords_BufferId = gl.genBuffer();
         }
         gl.bindBuffer(GL.ARRAY_BUFFER, dirArrows_ColorCoords_BufferId);
         gl.bufferData(GL.ARRAY_BUFFER, numShortsForColorCoords * SHORT_BYTES, colorCoords_ShortBuffer.flip(), GL.STATIC_DRAW);
      }

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
      {
         if (vbo_BufferObject == null) {
            vbo_BufferObject = BufferObject.get(GL.ARRAY_BUFFER, vboSize);
         }

         // Set VBO data to READ mode
         // - gl.bindBuffer()
         // - gl.bufferData()
         vbo_BufferObject.loadBufferData(vertices_ShortBuffer.flip(), vboSize * SHORT_BYTES);
      }

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
      return _trackBucket;
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

   TourTrack_Bucket getLineBucket() {

      TourTrack_Bucket trackBucket = null;

      if (_anotherTrackBucket != null) {

         trackBucket = _anotherTrackBucket;

         return trackBucket;
      }

      TourTrack_Bucket chainedBucked = _trackBucket;

      if (chainedBucked == null) {

         // insert new bucket at start
         chainedBucked = null;

      } else {

         if (_anotherTrackBucket != null) {
            chainedBucked = _anotherTrackBucket;
         }
      }

      if (trackBucket == null) {

         trackBucket = new TourTrack_Bucket();

         if (chainedBucked == null) {

            // insert at start

            trackBucket.next = _trackBucket;
            _trackBucket = trackBucket;

         } else {

            trackBucket.next = chainedBucked.next;
            chainedBucked.next = trackBucket;
         }
      }

      _anotherTrackBucket = trackBucket;

      return trackBucket;
   }

   /**
    * @return Returns number of {@link Short}'s for the color coordinates
    */
   private int getNumberOfShortsForColorCoords() {

      return _trackBucket == null

            ? 0
            : _trackBucket.colorCoords.size();
   }

   /**
    * @return Returns number of {@link Short}'s for the direction arrows
    */
   private int getNumberOfShortsForDirectionArrows() {

      return _trackBucket == null

            ? 0
            : _trackBucket.directionArrow_XYZPositions.size();
   }

   private int getNumberOfVbo() {

      return _trackBucket == null
            ? 0
            : _trackBucket.numVertices * 4;
   }

   /**
    * Set new bucket items and clear previous.
    */
   public void set(final TourTrack_Bucket newBucket) {

      for (TourTrack_Bucket previousBucket = _trackBucket; previousBucket != null; previousBucket = previousBucket.next) {
         previousBucket.clear();
      }

      _trackBucket = newBucket;
   }

}
