/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart.action;

import org.eclipse.jface.action.Action;

import net.tourbook.ui.tourChart.TourChart;

public class Action_GraphBackground extends Action {

	private TourChart	_tourChart;

	private String		_commandId;
	private boolean	_isSource;

	public Action_GraphBackground(final TourChart tourChart, final String commandId, final String label, final boolean isSource) {

		super(label, AS_RADIO_BUTTON);

		_tourChart = tourChart;
		_commandId = commandId;
		_isSource = isSource;
	}

	@Override
	public void run() {
		_tourChart.action_GraphBackground(isChecked(), _commandId, _isSource);
	}

}
