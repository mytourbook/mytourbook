package net.tourbook.tour;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class HandlerGraphLayoutSpeed extends AbstractHandler {

	public Object execute(ExecutionEvent arg0) throws ExecutionException {
		return null;
	}

	public boolean isEnabled() {
		System.out.println("Speed:\tisEnabled()");
		return super.isEnabled();
	}

	public boolean isHandled() {
		System.out.println("Speed:\tisHandled()");
		return super.isHandled();
	}
}
