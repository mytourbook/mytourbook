/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.data.TourMarker;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.action.ActionModifyColumns;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.oscim.core.MapPosition;

public class MapBookmarkView extends ViewPart implements ITourViewer {

	public static final String		ID			= "net.tourbook.map.bookmark.MapBookmarkView";	//$NON-NLS-1$

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();
	private final IDialogSettings	_state		= TourbookPlugin.getState(ID);

	private ITourEventListener		_tourEventListener;
	private IPartListener2			_partListener;

	private ActionOpenMarkerDialog	_actionEditTourMarkers;
	private ActionModifyColumns		_actionModifyColumns;

	private PixelConverter			_pc;

	private TableViewer				_bookmarkViewer;
	private ColumnManager			_columnManager;

	private boolean					_isInUpdate;

	private ColumnDefinition		_colDefName;
	private ColumnDefinition		_colDefVisibility;

	private final NumberFormat		_nf3		= NumberFormat.getNumberInstance();
	{
		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);
	}

	/*
	 * UI controls
	 */
	private Font		_boldFont;

	private Composite	_viewerContainer;

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
			return MapBookmarkManager.getAllMapBookmarks().toArray();
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

	private void clearView() {

		updateUI_MarkerViewer();
	}

	private void createActions() {

		_actionModifyColumns = new ActionModifyColumns(this);
	}

	@Override
	public void createPartControl(final Composite parent) {

		_pc = new PixelConverter(parent);
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		// define all columns for the viewer
		_columnManager = new ColumnManager(this, _state);
		_columnManager.setIsCategoryAvailable(true);
		defineAllColumns();

		createUI(parent);

		addPartListener();

		createActions();
		fillToolbar();

		updateUI_MarkerViewer();
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
		final Table table = new Table(parent, SWT.FULL_SELECTION | SWT.MULTI /* | SWT.BORDER */);
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

			}
		});

		_bookmarkViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {

			}
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

		defineColumn_Name();
	}

	/**
	 * Column: Name
	 */
	private void defineColumn_Name() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "name", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Slideout_MapBookmark_Column_Name);
		colDef.setColumnHeaderText(Messages.Slideout_MapBookmark_Column_Name);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(30));
		colDef.setColumnWeightData(new ColumnWeightData(30));

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
	private void defineColumn_Zoomlevel() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "zoomLebel", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Slideout_MapBookmark_Column_ZoomLevel_Tooltip);
		colDef.setColumnHeaderText(Messages.Slideout_MapBookmark_Column_ZoomLevel_Tooltip);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(5));
		colDef.setColumnWeightData(new ColumnWeightData(5));

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MapBookmark bookmark = (MapBookmark) cell.getElement();
				final MapPosition mapPos = bookmark.getMapPosition();

				cell.setText(Integer.toString(mapPos.zoomLevel));
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

//		_actionEditTourMarkers.setEnabled(isTourInDb && isSingleTour);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionEditTourMarkers);

		// add standard group which allows other plug-ins to contribute here
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		// set the marker which should be selected in the marker dialog
		final IStructuredSelection selection = (IStructuredSelection) _bookmarkViewer.getSelection();
		_actionEditTourMarkers.setTourMarker((TourMarker) selection.getFirstElement());

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

	@Override
	public ColumnViewer getViewer() {
		return _bookmarkViewer;
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

		updateUI_MarkerViewer();
	}

	private void saveState() {

		_columnManager.saveState(_state);
	}

	@Override
	public void setFocus() {

		_bookmarkViewer.getTable().setFocus();
	}

	@Override
	public void updateColumnHeader(final ColumnDefinition colDef) {}

	private void updateUI_MarkerViewer() {

		_isInUpdate = true;
		{
			_bookmarkViewer.setInput(new Object[0]);
		}
		_isInUpdate = false;

		enableActions();
	}
}
