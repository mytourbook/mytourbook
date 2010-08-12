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

import net.tourbook.tour.TourTypeFilterManager;
import net.tourbook.ui.CustomControlContribution;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.util.UI;
import net.tourbook.util.Util;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;
 
public class TourTypeContributionItem extends CustomControlContribution {

	private static final String		ID							= "net.tourbook.tourTypeFilter";		//$NON-NLS-1$

	public static final String		STATE_IS_SHOW_FILTER_TEXT	= "";									//$NON-NLS-1$
	public static final String		STATE_TEXT_LENGTH_IN_CHAR	= "";									//$NON-NLS-1$

	private final IDialogSettings	_state						= TourbookPlugin.getDefault() //
																		.getDialogSettingsSection(ID);

	private ToolItem				_tourTypeDropDown;

	private int						_textWidth;

	private ToolBar					_tourTypeToolBar;

	private boolean					_isUpdating;

	private boolean					_isShowText					= Util.getStateBoolean(
																		_state,
																		STATE_IS_SHOW_FILTER_TEXT,
																		false);
	private int						_textLengthInChar			= Util.getStateInt(
																		_state,
																		STATE_TEXT_LENGTH_IN_CHAR,
																		15);

	public TourTypeContributionItem() {
		this(ID);
	}

	protected TourTypeContributionItem(final String id) {
		super(id);
	}

	@Override
	protected Control createControl(final Composite parent) {

		if (PlatformUI.getWorkbench().isClosing()) {
			return new Label(parent, SWT.NONE);
		}

		final GC gc = new GC(parent);
		{
			_textWidth = gc.getFontMetrics().getAverageCharWidth() * _textLengthInChar;
		}
		gc.dispose();

		Control returnControl;

		if (UI.IS_OSX) {

			returnControl = createUI(parent);

		} else {

			/*
			 * on win32/linux a few pixel above and below the combobox are drawn, wrapping it into a
			 * composite removes the pixels
			 */
			final Composite container = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			{
				final Control uiControl = createUI(container);

				if (_isShowText) {
					GridDataFactory.fillDefaults() //
							.align(SWT.FILL, SWT.CENTER)
							.grab(false, true)
							.hint(_textWidth + 16 + 10, SWT.DEFAULT)
							.applyTo(uiControl);
				} else {
					GridDataFactory.fillDefaults() //
							.align(SWT.FILL, SWT.CENTER)
							.grab(false, true)
							.applyTo(uiControl);
				}
			}

			returnControl = container;

//			returnControl = createUI(parent);
		}

		TourTypeFilterManager.reselectLastTourTypeFilter(this);

		return returnControl;
	}

	private Control createUI(final Composite parent) {

		/*
		 * tour type filter toolbar which contains a drop down tooltitem button
		 */
		_tourTypeToolBar = new ToolBar(parent, SWT.FLAT | SWT.RIGHT);
//		_tourTypeToolBar.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		_tourTypeDropDown = new ToolItem(_tourTypeToolBar, SWT.DROP_DOWN);

		_tourTypeDropDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final ToolBar control = _tourTypeDropDown.getParent();
				UI.openControlMenu(control);
			}
		});

		_tourTypeToolBar.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				TourTypeFilterManager.selectNextFilter(event.count < 0);
			}
		});

		/*
		 * create tour type context menu
		 */
		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr) {

				// set menu items

				TourTypeFilterManager.fillMenu(menuMgr);
			}
		});

		// set menu for the toolbar, drop down tool item do not contain a control
		final Menu contextMenu = menuMgr.createContextMenu(_tourTypeToolBar);
		_tourTypeToolBar.setMenu(contextMenu);

		return _tourTypeToolBar;
	}

	public void updateUI(final TourTypeFilter ttFilter) {

		// prevent endless loops, during testing/debugging the toolbar was disposed
		if (_isUpdating || _tourTypeToolBar.isDisposed()) {
			return;
		}

		final String filterName = ttFilter.getFilterName();

		if (_isShowText) {

			final String shortFilterName = UI.shortenText(filterName, //
					_tourTypeToolBar,
					_textWidth - 16 - 10,
					true);

			_tourTypeDropDown.setText(shortFilterName);
		}

		_tourTypeDropDown.setToolTipText(filterName);
		_tourTypeDropDown.setImage(TourTypeFilter.getFilterImage(ttFilter));
	}
}
