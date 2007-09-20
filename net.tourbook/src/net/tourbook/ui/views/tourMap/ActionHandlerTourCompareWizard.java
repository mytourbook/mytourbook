package net.tourbook.ui.views.tourMap;

import net.tourbook.application.PerspectiveFactoryCompareTours;
import net.tourbook.util.PositionedWizardDialog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;

public class ActionHandlerTourCompareWizard extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		final IWorkbench workbench = PlatformUI.getWorkbench();
		final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

		try {
			// show tour compare perspective
			workbench.showPerspective(PerspectiveFactoryCompareTours.PERSPECTIVE_ID, window);

			// show tour compare view
			window.getActivePage().showView(CompareResultView.ID,
					null,
					IWorkbenchPage.VIEW_ACTIVATE);

		} catch (WorkbenchException e) {
			e.printStackTrace();
		}

		Wizard wizard = new WizardTourComparer();

		final WizardDialog dialog = new PositionedWizardDialog(HandlerUtil.getActiveShellChecked(event),
				wizard,
				WizardTourComparer.DIALOG_SETTINGS_SECTION,
				800,
				600);

		BusyIndicator.showWhile(null, new Runnable() {
			public void run() {
				dialog.open();
			}
		});

		return null;
	}

}
