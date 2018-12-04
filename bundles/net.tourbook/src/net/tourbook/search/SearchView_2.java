/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.web.WEB;
import net.tourbook.web.preferences.PrefPageWebBrowser;

public class SearchView_2 extends ViewPart implements ISearchView {

   public static final String       ID                             = "net.tourbook.search.SearchView_2"; //$NON-NLS-1$

   private static final String      STATE_USE_EXTERNAL_WEB_BROWSER = "STATE_USE_EXTERNAL_WEB_BROWSER";   //$NON-NLS-1$

   private final IDialogSettings    _state                         = TourbookPlugin.getState(ID);

   private PostSelectionProvider    _postSelectionProvider;
   private IPartListener2           _partListener;
   private ITourEventListener       _tourEventListener;

   private boolean                  _isWinInternalLoaded           = false;

   private ActionExternalSearchUI_2 _actionExternalSearchUI_2;

   /*
    * UI controls
    */
   private Browser   _browser;

   private PageBook  _pageBook;

//   private Composite _pageLinux;
   private Composite _pageWinExternalBrowser;
   private Composite _pageWinInternalBrowser;

   void actionSearchUI() {
      showUIPage();
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {

         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == SearchView_2.this) {
               SearchMgr.setSearchView(null);
            }
         }

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {}

         @Override
         public void partInputChanged(final IWorkbenchPartReference partRef) {}

         @Override
         public void partOpened(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == SearchView_2.this) {
               SearchMgr.setSearchView(SearchView_2.this);
            }
         }

         @Override
         public void partVisible(final IWorkbenchPartReference partRef) {}
      };

      getViewSite().getPage().addPartListener(_partListener);
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == SearchView_2.this) {
               return;
            }

            if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

               final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
               if (modifiedTours != null) {

                  // update modified tour

//						for (final TourData tourData : modifiedTours) {
//
//						}
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

   private void createActions() {

      _actionExternalSearchUI_2 = new ActionExternalSearchUI_2(this);

      fillActionBars();
   }

   @Override
   public void createPartControl(final Composite parent) {

      FTSearchManager.setupSuggester();

      addPartListener();
      addTourEventListener();

      // this part is a selection provider
      _postSelectionProvider = new PostSelectionProvider(ID);
      getSite().setSelectionProvider(_postSelectionProvider);

      createUI(parent);
      createActions();

      restoreState();

      showUIPage();
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook);

//		if (UI.IS_WIN) {
//
      // internal browser
      _pageWinInternalBrowser = new Composite(_pageBook, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageWinInternalBrowser);
      GridLayoutFactory.fillDefaults().applyTo(_pageWinInternalBrowser);

      createUI_10_SearchInternal(_pageWinInternalBrowser);

      // external browser
      _pageWinExternalBrowser = new Composite(_pageBook, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageWinExternalBrowser);
      GridLayoutFactory.fillDefaults().applyTo(_pageWinExternalBrowser);
      createUI_20_SearchExternal(_pageWinExternalBrowser);
//
//		} else {
//
//			// external browser
//			_pageLinux = new Composite(_pageBook, SWT.NONE);
//			GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageLinux);
//			GridLayoutFactory.fillDefaults().applyTo(_pageLinux);
//
//			createUI_30_Linux(_pageLinux);
//		}
   }

   private void createUI_10_SearchInternal(final Composite parent) {

      try {

         _browser = new Browser(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_browser);

      } catch (final SWTError e) {
         StatusUtil.showStatus("Could not instantiate Browser: " + e.getMessage(), e);//$NON-NLS-1$
         return;
      }

      _browser.addLocationListener(new LocationAdapter() {
         @Override
         public void changing(final LocationEvent event) {
            SearchMgr.onBrowserLocation(event);
         }
      });

   }

   private void createUI_20_SearchExternal(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
      {
         final Link linkExternalBrowser = new Link(container, SWT.WRAP | SWT.READ_ONLY);
         GridDataFactory.fillDefaults()//
               .grab(true, true)
               .applyTo(linkExternalBrowser);

         linkExternalBrowser.setText(NLS.bind(
               Messages.Search_View_Link_ExternalBrowser,
               SearchMgr.SEARCH_URL,
               SearchMgr.SEARCH_URL));

         linkExternalBrowser.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               WEB.openUrl(SearchMgr.SEARCH_URL);
            }
         });

         createUI_50_SetupExternalWebbrowser(parent, container);
      }
   }

//   private void createUI_30_Linux(final Composite parent) {
//
//      final Composite container = new Composite(parent, SWT.NONE);
//      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
//      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
//      {
//         /*
//          * Link: Search page url
//          */
//         final Link linkLinuxBrowser = new Link(container, SWT.WRAP | SWT.READ_ONLY);
//         GridDataFactory.fillDefaults()//
//               .grab(true, true)
//               .applyTo(linkLinuxBrowser);
//
//         linkLinuxBrowser.setText(NLS.bind(
//               Messages.Search_View_Link_LinuxBrowser,
//               SearchMgr.SEARCH_URL,
//               SearchMgr.SEARCH_URL));
//
//         linkLinuxBrowser.addSelectionListener(new SelectionAdapter() {
//            @Override
//            public void widgetSelected(final SelectionEvent e) {
//               WEB.openUrl(SearchMgr.SEARCH_URL);
//            }
//         });
//
//         createUI_50_SetupExternalWebbrowser(parent, container);
//      }
//   }

   private void createUI_50_SetupExternalWebbrowser(final Composite parent, final Composite container) {

      /*
       * Link: Setup browser
       */
      final Link linkSetupBrowser = new Link(container, SWT.WRAP);
      GridDataFactory.fillDefaults()//
            .align(SWT.FILL, SWT.END)
            .applyTo(linkSetupBrowser);

      linkSetupBrowser.setText(Messages.Search_View_Link_SetupExternalBrowser);
      linkSetupBrowser.setEnabled(true);
      linkSetupBrowser.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            PreferencesUtil.createPreferenceDialogOn(//
                  parent.getShell(),
                  PrefPageWebBrowser.ID,
                  null,
                  null).open();
         }
      });
   }

   @Override
   public void dispose() {

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      if (_partListener != null) {

         getViewSite().getPage().removePartListener(_partListener);
      }

      super.dispose();
   }

   private void enableActions() {

//      _actionExternalSearchUI_2.setEnabled(UI.IS_WIN);
   }

   private void fillActionBars() {

      /*
       * fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_actionExternalSearchUI_2);
   }

   @Override
   public IWorkbenchPart getPart() {
      return this;
   }

   @Override
   public PostSelectionProvider getPostSelectionProvider() {
      return _postSelectionProvider;
   }

   private void restoreState() {

      _actionExternalSearchUI_2.setChecked(Util.getStateBoolean(_state, STATE_USE_EXTERNAL_WEB_BROWSER, false));

      enableActions();
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_USE_EXTERNAL_WEB_BROWSER, _actionExternalSearchUI_2.isChecked());
   }

   @Override
   public void setFocus() {

//		if (UI.IS_WIN) {
//
      final boolean isInternal = _actionExternalSearchUI_2.isChecked() == false;

      if (isInternal) {

         _browser.setFocus();
      }
//		}
   }

   private void showUIPage() {

//      if (UI.IS_WIN) {
//
      final boolean isExternal = _actionExternalSearchUI_2.isChecked();

      if (isExternal) {

         _pageBook.showPage(_pageWinExternalBrowser);

      } else {

         _pageBook.showPage(_pageWinInternalBrowser);

         updateUI_WinInternalBrowser();
      }
//
//      } else {
//
//         _pageBook.showPage(_pageLinux);
//      }
   }

   private void updateUI_WinInternalBrowser() {

      if (_isWinInternalLoaded == false) {

         _isWinInternalLoaded = true;

         // show search page
         _browser.setUrl(SearchMgr.SEARCH_URL);
      }
   }
}
