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
package net.tourbook.tour.filter;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class Action_AddProfile extends Action {

	private SlideoutTourFilter _slideoutTourFilter;

	public Action_AddProfile(final SlideoutTourFilter slideoutTourFilter) {

		super(null, AS_PUSH_BUTTON);

		_slideoutTourFilter = slideoutTourFilter;

		setToolTipText(Messages.Slideout_TourFilter_Action_AddProfile_Tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Add));
	}

	@Override
	public void run() {
		_slideoutTourFilter.action_ProfileAdd();
	}

}
