/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import net.tourbook.application.ActionTourFilter;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class TourFilterManager {

	private static final String					TAG_PROFILE					= "Profile";								//$NON-NLS-1$
	private static final String					TAG_ROOT					= "TourFilterProfiles";						//$NON-NLS-1$

	private static final String					ATTR_IS_SELECTED			= "isSelected";								//$NON-NLS-1$
	private static final String					ATTR_NAME					= "name";									//$NON-NLS-1$
	private static final String					ATTR_TOUR_FILTER_VERSION	= "TourFilterVersion";						//$NON-NLS-1$

	private static final int					TOUR_FILTER_VERSION			= 1;
	private static final String					TOUR_FILTER_FILE_NAME		= "tour-filter.xml";						//$NON-NLS-1$

// SET_FORMATTING_OFF
	
	/**
	 * _bundle must be set here otherwise an exception occures in saveState()
	 */
	private static final Bundle					_bundle						= TourbookPlugin.getDefault().getBundle();
	private static final IDialogSettings		_state						= TourbookPlugin.getState("net.tourbook.tour.filter.TourFilterManager");
	private static final IPath					_stateLocation				= Platform.getStateLocation(_bundle);

	private final static IPreferenceStore		_prefStore					= TourbookPlugin.getPrefStore();
	
// SET_FORMATTING_ON

	private static ActionTourFilter				_actionTourFilter;

	private static ArrayList<TourFilterProfile>	_filterProfiles				= new ArrayList<>();
	private static TourFilterProfile			_selectedProfile;

	/**
	 * Fire event that the tour filter has changed.
	 */
	private static void fireTourFilterModifyEvent() {

		_prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
	}

	public static ArrayList<TourFilterProfile> getProfiles() {
		return _filterProfiles;
	}

	public static TourFilterProfile getSelectedProfile() {
		return _selectedProfile;
	}

	private static File getXmlFile() {

		final File layerFile = _stateLocation.append(TOUR_FILTER_FILE_NAME).toFile();

		return layerFile;
	}

	/**
	 * Read filter profile xml file.
	 * 
	 * @return
	 */
	private static void readFilterProfiles() {

		final File xmlFile = getXmlFile();

		if (xmlFile.exists()) {

			try (InputStreamReader reader = new InputStreamReader(new FileInputStream(xmlFile), UI.UTF_8)) {

				final XMLMemento xmlRoot = XMLMemento.createReadRoot(reader);
				for (final IMemento mementoChild : xmlRoot.getChildren()) {

					final XMLMemento xmlProfile = (XMLMemento) mementoChild;
					if (TAG_PROFILE.equals(xmlProfile.getType())) {

						final TourFilterProfile tourFilterProfile = new TourFilterProfile();

						tourFilterProfile.name = Util.getXmlString(xmlProfile, ATTR_NAME, UI.EMPTY_STRING);

						_filterProfiles.add(tourFilterProfile);

						// set selected profile
						if (Util.getXmlBoolean(xmlProfile, ATTR_IS_SELECTED, false)) {
							_selectedProfile = tourFilterProfile;
						}
					}
				}

			} catch (final Exception e) {
				StatusUtil.log(e);
			}
		}
	}

	public static void restoreState() {

		final boolean isTourFilterActive = _prefStore.getBoolean(ITourbookPreferences.APP_TOUR_FILTER_IS_SELECTED);

		_actionTourFilter.setSelection(isTourFilterActive);

		readFilterProfiles();
	}

	public static void saveState() {

		_prefStore.setValue(ITourbookPreferences.APP_TOUR_FILTER_IS_SELECTED, _actionTourFilter.getSelection());

		final XMLMemento xmlRoot = writeFilterProfile();
		final File xmlFile = getXmlFile();

		Util.writeXml(xmlRoot, xmlFile);
	}

	public static void setSelectedProfile(final TourFilterProfile selectedProfile) {
		_selectedProfile = selectedProfile;
	}

	/**
	 * Sets the state if the tour filter is active or not.
	 * 
	 * @param isSelected
	 */
	public static void setSelection(final boolean isSelected) {

		fireTourFilterModifyEvent();

	}

	public static void setTourFilterAction(final ActionTourFilter actionTourFilter) {
		_actionTourFilter = actionTourFilter;
	}

	/**
	 * @return
	 */
	private static XMLMemento writeFilterProfile() {

		XMLMemento xmlRoot = null;

		try {

			xmlRoot = writeFilterProfile_100_Root();

			for (final TourFilterProfile tourFilterProfile : _filterProfiles) {

				final IMemento xmlProfile = xmlRoot.createChild(TAG_PROFILE);

				xmlProfile.putString(ATTR_NAME, tourFilterProfile.name);

				// set flag for active profile
				if (tourFilterProfile == _selectedProfile) {
					xmlProfile.putBoolean(ATTR_IS_SELECTED, true);
				}

			}

		} catch (final Exception e) {
			StatusUtil.log(e);
		}

		return xmlRoot;
	}

	private static XMLMemento writeFilterProfile_100_Root() {

		final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

		// date/time
		xmlRoot.putString(Util.ATTR_ROOT_DATETIME, TimeTools.now().toString());

		// plugin version
		final Version version = _bundle.getVersion();
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MAJOR, version.getMajor());
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MINOR, version.getMinor());
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MICRO, version.getMicro());
		xmlRoot.putString(Util.ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

		// layer structure version
		xmlRoot.putInteger(ATTR_TOUR_FILTER_VERSION, TOUR_FILTER_VERSION);

		return xmlRoot;
	}

}
