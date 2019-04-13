/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.ProgressBar;
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

   private String               pluginId;
   private String               splashPath = "splash.bmp"; //$NON-NLS-1$
   private String               nextMessage;

   private Rectangle            textRect;
   private Rectangle            progressRect;

   private int                  totalWork;
   private int                  progress;

   /*
    * UI controls
    */
   private Shell       splashShell;
   private ProgressBar progressBar;
   private Label       textLabel;
   private Color       textColor;
   private Font        textFont;

   private SplashManager() {

//      String progressRectString = null;
//      final String messageRectString = null;
//
//      final IProduct product = Platform.getProduct();
//      if (product != null) {
//
//         progressRectString = product.getProperty(IProductConstants.STARTUP_PROGRESS_RECT);
//         messageRectString = product.getProperty(IProductConstants.STARTUP_MESSAGE_RECT);
//         foregroundColorString = product.getProperty(IProductConstants.STARTUP_FOREGROUND_COLOR);
//      }
//
//      // set progressbar position
//      Rectangle progressRect = parseRect(progressRectString);
//      if (progressRect == null) {
//         progressRect = new Rectangle(10, 0, 300, 15);
//      }
//      setProgressBarBounds(progressRect);

      // set message position
//      Rectangle messageRect = parseRect(messageRectString);
//      if (messageRect == null) {
////         messageRect = new Rectangle(10, 25, 300, 15);
      final Rectangle messageRect = new Rectangle(5, 120, 390, 20);
//      }
      setTextBounds(messageRect);

   }

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

      splashShell.close();
      splashShell = null;
      textLabel = null;
      textRect = null;
      textColor = null;
      textFont = null;
      progressBar = null;
      progressRect = null;
   }

   private Image createBackgroundImage(final Shell parent) {

      final Image splashImage = getImageDescriptor(pluginId, splashPath).createImage();

      parent.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            splashImage.dispose();
         }
      });

      return splashImage;
   }

   private ProgressBar createProgressBar(final Shell shell) {

      int style = SWT.BORDER | SWT.SMOOTH | SWT.HORIZONTAL;

      if (totalWork < 0) {
         // FIXME This does not work ... could be an issue with the UI thread
         // being busy in the background, so the progress bar has no chance
         // to update in indeterminate mode
         style |= SWT.INDETERMINATE;
      }

      final ProgressBar pb = new ProgressBar(shell, style);
      if (totalWork > 0) {
         pb.setMinimum(0);
         pb.setMaximum(totalWork);
      }

      return pb;
   }

   private Label createTextLabel(final Composite parent) {

      final Label label = new Label(parent, SWT.WRAP);
      /*
       * GridData gd = new GridData(); gd.horizontalAlignment = SWT.FILL; gd.verticalAlignment =
       * SWT.BOTTOM; gd.grabExcessHorizontalSpace = true; gd.grabExcessVerticalSpace = true;
       * label.setLayoutData(gd);
       */
      if (textColor == null) {
         textColor = parent.getDisplay().getSystemColor(SWT.COLOR_WHITE);
      }
      label.setForeground(textColor);
      if (textFont == null) {
         textFont = parent.getDisplay().getSystemFont();
      }
      label.setFont(textFont);

      if (nextMessage != null) {
         label.setText(nextMessage);
      }
      return label;
   }

   private Shell createUI_SplashShell() {

      final Shell shell = new Shell(SWT.TOOL | SWT.NO_TRIM);
      final Image image = createBackgroundImage(shell);

      shell.setBackgroundImage(image);
      shell.setBackgroundMode(SWT.INHERIT_DEFAULT);

      final Rectangle imageBounds = image.getBounds();

      textLabel = createTextLabel(shell);
      if (textRect == null) {
         textRect = new Rectangle(20,
               imageBounds.height - 60,
               imageBounds.width - 40,
               40);
      }
      textLabel.setBounds(textRect);

      if (totalWork != 0) {
         progressBar = createProgressBar(shell);
         if (progressRect == null) {
            progressRect = new Rectangle(0,
                  imageBounds.height - 14
                        - progressBar.getBorderWidth(),
                  imageBounds.width
                        - progressBar.getBorderWidth(),
                  14);
         }
         progressBar.setBounds(progressRect);
      }

      shell.addDisposeListener(new DisposeListener() {

         @Override
         public void widgetDisposed(final DisposeEvent e) {
            onDispose();
         }
      });

      shell.addPaintListener(new PaintListener() {

         @Override
         public void paintControl(final PaintEvent e) {
            onPaint(e);
         }
      });

      shell.setSize(imageBounds.width, imageBounds.height);
      shell.setLocation(getMonitorCenter(shell));

      return shell;
   }

   private void onDispose() {

   }

   private void onPaint(final PaintEvent e) {

      final GC gc = e.gc;
      gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));

      final int borderRight = 385;
      final int borderBottom = 101;

      final String copyRight = NLS.bind(Messages.App_Splash_Copyright, ApplicationVersion.SPLASH_COPYRIGHT_YEAR);
      final int textHeight = gc.textExtent(copyRight).y;

      final String version = "Version " + ApplicationVersion.getVersionSimple(); //$NON-NLS-1$
      final Point versionExtent = gc.textExtent(version);

      final String qualifier = ApplicationVersion.getVersionQualifier();
      final Point qualifierExtent = gc.textExtent(qualifier);

      final String dataLocation = Platform.getInstanceLocation().getURL().getPath();
      final Point dataLocationExtent = gc.textExtent(dataLocation);

      gc.drawText(version, //
            borderRight - versionExtent.x,
            borderBottom - versionExtent.y - 2 - qualifierExtent.y,
            true);

      // show location when data location is in debug mode
      if (dataLocation.contains("DEBUG")) { //$NON-NLS-1$

         gc.drawText(dataLocation, //
               borderRight - dataLocationExtent.x,
               borderBottom - versionExtent.y,
               true);

      } else {

         gc.drawText(qualifier, //
               borderRight - qualifierExtent.x,
               borderBottom - versionExtent.y,
               true);
      }

      gc.drawText(copyRight, 5, 162 - textHeight, true);
   }

   public void open() {

      if (pluginId == null) {
         throw new IllegalStateException(
               "The SplashPluginId has not been set."); //$NON-NLS-1$
      }

      if (splashPath == null) {
         throw new IllegalStateException(
               "The SplashImagePath has not been set."); //$NON-NLS-1$
      }

      splashShell = createUI_SplashShell();
      splashShell.open();
   }

   public void setMessage(final String message) {

      // log message if the splash screen do not show it
      System.out.println(UI.timeStamp() + " Splash message: " + message);//$NON-NLS-1$

      if (textLabel != null && !textLabel.isDisposed()) {

         splashShell.getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {

               textLabel.setText(message);

               splashShell.update();
            }
         });
      } else {
         nextMessage = message;
      }
   }

   public void setProgressBarBounds(final Rectangle rect) {

      Assert.isLegal(rect != null);
      this.progressRect = rect;
      if (progressBar != null) {
         progressBar.setBounds(rect);
      }
   }

   public void setSplashImagePath(final String splashPath) {

      Assert.isLegal(splashPath != null && !splashPath.equals(UI.EMPTY_STRING));
      this.splashPath = splashPath;
   }

   public void setSplashPluginId(final String pluginId) {

      Assert.isLegal(pluginId != null && !pluginId.equals(UI.EMPTY_STRING));
      this.pluginId = pluginId;
   }

   public void setTextBounds(final Rectangle rect) {

      Assert.isLegal(rect != null);

      this.textRect = rect;
      if (textLabel != null) {
         textLabel.setBounds(rect);
      }
   }

   public void setTextColor(final Color color) {

      Assert.isLegal(color != null);

      this.textColor = color;
      if (textLabel != null) {
         textLabel.setForeground(color);
      }
   }

   public void setTextFont(final Font font) {

      Assert.isLegal(font != null);

      this.textFont = font;
      if (textLabel != null) {
         textLabel.setFont(font);
      }
   }

   public void setTotalWork(final int totalWork) {

      this.totalWork = totalWork;

      if (progressBar != null) {
         progressBar.setMaximum(totalWork);
      }
   }

   public void worked(final int worked) {

      if (progressBar != null && !progressBar.isDisposed()) {

         progress += worked;

         splashShell.getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
               progressBar.setSelection(progress);
               splashShell.update();
            }
         });
      }
   }

}
