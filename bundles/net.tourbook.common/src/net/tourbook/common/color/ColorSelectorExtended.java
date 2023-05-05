/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.common.color;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

/**
 * Extends the {@link ColorSelector} with an open & close listener, which is fired when the dialog
 * is opened or closes.
 * <p>
 * This can be used to keep parent dialog opened when the color selector dialog is opened.
 * <p>
 * Extended 27. May 2021 to also set/get the custom colors and to save/restore it. This code was
 * completely copied from the whole original code and adjusted.
 */
public class ColorSelectorExtended extends EventManager {

   /**
    * Property name that signifies the selected color of this
    * <code>ColorSelector</code> has changed.
    *
    * @since 3.0
    */
   public static final String                         PROP_COLORCHANGE        = "colorValue";              //$NON-NLS-1$

   private static final String                        XML_STATE_CUSTOM_COLORS = "XML_STATE_CUSTOM_COLORS"; //$NON-NLS-1$

   private static final String                        TAG_CUSTOM_COLOR        = "color";                   //$NON-NLS-1$
   private static final String                        ATTR_RED                = "red";                     //$NON-NLS-1$
   private static final String                        ATTR_GREEN              = "green";                   //$NON-NLS-1$
   private static final String                        ATTR_BLUE               = "blue";                    //$NON-NLS-1$

   private Button                                     fButton;
   private RGB                                        fColorValue;
   private Point                                      fExtent;
   private Image                                      fImage;

   /**
    * All colors which are displayed as custom colors in the color dialog, is limited to 16 in Win.
    */
   private RGB[]                                      _allCustomRGBs;

   private final ListenerList<IColorSelectorListener> _openListeners          = new ListenerList<>();

   /**
    * Create a new instance of the receiver and the button that it wrappers in
    * the supplied parent <code>Composite</code>.
    *
    * @param parent
    *           The parent of the button.
    */
   public ColorSelectorExtended(final Composite parent) {

      fButton = new Button(parent, SWT.PUSH);
      fExtent = computeImageSize(parent);
      fImage = new Image(parent.getDisplay(), fExtent.x, fExtent.y);
      final GC gc = new GC(fImage);
      gc.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER));
      gc.fillRectangle(0, 0, fExtent.x, fExtent.y);
      gc.dispose();
      fButton.setImage(fImage);
      fButton.addSelectionListener(widgetSelectedAdapter(event -> open()));
      fButton.addDisposeListener(event -> {
         if (fImage != null) {
            fImage.dispose();
            fImage = null;
         }
      });
      fButton.getAccessible().addAccessibleListener(new AccessibleAdapter() {
         @Override
         public void getName(final AccessibleEvent e) {
            e.result = JFaceResources.getString("ColorSelector.Name"); //$NON-NLS-1$
         }
      });
   }

   /**
    * Adds a property change listener to this <code>ColorSelector</code>.
    * Events are fired when the color in the control changes via the user
    * clicking an selecting a new one in the color dialog. No event is fired in
    * the case where <code>setColorValue(RGB)</code> is invoked.
    *
    * @param listener
    *           a property change listener
    * @since 3.0
    */
   public void addListener(final IPropertyChangeListener listener) {
      addListenerObject(listener);
   }

   public void addOpenListener(final IColorSelectorListener listener) {
      _openListeners.add(listener);
   }

   /**
    * Compute the size of the image to be displayed.
    *
    * @param window
    *           -
    *           the window used to calculate
    * @return <code>Point</code>
    */
   private Point computeImageSize(final Control window) {
      final GC gc = new GC(window);
      final Font f = JFaceResources.getFontRegistry().get(
            JFaceResources.DIALOG_FONT);
      gc.setFont(f);
      final int height = gc.getFontMetrics().getHeight();
      gc.dispose();
      return new Point(height * 3 - 6, height);
   }

   /**
    * Fire an open event that the dialog is opened or closes.
    */
   private void fireOpenEvent(final boolean isOpened) {

      final Object[] listeners = _openListeners.getListeners();

      for (final Object listener : listeners) {

         final IColorSelectorListener colorSelectorListener = (IColorSelectorListener) listener;

         SafeRunnable.run(new SafeRunnable() {
            @Override
            public void run() {
               colorSelectorListener.colorDialogOpened(isOpened);
            }
         });
      }
   }

   /**
    * Get the button control being wrappered by the selector.
    *
    * @return <code>Button</code>
    */
   public Button getButton() {
      return fButton;
   }

   /**
    * Return the currently displayed color.
    *
    * @return <code>RGB</code>
    */
   public RGB getColorValue() {
      return fColorValue;
   }

   public RGB[] getCustomColors() {
      return _allCustomRGBs;
   }

   /**
    * Returns the currently displayed color as RGBA
    *
    * @param opacity
    *           Opacity values
    * @return
    */
   public RGBA getRGBA(final int opacity) {
      return new RGBA(fColorValue.red, fColorValue.green, fColorValue.blue, opacity);
   }

   public void open() {

      fireOpenEvent(true);

      openColorDialog();

      fireOpenEvent(false);
   }

   /**
    * Activate the editor for this selector. This causes the color selection
    * dialog to appear and wait for user input.
    *
    * @since 3.2
    */
   private void openColorDialog() {

      final ColorDialog colorDialog = new ColorDialog(fButton.getShell());

      colorDialog.setRGB(fColorValue);
      colorDialog.setRGBs(_allCustomRGBs);

      final RGB newColor = colorDialog.open();

      _allCustomRGBs = colorDialog.getRGBs();

      if (newColor != null) {

         final RGB oldValue = fColorValue;
         fColorValue = newColor;
         final Object[] finalListeners = getListeners();

         if (finalListeners.length > 0) {

            final PropertyChangeEvent pEvent = new PropertyChangeEvent(
                  this,
                  PROP_COLORCHANGE,
                  oldValue,
                  newColor);

            for (final Object finalListener : finalListeners) {
               final IPropertyChangeListener listener = (IPropertyChangeListener) finalListener;
               listener.propertyChange(pEvent);
            }
         }

         updateColorImage();
      }
   }

   /**
    * Removes the given listener from this <code>ColorSelector</code>. Has
    * no effect if the listener is not registered.
    *
    * @param listener
    *           a property change listener
    * @since 3.0
    */
   public void removeListener(final IPropertyChangeListener listener) {
      removeListenerObject(listener);
   }

   public void removeOpenListener(final IColorSelectorListener listener) {
      _openListeners.remove(listener);
   }

   public void restoreCustomColors(final IDialogSettings state) {

      final String stateValue = Util.getStateString(state, XML_STATE_CUSTOM_COLORS, null);

      if ((stateValue != null) && (stateValue.length() > 0)) {

         try {

            final Reader reader = new StringReader(stateValue);

            restoreCustomColors_Colors(XMLMemento.createReadRoot(reader));

         } catch (final WorkbenchException e) {
            // ignore
         }
      }
   }

   private void restoreCustomColors_Colors(final XMLMemento xmlMemento) {

      final ArrayList<RGB> allCustomRGB = new ArrayList<>();

      for (final IMemento colorMomento : xmlMemento.getChildren()) {

         final Integer red = colorMomento.getInteger(ATTR_RED);
         final Integer green = colorMomento.getInteger(ATTR_GREEN);
         final Integer blue = colorMomento.getInteger(ATTR_BLUE);

         if (red == null || green == null || blue == null) {
            // ignore
            continue;
         }

         allCustomRGB.add(new RGB(red, green, blue));
      }

      setCustomColors(allCustomRGB.toArray(new RGB[allCustomRGB.size()]));
   }

   public void saveCustomColors(final IDialogSettings state) {

      // Build the XML block for writing the bindings and active scheme.
      final XMLMemento xmlMemento = XMLMemento.createWriteRoot(XML_STATE_CUSTOM_COLORS);

      saveCustomColors_Colors(xmlMemento);

      // Write the XML block to the state store
      try (Writer writer = new StringWriter()) {

         xmlMemento.save(writer);
         state.put(XML_STATE_CUSTOM_COLORS, writer.toString());

      } catch (final IOException e) {

         StatusUtil.log(e);
      }
   }

   private void saveCustomColors_Colors(final XMLMemento xmlMemento) {

      if (_allCustomRGBs != null) {

         for (final RGB customRGB : _allCustomRGBs) {

            final IMemento xmlCustomColor = xmlMemento.createChild(TAG_CUSTOM_COLOR);

            xmlCustomColor.putInteger(ATTR_RED, customRGB.red);
            xmlCustomColor.putInteger(ATTR_GREEN, customRGB.green);
            xmlCustomColor.putInteger(ATTR_BLUE, customRGB.blue);
         }
      }
   }

   /**
    * Set the current color value and update the control.
    *
    * @param rgb
    *           The new color.
    */
   public void setColorValue(final RGB rgb) {

      fColorValue = rgb;
      updateColorImage();
   }

   /**
    * The custom colors must be set before the dialog is opened.
    *
    * @param allCustomRGBs
    */
   public void setCustomColors(final RGB[] allCustomRGBs) {

      _allCustomRGBs = allCustomRGBs;
   }

   /**
    * Set whether or not the button is enabled.
    *
    * @param state
    *           the enabled state.
    */
   public void setEnabled(final boolean state) {

      getButton().setEnabled(state);
   }

   public void setToolTipText(final String tooltipText) {

      fButton.setToolTipText(tooltipText);
   }

   /**
    * Update the image being displayed on the button using the current color
    * setting.
    */
   protected void updateColorImage() {
      final Display display = fButton.getDisplay();
      final GC gc = new GC(fImage);
      final Color color = new Color(display, fColorValue);
      gc.setBackground(color);
      gc.fillRectangle(1, 1, fExtent.x - 2, fExtent.y - 2);
      gc.dispose();
      fButton.setImage(fImage);
   }
}
