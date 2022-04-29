/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package net.tourbook.ui.views.tourSegmenter;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionExportViewCSV extends Action {

   private TourSegmenterView _tourSegmenterView;

   public ActionExportViewCSV(final TourSegmenterView tourSegmenterView) {

      super();

      _tourSegmenterView = tourSegmenterView;

      setText(Messages.App_Action_ExportViewCSV);
      setToolTipText(Messages.Tour_Segmenter_Action_ExportViewCSV_Tooltip);

      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.CSVFormat));
      setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.CSVFormat_Disabled));
   }

   @Override
   public void run() {
      _tourSegmenterView.actionExportViewCSV();
   }
}
