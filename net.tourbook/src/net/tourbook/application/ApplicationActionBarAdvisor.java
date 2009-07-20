/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

	private IWorkbenchWindow					fWindow;

	private IContributionItem					fActionViewShortList;
	private IWorkbenchAction					fActionPreferences;

	PersonContributionItem						fPersonSelector;
	TourTypeContributionItem					fTourTypeSelector;
	private MeasurementSystemContributionItem	fMeasurementSelector;

	private IWorkbenchAction					fActionAbout;
	private IWorkbenchAction					fActionQuit;

	private IWorkbenchAction					fActionSavePerspective;
	private IWorkbenchAction					fActionResetPerspective;
	private IWorkbenchAction					fActionClosePerspective;
	private IWorkbenchAction					fActionCloseAllPerspective;
	private IWorkbenchAction					fActionEditActionSets;

	public ApplicationActionBarAdvisor(final IActionBarConfigurer configurer) {
		super(configurer);
	}

	/**
	 * Adds the perspective actions to the specified menu.
	 */
	private void addPerspectiveActions(final MenuManager menu) {

		{
			final String openText = Messages.App_Action_open_perspective;
			final MenuManager changePerspMenuMgr = new MenuManager(openText, "openPerspective"); //$NON-NLS-1$
			final IContributionItem changePerspMenuItem = ContributionItemFactory.PERSPECTIVES_SHORTLIST.create(fWindow);
			changePerspMenuMgr.add(changePerspMenuItem);

			menu.add(changePerspMenuMgr);
		}

		menu.add(fActionSavePerspective);
		menu.add(fActionResetPerspective);
		menu.add(fActionClosePerspective);
		menu.add(fActionCloseAllPerspective);
	}

	private MenuManager createFileMenu() {

		final MenuManager fileMenu = new MenuManager(Messages.App_Action_Menu_file, IWorkbenchActionConstants.M_FILE);

		fileMenu.add(new GroupMarker("fileNew")); //$NON-NLS-1$

		fileMenu.add(new Separator("update")); //$NON-NLS-1$
		fileMenu.add(new Separator("databaseTools")); //$NON-NLS-1$
		fileMenu.add(new Separator());

		/*
		 * If we're on OS X we shouldn't show this command in the File menu. It should be invisible
		 * to the user. However, we should not remove it - the carbon UI code will do a search
		 * through our menu structure looking for it when Cmd-Q is invoked (or Quit is chosen from
		 * the application menu.
		 */
		final ActionContributionItem quitItem = new ActionContributionItem(fActionQuit);
		quitItem.setVisible(!"carbon".equals(SWT.getPlatform())); //$NON-NLS-1$
		fileMenu.add(quitItem);

		return fileMenu;
	}

	private MenuManager createHelpMenu() {
		/*
		 * help - menu
		 */
		final MenuManager helpMenu = new MenuManager(Messages.App_Action_Menu_help, IWorkbenchActionConstants.M_HELP);

		helpMenu.add(getAction(ActionFactory.ABOUT.getId()));

		return helpMenu;
	}

	private MenuManager createToolMenu() {

		final MenuManager toolMenu = new MenuManager(Messages.App_Action_Menu_tools, "net.tourbook.menu.main.tools"); //$NON-NLS-1$

		toolMenu.add(new GroupMarker("tools")); //$NON-NLS-1$
		toolMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

		toolMenu.add(new Separator());

		/*
		 * If we're on OS X we shouldn't show this command in the File menu. It should be invisible
		 * to the user. However, we should not remove it - the carbon UI code will do a search
		 * through our menu structure looking for it when Cmd-Q is invoked (or Quit is chosen from
		 * the application menu.
		 */
		final ActionContributionItem prefItem = new ActionContributionItem(fActionPreferences);
		prefItem.setVisible(!"carbon".equals(SWT.getPlatform())); //$NON-NLS-1$
		toolMenu.add(prefItem);

		return toolMenu;
	}

	private MenuManager createViewMenu() {

		final MenuManager viewMenu = new MenuManager(Messages.App_Action_Menu_view, "views"); //$NON-NLS-1$

		viewMenu.add(new Separator("defaultViews")); //$NON-NLS-1$
		viewMenu.add(fActionViewShortList);

		viewMenu.add(new Separator());
		addPerspectiveActions(viewMenu);

		return viewMenu;
	}

	@Override
	protected void fillCoolBar(final ICoolBarManager coolBar) {

		final IToolBarManager tbmPeople = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		tbmPeople.add(fPersonSelector);

		coolBar.add(new ToolBarContributionItem(tbmPeople, "people")); //$NON-NLS-1$

		// ---------------------------------------------------------

		final IToolBarManager tbmTourType = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		tbmTourType.add(fTourTypeSelector);

		coolBar.add(new ToolBarContributionItem(tbmTourType, "tourtype")); //$NON-NLS-1$

		// ---------------------------------------------------------

		if (TourbookPlugin.getDefault()
				.getPreferenceStore()
				.getBoolean(ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI)) {

			final IToolBarManager tbmSystem = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
			tbmSystem.add(fMeasurementSelector);

			coolBar.add(new ToolBarContributionItem(tbmSystem, "measurementSystem")); //$NON-NLS-1$
		}

		// ---------------------------------------------------------

//		final IToolBarManager tbmSave = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
//		tbmSave.add(fActionSave);
//		tbmSave.add(fActionSaveAll);
//
//		coolBar.add(new ToolBarContributionItem(tbmSave, "save")); //$NON-NLS-1$

	}

	@Override
	protected void fillMenuBar(final IMenuManager menuBar) {

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

		fPersonSelector = new PersonContributionItem();
		fTourTypeSelector = new TourTypeContributionItem();
		fMeasurementSelector = new MeasurementSystemContributionItem();

		fActionQuit = ActionFactory.QUIT.create(window);
		register(fActionQuit);

		fActionAbout = ActionFactory.ABOUT.create(window);
		fActionAbout.setText(Messages.App_Action_About);
		register(fActionAbout);

		fActionPreferences = ActionFactory.PREFERENCES.create(window);
		fActionPreferences.setText(Messages.App_Action_open_preferences);
		register(fActionPreferences);

		fActionViewShortList = ContributionItemFactory.VIEWS_SHORTLIST.create(window);

		fActionEditActionSets = ActionFactory.EDIT_ACTION_SETS.create(window);
		register(fActionEditActionSets);

		fActionSavePerspective = ActionFactory.SAVE_PERSPECTIVE.create(window);
		register(fActionSavePerspective);

		fActionResetPerspective = ActionFactory.RESET_PERSPECTIVE.create(window);
		register(fActionResetPerspective);

		fActionClosePerspective = ActionFactory.CLOSE_PERSPECTIVE.create(window);
		register(fActionClosePerspective);

		fActionCloseAllPerspective = ActionFactory.CLOSE_ALL_PERSPECTIVES.create(window);
		register(fActionCloseAllPerspective);

	}

	@Override
	public IStatus saveState(final IMemento memento) {

		fPersonSelector.saveState(memento);
		fTourTypeSelector.saveState(memento);

		return super.saveState(memento);
	}

}
