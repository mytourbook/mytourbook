/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Event;

class ActionToggleViewLayout extends Action {

   private TourBookView _tourBookView;

   /**
    * @param tourBookView
    */
   public ActionToggleViewLayout(final TourBookView tourBookView) {

      super(null, AS_PUSH_BUTTON);

      setToolTipText(Messages.Tour_Book_Action_ToggleViewLayout_Tooltip);
      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.TourBook_Month));

      _tourBookView = tourBookView;
   }

   @Override
   public void runWithEvent(final Event event) {
      _tourBookView.actionToggleViewLayout(event);
   }
}
