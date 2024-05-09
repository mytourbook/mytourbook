/*
 * Copyright (c) 2010, Martin Luboschik, Hilko Cords
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   - Neither the name of the University of Rostock (Germany) nor the
 *     names of its contributors may be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL MARTIN LUBOSCHIK OR HILKO CORDS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Please report any oportunity to speed up to
 * martin DOT luboschik AT uni-rostock DOT de
 *
 * Have fun with this labeling approach =)
 */
package particlelabeling;

/**
 * <p>
 * This class stores the particle-positions and generates an id for each stored particle. Storing
 * particles is
 * of constant time in case, that no new memory has to be allocated. Retrieving particle information
 * depends
 * on how many particles are in the result set.
 * </p>
 * <p>
 *
 * The accompanying source or binary forms are published under the terms of the <b>New BSD
 * License</b>:<br>
 * <br>
 * Copyright (c) 2010, Martin Luboschik, Hilko Cords<br>
 * All rights reserved.
 * </p>
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <ul>
 * <li>Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * <li>Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * <li>Neither the name of the University of Rostock (Germany) nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * </ul>
 * </p>
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL MARTIN LUBOSCHIK OR HILKO CORDS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * </p>
 *
 * @author M. Luboschik
 *
 * @version 1.0
 *
 * @see PointFeature
 */
public class ParticleStore {

   // the initial size of the particle store
   private int              _maximumNrOfParticles = 1_000_000;

   private ParticleList[][] _particleGrid;
   private float[][]        _particleData         = new float[_maximumNrOfParticles][3];
   private int              _lastParticleId       = -1;

   private int              _maxX, _maxY;                                               // size of the grid
   private int              _cellW, _cellH;                                             // width and height of one gridcell

   /**
    * Generates a new grid-storage for particles inside a rectangular area. This area is
    * located at (0,0) and is of provied width and height.
    *
    * @param width
    *           the width of the area covered by the grid.
    * @param height
    *           the height of the area covered by the grid.
    * @param cellWidth
    *           the width of a single gridcell.
    * @param cellHeight
    *           the height of a single gridcell.
    */
   public ParticleStore(final int width, final int height, final int cellWidth, final int cellHeight) {

      //subdividing the space into discrete cells
      _maxX = (int) Math.ceil((double) width / (double) cellWidth);
      _maxY = (int) Math.ceil((double) height / (double) cellHeight);
      _particleGrid = new ParticleList[_maxX + 1][_maxY + 1];

      _cellW = cellWidth;
      _cellH = cellHeight;

      for (int i = 0; i <= _maxX; i++) {
         for (int j = 0; j <= _maxY; j++) {

            //generating the single cells
            _particleGrid[i][j] = new ParticleList(cellWidth * cellHeight * 3);
         }
      }
   }

   /**
    * Adds a particle given by its position to this storage and returns the storage
    * internal id.
    *
    * @param x
    *           the x-coordinate of the particle.
    * @param y
    *           the y-coordinate of the particle.
    *
    * @return the internal id of the particle
    */
   public int addParticle(final float x, final float y) {

      //which gridcell?
      final int cellX = (int) x / _cellW;
      final int cellY = (int) y / _cellH;

      //gridcell defined?
      if (cellX > -1 && cellX <= _maxX && cellY > -1 && cellY <= _maxY) {

         //registering the particle to the cell
         _particleGrid[cellX][cellY].add(++_lastParticleId);

         //allocating new memory if storage is to small
         if (_lastParticleId == _maximumNrOfParticles) {

            //ARRAY VERGROESZERN UND UMKOPIEREN

            final float[][] newPData = new float[_maximumNrOfParticles * 2][3];

            for (int i = 0; i < _maximumNrOfParticles; i++) {
               for (int j = 0; j < 3; j++) {
                  newPData[i][j] = _particleData[i][j];
               }
            }

            _maximumNrOfParticles *= 2;

            System.out.println("ALLOCATING PARTICLE-DATA-SPACE - SPACE FOR " + (_maximumNrOfParticles) + " PARTICLES");

            _particleData = newPData;

         }

         //storing the particles information
         final float[] particle = _particleData[_lastParticleId];
         particle[0] = _lastParticleId; //id
         particle[1] = x; //x
         particle[2] = y; //y

         return _lastParticleId;
      }

      return -1;
   }

   /**
    * Returns the number of columns of the grid.
    *
    * @return the nr. of columns.
    */
   public int getColumns() {
      return _maxX;
   }

   /**
    * Returns particles that are registered in gridcells, which are in contact to the
    * specified rectangle. The rectangle is given by its top-left and bottom-right coordinates and
    * every gridcell intersecting this area is involved.
    *
    * @param x1
    *           the left coordinate.
    * @param y1
    *           the top coordinate.
    * @param x2
    *           the right coordinate.
    * @param y2
    *           the bottom coordinate.
    *
    * @return i particles whose information are given in an array: [i][0] is the particle-id,
    *         [i][1] is the x-coordinate and [i][2] is the y-coordinate.
    */
   public float[][] getInvolvedParticles(final float x1, final float y1, final float x2, final float y2) {

      //spanning which cells in hor. and vert. direction?
      final int rx1 = Math.max((int) x1 / _cellW, 0);
      final int rx2 = Math.min((int) x2 / _cellW, _maxX);
      final int ry1 = Math.max((int) y1 / _cellH, 0);
      final int ry2 = Math.min((int) y2 / _cellH, _maxY);

      int numParticlesWithinCell = 0;

      int indexX;
      int indexY;

      final float[][] particleData = _particleData;
      final ParticleList[][] particleGrid = _particleGrid;

      // calculating the concrete size of the result
      for (indexX = rx1; indexX <= rx2; indexX++) {

         final ParticleList[] particleListX = particleGrid[indexX];

         for (indexY = ry1; indexY <= ry2; indexY++) {

            numParticlesWithinCell += particleListX[indexY].numParticlesWithinCell;
         }
      }

      final float[][] output = new float[numParticlesWithinCell][];
      int l = 0;

      // storing the particles into the output array
      for (indexX = rx1; indexX <= rx2; indexX++) {

         final ParticleList[] particleListX = particleGrid[indexX];

         for (indexY = ry1; indexY <= ry2; indexY++) {

            final ParticleList particleListXY = particleListX[indexY];

            numParticlesWithinCell = particleListXY.numParticlesWithinCell;
            final int[] registeredParticles = particleListXY.registeredParticles;

            for (int k = 0; k < numParticlesWithinCell; k++) {

               output[l++] = particleData[registeredParticles[k]];
            }
         }
      }

      return output;
   }

   /**
    * Returns the particles that are registered within the specified gridcell.
    *
    * @param cellX
    *           column of the gridcell.
    * @param cellY
    *           row of the gridcell.
    *
    * @return i particles whose information are given in an array: [i][0] is the particle-id,
    *         [i][1] is the x-coordinate and [i][2] is the y-coordinate.
    */
   public float[][] getParticlesOfCell(final int cellX, final int cellY) {

      if ((cellX > -1) && (cellX <= _maxX) && (cellY > -1) && (cellY <= _maxY)) {

         final int numParticlesWithinCell = _particleGrid[cellX][cellY].numParticlesWithinCell;

         final float[][] output = new float[numParticlesWithinCell][];

         for (int k = 0; k < numParticlesWithinCell; k++) {
            output[k] = _particleData[_particleGrid[cellX][cellY].registeredParticles[k]];
         }

         return output;
      }

      return null;
   }

   /**
    * Returns the number of rows of the grid.
    *
    * @return the nr. of rows.
    */
   public int getRows() {

      return _maxY;
   }

   /**
    * Resets this storage. By doing so, no allocated memory is released, but internal
    * counters are reset.
    */
   public void reset() {

      for (int indexX = 0; indexX <= _maxX; indexX++) {
         for (int indexY = 0; indexY <= _maxY; indexY++) {

            _particleGrid[indexX][indexY].numParticlesWithinCell = 0;
         }
      }

      _lastParticleId = -1;
   }
}
