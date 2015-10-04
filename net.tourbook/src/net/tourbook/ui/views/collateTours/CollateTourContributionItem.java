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
package net.tourbook.ui.views.collateTours;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.data.TourType;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.TourTypeFilterSet;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
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

class CollateTourContributionItem extends ControlContribution {

	private static final String		ID					= "net.tourbook.tourTypeFilter";	//$NON-NLS-1$

	private CollatedToursView		_collatedToursView;

	private MouseListener			_mouseListener;
	private MouseTrackListener		_mouseTrackListener;
	private MouseWheelListener		_mouseWheelListener;

	private ActionOpenPrefDialog	_actionOpenTourTypePrefs;

	private int						_collateNameWidth;

	private boolean					_isUIUpdating;
	private boolean					_isContextOpening	= false;

	private TourTypeFilter			_selectCollateFilter;

	/*
	 * UI controls
	 */
	private Menu					_contextMenu;
	private Cursor					_cursorHand;

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

	public CollateTourContributionItem(final CollatedToursView collatedToursView) {

		super(ID);

		_collatedToursView = collatedToursView;
	}

	private void createActions() {

		_actionOpenTourTypePrefs = new ActionOpenPrefDialog(
				Messages.Action_TourType_ModifyTourTypeFilter,
				ITourbookPreferences.PREF_PAGE_TOUR_TYPE_FILTER);

		_actionOpenTourTypePrefs.setShell(_collatedToursView.getShell());
	}

	@Override
	protected Control createControl(final Composite parent) {

		initUI(parent);

		final Composite ui = createUI(parent);

		createActions();

		restoreState();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.spacing(0, 0)
				.applyTo(container);

		container.addMouseListener(_mouseListener);
		container.addMouseTrackListener(_mouseTrackListener);
		container.addMouseWheelListener(_mouseWheelListener);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI_32_FilterIcon(container);
			createUI_34_FilterText(container);
			createUI_36_ContextMenu();
		}

		return container;
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
//		_lnkFilterText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

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

	@Override
	public void dispose() {

		UI.disposeResource(_cursorHand);

		super.dispose();
	}

	/**
	 * Fills the tour type filter context menu.
	 * 
	 * @param menuMgr
	 */
	private void fillContextMenu(final IMenuManager menuMgr) {

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

	private void initUI(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);

		_cursorHand = new Cursor(parent.getDisplay(), SWT.CURSOR_HAND);

		_collateNameWidth = pc.convertWidthInCharsToPixels(20);

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
				}
			}

			@Override
			public void mouseExit(final MouseEvent e) {
				if (e.widget instanceof Control) {

					final Control control = (Control) e.widget;
					control.setCursor(null);
				}
			}

			@Override
			public void mouseHover(final MouseEvent e) {}
		};
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


	private void restoreState() {

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
				_collatedToursView.reloadViewer();
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
					: Messages.Collate_Tours_Link_SelectTourType;

			final String shortFilterName = UI.shortenText(filterName, _lnkFilterText, _collateNameWidth, true);

			final String filterTooltip = isTTFilter
					? updateUI_TooltipText(ttFilter, filterName)
					: Messages.Collate_Tours_Link_SelectTourType_Tooltip;

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
	private String updateUI_TooltipText(final TourTypeFilter ttFilter, final String filterName) {

		String filterTooltip;

		final int filterType = ttFilter.getFilterType();

		if (filterType == TourTypeFilter.FILTER_TYPE_TOURTYPE_SET) {

			final StringBuilder sb = new StringBuilder();
			sb.append(Messages.Collate_Tours_Label_TooltipHeader_Multiple);
			sb.append(filterName);
			sb.append(UI.NEW_LINE2);
			sb.append(Messages.App_TourType_ToolTip);

			final TourTypeFilterSet ttSet = ttFilter.getTourTypeSet();
			if (ttSet != null) {

				int counter = 0;

				for (final Object ttItem : ttSet.getTourTypes()) {
					if (ttItem instanceof TourType) {
						final TourType ttFilterFromSet = (TourType) ttItem;

						if (counter > 0) {
							sb.append("\n\t\t"); //$NON-NLS-1$
						}

						sb.append("\t"); //$NON-NLS-1$
						sb.append(ttFilterFromSet.getName());

						counter++;
					}
				}
			}
			filterTooltip = sb.toString();

		} else if (filterType == TourTypeFilter.FILTER_TYPE_DB) {

			filterTooltip = NLS.bind(Messages.Collate_Tours_Label_TooltipHeader_Single, filterName);

		} else {

			filterTooltip = filterName;
		}

		return filterTooltip;
	}
}
