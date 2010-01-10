package de.byteholder.geoclipse.mapprovider;

import java.util.concurrent.ConcurrentLinkedQueue;

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
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import de.byteholder.geoclipse.Activator;
import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.UI;
import de.byteholder.geoclipse.preferences.IMappingPreferences;
import de.byteholder.gpx.GeoPosition;

public class DialogMP extends TitleAreaDialog {

	private static final String				TIME_SPACER				= "       ";

	private static final String				COLUMN_SPACER			= "  ";										//$NON-NLS-1$

	private static final String				DEFAULT_MONO_FONT		= "Courier";

	protected static final int				MAX_VISIBLE_LOG_ENTRIES	= 500;

	private static final DateTimeFormatter	fDateTimeFormatter		= ISODateTimeFormat.hourMinuteSecondMillis();

	// the config dialogs use different map providers
	private MP_OLD								fMapProvider;

	// map is shared
	protected Map							fMap;

	private Font							fFontMono;

//	private static void displayAllLoadedFonts(final Shell shell) {
//		// display all scalable fonts in the system
//		FontData[] fd = shell.getDisplay().getFontList(null, true);
//		for (int i = 0; i < fd.length; i++) {
//			System.out.println(fd[i].getName());
//		}
//		// and the non-scalable ones
//		fd = shell.getDisplay().getFontList(null, false);
//		for (int i = 0; i < fd.length; i++) {
//			System.out.println(fd[i].getName());
//			// TODO remove SYSTEM.OUT.PRINTLN
//
//		}
//		System.out.println();
//		System.out.println();
//		System.out.println();
//	}

	public DialogMP(final Shell parentShell, final MP_OLD mapProvider) {
		super(parentShell);
		fMapProvider = mapProvider;
	}

	protected void actionSetFavoritePosition() {

		final int zoom = fMap.getZoom();
		final GeoPosition centerPosition = fMap.getCenterPosition();

		fMapProvider.setFavoriteZoom(zoom);
		fMapProvider.setFavoritePosition(centerPosition);

		fMapProvider.setLastUsedZoom(zoom);
		fMapProvider.setLastUsedPosition(centerPosition);
	}

	protected void actionShowFavoritePosition() {

		fMap.setZoom(fMapProvider.getFavoriteZoom());
		fMap.setGeoCenterPosition(fMapProvider.getFavoritePosition());

		fMap.queueMapRedraw();
 	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

//		displayAllLoadedFonts(shell);

		createMonoFont(shell.getDisplay());

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				if (fFontMono != null) {
					fFontMono.dispose();
				}
			}
		});
	}

	private void createMonoFont(final Display display) {

		final IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();

		final String loggingFontPrefs = prefStore.getString(IMappingPreferences.THEME_FONT_LOGGING);
		if (loggingFontPrefs.length() > 0) {
			try {
				fFontMono = new Font(display, new FontData(loggingFontPrefs));
			} catch (final Exception e) {
				// ignore
			}
		}

		if (fFontMono == null) {
			fFontMono = new Font(display, DEFAULT_MONO_FONT, 8, SWT.NORMAL);
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
			sb.append(fDateTimeFormatter.print(logEntry.time));
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

				sb.append(" ");

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
		return fFontMono;
	}

	protected void setMap(final Map map) {
		fMap = map;
	}
}
