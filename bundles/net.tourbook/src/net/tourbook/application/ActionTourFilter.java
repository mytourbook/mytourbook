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
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.tour.filter.SlideoutTourFilter;
import net.tourbook.tour.filter.TourFilterManager;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.ToolBar;

public class ActionTourFilter extends ActionToolbarSlideout {

// SET_FORMATTING_OFF

	private static final ImageDescriptor	actionImageDescriptor			= TourbookPlugin.getImageDescriptor(Messages.Image__TourFilter);
	private static final ImageDescriptor	actionImageDisabledDescriptor	= TourbookPlugin.getImageDescriptor(Messages.Image__TourFilter_Disabled);

// SET_FORMATTING_ON

	public ActionTourFilter() {

		super(actionImageDescriptor, actionImageDisabledDescriptor);

		// key short cut is not working, maybe it must be implemented another way
//		setId("command.net.tourbook.tour.filter");

		isToggleAction = true;
		notSelectedTooltip = Messages.Tour_Filter_Action_Tooltip;
	}

	@Override
	protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

		return new SlideoutTourFilter(toolbar, toolbar);
	}

	@Override
	protected void onSelect() {

		super.onSelect();

		// update tour filter
		TourFilterManager.setSelection(getSelection());
	}

}
