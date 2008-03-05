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
package net.tourbook.ui.views;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import net.tourbook.Messages;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ActionModifyColumns;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.TableColumnDefinition;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.ViewPart;

// author: Wolfgang Schramm
// create: 24.08.2007

public class TourPropertiesView extends ViewPart implements ITourViewer {

	public static final String		ID							= "net.tourbook.views.TourPropertiesView";		//$NON-NLS-1$

	private static final String		MEMENTO_SELECTED_TAB		= "tourProperties.selectedTab";				//$NON-NLS-1$
	private static final String		MEMENTO_COLUMN_SORT_ORDER	= "tourProperties.column_sort_order";			//$NON-NLS-1$
	private static final String		MEMENTO_COLUMN_WIDTH		= "tourProperties.column_width";				//$NON-NLS-1$

	private static IMemento			fSessionMemento;

	private CTabFolder				fTabFolder;
	private Label					fLblDate;
	private Label					fLblStartTime;
	private Label					fLblRecordingTime;
	private Label					fLblDrivingTime;
	private Label					fLblDatapoints;
	private Label					fLblTourType;
	private Label					fLblDeviceName;

	private Text					fTextTitle;
	private Text					fTextStartLocation;
	private Text					fTextEndLocation;
	private Text					fTextDescription;

	private PostSelectionProvider	fPostSelectionProvider;
	private ISelectionListener		fPostSelectionListener;
	private IPartListener2			fPartListener;
	private IPropertyChangeListener	fPrefChangeListener;

	private TourData				fTourData;
	public Calendar					fCalendar					= GregorianCalendar.getInstance();
	private DateFormat				fTimeFormatter				= DateFormat.getTimeInstance(DateFormat.SHORT);
	private NumberFormat			fNumberFormatter			= NumberFormat.getNumberInstance();
	private DateFormat				fDurationFormatter			= DateFormat.getTimeInstance(DateFormat.SHORT,
																		Locale.GERMAN);

	private TourEditor				fTourEditor;
	private TourChart				fTourChart;

	private ITourPropertyListener	fTourPropertyListener;

	private ScrolledComposite		fScrolledContainer;
	private Composite				fContentContainer;

	private Composite				fTourDataContainer;
	private TableViewer				fDataViewer;
	private ColumnManager			fColumnManager;

	private class TourDataContentProvider implements IStructuredContentProvider {

		public TourDataContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {

			if (fTourData == null) {
				return new Object[0];
			}

			final int[] timeSerie = fTourData.timeSerie;
			final int[] distanceSerie = fTourData.getDistanceSerie();
			final int[] altitudeSerie = fTourData.getAltitudeSerie();
			final int[] temperatureSerie = fTourData.getTemperatureSerie();

			final int[] cadenceSerie = fTourData.cadenceSerie;
			final int[] pulseSerie = fTourData.pulseSerie;

			final double[] longitudeSerie = fTourData.longitudeSerie;
			final double[] latitudeSerie = fTourData.latitudeSerie;

			final int serieLength = timeSerie.length;

			TourElement[] dataElements = new TourElement[serieLength];
			TourElement tourElement;

			for (int serieIndex = 0; serieIndex < dataElements.length; serieIndex++) {

				dataElements[serieIndex] = tourElement = new TourElement();

				tourElement.sequence = serieIndex;

				tourElement.time = timeSerie[serieIndex];

				if (distanceSerie != null) {
					tourElement.distance = distanceSerie[serieIndex];
				}
				if (altitudeSerie != null) {
					tourElement.altitude = altitudeSerie[serieIndex];
				}
				if (temperatureSerie != null) {
					tourElement.temperature = temperatureSerie[serieIndex];
				}
				if (cadenceSerie != null) {
					tourElement.cadence = cadenceSerie[serieIndex];
				}
				if (pulseSerie != null) {
					tourElement.pulse = pulseSerie[serieIndex];
				}

				if (longitudeSerie != null) {
					tourElement.longitude = longitudeSerie[serieIndex];
					tourElement.latitude = latitudeSerie[serieIndex];
				}
			}

			return (Object[]) (dataElements);
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	private class TourElement {

		public int		sequence;

		public int		time;
		public int		distance;
		public int		altitude;
		public int		temperature;
		public int		cadence;
		public int		pulse;

		public double	longitude;
		public double	latitude;

	}

	private void addPartListener() {

		// set the part listener
		fPartListener = new IPartListener2() {
			public void partActivated(IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(IWorkbenchPartReference partRef) {}

			public void partClosed(IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					// keep settings for this part
					saveSettings();
				}
			}

			public void partDeactivated(IWorkbenchPartReference partRef) {}

			public void partHidden(IWorkbenchPartReference partRef) {}

			public void partInputChanged(IWorkbenchPartReference partRef) {}

			public void partOpened(IWorkbenchPartReference partRef) {}

			public void partVisible(IWorkbenchPartReference partRef) {}
		};
		// register the listener in the page
		getSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					saveViewerSettings(fSessionMemento);

					// dispose viewer
					Control[] children = fTourDataContainer.getChildren();
					for (int childIndex = 0; childIndex < children.length; childIndex++) {
						children[childIndex].dispose();
					}

					createDataViewer(fTourDataContainer);

					restoreViewerSettings(fSessionMemento);

					fTourDataContainer.layout();

					updateViewer();
				}
			}
		};
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

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

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			@SuppressWarnings("unchecked") //$NON-NLS-1$
			public void propertyChanged(int propertyId, Object propertyData) {

				if (propertyId == TourManager.TOUR_PROPERTY_TOUR_TYPE_CHANGED
						|| propertyId == TourManager.TOUR_PROPERTY_TOUR_TYPE_CHANGED_IN_EDITOR) {

					if (fTourData == null) {
						return;
					}

					// get modified tours
					ArrayList<TourData> modifiedTours = (ArrayList<TourData>) propertyData;
					final long tourId = fTourData.getTourId();

					for (TourData tourData : modifiedTours) {
						if (tourData.getTourId() == tourId) {

							updateTourProperties(tourData);
							return;
						}
					}
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	private void createActions() {

		ActionModifyColumns actionModifyColumns = new ActionModifyColumns(this);

		/*
		 * fill view menu
		 */
		IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(actionModifyColumns);
	}

	/**
	 * @param parent
	 */
	private void createDataViewer(final Composite parent) {

		// parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		// table
		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		fDataViewer = new TableViewer(table);

		// define and create all columns
		fColumnManager = new ColumnManager(this);
		createDataViewerColumns(parent);
		fColumnManager.createColumns();

		fDataViewer.setContentProvider(new TourDataContentProvider());

		fDataViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					fireSliderPosition(selection);
				}
			}
		});
	}

	private void createDataViewerColumns(Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);

		TableColumnDefinition colDef;

		/*
		 * 1. column will be hidden because the alignment for the first column is always to the left
		 */
		colDef = TableColumnFactory.FIRST_COLUMN.createColumn(fColumnManager, pixelConverter);
		colDef.setCanModifyVisibility(false);
		colDef.setIsColumnMoveable(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {}
		});

		/*
		 * column: #
		 */
		colDef = TableColumnFactory.SEQUENCE.createColumn(fColumnManager, pixelConverter);
		colDef.setCanModifyVisibility(false);
		colDef.setIsColumnMoveable(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {

				cell.setText(Integer.toString(((TourElement) cell.getElement()).sequence));

				cell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			}
		});

		/*
		 * column: time
		 */
		colDef = TableColumnFactory.TOUR_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(Integer.toString(((TourElement) cell.getElement()).time));
			}
		});

		/*
		 * column: distance
		 */
		colDef = TableColumnFactory.DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {

				final int distance = ((TourElement) cell.getElement()).distance;
				cell.setText(fNumberFormatter.format((float) distance / 1000));
			}
		});

		/*
		 * column: altitude
		 */
		colDef = TableColumnFactory.ALTITUDE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(Integer.toString(((TourElement) cell.getElement()).altitude));
			}
		});

		/*
		 * column: pulse
		 */
		colDef = TableColumnFactory.PULSE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(Integer.toString(((TourElement) cell.getElement()).pulse));
			}
		});

		/*
		 * column: temperature
		 */
		colDef = TableColumnFactory.TEMPERATURE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(Integer.toString(((TourElement) cell.getElement()).temperature));
			}
		});

		/*
		 * column: cadence
		 */
		colDef = TableColumnFactory.CADENCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(Integer.toString(((TourElement) cell.getElement()).cadence));
			}
		});

		/*
		 * column: longitude
		 */
		colDef = TableColumnFactory.LONGITUDE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(Double.toString(((TourElement) cell.getElement()).longitude));
			}
		});

		/*
		 * column: latitude
		 */
		colDef = TableColumnFactory.LATITUDE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(Double.toString(((TourElement) cell.getElement()).latitude));
			}
		});

	}

	@Override
	public void createPartControl(Composite parent) {

		createUI(parent);

		addSelectionListener();
		addPartListener();
		addTourPropertyListener();
		addPrefListener();

		createActions();

		// this part is a selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		restoreState(fSessionMemento);

		// show data from last selection
		onChangeSelection(getSite().getWorkbenchWindow().getSelectionService().getSelection());
	}

	private Composite createTabLocation(Composite parent) {

		Label label;
		GridData gd;
		final PixelConverter pixelConverter = new PixelConverter(parent);

		final ScrolledComposite scrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);

		scrolledContainer.setExpandVertical(true);
		scrolledContainer.setExpandHorizontal(true);
		final Composite locationContainer;

		locationContainer = new Composite(scrolledContainer, SWT.NONE);
		locationContainer.setLayout(new GridLayout(2, false));
		locationContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		scrolledContainer.setContent(locationContainer);
		scrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				scrolledContainer.setMinSize(locationContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		gd = new GridData(SWT.FILL, SWT.NONE, true, false);

		{
			// title
			label = new Label(locationContainer, SWT.NONE);
			label.setText(Messages.Tour_Properties_Label_tour_title);
			fTextTitle = new Text(locationContainer, SWT.BORDER);
			fTextTitle.setLayoutData(gd);
			fTextTitle.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					onChangeContent();
				}
			});

			// start location
			label = new Label(locationContainer, SWT.NONE);
			label.setText(Messages.Tour_Properties_Label_start_location);
			fTextStartLocation = new Text(locationContainer, SWT.BORDER);
			fTextStartLocation.setLayoutData(gd);
			fTextStartLocation.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					onChangeContent();
				}
			});

			// end location
			label = new Label(locationContainer, SWT.NONE);
			label.setText(Messages.Tour_Properties_Label_end_location);
			fTextEndLocation = new Text(locationContainer, SWT.BORDER);
			fTextEndLocation.setLayoutData(gd);
			fTextEndLocation.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					onChangeContent();
				}
			});

			// description
			label = new Label(locationContainer, SWT.NONE);
			label.setText(Messages.Tour_Properties_Label_description);
			label.setLayoutData(new GridData(SWT.NONE, SWT.TOP, false, false));
			fTextDescription = new Text(locationContainer, SWT.BORDER
					| SWT.WRAP
					| SWT.MULTI
					| SWT.V_SCROLL
					| SWT.H_SCROLL);
			gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.heightHint = pixelConverter.convertHeightInCharsToPixels(2);
			fTextDescription.setLayoutData(gd);
			fTextDescription.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					onChangeContent();
				}
			});
		}

		return scrolledContainer;
	}

	private Composite createTabTime(Composite parent) {

		Label label;

		fScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		fScrolledContainer.setExpandVertical(true);
		fScrolledContainer.setExpandHorizontal(true);

		fContentContainer = new Composite(fScrolledContainer, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).spacing(10, 5).applyTo(fContentContainer);
		GridDataFactory.fillDefaults().applyTo(fContentContainer);

		fScrolledContainer.setContent(fContentContainer);
		fScrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				fScrolledContainer.setMinSize(fContentContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		{
			// tour date
			label = new Label(fContentContainer, SWT.NONE);
			label.setText(Messages.Tour_Properties_Label_tour_date);
			fLblDate = new Label(fContentContainer, SWT.NONE);

			// start time
			label = new Label(fContentContainer, SWT.NONE);
			label.setText(Messages.Tour_Properties_Label_start_time);
			fLblStartTime = new Label(fContentContainer, SWT.NONE);

			// recording time
			label = new Label(fContentContainer, SWT.NONE);
			label.setText(Messages.Tour_Properties_Label_recording_time);
			fLblRecordingTime = new Label(fContentContainer, SWT.NONE);

			// driving time
			label = new Label(fContentContainer, SWT.NONE);
			label.setText(Messages.Tour_Properties_Label_driving_time);
			fLblDrivingTime = new Label(fContentContainer, SWT.NONE);

			// tour type
			label = new Label(fContentContainer, SWT.NONE);
			label.setText(Messages.Tour_Properties_Label_device_name);
			fLblDeviceName = new Label(fContentContainer, SWT.NONE);

			// tour type
			label = new Label(fContentContainer, SWT.NONE);
			label.setText(Messages.Tour_Properties_Label_tour_type);
			fLblTourType = new Label(fContentContainer, SWT.NONE);

			// data points
			label = new Label(fContentContainer, SWT.NONE);
			label.setText(Messages.Tour_Properties_Label_datapoints);
			fLblDatapoints = new Label(fContentContainer, SWT.NONE);
		}

		return fScrolledContainer;
	}

	private Control createTabTourData(Composite parent) {

		fTourDataContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(fTourDataContainer);

		createDataViewer(fTourDataContainer);

		return fTourDataContainer;
	}

	private void createUI(Composite parent) {

		fTabFolder = new CTabFolder(parent, SWT.FLAT | SWT.BOTTOM);

		CTabItem fTabItemLocation = new CTabItem(fTabFolder, SWT.FLAT);
		fTabItemLocation.setText(Messages.Tour_Properties_label_location);
		fTabItemLocation.setControl(createTabLocation(fTabFolder));

		CTabItem fTabItemTime = new CTabItem(fTabFolder, SWT.FLAT);
		fTabItemTime.setText(Messages.Tour_Properties_label_time);
		fTabItemTime.setControl(createTabTime(fTabFolder));

		CTabItem fTabItemTourData = new CTabItem(fTabFolder, SWT.FLAT);
		fTabItemTourData.setText(Messages.Tour_Properties_label_tour_data);
		fTabItemTourData.setControl(createTabTourData(fTabFolder));

	}

	@Override
	public void dispose() {

		final IWorkbenchPage page = getSite().getPage();

		page.removePostSelectionListener(fPostSelectionListener);
		page.removePartListener(fPartListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		TourManager.getInstance().removePropertyListener(fTourPropertyListener);

		super.dispose();
	}

	/**
	 * enable controls when the data are from an editor
	 */
	private void enableControls() {

		boolean isEditor = fTourEditor != null;

		fTextTitle.setEnabled(isEditor);
		fTextStartLocation.setEnabled(isEditor);
		fTextEndLocation.setEnabled(isEditor);
		fTextDescription.setEnabled(isEditor);
	}

	/**
	 * select the chart slider(s) according to the selected marker(s)
	 */
	private void fireSliderPosition(StructuredSelection selection) {

		if (fTourChart == null) {

			TourChart tourChart = TourManager.getInstance().getActiveTourChart();

			if (tourChart == null || tourChart.isDisposed()) {
				return;
			} else {
				fTourChart = tourChart;
			}
		}

		Object[] selectedData = selection.toArray();

		if (selectedData.length > 1) {

			// two or more data are selected, set the 2 sliders to the first and last selected data

			fPostSelectionProvider.setSelection(new SelectionChartXSliderPosition(fTourChart,
					((TourElement) selectedData[0]).sequence,
					((TourElement) selectedData[selectedData.length - 1]).sequence));

		} else if (selectedData.length > 0) {

			// one data is selected

			fPostSelectionProvider.setSelection(new SelectionChartXSliderPosition(fTourChart,
					((TourElement) selectedData[0]).sequence,
					SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION));
		}
	}

	@SuppressWarnings("unchecked") //$NON-NLS-1$
	@Override
	public Object getAdapter(Class adapter) {

		if (adapter == ColumnViewer.class) {
			return fDataViewer;
		}

		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public ColumnManager getColumnManager() {
		return fColumnManager;
	}

	public TreeViewer getTreeViewer() {
		return null;
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {

		super.init(site, memento);

		// set the session memento if it's not yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	private void onChangeContent() {

		if (fTourEditor == null) {
			return;
		}

		fTourEditor.setTourDirty();

		// set changed data
		fTourData.setTourTitle(fTextTitle.getText());
		fTourData.setTourStartPlace(fTextStartLocation.getText());
		fTourData.setTourEndPlace(fTextEndLocation.getText());
		fTourData.setTourDescription(fTextDescription.getText());

	}

	private void onChangeSelection(ISelection selection) {

		if (selection instanceof SelectionTourData) {

			final SelectionTourData selectionTourData = (SelectionTourData) selection;
			final TourData tourData = selectionTourData.getTourData();
			if (tourData == null) {
				fTourEditor = null;
				fTourChart = null;
			} else {

				final TourChart tourChart = selectionTourData.getTourChart();

				// prevent loading the same tour
				if (tourChart != null && fTourChart != null && fTourChart.getTourData() == tourData) {
					return;
				}

				fTourEditor = null;
				fTourChart = tourChart;
				updateTourProperties(tourData);
			}

		} else if (selection instanceof SelectionTourId) {

			SelectionTourId tourIdSelection = (SelectionTourId) selection;

			// don't reload the same tour
			if (fTourData != null) {
				if (fTourData.getTourId().equals(tourIdSelection.getTourId())) {
					return;
				}
			}

			final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

			if (tourData != null) {
				fTourEditor = null;
				fTourChart = null;
				updateTourProperties(tourData);
			}

		} else if (selection instanceof SelectionActiveEditor) {

			final IEditorPart editor = ((SelectionActiveEditor) selection).getEditor();

			if (editor instanceof TourEditor) {

				// prevent loading the same tour
				final TourEditor tourEditor = (TourEditor) editor;
				if (tourEditor == fTourEditor) {
					return;
				}

				fTourEditor = tourEditor;
				fTourChart = fTourEditor.getTourChart();
				updateTourProperties(fTourChart.getTourData());
			}
		}
	}

	private void restoreViewerSettings(IMemento memento) {

		if (memento == null) {
			return;
		}

		// restore table columns sort order
		final String mementoColumnSortOrderIds = memento.getString(MEMENTO_COLUMN_SORT_ORDER);
		if (mementoColumnSortOrderIds != null) {
			fColumnManager.orderColumns(StringToArrayConverter.convertStringToArray(mementoColumnSortOrderIds));
		}

		// restore column width
		final String mementoColumnWidth = memento.getString(MEMENTO_COLUMN_WIDTH);
		if (mementoColumnWidth != null) {
			fColumnManager.setColumnWidth(StringToArrayConverter.convertStringToArray(mementoColumnWidth));
		}

		// hide first column, this is a hack to align the "first" column to right
		fDataViewer.getTable().getColumn(0).setWidth(0);
	}

	private void restoreState(IMemento memento) {

		if (memento == null) {

			// memento is not set, set defaults

			fTabFolder.setSelection(0);

		} else {

			// restore from memento

			// select tab
			Integer selectedTab = memento.getInteger(MEMENTO_SELECTED_TAB);
			if (selectedTab != null) {
				fTabFolder.setSelection(selectedTab);
			} else {
				fTabFolder.setSelection(0);
			}

			restoreViewerSettings(memento);
		}
	}

	private void saveViewerSettings(IMemento memento) {

		if (memento == null) {
			return;
		}

		// save column sort order
		memento.putString(MEMENTO_COLUMN_SORT_ORDER,
				StringToArrayConverter.convertArrayToString(fColumnManager.getColumnIds()));

		// save columns width
		final String[] columnIdAndWidth = fColumnManager.getColumnIdAndWidth();
		if (columnIdAndWidth != null) {
			memento.putString(MEMENTO_COLUMN_WIDTH, StringToArrayConverter.convertArrayToString(columnIdAndWidth));
		}
	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("TourPropertiesView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	@Override
	public void saveState(IMemento memento) {

		// save selected tab
		memento.putInteger(MEMENTO_SELECTED_TAB, fTabFolder.getSelectionIndex());

		saveViewerSettings(memento);
	}

	@Override
	public void setFocus() {

	}

	private void updateTourProperties(TourData tourData) {

		enableControls();

		// keep reference
		fTourData = tourData;

		/*
		 * location: time
		 */
		// tour date
		fLblDate.setText(TourManager.getTourDate(tourData));
		fLblDate.pack(true);

		// start time
		fCalendar.set(0, 0, 0, tourData.getStartHour(), tourData.getStartMinute(), 0);
		fLblStartTime.setText(fTimeFormatter.format(fCalendar.getTime()));

		// recording time
		final int recordingTime = tourData.getTourRecordingTime();
		if (recordingTime == 0) {
			fLblRecordingTime.setText(""); //$NON-NLS-1$
		} else {
			fCalendar.set(0, 0, 0, recordingTime / 3600, ((recordingTime % 3600) / 60), ((recordingTime % 3600) % 60));

			fLblRecordingTime.setText(fDurationFormatter.format(fCalendar.getTime()));
		}

		// driving time
		final int drivingTime = tourData.getTourDrivingTime();
		if (drivingTime == 0) {
			fLblDrivingTime.setText(""); //$NON-NLS-1$
		} else {
			fCalendar.set(0, 0, 0, drivingTime / 3600, ((drivingTime % 3600) / 60), ((drivingTime % 3600) % 60));

			fLblDrivingTime.setText(fDurationFormatter.format(fCalendar.getTime()));
		}

		// data points
		final int[] timeSerie = tourData.timeSerie;
		if (timeSerie == null) {
			fLblDatapoints.setText(""); //$NON-NLS-1$
		} else {
			final int dataPoints = timeSerie.length;
			fLblDatapoints.setText(Integer.toString(dataPoints));
		}

		// tour type
		final TourType tourType = tourData.getTourType();
		if (tourType == null) {
			fLblTourType.setText(""); //$NON-NLS-1$
		} else {
			fLblTourType.setText(tourType.getName());
		}

		// device name
		fLblDeviceName.setText(tourData.getDeviceName());

		/*
		 * tab: location
		 */
		// tour title
		final String tourTitle = fTourData.getTourTitle();
		fTextTitle.setText(tourTitle);

		// start location
		final String startLocation = fTourData.getTourStartPlace();
		fTextStartLocation.setText(startLocation);

		// end location
		final String endLocation = fTourData.getTourEndPlace();
		fTextEndLocation.setText(endLocation);

		// description
		final String description = fTourData.getTourDescription();
		fTextDescription.setText(description);

		fScrolledContainer.setMinSize(fContentContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		fContentContainer.pack(true);

		/*
		 * tab: tour data
		 */
		updateViewer();
	}

	private void updateViewer() {

		// update the viewer
		fNumberFormatter.setMinimumFractionDigits(3);
		fNumberFormatter.setMaximumFractionDigits(3);

		fDataViewer.setInput(new Object());
	}

}
