/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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

import java.util.Map;

import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.menus.UIElement;

public class ActionHandler_OpenDialog_Preferences extends AbstractHandler implements IElementUpdater {

   @Override
   public Object execute(final ExecutionEvent event) throws ExecutionException {

      // open pref dialog
      PreferencesUtil.createPreferenceDialogOn(Display.getCurrent().getActiveShell(), null, null, null).open();

      return null;
   }

   @SuppressWarnings("rawtypes")
   @Override
   public void updateElement(final UIElement uiElement, final Map parameters) {

      UI.setThemedIcon(uiElement, CommonImages.App_Options);
   }
}
