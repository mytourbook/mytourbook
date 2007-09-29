package net.tourbook.application;

import net.tourbook.ui.views.TourChartView;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactoryRawData implements IPerspectiveFactory {

	public static final String	PERSPECTIVE_ID	= "net.tourbook.perspective.RawData";	//$NON-NLS-1$

	private static final String	FOLDER_ID_TOP	= "top"; //$NON-NLS-1$
	private static final String	FOLDER_ID_LEFT	= "left"; //$NON-NLS-1$

	public void createInitialLayout(IPageLayout layout) {

		IFolderLayout leftFolder = layout.createFolder(FOLDER_ID_LEFT,
				IPageLayout.LEFT,
				0.4f,
				IPageLayout.ID_EDITOR_AREA);

		leftFolder.addView(RawDataView.ID);

		IFolderLayout topFolder = layout.createFolder(FOLDER_ID_TOP,
				IPageLayout.TOP,
				0.5f,
				IPageLayout.ID_EDITOR_AREA);

		topFolder.addView(TourChartView.ID);

		layout.setEditorAreaVisible(false);

	}

}
