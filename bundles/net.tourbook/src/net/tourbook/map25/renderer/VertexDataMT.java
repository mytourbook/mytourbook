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

import net.tourbook.map25.renderer.VertexDataMT.Chunk;

import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.SyncPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A linked list of array chunks to hold temporary vertex data.
 * <p/>
 * TODO override append() etc to update internal (cur) state.
 */
public class VertexDataMT extends Inlist.List<Chunk> {

   static final Logger       log                   = LoggerFactory.getLogger(VertexDataMT.class);

   /**
    * Size of array chunks. Must be multiple of:
    * 4 (LineLayer/PolygonLayer),
    * 24 (TexLineLayer - one block, i.e. two segments)
    * 24 (TextureLayer)
    */
   public static final int   SIZE                  = 360;

   /**
    * Shared chunk pool size.
    */
   private static final int  MAX_POOL              = 500;

   private static final Pool pool                  = new Pool();

   private Chunk             _currentChunk;

   /**
    * Set SIZE to get new item on add
    */
   private int               _numUsedChunkVertices = SIZE;
   private int               _numUsedChunkColors   = SIZE / 4;

   private short[]           _currentChunk_Vertices;

   /**
    * There are 4 color components for 4 chunks
    */
   private byte[]            _currentChunk_Colors;

   public static class Chunk extends Inlist<Chunk> {

      public final short[] _chunkVertices = new short[SIZE];
      public final byte[]  _chunkColors   = new byte[SIZE];

      public int           _numChunkUsedVertices;
      public int           _numChunkUsedColors;
   }

   private static class Pool extends SyncPool<Chunk> {

      public Pool() {
         super(MAX_POOL);
      }

      @Override
      protected boolean clearItem(final Chunk chunk) {

         chunk._numChunkUsedVertices = 0;
         chunk._numChunkUsedColors = 0;

         return true;
      }

      @Override
      protected Chunk createItem() {
         return new Chunk();
      }
   }

   public void add(final short a, final short b, final short c, final short d, final int color) {

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
       * Set color components rgb
       */
      _currentChunk_Colors[_numUsedChunkColors + 0] = (byte) ((color >>> 16) & 0xff); // red
      _currentChunk_Colors[_numUsedChunkColors + 1] = (byte) ((color >>> 8) & 0xff); // green
      _currentChunk_Colors[_numUsedChunkColors + 2] = (byte) ((color >>> 0) & 0xff); // blue
      _currentChunk_Colors[_numUsedChunkColors + 3] = (byte) ((color >>> 24) & 0xff); // opacity / alpha

      _numUsedChunkColors += 4;
   }

   /**
    * @param a
    * @param b
    * @param c
    * @param d
    * @param e
    * @param f
    */
   /*
    * THIS IS CURRENTLY USED ONLY FOR LineTexBucketMT which do not support color and will be
    * removed later on !!!
    */
   public void add(final short a, final short b, final short c, final short d, final short e, final short f) {

      if (_numUsedChunkVertices == SIZE) {
         getNext();
      }

      _currentChunk_Vertices[_numUsedChunkVertices + 0] = a;
      _currentChunk_Vertices[_numUsedChunkVertices + 1] = b;
      _currentChunk_Vertices[_numUsedChunkVertices + 2] = c;
      _currentChunk_Vertices[_numUsedChunkVertices + 3] = d;
      _currentChunk_Vertices[_numUsedChunkVertices + 4] = e;
      _currentChunk_Vertices[_numUsedChunkVertices + 5] = f;

      _numUsedChunkVertices += 6;
   }

   @Override
   public Chunk clear() {

      if (_currentChunk == null) {
         return null;
      }

      _currentChunk._numChunkUsedVertices = _numUsedChunkVertices;
      _currentChunk._numChunkUsedColors = _numUsedChunkColors;

      _numUsedChunkVertices = SIZE; // set SIZE to get new item on add

      _currentChunk = null;
      _currentChunk_Vertices = null;
      _currentChunk_Colors = null;

      return super.clear();
   }

   /**
    * Copy vertices into the <code>vertexBuffer</code> and colors into the <code>colorBuffer</code>
    *
    * @param vertexBuffer
    * @param colorBuffer
    * @return sum of elements added
    */
   public int compile(final ShortBuffer vertexBuffer, final ByteBuffer colorBuffer) {

      if (_currentChunk == null) {
         return 0;
      }

      _currentChunk._numChunkUsedVertices = _numUsedChunkVertices;
      _currentChunk._numChunkUsedColors = _numUsedChunkColors;

      int numAllUsedVertices = 0;
      for (Chunk chunk = head(); chunk != null; chunk = chunk.next) {

         numAllUsedVertices += chunk._numChunkUsedVertices;

         vertexBuffer.put(chunk._chunkVertices, 0, chunk._numChunkUsedVertices);

         if (colorBuffer != null) {
            colorBuffer.put(chunk._chunkColors, 0, chunk._numChunkUsedColors);
         }
      }

      dispose();

      return numAllUsedVertices;
   }

   public void dispose() {

      pool.releaseAll(super.clear());

      _numUsedChunkVertices = SIZE; // set SIZE to get new item on add

      _currentChunk = null;
      _currentChunk_Vertices = null;
      _currentChunk_Colors = null;
   }

   public boolean empty() {
      return _currentChunk == null;
   }

   private void getNext() {

      if (_currentChunk == null) {

         _currentChunk = pool.get();

         push(_currentChunk);

      } else {

         if (_currentChunk.next != null) {
            throw new IllegalStateException("seeeked..."); //$NON-NLS-1$
         }

         _currentChunk._numChunkUsedVertices = SIZE;
         _currentChunk._numChunkUsedColors = _numUsedChunkColors;

         _currentChunk.next = pool.get();
         _currentChunk = _currentChunk.next;
      }

      _currentChunk_Vertices = _currentChunk._chunkVertices;
      _currentChunk_Colors = _currentChunk._chunkColors;

      _numUsedChunkVertices = 0;
      _numUsedChunkColors = 0;
   }

   /**
    * Do not use!
    */
   /*
    * THIS IS CURRENTLY USED ONLY FOR LineTexBucketMT which do not support color and will be
    * removed later on !!!
    */
   public void seek(final int offset) {

      _numUsedChunkVertices += offset;
      _currentChunk._numChunkUsedVertices = _numUsedChunkVertices;

      if (_numUsedChunkVertices > SIZE || _numUsedChunkVertices < 0) {
         throw new IllegalStateException("seeked too far: " + offset + "/" + _numUsedChunkVertices); //$NON-NLS-1$ //$NON-NLS-2$
      }
   }
}
