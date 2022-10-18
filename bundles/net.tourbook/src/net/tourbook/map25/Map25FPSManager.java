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
package net.tourbook.map25;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

/**
 * Manage frame per seconds for the 2.5D map
 */
public class Map25FPSManager {

   public static int                            DEFAULT_FOREGROUND_FPS = 30;
   private static int                           DEFAULT_BACKGROUND_FPS = 1;

   private static LwjglApplication              _lwjglApp;
   private static LwjglApplicationConfiguration _appConfig;

   public static long getBackgroundFPS() {
      return _appConfig.backgroundFPS;
   }

   public static long getForegroundFPS() {
      return _appConfig.foregroundFPS;
   }

   public static void init(final LwjglApplication lwjglApp, final LwjglApplicationConfiguration appConfig) {

      _lwjglApp = lwjglApp;
      _appConfig = appConfig;

      _appConfig.foregroundFPS = DEFAULT_FOREGROUND_FPS;
      _appConfig.backgroundFPS = DEFAULT_BACKGROUND_FPS;
   }

   /**
    * Set animation parameters.
    * <p>
    * That an animation is working, it needs contiuous rendering.
    *
    * @param isActive
    * @param animationFPS
    */
   public static void setAnimation(final boolean isActive) {

      if (_lwjglApp == null) {

         // map is not yet setup

         return;
      }

      // disable rendering when not needed
      _lwjglApp.getGraphics().setContinuousRendering(isActive);
   }

   /**
    * Set background FPS to a higher rate. This is helpful when a slideout is opened, then it get's
    * the focus and the map is normally running with the background FPS.
    *
    * @param isEnabled
    */
   public static void setBackgroundFPSToAnimationFPS(final boolean isEnabled) {

      if (_appConfig == null) {

         // 2.5D map is not yet opened

         return;
      }

      _appConfig.backgroundFPS = isEnabled

            ? DEFAULT_FOREGROUND_FPS

            : DEFAULT_BACKGROUND_FPS;
   }

}
