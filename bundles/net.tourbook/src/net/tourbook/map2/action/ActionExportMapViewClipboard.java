/*******************************************************************************
 * Copyright (C) 2021, 2023 Frédéric Bard
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.tourbook.common.UI;
import net.tourbook.common.util.FileUtils;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;
import net.tourbook.map2.view.PngTransfer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;

public class ActionExportMapViewClipboard extends Action {

   private Map2View _map2View;

   public ActionExportMapViewClipboard(final Map2View mapView) {

      super(Messages.Map_Action_Export_Map_View_Clipboard, AS_PUSH_BUTTON);

      _map2View = mapView;
   }

   @Override
   public void run() {

      final Image mapViewImage = _map2View.getMapViewImage();
      final Clipboard clipboard = new Clipboard(Display.getCurrent());

      if (UI.IS_LINUX) {

         final PngTransfer imageTransfer = PngTransfer.getInstance();
         clipboard.setContents(new ImageData[] { mapViewImage.getImageData() },
               new Transfer[] { imageTransfer });
      } else {

         //Workaround for the issue on Windows where the map is missing but the legend
         //is displayed (GIMP and Inkscape). No issue when sing MS Paint

         try {
            final String absoluteFilePath = Files.createTempFile("map", ".png").toString(); //$NON-NLS-1$ //$NON-NLS-2$

            //We export the image to a file as a PNG image
            final ImageLoader loader = new ImageLoader();
            loader.data = new ImageData[] { mapViewImage.getImageData() };
            loader.save(absoluteFilePath, SWT.IMAGE_PNG);

            //We reload it to put it in the clipboard
            final Image image = new Image(Display.getCurrent(), absoluteFilePath);
            clipboard.setContents(new ImageData[] { image.getImageData() },
                  new Transfer[] { ImageTransfer.getInstance() });
            image.dispose();

            FileUtils.deleteIfExists(Paths.get(absoluteFilePath));
         } catch (final IOException e) {
            StatusUtil.log(e);
         }
      }

      mapViewImage.dispose();
      clipboard.dispose();

      final IStatusLineManager statusLineManager = UI.getStatusLineManager();
      if (statusLineManager == null) {
         return;
      }

      statusLineManager.setMessage(Messages.Map_Action_Export_Map_Clipboard_Copied_Info);

      Display.getCurrent().timerExec(3000, () -> statusLineManager.setMessage(null));
   }

}
