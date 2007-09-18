package net.tourbook.application;

import net.tourbook.ui.views.tourMap.CompareResultView;
import net.tourbook.ui.views.tourMap.TourMapComparedTourView;
import net.tourbook.ui.views.tourMap.TourMapReferenceTourView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactoryCompareTours implements IPerspectiveFactory {

	static final String			PERSPECTIVE_ID	= "net.tourbook.perspective.CompareTours";	//$NON-NLS-1$

	private static final String	FOLDER_ID_LIST	= "list";
	private static final String	FOLDER_ID_REF	= "ref";

	public void createInitialLayout(IPageLayout layout) {

		//--------------------------------------------------------------------------------

		IFolderLayout listFolder = layout.createFolder(FOLDER_ID_LIST,
				IPageLayout.LEFT,
				0.4f,
				IPageLayout.ID_EDITOR_AREA);

		listFolder.addView(CompareResultView.ID);

		//--------------------------------------------------------------------------------

		IFolderLayout refFolder = layout.createFolder(FOLDER_ID_REF,
				IPageLayout.TOP,
				0.7f,
				IPageLayout.ID_EDITOR_AREA);

		refFolder.addView(TourMapReferenceTourView.ID);

		//--------------------------------------------------------------------------------

		IFolderLayout compFolder = layout.createFolder("comp",
				IPageLayout.BOTTOM,
				0.5f,
				FOLDER_ID_REF);

		compFolder.addView(TourMapComparedTourView.ID);

		layout.setEditorAreaVisible(false);
	}

}
