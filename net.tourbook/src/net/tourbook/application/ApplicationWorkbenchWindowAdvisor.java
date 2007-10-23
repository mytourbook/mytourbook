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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.tourbook.Messages;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPropertyListener;
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

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	private ApplicationActionBarAdvisor			fApplicationActionBarAdvisor;

	private IPerspectiveDescriptor				lastPerspective;
	private IWorkbenchPage						lastActivePage;

	private IWorkbenchPart						lastActivePart;
	private String								lastPartTitle	= "";			//$NON-NLS-1$

	private final ApplicationWorkbenchAdvisor	wbAdvisor;

	private IPropertyListener					partPropertyListener;

	public ApplicationWorkbenchWindowAdvisor(ApplicationWorkbenchAdvisor wbAdvisor,
			IWorkbenchWindowConfigurer configurer) {
		super(configurer);
		this.wbAdvisor = wbAdvisor;

	}

	@Override
	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		fApplicationActionBarAdvisor = new ApplicationActionBarAdvisor(configurer);
		return fApplicationActionBarAdvisor;
	}

	@Override
	public void dispose() {
		UI.getInstance().dispose();
	}

	@Override
	public void postWindowCreate() {

		// show editor area
//		IWorkbenchPage activePage = getWindowConfigurer().getWindow().getActivePage();
//		activePage.setEditorAreaVisible(true);

		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();

		configurer.setTitle(Messages.App_Title + " - " + MyTourbookSplashHandler.APP_BUILD_ID);

	}

	@Override
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

				MessageDialog.openInformation(activeShell,
						Messages.App_Dlg_first_startup_title,
						Messages.App_Dlg_first_startup_msg);

				PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(activeShell,
						"net.tourbook.preferences.PrefPageClients", //$NON-NLS-1$
						null,
						null);

				dialog.open();

				// open raw data view
				try {
					getWindowConfigurer().getWindow().getActivePage().showView(RawDataView.ID,
							null,
							IWorkbenchPage.VIEW_ACTIVATE);
				} catch (PartInitException e) {
					e.printStackTrace();
				}

			}
			conn.close();

			// select person/tour type which was selected in the last session
			fApplicationActionBarAdvisor.personSelector.fireEventNewPersonIsSelected();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void preWindowOpen() {

		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();

		configurer.setInitialSize(new Point(900, 700));
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);
		configurer.setShowProgressIndicator(true);
		configurer.setShowPerspectiveBar(true);

		configurer.setTitle(Messages.App_Title + " - " + MyTourbookSplashHandler.APP_BUILD_ID);

		PlatformUI.getPreferenceStore()
				.setValue(IWorkbenchPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS, true);

		PlatformUI.getPreferenceStore()
				.setValue(IWorkbenchPreferenceConstants.SHOW_PROGRESS_ON_STARTUP, true);

		hookTitleUpdateListeners(configurer);
	}

	/**
	 * Hooks the listeners needed on the window
	 * 
	 * @param configurer
	 */
	private void hookTitleUpdateListeners(IWorkbenchWindowConfigurer configurer) {

		// hook up the listeners to update the window title

		configurer.getWindow().addPageListener(new IPageListener() {

			public void pageActivated(IWorkbenchPage page) {
				updateTitle();
			}

			public void pageClosed(IWorkbenchPage page) {
				updateTitle();
			}

			public void pageOpened(IWorkbenchPage page) {}
		});

		configurer.getWindow().addPerspectiveListener(new PerspectiveAdapter() {

			@Override
			public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
				updateTitle();
			}

			@Override
			public void perspectiveSavedAs(	IWorkbenchPage page,
											IPerspectiveDescriptor oldPerspective,
											IPerspectiveDescriptor newPerspective) {
				updateTitle();
			}

			@Override
			public void perspectiveDeactivated(	IWorkbenchPage page,
												IPerspectiveDescriptor perspective) {
				updateTitle();
			}
		});

		configurer.getWindow().getPartService().addPartListener(new IPartListener2() {

			public void partActivated(IWorkbenchPartReference ref) {
				if (ref instanceof IEditorReference || ref instanceof IViewReference) {
					updateTitle();
				}
			}

			public void partBroughtToTop(IWorkbenchPartReference ref) {
				if (ref instanceof IEditorReference || ref instanceof IViewReference) {
					updateTitle();
				}
			}

			public void partClosed(IWorkbenchPartReference ref) {
				updateTitle();
			}

			public void partDeactivated(IWorkbenchPartReference ref) {}

			public void partOpened(IWorkbenchPartReference ref) {}

			public void partHidden(IWorkbenchPartReference ref) {}

			public void partVisible(IWorkbenchPartReference ref) {}

			public void partInputChanged(IWorkbenchPartReference ref) {}
		});

		partPropertyListener = new IPropertyListener() {
			public void propertyChanged(Object source, int propId) {

				if (propId == IWorkbenchPartConstants.PROP_TITLE) {
					if (lastActivePart != null) {
						String newTitle = lastActivePart.getTitle();
						if (!lastPartTitle.equals(newTitle)) {
							recomputeTitle();
						}
					}
				}
			}
		};

	}

	/**
	 * Updates the window title. Format will be:
	 * <p>
	 * [pageInput -] [currentPerspective -] [editorInput -] [workspaceLocation -] productName
	 */
	private void updateTitle() {

		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		IWorkbenchWindow window = configurer.getWindow();

		IWorkbenchPart activePart = null;
		IWorkbenchPage currentPage = window.getActivePage();

		IPerspectiveDescriptor persp = null;

		if (currentPage != null) {
			persp = currentPage.getPerspective();

			activePart = currentPage.getActivePart();
		}

		// Nothing to do if the part hasn't changed
		if (activePart == lastActivePart
				&& currentPage == lastActivePage
				&& persp == lastPerspective) {
			return;
		}

		if (lastActivePart != null) {
			lastActivePart.removePropertyListener(partPropertyListener);
		}

		lastActivePart = activePart;
		lastActivePage = currentPage;
		lastPerspective = persp;

		if (activePart != null) {
			activePart.addPropertyListener(partPropertyListener);
		}

		recomputeTitle();
	}

	private String computeTitle() {

		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		IWorkbenchPage currentPage = configurer.getWindow().getActivePage();
		IWorkbenchPart activePart = null;

		if (currentPage != null) {
			activePart = currentPage.getActivePart();
		}

		String title = null;
		IProduct product = Platform.getProduct();
		if (product != null) {
			title = product.getName();
		}
		if (title == null) {
			title = ""; //$NON-NLS-1$
		}

		if (currentPage != null) {

			final String shellTitle = Messages.App_Window_Title;

			if (activePart != null) {
				lastPartTitle = activePart.getTitleToolTip();
				if (lastPartTitle != null) {
					if (lastPartTitle.length() > 0) {
						title = NLS.bind(shellTitle, lastPartTitle, title);
					}
				}
			}

			String label = ""; //$NON-NLS-1$

			IPerspectiveDescriptor persp = currentPage.getPerspective();
			if (persp != null) {
				label = persp.getLabel();
			}

			IAdaptable input = currentPage.getInput();
			if (input != null && !input.equals(wbAdvisor.getDefaultPageInput())) {
				label = currentPage.getLabel();
			}

			if (label != null && !label.equals("")) { //$NON-NLS-1$ 
				title = NLS.bind(shellTitle, label, title);
			}
		}

		return title;
	}

	private void recomputeTitle() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		String oldTitle = configurer.getTitle();
		String newTitle = computeTitle();
		if (!newTitle.equals(oldTitle)) {
			configurer.setTitle(newTitle);
		}
	}
}
