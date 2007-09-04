package net.tourbook.importdata;

import net.tourbook.Messages;
import net.tourbook.application.PerspectiveFactoryRawData;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;

public class ActionHandlerImportFromDeviceDirect extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		final WizardImportData importWizard = new WizardImportData();

		final WizardDialog dialog = new WizardImportDialog(HandlerUtil.getActiveShellChecked(event),
				importWizard,
				Messages.ImportWizard_Dlg_title);

		// create the dialog and shell which is required in setAutoDownload()
		dialog.create();

		importWizard.setAutoDownload();

		dialog.open();

		// show raw data perspective
		try {
			PlatformUI.getWorkbench().showPerspective(PerspectiveFactoryRawData.PERSPECTIVE_ID,
					HandlerUtil.getActiveWorkbenchWindow(event));
		} catch (WorkbenchException e) {
			e.printStackTrace();
		}

		return null;
	}

}
