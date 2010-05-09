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
import org.eclipse.swt.widgets.Combo;
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
	 * @param state
	 * @param key
	 * @param defaultValue
	 * @return Returns a boolean value from {@link IDialogSettings}. When the key is not found, the
	 *         default value is returned.
	 */
	public static boolean getStateBoolean(final IDialogSettings state, final String key, final boolean defaultValue) {
		return state.get(key) == null ? defaultValue : state.getBoolean(key);
	}

	/**
	 * @param state
	 * @param key
	 * @param defaultValue
	 * @return Returns a float value from {@link IDialogSettings}. When the key is not found, the
	 *         default value is returned.
	 */
	public static double getStateDouble(final IDialogSettings state, final String key, final double defaultValue) {
		try {
			return state.get(key) == null ? defaultValue : state.getDouble(key);
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

//	/**
//	 * @param combo
//	 *            combo box, the items in the combo box
//	 *            must correspond to the items in the states array
//	 * @param states
//	 *            array which contains all states
//	 * @return Returns the state which is selected or null when nothing is selected
//	 */
//	public static String getStateFromCombo(final Combo combo, final String[] states) {
//
//		final int selectedIndex = combo.getSelectionIndex();
//		if (selectedIndex == -1 || selectedIndex >= states.length) {
//			return null;
//		} else {
//			return states[selectedIndex];
//		}
//	}

	/**
	 * @param combo
	 *            combo box, the items in the combo box
	 *            must correspond to the items in the states array
	 * @param states
	 *            array which contains all states
	 * @param defaultState
	 *            state when an item is not selected in the combo box
	 * @return Returns the state which is selected in the combo box
	 */
	public static String getStateFromCombo(final Combo combo, final String[] states, final String defaultState) {

		final int selectedIndex = combo.getSelectionIndex();

		String selectedState;

		if (selectedIndex == -1) {
			selectedState = defaultState;
		} else {
			selectedState = states[selectedIndex];
		}

		return selectedState;
	}

	/**
	 * @param state
	 * @param key
	 * @param defaultValue
	 * @return Returns an integer value from {@link IDialogSettings}. When the key is not found, the
	 *         default value is returned.
	 */
	public static int getStateInt(final IDialogSettings state, final String key, final int defaultValue) {
		try {
			return state.get(key) == null ? defaultValue : state.getInt(key);
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * @param state
	 * @param key
	 * @param defaultValue
	 * @return Returns a long value from {@link IDialogSettings}. When the key is not found, the
	 *         default value is returned.
	 */
	public static long getStateLong(final IDialogSettings state, final String key, final int defaultValue) {
		try {
			return state.get(key) == null ? defaultValue : state.getLong(key);
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * @param state
	 * @param key
	 * @param defaultValue
	 * @return Returns a string value from {@link IDialogSettings}. When the key is not found, the
	 *         default value is returned.
	 */
	public static String getStateString(final IDialogSettings state, final String key, final String defaultValue) {

		final String stateValue = state.get(key);

		return stateValue == null ? defaultValue : stateValue;
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
	 * Selects an item in the combo box which is retrieved from a state.
	 * 
	 * @param state
	 * @param stateKey
	 * @param comboStates
	 *            this array must must have the same number of entries as the combo box has items
	 * @param defaultState
	 * @param combo
	 */
	public static void selectStateInCombo(	final IDialogSettings state,
											final String stateKey,
											final String[] comboStates,
											final String defaultState,
											final Combo combo) {

		final String stateValue = Util.getStateString(state, stateKey, defaultState);

		int stateIndex = 0;
		for (final String comboStateValue : comboStates) {
			if (stateValue.equals(comboStateValue)) {
				break;
			}

			stateIndex++;
		}

		combo.select(stateIndex);
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
