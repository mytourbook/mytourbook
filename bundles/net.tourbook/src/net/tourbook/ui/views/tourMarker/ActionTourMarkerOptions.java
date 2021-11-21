/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
package net.tourbook.ui.views.tourMarker;

import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.part.PageBook;

public class ActionTourMarkerOptions extends ActionToolbarSlideout {

   private Control _ownerControl;

   public ActionTourMarkerOptions(final PageBook pageBook) {
      _ownerControl = pageBook;
   }

   @Override
   protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

      final SlideoutTourMarkerOptions slideoutTourMarkerOptions = new SlideoutTourMarkerOptions(
            _ownerControl,
            toolbar);

      return slideoutTourMarkerOptions;
   }
}
