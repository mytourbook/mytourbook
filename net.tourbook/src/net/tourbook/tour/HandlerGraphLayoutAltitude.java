package net.tourbook.tour;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class HandlerGraphLayoutAltitude extends AbstractHandler {

	public Object execute(ExecutionEvent arg0) throws ExecutionException {
		return null;
	}

	public boolean isEnabled() {
		System.out.println("Altitude:\tisEnabled()");
		return super.isEnabled();
	}

	public boolean isHandled() {
		System.out.println("Altitude:\tisHandled()");
		return super.isHandled();
	}
}
