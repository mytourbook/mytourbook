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
package net.tourbook.ui.views.tourBook;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.ActionEditQuick;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionNewTours;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.ActionCollapseAll;
import net.tourbook.ui.ActionCollapseOthers;
import net.tourbook.ui.ActionEditTour;
import net.tourbook.ui.ActionExpandSelection;
import net.tourbook.ui.ActionModifyColumns;
import net.tourbook.ui.ActionOpenPrefDialog;
import net.tourbook.ui.ActionRefreshView;
import net.tourbook.ui.ActionSetTourType;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ISelectedTours;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.TreeColumnDefinition;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
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
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.ViewPart;

public class TourBookView extends ViewPart implements ISelectedTours, ITourViewer {

	static public final String			ID											= "net.tourbook.views.tourListView";			//$NON-NLS-1$

	private static final String			MEMENTO_TOURVIEWER_SELECTED_YEAR			= "tourbook.view.tourviewer.selected-year";	//$NON-NLS-1$
	private static final String			MEMENTO_TOURVIEWER_SELECTED_MONTH			= "tourbook.view.tourviewer.selected-month";	//$NON-NLS-1$
	private static final String			MEMENTO_TOURVIEWER_SELECT_YEAR_MONTH_TOURS	= "tourbook.view.select-year-month-tours";		//$NON-NLS-1$

	private static IMemento				fSessionMemento;

	private TreeViewer					fTourViewer;

	private ColumnManager				fColumnManager;
	private PostSelectionProvider		fPostSelectionProvider;

	private ISelectionListener			fPostSelectionListener;
	private IPartListener2				fPartListener;
	private ITourPropertyListener		fTourPropertyListener;
	private IPropertyChangeListener		fPrefChangeListener;

	TVITourBookRoot						fRootItem;

	TourPerson							fActivePerson;
	TourTypeFilter						fActiveTourTypeFilter;

	public NumberFormat					fNF											= NumberFormat.getNumberInstance();

	private ActionEditQuick				fActionEditQuick;

	private ActionSetTourTag			fActionAddTag;
	private ActionCollapseAll			fActionCollapseAll;
	private ActionCollapseOthers		fActionCollapseOthers;
	private ActionDeleteTour			fActionDeleteTour;
	private ActionEditTour				fActionEditTour;
	private ActionExpandSelection		fActionExpandSelection;
	private ActionModifyColumns			fActionModifyColumns;
	private ActionOpenPrefDialog		fActionOpenTagPrefs;
	private ActionSetTourTag			fActionRemoveTag;
	private ActionRemoveAllTags			fActionRemoveAllTags;
	private ActionRefreshView			fActionRefreshView;
	private ActionSelectYearMonthTours	fActionSelectYearMonthTours;
	private ActionSetTourType			fActionSetTourType;

	private int							fTourViewerSelectedYear						= -1;
	private int							fTourViewerSelectedMonth					= -1;
	private Long						fActiveTourId;

	private Composite					fViewerContainer;

	private class ItemComparer implements IElementComparer {

		public boolean equals(final Object a, final Object b) {

			if (a == b) {
				return true;
			}

			if (a instanceof TVITourBookYear && b instanceof TVITourBookYear) {

				final TVITourBookYear item1 = (TVITourBookYear) a;
				final TVITourBookYear item2 = (TVITourBookYear) b;
				return item1.fTourYear == item2.fTourYear;
			}

			if (a instanceof TVITourBookMonth && b instanceof TVITourBookMonth) {

				final TVITourBookMonth item1 = (TVITourBookMonth) a;
				final TVITourBookMonth item2 = (TVITourBookMonth) b;
				return item1.fTourYear == item2.fTourYear && item1.fTourMonth == item2.fTourMonth;
			}

			if (a instanceof TVITourBookTour && b instanceof TVITourBookTour) {

				final TVITourBookTour item1 = (TVITourBookTour) a;
				final TVITourBookTour item2 = (TVITourBookTour) b;
				return item1.fTourId == item2.fTourId;
			}

			return false;
		}

		public int hashCode(final Object element) {
			return 0;
		}
	}

	private class TourBookContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
		}

		public Object[] getElements(final Object inputElement) {
			return fRootItem.getFetchedChildrenAsArray();
		}

		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	void actionSelectYearMonthTours() {

		if (fActionSelectYearMonthTours.isChecked()) {
			// reselect selection
			fTourViewer.setSelection(fTourViewer.getSelection());
		}
	}

	private void addPartListener() {

		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		final Preferences prefStore = TourbookPlugin.getDefault().getPluginPreferences();

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

					fActivePerson = TourbookPlugin.getDefault().getActivePerson();
					fActiveTourTypeFilter = TourbookPlugin.getDefault().getActiveTourTypeFilter();

					reloadViewer();

				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// update tourbook viewer
					fTourViewer.refresh();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					fColumnManager.saveState(fSessionMemento);

					fColumnManager.clearColumns();
					defineViewerColumns(fViewerContainer);

					recreateViewer();

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					fTourViewer.getTree()
							.setLinesVisible(prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					fTourViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					fTourViewer.getTree().redraw();
				}
			}
		};

		// register the listener
		prefStore.addPropertyChangeListener(fPrefChangeListener);
	}

	private void addSelectionListener() {
		// this view part is a selection listener
		fPostSelectionListener = new ISelectionListener() {

			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (selection instanceof SelectionNewTours || selection instanceof SelectionDeletedTours) {
					reloadViewer();
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(final IWorkbenchPart part, final int propertyId, final Object propertyData) {

				if (propertyId == TourManager.TOUR_PROPERTIES_CHANGED) {

					/*
					 * it is possible when a tour type was modified, the tour can be hidden or
					 * visible in the viewer because of the tour type filter
					 */
					reloadViewer();

				} else if (propertyId == TourManager.TAG_STRUCTURE_CHANGED) {

					reloadViewer();

				} else if (propertyId == TourManager.ALL_TOURS_ARE_MODIFIED) {

					reloadViewer();
				}
			}
		};
		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	private void createActions() {

		fActionEditQuick = new ActionEditQuick(this);
		fActionEditTour = new ActionEditTour(this);
		fActionDeleteTour = new ActionDeleteTour(this);
		fActionSetTourType = new ActionSetTourType(this);

		fActionAddTag = new ActionSetTourTag(this, true);
		fActionRemoveTag = new ActionSetTourTag(this, false);
		fActionRemoveAllTags = new ActionRemoveAllTags(this);

		fActionModifyColumns = new ActionModifyColumns(this);
		fActionSelectYearMonthTours = new ActionSelectYearMonthTours(this);
		fActionRefreshView = new ActionRefreshView(this);

		fActionExpandSelection = new ActionExpandSelection(this);
		fActionCollapseAll = new ActionCollapseAll(this);
		fActionCollapseOthers = new ActionCollapseOthers(this);

		fActionOpenTagPrefs = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);

		fillActions();
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		// add the context menu to the table viewer
		final Control tourViewer = fTourViewer.getControl();
		final Menu menu = menuMgr.createContextMenu(tourViewer);
		tourViewer.setMenu(menu);
	}

	@Override
	public void createPartControl(final Composite parent) {

		// define all columns for the viewer
		fColumnManager = new ColumnManager(this, fSessionMemento);
		defineViewerColumns(parent);

		fViewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);

		createTourViewer(fViewerContainer);

		createActions();

		// set selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		addSelectionListener();
		addPartListener();
		addPrefListener();
		addTourPropertyListener();

		final TourbookPlugin tourbookPlugin = TourbookPlugin.getDefault();
		fActivePerson = tourbookPlugin.getActivePerson();
		fActiveTourTypeFilter = tourbookPlugin.getActiveTourTypeFilter();

		restoreState(fSessionMemento);

		// update the viewer
		fRootItem = new TVITourBookRoot(this);
		fTourViewer.setInput(this);

		reselectTourViewer();
	}

	private Control createTourViewer(final Composite parent) {

		// tour tree
		final Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FLAT | SWT.FULL_SELECTION | SWT.MULTI);

		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tree.setHeaderVisible(true);
		tree.setLinesVisible(TourbookPlugin.getDefault()
				.getPluginPreferences()
				.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		fTourViewer = new TreeViewer(tree);
		fColumnManager.createColumns();

		fTourViewer.setContentProvider(new TourBookContentProvider());
		fTourViewer.setComparer(new ItemComparer());
		fTourViewer.setUseHashlookup(true);

		fTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectTreeItem(event);
			}
		});

		fTourViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) fTourViewer.getSelection()).getFirstElement();

				if (selection instanceof TVITourBookTour) {

					// open tour in editor

					final TVITourBookTour tourItem = (TVITourBookTour) selection;
					TourManager.getInstance().openTourInEditor(tourItem.getTourId());

				} else if (selection != null) {

					// expand/collapse current item

					final TreeViewerItem tourItem = (TreeViewerItem) selection;

					if (fTourViewer.getExpandedState(tourItem)) {
						fTourViewer.collapseToLevel(tourItem, 1);
					} else {
						fTourViewer.expandToLevel(tourItem, 1);
					}
				}
			}
		});

		/*
		 * the context menu must be created after the viewer is created which is also done after the
		 * measurement system has changed
		 */
		createContextMenu();

		return tree;
	}

	/**
	 * Defines all columns for the table viewer in the column manager
	 * 
	 * @param parent
	 */
	private void defineViewerColumns(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);
		TreeColumnDefinition colDef;

		/*
		 * tree column: date
		 */
		colDef = TreeColumnFactory.DATE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new StyledCellLabelProvider() {
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

					if (tourItem instanceof TVITourBookMonth) {
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

		/*
		 * column: tour type
		 */
		colDef = TreeColumnFactory.TOUR_TYPE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {
					cell.setImage(UI.getInstance().getTourTypeImage(((TVITourBookTour) element).getTourTypeId()));
				}
			}
		});

		/*
		 * column: title
		 */
		colDef = TreeColumnFactory.TITLE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {
					cell.setText(((TVITourBookTour) element).fTourTitle);

					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: tags
		 */
		colDef = TreeColumnFactory.TOUR_TAGS.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {
					cell.setText(TourDatabase.getTagNames(((TVITourBookTour) element).fTagIds));

					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: recording time (h)
		 */
		colDef = TreeColumnFactory.RECORDING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long recordingTime = ((TVITourBookItem) element).colRecordingTime;

				cell.setText(new Formatter().format(Messages.Format_hhmm,
						(recordingTime / 3600),
						((recordingTime % 3600) / 60)).toString());

				setCellColor(cell, element);
			}
		});

		/*
		 * column: driving time (h)
		 */
		colDef = TreeColumnFactory.DRIVING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long drivingTime = ((TVITourBookItem) element).colDrivingTime;

				cell.setText(new Formatter().format(Messages.Format_hhmm,
						(drivingTime / 3600),
						((drivingTime % 3600) / 60)).toString());

				setCellColor(cell, element);
			}
		});

		/*
		 * column: distance (km/miles)
		 */
		colDef = TreeColumnFactory.DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				final TVITourBookItem tourItem = (TVITourBookItem) element;
				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);
				cell.setText(fNF.format(((float) tourItem.colDistance) / 1000 / UI.UNIT_VALUE_DISTANCE));

				setCellColor(cell, element);
			}
		});

		/*
		 * column: altitude up (m)
		 */
		colDef = TreeColumnFactory.ALTITUDE_UP.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final TVITourBookItem tourItem = (TVITourBookItem) element;
				cell.setText(Long.toString((long) (tourItem.colAltitudeUp / UI.UNIT_VALUE_ALTITUDE)));

				setCellColor(cell, element);
			}
		});

		/*
		 * column: altitude down (m)
		 */
		colDef = TreeColumnFactory.ALTITUDE_DOWN.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				cell.setText(Long.toString((long) (((TVITourBookItem) element).colAltitudeDown / UI.UNIT_VALUE_ALTITUDE)));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: max altitude
		 */
		colDef = TreeColumnFactory.MAX_ALTITUDE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				cell.setText(Long.toString((long) (((TVITourBookItem) element).colMaxAltitude / UI.UNIT_VALUE_ALTITUDE)));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: max speed
		 */
		colDef = TreeColumnFactory.MAX_SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);

				cell.setText(fNF.format(((TVITourBookItem) element).colMaxSpeed / UI.UNIT_VALUE_DISTANCE));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: max pulse
		 */
		colDef = TreeColumnFactory.MAX_PULSE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				cell.setText(Long.toString(((TVITourBookItem) element).colMaxPulse));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: avg speed km/h - mph
		 */
		colDef = TreeColumnFactory.AVG_SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);

				cell.setText(fNF.format(((TVITourBookItem) element).colAvgSpeed / UI.UNIT_VALUE_DISTANCE));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: avg pulse
		 */
		colDef = TreeColumnFactory.AVG_PULSE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				cell.setText(Long.toString(((TVITourBookItem) element).colAvgPulse));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: avg cadence
		 */
		colDef = TreeColumnFactory.AVG_CADENCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				cell.setText(Long.toString(((TVITourBookItem) element).colAvgCadence));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: avg temperature
		 */
		colDef = TreeColumnFactory.AVG_TEMPERATURE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				long temperature = ((TVITourBookItem) element).colAvgTemperature;

				if (UI.UNIT_VALUE_TEMPERATURE != 1) {
					temperature = (long) (temperature * UI.UNIT_FAHRENHEIT_MULTI + UI.UNIT_FAHRENHEIT_ADD);
				}

				cell.setText(Long.toString(temperature));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: timeinterval
		 */
		colDef = TreeColumnFactory.TIME_INTERVAL.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					cell.setText(Long.toString(((TVITourBookTour) element).getColumnTimeInterval()));
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: device distance
		 */
		colDef = TreeColumnFactory.DEVICE_DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					cell.setText(Long.toString((long) (((TVITourBookTour) element).getColumnStartDistance() / UI.UNIT_VALUE_DISTANCE)));
					setCellColor(cell, element);
				}
			}
		});
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getViewSite().getPage().removePartListener(fPartListener);
		TourManager.getInstance().removePropertyListener(fTourPropertyListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		super.dispose();
	}

	private void enableActions() {

		final ITreeSelection selection = (ITreeSelection) fTourViewer.getSelection();

		/*
		 * count number of selected items
		 */
		int tourItems = 0;
		int items = 0;
		int otherItems = 0;
		TVITourBookTour firstTour = null;

		for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			final Object treeItem = iter.next();
			if (treeItem instanceof TVITourBookTour) {
				if (tourItems == 0) {
					firstTour = (TVITourBookTour) treeItem;
				}
				tourItems++;
			} else {
				otherItems++;
			}
			items++;
		}

		final int selectedItems = selection.size();
		final boolean isTourSelected = tourItems > 0;

		final TVITourBookItem firstElement = (TVITourBookItem) selection.getFirstElement();
		final boolean firstElementHasChildren = firstElement == null ? false : firstElement.hasChildren();

		/*
		 * enable actions
		 */

		fActionEditTour.setEnabled(tourItems == 1);
		fActionEditQuick.setEnabled(tourItems == 1);

		// enable delete ation when at least one tour is selected
		if (isTourSelected) {
			fActionDeleteTour.setEnabled(true);
		} else {
			fActionDeleteTour.setEnabled(false);
		}

		final ArrayList<TourType> tourTypes = TourDatabase.getTourTypes();
		fActionSetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);

		// add tag
		fActionAddTag.setEnabled(isTourSelected);

		// remove tags
		if (firstTour != null && tourItems == 1) {

			// one tour is selected

			final ArrayList<Long> tagIds = firstTour.fTagIds;
			if (tagIds != null && tagIds.size() > 0) {

				// at least one tag is within the tour

				fActionRemoveAllTags.setEnabled(true);
				fActionRemoveTag.setEnabled(true);
			} else {
				// tags are not available
				fActionRemoveAllTags.setEnabled(false);
				fActionRemoveTag.setEnabled(false);
			}
		} else {

			// multiple tours are selected

			fActionRemoveTag.setEnabled(isTourSelected);
			fActionRemoveAllTags.setEnabled(isTourSelected);
		}

		fActionExpandSelection.setEnabled(selection.size() == 0 ? false : true);

		fActionExpandSelection.setEnabled(firstElement == null ? false : //
				selectedItems == 1 ? firstElementHasChildren : //
						true);

		fActionCollapseOthers.setEnabled(selectedItems == 1 && firstElementHasChildren);

		// enable/disable actions for the recent tags
		TagManager.enableRecentTagActions(isTourSelected);
	}

	private void fillActions() {

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(fActionSelectYearMonthTours);
		menuMgr.add(new Separator());
		
		menuMgr.add(fActionModifyColumns);

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(fActionExpandSelection);
		tbm.add(fActionCollapseAll);

		tbm.add(fActionRefreshView);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(fActionCollapseOthers);
		menuMgr.add(fActionExpandSelection);
		menuMgr.add(fActionCollapseAll);

		menuMgr.add(new Separator());
		menuMgr.add(fActionEditQuick);
		menuMgr.add(fActionSetTourType);
		menuMgr.add(fActionEditTour);
		menuMgr.add(fActionDeleteTour);

		menuMgr.add(new Separator());
		menuMgr.add(fActionAddTag);
		menuMgr.add(fActionRemoveTag);
		menuMgr.add(fActionRemoveAllTags);

		TagManager.fillRecentTagsIntoMenu(menuMgr, this, true, true);

		menuMgr.add(new Separator());
		menuMgr.add(fActionOpenTagPrefs);

		enableActions();
	}

	void firePostSelection(final ISelection selection) {
		fPostSelectionProvider.setSelection(selection);
	}

	Long getActiveTourId() {
		return fActiveTourId;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(final Class adapter) {

		if (adapter == ColumnViewer.class) {
			return fTourViewer;
		}

		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public ColumnManager getColumnManager() {
		return fColumnManager;
	}

	/**
	 * @param monthItem
	 * @param tourIds
	 * @return Return all tours for one month
	 */
	private void getMonthTourIds(final TVITourBookMonth monthItem, final ArrayList<Long> tourIds) {

		// get all tours for the month item
		for (final TreeViewerItem viewerItem : monthItem.getFetchedChildren()) {
			if (viewerItem instanceof TVITourBookTour) {

				final TVITourBookTour tourItem = (TVITourBookTour) viewerItem;
				tourIds.add(tourItem.getTourId());
			}
		}
	}

	public ArrayList<TourData> getSelectedTours() {

		// get selected tours
		final IStructuredSelection selectedTours = ((IStructuredSelection) fTourViewer.getSelection());

		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();

		// loop: all selected tours
		for (final Iterator<?> iter = selectedTours.iterator(); iter.hasNext();) {

			final Object treeItem = iter.next();

			if (treeItem instanceof TVITourBookTour) {

				final TVITourBookTour tviTour = ((TVITourBookTour) treeItem);

				final TourData tourData = TourManager.getInstance().getTourData(tviTour.getTourId());

				if (tourData != null) {
					selectedTourData.add(tourData);
				}
			}
		}

		return selectedTourData;
	}

	public ColumnViewer getViewer() {
		return fTourViewer;
	}

	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {
		super.init(site, memento);

		// set the session memento if it's not yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	public boolean isFromTourEditor() {
		return false;
	}

	private void onSelectTreeItem(final SelectionChangedEvent event) {

		final IStructuredSelection selectedTours = (IStructuredSelection) (event.getSelection());
		if (selectedTours.size() < 2) {

			// one item is selected

			final Object selectedItem = selectedTours.getFirstElement();
			if (selectedItem instanceof TVITourBookYear) {

				// year is selected

				final TVITourBookYear yearItem = ((TVITourBookYear) selectedItem);
				fTourViewerSelectedYear = yearItem.fTourYear;

				if (fActionSelectYearMonthTours.isChecked()) {

					// get all tours for the selected year
					final ArrayList<Long> tourIds = new ArrayList<Long>();

					for (final TreeViewerItem viewerItem : yearItem.getFetchedChildren()) {
						if (viewerItem instanceof TVITourBookMonth) {
							getMonthTourIds((TVITourBookMonth) viewerItem, tourIds);
						}
					}

					if (tourIds.size() > 0) {
						fPostSelectionProvider.setSelection(new SelectionTourIds(tourIds));
					}
				}

			} else if (selectedItem instanceof TVITourBookMonth) {

				// month is selected

				final TVITourBookMonth monthItem = (TVITourBookMonth) selectedItem;
				fTourViewerSelectedYear = monthItem.fTourYear;
				fTourViewerSelectedMonth = monthItem.fTourMonth;

				if (fActionSelectYearMonthTours.isChecked()) {

					// get all tours for the selected month
					final ArrayList<Long> tourIds = new ArrayList<Long>();

					getMonthTourIds(monthItem, tourIds);
					if (tourIds.size() > 0) {
						fPostSelectionProvider.setSelection(new SelectionTourIds(tourIds));
					}
				}

			} else if (selectedItem instanceof TVITourBookTour) {

				// tour is selected

				final TVITourBookTour tourItem = (TVITourBookTour) selectedItem;

				fTourViewerSelectedYear = tourItem.fTourYear;
				fTourViewerSelectedMonth = tourItem.fTourMonth;
				fActiveTourId = tourItem.getTourId();

				fPostSelectionProvider.setSelection(new SelectionTourId(fActiveTourId));
			}

		} else {

			// multiple items are selected

			final ArrayList<Long> tourIds = new ArrayList<Long>();
			boolean isFirstTour = true;

			// get all selected tours
			for (final Iterator<?> tourIterator = selectedTours.iterator(); tourIterator.hasNext();) {

				final Object viewItem = tourIterator.next();
				if (viewItem instanceof TVITourBookTour) {

					final TVITourBookTour tourItem = (TVITourBookTour) viewItem;
					tourIds.add(tourItem.getTourId());

					if (isFirstTour) {

						isFirstTour = false;

						fTourViewerSelectedYear = tourItem.fTourYear;
						fTourViewerSelectedMonth = tourItem.fTourMonth;
						fActiveTourId = tourItem.getTourId();
					}
				}
			}

			fPostSelectionProvider.setSelection(new SelectionTourIds(tourIds));
		}

		enableActions();
	}

	public void recreateViewer() {

		fViewerContainer.setRedraw(false);
		{
			final Object[] expandedElements = fTourViewer.getExpandedElements();
			final ISelection selection = fTourViewer.getSelection();

			fTourViewer.getTree().dispose();

			createTourViewer(fViewerContainer);
			fViewerContainer.layout();

			fTourViewer.setInput(fRootItem = new TVITourBookRoot(this));

			fTourViewer.setExpandedElements(expandedElements);
			fTourViewer.setSelection(selection);
		}
		fViewerContainer.setRedraw(true);
	}

	public void reloadViewer() {

		final Tree tree = fTourViewer.getTree();
		tree.setRedraw(false);
		{
			final Object[] expandedElements = fTourViewer.getExpandedElements();
			final ISelection selection = fTourViewer.getSelection();

			fTourViewer.setInput(fRootItem = new TVITourBookRoot(this));

			fTourViewer.setExpandedElements(expandedElements);
			fTourViewer.setSelection(selection);
		}
		tree.setRedraw(true);
	}

	private void reselectTourViewer() {

		// find the old selected year/month in the new tour items
		TreeViewerItem newYearItem = null;
		TreeViewerItem newMonthItem = null;
		final ArrayList<TreeViewerItem> yearItems = fRootItem.getChildren();

		/*
		 * get the year and month item in the data model
		 */
		for (final TreeViewerItem yearItem : yearItems) {
			final TVITourBookYear tourBookYear = ((TVITourBookYear) yearItem);
			if (tourBookYear.fTourYear == fTourViewerSelectedYear) {
				newYearItem = yearItem;

				final Object[] monthItems = tourBookYear.getFetchedChildrenAsArray();
				for (final Object monthItem : monthItems) {
					final TVITourBookMonth tourBookMonth = ((TVITourBookMonth) monthItem);
					if (tourBookMonth.fTourMonth == fTourViewerSelectedMonth) {
						newMonthItem = tourBookMonth;
						break;
					}
				}
				break;
			}
		}

		// select year/month in the viewer
		if (newMonthItem != null) {

			fTourViewer.setSelection(new StructuredSelection(newMonthItem) {}, false);

		} else if (newYearItem != null) {

			fTourViewer.setSelection(new StructuredSelection(newYearItem) {}, false);

		} else if (yearItems.size() > 0) {

			// the old year was not found, select the newest year

			final TreeViewerItem yearItem = yearItems.get(yearItems.size() - 1);

			fTourViewer.setSelection(new StructuredSelection(yearItem) {}, true);
		}

		// move the horizontal scrollbar to the left border
		final ScrollBar horizontalBar = fTourViewer.getTree().getHorizontalBar();
		if (horizontalBar != null) {
			horizontalBar.setSelection(0);
		}
	}

	private void restoreState(final IMemento memento) {

		if (memento != null) {

			/*
			 * restore states from the memento
			 */

			// set tour viewer reselection data
			final Integer selectedYear = memento.getInteger(MEMENTO_TOURVIEWER_SELECTED_YEAR);
			final Integer selectedMonth = memento.getInteger(MEMENTO_TOURVIEWER_SELECTED_MONTH);
			fTourViewerSelectedYear = selectedYear == null ? -1 : selectedYear;
			fTourViewerSelectedMonth = selectedMonth == null ? -1 : selectedMonth;

			final Boolean mementoSelectYearMonth = memento.getBoolean(MEMENTO_TOURVIEWER_SELECT_YEAR_MONTH_TOURS);
			if (mementoSelectYearMonth != null) {
				fActionSelectYearMonthTours.setChecked(mementoSelectYearMonth);
			}

		} else {

			// default: uncheck action because it degrades performance
			fActionSelectYearMonthTours.setChecked(false);
		}
	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("DeviceImportView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	@Override
	public void saveState(final IMemento memento) {

		// save selection in the tour viewer
		memento.putInteger(MEMENTO_TOURVIEWER_SELECTED_YEAR, fTourViewerSelectedYear);
		memento.putInteger(MEMENTO_TOURVIEWER_SELECTED_MONTH, fTourViewerSelectedMonth);

		// action: select tours for year/month
		memento.putBoolean(MEMENTO_TOURVIEWER_SELECT_YEAR_MONTH_TOURS, fActionSelectYearMonthTours.isChecked());

		fColumnManager.saveState(memento);
	}

	public void setActiveYear(final int activeYear) {
		fTourViewerSelectedYear = activeYear;
	}

	private void setCellColor(final ViewerCell cell, final Object element) {

		if (element instanceof TVITourBookYear) {
			cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_SUB));
		} else if (element instanceof TVITourBookMonth) {
			cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_SUB_SUB));
		} else if (element instanceof TVITourBookTour) {
			cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_TOUR));
		}
	}

	@Override
	public void setFocus() {
		fTourViewer.getControl().setFocus();
	}

}
