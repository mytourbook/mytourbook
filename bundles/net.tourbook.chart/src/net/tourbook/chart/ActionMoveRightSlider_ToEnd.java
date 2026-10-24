/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.chart;

import org.eclipse.jface.action.Action;

public class ActionMoveRightSlider_ToEnd extends Action {

   private Chart _chart;

	public ActionMoveRightSlider_ToEnd(final Chart chart) {

      _chart = chart;

      setText(Messages.Action_Move_RightSlider_ToEnd);

      setImageDescriptor(ChartActivator.getThemedImageDescriptor(ChartImages.XSliderTo_End));
   }

	@Override
	public void run() {

      _chart.onExecuteMoveRightSlider_ToEnd();
	}
}
