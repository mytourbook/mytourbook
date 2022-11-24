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

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;

import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.event.Event;
import org.oscim.gdx.GdxAssets;
import org.oscim.gdx.poi3d.ModelPosition;
import org.oscim.layers.Layer;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.map.Map;
import org.oscim.model.VtmModels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Layer for glTF models, copied/modified from {@link org.oscim.gdx.poi3d.GdxModelLayer}
 */
public class GLTFModelLayer extends Layer implements Map.UpdateListener {

   private static final Logger                          log                = LoggerFactory.getLogger(GLTFModelLayer.class);

   private static final int                             MIN_ZOOM           = BuildingLayer.MIN_ZOOM;

   private AssetManager                                 _assetManager;
   private GLTFModelRenderer                            _gltfRenderer;

   private boolean                                      _isLoading;

   /**
    * Key: {@link ModelPosition}<br>
    * Value: {@link ModelAndPathMT}
    */
   private java.util.Map<ModelPosition, ModelAndPathMT> _allModelData      = new HashMap<>();
   private Array<ModelInstance>                         _allModelInstances = new Array<>();

   public GLTFModelLayer(final Map map) {

      super(map);

      mRenderer = _gltfRenderer = new GLTFModelRenderer(mMap);

      // Material mat = new
      // Material(ColorAttribute.createDiffuse(Color.BLUE));
      // ModelBuilder modelBuilder = new ModelBuilder();
      // long attributes = Usage.Position | Usage.Normal |
      // Usage.TextureCoordinates;

      // mModel = modelBuilder.createSphere(10f, 10f, 10f, 12, 12,
      // mat, attributes);

      _assetManager = new AssetManager();
   }

   /**
    * Add model with specified path and position.
    *
    * @return the models position, can be modified during rendering e.g. to make animations.
    *         Don't forget to trigger map events (as it usually does if something changes).
    */
   public ModelPosition addModel(final String path, final double lat, final double lon, final float rotation) {

      final ModelPosition modelPosition = new ModelPosition(lat, lon, rotation);

      _allModelData.put(modelPosition, new ModelAndPathMT(path));

      _assetManager.load(path, Model.class);

      if (_isLoading == false) {
         _isLoading = true;
      }

      return modelPosition;
   }

   public ModelPosition addModel(final VtmModels model, final double lat, final double lon, final float rotation) {

      return addModel(GdxAssets.getAssetPath(model.getPath()), lat, lon, rotation);
   }

   public void dispose() {

      _assetManager.dispose();

      _gltfRenderer.dispose();
   }

   private void doneLoading() {

      for (final ModelAndPathMT modelAndPath : _allModelData.values()) {

         final Model gdxModel = _assetManager.get(modelAndPath.getPath());

         for (final Node node : gdxModel.nodes) {

            log.debug("loader node " + node.id);

            /* Use with {@link GdxRenderer3D} */
//
// GdxRenderer3D is not used
//
//            if (node.hasChildren() && (mG3d) instanceof GdxRenderer3D) {
//               if (model.nodes.size != 1) {
//                  throw new RuntimeException("Model has more than one node with GdxRenderer: " + model.toString());
//               }
//               node = node.getChild(0);
//               log.debug("loader node " + node.id);
//
//               model.nodes.removeIndex(0);
//               model.nodes.add(node);
//            }

            node.scale.set(1, 1, -1);
            node.rotation.setFromAxis(1, 0, 0, 90);
         }

         gdxModel.calculateTransforms();
         modelAndPath.setModel(gdxModel);
      }

      _isLoading = false;
   }

   public void onAppCreate() {
      // TODO Auto-generated method stub

   }

   @Override
   public void onMapEvent(final Event ev, final MapPosition mapPosition) {

//        if (ev == Map.CLEAR_EVENT) {
//             synchronized (g3d) {
//                g3d.instances.clear();
//            }
//        }

      if (_isLoading && _assetManager.update()) {
         doneLoading();
         refreshModelInstances();
      }

      if (_isLoading) {
         return;
      }

      final double latitude = MercatorProjection.toLatitude(mapPosition.y);
      final int mapScale = 1 << mapPosition.zoomLevel;
      final float groundScale = (float) MercatorProjection.groundResolutionWithScale(latitude, mapScale);

      float modelScale = 1f / groundScale;

      // increase model size
      modelScale *= 2000;

      synchronized (_gltfRenderer) {

         // remove if out of visible zoom range
         _gltfRenderer.allModelInstances.removeAll(_allModelInstances, true);
         if (mapPosition.getZoomLevel() >= 3 /* MIN_ZOOM */) {
            _gltfRenderer.allModelInstances.addAll(_allModelInstances);
         }

         for (final ModelInstance modelInstance : _allModelInstances) {

            final ModelPosition modelPos = (ModelPosition) modelInstance.userData;

            final float dx = (float) ((modelPos.x - mapPosition.x) * (Tile.SIZE << mapPosition.zoomLevel));
            final float dy = (float) ((modelPos.y - mapPosition.y) * (Tile.SIZE << mapPosition.zoomLevel));
            final float dxScaled = dx / modelScale;
            final float dyScaled = dy / modelScale;

            final Matrix4 modelTransform = modelInstance.transform;

            // reset matrix to identity matrix
            modelTransform.idt();

            modelTransform.scale(modelScale, modelScale, modelScale);
            modelTransform.translate(dxScaled, dyScaled, 0);
            modelTransform.rotate(0, 0, 1, modelPos.rotation);
         }
      }

      _gltfRenderer.mapCamera.setMapPosition(mapPosition.x, mapPosition.y, 1 << mapPosition.getZoomLevel());
   }

   private void refreshModelInstances() {

      _allModelInstances.clear();
      _gltfRenderer.allModelInstances.clear();

      for (final java.util.Map.Entry<ModelPosition, ModelAndPathMT> modelData : _allModelData.entrySet()) {

         final ModelAndPathMT modelAndMap = modelData.getValue();
         final ModelPosition modelPosition = modelData.getKey();

         final ModelInstance modelInstance = new ModelInstance(modelAndMap.getModel());

         // set model position into user data
         modelInstance.userData = modelPosition;

         _allModelInstances.add(modelInstance); // Local stored
         _gltfRenderer.allModelInstances.add(modelInstance); // g3d stored
      }
   }

   public void removeModel(final ModelPosition position) {

      _allModelData.remove(position);

      if (_isLoading == false) {
         refreshModelInstances();
      }
   }
}
