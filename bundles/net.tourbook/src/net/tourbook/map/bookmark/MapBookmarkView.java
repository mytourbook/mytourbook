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
package net.tourbook.map.bookmark;

import java.text.NumberFormat;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.oscim.core.MapPosition;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.action.ActionModifyColumns;

public class MapBookmarkView extends ViewPart implements ITourViewer {

	public static final String		ID				= "net.tourbook.map.bookmark.MapBookmarkView";	//$NON-NLS-1$

	private final IDialogSettings	_state		= TourbookPlugin.getState(ID);

	private ITourEventListener		_tourEventListener;
	private IPartListener2			_partListener;

	private ActionDeleteBookmark	_actionDeleteBookmark;
	private ActionModifyColumns	_actionModifyColumns;
	private ActionRenameBookmark	_actionRenameBookmark;

	private PixelConverter			_pc;

	private TableViewer				_bookmarkViewer;
	private ColumnManager			_columnManager;

	private final NumberFormat		_nf0			= NumberFormat.getNumberInstance();
	private final NumberFormat		_nfLatLon	= NumberFormat.getNumberInstance();
	{
		_nf0.setMinimumFractionDigits(0);
		_nf0.setMaximumFractionDigits(0);
		_nfLatLon.setMinimumFractionDigits(4);
		_nfLatLon.setMaximumFractionDigits(4);
	}

	/*
	 * UI controls
	 */
	private Composite	_parent;

	private Composite	_viewerContainer;

	/**
	 * Delete bookmark
	 */
	private class ActionDeleteBookmark extends Action {

		public ActionDeleteBookmark() {
			super(Messages.Map_Bookmark_Action_Bookmark_Delete, AS_PUSH_BUTTON);
		}

		@Override
		public void run() {
			onBookmark_Delete();
		}
	}

	/**
	 * Rename bookmark
	 */
	private class ActionRenameBookmark extends Action {

		public ActionRenameBookmark() {
			super(Messages.Map_Bookmark_Action_Bookmark_Rename, AS_PUSH_BUTTON);
		}

		@Override
		public void run() {
			onBookmark_Rename(true);
		}
	}

	private class BookmarkComparator extends ViewerComparator {

		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {

			if (e1 == null || e2 == null) {
				return 0;
			}

			final MapBookmark bookmark1 = (MapBookmark) e1;
			final MapBookmark bookmark2 = (MapBookmark) e2;

			return bookmark1.name.compareTo(bookmark2.name);
		}

		@Override
		public boolean isSorterProperty(final Object element, final String property) {

			// force resorting when a name is renamed
			return true;
		}
	}

	private class BookmarkProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object inputElement) {
			return MapBookmarkManager.getAllBookmarks().toArray();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	public MapBookmarkView() {
		super();
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {

				if (partRef.getPart(false) == MapBookmarkView.this) {
					MapBookmarkManager.setMapBookmarkView(null);
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {

				if (partRef.getPart(false) == MapBookmarkView.this) {
					MapBookmarkManager.setMapBookmarkView(MapBookmarkView.this);
				}
			}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		getViewSite().getPage().addPartListener(_partListener);
	}

	private void createActions() {

		_actionModifyColumns = new ActionModifyColumns(this);
		_actionDeleteBookmark = new ActionDeleteBookmark();
		_actionRenameBookmark = new ActionRenameBookmark();
	}

	@Override
	public void createPartControl(final Composite parent) {

		initUI(parent);

		// define all columns for the viewer
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns();

		createUI(parent);

		addPartListener();

		createActions();
		fillToolbar();

		updateUI_Viewer();
	}

	private void createUI(final Composite parent) {

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
		{
			createUI_10_TableViewer(_viewerContainer);
		}
	}

	private void createUI_10_TableViewer(final Composite parent) {

		/*
		 * create table
		 */
		final Table table = new Table(parent, SWT.FULL_SELECTION /* | SWT.MULTI /* | SWT.BORDER */);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		table.setHeaderVisible(true);
//		table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
		table.setLinesVisible(false);

		/*
		 * create table viewer
		 */
		_bookmarkViewer = new TableViewer(table);

//		// set editing support after the viewer is created but before the columns are created
//		net.tourbook.common.UI.setCellEditSupport(_markerViewer);
//
//		_colDefName.setEditingSupport(new MarkerEditingSupportLabel(_markerViewer));
//		_colDefVisibility.setEditingSupport(new MarkerEditingSupportVisibility(_markerViewer));

		_columnManager.createColumns(_bookmarkViewer);

		_bookmarkViewer.setUseHashlookup(true);
		_bookmarkViewer.setContentProvider(new BookmarkProvider());
		_bookmarkViewer.setComparator(new BookmarkComparator());

		_bookmarkViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onBookmark_Select();
			}
		});

		_bookmarkViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(final DoubleClickEvent event) {
				onBookmark_Rename(true);
			}
		});

		_bookmarkViewer.getTable().addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(final KeyEvent e) {

				switch (e.keyCode) {

				case SWT.DEL:
					onBookmark_Delete();
					break;

				case SWT.F2:
					onBookmark_Rename(false);
					break;

				default:
					break;
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {}
		});

		createUI_20_ContextMenu();
	}

	/**
	 * create the views context menu
	 */
	private void createUI_20_ContextMenu() {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		final Table table = (Table) _bookmarkViewer.getControl();
		final Menu tableContextMenu = menuMgr.createContextMenu(table);

		_columnManager.createHeaderContextMenu(table, tableContextMenu);
	}

	private void defineAllColumns() {

		defineColumn_10_Name();
		defineColumn_20_Zoomlevel();
		defineColumn_30_Scale();
		defineColumn_40_Bearing();
		defineColumn_50_Tilt();
		defineColumn_60_Latitude();
		defineColumn_70_Longitude();
	}

	/**
	 * Column: Name
	 */
	private void defineColumn_10_Name() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "name", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Map_Bookmark_Column_Name);
		colDef.setColumnHeaderText(Messages.Map_Bookmark_Column_Name);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(30));
//		colDef.setColumnWeightData(new ColumnWeightData(30));

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final MapBookmark bookmark = (MapBookmark) cell.getElement();

				cell.setText(bookmark.name);
			}
		});
	}

	/**
	 * Column: Zoomlevel
	 */
	private void defineColumn_20_Zoomlevel() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "zoomLevel", SWT.TRAIL); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Map_Bookmark_Column_ZoomLevel_Tooltip);
		colDef.setColumnHeaderText(Messages.Map_Bookmark_Column_ZoomLevel);
		colDef.setColumnHeaderToolTipText(Messages.Map_Bookmark_Column_ZoomLevel_Tooltip);

		colDef.setIsDefaultColumn();
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(5));
//		colDef.setColumnWeightData(new ColumnWeightData(5));

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MapBookmark bookmark = (MapBookmark) cell.getElement();
				final MapPosition mapPos = bookmark.getMapPosition();

				cell.setText(Integer.toString(mapPos.zoomLevel));
			}
		});
	}

	/**
	 * Column: Scale
	 */
	private void defineColumn_30_Scale() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "scale", SWT.TRAIL); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Map_Bookmark_Column_Scale);
		colDef.setColumnHeaderText(Messages.Map_Bookmark_Column_Scale);

		colDef.setIsDefaultColumn();
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(9));
//		colDef.setColumnWeightData(new ColumnWeightData(9));

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MapBookmark bookmark = (MapBookmark) cell.getElement();

				final double value = bookmark.getMapPosition().scale;

				String valueText;
				if (value == 0) {
					valueText = UI.EMPTY_STRING;
				} else {
					valueText = _nf0.format(value);
				}

				cell.setText(valueText);
			}
		});
	}

	/**
	 * Column: Bearing
	 */
	private void defineColumn_40_Bearing() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "bearing", SWT.TRAIL); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Map_Bookmark_Column_Bearing);
		colDef.setColumnHeaderText(Messages.Map_Bookmark_Column_Bearing);
		colDef.setColumnHeaderToolTipText(Messages.Map_Bookmark_Column_Bearing_Tooltip);
		colDef.setColumnUnit(UI.SYMBOL_DEGREE);

		colDef.setIsDefaultColumn();
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(9));
//		colDef.setColumnWeightData(new ColumnWeightData(9));

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MapBookmark bookmark = (MapBookmark) cell.getElement();

				final float value = bookmark.getMapPosition().bearing;
				String valueText;

				if (value == 0) {
					valueText = UI.EMPTY_STRING;
				} else {
					valueText = _nf0.format(value);
				}

				cell.setText(valueText);
			}
		});
	}

	/**
	 * Column: Tilt
	 */
	private void defineColumn_50_Tilt() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "tilt", SWT.TRAIL); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Map_Bookmark_Column_Tilt);
		colDef.setColumnHeaderText(Messages.Map_Bookmark_Column_Tilt);
		colDef.setColumnHeaderToolTipText(Messages.Map_Bookmark_Column_Tilt_Tooltip);
		colDef.setColumnUnit(UI.SYMBOL_DEGREE);

		colDef.setIsDefaultColumn();
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(9));
//		colDef.setColumnWeightData(new ColumnWeightData(9));

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MapBookmark bookmark = (MapBookmark) cell.getElement();

				final float value = bookmark.getMapPosition().tilt;
				String valueText;

				if (value == 0) {
					valueText = UI.EMPTY_STRING;
				} else {
					valueText = _nf0.format(value);
				}

				cell.setText(valueText);
			}
		});
	}

	/**
	 * Column: Latitude
	 */
	private void defineColumn_60_Latitude() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "latitude", SWT.TRAIL); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Map_Bookmark_Column_Latitude_Tooltip);
		colDef.setColumnHeaderText(Messages.Map_Bookmark_Column_Latitude);
		colDef.setColumnHeaderToolTipText(Messages.Map_Bookmark_Column_Latitude_Tooltip);

		colDef.setIsDefaultColumn();
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(9));
//		colDef.setColumnWeightData(new ColumnWeightData(9));

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MapBookmark bookmark = (MapBookmark) cell.getElement();
				final String valueText = _nfLatLon.format(bookmark.getLatitude());

				cell.setText(valueText);
			}
		});
	}

	/**
	 * Column: Longitude
	 */
	private void defineColumn_70_Longitude() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "longitude", SWT.TRAIL); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Map_Bookmark_Column_Longitude_Tooltip);
		colDef.setColumnHeaderText(Messages.Map_Bookmark_Column_Longitude);
		colDef.setColumnHeaderToolTipText(Messages.Map_Bookmark_Column_Longitude_Tooltip);

		colDef.setIsDefaultColumn();
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(9));
//		colDef.setColumnWeightData(new ColumnWeightData(9));

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MapBookmark bookmark = (MapBookmark) cell.getElement();
				final String valueText = _nfLatLon.format(bookmark.getLongitude());

				cell.setText(valueText);
			}
		});
	}

	@Override
	public void dispose() {

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		getViewSite().getPage().removePartListener(_partListener);

		super.dispose();
	}

	/**
	 * enable actions
	 */
	private void enableActions() {

		final MapBookmark selectedBookmark = getSelectedBookmark();
		final boolean isSelected = selectedBookmark != null;

		_actionDeleteBookmark.setEnabled(isSelected);
		_actionRenameBookmark.setEnabled(isSelected);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionRenameBookmark);
		menuMgr.add(_actionDeleteBookmark);

		enableActions();
	}

	private void fillToolbar() {

		final IActionBars actionBars = getViewSite().getActionBars();

		/*
		 * Fill view menu
		 */
		final IMenuManager menuMgr = actionBars.getMenuManager();

		menuMgr.add(new Separator());
		menuMgr.add(_actionModifyColumns);

		/*
		 * Fill view toolbar
		 */
//		final IToolBarManager tbm = actionBars.getToolBarManager();
//
//		tbm.add();
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	public Object getMarkerViewer() {
		return _bookmarkViewer;
	}

	/**
	 * @return Returns the selected bookmark or <code>null</code> if not selected
	 */
	private MapBookmark getSelectedBookmark() {

		final IStructuredSelection selection = (IStructuredSelection) _bookmarkViewer.getSelection();
		final MapBookmark selectedBookmark = (MapBookmark) selection.getFirstElement();

		return selectedBookmark;
	}

	@Override
	public ColumnViewer getViewer() {
		return _bookmarkViewer;
	}

	private void initUI(final Composite parent) {

		_parent = parent;

		_pc = new PixelConverter(parent);
	}

	private void onBookmark_Delete() {
		final MapBookmark selectedBookmark = getSelectedBookmark();

		if (selectedBookmark == null) {
			// this happened when deleting a bookmark
			return;
		}

		// update model
		MapBookmarkManager.getAllBookmarks().remove(selectedBookmark);
		MapBookmarkManager.getAllRecentBookmarks().remove(selectedBookmark);

		// update UI
		_bookmarkViewer.refresh();
		
		// update maps
		MapBookmarkManager.fireBookmarkEvent(selectedBookmark, net.tourbook.map.bookmark.IMapBookmarks.MapBookmarkEventType_modified);

		enableActions();
	}

	/**
	 * @param isOpenedWithMouse
	 *           Is <code>true</code> when the action is opened with the mouse, otherwise with the
	 *           keyboard
	 */
	private void onBookmark_Rename(final boolean isOpenedWithMouse) {
      
		final MapBookmark selectedBookmark = getSelectedBookmark();
		
		final BookmarkRenameDialog renameDialog = new BookmarkRenameDialog(

				_parent.getShell(),
				Messages.Map_Bookmark_Dialog_RenameBookmark_Title,
				Messages.Map_Bookmark_Dialog_RenameBookmark_Message,
				selectedBookmark.name,
				isOpenedWithMouse,
				new IInputValidator() {

					@Override
					public String isValid(final String newText) {

						if (newText.trim().length() == 0) {
							return Messages.Map_Bookmark_Dialog_ValidationRename;
						}

						return null;
					}
				});

		renameDialog.open();

		if (renameDialog.getReturnCode() != Window.OK) {
			return;
		}

		// update model
		selectedBookmark.name = renameDialog.getValue();

		// update ui
		_bookmarkViewer.refresh();

		// reselect bookmark
		_bookmarkViewer.setSelection(new StructuredSelection(selectedBookmark), true);
		
		//update maps
		MapBookmarkManager.fireBookmarkEvent(selectedBookmark, net.tourbook.map.bookmark.IMapBookmarks.MapBookmarkEventType_modified);
	}

	private void onBookmark_Select() {

		final MapBookmark selectedBookmark = getSelectedBookmark();

		if (selectedBookmark == null) {
			// this happened when deleting a bookmark
			return;
		}
		
		MapBookmarkManager.fireBookmarkEvent(selectedBookmark, net.tourbook.map.bookmark.IMapBookmarks.MapBookmarkEventType_moveto);

		enableActions();
	}

	void onDeleteBookmark(final MapBookmark deletedBookmark) {

		_bookmarkViewer.refresh();
	}

	void onUpdateBookmark(final MapBookmark updatedBookmark) {

	   final MapBookmark selectedBookmark = getSelectedBookmark();

	   _bookmarkViewer.refresh(true, true);
 
	   MapBookmarkManager.fireBookmarkEvent(selectedBookmark, net.tourbook.map.bookmark.IMapBookmarks.MapBookmarkEventType_modified);
	   System.out.println("!!! mapbookmarkview onUpdateBookmark: starting");

	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			_bookmarkViewer.getTable().dispose();
			createUI_10_TableViewer(_viewerContainer);
			_viewerContainer.layout();

			// update the viewer
			reloadViewer();
		}
		_viewerContainer.setRedraw(true);

		return _bookmarkViewer;
	}

	@Override
	public void reloadViewer() {

		updateUI_Viewer();
	}

	@PersistState
	private void saveState() {

		_columnManager.saveState(_state);
	}

	@Override
	public void setFocus() {

		_bookmarkViewer.getTable().setFocus();
	}

	@Override
	public void updateColumnHeader(final ColumnDefinition colDef) {}

	private void updateUI_Viewer() {

		_bookmarkViewer.setInput(new Object[0]);

		enableActions();
	}
}

