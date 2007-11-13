/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.core.runtime.IStatus;
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
import org.eclipse.swt.SWT;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

/**
 * An action bar advisor is responsible for creating, adding, and disposing of the fGraphActions
 * added to a workbench window. Each window will be populated with new fGraphActions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	private IWorkbenchAction					fActionSave;
	private IWorkbenchAction					fActionSaveAll;

	private IContributionItem					fActionViewShortList;
	private IWorkbenchAction					fActionPreferences;

	PersonContributionItem						personSelector;
	TourTypeContributionItem					tourTypeSelector;
	private MeasurementSystemContributionItem	measurementSelector;

	private IWorkbenchAction					fActionAbout;
	private IWorkbenchAction					fActionQuit;

	private IWorkbenchAction					savePerspectiveAction;
	private IWorkbenchAction					resetPerspectiveAction;
	private IWorkbenchAction					closePerspAction;
	private IWorkbenchAction					closeAllPerspsAction;
	private IWorkbenchAction					editActionSetAction;
	private IWorkbenchWindow					fWindow;

	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	/**
	 * Adds the perspective actions to the specified menu.
	 */
	private void addPerspectiveActions(MenuManager menu) {

		{
			String openText = Messages.App_Action_open_perspective;
			MenuManager changePerspMenuMgr = new MenuManager(openText, "openPerspective"); //$NON-NLS-1$
			IContributionItem changePerspMenuItem = ContributionItemFactory.PERSPECTIVES_SHORTLIST.create(fWindow);
			changePerspMenuMgr.add(changePerspMenuItem);

			menu.add(changePerspMenuMgr);
		}

		menu.add(savePerspectiveAction);
		menu.add(resetPerspectiveAction);
		menu.add(closePerspAction);
		menu.add(closeAllPerspsAction);
	}

	private MenuManager createFileMenu() {

		MenuManager fileMenu = new MenuManager(Messages.App_Action_Menu_file,
				IWorkbenchActionConstants.M_FILE);

		fileMenu.add(new GroupMarker("fileNew")); //$NON-NLS-1$

		fileMenu.add(fActionSave);
		fileMenu.add(fActionSaveAll);

		fileMenu.add(new GroupMarker("fileSave")); //$NON-NLS-1$

		fileMenu.add(new Separator("update")); //$NON-NLS-1$
		fileMenu.add(new Separator());

		/*
		 * If we're on OS X we shouldn't show this command in the File menu. It should be invisible
		 * to the user. However, we should not remove it - the carbon UI code will do a search
		 * through our menu structure looking for it when Cmd-Q is invoked (or Quit is chosen from
		 * the application menu.
		 */
		ActionContributionItem quitItem = new ActionContributionItem(fActionQuit);
		quitItem.setVisible(!"carbon".equals(SWT.getPlatform())); //$NON-NLS-1$
		fileMenu.add(quitItem);

		return fileMenu;
	}

	private MenuManager createHelpMenu() {
		/*
		 * help - menu
		 */
		MenuManager helpMenu = new MenuManager(Messages.App_Action_Menu_help,
				IWorkbenchActionConstants.M_HELP);

		helpMenu.add(getAction(ActionFactory.ABOUT.getId()));

		return helpMenu;
	}

	private MenuManager createToolMenu() {

		MenuManager toolMenu = new MenuManager(Messages.App_Action_Menu_tools,
				"net.tourbook.menu.main.tools"); //$NON-NLS-1$

		toolMenu.add(new GroupMarker("tools")); //$NON-NLS-1$
		toolMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

		toolMenu.add(new Separator());

		/*
		 * If we're on OS X we shouldn't show this command in the File menu. It should be invisible
		 * to the user. However, we should not remove it - the carbon UI code will do a search
		 * through our menu structure looking for it when Cmd-Q is invoked (or Quit is chosen from
		 * the application menu.
		 */
		ActionContributionItem prefItem = new ActionContributionItem(fActionPreferences);
		prefItem.setVisible(!"carbon".equals(SWT.getPlatform())); //$NON-NLS-1$
		toolMenu.add(prefItem);

		return toolMenu;
	}

	private MenuManager createViewMenu() {

		MenuManager viewMenu = new MenuManager(Messages.App_Action_Menu_view, "views"); //$NON-NLS-1$

		viewMenu.add(new Separator("defaultViews")); //$NON-NLS-1$
		viewMenu.add(fActionViewShortList);

		viewMenu.add(new Separator());
		addPerspectiveActions(viewMenu);

		return viewMenu;
	}

	@Override
	protected void fillCoolBar(ICoolBarManager coolBar) {

		IToolBarManager tbmPeople = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		tbmPeople.add(personSelector);

		coolBar.add(new ToolBarContributionItem(tbmPeople, "people")); //$NON-NLS-1$

		// ---------------------------------------------------------

		IToolBarManager tbmTourType = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		tbmTourType.add(tourTypeSelector);

		coolBar.add(new ToolBarContributionItem(tbmTourType, "tourtype")); //$NON-NLS-1$

		// ---------------------------------------------------------

		if (TourbookPlugin.getDefault()
				.getPreferenceStore()
				.getBoolean(ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI)) {

			IToolBarManager tbmSystem = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
			tbmSystem.add(measurementSelector);

			coolBar.add(new ToolBarContributionItem(tbmSystem, "measurementSystem")); //$NON-NLS-1$
		}

		// ---------------------------------------------------------

		IToolBarManager tbmSave = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		tbmSave.add(fActionSave);
		tbmSave.add(fActionSaveAll);

		coolBar.add(new ToolBarContributionItem(tbmSave, "save")); //$NON-NLS-1$

		// ---------------------------------------------------------

//		IToolBarManager openToolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
//
//		openToolbar.add(new Separator());
//		openToolbar.add(fActionTourChartView);
//
//		openToolbar.add(new Separator());
//		openToolbar.add(fActionRawDataView);
//		openToolbar.add(fActionTourBookView);
//		openToolbar.add(fActionTourMapView);
//		openToolbar.add(fActionStatisticsView);
//
//		coolBar.add(new ToolBarContributionItem(openToolbar, "main")); //$NON-NLS-1$

		// ---------------------------------------------------------

//		IToolBarManager tbmCompare = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
//
//		tbmCompare.add(new Separator());
//		tbmCompare.add(fActionTourCompareWizard);
//		tbmCompare.add(fActionTourCompareView);
//
//		coolBar.add(new ToolBarContributionItem(tbmCompare, "compare")); //$NON-NLS-1$

		// ---------------------------------------------------------

//		IToolBarManager prefToolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
//
//		prefToolbar.add(new Separator());
//		prefToolbar.add(fActionPreferences);
//
//		coolBar.add(new ToolBarContributionItem(prefToolbar, "pref")); //$NON-NLS-1$
	}

	@Override
	protected void fillMenuBar(IMenuManager menuBar) {

		/*
		 * create menu bar
		 */
		menuBar.add(createFileMenu());
		menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menuBar.add(createToolMenu());
		menuBar.add(createViewMenu());
		menuBar.add(createHelpMenu());
	}

	@Override
	protected void makeActions(final IWorkbenchWindow window) {

		fWindow = window;

		personSelector = new PersonContributionItem();
		tourTypeSelector = new TourTypeContributionItem();
		measurementSelector = new MeasurementSystemContributionItem();

		fActionQuit = ActionFactory.QUIT.create(window);
		register(fActionQuit);

		fActionAbout = ActionFactory.ABOUT.create(window);
		fActionAbout.setText(Messages.App_Action_About);
		register(fActionAbout);

		fActionPreferences = ActionFactory.PREFERENCES.create(window);
		fActionPreferences.setText(Messages.App_Action_open_preferences);
		register(fActionPreferences);

		fActionSave = ActionFactory.SAVE.create(window);
		register(fActionSave);

		fActionSaveAll = ActionFactory.SAVE_ALL.create(window);
		register(fActionSaveAll);

		fActionViewShortList = ContributionItemFactory.VIEWS_SHORTLIST.create(window);

		editActionSetAction = ActionFactory.EDIT_ACTION_SETS.create(window);
		register(editActionSetAction);

		savePerspectiveAction = ActionFactory.SAVE_PERSPECTIVE.create(window);
		register(savePerspectiveAction);

		resetPerspectiveAction = ActionFactory.RESET_PERSPECTIVE.create(window);
		register(resetPerspectiveAction);

		closePerspAction = ActionFactory.CLOSE_PERSPECTIVE.create(window);
		register(closePerspAction);

		closeAllPerspsAction = ActionFactory.CLOSE_ALL_PERSPECTIVES.create(window);
		register(closeAllPerspsAction);

	}

	@Override
	public IStatus saveState(IMemento memento) {

		personSelector.saveState(memento);
		tourTypeSelector.saveState(memento);

		return super.saveState(memento);
	}

}
