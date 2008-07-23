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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.views.tourCatalog.SelectionNewRefTours;
import net.tourbook.util.PostSelectionProvider;

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
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;

public class TourEditor extends EditorPart {

	public static final String				ID						= "net.tourbook.tour.TourEditor";	//$NON-NLS-1$

	private TourEditorInput					fEditorInput;

	private TourChart						fTourChart;
	private TourChartConfiguration			fTourChartConfig;
	private TourData						fTourData;

	private boolean							fIsTourDirty			= false;
	private boolean							fIsTourChanged			= false;
	private boolean							fIsTourPropertyModified	= false;


	private PostSelectionProvider			fPostSelectionProvider;
	private ISelectionListener				fPostSelectionListener;
	private IPartListener2					fPartListener;
	private ITourPropertyListener			fTourPropertyListener;

	private IHandlerService					fHandlerService;
	private ActionHandlerRevertTourEditor	fRevertActionHandler;

	private boolean							fIsRefTourCreated		= false;


	private void addPartListener() {

		// set the part listener
		fPartListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourEditor.this) {

					fTourChart.activateActionHandlers(getSite());

					updateRevertHandler();
				}
			}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (fIsTourChanged) {
					TourDatabase.getInstance().firePropertyChange(TourDatabase.TOUR_IS_CHANGED_AND_PERSISTED);
				}
			}

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

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(final int propertyId, final Object propertyData) {

				if (propertyId == TourManager.TOUR_PROPERTY_SEGMENT_LAYER_CHANGED) {

					fTourChart.updateSegmentLayer((Boolean) propertyData);

				} else if (propertyId == TourManager.TOUR_CHART_PROPERTY_IS_MODIFIED) {

					fTourChart.updateTourChart(true);

				} else if (propertyId == TourManager.TOUR_PROPERTIES_CHANGED) {

					if (fTourData == null) {
						return;
					}

					// get modified tours
					final ArrayList<?> modifiedTours = (ArrayList<?>) propertyData;
					final long tourId = fTourData.getTourId();

					// check if the tour in the editor was modified
					for (final Object object : modifiedTours) {

						if (object instanceof TourData) {
							final TourData tourData = (TourData) object;

							if (tourData.getTourId() == tourId) {
								if (isDirty()) {

									final ArrayList<TourData> modifiedTour = new ArrayList<TourData>();
									modifiedTour.add(tourData);
									if (TourManager.saveTourEditors(modifiedTour)) {
										updateTourData(tourData);
									}
								} else {
									updateTourData(tourData);
								}

								// exit here because only one tourdata can be inside a tour editor
								return;
							}
						}
					}
				}
			}

			private void updateTourData(final TourData tourData) {

				// keep changed data
				fTourData = tourData;

				fTourChart.updateTourChart(fTourData, true);
			}
		};

		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	private void createActions() {

		fHandlerService = (IHandlerService) getSite().getService(IHandlerService.class);

		fRevertActionHandler = new ActionHandlerRevertTourEditor(this);

		fHandlerService.activateHandler("net.tourbook.command.tourEditor.revert", //$NON-NLS-1$
				fRevertActionHandler);
	}

	@Override
	public void createPartControl(final Composite parent) {

		addPartListener();
		addTourPropertyListener();
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

		fTourChart.addTourModifyListener(new ITourModifyListener() {
			public void tourIsModified() {
				fIsTourDirty = true;
				fIsTourChanged = true;
				firePropertyChange(PROP_DIRTY);
				updateRevertHandler();
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

		TourManager.getInstance().removePropertyListener(fTourPropertyListener);

		super.dispose();
	}

	@Override
	public void doSave(final IProgressMonitor monitor) {

		TourDatabase.saveTour(fTourData);

		fIsTourDirty = false;
		fIsTourChanged = false;

		TourDatabase.getInstance().firePropertyChange(TourDatabase.TOUR_IS_CHANGED_AND_PERSISTED);

		// hide the dirty indicator
		firePropertyChange(PROP_DIRTY);

		updateRevertHandler();

		if (fIsRefTourCreated) {

			fIsRefTourCreated = false;

			// update tour map view
			firePostSelection(new SelectionNewRefTours());
		}

		if (fIsTourPropertyModified) {

			fIsTourPropertyModified = false;

			// notify all views which display the tour type
			final ArrayList<TourData> modifiedTour = new ArrayList<TourData>();
			modifiedTour.add(fTourData);

			TourManager.firePropertyChange(TourManager.TOUR_PROPERTIES_CHANGED, modifiedTour);
		}
	}

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

	public void revertTourData() {

		TourManager.getInstance().removeTourFromCache(fEditorInput.getTourId());

		fIsTourDirty = false;
		fIsTourChanged = false;

		firePropertyChange(PROP_DIRTY);
		updateRevertHandler();

		updateTourChart();

		// notify all views which display the tour
		final ArrayList<TourData> modifiedTour = new ArrayList<TourData>();
		modifiedTour.add(fTourData);

		TourManager.firePropertyChange(TourManager.TOUR_PROPERTIES_CHANGED, modifiedTour);
	}

	@Override
	public void setFocus() {

		fTourChart.setFocus();

		/*
		 * fire tour selection
		 */
		fPostSelectionProvider.setSelection(new SelectionActiveEditor(TourEditor.this));
	}

	/**
	 * set status a reference tour was created
	 */
	public void setRefTourIsCreated() {
		fIsRefTourCreated = true;
	}


	/**
	 * Set the tour dirty
	 */
	public void setTourDirty() {

		fIsTourDirty = true;
		fIsTourChanged = true;

		firePropertyChange(PROP_DIRTY);
		updateRevertHandler();
	}

	/**
	 * Marks the tour dirty and fire an {@link TourManager#TOUR_PROPERTIES_CHANGED} event when the
	 * tour is saved
	 */
	public void setTourPropertyIsModified() {

		fIsTourPropertyModified = true;

		// update changed properties (title has changed, this is an overkill and can be optimized)
		fTourChart.updateTourChart(fTourData, fTourChartConfig, false);

		setTourDirty();
	}

	private void updateRevertHandler() {
		fRevertActionHandler.setEnabled(fIsTourDirty);
		fRevertActionHandler.fireHandlerChanged();
	}

	private void updateTourChart() {

		// load the tourdata from the database
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

			final String tourTitle = TourManager.getTourDate(fTourData);

			fEditorInput.fEditorTitle = tourTitle == null ? "" : tourTitle; //$NON-NLS-1$
			setPartName(tourTitle);
			setTitleToolTip("title tooltip ???"); //$NON-NLS-1$
		}
	}

}
