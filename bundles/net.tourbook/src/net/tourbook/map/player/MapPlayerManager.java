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
package net.tourbook.map.player;

/**
 * Manage map animation player
 */
public class MapPlayerManager {

   private static MapPlayerView _mapPlayerView;

   private static int           _foregroundFPS = 5;
   private static int           _numAllFrames;

   private static long          _animationStartTime;
   private static int           _currentFrameNumber;
   private static float         _currentRelativePosition;
   private static boolean       _isAnimateFromRelativePosition;
   private static long          _lastUpdateTime;

   private static boolean       _isPlayerEnabled;

   public static int getCurrentFrameNumber() {

      final long currentTimeMS = System.currentTimeMillis();

      final int numAllFrames = _numAllFrames;

      int currentFrameNumber;

      if (_isAnimateFromRelativePosition) {

         _isAnimateFromRelativePosition = false;

         currentFrameNumber = (int) (numAllFrames * _currentRelativePosition);

//         /**
//          * Very important !!!
//          * <p>
//          * It took me several headaches to debug and find the reason, positionIndex must be an even
//          * number otherwise x and y are exchanged !!!
//          */
//         positionIndex /= 2;
//         positionIndex *= 2;

      } else {

         // how many arrows are moved in one second
         final int framesPerSecond = _foregroundFPS;

         // update sequence in one second
         final float frameDurationSec = 1f / framesPerSecond;
         final long frameDurationMS = (long) (frameDurationSec * 1000);

         // ensure there not more frames per second are displayed
         final long nextUpdateTimeMS = _lastUpdateTime + frameDurationMS;
         final long timeDiffMS = nextUpdateTimeMS - currentTimeMS;
         if (timeDiffMS > 0) {

            return _currentFrameNumber;
         }

         // the last frame is not displayed -> -2
         final int maxFrames = Math.max(0, numAllFrames - 2);
         final float maxLoopTime = maxFrames / framesPerSecond;

         final float timeDiffSinceFirstRun = (float) ((currentTimeMS - _animationStartTime) / 1000.0);
         final float currentTimeIndex = timeDiffSinceFirstRun % maxLoopTime;

         currentFrameNumber = Math.round(currentTimeIndex * framesPerSecond);

         // ensure to not jump back to the start when the end is not yet reached
         if (currentFrameNumber <= numAllFrames - 1) {
            currentFrameNumber = _currentFrameNumber + 1;
         }

         // ensure to move not more than one frame
         if (currentFrameNumber > _currentFrameNumber + 1) {
            currentFrameNumber = _currentFrameNumber + 1;
         }
      }

      // ensure bounds
      if (currentFrameNumber >= numAllFrames - 1) {
         currentFrameNumber = 0;
      }

      _currentFrameNumber = currentFrameNumber;
      _currentRelativePosition = currentFrameNumber / (float) numAllFrames;
      _lastUpdateTime = currentTimeMS;

      if (_mapPlayerView != null) {
         _mapPlayerView.updateFrameNumber(_currentFrameNumber);
      }

      return currentFrameNumber;
   }

   public static int getForegroundFPS() {

      return _foregroundFPS;
   }

   public static int getNumberofAllFrames() {
      return _numAllFrames;
   }

   public static boolean isAnimateFromRelativePosition() {
      return _isAnimateFromRelativePosition;
   }

   public static boolean isPlayerEnabled() {
      return _isPlayerEnabled;
   }

   public static void setAnimationStartTime() {

      _animationStartTime = System.currentTimeMillis();
   }

   public static void setForegroundFPS(final int foregroundFPS) {

      _foregroundFPS = foregroundFPS;
   }

   public static void setMapPlayerViewer(final MapPlayerView mapPlayerView) {

      _mapPlayerView = mapPlayerView;
   }

   public static void setPlayerData(final MapPlayerData mapPlayerData) {

      _isPlayerEnabled = mapPlayerData.isPlayerEnabled;
      _numAllFrames = mapPlayerData.numAnimatedPositions;
      _isAnimateFromRelativePosition = mapPlayerData.isAnimateFromRelativePosition;

      if (_mapPlayerView != null) {
         _mapPlayerView.updatePlayer(_isPlayerEnabled, _numAllFrames, _foregroundFPS, _isAnimateFromRelativePosition);
      }
   }

}
