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
import static org.oscim.renderer.bucket.RenderBucket.POLYGON;
import static org.oscim.renderer.bucket.RenderBucket.TEXLINE;

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
public class RenderBucketsAllMT extends TileData {

   static final Logger log = LoggerFactory.getLogger(RenderBucketsAllMT.class);

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

   private RenderBucketMT _firstChainedBucket;

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
   public BufferObject    vbo_BufferObject;
   public BufferObject    ibo_BufferObject;

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
   public int[]           offset                       = { 0, 0 };

   private RenderBucketMT _currentBucket;

   int                    vertexColor_BufferId         = Integer.MIN_VALUE;
   int                    dirArrows_BufferId           = Integer.MIN_VALUE;
   int                    dirArrows_ColorCoords_BufferId = Integer.MIN_VALUE;

   private ByteBuffer     _vertexColor_Buffer;
   private int            _vertexColor_BufferSize;
   private ShortBuffer    _dirArrow_Buffer;
   private int            _dirArrow_BufferSize;
   private ShortBuffer    _dirArrow_ColorCoord_Buffer;
   private int            _dirArrow_ColorCoord_BufferSize;

   /**
    * Number of {@link Short}'s used for the direction arrows
    */
   int                    numShortsForDirectionArrows;

   /**
    * Number of {@link Short}'s used for the color coordinates
    */
   int                    numShortsForColorCoords;

   public RenderBucketsAllMT() {}

   public static void initRenderer() {

      LineBucketMT.Renderer.init();
      LineTexBucketMT.Renderer.init();

//    PolygonBucket.Renderer.init();
//    TextureBucket.Renderer.init();
//    BitmapBucket.Renderer.init();
//    MeshBucket.Renderer.init();
//    HairLineBucket.Renderer.init();
//    CircleBucketMT.Renderer.init();
   }

//    public CircleBucket addCircleBucket(final int level, final CircleStyle style) {
//        final CircleBucket l = (CircleBucket) getBucket(level, CIRCLE);
//        if (l == null) {
//         return null;
//      }
//        l.circle = style;
//        return l;
//    }
//
//    public HairLineBucket addHairLineBucket(final int level, final LineStyle style) {
//        final HairLineBucket ll = getHairLineBucket(level);
//        if (ll == null) {
//         return null;
//      }
//        ll.line = style;
//
//        return ll;
//    }

   /**
    * add the LineBucket for a level with a given Line style. Levels are
    * ordered from bottom (0) to top
    */
   public LineBucketMT addLineBucket(final int level, final LineStyle style) {

      final LineBucketMT lineBucket = (LineBucketMT) getBucket(level, LINE);

      if (lineBucket == null) {
         return null;
      }

      // FIXME l.scale = style.width;
      lineBucket.scale = 1;
      lineBucket.line = style;

      return lineBucket;
   }

//    public MeshBucket addMeshBucket(final int level, final AreaStyle style) {
//        final MeshBucket l = (MeshBucket) getBucket(level, MESH);
//        if (l == null) {
//         return null;
//      }
//        l.area = style;
//        return l;
//    }
//
//    public PolygonBucket addPolygonBucket(final int level, final AreaStyle style) {
//        final PolygonBucket l = (PolygonBucket) getBucket(level, POLYGON);
//        if (l == null) {
//         return null;
//      }
//        l.area = style;
//        return l;
//    }

   /**
    * Binds vbo and ibo
    */
   public void bind() {

      if (vbo_BufferObject != null) {
         vbo_BufferObject.bind();
      }

      if (ibo_BufferObject != null) {
         ibo_BufferObject.bind();
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
      ibo_BufferObject = BufferObject.release(ibo_BufferObject);
   }

   /**
    * cleanup only when buckets are not used by tile or bucket anymore!
    */
   public void clearBuckets() {

      /* NB: set null calls clear() on each bucket! */
      for (RenderBucketMT l = _firstChainedBucket; l != null; l = l.next) {
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
   public boolean compile(final boolean addFill) {

      int vboSize = countVboSize();
      if (vboSize <= 0) {

         vbo_BufferObject = BufferObject.release(vbo_BufferObject);
         ibo_BufferObject = BufferObject.release(ibo_BufferObject);

         return false;
      }

      if (addFill) {
         vboSize += TILE_FILL_VERTICES * 2;
      }

      final int vertexColor_Size = vboSize;
      numShortsForDirectionArrows = countShortsForDirectionArrows();
      numShortsForColorCoords = countShortsForColorCoords();

      final ShortBuffer vboBucketBuffer = MapRenderer.getShortBuffer(vboSize);
      final ShortBuffer directionArrow_ShortBuffer = getBuffer_DirectionArrow(numShortsForDirectionArrows);
      final ShortBuffer colorCoords_ShortBuffer = getBuffer_ColorCoords(numShortsForColorCoords);
      final ByteBuffer colorBuffer = getBuffer_Color(vertexColor_Size);

      if (addFill) {
         vboBucketBuffer.put(fillShortCoords, 0, TILE_FILL_VERTICES * 2);
      }

      ShortBuffer iboBuffer = null;

      final int iboSize = countIboSize();
      if (iboSize > 0) {
         iboBuffer = MapRenderer.getShortBuffer(iboSize);
      }

      int vertexOffset = addFill ? TILE_FILL_VERTICES : 0;

      /*
       * Compile polygons
       */
      for (RenderBucketMT currentBucket = _firstChainedBucket; currentBucket != null; currentBucket = currentBucket.next) {

         if (currentBucket.type == POLYGON) {

            currentBucket.compile(vboBucketBuffer, iboBuffer, null);
            currentBucket.vertexOffset = vertexOffset;

            vertexOffset += currentBucket.numVertices;
         }
      }

      /*
       * Compile lines
       */
      offset[LINE] = vboBucketBuffer.position() * SHORT_BYTES;
      vertexOffset = 0;
      int directionArrowsOffset = 0;
      int colorCoordsOffset = 0;
      for (RenderBucketMT currentBucket = _firstChainedBucket; currentBucket != null; currentBucket = currentBucket.next) {

         if (currentBucket.type == LINE) {

            currentBucket.compile(vboBucketBuffer, iboBuffer, colorBuffer);
            currentBucket.vertexOffset = vertexOffset;

            vertexOffset += currentBucket.numVertices;
         }

         /*
          * Append direction arrow shorts into the buffer
          */
         final TShortArrayList directionArrowVertices = currentBucket.directionArrow_XYZPositions;
         final int numBucketShorts = directionArrowVertices.size();

         directionArrow_ShortBuffer.put(directionArrowVertices.toArray(), directionArrowsOffset, numBucketShorts);

         directionArrowsOffset += numBucketShorts;

         /*
          * Append color coord shorts into the buffer
          */
         final TShortArrayList colorCoordsVertices = currentBucket.colorCoords;
         final int numColorBucketShorts = colorCoordsVertices.size();

         colorCoords_ShortBuffer.put(colorCoordsVertices.toArray(), colorCoordsOffset, numColorBucketShorts);

         colorCoordsOffset += numColorBucketShorts;
      }

      /*
       * Compile others
       */
      for (RenderBucketMT currentBucket = _firstChainedBucket; currentBucket != null; currentBucket = currentBucket.next) {

         if (currentBucket.type != LINE && currentBucket.type != POLYGON) {
            currentBucket.compile(vboBucketBuffer, iboBuffer, null);
         }
      }

      if (vboSize != vboBucketBuffer.position()) {

         log.debug("wrong vertex buffer size: " //$NON-NLS-1$
               + " new size: " + vboSize //$NON-NLS-1$
               + " buffer pos: " + vboBucketBuffer.position() //$NON-NLS-1$
               + " buffer limit: " + vboBucketBuffer.limit() //$NON-NLS-1$
               + " buffer fill: " + vboBucketBuffer.remaining()); //$NON-NLS-1$

         return false;
      }

      if (iboSize > 0 && iboSize != iboBuffer.position()) {

         log.debug("wrong indice buffer size: " //$NON-NLS-1$
               + " new size: " + iboSize //$NON-NLS-1$
               + " buffer pos: " + iboBuffer.position() //$NON-NLS-1$
               + " buffer limit: " + iboBuffer.limit() //$NON-NLS-1$
               + " buffer fill: " + iboBuffer.remaining()); //$NON-NLS-1$

         return false;
      }

      /*
       * Direction arrows
       */
      if (dirArrows_BufferId == Integer.MIN_VALUE) {

         // create buffer id
         dirArrows_BufferId = gl.genBuffer();
      }
      gl.bindBuffer(GL.ARRAY_BUFFER, dirArrows_BufferId);
      gl.bufferData(GL.ARRAY_BUFFER, numShortsForDirectionArrows * SHORT_BYTES, directionArrow_ShortBuffer.flip(), GL.STATIC_DRAW);

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
      vbo_BufferObject.loadBufferData(vboBucketBuffer.flip(), vboSize * SHORT_BYTES);

      /*
       * IBO
       */
      if (iboSize > 0) {

         if (ibo_BufferObject == null) {
            ibo_BufferObject = BufferObject.get(GL.ELEMENT_ARRAY_BUFFER, iboSize);
         }

         // Set IBO data to READ mode
         ibo_BufferObject.loadBufferData(iboBuffer.flip(), iboSize * SHORT_BYTES);
      }

      return true;
   }

   private int countIboSize() {

      int numIndices = 0;

      for (RenderBucketMT bucket = _firstChainedBucket; bucket != null; bucket = bucket.next) {
         numIndices += bucket.numIndices;
      }

      return numIndices;
   }

   /**
    * @return Returns number of {@link Short}'s for the color coordinates
    */
   private int countShortsForColorCoords() {

      int numShorts = 0;

      for (RenderBucketMT bucket = _firstChainedBucket; bucket != null; bucket = bucket.next) {

         numShorts += bucket.colorCoords.size();
      }

      return numShorts;
   }

   /**
    * @return Returns number of {@link Short}'s for the direction arrows
    */
   private int countShortsForDirectionArrows() {

      int numShorts = 0;

      for (RenderBucketMT bucket = _firstChainedBucket; bucket != null; bucket = bucket.next) {

         numShorts += bucket.directionArrow_XYZPositions.size();
      }

      return numShorts;
   }

   private int countVboSize() {

      int vboSize = 0;

      for (RenderBucketMT bucket = _firstChainedBucket; bucket != null; bucket = bucket.next) {
         vboSize += bucket.numVertices * VERTEX_CNT[bucket.type];
      }

      return vboSize;
   }

   @Override
   protected void dispose() {
      clear();
   }

   /**
    * @return internal linked list of RenderBucket items
    */
   public RenderBucketMT get() {
      return _firstChainedBucket;
   }

   private RenderBucketMT getBucket(final int level, final int type) {

      RenderBucketMT typedBucket = null;

      if (_currentBucket != null && _currentBucket.level == level) {

         typedBucket = _currentBucket;

         if (typedBucket.type != type) {
            log.error("BUG wrong bucket {} {} on level {}", typedBucket.type, type, level); //$NON-NLS-1$
            throw new IllegalArgumentException();
         }

         return typedBucket;
      }

      RenderBucketMT chainedBucked = _firstChainedBucket;
      if (chainedBucked == null || chainedBucked.level > level) {
         /* insert new bucket at start */
         chainedBucked = null;
      } else {
         if (_currentBucket != null && level > _currentBucket.level) {
            chainedBucked = _currentBucket;
         }

         while (true) {

            /* found bucket */
            if (chainedBucked.level == level) {
               typedBucket = chainedBucked;
               break;
            }

            /* insert bucket between current and next bucket */
            if (chainedBucked.next == null || chainedBucked.next.level > level) {
               break;
            }

            chainedBucked = chainedBucked.next;
         }
      }

      if (typedBucket == null) {

         // add a new RenderElement
         if (type == LINE) {

            typedBucket = new LineBucketMT(level);

         } else if (type == TEXLINE) {

            typedBucket = new LineTexBucketMT(level);

//       } else if (type == POLYGON) {
//          bucket = new PolygonBucket(level);
//       } else if (type == MESH) {
//          bucket = new MeshBucket(level);
//       } else if (type == HAIRLINE) {
//          bucket = new HairLineBucket(level);
//
//       } else if (type == CIRCLE) {
//          typedBucket = new CircleBucketMT(level);

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
      if (typedBucket.type != type) {
         log.error("BUG wrong bucket {} {} on level {}", typedBucket.type, type, level); //$NON-NLS-1$
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
    * @param requestedSize
    *           Number of {@link Short}'s
    * @return
    */
   private ShortBuffer getBuffer_ColorCoords(final int requestedSize) {

      final int bufferBlockSize = 2048;
      final int numBufferBlocks = (requestedSize * SHORT_BYTES) / bufferBlockSize;
      final int roundedBufferSize = (numBufferBlocks + 1) * bufferBlockSize;

      if (_dirArrow_ColorCoord_Buffer == null || _dirArrow_ColorCoord_BufferSize < roundedBufferSize) {

         _dirArrow_ColorCoord_Buffer = ByteBuffer
               .allocateDirect(roundedBufferSize)
               .order(ByteOrder.nativeOrder())
               .asShortBuffer();

         _dirArrow_ColorCoord_BufferSize = roundedBufferSize;

      } else {

         // IMPORTANT: reset position to 0 to prevent BufferOverflowException

         _dirArrow_ColorCoord_Buffer.clear();
      }

      return _dirArrow_ColorCoord_Buffer;
   }

   /**
    * @param requestedSize
    *           Number of {@link Short}'s
    * @return
    */
   private ShortBuffer getBuffer_DirectionArrow(final int requestedSize) {

      final int bufferBlockSize = 2048;
      final int numBufferBlocks = (requestedSize * SHORT_BYTES) / bufferBlockSize;
      final int roundedBufferSize = (numBufferBlocks + 1) * bufferBlockSize;

      if (_dirArrow_Buffer == null || _dirArrow_BufferSize < roundedBufferSize) {

         _dirArrow_Buffer = ByteBuffer
               .allocateDirect(roundedBufferSize)
               .order(ByteOrder.nativeOrder())
               .asShortBuffer();

         _dirArrow_BufferSize = roundedBufferSize;

      } else {

         // IMPORTANT: reset position to 0 to prevent BufferOverflowException

         _dirArrow_Buffer.clear();
      }

      return _dirArrow_Buffer;
   }

//    /**
//     * Get or add the CircleBucket for a level. Levels are ordered from
//     * bottom (0) to top
//     */
//    public CircleBucket getCircleBucket(final int level) {
//        return (CircleBucket) getBucket(level, CIRCLE);
//    }
//
//    /**
//     * Get or add the TexLineBucket for a level. Levels are ordered from
//     * bottom (0) to top
//     */
//    public HairLineBucket getHairLineBucket(final int level) {
//        return (HairLineBucket) getBucket(level, HAIRLINE);
//    }

   /**
    * Get or add the LineBucket for a level. Levels are ordered from
    * bottom (0) to top
    */
   public LineBucketMT getLineBucket(final int level) {
      return (LineBucketMT) getBucket(level, LINE);
   }

   /**
    * Get or add the TexLineBucket for a level. Levels are ordered from
    * bottom (0) to top
    */
   public LineTexBucketMT getLineTexBucket(final int level) {
      return (LineTexBucketMT) getBucket(level, TEXLINE);
   }
//
//    /**
//     * Get or add the MeshBucket for a level. Levels are ordered from
//     * bottom (0) to top
//     */
//    public MeshBucket getMeshBucket(final int level) {
//        return (MeshBucket) getBucket(level, MESH);
//    }
//
//    /**
//     * Get or add the PolygonBucket for a level. Levels are ordered from
//     * bottom (0) to top
//     */
//    public PolygonBucket getPolygonBucket(final int level) {
//        return (PolygonBucket) getBucket(level, POLYGON);
//    }

   public void prepare() {
      for (RenderBucketMT l = _firstChainedBucket; l != null; l = l.next) {
         l.prepare();
      }
   }

   /**
    * Set new bucket items and clear previous.
    */
   public void set(final RenderBucketMT newBuckets) {

      for (RenderBucketMT previousBucket = _firstChainedBucket; previousBucket != null; previousBucket = previousBucket.next) {
         previousBucket.clear();
      }

      _firstChainedBucket = newBuckets;
   }

   public void setFrom(final RenderBucketsAllMT allBuckets) {

      if (allBuckets == this) {
         throw new IllegalArgumentException("Cannot set from oneself!"); //$NON-NLS-1$
      }

      set(allBuckets._firstChainedBucket);

      _currentBucket = null;
      allBuckets._firstChainedBucket = null;
      allBuckets._currentBucket = null;
   }
}
