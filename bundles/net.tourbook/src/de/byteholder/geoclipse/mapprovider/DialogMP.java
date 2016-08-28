/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package de.byteholder.geoclipse.mapprovider;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.time.TimeTools;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.UI;
import de.byteholder.geoclipse.preferences.IMappingPreferences;

public class DialogMP extends TitleAreaDialog {

	private static final String				TIME_SPACER				= "       ";			//$NON-NLS-1$
	private static final String				COLUMN_SPACER			= "  ";				//$NON-NLS-1$

	private static final String				DEFAULT_MONO_FONT		= "Courier";			//$NON-NLS-1$

	private static final DateTimeFormatter	_timeFormatter			= new DateTimeFormatterBuilder()
																			.append(DateTimeFormatter.ISO_TIME)
																			.appendInstant(3)
																			.toFormatter();

	protected static final int				MAX_VISIBLE_LOG_ENTRIES	= 500;

	// the config dialogs is using different map providers
	private MP								_mp;

	// map is shared
	protected Map							_map;

	private Font							_fontMono;

	public DialogMP(final Shell parentShell, final MP mp) {
		super(parentShell);
		_mp = mp;
	}

	protected void actionSetFavoritePosition() {

		final int zoom = _map.getZoom();
		final GeoPosition centerPosition = _map.getGeoCenter();

		_mp.setFavoriteZoom(zoom);
		_mp.setFavoritePosition(centerPosition);

		_mp.setLastUsedZoom(zoom);
		_mp.setLastUsedPosition(centerPosition);
	}

	protected void actionShowFavoritePosition() {

		_map.setZoom(_mp.getFavoriteZoom());
		_map.setMapCenter(_mp.getFavoritePosition());

		_map.paint();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		createMonoFont(shell.getDisplay());

		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				if (_fontMono != null) {
					_fontMono.dispose();
				}
			}
		});
	}

	private void createMonoFont(final Display display) {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		final String loggingFontPrefs = prefStore.getString(IMappingPreferences.THEME_FONT_LOGGING);
		if (loggingFontPrefs.length() > 0) {
			try {
				_fontMono = new Font(display, new FontData(loggingFontPrefs));
			} catch (final Exception e) {
				// ignore
			}
		}

		if (_fontMono == null) {
			_fontMono = new Font(display, DEFAULT_MONO_FONT, 9, SWT.NORMAL);
		}
	}

	/**
	 * @param logEntries
	 * @param comboTileImageLog
	 * @param tile
	 * @return Returns the newest log entry
	 */
	protected String displayLogEntries(final ConcurrentLinkedQueue<LogEntry> logEntries, final Combo comboTileImageLog) {

		LogEntry logEntry;

		final StringBuilder sb = new StringBuilder();
		String logText = null;

		while ((logEntry = logEntries.poll()) != null) {

			// remove old log entry
			if (comboTileImageLog.getItemCount() > MAX_VISIBLE_LOG_ENTRIES) {
				comboTileImageLog.remove(0);
			}

			sb.setLength(0);
			sb.append(TimeTools.getZonedDateTime(logEntry.time).format(_timeFormatter));
			sb.append(COLUMN_SPACER);
			sb.append(logEntry.counter);
			sb.append(COLUMN_SPACER);
			sb.append(logEntry.threadName);
			sb.append(COLUMN_SPACER);
			sb.append(logEntry.tileEventId);

			final Tile tile = logEntry.tile;
			if (tile != null) {

				final long queued = tile.getTimeIsQueued();
				final long start = tile.getTimeStartLoading();
				final long end = tile.getTimeEndLoading();

				sb.append(COLUMN_SPACER);

				// column: time how long it takes until loading starts
				String time = TIME_SPACER;
				if (start != 0) {
					time = time + Long.toString((start - queued) / 1000000 % 1000000);
				}
				int length = time.length();
				sb.append(time.substring(length - 7, length));

				sb.append(" "); //$NON-NLS-1$

				// column: time how long the tile image is being loaded
				time = TIME_SPACER;
				if (end != 0) {
					time = time + Long.toString((end - start) / 1000000 % 1000000);
				}
				length = time.length();
				sb.append(time.substring(length - 7, length));

				// column: tile key
				sb.append(COLUMN_SPACER);
				sb.append(tile.toString());

				if (tile.getOfflinePath() != null) {
					sb.append(COLUMN_SPACER);
					sb.append(tile.getOfflinePath());
				}
				if (tile.getUrl() != null) {
					sb.append(COLUMN_SPACER);
					sb.append(tile.getUrl());
				}
			}

			logText = sb.toString();

			comboTileImageLog.add(logText);

			// log to console
			System.out.println(logText);
		}

		return logText == null ? UI.EMPTY_STRING : logText;
	}

	protected Font getMonoFont() {
		return _fontMono;
	}

//	protected void setMap(final Map map) {
//		_map = map;
//	}
}
