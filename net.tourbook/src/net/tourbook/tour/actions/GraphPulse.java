package net.tourbook.tour.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

public class GraphPulse implements IEditorActionDelegate {

	private IEditorPart	fEditor;

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		fEditor = targetEditor;
	}

	public void run(IAction action) {
		int a = 0;
	}

	public void selectionChanged(IAction action, ISelection selection) {

	}

}
