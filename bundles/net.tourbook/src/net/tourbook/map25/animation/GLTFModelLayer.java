/*
 * Copyright 2014 Hannes Janetzek
 * Copyright 2018-2019 Gustl22
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
package net.tourbook.map25.animation;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;

import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.event.Event;
import org.oscim.gdx.poi3d.ModelPosition;
import org.oscim.layers.Layer;
import org.oscim.map.Map;

/**
 * Layer for glTF models, copied/modified from {@link org.oscim.gdx.poi3d.GdxModelLayer}
 */
public class GLTFModelLayer extends Layer implements Map.UpdateListener {

   private GLTFModelRenderer _gltfRenderer;

   private ModelPosition     _modelPosition;

   public GLTFModelLayer(final Map map) {

      super(map);

      mRenderer = _gltfRenderer = new GLTFModelRenderer(mMap);
   }

   public void dispose() {

      _gltfRenderer.dispose();
   }

   @Override
   public void onMapEvent(final Event event, final MapPosition mapPosition) {

      if (_gltfRenderer.scene == null) {

         // scene is not yet loaded
         return;
      }

      final int mapZoomLevel = mapPosition.zoomLevel;

      final int mapScale = 1 << mapZoomLevel;
      final int tileScale = Tile.SIZE << mapZoomLevel;

      final double latitude = MercatorProjection.toLatitude(mapPosition.y);
      final float groundScale = (float) MercatorProjection.groundResolutionWithScale(latitude, mapScale);

      float modelScale = 1f / groundScale;

      // increase model size to be more visible
      modelScale *= 2000;

      /*
       * Translate glTF model to map position
       */
      synchronized (_gltfRenderer) {

//         // remove if out of visible zoom range
//         _gltfRenderer.allModelInstances.removeAll(_allModelInstances, true);
//         if (mapZoomLevel >= 3 /* MIN_ZOOM */) {
//            _gltfRenderer.allModelInstances.addAll(_allModelInstances);
//         }

         final ModelInstance modelInstance = _gltfRenderer.scene.modelInstance;
         final ModelPosition modelPos = _modelPosition;

         final float dx = (float) ((modelPos.x - mapPosition.x) * tileScale);
         final float dy = (float) ((modelPos.y - mapPosition.y) * tileScale);
         final float dxScaled = dx / modelScale;
         final float dyScaled = dy / modelScale;

         final Matrix4 modelTransform = modelInstance.transform;

         // reset matrix to identity matrix
         modelTransform.idt();

         modelTransform.scale(modelScale, modelScale, modelScale);
         modelTransform.translate(dxScaled, dyScaled, 0);
         modelTransform.rotate(0, 0, 1, modelPos.rotation);
      }

      _gltfRenderer.mapCamera.setMapPosition(mapPosition.x, mapPosition.y, mapScale);
   }

   public void setupGLTFModel() {

      _modelPosition = new ModelPosition(47.275535, 8.625080, 90);
   }
}
