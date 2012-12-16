/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceView;
import net.tourbook.printing.ActionPrint;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourDoubleClickState;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionCollapseOthers;
import net.tourbook.ui.action.ActionComputeDistanceValuesFromGeoposition;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionExpandSelection;
import net.tourbook.ui.action.ActionJoinTours;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionRefreshView;
import net.tourbook.ui.action.ActionSetAltitudeValuesFromSRTM;
import net.tourbook.ui.action.ActionSetPerson;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.views.TourInfoToolTipCellLabelProvider;
import net.tourbook.ui.views.TourInfoToolTipStyledCellLabelProvider;
import net.tourbook.ui.views.TreeViewerTourInfoToolTip;
import net.tourbook.ui.views.rawData.ActionMergeTour;
import net.tourbook.ui.views.rawData.ActionReimportSubMenu;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class TourBookView extends ViewPart implements ITourProvider, ITourViewer3 {

	static public final String							ID									= "net.tourbook.views.tourListView";		//$NON-NLS-1$

	private static final String							STATE_SELECTED_YEAR					= "SelectedYear";							//$NON-NLS-1$

	private static final String							STATE_SELECTED_MONTH				= "SelectedMonth";							//$NON-NLS-1$
	private static final String							STATE_SELECTED_TOURS				= "SelectedTours";							//$NON-NLS-1$
	private static final String							STATE_IS_SELECT_YEAR_MONTH_TOURS	= "IsSelectYearMonthTours";				//$NON-NLS-1$
	private static final String							STATE_YEAR_SUB_CATEGORY				= "YearSubCategory";						//$NON-NLS-1$

	private static int									_yearSubCategory					= TourItem.ITEM_TYPE_MONTH;

	private final IPreferenceStore						_prefStore							= TourbookPlugin
																									.getDefault()
																									.getPreferenceStore();

	private final IDialogSettings						_state								= TourbookPlugin
																									.getDefault()
																									.getDialogSettingsSection(
																											ID);
	private ColumnManager								_columnManager;

	private PostSelectionProvider						_postSelectionProvider;

	private ISelectionListener							_postSelectionListener;
	private IPartListener2								_partListener;
	private ITourEventListener							_tourPropertyListener;
	private IPropertyChangeListener						_prefChangeListener;
	private TVITourBookRoot								_rootItem;

	private final NumberFormat							_nf1								= NumberFormat
																									.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	private final Calendar								_calendar							= GregorianCalendar
																									.getInstance();

	private final DateFormat							_timeFormatter						= DateFormat
																									.getTimeInstance(DateFormat.SHORT);
	private static final String[]						_weekDays							= DateFormatSymbols
																									.getInstance()
																									.getShortWeekdays();

	private int											_selectedYear						= -1;

	private int											_selectedYearSub					= -1;
	private final ArrayList<Long>						_selectedTourIds					= new ArrayList<Long>();
	private boolean										_isRecTimeFormat_hhmmss;

	private boolean										_isDriveTimeFormat_hhmmss;
	private boolean										_isToolTipInDate;

	private boolean										_isToolTipInTags;
	private boolean										_isToolTipInTime;
	private boolean										_isToolTipInTitle;
	private boolean										_isToolTipInWeekDay;

	private final TourDoubleClickState					_tourDoubleClickState				= new TourDoubleClickState();
	private TagMenuManager								_tagMenuMgr;
	private TreeViewerTourInfoToolTip					_tourInfoToolTip;

	/*
	 * UI controls
	 */
	private Composite									_viewerContainer;

	private TreeViewer									_tourViewer;
	private ActionEditQuick								_actionEditQuick;

	private ActionCollapseAll							_actionCollapseAll;

	private ActionCollapseOthers						_actionCollapseOthers;
	private ActionExpandSelection						_actionExpandSelection;
	private ActionDeleteTourMenu						_actionDeleteTour;

	private ActionEditTour								_actionEditTour;
	private ActionOpenTour								_actionOpenTour;
	private ActionOpenMarkerDialog						_actionOpenMarkerDialog;
	private ActionOpenAdjustAltitudeDialog				_actionOpenAdjustAltitudeDialog;
	private ActionMergeTour								_actionMergeTour;
	private ActionJoinTours								_actionJoinTours;
	private ActionComputeDistanceValuesFromGeoposition	_actionComputeDistanceValuesFromGeoposition;
	private ActionSetAltitudeValuesFromSRTM				_actionSetAltitudeFromSRTM;
	private ActionSetTourTypeMenu						_actionSetTourType;

	private ActionSelectAllTours						_actionSelectAllTours;
	private ActionYearSubCategorySelect					_actionYearSubCategorySelect;

	private ActionModifyColumns							_actionModifyColumns;
	private ActionRefreshView							_actionRefreshView;
	private ActionSetPerson								_actionSetOtherPerson;

	private ActionExport								_actionExportTour;
	private ActionReimportSubMenu						_actionReimportSubMenu;
	private ActionPrint									_actionPrintTour;

	private PixelConverter								_pc;

	private static class ItemComparer implements IElementComparer {

		@Override
		public boolean equals(final Object a, final Object b) {

			if (a == b) {
				return true;
			}

			if (a instanceof TVITourBookYear && b instanceof TVITourBookYear) {

				final TVITourBookYear item1 = (TVITourBookYear) a;
				final TVITourBookYear item2 = (TVITourBookYear) b;
				return item1.tourYear == item2.tourYear;
			}

			if (a instanceof TVITourBookYearSub && b instanceof TVITourBookYearSub) {

				final TVITourBookYearSub item1 = (TVITourBookYearSub) a;
				final TVITourBookYearSub item2 = (TVITourBookYearSub) b;
				return item1.tourYear == item2.tourYear && item1.tourYearSub == item2.tourYearSub;
			}

			if (a instanceof TVITourBookTour && b instanceof TVITourBookTour) {

				final TVITourBookTour item1 = (TVITourBookTour) a;
				final TVITourBookTour item2 = (TVITourBookTour) b;
				return item1.tourId == item2.tourId;
			}

			return false;
		}

		@Override
		public int hashCode(final Object element) {
			return 0;
		}
	}

	private class TourBookContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getChildren(final Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _rootItem.getFetchedChildrenAsArray();
		}

		@Override
		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		@Override
		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	void actionSelectYearMonthTours() {

		if (_actionSelectAllTours.isChecked()) {
			// reselect selection
			_tourViewer.setSelection(_tourViewer.getSelection());
		}
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourBookView.this) {
					saveState();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

//					fActivePerson = TourbookPlugin.getDefault().getActivePerson();
//					fActiveTourTypeFilter = TourbookPlugin.getDefault().getActiveTourTypeFilter();

					reloadViewer();

				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// update tourbook viewer
					_tourViewer.refresh();

					// redraw must be done to see modified tour type image colors
					_tourViewer.getTree().redraw();

				} else if (property.equals(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED)) {

					updateToolTipState();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

//					UI.updateUnits();

					_columnManager.saveState(_state);
					_columnManager.clearColumns();
					defineAllColumns(_viewerContainer);

					_tourViewer = (TreeViewer) recreateViewer(_tourViewer);

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					readDisplayFormats();

					_tourViewer.getTree().setLinesVisible(
							_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					_tourViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					_tourViewer.getTree().redraw();
				}
			}
		};

		// register the listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void addSelectionListener() {
		// this view part is a selection listener
		_postSelectionListener = new ISelectionListener() {

			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (selection instanceof SelectionDeletedTours) {
					reloadViewer();
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourPropertyListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (eventId == TourEventId.TOUR_CHANGED || eventId == TourEventId.UPDATE_UI) {

					/*
					 * it is possible when a tour type was modified, the tour can be hidden or
					 * visible in the viewer because of the tour type filter
					 */
					reloadViewer();

				} else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED
						|| eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

					reloadViewer();
				}
			}
		};
		TourManager.getInstance().addTourEventListener(_tourPropertyListener);
	}

	private void createActions() {

		_actionEditQuick = new ActionEditQuick(this);
		_actionEditTour = new ActionEditTour(this);
		_actionOpenTour = new ActionOpenTour(this);
		_actionDeleteTour = new ActionDeleteTourMenu(this);

		_actionOpenMarkerDialog = new ActionOpenMarkerDialog(this, true);
		_actionOpenAdjustAltitudeDialog = new ActionOpenAdjustAltitudeDialog(this);
		_actionMergeTour = new ActionMergeTour(this);
		_actionJoinTours = new ActionJoinTours(this);
		_actionComputeDistanceValuesFromGeoposition = new ActionComputeDistanceValuesFromGeoposition(this);
		_actionSetAltitudeFromSRTM = new ActionSetAltitudeValuesFromSRTM(this);
		_actionSetOtherPerson = new ActionSetPerson(this);

		_actionSetTourType = new ActionSetTourTypeMenu(this);

		_actionModifyColumns = new ActionModifyColumns(this);
		_actionSelectAllTours = new ActionSelectAllTours(this);
		_actionYearSubCategorySelect = new ActionYearSubCategorySelect(this);
		_actionRefreshView = new ActionRefreshView(this);

		_actionExpandSelection = new ActionExpandSelection(this);
		_actionCollapseAll = new ActionCollapseAll(this);
		_actionCollapseOthers = new ActionCollapseOthers(this);

		_actionExportTour = new ActionExport(this);
		_actionReimportSubMenu = new ActionReimportSubMenu(this);
		_actionPrintTour = new ActionPrint(this);

		_tagMenuMgr = new TagMenuManager(this, true);

		fillActionBars();
	}

	@Override
	public void createPartControl(final Composite parent) {

		_pc = new PixelConverter(parent);

		// define all columns for the viewer
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns(parent);

		createUI(parent);
		createActions();

		addSelectionListener();
		addPartListener();
		addPrefListener();
		addTourEventListener();

		// set selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());

		readDisplayFormats();
		restoreState();

		enableActions();

		// update the viewer
		_rootItem = new TVITourBookRoot(this);
		_tourViewer.setInput(this);

		reselectTourViewer();
	}

	private void createUI(final Composite parent) {

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
		{
			createUI10TourViewer(_viewerContainer);
		}
	}

	private void createUI10TourViewer(final Composite parent) {

		// tour tree
		final Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FLAT | SWT.FULL_SELECTION | SWT.MULTI);

		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tree.setHeaderVisible(true);
		tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		_tourViewer = new TreeViewer(tree);
		_columnManager.createColumns(_tourViewer);

		_tourViewer.setContentProvider(new TourBookContentProvider());
		_tourViewer.setComparer(new ItemComparer());
		_tourViewer.setUseHashlookup(true);

		_tourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectTreeItem(event);
			}
		});

		_tourViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) _tourViewer.getSelection()).getFirstElement();

				if (selection instanceof TVITourBookTour) {

					TourManager.getInstance().tourDoubleClickAction(TourBookView.this, _tourDoubleClickState);

				} else if (selection != null) {

					// expand/collapse current item

					final TreeViewerItem tourItem = (TreeViewerItem) selection;

					if (_tourViewer.getExpandedState(tourItem)) {
						_tourViewer.collapseToLevel(tourItem, 1);
					} else {
						_tourViewer.expandToLevel(tourItem, 1);
					}
				}
			}
		});

		/*
		 * the context menu must be created after the viewer is created which is also done after the
		 * measurement system has changed
		 */
		createUI20ContextMenu();

		// set tour info tooltip provider
		_tourInfoToolTip = new TreeViewerTourInfoToolTip(_tourViewer);
	}

	/**
	 * create the views context menu
	 */
	private void createUI20ContextMenu() {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		final Tree tree = (Tree) _tourViewer.getControl();
		final Menu treeContextMenu = menuMgr.createContextMenu(tree);
		treeContextMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuHidden(final MenuEvent e) {
				_tagMenuMgr.onHideMenu();
			}

			@Override
			public void menuShown(final MenuEvent menuEvent) {
				_tagMenuMgr.onShowMenu(menuEvent, tree, Display.getCurrent().getCursorLocation(), _tourInfoToolTip);
			}
		});

		_columnManager.createHeaderContextMenu(tree, treeContextMenu);
	}

	/**
	 * Defines all columns for the table viewer in the column manager, the sequence defines the
	 * default columns
	 * 
	 * @param parent
	 */
	private void defineAllColumns(final Composite parent) {

		defineColumnDate();
		defineColumnWeekDay();
		defineColumnTime();
		defineColumnTourTypeImage();
		defineColumnTourTypeText();

		defineColumnDistance();
		defineColumnAltitudeUp();
		defineColumnTimeDriving();

		defineColumnWeatherClouds();
		defineColumnTitle();
		defineColumnTags();

		defineColumnMarker();
		defineColumnCalories();
		defineColumnRestPulse();

		defineColumnTimeRecording();
		defineColumnTimeBreak();
		defineColumnTimeBreakRelative();

		defineColumnAltitudeDown();

		defineColumnMaxAltitude();
		defineColumnMaxSpeed();
		defineColumnMaxPulse();

		defineColumnAvgSpeed();
		defineColumnAvgPace();
		defineColumnAvgPulse();
		defineColumnAvgCadence();
		defineColumnAvgTemperature();

		defineColumnWeatherWindSpeed();
		defineColumnWeatherWindDirection();

		defineColumnWeekNo();
		defineColumnWeekYear();
		defineColumnTimeInterval();
//		defineColumnNumberOfTimeSlices();
		defineColumnDeviceDistance();

		defineColumnPerson();
	}

	/**
	 * column: altitude down (m)
	 */
	private void defineColumnAltitudeDown() {

		final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_DOWN.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long dbAltitudeDown = ((TVITourBookItem) element).colAltitudeDown;

				if (dbAltitudeDown == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Long.toString((long) (-dbAltitudeDown / UI.UNIT_VALUE_ALTITUDE)));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: altitude up (m)
	 */
	private void defineColumnAltitudeUp() {

		final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_UP.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long dbAltitudeUp = ((TVITourBookItem) element).colAltitudeUp;

				if (dbAltitudeUp == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Long.toString((long) (dbAltitudeUp / UI.UNIT_VALUE_ALTITUDE)));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: avg cadence
	 */
	private void defineColumnAvgCadence() {

		final TreeColumnDefinition colDef = TreeColumnFactory.AVG_CADENCE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long dbAvgCadence = ((TVITourBookItem) element).colAvgCadence;

				if (dbAvgCadence == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Long.toString(dbAvgCadence));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: avg pace min/km - min/mi
	 */
	private void defineColumnAvgPace() {

		final TreeColumnDefinition colDef = TreeColumnFactory.AVG_PACE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final float pace = ((TVITourBookItem) element).colAvgPace * UI.UNIT_VALUE_DISTANCE;

				if (pace == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(UI.format_mm_ss((long) pace));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: avg pulse
	 */
	private void defineColumnAvgPulse() {

		final TreeColumnDefinition colDef = TreeColumnFactory.AVG_PULSE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long dbAvgPulse = ((TVITourBookItem) element).colAvgPulse;

				if (dbAvgPulse == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Long.toString(dbAvgPulse));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: avg speed km/h - mph
	 */
	private void defineColumnAvgSpeed() {

		final TreeColumnDefinition colDef = TreeColumnFactory.AVG_SPEED.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				final float speed = ((TVITourBookItem) element).colAvgSpeed / UI.UNIT_VALUE_DISTANCE;
				if (speed == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf1.format(speed));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: avg temperature
	 */
	private void defineColumnAvgTemperature() {

		final TreeColumnDefinition colDef = TreeColumnFactory.AVG_TEMPERATURE.createColumn(_columnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				float temperature = ((TVITourBookItem) element).colAvgTemperature;

				if (temperature == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {

					if (UI.UNIT_VALUE_TEMPERATURE != 1) {
						temperature = temperature * UI.UNIT_FAHRENHEIT_MULTI + UI.UNIT_FAHRENHEIT_ADD;
					}

					cell.setText(_nf1.format(temperature));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: calories
	 */
	private void defineColumnCalories() {

		final TreeColumnDefinition colDef = TreeColumnFactory.CALORIES.createColumn(_columnManager, _pc);
		//colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long caloriesSum = ((TVITourBookItem) element).colCalories;

				cell.setText(Long.toString(caloriesSum));

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * tree column: date
	 */
	private void defineColumnDate() {

		final TreeColumnDefinition colDef = TreeColumnFactory.DATE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new TourInfoToolTipStyledCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInDate == false) {
					return null;
				}

				final Object element = cell.getElement();
				if ((element instanceof TVITourBookTour)) {
					return ((TVITourBookItem) element).getTourId();
				}

				return null;
			}

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final TVITourBookItem tourItem = (TVITourBookItem) element;

				if ((element instanceof TVITourBookTour)) {

					// tour item
					cell.setText(tourItem.treeColumn);

				} else {

					// year/month item
					final StyledString styledString = new StyledString();
					styledString.append(tourItem.treeColumn);
					styledString.append("   " + tourItem.colCounter, StyledString.QUALIFIER_STYLER); //$NON-NLS-1$

					if (tourItem instanceof TVITourBookYearSub) {
						cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_SUB_SUB));
					} else {
						cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_SUB));
					}
					cell.setText(styledString.getString());
					cell.setStyleRanges(styledString.getStyleRanges());
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: device distance
	 */
	private void defineColumnDeviceDistance() {

		final TreeColumnDefinition colDef = TreeColumnFactory.DEVICE_DISTANCE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					final long dbStartDistance = ((TVITourBookTour) element).getColumnStartDistance();

					if (dbStartDistance == 0) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						cell.setText(Long.toString((long) (dbStartDistance / UI.UNIT_VALUE_DISTANCE)));
					}

					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: distance (km/miles)
	 */
	private void defineColumnDistance() {

		final TreeColumnDefinition colDef = TreeColumnFactory.DISTANCE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final float dbDistance = ((TVITourBookItem) element).colDistance;

				if (dbDistance == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf1.format(dbDistance / 1000 / UI.UNIT_VALUE_DISTANCE));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: markers
	 */
	private void defineColumnMarker() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_MARKERS.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					final ArrayList<Long> markerIds = ((TVITourBookTour) element).getMarkerIds();
					if (markerIds == null) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						cell.setText(Integer.toString(markerIds.size()));
					}

					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: max altitude
	 */
	private void defineColumnMaxAltitude() {

		final TreeColumnDefinition colDef = TreeColumnFactory.MAX_ALTITUDE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long dbMaxAltitude = ((TVITourBookItem) element).colMaxAltitude;

				if (dbMaxAltitude == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Long.toString((long) (dbMaxAltitude / UI.UNIT_VALUE_ALTITUDE)));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: max pulse
	 */
	private void defineColumnMaxPulse() {

		final TreeColumnDefinition colDef = TreeColumnFactory.MAX_PULSE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long dbMaxPulse = ((TVITourBookItem) element).colMaxPulse;

				if (dbMaxPulse == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Long.toString(dbMaxPulse));
				}
				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: max speed
	 */
	private void defineColumnMaxSpeed() {

		final TreeColumnDefinition colDef = TreeColumnFactory.MAX_SPEED.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final float dbMaxSpeed = ((TVITourBookItem) element).colMaxSpeed;

				if (dbMaxSpeed == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf1.format(dbMaxSpeed / UI.UNIT_VALUE_DISTANCE));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: person
	 */
	private void defineColumnPerson() {

		final TreeColumnDefinition colDef = TreeColumnFactory.PERSON.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					final long dbPersonId = ((TVITourBookTour) element).colPersonId;

					cell.setText(PersonManager.getPersonName(dbPersonId));

//					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: rest pulse
	 */
	private void defineColumnRestPulse() {

		final TreeColumnDefinition colDef = TreeColumnFactory.RESTPULSE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final int restPulse = ((TVITourBookItem) element).colRestPulse;

				if (restPulse == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Integer.toString(restPulse));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: tags
	 */
	private void defineColumnTags() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TAGS.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInTags == false) {
					return null;
				}

				final Object element = cell.getElement();
				if ((element instanceof TVITourBookTour)) {
					return ((TVITourBookTour) element).getTourId();
				}

				return null;
			}

			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					cell.setText(TourDatabase.getTagNames(((TVITourBookTour) element).getTagIds()));
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: time
	 */
	private void defineColumnTime() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_START_TIME //
				.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInTime == false) {
					return null;
				}

				final Object element = cell.getElement();
				if ((element instanceof TVITourBookTour)) {
					return ((TVITourBookTour) element).getTourId();
				}

				return null;
			}

			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					final long tourDate = ((TVITourBookTour) element).colTourDate;
					_calendar.setTimeInMillis(tourDate);

					cell.setText(_timeFormatter.format(_calendar.getTime()));
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: paused time (h)
	 */
	private void defineColumnTimeBreak() {

		final TreeColumnDefinition colDef = TreeColumnFactory.PAUSED_TIME.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				/*
				 * display paused time relative to the recording time
				 */

				final Object element = cell.getElement();
				final TVITourBookItem item = (TVITourBookItem) element;

				final long dbPausedTime = item.colPausedTime;

				if (dbPausedTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					if (_isDriveTimeFormat_hhmmss) {
						cell.setText(UI.format_hh_mm_ss(dbPausedTime).toString());
					} else {
						cell.setText(UI.format_hh_mm(dbPausedTime + 30).toString());
					}
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: relative paused time %
	 */
	private void defineColumnTimeBreakRelative() {

		final TreeColumnDefinition colDef = TreeColumnFactory.PAUSED_TIME_RELATIVE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				/*
				 * display paused time relative to the recording time
				 */

				final Object element = cell.getElement();
				final TVITourBookItem item = (TVITourBookItem) element;

				final long dbPausedTime = item.colPausedTime;
				final long dbRecordingTime = item.colRecordingTime;

				final float relativePausedTime = dbRecordingTime == 0 ? 0 : (float) dbPausedTime
						/ dbRecordingTime
						* 100;

				cell.setText(_nf1.format(relativePausedTime));

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: driving time (h)
	 */
	private void defineColumnTimeDriving() {

		final TreeColumnDefinition colDef = TreeColumnFactory.DRIVING_TIME.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long drivingTime = ((TVITourBookItem) element).colDrivingTime;

				if (element instanceof TVITourBookTour) {
					if (_isDriveTimeFormat_hhmmss) {
						cell.setText(UI.format_hh_mm_ss(drivingTime).toString());
					} else {
						cell.setText(UI.format_hh_mm(drivingTime + 30).toString());
					}
				} else {
					cell.setText(UI.format_hh_mm(drivingTime + 30).toString());
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: timeinterval
	 */

	private void defineColumnTimeInterval() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TIME_INTERVAL.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					final short dbTimeInterval = ((TVITourBookTour) element).getColumnTimeInterval();
					if (dbTimeInterval == 0) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						cell.setText(Long.toString(dbTimeInterval));
					}

					setCellColor(cell, element);
				}
			}
		});
	}

//	/**
//	 * column: number of time slices
//	 */
//
//	private void defineColumnNumberOfTimeSlices() {
//
//		final TreeColumnDefinition colDef = TreeColumnFactory.TIME_SLICES.createColumn(_columnManager, _pc);
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//				final Object element = cell.getElement();
//				if (element instanceof TVITourBookTour) {
//
//					final short dbTimeSlices = ((TVITourBookTour) element).getColumnTimeSlices();
//					if (dbTimeSlices == 0) {
//						cell.setText(UI.EMPTY_STRING);
//					} else {
//						cell.setText(Long.toString(dbTimeSlices));
//					}
//
//					setCellColor(cell, element);
//				}
//			}
//		});
//	}

	/**
	 * column: recording time (h)
	 */
	private void defineColumnTimeRecording() {

		final TreeColumnDefinition colDef = TreeColumnFactory.RECORDING_TIME.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long recordingTime = ((TVITourBookItem) element).colRecordingTime;

				if (element instanceof TVITourBookTour) {
					if (_isRecTimeFormat_hhmmss) {
						cell.setText(UI.format_hh_mm_ss(recordingTime).toString());
					} else {
						cell.setText(UI.format_hh_mm(recordingTime + 30).toString());
					}
				} else {
					cell.setText(UI.format_hh_mm(recordingTime + 30).toString());
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: title
	 */
	private void defineColumnTitle() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TITLE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInTitle == false) {
					return null;
				}

				final Object element = cell.getElement();
				if ((element instanceof TVITourBookTour)) {
					return ((TVITourBookTour) element).getTourId();
				}

				return null;
			}

			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					cell.setText(((TVITourBookTour) element).colTourTitle);
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: tour type image
	 */
	private void defineColumnTourTypeImage() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TYPE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					final long tourTypeId = ((TVITourBookTour) element).getTourTypeId();
					final Image tourTypeImage = UI.getInstance().getTourTypeImage(tourTypeId);

					/*
					 * when a tour type image is modified, it will keep the same image resource only
					 * the content is modified but in the rawDataView the modified image is not
					 * displayed compared with the tourBookView which displays the correct image
					 */
					cell.setImage(tourTypeImage);
				}
			}
		});
	}

	/**
	 * column: tour type text
	 */
	private void defineColumnTourTypeText() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TYPE_TEXT.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					final long tourTypeId = ((TVITourBookTour) element).getTourTypeId();
					cell.setText(UI.getInstance().getTourTypeLabel(tourTypeId));
				}
			}
		});
	}

	/**
	 * column: clouds
	 */
	private void defineColumnWeatherClouds() {

		final TreeColumnDefinition colDef = TreeColumnFactory.CLOUDS.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final String windClouds = ((TVITourBookItem) element).colClouds;

				if (windClouds == null) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					//cell.setText(windClouds);
					final Image img = net.tourbook.common.UI.IMAGE_REGISTRY.get(windClouds);
					if (img != null) {
						cell.setImage(img);
					} else {
						cell.setText(windClouds);
					}
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: wind direction
	 */
	private void defineColumnWeatherWindDirection() {

		final TreeColumnDefinition colDef = TreeColumnFactory.WIND_DIR.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final int windDir = ((TVITourBookItem) element).colWindDir;

				if (windDir == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Integer.toString(windDir));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: weather
	 */
	private void defineColumnWeatherWindSpeed() {

		final TreeColumnDefinition colDef = TreeColumnFactory.WIND_SPEED.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final int windSpeed = (int) (((TVITourBookItem) element).colWindSpd / UI.UNIT_VALUE_DISTANCE);

				if (windSpeed == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Integer.toString(windSpeed));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: week day
	 */
	private void defineColumnWeekDay() {

		final TreeColumnDefinition colDef = TreeColumnFactory.WEEK_DAY.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInWeekDay == false) {
					return null;
				}

				final Object element = cell.getElement();
				if ((element instanceof TVITourBookTour)) {
					return ((TVITourBookTour) element).getTourId();
				}

				return null;
			}

			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					final int weekDay = ((TVITourBookTour) element).colWeekDay;

					cell.setText(_weekDays[weekDay]);
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: week
	 */
	private void defineColumnWeekNo() {

		final TreeColumnDefinition colDef = TreeColumnFactory.WEEK_NO.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final int week = ((TVITourBookItem) element).colWeekNo;

				if (week == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Integer.toString(week));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: week year
	 */
	private void defineColumnWeekYear() {

		final TreeColumnDefinition colDef = TreeColumnFactory.WEEKYEAR.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final int week = ((TVITourBookItem) element).colWeekYear;

				if (week == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Integer.toString(week));
				}

				setCellColor(cell, element);
			}
		});
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);
		TourManager.getInstance().removeTourEventListener(_tourPropertyListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void enableActions() {

		final ITreeSelection selection = (ITreeSelection) _tourViewer.getSelection();

		/*
		 * count number of selected items
		 */
		int tourItems = 0;

		TVITourBookTour firstTour = null;

		for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			final Object treeItem = iter.next();
			if (treeItem instanceof TVITourBookTour) {
				if (tourItems == 0) {
					firstTour = (TVITourBookTour) treeItem;
				}
				tourItems++;
			}
		}

		final int selectedItems = selection.size();
		final boolean isTourSelected = tourItems > 0;
		final boolean isOneTour = tourItems == 1;
		boolean isDeviceTour = false;

		final TVITourBookItem firstElement = (TVITourBookItem) selection.getFirstElement();
		final boolean firstElementHasChildren = firstElement == null ? false : firstElement.hasChildren();
		TourData firstSavedTour = null;

		if (isOneTour) {
			firstSavedTour = TourManager.getInstance().getTourData(firstTour.getTourId());
			isDeviceTour = firstSavedTour.isManualTour() == false;
		}

		/*
		 * enable actions
		 */
		_tourDoubleClickState.canEditTour = isOneTour;
		_tourDoubleClickState.canOpenTour = isOneTour;
		_tourDoubleClickState.canQuickEditTour = isOneTour;
		_tourDoubleClickState.canEditMarker = isOneTour;
		_tourDoubleClickState.canAdjustAltitude = isOneTour;

		_actionEditTour.setEnabled(isOneTour);
		_actionOpenTour.setEnabled(isOneTour);
		_actionEditQuick.setEnabled(isOneTour);
		_actionOpenMarkerDialog.setEnabled(isOneTour && isDeviceTour);
		_actionOpenAdjustAltitudeDialog.setEnabled(isOneTour && isDeviceTour);

		_actionMergeTour.setEnabled(isOneTour && isDeviceTour && firstSavedTour.getMergeSourceTourId() != null);
		_actionComputeDistanceValuesFromGeoposition.setEnabled(isTourSelected);
		_actionSetAltitudeFromSRTM.setEnabled(isTourSelected);

		// enable delete ation when at least one tour is selected
		if (isTourSelected) {
			_actionDeleteTour.setEnabled(true);
		} else {
			_actionDeleteTour.setEnabled(false);
		}

		_actionJoinTours.setEnabled(tourItems > 1);
		_actionSetOtherPerson.setEnabled(isTourSelected);

		_actionExportTour.setEnabled(isTourSelected);
		_actionReimportSubMenu.setEnabled(isTourSelected);
		_actionPrintTour.setEnabled(isTourSelected);

		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
		_actionSetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);

		_actionExpandSelection.setEnabled(selection.size() == 0 ? false : true);

		_actionExpandSelection.setEnabled(firstElement == null ? false : //
				selectedItems == 1 ? firstElementHasChildren : //
						true);

		_actionCollapseOthers.setEnabled(selectedItems == 1 && firstElementHasChildren);

		_tagMenuMgr.enableTagActions(isTourSelected, isOneTour, firstTour == null ? null : firstTour.getTagIds());

		TourTypeMenuManager.enableRecentTourTypeActions(isTourSelected, isOneTour
				? firstTour.getTourTypeId()
				: TourDatabase.ENTITY_IS_NOT_SAVED);
	}

	private void fillActionBars() {

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(_actionSelectAllTours);
		menuMgr.add(_actionYearSubCategorySelect);
		menuMgr.add(new Separator());

		menuMgr.add(_actionModifyColumns);

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionExpandSelection);
		tbm.add(_actionCollapseAll);

		tbm.add(_actionRefreshView);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionEditQuick);
		menuMgr.add(_actionEditTour);
		menuMgr.add(_actionOpenMarkerDialog);
		menuMgr.add(_actionOpenAdjustAltitudeDialog);
		menuMgr.add(_actionOpenTour);
		menuMgr.add(_actionMergeTour);
		menuMgr.add(_actionJoinTours);
		menuMgr.add(_actionComputeDistanceValuesFromGeoposition);
		menuMgr.add(_actionSetAltitudeFromSRTM);

		_tagMenuMgr.fillTagMenu(menuMgr);

		// tour type actions
		menuMgr.add(new Separator());
		menuMgr.add(_actionSetTourType);
		TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, this, true);

		menuMgr.add(new Separator());
		menuMgr.add(_actionCollapseOthers);
		menuMgr.add(_actionExpandSelection);
		menuMgr.add(_actionCollapseAll);

		menuMgr.add(new Separator());
		menuMgr.add(_actionExportTour);
		menuMgr.add(_actionReimportSubMenu);
		menuMgr.add(_actionPrintTour);

		menuMgr.add(new Separator());
		menuMgr.add(_actionSetOtherPerson);
		menuMgr.add(_actionDeleteTour);

		enableActions();
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	public PostSelectionProvider getPostSelectionProvider() {
		return _postSelectionProvider;
	}

	private void getSelectedTourData(final ArrayList<TourData> selectedTourData, final Set<Long> tourIdSet) {
		for (final Long tourId : tourIdSet) {
			selectedTourData.add(TourManager.getInstance().getTourData(tourId));
		}
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		// get selected tours

		final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());
		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();
		final HashMap<Long, Long> tourIds = new HashMap<Long, Long>();

		if (selectedTours.size() < 2) {

			// one item is selected

			final Object selectedItem = selectedTours.getFirstElement();
			if (selectedItem instanceof TVITourBookYear) {

				// one year is selected

				if (_actionSelectAllTours.isChecked()) {

					// loop: all months
					for (final TreeViewerItem viewerItem : ((TVITourBookYear) selectedItem).getFetchedChildren()) {
						if (viewerItem instanceof TVITourBookYearSub) {
							getYearSubTourIDs((TVITourBookYearSub) viewerItem, tourIds);
						}
					}
				}

			} else if (selectedItem instanceof TVITourBookYearSub) {

				// one month/week is selected

				if (_actionSelectAllTours.isChecked()) {
					getYearSubTourIDs((TVITourBookYearSub) selectedItem, tourIds);
				}

			} else if (selectedItem instanceof TVITourBookTour) {

				// one tour is selected

				tourIds.put(((TVITourBookTour) selectedItem).getTourId(), null);
			}

		} else {

			// multiple items are selected

			// get all selected tours, ignore year and month items
			for (final Iterator<?> tourIterator = selectedTours.iterator(); tourIterator.hasNext();) {
				final Object viewItem = tourIterator.next();

				if (viewItem instanceof TVITourBookTour) {
					tourIds.put(((TVITourBookTour) viewItem).getTourId(), null);
				}
			}
		}

		/*
		 * show busyindicator when multiple tours needs to be retrieved from the database
		 */
		if (tourIds.size() > 1) {
			BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
				@Override
				public void run() {
					getSelectedTourData(selectedTourData, tourIds.keySet());
				}
			});
		} else {
			getSelectedTourData(selectedTourData, tourIds.keySet());
		}

		return selectedTourData;
	}

	@Override
	public ColumnViewer getViewer() {
		return _tourViewer;
	}

	public int getYearSub() {
		return _yearSubCategory;
	}

	/**
	 * @param yearSubItem
	 * @param tourIds
	 * @return Return all tours for one yearSubItem
	 */
	private void getYearSubTourIDs(final TVITourBookYearSub yearSubItem, final HashMap<Long, Long> tourIds) {

		// get all tours for the month item
		for (final TreeViewerItem viewerItem : yearSubItem.getFetchedChildren()) {
			if (viewerItem instanceof TVITourBookTour) {

				final TVITourBookTour tourItem = (TVITourBookTour) viewerItem;
				tourIds.put(tourItem.getTourId(), null);
			}
		}
	}

	private void onSelectTreeItem(final SelectionChangedEvent event) {

		final boolean isSelectAllChildren = _actionSelectAllTours.isChecked();

		final HashMap<Long, Long> tourIds = new HashMap<Long, Long>();

		boolean isFirstYear = true;
		boolean isFirstYearSub = true;
		boolean isFirstTour = true;

		final IStructuredSelection selectedTours = (IStructuredSelection) (event.getSelection());
		// loop: all selected items
		for (final Iterator<?> itemIterator = selectedTours.iterator(); itemIterator.hasNext();) {

			final Object treeItem = itemIterator.next();

			if (isSelectAllChildren) {

				// get ALL tours from all selected tree items (year/month/tour)

				if (treeItem instanceof TVITourBookYear) {

					// year is selected

					final TVITourBookYear yearItem = ((TVITourBookYear) treeItem);
					if (isFirstYear) {
						// keep selected year
						isFirstYear = false;
						_selectedYear = yearItem.tourYear;
					}

					// get all tours for the selected year
					for (final TreeViewerItem viewerItem : yearItem.getFetchedChildren()) {
						if (viewerItem instanceof TVITourBookYearSub) {
							getYearSubTourIDs((TVITourBookYearSub) viewerItem, tourIds);
						}
					}

				} else if (treeItem instanceof TVITourBookYearSub) {

					// month/week/day is selected

					final TVITourBookYearSub yearSubItem = (TVITourBookYearSub) treeItem;
					if (isFirstYearSub) {
						// keep selected year/month/week/day
						isFirstYearSub = false;
						_selectedYear = yearSubItem.tourYear;
						_selectedYearSub = yearSubItem.tourYearSub;
					}

					// get all tours for the selected month
					getYearSubTourIDs(yearSubItem, tourIds);

				} else if (treeItem instanceof TVITourBookTour) {

					// tour is selected

					final TVITourBookTour tourItem = (TVITourBookTour) treeItem;
					if (isFirstTour) {
						// keep selected tour
						isFirstTour = false;
						_selectedYear = tourItem.tourYear;
						_selectedYearSub = tourItem.tourYearSub;
					}

					tourIds.put(tourItem.getTourId(), null);
				}

			} else {

				// get only selected tours

				if (treeItem instanceof TVITourBookTour) {

					final TVITourBookTour tourItem = (TVITourBookTour) treeItem;

					if (isFirstTour) {
						// keep selected tour
						isFirstTour = false;
						_selectedYear = tourItem.tourYear;
						_selectedYearSub = tourItem.tourYearSub;
					}

					tourIds.put(tourItem.getTourId(), null);
				}
			}
		}

		ISelection selection;
		if (tourIds.size() == 0) {

			// fire selection that nothing is selected

			selection = new SelectionTourIds(new ArrayList<Long>());

		} else {

			// keep selected tour id's
			_selectedTourIds.clear();
			_selectedTourIds.addAll(tourIds.keySet());

			selection = tourIds.size() == 1 ? new SelectionTourId(_selectedTourIds.get(0)) : new SelectionTourIds(
					_selectedTourIds);

		}
		_postSelectionProvider.setSelection(selection);

		enableActions();
	}

	private void readDisplayFormats() {

		_isRecTimeFormat_hhmmss = _prefStore.getString(ITourbookPreferences.VIEW_LAYOUT_RECORDING_TIME_FORMAT).equals(
				PrefPageAppearanceView.VIEW_TIME_LAYOUT_HH_MM_SS);

		_isDriveTimeFormat_hhmmss = _prefStore.getString(ITourbookPreferences.VIEW_LAYOUT_DRIVING_TIME_FORMAT).equals(
				PrefPageAppearanceView.VIEW_TIME_LAYOUT_HH_MM_SS);
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			final Object[] expandedElements = _tourViewer.getExpandedElements();
			final ISelection selection = _tourViewer.getSelection();

			_tourViewer.getTree().dispose();

			createUI10TourViewer(_viewerContainer);
			_viewerContainer.layout();

			_tourViewer.setInput(_rootItem = new TVITourBookRoot(this));

			_tourViewer.setExpandedElements(expandedElements);
			_tourViewer.setSelection(selection);
		}
		_viewerContainer.setRedraw(true);

		return _tourViewer;
	}

	@Override
	public void reloadViewer() {

		final Tree tree = _tourViewer.getTree();
		tree.setRedraw(false);
		{
			final Object[] expandedElements = _tourViewer.getExpandedElements();
			final ISelection selection = _tourViewer.getSelection();

			_tourViewer.setInput(_rootItem = new TVITourBookRoot(this));

			_tourViewer.setExpandedElements(expandedElements);
			_tourViewer.setSelection(selection, true);
		}
		tree.setRedraw(true);
	}

	public void reopenFirstSelectedTour() {

		Object selection = ((IStructuredSelection) _tourViewer.getSelection()).getFirstElement();
		_selectedYear = -1;
		_selectedYearSub = -1;
		TVITourBookTour item = null;
		if (selection instanceof TVITourBookTour) {
			item = (TVITourBookTour) selection;
			_selectedYear = item.tourYear;
			if (getYearSub() == TVITourBookItem.ITEM_TYPE_WEEK) {
				_selectedYearSub = item.tourWeek;
			} else {
				_selectedYearSub = item.tourMonth;
			}
		}

		reloadViewer();
		reselectTourViewer();

		selection = ((IStructuredSelection) _tourViewer.getSelection()).getFirstElement();
		if (selection instanceof TVITourBookTour) {
			item = (TVITourBookTour) selection;
			_tourViewer.collapseAll();
			_tourViewer.expandToLevel(item, 0);
			_tourViewer.setSelection(new StructuredSelection(item), false);
		}
	}

	private void reselectTourViewer() {

		// find the old selected year/[month/week] in the new tour items
		TreeViewerItem reselectYearItem = null;
		TreeViewerItem reselectYearSubItem = null;
		final ArrayList<TreeViewerItem> reselectTourItems = new ArrayList<TreeViewerItem>();

		/*
		 * get the year/month/tour item in the data model
		 */
		final ArrayList<TreeViewerItem> yearItems = _rootItem.getChildren();
		for (final TreeViewerItem yearItem : yearItems) {

			final TVITourBookYear tourBookYear = ((TVITourBookYear) yearItem);
			if (tourBookYear.tourYear == _selectedYear) {

				reselectYearItem = yearItem;

				final Object[] yearSubItems = tourBookYear.getFetchedChildrenAsArray();
				for (final Object yearSub : yearSubItems) {

					final TVITourBookYearSub tourBookYearSub = ((TVITourBookYearSub) yearSub);
					if (tourBookYearSub.tourYearSub == _selectedYearSub) {

						reselectYearSubItem = tourBookYearSub;

						final Object[] tourItems = tourBookYearSub.getFetchedChildrenAsArray();
						for (final Object tourItem : tourItems) {

							final TVITourBookTour tourBookTour = ((TVITourBookTour) tourItem);
							final long treeTourId = tourBookTour.tourId;

							for (final Long tourId : _selectedTourIds) {
								if (treeTourId == tourId) {
									reselectTourItems.add(tourBookTour);
									break;
								}
							}
						}
						break;
					}
				}
				break;
			}
		}

		// select year/month/tour in the viewer
		if (reselectTourItems.size() > 0) {

			_tourViewer.setSelection(new StructuredSelection(reselectTourItems) {}, false);

		} else if (reselectYearSubItem != null) {

			_tourViewer.setSelection(new StructuredSelection(reselectYearSubItem) {}, false);

		} else if (reselectYearItem != null) {

			_tourViewer.setSelection(new StructuredSelection(reselectYearItem) {}, false);

		} else if (yearItems.size() > 0) {

			// the old year was not found, select the newest year

			final TreeViewerItem yearItem = yearItems.get(yearItems.size() - 1);

			_tourViewer.setSelection(new StructuredSelection(yearItem) {}, true);
		}

		// move the horizontal scrollbar to the left border
		final ScrollBar horizontalBar = _tourViewer.getTree().getHorizontalBar();
		if (horizontalBar != null) {
			horizontalBar.setSelection(0);
		}
	}

	private void restoreState() {

		// set tour viewer reselection data
		try {
			_selectedYear = _state.getInt(STATE_SELECTED_YEAR);
		} catch (final NumberFormatException e) {
			_selectedYear = -1;
		}

		try {
			_selectedYearSub = _state.getInt(STATE_SELECTED_MONTH);
		} catch (final NumberFormatException e) {
			_selectedYearSub = -1;
		}

		final String[] selectedTourIds = _state.getArray(STATE_SELECTED_TOURS);
		_selectedTourIds.clear();

		if (selectedTourIds != null) {
			for (final String tourId : selectedTourIds) {
				try {
					_selectedTourIds.add(Long.valueOf(tourId));
				} catch (final NumberFormatException e) {
					// ignore
				}
			}
		}

		_actionSelectAllTours.setChecked(_state.getBoolean(STATE_IS_SELECT_YEAR_MONTH_TOURS));

		_yearSubCategory = Util.getStateInt(_state, STATE_YEAR_SUB_CATEGORY, TourItem.ITEM_TYPE_MONTH);
		_actionYearSubCategorySelect.setSubCategoryChecked(_yearSubCategory);

		updateToolTipState();
	}

	private void saveState() {

		// save selection in the tour viewer
		_state.put(STATE_SELECTED_YEAR, _selectedYear);
		_state.put(STATE_SELECTED_MONTH, _selectedYearSub);

		// convert tour id's into string
		final ArrayList<String> selectedTourIds = new ArrayList<String>();
		for (final Long tourId : _selectedTourIds) {
			selectedTourIds.add(tourId.toString());
		}
		_state.put(STATE_SELECTED_TOURS, selectedTourIds.toArray(new String[selectedTourIds.size()]));

		// action: select tours for year/yearSub
		_state.put(STATE_IS_SELECT_YEAR_MONTH_TOURS, _actionSelectAllTours.isChecked());

		_state.put(STATE_YEAR_SUB_CATEGORY, _yearSubCategory);

		_columnManager.saveState(_state);
	}

	public void setActiveYear(final int activeYear) {
		_selectedYear = activeYear;
	}

	private void setCellColor(final ViewerCell cell, final Object element) {

		if (element instanceof TVITourBookYear) {
			cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_SUB));
		} else if (element instanceof TVITourBookYearSub) {
			cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_SUB_SUB));
//		} else if (element instanceof TVITourBookTour) {
//			cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_TOUR));
		}
	}

	@Override
	public void setFocus() {
		_tourViewer.getControl().setFocus();
	}

	public void setYearSub(final int tourItemType) {
		_yearSubCategory = tourItemType;
	}

	private void updateToolTipState() {

		_isToolTipInDate = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_DATE);
		_isToolTipInTime = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TIME);
		_isToolTipInWeekDay = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_WEEKDAY);
		_isToolTipInTitle = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TITLE);
		_isToolTipInTags = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TAGS);
	}

}
