package net.tourbook.ui.views.rawData;

import net.tourbook.Messages;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionClearView extends Action {

	private RawDataView	fRawDataView;

	public ActionClearView(RawDataView rawDataView) {

		fRawDataView = rawDataView;

		setText(Messages.RawData_Action_clear_view);
		setToolTipText(Messages.RawData_Action_clear_view_tooltip);
		
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_remove_all));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_remove_all_disabled));
	}

	public void run() {

		// remove all tours
		RawDataManager.getInstance().removeAllTours();

		fRawDataView.updateViewer();
	}

}
