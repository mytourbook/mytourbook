/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;

public class TourInfoView extends ViewPart {

	public static final String			ID				= "net.tourbook.ui.views.TourInfoView";	//$NON-NLS-1$

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private PostSelectionProvider		_postSelectionProvider;
	private ISelectionListener			_postSelectionListener;
	private IPropertyChangeListener	_prefChangeListener;
	private ITourEventListener			_tourEventListener;

	private TourData						_tourData;

	/*
	 * UI controls
	 */
	private PageBook						_pageBook;

	private Composite						_pageNoData;
	private Composite						_pageContent;

	private Text							_txtDateTimeCreated;
	private Text							_txtDateTimeModified;
	private Text							_txtDeviceName;
	private Text							_txtDeviceFirmwareVersion;
	private Text							_txtDistanceSensor;
	private Text							_txtImportFilePath;
	private Text							_txtMergeFromTourId;
	private Text							_txtMergeIntoTourId;
	private Text							_txtPulseSensor;
	private Text							_txtPowerSensor;
	private Text							_txtPerson;
	private Text							_txtRefTour;
	private Text							_txtStrideSensor;
	private Text							_txtTimeSlicesCount;
	private Text							_txtTourId;

	private ScrolledComposite			_uiContainer;

	private Composite						_infoContainer;

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

				} else if (property.equals(ITourbookPreferences.GRAPH_MARKER_IS_MODIFIED)) {

					updateUI();
				}
			}
		};
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				if (part == TourInfoView.this) {
					return;
				}
				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == TourInfoView.this) {
					return;
				}

				if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {

						// update modified tour

						final long viewTourId = _tourData.getTourId();

						for (final TourData tourData : modifiedTours) {
							if (tourData.getTourId() == viewTourId) {

								// get modified tour
								_tourData = tourData;

								// removed old tour data from the selection provider
								_postSelectionProvider.clearSelection();

								updateUI();

								// nothing more to do, the view contains only one tour
								return;
							}
						}
					}

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();

				} else if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);

				} else if (eventId == TourEventId.MARKER_SELECTION) {

					if (eventData instanceof SelectionTourMarker) {

						final TourData tourData = ((SelectionTourMarker) eventData).getTourData();

						if (tourData != _tourData) {

							_tourData = tourData;

							updateUI();
						}
					}
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void clearView() {

		_tourData = null;

		// removed old tour data from the selection provider
		_postSelectionProvider.clearSelection();

		showInvalidPage();
	}

	private void createActions() {

	}

	@Override
	public void createPartControl(final Composite parent) {

		initUI();

		createUI(parent);
		createActions();

		addSelectionListener();
		addTourEventListener();
		addPrefListener();

		showInvalidPage();

		// this part is a selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

		// show markers from last selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (_tourData == null) {
			showTourFromTourProvider();
		}
	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook);

		_pageNoData = UI.createUI_PageNoData(_pageBook, Messages.UI_Label_no_chart_is_selected);

		_pageContent = new Composite(_pageBook, SWT.NONE);
		_pageContent.setLayout(new FillLayout());
		{
			createUI_10_Container(_pageContent);
		}
	}

	private Composite createUI_10_Container(final Composite parent) {

		/*
		 * scrolled container
		 */
		_uiContainer = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		_uiContainer.setExpandVertical(true);
		_uiContainer.setExpandHorizontal(true);
		_uiContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onResize();
			}
		});
		{
			_infoContainer = new Composite(_uiContainer, SWT.NONE);
			_infoContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			GridDataFactory.fillDefaults().grab(true, true).applyTo(_infoContainer);
			GridLayoutFactory.swtDefaults().applyTo(_infoContainer);
		}

		// set content for scrolled composite
		_uiContainer.setContent(_infoContainer);

		createUI_20_Data(_infoContainer);

		return _uiContainer;
	}

	private void createUI_20_Data(final Composite parent) {

		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()
				.numColumns(2)
				.spacing(10, 2)
				.applyTo(container);
		{
			/*
			 * date/time created
			 */
			createUI_Label(container, Messages.Tour_Editor_Label_DateTimeCreated);
			_txtDateTimeCreated = createUI_FieldText(container);

			/*
			 * date/time modified
			 */
			createUI_Label(container, Messages.Tour_Editor_Label_DateTimeModified);
			_txtDateTimeModified = createUI_FieldText(container);

			/*
			 * number of time slices
			 */
			createUI_Label(container, Messages.tour_editor_label_datapoints);
			_txtTimeSlicesCount = createUI_FieldText(container);

			/*
			 * device name
			 */
			createUI_Label(container, Messages.tour_editor_label_device_name);
			_txtDeviceName = createUI_FieldText(container);

			/*
			 * device firmware version
			 */
			createUI_Label(container, Messages.Tour_Editor_Label_DeviceFirmwareVersion);
			_txtDeviceFirmwareVersion = createUI_FieldText(container);

			/*
			 * distance sensor
			 */
			createUI_Label(container, Messages.Tour_Editor_Label_DistanceSensor);

			_txtDistanceSensor = createUI_FieldText(container);
			_txtDistanceSensor.setToolTipText(Messages.Tour_Editor_Label_DeviceSensor_Tooltip);

			/*
			 * stride sensor
			 */
			createUI_Label(container, Messages.Tour_Editor_Label_StrideSensor);

			_txtStrideSensor = createUI_FieldText(container);
			_txtStrideSensor.setToolTipText(Messages.Tour_Editor_Label_DeviceSensor_Tooltip);

			/*
			 * pulse sensor
			 */
			createUI_Label(container, Messages.Tour_Editor_Label_PulseSensor);

			_txtPulseSensor = createUI_FieldText(container);
			_txtPulseSensor.setToolTipText(Messages.Tour_Editor_Label_DeviceSensor_Tooltip);

			/*
			 * power sensor
			 */
			createUI_Label(container, Messages.Tour_Editor_Label_PowerSensor);

			_txtPowerSensor = createUI_FieldText(container);
			_txtPowerSensor.setToolTipText(Messages.Tour_Editor_Label_DeviceSensor_Tooltip);

			/*
			 * import file path
			 */
			createUI_Label(container, Messages.tour_editor_label_import_file_path);
			_txtImportFilePath = createUI_FieldText(container);

			/*
			 * person
			 */
			createUI_Label(container, Messages.tour_editor_label_person);
			_txtPerson = createUI_FieldText(container);

			/*
			 * tour id
			 */
			label = createUI_Label(container, Messages.tour_editor_label_tour_id);
			label.setToolTipText(Messages.tour_editor_label_tour_id_tooltip);

			_txtTourId = createUI_FieldText(container);

			/*
			 * reference tours
			 */
			label = createUI_Label(container, Messages.tour_editor_label_ref_tour);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);
			_txtRefTour = new Text(container, SWT.READ_ONLY | SWT.MULTI);
			_txtRefTour.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

			/*
			 * merged from tour id
			 */
			label = createUI_Label(container, Messages.tour_editor_label_merge_from_tour_id);
			label.setToolTipText(Messages.tour_editor_label_merge_from_tour_id_tooltip);

			_txtMergeFromTourId = createUI_FieldText(container);

			/*
			 * merged into tour id
			 */
			label = createUI_Label(container, Messages.tour_editor_label_merge_into_tour_id);
			label.setToolTipText(Messages.tour_editor_label_merge_into_tour_id_tooltip);

			_txtMergeIntoTourId = createUI_FieldText(container);
		}
	}

	private Text createUI_FieldText(final Composite parent) {

		final Text txtField = new Text(parent, SWT.READ_ONLY);
		txtField.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(txtField);

		return txtField;
	}

	private Label createUI_Label(final Composite parent, final String text) {

		final Label label = new Label(parent, SWT.NONE);
		label.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		label.setText(text);

		return label;
	}

	@Override
	public void dispose() {

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		getSite().getPage().removePostSelectionListener(_postSelectionListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void initUI() {

	}

	private void onResize() {

		// horizontal scroll bar ishidden, only the vertical scrollbar can be displayed
		int infoContainerWidth = _uiContainer.getBounds().width;
		final ScrollBar vertBar = _uiContainer.getVerticalBar();

		if (vertBar != null) {
			// vertical bar is displayed
			infoContainerWidth -= vertBar.getSize().x;
		}

		final Point minSize = _infoContainer.computeSize(infoContainerWidth, SWT.DEFAULT);

		_uiContainer.setMinSize(minSize);
	}

	private void onSelectionChanged(final ISelection selection) {

		long tourId = TourDatabase.ENTITY_IS_NOT_SAVED;

		if (selection instanceof SelectionTourData) {

			// a tour is selected

			final SelectionTourData tourDataSelection = (SelectionTourData) selection;
			_tourData = tourDataSelection.getTourData();

			if (_tourData != null) {
				tourId = _tourData.getTourId();
			}

		} else if (selection instanceof SelectionTourId) {

			tourId = ((SelectionTourId) selection).getTourId();

		} else if (selection instanceof SelectionTourIds) {

			final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
			if ((tourIds != null) && (tourIds.size() > 0)) {
				tourId = tourIds.get(0);
			}

		} else if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {
				tourId = refItem.getTourId();
			}

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();
			if (firstElement instanceof TVICatalogComparedTour) {
				tourId = ((TVICatalogComparedTour) firstElement).getTourId();
			} else if (firstElement instanceof TVICompareResultComparedTour) {
				tourId = ((TVICompareResultComparedTour) firstElement).getComparedTourData().getTourId();
			}

		} else if (selection instanceof SelectionDeletedTours) {

			clearView();
		}

		if (tourId >= TourDatabase.ENTITY_IS_NOT_SAVED) {

			final TourData tourData = TourManager.getInstance().getTourData(tourId);
			if (tourData != null) {
				_tourData = tourData;
			}
		}

		final boolean isTourAvailable = (tourId >= 0) && (_tourData != null);
		if (isTourAvailable) {
			updateUI();
		}
	}

	@Override
	public void setFocus() {

		_pageBook.setFocus();
	}

	private void showInvalidPage() {

		_pageBook.showPage(_pageNoData);
	}

	private void showTourFromTourProvider() {

		showInvalidPage();

		// a tour is not displayed, find a tour provider which provides a tour
		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {

				// validate widget
				if (_pageBook.isDisposed()) {
					return;
				}

				/*
				 * check if tour was set from a selection provider
				 */
				if (_tourData != null) {
					return;
				}

				final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();

				if ((selectedTours != null) && (selectedTours.size() > 0)) {
					onSelectionChanged(new SelectionTourData(selectedTours.get(0)));
				}
			}
		});
	}

	/**
	 * Update the UI from {@link #_tourData}.
	 */
	void updateUI() {

		if (_tourData == null) {
			return;
		}

		_pageBook.showPage(_pageContent);

		// data points
		final int[] timeSerie = _tourData.timeSerie;
		if (timeSerie == null) {
			_txtTimeSlicesCount.setText(UI.EMPTY_STRING);
		} else {
			final int dataPoints = timeSerie.length;
			_txtTimeSlicesCount.setText(Integer.toString(dataPoints));
		}
		_txtTimeSlicesCount.pack(true);

		// device name/version
		_txtDeviceName.setText(_tourData.getDeviceName());
		_txtDeviceName.pack(true);
		_txtDeviceFirmwareVersion.setText(_tourData.getDeviceFirmwareVersion());
		_txtDeviceFirmwareVersion.pack(true);

		// distance sensor
		_txtDistanceSensor.setText(_tourData.isDistanceSensorPresent()
				? Messages.Tour_Editor_Label_Sensor_Yes
				: Messages.Tour_Editor_Label_Sensor_No);

		// stride sensor
		_txtStrideSensor.setText(_tourData.isStrideSensorPresent()
				? Messages.Tour_Editor_Label_Sensor_Yes
				: Messages.Tour_Editor_Label_Sensor_No);

		// pulse sensor
		_txtPulseSensor.setText(_tourData.isPulseSensorPresent()
				? Messages.Tour_Editor_Label_Sensor_Yes
				: Messages.Tour_Editor_Label_Sensor_No);

		// power sensor
		_txtPowerSensor.setText(_tourData.isPowerSensorPresent()
				? Messages.Tour_Editor_Label_Sensor_Yes
				: Messages.Tour_Editor_Label_Sensor_No);

		// import file path
		final String importFilePath = _tourData.getImportFilePathNameText();
		_txtImportFilePath.setText(importFilePath);
		_txtImportFilePath.setToolTipText(importFilePath);

		/*
		 * reference tours
		 */
		final Collection<TourReference> refTours = _tourData.getTourReferences();
		if (refTours.size() > 0) {
			updateUI_RefTourInfo(refTours);
		} else {
			_txtRefTour.setText(Messages.tour_editor_label_ref_tour_none);
		}

		/*
		 * person
		 */
		final TourPerson tourPerson = _tourData.getTourPerson();
		if (tourPerson == null) {
			_txtPerson.setText(UI.EMPTY_STRING);
		} else {
			_txtPerson.setText(tourPerson.getName());
		}

		/*
		 * tour ID
		 */
		final Long tourId = _tourData.getTourId();
		if (tourId == null) {
			_txtTourId.setText(UI.EMPTY_STRING);
		} else {
			_txtTourId.setText(Long.toString(tourId));
		}

		/*
		 * date/time created
		 */
		final ZonedDateTime dtCreated = _tourData.getDateTimeCreated();
		_txtDateTimeCreated.setText(dtCreated == null ? //
				UI.EMPTY_STRING
				: dtCreated.format(TimeTools.Formatter_DateTime_M));

		/*
		 * date/time modified
		 */
		final ZonedDateTime dtModified = _tourData.getDateTimeModified();
		_txtDateTimeModified.setText(dtModified == null ? //
				UI.EMPTY_STRING
				: dtModified.format(TimeTools.Formatter_DateTime_M));

		/*
		 * merge from tour ID
		 */
		final Long mergeFromTourId = _tourData.getMergeSourceTourId();
		if (mergeFromTourId == null) {
			_txtMergeFromTourId.setText(UI.EMPTY_STRING);
		} else {
			_txtMergeFromTourId.setText(Long.toString(mergeFromTourId));
		}

		/*
		 * merge into tour ID
		 */
		final Long mergeIntoTourId = _tourData.getMergeTargetTourId();
		if (mergeIntoTourId == null) {
			_txtMergeIntoTourId.setText(UI.EMPTY_STRING);
		} else {
			_txtMergeIntoTourId.setText(Long.toString(mergeIntoTourId));
		}

		/*
		 * layout container to resize the labels
		 */
		onResize();
	}

	private void updateUI_RefTourInfo(final Collection<TourReference> refTours) {

		final ArrayList<TourReference> refTourList = new ArrayList<>(refTours);

		// sort reference tours by start index
		Collections.sort(refTourList, new Comparator<TourReference>() {
			@Override
			public int compare(final TourReference refTour1, final TourReference refTour2) {
				return refTour1.getStartValueIndex() - refTour2.getStartValueIndex();
			}
		});

		final StringBuilder sb = new StringBuilder();
		int refCounter = 0;

		for (final TourReference refTour : refTourList) {

			if (refCounter > 0) {
				sb.append(UI.NEW_LINE);
			}

			sb.append(refTour.getLabel());

			sb.append(" ("); //$NON-NLS-1$
			sb.append(refTour.getStartValueIndex());
			sb.append(UI.DASH_WITH_SPACE);
			sb.append(refTour.getEndValueIndex());
			sb.append(")"); //$NON-NLS-1$

			refCounter++;
		}

		_txtRefTour.setText(sb.toString());
//		_txtRefTour.pack(true);
		_txtRefTour.getParent().layout(true, true);
	}
}
