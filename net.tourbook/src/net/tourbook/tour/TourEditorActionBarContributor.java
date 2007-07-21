package net.tourbook.tour;

import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.EditorActionBarContributor;

public class TourEditorActionBarContributor extends EditorActionBarContributor {

	public TourEditorActionBarContributor() {
	}

	public void contributeToCoolBar(ICoolBarManager coolBarManager) {
		
		IEditorPart activeEditor = getPage().getActiveEditor();
		
		if (activeEditor == null) {
			return;
		}
	}

	public void contributeToMenu(IMenuManager menuManager) {
	}

	
}
