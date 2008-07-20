package net.tourbook.ui.views.tourCatalog;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

class ActionSaveComparedTours extends Action {

	private final CompareResultView	fCompareResultView;

	ActionSaveComparedTours(final CompareResultView compareResultView) {

		fCompareResultView = compareResultView;

		setText(Messages.Compare_Result_Action_save_checked_tours);
		setToolTipText(Messages.Compare_Result_Action_save_checked_tours_tooltip);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__save));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__save_disabled));
	}

	@Override
	public void run() {
		fCompareResultView.saveCompareResults();
	}
}
