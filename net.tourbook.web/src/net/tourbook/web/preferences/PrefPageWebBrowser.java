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
package net.tourbook.web.preferences;

import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.web.Messages;
import net.tourbook.web.WEB;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageWebBrowser extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String	ID		= "net.tourbook.web.preferences.PrefPageWebBrowser";	//$NON-NLS-1$

	private IDialogSettings		_state	= WEB.getState();

	private PixelConverter		_pc;

	/*
	 * UI controls
	 */
	private Button				_chkUseExternalWebBrowser;

	private Label				_lblHint;
	private Label				_lblInfo;

	private Text				_txtWebBrowser;

	public PrefPageWebBrowser() {
//		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(final Composite parent) {

		final Composite ui = createUI(parent);

		restoreState();

		enableControls();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			/*
			 * label: info
			 */
			_lblInfo = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.hint(UI.DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
					.applyTo(_lblInfo);
			_lblInfo.setText(Messages.PrefPage_Web_Label_ExternalWebBrowser_Info);

			/*
			 * Checkbox: Use external webbrowser
			 */
			_chkUseExternalWebBrowser = new Button(container, SWT.CHECK);
			GridDataFactory
					.fillDefaults()
					.indent(0, _pc.convertHorizontalDLUsToPixels(4))
					.applyTo(_chkUseExternalWebBrowser);
			_chkUseExternalWebBrowser.setText(Messages.PrefPage_Web_Checkbox_ExternalWebBrowser);
			_chkUseExternalWebBrowser.setToolTipText(UI.IS_WIN
					? Messages.PrefPage_Web_Checkbox_ExternalWebBrowser_Tooltip_Win
					: Messages.PrefPage_Web_Checkbox_ExternalWebBrowser_Tooltip_Linux);

			_chkUseExternalWebBrowser.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectExternalWebBrowser();
				}
			});

			final Composite browserContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.indent(_pc.convertWidthInCharsToPixels(3), 0)
					.applyTo(browserContainer);
			GridLayoutFactory.fillDefaults().applyTo(browserContainer);
			{
				/*
				 * Command line + parameters
				 */
				_txtWebBrowser = new Text(browserContainer, SWT.BORDER
						| SWT.WRAP
						| SWT.MULTI
						| SWT.V_SCROLL
						| SWT.H_SCROLL);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.hint(SWT.DEFAULT, _pc.convertHeightInCharsToPixels(10))
						.applyTo(_txtWebBrowser);

				/*
				 * Label: Hint
				 */
				_lblHint = new Label(browserContainer, SWT.WRAP);
				GridDataFactory.fillDefaults()//
						.hint(UI.DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
						.applyTo(_lblHint);

				_lblHint.setText(UI.IS_WIN
						? Messages.PrefPage_Web_Label_ExternalWebBrowser_Hint_Win
						: Messages.PrefPage_Web_Label_ExternalWebBrowser_Hint_Linux);
			}
		}

		return container;
	}

	private void enableControls() {

		final boolean useExternalWebBrowser = _chkUseExternalWebBrowser.getSelection();

		_lblHint.setEnabled(useExternalWebBrowser);
		_txtWebBrowser.setEnabled(useExternalWebBrowser);
	}

	@Override
	public void init(final IWorkbench workbench) {

	}

	@Override
	public boolean okToLeave() {

		return super.okToLeave();
	}

	private void onSelectExternalWebBrowser() {

		enableControls();
	}

	@Override
	protected void performDefaults() {

		_txtWebBrowser.setText(WEB.STATE_EXTERNAL_WEB_BROWSER_DEFAULT);
		_chkUseExternalWebBrowser.setSelection(WEB.STATE_USE_EXTERNAL_WEB_BROWSER_DEFAULT);

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();
		if (isOK) {
			saveState();
		}

		return isOK;
	}

	private void restoreState() {

		_txtWebBrowser.setText(Util.getStateString(
				_state,
				WEB.STATE_EXTERNAL_WEB_BROWSER,
				WEB.STATE_EXTERNAL_WEB_BROWSER_DEFAULT));

		_chkUseExternalWebBrowser.setSelection(Util.getStateBoolean(
				_state,
				WEB.STATE_USE_EXTERNAL_WEB_BROWSER,
				WEB.STATE_USE_EXTERNAL_WEB_BROWSER_DEFAULT));
	}

	private void saveState() {

		_state.put(WEB.STATE_EXTERNAL_WEB_BROWSER, _txtWebBrowser.getText());
		_state.put(WEB.STATE_USE_EXTERNAL_WEB_BROWSER, _chkUseExternalWebBrowser.getSelection());
	}
}
