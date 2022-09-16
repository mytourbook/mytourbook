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

   private static int                           _animationFPS;

   public static void init(final LwjglApplication lwjglApp, final LwjglApplicationConfiguration appConfig) {

      _lwjglApp = lwjglApp;
      _appConfig = appConfig;

      _appConfig.foregroundFPS = DEFAULT_FOREGROUND_FPS;
      _appConfig.backgroundFPS = DEFAULT_BACKGROUND_FPS;
   }

   /**
    * That an animation is working, it needs contiuous rendering.
    *
    * @param isActive
    */
   public static void setAnimationActive(final boolean isActive, final int animationFPS) {

      _animationFPS = animationFPS;

      if (_lwjglApp == null) {

         // map is not yet setup

         return;
      }

      _lwjglApp.getGraphics().setContinuousRendering(isActive);

      _appConfig.foregroundFPS = isActive

            ? _animationFPS

            : DEFAULT_BACKGROUND_FPS;
   }

   /**
    * Set animation FPS
    *
    * @param animationFPS
    * @param isSetBackgroundFPS
    */
   public static void setAnimationFPS(final int animationFPS, final boolean isSetBackgroundFPS) {

      _animationFPS = animationFPS;

      if (isSetBackgroundFPS) {
         _appConfig.backgroundFPS = _animationFPS;
      }
   }

   /**
    * Set background FPS to a higher rate. This is helpful when a slideout is opened, then it get's
    * the focus and the map is running with the background FPS.
    *
    * @param isEnabled
    */
   public static void setBackgroundFPSToAnimationFPS(final boolean isEnabled) {

      _appConfig.backgroundFPS = isEnabled

            ? _animationFPS

            : DEFAULT_BACKGROUND_FPS;
   }

}
