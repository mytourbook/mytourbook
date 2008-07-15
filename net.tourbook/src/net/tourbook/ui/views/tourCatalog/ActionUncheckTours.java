package net.tourbook.ui.views.tourCatalog;

import net.tourbook.Messages;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.StructuredSelection;

final class ActionUncheckTours extends Action {

	private final CompareResultView	fCompareResultView;

	ActionUncheckTours(final CompareResultView compareResultView) {
		super(Messages.Compare_Result_Action_uncheck_selected_tours);
		fCompareResultView = compareResultView;
	}

	@Override
	public void run() {

		// uncheck all selected tours

		final CheckboxTreeViewer viewer = fCompareResultView.getViewer();
		final StructuredSelection selection = (StructuredSelection) viewer.getSelection();
		if (selection.size() > 0) {

			for (final Object tour : selection.toArray()) {
				viewer.setChecked(tour, false);
			}
		}
	}
}
