package net.tourbook.application;

import net.tourbook.ui.views.TourChartView;
import net.tourbook.ui.views.TourMarkerView;
import net.tourbook.ui.views.tourBook.TourBookView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactoryTourBook implements IPerspectiveFactory {

	static final String			PERSPECTIVE_ID			= "net.tourbook.perspective.TourBook";	//$NON-NLS-1$

	private static final String	FOLDER_ID_CHART			= "chart";
	private static final String	FOLDER_ID_STATISTICS	= "stat";
	private static final String	FOLDER_ID_LIST			= "list";

	public void createInitialLayout(IPageLayout layout) {

		//--------------------------------------------------------------------------------

		IFolderLayout listFolder = layout.createFolder(FOLDER_ID_LIST,
				IPageLayout.LEFT,
				0.4f,
				IPageLayout.ID_EDITOR_AREA);

		listFolder.addView(TourBookView.ID);

		//--------------------------------------------------------------------------------

		IFolderLayout markerFolder = layout.createFolder(FOLDER_ID_STATISTICS,
				IPageLayout.BOTTOM,
				0.7f,
				FOLDER_ID_LIST);

		markerFolder.addView(TourMarkerView.ID);

		//--------------------------------------------------------------------------------

		IFolderLayout chartFolder = layout.createFolder(FOLDER_ID_CHART,
				IPageLayout.TOP,
				0.5f,
				IPageLayout.ID_EDITOR_AREA);

		chartFolder.addView(TourChartView.ID);
	}

}
