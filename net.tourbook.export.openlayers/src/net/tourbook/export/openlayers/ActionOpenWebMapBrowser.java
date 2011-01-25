package net.tourbook.export.openlayers;

import net.tourbook.mapping.TourMapView;
import net.tourbook.util.StatusUtil;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class ActionOpenWebMapBrowser implements IViewActionDelegate {

	private OpenLayersExportPlugin _openLayersExportPlugin = null;

	@Override
	public void run(IAction action) {
		if (_openLayersExportPlugin != null) {
			_openLayersExportPlugin.actionOpenWebMapBrowser();
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// do nothing
	}

	@Override
	public void init(IViewPart view) {

		if (view instanceof TourMapView) {
			_openLayersExportPlugin = new OpenLayersExportPlugin((TourMapView)view);
		} else {
			StatusUtil.log("unknown view '" + view + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}
