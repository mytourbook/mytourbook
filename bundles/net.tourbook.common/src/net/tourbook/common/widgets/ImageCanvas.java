/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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

import static org.eclipse.swt.events.FocusListener.focusGainedAdapter;
import static org.eclipse.swt.events.FocusListener.focusLostAdapter;
import static org.eclipse.swt.events.KeyListener.keyPressedAdapter;
import static org.eclipse.swt.events.MouseListener.mouseDownAdapter;
import static org.eclipse.swt.events.MouseTrackListener.mouseEnterAdapter;
import static org.eclipse.swt.events.MouseTrackListener.mouseExitAdapter;

import net.tourbook.common.UI;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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

   /**
    * @param parent
    * @param style
    */
   public ImageCanvas(final Composite parent, final int style) {

      super(parent, style);

      addPaintListener(this);

      addTraverseListener(traverseEvent -> {

         switch (traverseEvent.detail) {

         case SWT.TRAVERSE_TAB_NEXT:
         case SWT.TRAVERSE_TAB_PREVIOUS:

         case SWT.TRAVERSE_ESCAPE: // esc allows to close a dialog

            traverseEvent.doit = true; // enable traversal
            break;
         }
      });

      addKeyListener(keyPressedAdapter(keyEvent -> {

         // key listener enables traversal out

         // fire selection
         if (keyEvent.keyCode == ' ' || keyEvent.keyCode == SWT.CR) {
            fireSelection();
         }
      }));

      final Cursor cursor = getDisplay().getSystemCursor(SWT.CURSOR_IBEAM);

      addMouseTrackListener(mouseEnterAdapter(mouseEvent -> setCursor(cursor)));
      addMouseTrackListener(mouseExitAdapter(mouseEvent -> setCursor(null)));

      addFocusListener(focusGainedAdapter(focusEvent -> {
         _isFocusGained = true;

         redraw();
      }));
      addFocusListener(focusLostAdapter(focusEvent -> {
         _isFocusGained = false;

         redraw();
      }));

      addMouseListener(mouseDownAdapter(mouseEvent -> {

         setFocus();
         fireSelection();
      }));
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

   @Override
   public void paintControl(final PaintEvent e) {

      final GC gc = e.gc;

      /*
       * In focus control and focus gained can be different, for win7 it depends if a dialog was
       * opened with the mouse or keyboard !!!
       */
      final boolean isFocus = isFocusControl() || _isFocusGained;

      if (_image == null || _image.isDisposed()) {

         final Rectangle rect = getClientArea();
         final int devX = rect.x;
         final int devY = rect.y;
         final int width = rect.width;
         final int height = rect.height;

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

      final Point bestSize = UI.getBestFitCanvasSize(//
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

      // draw image
      gc.drawImage(_image, //
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

         gc.drawFocus(//
               offsetX,
               offsetY,
               bestSizeWidth,
               bestSizeHeight);
      }
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
