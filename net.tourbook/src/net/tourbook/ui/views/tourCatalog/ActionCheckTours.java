package net.tourbook.ui.views.tourCatalog;

import net.tourbook.Messages;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.StructuredSelection;

final class ActionCheckTours extends Action {

	private final CompareResultView	fCompareResultView;

	ActionCheckTours(final CompareResultView compareResultView) {
		super(Messages.Compare_Result_Action_check_selected_tours);
		fCompareResultView = compareResultView;
	}

	@Override
	public void run() {

		// check all selected compared tours which are not yet stored

		final CheckboxTreeViewer viewer = fCompareResultView.getViewer();
		final StructuredSelection selection = (StructuredSelection) viewer.getSelection();
		if (selection.size() > 0) {

			for (final Object tour : selection.toArray()) {

				if (tour instanceof CompareResultItemComparedTour) {
					final CompareResultItemComparedTour comparedTour = (CompareResultItemComparedTour) tour;
					if (comparedTour.compId == -1) {
						viewer.setChecked(tour, true);
					}
				}
			}
		}
	}
}
