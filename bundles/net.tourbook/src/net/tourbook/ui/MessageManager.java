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

/* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 *
 * this is a copy from MessageManager.java, this implementation is using a Form instead of a ScrolledForm
 *
 * New Methods: getMessageCount()
 *
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.IMessagePrefixProvider;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.Hyperlink;

/**
 * @see IMessageManager
 */

public class MessageManager implements IMessageManager {

   private static final DefaultPrefixProvider   DEFAULT_PREFIX_PROVIDER       = new DefaultPrefixProvider();

   private static FieldDecoration               standardError                 = FieldDecorationRegistry.getDefault()
         .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
   private static FieldDecoration               standardWarning               = FieldDecorationRegistry.getDefault()
         .getFieldDecoration(FieldDecorationRegistry.DEC_WARNING);
   private static FieldDecoration               standardInformation           = FieldDecorationRegistry.getDefault()
         .getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
   private static final String[]                SINGLE_MESSAGE_SUMMARY_KEYS   = {
         Messages.message_manager_sMessageSummary,
         Messages.message_manager_sMessageSummary,
         Messages.message_manager_sWarningSummary,
         Messages.message_manager_sErrorSummary };
   private static final String[]                MULTIPLE_MESSAGE_SUMMARY_KEYS = {
         Messages.message_manager_pMessageSummary,
         Messages.message_manager_pMessageSummary,
         Messages.message_manager_pWarningSummary,
         Messages.message_manager_pErrorSummary };
   private ArrayList<IMessage>                  messages                      = new ArrayList<>();

   private Hashtable<Control, ControlDecorator> decorators                    = new Hashtable<>();
   private boolean                              autoUpdate                    = true;
   private Form                                 scrolledForm;

   private IMessagePrefixProvider               prefixProvider                = DEFAULT_PREFIX_PROVIDER;

   private int                                  decorationPosition            = SWT.LEFT | SWT.BOTTOM;

   class ControlDecorator {
      private ControlDecoration   decoration;
      private ArrayList<IMessage> controlMessages = new ArrayList<>();
      private String              prefix;

      ControlDecorator(final Control control) {
         this.decoration = new ControlDecoration(control, decorationPosition, scrolledForm.getBody());
      }

      void addAll(final ArrayList<IMessage> target) {
         target.addAll(controlMessages);
      }

      void addMessage(final Object key, final String text, final Object data, final int type) {
         final Message message = MessageManager.this.addMessage(getPrefix(), key, text, data, type, controlMessages);
         message.control = decoration.getControl();
         if (isAutoUpdate()) {
            update();
         }
      }

      private void createPrefix() {
         if (prefixProvider == null) {
            prefix = UI.EMPTY_STRING;
            return;
         }
         prefix = prefixProvider.getPrefix(decoration.getControl());
         if (prefix == null) {
            // make a prefix anyway
            prefix = UI.EMPTY_STRING;
         }
      }

      String getPrefix() {
         if (prefix == null) {
            createPrefix();
         }
         return prefix;
      }

      public boolean isDisposed() {
         return decoration.getControl() == null;
      }

      boolean removeMessage(final Object key) {
         final Message message = findMessage(key, controlMessages);
         if (message != null) {
            controlMessages.remove(message);
            if (isAutoUpdate()) {
               update();
            }
         }
         return message != null;
      }

      boolean removeMessages() {
         if (controlMessages.isEmpty()) {
            return false;
         }
         controlMessages.clear();
         if (isAutoUpdate()) {
            update();
         }
         return true;
      }

      public void update() {
         if (controlMessages.isEmpty()) {
            decoration.setDescriptionText(null);
            decoration.hide();
         } else {
            final ArrayList<IMessage> peers = createPeers(controlMessages);
            final int type = peers.get(0).getMessageType();
            final String description = createDetails(createPeers(peers), true);
            if (type == IMessageProvider.ERROR) {
               decoration.setImage(standardError.getImage());
            } else if (type == IMessageProvider.WARNING) {
               decoration.setImage(standardWarning.getImage());
            } else if (type == IMessageProvider.INFORMATION) {
               decoration.setImage(standardInformation.getImage());
            }
            decoration.setDescriptionText(description);
            decoration.show();
         }
      }

      void updatePosition() {
         final Control control = decoration.getControl();
         decoration.dispose();
         this.decoration = new ControlDecoration(control, decorationPosition, scrolledForm.getBody());
         update();
      }

      void updatePrefix() {
         prefix = null;
      }
   }

   static class DefaultPrefixProvider implements IMessagePrefixProvider {

      @Override
      public String getPrefix(final Control c) {
         final Composite parent = c.getParent();
         final Control[] siblings = parent.getChildren();
         for (int i = 0; i < siblings.length; i++) {
            if (siblings[i] == c) {
               // this is us - go backward until you hit
               // a label-like widget
               for (int j = i - 1; j >= 0; j--) {
                  final Control label = siblings[j];
                  String ltext = null;
                  if (label instanceof Label) {
                     ltext = ((Label) label).getText();
                  } else if (label instanceof Hyperlink) {
                     ltext = ((Hyperlink) label).getText();
                  } else if (label instanceof CLabel) {
                     ltext = ((CLabel) label).getText();
                  }
                  if (ltext != null) {
                     if (!ltext.endsWith(net.tourbook.common.UI.SYMBOL_COLON)) {
                        return ltext + ": "; //$NON-NLS-1$
                     }
                     return ltext + net.tourbook.common.UI.SPACE1;
                  }
               }
               break;
            }
         }
         return null;
      }
   }

   static class Message implements IMessage {
      Control control;
      Object  data;
      Object  key;
      String  message;
      int     type;
      String  prefix;

      Message(final Object key, final String message, final int type, final Object data) {
         this.key = key;
         this.message = message;
         this.type = type;
         this.data = data;
      }

      /*
       * (non-Javadoc)
       * @see org.eclipse.ui.forms.messages.IMessage#getControl()
       */
      @Override
      public Control getControl() {
         return control;
      }

      /*
       * (non-Javadoc)
       * @see org.eclipse.ui.forms.messages.IMessage#getData()
       */
      @Override
      public Object getData() {
         return data;
      }

      /*
       * (non-Javadoc)
       * @see org.eclipse.jface.dialogs.IMessage#getKey()
       */
      @Override
      public Object getKey() {
         return key;
      }

      /*
       * (non-Javadoc)
       * @see org.eclipse.jface.dialogs.IMessageProvider#getMessage()
       */
      @Override
      public String getMessage() {
         return message;
      }

      /*
       * (non-Javadoc)
       * @see org.eclipse.jface.dialogs.IMessageProvider#getMessageType()
       */
      @Override
      public int getMessageType() {
         return type;
      }

      /*
       * (non-Javadoc)
       * @see org.eclipse.ui.forms.messages.IMessage#getPrefix()
       */
      @Override
      public String getPrefix() {
         return prefix;
      }
   }

   /**
    * Creates a new instance of the message manager that will work with the provided form.
    *
    * @param scrolledForm
    *           the form to control
    */
   public MessageManager(final Form scrolledForm) {
      this.scrolledForm = scrolledForm;
   }

   public static String createDetails(final IMessage[] messages) {
      if (messages == null || messages.length == 0) {
         return null;
      }
      final StringWriter sw = new StringWriter();
      final PrintWriter out = new PrintWriter(sw);

      for (int i = 0; i < messages.length; i++) {
         if (i > 0) {
            out.println();
         }
         out.print(getFullMessage(messages[i]));
      }
      out.flush();
      return sw.toString();
   }

   private static String getFullMessage(final IMessage message) {
      if (message.getPrefix() == null) {
         return message.getMessage();
      }
      return message.getPrefix() + message.getMessage();
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.forms.IMessageManager#addMessage(java.lang.Object, java.lang.String, int)
    */
   @Override
   public void addMessage(final Object key, final String messageText, final Object data, final int type) {
      addMessage(null, key, messageText, data, type, messages);
      if (isAutoUpdate()) {
         updateForm();
      }
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.forms.IMessageManager#addMessage(java.lang.Object, java.lang.String, int,
    * org.eclipse.swt.widgets.Control)
    */
   @Override
   public void addMessage(final Object key,
                          final String messageText,
                          final Object data,
                          final int type,
                          final Control control) {
      ControlDecorator dec = decorators.get(control);

      if (dec == null) {
         dec = new ControlDecorator(control);
         decorators.put(control, dec);
      }
      dec.addMessage(key, messageText, data, type);
      if (isAutoUpdate()) {
         updateForm();
      }
   }

   private Message addMessage(final String prefix,
                              final Object key,
                              final String messageText,
                              final Object data,
                              final int type,
                              final ArrayList<IMessage> list) {
      Message message = findMessage(key, list);
      if (message == null) {
         message = new Message(key, messageText, type, data);
         message.prefix = prefix;
         list.add(message);
      } else {
         message.message = messageText;
         message.type = type;
         message.data = data;
      }
      return message;
   }

   private String createDetails(final ArrayList<IMessage> messages, final boolean excludePrefix) {
      final StringWriter sw = new StringWriter();
      final PrintWriter out = new PrintWriter(sw);

      for (int i = 0; i < messages.size(); i++) {
         if (i > 0) {
            out.println();
         }
         final IMessage m = messages.get(i);
         out.print(excludePrefix ? m.getMessage() : getFullMessage(m));
      }
      out.flush();
      return sw.toString();
   }

   private ArrayList<IMessage> createPeers(final ArrayList<IMessage> messages) {
      final ArrayList<IMessage> peers = new ArrayList<>();
      int maxType = 0;
      for (final IMessage message2 : messages) {
         final Message message = (Message) message2;
         if (message.type > maxType) {
            peers.clear();
            maxType = message.type;
         }
         if (message.type == maxType) {
            peers.add(message);
         }
      }
      return peers;
   }

   /*
    * Adds the message if it does not already exist in the provided list.
    */

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.forms.IMessageManager#createSummary(org.eclipse.ui.forms.IMessage[])
    */
   @Override
   public String createSummary(final IMessage[] messages) {
      return createDetails(messages);
   }

   /*
    * Finds the message with the provided key in the provided list.
    */

   private Message findMessage(final Object key, final ArrayList<IMessage> list) {
      for (final Object element : list) {
         final Message message = (Message) element;
         if (message.getKey().equals(key)) {
            return message;
         }
      }
      return null;
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.forms.IMessageManager#getDecorationPosition()
    */
   @Override
   public int getDecorationPosition() {
      return decorationPosition;
   }

   /*
    * Updates the container by rolling the messages up from the controls.
    */

   /**
    * @return Returns the number of error messages
    */
   public int getErrorMessageCount() {

      int errors = 0;

      for (final Enumeration<ControlDecorator> enm = decorators.elements(); enm.hasMoreElements();) {
         final ControlDecorator dec = enm.nextElement();
         final ArrayList<?> allMessages = dec.controlMessages;

         for (final Object object : allMessages) {
            if (object instanceof Message) {
               final Message message = (Message) object;
               if (message.getMessageType() == IMessageProvider.ERROR) {
                  errors++;
               }
            }
         }
      }

      return errors;
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.forms.IMessageManager#getMessagePrefixProvider()
    */
   @Override
   public IMessagePrefixProvider getMessagePrefixProvider() {
      return prefixProvider;
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.forms.IMessageManager#isAutoUpdate()
    */
   @Override
   public boolean isAutoUpdate() {
      return autoUpdate;
   }

   private void pruneControlDecorators() {
      for (final Iterator<ControlDecorator> iter = decorators.values().iterator(); iter.hasNext();) {
         final ControlDecorator dec = iter.next();
         if (dec.isDisposed()) {
            iter.remove();
         }
      }
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.forms.IMessageManager#removeAllMessages()
    */
   @Override
   public void removeAllMessages() {
      boolean needsUpdate = false;
      for (final Enumeration<ControlDecorator> enm = decorators.elements(); enm.hasMoreElements();) {
         final ControlDecorator control = enm.nextElement();
         if (control.removeMessages()) {
            needsUpdate = true;
         }
      }
      if (!messages.isEmpty()) {
         messages.clear();
         needsUpdate = true;
      }
      if (needsUpdate && isAutoUpdate()) {
         updateForm();
      }
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.forms.IMessageManager#removeMessage(java.lang.Object)
    */
   @Override
   public void removeMessage(final Object key) {
      final Message message = findMessage(key, messages);
      if (message != null) {
         messages.remove(message);
         if (isAutoUpdate()) {
            updateForm();
         }
      }
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.forms.IMessageManager#removeMessage(java.lang.Object,
    * org.eclipse.swt.widgets.Control)
    */
   @Override
   public void removeMessage(final Object key, final Control control) {
      final ControlDecorator dec = decorators.get(control);
      if (dec == null) {
         return;
      }
      if (dec.removeMessage(key)) {
         if (isAutoUpdate()) {
            updateForm();
         }
      }
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.forms.IMessageManager#removeMessages()
    */
   @Override
   public void removeMessages() {
      if (!messages.isEmpty()) {
         messages.clear();
         if (isAutoUpdate()) {
            updateForm();
         }
      }
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.forms.IMessageManager#removeMessages(org.eclipse.swt.widgets.Control)
    */
   @Override
   public void removeMessages(final Control control) {
      final ControlDecorator dec = decorators.get(control);
      if (dec != null) {
         if (dec.removeMessages()) {
            if (isAutoUpdate()) {
               updateForm();
            }
         }
      }
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.forms.IMessageManager#setAutoUpdate(boolean)
    */
   @Override
   public void setAutoUpdate(final boolean autoUpdate) {
      final boolean needsUpdate = !this.autoUpdate && autoUpdate;
      this.autoUpdate = autoUpdate;
      if (needsUpdate) {
         update();
      }
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.forms.IMessageManager#setDecorationPosition(int)
    */
   @Override
   public void setDecorationPosition(final int position) {
      this.decorationPosition = position;
      for (final Object element : decorators.values()) {
         final ControlDecorator dec = (ControlDecorator) element;
         dec.updatePosition();
      }
   }

   /*
    * (non-Javadoc)
    * @seeorg.eclipse.ui.forms.IMessageManager#setMessagePrefixProvider(org.eclipse.ui.forms.
    * IMessagePrefixProvider)
    */
   @Override
   public void setMessagePrefixProvider(final IMessagePrefixProvider provider) {
      this.prefixProvider = provider;
      for (final Object element : decorators.values()) {
         final ControlDecorator dec = (ControlDecorator) element;
         dec.updatePrefix();
      }
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.forms.IMessageManager#update()
    */
   @Override
   public void update() {
      // Update decorations
      for (final Object element : decorators.values()) {
         final ControlDecorator dec = (ControlDecorator) element;
         dec.update();
      }
      // Update the form
      updateForm();
   }

   private void update(final ArrayList<IMessage> mergedList) {
      pruneControlDecorators();
      if (scrolledForm.getHead().getBounds().height == 0 || mergedList.isEmpty() || mergedList == null) {
         scrolledForm.setMessage(null, IMessageProvider.NONE);
         return;
      }
      final ArrayList<IMessage> peers = createPeers(mergedList);
      final int maxType = peers.get(0).getMessageType();
      String messageText;
      final IMessage[] array = peers.toArray(new IMessage[peers.size()]);
      if (peers.size() == 1 && ((Message) peers.get(0)).prefix == null) {
         // a single message
         final IMessage message = peers.get(0);
         messageText = message.getMessage();
         scrolledForm.setMessage(messageText, maxType, array);
      } else {
         // show a summary message for the message
         // and list of errors for the details
         if (peers.size() > 1) {
            messageText = Messages.bind(MULTIPLE_MESSAGE_SUMMARY_KEYS[maxType], new String[] { peers.size() + UI.EMPTY_STRING });
         } else {
            messageText = SINGLE_MESSAGE_SUMMARY_KEYS[maxType];
         }
         scrolledForm.setMessage(messageText, maxType, array);
      }
   }

   private void updateForm() {
      final ArrayList<IMessage> mergedList = new ArrayList<>();
      mergedList.addAll(messages);
      for (final Enumeration<ControlDecorator> enm = decorators.elements(); enm.hasMoreElements();) {
         final ControlDecorator dec = enm.nextElement();
         dec.addAll(mergedList);
      }
      update(mergedList);
   }
}
