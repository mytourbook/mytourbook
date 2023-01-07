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
import net.mgsx.gltf.scene3d.utils.IBLBuilder;
import net.tourbook.map.player.MapPlayerData;
import net.tourbook.map.player.MapPlayerManager;
import net.tourbook.map25.Map25ConfigManager;

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

   private MapPosition        _currentMapPosition;
   float[]                    _mapBox     = new float[8];

   private Scene              _scene;

   private SceneAsset         _sceneAsset;
   private SceneManager       _sceneManager;
   private DirectionalLightEx _light;

   private Cubemap            _environmentCubemap;
   private Cubemap            _diffuseCubemap;
   private Cubemap            _specularCubemap;
   private Texture            _brdfLUT;
//   private SceneSkybox        _skybox;
   private BoundingBox        _modelBoundingBox;

//   private Vector3            _boundingBoxCenter;
   private float  _boundingBoxMinMaxDistance;

   /**
    * Angle that the model is looking forward
    */
   private float  _modelForwardAngle;

   /**
    * The model length needs a factor that the top of the symbol is not before the geo location
    */
   private double _modelCenterToForwardFactor;

   private float  _prevDx;
   private float  _prevDy;
   private int    _prevPositionIndex;

   public GLTFModel_Renderer(final Map map) {

      _map = map;
   }

   private void activateFirstAnimation() {

      final Array<Animation> animations = _sceneAsset.scene.model.animations;

      if (animations != null && animations.size > 0) {

         final String animationID = animations.get(0).id;

         _scene.animationController.setAnimation(animationID, -1);

         System.out.println("Number of animations: " + animations.size + "  using " + animationID);
      }
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
      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/MT/skateboard/mt-skateboard.gltf"));
      _modelForwardAngle = 90;
      _modelCenterToForwardFactor = 1.4;

      // painted bicycle
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/MT/simple-bicycle/simple-bicycle.gltf"));
//      _modelForwardAngle = 90;
//      _modelCenterToForwardFactor = 5;

//      // hochrad
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/MT/hochrad/hochrad.gltf"));
//      _modelForwardAngle = -90;
//      _modelCenterToForwardFactor = -7;

      // wood truck
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/MT/wood-truck/wood-truck.gltf"));
//      _modelForwardAngle = -90;
//      _modelCenterToForwardFactor = -7;

      // wood plane
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/MT/wood-plane/wood-plane.gltf"));

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

      if (mapEvent != Map.POSITION_EVENT && mapEvent != Map.SCALE_EVENT) {

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

      if (Map25ConfigManager.getActiveTourTrackConfig().arrow_IsAnimate == false) {
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

      if (MapPlayerManager.isCompileMapScaleModified()) {

         _mapCamera.setMapPosition(

               _currentMapPosition.x,
               _currentMapPosition.y,
               MapPlayerManager.getCompileMapScale());
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

//      final int numModels = 0;
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

      final MapPlayerData mapPlayerData = MapPlayerManager.getMapPlayerData();
      if (mapPlayerData == null) {
         return;
      }

      final int[] allNotClipped_GeoLocationIndices = mapPlayerData.allNotClipped_GeoLocationIndices;
      final int numGeoLocationIndices = allNotClipped_GeoLocationIndices.length;

      if (numGeoLocationIndices <= 0) {
         return;
      }

      final double currentMapScale = _currentMapPosition.scale;
      final int currentMapZoomLevel = _currentMapPosition.zoomLevel;

      final int mapScale = 1 << currentMapZoomLevel;
      final int tileScale = Tile.SIZE << currentMapZoomLevel;

      int geoLocationIndex_0 = 0;
      int geoLocationIndex_1 = 0;
      double[] allProjectedPoints = mapPlayerData.allProjectedPoints;
      int numProjectedPoints = allProjectedPoints.length;
      int positionIndex_0;
      int positionIndex_1;
      double exactPositionIndex = 0;

      // compute frame position from relative position
      double relativePosition = MapPlayerManager.getRelativePosition();

      if (relativePosition > 2) {

         // end...start + forward

         relativePosition = relativePosition - 2;
      }

      if (relativePosition > 1 || relativePosition < 0) {

         // move model on RETURN TRACK

         final double relativeReturnPosition;

         if (relativePosition > 1) {

            // end...start
            relativeReturnPosition = relativePosition - 1;

         } else {

            // start...end
            relativeReturnPosition = relativePosition + 1;
         }

         allProjectedPoints = mapPlayerData.allProjectedPoints_ReturnTrack;

         numProjectedPoints = allProjectedPoints.length;
         final int numReturnPositions = numProjectedPoints / 2;

         exactPositionIndex = numReturnPositions * relativeReturnPosition;
         positionIndex_0 = (int) exactPositionIndex;
         positionIndex_0 = MathUtils.clamp(positionIndex_0, 0, numReturnPositions - 1);

         geoLocationIndex_0 = positionIndex_0;
         geoLocationIndex_1 = positionIndex_0 <= numReturnPositions - 2
               ? positionIndex_0 + 1
               : positionIndex_0;

      } else {

         // move model between start and end -> 0...1

         exactPositionIndex = numGeoLocationIndices * relativePosition;
         positionIndex_0 = (int) exactPositionIndex;
         positionIndex_0 = MathUtils.clamp(positionIndex_0, 0, numGeoLocationIndices - 1);

         positionIndex_1 = positionIndex_0 <= numGeoLocationIndices - 2
               ? positionIndex_0 + 1
               : positionIndex_0;

         geoLocationIndex_0 = allNotClipped_GeoLocationIndices[positionIndex_0];
         geoLocationIndex_1 = allNotClipped_GeoLocationIndices[positionIndex_1];

//         System.out.println(UI.timeStamp()
//
//               + " positionIndex: " + positionIndex
//               + "  exactPositionIndex:" + String.format("%7.4f", exactPositionIndex)
//
////                  + "  relativePosition:" + String.format("%7.4f", relativePosition)
//
//         );
// TODO remove SYSTEM.OUT.PRINTLN

      }

      // move model along the tour track

      final int projectedIndex_0 = geoLocationIndex_0 * 2;
      final int projectedIndex_1 = geoLocationIndex_1 * 2;
      double projectedPositionX = allProjectedPoints[projectedIndex_0];
      double projectedPositionY = allProjectedPoints[projectedIndex_0 + 1];

      /*
       * Do micro movements according to the exactly relative position
       */
      final double projectedPositionX_0 = projectedPositionX;
      final double projectedPositionY_0 = projectedPositionY;
      final double projectedPositionX_1 = allProjectedPoints[projectedIndex_1];
      final double projectedPositionY_1 = allProjectedPoints[projectedIndex_1 + 1];

      final double projectedPositionX_Diff = projectedPositionX_1 - projectedPositionX_0;
      final double projectedPositionY_Diff = projectedPositionY_1 - projectedPositionY_0;

      // 0...1
      final double microIndex = exactPositionIndex - (int) exactPositionIndex;

      final double advanceX = projectedPositionX_Diff * microIndex;
      final double advanceY = projectedPositionY_Diff * microIndex;

      projectedPositionX = projectedPositionX_0 + advanceX;
      projectedPositionY = projectedPositionY_0 + advanceY;

//         if (positionIndex_0 != _prevPositionIndex) {
//
//            _prevPositionIndex = positionIndex_0;
//
//            System.out.println();
//            System.out.println(UI.timeStamp()
//
//                  + "  PositionX_0:" + String.format("%11.8f", projectedPositionX_0)
//                  + "  PositionX_Diff:" + String.format("%13.10f", projectedPositionX_Diff)
//                  + "  advanceX:" + String.format("%13.10f", advanceX)
//                  + "  PositionX:" + String.format("%13.10f", projectedPositionX)
//
//                  + "  index:" + String.format("%6d", positionIndex_0)
//                  + "  microIndex:" + String.format("%6.4f", microIndex)
//
//            );
//         }
//TODO remove SYSTEM.OUT.PRINTLN

      /*
       * Translate glTF model to the map position
       */
      final float dx = (float) ((projectedPositionX - _currentMapPosition.x) * tileScale);
      final float dy = (float) ((projectedPositionY - _currentMapPosition.y) * tileScale);

      /*
       * Compute model scale
       */
      float modelScale;

      final double latitude = MercatorProjection.toLatitude(_currentMapPosition.y);
      final float groundScale = (float) MercatorProjection.groundResolutionWithScale(latitude, mapScale);

      final boolean isFixedSize = true;

      if (isFixedSize) {

         final int modelSize = 300;

         // viewport scale 2 map scale: 1...2
         final float vp2mp = (float) (currentMapScale / MapPlayerManager.getCompileMapScale());

         final float vp2mpModelSize = modelSize / vp2mp;

         /*
          * This algorithm is not perfect as the model can still be flickering (size is larger or
          * smaller) but it is better than nothing. I spend 2 full days to get the flickering partly
          * fixed
          */
         if (vp2mp < 1.0) {

            modelScale = vp2mpModelSize / 2;

         } else if (vp2mp > 2.0) {

            modelScale = vp2mpModelSize * 2;

         } else {

            modelScale = vp2mpModelSize;
         }

         modelScale /= _boundingBoxMinMaxDistance;

      } else {

         modelScale = 1f / groundScale;

         /*
          * Adjust to a normalized size which depends on the model size because the models can have
          * big size differences
          */
         modelScale /= _boundingBoxMinMaxDistance;

         // increase model size to be more visible
         modelScale *= 100;
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
      float animationAngle = -MapPlayerManager.getAnimationForwardAngle();
      animationAngle += _modelForwardAngle;

      final double halfSize = modelScale / 2;
      final double center2BorderSize = halfSize * _modelCenterToForwardFactor;
      final float forwardX = (float) (center2BorderSize * MathUtils.cosDeg(animationAngle));
      final float forwardY = (float) (center2BorderSize * MathUtils.sinDeg(animationAngle));

      final Matrix4 modelTransform = _scene.modelInstance.transform;

      // reset matrix to identity matrix
      modelTransform.idt();

      modelTransform.scale(modelScale, modelScale, modelScale);

      modelTransform.rotate(1, 0, 0, 90);
      modelTransform.rotate(0, 1, 0, animationAngle);

      // move to the track position
      final float modelPosX = dx + forwardX;
      final float modelPosY = dy + forwardY;
      modelTransform.trn(modelPosX, modelPosY, 0);

//      if (dx != _prevDx || dy != _prevDy) {
//
//         _prevDx = dx;
//         _prevDy = dy;
//
//         System.out.println(UI.timeStamp()
//
//               + "  dx:" + String.format("%10.2f", dx)
//               + "  dy:" + String.format("%10.2f", dy)
//               + "  modelX:" + String.format("%10.4f", modelPosX)
//               + "  modelY:" + String.format("%10.4f", modelPosY)
//               + "  index:" + String.format("%6d", (int) exactPositionIndex)
//               + "  microIndex:" + String.format("%6.4f", microIndex)
//
//         );
////TODO remove SYSTEM.OUT.PRINTLN
//
//         if (microIndex < 0.0001 || microIndex > 0.9999) {
//            System.out.println();
//         }
////TODO remove SYSTEM.OUT.PRINTLN
//      }
   }

   @Override
   public boolean setup() {

      _sceneAsset = loadGLTFModel();

      _scene = new Scene(_sceneAsset.scene);

      // get bounding box
      _modelBoundingBox = new BoundingBox();
      _scene.modelInstance.calculateBoundingBox(_modelBoundingBox);
      _boundingBoxMinMaxDistance = _modelBoundingBox.max.dst(_modelBoundingBox.min);
//      _boundingBoxCenter = _modelBoundingBox.getCenter(new Vector3());

      _sceneManager = new SceneManager();
      _sceneManager.addScene(_scene);

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
      _brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

      _sceneManager.setAmbientLight(1f);
      _sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, _brdfLUT));
      _sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(_specularCubemap));
      _sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(_diffuseCubemap));

      // setup skybox
//      skybox = new SceneSkybox(environmentCubemap);
//      sceneManager.setSkyBox(skybox);

      activateFirstAnimation();

      return true;
   }

   @Override
   public synchronized void update(final GLViewport viewport) {

      setReady(true);

      if (isReady() == false) {

         _mapCamera.setPosition(viewport.pos);

         setReady(true);
      }

   }
}
