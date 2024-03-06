package net.sf.swtaddons.autocomplete;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;

public abstract class AutocompleteWidget {
	
	protected AutocompleteContentProposalProvider provider = null;
	protected ContentProposalAdapter adapter = null;

	/**
	 * Return a character array representing the keyboard input triggers
	 * used for firing the ContentProposalAdapter
	 * 
	 * @return
	 * 		character array of trigger chars
	 */
	protected char[] getAutoactivationChars() {
		String lowercaseLetters = "abcdefghijklmnopqrstuvwxyz"; //$NON-NLS-1$
		String uppercaseLetters = lowercaseLetters.toUpperCase();
		String numbers = "0123456789"; //$NON-NLS-1$
		//String delete = new String(new char[] {SWT.DEL});
		// the event in {@link ContentProposalAdapter#addControlListener(Control control)}
		// holds onto a character and when the DEL key is pressed that char
		// value is 8 so the line below catches the DEL keypress
		String delete = new String(new char[] {8}); 
		String allChars = lowercaseLetters + uppercaseLetters + numbers + delete;
		return allChars.toCharArray();
	}

	/**
	 * Returns KeyStroke object which when pressed will fire the
	 * ContentProposalAdapter
	 * 
	 * @return
	 * 		the activation keystroke
	 */
	protected KeyStroke getActivationKeystroke() {
		//keyStroke = KeyStroke.getInstance("Ctrl+Space");
		// Activate on <ctrl><space>
		return KeyStroke.getInstance(new Integer(SWT.CTRL).intValue(), new Integer(' ').intValue());
		//return null;
	}

	protected abstract AutocompleteContentProposalProvider getContentProposalProvider(String[] proposals);

}
