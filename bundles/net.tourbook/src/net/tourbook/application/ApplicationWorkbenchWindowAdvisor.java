/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.measurement_system.DialogSelectMeasurementSystem;
import net.tourbook.common.measurement_system.MeasurementSystem_Manager;
import net.tourbook.common.swimming.SwimStrokeManager;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourPerson;
import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;
import net.tourbook.map.bookmark.MapBookmarkManager;
import net.tourbook.map3.view.Map3Manager;
import net.tourbook.map3.view.Map3State;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPagePeople;
import net.tourbook.proxy.DefaultProxySelector;
import net.tourbook.proxy.IPreferences;
import net.tourbook.search.FTSearchManager;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tag.tour.filter.TourTagFilterManager;
import net.tourbook.tour.TourTypeFilterManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.tour.filter.TourFilterManager;
import net.tourbook.tour.filter.geo.TourGeoFilter_Manager;
import net.tourbook.tour.photo.TourPhotoManager;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.tourType.TourTypeManager;
//import net.tourbook.ui.UI;
import net.tourbook.ui.views.rawData.RawDataView;
import net.tourbook.web.WebContentServer;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.sdk.P2_Activator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.internal.IPreferenceConstants;
import org.eclipse.ui.internal.WorkbenchPlugin;

@SuppressWarnings("restriction")
public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

   private static IPreferenceStore           _prefStore     = TourbookPlugin.getPrefStore();

   private ApplicationActionBarAdvisor       _applicationActionBarAdvisor;
   private IPerspectiveDescriptor            _lastPerspective;

   private IWorkbenchPage                    _lastActivePage;
   private IWorkbenchPart                    _lastActivePart;
   private String                            _lastPartTitle = UI.EMPTY_STRING;

   private String                            _appTitle;

   private final ApplicationWorkbenchAdvisor _wbAdvisor;

   private IPropertyListener                 _partPropertyListener;

   public ApplicationWorkbenchWindowAdvisor(final ApplicationWorkbenchAdvisor wbAdvisor,
                                            final IWorkbenchWindowConfigurer configurer) {
      super(configurer);

      _wbAdvisor = wbAdvisor;

      _appTitle = Messages.App_Title + UI.DASH_WITH_SPACE
            + ApplicationVersion.getVersionSimple()
            + ApplicationVersion.getDevelopmentId();
   }

   public static void setupProxy() {

      ProxySelector.setDefault(new DefaultProxySelector(ProxySelector.getDefault()));

      // if http-authentication
      final String proxyUser = _prefStore.getString(IPreferences.PROXY_USER);
      final String proxyPassword = _prefStore.getString(IPreferences.PROXY_PWD);

      final Authenticator authenticator = new Authenticator() {

         @Override
         public PasswordAuthentication getPasswordAuthentication() {
            if (getRequestorType().equals(Authenticator.RequestorType.PROXY)) {
               return (new PasswordAuthentication(proxyUser, proxyPassword.toCharArray()));
            }
            return null;
         }
      };

      if (authenticator != null) {
         Authenticator.setDefault(authenticator);
      }
   }

   private String computeTitle() {

      final IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
      final IWorkbenchPage currentPage = configurer.getWindow().getActivePage();
      IWorkbenchPart activePart = null;

      if (currentPage != null) {
         activePart = currentPage.getActivePart();
      }

//		String title = null;
//		final IProduct product = Platform.getProduct();
//		if (product != null) {
//			title = product.getName() + " - " + ApplicationVersion.APP_VERSION; //$NON-NLS-1$
//		}
//		if (title == null) {
//			title = UI.EMPTY_STRING;
//		}

      String title = _appTitle;

      if (currentPage != null) {

         final String shellTitle = Messages.App_Window_Title;

         if (activePart != null) {
            _lastPartTitle = activePart.getTitleToolTip();
            if (_lastPartTitle != null) {
               if (_lastPartTitle.length() > 0) {
                  title = NLS.bind(shellTitle, _lastPartTitle, title);
               }
            }
         }

         String label = UI.EMPTY_STRING;

         final IPerspectiveDescriptor persp = currentPage.getPerspective();
         if (persp != null) {
            label = persp.getLabel();
         }

         final IAdaptable input = currentPage.getInput();
         if ((input != null) && !input.equals(_wbAdvisor.getDefaultPageInput())) {
            label = currentPage.getLabel();
         }

         if (StringUtils.hasContent(label)) {
            title = NLS.bind(shellTitle, label, title);
         }
      }

      return title;
   }

   @Override
   public ActionBarAdvisor createActionBarAdvisor(final IActionBarConfigurer configurer) {

      _applicationActionBarAdvisor = new ApplicationActionBarAdvisor(configurer);

      return _applicationActionBarAdvisor;
   }

   private void firstApplicationStart() {

      final Shell activeShell = Display.getCurrent().getActiveShell();

      MessageDialog.openInformation(
            activeShell,
            Messages.App_Dialog_FirstStartup_Title,
            Messages.App_Dialog_FirstStartup_Message);

      // select measurement system
      new DialogSelectMeasurementSystem(activeShell).open();

      // tell the pref page to create a new default person
      final Boolean isCreatePerson = Boolean.TRUE;

      // this dialog fires an event that the person list is modified
      PreferencesUtil.createPreferenceDialogOn(
            activeShell,
            PrefPagePeople.ID,
            new String[] { PrefPagePeople.ID },
            isCreatePerson,
            PreferencesUtil.OPTION_FILTER_LOCKED).open();

      // set first person as active person
      final ArrayList<TourPerson> allPeople = PersonManager.getTourPeople();
      TourbookPlugin.setActivePerson(allPeople.get(0));
      _prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());

      // tip to save tour
      MessageDialog.openInformation(
            activeShell,
            Messages.App_Dialog_FirstStartupTip_Title,
            Messages.App_Dialog_FirstStartupTip_Message);

      // open raw data view
      try {
         getWindowConfigurer()
               .getWindow()
               .getActivePage()
               .showView(RawDataView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);
      } catch (final PartInitException e) {
         StatusUtil.log(e);
      }
   }

   /**
    * Hooks the listeners needed on the window
    *
    * @param configurer
    */
   private void hookTitleUpdateListeners(final IWorkbenchWindowConfigurer configurer) {

      // hook up the listeners to update the window title

      final IWorkbenchWindow configurerWindow = configurer.getWindow();

      configurerWindow.addPageListener(new IPageListener() {

         @Override
         public void pageActivated(final IWorkbenchPage page) {
            updateTitle();
         }

         @Override
         public void pageClosed(final IWorkbenchPage page) {
            updateTitle();
         }

         @Override
         public void pageOpened(final IWorkbenchPage page) {}
      });

      configurerWindow.addPerspectiveListener(new PerspectiveAdapter() {

         @Override
         public void perspectiveActivated(final IWorkbenchPage page, final IPerspectiveDescriptor perspective) {
            updateTitle();
         }

         @Override
         public void perspectiveDeactivated(final IWorkbenchPage page, final IPerspectiveDescriptor perspective) {
            updateTitle();
         }

         @Override
         public void perspectiveSavedAs(final IWorkbenchPage page,
                                        final IPerspectiveDescriptor oldPerspective,
                                        final IPerspectiveDescriptor newPerspective) {
            updateTitle();
         }
      });

      configurerWindow.getPartService().addPartListener(new IPartListener2() {

         @Override
         public void partActivated(final IWorkbenchPartReference ref) {
            if ((ref instanceof IEditorReference) || (ref instanceof IViewReference)) {
               updateTitle();
            }
         }

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference ref) {
            if ((ref instanceof IEditorReference) || (ref instanceof IViewReference)) {
               updateTitle();
            }
         }

         @Override
         public void partClosed(final IWorkbenchPartReference ref) {
            updateTitle();
         }

         @Override
         public void partDeactivated(final IWorkbenchPartReference ref) {}

         @Override
         public void partHidden(final IWorkbenchPartReference ref) {}

         @Override
         public void partInputChanged(final IWorkbenchPartReference ref) {}

         @Override
         public void partOpened(final IWorkbenchPartReference ref) {}

         @Override
         public void partVisible(final IWorkbenchPartReference ref) {}
      });

      _partPropertyListener = new IPropertyListener() {
         @Override
         public void propertyChanged(final Object source, final int propId) {

            if (propId == IWorkbenchPartConstants.PROP_TITLE) {
               if (_lastActivePart != null) {
                  final String newTitle = _lastActivePart.getTitle();
                  if (!_lastPartTitle.equals(newTitle)) {
                     recomputeTitle();
                  }
               }
            }
         }
      };
   }

   private void loadPeopleData() {

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final String sqlString = "SELECT *  FROM " + TourDatabase.TABLE_TOUR_PERSON; //$NON-NLS-1$

         final PreparedStatement statement = conn.prepareStatement(sqlString);
         final ResultSet result = statement.executeQuery();

         if (result.next()) {

            /**
             * People are available.
             * <p>
             * Check if the user have selected a measurement system, after version
             * 20.11 the default system would be metric.
             */

            MeasurementSystem_Manager.selectMeasurementSystem();

            return;

         } else {

            // no people are in the db -> this is the first startup of the application
            firstApplicationStart();
         }

         // select new person
         _applicationActionBarAdvisor._personContribItem.selectFirstPerson();

      } catch (final SQLException e) {
         net.tourbook.ui.UI.showSQLException(e);
      }
   }

   private void onPostSelectionChanged(final IWorkbenchPart part, final ISelection selection) {

      // debug current selection
//		System.out.println(net.tourbook.common.UI.timeStampNano() + " \t");
//		System.out.println(net.tourbook.common.UI.timeStampNano() + " \t");

//		System.out.println(net.tourbook.common.UI.timeStampNano() + " WbWAdvisor - current post selection: "
////				+ selection.getClass().getSimpleName()
//				+ (" (" + selection.getClass().getCanonicalName() + ")  ")
//				+ selection);

//		if (selection instanceof PhotosWithExifSelection) {
//
////			if (_isViewOpening == false) {
////
////				/**
////				 * prevent runtime exception
////				 * <p>
////				 * Prevented recursive attempt to activate part
////				 * net.tourbook.photo.PhotosAndToursView.ID while still in the middle of activating
////				 * part net.tourbook.photo.PicDirView
////				 */
////
////				_isViewOpening = true;
////
//
//			Display.getCurrent().asyncExec(new Runnable() {
//				public void run() {
//					PhotoManager.openPhotoMergePerspective((PhotosWithExifSelection) selection);
//				}
//			});
////
////				_isViewOpening = false;
////			}
//		}
   }

   @Override
   public void postWindowClose() {

      // do last cleanup, this dispose causes NPE in e4 when run in dispose() method

      TourTypeImage.dispose();
   }

   @Override
   public void postWindowCreate() {

      // show editor area
//		IWorkbenchPage activePage = getWindowConfigurer().getWindow().getActivePage();
//		activePage.setEditorAreaVisible(true);

      final IWorkbenchWindowConfigurer configurer = getWindowConfigurer();

      configurer.setTitle(_appTitle);

      /**
       * THIS IS VERY CRITICAL TO BE SET BEFORE THE ASYNC RUNNABLE STARTS, OTHERWISE THE VIEWS
       * DISPLAY THE WRONG DATA. E.G. COLLATED TOURS SHOWS ALL TOURS AND NOT FOR THE SELECTED
       * TOURTYPE.
       */
      TourTypeManager.restoreState();
      TourTypeFilterManager.restoreState();

      TourFilterManager.restoreState();
      TourGeoFilter_Manager.restoreState();
      TourTagFilterManager.restoreState();
   }

   @Override
   public void postWindowOpen() {

      Display.getDefault().asyncExec(new Runnable() {
         @Override
         public void run() {

            TagMenuManager.restoreTagState();
            TourTypeMenuManager.restoreState();

            loadPeopleData();
            setupAppSelectionListener();

            setupProxy();
         }
      });
   }

   @Override
   public void preWindowOpen() {

      final IWorkbenchWindowConfigurer configurer = getWindowConfigurer();

      configurer.setInitialSize(new Point(950, 700));

      configurer.setShowPerspectiveBar(true);
      configurer.setShowCoolBar(true);
      configurer.setShowProgressIndicator(true);

// status line shows photo selection and loading state
//		configurer.setShowStatusLine(false);

      configurer.setTitle(_appTitle);

      final IPreferenceStore uiPrefStore = PlatformUI.getPreferenceStore();

      uiPrefStore.setValue(IWorkbenchPreferenceConstants.SHOW_PROGRESS_ON_STARTUP, true);

      // show memory monitor
      final boolean isMemoryMonitorVisible = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_SHOW_MEMORY_MONITOR);
      uiPrefStore.setValue(IWorkbenchPreferenceConstants.SHOW_MEMORY_MONITOR, isMemoryMonitorVisible);

      hookTitleUpdateListeners(configurer);

      /*
       * Display the progress dialog for UI jobs, when pressing the hide button there is no other
       * way to display the dialog again
       */
      WorkbenchPlugin.getDefault().getPreferenceStore().setValue(IPreferenceConstants.RUN_IN_BACKGROUND, false);

      // must be initialized early to set photoServiceProvider in the Photo
      TourPhotoManager.restoreState();

      FormatManager.updateDisplayFormats();

      ThemeUtil.setupTheme();
   }

   @Override
   public boolean preWindowShellClose() {

      MeasurementSystem_Manager.saveState();
      _applicationActionBarAdvisor._personContribItem.saveState();

      TagMenuManager.saveTagState();
      TourTagFilterManager.saveState();

      TourTypeFilterManager.saveState();
      TourTypeMenuManager.saveState();
      TourTypeManager.saveState();

      TourFilterManager.saveState();
      TourGeoFilter_Manager.saveState();
      TourPhotoManager.saveState();
      MapBookmarkManager.saveState();
      SwimStrokeManager.saveState();

      FTSearchManager.closeIndexReaderSuggester();
      WebContentServer.stop();

      /**
       * Save map3 state only when map is initialized (displayed). When this state is not checked
       * and map is not yet initialized, the map will be initialized which produces an annoying
       * delay when the application is being closing.
       */
      if (Map3State.isMapInitialized) {
         Map3Manager.saveState();
      }

      return super.preWindowShellClose();
   }

   private void recomputeTitle() {

      final IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
      final String oldTitle = configurer.getTitle();
      final String newTitle = computeTitle();

      if (!newTitle.equals(oldTitle)) {
         configurer.setTitle(newTitle);
      }
   }

   @Override
   public IStatus restoreState(final IMemento memento) {

      P2_Activator.setUpdateSites(memento);

      return super.restoreState(memento);
   }

   @Override
   public IStatus saveState(final IMemento memento) {

      P2_Activator.saveState(memento);

      return super.saveState(memento);
   }

   private void setupAppSelectionListener() {

      final ISelectionService selectionService = PlatformUI
            .getWorkbench()
            .getActiveWorkbenchWindow()
            .getSelectionService();

      selectionService.addPostSelectionListener(new ISelectionListener() {
         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
            onPostSelectionChanged(part, selection);
         }
      });
   }



   /**
    * Updates the window title. Format will be:
    * <p>
    * [pageInput -] [currentPerspective -] [editorInput -] [workspaceLocation -] productName
    */
   private void updateTitle() {

      final IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
      final IWorkbenchWindow window = configurer.getWindow();

      IWorkbenchPart activePart = null;
      final IWorkbenchPage currentPage = window.getActivePage();

      IPerspectiveDescriptor persp = null;

      if (currentPage != null) {
         persp = currentPage.getPerspective();

         activePart = currentPage.getActivePart();
      }

      // Nothing to do if the part hasn't changed
      if ((activePart == _lastActivePart) && (currentPage == _lastActivePage) && (persp == _lastPerspective)) {
         return;
      }

      if (_lastActivePart != null) {
         _lastActivePart.removePropertyListener(_partPropertyListener);
      }

      _lastActivePart = activePart;
      _lastActivePage = currentPage;
      _lastPerspective = persp;

      if (activePart != null) {
         activePart.addPropertyListener(_partPropertyListener);
      }

      recomputeTitle();
   }
}
