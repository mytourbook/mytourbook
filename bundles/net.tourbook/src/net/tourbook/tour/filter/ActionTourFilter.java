package net.tourbook.tour.filter;

import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;

import org.eclipse.swt.widgets.ToolBar;

public class ActionTourFilter extends ActionToolbarSlideout {

//	private static ImageDescriptor	actionImage			= TourbookPlugin.getImageDescriptor(Messages.Image__TourFilter);
//	private static ImageDescriptor	actionImageDisabled	= TourbookPlugin
//			.getImageDescriptor(Messages.Image__TourFilter_Disabled);

	public ActionTourFilter() {
//		super(actionImage, actionImageDisabled);
	}

	@Override
	protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

		return new SlideoutTourFilter(toolbar, toolbar);
	}

}
