/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
 *  
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation version 2 of the License.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA    
 *******************************************************************************/
package net.tourbook.osm;

import net.tourbook.data.TourData;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import de.byteholder.geoclipse.MapView;
import de.byteholder.geoclipse.swt.Map;
import de.byteholder.gpx.GeoPosition;

/**
 * @author Wolfgang Schramm
 * @since 1.3
 */
public class OSMView extends ViewPart {

	final public static String	ID	= "net.tourbook.osm.OSMViewID"; //$NON-NLS-1$

	private ISelectionListener	fPostSelectionListener;

	public OSMView() {}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				onChangeSelection(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	@Override
	public void createPartControl(Composite parent) {
		addSelectionListener();
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);

		super.dispose();
	}

	private void onChangeSelection(ISelection selection) {

		if (selection instanceof SelectionTourData) {

			final SelectionTourData selectionTourData = (SelectionTourData) selection;
			final TourData tourData = selectionTourData.getTourData();
			drawTour(tourData);

		} else if (selection instanceof SelectionTourId) {

			SelectionTourId tourIdSelection = (SelectionTourId) selection;

			final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

			drawTour(tourData);

		} else if (selection instanceof SelectionActiveEditor) {

			final IEditorPart editor = ((SelectionActiveEditor) selection).getEditor();

			if (editor instanceof TourEditor) {
				TourEditor fTourEditor = (TourEditor) editor;
				TourChart fTourChart = fTourEditor.getTourChart();
				drawTour(fTourChart.getTourData());
			}
		}
	}

	private void drawTour(TourData tourData) {

		if (tourData == null) {
			return;
		}

		if (tourData.longitudeSerie == null || tourData.latitudeSerie == null) {
			return;
		}

		MapView mapView = (MapView) getSite().getPage().findView(MapView.ID);

		if (mapView == null) {
			try {
				mapView = (MapView) getSite().getPage().showView(MapView.ID);
			} catch (PartInitException e) {
				MessageDialog.openError(getSite().getShell(), "Error", "Error opening view:" + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
				e.printStackTrace();
			}
		}

		PaintManager.getInstance().setTourData(tourData);

		final Map map = mapView.getMap();

//		map.setZoom(8);
		map.setCenterPosition(new GeoPosition(tourData.latitudeSerie[0], tourData.longitudeSerie[0]));
		map.queueRedraw();
	}

	@Override
	public void setFocus() {}
}
