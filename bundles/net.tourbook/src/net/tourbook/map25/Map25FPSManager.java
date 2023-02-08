/*******************************************************************************
 * Copyright (C) 2022, 2023 Wolfgang Schramm and Contributors
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

import org.eclipse.swt.widgets.Display;

/**
 * Manage frame per seconds for the 2.5D map
 */
public class Map25FPSManager {

   public static final int                      DEFAULT_FOREGROUND_FPS = 30;
   private static final int                     DEFAULT_BACKGROUND_FPS = 2;

   private static LwjglApplication              _lwjglApp;
   private static LwjglApplicationConfiguration _appConfig;

   private static boolean                       _isBackgroundToAnimationFPS;

   private static int[]                         _eventCounter          = new int[1];

   public static void init(final LwjglApplication lwjglApp, final LwjglApplicationConfiguration appConfig) {

      _lwjglApp = lwjglApp;
      _appConfig = appConfig;

      _appConfig.foregroundFPS = DEFAULT_FOREGROUND_FPS;
      _appConfig.backgroundFPS = DEFAULT_BACKGROUND_FPS;
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

      _isBackgroundToAnimationFPS = isEnabled;

      if (isEnabled) {

         _appConfig.backgroundFPS = DEFAULT_FOREGROUND_FPS;

      } else {

         if (Map25App.isBackgroundFPS()) {

            // delay background FPS that switching between different maps do not stop and restart the animation

            _eventCounter[0]++;

            Display.getDefault().timerExec(2000, new Runnable() {

               final int __runnableCounter = _eventCounter[0];

               @Override
               public void run() {

                  // skip all events which has not yet been executed
                  if (__runnableCounter != _eventCounter[0]) {

                     // a newer event occurred

                     return;
                  }

                  if (_isBackgroundToAnimationFPS == false) {

                     _appConfig.backgroundFPS = Map25App.getBackgroundFPS();
                  }
               }
            });
         }
      }
   }

   /**
    * That an animation is working, it needs continuous rendering otherwise the model is only
    * rendered e.g. during the mouse movement
    *
    * @param isActive
    */
   public static void setContinuousRendering(final boolean isActive) {

      if (_lwjglApp == null) {

         // map is not yet setup

         return;
      }

      // disable continuous rendering when not needed
      _lwjglApp.getGraphics().setContinuousRendering(isActive);
   }
}
