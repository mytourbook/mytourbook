/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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

import static org.oscim.backend.GLAdapter.gl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;

import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneModel;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;
import net.tourbook.Messages;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.map.model.MapModel;
import net.tourbook.map.model.MapModelManager;
import net.tourbook.map.player.ModelPlayerManager;

import org.oscim.backend.GL;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.event.Event;
import org.oscim.map.Map;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.LayerRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer/shader for the glTF model
 * <p>
 * Original source {@link org.oscim.gdx.poi3d.GdxRenderer3D2}
 */
public class GLTFModel_Renderer extends LayerRenderer {

   static final Logger        log         = LoggerFactory.getLogger(GLTFModel_Renderer.class);

   private Map                _map;
   private MapCameraMT        _mapCamera;

   private Vector3            _tempVector = new Vector3();

   /**
    * Map position for the current frame
    */
   private MapPosition        _currentMapPosition;

   float[]                    _mapBox     = new float[8];

   private Scene              _scene;
   private SceneAsset         _sceneAsset;
   private SceneManager       _sceneManager;

   private DirectionalLightEx _light;

   private float              _modelBoundingBox_MinMaxDistance;

   private Cubemap            _environmentCubemap;
   private Cubemap            _diffuseCubemap;
   private Cubemap            _specularCubemap;
   private Texture            _brdfLUT;
// private SceneSkybox        _skybox;

   /**
    * Angle in degrees that the model is looking forward
    */
   private float _modelForwardAngle;

   /**
    * The model length needs a factor that the top of the symbol is not before the geo location
    */
   private float _modelCenterToForwardFactor;

//   private float  _prevDx;
//   private float  _prevDy;
//   private double _prevRelativePosition;

   public GLTFModel_Renderer(final Map map) {

      _map = map;

      MapModelManager.setGLTFRenderer(this);
   }

   public void dispose() {

      if (_sceneManager == null) {
         return;
      }

      _sceneManager.dispose();
      _sceneAsset.dispose();

      _environmentCubemap.dispose();
      _diffuseCubemap.dispose();
      _specularCubemap.dispose();

      _brdfLUT.dispose();
//      skybox.dispose();
   }

   private SceneAsset loadGLTFModel() {

      SceneAsset asset = null;

      /*
       * MT models
       */

      // skateboard
      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/MT/skateboard/mt-skateboard.gltf")); //$NON-NLS-1$
      _modelForwardAngle = 90;
      _modelCenterToForwardFactor = 1.4f;

      // wood plane
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/MT/wood-plane/wood-plane.gltf"));

      // painted bicycle
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/MT/simple-bicycle/simple-bicycle.gltf"));
//      _modelForwardAngle = 90;
//      _modelCenterToForwardFactor = 5;

      // hochrad
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/MT/hochrad/hochrad.gltf"));
//      _modelForwardAngle = -90;
//      _modelCenterToForwardFactor = -7;

      // wood truck
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/MT/wood-truck/wood-truck.gltf"));
//      _modelForwardAngle = -0;
//      _modelCenterToForwardFactor = -7;

      /*
       * sketchfab.com models
       */

      // simple bicycle with globe
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/2d_bike__downloadable_for_first_10_users/scene.gltf"));
//      _modelForwardAngle = 90;
//      _modelZAdjustment = MODEL_Z_ADJUSTMENT.BoundingBox_Min_Negative;

      // Hochrad - light reflection
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/pennyfarthest_bicycle/scene.gltf"));
//      _modelForwardAngle = -90;
//      _modelCenterToForwardFactor = 1;
//      _modelZAdjustment = MODEL_Z_ADJUSTMENT.BoundingBox_Min_Negative;

      // gears - light reflection
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/gears/scene.gltf"));
//      _modelForwardAngle = 90;
//      _modelZAdjustment = MODEL_Z_ADJUSTMENT.BoundingBox_Min_Negative;

      // walking roboter
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/robot-_walk_animation/scene.gltf"));

      // skateboard
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/skateboard_animated_-_blockbench/scene.gltf"));
//      _modelForwardAngle = 90;

      // zeppelin
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/zeppelin_aircraft/scene.gltf"));
//      _modelForwardAngle = 180;

      // Locomotive frame
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/locomotive/scene.gltf"));
//      _modelForwardAngle = 180;
//      _modelZAdjustment = MODEL_Z_ADJUSTMENT.BoundingBox_Min_Negative;
//      _modelCenterToForwardFactor = 5;

      // Alter Anhänger
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/medieval_cart/scene.gltf"));
//      _modelForwardAngle = 180;

      // z-fighting underneath
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/lawn_mower_low_poly/scene.gltf"));

      // has z-fighting
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/toy_truck/scene.gltf"));

      // Historic baloon with model issue
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/steampunk_air_baloon/scene.gltf"));

      // wood truck with position issue
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/basic_truck/scene.gltf"));

      // wood plane - light reflection
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/basic_plane/scene.gltf"));
//      _modelZAdjustment = MODEL_Z_ADJUSTMENT.BoundingBox_Min_Negative;

//
//    WITH EXCEPTION ISSUES
//

      // Array index out of range: 8
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/drunk_monster_truck/scene.gltf"));

      // too many bones: 64, max configured: 24
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/jo_on_bike__rigged__animated/scene.gltf"));

      return asset;
   }

   /**
    * Set current map position
    *
    * @param mapEvent
    * @param mapPosition
    */
   void onMapEvent(final Event mapEvent, final MapPosition mapPosition) {

      if (mapPosition != null && _currentMapPosition == null) {

         // ensure that the initial map position is set, otherwise the model is not rendered

      } else if (mapEvent != Map.POSITION_EVENT && mapEvent != Map.SCALE_EVENT) {

         // ignore other events

         return;
      }

      if (_mapCamera == null) {
         return;
      }

      _currentMapPosition = mapPosition;

      final int mapZoomLevel = mapPosition.zoomLevel;
      final int mapScale = 1 << mapZoomLevel;

      _mapCamera.setMapPosition(mapPosition.x, mapPosition.y, mapScale);
   }

   @Override
   public void render(final GLViewport viewport) {

      if (ModelPlayerManager.isMapModelVisible() == false) {
         return;
      }

      if (_scene == null || _currentMapPosition == null) {
         return;
      }

//      final long start = System.nanoTime();

//    // remove if out of visible zoom range
//    _gltfRenderer.allModelInstances.removeAll(_allModelInstances, true);
//    if (mapZoomLevel >= 3 /* MIN_ZOOM */) {
//       _gltfRenderer.allModelInstances.addAll(_allModelInstances);
//    }

      if (ModelPlayerManager.isCompileMapScaleModified()) {

         _mapCamera.setMapPosition(

               _currentMapPosition.x,
               _currentMapPosition.y,
               ModelPlayerManager.getCompileMapScale());
      }

      _mapCamera.update(viewport);

      render_UpdateModelPosition();

      gl.depthMask(true);

//      if (viewportPosition.zoomLevel < 10) {
//         gl.clear(GL.DEPTH_BUFFER_BIT);
//      }

      // Unbind via GLState to ensure no buffer is replaced by accident
      GLState.bindElementBuffer(GLState.UNBIND);
      GLState.bindBuffer(GL.ARRAY_BUFFER, GLState.UNBIND);

      // set state that is expected after modelBatch.end();
      // modelBatch keeps track of its own state
      GLState.enableVertexArrays(GLState.DISABLED, GLState.DISABLED);
      GLState.bindTex2D(GLState.DISABLED);
      GLState.useProgram(GLState.DISABLED);
      GLState.test(false, false);
      GLState.blend(false);

      // gl.cullFace(GL.BACK);
      // flip front face cause of mirror inverted y-axis
      gl.frontFace(GL.CCW);

//    final int numModels = 0;
      int numRendered = 0;

      final MapPosition cameraMapPosition = _mapCamera.mMapPosition;
      final MapPosition viewportPosition = viewport.pos;

      _map.viewport().getMapExtents(_mapBox, 10);

      final float dx = (float) (cameraMapPosition.x - viewportPosition.x) * (Tile.SIZE << cameraMapPosition.zoomLevel);
      final float dy = (float) (cameraMapPosition.y - viewportPosition.y) * (Tile.SIZE << cameraMapPosition.zoomLevel);
      final float scale = (float) (cameraMapPosition.scale / viewportPosition.scale);

      for (int i = 0; i < 8; i += 2) {

         _mapBox[i] *= scale;
         _mapBox[i] -= dx;
         _mapBox[i + 1] *= scale;
         _mapBox[i + 1] -= dy;
      }

      synchronized (this) {

//         for (final ModelInstance modelInstance : allModelInstances) {

         final ModelInstance modelInstance = _scene.modelInstance;

         modelInstance.transform.getTranslation(_tempVector);

         _tempVector.scl(0.9f, 0.9f, 1);

//         final boolean isInMapBox = GeometryUtils.pointInPoly(_tempVector.x, _tempVector.y, _mapBox, 8, 0);

//       if (isInMapBox == false) {
//          continue;
//       }

         numRendered++;
//         }

         if (numRendered > 0) {

            final float deltaTime = Gdx.graphics.getDeltaTime();

            // render
            _sceneManager.update(deltaTime);
            _sceneManager.render();
         }
      }

      // GLUtils.checkGlError("<" + TAG);

      gl.frontFace(GL.CW);
      gl.depthMask(false);
      GLState.bindElementBuffer(GLState.UNBIND);
      GLState.bindBuffer(GL.ARRAY_BUFFER, GLState.UNBIND);

      // GLState.bindTex2D(-1);
      // GLState.useProgram(-1);

//      final long end = System.nanoTime();
//      final float timeDiff = (float) (end - start) / 1000000;
//      System.out.println(String.format("%6.4f ms   %d / %d", timeDiff, numModels, numRendered));
//      // TODO remove SYSTEM.OUT.PRINTLN
   }

   /**
    * Update model position, partly copied from {@link org.oscim.gdx.poi3d.GdxModelLayer}
    */
   private void render_UpdateModelPosition() {

      final double[] projectedPositionXY = ModelPlayerManager.getCurrentProjectedPosition();

      if (projectedPositionXY == null) {
         return;
      }

      /*
       * Translate glTF model to the map position
       */
      final double currentMapScale = _currentMapPosition.scale;
      final int currentMapZoomLevel = _currentMapPosition.zoomLevel;

      final int mapScale = 1 << currentMapZoomLevel;
      final int tileScale = Tile.SIZE << currentMapZoomLevel;

      final float dX = (float) ((projectedPositionXY[0] - _currentMapPosition.x) * tileScale);
      final float dY = (float) ((projectedPositionXY[1] - _currentMapPosition.y) * tileScale);

//TODO dX/dY could be optimized for invisible positions

//      if (dX != _prevDx || dY != _prevDy || relativePosition != _prevRelativePosition) {
//
//         System.out.println(UI.timeStamp()
//
//               + "  dx:" + String.format("%7.2f", dX /*- _prevDx*/)
//               + "  dy:" + String.format("%7.2f", dY /*- _prevDy*/)
//
//               + "  geoIndex:" + String.format("%6d", geoLocationIndex_0)
//               + "  index_0:" + String.format("%6d", positionIndex_0)
//               + "  microIndex:" + String.format("%6.4f", microIndex)
//
//         );
//
//         _prevDx = dX;
//         _prevDy = dY;
//         _prevRelativePosition = relativePosition;
//
//         if (microIndex < 0.0001 || microIndex > 0.9999) {
//            System.out.println();
//         }
////TODO remove SYSTEM.OUT.PRINTLN
//      }

      /*
       * Compute model scale
       */
      float modelSize;

      final double latitude = MercatorProjection.toLatitude(_currentMapPosition.y);
      final float groundScale = (float) MercatorProjection.groundResolutionWithScale(latitude, mapScale);

      final boolean isFixedSize = true;

      if (isFixedSize) {

         // viewport scale 2 map scale: 1...2
         final float vp2mp = (float) (currentMapScale / ModelPlayerManager.getCompileMapScale());

         final float vp2mpModelSize = ModelPlayerManager.getModelSize() / vp2mp;

         /**
          * This algorithm is not perfect as the model can still be flickering (size is larger or
          * smaller) but it is better than nothing. I spend 2 full days to get the flickering partly
          * fixed
          */
         if (vp2mp < 1.0) {

            modelSize = vp2mpModelSize / 2;

         } else if (vp2mp > 2.0) {

            modelSize = vp2mpModelSize * 2;

         } else {

            modelSize = vp2mpModelSize;
         }

         modelSize /= _modelBoundingBox_MinMaxDistance;

      } else {

         modelSize = 1f / groundScale;

         /*
          * Adjust to a normalized size which depends on the model size because the models can have
          * big size differences
          */
         modelSize /= _modelBoundingBox_MinMaxDistance;

         // increase model size to be more visible
         modelSize *= 100;
      }

      /**
       * Adjust the center of the symbol to be at the top of the symbol, needs some maths with
       * sinus/cosinus.
       * <p>
       * It took me many hours to get this math fixed because this matrix cannot do it as in the
       * shader code, e.g.
       * - rotate<br>
       * - scale<br>
       * - translate head to center<br>
       * - translate symbol to geo location<br>
       */
      float animationAngle = -ModelPlayerManager.getModelAngle();
      animationAngle += _modelForwardAngle;

      final double modelHalfSize = modelSize / 2;
      final float modelCenterToForwardFactor = _modelCenterToForwardFactor == 0 ? 0.001f : _modelCenterToForwardFactor;

      final double center2BorderSize = modelHalfSize * modelCenterToForwardFactor;

      final float forwardX = (float) (center2BorderSize * MathUtils.cosDeg(animationAngle));
      final float forwardY = (float) (center2BorderSize * MathUtils.sinDeg(animationAngle));

      final Matrix4 modelTransform = _scene.modelInstance.transform;

      // reset matrix to identity matrix
      modelTransform.idt();

      modelTransform.scale(modelSize, modelSize, modelSize);

      modelTransform.rotate(1, 0, 0, 90);
      modelTransform.rotate(0, 1, 0, animationAngle);

      // move to the track position
      final float modelPosX = dX + forwardX;
      final float modelPosY = dY + forwardY;
      modelTransform.trn(modelPosX, modelPosY, 0);
   }

   @Override
   public boolean setup() {

      _sceneManager = new SceneManager();

      _mapCamera = new MapCameraMT(_map);
      _sceneManager.setCamera(_mapCamera);

      // setup light
      _light = new DirectionalLightEx();
      _light.direction.set(1, -3, 1).nor();
      _light.color.set(Color.WHITE);
      _sceneManager.environment.add(_light);

      // setup quick IBL (image based lighting)
      final IBLBuilder iblBuilder = IBLBuilder.createOutdoor(_light);
      _environmentCubemap = iblBuilder.buildEnvMap(1024);
      _diffuseCubemap = iblBuilder.buildIrradianceMap(256);
      _specularCubemap = iblBuilder.buildRadianceMap(10);
      iblBuilder.dispose();

      // This texture is provided by the library, no need to have it in your assets.
      _brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png")); //$NON-NLS-1$

      _sceneManager.setAmbientLight(1f);
      _sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, _brdfLUT));
      _sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(_specularCubemap));
      _sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(_diffuseCubemap));

      // setup skybox
//    skybox = new SceneSkybox(environmentCubemap);
//    sceneManager.setSkyBox(skybox);

      setupScene_RunningInGLThread(MapModelManager.getSelectedModel());

      return true;
   }

   public void setupScene(final MapModel mapModel) {

      _map.post(() -> {
         setupScene_RunningInGLThread(mapModel);
      });
   }

   private void setupScene_RunningInGLThread(final MapModel mapModel) {

      /*
       * Cleanup previous scene
       */
      if (_scene != null) {
         _sceneManager.removeScene(_scene);
      }

      if (_sceneAsset != null) {
         _sceneAsset.dispose();
      }

      /*
       * Load GLTF model
       */
      try {

         _sceneAsset = new GLTFLoader().load(Gdx.files.absolute(mapModel.filepath));

      } catch (final Exception e) {

         // model could not be loaded -> load default default model

         StatusUtil.showStatus(String.format(

               Messages.Model_Player_Error_InvalidModelFilepath,
               mapModel.name,
               mapModel.filepath),

               e);

         final MapModel defaultModel = MapModelManager.getDefaultDefaultModel();

         _sceneAsset = new GLTFLoader().load(Gdx.files.absolute(defaultModel.filepath));
      }

      updateUI_ModelProperties(mapModel);

      final SceneModel sceneModel = _sceneAsset.scene;

      /*
       * Setup new scene
       */
      _scene = new Scene(sceneModel);

      // get model bounding box
      final BoundingBox modelBoundingBox = new BoundingBox();
      _scene.modelInstance.calculateBoundingBox(modelBoundingBox);
      _modelBoundingBox_MinMaxDistance = modelBoundingBox.max.dst(modelBoundingBox.min);

      _sceneManager.addScene(_scene);

      final Array<Animation> allModelAnimations = sceneModel.model.animations;
      if (allModelAnimations != null && allModelAnimations.size > 0) {

         final String animationID = allModelAnimations.get(0).id;

         _scene.animationController.setAnimation(animationID, -1);

         System.out.println("Number of animations: " + allModelAnimations.size + "  using " + animationID); //$NON-NLS-1$ //$NON-NLS-2$
      }
   }

   @Override
   public synchronized void update(final GLViewport viewport) {

      setReady(true);

      if (isReady() == false) {

         _mapCamera.setPosition(viewport.pos);

         setReady(true);
      }

   }

   /**
    * Update model properties
    *
    * @param mapModel
    */
   public void updateUI_ModelProperties(final MapModel mapModel) {

      _modelForwardAngle = mapModel.forwardAngle;
      _modelCenterToForwardFactor = mapModel.headPositionFactor;
   }
}
