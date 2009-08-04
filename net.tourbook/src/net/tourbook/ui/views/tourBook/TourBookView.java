/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.export.ActionExport;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceView;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.TreeViewerItem;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionCollapseOthers;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionExpandSelection;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.action.ActionOpenPrefDialog;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionRefreshView;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.views.rawData.ActionMergeTour;
import net.tourbook.util.ColumnManager;
import net.tourbook.util.ITourViewer;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.TreeColumnDefinition;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class TourBookView extends ViewPart implements ITourProvider, ITourViewer {

	static public final String				ID								= "net.tourbook.views.tourListView";			//$NON-NLS-1$

	private final IDialogSettings			fState							= TourbookPlugin.getDefault()
																					.getDialogSettingsSection(
																							"ViewTourBook");				//$NON-NLS-1$

	private static final String				STATE_SELECTED_YEAR				= "selectedYear";								//$NON-NLS-1$
	private static final String				STATE_SELECTED_MONTH			= "selectedMonth";								//$NON-NLS-1$
	private static final String				STATE_SELECTED_TOURS			= "selectedTours";								//$NON-NLS-1$

	private static final String				STATE_SELECT_YEAR_MONTH_TOURS	= "isSelectYearMonthTours";					//$NON-NLS-1$

	private TreeViewer						fTourViewer;

	private ColumnManager					fColumnManager;
	private PostSelectionProvider			fPostSelectionProvider;

	private ISelectionListener				fPostSelectionListener;
	private IPartListener2					fPartListener;
	private ITourEventListener				fTourPropertyListener;
	private IPropertyChangeListener			fPrefChangeListener;

	TVITourBookRoot							fRootItem;

	TourPerson								fActivePerson;
	TourTypeFilter							fActiveTourTypeFilter;

	private NumberFormat					fNF								= NumberFormat.getNumberInstance();
	private Calendar						fCalendar						= GregorianCalendar.getInstance();
	private DateFormat						fTimeFormatter					= DateFormat.getTimeInstance(DateFormat.SHORT);

	private ActionEditQuick					fActionEditQuick;

	private ActionCollapseAll				fActionCollapseAll;
	private ActionCollapseOthers			fActionCollapseOthers;
	private ActionExpandSelection			fActionExpandSelection;

	private ActionDeleteTour				fActionDeleteTour;
	private ActionEditTour					fActionEditTour;
	private ActionOpenTour					fActionOpenTour;
	private ActionOpenMarkerDialog			fActionOpenMarkerDialog;
	private ActionOpenAdjustAltitudeDialog	fActionOpenAdjustAltitudeDialog;
	private ActionMergeTour					fActionMergeTour;

	private ActionSetTourTypeMenu			fActionSetTourType;
	private ActionSetTourTag				fActionAddTag;
	private ActionSetTourTag				fActionRemoveTag;
	private ActionRemoveAllTags				fActionRemoveAllTags;
	private ActionOpenPrefDialog			fActionOpenTagPrefs;

	private ActionSelectAllTours			fActionSelectAllTours;
	private ActionModifyColumns				fActionModifyColumns;
	private ActionRefreshView				fActionRefreshView;

	private ActionExport					fActionExportTour;

	private int								fSelectedYear					= -1;
	private int								fSelectedMonth					= -1;
//	private long							fSelectedTourId;
	private ArrayList<Long>					fSelectedTourIds				= new ArrayList<Long>();						;

	private Composite						fViewerContainer;

	private boolean							fIsRecTimeFormat_hhmmss;
	private boolean							fIsDriveTimeFormat_hhmmss;

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

		if (fActionSelectAllTours.isChecked()) {
			// reselect selection
			fTourViewer.setSelection(fTourViewer.getSelection());
		}
	}

	private void addPartListener() {

		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourBookView.this) {

					saveState();

//					TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, TourBookView.this);
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

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

					// redraw must be done to see modified tour type image colors
					fTourViewer.getTree().redraw();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					fColumnManager.saveState(fState);
					fColumnManager.clearColumns();
					defineViewerColumns(fViewerContainer);

					fTourViewer = (TreeViewer) recreateViewer(fTourViewer);

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					readDisplayFormats();

					fTourViewer.getTree().setLinesVisible(
							prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

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

				if (selection instanceof SelectionDeletedTours) {
					reloadViewer();
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourEventListener() {

		fTourPropertyListener = new ITourEventListener() {
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
		TourManager.getInstance().addTourEventListener(fTourPropertyListener);
	}

	private void createActions() {

		fActionEditQuick = new ActionEditQuick(this);
		fActionEditTour = new ActionEditTour(this);
		fActionOpenTour = new ActionOpenTour(this);
		fActionDeleteTour = new ActionDeleteTour(this);

		fActionOpenMarkerDialog = new ActionOpenMarkerDialog(this, true);
		fActionOpenAdjustAltitudeDialog = new ActionOpenAdjustAltitudeDialog(this);
		fActionMergeTour = new ActionMergeTour(this);

		fActionSetTourType = new ActionSetTourTypeMenu(this);
		fActionAddTag = new ActionSetTourTag(this, true);
		fActionRemoveTag = new ActionSetTourTag(this, false);
		fActionRemoveAllTags = new ActionRemoveAllTags(this);

		fActionModifyColumns = new ActionModifyColumns(this);
		fActionSelectAllTours = new ActionSelectAllTours(this);
		fActionRefreshView = new ActionRefreshView(this);

		fActionExpandSelection = new ActionExpandSelection(this);
		fActionCollapseAll = new ActionCollapseAll(this);
		fActionCollapseOthers = new ActionCollapseOthers(this);

		fActionOpenTagPrefs = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);

		fActionExportTour = new ActionExport(this);

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
		fColumnManager = new ColumnManager(this, fState);
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
		addTourEventListener();

		final TourbookPlugin tourbookPlugin = TourbookPlugin.getDefault();
		fActivePerson = tourbookPlugin.getActivePerson();
		fActiveTourTypeFilter = tourbookPlugin.getActiveTourTypeFilter();

		readDisplayFormats();
		restoreState();

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
		tree.setLinesVisible(TourbookPlugin.getDefault().getPluginPreferences().getBoolean(
				ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		fTourViewer = new TreeViewer(tree);
		fColumnManager.createColumns(fTourViewer);

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
		 * column: time
		 */
		colDef = TreeColumnFactory.TOUR_START_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					final long tourDate = ((TVITourBookTour) element).fTourDate;
					fCalendar.setTimeInMillis(tourDate);

					cell.setText(fTimeFormatter.format(fCalendar.getTime()));
					setCellColor(cell, element);
				}
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

					final long tourTypeId = ((TVITourBookTour) element).getTourTypeId();
					final Image tourTypeImage = UI.getInstance().getTourTypeImage(tourTypeId);

					cell.setText(UI.getInstance().getTourTypeLabel(tourTypeId));
					/*
					 * when a tour type image is modified, it will keep the same image resource only
					 * the content is modified but in the rawDataView the modified image is not
					 * displayed compared with the tourBookView which displays the correct image
					 */
//					final byte[] imageData = tourTypeImage.getImageData().data;
//					final StringBuilder sb = new StringBuilder();
//					for (final byte b : imageData) {
//						sb.append(b);
//					}
					cell.setImage(tourTypeImage);
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

					cell.setText(TourDatabase.getTagNames(((TVITourBookTour) element).getTagIds()));
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: markers
		 */
		colDef = TreeColumnFactory.TOUR_MARKERS.createColumn(fColumnManager, pixelConverter);
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

				if (element instanceof TVITourBookTour) {
					if (fIsRecTimeFormat_hhmmss) {
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

		/*
		 * column: driving time (h)
		 */
		colDef = TreeColumnFactory.DRIVING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long drivingTime = ((TVITourBookItem) element).colDrivingTime;

				if (element instanceof TVITourBookTour) {
					if (fIsDriveTimeFormat_hhmmss) {
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

		/*
		 * column: paused time (h)
		 */
		colDef = TreeColumnFactory.PAUSED_TIME.createColumn(fColumnManager, pixelConverter);
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
					if (fIsDriveTimeFormat_hhmmss) {
						cell.setText(UI.format_hh_mm_ss(dbPausedTime).toString());
					} else {
						cell.setText(UI.format_hh_mm(dbPausedTime + 30).toString());
					}
				}

				setCellColor(cell, element);
			}
		});

		/*
		 * column: relative paused time %
		 */
		colDef = TreeColumnFactory.PAUSED_TIME_RELATIVE.createColumn(fColumnManager, pixelConverter);
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

				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);
				cell.setText(fNF.format(relativePausedTime));

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
				final float dbDistance = ((TVITourBookItem) element).colDistance;

				if (dbDistance == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {

					fNF.setMinimumFractionDigits(1);
					fNF.setMaximumFractionDigits(1);
					cell.setText(fNF.format(dbDistance / 1000 / UI.UNIT_VALUE_DISTANCE));
				}

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
				final long dbAltitudeUp = ((TVITourBookItem) element).colAltitudeUp;

				if (dbAltitudeUp == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Long.toString((long) (dbAltitudeUp / UI.UNIT_VALUE_ALTITUDE)));
				}

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
				final long dbAltitudeDown = ((TVITourBookItem) element).colAltitudeDown;

				if (dbAltitudeDown == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Long.toString((long) (-dbAltitudeDown / UI.UNIT_VALUE_ALTITUDE)));
				}

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
				final long dbMaxAltitude = ((TVITourBookItem) element).colMaxAltitude;

				if (dbMaxAltitude == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Long.toString((long) (dbMaxAltitude / UI.UNIT_VALUE_ALTITUDE)));
				}

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
				final float dbMaxSpeed = ((TVITourBookItem) element).colMaxSpeed;

				if (dbMaxSpeed == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {

					fNF.setMinimumFractionDigits(1);
					fNF.setMaximumFractionDigits(1);

					cell.setText(fNF.format(dbMaxSpeed / UI.UNIT_VALUE_DISTANCE));
				}

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
				final long dbMaxPulse = ((TVITourBookItem) element).colMaxPulse;

				if (dbMaxPulse == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Long.toString(dbMaxPulse));
				}
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

				final float speed = ((TVITourBookItem) element).colAvgSpeed / UI.UNIT_VALUE_DISTANCE;
				if (speed == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					fNF.setMinimumFractionDigits(1);
					fNF.setMaximumFractionDigits(1);

					cell.setText(fNF.format(speed));
				}

				setCellColor(cell, element);
			}
		});

		/*
		 * column: avg pace min/km - min/mi
		 */
		colDef = TreeColumnFactory.AVG_PACE.createColumn(fColumnManager, pixelConverter);
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

		/*
		 * column: avg pulse
		 */
		colDef = TreeColumnFactory.AVG_PULSE.createColumn(fColumnManager, pixelConverter);
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

		/*
		 * column: avg cadence
		 */
		colDef = TreeColumnFactory.AVG_CADENCE.createColumn(fColumnManager, pixelConverter);
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

		/*
		 * column: avg temperature
		 */
		colDef = TreeColumnFactory.AVG_TEMPERATURE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				long temperature = ((TVITourBookItem) element).colAvgTemperature;

				if (temperature == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {

					if (UI.UNIT_VALUE_TEMPERATURE != 1) {
						temperature = (long) (temperature * UI.UNIT_FAHRENHEIT_MULTI + UI.UNIT_FAHRENHEIT_ADD);
					}

					cell.setText(Long.toString(temperature));
				}

				setCellColor(cell, element);
			}
		});

		/*
		 * column: week
		 */
		colDef = TreeColumnFactory.WEEK.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final int week = ((TVITourBookItem) element).colWeek;

				if (week == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Integer.toString(week));
				}

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

		/*
		 * column: device distance
		 */
		colDef = TreeColumnFactory.DEVICE_DISTANCE.createColumn(fColumnManager, pixelConverter);
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

//		/*
//		 * column: device name
//		 */
//		colDef = TreeColumnFactory.DEVICE_NAME.createColumn(fColumnManager, pixelConverter);
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//				final Object element = cell.getElement();
//				if (element instanceof TVITourBookTour) {
//
//					cell.setText(((TVITourBookTour) cell.getElement()).getDeviceName());
//					setCellColor(cell, element);
//				}
//			}
//		});

	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getViewSite().getPage().removePartListener(fPartListener);
		TourManager.getInstance().removeTourEventListener(fTourPropertyListener);

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
		fActionEditTour.setEnabled(isOneTour);
		fActionOpenTour.setEnabled(isOneTour);
		fActionEditQuick.setEnabled(isOneTour);
		fActionOpenMarkerDialog.setEnabled(isOneTour && isDeviceTour);
		fActionOpenAdjustAltitudeDialog.setEnabled(isOneTour && isDeviceTour);
		fActionMergeTour.setEnabled(isOneTour && isDeviceTour && firstSavedTour.getMergeSourceTourId() != null);

		// enable delete ation when at least one tour is selected
		if (isTourSelected) {
			fActionDeleteTour.setEnabled(true);
		} else {
			fActionDeleteTour.setEnabled(false);
		}

		fActionExportTour.setEnabled(isTourSelected);

		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
		fActionSetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);

		// add tag
		fActionAddTag.setEnabled(isTourSelected);

		// remove tags
		if (isOneTour) {

			// one tour is selected

			final ArrayList<Long> tagIds = firstTour.getTagIds();
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
		menuMgr.add(fActionSelectAllTours);
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
		menuMgr.add(fActionEditTour);
		menuMgr.add(fActionOpenMarkerDialog);
		menuMgr.add(fActionOpenAdjustAltitudeDialog);
		menuMgr.add(fActionMergeTour);
		menuMgr.add(fActionOpenTour);
		menuMgr.add(fActionExportTour);

		menuMgr.add(new Separator());
		menuMgr.add(fActionSetTourType);
		menuMgr.add(fActionAddTag);
		menuMgr.add(fActionRemoveTag);
		menuMgr.add(fActionRemoveAllTags);
		TagManager.fillRecentTagsIntoMenu(menuMgr, this, true, true);
		menuMgr.add(fActionOpenTagPrefs);

		menuMgr.add(new Separator());
		menuMgr.add(fActionDeleteTour);

		enableActions();
	}

	void firePostSelection(final ISelection selection) {
		fPostSelectionProvider.setSelection(selection);
	}

//	Long getActiveTourId() {
//		return fSelectedTourId;
//	}

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

	PostSelectionProvider getPostSelectionProvider() {
		return fPostSelectionProvider;
	}

	private void getSelectedTourData(final ArrayList<TourData> selectedTourData, final ArrayList<Long> tourIds) {
		for (final Long tourId : tourIds) {
			selectedTourData.add(TourManager.getInstance().getTourData(tourId));
		}
	}

	public ArrayList<TourData> getSelectedTours() {

		// get selected tours

		final IStructuredSelection selectedTours = ((IStructuredSelection) fTourViewer.getSelection());
		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();
		final ArrayList<Long> tourIds = new ArrayList<Long>();

		if (selectedTours.size() < 2) {

			// one item is selected

			final Object selectedItem = selectedTours.getFirstElement();
			if (selectedItem instanceof TVITourBookYear) {

				// one year is selected

				if (fActionSelectAllTours.isChecked()) {

					// loop: all months
					for (final TreeViewerItem viewerItem : ((TVITourBookYear) selectedItem).getFetchedChildren()) {
						if (viewerItem instanceof TVITourBookMonth) {
							getMonthTourIds((TVITourBookMonth) viewerItem, tourIds);
						}
					}
				}

			} else if (selectedItem instanceof TVITourBookMonth) {

				// one month is selected

				if (fActionSelectAllTours.isChecked()) {
					getMonthTourIds((TVITourBookMonth) selectedItem, tourIds);
				}

			} else if (selectedItem instanceof TVITourBookTour) {

				// one tour is selected

				tourIds.add(((TVITourBookTour) selectedItem).getTourId());
			}

		} else {

			// multiple items are selected

			// get all selected tours, ignore year and month items
			for (final Iterator<?> tourIterator = selectedTours.iterator(); tourIterator.hasNext();) {
				final Object viewItem = tourIterator.next();

				if (viewItem instanceof TVITourBookTour) {
					tourIds.add(((TVITourBookTour) viewItem).getTourId());
				}
			}
		}

		/*
		 * show busyindicator when multiple tours needs to be retrieved from the database
		 */
		if (tourIds.size() > 1) {
			BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
				public void run() {
					getSelectedTourData(selectedTourData, tourIds);
				}
			});
		} else {
			getSelectedTourData(selectedTourData, tourIds);
		}

		return selectedTourData;
	}

	public ColumnViewer getViewer() {
		return fTourViewer;
	}

	private void onSelectTreeItem(final SelectionChangedEvent event) {

		fSelectedTourIds.clear();

		final IStructuredSelection selectedTours = (IStructuredSelection) (event.getSelection());
		if (selectedTours.size() < 2) {

			// one item is selected

			final Object selectedItem = selectedTours.getFirstElement();
			if (selectedItem instanceof TVITourBookYear) {

				// year is selected

				final TVITourBookYear yearItem = ((TVITourBookYear) selectedItem);
				fSelectedYear = yearItem.fTourYear;

				if (fActionSelectAllTours.isChecked()) {

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
				fSelectedYear = monthItem.fTourYear;
				fSelectedMonth = monthItem.fTourMonth;

				if (fActionSelectAllTours.isChecked()) {

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

				fSelectedYear = tourItem.fTourYear;
				fSelectedMonth = tourItem.fTourMonth;

				final Long tourId = tourItem.getTourId();
				fSelectedTourIds.add(tourId);

				fPostSelectionProvider.setSelection(new SelectionTourId(tourId));
			}

		} else {

			// multiple items are selected

			boolean isFirstTour = true;

			// get all selected tours
			for (final Iterator<?> tourIterator = selectedTours.iterator(); tourIterator.hasNext();) {

				final Object viewItem = tourIterator.next();
				if (viewItem instanceof TVITourBookTour) {

					final TVITourBookTour tourItem = (TVITourBookTour) viewItem;
					fSelectedTourIds.add(tourItem.getTourId());

					if (isFirstTour) {
						isFirstTour = false;

						fSelectedYear = tourItem.fTourYear;
						fSelectedMonth = tourItem.fTourMonth;
					}
				}
			}

			fPostSelectionProvider.setSelection(new SelectionTourIds(fSelectedTourIds));
		}

		enableActions();
	}

	private void readDisplayFormats() {

		final Preferences prefStore = TourbookPlugin.getDefault().getPluginPreferences();

		fIsRecTimeFormat_hhmmss = prefStore.getString(ITourbookPreferences.VIEW_LAYOUT_RECORDING_TIME_FORMAT).equals(
				PrefPageAppearanceView.VIEW_TIME_LAYOUT_HH_MM_SS);

		fIsDriveTimeFormat_hhmmss = prefStore.getString(ITourbookPreferences.VIEW_LAYOUT_DRIVING_TIME_FORMAT).equals(
				PrefPageAppearanceView.VIEW_TIME_LAYOUT_HH_MM_SS);
	}

	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

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

		return fTourViewer;
	}

	public void reloadViewer() {

		final Tree tree = fTourViewer.getTree();
		tree.setRedraw(false);
		{
			final Object[] expandedElements = fTourViewer.getExpandedElements();
			final ISelection selection = fTourViewer.getSelection();

			fTourViewer.setInput(fRootItem = new TVITourBookRoot(this));

			fTourViewer.setExpandedElements(expandedElements);
			fTourViewer.setSelection(selection, true);
		}
		tree.setRedraw(true);
	}

	private void reselectTourViewer() {

		// find the old selected year/month in the new tour items
		TreeViewerItem reselectYearItem = null;
		TreeViewerItem reselectMonthItem = null;
		final ArrayList<TreeViewerItem> reselectTourItems = new ArrayList<TreeViewerItem>();

		/*
		 * get the year/month/tour item in the data model
		 */
		final ArrayList<TreeViewerItem> yearItems = fRootItem.getChildren();
		for (final TreeViewerItem yearItem : yearItems) {

			final TVITourBookYear tourBookYear = ((TVITourBookYear) yearItem);
			if (tourBookYear.fTourYear == fSelectedYear) {

				reselectYearItem = yearItem;

				final Object[] monthItems = tourBookYear.getFetchedChildrenAsArray();
				for (final Object monthItem : monthItems) {

					final TVITourBookMonth tourBookMonth = ((TVITourBookMonth) monthItem);
					if (tourBookMonth.fTourMonth == fSelectedMonth) {

						reselectMonthItem = tourBookMonth;

						final Object[] tourItems = tourBookMonth.getFetchedChildrenAsArray();
						for (final Object tourItem : tourItems) {

							final TVITourBookTour tourBookTour = ((TVITourBookTour) tourItem);
							final long treeTourId = tourBookTour.fTourId;

							for (final Long tourId : fSelectedTourIds) {
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

			fTourViewer.setSelection(new StructuredSelection(reselectTourItems) {}, false);

		} else if (reselectMonthItem != null) {

			fTourViewer.setSelection(new StructuredSelection(reselectMonthItem) {}, false);

		} else if (reselectYearItem != null) {

			fTourViewer.setSelection(new StructuredSelection(reselectYearItem) {}, false);

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

	private void restoreState() {

		// set tour viewer reselection data
		try {
			fSelectedYear = fState.getInt(STATE_SELECTED_YEAR);
		} catch (final NumberFormatException e) {
			fSelectedYear = -1;
		}

		try {
			fSelectedMonth = fState.getInt(STATE_SELECTED_MONTH);
		} catch (final NumberFormatException e) {
			fSelectedMonth = -1;
		}

		final String[] selectedTourIds = fState.getArray(STATE_SELECTED_TOURS);
		fSelectedTourIds.clear();

		if (selectedTourIds != null) {
			for (final String tourId : selectedTourIds) {
				try {
					fSelectedTourIds.add(Long.parseLong(tourId));
				} catch (final NumberFormatException e) {
					// ignore
				}
			}
		}

		fActionSelectAllTours.setChecked(fState.getBoolean(STATE_SELECT_YEAR_MONTH_TOURS));

	}

	private void saveState() {

		// save selection in the tour viewer
		fState.put(STATE_SELECTED_YEAR, fSelectedYear);
		fState.put(STATE_SELECTED_MONTH, fSelectedMonth);

		// convert tour id's into string
		final ArrayList<String> selectedTourIds = new ArrayList<String>();
		for (final Long tourId : fSelectedTourIds) {
			selectedTourIds.add(tourId.toString());
		}
		fState.put(STATE_SELECTED_TOURS, selectedTourIds.toArray(new String[selectedTourIds.size()]));

		// action: select tours for year/month
		fState.put(STATE_SELECT_YEAR_MONTH_TOURS, fActionSelectAllTours.isChecked());

		fColumnManager.saveState(fState);
	}

	public void setActiveYear(final int activeYear) {
		fSelectedYear = activeYear;
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
