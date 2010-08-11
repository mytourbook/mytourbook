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

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PlatformUI;

public class TourTypeContributionItem extends CustomControlContribution {

	private static final String	ID	= "net.tourbook.tourtypefilter";	//$NON-NLS-1$

	private Button				_btnTTFilter;

	private Point				_textSize;

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
			_textSize = gc.textExtent("0123456789");
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
				GridDataFactory.fillDefaults() //
						.align(SWT.FILL, SWT.CENTER)
						.grab(false, true)
						.hint(_textSize.x, SWT.DEFAULT)
						.applyTo(uiControl);
			}

			returnControl = container;

//			returnControl = createUI(parent);
		}

//		_btnTTFilter.setSize(textSize);

		TourTypeFilterManager.reselectLastTourTypeFilter(this);

		return returnControl;
	}

	private Control createUI(final Composite parent) {

		_btnTTFilter = new Button(parent, SWT.PUSH /* | SWT.FLAT */| SWT.LEAD);

		_btnTTFilter.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				TourTypeFilterManager.selectNextFilter(event.count < 0);
			}
		});
		_btnTTFilter.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				UI.openControlMenu(_btnTTFilter);
			}
		});

		/*
		 * tour type menu
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

		// set menu for the tag item
		final Menu contextMenu = menuMgr.createContextMenu(_btnTTFilter);
		_btnTTFilter.setMenu(contextMenu);

		return _btnTTFilter;
	}

	public void updateUI(final TourTypeFilter ttFilter) {

		final String filterName = ttFilter.getFilterName();
		final String shortFilterName = UI.shortenText(filterName, //
				_btnTTFilter,
//				_textSize.x - 16 - 5 - 5,
				_textSize.x - 16 - 5 - 5,
				true);

		_btnTTFilter.setText(shortFilterName);
		_btnTTFilter.setToolTipText(filterName);
		_btnTTFilter.setImage(TourTypeFilter.getFilterImage(ttFilter));
	}
}
