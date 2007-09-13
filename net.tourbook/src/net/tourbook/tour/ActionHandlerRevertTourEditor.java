package net.tourbook.tour;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;

public class ActionHandlerRevertTourEditor extends AbstractHandler {

	private boolean		fisEnabled	= false;
	private TourEditor	fTourEditor;

	public ActionHandlerRevertTourEditor(TourEditor tourEditor) {
		fTourEditor = tourEditor;
	}

	@Override
	public Object execute(ExecutionEvent execEvent) throws ExecutionException {

		fTourEditor.revertTourData();

		return null;
	}

	/**
	 * Update the UI enablement state for the action handler
	 */
	public void fireHandlerChanged() {
		fireHandlerChanged(new HandlerEvent(this, true, false));
	}

	@Override
	public boolean isEnabled() {
		return fisEnabled;
	}

	public void setEnabled(boolean isEnabled) {
		fisEnabled = isEnabled;
	}

}
