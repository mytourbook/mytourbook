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
package net.tourbook.ui.views.tourBook;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.action.Action;

class ActionToggleMonthWeek extends Action {

	private TourBookView	_tourBookView;

	/**
	 * @param tourBookView
	 */
	public ActionToggleMonthWeek(final TourBookView tourBookView) {

		super(null, AS_PUSH_BUTTON);

		setToolTipText(Messages.Tour_Book_Action_ToggleMonthWeek_Tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TourBook_Month));

		_tourBookView = tourBookView;
	}

	@Override
	public void run() {
		_tourBookView.actionToggleMonthWeek();
	}
}
