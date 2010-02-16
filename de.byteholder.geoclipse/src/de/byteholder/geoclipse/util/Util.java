package de.byteholder.geoclipse.util;

import java.net.MalformedURLException;
import java.net.URL;

import net.tourbook.util.StatusUtil;
 
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

public class Util {

	private static final String	URL_SPACE				= " ";		//$NON-NLS-1$
	private static final String	URL_SPACE_REPLACEMENT	= "%20";	//$NON-NLS-1$

	public static int adjustScaleValueOnMouseScroll(final MouseEvent event) {

		// accelerate with Ctrl + Shift key
		int accelerator = (event.stateMask & SWT.CONTROL) != 0 ? 10 : 1;
		accelerator *= (event.stateMask & SWT.SHIFT) != 0 ? 5 : 1;

		final Scale scale = (Scale) event.widget;
		final int newValueDiff = event.count > 0 ? accelerator : -accelerator;

		return scale.getSelection() + newValueDiff;
	}

	public static void adjustSpinnerValueOnMouseScroll(final MouseEvent event) {

		// accelerate with Ctrl + Shift key
		int accelerator = (event.stateMask & SWT.CONTROL) != 0 ? 10 : 1;
		accelerator *= (event.stateMask & SWT.SHIFT) != 0 ? 5 : 1;

		final Spinner spinner = (Spinner) event.widget;
		final int newValue = ((event.count > 0 ? 1 : -1) * accelerator);

		spinner.setSelection(spinner.getSelection() + newValue);
	}

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
			for (int i = 0; i < ch.length; i++) {
				if (obfuscateAll || shouldEncodeArgChar(ch[i])) {
					// escape non-alphanumeric characters
					sb.append("%"); //$NON-NLS-1$
					sb.append(Integer.toHexString(0x100 + (ch[i] & 0xFF)).substring(1));
				} else {
					// letters and digits are ok as-is
					sb.append(ch[i]);
				}
			}
		}

		return sb.toString();
	}

	public static void log(final String message) {
//		System.out.println(//
//				System.nanoTime() + " "
//				//                                                 012345678901234				
//						+ Thread.currentThread().getName().concat("               ").substring(0, 14)
//						+ "\t"
//						+ message);
	}

	/**
	 * Open a link
	 */
	public static void openLink(final Shell shell, String href) {
		
		// format the href for an html file (file:///<filename.html>
		// required for Mac only.
		if (href.startsWith("file:")) { //$NON-NLS-1$
			href = href.substring(5);
			while (href.startsWith("/")) { //$NON-NLS-1$
				href = href.substring(1);
			}
			href = "file:///" + href; //$NON-NLS-1$
		}
		
		final IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
		
		try {

			final IWebBrowser browser = support.getExternalBrowser();
			browser.openURL(new URL(urlEncodeForSpaces(href.toCharArray())));

		} catch (final MalformedURLException e) {
			StatusUtil.showStatus(e);
		} catch (final PartInitException e) {
			StatusUtil.showStatus(e);
		}
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

	/**
	 * This method encodes the url, removes the spaces from the url and replaces
	 * the same with <code>"%20"</code>. This method is required to fix Bug
	 * 77840.
	 * 
	 * @since 3.0.2
	 */
	private static String urlEncodeForSpaces(final char[] input) {
		final StringBuffer retu = new StringBuffer(input.length);
		for (int i = 0; i < input.length; i++) {
			if (input[i] == ' ') {
				retu.append("%20"); //$NON-NLS-1$
			} else {
				retu.append(input[i]);
			}
		}
		return retu.toString();
	}

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
