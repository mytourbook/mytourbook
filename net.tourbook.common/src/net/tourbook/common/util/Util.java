/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;

import net.tourbook.common.UI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.joda.time.DateTime;
import org.xml.sax.Attributes;

public class Util {

	private static final String	URL_SPACE								= " ";		//$NON-NLS-1$
	private static final String	URL_SPACE_REPLACEMENT					= "%20";	//$NON-NLS-1$

	public static final String	UNIQUE_ID_SUFFIX_GARMIN_FIT				= "12653";	//$NON-NLS-1$
	public static final String	UNIQUE_ID_SUFFIX_GARMIN_TCX				= "42984";	//$NON-NLS-1$
	public static final String	UNIQUE_ID_SUFFIX_GPX					= "31683";	//$NON-NLS-1$
	public static final String	UNIQUE_ID_SUFFIX_NMEA					= "32481";	//$NON-NLS-1$
	public static final String	UNIQUE_ID_SUFFIX_POLAR_HRM				= "63193";	//$NON-NLS-1$
	public static final String	UNIQUE_ID_SUFFIX_POLAR_PDD				= "76913";	//$NON-NLS-1$
	public static final String	UNIQUE_ID_SUFFIX_POLAR_TRAINER			= "13457";	//$NON-NLS-1$
	public static final String	UNIQUE_ID_SUFFIX_SPORT_TRACKS_FITLOG	= "24168";	//$NON-NLS-1$

	public static int adjustScaleValueOnMouseScroll(final MouseEvent event) {

		// accelerate with Ctrl + Shift key
		final int accelerator = getKeyAccelerator(event);

		final Scale scale = (Scale) event.widget;
		final int newValueDiff = event.count > 0 ? accelerator : -accelerator;

		return scale.getSelection() + newValueDiff;
	}

	public static void adjustSpinnerValueOnMouseScroll(final MouseEvent event) {

		final int accelerator = getKeyAccelerator(event);

		final Spinner spinner = (Spinner) event.widget;
		final int newValue = ((event.count > 0 ? 1 : -1) * accelerator);

		spinner.setSelection(spinner.getSelection() + newValue);
	}

	/**
	 * @param text
	 * @return Returns MD5 for the text or <code>null</code> when an error occures.
	 */
	public static String computeMD5(final String text) {

		MessageDigest md5Checker;
		try {
			md5Checker = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
		} catch (final NoSuchAlgorithmException e) {
			return null;
		}

		final byte[] textBytes = text.getBytes();
		for (final byte textByte : textBytes) {
			md5Checker.update(textByte);
		}

		final byte[] digest = md5Checker.digest();

		final StringBuilder buf = new StringBuilder();
		for (final byte element : digest) {
			if ((element & 0xFF) < 0x10) {
				buf.append('0');
			}
			buf.append(Integer.toHexString(element & 0xFF));
		}

		return buf.toString();
	}

	public static float[][] convertDoubleToFloat(final double[][] doubleSeries) {

		final float[][] floatSeries = new float[doubleSeries.length][];

		for (int serieIndex = 0; serieIndex < doubleSeries.length; serieIndex++) {

			final double[] doubleValues = doubleSeries[serieIndex];
			if (doubleValues != null) {

				final float[] floatSerie = floatSeries[serieIndex] = new float[doubleValues.length];

				for (int floatIndex = 0; floatIndex < floatSerie.length; floatIndex++) {
					floatSeries[serieIndex][floatIndex] = (float) doubleValues[floatIndex];
				}
			}
		}
		return floatSeries;
	}

	public static double[] convertIntToDouble(final int[] intValues) {

		if (intValues == null) {
			return null;
		}

		if (intValues.length == 0) {
			return new double[0];
		}

		final double[] doubleValues = new double[intValues.length];

		for (int valueIndex = 0; valueIndex < intValues.length; valueIndex++) {
			doubleValues[valueIndex] = intValues[valueIndex];
		}

		return doubleValues;
	}

	public static float[] convertIntToFloat(final int[] intValues) {

		if (intValues == null) {
			return null;
		}

		if (intValues.length == 0) {
			return new float[0];
		}

		final float[] floatValues = new float[intValues.length];

		for (int valueIndex = 0; valueIndex < intValues.length; valueIndex++) {
			floatValues[valueIndex] = intValues[valueIndex];
		}

		return floatValues;
	}

	public static float[][] convertIntToFloat(final int[][] intValues) {

		if (intValues == null) {
			return null;
		}

		if (intValues.length == 0 || intValues[0].length == 0) {
			return new float[0][0];
		}

		final float[][] floatValues = new float[intValues.length][intValues[0].length];

		for (int outerIndex = 0; outerIndex < intValues.length; outerIndex++) {

			final int[] intOuterValues = intValues[outerIndex];
			final float[] floutOuterValues = floatValues[outerIndex];

			for (int innerIndex = 0; innerIndex < intOuterValues.length; innerIndex++) {
				floutOuterValues[innerIndex] = intOuterValues[innerIndex];
			}
		}

		return floatValues;
	}

	/**
	 * To convert the InputStream to String we use the BufferedReader.readLine() method. We iterate
	 * until the BufferedReader return null which means there's no more data to read. Each line will
	 * appended to a StringBuilder and returned as String.
	 */
	public static String convertStreamToString(final InputStream is) throws IOException {

		if (is != null) {

			final StringBuilder sb = new StringBuilder();
			String line;

			try {
				final BufferedReader reader = new BufferedReader(new InputStreamReader(is, UI.UTF_8));
				while ((line = reader.readLine()) != null) {
					sb.append(line).append(UI.NEW_LINE);
				}
			} finally {
				is.close();
			}
			return sb.toString();
		} else {
			return UI.EMPTY_STRING;
		}
	}

	/**
	 * @param sourceString
	 * @param lookFor
	 * @return Returns the number of characters which are found in the string or -1 when the string
	 *         is <code>null</code>
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
	 * Creates a {@link DateTime} from the number: YYYYMMDDhhmmss
	 * 
	 * @param yyyymmddhhmmss
	 * @return
	 */
	public static DateTime createDateTimeFromYMDhms(final long yyyymmddhhmmss) {

		final int year = (int) (yyyymmddhhmmss / 10000000000L) % 10000;
		final int month = (int) (yyyymmddhhmmss / 100000000) % 100;
		final int day = (int) (yyyymmddhhmmss / 1000000) % 100;
		final int hour = (int) (yyyymmddhhmmss / 10000) % 100;
		final int minute = (int) (yyyymmddhhmmss / 100 % 100);
		final int second = (int) (yyyymmddhhmmss % 100);

		return new DateTime(year, month, day, hour, minute, second, 0);
	}

	/**
	 * creates a double array backup
	 * 
	 * @param original
	 * @return Returns a copy of a <code>double[]</code> or <code>null</code> when the original data
	 *         is <code>null</code>.
	 */
	public static double[] createDoubleCopy(final double[] original) {

		double[] backup = null;

		if (original != null) {
			final int serieLength = original.length;
			backup = new double[serieLength];
			System.arraycopy(original, 0, backup, 0, serieLength);
		}

		return backup;
	}

	/**
	 * creates a float array backup
	 * 
	 * @param original
	 * @return Returns a copy of a <code>float[]</code> or <code>null</code> when the original data
	 *         is <code>null</code>.
	 */
	public static float[] createFloatCopy(final float[] original) {

		float[] backup = null;

		if (original != null) {
			final int serieLength = original.length;
			backup = new float[serieLength];
			System.arraycopy(original, 0, backup, 0, serieLength);
		}

		return backup;
	}

	/**
	 * creates a int array backup
	 * 
	 * @param original
	 * @return the backup array or <code>null</code> when the original data is <code>null</code>
	 */
	public static int[] createIntegerCopy(final int[] original) {

		int[] backup = null;

		if (original != null) {
			final int serieLength = original.length;
			backup = new int[serieLength];
			System.arraycopy(original, 0, backup, 0, serieLength);
		}

		return backup;
	}

	public static void delayThread(final int delayTime) {
		try {
			Thread.sleep(delayTime);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void deleteTempFile(final File tempFile) {

		if (tempFile == null || tempFile.exists() == false) {
			return;
		}

		try {

			if (tempFile.delete() == false) {
				StatusUtil.log(String.format("Temp file cannot be deleted: %s", tempFile.getAbsolutePath())); //$NON-NLS-1$
			}

		} catch (final SecurityException e) {
			StatusUtil.showStatus(String.format("Temp file cannot be deleted: %s", tempFile.getAbsolutePath())); //$NON-NLS-1$
		}
	}

	public static Resource disposeResource(final Resource resource) {
		if (resource != null && !resource.isDisposed()) {
			resource.dispose();
		}
		return null;
	}

	public static String encodeSpace(final String urlString) {
		return urlString.replaceAll(URL_SPACE, URL_SPACE_REPLACEMENT);
	}

	private static int getKeyAccelerator(final MouseEvent event) {

		boolean isCtrlKey;
		boolean isShiftKey;

		if (UI.IS_OSX) {
			isCtrlKey = (event.stateMask & SWT.MOD1) > 0;
			isShiftKey = (event.stateMask & SWT.MOD3) > 0;
//			isAltKey = (event.stateMask & SWT.MOD3) > 0;
		} else {
			isCtrlKey = (event.stateMask & SWT.MOD1) > 0;
			isShiftKey = (event.stateMask & SWT.MOD2) > 0;
//			isAltKey = (event.stateMask & SWT.MOD3) > 0;
		}

		// accelerate with Ctrl + Shift key
		int accelerator = isCtrlKey ? 10 : 1;
		accelerator *= isShiftKey ? 5 : 1;

		return accelerator;
	}

	public static int getNumberOfDigits(int number) {

		int counter = 0;

		while (number > 0) {
			counter++;
			number = number / 10;
		}

		return counter;
	}

	public static String getSQLExceptionText(final SQLException e) {

		final StringBuilder sb = new StringBuilder()//
				.append("SQLException") //$NON-NLS-1$
				.append(UI.NEW_LINE2)
				.append("SQLState: " + (e).getSQLState()) //$NON-NLS-1$
				.append(UI.NEW_LINE)
				.append("Severity: " + (e).getErrorCode()) //$NON-NLS-1$
				.append(UI.NEW_LINE)
				.append("Message: " + (e).getMessage()) //$NON-NLS-1$
				.append(UI.NEW_LINE);

		return sb.toString();
	}

	/**
	 * @param state
	 * @param key
	 * @param defaultValue
	 * @return Returns a string value from {@link IDialogSettings}. When the key is not found, the
	 *         default value is returned.
	 */
	public static String[] getStateArray(final IDialogSettings state, final String key, final String[] defaultValue) {

		final String[] stateValue = state.getArray(key);

		return stateValue == null ? defaultValue : stateValue;
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

	/**
	 * @param state
	 * @param key
	 * @param defaultValue
	 * @return Returns a float value from {@link IDialogSettings}. When the key is not found, the
	 *         default value is returned.
	 */
	public static float getStateFloat(final IDialogSettings state, final String key, final float defaultValue) {
		try {
			return state.get(key) == null ? defaultValue : state.getFloat(key);
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * @param combo
	 *            combo box, the items in the combo box must correspond to the items in the states
	 *            array
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

	public static int[] getStateIntArray(final IDialogSettings state, final String key, final int[] defaultValue) {

		final String[] stringValues = state.getArray(key);

		if (stringValues == null) {
			return defaultValue;
		}

		final ArrayList<Integer> intValues = new ArrayList<Integer>();

		for (final String stringValue : stringValues) {

			try {

				final int intValue = Integer.parseInt(stringValue);

				intValues.add(intValue);

			} catch (final NumberFormatException e) {
				// just ignore
			}
		}

		int intIndex = 0;
		final int[] intintValues = new int[intValues.size()];
		for (final Integer intValue : intValues) {
			intintValues[intIndex++] = intValue;
		}

		return intintValues;
	}

	/**
	 * @param state
	 * @param key
	 * @param defaultValue
	 * @return Returns a long value from {@link IDialogSettings}. When the key is not found, the
	 *         default value is returned.
	 */
	public static long getStateLong(final IDialogSettings state, final String key, final long defaultValue) {
		try {
			return state.get(key) == null ? defaultValue : state.getLong(key);
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

	public static long[] getStateLongArray(final IDialogSettings state, final String key, final long[] defaultValue) {

		final String[] stringValues = state.getArray(key);

		if (stringValues == null) {
			return defaultValue;
		}

		final ArrayList<Long> longValues = new ArrayList<Long>();

		for (final String stringValue : stringValues) {

			try {

				final long longValue = Long.parseLong(stringValue);

				longValues.add(longValue);

			} catch (final NumberFormatException e) {
				// just ignore
			}
		}

		int intIndex = 0;
		final long[] longlongValues = new long[longValues.size()];
		for (final Long longValue : longValues) {
			longlongValues[intIndex++] = longValue;
		}

		return longlongValues;
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
	 * Remove duplicate items from a list and add a new item.
	 * 
	 * @param allItems
	 * @param newItem
	 * @param maxItems
	 * @return
	 */
	public static String[] getUniqueItems(final String[] allItems, final String newItem, final int maxItems) {

		final ArrayList<String> newItems = new ArrayList<String>();

		newItems.add(newItem);

		for (final String oldItem : allItems) {

			// ignore duplicate entries
			if (newItem.equals(oldItem) == false) {
				newItems.add(oldItem);
			}

			if (maxItems > 0) {
				if (newItems.size() >= maxItems) {
					break;
				}
			}
		}

		return newItems.toArray(new String[newItems.size()]);
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
	 * @param fileName
	 * @return Returns <code>true</code> when fileName is a valid directory
	 */
	public static boolean isDirectory(String fileName) {

		fileName = fileName.trim();
		final File file = new File(fileName);

		return file.isDirectory();
	}

	/**
	 * Open link in the browser
	 */
	public static void openLink(final Shell shell, String href) {

		// format the href for an html file (file:///<filename.html>
		// required for Mac only.
		if (href.startsWith("file:")) { //$NON-NLS-1$
			href = href.substring(5);
			while (href.startsWith("/")) { //$NON-NLS-1$
				href = href.substring(1);
			}
			href = "file:///" + href; //$NON-NLS-1$
		}

		final IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();

		try {

			final IWebBrowser browser = support.getExternalBrowser();
			browser.openURL(new URL(urlEncodeForSpaces(href.toCharArray())));

		} catch (final MalformedURLException e) {
			StatusUtil.showStatus(e);
		} catch (final PartInitException e) {
			StatusUtil.showStatus(e);
		}
	}

	/**
	 * Parses SAX attribute
	 * 
	 * @param attributes
	 * @param attributeName
	 * @return Returns double value or {@link Double#MIN_VALUE} when attribute is not available or
	 *         cannot be parsed.
	 */
	public static double parseDouble(final Attributes attributes, final String attributeName) {

		try {
			final String valueString = attributes.getValue(attributeName);
			if (valueString != null) {
				return Double.parseDouble(valueString);
			}
		} catch (final NumberFormatException e) {
			// do nothing
		}
		return Double.MIN_VALUE;
	}

	/**
	 * Parses SAX attribute
	 * 
	 * @param attributes
	 * @param attributeName
	 * @return Returns float value or {@link Float#MIN_VALUE} when attribute is not available or
	 *         cannot be parsed.
	 */
	public static float parseFloat(final Attributes attributes, final String attributeName) {

		try {
			final String valueString = attributes.getValue(attributeName);
			if (valueString != null) {
				return Float.parseFloat(valueString);
			}
		} catch (final NumberFormatException e) {
			// do nothing
		}
		return Float.MIN_VALUE;
	}

	/**
	 * Parses SAX attribute
	 * 
	 * @param attributes
	 * @param attributeName
	 * @return Returns integer value or {@link Integer#MIN_VALUE} when attribute is not available or
	 *         cannot be parsed.
	 */
	public static int parseInt(final Attributes attributes, final String attributeName) {

		try {
			final String valueString = attributes.getValue(attributeName);
			if (valueString != null) {
				return Integer.parseInt(valueString);
			}
		} catch (final NumberFormatException e) {
			// do nothing
		}
		return Integer.MIN_VALUE;
	}

	/**
	 * Parses SAX attribute
	 * 
	 * @param attributes
	 * @param attributeName
	 * @return Returns integer value or 0 when attribute is not available or cannot be parsed.
	 */
	public static int parseInt0(final Attributes attributes, final String attributeName) {

		try {
			final String valueString = attributes.getValue(attributeName);
			if (valueString != null) {
				return Integer.parseInt(valueString);
			}
		} catch (final NumberFormatException e) {
			// do nothing
		}
		return 0;
	}

	/**
	 * Parses SAX attribute
	 * 
	 * @param attributes
	 * @param attributeName
	 * @return Returns long value or {@link Long#MIN_VALUE} when attribute is not available or
	 *         cannot be parsed.
	 */
	public static long parseLong(final Attributes attributes, final String attributeName) {

		try {
			final String valueString = attributes.getValue(attributeName);
			if (valueString != null) {
				return Long.parseLong(valueString);
			}
		} catch (final NumberFormatException e) {
			// do nothing
		}
		return Long.MIN_VALUE;
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
	 * Selects a text item in the combo box. When text item is not available, the first item is
	 * selected
	 * 
	 * @param combo
	 * @param comboItems
	 * @param selectedItem
	 *            Text which should be selected in the combo box
	 */
	public static void selectTextInCombo(final Combo combo, final String[] comboItems, final String selectedItem) {

		int comboIndex = 0;

		for (final String comboStateValue : comboItems) {
			if (selectedItem.equals(comboStateValue)) {
				break;
			}

			comboIndex++;
		}

		combo.select(comboIndex);
	}

	/**
	 * Set the state for an integer array, integer values are converted into string value.
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

	public static void setState(final IDialogSettings state, final String stateKey, final long[] values) {

		final String[] stateIndices = new String[values.length];

		for (int index = 0; index < values.length; index++) {
			stateIndices[index] = Long.toString(values[index]);
		}

		state.put(stateKey, stateIndices);
	}

	public static void showSQLException(SQLException e) {

		while (e != null) {

			final String sqlExceptionText = getSQLExceptionText(e);

			System.out.println(sqlExceptionText);
			e.printStackTrace();

			MessageDialog.openError(Display.getCurrent().getActiveShell(), //
					"SQL Error",//$NON-NLS-1$
					sqlExceptionText);

			e = e.getNextException();
		}

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

	/**
	 * Open view and do not activate it
	 * 
	 * @param viewId
	 * @return
	 * @throws PartInitException
	 */
	public static IViewPart showViewNotActive(final String viewId) {

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

			return page.showView(viewId, null, IWorkbenchPage.VIEW_VISIBLE);

		} catch (final PartInitException e) {
			StatusUtil.showStatus(e);
		}

		return null;
	}

	public static void sqlClose(final Statement stmt) {

		if (stmt != null) {
			try {
				stmt.close();
			} catch (final SQLException e) {
				showSQLException(e);
			}
		}
	}

	/**
	 * This method encodes the url, removes the spaces from the url and replaces the same with
	 * <code>"%20"</code>. This method is required to fix Bug 77840.
	 * 
	 * @since 3.0.2
	 */
	private static String urlEncodeForSpaces(final char[] input) {
		final StringBuffer retu = new StringBuffer(input.length);
		for (final char element : input) {
			if (element == ' ') {
				retu.append("%20"); //$NON-NLS-1$
			} else {
				retu.append(element);
			}
		}
		return retu.toString();
	}
}
