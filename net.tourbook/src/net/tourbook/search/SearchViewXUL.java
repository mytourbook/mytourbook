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

import net.tourbook.web.WebContentServer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class SearchViewXUL extends ViewPart {

	public static final String	ID	= "net.tourbook.search.SearchViewXUL";	//$NON-NLS-1$

	private Search				_search;

	private IPartListener2		_partListener;

	private void addPartListener() {

		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {

				if (partRef.getPart(false) == SearchViewXUL.this) {

					// stop webserver for debugging
					WebContentServer.stop();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		getViewSite().getPage().addPartListener(_partListener);
	}

	@Override
	public void createPartControl(final Composite parent) {

		Browser browser;

		try {

			System.setProperty("org.eclipse.swt.browser.XULRunnerPath", "C:\\E\\XULRunner\\xulrunner-10-32");

			browser = new Browser(parent, SWT.MOZILLA);

		} catch (final SWTError e) {
			System.out.println("Could not instantiate Browser: " + e.getMessage());
			return;
		}

		addPartListener();

		_search = new Search(browser);
	}

	@Override
	public void dispose() {

		getViewSite().getPage().removePartListener(_partListener);

		super.dispose();
	}

	@Override
	public void setFocus() {
		_search.setFocus();
	}

}
