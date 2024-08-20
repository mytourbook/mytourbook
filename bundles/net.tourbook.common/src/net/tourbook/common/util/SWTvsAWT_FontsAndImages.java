package net.tourbook.common.util;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class SWTvsAWT_FontsAndImages {

   private static String               AUTO_SCALE;

   private static int                  IMAGE_WIDTH;
   private static int                  IMAGE_HEIGHT;

   private static final String         FONT_NAME            = "Segoe UI";
   private static int[]                FONT_SIZE;

   private static final int            MARGIN               = 2;

   private static final int            SHELL_X              = 1500;
   private static final int            SHELL_Y              = 100;

   private static final int            FOREGROUND_COLOR_SWT = SWT.COLOR_BLACK;
   private static final int            BACKGROUND_COLOR_SWT = SWT.COLOR_WHITE;

   private static final java.awt.Color FOREGROUND_COLOR_AWT = java.awt.Color.black;
   private static final java.awt.Color BACKGROUND_COLOR_AWT = java.awt.Color.white;

   private static Display              _swtDisplay;
   private static Font[]               _swtFont;
   private static int[]                _swtFontHeight;

   private static float                _deviceScaling;
   private static int                  _scaledWidth;
   private static int                  _scaledHeight;

   static {

      AUTO_SCALE = "quarter";
      AUTO_SCALE = "false";

      AUTO_SCALE = "150";
      AUTO_SCALE = "200";
      AUTO_SCALE = "100";

      FONT_SIZE = new int[] { //

            10, //
            12, //
            16, //
            20, //
            24, //
//          28, //
            36 //
      };

      System.setProperty("swt.autoScale", AUTO_SCALE);
      System.setProperty("swt.autoScale.updateOnRuntime", "true");

      _swtDisplay = new Display();

      final int numFonts = FONT_SIZE.length;
      int allFontHeights = 0;

      _swtFont = new Font[numFonts];
      _swtFontHeight = new int[numFonts];

      for (int fontIndex = 0; fontIndex < numFonts; fontIndex++) {

         final Font swtFont = new Font(_swtDisplay, FONT_NAME, FONT_SIZE[fontIndex], SWT.NORMAL);
         final FontData swtFontData = swtFont.getFontData()[0];

         final int fontHeight = swtFontData.getHeight();

         _swtFont[fontIndex] = swtFont;
         _swtFontHeight[fontIndex] = fontHeight;

         allFontHeights += fontHeight;
      }

      IMAGE_WIDTH = 1200;
      IMAGE_HEIGHT = (int) (allFontHeights * 1.9);

      _deviceScaling = DPIUtil.getDeviceZoom() / 100f;

      _scaledWidth = (int) (IMAGE_WIDTH * _deviceScaling);
      _scaledHeight = (int) (IMAGE_HEIGHT * _deviceScaling);
   }

   private static class ScaledImageDataProvider implements ImageDataProvider {

      private ImageData _imageData;

      public ScaledImageDataProvider(final ImageData imageData) {

         _imageData = imageData;
      }

      @Override
      public ImageData getImageData(final int zoom) {

         return _imageData;
      }
   }

   private static ImageData convertAWTtoSWT(final BufferedImage awtImage) {

      final int imageWidth = awtImage.getWidth();
      final int imageHeight = awtImage.getHeight();

      final ImageData swtImageData = new ImageData(imageWidth,
            imageHeight,
            24,
            new PaletteData(0xFF0000, 0xFF00, 0xFF));

      final int scansize = (((imageWidth * 3) + 3) * 4) / 4;

      final WritableRaster alphaRaster = awtImage.getAlphaRaster();
      final byte[] alphaBytes = new byte[imageWidth];

      for (int y = 0; y < imageHeight; y++) {

         final int[] buff = awtImage.getRGB(0, y, imageWidth, 1, null, 0, scansize);

         swtImageData.setPixels(0, y, imageWidth, buff, 0);

         if (alphaRaster != null) {

            final int[] alpha = alphaRaster.getPixels(0, y, imageWidth, 1, (int[]) null);

            for (int i = 0; i < imageWidth; i++) {
               alphaBytes[i] = (byte) alpha[i];
            }

            swtImageData.setAlphas(0, y, imageWidth, alphaBytes, 0);
         }
      }

      return swtImageData;
   }

   private static BufferedImage createAWTImage() {

      final BufferedImage awtImage = new BufferedImage(_scaledWidth, _scaledHeight, BufferedImage.TYPE_4BYTE_ABGR);

      final Graphics2D g2d = awtImage.createGraphics();
      try {

         g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

         g2d.setBackground(BACKGROUND_COLOR_AWT);
         g2d.clearRect(0, 0, _scaledWidth, _scaledHeight);

         int devY = 0;

         for (int fontIndex = 0; fontIndex < FONT_SIZE.length; fontIndex++) {

            final int scaledFontSize = (int) (FONT_SIZE[fontIndex] * _deviceScaling);

            final java.awt.Font font = new java.awt.Font(FONT_NAME, java.awt.Font.PLAIN, scaledFontSize);
            g2d.setFont(font);

            final FontMetrics fontMetrics = g2d.getFontMetrics();
            final int textHeight = fontMetrics.getHeight();
            devY += textHeight;

            g2d.setColor(FOREGROUND_COLOR_AWT);
            g2d.drawString(" AWT" + getTestText(fontIndex), MARGIN, devY);
         }

      } finally {

         g2d.dispose();
      }

      return awtImage;
   }

   private static Image createSWTImage() {

      final Image swtImage = new Image(_swtDisplay, IMAGE_WIDTH, IMAGE_HEIGHT);

      final GC gc = new GC(swtImage);
      {
         gc.setAntialias(SWT.ON);

         gc.setBackground(_swtDisplay.getSystemColor(BACKGROUND_COLOR_SWT));
         gc.fillRectangle(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

         gc.setForeground(_swtDisplay.getSystemColor(FOREGROUND_COLOR_SWT));

         int devY = 0;

         for (int fontIndex = 0; fontIndex < FONT_SIZE.length; fontIndex++) {

            gc.setFont(_swtFont[fontIndex]);
            gc.drawString(" SWT" + getTestText(fontIndex), MARGIN, devY, true);

            devY += _swtFontHeight[fontIndex] + 10;
         }
      }
      gc.dispose();

      return swtImage;
   }

   private static String getTestText(final int fontIndex) {

      return "  autoScale=%s  FONT SIZE=%d".formatted(AUTO_SCALE, FONT_SIZE[fontIndex]);
   }

   public static void main(final String[] args) {

      final Image swtImage = createSWTImage();
      final BufferedImage awtImage = createAWTImage();

      final ImageData swtImageDataFromAwt = convertAWTtoSWT(awtImage);
      final ScaledImageDataProvider imageDataProvider = new ScaledImageDataProvider(swtImageDataFromAwt);

      final Image swtImageFromAwt = new Image(_swtDisplay, imageDataProvider);

      final Shell shell = new Shell(_swtDisplay);
      shell.setText("SWT vs AWT scaling");
      shell.setLocation(SHELL_X, SHELL_Y);

      shell.setSize(
            IMAGE_WIDTH + 20,
            IMAGE_HEIGHT * 2 + 20);

      shell.addListener(SWT.Paint, event -> {

         int devY = MARGIN;

         final GC gc = event.gc;

         gc.drawImage(swtImage, MARGIN, devY);
         devY += IMAGE_HEIGHT + MARGIN;

         gc.drawImage(swtImageFromAwt, MARGIN, devY);
      });

      shell.open();

      while (!shell.isDisposed()) {
         if (!_swtDisplay.readAndDispatch()) {
            _swtDisplay.sleep();
         }
      }

      swtImage.dispose();
      swtImageFromAwt.dispose();

      for (final Font font : _swtFont) {
         font.dispose();
      }

      _swtDisplay.dispose();
   }
}
