/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourTypeFilterManager;

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
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

/**
 * An action bar advisor is responsible for creating, adding, and disposing of the fGraphActions
 * added to a workbench window. Each window will be populated with new fGraphActions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	private IWorkbenchWindow					_window;

	private IWorkbenchAction					_actionPreferences;

	private IWorkbenchAction					_actionAbout;
	private IWorkbenchAction					_actionQuit;

	private IWorkbenchAction					_actionSavePerspective;
	private IWorkbenchAction					_actionResetPerspective;
	private IWorkbenchAction					_actionClosePerspective;
	private IWorkbenchAction					_actionCloseAllPerspective;
	private IWorkbenchAction					_actionEditActionSets;

	PersonContributionItem						_personContribItem;
	private TourTypeContributionItem			_tourTypeContribItem;
	private MeasurementSystemContributionItem	_measurementContribItem;

	private ActionOtherViews					_actionOtherViews;

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
			final IContributionItem changePerspMenuItem = ContributionItemFactory.PERSPECTIVES_SHORTLIST
					.create(_window);

			changePerspMenuMgr.add(changePerspMenuItem);

			menu.add(changePerspMenuMgr);
		}

		menu.add(_actionSavePerspective);
		menu.add(_actionResetPerspective);
		menu.add(_actionClosePerspective);
		menu.add(_actionCloseAllPerspective);
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
		final ActionContributionItem quitItem = new ActionContributionItem(_actionQuit);
		quitItem.setVisible(!"carbon".equals(SWT.getPlatform())); //$NON-NLS-1$
		fileMenu.add(quitItem);

		return fileMenu;
	}

	private MenuManager createHelpMenu() {

		/*
		 * help - menu
		 */
		final MenuManager helpMenu = new MenuManager(Messages.App_Action_Menu_help, IWorkbenchActionConstants.M_HELP);

		helpMenu.add(new Separator("about")); //$NON-NLS-1$
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
		final ActionContributionItem prefItem = new ActionContributionItem(_actionPreferences);
		prefItem.setVisible(!"carbon".equals(SWT.getPlatform())); //$NON-NLS-1$
		toolMenu.add(prefItem);

		return toolMenu;
	}

	private MenuManager createViewMenu() {

		final MenuManager viewMenu = new MenuManager(Messages.App_Action_Menu_view, "views"); //$NON-NLS-1$

		viewMenu.add(new Separator("defaultViews")); //$NON-NLS-1$

		viewMenu.add(new Separator());
		viewMenu.add(_actionOtherViews);
		addPerspectiveActions(viewMenu);

		viewMenu.add(new Separator("adminViews")); //$NON-NLS-1$

		return viewMenu;
	}

	@Override
	protected void fillCoolBar(final ICoolBarManager coolBar) {

		final IToolBarManager tbMgrPeople = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		tbMgrPeople.add(_personContribItem);

		final ToolBarContributionItem tbItemPeople = new ToolBarContributionItem(tbMgrPeople, "people"); //$NON-NLS-1$
		coolBar.add(tbItemPeople);

		// ---------------------------------------------------------

		final IToolBarManager tbMgrTourType = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		final ToolBarContributionItem tbItemTourType = new ToolBarContributionItem(tbMgrTourType, "tourtype"); //$NON-NLS-1$

		coolBar.add(tbItemTourType);
		tbMgrTourType.add(_tourTypeContribItem);

		// ---------------------------------------------------------

		final boolean isShowMeasurement = TourbookPlugin
				.getDefault()
				.getPreferenceStore()
				.getBoolean(ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI);

		if (isShowMeasurement) {

			final IToolBarManager tbMgrSystem = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
			tbMgrSystem.add(_measurementContribItem);

			final ToolBarContributionItem tbItemMeasurement = new ToolBarContributionItem(
					tbMgrSystem,
					"measurementSystem"); //$NON-NLS-1$
			coolBar.add(tbItemMeasurement);
		}

		// this must be set after the coolbar is created, otherwise it stops populating the coolbar
		TourTypeFilterManager.setToolBarContribItem(coolBar, tbMgrTourType, tbItemTourType, _tourTypeContribItem);
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

		_window = window;

		_personContribItem = new PersonContributionItem();
		_tourTypeContribItem = new TourTypeContributionItem();
		_measurementContribItem = new MeasurementSystemContributionItem();

		_actionQuit = ActionFactory.QUIT.create(window);
		register(_actionQuit);

		_actionAbout = ActionFactory.ABOUT.create(window);
		_actionAbout.setText(Messages.App_Action_About);
		register(_actionAbout);

		_actionPreferences = ActionFactory.PREFERENCES.create(window);
		_actionPreferences.setText(Messages.App_Action_open_preferences);
		_actionPreferences.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__options));
		register(_actionPreferences);

		_actionOtherViews = new ActionOtherViews(window);

		_actionEditActionSets = ActionFactory.EDIT_ACTION_SETS.create(window);
		register(_actionEditActionSets);

		_actionSavePerspective = ActionFactory.SAVE_PERSPECTIVE.create(window);
		register(_actionSavePerspective);

		_actionResetPerspective = ActionFactory.RESET_PERSPECTIVE.create(window);
		register(_actionResetPerspective);

		_actionClosePerspective = ActionFactory.CLOSE_PERSPECTIVE.create(window);
		register(_actionClosePerspective);

		_actionCloseAllPerspective = ActionFactory.CLOSE_ALL_PERSPECTIVES.create(window);
		register(_actionCloseAllPerspective);

		/*
		 * keep action bar advisor to register other actions
		 */
//		TourbookPlugin.getDefault().setActionBarAdvisor(this);
	}

//	public void registerAction(final IAction action) {
//		register(action);
//	}

	@Override
	public IStatus saveState(final IMemento memento) {

		_personContribItem.saveState(memento);
		TourTypeFilterManager.saveState(memento);

		return super.saveState(memento);
	}

}
