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

import static org.oscim.renderer.MapRenderer.COORD_SCALE;

import java.lang.reflect.Field;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.MapPosition;
import org.oscim.map.Viewport;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLMatrix;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.BitmapBucket;

/**
 * Copied from org.oscim.renderer.BucketRenderer and adjusted
 */
public class CompassRoseRenderer extends BucketRenderer {

   private Bitmap      _bitmap;
   private int         _bitmapWidth;
   private int         _bitmapHeight;
   private boolean     _isUpdateBitmap;

   private GLMatrix    _tempMatrix  = new GLMatrix();

   private MapPosition _mapPosition = new MapPosition();

   public CompassRoseRenderer() {

      super();

      /*
       * Prevent exception when mBitmap == null
       */
      mInitialized = true;
   }

   @Override
   protected synchronized void compile() {

      if (_bitmap == null) {
         return;
      }

      synchronized (_bitmap) {
         super.compile();
      }
   }

   private float getClassField(final Viewport viewport, final String fieldName) {

      try {

         final Field classField = viewport.getClass().getSuperclass().getDeclaredField(fieldName);

         classField.setAccessible(true);

         return classField.getFloat(viewport);

      } catch (NoSuchFieldException
            | SecurityException
            | IllegalArgumentException
            | IllegalAccessException e) {

         e.printStackTrace();
      }

      return 0;
   }

   @Override
   public synchronized void render(final GLViewport viewport) {

      /**
       * Original comment in commit 3a8db9cc7cb024f7cc399ec534ac5a543115b2cb
       * <p>
       * "ScaleBar disappears sometimes fix by Erik Duisters, fixes #155"
       */
      GLState.test(false, false);

      ///////////////////////////////////////////////////////////////////////////////////

      viewport.getMapPosition(_mapPosition);

      // hack: get value from a protected field
      final int viewportWidth = (int) getClassField(viewport, "mWidth"); //$NON-NLS-1$
      final int viewportHeight = (int) getClassField(viewport, "mHeight"); //$NON-NLS-1$

      final float offsetX = -10;
      final float offsetY = 10;

      // top-right position
      final float screenX = viewportWidth * 0.5f - _bitmapWidth + offsetX;
      final float screenY = -viewportHeight * 0.5f + offsetY;

      final float invScale = 1f / COORD_SCALE;

      final float mapBearing = _mapPosition.bearing;
      final float mapTilt = _mapPosition.tilt;

      // flip when looking map from the underground
      final float imageBearing = mapTilt <= 90
            ? mapBearing
            : (180 - mapBearing);

      // tilt the compass rose less than the map
      final float tiltScale = 0.9f;
      final float imageTilt = mapTilt <= 90
            ? mapTilt * tiltScale
            : (180 - mapTilt) * tiltScale;

      final GLMatrix mvpMatrix = viewport.mvp;

      // set matrix: set it's scale
      mvpMatrix.setScale(invScale, invScale, 1);

      // multiply: center image in the origin 0,0
      _tempMatrix.setTranslation(-_bitmapWidth * 0.5f, -_bitmapHeight * 0.5f, 1);
      mvpMatrix.multiplyLhs(_tempMatrix);

      // multiply: rotate: bearing
      _tempMatrix.setRotation(imageBearing, 0f, 0f, 1f);
      mvpMatrix.multiplyLhs(_tempMatrix);

      // multiply: rotate: tilt
      _tempMatrix.setRotation(-imageTilt, 1f, 0f, 0f);
      mvpMatrix.multiplyLhs(_tempMatrix);

      // multiply: move image to the top-right corner
      _tempMatrix.setTranslation(screenX + _bitmapWidth * 0.5f, screenY + _bitmapHeight * 0.5f, 1);
      mvpMatrix.multiplyLhs(_tempMatrix);

      // multiply: apply projection
      mvpMatrix.multiplyLhs(viewport.proj);

      BitmapBucket.Renderer.draw(buckets.get(), viewport, 1, .7f);
   }

   /**
    * @param bitmap
    *           with dimension being power of two
    * @param width
    *           width used
    * @param height
    *           height used
    */
   public synchronized void setBitmap(final Bitmap bitmap, final int width, final int height) {

      _bitmap = bitmap;
      _bitmapWidth = width;
      _bitmapHeight = height;

      mInitialized = false;
   }

   @Override
   public synchronized void update(final GLViewport v) {

      if (!mInitialized) {

         buckets.clear();

         final BitmapBucket l = new BitmapBucket(true);
         l.setBitmap(_bitmap, _bitmapWidth, _bitmapHeight);
         buckets.set(l);

         _isUpdateBitmap = true;
      }

      if (_isUpdateBitmap) {

         _isUpdateBitmap = false;

         compile();
      }
   }

   public synchronized void updateBitmap() {
      _isUpdateBitmap = true;
   }
}
