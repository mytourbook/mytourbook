/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

public class ActionZoomIn extends Action {

   private Chart _chart;

   public ActionZoomIn(final Chart chart) {

      _chart = chart;

      setText(Messages.Action_zoom_in);
      setToolTipText(Messages.Action_zoom_in_tooltip);

      setImageDescriptor(ChartActivator.getThemedImageDescriptor(ChartImages.Zoom_In));
      setDisabledImageDescriptor(ChartActivator.getThemedImageDescriptor(ChartImages.Zoom_In_Disabled));
   }

   @Override
   public void run() {
      _chart.onExecuteZoomIn(1.0);
   }

}
