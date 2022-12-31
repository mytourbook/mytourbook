/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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

import static org.oscim.utils.FastMath.clamp;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.oscim.renderer.MapRenderer;

/**
 * Manage map animation player
 */
public class MapPlayerManager {

   /**
    * Max value for the scale control which cannot have negative values but the speed can be
    * negative.
    */
   static final int                     SPEED_JOG_WHEEL_MAX      = 50;
   static final int                     SPEED_JOG_WHEEL_MAX_HALF = SPEED_JOG_WHEEL_MAX / 2;

   private static final int             DEFAULT_MOVING_SPEED     = 10;

   private static final String          STATE_FOREGROUND_FPS     = "STATE_FOREGROUND_FPS";                                             //$NON-NLS-1$
   private static final String          STATE_IS_PLAYING_LOOP    = "STATE_IS_PLAYING_LOOP";                                            //$NON-NLS-1$
   private static final String          STATE_IS_RELIVE_PLAYING  = "STATE_IS_RELIVE_PLAYING";                                          //$NON-NLS-1$
   private static final String          STATE_DIRECTION_SPEED    = "STATE_DIRECTION_SPEED";                                            //$NON-NLS-1$
   //
   private static final IDialogSettings _state                   = TourbookPlugin.getState("net.tourbook.map.player.MapPlayerManager");//$NON-NLS-1$

   private static MapPlayerView         _mapPlayerView;

   private static int                   _foregroundFPS;

   /**
    * Frame number which is currently displayed, it's in the range from
    * 1...{@link #_numAllVisibleFrames}
    */
   private static int                   _currentVisibleFrameNumber;

   /**
    * Number of frames for an animation
    */
   private static int                   _numAllVisibleFrames;

   public static long                   animationDuration        = 1000;

   private static int                   _movingSpeed             = DEFAULT_MOVING_SPEED;

   private static long                  _animationEndTime;
   private static float                 _animationForwardAngle;
   private static double                _lastLeftDuration;

   private static double                _currentRelativePosition;
   private static double                _nextRelativePosition;
   private static double                _startRelativePosition;

   private static int                   _currentNotClippedLocationIndex;
   private static int                   _currentVisibleIndex;

   private static boolean               _isAnimateFromRelativePosition;
   private static boolean               _isAnimationVisible;
   private static boolean               _isPlayerEnabled;
   private static boolean               _isPlayerRunning         = true;
   private static boolean               _isPlayingLoop;
   private static boolean               _isReLivePlaying;

   private static MapPlayerData         _mapPlayerData;

   /**
    * Map scale with which the tour track was compiled
    */
   private static double                _compileMapScale;
   private static boolean               _isCompileMapScaleSet;

   /**
    * When <code>true</code> then an animated triangle shows the exact cursor position
    */
   private static boolean               _isShowAnimationCursor;

   private static Object                RELATIVE_POSITION        = new Object();

   /**
    * @return Returns the angle for the model forward direction
    */
   public static float getAnimationForwardAngle() {

      return _animationForwardAngle;
   }

   public static double getCompileMapScale() {

      return _compileMapScale;
   }

   /**
    * @return Returns the last computed frame numer, it's in the range from
    *         1...{@link #_numAllVisibleFrames}
    */
   public static int getCurrentVisibleFrameNumber() {

      return _currentVisibleFrameNumber < 1

            // frames are starting with 1
            ? 1

            : _currentVisibleFrameNumber;
   }

   public static int getForegroundFPS() {

      return _foregroundFPS;
   }

   /**
    * @return Returns the moving speed value for the jog wheel control (scale)
    */
   public static int getJogWheelSpeed() {

      return _movingSpeed

            // adjust to the center of the scale control
            + SPEED_JOG_WHEEL_MAX_HALF;
   }

   public static MapPlayerData getMapPlayerData() {

      return _mapPlayerData;
   }

   public static int getMovingSpeed() {
      return _movingSpeed;
   }

   /**
    * Compute the next visible frame number, called from
    * {@link net.tourbook.map25.renderer.TourTrack_Shader#paint}
    *
    * @return Returns an index <code>0...</code>{@link #_numAllVisibleFrames}<code> - 1</code> for
    *         the next frame <code>1...</code>{@link #_numAllVisibleFrames}
    */
   public static int getNextVisibleFrameIndex() {

      if (

      // player is paused
      _isPlayerRunning == false

            // exception: compute current frame when a relative position is set,
            //            this is used when timeline is dragged/selected
            && _isAnimateFromRelativePosition == false) {

         return _currentVisibleIndex;
      }

      if (_mapPlayerData == null || _mapPlayerData.allNotClipped_GeoLocationIndices == null) {
         return 0;
      }

      final int[] allNotClipped_GeoLocationIndices = _mapPlayerData.allNotClipped_GeoLocationIndices;
      final int numNotClipped_GeoLocationIndices = allNotClipped_GeoLocationIndices.length;

      if (numNotClipped_GeoLocationIndices == 0) {
         return 0;
      }

      int nextFrameNumber = 0;
      boolean isComputeNextVisibleIndex = false;

      if (_isAnimateFromRelativePosition) {

         // 1. Prio: Use relative position

         _isAnimateFromRelativePosition = false;

         _currentNotClippedLocationIndex = (int) Math.round(numNotClipped_GeoLocationIndices * _currentRelativePosition);

         isComputeNextVisibleIndex = true;

      } else if (_isPlayingLoop && _currentVisibleFrameNumber >= _numAllVisibleFrames) {

         // 2. Prio: Loop animation

         // start loop with first frame

         nextFrameNumber = 1;

         _currentNotClippedLocationIndex = 0;

      } else {

         // 3. Prio: Compute next frame

         if (_currentNotClippedLocationIndex < numNotClipped_GeoLocationIndices - 2) {
            _currentNotClippedLocationIndex++;
         }

         _currentRelativePosition = (double) _currentNotClippedLocationIndex / numNotClipped_GeoLocationIndices;

         isComputeNextVisibleIndex = true;
      }

      if (isComputeNextVisibleIndex) {

         /*
          * Compute visible index from not clipped index
          */

         // ensure bounds
         _currentNotClippedLocationIndex = clamp(_currentNotClippedLocationIndex, 0, numNotClipped_GeoLocationIndices - 1);

         final int[] allVisibleGeoLocationIndices = _mapPlayerData.allVisible_GeoLocationIndices;
         final int notClippedIndex = allNotClipped_GeoLocationIndices[_currentNotClippedLocationIndex];

         nextFrameNumber = MtMath.searchNearestIndex(allVisibleGeoLocationIndices, notClippedIndex);
      }

      // ensure bounds
      if (nextFrameNumber > _numAllVisibleFrames) {
         nextFrameNumber = _numAllVisibleFrames;
      }

      _currentVisibleIndex = getValidIndex(nextFrameNumber);

      return _currentVisibleIndex;
   }

   /**
    * Compute the next frame number which depends on the time or other parameters
    *
    * @return Returns an index <code>0...</code>{@link #_numAllVisibleFrames}<code> - 1</code> for
    *         the next frame <code>1...</code>{@link #_numAllVisibleFrames}
    */

   public static int getNumberOfVisibleFrames() {

      return _numAllVisibleFrames;
   }

   /**
    * This is called from
    * {@link net.tourbook.map25.animation.GLTFModel_Renderer#render_UpdateModelPosition()}
    *
    * @return
    */
   public static double getRelativePosition() {

      synchronized (RELATIVE_POSITION) {

         final long currentFrameTime = MapRenderer.frametime;
         final float leftDuration = _animationEndTime - currentFrameTime;

         if (leftDuration < 0) {

            // animation has finished

            /*
             * Fix rounding, otherwise the requested relative position is mostly not exactly set
             * which causes the model to be not at the requested position. This can be easily
             * checked with the start and end position (Home/End button).
             */
            _currentRelativePosition = _nextRelativePosition;

            if (_lastLeftDuration > 0) {

               _lastLeftDuration = 0;

               // redraw
               _isAnimateFromRelativePosition = true;
            }

            return _currentRelativePosition;
         }

         // advance to the next animated position

         final float relativeRemainingDuration = leftDuration / animationDuration; // 0...1
         final float advance = clamp(1.0f - relativeRemainingDuration, 0, 1);

         final double positionDelta = _nextRelativePosition - _startRelativePosition;
         final double positionDiff = positionDelta * advance;
         final double currentRelativePosition = _startRelativePosition + positionDiff;

         _currentRelativePosition = clamp(currentRelativePosition, 0, 1);

//         System.out.println(UI.timeStamp() + " getRelativePosition: " + _currentRelativePosition);
//         // TODO remove SYSTEM.OUT.PRINTLN

         // redraw
         _isAnimateFromRelativePosition = true;

         _lastLeftDuration = leftDuration;
      }

      return _currentRelativePosition;
   }

   /**
    * Convert frame number 1...n -> array index 0...n-1
    *
    * @param frameNumber
    * @return
    */
   private static int getValidIndex(final int frameNumber) {

      final int arrayIndex = frameNumber <= 0 ? 0 : frameNumber - 1;

      return arrayIndex;
   }

   public static boolean isAnimationVisible() {
      return _isAnimationVisible;
   }

   /**
    * @return Returns <code>true</code> when the {@link #_compileMapScale} was just set. This flag
    *         is reset after calling this method.
    */
   public static boolean isCompileMapScaleModified() {

      final boolean isCompileMapScaleSet = _isCompileMapScaleSet;

      _isCompileMapScaleSet = false;

      return isCompileMapScaleSet;
   }

   /**
    * @return Returns <code>true</code> when the last frame in the animation is reached
    */
   public static boolean isLastFrame() {

      return _currentVisibleFrameNumber == _numAllVisibleFrames;
   }

   private static boolean isPlayerAvailable() {
      return _mapPlayerView != null;
   }

   public static boolean isPlayerEnabled() {
      return _isPlayerEnabled;
   }

   public static boolean isPlayerRunning() {
      return _isPlayerRunning;
   }

   public static boolean isPlayingLoop() {
      return _isPlayingLoop;
   }

   public static boolean isReLivePlaying() {
      return _isReLivePlaying;
   }

   public static boolean isShowAnimationCursor() {

      _isShowAnimationCursor = true;

      return _isShowAnimationCursor;
   }

   public static void restoreState() {

// SET_FORMATTING_OFF

      _foregroundFPS    = Util.getStateInt(_state, STATE_FOREGROUND_FPS, 10);
      _isPlayingLoop    = Util.getStateBoolean(_state, STATE_IS_PLAYING_LOOP, false);
      _isReLivePlaying  = Util.getStateBoolean(_state, STATE_IS_RELIVE_PLAYING, false);

      _movingSpeed      = Util.getStateInt(_state, STATE_DIRECTION_SPEED, DEFAULT_MOVING_SPEED);

// SET_FORMATTING_ON
   }

   public static void saveState() {

// SET_FORMATTING_OFF

      _state.put(STATE_FOREGROUND_FPS,    _foregroundFPS);
      _state.put(STATE_IS_PLAYING_LOOP,   _isPlayingLoop);
      _state.put(STATE_IS_RELIVE_PLAYING, _isReLivePlaying);
      _state.put(STATE_DIRECTION_SPEED,   _movingSpeed);

// SET_FORMATTING_ON
   }

   public static void setAnimationForwardAngle(final float animationForwardAngle) {

      _animationForwardAngle = animationForwardAngle;
   }

   public static void setCompileMapScale(final double scale) {

      _compileMapScale = scale;

      _isCompileMapScaleSet = true;
   }

   public static void setForegroundFPS(final int foregroundFPS) {

      _foregroundFPS = foregroundFPS;
   }

   public static void setIsAnimationVisible(final boolean isAnimationVisible) {

      _isAnimationVisible = isAnimationVisible;

      if (isPlayerAvailable()) {
         _mapPlayerView.updateAnimationVisibility();
      }
   }

   public static void setIsPlayerRunning(final boolean isPlayerRunning) {

      _isPlayerRunning = isPlayerRunning;
   }

   public static void setIsPlayingLoop(final boolean isPlayingLoop) {

      _isPlayingLoop = isPlayingLoop;
   }

   public static void setIsReLivePlaying(final boolean isReLivePlaying) {

      _isReLivePlaying = isReLivePlaying;
   }

   public static void setIsShowAnimationCursor(final boolean isShowAnimationCursor) {

      _isShowAnimationCursor = isShowAnimationCursor;
   }

   public static void setMapPlayerViewer(final MapPlayerView mapPlayerView) {

      _mapPlayerView = mapPlayerView;
   }

   public static void setMovingSpeedFromJogWheel(final int jogWheelSpeed) {

      _movingSpeed = jogWheelSpeed

            // adjust to the center of the scale control
            - SPEED_JOG_WHEEL_MAX_HALF;
   }

   /**
    * Setup map player with all necessary data to run the animation.
    * <p>
    * This method is called when new data are set into the shader buffer data, for a new zoom level
    * or when map is moved more than a tile
    *
    * @param mapPlayerData
    */
   public static void setPlayerData(final MapPlayerData mapPlayerData) {

      _mapPlayerData = mapPlayerData;

      _isPlayerEnabled = mapPlayerData.isPlayerEnabled;

      _numAllVisibleFrames = mapPlayerData.allVisible_PixelPositions == null
            ? 0
            : mapPlayerData.allVisible_PixelPositions.length / 2;

      if (isPlayerAvailable()) {
         _mapPlayerView.updatePlayer();
      }
   }

   /**
    * Move player head to a relative position and start playing at this position
    *
    * @param newRelativePosition
    *           Is between 0...1
    */
   public static void setRelativePosition(final double newRelativePosition) {

      synchronized (RELATIVE_POSITION) {

         animationDuration = 1000;

         final long currentFrameTime = MapRenderer.frametime;
         _animationEndTime = currentFrameTime + animationDuration;

         // keep current position
         _startRelativePosition = _currentRelativePosition;

         // the next painted frame will recognize this position
         _nextRelativePosition = newRelativePosition;

         // this will also force to compute the frame even when player is paused
         _isAnimateFromRelativePosition = true;
      }
   }
}
