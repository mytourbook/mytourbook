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
package net.tourbook.tour;

// author:	Wolfgang Schramm 
// created:	6. July 2007

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPersistableEditor;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

public class TourEditor extends EditorPart implements IPersistableEditor {

	public static final String		ID				= "net.tourbook.tour.TourEditor";	//$NON-NLS-1$

	private static final String		MEMENTO_TOUR_ID	= "tourId";						//$NON-NLS-1$

	private TourEditorInput			fEditorInput;

	private TourChart				fTourChart;
	private TourChartConfiguration	fTourChartConfig;
	private TourData				fTourData;

	private boolean					fIsTourDirty	= false;

	private PostSelectionProvider	fPostSelectionProvider;
	private ISelectionListener		fPostSelectionListener;
	private IPartListener2			fPartListener;
	private ITourEventListener		fTourEventListener;

	private void addPartListener() {

		// set the part listener
		fPartListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourEditor.this) {
					if (partRef.getPart(false) == TourEditor.this) {
						fPostSelectionProvider.setSelection(new SelectionTourData(fTourChart, fTourData));
					}
				}
			}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		// register the part listener
		getSite().getPage().addPartListener(fPartListener);
	}

	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {

			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == TourEditor.this) {
					return;
				}

				if (selection instanceof SelectionChartXSliderPosition) {

					fTourChart.setXSliderPosition((SelectionChartXSliderPosition) selection);

				} else if (!selection.isEmpty() && selection instanceof SelectionDeletedTours) {

					final SelectionDeletedTours tourSelection = (SelectionDeletedTours) selection;
					final ArrayList<ITourItem> removedTours = tourSelection.removedTours;
					final long tourId = fTourData.getTourId().longValue();

					// find the current tour id in the removed tour id's
					for (final ITourItem tourItem : removedTours) {
						if (tourId == tourItem.getTourId().longValue()) {

							// close this editor
							getSite().getPage().closeEditor(TourEditor.this, false);
						}
					}
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourEventListener() {

		fTourEventListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part,
										final TourEventId eventId,
										final Object eventData) {

				if (part == TourEditor.this) {
					return;
				}

				if (eventId == TourEventId.SEGMENT_LAYER_CHANGED) {

					fTourChart.updateSegmentLayer((Boolean) eventData);

				} else if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

					fTourChart.updateTourChart(true);

				} else if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

					if (fTourData == null) {
						return;
					}

					final TourEvent tourProperties = (TourEvent) eventData;

					// get modified tours
					final ArrayList<TourData> modifiedTours = tourProperties.getModifiedTours();
					final long tourId = fTourData.getTourId();

					// check if the tour in the editor was modified
					for (final Object object : modifiedTours) {
						if (object instanceof TourData) {

							final TourData tourData = (TourData) object;
							if (tourData.getTourId() == tourId) {

								updateChart(tourData);

								// exit here because only one tourdata can be inside a tour editor
								return;
							}
						}
					}

				} else if (eventId == TourEventId.UPDATE_UI) {

					// check if this tour viewer contains a tour which must be updated

					// update editor
					if (UI.containsTourId(eventData, fTourData.getTourId()) != null) {

						// reload tour data and update chart
						updateChart(TourManager.getInstance().getTourData(fTourData.getTourId()));
					}
				}
			}

			private void updateChart(final TourData tourData) {
				
				// keep modified data
				fTourData = tourData;
				
				// update chart
				fTourChart.updateTourChart(tourData, false);
			}
		};

		TourManager.getInstance().addPropertyListener(fTourEventListener);
	}

	private void createActions() {

	}

	@Override
	public void createPartControl(final Composite parent) {

		addPartListener();
		addTourEventListener();
		createActions();

		fTourChart = new TourChart(parent, SWT.FLAT, true);

		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);
		fTourChart.setContextProvider(new TourChartContextProvider(this));

		// fire a slider move selection when a slider was moved in the tour chart
		fTourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(final SelectionChartInfo chartInfoSelection) {
				fPostSelectionProvider.setSelection(chartInfoSelection);
			}
		});

		fTourChartConfig = TourManager.createTourChartConfiguration();
		fTourChart.createTourEditorActionHandlers(fTourChartConfig);

		updateTourChart();
	}

	@Override
	public void dispose() {

		final IWorkbenchPartSite site = getSite();

		site.getPage().removePartListener(fPartListener);
		site.getPage().removePostSelectionListener(fPostSelectionListener);

		TourManager.getInstance().removePropertyListener(fTourEventListener);

		super.dispose();
	}

	@Override
	public void doSave(final IProgressMonitor monitor) {}

	@Override
	public void doSaveAs() {}

	/**
	 * provides the service to fire post selections
	 * 
	 * @param selection
	 */
	public void firePostSelection(final ISelection selection) {
		fPostSelectionProvider.setSelection(selection);
	}

	public TourChart getTourChart() {
		return fTourChart;
	}

	public TourData getTourData() {
		return fTourData;
	}

	@Override
	public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {

		setSite(site);
		setInput(input);

		fEditorInput = (TourEditorInput) input;

		// set selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		addSelectionListener();
	}

	@Override
	public boolean isDirty() {
		return fIsTourDirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public void restoreState(final IMemento memento) {

		if (memento == null) {
			return;
		}
	}

	public void saveState(final IMemento memento) {
		memento.putString(MEMENTO_TOUR_ID, Long.toString(fEditorInput.getTourId()));
	}

	@Override
	public void setFocus() {
		fTourChart.setFocus();
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();

		sb.append("[TourEditor] "); //$NON-NLS-1$
		sb.append(fTourData);

		return sb.toString();
	}

	/**
	 * load tour data and update the tour chart
	 */
	private void updateTourChart() {

		// load tourdata 
		fTourData = TourManager.getInstance().getTourData(fEditorInput.getTourId());

		if (fTourData != null) {

			// show the tour chart

			fTourChart.addDataModelListener(new IDataModelListener() {
				public void dataModelChanged(final ChartDataModel changedChartDataModel) {

					// set title
					changedChartDataModel.setTitle(NLS.bind(Messages.Tour_Book_Label_chart_title,
							TourManager.getTourTitleDetailed(fTourData)));
				}
			});

			fTourChart.updateTourChart(fTourData, fTourChartConfig, false);

			final String tourTitle = TourManager.getTourDateShort(fTourData);

			fEditorInput.fEditorTitle = tourTitle == null ? "" : tourTitle; //$NON-NLS-1$

			setPartName(tourTitle);
			setTitleToolTip("title tooltip ???"); //$NON-NLS-1$
		}
	}

}
