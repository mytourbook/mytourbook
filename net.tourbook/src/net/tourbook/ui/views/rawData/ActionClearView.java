package net.tourbook.ui.views.rawData;

import net.tourbook.importdata.RawDataManager;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionClearView extends Action {

	private RawDataView	fRawDataView;

	public ActionClearView(RawDataView rawDataView) {

		fRawDataView = rawDataView;

		setText("Clear View");
		setToolTipText("Remove all tours from this view, saved tours will not be touched");
		
		setImageDescriptor(TourbookPlugin.getImageDescriptor("remove_all.gif"));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor("remove_all_disabled.gif"));
	}

	public void run() {

		// remove all tours
		RawDataManager.getInstance().getTourData().clear();

		fRawDataView.updateViewer();
	}

}
