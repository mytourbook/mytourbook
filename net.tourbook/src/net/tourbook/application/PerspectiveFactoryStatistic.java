package net.tourbook.application;

import net.tourbook.Messages;
import net.tourbook.ui.views.TourChartView;
import net.tourbook.ui.views.TourStatisticsView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactoryStatistic implements IPerspectiveFactory {

	static final String			PERSPECTIVE_ID		= "net.tourbook.perspective.Statistic"; //$NON-NLS-1$

	private static final String	FOLDER_ID_BOTTOM	= "bottom"; //$NON-NLS-1$
	private static final String	FOLDER_ID_TOP		= "top"; //$NON-NLS-1$

	public void createInitialLayout(IPageLayout layout) {

		IFolderLayout topFolder = layout.createFolder(FOLDER_ID_TOP,
				IPageLayout.TOP,
				0.5f,
				IPageLayout.ID_EDITOR_AREA);

		topFolder.addView(TourStatisticsView.ID);

		IFolderLayout bottomFolder = layout.createFolder(FOLDER_ID_BOTTOM,
				IPageLayout.LEFT,
				0.5f,
				IPageLayout.ID_EDITOR_AREA);

		bottomFolder.addView(TourChartView.ID);

//		IPlaceholderFolderLayout rightFolder = layout.createPlaceholderFolder("right",
//				IPageLayout.RIGHT,
//				0.5f,
//				IPageLayout.ID_EDITOR_AREA);
//
//		rightFolder.addPlaceholder(TourChartView.ID);

		// hide editor area
		layout.setEditorAreaVisible(false);

	}
}
