/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.map.bookmark;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;

public class ActionMapBookmarks extends ActionToolbarSlideout {

   private Composite     _ownerControl;
   private IMapBookmarks _mapBookmarks;

   /**
    * @param ownerControl
    * @param mapBookmarks
    * @param canAnimate
    */
   public ActionMapBookmarks(final Composite ownerControl,
                             final IMapBookmarks mapBookmarks) {

      super(TourbookPlugin.getThemedImageDescriptor(Images.MapBookmark),
            TourbookPlugin.getThemedImageDescriptor(Images.MapBookmark_Disabled));

      _ownerControl = ownerControl;
      _mapBookmarks = mapBookmarks;
   }

   @Override
   protected ToolbarSlideout createSlideout(final ToolBar toolbar) {
      return new SlideoutMapBookmarks(_ownerControl, toolbar, _mapBookmarks);
   }

   @Override
   protected void onBeforeOpenSlideout() {
      _mapBookmarks.closeOpenedDialogs(this);
   }
}
