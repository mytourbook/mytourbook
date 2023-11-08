/*******************************************************************************
 * Copyright (c) 2021 Laurent CARON
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

/**
 * Configuration class for the DualList widget
 */
public class MT_DLConfiguration {

   private Color   itemsBackgroundColor,
         itemsOddLinesColor,
         itemsForegroundColor,
         selectionBackgroundColor,
         selectionOddLinesColor,
         selectionForegroundColor;

   private Image   doubleDownImage,
         doubleUpImage,
         doubleLeftImage,
         doubleRightImage,
         downImage,
         leftImage,
         upImage, rightImage;

   private int     itemsTextAlignment = SWT.LEFT, selectionTextAlignment = SWT.LEFT;

   private boolean doubleRightVisible = true;
   private boolean doubleLeftVisible  = true;
   private boolean doubleUpVisible    = true;
   private boolean upVisible          = true;
   private boolean doubleDownVisible  = true;
   private boolean downVisible        = true;

   /**
    * @return the image for the "double down" button
    */
   public Image getDoubleDownImage() {
      return doubleDownImage;
   }

   /**
    * @return the image for the "double left" button
    */
   public Image getDoubleLeftImage() {
      return doubleLeftImage;
   }

   /**
    * @return the image for the "double right" button
    */
   public Image getDoubleRightImage() {
      return doubleRightImage;
   }

   /**
    * @return the image for the "double up" button
    */
   public Image getDoubleUpImage() {
      return doubleUpImage;
   }

   /**
    * @return the image for the "down" button
    */
   public Image getDownImage() {
      return downImage;
   }

   /**
    * @return the background color of the items panel
    */
   public Color getItemsBackgroundColor() {
      return itemsBackgroundColor;
   }

   /**
    * @return the foreground color of the items panel
    */
   public Color getItemsForegroundColor() {
      return itemsForegroundColor;
   }

   /**
    * @return the background color of the odd lines for the unselected items list
    */
   public Color getItemsOddLinesColor() {
      return itemsOddLinesColor;
   }

   /**
    * @return the text alignment (SWT.RIGHT, SWT.CENTER, SWT.LEFT) for the unselected items
    */
   public int getItemsTextAlignment() {
      return itemsTextAlignment;
   }

   /**
    * @return the image for the "left" button
    */
   public Image getLeftImage() {
      return leftImage;
   }

   /**
    * @return the image for the "right" button
    */
   public Image getRightImage() {
      return rightImage;
   }

   /**
    * @return the background color of the selected items panel
    */
   public Color getSelectionBackgroundColor() {
      return selectionBackgroundColor;
   }

   /**
    * @return the foreground color of the items panel
    */
   public Color getSelectionForegroundColor() {
      return selectionForegroundColor;
   }

   /**
    * @return the background color of the odd lines for the selected items list
    */
   public Color getSelectionOddLinesColor() {
      return selectionOddLinesColor;
   }

   /**
    * @return the text alignment (SWT.RIGHT, SWT.CENTER, SWT.LEFT) for the selected items
    */
   public int getSelectionTextAlignment() {
      return selectionTextAlignment;
   }

   /**
    * @return the image for the "up" button
    */
   public Image getUpImage() {
      return upImage;
   }

   /**
    * @return <code>true</code> if the "double down" button is visible, <code>false</code> otherwise
    */
   public boolean isDoubleDownVisible() {
      return doubleDownVisible;
   }

   /**
    * @return <code>true</code> if the "double left" button is visible, <code>false</code> otherwise
    */
   public boolean isDoubleLeftVisible() {
      return doubleLeftVisible;
   }

   /**
    * @return <code>true</code> if the "double right" button is visible, <code>false</code>
    *         otherwise
    */
   public boolean isDoubleRightVisible() {
      return doubleRightVisible;
   }

   /**
    * @return <code>true</code> if the "double up" button is visible, <code>false</code> otherwise
    */
   public boolean isDoubleUpVisible() {
      return doubleUpVisible;
   }

   /**
    * @return <code>true</code> if the "down" button is visible, <code>false</code> otherwise
    */
   public boolean isDownVisible() {
      return downVisible;
   }

   /**
    * @return <code>true</code> if the "up" button is visible, <code>false</code> otherwise
    */
   public boolean isUpVisible() {
      return upVisible;
   }

   /**
    * @param image
    *           the image for the "double down" button to set
    */
   public MT_DLConfiguration setDoubleDownImage(final Image image) {
      this.doubleDownImage = image;
      return this;
   }

   /**
    * @param visible
    *           the visibility of the "double down" button
    */
   public MT_DLConfiguration setDoubleDownVisible(final boolean visible) {
      this.doubleDownVisible = visible;
      return this;
   }

   /**
    * @param image
    *           the image for the "double left" button to set
    */
   public MT_DLConfiguration setDoubleLeftImage(final Image image) {
      this.doubleLeftImage = image;
      return this;
   }

   /**
    * @param visible
    *           the visibility of the "double left" button
    */
   public MT_DLConfiguration setDoubleLeftVisible(final boolean visible) {
      this.doubleLeftVisible = visible;
      return this;
   }

   /**
    * @param image
    *           the image for the "double right" button to set
    */
   public MT_DLConfiguration setDoubleRightImage(final Image image) {
      this.doubleRightImage = image;
      return this;
   }

   /**
    * @param visible
    *           the visibility of the "double right" button
    */
   public MT_DLConfiguration setDoubleRightVisible(final boolean visible) {
      this.doubleRightVisible = visible;
      return this;
   }

   /**
    * @param image
    *           the image for the "double up" button to set
    */
   public MT_DLConfiguration setDoubleUpImage(final Image image) {
      this.doubleUpImage = image;
      return this;
   }

   /**
    * @param visible
    *           the visibility of the "double up" button
    */
   public MT_DLConfiguration setDoubleUpVisible(final boolean visible) {
      this.doubleUpVisible = visible;
      return this;
   }

   /**
    * @param image
    *           the image for the "down" button to set
    */
   public MT_DLConfiguration setDownImage(final Image image) {
      this.downImage = image;
      return this;
   }

   /**
    * @param visible
    *           the visibility of the "down" button
    */
   public MT_DLConfiguration setDownVisible(final boolean visible) {
      this.downVisible = visible;
      return this;
   }

   /**
    * @param color
    *           the background color of the items panel to set
    */
   public MT_DLConfiguration setItemsBackgroundColor(final Color color) {
      if (color != null && color.isDisposed()) {
         SWT.error(SWT.ERROR_INVALID_ARGUMENT);
      }
      this.itemsBackgroundColor = color;
      return this;
   }

   /**
    * @param color
    *           the foreground color of the items panel to set
    *
    * @return
    */
   public MT_DLConfiguration setItemsForegroundColor(final Color color) {
      if (color != null && color.isDisposed()) {
         SWT.error(SWT.ERROR_INVALID_ARGUMENT);
      }
      this.itemsForegroundColor = color;
      return this;
   }

   /**
    * @param color
    *           the background color of the odd lines for the unselected items list to set
    */
   public MT_DLConfiguration setItemsOddLinesColor(final Color color) {
      if (color != null && color.isDisposed()) {
         SWT.error(SWT.ERROR_INVALID_ARGUMENT);
      }
      this.itemsOddLinesColor = color;
      return this;
   }

   /**
    * @param alignment
    *           the text alignment (SWT.RIGHT, SWT.CENTER, SWT.LEFT) for the unselected items to set
    */
   public MT_DLConfiguration setItemsTextAlignment(final int alignment) {
      if (alignment != SWT.NONE && alignment != SWT.LEFT && alignment != SWT.RIGHT && alignment != SWT.CENTER) {
         SWT.error(SWT.ERROR_INVALID_ARGUMENT);
      }
      this.itemsTextAlignment = alignment;
      return this;
   }

   /**
    * @param image
    *           the image for the "left" button to set
    */
   public MT_DLConfiguration setLeftImage(final Image image) {
      this.leftImage = image;
      return this;
   }

   /**
    * @param image
    *           the image for the "right" button to set
    */
   public MT_DLConfiguration setRightImage(final Image image) {
      this.rightImage = image;
      return this;
   }

   /**
    * @param color
    *           the background color of the items panel to set
    */
   public MT_DLConfiguration setSelectionBackgroundColor(final Color color) {
      if (color != null && color.isDisposed()) {
         SWT.error(SWT.ERROR_INVALID_ARGUMENT);
      }
      this.selectionBackgroundColor = color;
      return this;
   }

   /**
    * @param color
    *           the foreground color of the selection panel to set
    *
    * @return
    */
   public MT_DLConfiguration setSelectionForegroundColor(final Color color) {
      if (color != null && color.isDisposed()) {
         SWT.error(SWT.ERROR_INVALID_ARGUMENT);
      }
      this.selectionForegroundColor = color;
      return this;
   }

   /**
    * @param color
    *           the background color of the odd lines for the selected items list to set
    */
   public MT_DLConfiguration setSelectionOddLinesColor(final Color color) {
      if (color != null && color.isDisposed()) {
         SWT.error(SWT.ERROR_INVALID_ARGUMENT);
      }
      this.selectionOddLinesColor = color;
      return this;
   }

   /**
    * @param alignment
    *           the text alignment (SWT.RIGHT, SWT.CENTER, SWT.LEFT) for the unselected items to set
    */
   public MT_DLConfiguration setSelectionTextAlignment(final int alignment) {
      if (alignment != SWT.NONE && alignment != SWT.LEFT && alignment != SWT.RIGHT && alignment != SWT.CENTER) {
         SWT.error(SWT.ERROR_INVALID_ARGUMENT);
      }
      this.selectionTextAlignment = alignment;
      return this;
   }

   /**
    * @param image
    *           the image for the "up" button to set
    */
   public MT_DLConfiguration setUpImage(final Image image) {
      this.upImage = image;
      return this;
   }

   /**
    * @param visible
    *           the visibility of the "up" button
    */
   public MT_DLConfiguration setUpVisible(final boolean visible) {
      this.upVisible = visible;
      return this;
   }

}
