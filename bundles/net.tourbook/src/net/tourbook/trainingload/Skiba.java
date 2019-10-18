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
 * Class that calculates the GOVSS (Gravity Ordered Velocity Stress Score) for a given run and
 * athlete.
 * The GOVSS concept was found by Dr Skiba (www.physfarm.com)
 * Official research paper : http://runscribe.com/wp-content/uploads/power/GOVSS.pdf
 * Note : This function will assume that the tour is a run activity. If not, be aware that the GOVSS
 * value will not be accurate.
 */
public class Skiba {

   public static int ComputeGovss(final TourPerson tourPerson, final TourData tourData) {
      //What data from the athlete do we need ?
      //When identified, add them in the users preference page

      //Use the equations to compute the GOVSS

      //Should that trigger a recompute of the Performance chart data ?
      return -1;
   }

   //TODO : Add the GOVSS column in the tour book view
}
