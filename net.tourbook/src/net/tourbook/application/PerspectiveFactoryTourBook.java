package net.tourbook.application;

import net.tourbook.ui.views.TourChartView;
import net.tourbook.ui.views.tourBook.TourBookView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactoryTourBook implements IPerspectiveFactory {

	static final String	PERSPECTIVE_ID	= "net.tourbook.perspective.TourBook";	//$NON-NLS-1$

	public void createInitialLayout(IPageLayout layout) {

		IFolderLayout leftFolder = layout.createFolder("left",
				IPageLayout.LEFT,
				0.4f,
				IPageLayout.ID_EDITOR_AREA);

		leftFolder.addView(TourBookView.ID);

		IFolderLayout bottomFolder = layout.createFolder("bottom",
				IPageLayout.BOTTOM,
				0.5f,
				IPageLayout.ID_EDITOR_AREA);

		bottomFolder.addView(TourChartView.ID);

//		layout.setEditorAreaVisible(false);
	}

}
