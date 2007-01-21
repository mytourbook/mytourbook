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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.tourbook.Messages;
import net.tourbook.database.TourDatabase;
import net.tourbook.views.rawData.RawDataView;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	private ApplicationActionBarAdvisor	fApplicationActionBarAdvisor;

	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		fApplicationActionBarAdvisor = new ApplicationActionBarAdvisor(configurer);
		return fApplicationActionBarAdvisor;
	}

	public void postWindowCreate() {

		// hide editor area
		IWorkbenchPage activePage = getWindowConfigurer().getWindow().getActivePage();
		activePage.setEditorAreaVisible(false);
	}

	public void postWindowRestore() {

		// select person/tour type which was selected in the last session
		fApplicationActionBarAdvisor.personSelector.fireEventNewPersonIsSelected();

		IWorkbenchPage activePage = getWindowConfigurer().getWindow().getActivePage();
		activePage.closeAllEditors(false);
	}

	public void preWindowOpen() {

		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();

		configurer.setInitialSize(new Point(900, 700));
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);
		configurer.setShowProgressIndicator(true);

		configurer.setTitle(Messages.App_Title);

		PlatformUI.getPreferenceStore().putValue(
				IWorkbenchPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS,
				"false"); //$NON-NLS-1$

	}

	public void postWindowOpen() {

		String sqlString = "SELECT *  FROM " + TourDatabase.TABLE_TOUR_PERSON; //$NON-NLS-1$

		try {
			Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sqlString);
			ResultSet result = statement.executeQuery();

			if (result.next()) {
				// people are available, nothing more to do
				return;
			} else {

				// no people are in the db, open the pref dialog to enter people

				Shell activeShell = Display.getCurrent().getActiveShell();

				MessageDialog
						.openInformation(
								activeShell,
								Messages.App_Dlg_first_startup_title,
								Messages.App_Dlg_first_startup_msg);

				PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
						activeShell,
						"net.tourbook.preferences.PrefPageClients", //$NON-NLS-1$
						null,
						null);

				dialog.open();

				// open raw data view
				try {
					getWindowConfigurer().getWindow().getActivePage().showView(
							RawDataView.ID,
							null,
							IWorkbenchPage.VIEW_ACTIVATE);
				} catch (PartInitException e) {
					e.printStackTrace();
				}

			}
			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
}
