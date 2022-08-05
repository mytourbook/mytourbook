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
package net.tourbook.map25.layer.compassrose;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.NIO;
import net.tourbook.common.util.StatusUtil;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.layers.Layer;
import org.oscim.map.Map;

public class CompassRoseLayer extends Layer {

   private final CompassRoseRenderer _layerRenderer;

   public CompassRoseLayer(final Map map) {

      super(map);

      mRenderer = _layerRenderer = new CompassRoseRenderer();

      createBitmap();
   }

   private void createBitmap() {

      // check if layer is visible
      if (isEnabled() == false) {
         return;
      }

      final int imageSize = 120;

      try {

         // using 3D map compass image
         final URL bundleUrl = TourbookPlugin.getDefault().getBundle().getEntry("/images/map25/notched-compass.png");//$NON-NLS-1$
         final String fileName = NIO.getAbsolutePathFromBundleUrl(bundleUrl);

         try (FileInputStream stream = new FileInputStream(fileName)) {

            final Bitmap oscimBitmap = CanvasAdapter.decodeBitmap(stream);

            _layerRenderer.setBitmap(oscimBitmap, imageSize, imageSize);

         } catch (final IOException e1) {
            StatusUtil.showStatus(e1);
         }

      } catch (final IOException e) {
         StatusUtil.showStatus(e);
      }

   }

}
