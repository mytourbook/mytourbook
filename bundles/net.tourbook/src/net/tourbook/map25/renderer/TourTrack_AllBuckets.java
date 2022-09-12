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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.oscim.backend.GL;
import org.oscim.renderer.MapRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is primarily intended for rendering the vector elements of a
 * MapTile. It can be used for other purposes as well but some optimizations
 * (and limitations) probably wont make sense in different contexts.
 */
public class TourTrack_AllBuckets {

   private static final Logger log         = LoggerFactory.getLogger(TourTrack_AllBuckets.class);

   /**
    * Number of bytes for a <b>short</b> value
    */
   private static final int    SHORT_BYTES = 2;

   private TourTrack_Bucket    _trackBucket;
   private TourTrack_Bucket    _anotherTrackBucket;

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

   public TourTrack_AllBuckets() {

   }

   /**
    * cleanup only when buckets are not used by tile or bucket anymore!
    */
   public void clear() {

      // NB: set null calls clear() on each bucket!
      set(null);
      _anotherTrackBucket = null;
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

            _trackBucket = trackBucket;
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
    * Set new bucket and clear previous
    */
   public void set(final TourTrack_Bucket newBucket) {

      final TourTrack_Bucket previousBucket = _trackBucket;

      if (previousBucket != null) {
         previousBucket.clear();
      }

      _trackBucket = newBucket;
   }

   /**
    * Compile different types of buckets in one {@link #vbo_BufferObject VBO}.
    *
    * @param addFill
    *           Fill tile (add {@link #TILE_FILL_VERTICES 4} vertices).
    * @return true if compilation succeeded.
    */
   public boolean setOpenGLBufferData() {

      final int numVertices = getNumberOfVbo();
      if (numVertices <= 0) {
         return false;
      }

      final int vertexColor_Size = numVertices;
      numShortsForDirectionArrows = getNumberOfShortsForDirectionArrows();
      numShortsForColorCoords = getNumberOfShortsForColorCoords();

      final ShortBuffer vertices_ShortBuffer = MapRenderer.getShortBuffer(numVertices);
      final ShortBuffer directionArrow_ShortBuffer = MapRenderer.getShortBuffer(numShortsForDirectionArrows);
      final ShortBuffer colorCoords_ShortBuffer = MapRenderer.getShortBuffer(numShortsForColorCoords);
      final ByteBuffer colorBuffer = getBuffer_Color(vertexColor_Size);

      /*
       * Compile lines
       */

      _trackBucket.fillVerticesBuffer(vertices_ShortBuffer, colorBuffer);

      if (numVertices != vertices_ShortBuffer.position()) {

         log.debug("wrong vertex buffer size: " //$NON-NLS-1$
               + " new size: " + numVertices //$NON-NLS-1$
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
         final ShortArrayList directionArrowVertices = _trackBucket.directionArrow_XYZPositions;
         final int numBucketShorts = directionArrowVertices.size();
         directionArrow_ShortBuffer.put(directionArrowVertices.toArray(), 0, numBucketShorts);

         gl.bindBuffer(GL.ARRAY_BUFFER, TourTrack_Shader.bufferId_DirArrows);
         gl.bufferData(GL.ARRAY_BUFFER, numShortsForDirectionArrows * SHORT_BYTES, directionArrow_ShortBuffer.flip(), GL.STATIC_DRAW);

         // append color coord shorts into the buffer
         final ShortArrayList colorCoordsVertices = _trackBucket.colorCoords;
         final int numColorBucketShorts = colorCoordsVertices.size();
         colorCoords_ShortBuffer.put(colorCoordsVertices.toArray(), 0, numColorBucketShorts);

         gl.bindBuffer(GL.ARRAY_BUFFER, TourTrack_Shader.bufferId_DirArrows_ColorCoords);
         gl.bufferData(GL.ARRAY_BUFFER, numShortsForColorCoords * SHORT_BYTES, colorCoords_ShortBuffer.flip(), GL.STATIC_DRAW);
      }

      /*
       * Track vertices
       */
      {
         /**
          * Load vertex color into the GPU
          * <p>
          * VERY IMPORTANT
          * <p>
          * BUFFER MUST BE BINDED BEFORE VBO otherwise the map is mostly covered with the vertex
          * color
          * !!!
          */
         gl.bindBuffer(GL.ARRAY_BUFFER, TourTrack_Shader.bufferId_VerticesColor);
         gl.bufferData(GL.ARRAY_BUFFER, vertexColor_Size, _vertexColor_Buffer.flip(), GL.STATIC_DRAW);

         gl.bindBuffer(GL.ARRAY_BUFFER, TourTrack_Shader.bufferId_Vertices);
         gl.bufferData(GL.ARRAY_BUFFER, numVertices * SHORT_BYTES, vertices_ShortBuffer.flip(), GL.STATIC_DRAW);
      }

      return true;
   }

}
