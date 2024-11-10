/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.common.autocomplete;

import org.eclipse.jface.bindings.keys.KeyStroke;

// Original: net.sf.swtaddons.autocomplete

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;

public abstract class AutoComplete_WidgetMT {

   protected AutoComplete_ContentProposalProviderMT provider;
   protected ContentProposalAdapter                 adapter;

   /**
    * Returns KeyStroke object which when pressed will fire the
    * ContentProposalAdapter
    *
    * @return
    *         the activation keystroke
    */
   protected KeyStroke getActivationKeystroke() {

      //keyStroke = KeyStroke.getInstance("Ctrl+Space");
      // Activate on <ctrl><space>

      return KeyStroke.getInstance(Integer.valueOf(SWT.CTRL), Integer.valueOf(' '));
      //return null;
   }

   /**
    * Return a character array representing the keyboard input triggers
    * used for firing the ContentProposalAdapter
    *
    * @return
    *         character array of trigger chars
    */
   protected char[] getAutoActivationChars() {

      final String lowercaseLetters = "abcdefghijklmnopqrstuvwxyz"; //$NON-NLS-1$
      final String uppercaseLetters = lowercaseLetters.toUpperCase();
      final String numbers = "0123456789"; //$NON-NLS-1$

      //String delete = new String(new char[] {SWT.DEL});
      // the event in {@link ContentProposalAdapter#addControlListener(Control control)}
      // holds onto a character and when the DEL key is pressed that char
      // value is 8 so the line below catches the DEL keypress

      final String deleteKey = new String(new char[] { 8 });
      final String allChars = lowercaseLetters + uppercaseLetters + numbers + deleteKey;

      return allChars.toCharArray();
   }

   protected abstract AutoComplete_ContentProposalProviderMT getContentProposalProvider(String[] proposals);
}
