/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.statistic;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;

public class ActionSynchChartScale extends Action {

	private TourStatisticsView	_statView;

	public ActionSynchChartScale(final TourStatisticsView statView) {

		super(UI.EMPTY_STRING, AS_CHECK_BOX);

		_statView = statView;

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__synch_statistics));
		setToolTipText(Messages.tourCatalog_view_action_synch_chart_years_tooltip);
	}

	@Override
	public void run() {
		_statView.actionSynchScale(isChecked());
	}
}
