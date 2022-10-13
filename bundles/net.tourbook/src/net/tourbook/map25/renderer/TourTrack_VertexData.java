/*
 * Copyright 2012 Hannes Janetzek
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

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import net.tourbook.map25.renderer.TourTrack_VertexData.Chunk;

import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.SyncPool;

/**
 * A linked list of array chunks to hold temporary vertex data.
 */
public class TourTrack_VertexData extends Inlist.List<Chunk> {

   /**
    * Size of array chunks. Must be multiple of:
    * 4 (LineLayer/PolygonLayer),
    * 24 (TexLineLayer - one block, i.e. two segments)
    * 24 (TextureLayer)
    */
   public static final int   SIZE                  = 10_000;

   /**
    * Shared chunk pool size.
    */
   private static final int  MAX_POOL              = 500;

   private static final Pool pool                  = new Pool();

   /**
    * Set SIZE to get new item on add
    */
   private int               _numUsedChunkVertices = SIZE;
   private int               _numUsedChunkColors;

   private Chunk             _currentChunk;
   private short[]           _currentChunk_Vertices;
   private byte[]            _currentChunk_Colors;

   public static class Chunk extends Inlist<Chunk> {

      public final short[] __chunkVertices = new short[SIZE];
      public final byte[]  __chunkColors   = new byte[SIZE];

      public int           __numChunkUsedVertices;
      public int           __numChunkUsedColors;
   }

   private static class Pool extends SyncPool<Chunk> {

      public Pool() {
         super(MAX_POOL);
      }

      @Override
      protected boolean clearItem(final Chunk chunk) {

         chunk.__numChunkUsedVertices = 0;
         chunk.__numChunkUsedColors = 0;

         return true;
      }

      @Override
      protected Chunk createItem() {

         return new Chunk();
      }
   }

   public void add(final short a,
                   final short b,
                   final short c,
                   final short d,
                   final int color) {

      if (_numUsedChunkVertices == SIZE) {
         getNext();
      }

      /*
       * Set vertices
       */
      _currentChunk_Vertices[_numUsedChunkVertices + 0] = a;
      _currentChunk_Vertices[_numUsedChunkVertices + 1] = b;
      _currentChunk_Vertices[_numUsedChunkVertices + 2] = c;
      _currentChunk_Vertices[_numUsedChunkVertices + 3] = d;

      _numUsedChunkVertices += 4;

      /*
       * Set color components, argb -> rgba
       */
      _currentChunk_Colors[_numUsedChunkColors + 0] = (byte) ((color >>> 16) & 0xff); // red
      _currentChunk_Colors[_numUsedChunkColors + 1] = (byte) ((color >>> 8) & 0xff); // green
      _currentChunk_Colors[_numUsedChunkColors + 2] = (byte) ((color >>> 0) & 0xff); // blue
      _currentChunk_Colors[_numUsedChunkColors + 3] = (byte) ((color >>> 24) & 0xff); // opacity / alpha

      _numUsedChunkColors += 4;
   }

   @Override
   public Chunk clear() {

      if (_currentChunk == null) {
         return null;
      }

      _currentChunk.__numChunkUsedVertices = _numUsedChunkVertices;
      _currentChunk.__numChunkUsedColors = _numUsedChunkColors;

      _numUsedChunkVertices = SIZE; // set to SIZE to get new a item on add

      _currentChunk = null;
      _currentChunk_Vertices = null;
      _currentChunk_Colors = null;

      return super.clear();
   }

   public void dispose() {

      pool.releaseAll(super.clear());

      _numUsedChunkVertices = SIZE; // set SIZE to get new item on add

      _currentChunk = null;
      _currentChunk_Vertices = null;
      _currentChunk_Colors = null;
   }

   /**
    * Copy vertices into the <code>vertexBuffer</code> and colors into the <code>colorBuffer</code>
    *
    * @param vertexBuffer
    * @param colorBuffer
    * @return sum of elements added
    */
   public int fillVerticesBuffer(final ShortBuffer vertexBuffer, final ByteBuffer colorBuffer) {

      if (_currentChunk == null) {
         return 0;
      }

      _currentChunk.__numChunkUsedVertices = _numUsedChunkVertices;
      _currentChunk.__numChunkUsedColors = _numUsedChunkColors;

      int numAllUsedVertices = 0;

      for (Chunk chunk = head(); chunk != null; chunk = chunk.next) {

         numAllUsedVertices += chunk.__numChunkUsedVertices;

         vertexBuffer.put(chunk.__chunkVertices, 0, chunk.__numChunkUsedVertices);

         if (colorBuffer != null) {
            colorBuffer.put(chunk.__chunkColors, 0, chunk.__numChunkUsedColors);
         }
      }

      dispose();

      return numAllUsedVertices;
   }

   private void getNext() {

      if (_currentChunk == null) {

         _currentChunk = pool.get();

         push(_currentChunk);

      } else {

         if (_currentChunk.next != null) {
            throw new IllegalStateException("seeeked..."); //$NON-NLS-1$
         }

         _currentChunk.__numChunkUsedVertices = SIZE;
         _currentChunk.__numChunkUsedColors = _numUsedChunkColors;

         _currentChunk.next = pool.get();
         _currentChunk = _currentChunk.next;
      }

      _currentChunk_Vertices = _currentChunk.__chunkVertices;
      _currentChunk_Colors = _currentChunk.__chunkColors;

      _numUsedChunkVertices = 0;
      _numUsedChunkColors = 0;
   }

}
