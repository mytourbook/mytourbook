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

import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.tour.TourTypeFilterManager;
import net.tourbook.ui.CustomControlContribution;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PlatformUI;

public class TourTypeContributionItem_Button extends CustomControlContribution {

	private static final String		ID							= "net.tourbook.tourTypeFilter";		//$NON-NLS-1$

	public static final String		STATE_IS_SHOW_FILTER_TEXT	= "";									//$NON-NLS-1$
	public static final String		STATE_TEXT_LENGTH_IN_CHAR	= "";									//$NON-NLS-1$

	private final IDialogSettings	_state						= TourbookPlugin.getDefault() //
																		.getDialogSettingsSection(ID);

	private ToolBarContributionItem	_tbItemTourType;
	private int						_textWidth;

	private boolean					_isUpdating;

	private boolean					_isShowText					= Util.getStateBoolean(
																		_state,
																		STATE_IS_SHOW_FILTER_TEXT,
																		true);
	private int						_textLengthInChar			= Util.getStateInt(
																		_state,
																		STATE_TEXT_LENGTH_IN_CHAR,
																		25);

	private Button					_ttFilter;

	public TourTypeContributionItem_Button() {
		this(ID);
	}

	protected TourTypeContributionItem_Button(final String id) {
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

//		TourTypeFilterManager.reselectLastTourTypeFilter(this);

		return returnControl;
	}

	private Control createUI(final Composite parent) {

		_ttFilter = new Button(parent, SWT.NONE);
//		_ttFilter.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		_ttFilter.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				UI.openControlMenu(_ttFilter);
			}
		});

		_ttFilter.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(final KeyEvent e) {
				switch (e.keyCode) {

				case SWT.ARROW_UP:
					TourTypeFilterManager.selectNextFilter(false);
					break;

				case SWT.ARROW_DOWN:
					TourTypeFilterManager.selectNextFilter(true);
					break;

				default:
					break;
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {}
		});

		_ttFilter.addMouseWheelListener(new MouseWheelListener() {
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
		final Menu contextMenu = menuMgr.createContextMenu(_ttFilter);
		_ttFilter.setMenu(contextMenu);

		return _ttFilter;
	}

	public void setToolBarContribItem(final ToolBarContributionItem tbItemTourType) {
		_tbItemTourType = tbItemTourType;
	}

	public void updateUI(final TourTypeFilter ttFilter) {

		// prevent endless loops, during testing/debugging the toolbar was disposed
		if (_isUpdating || _ttFilter.isDisposed()) {
			return;
		}

		_isUpdating = true;
		{
			final String filterName = ttFilter.getFilterName();

			if (_isShowText) {

				final String shortFilterName = UI.shortenText(filterName, //
						_ttFilter,
						_textWidth - 16 - 10,
						true);

				_ttFilter.setText(shortFilterName);
			}

			_ttFilter.setToolTipText(filterName);
			_ttFilter.setImage(TourTypeFilter.getFilterImage(ttFilter));

//			_tbItemTourType.update(ICoolBarManager.SIZE);
		}
		_isUpdating = false;
	}
}
