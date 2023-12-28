/*******************************************************************************
 * Copyright (C) 2005, 2024 Wolfgang Schramm and Contributors
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import net.tourbook.Messages;
import net.tourbook.common.UI;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

/**
 * Original implementation from de.emsw.ui.splash.internal.SplashServiceImpl.java
 * <p>
 * It worked only once when the configuration was deleted ahead, any further app starts did not call
 * the @PostContextCreate method when the parameter <b>ISplashService</b> was set.
 * <p>
 * It took many hours to analyze it and skip this workflow and use this manager instead of a osgi
 * service.
 */
public class SplashManager {

   private static SplashManager _instance;

   private String               _splashFile = "splash.png"; //$NON-NLS-1$

   private Rectangle            _splashImageBounds;

   /*
    * UI controls
    */
   private Shell _splashShell;
   private Label _lblProgressMessage;

   private SplashManager() {}

   public static ImageDescriptor getImageDescriptor(final String pluginId, String path) {

      try {

         if (!path.startsWith("/")) { //$NON-NLS-1$
            path = "/" + path; //$NON-NLS-1$
         }

         URL url = new URL("platform:/plugin/" + pluginId + path); //$NON-NLS-1$
         url = FileLocator.resolve(url);

         return ImageDescriptor.createFromURL(url);

      } catch (final MalformedURLException e) {

         final String msg = NLS
               .bind("The image path {0} in not a valid location in the bundle {1}.", //$NON-NLS-1$
                     path,
                     pluginId);
         throw new RuntimeException(msg, e);

      } catch (final IOException e) {

         final String msg = NLS.bind(
               "The image {0} was not found in the bundle {1}.", //$NON-NLS-1$
               path,
               pluginId);
         throw new RuntimeException(msg, e);
      }
   }

   public static SplashManager getInstance() {

      if (_instance == null) {
         _instance = new SplashManager();
      }

      return _instance;
   }

   public static Point getMonitorCenter(final Shell shell) {

      final Monitor primary = shell.getDisplay().getPrimaryMonitor();

      final Rectangle bounds = primary.getBounds();
      final Rectangle rect = shell.getBounds();

      final int x = bounds.x + (bounds.width - rect.width) / 2;
      final int y = bounds.y + (bounds.height - rect.height) / 2;

      return new Point(x, y);
   }

   public void close() {

      if (_splashShell.isDisposed() == false) {
         _splashShell.close();
      }

      _splashShell = null;
   }

   private Image createBackgroundImage(final Shell parent) {

      final Image splashImage = getImageDescriptor(TourbookPlugin.PLUGIN_ID, _splashFile).createImage();

      parent.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            splashImage.dispose();
         }
      });

      return splashImage;
   }

   private Shell createUI_SplashShell() {

      final Shell shell = new Shell(SWT.TOOL | SWT.NO_TRIM);

      final Image image = createBackgroundImage(shell);
      _splashImageBounds = image.getBounds();

      shell.setSize(_splashImageBounds.width, _splashImageBounds.height);
      shell.setLocation(getMonitorCenter(shell));

      shell.setBackgroundImage(image);
      shell.setBackgroundMode(SWT.INHERIT_DEFAULT);

      shell.addPaintListener(paintEvent -> onPaint(paintEvent));

      _lblProgressMessage = new Label(shell, SWT.WRAP);
      _lblProgressMessage.setForeground(UI.SYS_COLOR_WHITE);
      _lblProgressMessage.setText(UI.EMPTY_STRING);

      return shell;
   }

   public Shell getShell() {

      return _splashShell;
   }

   private void onPaint(final PaintEvent paintEvent) {

      final GC gc = paintEvent.gc;

      final String copyRightText = NLS.bind(Messages.App_Splash_Copyright, ApplicationVersion.SPLASH_COPYRIGHT_YEAR);
      final int textHeight = gc.textExtent(copyRightText).y;

      final int splashWidth = _splashImageBounds.width;
      final int splashHeight = _splashImageBounds.height;

      final int borderSize = 6;
      final int borderRight = splashWidth - 11;

      final int bottomVersion = 100;
      final int bottomCopyright = splashHeight - borderSize;

      final String version = "Version " + ApplicationVersion.getVersionSimple(); //$NON-NLS-1$
      final Point versionExtent = gc.textExtent(version);

      final String qualifier = ApplicationVersion.getVersionQualifier();
      final Point qualifierExtent = gc.textExtent(qualifier);

      final String dataLocation = Platform.getInstanceLocation().getURL().getPath();
      final Point dataLocationExtent = gc.textExtent(dataLocation);

      gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));

      _lblProgressMessage.setBounds(new Rectangle(
            borderSize,
            (int) (splashHeight - 3.0f * textHeight),
            splashWidth - borderSize,
            textHeight));

      gc.drawText(version,
            borderRight - versionExtent.x,
            bottomVersion - versionExtent.y - 2 - qualifierExtent.y,
            true);

      // show location when data location is in debug mode
      if (dataLocation.contains("DEBUG")) { //$NON-NLS-1$

         gc.drawText(dataLocation,
               borderRight - dataLocationExtent.x,
               bottomVersion - versionExtent.y,
               true);

      } else {

         gc.drawText(qualifier,
               borderRight - qualifierExtent.x,
               bottomVersion - versionExtent.y,
               true);
      }

      gc.drawText(copyRightText,
            borderSize,
            bottomCopyright - textHeight,
            true);
   }

   public void open() {

      _splashShell = createUI_SplashShell();
      _splashShell.open();
   }

   public void setMessage(final String message) {

      // log message if the splash screen do not show it
      System.out.println(UI.timeStamp() + "[Splash message] " + message); //$NON-NLS-1$

      _splashShell.getDisplay().syncExec(() -> {

         if (_lblProgressMessage.isDisposed()) {
            return;
         }

         _lblProgressMessage.setText(message);

         _splashShell.update();
      });
   }

}
