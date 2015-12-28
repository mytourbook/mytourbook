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
package net.tourbook.tour;

import static net.tourbook.ui.UI.getIconUrl;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CopyOnWriteArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.web.WEB;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

/**
 * Tour log view.
 */
public class TourLogView extends ViewPart {

	public static final String	ID									= "net.tourbook.tour.TourLogView";					//$NON-NLS-1$
	//
	private static final String	STATE_COPY							= "COPY";											//$NON-NLS-1$
	private static final String	STATE_DELETE						= "DELETE";										//$NON-NLS-1$
	private static final String	STATE_ERROR							= "ERROR";											//$NON-NLS-1$
	private static final String	STATE_OK							= "OK";											//$NON-NLS-1$
	private static final String	STATE_SAVE							= "SAVE";											//$NON-NLS-1$
	//
	public static final String	CSS_LOG_TITLE						= "title";
	private static final String	CSS_LOG_SUB_ITEM					= "subItem";
	//
	private static final String	DOM_ID_LOG							= "logs";											//$NON-NLS-1$
	private static final String	WEB_RESOURCE_TOUR_IMPORT_LOG_CSS	= "tour-import-log.css";							//$NON-NLS-1$
	//
	private IPartListener2		_partListener;
	//
	private Action				_actionReset;
	//
	private boolean				_isBrowserCompleted;
	private String				_cssFromFile;
	private String				_noBrowserLog						= UI.EMPTY_STRING;
	//
	private String				_imageUrl_StateCopy					= getIconUrl(Messages.Image__State_Copy);
	private String				_imageUrl_StateDeleteDevice			= getIconUrl(Messages.Image__State_Deleted_Device);
	private String				_imageUrl_StateDeleteBackup			= getIconUrl(Messages.Image__State_Deleted_Backup);
	private String				_imageUrl_StateError				= getIconUrl(Messages.Image__State_Error);
	private String				_imageUrl_StateOK					= getIconUrl(Messages.Image__State_OK);
	private String				_imageUrl_StateSave					= getIconUrl(Messages.Image__State_Save);
	/*
	 * UI controls
	 */
	private Browser				_browser;
	private PageBook			_pageBook;

	private Composite			_page_NoBrowser;
	private Composite			_page_WithBrowser;
	private Text				_txtNoBrowser;

	public class ActionReset extends Action {

		public ActionReset() {

			setText(Messages.Tour_Log_Action_Clear_Tooltip);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__remove_all));
		}

		@Override
		public void run() {
			actionClearView();
		}
	}

	private void actionClearView() {

		TourLogManager.clear();
	}

	public void addLog(final TourLog tourLog) {

		final boolean isBrowserAvailable = _browser != null && _browser.isDisposed() == false;

		if (isBrowserAvailable && _isBrowserCompleted == false) {

			// this occures when the view is opening but not yet ready
			return;
		}

		String jsText = UI.replaceJS_BackSlash(tourLog.message);
		jsText = UI.replaceJS_Apostrophe(jsText);
		final String message = jsText;
		final String subItem = tourLog.isSubLogItem ? CSS_LOG_SUB_ITEM : UI.EMPTY_STRING;
		final String css = tourLog.css == null ? UI.EMPTY_STRING : tourLog.css;

		final String stateNoBrowser[] = { UI.EMPTY_STRING };
		final String stateWithBrowser[] = { UI.EMPTY_STRING };

		getImportState(tourLog, stateNoBrowser, stateWithBrowser);

		// Run always async that the flow is not blocked.
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {

				final String noBrowserText = tourLog.time + UI.SPACE + stateNoBrowser[0] + UI.SPACE + message;

//				System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//						+ ("\t" + noBrowserText));
//				// TODO remove SYSTEM.OUT.PRINTLN

				if (isBrowserAvailable) {

					final String js = UI.EMPTY_STRING//

							+ ("var tr = document.createElement('TR');\n") //$NON-NLS-1$

							// time
							+ ("var td = document.createElement('TD');\n") //$NON-NLS-1$
							+ ("td.appendChild(document.createTextNode('" + tourLog.time + "'));\n") //$NON-NLS-1$ //$NON-NLS-2$
							+ ("tr.appendChild(td);\n") //$NON-NLS-1$

							// state
							+ ("var td = document.createElement('TD');\n") //$NON-NLS-1$
							+ ("td.className='column icon';\n") //$NON-NLS-1$
							+ stateWithBrowser[0]
							+ ("tr.appendChild(td);\n") //$NON-NLS-1$

							// message
							+ ("var td = document.createElement('TD');\n") //$NON-NLS-1$
							+ ("td.className='column logItem " + subItem + " " + css + "';\n") //$NON-NLS-1$
							+ ("td.appendChild(document.createTextNode('" + message + "'));\n") //$NON-NLS-1$ //$NON-NLS-2$
							+ ("tr.appendChild(td);\n") //$NON-NLS-1$

							+ ("var logTable = document.getElementById(\"" + DOM_ID_LOG + "\");\n") //$NON-NLS-1$ //$NON-NLS-2$
							+ ("logTable.tBodies[0].appendChild(tr);\n") //$NON-NLS-1$

							// scroll to the bottom -> this do not work
//							+ ("debugger;\n") //$NON-NLS-1$
							+ ("var html = document.documentElement;\n") //$NON-NLS-1$
							+ ("var scrollHeight = html.scrollHeight;\n") //$NON-NLS-1$
							+ ("html.scrollTop = scrollHeight;\n") //$NON-NLS-1$
					;

					_browser.execute(js);

				} else {

					addNoBrowserLog(noBrowserText);
				}
			}
		});
	}

	private void addNoBrowserLog(final String newLogText) {

		if (_noBrowserLog.length() == 0) {

			_noBrowserLog = newLogText;

		} else {

			_noBrowserLog += UI.NEW_LINE1 + newLogText;
		}

		_txtNoBrowser.setText(_noBrowserLog);
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourLogView.this) {
					TourLogManager.setLogView(null);
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourLogView.this) {
					TourLogManager.setLogView(TourLogView.this);
				}
			}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		getViewSite().getPage().addPartListener(_partListener);
	}

	/**
	 * Clear logs.
	 */
	public void clear() {

		_noBrowserLog = UI.EMPTY_STRING;

		updateUI_InitBrowser();
	}

	private void createActions() {

		_actionReset = new ActionReset();
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

		sb.append("<table id='" + DOM_ID_LOG + "'><tbody>\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("</tbody></table>\n"); //$NON-NLS-1$

		return sb.toString();
	}

	@Override
	public void createPartControl(final Composite parent) {

		initUI(parent);

		createActions();

		createUI(parent);

		fillToolbar();

		addPartListener();

		updateUI();
	}

	private void createUI(final Composite parent) {

		final Color bgColor = Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND);

		_pageBook = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook);

		_page_NoBrowser = new Composite(_pageBook, SWT.NONE);
		_page_NoBrowser.setBackground(bgColor);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_page_NoBrowser);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_page_NoBrowser);
		{
			_txtNoBrowser = new Text(_page_NoBrowser, SWT.MULTI | SWT.READ_ONLY);
			_txtNoBrowser.setBackground(bgColor);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.align(SWT.FILL, SWT.FILL)
					.applyTo(_txtNoBrowser);

			addNoBrowserLog(Messages.UI_Label_BrowserCannotBeCreated);
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

			if (_browser != null) {

				GridDataFactory.fillDefaults().grab(true, true).applyTo(_browser);

				_browser.addProgressListener(new ProgressAdapter() {
					@Override
					public void completed(final ProgressEvent event) {

						onBrowser_Completed(event);
					}
				});
			}

		} catch (final SWTError e) {

			addNoBrowserLog(NLS.bind(Messages.UI_Label_BrowserCannotBeCreated_Error, e.getMessage()));
		}
	}

	@Override
	public void dispose() {

		getViewSite().getPage().removePartListener(_partListener);

		super.dispose();
	}

	private void fillToolbar() {

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionReset);
	}

	private void getImportState(final TourLog importLog, final String[] stateNoBrowser, final String[] stateWithBrowser) {

		switch (importLog.state) {

		case IMPORT_OK:
			stateNoBrowser[0] = STATE_OK;
			stateWithBrowser[0] = js_SetStyleBgImage(_imageUrl_StateOK);
			break;

		case EASY_IMPORT_COPY:
			stateNoBrowser[0] = STATE_COPY;
			stateWithBrowser[0] = js_SetStyleBgImage(_imageUrl_StateCopy);
			break;

		case EASY_IMPORT_DELETE_BACKUP:
			stateNoBrowser[0] = STATE_DELETE;
			stateWithBrowser[0] = js_SetStyleBgImage(_imageUrl_StateDeleteBackup);
			break;

		case EASY_IMPORT_DELETE_DEVICE:
			stateNoBrowser[0] = STATE_DELETE;
			stateWithBrowser[0] = js_SetStyleBgImage(_imageUrl_StateDeleteDevice);
			break;

		case IMPORT_ERROR:
			stateNoBrowser[0] = STATE_ERROR;
			stateWithBrowser[0] = js_SetStyleBgImage(_imageUrl_StateError);
			break;

		case TOUR_DELETED:
			stateNoBrowser[0] = STATE_DELETE;
			stateWithBrowser[0] = js_SetStyleBgImage(_imageUrl_StateDeleteBackup);
			break;

		case TOUR_SAVED:
			stateNoBrowser[0] = STATE_SAVE;
			stateWithBrowser[0] = js_SetStyleBgImage(_imageUrl_StateSave);
			break;

		default:
			break;
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

	public boolean isDisposed() {

		return _pageBook == null || _pageBook.isDisposed();
	}

	private String js_SetStyleBgImage(final String imageUrl) {

		return "td.style.backgroundImage=\"url('" + imageUrl + "')\";\n"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void onBrowser_Completed(final ProgressEvent event) {

		_isBrowserCompleted = true;

		// a redraw MUST be done otherwise nothing is displayed
		_browser.setRedraw(true);

		// show already logged items
		final CopyOnWriteArrayList<TourLog> importLogs = TourLogManager.getLogs();
		for (final TourLog importLog : importLogs) {
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

		final boolean isBrowserAvailable = _browser != null && _browser.isDisposed() == false;

		// set dashboard page
		_pageBook.showPage(isBrowserAvailable//
				? _page_WithBrowser
				: _page_NoBrowser);

		if (isBrowserAvailable == false) {
			return;
		}

		updateUI_InitBrowser();
	}

	private void updateUI_InitBrowser() {

		if (_browser == null || _browser.isDisposed()) {
			return;
		}

		final String html = createHTML();

		_isBrowserCompleted = false;

		_browser.setText(html);
	}

}
