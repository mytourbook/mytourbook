package net.tourbook.application;

import net.tourbook.Messages;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.tour.filter.SlideoutTourFilter;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.ToolBar;

public class ActionTourFilter extends ActionToolbarSlideout {

// SET_FORMATTING_OFF

	private static final ImageDescriptor	actionImageDescriptor			= TourbookPlugin.getImageDescriptor(Messages.Image__TourFilter);
	private static final ImageDescriptor	actionImageDisabledDescriptor	= TourbookPlugin.getImageDescriptor(Messages.Image__TourFilter_Disabled);

// SET_FORMATTING_ON

	public ActionTourFilter() {

		super(actionImageDescriptor, actionImageDisabledDescriptor);

		isToggleAction = true;
		notSelectedTooltip = Messages.Tour_Filter_Action_Tooltip;
	}

	@Override
	protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

		return new SlideoutTourFilter(toolbar, toolbar);
	}

}
