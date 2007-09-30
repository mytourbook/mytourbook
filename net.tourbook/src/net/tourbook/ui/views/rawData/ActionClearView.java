package net.tourbook.ui.views.rawData;

import net.tourbook.Messages;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionClearView extends Action {

	private RawDataView	fRawDataView;

	public ActionClearView(RawDataView rawDataView) {

		fRawDataView = rawDataView;

		setText(Messages.Raw_Data_Action_clear_view);
		setToolTipText(Messages.Raw_Data_Action_clear_view_tooltip);
		
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__remove_all));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__remove_all_disabled));
	}

	public void run() {

		// remove all tours
		RawDataManager.getInstance().removeAllTours();

		fRawDataView.updateViewer();
	}

}
