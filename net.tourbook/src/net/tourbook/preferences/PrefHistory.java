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
package net.tourbook.preferences;

import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;

public class PrefHistory {
	/**
	 * max number of strings store in preference file.
	 */
	public static final int	MAX_HISTORY_ENTRIES	= 10;

	/**
	 * This method added one string to a existing preference store. Empty string will be filters and
	 * leading and trailing whitespace omitted. They strings can be restored with the method
	 * getHistory.
	 * 
	 * @see getHistory
	 * @see ITourbookPreferences
	 * @author Stefan F.
	 * @param prefName
	 *            Name of the preference key defined in interface ITourbookPreferences
	 * @param string
	 *            string to store
	 * @return true if history has been changed
	 */
	public static final boolean saveHistory(String prefName, String string) {
		String[] previous = getHistory(prefName);

		if (string == null)
			return false;

		String newEntry = string.trim();

		if (newEntry.isEmpty())
			return false;

		ArrayList<String> list = new ArrayList<String>();

		for (String prev : previous) {
			list.add(prev);
		}

		int index = list.indexOf(string);

		if (index >= 0) {
			/* test if already at first position */
			if (index == 0)
				return false;
			/* move existing entry to first position */
			list.remove(index);
		} else if (list.size() >= MAX_HISTORY_ENTRIES) {

			/* remove last entry */
			list.remove(MAX_HISTORY_ENTRIES - 1);
		}
		/* add entry at first position */
		list.add(0, string);

		TourbookPlugin.getDefault().getPreferenceStore().setValue(prefName, createString(list));

		return true;
	}

	/**
	 * This method stores strings separated by '\r' to the plugin preference store. Empty string will
	 * be filters and leading and trailing whitespace omitted. They strings can be restored with the
	 * method getHistory.
	 * 
	 * @see getHistory
	 * @see ITourbookPreferences
	 * @author Stefan F.
	 * @param prefName
	 *            Name of the preference key defined in interface ITourbookPreferences
	 * @param list
	 *            list of strings to store
	 */
	public static final void saveHistory(String prefName, String[] list) {

		StringBuilder buffer = new StringBuilder(64);
		int entries = 0;
		for (String element : list) {
			if (!element.trim().isEmpty()) {
				if (entries != 0)
					buffer.append('\r'); //$NON-NLS-1$
				buffer.append(element);
				entries++;
			}
			if (entries >= MAX_HISTORY_ENTRIES)
				break;
		}
		TourbookPlugin.getDefault().getPreferenceStore().setValue(prefName, buffer.toString());

	}

	/**
	 * Read stored preferences from plugin cache. This method can only read preferences stored with
	 * the function saveHistory.
	 * 
	 * @see saveHistory
	 * @see ITourbookPreferences
	 * @author Stefan F.
	 * @param prefName
	 *            Name of the preference key defined in interface ITourbookPreferences
	 * @return All stored entries as array of String.
	 */
	public static String[] getHistory(String prefName) {
		String string = TourbookPlugin.getDefault().getPreferenceStore().getString(prefName);
		if (string == null) {
			return new String[] {}; //$NON-NLS-1$
		}

		return string.split("\r"); //$NON-NLS-1$
	}

	/**
	 * Helper method for saveHistory, creates one String '\r' separated to store prefernces
	 * 
	 * @see saveHistory
	 * @author Stefan F.
	 * @param list
	 *            Array of strings to convert
	 * @return ';' separated String.
	 */
	private static String createString(ArrayList<String> list) {
		StringBuilder buffer = new StringBuilder();
		for (String element : list) {
			buffer.append(element);
			buffer.append("\r"); //$NON-NLS-1$
		}
		/* remove last ';' */
		if (buffer.length() > 0)
			buffer.setLength(buffer.length() - 1);

		return buffer.toString();
	}

}
