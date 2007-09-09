package net.tourbook.application;

import net.tourbook.ui.views.tourMap.TourMapComparedTourView;
import net.tourbook.ui.views.tourMap.TourMapReferenceTourView;
import net.tourbook.ui.views.tourMap.TourMapView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactoryTourMap implements IPerspectiveFactory {

	static final String			PERSPECTIVE_ID	= "net.tourbook.perspective.TourMap";	//$NON-NLS-1$

	private static final String	REF_FOLDER_ID	= "ref";

	public void createInitialLayout(IPageLayout layout) {
		IFolderLayout leftFolder = layout.createFolder("left",
				IPageLayout.LEFT,
				0.3f,
				IPageLayout.ID_EDITOR_AREA);

		leftFolder.addView(TourMapView.ID);

		IFolderLayout refFolder = layout.createFolder(REF_FOLDER_ID,
				IPageLayout.TOP,
				0.7f,
				IPageLayout.ID_EDITOR_AREA);

		refFolder.addView(TourMapReferenceTourView.ID);

		IFolderLayout compFolder = layout.createFolder("comp",
				IPageLayout.BOTTOM,
				0.5f,
				REF_FOLDER_ID);

		compFolder.addView(TourMapComparedTourView.ID);
	}

}
