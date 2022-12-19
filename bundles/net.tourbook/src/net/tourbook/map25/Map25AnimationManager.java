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

import java.util.concurrent.atomic.AtomicInteger;

import org.oscim.core.BoundingBox;
import org.oscim.core.MapPosition;
import org.oscim.map.Animator;
import org.oscim.map.Map;
import org.oscim.utils.animation.Easing;

public class Map25AnimationManager {

   private static final AtomicInteger _asyncCounter        = new AtomicInteger();
   private static int                 _animationDuration   = 500;
   private static Easing.Type         _animationEasingType = Easing.Type.LINEAR;
   private static boolean             _isAnimateLocation   = true;
   private static long                _lastAnimationTime;

   /**
    * Set map location with or without animation
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
      _animationDuration = 2000;
      _animationEasingType = Easing.Type.SINE_INOUT;

      map.post(() -> setMapLocation_InMapThread(map, mapPosition));
   }

   private static void setMapLocation_InMapThread(final Map map, final MapPosition mapPosition) {

      final boolean isRunAnimation = _isAnimateLocation && _animationDuration > 0;

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

      final long timeDiff = System.currentTimeMillis() - _lastAnimationTime;

      if (timeDiff > 400) {

         // next drawing is overdue

//         System.out.println((System.currentTimeMillis() + " overdue"));
//         // TODO remove SYSTEM.OUT.PRINTLN

         setMapLocation_StartAnimation(map, mapPosition);

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

//               System.out.println((System.currentTimeMillis() + " scheduled"));
//               // TODO remove SYSTEM.OUT.PRINTLN

               map.post(() -> setMapLocation_StartAnimation(map, mapPosition));
            }
         };

         // schedule animation
         final long nextScheduleMS = _animationDuration - timeDiff;

//         System.out.println((System.currentTimeMillis() + " nextScheduleMS: " + nextScheduleMS));
//         // TODO remove SYSTEM.OUT.PRINTLN

         map.postDelayed(runnable, 200);
      }

   }

   private static void setMapLocation_StartAnimation(final Map map, final MapPosition mapPosition) {

      map.animator().animateTo(
            _animationDuration,
            mapPosition,
            _animationEasingType);

      // updateMap() is very important otherwise the animation is not working
      map.updateMap(true);

      _lastAnimationTime = System.currentTimeMillis();
   }
}
