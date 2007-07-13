/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

// author:	Wolfgang Schramm 
// created:	6. July 2007

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.views.SelectionTourSegmentLayer;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

public class TourEditor extends EditorPart {

	public static final String		ID				= "net.tourbook.tour.TourEditor";

	private TourEditorInput			fEditorInput;

	private TourChart				fTourChart;
	private TourChartConfiguration	fTourChartConfig;
	private TourData				fTourData;

	private boolean					fIsTourDirty	= false;

	private PostSelectionProvider	fPostSelectionProvider;
	private ISelectionListener		fPostSelectionListener;

	public void doSave(IProgressMonitor monitor) {
		
		TourDatabase.saveTour(fTourData);
		fIsTourDirty = false;
		
		// update (hide) the dirty indicator
		firePropertyChange(PROP_DIRTY);
	}

	public void doSaveAs() {}

	public void init(IEditorSite site, IEditorInput input) throws PartInitException {

		setSite(site);
		setInput(input);

		fEditorInput = (TourEditorInput) input;

		// set selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		setPostSelectionListener();
	}

	public boolean isDirty() {
		return fIsTourDirty;
	}

	public boolean isSaveAsAllowed() {
		return false;
	}

	public void createPartControl(Composite parent) {

		fTourChart = new TourChart(parent, SWT.FLAT, true);

		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);
		fTourChart.addTourModifyListener(new ITourModifyListener() {
			public void tourIsModified() {

				fIsTourDirty = true;

				firePropertyChange(PROP_DIRTY);
			}
		});

		fTourChartConfig = TourManager.createTourChartConfiguration();

		// load the tourdata from the database
		fTourData = TourDatabase.getTourDataByTourId(fEditorInput.fTourId);

		if (fTourData != null) {

			// show the tour chart

			fTourChart.addDataModelListener(new IDataModelListener() {
				public void dataModelChanged(ChartDataModel changedChartDataModel) {

					// set title
					changedChartDataModel.setTitle(NLS.bind(
							Messages.TourBook_Label_chart_title,
							TourManager.getTourTitleDetailed(fTourData)));
				}
			});

			fTourChart.updateChart(fTourData, fTourChartConfig, false);

			setPartName(TourManager.getTourDate(fTourData));
			setTitleToolTip("title tooltip ???");
		}
	}

	public void setFocus() {
		fPostSelectionProvider.setSelection(new TourChartSelection(fTourChart));
	}

	private void setPostSelectionListener() {
		// this view part is a selection listener
		fPostSelectionListener = new ISelectionListener() {

			public void selectionChanged(IWorkbenchPart part, ISelection selection) {

				if (selection instanceof SelectionTourSegmentLayer) {
					fTourChart
							.updateSegmentLayer(((SelectionTourSegmentLayer) selection).isLayerVisible);
				}

				if (selection instanceof SelectionChartXSliderPosition) {
					fTourChart.setXSliderPosition((SelectionChartXSliderPosition) selection);
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

}
