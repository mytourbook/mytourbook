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
package net.tourbook.util;

import java.util.Calendar;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class Util {

	/**
	 * @param sourceString
	 * @param lookFor
	 * @return Returns the number of characters which are found in the string or -1 when the
	 *         string is <code>null</code>
	 */
	public static int countCharacter(final String sourceString, final char lookFor) {

		if (sourceString == null) {
			return -1;
		}

		int count = 0;

		for (int i = 0; i < sourceString.length(); i++) {
			final char c = sourceString.charAt(i);
			if (c == lookFor) {
				count++;
			}
		}

		return count;
	}

	/**
	 * creates a int array backup
	 * 
	 * @param original
	 * @return the backup array or <code>null</code> when the original data is <code>null</code>
	 */
	public static int[] createDataSerieCopy(final int[] original) {

		int[] backup = null;

		if (original != null) {
			final int serieLength = original.length;
			backup = new int[serieLength];
			System.arraycopy(original, 0, backup, 0, serieLength);
		}

		return backup;
	}

	public static Resource disposeResource(final Resource resource) {
		if (resource != null && !resource.isDisposed()) {
			resource.dispose();
		}
		return null;
	}

	/**
	 * @param settings
	 * @param key
	 * @param defaultValue
	 * @return Returns a boolean value from {@link IDialogSettings}. When the key is not found, the
	 *         default value is returned.
	 */
	public static boolean getStateBoolean(final IDialogSettings settings, final String key, final boolean defaultValue) {
		return settings.get(key) == null ? defaultValue : settings.getBoolean(key);
	}

	/**
	 * @param settings
	 * @param key
	 * @param defaultValue
	 * @return Returns a float value from {@link IDialogSettings}. When the key is not found, the
	 *         default value is returned.
	 */
	public static double getStateDouble(final IDialogSettings settings, final String key, final double defaultValue) {
		try {
			return settings.get(key) == null ? defaultValue : settings.getDouble(key);
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * @param settings
	 * @param key
	 * @param defaultValue
	 * @return Returns an integer value from {@link IDialogSettings}. When the key is not found, the
	 *         default value is returned.
	 */
	public static int getStateInt(final IDialogSettings settings, final String key, final int defaultValue) {
		try {
			return settings.get(key) == null ? defaultValue : settings.getInt(key);
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * found here: http://www.odi.ch/prog/design/datetime.php
	 * 
	 * @param cal
	 * @return
	 */
	public static int getYearForWeek(final Calendar cal) {

		final int year = cal.get(Calendar.YEAR);
		final int week = cal.get(Calendar.WEEK_OF_YEAR);
		final int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

		if (week == 1 && dayOfMonth > 20) {
			return year + 1;
		}

		if (week >= 52 && dayOfMonth < 10) {
			return year - 1;
		}

		return year;
	}

	/**
	 * Set the state for an integer array
	 * 
	 * @param state
	 * @param stateKey
	 * @param intValues
	 */
	public static void setState(final IDialogSettings state, final String stateKey, final int[] intValues) {

		final String[] stateIndices = new String[intValues.length];
		for (int index = 0; index < intValues.length; index++) {
			stateIndices[index] = Integer.toString(intValues[index]);
		}

		state.put(stateKey, stateIndices);
	}

	/**
	 * Open view and activate it
	 * 
	 * @param viewId
	 * @return
	 * @throws PartInitException
	 */
	public static IViewPart showView(final String viewId) {

		try {

			final IWorkbench wb = PlatformUI.getWorkbench();
			if (wb == null) {
				return null;
			}

			final IWorkbenchWindow wbWin = wb.getActiveWorkbenchWindow();
			if (wbWin == null) {
				return null;
			}

			final IWorkbenchPage page = wbWin.getActivePage();
			if (page == null) {
				return null;
			}

			return page.showView(viewId, null, IWorkbenchPage.VIEW_ACTIVATE);

		} catch (final PartInitException e) {
			StatusUtil.showStatus(e);
		}

		return null;
	}

}
