/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
import net.tourbook.importdata.ActionImportFromDevice;
import net.tourbook.importdata.ActionImportFromDeviceDirect;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.views.ActionOpenView;
import net.tourbook.ui.views.TourChartView;
import net.tourbook.ui.views.TourStatisticsView;
import net.tourbook.ui.views.rawData.ActionImportFromFile;
import net.tourbook.ui.views.rawData.RawDataView;
import net.tourbook.ui.views.tourBook.TourBookView;
import net.tourbook.ui.views.tourMap.CompareResultView;
import net.tourbook.ui.views.tourMap.TourMapView;
import net.tourbook.ui.views.tourMap.WizardTourComparer;
import net.tourbook.util.PositionedWizardDialog;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

/**
 * An action bar advisor is responsible for creating, adding, and disposing of the fGraphActions
 * added to a workbench window. Each window will be populated with new fGraphActions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	private ActionImportFromFile			fActionImportFromFile;
	private ActionImportFromDevice			fActionImportFromDevice;
	private ActionImportFromDeviceDirect	fActionImportFromDeviceDirect;

	private ActionOpenView					fActionRawDataView;
	private ActionOpenView					fActionStatisticsView;
	private ActionOpenView					fActionTourBookView;
	private ActionOpenView					fActionTourMapView;
	private ActionOpenView					fActionTourChartView;
	private ActionOpenView					fActionTourCompareView;

	private Action							fActionTourCompareWizard;

	private IWorkbenchAction				fActionPreferences;

	PersonContributionItem					personSelector;
	TourTypeContributionItem				tourTypeSelector;
	private IWorkbenchAction				fActionSave;
	private IWorkbenchAction				fActionSaveAll;

	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	public IStatus saveState(IMemento memento) {
		personSelector.saveState(memento);
		tourTypeSelector.saveState(memento);
		return super.saveState(memento);
	}

	protected void makeActions(final IWorkbenchWindow window) {

		personSelector = new PersonContributionItem();
		tourTypeSelector = new TourTypeContributionItem();

		register(ActionFactory.QUIT.create(window));

		register(ActionFactory.ABOUT.create(window));
		getAction(ActionFactory.ABOUT.getId()).setText(Messages.Action_About);

		fActionImportFromFile = new ActionImportFromFile();
		fActionImportFromDevice = new ActionImportFromDevice(window);
		fActionImportFromDeviceDirect = new ActionImportFromDeviceDirect(window);

		fActionTourChartView = new ActionOpenView(
				window,
				"Tour Chart",
				"Shows a chart for the currently selected tour",
				TourChartView.ID,
				ICommandIds.CMD_OPENVIEW_TOURCHART,
				"tour-chart.gif");

		fActionRawDataView = new ActionOpenView(
				window,
				Messages.Action_openview_rawdata,
				Messages.Action_openview_rawdata_tooltip,
				RawDataView.ID,
				ICommandIds.CMD_OPENVIEW_IMPORTEDDATA,
				Messages.Image_view_rawdata);

		fActionStatisticsView= new ActionOpenView(
				window,
				"",
				"",
				TourStatisticsView.ID,
				ICommandIds.CMD_OPENVIEW_STATISTICS,
				Messages.Image_show_statistics);

		fActionTourBookView = new ActionOpenView(
				window,
				Messages.Action_openview_tourbook,
				Messages.Action_openview_tourbook_tooltip,
				TourBookView.ID,
				ICommandIds.CMD_OPENVIEW_TOURLIST,
				Messages.Image_view_tourbool);

		fActionTourMapView = new ActionOpenView(
				window,
				Messages.Action_openview_tourmap,
				Messages.Action_openview_tourmap_tooltip,
				TourMapView.ID,
				ICommandIds.CMD_OPENVIEW_TOURMAP,
				Messages.Image_view_tourmap);

		fActionTourCompareView = new ActionOpenView(
				window,
				Messages.Action_openview_compare_result,
				Messages.Action_openview_compare_result_tooltip,
				CompareResultView.ID,
				ICommandIds.CMD_OPENVIEW_TOURCOMPARER,
				Messages.Image_view_compare_result);

		fActionPreferences = ActionFactory.PREFERENCES.create(window);
		fActionPreferences.setText(Messages.Action_open_preferences);

		fActionSave = ActionFactory.SAVE.create(window);
		register(fActionSave);
		fActionSaveAll = ActionFactory.SAVE_ALL.create(window);
		register(fActionSaveAll);

		fActionTourCompareWizard = new Action() {

			{
				setText(Messages.Action_open_compare_wizard);
				setToolTipText(Messages.Action_open_compare_wizard_tooltip);
				setImageDescriptor(TourbookPlugin
						.getImageDescriptor(Messages.Image_view_compare_wizard));
			}

			public void run() {
				Wizard wizard = new WizardTourComparer();

				final WizardDialog dialog = new PositionedWizardDialog(
						window.getShell(),
						wizard,
						WizardTourComparer.DIALOG_SETTINGS_SECTION,
						800,
						600);

				BusyIndicator.showWhile(null, new Runnable() {
					public void run() {
						dialog.open();
					}
				});

			}

		};
	}

	protected void fillMenuBar(IMenuManager menuBar) {

		// file - menu
		MenuManager fileMenu = new MenuManager(
				Messages.Action_Menu_file,
				IWorkbenchActionConstants.M_FILE);

		fileMenu.add(fActionSave);
		fileMenu.add(fActionSaveAll);
		fileMenu.add(new Separator());

		fileMenu.add(fActionImportFromFile);
		fileMenu.add(fActionImportFromDevice);
		fileMenu.add(fActionImportFromDeviceDirect);
		fileMenu.add(new Separator());

		fileMenu.add(fActionPreferences);
		fileMenu.add(new Separator());

		fileMenu.add(new Separator("update")); //$NON-NLS-1$
		fileMenu.add(new Separator());

		fileMenu.add(getAction(ActionFactory.QUIT.getId()));

		// disabled - it's necesarry when the tour editor is reactivated
		// fileMenu.add(fActionSave);
		// fileMenu.add(fActionClose);
		// fileMenu.add(new Separator());

		// view - menu
		MenuManager tourMenu = new MenuManager(Messages.Action_Menu_view, null);

		tourMenu.add(fActionTourChartView);
		tourMenu.add(new Separator());

		tourMenu.add(fActionRawDataView);
		tourMenu.add(fActionTourBookView);
		tourMenu.add(fActionTourMapView);
		tourMenu.add(fActionStatisticsView);
		tourMenu.add(new Separator());

		tourMenu.add(fActionTourCompareWizard);
		tourMenu.add(fActionTourCompareView);

		// help - menu
		MenuManager helpMenu = new MenuManager(
				Messages.Action_Menu_help,
				IWorkbenchActionConstants.M_HELP);

		helpMenu.add(getAction(ActionFactory.ABOUT.getId()));

		// create menu bar
		menuBar.add(fileMenu);
		menuBar.add(tourMenu);
		menuBar.add(helpMenu);
	}

	protected void fillCoolBar(ICoolBarManager coolBar) {

		IToolBarManager peopleToolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		peopleToolbar.add(personSelector);

		coolBar.add(new ToolBarContributionItem(peopleToolbar, "people")); //$NON-NLS-1$

		// ---------------------------------------------------------

		IToolBarManager tourTypeToolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		tourTypeToolbar.add(tourTypeSelector);

		coolBar.add(new ToolBarContributionItem(tourTypeToolbar, "tourtype")); //$NON-NLS-1$

		// ---------------------------------------------------------

		IToolBarManager importToolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		importToolbar.add(fActionImportFromFile);
		importToolbar.add(fActionImportFromDevice);
		importToolbar.add(fActionImportFromDeviceDirect);

		coolBar.add(new ToolBarContributionItem(importToolbar, "import")); //$NON-NLS-1$

		// ---------------------------------------------------------

		IToolBarManager openToolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);

		openToolbar.add(new Separator());
		openToolbar.add(fActionTourChartView);

		openToolbar.add(new Separator());
		openToolbar.add(fActionRawDataView);
		openToolbar.add(fActionTourBookView);
		openToolbar.add(fActionTourMapView);
		openToolbar.add(fActionStatisticsView);

		coolBar.add(new ToolBarContributionItem(openToolbar, "main")); //$NON-NLS-1$

		// ---------------------------------------------------------

		IToolBarManager compareToolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);

		compareToolbar.add(new Separator());
		compareToolbar.add(fActionTourCompareWizard);
		compareToolbar.add(fActionTourCompareView);

		coolBar.add(new ToolBarContributionItem(compareToolbar, "compare")); //$NON-NLS-1$

		// ---------------------------------------------------------

		IToolBarManager prefToolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);

		prefToolbar.add(new Separator());
		prefToolbar.add(fActionPreferences);

		coolBar.add(new ToolBarContributionItem(prefToolbar, "pref")); //$NON-NLS-1$
	}

}
