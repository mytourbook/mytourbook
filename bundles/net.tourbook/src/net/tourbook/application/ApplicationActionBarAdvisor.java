/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.tag.tour.filter.TourTagFilterManager;
import net.tourbook.tour.TourTypeFilterManager;
import net.tourbook.tour.filter.TourFilterManager;
import net.tourbook.tour.filter.geo.TourGeoFilter_Manager;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

/**
 * An action bar advisor is responsible for creating, adding, and disposing of the fGraphActions
 * added to a workbench window. Each window will be populated with new fGraphActions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

   private static final String               MENU_CONTRIB_TOOLBAR_APP_FILTER = "mc_tb_AppFilter";             //$NON-NLS-1$

   private static IPreferenceStore           _prefStoreCommon                = CommonActivator.getPrefStore();

   private IWorkbenchWindow                  _window;

   private IWorkbenchAction                  _actionPreferences;

   private IWorkbenchAction                  _actionAbout;
   private IWorkbenchAction                  _actionQuit;

   private IWorkbenchAction                  _actionSavePerspective;
   private IWorkbenchAction                  _actionResetPerspective;
   private IWorkbenchAction                  _actionClosePerspective;
   private IWorkbenchAction                  _actionCloseAllPerspective;

   private ActionContributionItem            _quitItem;
   private ActionContributionItem            _prefItem;

   /**
    * Customize perspective is disabled, because when the dialog is closed, the tour type UI
    * controls will be disposed, after some investigation an easy solution was not found.
    * <p>
    * This action cannot be removed from the perspective context menu (it's created internally),
    * only from the app menu.
    */
//   private IWorkbenchAction               _actionEditActionSets;

   PersonContributionItem                    _personContribItem;
   private ActionTourDataFilter              _actionTour_DataFilter;
   private ActionTourGeoFilter               _actionTour_GeoFilter;
   private ActionTourTagFilter               _actionTour_TagFilter;
   private TourTypeContributionItem          _tourTypeContribItem;
   private MeasurementSystemContributionItem _measurementContribItem;

   private ActionOtherViews                  _actionOtherViews;

   public ApplicationActionBarAdvisor(final IActionBarConfigurer configurer) {
      super(configurer);
   }

   /**
    * Adds the perspective actions to the specified menu.
    */
   private void addPerspectiveActions(final MenuManager menu) {

      {
         final MenuManager perspectiveMenuMgr = new MenuManager(
               Messages.App_Action_open_perspective,
               "openPerspective"); //$NON-NLS-1$

         final IContributionItem changePerspMenuItem = ContributionItemFactory.PERSPECTIVES_SHORTLIST.create(_window);

         perspectiveMenuMgr.add(changePerspMenuItem);

         menu.add(perspectiveMenuMgr);
      }

      menu.add(_actionSavePerspective);
      menu.add(_actionResetPerspective);
      menu.add(_actionClosePerspective);
      menu.add(_actionCloseAllPerspective);
   }

   private MenuManager createMenu_10_New() {

      final MenuManager newMenu = new MenuManager(Messages.App_Action_Menu_New, "m_New"); //$NON-NLS-1$

      newMenu.add(new GroupMarker("ci_New")); //$NON-NLS-1$
      newMenu.add(_quitItem);

      return newMenu;
   }

   private MenuManager createMenu_20_Directories() {

      final MenuManager dirMenu = new MenuManager(Messages.App_Action_Menu_Directory, "m_Directory"); //$NON-NLS-1$

      dirMenu.add(new Separator("defaultViews")); //$NON-NLS-1$

      return dirMenu;
   }

   private MenuManager createMenu_30_Tour() {

      final MenuManager tourMenu = new MenuManager(Messages.App_Action_Menu_Tour, "m_Tour"); //$NON-NLS-1$

      tourMenu.add(new Separator("defaultViews")); //$NON-NLS-1$

      tourMenu.add(new GroupMarker("group_AfterSaveTour")); //$NON-NLS-1$

      return tourMenu;
   }

   private MenuManager createMenu_35_Map() {

      final MenuManager mapMenu = new MenuManager(Messages.App_Action_Menu_Map, "m_Map"); //$NON-NLS-1$

      mapMenu.add(new Separator("defaultViews")); //$NON-NLS-1$

      return mapMenu;
   }

   private MenuManager createMenu_40_Tool() {

      final MenuManager toolMenu = new MenuManager(Messages.App_Action_Menu_tools, "m_Tools"); //$NON-NLS-1$

      toolMenu.add(new GroupMarker("tools")); //$NON-NLS-1$
      toolMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

      toolMenu.add(new Separator());
      toolMenu.add(_actionOtherViews);
      addPerspectiveActions(toolMenu);

      toolMenu.add(new Separator());
      toolMenu.add(_prefItem);

      return toolMenu;
   }

   private MenuManager createMenu_50_Help() {

      final MenuManager helpMenu = new MenuManager(Messages.App_Action_Menu_help, "m_Help");//$NON-NLS-1$

      helpMenu.add(new Separator("about")); //$NON-NLS-1$
      helpMenu.add(getAction(ActionFactory.ABOUT.getId()));

      return helpMenu;
   }

   @Override
   protected void fillCoolBar(final ICoolBarManager coolBar) {

      {
         /*
          * Toolbar: People
          */
         final IToolBarManager tbMgr_People = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
         tbMgr_People.add(_personContribItem);

         final ToolBarContributionItem tbItemPeople = new ToolBarContributionItem(tbMgr_People, "people"); //$NON-NLS-1$
         coolBar.add(tbItemPeople);
      }

      {
         /*
          * Toolbar: Tour type
          */
         final IToolBarManager tbMgr_TourType = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
         final ToolBarContributionItem tbItemTourType = new ToolBarContributionItem(tbMgr_TourType, "tourtype"); //$NON-NLS-1$

         coolBar.add(tbItemTourType);
         tbMgr_TourType.add(_tourTypeContribItem);
      }

      {
         /*
          * Toolbar: Tour filter
          */
         final IToolBarManager tbMgr_TourFilter = new ToolBarManager(SWT.FLAT | SWT.RIGHT);

         {
            /*
             * Tour data filter
             */
            final ToolBarContributionItem tbItemTourDataFilter = new ToolBarContributionItem(
                  tbMgr_TourFilter,
                  MENU_CONTRIB_TOOLBAR_APP_FILTER);

            coolBar.add(tbItemTourDataFilter);
            tbMgr_TourFilter.add(_actionTour_DataFilter);

            TourFilterManager.setTourFilterAction(_actionTour_DataFilter);
         }
         {
            /*
             * Tour geo filter
             */
            final ToolBarContributionItem tbItemContribItem = new ToolBarContributionItem(
                  tbMgr_TourFilter,
                  MENU_CONTRIB_TOOLBAR_APP_FILTER);

            coolBar.add(tbItemContribItem);
            tbMgr_TourFilter.add(_actionTour_GeoFilter);

            TourGeoFilter_Manager.setAction_TourGeoFilter(_actionTour_GeoFilter);
         }
         {
            /*
             * Tour tag filter
             */
            final ToolBarContributionItem tbItemTourTagFilter = new ToolBarContributionItem(
                  tbMgr_TourFilter,
                  MENU_CONTRIB_TOOLBAR_APP_FILTER);

            coolBar.add(tbItemTourTagFilter);
            tbMgr_TourFilter.add(_actionTour_TagFilter);

            TourTagFilterManager.setTourTagFilterAction(_actionTour_TagFilter);
         }
      }

      {
         /**
          * Toolbar: mc_tb_TourEditor
          * <p>
          * The save action is added with the plugin.xml menuContrigution -> complicated but this
          * was the only way I've found, that the save action is at the end of the toolbar and
          * not at the beginning
          * <p>
          * This toolbar is also used as a placeholder for the toolbar location
          */
         final IToolBarManager tbm_TourEditor = new ToolBarManager(SWT.FLAT | SWT.RIGHT);

         final ToolBarContributionItem tbContribItem = new ToolBarContributionItem(
               tbm_TourEditor,
               "mc_tb_TourEditor"); //$NON-NLS-1$

         coolBar.add(tbContribItem);
      }

      {
         /*
          * Toolbar: Measurement
          */
         final boolean isShowMeasurement = _prefStoreCommon.getBoolean(ICommonPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI);
         if (isShowMeasurement) {

            final IToolBarManager tbMgr_System = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
            tbMgr_System.add(_measurementContribItem);

            final ToolBarContributionItem tbItemMeasurement = new ToolBarContributionItem(
                  tbMgr_System,
                  "measurementSystem"); //$NON-NLS-1$
            coolBar.add(tbItemMeasurement);
         }
      }

      // this must be set after the coolbar is created, otherwise it stops populating the coolbar
      TourTypeFilterManager.setToolBarContribItem(_tourTypeContribItem);
   }

   @Override
   protected void fillMenuBar(final IMenuManager menuBar) {

      /*
       * Create app menu
       */
      menuBar.add(createMenu_10_New());
      menuBar.add(createMenu_20_Directories());
      menuBar.add(createMenu_30_Tour());
      menuBar.add(createMenu_35_Map());
      menuBar.add(createMenu_40_Tool());
      menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
      menuBar.add(createMenu_50_Help());
   }

   @Override
   protected void makeActions(final IWorkbenchWindow window) {

      final boolean isOSX = "carbon".equals(SWT.getPlatform());//$NON-NLS-1$

      _window = window;

      _personContribItem = new PersonContributionItem();
      _actionTour_DataFilter = new ActionTourDataFilter();
      _actionTour_GeoFilter = new ActionTourGeoFilter();
      _actionTour_TagFilter = new ActionTourTagFilter();
      _tourTypeContribItem = new TourTypeContributionItem();
      _measurementContribItem = new MeasurementSystemContributionItem();

      _actionQuit = ActionFactory.QUIT.create(window);
      register(_actionQuit);

      _actionAbout = ActionFactory.ABOUT.create(window);
      _actionAbout.setText(Messages.App_Action_About);
      register(_actionAbout);

      _actionPreferences = ActionFactory.PREFERENCES.create(window);
      _actionPreferences.setText(Messages.App_Action_open_preferences);
      _actionPreferences.setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Options));
      register(_actionPreferences);

      _actionOtherViews = new ActionOtherViews(window);

      _actionSavePerspective = ActionFactory.SAVE_PERSPECTIVE.create(window);
      _actionResetPerspective = ActionFactory.RESET_PERSPECTIVE.create(window);
      _actionClosePerspective = ActionFactory.CLOSE_PERSPECTIVE.create(window);
      _actionCloseAllPerspective = ActionFactory.CLOSE_ALL_PERSPECTIVES.create(window);

      register(_actionSavePerspective);
      register(_actionResetPerspective);
      register(_actionClosePerspective);
      register(_actionCloseAllPerspective);

      /*
       * If we're on OS X we shouldn't show this command in the File menu. It should be invisible to
       * the user. However, we should not remove it - the carbon UI code will do a search through
       * our menu structure looking for it when Cmd-Q is invoked (or Quit is chosen from the
       * application menu.
       */
      _quitItem = new ActionContributionItem(_actionQuit);
      _quitItem.setVisible(!isOSX);

      _prefItem = new ActionContributionItem(_actionPreferences);
      _prefItem.setVisible(!isOSX);
   }

}
