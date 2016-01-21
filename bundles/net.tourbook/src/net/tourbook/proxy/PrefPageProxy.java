/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
 ********************************************************************************
 *
 * @author Meinhard Ritscher
 *
 ********************************************************************************

 This class implements a preference page for proxy settings. Useful for
 running proxy behind a (company firewalling) proxy

 *******************************************************************************/

package net.tourbook.proxy;

import net.tourbook.application.ApplicationWorkbenchWindowAdvisor;
import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageProxy extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String	ID				= "net.tourbook.preferences.PrefPageProxyId";			//$NON-NLS-1$

	private IPreferenceStore	_prefStore		= TourbookPlugin.getDefault().getPreferenceStore();

	private boolean				_isJVMSystem	= DefaultProxySelector.willJvmRetrieveSystemProxies();

	private Button				_rdoNoProxy;
	private Button				_rdoSystemProxy;
	private Button				_rdoProxy;
	private Button				_rdoSocksProxy;

	private Label				_lblSystemProxyInfo;

	private Label				_lblProxyServer;
	private Label				_lblProxyPort;
	private Label				_lblProxyCredentials;
	private Label				_lblProxyUser;
	private Label				_lblProxyPassword;
	private Text				_txtProxyServer;
	private Text				_txtProxyPort;
	private Text				_txtProxyUser;
	private Text				_txtProxyPassword;

	private Label				_lblSocksServer;
	private Label				_lblSocksPort;
	private Text				_txtSocksProxyServer;
	private Text				_txtSocksProxyPort;

	@Override
	protected Control createContents(final Composite parent) {

		final Control ui = createUI(parent);

		restoreState();

		enableControls();

		return ui;
	}

	private Control createUI(final Composite parent) {

		initializeDialogUnits(parent);

		final SelectionAdapter selectListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableControls();
				validateData();
			}
		};

		final ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				validateData();
			}
		};

		final int infoHint = convertWidthInCharsToPixels(60);
		final int fieldWidth = convertWidthInCharsToPixels(25);
		final int proxyVIndent = 5;
		final int defaultHIndent = 16;

		final GridDataFactory gdLabel = GridDataFactory.fillDefaults()//
				.indent(defaultHIndent, 0)
				.align(SWT.FILL, SWT.CENTER);

		final GridDataFactory gdRadio = GridDataFactory.fillDefaults()//
				.grab(true, false)
				.indent(0, proxyVIndent)
				.span(2, 1);

		final GridDataFactory gdServer = GridDataFactory.fillDefaults();
		final GridDataFactory gdField = GridDataFactory.swtDefaults().hint(fieldWidth, SWT.DEFAULT);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * no proxy
			 */
			_rdoNoProxy = new Button(container, SWT.RADIO);
			_rdoNoProxy.setText(Messages.prefPage_proxy_radio_noProxy);
			_rdoNoProxy.addSelectionListener(selectListener);
			GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_rdoNoProxy);

			/*
			 * system proxy
			 */
			_rdoSystemProxy = new Button(container, SWT.RADIO);
			_rdoSystemProxy.setText(Messages.prefPage_proxy_radio_systemProxy);
			_rdoSystemProxy.addSelectionListener(selectListener);
			gdRadio.applyTo(_rdoSystemProxy);
			{
				_lblSystemProxyInfo = new Label(container, SWT.WRAP);
				GridDataFactory.fillDefaults() //
						.grab(true, false)
						.span(2, 1)
						.indent(defaultHIndent, 0)
						.hint(infoHint, SWT.DEFAULT)
						.applyTo(_lblSystemProxyInfo);
			}

			/*
			 * proxy
			 */
			_rdoProxy = new Button(container, SWT.RADIO);
			_rdoProxy.setText(Messages.prefPage_proxy_radio_Proxy);
			_rdoProxy.addSelectionListener(selectListener);
			gdRadio.applyTo(_rdoProxy);
			{
				/*
				 * proxy server
				 */
				_lblProxyServer = new Label(container, SWT.NONE);
				_lblProxyServer.setText(Messages.prefPage_proxy_ProxyServer);
				gdLabel.applyTo(_lblProxyServer);

				_txtProxyServer = new Text(container, SWT.BORDER);
				_txtProxyServer.addModifyListener(modifyListener);
				gdServer.applyTo(_txtProxyServer);

				/*
				 * proxy port
				 */
				_lblProxyPort = new Label(container, SWT.NONE);
				_lblProxyPort.setText(Messages.prefPage_proxy_ProxyPort);
				gdLabel.applyTo(_lblProxyPort);

				_txtProxyPort = new Text(container, SWT.BORDER);
				_txtProxyPort.addModifyListener(modifyListener);
				gdField.applyTo(_txtProxyPort);

				/*
				 * proxy authentification
				 */
				_lblProxyCredentials = new Label(container, SWT.NONE);
				_lblProxyCredentials.setText(Messages.http_ProxyCredentials);
				_lblProxyCredentials.setToolTipText(Messages.http_ProxyCredentialsToolTip);
				GridDataFactory.fillDefaults().span(2, 1).indent(defaultHIndent, 0).applyTo(_lblProxyCredentials);

				/*
				 * proxy user
				 */
				_lblProxyUser = new Label(container, SWT.NONE);
				_lblProxyUser.setText(Messages.prefPage_proxy_ProxyUser);
				gdLabel.applyTo(_lblProxyUser);

				_txtProxyUser = new Text(container, SWT.BORDER);
				_txtProxyUser.addModifyListener(modifyListener);
				gdField.applyTo(_txtProxyUser);

				/*
				 * proxy password
				 */
				_lblProxyPassword = new Label(container, SWT.NONE);
				_lblProxyPassword.setText(Messages.prefPage_proxy_ProxyPassword);
				gdLabel.applyTo(_lblProxyPassword);

				_txtProxyPassword = new Text(container, SWT.SINGLE | SWT.BORDER);
				_txtProxyPassword.setEchoChar('*');
				_txtProxyPassword.addModifyListener(modifyListener);
				gdField.applyTo(_txtProxyPassword);
			}

			/*
			 * socks proxy
			 */
			_rdoSocksProxy = new Button(container, SWT.RADIO);
			_rdoSocksProxy.setText(Messages.prefPage_proxy_radio_socksProxy);
			_rdoSocksProxy.addSelectionListener(selectListener);
			gdRadio.applyTo(_rdoSocksProxy);
			{
				/*
				 * socks server
				 */
				_lblSocksServer = new Label(container, SWT.NONE);
				_lblSocksServer.setText(Messages.prefPage_proxy_SocksProxyServer);
				gdLabel.applyTo(_lblSocksServer);

				_txtSocksProxyServer = new Text(container, SWT.BORDER);
				_txtSocksProxyServer.addModifyListener(modifyListener);
				gdServer.applyTo(_txtSocksProxyServer);

				/*
				 * socks port
				 */
				_lblSocksPort = new Label(container, SWT.NONE);
				_lblSocksPort.setText(Messages.prefPage_proxy_SocksProxyPort);
				gdLabel.applyTo(_lblSocksPort);

				_txtSocksProxyPort = new Text(container, SWT.BORDER);
				_txtSocksProxyPort.addModifyListener(modifyListener);
				gdField.applyTo(_txtSocksProxyPort);
			}
		}

		return container;
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return _prefStore;
	}

	private void enableControls() {

		final boolean isSystemProxy = _rdoSystemProxy.getSelection();
		final boolean isProxy = _rdoProxy.getSelection();
		final boolean isSocks = _rdoSocksProxy.getSelection();

		_rdoSystemProxy.setEnabled(_isJVMSystem);
		_lblSystemProxyInfo.setEnabled(_isJVMSystem && isSystemProxy);

		_lblProxyServer.setEnabled(isProxy);
		_lblProxyPort.setEnabled(isProxy);
		_lblProxyCredentials.setEnabled(isProxy);
		_lblProxyUser.setEnabled(isProxy);
		_lblProxyPassword.setEnabled(isProxy);
		_txtProxyServer.setEnabled(isProxy);
		_txtProxyPort.setEnabled(isProxy);
		_txtProxyUser.setEnabled(isProxy);
		_txtProxyPassword.setEnabled(isProxy);

		_lblSocksPort.setEnabled(isSocks);
		_lblSocksServer.setEnabled(isSocks);
		_txtSocksProxyServer.setEnabled(isSocks);
		_txtSocksProxyPort.setEnabled(isSocks);
	}

	@Override
	public void init(final IWorkbench workbench) {}

	@Override
	public boolean okToLeave() {
		if (validateData() == false) {
			return false;
		}

		return super.okToLeave();
	}

	@Override
	public boolean performCancel() {
		return super.performCancel();
	}

	@Override
	protected void performDefaults() {

		if (_isJVMSystem) {
			_rdoSystemProxy.setSelection(true);
			_rdoNoProxy.setSelection(false);
		} else {
			_rdoSystemProxy.setSelection(false);
			_rdoNoProxy.setSelection(true);
		}

		_rdoProxy.setSelection(false);
		_rdoSocksProxy.setSelection(false);

		enableControls();

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		if (validateData() == false) {
			return false;
		}
		saveState();

		return super.performOk();
	}

	private void restoreState() {

		final String proxy = _prefStore.getString(IPreferences.PROXY_METHOD);

		if (proxy.equals(IPreferences.NO_PROXY)) {
			_rdoNoProxy.setSelection(true);
		} else if (proxy.equals(IPreferences.PROXY)) {
			_rdoProxy.setSelection(true);
		} else if (proxy.equals(IPreferences.SOCKS_PROXY)) {
			_rdoSocksProxy.setSelection(true);
		} else if (proxy.equals(IPreferences.SYSTEM_PROXY)) {
			_rdoSystemProxy.setSelection(true);
		} else {
			_rdoNoProxy.setSelection(true);
		}

		_txtProxyServer.setText(_prefStore.getString(IPreferences.PROXY_SERVER_ADDRESS));
		_txtProxyPort.setText(_prefStore.getString(IPreferences.PROXY_SERVER_PORT));
		_txtProxyUser.setText(_prefStore.getString(IPreferences.PROXY_USER));
		_txtProxyPassword.setText(_prefStore.getString(IPreferences.PROXY_PWD));
		_txtSocksProxyServer.setText(_prefStore.getString(IPreferences.SOCKS_PROXY_SERVER_ADDRESS));
		_txtSocksProxyPort.setText(_prefStore.getString(IPreferences.SOCKS_PROXY_SERVER_PORT));

		/*
		 * setup system proxy
		 */
		if (_isJVMSystem) {
			_lblSystemProxyInfo.setText(Messages.systemProxyInformationActivated);
		} else {
			_lblSystemProxyInfo.setText(Messages.systemProxyInformationDeactivated);
			_lblSystemProxyInfo.setToolTipText(Messages.systemProxyInformationDeactivatedToolTip);
		}
	}

	private void saveState() {

		if (_rdoNoProxy.getSelection()) {
			_prefStore.setValue(IPreferences.PROXY_METHOD, IPreferences.NO_PROXY);
		} else if (_rdoProxy.getSelection()) {
			_prefStore.setValue(IPreferences.PROXY_METHOD, IPreferences.PROXY);
		} else if (_rdoSocksProxy.getSelection()) {
			_prefStore.setValue(IPreferences.PROXY_METHOD, IPreferences.SOCKS_PROXY);
		} else if (_rdoSystemProxy.getSelection()) {
			_prefStore.setValue(IPreferences.PROXY_METHOD, IPreferences.SYSTEM_PROXY);
		}

		_prefStore.setValue(IPreferences.PROXY_SERVER_ADDRESS, _txtProxyServer.getText().trim());
		_prefStore.setValue(IPreferences.PROXY_SERVER_PORT, _txtProxyPort.getText().trim());
		_prefStore.setValue(IPreferences.PROXY_USER, _txtProxyUser.getText().trim());
		_prefStore.setValue(IPreferences.PROXY_PWD, _txtProxyPassword.getText().trim());
		_prefStore.setValue(IPreferences.SOCKS_PROXY_SERVER_ADDRESS, _txtSocksProxyServer.getText().trim());
		_prefStore.setValue(IPreferences.SOCKS_PROXY_SERVER_PORT, _txtSocksProxyPort.getText().trim());

		ApplicationWorkbenchWindowAdvisor.setupProxy();
	}

	private boolean validateData() {

		boolean isValid = true;

		if (_rdoProxy.getSelection()) {
			if (DefaultProxySelector.parseProxyPortValue(_txtProxyPort.getText()) == 0) {
				isValid = false;
				// ToDo: check for an valid url or IP?
			}
		} else if (_rdoProxy.getSelection()) {
			if (DefaultProxySelector.parseProxyPortValue(_txtSocksProxyPort.getText()) == 0) {
				isValid = false;
				// ToDo: check for an valid url or IP?
			}
		}

		if (isValid) {
			setErrorMessage(null);
		}
		setValid(isValid);

		return isValid;
	}

}
