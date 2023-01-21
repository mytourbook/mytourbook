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

import com.badlogic.gdx.math.MathUtils;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.Util;
import net.tourbook.map.IMapSyncListener.SyncParameter;
import net.tourbook.map.MapManager;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Display;
import org.oscim.core.MapPosition;
import org.oscim.renderer.MapRenderer;

/**
 * Manage map animation player
 */
public class MapPlayerManager {

   /**
    * Max value for the scale control which cannot have negative values but the speed can be
    * negative.
    */
   static final int                     SPEED_JOG_WHEEL_MAX              = 200;
   static final int                     SPEED_JOG_WHEEL_MAX_HALF         = SPEED_JOG_WHEEL_MAX / 2;

   private static final int             DEFAULT_MOVING_SPEED             = 10;

   private static final String          STATE_IS_PLAYING_LOOP            = "STATE_IS_PLAYING_LOOP";                                            //$NON-NLS-1$
   private static final String          STATE_IS_RELIVE_PLAYING          = "STATE_IS_RELIVE_PLAYING";                                          //$NON-NLS-1$
   private static final String          STATE_JOG_WHEEL_SPEED            = "STATE_JOG_WHEEL_SPEED";                                            //$NON-NLS-1$
   private static final String          STATE_JOG_WHEEL_SPEED_MULTIPLIER = "STATE_JOG_WHEEL_SPEED_MULTIPLIER";                                 //$NON-NLS-1$
   private static final String          STATE_MODEL_TURNING_ANGLE        = "STATE_MODEL_TURNING_ANGLE";                                        //$NON-NLS-1$
   //
   private static final IDialogSettings _state                           = TourbookPlugin.getState("net.tourbook.map.player.MapPlayerManager");//$NON-NLS-1$

   private static MapPlayerView         _mapPlayerView;

   /**
    * Frame number which is currently displayed, it's in the range from
    * 1...{@link #_numAllVisibleFrames}
    */
   private static int                   _currentVisibleFrameNumber;

   /**
    * Number of frames for an animation
    */
   private static int                   _numAllVisibleFrames;

   /**
    * Is between - {@value #SPEED_JOG_WHEEL_MAX_HALF} ... + {@value #SPEED_JOG_WHEEL_MAX_HALF}
    */
   private static int                   _jogWheelSpeed                   = DEFAULT_MOVING_SPEED;

   private static long                  _animationEndTime;
   private static double                _lastRemainingDuration;

   /**
    * Projected position 0...1 of the model in the current frame, it also includes the micro
    * movements according to the exact relative position
    * <p>
    * {@link #_projectedPosition}[0] = x<br>
    * {@link #_projectedPosition}[1] = y<br>
    */
   private static double[]              _projectedPosition               = new double[2];
   private static long                  _projectedPosition_Time;

   /**
    * Relative position for the current frame
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
    * 0 ... 1 start...end for the NORMAL TRACK model movement<br>
    * 1 ... 2 RETURN TRACK from end...start<br>
    * 0 ...-1 RETURN TRACK from start...end
    */
   private static double                _relativePosition_Current;
   private static double                _relativePosition_Start;
   private static double                _relativePosition_End;

   private static int                   _currentVisibleIndex;

   private static boolean               _isAnimationVisible;
   private static boolean               _isPlayerEnabled;
   private static boolean               _isPlayerRunning                 = true;

   /**
    * When <code>true</code> then the model can be moving on the RETURN TRACK when it is between
    * end...start (1...2) or start...end (0...-1) otherwise the model is only on the NORMAL TRACK
    * (0...1)
    */
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

   private static Object                RELATIVE_POSITION                = new Object();

   private static int[]                 _scheduleCounter                 = new int[1];

   private static double                _nextPosition_OnNormalTrack;
   private static double                _nextPosition_OnReturnTrack;
   private static TrackState            _trackState_NormalTrack;
   private static TrackState            _trackState_ReturnTrack;

   private static long                  _lastTimelineUpdateTime;

   private static MapPosition           _mapPosition                     = new MapPosition();

   /**
    * Default animation time in milliseconds
    */
   private static int                   _modelAnimationTime              = 1000;

   /**
    * Model speed when moving on the NORMAL TRACK in units per second
    */
   private static int                   _normalTrackSpeed                = 200;

   /**
    * Model speed when moving on the RETURN TRACK
    */
   private static int                   _returnTrackSpeed_PixelPerSecond = 200;

   private static int                   _jogWheelSpeedFactor             = 50;
   private static int                   _jogWheelSpeedMultiplier         = 1;

   /**
    * Size of the moving model when the size is not scaled according to the map
    */
   public static int                    fixedModelSize                   = 400;

   /**
    * Angle how much the animated model is rotated in the next frame
    */
   private static float                 _modelTurningAngle               = 0.5f;

   private static boolean               _isModelMovingForward;
   private static float                 _modelForwardAngle;
   private static float                 _previousAngle;
   private static double                _previousRelativePosition;
// private static double                _previousRelativePositionTest;
   private static double                _previousProjectedPositionX;
   private static double                _previousProjectedPositionY;
   private static int                   _prevValue;

   enum TrackState {

      MOVING, //
      SCHEDULED, //
      IDLE, //
   }

   public static long getAnimationDuration() {
      return _modelAnimationTime;
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

   /**
    * @return Returns the moving speed value for the jog wheel control (scale)
    */
   public static int getJogWheelSpeed() {

      return _jogWheelSpeed

            // adjust to the center of the scale control
            + SPEED_JOG_WHEEL_MAX_HALF;
   }

   public static MapPlayerData getMapPlayerData() {

      return _mapPlayerData;
   }

   /**
    * @return Returns the angle for the model forward direction
    */
   public static float getModelAngle() {

      return _modelForwardAngle;
   }

   public static float getModelTurningAngle() {

      return _modelTurningAngle;
   }

   public static int getMovingSpeed() {

      return _jogWheelSpeed;
   }

   /**
    * Compute the next visible frame number, called from
    * {@link net.tourbook.map25.renderer.TourTrack_Shader#paint}
    *
    * @return Returns an index <code>0...</code>{@link #_numAllVisibleFrames}<code> - 1</code> for
    *         the next frame <code>1...</code>{@link #_numAllVisibleFrames}
    */
   public static int getNextVisibleFrameIndex() {

      if (_isPlayerRunning == false) {

         // player is paused
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

      /*
       * Get visible index from not clipped index
       */
      final double rawIndex = numNotClipped_GeoLocationIndices * _relativePosition_Current;
      int notClippedLocationIndex = (int) Math.round(rawIndex);

      // ensure bounds
      notClippedLocationIndex = clamp(notClippedLocationIndex, 0, numNotClipped_GeoLocationIndices - 1);

      final int[] allVisibleGeoLocationIndices = _mapPlayerData.allVisible_GeoLocationIndices;
      final int notClippedIndex = allNotClipped_GeoLocationIndices[notClippedLocationIndex];

      int nextFrameNumber = MtMath.searchNearestIndex(allVisibleGeoLocationIndices, notClippedIndex);

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
    * @return Returns the {@link #_projectedPosition} of the animated model for the current frame or
    *         <code>null</code> when data are missing
    */
   public static double[] getProjectedPosition() {

      final long currentFrameTime = MapRenderer.frametime;

      // check if position is already computed
      if (_projectedPosition_Time == currentFrameTime) {
         return _projectedPosition;
      }

      if (_mapPlayerData == null) {
         return null;
      }

      final int[] allNotClipped_GeoLocationIndices = _mapPlayerData.allNotClipped_GeoLocationIndices;
      final int numGeoLocations = allNotClipped_GeoLocationIndices.length;
      final int lastGeoLocationIndex = numGeoLocations - 1;

      if (lastGeoLocationIndex < 0) {
         return null;
      }

      _previousProjectedPositionX = _projectedPosition[0];
      _previousProjectedPositionY = _projectedPosition[1];

      /*
       * Compute position
       */
      // set projected position into "_projectedPosition"
      getProjectedPosition_Compute(allNotClipped_GeoLocationIndices, lastGeoLocationIndex);

      // keep time when position was computed
      _projectedPosition_Time = currentFrameTime;

      /*
       * Set model angle
       */
      final double projectedPositionX = _projectedPosition[0];
      final double projectedPositionY = _projectedPosition[1];

      setModelAngle(projectedPositionX, projectedPositionY, _previousProjectedPositionX, _previousProjectedPositionY);

      /*
       * Fire map position
       */
      if (_isPlayerRunning) {

         // set map center to the current model position

         _mapPosition.x = projectedPositionX;
         _mapPosition.y = projectedPositionY;

         MapManager.fireSyncMapEvent(_mapPosition, null, SyncParameter.SHOW_MAP_POSITION_WITHOUT_ANIMATION);
      }

      return _projectedPosition;
   }

   private static void getProjectedPosition_Compute(final int[] allNotClipped_GeoLocationIndices,
                                                    final int lastGeoLocationIndex) {

      double relativePosition = getRelativePosition();

      double[] allProjectedPoints;

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

         allProjectedPoints = _mapPlayerData.allProjectedPoints_ReturnTrack;

         final int numProjectedPoints = allProjectedPoints.length;
         final int numReturnPositions = numProjectedPoints / 2;
         final int lastReturnIndex = numReturnPositions - 1;

         exactLocationIndex = lastReturnIndex * relativeReturnPosition;

         positionIndex_0 = (int) exactLocationIndex;

         geoLocationIndex_0 = positionIndex_0;
         geoLocationIndex_1 = positionIndex_0 <= lastReturnIndex - 1
               ? positionIndex_0 + 1
               : positionIndex_0;

      } else {

         // move model on NORMAL TRACK, relativePosition is >= 0 && <= 1

         final float[] allDistanceSeries = _mapPlayerData.allDistanceSeries;
         final float totalDistance = allDistanceSeries[allDistanceSeries.length - 1];

         final float positionDistance = (float) (relativePosition * totalDistance);

         final int newDistanceIndex = MtMath.searchNearestIndex(allDistanceSeries, positionDistance);
         final int distanceGeoIndex = MtMath.searchNearestIndex(allNotClipped_GeoLocationIndices, newDistanceIndex);
         final int distanceGeoLocation = allNotClipped_GeoLocationIndices[distanceGeoIndex];

         /*
          *
          */
         allProjectedPoints = _mapPlayerData.allProjectedPoints_NormalTrack;

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

         if (geoLocationIndex_0 != _prevValue) {

            _prevValue = geoLocationIndex_0;

            System.out.println(UI.timeStamp()

                  + " diff: " + String.format("%5d", distanceGeoLocation - newDistanceIndex)
//                  + "  geoIndex: " + String.format("%5d", geoLocationIndex_0)
                  + "  distance geoIndex: " + String.format("%5d", distanceGeoLocation)
                  + "  distanceIndex: " + String.format("%5d", newDistanceIndex)

            );
// TODO remove SYSTEM.OUT.PRINTLN
         }
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
    * @return Returns the relative position {@link #_relativePosition_Current} which depends
    *         on the remaining animation time, it is between
    *         <p>
    *         0 ... 1 start...end for the normal model movement<br>
    *         1 ... 2 return track end...start<br>
    *         0 ...-1 return track start...end
    */
   private static double getRelativePosition() {

      synchronized (RELATIVE_POSITION) {

         if (_isPlayerRunning) {

            return getRelativePosition_20_FromJogWheel();

         } else {

            // player is not running, it was a manual selection on the timeline

            return getRelativePosition_10_FromTimeline();
         }
      }
   }

   private static double getRelativePosition_10_FromTimeline() {

      final long currentFrameTime = MapRenderer.frametime;
      final float remainingDuration = _animationEndTime - currentFrameTime;

      // check if animation has finished
      if (remainingDuration < 0) {

         // animation time is over, return last position

         /*
          * Ensure that the model is on the NORMAL TRACK
          */
         if (_relativePosition_End < 0) {

            // model was moving on the RETURN TRACK from start...end -> set to normal end

            _relativePosition_End = 1;

         } else if (_relativePosition_End > 1) {

            // model was moving on the RETURN TRACK from end...start -> set to normal start

            _relativePosition_End = 0;
         }

         /*
          * Fix rounding, otherwise the requested relative position is mostly not exactly set
          * which causes the model to be not at the requested position. This can be easily
          * checked with the start and end position (Home/End button).
          */
         if (_relativePosition_Current != _relativePosition_End) {

            _relativePosition_Current = _relativePosition_End;
         }

         if (_lastRemainingDuration > 0) {

            _lastRemainingDuration = 0;
         }

         /*
          * Update track state
          */
         if (_trackState_ReturnTrack != TrackState.IDLE) {

            _trackState_ReturnTrack = TrackState.IDLE;
         }

         // move on NORMAL TRACK when after the RETURN TRACK is IDLE
         if (_trackState_NormalTrack == TrackState.SCHEDULED) {

            setRelativePosition_ScheduleNewPosition_Task();

         } else {

            _trackState_NormalTrack = TrackState.IDLE;
         }

         return _relativePosition_Current;
      }

      // advance to the next animated frame

      final float relativeRemaining = remainingDuration / _modelAnimationTime; // 0...1
      final float relativeAdvance = clamp(1.0f - relativeRemaining, 0, 1);

      if (_relativePosition_End < 0) {

         // model is moving on the RETURN TRACK -> start...end -> 0...-1

         final double startEndDiff = _relativePosition_End - _relativePosition_Start;
         final double startEndAdvance = startEndDiff * relativeAdvance;
         final double currentRelativePosition = _relativePosition_Start + startEndAdvance;

         _relativePosition_Current = currentRelativePosition;

      } else if (_relativePosition_End > 1) {

         // model is moving on the RETURN TRACK -> end...start -> 1...2

         final double startEndDiff = _relativePosition_End - _relativePosition_Start;
         final double startEndAdvance = startEndDiff * relativeAdvance;
         final double currentRelativePosition = _relativePosition_Start + startEndAdvance;

         _relativePosition_Current = currentRelativePosition;

      } else {

         // _relativePosition_EndFrame: 0...1 -> model is moving on the NORMAL TRACK -> start...end

         if (_relativePosition_Current < 0) {

            // model is still moving on the RETURN TRACK from start...end -> 0...-1

            final double remainingStartFrame = 1 + _relativePosition_Start;
            final double remainingEndFrame = 1 - _relativePosition_End;

            final double startEndDiff = remainingStartFrame + remainingEndFrame;
            final double startEndAdvance = startEndDiff * relativeAdvance;
            double currentRelativePosition = _relativePosition_Start - startEndAdvance;

            // check if model in on the NORMAL or RETURN TRACK
            if (currentRelativePosition < -1) {

               // model is now back on the NORMAL TRACK -> 0...1

               currentRelativePosition += 2;

               _relativePosition_Start = _relativePosition_Start + 2;
               _relativePosition_Current = clamp(currentRelativePosition, 0, 1);

            } else {

               // model is still on the RETURN TRACK -> 0...-1

               _relativePosition_Current = clamp(currentRelativePosition, -1, 0);
            }

         } else if (_relativePosition_Current > 1) {

            // model is still moving on the RETURN TRACK from end...start -> 1...2

            final double startEndDiff = 2 - _relativePosition_Start + _relativePosition_End;
            final double startEndAdvance = startEndDiff * relativeAdvance;
            double currentRelativePosition = _relativePosition_Start + startEndAdvance;

            // check if model in on the NORMAL or RETURN TRACK
            if (currentRelativePosition > 2) {

               // model is now back on the NORMAL TRACK -> 0...1

               currentRelativePosition -= 2;

               _relativePosition_Start = _relativePosition_Start - 2;
               _relativePosition_Current = clamp(currentRelativePosition, 0, 1);

            } else {

               // model is still on the RETURN TRACK -> 1...2

               _relativePosition_Current = clamp(currentRelativePosition, 1, 2);
            }

         } else {

            // _relativePosition_CurrentFrame: 0...1 -> model is moving on the NORMAL TRACK -> start...end

            final double startEndDiff = _relativePosition_End - _relativePosition_Start;
            final double startEndAdvance = startEndDiff * relativeAdvance;
            final double currentRelativePosition = _relativePosition_Start + startEndAdvance;

            _relativePosition_Current = clamp(currentRelativePosition, 0, 1);
         }
      }

      _lastRemainingDuration = remainingDuration;

      return _relativePosition_Current;
   }

   /**
    * Compute relative position for the play head when in jog wheel mode.
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
    * @return Returns the relative position {@link #_relativePosition_Current}, it is between
    *         <p>
    *         0 ... 1 start...end for the normal model movement<br>
    *         1 ... 2 return track end...start<br>
    *         0 ...-1 return track start...end
    */
   private static double getRelativePosition_20_FromJogWheel() {

      double nextPosition;

      if (_relativePosition_Current >= 0 && _relativePosition_Current <= 1) {

         // model is moving on the NORMAL TRACK, get next position

         _jogWheelSpeedFactor = 10

               // this needs to be improved then depending on the track length, the speed is different
               * _jogWheelSpeedMultiplier;

         final double mapScale = _mapPlayerData.mapScale;

         final double speedValue = (double) _jogWheelSpeed / SPEED_JOG_WHEEL_MAX_HALF;

         final double speedValue_Scaled = speedValue / mapScale * _jogWheelSpeedFactor;

         nextPosition = _relativePosition_Current + speedValue_Scaled;

      } else {

         // model is moving on the RETURN TRACK

         final double returnSpeed = 0.02;

//         final long foregroundFPS = Map25FPSManager.getForegroundFPS();
//         final float frameDurationMS = 1000f / foregroundFPS;
//         final double end2StartPixelDistance = mapPlayerData.trackEnd2StartPixelDistance;
//         final double end2Start_AnimationTime = _defaultAnimationTime * (end2StartPixelDistance / _returnTrackSpeed_PixelPerSecond);
//

         final double positionDiff = _jogWheelSpeed > 0
               ? returnSpeed
               : -returnSpeed;

         nextPosition = _relativePosition_Current + positionDiff;
      }

      _relativePosition_Current = getRelativePosition_30_CheckStartEnd(nextPosition);

      // !!! must also update the relative end position otherwise the model would jump when timeline is selected !!!
      _relativePosition_End = _relativePosition_Current;

      /*
       * Show moved model position in the player time line
       */
      if (isPlayerViewAvailable()) {

         final long frametime = MapRenderer.frametime;
         final long updateTimeDiff = frametime - _lastTimelineUpdateTime;

         // reduce timeline updates, 100ms == 10 / second
         if (updateTimeDiff > 100) {

            _lastTimelineUpdateTime = frametime;

            _mapPlayerView.updatePlayer_Timeline(_relativePosition_Current);
         }
      }

//      final double positionDiff = _relativePosition_CurrentFrame - _previousRelativePositionTest;
//
//      System.out.println(UI.timeStamp() + " diff: " + positionDiff);
//// TODO remove SYSTEM.OUT.PRINTLN
//
//      _previousRelativePositionTest = _relativePosition_CurrentFrame;

      return _relativePosition_Current;
   }

   private static double getRelativePosition_30_CheckStartEnd(final double nextPosition) {

      if (_isPlayingLoop) {

         if (nextPosition < -1) {

            // was on 0...-1 but is now back on the NORMAL TRACK -> 0...1

            return 1;

         } else if (nextPosition > 2) {

            // was on 1...2 but is now back on the NORMAL TRACK -> 0...1

            return 0;
         }

      } else {

         // model is not looping

         if (nextPosition < 0) {

            return 0;

         } else if (nextPosition > 1) {

            return 1;
         }
      }

      return nextPosition;
   }

   public static int getSpeedMultiplier() {

      return _jogWheelSpeedMultiplier;
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

   public static boolean isPlayerEnabled() {
      return _isPlayerEnabled;
   }

   public static boolean isPlayerRunning() {
      return _isPlayerRunning;
   }

   private static boolean isPlayerViewAvailable() {
      return _mapPlayerView != null;
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

      _isPlayingLoop             = Util.getStateBoolean( _state, STATE_IS_PLAYING_LOOP,            false);
      _isReLivePlaying           = Util.getStateBoolean( _state, STATE_IS_RELIVE_PLAYING,          false);
      _jogWheelSpeed             = Util.getStateInt(     _state, STATE_JOG_WHEEL_SPEED,            DEFAULT_MOVING_SPEED);
      _jogWheelSpeedMultiplier   = Util.getStateInt(     _state, STATE_JOG_WHEEL_SPEED_MULTIPLIER, 1);
      _modelTurningAngle         = Util.getStateFloat(   _state, STATE_MODEL_TURNING_ANGLE,        5.0f);

// SET_FORMATTING_ON
   }

   public static void saveState() {

// SET_FORMATTING_OFF

      _state.put(STATE_IS_PLAYING_LOOP,            _isPlayingLoop);
      _state.put(STATE_IS_RELIVE_PLAYING,          _isReLivePlaying);
      _state.put(STATE_JOG_WHEEL_SPEED,            _jogWheelSpeed);
      _state.put(STATE_JOG_WHEEL_SPEED_MULTIPLIER, _jogWheelSpeedMultiplier);
      _state.put(STATE_MODEL_TURNING_ANGLE,        _modelTurningAngle);

// SET_FORMATTING_ON
   }

   public static void setCompileMapScale(final double x, final double y, final double scale) {

      _compileMapX = x;
      _compileMapY = y;
      _compileMapScale = scale;

      _isCompileMapScaleSet = true;
   }

   public static void setIsAnimationVisible(final boolean isAnimationVisible) {

      _isAnimationVisible = isAnimationVisible;

      if (isPlayerViewAvailable()) {
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

   /**
    * Set the angle between two positions into {@link #_modelForwardAngle}
    *
    * @param projectedX1
    * @param projectedY1
    * @param projectedX2
    * @param projectedY2
    */
   private static void setModelAngle(final double projectedX1,
                                     final double projectedY1,
                                     final double projectedX2,
                                     final double projectedY2) {

      if (projectedX1 == projectedX2 && projectedY1 == projectedY2) {
         return;
      }

      final float p21Angle = setModelAngle_GetAngleFromPositions(projectedX1, projectedY1, projectedX2, projectedY2);

      float p21AngleSmoothed = p21Angle;

      final float angleDiff = setModelAngle_Difference(p21Angle, _previousAngle);

      if (Math.abs(angleDiff) > _modelTurningAngle) {

         // default angle is larger than the min smooth angle
         // -> smoothout the animation with a smallers angle

         /*
          * Find the smallest angle diff to the current position
          */
         final float prevAngle1Smooth = _previousAngle + _modelTurningAngle;
         final float prevAngle2Smooth = _previousAngle - _modelTurningAngle;

         final float angleDiff1 = setModelAngle_Shortest(p21Angle, prevAngle1Smooth);
         final float angleDiff2 = setModelAngle_Shortest(p21Angle, prevAngle2Smooth);

         // use the smallest difference
         p21AngleSmoothed = angleDiff1 < angleDiff2
               ? prevAngle1Smooth
               : prevAngle2Smooth;
      }

      p21AngleSmoothed = p21AngleSmoothed % 360;

      final float angleDiffLog = _previousAngle - p21AngleSmoothed;
      if (angleDiffLog > 0.1) {

//         System.out.println(""
//
//               + " prev: " + String.format("%6.1f", _previousAngle)
//               + " p21Angle: " + String.format("%6.1f", p21Angle)
//
//         );
// TODO remove SYSTEM.OUT.PRINTLN
      }

      _previousAngle = p21AngleSmoothed;

      final float modelForwardAngle = p21AngleSmoothed

            // must be turned otherwise it looks in the wrong direction
            + 90

      ;

      _modelForwardAngle = modelForwardAngle % 360;
   }

   /**
    * Source:
    * https://stackoverflow.com/questions/1878907/how-can-i-find-the-difference-between-two-angles
    *
    * @param angle1
    * @param angle2
    * @return Returns the difference between two angles 0...360
    */
   private static float setModelAngle_Difference(final float angle1, final float angle2) {

      float angleDiff = angle1 - angle2;

      angleDiff = (angleDiff + 540) % 360 - 180;

      return angleDiff;
   }

   private static float setModelAngle_GetAngleFromPositions(final double x1, final double y1, final double x2, final double y2) {

      double deltaXDouble;
      double deltaYDouble;

      if (_isModelMovingForward) {

         deltaXDouble = x2 - x1;
         deltaYDouble = y1 - y2;

      } else {

         deltaXDouble = x1 - x2;
         deltaYDouble = y2 - y1;
      }

      final float deltaX = (float) deltaXDouble;
      final float deltaY = (float) deltaYDouble;

      final double angleDegree = Math.toDegrees(MathUtils.atan2(deltaY, deltaX));

      return (float) ((angleDegree < 0) ? (360d + angleDegree) : angleDegree);
   }

   /**
    * Source:
    * https://stackoverflow.com/questions/2708476/rotation-interpolation
    *
    * @param angle1
    * @param angle2
    * @return Returns the difference between two angles 0...360
    */
   private static float setModelAngle_Shortest(final float angle1, final float angle2) {

      final float angleDiff = ((((angle1 - angle2) % 360) + 540) % 360) - 180;

      return Math.abs(angleDiff);
   }

   public static void setMovingSpeedFromJogWheel(final int jogWheelSpeed) {

      _jogWheelSpeed = jogWheelSpeed

            // adjust to the center of the scale control
            - SPEED_JOG_WHEEL_MAX_HALF;

      _isModelMovingForward = _jogWheelSpeed >= 0;
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

      if (isPlayerViewAvailable()) {
         _mapPlayerView.updatePlayer();
      }
   }

   /**
    * Move player head to a relative position and start playing to this position, it is called
    * from {@link net.tourbook.map.player.MapPlayerView#setMapAndModelPosition(double)}
    * <p>
    * The relative position is in this moving loop, start and end must not be at the same position:
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
    *           which is between
    *           <p>
    *           0 ... 1 start...end for the normal model movement<br>
    *           1 ... 2 return track end...start<br>
    *           0 ...-1 return track start...end
    */
   public static void setRelativePosition(final double newRelativePosition) {

      // ignore the same position
      if (newRelativePosition == _relativePosition_End) {
         return;
      }

      if (_mapPlayerData == null) {
         return;
      }

      synchronized (RELATIVE_POSITION) {

         /**
          * !!! Complicated !!!
          * <p>
          * The track state is necessary that the model in not moving on the NORMAL TRACK in reverse
          * direction by skipping the RETURN TRACK
          */

         final boolean isSetNormalTrack = newRelativePosition >= 0 && newRelativePosition <= 1;
         final boolean isCurrentlyOnNormalTrack = _relativePosition_Current >= 0 && _relativePosition_Current <= 1;

         /*
          * Set forward flag
          */
         _isModelMovingForward = true;
         if (isSetNormalTrack) {
            _isModelMovingForward = newRelativePosition >= _previousRelativePosition;
         } else {
            if (newRelativePosition == 2) {
               _isModelMovingForward = false;
            }
         }
         _previousRelativePosition = newRelativePosition;

         /*
          * Set position
          */
         if (isSetNormalTrack && isCurrentlyOnNormalTrack
               && _trackState_ReturnTrack == TrackState.IDLE
               && _trackState_NormalTrack == TrackState.IDLE) {

            // model is
            // - moving on the NORMAL TRACK
            // - keeps moving on the NORMAL TRACK
            // - nothing is scheduled

            setRelativePosition_0(newRelativePosition);

         } else {

            setRelativePosition_ScheduleNewPosition(newRelativePosition);
         }
      }
   }

   private static void setRelativePosition_0(final double newRelativePosition) {

      final int animationTime = setRelativePosition_GetAnimationTime(newRelativePosition);

      _animationEndTime = MapRenderer.frametime + animationTime;

      // set new start position from the current position
      _relativePosition_Start = _relativePosition_Current;

      _relativePosition_End = newRelativePosition;
   }

   /**
    * @param newRelativePosition
    * @return Returns the animation duration time for the next position
    */
   private static int setRelativePosition_GetAnimationTime(final double newRelativePosition) {

      _modelAnimationTime = 1000;

      _returnTrackSpeed_PixelPerSecond = 200;

      if (_mapPlayerData == null) {
         return _modelAnimationTime;
      }

      if (newRelativePosition >= 0 && newRelativePosition <= 1) {

         // 0...1 -> model is moving on the NORMAL TRACK

         return _modelAnimationTime;

      } else {

         // model is moving on the RETURN TRACK

         final double pixelDistance = _mapPlayerData.trackEnd2StartPixelDistance;

         final double animationTime = _modelAnimationTime * (pixelDistance / _returnTrackSpeed_PixelPerSecond);

//      System.out.println(UI.timeStamp()
//
//            + " pixelDistance: " + pixelDistance
//            + " animationTime: " + animationTime
//
//      );
// TODO remove SYSTEM.OUT.PRINTLN

         return (int) clamp(animationTime, 1, _modelAnimationTime);
      }
   }

   private static void setRelativePosition_ScheduleNewPosition(final double newRelativePosition) {

//      System.out.println(UI.timeStamp() + " scheduled: " + newRelativePosition);
//// TODO remove SYSTEM.OUT.PRINTLN

      final long currentFrameTime = MapRenderer.frametime;

      // set scheduled time which is after the last animation
      final long remainingAnimationTime = _animationEndTime - currentFrameTime;
      final long nextScheduledTime = remainingAnimationTime > 0

            // start schedule at the end of the current animation
            ? remainingAnimationTime

            : 0;

      final boolean isSetNormalTrack = newRelativePosition >= 0 && newRelativePosition <= 1;
      final boolean isSetReturnTrack = isSetNormalTrack == false;

      if (isSetReturnTrack) {

         // set RETURN TRACK

         _nextPosition_OnReturnTrack = newRelativePosition;

         _trackState_ReturnTrack = TrackState.SCHEDULED;

         // a RETURN TRACK overwrites the NORMAL TRACK
         _trackState_NormalTrack = TrackState.IDLE;

      } else {

         // set NORMAL TRACK

         _nextPosition_OnNormalTrack = newRelativePosition;

         _trackState_NormalTrack = TrackState.SCHEDULED;
      }

      if (nextScheduledTime == 0) {

         // run task now

         setRelativePosition_ScheduleNewPosition_Task();

      } else {

         // schedule task

         _scheduleCounter[0]++;

         final Display display = Display.getDefault();

         // timerExec MUST be run from the display thread, otherwise org.eclipse.swt.SWTException: Invalid thread access
         display.syncExec(() -> {

            display.timerExec((int) nextScheduledTime, new Runnable() {

               final int __runnableCounter = _scheduleCounter[0];

               @Override
               public void run() {

                  // skip all events which has not yet been executed
                  if (__runnableCounter != _scheduleCounter[0]) {

                     // a newer event occurred

                     return;
                  }

                  setRelativePosition_ScheduleNewPosition_Task();
               }
            });
         });
      }
   }

   private static void setRelativePosition_ScheduleNewPosition_Task() {

      if (_trackState_ReturnTrack == TrackState.SCHEDULED) {

         _trackState_ReturnTrack = TrackState.MOVING;

         setRelativePosition_0(_nextPosition_OnReturnTrack);

      } else if (_trackState_NormalTrack == TrackState.SCHEDULED) {

         _trackState_NormalTrack = TrackState.MOVING;

         setRelativePosition_0(_nextPosition_OnNormalTrack);
      }
   }

   public static void setSpeedMultiplier(final int speedMultiplier) {

      _jogWheelSpeedMultiplier = speedMultiplier;
   }

   public static void setTurningAngle(final float modelTurningAngle) {

      _modelTurningAngle = modelTurningAngle;
   }

}
