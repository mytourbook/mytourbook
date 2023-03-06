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
package net.tourbook.map.player;

import static org.oscim.utils.FastMath.clamp;

import com.badlogic.gdx.math.MathUtils;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.Util;
import net.tourbook.map.IMapSyncListener.SyncParameter;
import net.tourbook.map.MapManager;
import net.tourbook.map.model.MapModelManager;
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25FPSManager;
import net.tourbook.map25.Map25View;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Display;
import org.oscim.core.MapPosition;
import org.oscim.renderer.MapRenderer;

/**
 * Is managing the movement of the map model and cursor
 */
public class ModelPlayerManager {

   /**
    * Max value for the scale control which cannot have negative values but the speed can be
    * negative.
    */
   static final int                     SPEED_JOG_WHEEL_MAX               = 2 * 100;
   static final int                     SPEED_JOG_WHEEL_MAX_HALF          = SPEED_JOG_WHEEL_MAX / 2;

   public static final int              MAP_ZOOM_LEVEL_IS_NOT_AVAILABLE   = -1;

   private static final String          STATE_IS_MAP_MODEL_VISIBLE        = "STATE_IS_MAP_MODEL_VISIBLE";                                         //$NON-NLS-1$
   private static final String          STATE_IS_MAP_MODEL_CURSOR_VISIBLE = "STATE_IS_MAP_MODEL_CURSOR_VISIBLE";                                  //$NON-NLS-1$
   private static final String          STATE_IS_PLAYER_RUNNING           = "STATE_IS_PLAYER_RUNNING";                                            //$NON-NLS-1$
   private static final String          STATE_IS_PLAYING_LOOP             = "STATE_IS_PLAYING_LOOP";                                              //$NON-NLS-1$
   private static final String          STATE_IS_RELIVE_PLAYING           = "STATE_IS_RELIVE_PLAYING";                                            //$NON-NLS-1$
   private static final String          STATE_JOG_WHEEL_SPEED             = "STATE_JOG_WHEEL_SPEED";                                              //$NON-NLS-1$
   private static final String          STATE_JOG_WHEEL_SPEED_MULTIPLIER  = "STATE_JOG_WHEEL_SPEED_MULTIPLIER";                                   //$NON-NLS-1$
   private static final String          STATE_MODEL_CURSOR_SIZE           = "STATE_MODEL_CURSOR_SIZE";                                            //$NON-NLS-1$
   private static final String          STATE_MODEL_SIZE                  = "STATE_MODEL_SIZE";                                                   //$NON-NLS-1$
   private static final String          STATE_MODEL_TURNING_ANGLE         = "STATE_MODEL_TURNING_ANGLE";                                          //$NON-NLS-1$
   private static final String          STATE_RELATIVE_POSITION           = "STATE_RELATIVE_POSITION";                                            //$NON-NLS-1$
   //
   private static final IDialogSettings _state                            = TourbookPlugin.getState("net.tourbook.map.player.ModelPlayerManager");//$NON-NLS-1$

   private static final int             MODEL_SIZE_DEFAULT                = 200;
   static final int                     MODEL_SIZE_MIN                    = 20;
   static final int                     MODEL_SIZE_MAX                    = 10_000;
   private static final int             MODEL_CURSOR_SIZE_DEFAULT         = 200;
   static final int                     MODEL_CURSOR_SIZE_MIN             = 10;
   static final int                     MODEL_CURSOR_SIZE_MAX             = 10_000;

   private static Map25View             _map25View;
   private static ModelPlayerView       _modelPlayerView;

   private static int                   _currentVisiblePositionIndex;

   /**
    * Number of frames for an animation
    */
   private static int                   _numAllVisiblePositions;

   /**
    * Is between - {@value #SPEED_JOG_WHEEL_MAX_HALF} ... + {@value #SPEED_JOG_WHEEL_MAX_HALF}
    */
   private static int                   _jogWheelSpeed                    = 10;

   private static long                  _animationEndTime;
   private static double                _lastRemainingDuration;

   /**
    * Projected position 0...1 of the model in the current frame, it also includes the micro
    * movements according to the exact relative position
    * <p>
    * <code>
    * _currentProjectedPosition[0] = x<br>
    * _currentProjectedPosition[1] = y<br>
    * </code>
    */
   private static double[]              _currentProjectedPosition         = new double[2];
   private static long                  _currentProjectedPosition_Time;

   /**
    * Geo location index for the current position of the model/cursor
    */
   private static int                   _currentVisibleGeoLocationIndex;

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

   private static boolean               _isMapModelVisible;

   /**
    * When <code>true</code> then an animated triangle shows the exact cursor position
    */
   private static boolean               _isMapModelCursorVisible;

   private static boolean               _isPlayerRunning;

   /**
    * When <code>true</code> then the model can be moving on the RETURN TRACK when it is between
    * end...start (1...2) or start...end (0...-1) otherwise the model is only on the NORMAL TRACK
    * (0...1)
    */
   private static boolean               _isPlayingLoop;
   private static boolean               _isReLivePlaying;

   private static ModelPlayerData       _modelPlayerData;

   /**
    * Map scale with which the tour track was compiled
    */
   private static double                _compileMapScale;
   private static boolean               _isCompileMapScaleSet;

   private static double                _compileMapX;
   private static double                _compileMapY;

   private static Object                RELATIVE_POSITION                 = new Object();

   private static int[]                 _scheduleCounter                  = new int[1];

   private static double                _nextPosition_OnNormalTrack;
   private static double                _nextPosition_OnReturnTrack;
   private static TrackState            _trackState_NormalTrack;
   private static TrackState            _trackState_ReturnTrack;

   private static long                  _lastTimelineUpdateTime;

   private static MapPosition           _mapPosition                      = new MapPosition();

   /**
    * Default animation time in milliseconds
    */
   private static int                   _modelAnimationTime               = 1000;

   /**
    * Model speed when moving on the RETURN TRACK
    */
   private static int                   _returnTrackSpeed_PixelPerSecond  = 200;

   private static int                   _jogWheelSpeedMultiplier          = 1;

   /**
    * Size of the moving model when the size is not scaled according to the map
    */
   private static int                   _modelSize;

   /**
    * Angle how much the animated model is rotated in the next frame
    */
   private static int                   _modelTurningAngle;

   private static int                   _modelCursorSize;

   private static boolean               _isModelMovingForward;
   private static float                 _modelForwardAngle;
   private static float                 _previousAngle;
   private static double                _previousRelativePosition;
   private static double                _previousProjectedPositionX;
   private static double                _previousProjectedPositionY;

//   private static double                _debugPrevValue;
//   private static String                _debugTimeStamp                   = UI.timeStamp();

   enum TrackState {

      MOVING, //
      SCHEDULED, //
      IDLE, //
   }

   /**
    * @return Returns <code>true</code> when 2.5D map is displayed
    */
   public static boolean canShowMapModel() {

      return isMap25ViewAvailable();
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
    * @return Returns {@link #_currentProjectedPosition} of the animated model for the current
    *         frame or <code>null</code> when data are missing
    */
   public static double[] getCurrentProjectedPosition() {

      final long currentFrameTime = MapRenderer.frametime;

      // check if position is already computed
      if (_currentProjectedPosition_Time == currentFrameTime) {
         return _currentProjectedPosition;
      }

      if (_modelPlayerData == null) {
         return null;
      }

      final int[] allNotClipped_GeoLocationIndices = _modelPlayerData.allNotClipped_GeoLocationIndices;
      final int numGeoLocations = allNotClipped_GeoLocationIndices.length;
      final int lastGeoLocationIndex = numGeoLocations - 1;

      if (lastGeoLocationIndex < 0) {
         return null;
      }

      _previousProjectedPositionX = _currentProjectedPosition[0];
      _previousProjectedPositionY = _currentProjectedPosition[1];

      /*
       * Compute position
       */
      // set projected position into "_projectedPosition"
      getCurrentProjectedPosition_ComputePosition(allNotClipped_GeoLocationIndices, lastGeoLocationIndex);

      // keep time when position was computed
      _currentProjectedPosition_Time = currentFrameTime;

      /*
       * Set model angle
       */
      final double projectedPositionX = _currentProjectedPosition[0];
      final double projectedPositionY = _currentProjectedPosition[1];

      setModelAngle(projectedPositionX, projectedPositionY, _previousProjectedPositionX, _previousProjectedPositionY);

      /*
       * Fire map position
       */
      if (_isPlayerRunning || _isReLivePlaying) {

         // set map center to the current model position

         _mapPosition.x = projectedPositionX;
         _mapPosition.y = projectedPositionY;

         if (isMap25ViewAvailable()) {

            final MapPosition mapPosition = _map25View.getMapPosition();

            _mapPosition.zoomLevel = mapPosition.zoomLevel;

            _mapPosition.bearing = mapPosition.bearing;
            _mapPosition.roll = mapPosition.roll;
            _mapPosition.tilt = mapPosition.tilt;

         } else {

            _mapPosition.zoomLevel = MAP_ZOOM_LEVEL_IS_NOT_AVAILABLE;
         }

         MapManager.fireSyncMapEvent(_mapPosition, null, SyncParameter.SHOW_MAP_POSITION_WITHOUT_ANIMATION);
      }

      return _currentProjectedPosition;
   }

   private static void getCurrentProjectedPosition_ComputePosition(final int[] allNotClipped_GeoLocationIndices,
                                                                   final int lastGeoLocationIndex) {

      double relativePosition = getRelativePosition();

      double[] allProjectedPoints;

      int geoLocationIndex_0 = 0;
      int geoLocationIndex_1 = 0;
      int positionIndex_0;
      double exactLocationIndex = 0;

      // 0...1
      double subIndex;

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

         allProjectedPoints = _modelPlayerData.allProjectedPoints_ReturnTrack;

         final int numProjectedPoints = allProjectedPoints.length;
         final int numReturnPositions = numProjectedPoints / 2;
         final int lastReturnIndex = numReturnPositions - 1;

         exactLocationIndex = lastReturnIndex * relativeReturnPosition;

         subIndex = exactLocationIndex - (int) exactLocationIndex;

         positionIndex_0 = (int) exactLocationIndex;

         geoLocationIndex_0 = positionIndex_0;
         geoLocationIndex_1 = positionIndex_0 <= lastReturnIndex - 1
               ? positionIndex_0 + 1
               : positionIndex_0;

      } else {

         // move model on NORMAL TRACK, relativePosition is >= 0 && <= 1

         allProjectedPoints = _modelPlayerData.allProjectedPoints_NormalTrack;

         final float[] allDistanceSeries = _modelPlayerData.allDistanceSeries;
         final int lastDistanceIndex = allDistanceSeries.length - 1;

         final float totalDistance = allDistanceSeries[lastDistanceIndex];
         final float positionDistance = (float) (relativePosition * totalDistance);

         final int distanceIndex = MtMath.searchIndex(allDistanceSeries, positionDistance);

         geoLocationIndex_0 = distanceIndex;
         geoLocationIndex_1 = geoLocationIndex_0 < lastDistanceIndex

               ? geoLocationIndex_0 + 1
               : geoLocationIndex_0;

         final float distance_0 = allDistanceSeries[geoLocationIndex_0];
         final float distance_1 = allDistanceSeries[geoLocationIndex_1];

         final float distanceDiff = distance_1 - distance_0;
         final float subDiff = positionDistance - distance_0;

         subIndex = distanceDiff == 0 ? 0 : subDiff / distanceDiff;

//         if (_debugPrevValue != relativePosition) {
//
//            System.out.println(UI.timeStamp()
//
////                  + " diff: " + String.format("%5d", geoLocationIndex_0 - distanceIndex)
//
//                  + "  geoIndex: " + String.format("%5d", geoLocationIndex_0)
//
//                  + "  relPos: " + String.format("%6.3f", relativePosition)
//                  + "  relDiff: " + String.format("%9.6f", relativePosition - _debugPrevValue)
//
////
////                  + "  subIndex: " + String.format("%6.3f", subIndex)
////                  + "  indexDiff: " + String.format("%6.3f", subIndex - _prevValue)
//
////                  + "  distanceIndex: " + String.format("%5d", distanceIndex)
//
//            );
//// TODO remove SYSTEM.OUT.PRINTLN
//
//            _debugPrevValue = relativePosition;
//         }
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

      final double advanceX = projectedPositionX_Diff * subIndex;
      final double advanceY = projectedPositionY_Diff * subIndex;

      final double projectedPositionX = projectedPositionX_0 + advanceX;
      final double projectedPositionY = projectedPositionY_0 + advanceY;

      _currentProjectedPosition[0] = projectedPositionX;
      _currentProjectedPosition[1] = projectedPositionY;

      _currentVisibleGeoLocationIndex = MtMath.searchIndex(_modelPlayerData.allVisible_GeoLocationIndices, geoLocationIndex_0);
   }

   public static double getCurrentRelativePosition() {

      return _relativePosition_Current;
   }

   /**
    * @return Returns the geo location index for the {@link #_currentProjectedPosition} into
    *         {@link ModelPlayerData#allVisible_GeoLocationIndices}
    */
   public static int getCurrentVisibleGeoLocationIndex() {

      return _currentVisibleGeoLocationIndex;
   }

   /**
    * @return Returns the moving speed value for the jog wheel control (scale)
    */
   public static int getJogWheelSpeed() {

      return _jogWheelSpeed

            // adjust to the center of the scale control
            + SPEED_JOG_WHEEL_MAX_HALF;
   }

   /**
    * @return Returns the angle for the model forward direction
    */
   public static float getModelAngle() {

      return _modelForwardAngle;
   }

   public static short getModelCursorSize() {
      return (short) _modelCursorSize;
   }

   /**
    * @return Returns the track data for the currently played tour
    */
   public static ModelPlayerData getModelPlayerData() {

      return _modelPlayerData;
   }

   public static int getModelSize() {
      return _modelSize;
   }

   public static int getModelTurningAngle() {

      return _modelTurningAngle;
   }

   public static int getMovingSpeed() {

      return _jogWheelSpeed;
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

            return getRelativePosition_20_From_JogWheel();

         } else {

            // player is not running, it was a manual selection on the timeline

            return getRelativePosition_10_From_Timeline();
         }
      }
   }

   private static double getRelativePosition_10_From_Timeline() {

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
   private static double getRelativePosition_20_From_JogWheel() {

      double nextPosition;

      if (_relativePosition_Current >= 0 && _relativePosition_Current <= 1) {

         // model is moving on the NORMAL TRACK, get next position

         final float[] allDistanceSeries = _modelPlayerData.allDistanceSeries;
         final float totalDistance = allDistanceSeries[allDistanceSeries.length - 1];

         final float distanceFactor = totalDistance == 0
               ? 1
               : 100_000 / totalDistance;

         final double jogWheelSpeed = (double) _jogWheelSpeed / SPEED_JOG_WHEEL_MAX_HALF;
         final double mapScale = _modelPlayerData.mapScale;
         final float jogWheelSpeedFactor = distanceFactor * _jogWheelSpeedMultiplier;

         final double scaledSpeedValue = jogWheelSpeed / mapScale * jogWheelSpeedFactor;

         nextPosition = _relativePosition_Current + scaledSpeedValue;

      } else {

         // model is moving on the RETURN TRACK

         final double returnSpeed = 0.02;

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

            _modelPlayerView.updatePlayer_Timeline(_relativePosition_Current);
         }
      }

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

      return _currentVisiblePositionIndex == _numAllVisiblePositions - 1;
   }

   private static boolean isMap25ViewAvailable() {
      return _map25View != null;
   }

   public static boolean isMapModelCursorVisible() {
      return _isMapModelCursorVisible;
   }

   public static boolean isMapModelVisible() {
      return _isMapModelVisible;
   }

   public static boolean isPlayerRunning() {
      return _isPlayerRunning;
   }

   private static boolean isPlayerViewAvailable() {
      return _modelPlayerView != null;
   }

   public static boolean isPlayingLoop() {
      return _isPlayingLoop;
   }

   public static boolean isReLivePlaying() {
      return _isReLivePlaying;
   }

   public static void restoreState() {

// SET_FORMATTING_OFF

      _isMapModelVisible         = Util.getStateBoolean( _state, STATE_IS_MAP_MODEL_VISIBLE,          true);
      _isMapModelCursorVisible   = Util.getStateBoolean( _state, STATE_IS_MAP_MODEL_CURSOR_VISIBLE,   true);
      _isPlayerRunning           = Util.getStateBoolean( _state, STATE_IS_PLAYER_RUNNING,             true);
      _isPlayingLoop             = Util.getStateBoolean( _state, STATE_IS_PLAYING_LOOP,               false);
      _isReLivePlaying           = Util.getStateBoolean( _state, STATE_IS_RELIVE_PLAYING,             false);
      _jogWheelSpeed             = Util.getStateInt(     _state, STATE_JOG_WHEEL_SPEED,               10);
      _jogWheelSpeedMultiplier   = Util.getStateInt(     _state, STATE_JOG_WHEEL_SPEED_MULTIPLIER,    1);
      _modelSize                 = Util.getStateInt(     _state, STATE_MODEL_SIZE,                    MODEL_SIZE_DEFAULT,           MODEL_SIZE_MIN,            MODEL_SIZE_MAX);
      _modelCursorSize           = Util.getStateInt(     _state, STATE_MODEL_CURSOR_SIZE,             MODEL_CURSOR_SIZE_DEFAULT,    MODEL_CURSOR_SIZE_MIN,     MODEL_CURSOR_SIZE_MAX);
      _modelTurningAngle         = Util.getStateInt(     _state, STATE_MODEL_TURNING_ANGLE,           10);
      _relativePosition_Current  = Util.getStateDouble(  _state, STATE_RELATIVE_POSITION,             0);

// SET_FORMATTING_ON
   }

   public static void restoreState_UI() {

      if (isPlayerViewAvailable()) {

         Display.getDefault().syncExec(() -> _modelPlayerView.restoreState());
      }

      Map25FPSManager.setContinuousRendering(_isMapModelVisible || _isMapModelCursorVisible || _isPlayerRunning);

      setIsModelMovingForward(_jogWheelSpeed >= 0);
   }

   public static void saveState() {

// SET_FORMATTING_OFF

      _state.put(STATE_IS_MAP_MODEL_VISIBLE,          _isMapModelVisible);
      _state.put(STATE_IS_MAP_MODEL_CURSOR_VISIBLE,   _isMapModelCursorVisible);
      _state.put(STATE_IS_PLAYER_RUNNING,             _isPlayerRunning);
      _state.put(STATE_IS_PLAYING_LOOP,               _isPlayingLoop);
      _state.put(STATE_IS_RELIVE_PLAYING,             _isReLivePlaying);
      _state.put(STATE_JOG_WHEEL_SPEED,               _jogWheelSpeed);
      _state.put(STATE_JOG_WHEEL_SPEED_MULTIPLIER,    _jogWheelSpeedMultiplier);
      _state.put(STATE_MODEL_SIZE,                    _modelSize);
      _state.put(STATE_MODEL_CURSOR_SIZE,             _modelCursorSize);
      _state.put(STATE_MODEL_TURNING_ANGLE,           _modelTurningAngle);
      _state.put(STATE_RELATIVE_POSITION,             _relativePosition_Current);

// SET_FORMATTING_ON

      MapModelManager.saveState();
   }

   public static void setCompileMapScale(final double x, final double y, final double scale) {

      _compileMapX = x;
      _compileMapY = y;
      _compileMapScale = scale;

      _isCompileMapScaleSet = true;
   }

   public static void setIsMapModelCursorVisible(final boolean isMapModelCursorVisible) {

      _isMapModelCursorVisible = isMapModelCursorVisible;

      updateUI_Map();
   }

   public static void setIsMapModelVisible(final boolean isMapModelVisible) {

      _isMapModelVisible = isMapModelVisible;

      updateUI_Map();
   }

   private static void setIsModelMovingForward(final boolean isModelMovingForward) {

      _isModelMovingForward = isModelMovingForward;
   }

   public static void setIsPlayerRunning(final boolean isPlayerRunning) {

      _isPlayerRunning = isPlayerRunning;

      updateUI_Map();
   }

   public static void setIsPlayingLoop(final boolean isPlayingLoop) {

      _isPlayingLoop = isPlayingLoop;
   }

   public static void setIsReLivePlaying(final boolean isReLivePlaying) {

      _isReLivePlaying = isReLivePlaying;

      updateUI_Map();
   }

   public static void setMap25View(final Map25View map25View) {

      _map25View = map25View;

      if (isPlayerViewAvailable()) {
         _modelPlayerView.updateMapModelVisibility();
      }
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
      final float angleDiffAbs = Math.abs(angleDiff);

      if (angleDiffAbs > 0.1) {

         // the next angle is larger than a min smooth angle
         // -> smoothout the animation with a smallers angle

         final float modelTurningAngle = (float) (angleDiffAbs * 0.01 * _modelTurningAngle);

         /*
          * Find the smallest angle diff to the current position
          */
         final float prevAngle1Smooth = _previousAngle + modelTurningAngle;
         final float prevAngle2Smooth = _previousAngle - modelTurningAngle;

         final float angleDiff1 = setModelAngle_Shortest(p21Angle, prevAngle1Smooth);
         final float angleDiff2 = setModelAngle_Shortest(p21Angle, prevAngle2Smooth);

         // use the smallest difference
         p21AngleSmoothed = angleDiff1 < angleDiff2
               ? prevAngle1Smooth
               : prevAngle2Smooth;
      }

      p21AngleSmoothed = p21AngleSmoothed % 360;

      _previousAngle = p21AngleSmoothed;

      final float modelForwardAngle = p21AngleSmoothed

            // must be turned otherwise it looks in the wrong direction
            + 90;

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

   public static void setModelCursorSize(final int value) {

      _modelCursorSize = value;

      if (isMap25ViewAvailable()) {

         // this could be optimized, that not the whole track is recomputed, only the map model cursor size
         _map25View.getMapApp().getLayer_Tour().getTourTrackRenderer().onModifyMapModelOrCursor();
      }
   }

   public static void setModelPlayerView(final ModelPlayerView modelPlayerView) {

      _modelPlayerView = modelPlayerView;
   }

   public static void setModelSize(final int modelSize) {

      _modelSize = modelSize;
   }

   public static void setMovingSpeedFromJogWheel(final int jogWheelSpeed) {

      _jogWheelSpeed = jogWheelSpeed

            // adjust to the center of the scale control
            - SPEED_JOG_WHEEL_MAX_HALF;

      setIsModelMovingForward(_jogWheelSpeed >= 0);
   }

   /**
    * Setup model player with all necessary data to run the animation.
    * <p>
    * This method is called when new data are set into the shader buffer data, for a new zoom level
    * or when map is moved more than a tile
    *
    * @param modelPlayerData
    */
   public static void setPlayerData(final ModelPlayerData modelPlayerData) {

      _modelPlayerData = modelPlayerData;

      _numAllVisiblePositions = modelPlayerData.allVisible_GeoLocationIndices == null
            ? 0
            : modelPlayerData.allVisible_GeoLocationIndices.length;

      if (isPlayerViewAvailable()) {
         _modelPlayerView.updatePlayer();
      }
   }

   /**
    * Move player head to a relative position and start playing to this position, it is called
    * from {@link net.tourbook.map.player.ModelPlayerView#setMapAndModelPosition(double)}
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

      if (_modelPlayerData == null) {
         return;
      }

      synchronized (RELATIVE_POSITION) {

         /**
          * !!! Complicated !!!
          * <p>
          * The track state is necessary that the model in not moving on the NORMAL TRACK in reverse
          * direction by skipping the RETURN TRACK
          */

         final boolean isNewPositionOnNormalTrack = newRelativePosition >= 0 && newRelativePosition <= 1;
         final boolean isCurrentPositionOnNormalTrack = _relativePosition_Current >= 0 && _relativePosition_Current <= 1;

         /*
          * Set forward flag
          */
         boolean isModelMovingForward = true;

         if (isNewPositionOnNormalTrack) {

            isModelMovingForward = _previousRelativePosition == ModelPlayerView.RELATIVE_MODEL_POSITION_ON_RETURN_PATH_END_TO_START

                  // model moved from the end to the start
                  ? true

                  : _previousRelativePosition == ModelPlayerView.RELATIVE_MODEL_POSITION_ON_RETURN_PATH_START_TO_END

                        // model moved from the start to the end
                        ? false

                        : newRelativePosition >= _previousRelativePosition;

         } else {

            if (newRelativePosition == ModelPlayerView.RELATIVE_MODEL_POSITION_ON_RETURN_PATH_START_TO_END) {

               isModelMovingForward = false;
            }
         }

         setIsModelMovingForward(isModelMovingForward);

         _previousRelativePosition = newRelativePosition;

         /*
          * Set position
          */
         if (isNewPositionOnNormalTrack && isCurrentPositionOnNormalTrack
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

      if (_modelPlayerData == null) {
         return _modelAnimationTime;
      }

      if (newRelativePosition >= 0 && newRelativePosition <= 1) {

         // 0...1 -> model is moving on the NORMAL TRACK

         return _modelAnimationTime;

      } else {

         // model is moving on the RETURN TRACK

         final double pixelDistance = _modelPlayerData.trackEnd2StartPixelDistance;

         final double animationTime = _modelAnimationTime * (pixelDistance / _returnTrackSpeed_PixelPerSecond);

         return (int) clamp(animationTime, 1, _modelAnimationTime);
      }
   }

   private static void setRelativePosition_ScheduleNewPosition(final double newRelativePosition) {

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

   public static void setTurningAngle(final int modelTurningAngle) {

      _modelTurningAngle = modelTurningAngle;
   }

   private static void updateUI_Map() {

      if (isMap25ViewAvailable() == false) {
         return;
      }

      final Map25App map25App = _map25View.getMapApp();

      if (_isMapModelVisible || _isMapModelCursorVisible || _isPlayerRunning || _isReLivePlaying) {

         // setup data when map model + cursor is displayed

         map25App.getLayer_Tour().getTourTrackRenderer().onModifyMapModelOrCursor();
      }

      Map25FPSManager.setContinuousRendering(_isMapModelVisible || _isMapModelCursorVisible || _isPlayerRunning || _isReLivePlaying);

      map25App.getMap().updateMap();
   }

}
