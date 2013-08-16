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
package net.tourbook.common.preferences;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.GraphColorManager;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;

/**
 * Class used to initialize default preference values.
 */
public class CommonPreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {

		final IPreferenceStore commonPrefStore = CommonActivator.getPrefStore();

		/*
		 * graph color preferences
		 */
		for (final ColorDefinition graphDefinition : GraphColorManager.getInstance().getGraphColorDefinitions()) {

			final String graphPrefName = graphDefinition.getGraphPrefName();

			PreferenceConverter.setDefault(
					commonPrefStore,
					graphPrefName + GraphColorManager.PREF_COLOR_BRIGHT,
					graphDefinition.getDefaultGradientBright());

			PreferenceConverter.setDefault(
					commonPrefStore,
					graphPrefName + GraphColorManager.PREF_COLOR_DARK,
					graphDefinition.getDefaultGradientDark());

			PreferenceConverter.setDefault(
					commonPrefStore,
					graphPrefName + GraphColorManager.PREF_COLOR_LINE,
					graphDefinition.getDefaultLineColor());

		}

	}
}
