package net.tourbook.application;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.handlers.HandlerUtil;

public class ActionHandlerPreferences extends AbstractHandler {

	private IWorkbenchAction	fPrefAction;

	public ActionHandlerPreferences() {

	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		if (fPrefAction == null) {
			fPrefAction = ActionFactory.PREFERENCES.create(HandlerUtil.getActiveWorkbenchWindow(event));
		}

		fPrefAction.run();

		return null;
	}

}
