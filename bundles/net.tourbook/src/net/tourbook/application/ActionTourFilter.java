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
package net.tourbook.application;

import net.tourbook.Messages;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.tour.filter.ActionToolbarSlideoutAdv;
import net.tourbook.tour.filter.SlideoutTourFilter;
import net.tourbook.tour.filter.TourFilterManager;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.ToolItem;

public class ActionTourFilter extends ActionToolbarSlideoutAdv {

// SET_FORMATTING_OFF

	private static final ImageDescriptor	_actionImageDescriptor			= TourbookPlugin.getImageDescriptor(Messages.Image__TourFilter);
	private static final ImageDescriptor	_actionImageDisabledDescriptor	= TourbookPlugin.getImageDescriptor(Messages.Image__TourFilter_Disabled);

	private static final IDialogSettings	_state	= TourbookPlugin.getState("TourFilter");//$NON-NLS-1$
	
	private SlideoutTourFilter _slideoutTourFilter;
	
// SET_FORMATTING_ON

	public ActionTourFilter() {

		super(_actionImageDescriptor, _actionImageDisabledDescriptor);

		isToggleAction = true;
		notSelectedTooltip = Messages.Tour_Filter_Action_Tooltip;
	}

	@Override
	protected AdvancedSlideout createSlideout(final ToolItem toolItem) {

		_slideoutTourFilter = new SlideoutTourFilter(toolItem, _state);

		_slideoutTourFilter.setVerticalPosition(true);

		return _slideoutTourFilter;
	}

	@Override
	protected void onSelect() {

		super.onSelect();

		// update tour filter
		TourFilterManager.setFilterEnabled(getSelection());
	}

}
