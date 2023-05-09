/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.chart.preferences;

import net.tourbook.chart.ChartActivator;
import net.tourbook.chart.MouseWheel2KeyTranslation;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class ChartPreferenceInitializer extends AbstractPreferenceInitializer {

   public ChartPreferenceInitializer() {}

   @Override
   public void initializeDefaultPreferences() {

      final IPreferenceStore store = ChartActivator.getPrefStore();

      // mouse wheel to key translation
      store.setDefault(IChartPreferences.GRAPH_MOUSE_KEY_TRANSLATION, MouseWheel2KeyTranslation.Up_Left.name());
   }

}
