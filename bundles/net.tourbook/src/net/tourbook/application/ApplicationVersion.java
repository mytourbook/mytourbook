/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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

import net.tourbook.ui.UI;

import org.osgi.framework.Version;

public class ApplicationVersion {

	/**
	 * Copyright year which is displayed in the splash screen.
	 */
	public static final String	SPLASH_COPYRIGHT_YEAR		= "2018";			//$NON-NLS-1$

// X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X
	private static final String	DEVELOPMENT_VERSION_TEXT	= UI.EMPTY_STRING;
	private static final String	DEV_WINDOW_TITLE			= UI.EMPTY_STRING;
	private static String		_subVersion					= UI.EMPTY_STRING;

//// X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X
//	private static final String	DEVELOPMENT_VERSION_TEXT	= "DEVELOPMENT";									//$NON-NLS-1$
//	private static final String	DEV_WINDOW_TITLE			= UI.DASH_WITH_SPACE + DEVELOPMENT_VERSION_TEXT;
//	private static String		_subVersion					= " improvements";									//$NON-NLS-1$

// X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X
//	private static final String	DEVELOPMENT_VERSION_TEXT	= "WEB 2.0";										//$NON-NLS-1$
//	private static final String	DEV_WINDOW_TITLE			= UI.DASH_WITH_SPACE + DEVELOPMENT_VERSION_TEXT;
//	private static String		_subVersion					= UI.EMPTY_STRING;

	private static String		_versionFull;
	private static String		_versionSimple;
	private static String		_qualifierText;

	private static boolean		_isDev;

	private static void createVersionText() {

		final Version version = TourbookPlugin.getDefault().getVersion();
		final String qualifier = version.getQualifier();

		_isDev = qualifier.contains("qualifier"); //$NON-NLS-1$

		_qualifierText = _isDev ? //
				//
				// this text is used to identify development versions
				DEVELOPMENT_VERSION_TEXT
				//
				: qualifier;

		_qualifierText += _subVersion;

		_versionSimple = UI.EMPTY_STRING
				+ version.getMajor()
				+ UI.SYMBOL_DOT
				+ version.getMinor()
				+ UI.SYMBOL_DOT
				+ version.getMicro();

		_versionFull = _versionSimple + UI.SYMBOL_DOT + _qualifierText;
	}

	public static String getDevelopmentId() {

		String id = _isDev ? DEV_WINDOW_TITLE : UI.EMPTY_STRING;

		id += _subVersion;

		return id;
	}

	public static String getVersionFull() {

		if (_versionFull != null) {
			return _versionFull;
		}

		createVersionText();

		return _versionFull;
	}

	public static String getVersionQualifier() {

		if (_qualifierText != null) {
			return _qualifierText;
		}

		createVersionText();

		return _qualifierText;
	}

	public static String getVersionSimple() {

		if (_versionSimple != null) {
			return _versionSimple;
		}

		createVersionText();

		return _versionSimple;
	}
}
