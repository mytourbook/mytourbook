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
import net.tourbook.tag.TagManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
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

	public ApplicationWorkbenchWindowAdvisor(	final ApplicationWorkbenchAdvisor wbAdvisor,
												final IWorkbenchWindowConfigurer configurer) {
		super(configurer);
		this.wbAdvisor = wbAdvisor;

	}

	private String computeTitle() {

		final IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		final IWorkbenchPage currentPage = configurer.getWindow().getActivePage();
		IWorkbenchPart activePart = null;

		if (currentPage != null) {
			activePart = currentPage.getActivePart();
		}

		String title = null;
		final IProduct product = Platform.getProduct();
		if (product != null) {
			title = product.getName() + " (" + MyTourbookSplashHandler.APP_BUILD_ID_VERSION + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (title == null) {
			title = UI.EMPTY_STRING;
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

			String label = UI.EMPTY_STRING;

			final IPerspectiveDescriptor persp = currentPage.getPerspective();
			if (persp != null) {
				label = persp.getLabel();
			}

			final IAdaptable input = currentPage.getInput();
			if (input != null && !input.equals(wbAdvisor.getDefaultPageInput())) {
				label = currentPage.getLabel();
			}

			if (label != null && !label.equals(UI.EMPTY_STRING)) {
				title = NLS.bind(shellTitle, label, title);
			}
		}

		return title;
	}

	@Override
	public ActionBarAdvisor createActionBarAdvisor(final IActionBarConfigurer configurer) {
		fApplicationActionBarAdvisor = new ApplicationActionBarAdvisor(configurer);
		return fApplicationActionBarAdvisor;
	}

	@Override
	public void dispose() {
		UI.getInstance().dispose();
	}

	/**
	 * Hooks the listeners needed on the window
	 * 
	 * @param configurer
	 */
	private void hookTitleUpdateListeners(final IWorkbenchWindowConfigurer configurer) {

		// hook up the listeners to update the window title

		configurer.getWindow().addPageListener(new IPageListener() {

			public void pageActivated(final IWorkbenchPage page) {
				updateTitle();
			}

			public void pageClosed(final IWorkbenchPage page) {
				updateTitle();
			}

			public void pageOpened(final IWorkbenchPage page) {}
		});

		configurer.getWindow().addPerspectiveListener(new PerspectiveAdapter() {

			@Override
			public void perspectiveActivated(final IWorkbenchPage page, final IPerspectiveDescriptor perspective) {
				updateTitle();
			}

			@Override
			public void perspectiveDeactivated(final IWorkbenchPage page, final IPerspectiveDescriptor perspective) {
				updateTitle();
			}

			@Override
			public void perspectiveSavedAs(	final IWorkbenchPage page,
											final IPerspectiveDescriptor oldPerspective,
											final IPerspectiveDescriptor newPerspective) {
				updateTitle();
			}
		});

		configurer.getWindow().getPartService().addPartListener(new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference ref) {
				if (ref instanceof IEditorReference || ref instanceof IViewReference) {
					updateTitle();
				}
			}

			public void partBroughtToTop(final IWorkbenchPartReference ref) {
				if (ref instanceof IEditorReference || ref instanceof IViewReference) {
					updateTitle();
				}
			}

			public void partClosed(final IWorkbenchPartReference ref) {
				updateTitle();
			}

			public void partDeactivated(final IWorkbenchPartReference ref) {}

			public void partHidden(final IWorkbenchPartReference ref) {}

			public void partInputChanged(final IWorkbenchPartReference ref) {}

			public void partOpened(final IWorkbenchPartReference ref) {}

			public void partVisible(final IWorkbenchPartReference ref) {}
		});

		partPropertyListener = new IPropertyListener() {
			public void propertyChanged(final Object source, final int propId) {

				if (propId == IWorkbenchPartConstants.PROP_TITLE) {
					if (lastActivePart != null) {
						final String newTitle = lastActivePart.getTitle();
						if (!lastPartTitle.equals(newTitle)) {
							recomputeTitle();
						}
					}
				}
			}
		};

	}

	private void loadPeopleData() {
		final String sqlString = "SELECT *  FROM " + TourDatabase.TABLE_TOUR_PERSON; //$NON-NLS-1$

		try {
			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			final ResultSet result = statement.executeQuery();

			if (result.next()) {
				// people are available, nothing more to do
				return;
			} else {

				// no people are in the db, open the pref dialog to enter people

				final Shell activeShell = Display.getCurrent().getActiveShell();

				MessageDialog.openInformation(activeShell,
						Messages.App_Dlg_first_startup_title,
						Messages.App_Dlg_first_startup_msg);

				final PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(activeShell,
						"net.tourbook.preferences.PrefPageClients", //$NON-NLS-1$
						null,
						null);

				dialog.open();

				// open raw data view
				try {
					getWindowConfigurer().getWindow().getActivePage().showView(RawDataView.ID,
							null,
							IWorkbenchPage.VIEW_ACTIVATE);
				} catch (final PartInitException e) {
					e.printStackTrace();
				}

			}
			conn.close();

			// select person/tour type which was selected in the last session
			fApplicationActionBarAdvisor.personSelector.fireEventNewPersonIsSelected();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	@Override
	public void postWindowCreate() {

		// show editor area
//		IWorkbenchPage activePage = getWindowConfigurer().getWindow().getActivePage();
//		activePage.setEditorAreaVisible(true);

		final IWorkbenchWindowConfigurer configurer = getWindowConfigurer();

		configurer.setTitle(Messages.App_Title + " - " + MyTourbookSplashHandler.APP_BUILD_ID); //$NON-NLS-1$

	}

	@Override
	public void postWindowOpen() {

		TagManager.restoreSettings();
		loadPeopleData();
	}

	@Override
	public void preWindowOpen() {

		final IWorkbenchWindowConfigurer configurer = getWindowConfigurer();

		configurer.setInitialSize(new Point(900, 700));
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);
		configurer.setShowProgressIndicator(true);
		configurer.setShowPerspectiveBar(true);

		configurer.setTitle(Messages.App_Title + " - " + MyTourbookSplashHandler.APP_BUILD_ID); //$NON-NLS-1$

		final IPreferenceStore preferenceStore = PlatformUI.getPreferenceStore();

		preferenceStore.setValue(IWorkbenchPreferenceConstants.SHOW_MEMORY_MONITOR, true);
		preferenceStore.setValue(IWorkbenchPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS, true);
		preferenceStore.setValue(IWorkbenchPreferenceConstants.SHOW_PROGRESS_ON_STARTUP, true);

		hookTitleUpdateListeners(configurer);
	}

	@Override
	public boolean preWindowShellClose() {

//		TourDatabase.getInstance().closeConnectionPool();

		TagManager.saveSettings();

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
		if (activePart == lastActivePart && currentPage == lastActivePage && persp == lastPerspective) {
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
}
