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
import java.util.LinkedList;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.ToolbarSlideout;

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
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.oscim.core.MapPosition;
import org.oscim.utils.Easing;
import org.oscim.utils.Easing.Type;

/**
 * Slideout for map bookmarks
 */
public class SlideoutMapBookmarks extends ToolbarSlideout {

	private IMapBookmarks		_mapBookmarks;

	private TableViewer			_bookmarkViewer;

	private SelectionAdapter	_defaultSelectionListener;
	private MouseWheelListener	_defaultMouseWheelListener;
	private FocusListener		_keepOpenListener;

	private final NumberFormat	_nfLatLon	= NumberFormat.getNumberInstance();
	{
		_nfLatLon.setMinimumFractionDigits(2);
		_nfLatLon.setMaximumFractionDigits(2);
	}

	private PixelConverter	_pc;

	private Font			_boldFont;

	/*
	 * UI controls
	 */
	private Composite		_parent;

	private Button			_btnDelete;
	private Button			_btnRename;

	private Button			_chkIsAnimateLocation;

	private Label			_lblAnimationTime;
	private Label			_lblAnimationEasingType;
	private Label			_lblSeconds;

	private Spinner			_spinnerAnimationTime;
	private Spinner			_spinnerNumRecentBookmarks;
	private Spinner			_spinnerNumBookmarkItems;

	private Combo			_comboAnimationEasingType;

	private boolean			_canAnimate;

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
				cursorLocation.y += 50;

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
	 * @param canAnimate
	 *            When <code>true</code> then the UI widgets are displayed to configure the
	 *            animation
	 */
	public SlideoutMapBookmarks(final Control ownerControl,
								final ToolBar toolBar,
								final IMapBookmarks mapBookmarks,
								final boolean canAnimate) {

		super(ownerControl, toolBar);

		_mapBookmarks = mapBookmarks;
		_canAnimate = canAnimate;
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

		enableActions();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(shellContainer);
		{
			final Composite container = new Composite(shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(container);
			GridLayoutFactory.fillDefaults().applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			{
				createUI_10_Title(container);

				createUI_50_BookmarkViewer(container);
				createUI_60_BookmarkActions(container);

				createUI_70_Options(container);

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
//		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
	}

	private void createUI_50_BookmarkViewer(final Composite parent) {

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()
				.hint(
						_pc.convertWidthInCharsToPixels(50),
						_pc.convertHeightInCharsToPixels((int) (MapBookmarkManager.numberOfBookmarkItems * 1.4)))
				.applyTo(layoutContainer);

		final TableColumnLayout tableLayout = new TableColumnLayout();
		layoutContainer.setLayout(tableLayout);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(true);

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
			// Column: Zoomlevel

			tvc = new TableViewerColumn(_bookmarkViewer, SWT.TRAIL);
			tc = tvc.getColumn();
			tc.setText(Messages.Slideout_MapBookmark_Column_ZoomLevel);
			tc.setToolTipText(Messages.Slideout_MapBookmark_Column_ZoomLevel_Tooltip);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final MapBookmark bookmark = (MapBookmark) cell.getElement();
					final MapPosition mapPos = bookmark.getMapPosition();

					cell.setText(Integer.toString(mapPos.zoomLevel));
				}
			});
			tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(5), true));
		}
		{
			// Column: Latitude

			tvc = new TableViewerColumn(_bookmarkViewer, SWT.TRAIL);
			tc = tvc.getColumn();
			tc.setText(Messages.Slideout_MapBookmark_Column_Latitude);
			tc.setToolTipText(Messages.Slideout_MapBookmark_Column_Latitude_Tooltip);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final MapBookmark bookmark = (MapBookmark) cell.getElement();
					final String valueText = _nfLatLon.format(bookmark.getLatitude());

					cell.setText(valueText);
				}
			});
			tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(9), true));
		}
		{
			// Column: Longitude

			tvc = new TableViewerColumn(_bookmarkViewer, SWT.TRAIL);
			tc = tvc.getColumn();
			tc.setText(Messages.Slideout_MapBookmark_Column_Longitude);
			tc.setToolTipText(Messages.Slideout_MapBookmark_Column_Longitude_Tooltip);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final MapBookmark bookmark = (MapBookmark) cell.getElement();
					final String valueText = _nfLatLon.format(bookmark.getLongitude());

					cell.setText(valueText);
				}
			});
			tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(9), true));
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

	private void createUI_60_BookmarkActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.align(SWT.END, SWT.FILL)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Button: Rename
				 */
				_btnRename = new Button(container, SWT.PUSH);
				_btnRename.setText(Messages.App_Action_Rename);
				_btnRename.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onBookmark_Rename();
					}
				});

				// set button default width
				UI.setButtonLayoutData(_btnRename);
			}
			{
				/*
				 * Button: Delete
				 */
				_btnDelete = new Button(container, SWT.PUSH);
				_btnDelete.setText(Messages.App_Action_Delete);
				_btnDelete.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onBookmark_Delete();
					}
				});

				// set button default width
				UI.setButtonLayoutData(_btnDelete);
			}
		}
	}

	private void createUI_70_Options(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.indent(0, 10)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			if (_canAnimate) {
				createUI_72_Options_Animation(container);
			}
			createUI_74_Options_NumItems(container);
		}
	}

	private void createUI_72_Options_Animation(final Composite parent) {

		final SelectionAdapter animationListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
				onSelectAnimation();
			}
		};

		{
			/*
			 * Is animate location
			 */
			_chkIsAnimateLocation = new Button(parent, SWT.CHECK);
			_chkIsAnimateLocation.setText(Messages.Slideout_MapBookmark_Checkbox_IsAnimationLocation);
			_chkIsAnimateLocation.setToolTipText(Messages.Slideout_MapBookmark_Checkbox_IsAnimationLocation_Tooltip);
			_chkIsAnimateLocation.addSelectionListener(_defaultSelectionListener);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsAnimateLocation);
		}
		{
			/*
			 * Animation time
			 */

			// Label
			_lblAnimationTime = new Label(parent, SWT.NONE);
			_lblAnimationTime.setText(Messages.Slideout_MapBookmark_Label_AnimationTime);
			_lblAnimationTime.setToolTipText(Messages.Slideout_MapBookmark_Label_AnimationTime_Tooltip);
			GridDataFactory.fillDefaults().indent(_pc.convertWidthInCharsToPixels(3), 0).applyTo(_lblAnimationTime);

			final Composite timeContainer = new Composite(parent, SWT.NONE);
//				GridDataFactory.fillDefaults().grab(true, false).applyTo(timeContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(timeContainer);
			{

				// Spinner
				_spinnerAnimationTime = new Spinner(timeContainer, SWT.BORDER);
				_spinnerAnimationTime.setMinimum((int) (MapBookmarkManager.LOCATION_ANIMATION_TIME_MIN * 10));
				_spinnerAnimationTime.setMaximum((int) (MapBookmarkManager.LOCATION_ANIMATION_TIME_MAX * 10));
				_spinnerAnimationTime.setPageIncrement(10);
				_spinnerAnimationTime.setDigits(1);
				_spinnerAnimationTime.addSelectionListener(animationListener);
				_spinnerAnimationTime.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangeUI();
						onSelectAnimation();
					}
				});

				// Label
				_lblSeconds = new Label(timeContainer, SWT.NONE);
				_lblSeconds.setText(Messages.App_Unit_Seconds_Small);
			}
		}
		{
			/*
			 * Easing type
			 */

			// Label
			_lblAnimationEasingType = new Label(parent, SWT.NONE);
			_lblAnimationEasingType.setText(Messages.Slideout_MapBookmark_Label_AnimationEasingType);
			_lblAnimationEasingType.setToolTipText(Messages.Slideout_MapBookmark_Label_AnimationEasingType_Tooltip);
			GridDataFactory
					.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(_pc.convertWidthInCharsToPixels(3), 0)
					.applyTo(_lblAnimationEasingType);

			// combo
			_comboAnimationEasingType = new Combo(parent, SWT.READ_ONLY);
			_comboAnimationEasingType.addFocusListener(_keepOpenListener);
			_comboAnimationEasingType.addSelectionListener(animationListener);

			// fill combo
			for (final Type easingType : Easing.Type.values()) {
				_comboAnimationEasingType.add(easingType.name());
			}
		}
	}

	private void createUI_74_Options_NumItems(final Composite parent) {

		{
			/*
			 * Number of bookmark list entries
			 */

			// Label
			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.Slideout_MapBookmark_Label_NumBookmarkListItems);
			label.setToolTipText(Messages.Slideout_MapBookmark_Label_NumBookmarkListItems_Tooltip);

			// Spinner
			_spinnerNumBookmarkItems = new Spinner(parent, SWT.BORDER);
			_spinnerNumBookmarkItems.setMinimum(MapBookmarkManager.NUM_BOOKMARK_ITEMS_MIN);
			_spinnerNumBookmarkItems.setMaximum(MapBookmarkManager.NUM_BOOKMARK_ITEMS_MAX);
			_spinnerNumBookmarkItems.setPageIncrement(5);
			_spinnerNumBookmarkItems.addSelectionListener(_defaultSelectionListener);
			_spinnerNumBookmarkItems.addMouseWheelListener(_defaultMouseWheelListener);
		}
		{
			/*
			 * Number of context menu items
			 */

			// Label
			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.Slideout_MapBookmark_Label_NumContextMenuItems);
			label.setToolTipText(Messages.Slideout_MapBookmark_Label_NumContextMenuItems_Tooltip);

			// Spinner
			_spinnerNumRecentBookmarks = new Spinner(parent, SWT.BORDER);
			_spinnerNumRecentBookmarks.setMinimum(MapBookmarkManager.NUM_RECENT_BOOKMARKS_MIN);
			_spinnerNumRecentBookmarks.setMaximum(MapBookmarkManager.NUM_RECENT_BOOKMARKS_MAX);
			_spinnerNumRecentBookmarks.setPageIncrement(5);
			_spinnerNumRecentBookmarks.addSelectionListener(_defaultSelectionListener);
			_spinnerNumRecentBookmarks.addMouseWheelListener(_defaultMouseWheelListener);
		}
	}

	private Type easingType_GetSelected() {

		final int selectedIndex = _comboAnimationEasingType.getSelectionIndex();

		final Type[] easingTypes = Easing.Type.values();

		return easingTypes[selectedIndex];
	}

	private void easingType_SetSelected(final Type selectEasingType) {

		final Type[] easingTypes = Easing.Type.values();

		int selectIndex = 0;

		for (int typeIndex = 0; typeIndex < easingTypes.length; typeIndex++) {

			if (selectEasingType == easingTypes[typeIndex]) {

				selectIndex = typeIndex;

				break;
			}
		}

		_comboAnimationEasingType.select(selectIndex);
	}

	private void enableActions() {

		final MapBookmark selectedBookmark = getSelectedBookmark();

		final boolean isBookmarkSelected = selectedBookmark != null;

		_btnDelete.setEnabled(isBookmarkSelected);
		_btnRename.setEnabled(isBookmarkSelected);

		if (_canAnimate) {

			final boolean isAnimation = _chkIsAnimateLocation.getSelection();

			_comboAnimationEasingType.setEnabled(isAnimation);
			_lblAnimationEasingType.setEnabled(isAnimation);
			_lblAnimationTime.setEnabled(isAnimation);
			_lblSeconds.setEnabled(isAnimation);
			_spinnerAnimationTime.setEnabled(isAnimation);
		}
	}

	private MapBookmark getSelectedBookmark() {

		final IStructuredSelection selection = (IStructuredSelection) _bookmarkViewer.getSelection();
		final MapBookmark selectedBookmark = (MapBookmark) selection.getFirstElement();

		return selectedBookmark;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChangeUI();
			}
		};

		_keepOpenListener = new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {

				/*
				 * This will fix the problem that when the list of a combobox is displayed, then the
				 * slideout will disappear :-(((
				 */
				setIsAnotherDialogOpened(true);
			}

			@Override
			public void focusLost(final FocusEvent e) {
				setIsAnotherDialogOpened(false);
			}
		};
	}

	private void onBookmark_Delete() {

		final MapBookmark selectedBookmark = getSelectedBookmark();

		if (selectedBookmark == null) {
			return;
		}

		// update model
		MapBookmarkManager.getAllMapBookmarks().remove(selectedBookmark);
		MapBookmarkManager.getAllRecentBookmarks().remove(selectedBookmark);

		// update UI
		_bookmarkViewer.refresh();

		enableActions();
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

		_mapBookmarks.moveToMapLocation(selectedBookmark);

		enableActions();
	}

	private void onChangeUI() {

		saveState();

		enableActions();
	}

	private void onSelectAnimation() {

		final LinkedList<MapBookmark> recentBookmarks = MapBookmarkManager.getAllRecentBookmarks();

		if (recentBookmarks.size() < 2) {
			return;
		}

		final MapBookmark prevBookmark = recentBookmarks.get(1);

		_mapBookmarks.moveToMapLocation(prevBookmark);
	}

	private void restoreState() {

		if (_canAnimate) {

			_chkIsAnimateLocation.setSelection(MapBookmarkManager.isAnimateLocation);
			_spinnerAnimationTime.setSelection((int) (MapBookmarkManager.animationTime * 10));

			easingType_SetSelected(MapBookmarkManager.animationEasingType);
		}

		_spinnerNumBookmarkItems.setSelection(MapBookmarkManager.numberOfBookmarkItems);
		_spinnerNumRecentBookmarks.setSelection(MapBookmarkManager.numberOfRecentBookmarks);
	}

	private void saveState() {

		if (_canAnimate) {

			MapBookmarkManager.isAnimateLocation = _chkIsAnimateLocation.getSelection();
			MapBookmarkManager.animationTime = (float) _spinnerAnimationTime.getSelection() / 10;

			MapBookmarkManager.animationEasingType = easingType_GetSelected();
		}

		MapBookmarkManager.numberOfBookmarkItems = _spinnerNumBookmarkItems.getSelection();
		MapBookmarkManager.numberOfRecentBookmarks = _spinnerNumRecentBookmarks.getSelection();
	}

}
