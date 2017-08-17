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
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.common.tooltip.ToolbarSlideout;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Map bookmarks
 */
public class SlideoutMapBookmarks extends ToolbarSlideout {

	private IMapBookmarks			_mapBookmarks;

	private ArrayList<MapBookmark>	_allBookmarks;

	private TableViewer				_bookmarkViewer;

	private final NumberFormat		_nfLatLon	= NumberFormat.getNumberInstance();
	{
		_nfLatLon.setMinimumFractionDigits(4);
		_nfLatLon.setMaximumFractionDigits(4);
	}

	private PixelConverter	_pc;

	private Font			_boldFont;

	{
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
	}

	/*
	 * UI controls
	 */
	private Composite _parent;

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
			return _allBookmarks.toArray();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	public class BookmarkRenameDialog extends InputDialog {

		public BookmarkRenameDialog(final Shell parentShell,
									final String dialogTitle,
									final String dialogMessage,
									final String initialValue,
									final IInputValidator validator) {

			super(parentShell, dialogTitle, dialogMessage, initialValue, validator);
		}

		@Override
		protected void createButtonsForButtonBar(final Composite parent) {

			super.createButtonsForButtonBar(parent);

			// set text for the OK button
			final Button _btnSave = getButton(IDialogConstants.OK_ID);
			_btnSave.setText(Messages.Slideout_MapBookmark_Button_Rename);
		}

		@Override
		protected Point getInitialLocation(final Point initialSize) {

			try {

				final Point cursorLocation = Display.getCurrent().getCursorLocation();

				// center below the cursor location
				cursorLocation.x -= initialSize.x / 2;
				cursorLocation.y += 10;

				return cursorLocation;

			} catch (final NumberFormatException ex) {

				return super.getInitialLocation(initialSize);
			}
		}

	}

	/**
	 * @param ownerControl
	 * @param toolBar
	 * @param mapBookmarks
	 */
	public SlideoutMapBookmarks(final Control ownerControl,
								final ToolBar toolBar,
								final IMapBookmarks mapBookmarks) {

		super(ownerControl, toolBar);

		_mapBookmarks = mapBookmarks;
	}

	private void createActions() {

	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		_parent = parent;

		initUI(parent);

		createActions();

		final Composite ui = createUI(parent);

		restoreState();

		// fill bookmark viewer
		_bookmarkViewer.setInput(new Object());

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(shellContainer);
		{
			final Composite container = new Composite(shellContainer, SWT.NONE);
			GridDataFactory
					.fillDefaults()//
					//					.grab(true, false)
					.applyTo(container);
			GridLayoutFactory
					.fillDefaults()//
					.numColumns(2)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			{
				createUI_10_Title(container);
				createUI_12_Actions(container);

				createUI_50_BookmarkViewer(container);

			}
		}

		return shellContainer;
	}

	private void createUI_10_Title(final Composite parent) {

		/*
		 * Label: Slideout title
		 */
		final Label label = new Label(parent, SWT.NONE);
		label.setFont(_boldFont);
		label.setText(Messages.Slideout_MapBookmark_Label_Title);
		GridDataFactory
				.fillDefaults()//
				.align(SWT.BEGINNING, SWT.CENTER)
				.applyTo(label);
	}

	private void createUI_12_Actions(final Composite parent) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
		GridDataFactory
				.fillDefaults()//
				//				.grab(true, false)
				.align(SWT.END, SWT.BEGINNING)
				.indent(_pc.convertWidthInCharsToPixels(5), 0)
				.applyTo(toolbar);

		final ToolBarManager tbm = new ToolBarManager(toolbar);

//		tbm.add(_actionRestoreDefaults);

		tbm.update(true);
	}

	private void createUI_50_BookmarkViewer(final Composite parent) {

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				//				.grab(true, true)
				.span(2, 1)
				.hint(_pc.convertWidthInCharsToPixels(50), SWT.DEFAULT)
				.applyTo(layoutContainer);

		final TableColumnLayout tableLayout = new TableColumnLayout();
		layoutContainer.setLayout(tableLayout);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION);

		table.setLayout(new TableLayout());

		// !!! this prevents that the horizontal scrollbar is displayed, but is not always working :-(
		table.setHeaderVisible(false);
//		table.setHeaderVisible(true);

		_bookmarkViewer = new TableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tc;

		{
			// Column: Bookmark name

			tvc = new TableViewerColumn(_bookmarkViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText(Messages.Slideout_MapBookmark_Column_Name);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final MapBookmark bookmark = (MapBookmark) cell.getElement();

					cell.setText(bookmark.name);
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(1, false));
		}
		{
			// Column: Latitude

			tvc = new TableViewerColumn(_bookmarkViewer, SWT.TRAIL);
			tc = tvc.getColumn();
			tc.setText(net.tourbook.ui.Messages.ColumnFactory_latitude);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final MapBookmark bookmark = (MapBookmark) cell.getElement();
					final String valueText = _nfLatLon.format(bookmark.getLatitude());

					cell.setText(valueText);
				}
			});
			tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(9), false));
		}
		{
			// Column: Longitude

			tvc = new TableViewerColumn(_bookmarkViewer, SWT.TRAIL);
			tc = tvc.getColumn();
			tc.setText(net.tourbook.ui.Messages.ColumnFactory_longitude);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final MapBookmark bookmark = (MapBookmark) cell.getElement();
					final String valueText = _nfLatLon.format(bookmark.getLongitude());

					cell.setText(valueText);
				}
			});
			tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(9), false));
		}

		/*
		 * create table viewer
		 */
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
				onBookmark_Rename();
			}
		});

		_bookmarkViewer.getTable().addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(final KeyEvent e) {

				if (e.keyCode == SWT.DEL) {
					onBookmark_Delete();
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {}
		});
	}

	private MapBookmark getSelectedBookmark() {

		final IStructuredSelection selection = (IStructuredSelection) _bookmarkViewer.getSelection();
		final MapBookmark selectedBookmark = (MapBookmark) selection.getFirstElement();

		return selectedBookmark;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

	}

	private void onBookmark_Delete() {

		final MapBookmark selectedBookmark = getSelectedBookmark();

		if (selectedBookmark == null) {
			return;
		}

		// update model
		_allBookmarks.remove(selectedBookmark);
		MapBookmarkManager.getAllRecentBookmarks().remove(selectedBookmark);

		// update UI
		_bookmarkViewer.refresh();
	}

	private void onBookmark_Rename() {

		final MapBookmark selectedBookmark = getSelectedBookmark();

		final BookmarkRenameDialog addDialog = new BookmarkRenameDialog(

				_parent.getShell(),
				Messages.Slideout_MapBookmark_Dialog_RenameBookmark_Title,
				Messages.Slideout_MapBookmark_Dialog_RenameBookmark_Message,
				selectedBookmark.name,
				new IInputValidator() {

					@Override
					public String isValid(final String newText) {

						if (newText.trim().length() == 0) {
							return Messages.Slideout_MapBookmark_Dialog_ValidationRename;
						}

						return null;
					}
				});

		setIsAnotherDialogOpened(true);
		{
			addDialog.open();
		}
		setIsAnotherDialogOpened(false);

		if (addDialog.getReturnCode() != Window.OK) {
			return;
		}

		// update model
		selectedBookmark.name = addDialog.getValue();

		// update ui
		_bookmarkViewer.refresh();

		// reselect bookmark
		_bookmarkViewer.setSelection(new StructuredSelection(selectedBookmark), true);
	}

	private void onBookmark_Select() {

		final MapBookmark selectedBookmark = getSelectedBookmark();

		if (selectedBookmark == null) {
			// this happened when deleting a bookmark
			return;
		}

		_mapBookmarks.onSelectBookmark(selectedBookmark);
	}

	private void restoreState() {

		_allBookmarks = MapBookmarkManager.getAllMapBookmarks();

	}

}
