package net.tourbook.application;

import net.tourbook.ui.views.TourChartView;
import net.tourbook.ui.views.tourMap.TourMapView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactoryTourMap implements IPerspectiveFactory {

	static final String	PERSPECTIVE_ID	= "net.tourbook.perspectiveTourMap"; //$NON-NLS-1$

	public void createInitialLayout(IPageLayout layout) {
		IFolderLayout leftFolder = layout.createFolder("left",
				IPageLayout.LEFT,
				0.4f,
				IPageLayout.ID_EDITOR_AREA);

		leftFolder.addView(TourMapView.ID);

		IFolderLayout topFolder = layout.createFolder("top",
				IPageLayout.TOP,
				0.5f,
				IPageLayout.ID_EDITOR_AREA);

		topFolder.addView(TourChartView.ID);

		layout.setEditorAreaVisible(false);

	}

}
