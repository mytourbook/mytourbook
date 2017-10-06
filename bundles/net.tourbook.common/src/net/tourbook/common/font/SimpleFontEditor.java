/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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

import net.tourbook.common.UI;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

public class SimpleFontEditor extends Composite {

	private final ListenerList	_fontListeners	= new ListenerList();

	private FontData[]			_selectedFontData;

	/*
	 * UI controls
	 */
	private Button				_btnChangeFont;

	private Label				_lblSelectedFont;

	private Font				_selectedFont;

	private Spinner				_spinnerFontSize;

	public SimpleFontEditor(final Composite parent, final int style) {

		super(parent, style);

		GridLayoutFactory.fillDefaults().applyTo(this);

		createUI();
	}

	public void addFontListener(final IFontEditorListener listener) {

		_fontListeners.add(listener);
	}

	private void createUI() {

		final PixelConverter pc = new PixelConverter(this);

		final Composite container = new Composite(this, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(5, 2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			{
				/*
				 * Button: Change
				 */
				final String buttonText = JFaceResources.getString("openChange"); //$NON-NLS-1$

				_btnChangeFont = new Button(container, SWT.PUSH);
				_btnChangeFont.setText(buttonText);
				_btnChangeFont.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent event) {
						onChangeFont();
					}
				});
			}
			{
				// Spinner
				_spinnerFontSize = new Spinner(container, SWT.BORDER);
				_spinnerFontSize.setMinimum(2);
				_spinnerFontSize.setMaximum(100);
				_spinnerFontSize.setPageIncrement(5);
				_spinnerFontSize.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onChangeFontSize();
					}
				});
				_spinnerFontSize.addMouseWheelListener(new MouseWheelListener() {

					@Override
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangeFontSize();
					}
				});

				GridDataFactory
						.fillDefaults()//
						.align(SWT.BEGINNING, SWT.CENTER)
						.applyTo(_spinnerFontSize);
			}
			{
				/*
				 * Font text
				 */
				_lblSelectedFont = new Label(container, SWT.LEFT);
				_lblSelectedFont.setFont(container.getFont());

				_lblSelectedFont.addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(final DisposeEvent e) {
						UI.disposeResource(_selectedFont);
					}
				});

				_lblSelectedFont.addMouseListener(new MouseAdapter() {

					@Override
					public void mouseDoubleClick(final MouseEvent e) {

						// modify font
						onChangeFont();
					}
				});

				GridDataFactory
						.fillDefaults()//
						//						.align(SWT.FILL, SWT.CENTER)
						.grab(true, false)
						.hint(pc.convertWidthInCharsToPixels(12), SWT.DEFAULT)
						.span(2, 1)
						.applyTo(_lblSelectedFont);
			}
		}
	}

	private void fireFontChanged(final FontData font) {

		FontData[] oldFont = _selectedFontData;

		if (oldFont == null) {
			oldFont = JFaceResources.getFontRegistry().defaultFont().getFontData();
		}

		final FontData[] newFontData = new FontData[1];
		newFontData[0] = font;

		updateFont(newFontData);

		final Object[] listeners = _fontListeners.getListeners();
		for (final Object listener : listeners) {

			final IFontEditorListener fontDialogListener = (IFontEditorListener) listener;

			SafeRunnable.run(new SafeRunnable() {
				@Override
				public void run() {
					fontDialogListener.fontSelected(font);
				}
			});
		}
	}

	/**
	 * Fire an open event that the dialog is opened or closes.
	 */
	private void fireOpenEvent(final boolean isOpened) {

		final Object[] listeners = _fontListeners.getListeners();

		for (final Object listener : listeners) {

			final IFontEditorListener fontDialogListener = (IFontEditorListener) listener;

			SafeRunnable.run(new SafeRunnable() {
				@Override
				public void run() {
					fontDialogListener.fontDialogOpened(isOpened);
				}
			});
		}
	}

	/**
	 * Get the system default font data.
	 * 
	 * @return FontData[]
	 */
	private FontData[] getDefaultFontData() {

		return getDisplay().getSystemFont().getFontData();
	}

	public FontData getSelection() {

		return _selectedFontData[0];
	}

	private void onChangeFont() {

		FontData selectedFont;

		final FontDialog fontDialog = new FontDialog(_btnChangeFont.getShell());

		fontDialog.setFontList(_selectedFontData);
		fontDialog.setEffectsVisible(false);

		fireOpenEvent(true);
		{
			selectedFont = fontDialog.open();
		}
		fireOpenEvent(false);

		if (selectedFont != null) {
			fireFontChanged(selectedFont);
		}
	}

	private void onChangeFontSize() {

		final FontData[] selectedFont = _selectedFontData;

		final FontData font = selectedFont[0];
		font.setHeight(_spinnerFontSize.getSelection());

		final FontData[] validFont = JFaceResources.getFontRegistry().filterData(selectedFont, getDisplay());

		fireFontChanged(validFont[0]);
	}

	public void removeOpenListener(final IFontEditorListener listener) {
		_fontListeners.remove(listener);
	}

	@Override
	public void setEnabled(final boolean isEnabled) {

		_btnChangeFont.setEnabled(isEnabled);
		_lblSelectedFont.setEnabled(isEnabled);
		_spinnerFontSize.setEnabled(isEnabled);

		super.setEnabled(isEnabled);
	}

	public void setSelection(final FontData fontData) {

		updateFont(new FontData[] { fontData });
	}

	/**
	 * Updates the change font button and the previewer to reflect the newly selected font.
	 * 
	 * @param font
	 *            The FontData[] to update with.
	 */
	private void updateFont(final FontData font[]) {

		FontData[] bestFont = JFaceResources.getFontRegistry().filterData(font, getDisplay());

		//if we have nothing valid do as best we can
		if (bestFont == null) {
			bestFont = getDefaultFontData();
		}

		//Now cache this value in the receiver
		_selectedFontData = bestFont;

		if (_lblSelectedFont != null) {

			final String fontText = StringConverter.asString(_selectedFontData[0]);

			_lblSelectedFont.setText(fontText);
			_lblSelectedFont.setToolTipText(fontText);

			UI.disposeResource(_selectedFont);

			_selectedFont = new Font(getDisplay(), _selectedFontData);
			_lblSelectedFont.setFont(_selectedFont);
			_spinnerFontSize.setSelection(_selectedFontData[0].getHeight());

			// ensure that the selected font is displayed
			_spinnerFontSize.getParent().layout(true, true);
			this.getShell().pack(true);
		}
	}
}
