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
package net.tourbook.search;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.part.ViewPart;

public class BrowserView extends ViewPart {

	public static final String	ID	= "net.tourbook.search.BrowserView";	//$NON-NLS-1$

	private Browser				_browser;

	/*
	 * UI controls
	 */
//	private Browser				_browser;

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		_browser.setUrl("http://dojotoolkit.org/api/");

	}

	private void createUI(final Composite parent) {

		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		parent.setLayout(gridLayout);
		final ToolBar toolbar = new ToolBar(parent, SWT.NONE);
		final ToolItem itemBack = new ToolItem(toolbar, SWT.PUSH);
		itemBack.setText("Back");
		final ToolItem itemForward = new ToolItem(toolbar, SWT.PUSH);
		itemForward.setText("Forward");
		final ToolItem itemStop = new ToolItem(toolbar, SWT.PUSH);
		itemStop.setText("Stop");
		final ToolItem itemRefresh = new ToolItem(toolbar, SWT.PUSH);
		itemRefresh.setText("Refresh");
		final ToolItem itemGo = new ToolItem(toolbar, SWT.PUSH);
		itemGo.setText("Go");

		GridData data = new GridData();
		data.horizontalSpan = 3;
		toolbar.setLayoutData(data);

		final Label labelAddress = new Label(parent, SWT.NONE);
		labelAddress.setText("Address");

		final Text location = new Text(parent, SWT.BORDER);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		data.grabExcessHorizontalSpace = true;
		location.setLayoutData(data);

		try {

//			_browser = new Browser(parent, SWT.NONE);

			System.setProperty("org.eclipse.swt.browser.XULRunnerPath", "C:\\E\\XULRunner\\xulrunner-10-32");
			_browser = new Browser(parent, SWT.MOZILLA);

		} catch (final SWTError e) {
			System.out.println("Could not instantiate Browser: " + e.getMessage());
			return;
		}

		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		data.horizontalSpan = 3;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		_browser.setLayoutData(data);

		final Label status = new Label(parent, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		status.setLayoutData(data);

		final ProgressBar progressBar = new ProgressBar(parent, SWT.NONE);
		data = new GridData();
		data.horizontalAlignment = GridData.END;
		progressBar.setLayoutData(data);

		/* event handling */
		final Listener listener = new Listener() {
			@Override
			public void handleEvent(final Event event) {
				final ToolItem item = (ToolItem) event.widget;
				final String string = item.getText();
				if (string.equals("Back")) {
					_browser.back();
				} else if (string.equals("Forward")) {
					_browser.forward();
				} else if (string.equals("Stop")) {
					_browser.stop();
				} else if (string.equals("Refresh")) {
					_browser.refresh();
				} else if (string.equals("Go")) {
					_browser.setUrl(location.getText());
				}
			}
		};
		_browser.addProgressListener(new ProgressListener() {
			@Override
			public void changed(final ProgressEvent event) {
				if (event.total == 0) {
					return;
				}
				final int ratio = event.current * 100 / event.total;
				progressBar.setSelection(ratio);
			}

			@Override
			public void completed(final ProgressEvent event) {
				progressBar.setSelection(0);
			}
		});
		_browser.addStatusTextListener(new StatusTextListener() {
			@Override
			public void changed(final StatusTextEvent event) {
				status.setText(event.text);
			}
		});
		_browser.addLocationListener(new LocationListener() {
			@Override
			public void changed(final LocationEvent event) {
				if (event.top) {
					location.setText(event.location);
				}
			}

			@Override
			public void changing(final LocationEvent event) {}
		});
		itemBack.addListener(SWT.Selection, listener);
		itemForward.addListener(SWT.Selection, listener);
		itemStop.addListener(SWT.Selection, listener);
		itemRefresh.addListener(SWT.Selection, listener);
		itemGo.addListener(SWT.Selection, listener);
		location.addListener(SWT.DefaultSelection, new Listener() {
			@Override
			public void handleEvent(final Event e) {
				_browser.setUrl(location.getText());
			}
		});
	}

	private void createUI_30_Browser(final Composite parent) {

//		try {
//
//			try {
//
//				final boolean IS_FORCE_XULRUNNER = true;
//
//				if (IS_FORCE_XULRUNNER) {
//
////					System.setProperty("org.eclipse.swt.browser.XULRunnerPath", "C:\\E\\XULRunner\\xulrunner");
////					System.setProperty("org.eclipse.swt.browser.XULRunnerPath", "C:\\E\\XULRunner\\xulrunner-10-64xxx");
////					-Dorg.eclipse.swt.browser.XULRunnerPath=...
//
//					System.setProperty("org.eclipse.swt.browser.XULRunnerPath", "C:\\E\\XULRunner\\xulrunner-10-32");
//					_browser = new Browser(parent, SWT.MOZILLA);
//
//				} else {
//
//					// use default browser
//					_browser = new Browser(parent, SWT.NONE);
//
//					// DEBUG: force error in win7
////					_browser = new Browser(parent, SWT.MOZILLA);
//				}
//
//			} catch (final Exception e) {
//
//				/**
//				 * Use mozilla browser, this is necessary for Linux when default browser fails
//				 * however the XULrunner needs to be installed.
//				 * <p>
//				 * e.g. for Eclipse 3.8.2
//				 *
//				 * <pre>
//				 *
//				 * XURL=https://ftp.mozilla.org/pub/mozilla.org/xulrunner/releases/10.0.2/runtimes/xulrunner-10.0.2.en-US.linux-x86_64.tar.bz2
//				 * cd /opt
//				 * sudo sh -c "wget -O- $XURL | tar -xj"
//				 * sudo ln -s /opt/xulrunner/xulrunner /usr/bin/xulrunner
//				 * sudo ln -s /opt/xulrunner/xpcshell /usr/bin/xpcshell
//				 *
//				 * </pre>
//				 */
//				_browser = new Browser(parent, SWT.MOZILLA);
//
//			}
//
//			GridDataFactory.fillDefaults().grab(true, true).applyTo(_browser);
//
//			_browser.addLocationListener(new LocationAdapter() {
//				@Override
//				public void changing(final LocationEvent event) {
//					onBrowserLocation(event);
//				}
//			});
//
//			_browser.addProgressListener(new ProgressAdapter() {
//				@Override
//				public void completed(final ProgressEvent event) {
//					onBrowserCompleted(event);
//				}
//			});
//
//			setupBrowser();
//
//		} catch (final SWTError e) {
//
//			StatusUtil.log(e);
//		}
	}

	@Override
	public void setFocus() {

	}

}
