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
package net.tourbook.map2.action;

import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;
import net.tourbook.map2.view.PngTransfer;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class ActionExportMapViewClipboard extends Action {

   private Map2View _map2View;

   public ActionExportMapViewClipboard(final Map2View mapView) {

      super(Messages.map_action_export_map_view_clipboard, AS_PUSH_BUTTON);

      _map2View = mapView;
   }

   @Override
   public void run() {

      final Image mapViewImage = _map2View.getMapViewImage();

      final Clipboard cb = new Clipboard(Display.getCurrent());

      final PngTransfer imageTransfer = PngTransfer.getInstance();
      cb.setContents(new Object[] { mapViewImage.getImageData() },
            new Transfer[] { imageTransfer });

      mapViewImage.dispose();
   }

}
