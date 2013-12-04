/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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

	private static final String	DEVELOPMENT_VERSION_TEXT	= UI.EMPTY_STRING;
	private static final String	DEV_WINDOW_TITLE			= UI.EMPTY_STRING;
	private static String		_subVersion					= UI.EMPTY_STRING;
// X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X
//	private static final String	DEVELOPMENT_VERSION_TEXT	= "DEVELOPMENT";									//$NON-NLS-1$
//	private static final String	DEV_WINDOW_TITLE			= UI.DASH_WITH_SPACE + DEVELOPMENT_VERSION_TEXT;
//	private static String		_subVersion					= " map3";											//$NON-NLS-1$

// this is disabled because it contains redundant information and too much numbers which nobody needs
// the version number is now used from the plugin version, see below
//
//	public static final String	APP_VERSION	= TourbookPlugin.getDefault().getVersion().toString();
//	public static final String	APP_VERSION	= "11.8.2"; //$NON-NLS-1$

//	public static final String	APP_VERSION	= "11.8.2";
//	public static final String	APP_VERSION	= "11.8.1";
//	public static final String	APP_VERSION	= "11.8";
//	public static final String	APP_VERSION	= "11.3";
//	public static final String	APP_VERSION	= "10.11";
//	public static final String	APP_VERSION	= "10.10";
//	public static final String	APP_VERSION	= "10.7";
//	public static final String	APP_VERSION	= "10.3.1";
//	public static final String	APP_VERSION	= "10.3";
//	public static final String	APP_BUILD_ID_VERSION	= "10.2.1";
//	public static final String	APP_BUILD_ID			= APP_BUILD_ID_VERSION + ".v2010-02-17";
//	public static final String	APP_BUILD_ID_VERSION	= "9.08.01";
//	public static final String	APP_BUILD_ID			= APP_BUILD_ID_VERSION + ".v2009-08-21";
//	public static final String	APP_BUILD_ID_VERSION	= "9.07.0";
//	public static final String	APP_BUILD_ID			= APP_BUILD_ID_VERSION + ".v2009-07-05";
//	public static final String	APP_BUILD_ID_VERSION	= "9.05.5";
//	public static final String	APP_BUILD_ID			= APP_BUILD_ID_VERSION + ".v2009-06-21";
//	public static final String	APP_BUILD_ID_VERSION	= "9.05.4";
//	public static final String	APP_BUILD_ID			= APP_BUILD_ID_VERSION + ".v2009-06-??";
//	public static final String	APP_BUILD_ID_VERSION	= "9.05.3";
//	public static final String	APP_BUILD_ID			= APP_BUILD_ID_VERSION + ".v2009-06-08";
//	public static final String	APP_BUILD_ID_VERSION	= "9.05.1";
//	public static final String	APP_BUILD_ID			= APP_BUILD_ID_VERSION + ".v2009-05-25";
//	public static final String	APP_BUILD_ID_VERSION	= "9.05";
//	public static final String	APP_BUILD_ID			= APP_BUILD_ID_VERSION + ".v2009-05-09";
//	public static final String	APP_BUILD_ID_VERSION	= "9.01";
//	public static final String	APP_BUILD_ID			= APP_BUILD_ID_VERSION + ".v2009-01-07";
//	public static final String	APP_BUILD_ID_VERSION	= "8.11";
//	public static final String	APP_BUILD_ID			= APP_BUILD_ID_VERSION + ".v2008-11-22";
//	public static final String	APP_BUILD_ID_VERSION	= "1.6.1";
//	public static final String	APP_BUILD_ID			= APP_BUILD_ID_VERSION + ".v2008-09";
//	public static final String	APP_BUILD_ID_VERSION	= "1.6.0";
//	public static final String	APP_BUILD_ID			= APP_BUILD_ID_VERSION + ".v2008-08";
//	public static final String	APP_BUILD_ID			= "1.5.0.v20080509";
//	public static final String	APP_BUILD_ID			= "1.4.0.v20080312";
//	public static final String	APP_BUILD_ID			= "1.3.0.v20080125";
//	public static final String	APP_BUILD_ID			= "1.2.0.v20071231";
//	public static final String	APP_BUILD_ID			= "1.1.0.v20071107";

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
				: qualifier.substring(0, 8) + UI.DASH + qualifier.substring(8);

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
