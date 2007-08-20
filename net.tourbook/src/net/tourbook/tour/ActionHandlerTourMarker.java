package net.tourbook.tour;

import net.tourbook.database.TourDatabase;
import net.tourbook.ui.views.tourBook.MarkerDialog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class ActionHandlerTourMarker extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IEditorPart editorPart = HandlerUtil.getActiveEditorChecked(event);

		TourEditor tourEditor;
		TourChart tourChart;

		if (editorPart instanceof TourEditor) {
			tourEditor = ((TourEditor) editorPart);
			tourChart = tourEditor.getTourChart();
		} else {
			return null;
		}

		(new MarkerDialog(Display.getCurrent().getActiveShell(), tourChart.fTourData, null)).open();

		/*
		 * Currently the dialog works with the markers from the tour editor not with a backup, so
		 * changes in the dialog are made in the tourdata of the tour editor -> the tour will be
		 * dirty when this dialog was opened
		 */

		// force the tour to be saved
		tourChart.setTourDirty(true);

		// update chart
		tourChart.updateMarkerLayer(true);

		// update marker list and other listener
		TourDatabase.getInstance().firePropertyChange(TourDatabase.PROPERTY_TOUR_IS_CHANGED);

		return null;
	}

}
