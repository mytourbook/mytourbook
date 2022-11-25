/*
 * Copyright 2014 Hannes Janetzek
 * Copyright 2018 Gustl22
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
package net.tourbook.map25.animation;

import static org.oscim.backend.GLAdapter.gl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.math.Vector3;
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

import org.oscim.backend.GL;
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
 * Gdx renderer for more complex 3D models.
 */
public class GLTFModelRenderer extends LayerRenderer {

   static final Logger log = LoggerFactory.getLogger(GLTFModelRenderer.class);

   private Map         mMap;
   public MapCameraMT  mapCamera;

//   private ModelBatch          modelBatch;
//   private Environment         lights;

//   public Array<ModelInstance> allModelInstances = new Array<>();

   private Vector3            _tempVector = new Vector3();

   float[]                    _mapBox     = new float[8];

   private SceneAsset         sceneAsset;

   Scene                      scene;
   private SceneManager       sceneManager;

   private DirectionalLightEx light;
   private Cubemap            environmentCubemap;
   private Cubemap            diffuseCubemap;
   private Cubemap            specularCubemap;
   private Texture            brdfLUT;
   private SceneSkybox        skybox;

//   private boolean             loading;

   public GLTFModelRenderer(final Map map) {

      mMap = map;
   }

   private void activateFirstAnimation() {

      final Array<Animation> animations = sceneAsset.scene.model.animations;

      if (animations != null && animations.size > 0) {

         final String animationID = animations.get(0).id;

         scene.animationController.setAnimation(animationID, -1);

         System.out.println("Number of animations: " + animations.size + "  using " + animationID);
      }
   }

   public void dispose() {

      sceneManager.dispose();
      sceneAsset.dispose();

      environmentCubemap.dispose();
      diffuseCubemap.dispose();
      specularCubemap.dispose();

      brdfLUT.dispose();
      skybox.dispose();
   }

   private SceneAsset loadGLTFModel() {

      SceneAsset asset = null;

      // slime
//      asset = new GLTFLoader().load(Gdx.files.internal("models/Alien Slime.gltf"));

      // wood truck
      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/basic_truck/scene.gltf"));

      // wood plane - light reflection
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/basic_plane/scene.gltf"));

      // simple bicycle
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/2d_bike__downloadable_for_first_10_users/scene.gltf"));

      // Hochrad - light reflection
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/pennyfarthest_bicycle/scene.gltf"));

      // gears - light reflection
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/gears/scene.gltf"));

      // walking roboter
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/robot-_walk_animation/scene.gltf"));

      // skateboard
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/skateboard_animated_-_blockbench/scene.gltf"));

      // zeppelin
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/zeppelin_aircraft/scene.gltf"));

      // Alter Anhänger
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/medieval_cart/scene.gltf"));

      // z-fighting underneath
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/lawn_mower_low_poly/scene.gltf"));

      // Locomotive frame
//      asset = new GLTFLoader().load(Gdx.files.absolute("C:/DAT/glTF/sketchfab.com/locomotive/scene.gltf"));

      // with z-fighting
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

      if (scene == null) {
         return;
      }

      final long start = System.nanoTime();

      // GLUtils.checkGlError(">" + TAG);

      gl.depthMask(true);

      if (viewport.pos.zoomLevel < 10) {
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

      mapCamera.update(viewport);

      final int numModels = 0;
      int numRendered = 0;

      final Viewport mapViewport = mMap.viewport();
      mapViewport.getMapExtents(_mapBox, 10);

      final float scale = (float) (mapCamera.mMapPosition.scale / viewport.pos.scale);

      final float dx = (float) (mapCamera.mMapPosition.x - viewport.pos.x) * (Tile.SIZE << mapCamera.mMapPosition.zoomLevel);
      final float dy = (float) (mapCamera.mMapPosition.y - viewport.pos.y) * (Tile.SIZE << mapCamera.mMapPosition.zoomLevel);

      for (int i = 0; i < 8; i += 2) {

         _mapBox[i] *= scale;
         _mapBox[i] -= dx;
         _mapBox[i + 1] *= scale;
         _mapBox[i + 1] -= dy;
      }

      synchronized (this) {

//         for (final ModelInstance modelInstance : allModelInstances) {

         final ModelInstance modelInstance = scene.modelInstance;

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

            sceneManager.update(deltaTime);
            sceneManager.render();
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

   @Override
   public boolean setup() {

      sceneAsset = loadGLTFModel();

      mapCamera = new MapCameraMT(mMap);

      scene = new Scene(sceneAsset.scene);
      sceneManager = new SceneManager();
      sceneManager.addScene(scene);

      // setup camera (The BoomBox model is very small so you may need to adapt camera settings for your scene)
//      camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
//      camera.near = 0.01f;
//      camera.far = 200000;

      sceneManager.setCamera(mapCamera);

//      cameraController = new MyCameraInputController(camera);
//      Gdx.input.setInputProcessor(cameraController);
//
//      // adjust scene to the model bounding box
//      adjustSceneToBoundingBox();

      // setup light
      light = new DirectionalLightEx();
      light.direction.set(1, -3, 1).nor();
      light.color.set(Color.WHITE);
      sceneManager.environment.add(light);

      // setup quick IBL (image based lighting)
      final IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
      environmentCubemap = iblBuilder.buildEnvMap(1024);
      diffuseCubemap = iblBuilder.buildIrradianceMap(256);
      specularCubemap = iblBuilder.buildRadianceMap(10);
      iblBuilder.dispose();

      // This texture is provided by the library, no need to have it in your assets.
      brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

      sceneManager.setAmbientLight(1f);
      sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
      sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
      sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

      // setup skybox
      skybox = new SceneSkybox(environmentCubemap);
      sceneManager.setSkyBox(skybox);

      activateFirstAnimation();

      return true;
   }

   @Override
   public synchronized void update(final GLViewport viewport) {

      // if (loading && assets.update())
      // doneLoading();

      if (isReady() == false) {
//         mapCamera.setPosition(viewport.pos);
         setReady(true);
      }

      // if (changed) {
      // cam.update(position, matrices);
      // }
   }
}
