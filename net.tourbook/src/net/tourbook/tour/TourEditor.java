/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.data.TourData;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

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

/**
 * Editorpart for a tour, each tour has it's own editor part.
 * <p>
 * Tours can be edited <b>only</b> in the {@link TourDataEditorView}.
 */
public class TourEditor extends EditorPart implements IPersistableEditor {

	public static final String		ID				= "net.tourbook.tour.TourEditor";	//$NON-NLS-1$

	private static final String		MEMENTO_TOUR_ID	= "tourId";						//$NON-NLS-1$

	private TourEditorInput			_editorInput;

	private TourChart				_tourChart;
	private TourChartConfiguration	_tourChartConfig;
	private TourData				_tourData;

	private boolean					_isTourDirty	= false;

	private PostSelectionProvider	_postSelectionProvider;
	private ISelectionListener		_postSelectionListener;
	private IPartListener2			_partListener;
	private ITourEventListener		_tourEventListener;

	private void addPartListener() {

		// set the part listener
		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourEditor.this) {
					if (partRef.getPart(false) == TourEditor.this) {
						_postSelectionProvider.setSelection(new SelectionTourData(_tourChart, _tourData));
					}
				}
			}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourEditor.this) {
					_tourChart.partIsHidden();
				}
			}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourEditor.this) {
					_tourChart.partIsVisible();
				}
			}
		};

		// register the part listener
		getSite().getPage().addPartListener(_partListener);
	}

	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {

			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == TourEditor.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == TourEditor.this) {
					return;
				}

				if (eventId == TourEventId.SEGMENT_LAYER_CHANGED) {

					_tourChart.updateLayerSegment((Boolean) eventData);

				} else if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

					_tourChart.updateTourChart(true);

				} else if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

					if (_tourData == null) {
						return;
					}

					final TourEvent tourEvent = (TourEvent) eventData;

					// get modified tours
					final ArrayList<TourData> modifiedTours = tourEvent.getModifiedTours();
					final long tourId = _tourData.getTourId();

					// check if the tour in the editor was modified
					for (final Object object : modifiedTours) {
						if (object instanceof TourData) {

							final TourData tourData = (TourData) object;
							if (tourData.getTourId() == tourId) {

								updateChart(tourData);

								// removed old tour data from the selection provider
								_postSelectionProvider.clearSelection();

								// exit here because only one tourdata can be inside a tour editor
								return;
							}
						}
					}

				} else if (eventId == TourEventId.UPDATE_UI) {

					// check if this tour viewer contains a tour which must be updated

					// update editor
					if (UI.containsTourId(eventData, _tourData.getTourId()) != null) {

						// reload tour data and update chart
						updateChart(TourManager.getInstance().getTourData(_tourData.getTourId()));
					}
				}
			}

			private void updateChart(final TourData tourData) {

				// keep modified data
				_tourData = tourData;

				// update chart
				_tourChart.updateTourChart(tourData, false);
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void createActions() {

	}

	@Override
	public void createPartControl(final Composite parent) {

		addPartListener();
		addTourEventListener();
		createActions();

		_tourChart = new TourChart(parent, SWT.FLAT, true);

		_tourChart.setShowZoomActions(true);
		_tourChart.setShowSlider(true);
		_tourChart.setContextProvider(new TourChartContextProvider(this));

		// fire a slider move selection when a slider was moved in the tour chart
		_tourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(final SelectionChartInfo chartInfoSelection) {
				_postSelectionProvider.setSelection(chartInfoSelection);
			}
		});

		_tourChartConfig = TourManager.createDefaultTourChartConfig();
		_tourChart.createActions_TourEditor(_tourChartConfig);

		updateTourChart();
	}

	@Override
	public void dispose() {

		final IWorkbenchPartSite site = getSite();

		site.getPage().removePartListener(_partListener);
		site.getPage().removePostSelectionListener(_postSelectionListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		super.dispose();
	}

	@Override
	public void doSave(final IProgressMonitor monitor) {}

	@Override
	public void doSaveAs() {}

	public TourChart getTourChart() {
		return _tourChart;
	}

	public TourData getTourData() {
		return _tourData;
	}

	@Override
	public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {

		setSite(site);
		setInput(input);

		_editorInput = (TourEditorInput) input;

		// set selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

		addSelectionListener();
	}

	@Override
	public boolean isDirty() {
		return _isTourDirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	private void onSelectionChanged(final ISelection selection) {

		if (selection instanceof SelectionChartXSliderPosition) {

			final SelectionChartXSliderPosition xSliderPosition = (SelectionChartXSliderPosition) selection;
			final Chart chart = xSliderPosition.getChart();
			if (chart == null) {
				return;
			}

			if (chart != _tourChart) {

				// it's not the same chart, check if it's the same tour

				final Object tourId = chart.getChartDataModel().getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
				if (tourId instanceof Long) {

					final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
					if (tourData != null) {

						if (_tourData.equals(tourData)) {

							// it's the same tour, overwrite chart

							xSliderPosition.setChart(_tourChart);
						}
					}
				}
			}

			_tourChart.setXSliderPosition(xSliderPosition);

		} else if (selection instanceof SelectionDeletedTours) {

			final SelectionDeletedTours tourSelection = (SelectionDeletedTours) selection;
			final ArrayList<ITourItem> removedTours = tourSelection.removedTours;
			final long tourId = _tourData.getTourId().longValue();

			// find the current tour id in the removed tour id's
			for (final ITourItem tourItem : removedTours) {
				if (tourId == tourItem.getTourId().longValue()) {

					// close this editor
					getSite().getPage().closeEditor(TourEditor.this, false);
				}
			}

		} else if (selection instanceof SelectionChartInfo) {

			final ChartDataModel chartDataModel = ((SelectionChartInfo) selection).chartDataModel;
			if (chartDataModel != null) {

				final Object tourId = chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
				if (tourId instanceof Long) {

					final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
					if (tourData != null) {

						if (_tourData == null || _tourData.equals(tourData) == false) {

							_tourData = tourData;

							updateTourChart();
						}

						final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

						// set slider position
						_tourChart.setXSliderPosition(new SelectionChartXSliderPosition(
								_tourChart,
								chartInfo.leftSliderValuesIndex,
								chartInfo.rightSliderValuesIndex));
					}
				}
			}
		}
	}

	public void restoreState(final IMemento memento) {

		if (memento == null) {
			return;
		}
	}

	public void saveState(final IMemento memento) {
		memento.putString(MEMENTO_TOUR_ID, Long.toString(_editorInput.getTourId()));
	}

	@Override
	public void setFocus() {
		_tourChart.setFocus();
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();

		sb.append("[TourEditor] "); //$NON-NLS-1$
		sb.append(_tourData);

		return sb.toString();
	}

	/**
	 * load tour data and update the tour chart
	 */
	private void updateTourChart() {

		// load tourdata
		_tourData = TourManager.getInstance().getTourData(_editorInput.getTourId());

		if (_tourData != null) {

			// show the tour chart

			_tourChart.addDataModelListener(new IDataModelListener() {
				public void dataModelChanged(final ChartDataModel changedChartDataModel) {

					// set title
					changedChartDataModel.setTitle(NLS.bind(
							Messages.Tour_Book_Label_chart_title,
							TourManager.getTourTitleDetailed(_tourData)));
				}
			});

			_tourChart.updateTourChart(_tourData, _tourChartConfig, false);

			final String tourTitle = TourManager.getTourDateShort(_tourData);

			_editorInput.editorTitle = tourTitle == null ? UI.EMPTY_STRING : tourTitle;

			setPartName(tourTitle);
			setTitleToolTip("title tooltip ???"); //$NON-NLS-1$
		}
	}

}
