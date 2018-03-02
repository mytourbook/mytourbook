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
package net.tourbook.application;

import net.tourbook.Messages;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.tooltip.SlideoutLocation;
import net.tourbook.tag.tour.filter.SlideoutTest;
import net.tourbook.tag.tour.filter.TourTagFilterManager;
import net.tourbook.tour.filter.ActionToolbarSlideoutAdv;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.ToolItem;

public class ActionTourTagFilter extends ActionToolbarSlideoutAdv {

// SET_FORMATTING_OFF

	private static final ImageDescriptor	_actionImageDescriptor			= TourbookPlugin.getImageDescriptor(Messages.Image__TourTagFilter);
	private static final ImageDescriptor	_actionImageDisabledDescriptor	= TourbookPlugin.getImageDescriptor(Messages.Image__TourTagFilter_Disabled);

	private static final IDialogSettings	_state	= TourbookPlugin.getState("TourTagFilterSlideout");//$NON-NLS-1$
	
// SET_FORMATTING_ON

	private AdvancedSlideout				_slideoutTourTagFilter;

	public ActionTourTagFilter() {

		super(_actionImageDescriptor, _actionImageDisabledDescriptor);

		isToggleAction = true;
		notSelectedTooltip = Messages.Tour_Tag_Filter_Action_Tooltip;
	}

	@Override
	protected AdvancedSlideout createSlideout(final ToolItem toolItem) {

		_slideoutTourTagFilter = new SlideoutTest(toolItem, _state);

//		_slideoutTourTagFilter = new SlideoutTourTagFilter(toolItem, _state);
		_slideoutTourTagFilter.setSlideoutLocation(SlideoutLocation.ABOVE_CENTER);

		return _slideoutTourTagFilter;
	}

	@Override
	protected void onSelect() {

		super.onSelect();

		// update tour tag filter
		TourTagFilterManager.setFilterEnabled(getSelection());
	}

}
