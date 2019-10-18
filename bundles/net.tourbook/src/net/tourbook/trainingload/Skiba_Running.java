/*******************************************************************************
 * Copyright (C) 2019  Frederic Bard
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
package net.tourbook.trainingload;

import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;

/**
 * Class that implements several of Dr Skiba's formulas that apply to running
 * More information can be found on www.physfarm.com
 */
public class Skiba_Running {

   //TODO : Add the GOVSS column in the tour book view
   //TODO Use this equation to display an estimated power graph in the tour chart ?If yes, it's low on the totem pole

   /**
    * Computes Caero (This is the energy cost of overcoming aerodynamic drag)
    *
    * @param speed
    * @return
    */
   private static double computeCostAerodynamicDrag(final double speed, final double athleteHeight, final double athleteWeight) {

      final double Af = 0.2025 * 0.266 * Math.pow(athleteHeight, 0.725) * Math.pow(athleteWeight, 0.425);
      final double CAero = 0.5 * 1.2 * 0.9 * Af * Math.pow(speed, 2);

      return CAero;
   }


   /**
    * Computes Ci (The energy cost to cover any given distance) with i being the slope of the
    * running surface (in ??? unit)
    *
    * @return
    */
   private static double computeCostDistanceWithSlope(final double slope) {

      final double Cslope = (155.4 * Math.pow(slope, 5)) - (30.4 * Math.pow(slope, 4)) - (43.3 * Math.pow(slope, 3)) + (46.3 * Math.pow(slope, 2))
            + (19.5 * slope) + 3.6;

      return Cslope;
   }

   /**
    * Computes Ckin (The energy cost of changes in velocity)
    *
    * @return
    */
   private static double computeCostKineticEnergy(final double distance, final double initialSpeed, final double speed) {

      final double Ckin = 0.5 * (Math.pow(speed, 2) - Math.pow(initialSpeed, 2)) / distance;

      return Ckin;
   }

   /**
    * Function that calculates the GOVSS (Gravity Ordered Velocity Stress Score) for a given run and
    * athlete.
    * References
    * http://runscribe.com/wp-content/uploads/power/GOVSS.pdf
    * https://3record.de/about/power_estimation#ref_4
    * Note : This function will assume that the tour is a run activity. If not, be aware that the
    * GOVSS value will not be accurate.
    *
    * @param tourPerson
    * @param tourData
    * @return
    */
   public static int ComputeGovss(final TourPerson tourPerson, final TourData tourData) {
      //What data from the athlete do we need ?
      //When identified, add them in the users preference page

      //Determine TP from preference page Thresholdpower

      //For each 120s interval
      //compute power values
      // raise each value to the 4thpower
      // avg(values above)

      // Below is the lactate normalized power
      //Take the 4th root of step 5 : Math.power(value above, 0.25)

      //Divide LNP by TP to get the Intensity Weighting Fraction.

      //Multiply LNP by duration of workout (recording time in seconds)

      //Multiply value above by IWF

      ///Divide value above by the amount of work performed during the 10k to 1 hr test (threshold power in watts x number of seconds).

      // 11. Multiply the number from step 10 by 100 to obtain the final training stress in GOVSS.

      //Should that trigger a recompute of the Performance chart data ?
      return -1;
   }

   /**
    * Function that calculates the running power for a given distance, time and athlete.
    *
    * @param athleteHeight
    * @param athleteWeight
    * @return
    */
   public static double ComputePower(final double athleteHeight, final double athleteWeight) {

      final double speed = 0.0;

      final double CAero = computeCostAerodynamicDrag(0.0, athleteHeight, athleteWeight);
      final double Ckin = computeCostKineticEnergy(0.0, 0.0, speed);
      final double Cslope = computeCostDistanceWithSlope(0.0);
      final double efficiency = (0.25 + (0.054 * speed)) * (1 - ((0.5 * speed) / 8.33));

      final double power = (CAero + Ckin + Cslope * efficiency * athleteWeight) * speed;

      return power;
   }
}
