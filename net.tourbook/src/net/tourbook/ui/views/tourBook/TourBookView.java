/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.ActionModifyColumns;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.TreeColumnDefinition;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.rawData.SelectionRawData;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
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

public class TourBookView extends ViewPart {

	static public final String		ID									= "net.tourbook.views.tourListView";			//$NON-NLS-1$

	private static final String		MEMENTO_TOURVIEWER_SELECTED_YEAR	= "tourbookview.tourviewer.selected-year";		//$NON-NLS-1$
	private static final String		MEMENTO_TOURVIEWER_SELECTED_MONTH	= "tourbookview.tourviewer.selected-month";	//$NON-NLS-1$
	private static final String		MEMENTO_LAST_SELECTED_TOUR_TYPE_ID	= "tourbookview.last-selected-tour-type-id";	//$NON-NLS-1$
	private static final String		MEMENTO_COLUMN_SORT_ORDER			= "tourbookview.column_sort_order";			//$NON-NLS-1$
	private static final String		MEMENTO_COLUMN_WIDTH				= "tourbookview.column_width";					//$NON-NLS-1$

	private static IMemento			fSessionMemento;

	private TreeViewer				fTourViewer;
	private ColumnManager			fColumnManager;

	private PostSelectionProvider	fPostSelectionProvider;
	private ISelectionListener		fPostSelectionListener;
	private IPartListener2			fPartListener;

	private IPropertyChangeListener	fPrefChangeListener;

	TVITourBookRoot					fRootItem;
	TourPerson						fActivePerson;

	long							fActiveTourTypeId;

	public NumberFormat				fNF									= NumberFormat.getNumberInstance();

	private RGB						fRGBYearFg							= new RGB(255, 255, 255);
	private RGB						fRGBMonthFg							= new RGB(128, 64, 0);
	private RGB						fRGBTourFg							= new RGB(0, 0, 128);

	private RGB						fRGBYearBg							= new RGB(111, 130, 197);
	private RGB						fRGBMonthBg							= new RGB(220, 220, 255);
	private RGB						fRGBTourBg							= new RGB(240, 240, 255);

	private Color					fColorYearFg;
	private Color					fColorMonthFg;
	private Color					fColorTourFg;

	private Color					fColorYearBg;
	private Color					fColorMonthBg;
	private Color					fColorTourBg;

	public Font						fFontNormal;
	public Font						fFontBold;

	private ActionEditTour			fActionEditTour;
	private ActionDeleteTour		fActionDeleteTour;
	private ActionSetTourType		fActionSetTourType;
	private ActionSetTourType		fActionSetLastTourType;
	private ActionModifyColumns		fActionModifyColumns;

	private int						fTourViewerSelectedYear				= -1;
	private int						fTourViewerSelectedMonth			= -1;

	Long							fActiveTourId;
	private int						fLastSelectedTourTypeId;

	private class TourBookContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildren();
		}

		public Object[] getElements(Object inputElement) {
			return fRootItem.getFetchedChildren();
		}

		public Object getParent(Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		public boolean hasChildren(Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
	}

	private void addPartListener() {

		// set the part listener
		fPartListener = new IPartListener2() {
			public void partActivated(IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(IWorkbenchPartReference partRef) {}

			public void partClosed(IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId()))
					saveSettings();
			}

			public void partDeactivated(IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					// saveSettings();
				}
			}

			public void partHidden(IWorkbenchPartReference partRef) {}

			public void partInputChanged(IWorkbenchPartReference partRef) {}

			public void partOpened(IWorkbenchPartReference partRef) {}

			public void partVisible(IWorkbenchPartReference partRef) {}
		};
		// register the listener in the page
		getViewSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

					fActivePerson = TourbookPlugin.getDefault().getActivePerson();
					fActiveTourTypeId = TourbookPlugin.getDefault().getActiveTourType().getTypeId();

					refreshTourViewer();
				}

				if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// update tourbook viewer
					UI.getInstance().disposeTourTypeImages();
					fTourViewer.refresh();
				}
			}
		};

		// register the listener
		TourbookPlugin.getDefault()
				.getPluginPreferences()
				.addPropertyChangeListener(fPrefChangeListener);
	}

	private void createActions() {

		fActionEditTour = new ActionEditTour(this);
		fActionDeleteTour = new ActionDeleteTour(this);
		fActionSetLastTourType = new ActionSetTourType(this, false);
		fActionSetTourType = new ActionSetTourType(this, true);
		fActionModifyColumns = new ActionModifyColumns(fColumnManager);

		/*
		 * fill toolbar
		 */
//		IToolBarManager viewTbm = getViewSite().getActionBars().getToolBarManager();
//		viewTbm.add(fActionShowViewDetailsViewer);
		/*
		 * fill site menu
		 */
		IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(fActionModifyColumns);
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				TourBookView.this.fillContextMenu(manager);
			}
		});

		// add the context menu to the table viewer
		Control tourViewer = fTourViewer.getControl();
		Menu menu = menuMgr.createContextMenu(tourViewer);
		tourViewer.setMenu(menu);
	}

	public void createPartControl(Composite parent) {

		createResources();

		createTourViewer(parent);
		createActions();
		createContextMenu();

		// set selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		setPostSelectionListener();
		addPartListener();
		addPrefListener();

		fActivePerson = TourbookPlugin.getDefault().getActivePerson();
		fActiveTourTypeId = TourbookPlugin.getDefault().getActiveTourType().getTypeId();
		restoreState(fSessionMemento);

		// update the viewer
		fRootItem = new TVITourBookRoot(this);
		fTourViewer.setInput(this);

		reselectTourViewer();
	}

	private void createResources() {

		Display display = Display.getCurrent();

		fColorYearFg = new Color(display, fRGBYearFg);
		fColorYearBg = new Color(display, fRGBYearBg);
		fColorMonthFg = new Color(display, fRGBMonthFg);
		fColorMonthBg = new Color(display, fRGBMonthBg);
		fColorTourFg = new Color(display, fRGBTourFg);
		fColorTourBg = new Color(display, fRGBTourBg);

		fFontNormal = JFaceResources.getFontRegistry().get(JFaceResources.DIALOG_FONT);
		fFontBold = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
	}

	private Control createTourViewer(Composite parent) {

		// tour tree
		final Tree tree = new Tree(parent, SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.BORDER
				| SWT.FLAT
				| SWT.FULL_SELECTION
				| SWT.MULTI);

		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tree.setHeaderVisible(true);
		tree.setLinesVisible(false);

		fTourViewer = new TreeViewer(tree);

		// define and create all columns
		fColumnManager = new ColumnManager(fTourViewer);
		defineAllColumns(parent);
		fColumnManager.createColumns();

		fTourViewer.setContentProvider(new TourBookContentProvider());
		fTourViewer.setUseHashlookup(true);

		fTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {

				Object selectedItem = ((IStructuredSelection) (event.getSelection())).getFirstElement();

				if (selectedItem instanceof TVITourBookYear) {

					// year is selected

//					TVITourBookYear yearItem = ((TVITourBookYear) selectedItem);

				} else if (selectedItem instanceof TVITourBookMonth) {

					// month is selected

//					TVITourBookMonth monthItem = (TVITourBookMonth) selectedItem;

				} else if (selectedItem instanceof TVITourBookTour) {

					// tour is selected

					TVITourBookTour tourItem = (TVITourBookTour) selectedItem;
					
					fActiveTourId = tourItem.getTourId();
					fPostSelectionProvider.setSelection(new SelectionTourId(fActiveTourId));
				}

				enableActions();
			}
		});

		fTourViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {

				Object selection = ((IStructuredSelection) fTourViewer.getSelection()).getFirstElement();

				if (selection instanceof TVITourBookTour) {

					// open tour in editor

					TVITourBookTour tourItem = (TVITourBookTour) selection;
					TourManager.getInstance().openTourInEditor(tourItem.getTourId());

				} else if (selection != null) {

					// expand/collapse current item

					TreeViewerItem tourItem = (TreeViewerItem) selection;

					if (fTourViewer.getExpandedState(tourItem)) {
						fTourViewer.collapseToLevel(tourItem, 1);
					} else {
						fTourViewer.expandToLevel(tourItem, 1);
					}
				}
			}
		});

		return tree;
	}

	/**
	 * Defines all columns for the table viewer in the column manager
	 * 
	 * @param parent
	 */
	private void defineAllColumns(Composite parent) {

		PixelConverter pixelConverter = new PixelConverter(parent);
		TreeColumnDefinition colDef;

		/*
		 * column: date
		 */
		colDef = TreeColumnFactory.DATE.createColumn(fColumnManager, pixelConverter);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				cell.setText(Long.toString(tourItem.fFirstColumn));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: title
		 */
		colDef = TreeColumnFactory.TITLE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				if (element instanceof TVITourBookTour) {
					cell.setText(tourItem.fTourTitle);
				} else {
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: distance (km)
		 */
		colDef = TreeColumnFactory.DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);
				cell.setText(fNF.format(((float) tourItem.fColumnDistance) / 1000));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: tour type
		 */
		colDef = TreeColumnFactory.TOUR_TYPE.createColumn(fColumnManager, pixelConverter);
//		colDef.setColumnResizable(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {
					cell.setImage(UI.getInstance()
							.getTourTypeImage(((TVITourBookTour) element).getTourTypeId()));
				} else {
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: recording time (h)
		 */
		colDef = TreeColumnFactory.RECORDING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {

				final Object element = cell.getElement();
				long recordingTime = ((TourBookTreeViewerItem) element).fColumnRecordingTime;

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
			public void update(ViewerCell cell) {

				final Object element = cell.getElement();
				long drivingTime = ((TourBookTreeViewerItem) element).fColumnDrivingTime;

				cell.setText(new Formatter().format(Messages.Format_hhmm,
						(drivingTime / 3600),
						((drivingTime % 3600) / 60)).toString());
				setCellColor(cell, element);
			}
		});

		/*
		 * column: altitude up (m)
		 */
		colDef = TreeColumnFactory.ALTITUDE_UP.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				cell.setText(Long.toString(tourItem.fColumnAltitudeUp));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: number of tours
		 */
		colDef = TreeColumnFactory.TOUR_COUNTER.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				if ((element instanceof TVITourBookTour) == false) {
					cell.setText(Long.toString(((TourBookTreeViewerItem) element).fColumnCounter));
				}
				setCellColor(cell, element);
			}
		});

		/*
		 * column: device distance
		 */
		colDef = TreeColumnFactory.DEVICE_DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {
					cell.setText(Long.toString(((TVITourBookTour) element).getColumnStartDistance()));
				}
				setCellColor(cell, element);
			}
		});

		colDef = TreeColumnFactory.TIME_INTERVAL.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {
					cell.setText(Long.toString(((TVITourBookTour) element).getColumnTimeInterval()));
				}
				setCellColor(cell, element);
			}
		});

		colDef = TreeColumnFactory.MAX_SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);
				cell.setText(fNF.format(tourItem.fColumnMaxSpeed));
				setCellColor(cell, element);
			}
		});

		colDef = TreeColumnFactory.AVG_SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);
				cell.setText(fNF.format(tourItem.fColumnAvgSpeed));
				setCellColor(cell, element);
			}
		});

		colDef = TreeColumnFactory.MAX_ALTITUDE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				cell.setText(Long.toString(tourItem.fColumnMaxAltitude));
				setCellColor(cell, element);
			}
		});

		colDef = TreeColumnFactory.MAX_PULSE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				cell.setText(Long.toString(tourItem.fColumnMaxPulse));
				setCellColor(cell, element);
			}
		});

		colDef = TreeColumnFactory.AVG_PULSE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				cell.setText(Long.toString(tourItem.fColumnAvgPulse));
				setCellColor(cell, element);
			}
		});

		colDef = TreeColumnFactory.AVG_CADENCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				cell.setText(Long.toString(tourItem.fColumnAvgCadence));
				setCellColor(cell, element);
			}
		});

		colDef = TreeColumnFactory.AVG_TEMPERATURE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				cell.setText(Long.toString(tourItem.fColumnAvgTemperature));
				setCellColor(cell, element);
			}
		});

		// TableColumnFactory.DEVICE_PROFILE.createColumn(fColumnManager,
		// pixelConverter);
// TableColumnFactory.DEVICE_NAME.createColumn(fColumnManager, pixelConverter);

	}

	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getViewSite().getPage().removePartListener(fPartListener);

		TourbookPlugin.getDefault()
				.getPluginPreferences()
				.removePropertyChangeListener(fPrefChangeListener);

		fColorYearFg.dispose();
		fColorYearBg.dispose();
		fColorMonthFg.dispose();
		fColorMonthBg.dispose();
		fColorTourFg.dispose();
		fColorTourBg.dispose();

		super.dispose();
	}

	@SuppressWarnings("unchecked")//$NON-NLS-1$
	private void enableActions() {

		ITreeSelection selection = (ITreeSelection) fTourViewer.getSelection();

		// number ob selected tour items
		int tourItems = 0;

		// count how many tour items are selected
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			if (iter.next() instanceof TVITourBookTour) {
				tourItems++;
			}
		}

		fActionEditTour.setEnabled(tourItems == 1);

		// enable the delete button when only tours are selected
		if (tourItems > 0 && selection.size() == tourItems) {
			fActionDeleteTour.setEnabled(true);
		} else {
			fActionDeleteTour.setEnabled(false);
		}

		fActionSetTourType.setEnabled(tourItems > 0);
		fActionSetLastTourType.setEnabled(tourItems > 0);
	}

	private void fillContextMenu(IMenuManager menuMgr) {

		TourType selectedTourType;
		if ((selectedTourType = fActionSetTourType.getSelectedTourType()) != null) {

			fActionSetLastTourType.setSelectedTourType(selectedTourType);
			fActionSetLastTourType.setText(NLS.bind(Messages.TourBook_Action_set_tour_type,
					selectedTourType.getName()));
			fActionSetLastTourType.setEnabled(true);
			menuMgr.add(fActionSetLastTourType);
		} else {
			fActionSetLastTourType.setEnabled(false);
		}

		menuMgr.add(fActionSetTourType);

		menuMgr.add(new Separator());
		menuMgr.add(fActionEditTour);
		menuMgr.add(fActionDeleteTour);

		menuMgr.add(new Separator());
		menuMgr.add(fActionModifyColumns);

		enableActions();
	}

	void firePostSelection(ISelection selection) {
		fPostSelectionProvider.setSelection(selection);
	}

	TreeViewer getTourViewer() {
		return fTourViewer;
	}

	/**
	 * @param initializeYearMonth
	 *        reset the selected year/month when set to <code>true</code>
	 */
	private void getTourViewerSelection(boolean initializeYearMonth) {

		if (initializeYearMonth) {
			fTourViewerSelectedYear = -1;
			fTourViewerSelectedMonth = -1;
		}

		ITreeSelection selectedItems = (ITreeSelection) fTourViewer.getSelection();
		TreePath[] treePaths = selectedItems.getPaths();

		// get selected year/month
		for (TreePath treePath : treePaths) {
			for (int segmentIndex = 0; segmentIndex < treePath.getSegmentCount(); segmentIndex++) {
				Object treeItem = treePath.getSegment(segmentIndex);
				if (treeItem instanceof TVITourBookYear) {
					fTourViewerSelectedYear = ((TVITourBookYear) treeItem).fTourYear;
				} else if (treeItem instanceof TVITourBookMonth) {
					fTourViewerSelectedMonth = ((TVITourBookMonth) treeItem).fTourMonth;
				}
			}

			// currently only the first selected entry is supported
			break;
		}
	}

	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);

		// set the session memento if it's not yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	private void refreshTourViewer() {

		getTourViewerSelection(true);

		// refresh the tree viewer
		fRootItem.fetchChildren();
		fTourViewer.refresh();

		if (fTourViewerSelectedYear == -1) {
			return;
		}

		reselectTourViewer();
	}

	private void reselectTourViewer() {

		// find the old selected year/month in the new tour items
		TreeViewerItem newYearItem = null;
		TreeViewerItem newMonthItem = null;
		ArrayList<TreeViewerItem> yearItems = fRootItem.getChildren();

		/*
		 * get the year and month item in the data model
		 */
		for (TreeViewerItem yearItem : yearItems) {
			TVITourBookYear tourBookYear = ((TVITourBookYear) yearItem);
			if (tourBookYear.fTourYear == fTourViewerSelectedYear) {
				newYearItem = yearItem;

				Object[] monthItems = tourBookYear.getFetchedChildren();
				for (Object monthItem : monthItems) {
					TVITourBookMonth tourBookMonth = ((TVITourBookMonth) monthItem);
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

			TreeViewerItem yearItem = yearItems.get(yearItems.size() - 1);

			fTourViewer.setSelection(new StructuredSelection(yearItem) {}, true);
		}
	}

	private void restoreState(IMemento memento) {

		if (memento != null) {

			/*
			 * restore states from the memento
			 */

			// set tour viewer reselection data
			Integer selectedYear = memento.getInteger(MEMENTO_TOURVIEWER_SELECTED_YEAR);
			Integer selectedMonth = memento.getInteger(MEMENTO_TOURVIEWER_SELECTED_MONTH);
			fTourViewerSelectedYear = selectedYear == null ? -1 : selectedYear;
			fTourViewerSelectedMonth = selectedMonth == null ? -1 : selectedMonth;

			// restore columns sort order
			final String mementoColumnSortOrderIds = memento.getString(MEMENTO_COLUMN_SORT_ORDER);
			if (mementoColumnSortOrderIds != null) {
				fColumnManager.orderColumns(StringToArrayConverter.convertStringToArray(mementoColumnSortOrderIds));
			}

			// restore column width
			final String mementoColumnWidth = memento.getString(MEMENTO_COLUMN_WIDTH);
			if (mementoColumnWidth != null) {
				fColumnManager.setColumnWidth(StringToArrayConverter.convertStringToArray(mementoColumnWidth));
			}

		}
	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("DeviceImportView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	public void saveState(IMemento memento) {

		// save selection in the tour viewer
		memento.putInteger(MEMENTO_TOURVIEWER_SELECTED_YEAR, fTourViewerSelectedYear);
		memento.putInteger(MEMENTO_TOURVIEWER_SELECTED_MONTH, fTourViewerSelectedMonth);
		memento.putInteger(MEMENTO_LAST_SELECTED_TOUR_TYPE_ID, fLastSelectedTourTypeId);

		// save column sort order
		memento.putString(MEMENTO_COLUMN_SORT_ORDER,
				StringToArrayConverter.convertArrayToString(fColumnManager.getColumnIds()));

		// save columns width
		memento.putString(MEMENTO_COLUMN_WIDTH,
				StringToArrayConverter.convertArrayToString(fColumnManager.getColumnIdAndWidth()));
	}

	public void setActiveYear(int activeYear) {
		fTourViewerSelectedYear = activeYear;
	}

	private void setCellColor(ViewerCell cell, final Object element) {

		if (element instanceof TVITourBookMonth) {
			cell.setBackground(fColorMonthBg);
		}
		if (element instanceof TVITourBookYear) {
			cell.setForeground(fColorYearFg);
			cell.setBackground(fColorYearBg);
		}
	}

	public void setFocus() {
		fTourViewer.getControl().setFocus();
	}

	private void setPostSelectionListener() {
		// this view part is a selection listener
		fPostSelectionListener = new ISelectionListener() {

			public void selectionChanged(IWorkbenchPart part, ISelection selection) {

				if (!selection.isEmpty() && selection instanceof SelectionRawData) {
					refreshTourViewer();
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

}
