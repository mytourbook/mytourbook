package de.byteholder.geoclipse.swt;

import java.io.File;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.logging.StatusUtil;

public class UI {

	public static final String	EMPTY_STRING	= "";		//$NON-NLS-1$
	public static final String	SPACE			= " ";		//$NON-NLS-1$
	public static final char	DASH			= '-';
	public static final String	DASH_WITH_SPACE	= " - ";	//$NON-NLS-1$
	public static final String	NEW_LINE		= "\n";	//$NON-NLS-1$
	public static final String	DOTS			= "...";	//$NON-NLS-1$
	public static final String	MBYTES			= "MByte";	//$NON-NLS-1$

//	private static final int	MAX_ID_LENGTH	= 150;

	public static void addSashColorHandler(final Sash sash) {

		sash.addMouseTrackListener(new MouseTrackListener() {

			public void mouseEnter(final MouseEvent e) {
				sash.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
			}

			public void mouseExit(final MouseEvent e) {
				sash.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			}

			public void mouseHover(final MouseEvent e) {}
		});

		sash.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				// hide background when sash is dragged

				if (e.detail == SWT.DRAG) {
					sash.setBackground(null);
				} else {
					sash.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
				}
			}
		});
	}

	/**
	 * @param file
	 * @return Returns <code>true</code> when the file should be overwritten, otherwise
	 *         <code>false</code>
	 */
	public static boolean confirmOverwrite(final File file) {

		final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		final MessageDialog dialog = new MessageDialog(
				shell,
				Messages.App_Dlg_ConfirmFileOverwrite_Title,
				null,
				NLS.bind(Messages.App_Dlg_ConfirmFileOverwrite_Message, file.getPath()),
				MessageDialog.QUESTION,
				new String[] { IDialogConstants.YES_LABEL, IDialogConstants.CANCEL_LABEL },
				0);

		dialog.open();

		return dialog.getReturnCode() == 0;
	}

	/**
	 * @param name
	 * @param maxLength
	 * @return
	 */
	public static String createIdFromName(String name, final int maxLength) {

		name = name.toLowerCase();

		/*
		 * replace all invalid filepath chars into dashs
		 */
		final StringBuilder sbId = new StringBuilder();
		int addedChars = 0;
		char lastChar = 0;

		for (int charIndex = 0; charIndex < name.length(); charIndex++) {

			final char nameChar = name.charAt(charIndex);
			final int nameInt = name.charAt(charIndex);

			if (nameInt > 0x00 && nameInt < 0x80 && Character.isJavaIdentifierPart(nameChar)) {

				sbId.append(nameChar);
				lastChar = nameChar;
				addedChars++;

			} else if (nameChar == ' '
					|| nameChar == DASH
					|| nameChar == '_'
					|| nameChar == '.'
					|| nameChar == '/'
					|| nameChar == ':') {

				// don't repeat dashes

				if (lastChar != DASH) {

					// replace with a dash
					sbId.append(DASH);
					addedChars++;

					lastChar = DASH;
				}
			}
		}

		/*
		 * shorten id because it is used for a filepath and this has a maximum length of 255
		 */
		if (addedChars > maxLength) {

			final int length = sbId.length();
			final char[] originalId = new char[length];
			sbId.getChars(0, length, originalId, 0);

			/*
			 * find longest word
			 */
			int currentWordLength = 0;
			int maxWordLength = 0;
			for (int charIndex = 0; charIndex < originalId.length; charIndex++) {
				if (originalId[charIndex] == DASH) {
					maxWordLength = (maxWordLength >= currentWordLength) ? maxWordLength : currentWordLength;
					currentWordLength = 0;
				} else {
					currentWordLength++;
				}
			}

			/*
			 * shorten in each round the longest word
			 */
			final StringBuilder sbShorten = new StringBuilder();
			boolean isLastCheck = false;

			for (int wordLength = maxWordLength - 1; wordLength >= 0; wordLength--) {

				if (wordLength == 0) {
					/*
					 * try to create a key without dashes and with only one character for each word
					 */
					isLastCheck = true;
					wordLength = 1;
				}
				// clear buffer
				sbShorten.setLength(0);

				boolean isFirstWord = true;
				int wordStartIndex = 0;
				int wordCharCounter = 0;

				for (int charIndex = 0; charIndex < originalId.length; charIndex++) {

					if (originalId[charIndex] == DASH) {

						// append dash for the subsequent words
						if (isFirstWord) {
							isFirstWord = false;
						} else {
							if (isLastCheck == false) {
								sbShorten.append(DASH);
							}
						}

						if (wordCharCounter > wordLength) {
							// shorten word
							sbShorten.append(sbId.subSequence(wordStartIndex, wordStartIndex + wordLength));
						} else {
							// use original word
							sbShorten.append(sbId.subSequence(wordStartIndex, wordStartIndex + wordCharCounter));
						}

						// set start for the next word
						wordStartIndex = charIndex + 1;

						wordCharCounter = 0;

					} else {

						wordCharCounter++;
					}
				}

				// check if the unique key is short enough
				if (sbShorten.length() < maxLength) {
					break;
				}

				if (isLastCheck) {
					break;
				}
			}

			if (sbShorten.length() > maxLength) {
				StatusUtil.showStatus(Messages.App_Error_TooManyWords, new Exception());
			}

			sbId.setLength(0);
			sbId.append(sbShorten);
		}

		return sbId.toString();
	}
}
