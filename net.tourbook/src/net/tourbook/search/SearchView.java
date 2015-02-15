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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

import de.byteholder.geoclipse.util.Util;

public class SearchView extends ViewPart implements IActiveSearchView {

	public static final String		ID	= "net.tourbook.search.SearchView"; //$NON-NLS-1$

	private PostSelectionProvider	_postSelectionProvider;

	private IPartListener2			_partListener;
	private ITourEventListener		_tourEventListener;

	/*
	 * UI controls
	 */
	private Browser					_browser;

	private void addPartListener() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {
				SearchMgr.setActiveSearchView(SearchView.this);
			}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {
				SearchMgr.setActiveSearchView(null);
			}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == SearchView.this) {
					return;
				}

				if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {

						// update modified tour

						for (final TourData tourData : modifiedTours) {

						}
					}

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();

				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void clearView() {

		// removed old tour data from the selection provider
		_postSelectionProvider.clearSelection();
	}

	@Override
	public void createPartControl(final Composite parent) {

		FTSearchManager.setupSuggester();

		if (UI.IS_WIN) {
			createUI_10_Search(parent);
		} else {
			createUI_20_Linux(parent);
		}
	}

	private void createUI_10_Search(final Composite parent) {

		try {

			_browser = new Browser(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(_browser);

		} catch (final SWTError e) {
			StatusUtil.showStatus("Could not instantiate Browser: " + e.getMessage(), e);
			return;
		}

		_browser.addLocationListener(new LocationAdapter() {
			@Override
			public void changing(final LocationEvent event) {
				SearchMgr.onBrowserLocation(event);
			}
		});

		addPartListener();
		addTourEventListener();

		// this part is a selection provider
		_postSelectionProvider = new PostSelectionProvider(ID);
		getSite().setSelectionProvider(_postSelectionProvider);

		// show search page
		_browser.setUrl(SearchMgr.SEARCH_URL);
	}

	private void createUI_20_Linux(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
		{
			final Link linkLinuxBrowser = new Link(container, SWT.WRAP | SWT.READ_ONLY);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.align(SWT.FILL, SWT.BEGINNING)
					.applyTo(linkLinuxBrowser);

			linkLinuxBrowser.setText(NLS.bind(
					Messages.Search_View_Link_LinuxBrowser,
					SearchMgr.SEARCH_URL,
					SearchMgr.SEARCH_URL));

			linkLinuxBrowser.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					Util.openLink(Display.getCurrent().getActiveShell(), SearchMgr.SEARCH_URL);
				}
			});
		}
	}

	@Override
	public void dispose() {

		TourManager.getInstance().removeTourEventListener(_tourEventListener);
		getViewSite().getPage().removePartListener(_partListener);

		super.dispose();
	}

	@Override
	public IWorkbenchPart getPart() {
		return this;
	}

	@Override
	public PostSelectionProvider getPostSelectionProvider() {
		return _postSelectionProvider;
	}

	@Override
	public void setFocus() {

		if (_browser != null) {
			_browser.setFocus();
		}
	}
}
