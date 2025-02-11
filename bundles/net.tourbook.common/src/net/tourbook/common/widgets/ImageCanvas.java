/*******************************************************************************
 * Copyright (C) 2010, 2024 Wolfgang Schramm and Contributors
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
/**
 * @author Wolfgang Schramm
 * @author Alfred Barten
 */
package net.tourbook.common.widgets;

import net.tourbook.common.UI;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

/**
 * Simple canvas which draws an image. The image is disposed when the canvas is disposed.
 */
public class ImageCanvas extends Canvas implements PaintListener {

   private Image                                 _image;

   private boolean                               _isCentered;
   private boolean                               _isLead;

   private final ListenerList<SelectionListener> _selectionListener = new ListenerList<>();

   private boolean                               _isFocusGained;
   private boolean                               _isSmoothImages;

   private Rectangle                             _imageBounds;

   /**
    * @param parent
    * @param style
    */
   public ImageCanvas(final Composite parent, final int style) {

      super(parent, style);

      addPaintListener(this);

      addTraverseListener(new TraverseListener() {
         @Override
         public void keyTraversed(final TraverseEvent e) {

            switch (e.detail) {

            case SWT.TRAVERSE_TAB_NEXT:
            case SWT.TRAVERSE_TAB_PREVIOUS:

            case SWT.TRAVERSE_ESCAPE: // esc allows to close a dialog

               e.doit = true; // enable traversal
               break;
            }
         }
      });

      addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(final KeyEvent e) {

            // key listener enables traversal out

            // fire selection
            if (e.keyCode == ' ' || e.keyCode == SWT.CR) {
               fireSelection();
            }
         }
      });

      final Cursor cursor = getDisplay().getSystemCursor(SWT.CURSOR_IBEAM);

      addMouseTrackListener(new MouseTrackListener() {

         @Override
         public void mouseEnter(final MouseEvent e) {
            setCursor(cursor);
         }

         @Override
         public void mouseExit(final MouseEvent e) {
            setCursor(null);
         }

         @Override
         public void mouseHover(final MouseEvent e) {}
      });

      addFocusListener(new FocusAdapter() {
         @Override
         public void focusGained(final FocusEvent e) {

            _isFocusGained = true;

            redraw();
         }

         @Override
         public void focusLost(final FocusEvent e) {

            _isFocusGained = false;

            redraw();
         }
      });

      addMouseListener(new MouseAdapter() {
         @Override
         public void mouseDown(final MouseEvent e) {

            setFocus();

            fireSelection();
         }
      });
   }

   public void addSelectionListener(final SelectionListener listener) {
      _selectionListener.add(listener);
   }

   @Override
   public void dispose() {

      super.dispose();

      _image.dispose();
   }

   /**
    * Do custom painting when an image is not valid, e.g. when it is loading
    *
    * @param gc
    * @param clientArea
    *
    * @return Returns <code>true</code> when it is customized painted, <code>false</code> when it is
    *         not painted and the default painting should be performed
    */
   public boolean drawInvalidImage(final GC gc, final Rectangle clientArea) {

      return false;
   }

   /**
    * Fires an event when the image canvas was selected.
    */
   private void fireSelection() {

      for (final Object listener : _selectionListener.getListeners()) {

         final Event event = new Event();
         event.widget = this;

         ((SelectionListener) listener).widgetSelected(new SelectionEvent(event));
      }
   }

   public Image getImage() {
      return _image;
   }

   /**
    * @return Returns the position within the canvas and the size of the painted image
    */
   public Rectangle getImageBounds() {

      return _imageBounds;
   }

   @Override
   public void paintControl(final PaintEvent e) {

      final GC gc = e.gc;

      /*
       * In focus control and focus gained can be different, for win7 it depends if a dialog was
       * opened with the mouse or keyboard !!!
       */
      final boolean isFocus = isFocusControl() || _isFocusGained;

      if (_image == null || _image.isDisposed()) {

         final Rectangle clientArea = getClientArea();

         if (drawInvalidImage(gc, clientArea)) {
            return;
         }

         final int devX = clientArea.x;
         final int devY = clientArea.y;
         final int width = clientArea.width;
         final int height = clientArea.height;

         // draw image indicator
         gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
         gc.fillRoundRectangle(devX, devY, width, height, 0, 0);

         // draw focus
         if (isFocus) {
            gc.drawFocus(devX, devY, width, height);
         }

         return;
      }

      final Rectangle imageRect = _image.getBounds();
      final Rectangle canvasBounds = getBounds();

      final int imageWidth = imageRect.width;
      final int imageHeight = imageRect.height;

      final int canvasWidth = canvasBounds.width;
      final int canvasHeight = canvasBounds.height;

      final Point bestSize = UI.getBestFitCanvasSize(
            imageWidth,
            imageHeight,
            canvasWidth,
            canvasHeight);

      int bestSizeWidth = bestSize.x;
      int bestSizeHeight = bestSize.y;

      // ensure image is not enlarged
      if (bestSizeWidth > imageWidth || bestSizeHeight > imageHeight) {
         bestSizeWidth = imageWidth;
         bestSizeHeight = imageHeight;
      }

      final int offsetX = _isLead ? 0 : (canvasWidth - bestSizeWidth) / 2;
      final int offsetY = _isCentered ? (canvasHeight - bestSizeHeight) / 2 : 0;

      if (_isSmoothImages) {

         gc.setAntialias(SWT.ON);

         // Linux needs SWT.HIGH otherwise it is not interpolated
         gc.setInterpolation(SWT.HIGH);

      } else {

         gc.setAntialias(SWT.OFF);
         gc.setInterpolation(SWT.OFF);
      }

      // draw image
      gc.drawImage(_image,
            0,
            0,
            imageWidth,
            imageHeight,
            //
            offsetX,
            offsetY,
            bestSizeWidth,
            bestSizeHeight);

      // draw focus
      if (isFocus) {

         gc.drawFocus(
               offsetX,
               offsetY,
               bestSizeWidth,
               bestSizeHeight);
      }

      _imageBounds = new Rectangle(

            offsetX,
            offsetY,
            bestSizeWidth,
            bestSizeHeight);
   }

   public void removeSelectionListener(final SelectionListener listener) {
      _selectionListener.remove(listener);
   }

   /**
    * Sets a new image and draws it, the old image is disposed.
    *
    * @param image
    */
   public void setImage(final Image image) {

      if (_image != null && image != null && _image == image && _image.isDisposed() == false) {

         // the new image is the same as the old image, do nothing
         return;
      }

      // dispose old image
      if (_image != null) {
         _image.dispose();
      }

      // set new image
      _image = image;

      redraw();
   }

   /**
    * Sets a new image and draws it, the old image is disposed when <code>isDispose</code> is
    * <code>true</code>, otherwise it will not be disposed and the caller must dispose the image.
    *
    * @param image
    * @param isDispose
    */
   public void setImage(final Image image, final boolean isDispose) {

      if (_image != null && image != null && _image == image && _image.isDisposed() == false) {

         // the new image is the same as the old image, do nothing
         return;
      }

      // dispose old image
      if (isDispose && _image != null) {
         _image.dispose();
      }

      // set new image
      _image = image;

      redraw();
   }

   /**
    * @param isSmoothImages
    *           When <code>true</code> then smaller images are interpolated
    */
   public void setIsSmoothImages(final boolean isSmoothImages) {

      _isSmoothImages = isSmoothImages;
   }

   /**
    * !!! VERY IMPORTANT !!!
    * <p>
    * Setting SWT.CENTER or SWT.LEAD in the constructor will disable the traversal events.
    *
    * @param style
    *           {@link SWT#CENTER} Center vertical/horizontal.<br>
    *           {@link SWT#LEAD} Leading alignment
    */
   public void setStyle(final int style) {

      _isCentered = (style & SWT.CENTER) != 0;
      _isLead = (style & SWT.LEAD) != 0;

   }

}
