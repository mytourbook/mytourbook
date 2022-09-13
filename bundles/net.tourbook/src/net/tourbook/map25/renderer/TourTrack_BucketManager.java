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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.oscim.backend.GL;
import org.oscim.renderer.MapRenderer;

/**
 * Original class: org.oscim.renderer.bucket.RenderBuckets
 */
public class TourTrack_BucketManager {

   /**
    * Number of bytes for a <b>short</b> value
    */
   private static final int SHORT_BYTES = 2;

   private TourTrack_Bucket _trackBucket_Painter;
   private TourTrack_Bucket _trackBucket_Worker;

   private ByteBuffer       _vertexColor_Buffer;
   private int              _vertexColor_BufferSize;

   /**
    * Number of {@link Short}'s used for the direction arrows
    */
   int                      numShortsForDirectionArrows;


   public TourTrack_BucketManager() {

   }

   /**
    * Cleanup only when buckets are not used by tile or bucket anymore!
    */
   public void clear() {

      setBucket_Painter(null);
      _trackBucket_Worker = null;
   }

   /**
    * Fill OpenGL buffer with the vertices/color/direction arrow data.
    *
    * @return Returns <code>true</code> when data are available
    */
   public boolean fillOpenGLBufferData() {

      final int numVertices = _trackBucket_Painter == null ? 0 : _trackBucket_Painter.numVertices * 4;
      if (numVertices <= 0) {
         return false;
      }

      final int numShortsForColorCoords = _trackBucket_Painter.directionArrow_colorCoords.size();
      numShortsForDirectionArrows = _trackBucket_Painter.directionArrow_XYZPositions.size();

// SET_FORMATTING_OFF

      final ShortBuffer vertices_ShortBuffer       = MapRenderer.getShortBuffer(numVertices);
      final ShortBuffer directionArrow_ShortBuffer = MapRenderer.getShortBuffer(numShortsForDirectionArrows);
      final ShortBuffer colorCoords_ShortBuffer    = MapRenderer.getShortBuffer(numShortsForColorCoords);
      final ByteBuffer  colorBuffer                = getBuffer_Color(numVertices);

// SET_FORMATTING_ON

      _trackBucket_Painter.fillVerticesBuffer(vertices_ShortBuffer, colorBuffer);

      /*
       * Direction arrows
       */
      {
         // set direction arrow shorts into the buffer
         final ShortArrayList directionArrowVertices = _trackBucket_Painter.directionArrow_XYZPositions;
         final int numBucketShorts = directionArrowVertices.size();
         directionArrow_ShortBuffer.put(directionArrowVertices.toArray(), 0, numBucketShorts);

         gl.bindBuffer(GL.ARRAY_BUFFER, TourTrack_Shader.bufferId_DirArrows);
         gl.bufferData(GL.ARRAY_BUFFER, numShortsForDirectionArrows * SHORT_BYTES, directionArrow_ShortBuffer.flip(), GL.STATIC_DRAW);

         // append color coord shorts into the buffer
         final ShortArrayList colorCoordsVertices = _trackBucket_Painter.directionArrow_colorCoords;
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
         gl.bufferData(GL.ARRAY_BUFFER, numVertices, _vertexColor_Buffer.flip(), GL.STATIC_DRAW);

         gl.bindBuffer(GL.ARRAY_BUFFER, TourTrack_Shader.bufferId_Vertices);
         gl.bufferData(GL.ARRAY_BUFFER, numVertices * SHORT_BYTES, vertices_ShortBuffer.flip(), GL.STATIC_DRAW);
      }

      return true;
   }

   TourTrack_Bucket getBucket_Painter() {

      return _trackBucket_Painter;
   }

   TourTrack_Bucket getBucket_Worker() {

      TourTrack_Bucket trackBucket = null;

      if (_trackBucket_Worker != null) {

         trackBucket = _trackBucket_Worker;

         return trackBucket;
      }

      TourTrack_Bucket chainedBucked = _trackBucket_Painter;

      if (chainedBucked == null) {

         // insert new bucket at start
         chainedBucked = null;

      } else {

         if (_trackBucket_Worker != null) {
            chainedBucked = _trackBucket_Worker;
         }
      }

      if (trackBucket == null) {

         trackBucket = new TourTrack_Bucket();

         if (chainedBucked == null) {

            // insert at start

            _trackBucket_Painter = trackBucket;
         }
      }

      _trackBucket_Worker = trackBucket;

      return trackBucket;
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
    * Set new bucket and clear previous
    */
   public void setBucket_Painter(final TourTrack_Bucket newBucket) {

      final TourTrack_Bucket previousPainterBucket = _trackBucket_Painter;

      if (previousPainterBucket != null) {
         previousPainterBucket.clear();
      }

      _trackBucket_Painter = newBucket;
   }

}
