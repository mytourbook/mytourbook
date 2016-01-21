package de.byteholder.geoclipse.map;

import java.io.File;

import net.tourbook.common.util.StatusUtil;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import de.byteholder.geoclipse.Messages;

public class UI {

	public static final String	EMPTY_STRING	= "";				//$NON-NLS-1$
	public static final String	SPACE			= " ";				//$NON-NLS-1$
	public static final char	DASH			= '-';
	public static final String	DASH_WITH_SPACE	= " - ";			//$NON-NLS-1$
	public static final String	KOMMA			= ",";				//$NON-NLS-1$
	public static final String	NEW_LINE		= "\n";			//$NON-NLS-1$
	public static final String	DOTS			= "...";			//$NON-NLS-1$
	public static final String	MBYTES			= "MByte";			//$NON-NLS-1$

	public static final String	UTF_8			= "UTF-8";			//$NON-NLS-1$

	/**
	 * THIS IS COPIED FROM org.eclipse.jface.dialogs.Dialog <br>
	 * <br>
	 * The dialog settings key name for stored dialog width.
	 * 
	 * @since 3.2
	 */
	public static final String	DIALOG_WIDTH	= "DIALOG_WIDTH";	//$NON-NLS-1$

	/**
	 * THIS IS COPIED FROM org.eclipse.jface.dialogs.Dialog <br>
	 * <br>
	 * The dialog settings key name for stored dialog height.
	 * 
	 * @since 3.2
	 */
	public static final String	DIALOG_HEIGHT	= "DIALOG_HEIGHT";	//$NON-NLS-1$

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
			for (final char element : originalId) {
				if (element == DASH) {
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

	/**
	 * Create a transparent image
	 * 
	 * @param imageSize
	 * @return
	 */
	public static ImageData createTransparentImageData(final int imageSize) {

		ImageData transparentImageData;

		if (net.tourbook.common.UI.IS_OSX) {

			// OSX
			transparentImageData = new ImageData(//
					imageSize,
					imageSize,
					32,
					getPaletteData());

		} else {

			// Win & Linux
			transparentImageData = new ImageData(//
					imageSize,
					imageSize,
					24,
					getPaletteData());
		}

		final RGB transparentRGB = Map.getTransparentRGB();

		transparentImageData.transparentPixel = transparentImageData.palette.getPixel(transparentRGB);

		setBackgroundColor(transparentRGB, transparentImageData);

		return transparentImageData;
	}

	public static PaletteData getPaletteData() {

		if (net.tourbook.common.UI.IS_OSX) {

			// OSX
			return new PaletteData(0xff0000, 0xff00, 0xff);

		} else {

			// Win & Linux
			return new PaletteData(0xff, 0xff00, 0xff0000);
		}
	}

	public static void setBackgroundColor(final RGB backgroundRGB, final ImageData imageData) {

		final byte blue = (byte) backgroundRGB.blue;
		final byte green = (byte) backgroundRGB.green;
		final byte red = (byte) backgroundRGB.red;

		final byte[] dstData = imageData.data;
		final int dstWidth = imageData.width;
		final int dstHeight = imageData.height;
		final int dstBytesPerLine = imageData.bytesPerLine;
		final int dstPixelBytes = imageData.depth == 32 ? 4 : 3;

		for (int dstY = 0; dstY < dstHeight; dstY++) {

			final int dstYBytesPerLine = dstY * dstBytesPerLine;

			for (int dstX = 0; dstX < dstWidth; dstX++) {

				final int dataIndex = dstYBytesPerLine + (dstX * dstPixelBytes);

				if (dstPixelBytes == 4) {
					dstData[dataIndex + 0] = (byte) (0x00);
					dstData[dataIndex + 1] = (byte) (red & 0xff);
					dstData[dataIndex + 2] = (byte) (green & 0xff);
					dstData[dataIndex + 3] = (byte) (blue & 0xff);
				} else {
					dstData[dataIndex + 0] = (byte) (blue & 0xff);
					dstData[dataIndex + 1] = (byte) (green & 0xff);
					dstData[dataIndex + 2] = (byte) (red & 0xff);
				}
			}
		}
	}
}
