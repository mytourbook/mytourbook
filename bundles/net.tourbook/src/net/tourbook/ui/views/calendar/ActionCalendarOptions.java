/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.calendar;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.tour.filter.ActionToolbarSlideoutAdv;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.ToolItem;

public class ActionCalendarOptions extends ActionToolbarSlideoutAdv {

	private static final IDialogSettings	_state	= TourbookPlugin.getState("SlideoutCalendarOptions");	//$NON-NLS-1$

	private CalendarView					_calendarView;
	private SlideoutCalendarOptions			_slideoutCalendarOptions;

	public ActionCalendarOptions(final CalendarView calendarView) {

		super();

		_calendarView = calendarView;
	}

	@Override
	protected AdvancedSlideout createSlideout(final ToolItem toolItem) {

		_slideoutCalendarOptions = new SlideoutCalendarOptions(toolItem, _state, _calendarView);

		return _slideoutCalendarOptions;
	}

	SlideoutCalendarOptions getSlideout() {
		return _slideoutCalendarOptions;
	}

	@Override
	protected void onBeforeOpenSlideout() {

		// ensure other dialogs are closed
		_calendarView.closeOpenedDialogs(this);
	}

}
