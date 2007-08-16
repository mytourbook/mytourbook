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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

public class TourEditor extends EditorPart {

	public static final String		ID				= "net.tourbook.tour.TourEditor";

	private TourEditorInput			fEditorInput;

	private TourChart				fTourChart;
	private TourChartConfiguration	fTourChartConfig;
	private TourData				fTourData;

	private boolean					fIsTourDirty	= false;

	private ISelectionListener		fPostSelectionListener;
	private IPartListener2			fPartListener;

	private void addPartListener() {

		// set the part listener
		fPartListener = new IPartListener2() {

			public void partActivated(IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourEditor.this) {
					fTourChart.activateActionHandlers(getSite());
				}
			}

			public void partBroughtToTop(IWorkbenchPartReference partRef) {}

			public void partClosed(IWorkbenchPartReference partRef) {}

			public void partDeactivated(IWorkbenchPartReference partRef) {}

			public void partHidden(IWorkbenchPartReference partRef) {}

			public void partInputChanged(IWorkbenchPartReference partRef) {}

			public void partOpened(IWorkbenchPartReference partRef) {}

			public void partVisible(IWorkbenchPartReference partRef) {}
		};

		// register the part listener
		getSite().getPage().addPartListener(fPartListener);
	}

	public void createPartControl(Composite parent) {

		addPartListener();

		fTourChart = new TourChart(parent, SWT.FLAT, false);

		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);

		fTourChart.addTourModifyListener(new ITourModifyListener() {
			public void tourIsModified() {
				fIsTourDirty = true;
				firePropertyChange(PROP_DIRTY);
			}
		});

		fTourChartConfig = TourManager.createTourChartConfiguration();
		fTourChart.createTourActionHandlers(fTourChartConfig);

		// load the tourdata from the database
		fTourData = TourManager.getInstance().getTourData(fEditorInput.fTourId);

		if (fTourData != null) {

			// show the tour chart

			fTourChart.addDataModelListener(new IDataModelListener() {
				public void dataModelChanged(ChartDataModel changedChartDataModel) {

					// set title
					changedChartDataModel.setTitle(NLS.bind(Messages.TourBook_Label_chart_title,
							TourManager.getTourTitleDetailed(fTourData)));
				}
			});

			fTourChart.updateChart(fTourData, fTourChartConfig, false);

			final String tourTitle = TourManager.getTourDate(fTourData);
			
			fEditorInput.fEditorTitle=tourTitle;
			setPartName(tourTitle);
			setTitleToolTip("title tooltip ???");
		}
	}

	public void dispose() {

		final IWorkbenchPartSite site = getSite();

		if (site != null) {
			site.getPage().removePartListener(fPartListener);
			site.setSelectionProvider(null);
		}

//		if (fHandlerActivationAltitude != null && fHandlerService != null) {
//			fHandlerService.deactivateHandler(fHandlerActivationAltitude);
//		}

//	    if (getEditorSite().getActionBarContributor().getActiveEditor() == this)
//	    {
//	      getEditorSite().getActionBarContributor().setActiveEditor(null);
//	    }
		super.dispose();
	}

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
//		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		setPostSelectionListener();
	}

	public boolean isDirty() {
		return fIsTourDirty;
	}

//	public void setActiveEditor(TourEditor tourEditor) {
//		fTourChart.updateStatus();
//	}

	public boolean isSaveAsAllowed() {
		return false;
	}

	public void setFocus() {

		fTourChart.setFocus();

		/*
		 * fire tour selection
		 */
//		fPostSelectionProvider.setSelection(new SelectionTourData(fTourChart.fTourData));
	}

	private void setPostSelectionListener() {
		// this view part is a selection listener
		fPostSelectionListener = new ISelectionListener() {

			public void selectionChanged(IWorkbenchPart part, ISelection selection) {

				if (selection instanceof SelectionTourSegmentLayer) {
					fTourChart.updateSegmentLayer(((SelectionTourSegmentLayer) selection).isLayerVisible);
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
