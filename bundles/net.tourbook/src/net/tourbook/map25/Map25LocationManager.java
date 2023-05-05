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

import java.util.concurrent.atomic.AtomicInteger;

import net.tourbook.map.player.ModelPlayerManager;

import org.oscim.core.BoundingBox;
import org.oscim.core.MapPosition;
import org.oscim.map.Animator;
import org.oscim.map.Map;
import org.oscim.utils.animation.Easing;

/**
 * Manage map location requests
 */
public class Map25LocationManager {

   private static final AtomicInteger _asyncCounter        = new AtomicInteger();
   private static Easing.Type         _animationEasingType = Easing.Type.LINEAR;
   private static boolean             _isAnimateLocation   = true;
   private static long                _lastAnimationTime;

   /**
    * Set map location with animation
    *
    * @param map
    * @param boundingBox
    * @param locationAnimationTime
    */
   public static void setMapLocation(final Map map, final BoundingBox boundingBox, int locationAnimationTime) {

      final Animator animator = map.animator();

      // zero will not move the map, set 1 ms
      if (locationAnimationTime == 0 || _isAnimateLocation == false) {
         locationAnimationTime = 1;
      }

      animator.animateTo(
            locationAnimationTime,
            boundingBox,
            Easing.Type.LINEAR,
            Animator.ANIM_MOVE | Animator.ANIM_SCALE);
   }

   public static void setMapLocation(final Map map, final MapPosition mapPosition) {

      _isAnimateLocation = true;

      _animationEasingType = Easing.Type.QUART_INOUT;
      _animationEasingType = Easing.Type.QUINT_INOUT;
      _animationEasingType = Easing.Type.QUAD_INOUT;
      _animationEasingType = Easing.Type.SINE_IN;
      _animationEasingType = Easing.Type.SINE_INOUT;
      _animationEasingType = Easing.Type.SINE_OUT;

      _animationEasingType = Easing.Type.LINEAR;

      map.post(() -> setMapLocation_InMapThread(map, mapPosition));
   }

   private static void setMapLocation_InMapThread(final Map map, final MapPosition mapPosition) {

      final long animationDuration = ModelPlayerManager.getAnimationDuration();

      final boolean isRunAnimation = _isAnimateLocation && animationDuration > 0;

      if (isRunAnimation == false) {

         /*
          * No animation
          */

         map.setMapPosition(mapPosition);

         return;
      }

      /*
       * Run animation
       */

      final long timeDiffLastRun = System.currentTimeMillis() - _lastAnimationTime;

      if (timeDiffLastRun > animationDuration / 2) {

         // next drawing is overdue

         setMapLocation_StartAnimation(map, mapPosition, 0);

      } else {

         /*
          * Schedule next drawing
          */

         final Runnable runnable = new Runnable() {

            final int __asynchRunnableCounter = _asyncCounter.incrementAndGet();

            @Override
            public void run() {

               // check if a newer runnable is available
               if (__asynchRunnableCounter != _asyncCounter.get()) {

                  // a newer event is available
                  return;
               }

               map.post(() -> setMapLocation_StartAnimation(map, mapPosition, __asynchRunnableCounter));
            }
         };

         // schedule animation
//       final long nextScheduleMS = ModelPlayerManager.animationDuration - timeDiffLastRun;
         final long nextScheduleMS = animationDuration / 2;

         map.postDelayed(runnable, nextScheduleMS);
      }

   }

   private static void setMapLocation_StartAnimation(final Map map, final MapPosition mapPosition, final int runnableCounter) {

      map.animator().animateTo(
            ModelPlayerManager.getAnimationDuration(),
            mapPosition,
            _animationEasingType);

      // updateMap() is very important otherwise the animation is not working
      map.updateMap(true);

      _lastAnimationTime = System.currentTimeMillis();
   }
}
