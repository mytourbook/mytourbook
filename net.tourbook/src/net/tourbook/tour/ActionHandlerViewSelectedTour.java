package net.tourbook.tour;

import net.tourbook.ui.views.TourChartView;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class ActionHandlerViewSelectedTour extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		try {
			PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow()
					.getActivePage()
					.showView(TourChartView.ID, null, IWorkbenchPage.VIEW_VISIBLE);

		} catch (PartInitException e) {
			e.printStackTrace();
		}
		return null;
	}

}
