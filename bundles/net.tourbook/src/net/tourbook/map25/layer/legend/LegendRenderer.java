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
package net.tourbook.map25.layer.legend;

import org.oscim.renderer.BitmapRenderer;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;

public class LegendRenderer extends BitmapRenderer {

   private boolean _isLegendVisible;

   public LegendRenderer() {

      super();

      /*
       * Prevent exception when mBitmap == null
       */
      mInitialized = true;
   }

   @Override
   public synchronized void render(final GLViewport viewport) {

      if (_isLegendVisible == false) {
         return;
      }

      /**
       * Original comment in commit 3a8db9cc7cb024f7cc399ec534ac5a543115b2cb
       * <p>
       * "ScaleBar disappears sometimes fix by Erik Duisters, fixes #155"
       */
      GLState.test(false, false);

      super.render(viewport);
   }

   /**
    * @param isLegendVisible
    */
   public void setLegendVisible(final boolean isLegendVisible) {

      _isLegendVisible = isLegendVisible;
   }

   @Override
   public synchronized void update(final GLViewport v) {

      if (_isLegendVisible == false) {
         return;
      }

      super.update(v);
   }

}
