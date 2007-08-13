package net.tourbook.tour;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.IMenuService;

public class ActionHandlerChartOptions extends TCActionHandler {

	public ActionHandlerChartOptions() {
		fCommandId = TourChart.COMMAND_ID_CHART_OPTIONS;
	}

	@Override
	public Object execute(ExecutionEvent execEvent) throws ExecutionException {

		// show the drop-down menu, this only works in the runWithEvent not in the run method
//		getMenuCreator().getMenu(fTBM.getControl()).setVisible(true);

		IMenuService menuService = (IMenuService) PlatformUI.getWorkbench()
				.getService(IMenuService.class);
		
//		menuService.addContributionFactory(factory)
		
		return null;
	}

}
