/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.data.TourType;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.TourTypeFilterSet;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutCollateTours extends AnimatedToolTipShell {

	// initialize with default values which are (should) never be used
	private Rectangle				_toolTipItemBounds	= new Rectangle(0, 0, 50, 50);

	private final WaitTimer			_waitTimer			= new WaitTimer();

	private boolean					_isWaitTimerStarted;
	private boolean					_canOpenToolTip;
	private boolean					_isContextOpening;
	private boolean					_isShowTourTypeContextMenu;
	private boolean					_isUIUpdating;

	private MouseListener			_mouseListener;
	private MouseTrackListener		_mouseTrackListener;
	private MouseWheelListener		_mouseWheelListener;

	private TourBookView			_tourBookView;

	private int						_collateNameWidth;

	private long					_lastHideTime;
	private long					_lastOpenTime;

	private TourTypeFilter			_selectCollateFilter;

	private ActionOpenPrefDialog	_actionOpenTourTypePrefs;

	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private Menu					_contextMenu;
	private Cursor					_cursorHand;

	private Composite				_shellContainer;

	private Label					_lblFilterIcon;

	private Link					_lnkFilterText;

	private class ActionTTFilter extends Action {

		private TourTypeFilter	__ttFilter;
		private ImageDescriptor	__filterImageDescriptor;

		public ActionTTFilter(final TourTypeFilter ttFilter) {

			super(ttFilter.getFilterName(), AS_PUSH_BUTTON);

			__ttFilter = ttFilter;
			__filterImageDescriptor = TourTypeFilter.getFilterImageDescriptor(ttFilter);
		}

		@Override
		public void run() {

			selectCollateFilter(__ttFilter);
		}

		@Override
		public String toString() {
			return __ttFilter.toString();
		}
	}

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public SlideoutCollateTours(final Control ownerControl,
								final ToolBar toolBar,
								final IDialogSettings state,
								final TourBookView tourBookView) {

		super(ownerControl);

		_tourBookView = tourBookView;

		addListener(ownerControl, toolBar);

		setToolTipCreateStyle(AnimatedToolTipShell.TOOLTIP_STYLE_KEEP_CONTENT);
		setBehaviourOnMouseOver(AnimatedToolTipShell.MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
		setIsKeepShellOpenWhenMoved(false);

		setFadeInSteps(1);
		setFadeOutSteps(10);
		setFadeOutDelaySteps(1);
	}

	private void addListener(final Control ownerControl, final ToolBar toolBar) {

		toolBar.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(final MouseEvent e) {

				// prevent to open the tooltip
				_canOpenToolTip = false;
			}
		});
	}

	@Override
	protected void beforeHideToolTip() {

	}

	@Override
	protected boolean canCloseToolTip() {

		boolean isCanClose = true;

		if (_contextMenu.isVisible() || _isContextOpening || _tourBookView.isInUIUpdate()) {

			isCanClose = false;
		}

		return isCanClose;
	}

	@Override
	protected boolean canShowToolTip() {
		return true;
	}

	@Override
	protected boolean closeShellAfterHidden() {

		/*
		 * Close the tooltip that the state is saved.
		 */

		return true;
	}

	private void createActions() {

		_actionOpenTourTypePrefs = new ActionOpenPrefDialog(
				Messages.Action_TourType_ModifyTourTypeFilter,
				ITourbookPreferences.PREF_PAGE_TOUR_TYPE_FILTER);

		_actionOpenTourTypePrefs.setShell(_tourBookView.getShell());
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI(parent);

		final Composite ui = createUI(parent);

		createActions();

		restoreState();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
		{
			final Composite container = new Composite(_shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults()//
					.numColumns(1)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			{
				createUI_10_Title(container);
				createUI_30_TourTypeSelector(container);
			}
		}

		return _shellContainer;
	}

	private void createUI_10_Title(final Composite parent) {

		/*
		 * Label: Slideout title
		 */
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(Messages.Slideout_CollatedTours_Label_Title);
		label.setFont(JFaceResources.getBannerFont());
	}

	private void createUI_30_TourTypeSelector(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(container);
		container.addMouseListener(_mouseListener);
		container.addMouseTrackListener(_mouseTrackListener);
		container.addMouseWheelListener(_mouseWheelListener);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.spacing(0, 0)
				.applyTo(container);
//		_filterContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI_32_FilterIcon(container);
			createUI_34_FilterText(container);
			createUI_36_ContextMenu();
		}
	}

	private void createUI_32_FilterIcon(final Composite parent) {

		_lblFilterIcon = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(false, true)
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_lblFilterIcon);

		_lblFilterIcon.addMouseListener(_mouseListener);
		_lblFilterIcon.addMouseTrackListener(_mouseTrackListener);
		_lblFilterIcon.addMouseWheelListener(_mouseWheelListener);
	}

	private void createUI_34_FilterText(final Composite parent) {

		_lnkFilterText = new Link(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.hint(_collateNameWidth, SWT.DEFAULT)
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_lnkFilterText);

		_lnkFilterText.addMouseListener(_mouseListener);
		_lnkFilterText.addMouseTrackListener(_mouseTrackListener);
		_lnkFilterText.addMouseWheelListener(_mouseWheelListener);

		_lnkFilterText.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				openContextMenu();
			}
		});

		_lnkFilterText.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(final KeyEvent e) {
				switch (e.keyCode) {

				case SWT.ARROW_UP:
					selectCollateFilter_Next(false);
					break;

				case SWT.ARROW_DOWN:
					selectCollateFilter_Next(true);
					break;

				/*
				 * These keys must be set because when the context menu is close, these keys are not
				 * working any more. They are working after the controls gets the focus
				 */
				case SWT.CR:
				case ' ':
					openContextMenu();
					break;

				default:
					break;
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {}
		});
	}

	/**
	 * create tour type filter context menu
	 */
	private void createUI_36_ContextMenu() {

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr) {

				// set all menu items
				fillContextMenu(menuMgr);
			}
		});

		// set context menu and set the parent to the tour type filter icon
		_contextMenu = menuMgr.createContextMenu(_lblFilterIcon);

		_contextMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuHidden(final MenuEvent e) {
//				int a = 0;
//				a++;
			}

			@Override
			public void menuShown(final MenuEvent e) {
//				int a = 0;
//				a++;
			}
		});

		_lblFilterIcon.setMenu(_contextMenu);
	}

	/**
	 * Fills the tour type filter context menu.
	 * 
	 * @param menuMgr
	 */
	private void fillContextMenu(final IMenuManager menuMgr) {

//		final TourTypeFilter activeTTFilter = TourbookPlugin.getActiveTourTypeFilter();

		final ArrayList<TourTypeFilter> _tourTypeFilters = CollateTourManager.getAllCollateFilters();

		for (final TourTypeFilter tourTypeFilter : _tourTypeFilters) {

			final ActionTTFilter ttFilterAction = new ActionTTFilter(tourTypeFilter);

			// check filter which is currently selected in the UI
			final boolean isChecked = _selectCollateFilter == ttFilterAction.__ttFilter;

			ttFilterAction.setChecked(isChecked);
			ttFilterAction.setEnabled(isChecked == false);

			String filterName;

			if (isChecked) {
				filterName = ">>> " + ttFilterAction.__ttFilter.getFilterName() + " <<<";//$NON-NLS-1$ //$NON-NLS-2$
			} else {
				filterName = ttFilterAction.__ttFilter.getFilterName();
			}

			ttFilterAction.setText(filterName);

			// disabled filter image is hidden because it look ugly on win32
			ttFilterAction.setImageDescriptor(isChecked ? null : ttFilterAction.__filterImageDescriptor);

			menuMgr.add(ttFilterAction);
		}

		menuMgr.add(new Separator());
		menuMgr.add(_actionOpenTourTypePrefs);
	}

	public Shell getShell() {

		if (_shellContainer == null) {
			return null;
		}

		return _shellContainer.getShell();
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

//		final int tipWidth = tipSize.x;
		final int tipHeight = tipSize.y;

//		final int itemWidth = _toolTipItemBounds.width;
		final int itemHeight = _toolTipItemBounds.height;

		// center horizontally
		final int devX = _toolTipItemBounds.x;// + itemWidth / 2 - tipWidth / 2;
		int devY = _toolTipItemBounds.y + itemHeight + 0;

		final Rectangle displayBounds = this.getShell().getDisplay().getBounds();

		if (devY + tipHeight > displayBounds.height) {

			// slideout is below bottom, show it above the action button

			devY = _toolTipItemBounds.y - tipHeight;
		}

		return new Point(devX, devY);

	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_cursorHand = new Cursor(parent.getDisplay(), SWT.CURSOR_HAND);

		_collateNameWidth = _pc.convertWidthInCharsToPixels(20);

		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		_mouseWheelListener = new MouseWheelListener() {

			private int	__lastEventTime;

			@Override
			public void mouseScrolled(final MouseEvent event) {

				if (event.time == __lastEventTime) {
					// prevent doing the same for the same event, this occured when mouse is scrolled -> the event is fired 2x times
					return;
				}

				if (_contextMenu.isDisposed()) {
					return;
				}
				_contextMenu.setVisible(false);
				_lnkFilterText.setFocus();

				__lastEventTime = event.time;

				selectCollateFilter_Next(event.count < 0);
			}
		};

		_mouseListener = new MouseListener() {
			@Override
			public void mouseDoubleClick(final MouseEvent e) {}

			@Override
			public void mouseDown(final MouseEvent e) {
				_lnkFilterText.setFocus();
				openContextMenu();
			}

			@Override
			public void mouseUp(final MouseEvent e) {}
		};

		_mouseTrackListener = new MouseTrackListener() {
			@Override
			public void mouseEnter(final MouseEvent e) {
				if (e.widget instanceof Control) {

					final Control control = (Control) e.widget;

					if (control.isDisposed()) {

						/**
						 * This error occures when the customized dialog for the perspective is
						 * opened -> needs to be fixed.
						 */

						return;
					}

					control.setCursor(_cursorHand);

					openContextMenu_Open(control, e);
				}
			}

			@Override
			public void mouseExit(final MouseEvent e) {
				if (e.widget instanceof Control) {

					final Control control = (Control) e.widget;
					control.setCursor(null);

					openContextMenu_Hide(control, e);
				}
			}

			@Override
			public void mouseHover(final MouseEvent e) {}
		};
	}

	@Override
	protected boolean isInNoHideArea(final Point displayCursorLocation) {

		return _tourBookView.isInUIUpdate();
	}

	@Override
	protected Rectangle noHideOnMouseMove() {

		return _toolTipItemBounds;
	}

	private void onDispose() {

		UI.disposeResource(_cursorHand);
	}

	/**
	 * @param toolTipItemBounds
	 * @param isOpenDelayed
	 */
	public void open(final Rectangle toolTipItemBounds, final boolean isOpenDelayed) {

		if (isToolTipVisible()) {
			return;
		}

		if (isOpenDelayed == false) {

			if (toolTipItemBounds != null) {

				_toolTipItemBounds = toolTipItemBounds;

				showToolTip();
			}

		} else {

			if (toolTipItemBounds == null) {

				// item is not hovered any more

				_canOpenToolTip = false;

				return;
			}

			_toolTipItemBounds = toolTipItemBounds;
			_canOpenToolTip = true;

			if (_isWaitTimerStarted == false) {

				_isWaitTimerStarted = true;

				Display.getCurrent().timerExec(50, _waitTimer);
			}
		}
	}

	private void open_Runnable() {

		_isWaitTimerStarted = false;

		if (_canOpenToolTip) {
			showToolTip();
		}
	}

	private void openContextMenu() {

		if (_contextMenu.isVisible() || _isContextOpening) {
			return;
		}

		_isContextOpening = true;

		final Rectangle rect = _lblFilterIcon.getBounds();
		Point pt = new Point(rect.x, rect.y + rect.height);
		pt = _lblFilterIcon.getParent().toDisplay(pt);

		_contextMenu.setLocation(pt.x, pt.y);
		_contextMenu.setVisible(true);

		_isContextOpening = false;
	}

	private void openContextMenu_Hide(final Control control, final MouseEvent mouseEvent) {

		_lastHideTime = mouseEvent.time & 0xFFFFFFFFL;
	}

	private void openContextMenu_Open(final Control control, final MouseEvent mouseEvent) {

		if (_isShowTourTypeContextMenu == false || _contextMenu.isVisible() || _isContextOpening) {
			// nothing to do
			return;
		}

		_lastOpenTime = mouseEvent.time & 0xFFFFFFFFL;

		/*
		 * delay opening that the context is not opened when the tour type label is only hovered and
		 * something else is selected
		 */
		Display.getDefault().timerExec(500, new Runnable() {
			@Override
			public void run() {

				// check if a hide event has occured
				if (_lastHideTime > _lastOpenTime) {
					return;
				}

				openContextMenu();
			}
		});
	}

	private void restoreState() {

		restoreTourTypeFilter();
	}

	private void restoreTourTypeFilter() {

		_selectCollateFilter = CollateTourManager.getSelectedCollateFilter();

		// try to reselect the last tour type filter
		updateUI(_selectCollateFilter);
	}

	private void selectCollateFilter(final TourTypeFilter ttFilter) {

		_selectCollateFilter = ttFilter;

		updateUI(ttFilter);

		CollateTourManager.setSelectedCollateFilter(ttFilter);

		// run async that the UI is updated before a longer job to get the data from the db
		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {

				if (_cursorHand.isDisposed()) {
					return;
				}

				// the tree root items gets the selected tour type filter from the collate manager
				_tourBookView.reloadViewer();
			}
		});
	}

	private void selectCollateFilter_Next(final boolean isNext) {

		final ArrayList<TourTypeFilter> collateFilters = CollateTourManager.getAllCollateFilters();

		int selectedFilterIndex = 0;

		// get filter which is currently selected
		for (final TourTypeFilter collateFilter : collateFilters) {

			if (collateFilter == _selectCollateFilter) {
				break;
			}

			selectedFilterIndex++;
		}

		if (isNext && selectedFilterIndex < collateFilters.size() - 1) {

			// select next filter

			selectCollateFilter(collateFilters.get(++selectedFilterIndex));

		} else if (isNext == false && selectedFilterIndex > 0) {

			// select previous filter

			selectCollateFilter(collateFilters.get(--selectedFilterIndex));
		}
	}

	private void updateUI(final TourTypeFilter ttFilter) {

		// prevent endless loops
		if (_isUIUpdating) {
			return;
		}

		_isUIUpdating = true;
		{
			final boolean isTTFilter = ttFilter != null;

			final String filterName = isTTFilter //
					? ttFilter.getFilterName()
					: Messages.Slideout_CollatedTours_Label_SelectTourType;

			final String shortFilterName = UI.shortenText(filterName, _lnkFilterText, _collateNameWidth, true);
			final String filterTooltip = updateUI_TooltipText(isTTFilter, ttFilter, filterName);

			_lnkFilterText.setText("<a>" + shortFilterName + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
			_lnkFilterText.setToolTipText(filterTooltip);

			if (isTTFilter) {
				_lblFilterIcon.setImage(TourTypeFilter.getFilterImage(ttFilter));
			}
		}
		_isUIUpdating = false;
	}

	/**
	 * Create filter tooltip.
	 */
	private String updateUI_TooltipText(final boolean isTTFilter, final TourTypeFilter ttFilter, final String filterName) {

		String filterTooltip;

		if (isTTFilter) {

			if (ttFilter.getFilterType() == TourTypeFilter.FILTER_TYPE_TOURTYPE_SET) {

				final StringBuilder sb = new StringBuilder();
				sb.append(Messages.App_TourType_ToolTipTitle);
				sb.append(filterName);
				sb.append(UI.NEW_LINE2);
				sb.append(Messages.App_TourType_ToolTip);

				final TourTypeFilterSet ttSet = ttFilter.getTourTypeSet();
				if (ttSet != null) {

					int counter = 0;

					for (final Object ttItem : ttSet.getTourTypes()) {
						if (ttItem instanceof TourType) {
							final TourType ttFilterFromSet = (TourType) ttItem;

							if (counter == 0) {
								sb.append("\n"); //$NON-NLS-1$
							}

							sb.append("\n\t\t\t"); //$NON-NLS-1$
							sb.append(ttFilterFromSet.getName());

							counter++;
						}
					}
				}
				filterTooltip = sb.toString();

			} else {
				filterTooltip = filterName;
			}

		} else {

			filterTooltip = filterName;
		}

		return filterTooltip;
	}

}
