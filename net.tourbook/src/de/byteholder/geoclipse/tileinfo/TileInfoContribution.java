/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

public class TileInfoContribution extends WorkbenchWindowControlContribution {

	private static final int	UPDATE_INTERVAL	= 500;	// ms

	private TileInfoManager		_tileInfoManager;

	private boolean				_isUpdateUI;

	private Display				_display;
	private TileInfoControl		_infoWidget;

	private int					_statIsQueued;

	private int					_statErrorLoading;
	private int					_statStartLoading;
	private int					_statEndLoading;

	private int					_statErrorPaintingSRTM;
	private int					_statEndPaintingSRTM;
	private int					_statStartPaintingSRTM;

	private int					_statStartSRTM;
	private int					_statEndSRTM;
	private int					_statErrorSRTM;

	private String				_srtmRemoteName;
	private long				_srtmReceivedBytes;

	private final Runnable		_updateRunnable;
	{
		_updateRunnable = new Runnable() {
			public void run() {

				if (_infoWidget == null && _infoWidget.isDisposed()) {
					return;
				}

				if (_isUpdateUI) {
					_isUpdateUI = false;
					updateUIInUIThread();
				}

				_display.timerExec(UPDATE_INTERVAL, this);
			}
		};
	}

	void actionClearStatistics() {

		_statIsQueued = 0;

		_statStartLoading = 0;
		_statEndLoading = 0;
		_statErrorLoading = 0;

		_statEndPaintingSRTM = 0;
		_statStartPaintingSRTM = 0;
		_statErrorPaintingSRTM = 0;

		_statStartSRTM = 0;
		_statEndSRTM = 0;
		_statErrorSRTM = 0;

		_srtmRemoteName = null;
		_srtmReceivedBytes = 0;

		_isUpdateUI = true;
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

		final Menu menu = menuMgr.createContextMenu(_infoWidget);
		_infoWidget.setMenu(menu);
	}

	@Override
	protected Control createControl(final Composite parent) {

		if (_tileInfoManager == null) {
			_tileInfoManager = TileInfoManager.getInstance();
			_tileInfoManager.setTileInfoContribution(this);
		}

		_display = parent.getDisplay();
		_display.asyncExec(new Runnable() {
			public void run() {
				if (_infoWidget != null && _infoWidget.isDisposed() == false) {
					_display.timerExec(UPDATE_INTERVAL, _updateRunnable);
				}
			}
		});

		_infoWidget = new TileInfoControl(parent, getOrientation());
		createContextMenu();

		// force painting after the control is recreated when the bar was move with the mouse
		_isUpdateUI = true;

		updateUIInUIThread();

		return _infoWidget;
	}

	private void fillMenu(final IMenuManager menuMgr) {
		menuMgr.add(new ActionClearStatistics(this));
	}

	public void updateInfo(final TileEventId tileEventId) {

		if (tileEventId == TileEventId.TILE_RESET_QUEUES) {

			_statIsQueued = 0;
			_statStartLoading = 0;
			_statEndLoading = 0;

		} else if (tileEventId == TileEventId.TILE_IS_QUEUED) {

			_statIsQueued++;

		} else if (tileEventId == TileEventId.TILE_START_LOADING) {

			_statStartLoading++;

		} else if (tileEventId == TileEventId.TILE_END_LOADING) {

			_statEndLoading++;
			_statIsQueued--;

		} else if (tileEventId == TileEventId.TILE_ERROR_LOADING) {

			_statErrorLoading++;
			_statIsQueued--;

		} else if (tileEventId == TileEventId.SRTM_PAINTING_ERROR) {

			_statErrorPaintingSRTM++;
			_statIsQueued--;

		} else if (tileEventId == TileEventId.SRTM_PAINTING_START) {

			_statStartPaintingSRTM++;

		} else if (tileEventId == TileEventId.SRTM_PAINTING_END) {

			_statEndPaintingSRTM++;
			_statIsQueued--;

		} else if (tileEventId == TileEventId.SRTM_DATA_START_LOADING) {

			_statStartSRTM++;

		} else if (tileEventId == TileEventId.SRTM_DATA_END_LOADING) {

			_statEndSRTM++;
		}

		/*
		 * when stat is cleared, que can get negative, prevent this
		 */
		if (_statIsQueued < 0) {
			_statIsQueued = 0;
		}

		/*
		 * adjust start value, the end value can be higher when an error occured but can confuse the
		 * user
		 */
		if (_statEndLoading > _statStartLoading) {
			_statStartLoading = _statEndLoading;
		}
		if (_statEndPaintingSRTM > _statStartPaintingSRTM) {
			_statStartPaintingSRTM = _statEndPaintingSRTM;
		}

		_isUpdateUI = true;
	}

	public void updateSRTMInfo(final TileEventId tileEvent, final String remoteName, final long receivedBytes) {

		_srtmRemoteName = remoteName;
		_srtmReceivedBytes = receivedBytes < 0 ? receivedBytes : receivedBytes / 1024;

		if (tileEvent == TileEventId.SRTM_DATA_START_LOADING) {
			_statStartSRTM++;
		} else if (tileEvent == TileEventId.SRTM_DATA_END_LOADING) {
			_srtmRemoteName = null;
			_statEndSRTM++;
		} else if (tileEvent == TileEventId.SRTM_DATA_ERROR_LOADING) {
			_statErrorSRTM++;
		}

		if (Display.getCurrent() != null) {
			updateUIInUIThread();
		} else {
			_isUpdateUI = true;
		}
	}

	private void updateUIInUIThread() {

		if (_srtmRemoteName != null) {

			// update remote loading file

			if (_srtmReceivedBytes <= 0) {

				// negative values show that the ftp download gets initialized

				if (_srtmReceivedBytes == -99) {

					_infoWidget.updateInfo(NLS.bind(Messages.TileInfo_Control_Statistics_DownloadDataFile, //
							new Object[] { Integer.toString(_statIsQueued % 1000), _srtmRemoteName }));

				} else {

					final StringBuffer sb = new StringBuffer();
					for (int index = 0; index < -_srtmReceivedBytes; index++) {
						sb.append('.');
					}
					_infoWidget.updateInfo(NLS.bind(Messages.TileInfo_Control_Statistics_DownloadDataInit, //
							new Object[] { Integer.toString(_statIsQueued % 1000), _srtmRemoteName, sb.toString() }));
				}

			} else {

				_infoWidget.updateInfo(NLS.bind(Messages.TileInfo_Control_Statistics_DownloadData, new Object[] {
						Integer.toString(_statIsQueued % 1000),
						_srtmRemoteName,
						Long.toString(_srtmReceivedBytes) }));
			}

			return;
		}

		final long statSum = _statIsQueued
				+ _statErrorLoading
				+ _statStartLoading
				+ _statEndLoading
				+ _statErrorPaintingSRTM
				+ _statStartPaintingSRTM
				+ _statEndPaintingSRTM
				+ _statStartSRTM
				+ _statEndSRTM
				+ _statErrorSRTM;

		if (statSum == 0) {
			_infoWidget.updateInfo(Messages.TileInfo_Control_DefaultTitle);
		} else {

			// show only truncated decimals
			_infoWidget.updateInfo(//
					Integer.toString(_statIsQueued % 100000),
					Integer.toString(_statErrorLoading % 10000),
					Integer.toString(_statEndLoading % 100000),
					Integer.toString(_statStartLoading % 100000),
					Integer.toString(_statErrorPaintingSRTM % 1000),
					Integer.toString(_statEndPaintingSRTM % 1000),
					Integer.toString(_statStartPaintingSRTM % 1000),
					Integer.toString(_statEndSRTM % 100),
					Integer.toString(_statStartSRTM % 100),
					Integer.toString(_statErrorSRTM % 100));
		}
	}
}
