/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook;

import net.tourbook.Messages;

import org.eclipse.jface.action.Action;

public class ActionSelectAllTours extends Action {

	private TourBookView	_tourViewer;

	public ActionSelectAllTours(final TourBookView tourViewer) {

		super(null, AS_CHECK_BOX);

		_tourViewer = tourViewer;

		setText(Messages.action_tourbook_select_year_month_tours);
	}

	@Override
	public void run() {
		_tourViewer.actionSelectYearMonthTours();
	}
}
