/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.animation;

import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.layers.Layer;
import org.oscim.map.Map;

/**
 * Layer for glTF models, original source {@link org.oscim.gdx.poi3d.GdxModelLayer}
 */
public class GLTFModel_Layer extends Layer implements Map.UpdateListener {

   private GLTFModel_Renderer _gltfRenderer;

   public GLTFModel_Layer(final Map map) {

      super(map);

      mRenderer = _gltfRenderer = new GLTFModel_Renderer(mMap);
   }

   public void dispose() {

      _gltfRenderer.dispose();
   }

   @Override
   public void onMapEvent(final Event event, final MapPosition mapPosition) {

      _gltfRenderer.onMapEvent(event, mapPosition);
   }

}
