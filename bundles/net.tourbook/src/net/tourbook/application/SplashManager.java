package net.tourbook.application;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
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
   private String               splashPath = "splash.bmp";
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

   public static ImageDescriptor getImageDescriptor(final String pluginId, String path) {

      try {

         if (!path.startsWith("/")) {
            path = "/" + path;
         }

         URL url = new URL("platform:/plugin/" + pluginId + path);
         url = FileLocator.resolve(url);

         return ImageDescriptor.createFromURL(url);

      } catch (final MalformedURLException e) {

         final String msg = NLS
               .bind("The image path {0} in not a valid location in the bundle {1}.",
                     path,
                     pluginId);
         throw new RuntimeException(msg, e);

      } catch (final IOException e) {

         final String msg = NLS.bind(
               "The image {0} was not found in the bundle {1}.",
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

   private Shell createSplashShell() {

      final Shell shell = new Shell(SWT.TOOL | SWT.NO_TRIM);
      final Image image = createBackgroundImage(shell);
      shell.setBackgroundImage(image);
      shell.setBackgroundMode(SWT.INHERIT_DEFAULT);
      final Rectangle imageBounds = image.getBounds();

      /*
       * final GridLayout layout = new GridLayout(); layout.numColumns = 1; layout.marginHeight =
       * 40; layout.marginWidth = 20; layout.verticalSpacing = 6; layout.horizontalSpacing = 6;
       * shell.setLayout(layout);
       */
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

      shell.setSize(imageBounds.width, imageBounds.height);
      shell.setLocation(getMonitorCenter(shell));

      return shell;
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

   public void open() {

      if (pluginId == null) {
         throw new IllegalStateException(
               "The SplashPluginId has not been set.");
      }

      if (splashPath == null) {
         throw new IllegalStateException(
               "The SplashImagePath has not been set.");
      }

      splashShell = createSplashShell();
      splashShell.open();
   }

   private Rectangle parseRect(final String string) {
      if (string == null) {
         return null;
      }
      int x, y, w, h;
      int lastPos = 0;
      try {
         int i = string.indexOf(',', lastPos);
         x = Integer.parseInt(string.substring(lastPos, i));
         lastPos = i + 1;
         i = string.indexOf(',', lastPos);
         y = Integer.parseInt(string.substring(lastPos, i));
         lastPos = i + 1;
         i = string.indexOf(',', lastPos);
         w = Integer.parseInt(string.substring(lastPos, i));
         lastPos = i + 1;
         h = Integer.parseInt(string.substring(lastPos));
      } catch (final RuntimeException e) {
         // sloppy error handling
         return null;
      }
      return new Rectangle(x, y, w, h);
   }

   public void setMessage(final String message) {

      if (textLabel != null && !textLabel.isDisposed()) {

         splashShell.getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {

               textLabel.setText(message);

               // splashShell.layout();
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

      Assert.isLegal(splashPath != null && !splashPath.equals(""));
      this.splashPath = splashPath;
   }

   public void setSplashPluginId(final String pluginId) {

      Assert.isLegal(pluginId != null && !pluginId.equals(""));
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
