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

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Limits;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 * This test renders a scene to a texture, then displays the texture on a cube.
 */
public class Map25AnimationApp extends SimpleApplication implements ActionListener {

   private static final String TOGGLE_UPDATE = "Toggle Update";

   private float               _angle        = 0;

   private Geometry            _geoOffscreenBox;
   private ViewPort            _offscreenViewPort;

   private Texture2D           _offscreenTexture;

   public static void main(final String[] args) {

      final Map25AnimationApp app = new Map25AnimationApp();

      // show hide/settings dialog
      app.setShowSettings(false);

      app.start();

//    import com.jme3.system.JmeContext.Type;
//      app.start(com.jme3.system.JmeContext.Type.OffscreenSurface);
   }

   @Override
   public void onAction(final String name, final boolean isPressed, final float tpf) {

      if (name.equals(TOGGLE_UPDATE) && isPressed) {
         _offscreenViewPort.setEnabled(!_offscreenViewPort.isEnabled());
      }
   }

   private Texture2D setupOffscreenTexture() {

      // setup framebuffer's cam
      final Camera offscreenCamera = new Camera(512, 512);
      offscreenCamera.setFrustumPerspective(45f, 1f, 1f, 1000f);
      offscreenCamera.setLocation(new Vector3f(0f, 0f, -5f));
      offscreenCamera.lookAt(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y);

      // setup framebuffer's texture
      final Texture2D offscreenTex = new Texture2D(512, 512, Format.RGBA8);
      offscreenTex.setMinFilter(Texture.MinFilter.Trilinear);
      offscreenTex.setMagFilter(Texture.MagFilter.Bilinear);

      // create offscreen framebuffer and setup framebuffer to use texture
      final FrameBuffer offscreenBuffer = new FrameBuffer(512, 512, 1);
      offscreenBuffer.setDepthTarget(FrameBufferTarget.newTarget(Format.Depth));
      offscreenBuffer.addColorTarget(FrameBufferTarget.newTarget(offscreenTex));

      _offscreenViewPort = renderManager.createPreView("Offscreen View", offscreenCamera);
      _offscreenViewPort.setClearFlags(true, true, true);
      _offscreenViewPort.setBackgroundColor(ColorRGBA.DarkGray);

      // set viewport to render to offscreen framebuffer
      _offscreenViewPort.setOutputFrameBuffer(offscreenBuffer);

      // setup framebuffer's scene
      final Box boxMesh = new Box(1, 1, 1);

//      final Material material1 = assetManager.loadMaterial("Interface/Logo/Logo.j3m");
      final Material material2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

      final Material material = material2;
      material.setColor("Color", ColorRGBA.Orange);

      _geoOffscreenBox = new Geometry("box", boxMesh);
      _geoOffscreenBox.setMaterial(material);

      // attach the scene to the viewport to be rendered
      _offscreenViewPort.attachScene(_geoOffscreenBox);

      return offscreenTex;
   }

   @Override
   public void simpleInitApp() {

      // this is not working
      renderer.setDefaultAnisotropicFilter(Math.min(renderer.getLimits().get(Limits.TextureAnisotropy), 8));

      flyCam.setDragToRotate(true);
      flyCam.setMoveSpeed(10);
      flyCam.setRotationSpeed(10);
      flyCam.setZoomSpeed(10);

      cam.setLocation(new Vector3f(3, 3, 3));
      cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

      // setup main scene
      final Geometry quad = new Geometry("box", new Box(0.5f, 0.5f, 1));

      _offscreenTexture = setupOffscreenTexture();

      final Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
      mat.setTexture("ColorMap", _offscreenTexture);
      quad.setMaterial(mat);
      rootNode.attachChild(quad);

      inputManager.addMapping(TOGGLE_UPDATE, new KeyTrigger(KeyInput.KEY_SPACE));
      inputManager.addListener(this, TOGGLE_UPDATE);
   }

   @Override
   public void simpleUpdate(final float timePerFrame) {

      if (_offscreenViewPort.isEnabled()) {

         _angle += timePerFrame / 3;
         _angle %= FastMath.TWO_PI;

         final Quaternion quaternion = new Quaternion();
         quaternion.fromAngles(_angle, 0, _angle);

         _geoOffscreenBox.setLocalRotation(quaternion);
         _geoOffscreenBox.updateLogicalState(timePerFrame);
         _geoOffscreenBox.updateGeometricState();
      }
   }

}
