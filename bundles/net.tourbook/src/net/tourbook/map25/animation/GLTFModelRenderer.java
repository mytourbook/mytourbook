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

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

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

   static final Logger         log               = LoggerFactory.getLogger(GLTFModelRenderer.class);

   private Map                 mMap;
   public MapCameraMT          mapCamera;

   private ModelBatch          modelBatch;
   private Environment         lights;

   public Array<ModelInstance> allModelInstances = new Array<>();

   private Vector3             _tempVector       = new Vector3();

   float[]                     _mapBox           = new float[8];

   private boolean             loading;

   public GLTFModelRenderer(final Map map) {

      mMap = map;
   }

   public void dispose() {

//      modelBatch.dispose();
//      assets.dispose();
//      assets = null;
//      axesModel.dispose();
//      axesModel = null;
   }

   @Override
   public void render(final GLViewport viewport) {

      if (allModelInstances.size == 0) {
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

      int numModels = 0;
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

         modelBatch.begin(mapCamera);
         {
            numModels = allModelInstances.size;

            for (final ModelInstance modelInstance : allModelInstances) {

               modelInstance.transform.getTranslation(_tempVector);

               _tempVector.scl(0.9f, 0.9f, 1);

               final boolean isInMapBox = GeometryUtils.pointInPoly(_tempVector.x, _tempVector.y, _mapBox, 8, 0);

               if (isInMapBox == false) {
                  continue;
               }

               modelBatch.render(modelInstance, lights);

               numRendered++;
            }
         }
         modelBatch.end();
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

      modelBatch = new ModelBatch(new DefaultShaderProvider());

      lights = new Environment();

      lights.add(new DirectionalLight().set(0.7f, 0.7f, 0.7f, 0, 1, -0.2f));
      lights.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));

      mapCamera = new MapCameraMT(mMap);

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
