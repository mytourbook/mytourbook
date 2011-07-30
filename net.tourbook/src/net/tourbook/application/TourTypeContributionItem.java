/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.application;

import net.tourbook.Messages;
import net.tourbook.data.TourType;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourTypeFilterManager;
import net.tourbook.ui.CustomControlContribution;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.TourTypeFilterSet;
import net.tourbook.util.UI;
import net.tourbook.util.Util;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.ui.PlatformUI;

public class TourTypeContributionItem extends CustomControlContribution {

	private static final String		ID							= "net.tourbook.tourTypeFilter";					//$NON-NLS-1$

	public static final String		STATE_TEXT_LENGTH_IN_CHAR	= "";												//$NON-NLS-1$

	private final IDialogSettings	_state						= TourbookPlugin.getDefault() //
																		.getDialogSettingsSection(ID);

	private final IPreferenceStore	_prefStore					= TourbookPlugin.getDefault().getPreferenceStore();

	private int						_textWidth;
	private final int				_textLengthInChar			= Util.getStateInt(
																		_state,
																		STATE_TEXT_LENGTH_IN_CHAR,
																		18);

	private Cursor					_cursorHand;

	private Label					_lblFilterIcon;
	private Link					_lnkFilterText;

	private MouseWheelListener		mouseWheelListener;
	private MouseListener			mouseListener;
	private MouseTrackListener		mouseTrackListener;

	private boolean					_isUIUpdating;

	private Menu					_contextMenu;

	private IPropertyChangeListener	_prefListener;

	private boolean					_isContextOpening			= false;
	private boolean					_isShowTourTypeContextMenu;

	private Composite				_filterContainer;

	private long					_lastOpenTime;
	private long					_lastHideTime;

	{
		mouseWheelListener = new MouseWheelListener() {

			private int	__lastEventTime;

			@Override
			public void mouseScrolled(final MouseEvent event) {

				if (event.time == __lastEventTime) {
					// prevent doing the same for the same event, this occured when mouse is scrolled -> the event is fired 2x times
					return;
				}

				__lastEventTime = event.time;

				TourTypeFilterManager.selectNextFilter(event.count < 0);

				_contextMenu.setVisible(false);
				_lnkFilterText.setFocus();
			}
		};

		mouseListener = new MouseListener() {
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

		mouseTrackListener = new MouseTrackListener() {
			@Override
			public void mouseEnter(final MouseEvent e) {
				if (e.widget instanceof Control) {

					final Control control = (Control) e.widget;
					control.setCursor(_cursorHand);

					openContextMenuOpen(control, e);
				}
			}

			@Override
			public void mouseExit(final MouseEvent e) {
				if (e.widget instanceof Control) {

					final Control control = (Control) e.widget;
					control.setCursor(null);

					openContextMenuHide(control, e);

//					/*
//					 * this is not working because the menu gets the focus and I didn't find a
//					 * solution to hide the context menu when the mouse is moving outside of the
//					 * context menu, 5.10.2010
//					 */
//					if (_contextMenu.isVisible()) {
//						_contextMenu.setVisible(false);
//					}
				}
			}

			@Override
			public void mouseHover(final MouseEvent e) {}
		};
	}

	public TourTypeContributionItem() {
		this(ID);
	}

	protected TourTypeContributionItem(final String id) {
		super(id);
	}

	private void addPrefListener() {

		_prefListener = new IPropertyChangeListener() {

			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();
				if (property.equals(ITourbookPreferences.APPEARANCE_SHOW_TOUR_TYPE_CONTEXT_MENU)) {

					if (event.getNewValue() instanceof Boolean) {
						_isShowTourTypeContextMenu = (Boolean) event.getNewValue();
					}
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefListener);
	}

	@Override
	protected Control createControl(final Composite parent) {

		if (PlatformUI.getWorkbench().isClosing()) {
			return new Label(parent, SWT.NONE);
		}

		final Control ui = createUI(parent);

		_cursorHand = new Cursor(parent.getDisplay(), SWT.CURSOR_HAND);

		TourTypeFilterManager.restoreTourTypeFilter();
		addPrefListener();
		restoreState();

		return ui;
	}

	private Control createUI(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);
		_textWidth = pc.convertWidthInCharsToPixels(_textLengthInChar);

		_filterContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_filterContainer);
		_filterContainer.addMouseListener(mouseListener);
		_filterContainer.addMouseTrackListener(mouseTrackListener);
		_filterContainer.addMouseWheelListener(mouseWheelListener);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).applyTo(_filterContainer);
//		_filterContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI10FilterIcon(_filterContainer);
			createUI20FilterText(_filterContainer);
			createUI30ContextMenu();
		}

		return _filterContainer;
	}

	private void createUI10FilterIcon(final Composite parent) {

		_lblFilterIcon = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).align(SWT.FILL, SWT.CENTER).applyTo(_lblFilterIcon);

		_lblFilterIcon.addMouseListener(mouseListener);
		_lblFilterIcon.addMouseTrackListener(mouseTrackListener);
		_lblFilterIcon.addMouseWheelListener(mouseWheelListener);
	}

	private void createUI20FilterText(final Composite parent) {

		_lnkFilterText = new Link(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.hint(_textWidth, SWT.DEFAULT)
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_lnkFilterText);

		_lnkFilterText.addMouseListener(mouseListener);
		_lnkFilterText.addMouseTrackListener(mouseTrackListener);
		_lnkFilterText.addMouseWheelListener(mouseWheelListener);

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
					TourTypeFilterManager.selectNextFilter(false);
					break;

				case SWT.ARROW_DOWN:
					TourTypeFilterManager.selectNextFilter(true);
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
	private void createUI30ContextMenu() {

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr) {
				// set all menu items
				TourTypeFilterManager.fillMenu(menuMgr);
			}
		});

		// set context menu and set the parent to the tour type filter icon
		_contextMenu = menuMgr.createContextMenu(_lblFilterIcon);

		_contextMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuHidden(final MenuEvent e) {
				int a = 0;
				a++;
			}

			@Override
			public void menuShown(final MenuEvent e) {
				int a = 0;
				a++;
			}
		});

		_lblFilterIcon.setMenu(_contextMenu);
	}

	@Override
	public void dispose() {

		if (_cursorHand != null) {
			_cursorHand.dispose();
		}

		_prefStore.removePropertyChangeListener(_prefListener);

		super.dispose();
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

//		/**
//		 * Eventloop MUST be run otherwise the first click in the menu is not executed because of
//		 * the delayed execution
//		 */
//		final Display display = _contextMenu.getDisplay();
//
//		while (!_contextMenu.isDisposed() && _contextMenu.isVisible()) {
//			if (!display.readAndDispatch()) {
//				display.sleep();
//			}
//		}

		_isContextOpening = false;
	}

	private void openContextMenuHide(final Control control, final MouseEvent mouseEvent) {
		_lastHideTime = mouseEvent.time & 0xFFFFFFFFL;
	}

	private void openContextMenuOpen(final Control control, final MouseEvent mouseEvent) {

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

		_isShowTourTypeContextMenu = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_SHOW_TOUR_TYPE_CONTEXT_MENU);
	}

	public void updateUI(final TourTypeFilter ttFilter) {

		// prevent endless loops
		if (_isUIUpdating) {
			return;
		}

		_isUIUpdating = true;
		{
			final String filterName = ttFilter.getFilterName();
			final String shortFilterName = UI.shortenText(filterName, _lnkFilterText, _textWidth, true);

			/*
			 * create filter tooltip
			 */
			String filterTooltip;
			if (ttFilter.getFilterType() == TourTypeFilter.FILTER_TYPE_TOURTYPE_SET) {

				final StringBuilder sb = new StringBuilder();
				sb.append(Messages.App_TourType_ToolTipTitle);
				sb.append(filterName);
				sb.append(UI.NEW_LINE2);
				sb.append(Messages.App_TourType_ToolTip);
//				sb.append(UI.NEW_LINE);

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

			_lnkFilterText.setText("<a>" + shortFilterName + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
			_lnkFilterText.setToolTipText(filterTooltip);
			_lblFilterIcon.setImage(TourTypeFilter.getFilterImage(ttFilter));
		}
		_isUIUpdating = false;
	}
}
