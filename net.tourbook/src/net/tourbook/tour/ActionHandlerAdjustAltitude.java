package net.tourbook.tour;

import net.tourbook.database.TourDatabase;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class ActionHandlerAdjustAltitude extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		// open the dialog to adjust the altitude

		IEditorPart editorPart = HandlerUtil.getActiveEditorChecked(event);

		TourEditor tourEditor;
		TourChart tourChart;

		if (editorPart instanceof TourEditor) {
			tourEditor = ((TourEditor) editorPart);
			tourChart = tourEditor.getTourChart();
		} else {
			return null;
		}

		AdjustAltitudeDialog dialog;

		if (tourChart != null) {
			dialog = new AdjustAltitudeDialog(tourChart.getShell(), tourChart);
		} else {
			return null;
		}

		dialog.create();

		/*
		 * initialize the dialog
		 */
		dialog.init();

		if (dialog.open() == Window.OK) {
			tourEditor.setTourDirty();
		} else {
			dialog.restoreOriginalAltitudeValues();
		}
		tourChart.updateChart(true);

		/*
		 * when in the tour chart view this tour is visible, another tour is selected and then again
		 * this tour, the displayed data are from the cache which where changed from this dialog,
		 * but the tour chart view should show the tour from the database
		 */
		TourManager.getInstance().removeTourFromCache(tourChart.fTourData.getTourId());

		TourDatabase.getInstance().firePropertyChange(TourDatabase.PROPERTY_TOUR_IS_CHANGED);

		return null;
	}
}
