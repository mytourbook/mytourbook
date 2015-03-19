package de.byteholder.geoclipse.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;

public class Util {

	private static final String	URL_SPACE				= " ";		//$NON-NLS-1$
	private static final String	URL_SPACE_REPLACEMENT	= "%20";	//$NON-NLS-1$

	public static String encodeSpace(final String urlString) {
		return urlString.replaceAll(URL_SPACE, URL_SPACE_REPLACEMENT);
	}

	/**
	 *** Hex-encodes a URL argument
	 ***
	 *** @param s
	 *            The URL argument to encode
	 *** @param obfuscateAll
	 *            True to force hex-encoding on all argument characters
	 *** @return The StringBuffer where the hex-encoded String will be placed
	 **/
	public static String encodeUrl(final String s, final boolean obfuscateAll) {

		final StringBuilder sb = new StringBuilder();

		if (s != null) {
			final char ch[] = new char[s.length()];
			s.getChars(0, s.length(), ch, 0);
			for (final char element : ch) {
				if (obfuscateAll || shouldEncodeArgChar(element)) {
					// escape non-alphanumeric characters
					sb.append("%"); //$NON-NLS-1$
					sb.append(Integer.toHexString(0x100 + (element & 0xFF)).substring(1));
				} else {
					// letters and digits are ok as-is
					sb.append(element);
				}
			}
		}

		return sb.toString();
	}

	/**
	 *** Returns true if the specified character should be hex-encoded in a URL
	 ***
	 * @param ch
	 *            The character to test
	 *** @return True if the specified character should be hex-encoded in a URL
	 **/
	private static boolean shouldEncodeArgChar(final char ch) {
		if (Character.isLetterOrDigit(ch)) {
			return false;
		} else if ((ch == '_') || (ch == '-') || (ch == '.')) {
			return false;
		} else {
			return true;
		}
	}

	public static VerifyListener verifyListenerInteger(final boolean canBeNegative) {

		return new VerifyListener() {
			@Override
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
