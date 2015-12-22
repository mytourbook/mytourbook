/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.rawData;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CopyOnWriteArrayList;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.importdata.ImportLog;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.web.WEB;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

/**
 * Import log view.
 */
public class ImportLogView extends ViewPart {

	public static final String		ID									= "net.tourbook.views.rawData.ImportLogView";	//$NON-NLS-1$
	//
	private static final String		WEB_RESOURCE_TOUR_IMPORT_LOG_CSS	= "tour-import-log.css";						//$NON-NLS-1$
	private static final String		DOM_ID_LOG							= "logs";
	//
	private boolean					_isBrowserCompleted;
	private String					_cssFromFile;

	private final DateTimeFormatter	_dtFormatterTime					= new DateTimeFormatterBuilder()
																				.appendHourOfDay(2)
																				.appendLiteral(':')
																				.appendMinuteOfHour(2)
																				.appendLiteral(':')
																				.appendSecondOfMinute(2)
																				.appendLiteral(',')
																				.appendFractionOfSecond(3, 3)
																				.toFormatter();

	/*
	 * UI controls
	 */
	private Browser					_browser;

	private PageBook				_pageBook;
	private Composite				_page_NoBrowser;
	private Composite				_page_WithBrowser;

	private Text					_txtNoBrowser;

	public void addLog(final ImportLog importLog) {

		if (_isBrowserCompleted == false) {
			// this occures when the view is opening but not yet ready
			return;
		}

		String jsText = UI.replaceJS_BackSlash(importLog.message);
		jsText = UI.replaceJS_Apostrophe(jsText);

		final String message = _dtFormatterTime.print(System.currentTimeMillis()) + UI.SPACE + jsText;

		if (_browser == null) {
			StatusUtil.logInfo(message);
			return;
		}

		final String js = UI.EMPTY_STRING//

				+ ("var para = document.createElement('P');\n")
				+ ("para.className='logItem';\n")
				+ ("para.innerHTML='" + message + "';\n")

				+ ("document.getElementById(\"" + DOM_ID_LOG + "\").appendChild(para);\n") //$NON-NLS-1$ //$NON-NLS-2$
		;

		final Display display = Display.getDefault();

		if (Thread.currentThread() == display.getThread()) {

			_browser.execute(js);

		} else {

			display.asyncExec(new Runnable() {
				@Override
				public void run() {

					_browser.execute(js);
				}
			});
		}

	}

	/**
	 * Clear browser.
	 */
	public void clear() {
		updateUI_InitBrowser();
	}

	private String createHTML() {

		final String html = "" // //$NON-NLS-1$
				+ "<!DOCTYPE html>\n" // ensure that IE is using the newest version and not the quirk mode //$NON-NLS-1$
				+ "<html style='height: 100%; width: 100%; margin: 0px; padding: 0px;'>\n" //$NON-NLS-1$
				+ ("<head>\n" + createHTML_10_Head() + "\n</head>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("<body>\n" + createHTML_20_Body() + "\n</body>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ "</html>"; //$NON-NLS-1$

		return html;
	}

	private String createHTML_10_Head() {

		final String html = ""// //$NON-NLS-1$
				+ "	<meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />\n" //$NON-NLS-1$
				+ "	<meta http-equiv='X-UA-Compatible' content='IE=edge' />\n" //$NON-NLS-1$
				+ _cssFromFile
				+ "\n"; //$NON-NLS-1$

		return html;
	}

	private String createHTML_20_Body() {

		final StringBuilder sb = new StringBuilder();

		sb.append("<div id='" + DOM_ID_LOG + "' class=''>\n"); //$NON-NLS-1$
		sb.append("</div>\n"); //$NON-NLS-1$

		return sb.toString();
	}

	@Override
	public void createPartControl(final Composite parent) {

		initUI(parent);

		createUI(parent);

		updateUI();
	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook);

		_page_NoBrowser = new Composite(_pageBook, SWT.NONE);
		_page_NoBrowser.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_page_NoBrowser);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_page_NoBrowser);
		{
			_txtNoBrowser = new Text(_page_NoBrowser, SWT.WRAP | SWT.READ_ONLY);
			_txtNoBrowser.setText(Messages.UI_Label_BrowserCannotBeCreated);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.align(SWT.FILL, SWT.BEGINNING)
					.applyTo(_txtNoBrowser);
		}

		_page_WithBrowser = new Composite(_pageBook, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_page_WithBrowser);
		{
			createUI_10_Browser(_page_WithBrowser);
		}
	}

	private void createUI_10_Browser(final Composite parent) {

		try {

			try {

				// use default browser
				_browser = new Browser(parent, SWT.NONE);

				// initial setup
				_browser.setRedraw(false);

			} catch (final Exception e) {

				/*
				 * Use mozilla browser, this is necessary for Linux when default browser fails
				 * however the XULrunner needs to be installed.
				 */
				_browser = new Browser(parent, SWT.MOZILLA);
			}

			GridDataFactory.fillDefaults().grab(true, true).applyTo(_browser);

			_browser.addProgressListener(new ProgressAdapter() {
				@Override
				public void completed(final ProgressEvent event) {

					onBrowser_Completed(event);
				}
			});

		} catch (final SWTError e) {

			_txtNoBrowser.setText(NLS.bind(Messages.UI_Label_BrowserCannotBeCreated_Error, e.getMessage()));
		}
	}

	private void initUI(final Composite parent) {

		/*
		 * Webpage css
		 */
		try {

			final File webFile = WEB.getResourceFile(WEB_RESOURCE_TOUR_IMPORT_LOG_CSS);
			final String css = Util.readContentFromFile(webFile.getAbsolutePath());

			_cssFromFile = ""// //$NON-NLS-1$
					+ "<style>\n" //$NON-NLS-1$
					+ css
					+ "</style>\n"; //$NON-NLS-1$

		} catch (IOException | URISyntaxException e) {
			StatusUtil.log(e);
		}

	}

	private void onBrowser_Completed(final ProgressEvent event) {

		_isBrowserCompleted = true;

		// a redraw MUST be done otherwise nothing is displayed
		_browser.setRedraw(true);

		// show already logged items
		final CopyOnWriteArrayList<ImportLog> importLogs = RawDataManager.getImportLogs();
		for (final ImportLog importLog : importLogs) {
			addLog(importLog);
		}
	}

	@Override
	public void setFocus() {

		if (_browser != null) {
			_browser.setFocus();
		}
	}

	/**
	 * Set/create dashboard page.
	 */
	private void updateUI() {

		final boolean isBrowserAvailable = _browser != null;

		// set dashboard page
		_pageBook.showPage(isBrowserAvailable//
				? _page_WithBrowser
				: _page_NoBrowser);

		if (!isBrowserAvailable) {
			return;
		}

		updateUI_InitBrowser();
	}

	private void updateUI_InitBrowser() {

		if (_browser.isDisposed()) {
			return;
		}

		final String html = createHTML();

		_isBrowserCompleted = false;

		_browser.setText(html);
	}

}
