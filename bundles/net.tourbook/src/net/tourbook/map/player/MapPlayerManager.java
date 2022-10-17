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

   private static int           _foregroundFPS   = 20;

   /**
    * Frame number which is currently displayed, it's in the range from 1...{@link #_numAllFrames}
    */
   private static int           _currentFrameNumber;

   /**
    * Number of frames for an animation
    */
   private static int           _numAllFrames;

   private static long          _animationStartTime;
   private static float         _currentRelativePosition;
   private static boolean       _isAnimateFromRelativePosition;
   private static long          _lastUpdateTime;

   private static boolean       _isPlayerEnabled;
   private static boolean       _isPlayerRunning = true;

   /**
    * @return Returns an index <code>0...</code>{@link #_numAllFrames}<code> - 1</code> for the
    *         current frame <code>1...</code>{@link #_numAllFrames}
    */
   public static int getCurrentFrameIndex() {

      if (_isPlayerRunning == false

            // exception: compute current frame when relative positions are set,
            //            this is used when timeline is dragged/selected
            && _isAnimateFromRelativePosition == false) {

         // player is paused

         return getValidIndex(_currentFrameNumber);
      }

      final long currentTimeMS = System.currentTimeMillis();

      int nextFrameNumber = 0;

      if (_isAnimateFromRelativePosition) {

         _isAnimateFromRelativePosition = false;

         nextFrameNumber = (int) (_numAllFrames * _currentRelativePosition);

      } else {

         // get frame duration
         final float frameDurationSec = 1f / _foregroundFPS;
         final long frameDurationMS = (long) (frameDurationSec * 1000);

         // get next frame time
         final long nextFrameTimeMS = _lastUpdateTime + frameDurationMS;
         final long timeDiffMS = nextFrameTimeMS - currentTimeMS;

         // ensure that not more frames per second are displayed
         if (timeDiffMS > 0) {

            return getValidIndex(_currentFrameNumber);
         }

         final int maxFrames = Math.max(0, _numAllFrames);
         final float maxLoopTime = maxFrames / _foregroundFPS;

         final float timeDiffSinceFirstRun = (float) ((currentTimeMS - _animationStartTime) / 1000.0);
         final float currentTimeIndex = timeDiffSinceFirstRun % maxLoopTime;

         final int nextFrameByTime = Math.round(currentTimeIndex * _foregroundFPS);
         nextFrameNumber = nextFrameByTime;

         // ensure to not jump back to the start when the end is not yet reached
         if (nextFrameNumber < _numAllFrames) {
            nextFrameNumber = _currentFrameNumber + 1;
         }

         // ensure to move not more than one frame
         if (nextFrameNumber > _currentFrameNumber + 1) {
            nextFrameNumber = _currentFrameNumber + 1;
         }
      }

      // ensure bounds
      if (nextFrameNumber > _numAllFrames) {
         nextFrameNumber = _numAllFrames;
      }

      _currentFrameNumber = nextFrameNumber;
      _currentRelativePosition = nextFrameNumber / (float) _numAllFrames;
      _lastUpdateTime = currentTimeMS;

      if (_mapPlayerView != null) {
         _mapPlayerView.updateFrameNumber(_currentFrameNumber);
      }

      return getValidIndex(nextFrameNumber);
   }

   public static int getForegroundFPS() {

      return _foregroundFPS;
   }

   public static int getNumberofAllFrames() {
      return _numAllFrames;
   }

   /**
    * Convert frame number 1...n -> array index 0...n-1
    *
    * @param frameNumber
    * @return
    */
   private static int getValidIndex(final int frameNumber) {

      final int arrayIndex = frameNumber == 0 ? 0 : frameNumber - 1;

      return arrayIndex;
   }

   public static boolean isAnimateFromRelativePosition() {
      return _isAnimateFromRelativePosition;
   }

   public static boolean isPlayerEnabled() {
      return _isPlayerEnabled;
   }

   public static boolean isPlayerRunning() {
      return _isPlayerRunning;
   }

   public static void setAnimationStartTime() {

      _animationStartTime = System.currentTimeMillis();
   }

   public static void setForegroundFPS(final int foregroundFPS) {

      _foregroundFPS = foregroundFPS;
   }

   public static void setIsPlayerRunning(final boolean isPlayerRunning) {
      _isPlayerRunning = isPlayerRunning;
   }

   public static void setMapPlayerViewer(final MapPlayerView mapPlayerView) {

      _mapPlayerView = mapPlayerView;
   }

   /**
    * Move player head to a relative position
    *
    * @param relativePosition
    */
   public static void setRelativePosition(final float relativePosition) {

      // the next frame will recognize this position
      _currentRelativePosition = relativePosition;

      // this will also force to compute the frame even when player is paused
      _isAnimateFromRelativePosition = true;
   }

   /**
    * Setup map player with all necessary data to run the animation
    *
    * @param mapPlayerData
    */
   public static void setupPlayer(final MapPlayerData mapPlayerData) {

      _isPlayerEnabled = mapPlayerData.isPlayerEnabled;
      _numAllFrames = mapPlayerData.numAnimatedPositions;
      _isAnimateFromRelativePosition = mapPlayerData.isAnimateFromRelativePosition;

      if (_mapPlayerView != null) {
         _mapPlayerView.updatePlayer(_isPlayerEnabled, _numAllFrames, _foregroundFPS, _isAnimateFromRelativePosition);
      }
   }

}
