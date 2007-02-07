/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
import net.tourbook.dataImport.ActionDeviceImport;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.util.PositionedWizardDialog;
import net.tourbook.views.ActionOpenView;
import net.tourbook.views.rawData.RawDataView;
import net.tourbook.views.tourBook.TourBookView;
import net.tourbook.views.tourMap.CompareResultView;
import net.tourbook.views.tourMap.TourMapView;
import net.tourbook.views.tourMap.WizardTourComparer;

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
 * An action bar advisor is responsible for creating, adding, and disposing of
 * the fGraphActions added to a workbench window. Each window will be populated
 * with new fGraphActions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	private ActionDeviceImport	fActionImport;
	private ActionDeviceImport	fActionImportDirect;
	private ActionOpenView		fActionRawDataView;

	private ActionOpenView		fActionTourBookView;
	private ActionOpenView		fActionTourMapView;

	private ActionOpenView		fActionTourCompareView;
	private Action				fActionTourCompareWizard;

	private IWorkbenchAction	fActionPreferences;

	PersonContributionItem		personSelector;
	TourTypeContributionItem	tourTypeSelector;

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

		fActionImport = new ActionDeviceImport(window, false,Messages.Image_import_rawdata);
		fActionImportDirect = new ActionDeviceImport(window, true,Messages.Image_import_rawdata_direct);

		fActionRawDataView = new ActionOpenView(
				window,
				Messages.Action_openview_rawdata,
				Messages.Action_openview_rawdata_tooltip,
				RawDataView.ID,
				ICommandIds.CMD_OPENVIEW_IMPORTEDDATA,
				Messages.Image_view_rawdata);

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
						WizardTourComparer.DIALOG_SETTINGS_SECTION);

				BusyIndicator.showWhile(null, new Runnable() {
					public void run() {
						dialog.open();
					}
				});

			}

		};
	}

	protected void fillMenuBar(IMenuManager menuBar) {

		MenuManager fileMenu = new MenuManager(
				Messages.Action_Menu_file,
				IWorkbenchActionConstants.M_FILE);
		menuBar.add(fileMenu);

		fileMenu.add(fActionImportDirect);
		fileMenu.add(fActionImport);
		fileMenu.add(fActionTourCompareWizard);

		fileMenu.add(new Separator());
		fileMenu.add(fActionPreferences);

		fileMenu.add(new Separator("update")); //$NON-NLS-1$

		fileMenu.add(new Separator());
		fileMenu.add(getAction(ActionFactory.QUIT.getId()));

		// disabled - it's necesarry when the tour editor is reactivated
		// fileMenu.add(fActionSave);
		// fileMenu.add(fActionClose);
		// fileMenu.add(new Separator());

		MenuManager viewMenu = new MenuManager(Messages.Action_Menu_view, null);
		menuBar.add(viewMenu);
		viewMenu.add(fActionRawDataView);
		viewMenu.add(fActionTourBookView);
		viewMenu.add(fActionTourMapView);
		viewMenu.add(fActionTourCompareView);

		menuBar.add(getAction(ActionFactory.ABOUT.getId()));
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
		importToolbar.add(fActionImport);
		importToolbar.add(fActionRawDataView);

		coolBar.add(new ToolBarContributionItem(importToolbar, "import")); //$NON-NLS-1$

		// ---------------------------------------------------------

		IToolBarManager openToolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);

		openToolbar.add(new Separator());
		openToolbar.add(fActionTourBookView);
		openToolbar.add(fActionTourMapView);

		openToolbar.add(new Separator());
		openToolbar.add(fActionTourCompareWizard);
		openToolbar.add(fActionTourCompareView);

		openToolbar.add(new Separator());
		openToolbar.add(fActionPreferences);

		coolBar.add(new ToolBarContributionItem(openToolbar, "main")); //$NON-NLS-1$
	}

}
