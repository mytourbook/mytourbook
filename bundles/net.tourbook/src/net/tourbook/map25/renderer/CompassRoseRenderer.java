/*
 * based on  https://github.com/telemaxx/vtm/blob/master/vtm-playground/src/org/oscim/test/renderer/SymbolRenderLayer.java
 *
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016 devemux86
 * Copyright 2022 Thomas Theussing
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

import net.tourbook.map25.layer.marker.MarkerShape;
import net.tourbook.map25.layer.marker.MarkerToolkit;

import org.eclipse.swt.internal.DPIUtil;
import org.oscim.layers.Layer;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.SymbolBucket;
import org.oscim.renderer.bucket.SymbolItem;

public class CompassRoseRenderer extends BucketRenderer {

   private boolean       _isShaderSetup;

   private Layer         _layer;

   private int           compassRoseSize     = 300;

   private MarkerToolkit _compassRoseToolkit = new MarkerToolkit(MarkerShape.COMPASS);

   public CompassRoseRenderer(final Layer compassRoseLayer, final int compassSize) {
      super();

      this.compassRoseSize = compassSize;

      final int scaledcompassRoseSize = DPIUtil.autoScaleUp(compassRoseSize);

      System.out.println("Compass Size: " + " unscaled: " + compassSize + ", scaled: " + scaledcompassRoseSize);

      _layer = compassRoseLayer;

      final SymbolBucket l = new SymbolBucket();

      buckets.set(l);

      final SymbolItem it = SymbolItem.pool.get();
      it.billboard = false;

      try {
         it.bitmap = _compassRoseToolkit.drawCompassRose(scaledcompassRoseSize, 0xFFFFFFFF); //AARRGGBB
         //it.bitmap = _compassRoseToolkit.getMarkerSymbol().getBitmap();
      } catch (final Exception e) {
         e.printStackTrace();
      }
      l.addSymbol(it);
   }

   private boolean init() {
//  Here where in the HexagonRenderer the VERTEXT_SHADER and the programObject defined.
      return true;
   }

   @Override
   public void update(final GLViewport viewport) {

      if (_layer.isEnabled() == false) {
         return;
      }

      if (_isShaderSetup == false) {

         if (init() == false) {
            return;
         }

         _isShaderSetup = true;

         compile();

      }

      mMapPosition.copy(viewport.pos);

   }
}
