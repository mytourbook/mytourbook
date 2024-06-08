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
 * This class stores particle-ids of particles laying within the gridcell backuped
 * by this list. Registering particles is
 * of constant time in case, that no new memory has to be allocated. Retrieving particle information
 * is
 * via public variables.
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
public class ParticleList {

   /**
    * The ids of particles laying within the cell, backuped by this list.
    */
   public int[] registeredParticles;

   /**
    * The number of particles laying within the cell, backuped by this list.
    */
   public int   numParticlesWithinCell;

   private int  _maxLength;

   /**
    * Builds a new ParticleList with capacity to store <code>psize</code> particles.
    *
    * @param psize
    *           the initial capacity for storing particles.
    */
   public ParticleList(final int psize) {

      registeredParticles = new int[psize];
      numParticlesWithinCell = 0;

      _maxLength = registeredParticles.length;
   }

   /**
    * Adds an particle to the gridcell backuped by this ParticleList. Only needs the id of the
    * particle to register since the position is stored inside {@link ParticleStore}.
    *
    * @param id
    *           the id of the particle to register within this cell.
    */
   public void add(final int id) {

      //allocating new memory
      if (numParticlesWithinCell == _maxLength) {

         System.out.println("ALLOCAING NEW GRIDCELL-SPACE - SPACE FOR " + _maxLength + " PARTICLES"); //$NON-NLS-1$ //$NON-NLS-2$

         final int[] newRegPart = new int[this._maxLength * 2];

         for (int i = 0; i < _maxLength; i++) {
            newRegPart[i] = registeredParticles[i];
         }

         _maxLength *= 2;
         registeredParticles = newRegPart;

      }

      //registering the particle
      registeredParticles[numParticlesWithinCell++] = id;

   }
}
