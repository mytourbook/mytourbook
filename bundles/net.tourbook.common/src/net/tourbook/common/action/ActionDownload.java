/*******************************************************************************
 * Copyright (C) 2023, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.common.action;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.ui.SubMenu;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;

public abstract class ActionDownload extends SubMenu {

   /**
    * Common action to download something
    *
    * @param downloadTooltip
    */
   public ActionDownload(final String downloadTooltip) {

      super(UI.EMPTY_STRING, AS_PUSH_BUTTON);

      setToolTipText(downloadTooltip);

      setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Download));
   }

   public ToolBar createUI(final Composite parent) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(this);
      tbm.update(true);

      return toolbar;
   }

}
