package net.tourbook.ext.srtm;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;

public class UI {

	public static final String	EMPTY_STRING	= "";	//$NON-NLS-1$
	public static final String	STRING_0		= "0";	//$NON-NLS-1$

	public static VerifyListener verifyListenerInteger(final boolean canBeNegative) {

		return new VerifyListener() {
			public void verifyText(final VerifyEvent e) {

				// check backspace and del key
				if (e.character == SWT.BS || e.character == SWT.DEL) {
					return;
				}

				// check '-' key
				if (canBeNegative && e.character == '-') {
					return;
				}

				try {
					Integer.parseInt(e.text);
				} catch (final NumberFormatException ex) {
					e.doit = false;
				}
			}
		};
	}

}
