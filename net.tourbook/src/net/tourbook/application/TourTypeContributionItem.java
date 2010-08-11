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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PlatformUI;

public class TourTypeContributionItem extends CustomControlContribution {

	private static final String	ID	= "net.tourbook.tourtypefilter";	//$NON-NLS-1$

	private Button				_btnTTFilter;

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

		Control returnControl;

		if (UI.IS_OSX) {

			returnControl = createUI(parent);

		} else {

//			/*
//			 * on win32 a few pixel above and below the combobox are drawn, wrapping it into a
//			 * composite removes the pixels
//			 */
//			final Composite container = new Composite(parent, SWT.NONE);
//			GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
//			{
//				final Control uiControl = createUI(container);
//				uiControl.setLayoutData(new GridData(SWT.NONE, SWT.CENTER, false, true));
//			}
//
//			returnControl = container;

			returnControl = createUI(parent);
		}

		TourTypeFilterManager.reselectLastTourTypeFilter(this);

		return returnControl;
	}

	private Control createUI(final Composite parent) {

		_btnTTFilter = new Button(parent, SWT.FLAT | SWT.LEFT);

		_btnTTFilter.setSize(100, _btnTTFilter.getSize().x);

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

		_btnTTFilter.setText(ttFilter.getFilterName());
		_btnTTFilter.setToolTipText(ttFilter.getFilterName());

		_btnTTFilter.setImage(TourTypeFilter.getFilterImage(ttFilter));
	}

}
