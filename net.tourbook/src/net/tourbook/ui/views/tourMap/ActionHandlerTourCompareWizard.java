package net.tourbook.ui.views.tourMap;

import net.tourbook.util.PositionedWizardDialog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.handlers.HandlerUtil;

public class ActionHandlerTourCompareWizard extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

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
