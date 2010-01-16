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
package de.byteholder.geoclipse.tileinfo;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.map.event.TileEventId;

/**
 */
public class TileInfoContribution extends WorkbenchWindowControlContribution {

	private static final int	UPDATE_INTERVAL	= 500;	// ms

	private TileInfoManager		fTileInfoManager;

	protected boolean			fIsUpdateUI;

	private Display				fDisplay;
	private TileInfoControl		fInfoWidget;

	private static int			fStatIsQueued;

	private static int			fStatErrorLoading;
	private static int			fStatStartLoading;
	private static int			fStatEndLoading;

	private static int			fStatErrorPaintingSRTM;
	private static int			fStatEndPaintingSRTM;
	private static int			fStatStartPaintingSRTM;

	private int					fStatStartSRTM;
	private int					fStatEndSRTM;
	private int					fStatErrorSRTM;

	private String				fSRTMRemoteName;
	private long				fSRTMReceivedBytes;

	private final Runnable		fUpdateRunnable	= new Runnable() {
													public void run() {

														if (fInfoWidget == null && fInfoWidget.isDisposed()) {
															return;
														}

														if (fIsUpdateUI) {
															fIsUpdateUI = false;
															updateUIInUIThread();
														}

														fDisplay.timerExec(UPDATE_INTERVAL, this);
													}
												};

	void actionClearStatistics() {

		fStatIsQueued = 0;

		fStatStartLoading = 0;
		fStatEndLoading = 0;
		fStatErrorLoading = 0;

		fStatEndPaintingSRTM = 0;
		fStatStartPaintingSRTM = 0;
		fStatErrorPaintingSRTM = 0;

		fStatStartSRTM = 0;
		fStatEndSRTM = 0;
		fStatErrorSRTM = 0;

		fSRTMRemoteName = null;
		fSRTMReceivedBytes = 0;

		fIsUpdateUI = true;
	}

	/**
	 * Creates the context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {
				fillMenu(menuMgr);
			}
		});

		final Menu menu = menuMgr.createContextMenu(fInfoWidget);
		fInfoWidget.setMenu(menu);
	}

	@Override
	protected Control createControl(final Composite parent) {

		if (fTileInfoManager == null) {
			fTileInfoManager = TileInfoManager.getInstance();
			fTileInfoManager.setTileInfoContribution(this);
		}

		fDisplay = parent.getDisplay();
		fDisplay.asyncExec(new Runnable() {
			public void run() {
				if (fInfoWidget != null && fInfoWidget.isDisposed() == false) {
					fDisplay.timerExec(UPDATE_INTERVAL, fUpdateRunnable);
				}
			}
		});

		fInfoWidget = new TileInfoControl(parent, getOrientation());
		createContextMenu();

		// force painting after the control is recreated when the bar was move with the mouse
		fIsUpdateUI = true;

		updateUIInUIThread();

		return fInfoWidget;
	}

	private void fillMenu(final IMenuManager menuMgr) {
		menuMgr.add(new ActionClearStatistics(this));
	}

	void updateInfo(final TileEventId tileEventId) {

		if (tileEventId == TileEventId.TILE_RESET_QUEUES) {
			fStatIsQueued = 0;
			fStatStartLoading = 0;
			fStatEndLoading = 0;
		} else if (tileEventId == TileEventId.TILE_IS_QUEUED) {
			fStatIsQueued++;
		} else if (tileEventId == TileEventId.TILE_START_LOADING) {
			fStatStartLoading++;
		} else if (tileEventId == TileEventId.TILE_END_LOADING) {
			fStatEndLoading++;
			fStatIsQueued--;
		} else if (tileEventId == TileEventId.TILE_ERROR_LOADING) {
			fStatErrorLoading++;
			fStatIsQueued--;
		} else if (tileEventId == TileEventId.SRTM_PAINTING_ERROR) {
			fStatErrorPaintingSRTM++;
			fStatIsQueued--;
		} else if (tileEventId == TileEventId.SRTM_PAINTING_START) {
			fStatStartPaintingSRTM++;
		} else if (tileEventId == TileEventId.SRTM_PAINTING_END) {
			fStatEndPaintingSRTM++;
			fStatIsQueued--;
		} else if (tileEventId == TileEventId.SRTM_DATA_START_LOADING) {
			fStatStartSRTM++;
		} else if (tileEventId == TileEventId.SRTM_DATA_END_LOADING) {
			fStatEndSRTM++;
		}

		/*
		 * when stat is cleared, que can get negative, prevent this
		 */
		if (fStatIsQueued < 0) {
			fStatIsQueued = 0;
		}

		/*
		 * adjust start value, the end value can be higher when an error occured but can confuse the
		 * user
		 */
		if (fStatEndLoading > fStatStartLoading) {
			fStatStartLoading = fStatEndLoading;
		}
		if (fStatEndPaintingSRTM > fStatStartPaintingSRTM) {
			fStatStartPaintingSRTM = fStatEndPaintingSRTM;
		}

		fIsUpdateUI = true;
	}

	public void updateSRTMInfo(final TileEventId tileEvent, final String remoteName, final long receivedBytes) {

		fSRTMRemoteName = remoteName;
		fSRTMReceivedBytes = receivedBytes < 0 ? receivedBytes : receivedBytes / 1024;

		if (tileEvent == TileEventId.SRTM_DATA_START_LOADING) {
			fStatStartSRTM++;
		} else if (tileEvent == TileEventId.SRTM_DATA_END_LOADING) {
			fSRTMRemoteName = null;
			fStatEndSRTM++;
		} else if (tileEvent == TileEventId.SRTM_DATA_ERROR_LOADING) {
			fStatErrorSRTM++;
		}

		if (Display.getCurrent() != null) {
			updateUIInUIThread();
		} else {
			fIsUpdateUI = true;
		}
	}

	private void updateUIInUIThread() {

		if (fSRTMRemoteName != null) {

			// update remote loading file

			if (fSRTMReceivedBytes <= 0) {

				// negative values show that the ftp download gets initialized

				if (fSRTMReceivedBytes == -99) {

					fInfoWidget.updateInfo(NLS.bind(Messages.TileInfo_Control_Statistics_DownloadDataFile, //
							new Object[] { Integer.toString(fStatIsQueued % 1000), fSRTMRemoteName }));

				} else {

					final StringBuffer sb = new StringBuffer();
					for (int index = 0; index < -fSRTMReceivedBytes; index++) {
						sb.append('.');
					}
					fInfoWidget.updateInfo(NLS.bind(Messages.TileInfo_Control_Statistics_DownloadDataInit, //
							new Object[] { Integer.toString(fStatIsQueued % 1000), fSRTMRemoteName, sb.toString() }));
				}

			} else {

				fInfoWidget.updateInfo(NLS.bind(Messages.TileInfo_Control_Statistics_DownloadData, new Object[] {
						Integer.toString(fStatIsQueued % 1000),
						fSRTMRemoteName,
						Long.toString(fSRTMReceivedBytes) }));
			}

			return;
		}

		final long statSum = fStatIsQueued
				+ fStatErrorLoading
				+ fStatStartLoading
				+ fStatEndLoading
				+ fStatErrorPaintingSRTM
				+ fStatStartPaintingSRTM
				+ fStatEndPaintingSRTM
				+ fStatStartSRTM
				+ fStatEndSRTM
				+ fStatErrorSRTM;

		if (statSum == 0) {
			fInfoWidget.updateInfo(Messages.TileInfo_Control_DefaultTitle);
		} else {

			// show maximum with 3 or 2 decimals
			fInfoWidget.updateInfo(
					Integer.toString(fStatIsQueued % 1000),
					Integer.toString(fStatErrorLoading % 1000),
					Integer.toString(fStatEndLoading % 1000),
					Integer.toString(fStatStartLoading % 1000),
					Integer.toString(fStatErrorPaintingSRTM % 1000),
					Integer.toString(fStatEndPaintingSRTM % 1000),
					Integer.toString(fStatStartPaintingSRTM % 1000),
					Integer.toString(fStatEndSRTM % 100),
					Integer.toString(fStatStartSRTM % 100),
					Integer.toString(fStatErrorSRTM % 100));
		}
	}
}
