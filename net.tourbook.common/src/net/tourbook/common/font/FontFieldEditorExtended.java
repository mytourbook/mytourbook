/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.common.font;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * This is a copy of {@link orgFontFieldEditor} with an open & close listener which is fired when
 * the dialog is opened or closes. There are also other adjustments.
 * <p>
 * This can be used to keep parent dialog opened when the font dialog is opened.
 */
public class FontFieldEditorExtended extends FieldEditor {

	private final ListenerList	_openListeners	= new ListenerList();

	/**
	 * The text for the change font button, or <code>null</code> if missing.
	 */
	private String				_buttonText;

	/**
	 * The text for the preview, or <code>null</code> if no preview is desired
	 */
	private String				_previewAreaText;

	/*
	 * UI controls
	 */
	/**
	 * The change font button, or <code>null</code> if none (before creation and after disposal).
	 */
	private Button				_btnChangeFont	= null;

	/**
	 * Font data for the chosen font button, or <code>null</code> if none.
	 */
	private FontData[]			chosenFont;

	/**
	 * Editor lable.
	 */
	private Label				_lblEditor;

	/**
	 * The label that displays the selected font, or <code>null</code> if none.
	 */
	private Label				_lblSelectedFont;

	/**
	 * The previewer, or <code>null</code> if none.
	 */
	private DefaultPreviewer	_fontPreviewer;

	/**
	 * Internal font previewer implementation.
	 */
	private class DefaultPreviewer {

		private String	_previewText;

		private Font	_previewFont;
		private Text	_txtPreviewText;

		/**
		 * Constructor for the previewer.
		 * 
		 * @param previewText
		 * @param parent
		 */
		public DefaultPreviewer(final String previewText, final Composite parent) {

			_previewText = previewText;

			_txtPreviewText = new Text(parent, SWT.READ_ONLY | SWT.BORDER | SWT.WRAP);
			_txtPreviewText.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(final DisposeEvent e) {
					if (_previewFont != null) {
						_previewFont.dispose();
					}
				}
			});

			if (_previewText != null) {
				_txtPreviewText.setText(_previewText);
			}
		}

		/**
		 * @return the control the previewer is using
		 */
		public Control getControl() {
			return _txtPreviewText;
		}

		public int getPreferredHeight() {
			return convertHorizontalDLUsToPixels(_txtPreviewText, 4 * 8);
		}

		/**
		 * @return the preferred size of the previewer.
		 */
		public int getPreferredWidth() {
			return convertHorizontalDLUsToPixels(_txtPreviewText, 40 * 4);
		}

		public void setEnabled(final boolean isEnabled) {
			_txtPreviewText.setEnabled(isEnabled);
		}

		/**
		 * Set the font to display with
		 * 
		 * @param fontData
		 */
		public void setFont(final FontData[] fontData) {

			if (_previewFont != null) {
				_previewFont.dispose();
			}

			_previewFont = new Font(_txtPreviewText.getDisplay(), fontData);
			_txtPreviewText.setFont(_previewFont);
		}
	}

	/**
	 * Creates a new font field editor
	 */
	protected FontFieldEditorExtended() {}

	/**
	 * Creates a font field editor without a preview.
	 * 
	 * @param name
	 *            the name of the preference this field editor works on
	 * @param labelText
	 *            the label text of the field editor
	 * @param parent
	 *            the parent of the field editor's control
	 */
	public FontFieldEditorExtended(final String name, final String labelText, final Composite parent) {
		this(name, labelText, null, parent);

	}

	/**
	 * Creates a font field editor with an optional preview area.
	 * 
	 * @param name
	 *            the name of the preference this field editor works on
	 * @param labelText
	 *            the label text of the field editor
	 * @param previewAreaText
	 *            the text used for the preview window. If it is <code>null</code> there will be no
	 *            preview area,
	 * @param parent
	 *            the parent of the field editor's control
	 */
	public FontFieldEditorExtended(	final String name,
									final String labelText,
									final String previewAreaText,
									final Composite parent) {
		init(name, labelText);

		_previewAreaText = previewAreaText;
		_buttonText = JFaceResources.getString("openChange"); //$NON-NLS-1$

		createControl(parent);
	}

	public void addOpenListener(final IFontDialogListener listener) {
		_openListeners.add(listener);
	}

	@Override
	protected void adjustForNumColumns(final int numColumns) {

		GridData data = new GridData();
		if (_lblSelectedFont.getLayoutData() != null) {
			data = (GridData) _lblSelectedFont.getLayoutData();
		}

		data.horizontalSpan = numColumns - getNumberOfControls() + 1;
		_lblSelectedFont.setLayoutData(data);
	}

	@Override
	protected void applyFont() {

		if (chosenFont != null && _fontPreviewer != null) {
			_fontPreviewer.setFont(chosenFont);
		}
	}

	@Override
	protected void doFillIntoGrid(final Composite parent, final int numColumns) {

		{
			// create editor label
			_lblEditor = getLabelControl(parent);

			final GridData gd = new GridData();
			gd.verticalAlignment = SWT.BEGINNING;

			_lblEditor.setLayoutData(gd);
		}

		{
			if (_previewAreaText != null) {

				_fontPreviewer = new DefaultPreviewer(_previewAreaText, parent);

				final GridData gd = new GridData(GridData.FILL_HORIZONTAL);
				gd.widthHint = _fontPreviewer.getPreferredWidth();
				gd.heightHint = _fontPreviewer.getPreferredHeight();
				_fontPreviewer.getControl().setLayoutData(gd);
			}
		}

		{
			_btnChangeFont = getChangeControl(parent);

			final int widthHint = convertHorizontalDLUsToPixels(_btnChangeFont, IDialogConstants.BUTTON_WIDTH);
			final int defaultWidth = _btnChangeFont.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x;

			final GridData gd = new GridData();
			gd.widthHint = Math.max(widthHint, defaultWidth);
			_btnChangeFont.setLayoutData(gd);
		}

		{
			_lblSelectedFont = getValueControl(parent);

			final GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
			gd.horizontalSpan = numColumns - getNumberOfControls() + 1;
			_lblSelectedFont.setLayoutData(gd);
		}
	}

	@Override
	protected void doLoad() {

		if (_btnChangeFont == null) {
			return;
		}

		updateFont(PreferenceConverter.getFontDataArray(getPreferenceStore(), getPreferenceName()));
	}

	@Override
	protected void doLoadDefault() {

		if (_btnChangeFont == null) {
			return;
		}

		updateFont(PreferenceConverter.getDefaultFontDataArray(getPreferenceStore(), getPreferenceName()));
	}

	@Override
	protected void doStore() {

		if (chosenFont != null) {
			PreferenceConverter.setValue(getPreferenceStore(), getPreferenceName(), chosenFont);
		}
	}

	/**
	 * Fire an open event that the dialog is opened or closes.
	 */
	private void fireOpenEvent(final boolean isOpened) {

		final Object[] listeners = _openListeners.getListeners();

		for (final Object listener : listeners) {

			final IFontDialogListener dialogListener = (IFontDialogListener) listener;

			SafeRunnable.run(new SafeRunnable() {
				@Override
				public void run() {
					dialogListener.fontDialogOpened(isOpened);
				}
			});
		}
	}

	/**
	 * Returns the change button for this field editor.
	 * 
	 * @param parent
	 *            The Composite to create the button in if required.
	 * @return the change button
	 */
	protected Button getChangeControl(final Composite parent) {

		if (_btnChangeFont == null) {

			_btnChangeFont = new Button(parent, SWT.PUSH);
			if (_buttonText != null) {
				_btnChangeFont.setText(_buttonText);
			}

			_btnChangeFont.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					final FontDialog fontDialog = new FontDialog(_btnChangeFont.getShell());
					if (chosenFont != null) {
						fontDialog.setFontList(chosenFont);
					}

					fireOpenEvent(true);

					final FontData font = fontDialog.open();

					fireOpenEvent(false);

					if (font != null) {

						FontData[] oldFont = chosenFont;
						if (oldFont == null) {
							oldFont = JFaceResources.getDefaultFont().getFontData();
						}
						setPresentsDefaultValue(false);

						final FontData[] newFontData = new FontData[1];
						newFontData[0] = font;
						updateFont(newFontData);

						fireValueChanged(VALUE, oldFont[0], font);
					}

				}
			});
			_btnChangeFont.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(final DisposeEvent event) {
					_btnChangeFont = null;
				}
			});
			_btnChangeFont.setFont(parent.getFont());
			setButtonLayoutData(_btnChangeFont);
		} else {
			checkParent(_btnChangeFont, parent);
		}
		return _btnChangeFont;
	}

	/**
	 * Get the system default font data.
	 * 
	 * @return FontData[]
	 */
	private FontData[] getDefaultFontData() {
		return _lblSelectedFont.getDisplay().getSystemFont().getFontData();
	}

	@Override
	public int getNumberOfControls() {

//		if (_fontPreviewer == null) {
//			return 3;
//		}
//
//		return 4;

		return 2;
	}

	/**
	 * Returns the preferred preview height.
	 * 
	 * @return the height, or <code>-1</code> if no previewer is installed
	 */
	public int getPreferredPreviewHeight() {

		if (_fontPreviewer == null) {
			return -1;
		}

		return _fontPreviewer.getPreferredHeight();
	}

	/**
	 * Returns the preview control for this field editor.
	 * 
	 * @return the preview control
	 */
	public Control getPreviewControl() {
		if (_fontPreviewer == null) {
			return null;
		}

		return _fontPreviewer.getControl();
	}

	/**
	 * Returns the value control for this field editor. The value control displays the currently
	 * selected font name.
	 * 
	 * @param parent
	 *            The Composite to create the viewer in if required
	 * @return the value control
	 */
	protected Label getValueControl(final Composite parent) {

		if (_lblSelectedFont == null) {
			_lblSelectedFont = new Label(parent, SWT.LEFT);
			_lblSelectedFont.setFont(parent.getFont());
			_lblSelectedFont.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(final DisposeEvent event) {
					_lblSelectedFont = null;
				}
			});
		} else {
			checkParent(_lblSelectedFont, parent);
		}

		return _lblSelectedFont;
	}

	public void removeOpenListener(final IFontDialogListener listener) {
		_openListeners.remove(listener);
	}

	/**
	 * Sets the text of the change button.
	 * 
	 * @param text
	 *            the new text
	 */
	public void setChangeButtonText(final String text) {

		Assert.isNotNull(text);

		_buttonText = text;
		if (_btnChangeFont != null) {
			_btnChangeFont.setText(text);
		}
	}

	@Override
	public void setEnabled(final boolean isEnabled, final Composite parent) {

		super.setEnabled(isEnabled, parent);

		getChangeControl(parent).setEnabled(isEnabled);
		getValueControl(parent).setEnabled(isEnabled);

		if (_fontPreviewer != null) {
			_fontPreviewer.setEnabled(isEnabled);
		}
	}

	/**
	 * Set indent for the first column fields.
	 * 
	 * @param horizontalIndent
	 * @param verticalIndent
	 */
	public void setFirstColumnIndent(final int horizontalIndent, final int verticalIndent) {

		GridData gd = (GridData) _lblEditor.getLayoutData();
		gd.horizontalIndent = horizontalIndent;
		gd.verticalIndent = verticalIndent;

		gd = (GridData) _btnChangeFont.getLayoutData();
		gd.horizontalIndent = horizontalIndent;
		gd.verticalIndent = verticalIndent;
	}

	/**
	 * Store the default preference for the field being edited
	 */
	protected void setToDefault() {

		final FontData[] defaultFontData = PreferenceConverter.getDefaultFontDataArray(
				getPreferenceStore(),
				getPreferenceName());

		PreferenceConverter.setValue(getPreferenceStore(), getPreferenceName(), defaultFontData);
	}

	/**
	 * Updates the change font button and the previewer to reflect the newly selected font.
	 * 
	 * @param font
	 *            The FontData[] to update with.
	 */
	private void updateFont(final FontData font[]) {

		FontData[] bestFont = JFaceResources.getFontRegistry().filterData(font, _lblSelectedFont.getDisplay());

		//if we have nothing valid do as best we can
		if (bestFont == null) {
			bestFont = getDefaultFontData();
		}

		//Now cache this value in the receiver
		this.chosenFont = bestFont;

		if (_lblSelectedFont != null) {
			_lblSelectedFont.setText(StringConverter.asString(chosenFont[0]));
		}
		if (_fontPreviewer != null) {
			_fontPreviewer.setFont(bestFont);
		}
	}
}
