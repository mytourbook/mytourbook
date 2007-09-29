package net.tourbook.application;

import net.tourbook.ui.views.tourMap.TourMapViewComparedTour;
import net.tourbook.ui.views.tourMap.TourMapViewReferenceTour;
import net.tourbook.ui.views.tourMap.TourMapView;
import net.tourbook.ui.views.tourMap.TourMapViewYearStatistic;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactoryTourMap implements IPerspectiveFactory {

	static final String			PERSPECTIVE_ID	= "net.tourbook.perspective.TourMap";	//$NON-NLS-1$

	private static final String	FOLDER_ID_COMP	= "comp"; //$NON-NLS-1$
	private static final String	FOLDER_ID_STAT	= "stat"; //$NON-NLS-1$
	private static final String	FOLDER_ID_LIST	= "list"; //$NON-NLS-1$
	private static final String	FOLDER_ID_REF	= "ref"; //$NON-NLS-1$

	public void createInitialLayout(IPageLayout layout) {

		//--------------------------------------------------------------------------------

		IFolderLayout listFolder = layout.createFolder(FOLDER_ID_LIST,
				IPageLayout.LEFT,
				0.3f,
				IPageLayout.ID_EDITOR_AREA);

		listFolder.addView(TourMapView.ID);

		//--------------------------------------------------------------------------------

		IFolderLayout statFolder = layout.createFolder(FOLDER_ID_STAT,
				IPageLayout.BOTTOM,
				0.7f,
				FOLDER_ID_LIST);

		statFolder.addView(TourMapViewYearStatistic.ID);

		//--------------------------------------------------------------------------------

		IFolderLayout refFolder = layout.createFolder(FOLDER_ID_REF,
				IPageLayout.TOP,
				0.7f,
				IPageLayout.ID_EDITOR_AREA);

		refFolder.addView(TourMapViewReferenceTour.ID);

		//--------------------------------------------------------------------------------

		IFolderLayout compFolder = layout.createFolder(FOLDER_ID_COMP,
				IPageLayout.BOTTOM,
				0.5f,
				FOLDER_ID_REF);

		compFolder.addView(TourMapViewComparedTour.ID);

		layout.setEditorAreaVisible(false);
	}

}
