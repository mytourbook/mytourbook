/*******************************************************************************
 * Copyright (C) 2021, 2025 Wolfgang Schramm and Contributors
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.menus.UIElement;

public class ActionHandler_OpenDialog_Preferences extends AbstractHandler implements IElementUpdater {

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   @Override
   public Object execute(final ExecutionEvent event) throws ExecutionException {

      final String lastPrefID = _prefStore.getString(ITourbookPreferences.APP_LAST_SELECTED_PREFERENCE_PAGE_ID);
      final String preferencePageId = lastPrefID.length() == 0 ? null : lastPrefID;

      // open pref dialog
      final PreferenceDialog preferenceDialog = PreferencesUtil.createPreferenceDialogOn(

            Display.getCurrent().getActiveShell(),
            preferencePageId,
            null,
            null);

      preferenceDialog.open();

      try {

         // getSelectedNodePreference() is protected, hack it :-)
         final Method getMethod = PreferenceDialog.class.getDeclaredMethod("getSelectedNodePreference");//$NON-NLS-1$

         getMethod.setAccessible(true);
         final Object retValue = getMethod.invoke(preferenceDialog);

         if (retValue instanceof final String selectedPredId) {

            _prefStore.setValue(ITourbookPreferences.APP_LAST_SELECTED_PREFERENCE_PAGE_ID, selectedPredId);
         }

      } catch (final NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
         StatusUtil.log(e);
      }

      return null;
   }

   @SuppressWarnings("rawtypes")
   @Override
   public void updateElement(final UIElement uiElement, final Map parameters) {

      UI.setThemedIcon(uiElement, CommonImages.App_Options);
   }
}
