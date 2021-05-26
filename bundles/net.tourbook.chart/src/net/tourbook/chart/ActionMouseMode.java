/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

public class ActionMouseMode extends Action {

   private Chart _chart;

   public ActionMouseMode(final Chart chart) {

      super(Messages.Action_mouse_mode, Action.AS_CHECK_BOX);

      _chart = chart;

      setToolTipText(Messages.Action_mouse_mode_tooltip);

      setImageDescriptor(ChartActivator.getThemedImageDescriptor(ChartImages.MouseMode));
   }

   @Override
   public void run() {
      _chart.onExecuteMouseMode(isChecked());
   }

}
