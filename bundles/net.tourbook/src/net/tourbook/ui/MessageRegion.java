/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * The MessageRegion is the optional area to show messages in the page.
 * <p>
 * Copied from org.eclipse.jface.preference.PreferencePage.MessageRegion see:
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=84061
 * </p>
 *
 * @since 3.1
 */
class MessageRegion {

   private Text      messageText;

   private Label     messageImageLabel;

   private Composite messageComposite;

   private String    lastMessageText = UI.EMPTY_STRING;

   private int       lastMessageType;

   /**
    * Create a new instance of the receiver.
    */
   public MessageRegion() {
      //No initial behavior
   }

   /**
    * Clear the error message. Restore the previously displayed message if there is one, if not
    * restore the title label.
    */
   public void clearErrorMessage() {
      updateText(lastMessageText, lastMessageType);
   }

   /**
    * Create the contents for the receiver.
    *
    * @param parent
    *           the Composite that the children will be created in
    */
   public void createContents(final Composite parent) {
      messageComposite = new Composite(parent, SWT.NONE);
      final GridLayout messageLayout = new GridLayout();
      messageLayout.numColumns = 2;
      messageLayout.marginWidth = 0;
      messageLayout.marginHeight = 0;
      messageLayout.makeColumnsEqualWidth = false;
      messageComposite.setLayout(messageLayout);
      messageImageLabel = new Label(messageComposite, SWT.NONE);

      final GridData imageData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
      final Image sizingImage = JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_ERROR);
      Rectangle imageBounds;
      if (sizingImage == null) {
         imageBounds = new Rectangle(0,
               0,
               IDialogConstants.VERTICAL_MARGIN * 2,
               IDialogConstants.VERTICAL_MARGIN * 2);
      } else {
         imageBounds = sizingImage.getBounds();
      }
      imageData.heightHint = imageBounds.height + IDialogConstants.VERTICAL_SPACING;
      imageData.widthHint = imageBounds.width + IDialogConstants.HORIZONTAL_SPACING;
      messageImageLabel.setLayoutData(imageData);

      messageText = new Text(messageComposite, SWT.NONE);
      messageText.setEditable(false);
      messageText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

      final GridData textData = new GridData(GridData.GRAB_HORIZONTAL
            | GridData.FILL_HORIZONTAL
            | GridData.VERTICAL_ALIGN_CENTER);
      messageText.setLayoutData(textData);
      hideRegion();

   }

   /**
    * Hide the message region and clear out the caches.
    */
   private void hideRegion() {
      messageComposite.setVisible(false);
      lastMessageText = null;
      lastMessageType = IMessageProvider.NONE;
   }

   /**
    * Set the colors of the message area.
    *
    * @param color
    *           The color to be use in the message area.
    */
   private void setMessageColors(final Color color) {
      messageText.setBackground(color);
      messageComposite.setBackground(color);
      messageImageLabel.setBackground(color);
   }

   /**
    * Set the layoutData for the messageArea. In most cases this will be a copy of the layoutData
    * used in setTitleLayoutData.
    *
    * @param layoutData
    *           the layoutData for the message area composite.
    */
   public void setMessageLayoutData(final Object layoutData) {
      messageComposite.setLayoutData(layoutData);
   }

   /**
    * Show and enable the widgets in the message region
    */
   private void showRegion() {
      messageComposite.setVisible(true);
   }

   /**
    * Show the new message in the message text and update the image. Base the background color on
    * whether or not there are errors.
    *
    * @param newMessage
    *           The new value for the message
    * @param newType
    *           One of the IMessageProvider constants. If newType is IMessageProvider.NONE show
    *           the title.
    * @see IMessageProvider
    */
   public void updateText(final String newMessage, final int newType) {
      Image newImage = null;
      boolean showingError = false;
      switch (newType) {
      case IMessageProvider.NONE:
         hideRegion();
         return;
      case IMessageProvider.INFORMATION:
         newImage = JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_INFO);
         break;
      case IMessageProvider.WARNING:
         newImage = JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING);
         break;
      case IMessageProvider.ERROR:
         newImage = JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_ERROR);
         showingError = true;
         break;
      }

      if (newMessage == null) {//No message so clear the area
         hideRegion();
         return;
      }
      showRegion();
      // Any more updates required
      if (newMessage.equals(messageText.getText()) && newImage == messageImageLabel.getImage()) {
         return;
      }
      messageImageLabel.setImage(newImage);
      messageText.setText(newMessage);
      if (showingError) {
         setMessageColors(JFaceColors.getErrorBackground(messageComposite.getDisplay()));
      } else {
         lastMessageText = newMessage;
         setMessageColors(JFaceColors.getBannerBackground(messageComposite.getDisplay()));
      }

   }
}
