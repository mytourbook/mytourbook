/*******************************************************************************
 * Copyright (c) 2011-2021 Laurent CARON
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Laurent CARON (laurent.caron at gmail dot com) - initial API
 * and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.opal.duallist.mt;

import net.tourbook.common.UI;

import org.eclipse.nebula.widgets.opal.commons.OpalItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

/**
 * Instances of this class represents items manipulated by this DualList widget
 */
public class MT_DLItem extends OpalItem {

   private static final char NL = UI.NEW_LINE;

   private LAST_ACTION       _lastAction;

   private String            _text2;

   public enum LAST_ACTION {

      NONE, //
      SELECTION, //
      DESELECTION
   }

   /**
    * Constructor
    *
    * @param text
    *           the text displayed in the DualList widget for this item
    */
   public MT_DLItem(final String text) {

      this(text, null);
   }

   /**
    * Constructor
    *
    * @param text
    *           the text displayed in the DualList widget for this item
    * @param image
    *           the image displayed in the DualList widget for this item
    */
   public MT_DLItem(final String text, final Image image) {
      this(text, image, (Font) null, (Color) null);
   }

   /**
    * Constructor
    *
    * @param text
    *           the text displayed in the DualList widget for this item
    * @param image
    *           the image displayed in the DualList widget for this item
    * @param foregroundColor
    *           the foreground color displayed in the DualList widget
    *           for this item
    * @param backgroundColor
    *           the background color displayed in the DualList widget
    *           for this item
    */
   public MT_DLItem(final String text, final Image image, final Color foregroundColor, final Color backgroundColor) {

      setText(text);
      setImage(image);

      setForeground(foregroundColor);
      setBackground(backgroundColor);

      _lastAction = LAST_ACTION.NONE;
   }

   /**
    * Constructor
    *
    * @param text
    *           the text displayed in the DualList widget for this item
    * @param image
    *           the image displayed in the DualList widget for this item
    * @param font
    *           the font displayed in the DualList widget for this item
    */
   public MT_DLItem(final String text, final Image image, final Font font) {
      this(text, image, font, null);
   }

   /**
    * Constructor
    *
    * @param text
    *           the text displayed in the DualList widget for this item
    * @param image
    *           the image displayed in the DualList widget for this item
    * @param font
    *           the font displayed in the DualList widget for this item
    * @param foregroundColor
    *           the foreground color displayed in the DualList widget
    *           for this item
    */
   public MT_DLItem(final String text, final Image image, final Font font, final Color foregroundColor) {

      setText(text);
      setImage(image);

      setFont(font);
      setForeground(foregroundColor);

      _lastAction = LAST_ACTION.NONE;
   }

   public MT_DLItem(final String text1,
                    final String text2,
                    final String dataKey,
                    final Object data) {

      setText(text1);
      _text2 = text2;

      setData(dataKey, data);
   }

   /**
    * @see org.eclipse.nebula.widgets.opal.commons.OpalItem#getHeight()
    */
   @Override
   public int getHeight() {
      throw new UnsupportedOperationException("MT_DLItem does not support this method"); //$NON-NLS-1$
   }

   /**
    * @return the last action (NONE, SELECTION, DESELECTION)
    */
   public LAST_ACTION getLastAction() {
      return _lastAction;
   }

   public String getText2() {
      return _text2;
   }

   /**
    * @see org.eclipse.nebula.widgets.opal.commons.OpalItem#setHeight(int)
    */
   @Override
   public void setHeight(final int height) {
      throw new UnsupportedOperationException("MT_DLItem does not support this method"); //$NON-NLS-1$
   }

   /**
    * @param lastAction
    *           the last action performed on this DLItem
    *
    * @return
    */
   public MT_DLItem setLastAction(final LAST_ACTION lastAction) {

      _lastAction = lastAction;

      return this;
   }

   /**
    * Change the selection state of this item
    *
    * @param selection
    *
    * @return
    */
   public MT_DLItem setSelected(final boolean selection) {

      _lastAction = selection ? LAST_ACTION.SELECTION : LAST_ACTION.DESELECTION;

      return this;
   }

   public void setText2(final String text2) {

      _text2 = text2;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "MT_DLItem" + NL //                           //$NON-NLS-1$

            + " text        = " + getText() + NL //         //$NON-NLS-1$
            + " text2       = " + _text2 + NL //            //$NON-NLS-1$
            + " _lastAction = " + _lastAction + NL //       //$NON-NLS-1$

            + NL;
   }
}
