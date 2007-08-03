package net.tourbook.application;

import net.tourbook.ui.views.TourChartView;
import net.tourbook.ui.views.TourStatisticsView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPlaceholderFolderLayout;

public class PerspectiveFactoryStatistic implements IPerspectiveFactory {

//	static final String	PERSPECTIVE_ID	= "net.tourbook.perspectiveStatistic";	//$NON-NLS-1$

	public void createInitialLayout(IPageLayout layout) {

		// hide editor area
		layout.setEditorAreaVisible(false);

		IFolderLayout topFolder = layout.createFolder("top",
				IPageLayout.TOP,
				0.5f,
				IPageLayout.ID_EDITOR_AREA);

		topFolder.addView(TourStatisticsView.ID);

		IPlaceholderFolderLayout rightFolder = layout.createPlaceholderFolder("right",
				IPageLayout.RIGHT,
				0.5f,
				IPageLayout.ID_EDITOR_AREA);

		rightFolder.addPlaceholder(TourChartView.ID);

	}
}
