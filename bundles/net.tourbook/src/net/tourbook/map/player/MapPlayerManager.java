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
   private static double                _lastRemainingDuration;

   private static double[]              _projectedPosition       = new double[2];
   private static long                  _projectedPosition_Time;

   private static double                _relativePosition_StartFrame;
   private static double                _relativePosition_EndFrame;
   private static double                _relativePosition_CurrentFrame;
   private static double                _movingDiff;

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

   private static double                _compileMapX;
   private static double                _compileMapY;

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

   public static double getCompileMapX() {

      return _compileMapX;
   }

   public static double getCompileMapY() {

      return _compileMapY;
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

      _isAnimateFromRelativePosition = true;

      if (_isAnimateFromRelativePosition) {

         // 1. Prio: Use relative position

         _currentNotClippedLocationIndex = (int) Math.round(numNotClipped_GeoLocationIndices * _relativePosition_CurrentFrame);

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

         _relativePosition_CurrentFrame = (double) _currentNotClippedLocationIndex / numNotClipped_GeoLocationIndices;

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
    * @return Returns the projected position of the animated model for the current frame
    */
   public static double[] getProjectedPosition() {

      final long currentFrameTime = MapRenderer.frametime;

      // check if position is already computed
      if (_projectedPosition_Time == currentFrameTime) {
         return _projectedPosition;
      }

      final MapPlayerData mapPlayerData = MapPlayerManager.getMapPlayerData();
      if (mapPlayerData == null) {
         return null;
      }

      final int[] allNotClipped_GeoLocationIndices = mapPlayerData.allNotClipped_GeoLocationIndices;
      final int numGeoLocations = allNotClipped_GeoLocationIndices.length;
      final int lastGeoLocationIndex = numGeoLocations - 1;

      if (lastGeoLocationIndex < 0) {
         return null;
      }

      double relativePosition = getRelativePosition();

//      if (relativePosition == _prevRelativePosition) {
//// This would need a reset option for a new tour
////         return;
//      }

      double[] allProjectedPoints;
      int numProjectedPoints;
      int geoLocationIndex_0 = 0;
      int geoLocationIndex_1 = 0;
      int positionIndex_0;
      int positionIndex_1;
      double exactLocationIndex = 0;

      // compute frame position from relative position

      if (relativePosition > 2) {

         // end...start + forward

         relativePosition = relativePosition - 2;
      }

      if (relativePosition > 1 || relativePosition < 0) {

         // move model on RETURN TRACK

         final double relativeReturnPosition;

         if (relativePosition > 1) {

            // end...start
            relativeReturnPosition = relativePosition - 1;

         } else {

            // relativePosition < 0

            // start...end
            relativeReturnPosition = relativePosition + 1;
         }

         allProjectedPoints = mapPlayerData.allProjectedPoints_ReturnTrack;

         numProjectedPoints = allProjectedPoints.length;
         final int numReturnPositions = numProjectedPoints / 2;
         final int lastReturnIndex = numReturnPositions - 1;

         exactLocationIndex = lastReturnIndex * relativeReturnPosition;

         positionIndex_0 = (int) exactLocationIndex;

         geoLocationIndex_0 = positionIndex_0;
         geoLocationIndex_1 = positionIndex_0 <= lastReturnIndex - 1
               ? positionIndex_0 + 1
               : positionIndex_0;

      } else {

         // relativePosition is >= 0 && <= 1 -> move model on NORMAL TRACK

//         if (relativePosition > 0.95) {
//            int a = 0;
//            a++;
//         }

         allProjectedPoints = mapPlayerData.allProjectedPoints_NormalTrack;
         numProjectedPoints = allProjectedPoints.length;

         // adjust last index by -1 that positionIndex_1 can point to the last index
         final int lastAdjusted_GeoLocationIndex = lastGeoLocationIndex > 0
               ? lastGeoLocationIndex - 1
               : lastGeoLocationIndex;

         exactLocationIndex = lastGeoLocationIndex * relativePosition;

         positionIndex_0 = (int) exactLocationIndex;
         positionIndex_1 = positionIndex_0 <= lastAdjusted_GeoLocationIndex

               // check bounds
               && positionIndex_0 <= lastGeoLocationIndex - 1

                     ? positionIndex_0 + 1
                     : positionIndex_0;

         geoLocationIndex_0 = allNotClipped_GeoLocationIndices[positionIndex_0];
         geoLocationIndex_1 = allNotClipped_GeoLocationIndices[positionIndex_1];
      }

      /*
       * Do micro movements according to the exact relative position
       */
      final int projectedIndex_0 = geoLocationIndex_0 * 2;
      final int projectedIndex_1 = geoLocationIndex_1 * 2;

      final double projectedPositionX_0 = allProjectedPoints[projectedIndex_0];
      final double projectedPositionY_0 = allProjectedPoints[projectedIndex_0 + 1];
      final double projectedPositionX_1 = allProjectedPoints[projectedIndex_1];
      final double projectedPositionY_1 = allProjectedPoints[projectedIndex_1 + 1];

      final double projectedPositionX_Diff = projectedPositionX_1 - projectedPositionX_0;
      final double projectedPositionY_Diff = projectedPositionY_1 - projectedPositionY_0;

      // 0...1
      final double microIndex = exactLocationIndex - (int) exactLocationIndex;

      final double advanceX = projectedPositionX_Diff * microIndex;
      final double advanceY = projectedPositionY_Diff * microIndex;

      final double projectedPositionX = projectedPositionX_0 + advanceX;
      final double projectedPositionY = projectedPositionY_0 + advanceY;

      _projectedPosition[0] = projectedPositionX;
      _projectedPosition[1] = projectedPositionY;

      _projectedPosition_Time = currentFrameTime;

      return _projectedPosition;
   }

   /**
    * Compute relative position for the play head, it is called from
    * {@link net.tourbook.map25.animation.GLTFModel_Renderer#render_UpdateModelPosition()}
    * <p>
    * The relative position is for this moving loop, start and end must not be at the same position:
    *
    * <pre>
    *
    *             >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    *           /\                                        \/
    *          /\                                          \/
    *          /\                                          \/
    *          /\          0       NORMAL >>     1         \/
    *           /\                                        \/
    *             <<<<<< START  << RETURN >>    END <<<<<<
    *
    *                      2    << RETURN        1
    *                      0       RETURN >>    -1
    * </pre>
    * <p>
    *
    * @return Returns the relative position {@link #_relativePosition_CurrentFrame} which depends on
    *         the remaining animation time, it is between <br>
    *         0 ... 1 start...end for the normal model movement<br>
    *         1 ... 2 return track end...start<br>
    *         0 ...-1 return track start...end
    */
   private static double getRelativePosition() {

      final long currentFrameTime = MapRenderer.frametime;

      synchronized (RELATIVE_POSITION) {

         final float remainingDuration = _animationEndTime - currentFrameTime;

         // check if animation has finished
         if (remainingDuration < 0) {

            // animation time has expired, return last position

            if (_relativePosition_EndFrame < 0) {

               // model was moving on the return track from start...end -> set to normal end

               _relativePosition_EndFrame = 1;

            } else if (_relativePosition_EndFrame > 1) {

               // model was moving on the return track from end...start -> set to normal start

               _relativePosition_EndFrame = 0;
            }

            /*
             * Fix rounding, otherwise the requested relative position is mostly not exactly set
             * which causes the model to be not at the requested position. This can be easily
             * checked with the start and end position (Home/End button).
             */
            if (_relativePosition_CurrentFrame != _relativePosition_EndFrame) {

               _relativePosition_CurrentFrame = _relativePosition_EndFrame;
               _relativePosition_CurrentFrame = clamp(_relativePosition_CurrentFrame, 0, 1);
            }

            if (_lastRemainingDuration > 0) {

               _lastRemainingDuration = 0;

               // redraw
               _isAnimateFromRelativePosition = true;
            }

            return _relativePosition_CurrentFrame;
         }

         // advance to the next animated frame

         final float relativeRemaining = remainingDuration / animationDuration; // 0...1
         final float relativeAdvance = clamp(1.0f - relativeRemaining, 0, 1);

         if (_relativePosition_EndFrame < 0) {

            // model is moving on the RETURN TRACK -> start...end -> 0...-1

            final double startEndDiff = _relativePosition_EndFrame - _relativePosition_StartFrame;
            final double startEndAdvance = startEndDiff * relativeAdvance;
            final double currentRelativePosition = _relativePosition_StartFrame + startEndAdvance;

            _relativePosition_CurrentFrame = currentRelativePosition;

         } else if (_relativePosition_EndFrame > 1) {

            // model is moving on the RETURN TRACK -> end...start -> 1...2

            final double startEndDiff = _relativePosition_EndFrame - _relativePosition_StartFrame;
            final double startEndAdvance = startEndDiff * relativeAdvance;
            final double currentRelativePosition = _relativePosition_StartFrame + startEndAdvance;

            _relativePosition_CurrentFrame = currentRelativePosition;

         } else {

            // _relativePosition_EndFrame: 0...1 -> model is moving on the NORMAL TRACK -> start...end

            if (_relativePosition_CurrentFrame < 0) {

               // model is still moving on the RETURN TRACK from start...end -> 0...-1

               final double remainingStartFrame = 1 + _relativePosition_StartFrame;
               final double remainingEndFrame = 1 - _relativePosition_EndFrame;

               final double startEndDiff = remainingStartFrame + remainingEndFrame;
               final double startEndAdvance = startEndDiff * relativeAdvance;
               double currentRelativePosition = _relativePosition_StartFrame - startEndAdvance;

               // check if model in on the NORMAL or RETURN TRACK
               if (currentRelativePosition < -1) {

                  // model is now back on the NORMAL TRACK -> 0...1

                  currentRelativePosition += 2;

                  _relativePosition_StartFrame = _relativePosition_StartFrame + 2;
                  _relativePosition_CurrentFrame = clamp(currentRelativePosition, 0, 1);

               } else {

                  // model is still on the RETURN TRACK -> 0...-1

                  _relativePosition_CurrentFrame = clamp(currentRelativePosition, -1, 0);
               }

            } else if (_relativePosition_CurrentFrame > 1) {

               // model is still moving on the RETURN TRACK from end...start -> 1...2

               final double startEndDiff = 2 - _relativePosition_StartFrame + _relativePosition_EndFrame;
               final double startEndAdvance = startEndDiff * relativeAdvance;
               double currentRelativePosition = _relativePosition_StartFrame + startEndAdvance;

               // check if model in on the NORMAL or RETURN TRACK
               if (currentRelativePosition > 2) {

                  // model is now back on the NORMAL TRACK -> 0...1

                  currentRelativePosition -= 2;

                  _relativePosition_StartFrame = _relativePosition_StartFrame - 2;
                  _relativePosition_CurrentFrame = clamp(currentRelativePosition, 0, 1);

               } else {

                  // model is still on the RETURN TRACK -> 1...2

                  _relativePosition_CurrentFrame = clamp(currentRelativePosition, 1, 2);
               }

            } else {

               // _relativePosition_CurrentFrame: 0...1 -> model is moving on the NORMAL TRACK -> start...end

               final double startEndDiff = _relativePosition_EndFrame - _relativePosition_StartFrame;
               final double startEndAdvance = startEndDiff * relativeAdvance;
               final double currentRelativePosition = _relativePosition_StartFrame + startEndAdvance;

               _relativePosition_CurrentFrame = clamp(currentRelativePosition, 0, 1);
            }
         }

         // redraw
         _isAnimateFromRelativePosition = true;

         _lastRemainingDuration = remainingDuration;

//         System.out.println(UI.timeStamp()
//               + " getPosition   "
//               + "  Current:" + String.format("%7.4f", _relativePosition_CurrentFrame)
//               + "  End:" + String.format("%7.4f", _relativePosition_EndFrame)
////               + "  remaining:" + remainingDuration
//         );
// TODO remove SYSTEM.OUT.PRINTLN

      }

      return _relativePosition_CurrentFrame;
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

   public static void setCompileMapScale(final double x, final double y, final double scale) {

      _compileMapX = x;
      _compileMapY = y;
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
    * Move player head to a relative position and start playing at this position, it is called
    * from {@link net.tourbook.map.player.MapPlayerView#setMapAndModelPosition(double)}
    * <p>
    * Relative position in this moving loop, start and end must not be at the same position:
    *
    * <pre>
    *
    *             >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    *           /\                                        \/
    *          /\                                          \/
    *          /\                                          \/
    *          /\          0       NORMAL >>     1         \/
    *           /\                                        \/
    *             <<<<<< START  << RETURN >>    END <<<<<<
    *
    *                      2    << RETURN        1
    *                      0       RETURN >>    -1
    * </pre>
    *
    * @param newRelativePosition
    *           which is between <br>
    *           0 ... 1 start...end for the normal model movement<br>
    *           1 ... 2 return track end...start<br>
    *           0 ...-1 return track start...end
    * @param movingDiff
    *           This value is positive when moving forward
    */
   public static void setRelativePosition(final double newRelativePosition, final double movingDiff) {

      // ignore the same position otherwise it would slow down the movement when not looping
      if (newRelativePosition == _relativePosition_EndFrame) {
         return;
      }

      synchronized (RELATIVE_POSITION) {

         animationDuration = 1000;

         final long currentFrameTime = MapRenderer.frametime;
         _animationEndTime = currentFrameTime + animationDuration;

         // set new start position from the current position
         _relativePosition_StartFrame = _relativePosition_CurrentFrame;
         _movingDiff = movingDiff;

         /**
          * !!! Complicated !!!
          * <p>
          * This adjustment is necessary that the model in not moving on the NORMAL TRACK in reverse
          * direction by skipping the RETURN TRACK
          */
         if (true

               // the current model movement should be on the RETURN TRACK
               && _relativePosition_EndFrame == 2

               // but the model has not yet left the NORMAL TRACK
               && _relativePosition_CurrentFrame <= 1

               // the model should be moving again on the NORMAL TRACK
               && newRelativePosition < 1) {

            // skip until the _relativePosition_CurrentFrame is >1

            return;

         } else if (true

               // the current model movement should be on the RETURN TRACK
               && _relativePosition_EndFrame == -1

               // but the model has not yet left the NORMAL TRACK
               && _relativePosition_CurrentFrame >= 0

               // the model should be moving again on the NORMAL TRACK
               && newRelativePosition < 1) {

            // skip until the _relativePosition_CurrentFrame is <0

            return;
         }

         _relativePosition_EndFrame = newRelativePosition;

         // this will also force to compute the frame even when player is paused
         _isAnimateFromRelativePosition = true;

//         System.out.println(UI.timeStamp()
//
//               + "  Start:" + String.format("%7.4f", _relativePosition_StartFrame)
//               + "  End:" + String.format("%7.4f", newRelativePosition)
//
//         );
// TODO remove SYSTEM.OUT.PRINTLN
      }
   }
}
