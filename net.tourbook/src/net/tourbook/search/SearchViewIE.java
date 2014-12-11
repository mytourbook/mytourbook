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
package net.tourbook.search;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class SearchViewIE extends ViewPart {

	public static final String	ID	= "net.tourbook.search.SearchViewIE";	//$NON-NLS-1$

	private SearchWebService				_search;

	@Override
	public void createPartControl(final Composite parent) {

		Browser browser;
		try {

			browser = new Browser(parent, SWT.NONE);

		} catch (final SWTError e) {
			System.out.println("Could not instantiate Browser: " + e.getMessage());
			return;
		}

		_search = new SearchWebService(browser);
	}

	@Override
	public void setFocus() {
		_search.setFocus();
	}

}
