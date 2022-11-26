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
package net.tourbook.map25.animation;

import static org.oscim.backend.GLAdapter.gl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Animation;
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
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;
import net.tourbook.map.player.MapPlayerManager;

import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.oscim.backend.GL;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.map.Map;
import org.oscim.map.Viewport;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.LayerRenderer;
import org.oscim.utils.geom.GeometryUtils;
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

   private MapPosition        _mapPosition;
   float[]                    _mapBox     = new float[8];

   private Scene              _scene;
   private SceneAsset         _sceneAsset;
   private SceneManager       _sceneManager;

   private DirectionalLightEx _light;
   private Cubemap            _environmentCubemap;
   private Cubemap            _diffuseCubemap;
   private Cubemap            _specularCubemap;
   private Texture            _brdfLUT;
   private SceneSkybox        _skybox;

   private BoundingBox        _modelBoundingBox;
   private Vector3            _boundingBoxCenter;
   private float              _boundingBoxMinMaxDistance;

//   private boolean             loading;

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

      // wood truck
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/basic_truck/scene.gltf"));

      // wood plane - light reflection
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/basic_plane/scene.gltf"));

      // simple bicycle
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/2d_bike__downloadable_for_first_10_users/scene.gltf"));

      // Hochrad - light reflection
      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/pennyfarthest_bicycle/scene.gltf"));

      // gears - light reflection
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/gears/scene.gltf"));

      // walking roboter
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/robot-_walk_animation/scene.gltf"));

      // skateboard
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/skateboard_animated_-_blockbench/scene.gltf"));

      // zeppelin
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/zeppelin_aircraft/scene.gltf"));

      // Locomotive frame
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/locomotive/scene.gltf"));

      // Alter Anhänger
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/medieval_cart/scene.gltf"));

      // z-fighting underneath
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/lawn_mower_low_poly/scene.gltf"));

      // has z-fighting
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/toy_truck/scene.gltf"));

      // Historic baloon with model issue
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/steampunk_air_baloon/scene.gltf"));

//
//    WITH EXCEPTION ISSUES
//

      // Array index out of range: 8
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/drunk_monster_truck/scene.gltf"));

      // too many bones: 64, max configured: 24
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/jo_on_bike__rigged__animated/scene.gltf"));

      return asset;
   }

   @Override
   public void render(final GLViewport viewport) {

      if (_scene == null || _mapPosition == null) {
         return;
      }

      final ShortArrayList animatedPositions = MapPlayerManager.getAnimatedPositions();
      if (animatedPositions == null) {
         return;
      }

//      final long start = System.nanoTime();

//    // remove if out of visible zoom range
//    _gltfRenderer.allModelInstances.removeAll(_allModelInstances, true);
//    if (mapZoomLevel >= 3 /* MIN_ZOOM */) {
//       _gltfRenderer.allModelInstances.addAll(_allModelInstances);
//    }

      _mapCamera.update(viewport);
      render_UpdateModelPosition_Static(animatedPositions);
//      render_UpdateModelPosition(animatedPositions);

      final MapPosition cameraMapPosition = _mapCamera.mMapPosition;
      final MapPosition viewportPosition = viewport.pos;

      gl.depthMask(true);

      if (viewportPosition.zoomLevel < 10) {
         gl.clear(GL.DEPTH_BUFFER_BIT);
      }

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

      final int numModels = 0;
      int numRendered = 0;

      final Viewport mapViewport = _map.viewport();
      mapViewport.getMapExtents(_mapBox, 10);

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

         final boolean isInMapBox = GeometryUtils.pointInPoly(_tempVector.x, _tempVector.y, _mapBox, 8, 0);

//       if (isInMapBox == false) {
//          continue;
//       }

//            modelBatch.render(modelInstance, lights);

         numRendered++;
//         }

         if (numRendered > 0) {

            final float deltaTime = Gdx.graphics.getDeltaTime();

            // render
//            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

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
    * Update model position
    *
    * @param animatedPositions
    */
   private void render_UpdateModelPosition(final ShortArrayList animatedPositions) {


      final ModelInstance modelInstance = _scene.modelInstance;
      final Matrix4 modelTransform = modelInstance.transform;

      final Object userData = modelInstance.userData;
      final int currentFrameNumber = MapPlayerManager.getCurrentFrameNumber();

      final int frameIndex = currentFrameNumber - 1;

      // get animated position
      final int xyPosIndex = frameIndex * 2;
      final int xyPrevPosIndex = xyPosIndex > 1 ? xyPosIndex - 2 : 0;

      final short pos1X = animatedPositions.get(xyPrevPosIndex);
      final short pos1Y = animatedPositions.get(xyPrevPosIndex + 1);
      final short pos2X = animatedPositions.get(xyPosIndex);
      final short pos2Y = animatedPositions.get(xyPosIndex + 1);

      final int mapZoomLevel = _mapPosition.zoomLevel;

      final int mapScale = 1 << mapZoomLevel;
      final int tileScale = Tile.SIZE << mapZoomLevel;

      final double latitude = MercatorProjection.toLatitude(_mapPosition.y);
      final float groundScale = (float) MercatorProjection.groundResolutionWithScale(latitude, mapScale);

      float modelScale = 1f / groundScale;

      /*
       * Adjust to a normalized size which depends on the model size because the models can have big
       * size differences
       */
      modelScale /= _boundingBoxMinMaxDistance;

      // increase model size to be more visible
//    modelScale *= 500_000;
      modelScale *= 20000;

      /*
       * Translate glTF model to the map position
       */

//    // remove if out of visible zoom range
//    _gltfRenderer.allModelInstances.removeAll(_allModelInstances, true);
//    if (mapZoomLevel >= 3 /* MIN_ZOOM */) {
//       _gltfRenderer.allModelInstances.addAll(_allModelInstances);
//    }

      float dx = (float) ((modelPositionX - _mapPosition.x) * tileScale);
      float dy = (float) ((modelPositionY - _mapPosition.y) * tileScale);

//      dx = pos1X;
//      dy = pos1Y;

      dx *= 1;
      dy *= 1;

      final float dxScaled = dx / modelScale;
      final float dyScaled = dy / modelScale;

      final Vector3 bbMin = _modelBoundingBox.min;
      final Vector3 bbMax = _modelBoundingBox.max;

      final float bbMinY = bbMin.y;
      final float bbMaxY = bbMax.y;
      final float zAdjustment = -bbMinY;// - bbMaxY;

      final Vector3 bboxCenter = _boundingBoxCenter;
      final float xAdjustment = bboxCenter.x;

      // reset matrix to identity matrix
      modelTransform.idt();

//      modelTransform.scale(modelScale, modelScale, modelScale);
//
//    modelTransform.translate(dxScaled - xAdjustment, dyScaled, zAdjustment);
//      modelTransform.translate(dxScaled, dyScaled, 0);
//
//      modelTransform.rotate(1, 0, 0, 90);
//      modelTransform.rotate(0, 1, 0, -90 - MapPlayerManager.getAnimatedAngle());

      modelTransform.scale(modelScale, modelScale, modelScale);
      modelTransform.translate(dxScaled - xAdjustment, dyScaled, zAdjustment);
      modelTransform.rotate(1, 0, 0, 90);
      modelTransform.rotate(0, 1, 0, -90 - MapPlayerManager.getAnimatedAngle());

   }

   /**
    * Update model position
    *
    * @param animatedPositions
    */
   private void render_UpdateModelPosition_Static(final ShortArrayList animatedPositions) {

      // lago di garda
//      final ModelPosition modelPosition = new ModelPosition(45.876624, 10.865479, 0);

      // hinwiler autobahn rondell
//      final double modelPositionX = MercatorProjection.longitudeToX(8.818411);
//      final double modelPositionY = MercatorProjection.latitudeToY(47.288967);

      // center of Amden tour
      final double modelPositionX = MercatorProjection.longitudeToX(9.122086);
      final double modelPositionY = MercatorProjection.latitudeToY(47.199977);

      final ModelInstance modelInstance = _scene.modelInstance;
      final Matrix4 modelTransform = modelInstance.transform;

      final int mapZoomLevel = _mapPosition.zoomLevel;

      final int mapScale = 1 << mapZoomLevel;
      final int tileScale = Tile.SIZE << mapZoomLevel;

      final double latitude = MercatorProjection.toLatitude(_mapPosition.y);
      final float groundScale = (float) MercatorProjection.groundResolutionWithScale(latitude, mapScale);

      float modelScale = 1f / groundScale;

      /*
       * Adjust to a normalized size which depends on the model size because the models can have big
       * size differences
       */
      modelScale /= _boundingBoxMinMaxDistance;

      // increase model size to be more visible
      modelScale *= 20000;

      /*
       * Translate glTF model to the map position
       */
      final float dx = (float) ((modelPositionX - _mapPosition.x) * tileScale);
      final float dy = (float) ((modelPositionY - _mapPosition.y) * tileScale);

      final float dxScaled = dx / modelScale;
      final float dyScaled = dy / modelScale;

      final Vector3 bbMin = _modelBoundingBox.min;
      final Vector3 bbMax = _modelBoundingBox.max;

      final float bbMinY = bbMin.y;
      final float bbMaxY = bbMax.y;
      final float zAdjustment = -bbMinY;// - bbMaxY;

      final Vector3 bboxCenter = _boundingBoxCenter;
      final float xAdjustment = bboxCenter.x;

      // reset matrix to identity matrix
      modelTransform.idt();

      modelTransform.scale(modelScale, modelScale, modelScale);
      modelTransform.translate(dxScaled - xAdjustment, dyScaled, zAdjustment);
      modelTransform.rotate(1, 0, 0, 90);
      modelTransform.rotate(0, 1, 0, -90 - MapPlayerManager.getAnimatedAngle());
   }

   void setMapPosition(final MapPosition mapPosition) {

      if (_mapCamera == null) {
         return;
      }

      _mapPosition = mapPosition;

      /*
       * Camera position must be set BEFORE render() method otherwise the old position is used
       * (maybe for one frame) and it is flickering
       */
      final int mapZoomLevel = mapPosition.zoomLevel;
      final int mapScale = 1 << mapZoomLevel;

      _mapCamera.setMapPosition(mapPosition.x, mapPosition.y, mapScale);
   }

   @Override
   public boolean setup() {

      _sceneAsset = loadGLTFModel();

      _scene = new Scene(_sceneAsset.scene);

      // get bounding box
      _modelBoundingBox = new BoundingBox();
      _scene.modelInstance.calculateBoundingBox(_modelBoundingBox);
      _boundingBoxMinMaxDistance = _modelBoundingBox.max.dst(_modelBoundingBox.min);
      _boundingBoxCenter = _modelBoundingBox.getCenter(new Vector3());

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
