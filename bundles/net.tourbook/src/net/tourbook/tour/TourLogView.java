/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.web.WEB;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

/**
 * Tour log view
 */
public class TourLogView extends ViewPart {

   public static final String  ID                               = "net.tourbook.tour.TourLogView";        //$NON-NLS-1$

   private static final char   NL                               = UI.NEW_LINE;

   private static final String STATE_COPY                       = "COPY";                                 //$NON-NLS-1$
   private static final String STATE_DELETE                     = "DELETE";                               //$NON-NLS-1$
   private static final String STATE_ERROR                      = "ERROR";                                //$NON-NLS-1$
   private static final String STATE_EXCEPTION                  = "EXCEPTION";                            //$NON-NLS-1$
   private static final String STATE_INFO                       = "INFO";                                 //$NON-NLS-1$
   private static final String STATE_OK                         = "OK";                                   //$NON-NLS-1$
   private static final String STATE_SAVE                       = "SAVE";                                 //$NON-NLS-1$

   public static final String  CSS_LOG_INFO                     = "info";                                 //$NON-NLS-1$
   private static final String CSS_LOG_ITEM                     = "logItem";                              //$NON-NLS-1$
   private static final String CSS_LOG_SUB_ITEM                 = "subItem";                              //$NON-NLS-1$
   public static final String  CSS_LOG_TITLE                    = "title";                                //$NON-NLS-1$

   private static final String DOM_ID_LOG                       = "logs";                                 //$NON-NLS-1$
   private static final String WEB_RESOURCE_TOUR_IMPORT_LOG_CSS = "tour-import-log.css";                  //$NON-NLS-1$

   private IPartListener2      _partListener;

   private Action              _action_CopyIntoClipboard;
   private Action              _action_Reset;

   private boolean             _isNewUI;
   private boolean             _isBrowserCompleted;

   private String              _tourLogCSS;
   private String              _noBrowserLog                    = UI.EMPTY_STRING;

   private String              _imageUrl_StateCopy              = getIconUrl(Images.State_Copy);
   private String              _imageUrl_StateDeleteDevice      = getIconUrl(Images.State_Deleted_Device);
   private String              _imageUrl_StateDeleteBackup      = getIconUrl(Images.State_Deleted_Backup);
   private String              _imageUrl_StateError             = getIconUrl(Images.State_Error);
   private String              _imageUrl_StateInfo              = getIconUrl(Images.State_Info);
   private String              _imageUrl_StateOK                = getIconUrl(Images.State_OK);
   private String              _imageUrl_StateSave              = getIconUrl(Images.State_Save);

   /*
    * UI controls
    */
   private Browser   _browser;
   private PageBook  _pageBook;

   private Composite _page_NoBrowser;
   private Composite _page_WithBrowser;
   private Text      _txtNoBrowser;

   private class Action_CopyLogValuesIntoClipboard extends Action {

      Action_CopyLogValuesIntoClipboard() {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);

         // Copy log into the clipboard
         setToolTipText(Messages.Tour_Log_Action_CopyTourLogIntoClipboard_Tooltip);

         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Copy));
         setDisabledImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Copy_Disabled));
      }

      @Override
      public void run() {
         onAction_CopyLogIntoClipboard();
      }
   }

   private class Action_Reset extends Action {

      public Action_Reset() {

         setText(Messages.Tour_Log_Action_Clear_Tooltip);
         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_RemoveAll));
      }

      @Override
      public void run() {
         onAction_ClearView();
      }
   }

   public void addLog(final TourLog tourLog) {

      if (isBrowserAvailable() && _isBrowserCompleted == false) {

         // this occures when the view is opening but not yet ready
         return;
      }

      // Run always async that the flow is not blocked
      Display.getDefault().asyncExec(() -> {

         final String noBrowserText = createNoBrowserText(tourLog, getStateImage_NoBrowser(tourLog.state));

         if (isBrowserAvailable()) {

            String jsText = UI.replaceJS_BackSlash(tourLog.message);
            jsText = UI.replaceJS_Apostrophe(jsText);
            final String message = jsText;

            final String subItem = tourLog.isSubLogItem
                  ? CSS_LOG_SUB_ITEM
                  : UI.EMPTY_STRING;

            final String css = tourLog.css == null
                  ? CSS_LOG_ITEM
                  : tourLog.css;

            final String[] messageSplitted = message.split(WEB.HTML_ELEMENT_BR);

// SET_FORMATTING_OFF

            String tdContent;

            if (messageSplitted.length == 1) {

               tdContent = "td.appendChild(document.createTextNode('" + message + "'));"     + NL; //$NON-NLS-1$ //$NON-NLS-2$

            } else {

               tdContent = UI.EMPTY_STRING

                     + "var span = document.createElement('SPAN');"                          + NL //$NON-NLS-1$
                     + "span.innerHTML='" + message + "';"                                   + NL //$NON-NLS-1$ //$NON-NLS-2$
                     + "td.appendChild(span);"                                               + NL  //$NON-NLS-1$
               ;
            }

            final String js = UI.EMPTY_STRING

                  + "var tr = document.createElement('TR');"                                 + NL //$NON-NLS-1$
                  + "tr.className='row';"                                                    + NL //$NON-NLS-1$

                  // time
                  + "var td = document.createElement('TD');"                                 + NL //$NON-NLS-1$
                  + "td.appendChild(document.createTextNode('" + tourLog.time + "'));"       + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "tr.appendChild(td);"                                                    + NL //$NON-NLS-1$

                  // state (icon)
                  + "var td = document.createElement('TD');"                                 + NL //$NON-NLS-1$
                  + "td.className='column icon';"                                            + NL //$NON-NLS-1$
                  + getStateImage_WithBrowser(tourLog.state)
                  + "tr.appendChild(td);"                                                    + NL //$NON-NLS-1$

                  // message
                  + "var td = document.createElement('TD');"                                 + NL //$NON-NLS-1$
                  + "td.className='column " + subItem + UI.SPACE1 + css + "';"               + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + tdContent

                  + "tr.appendChild(td);"                                                    + NL //$NON-NLS-1$

                  + "var logTable = document.getElementById(\"" + DOM_ID_LOG + "\");"        + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "var htmlSectionElement = logTable.tBodies[0]"                           + NL //$NON-NLS-1$

                  // reduce number of log rows to reduce cpu cycles, when too many rows,
                  // time can be > 100ms for the js code, this code is now not more than 10ms
                  + "var allRows = htmlSectionElement.rows"                                  + NL //$NON-NLS-1$
                  + "var numRows = allRows.length"                                           + NL //$NON-NLS-1$
                  + "if (numRows > 1000) {"                                                  + NL //$NON-NLS-1$

                  // delete top row
                  + "   htmlSectionElement.deleteRow(0)"                                     + NL //$NON-NLS-1$
                  + "}"                                                                      + NL //$NON-NLS-1$

                  + "htmlSectionElement.appendChild(tr);"                                    + NL //$NON-NLS-1$

                  // scroll to the bottom -> debugger do not work
//                + ("debugger;\n") //$NON-NLS-1$

                  + "var html = document.documentElement;"                                   + NL //$NON-NLS-1$
                  + "var scrollHeight = html.scrollHeight;"                                  + NL //$NON-NLS-1$
                  + "html.scrollTop = scrollHeight;"                                         + NL //$NON-NLS-1$
            ;

// SET_FORMATTING_ON

            _browser.execute(js);

            // log the log text to the console
            final boolean isLogToConsole = true;
            if (isLogToConsole) {
               System.out.println("[TourLog] " + noBrowserText);//$NON-NLS-1$
            }

         } else {

            addLog_NoBrowser(noBrowserText);
         }

         enableControls();
      });
   }

   private void addLog_NoBrowser(final String newLogText) {

      if (_noBrowserLog.length() == 0) {

         _noBrowserLog = newLogText;

      } else {

         _noBrowserLog += UI.NEW_LINE1 + newLogText;
      }

      _txtNoBrowser.setText(_noBrowserLog);

      // scroll to the bottom
      _txtNoBrowser.setTopIndex(_txtNoBrowser.getLineCount() - 1);
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
      _txtNoBrowser.setText(_noBrowserLog);

      updateUI_InitBrowser();
   }

   private void createActions() {

      _action_Reset = new Action_Reset();
      _action_CopyIntoClipboard = new Action_CopyLogValuesIntoClipboard();
   }

   private String createHTML() {

      final String html = UI.EMPTY_STRING

            + "<!DOCTYPE html>" + NL // ensure that IE is using the newest version and not the quirk mode //$NON-NLS-1$
            + "<html style='height: 100%; width: 100%; margin: 0px; padding: 0px;'>" + NL //    //$NON-NLS-1$
            + "<head>" + NL + createHTML_10_Head() + NL + "</head>" + NL //                     //$NON-NLS-1$ //$NON-NLS-2$
            + "<body>" + NL + createHTML_20_Body() + NL + "</body>" + NL //                     //$NON-NLS-1$ //$NON-NLS-2$
            + "</html>" //                                                                      //$NON-NLS-1$
      ;

      return html;
   }

   private String createHTML_10_Head() {

      final String html = UI.EMPTY_STRING

            + "   <meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />" + NL //      //$NON-NLS-1$
            + "   <meta http-equiv='X-UA-Compatible' content='IE=edge' />" + NL //                    //$NON-NLS-1$
            + _tourLogCSS + NL;

      return html;
   }

   private String createHTML_20_Body() {

      final StringBuilder sb = new StringBuilder();

      sb.append("<table id='" + DOM_ID_LOG + "'><tbody>" + NL); //$NON-NLS-1$ //$NON-NLS-2$
      sb.append("</tbody></table>" + NL); //$NON-NLS-1$

      return sb.toString();
   }

   private String createNoBrowserText(final TourLog tourLog, final String stateNoBrowser) {

      final String subIndent = tourLog.isSubLogItem
            ? UI.SPACE3
            : UI.EMPTY_STRING;

      return String.format("[%s] %s %-5s %s   %s", //$NON-NLS-1$

            tourLog.threadName,
            tourLog.time,
            stateNoBrowser, // text instead of an icon
            subIndent,
            tourLog.message

      );
   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI();

      createActions();

      createUI(parent);
      restoreState();

      fillToolbar();

      addPartListener();

      updateUI();

      enableControls();
   }

   private void createUI(final Composite parent) {

      final Color bgColor = Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND);

      _pageBook = new PageBook(parent, SWT.NONE);

      _page_NoBrowser = new Composite(_pageBook, SWT.NONE);
      _page_NoBrowser.setBackground(bgColor);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_page_NoBrowser);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_page_NoBrowser);
      {
         _txtNoBrowser = new Text(_page_NoBrowser, SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
         _txtNoBrowser.setBackground(bgColor);
         GridDataFactory.fillDefaults()
               .grab(true, true)
               .align(SWT.FILL, SWT.FILL)
               .applyTo(_txtNoBrowser);
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

//            /*
//             * Use mozilla browser, this is necessary for Linux when default browser fails
//             * however the XULrunner needs to be installed.
//             */
//            _browser = new Browser(parent, SWT.MOZILLA);
         }

         if (_browser != null) {

            GridDataFactory.fillDefaults().grab(true, true).applyTo(_browser);

            _browser.addProgressListener(new ProgressAdapter() {
               @Override
               public void completed(final ProgressEvent event) {

                  onBrowser_Completed();
               }
            });

            _browser.addLocationListener(new LocationAdapter() {
               @Override
               public void changing(final LocationEvent event) {

                  onLocation_Changing(event);
               }

            });
         }

      } catch (final SWTError e) {

         addLog_NoBrowser(NLS.bind(Messages.UI_Label_BrowserCannotBeCreated_Error, e.getMessage()));
      }
   }

   private void createUI_NewUI() {

      if (_page_WithBrowser == null) {

         _page_WithBrowser = new Composite(_pageBook, SWT.NONE);

         GridLayoutFactory.fillDefaults().applyTo(_page_WithBrowser);
         {
            createUI_10_Browser(_page_WithBrowser);
         }
      }
   }

   @Override
   public void dispose() {

      getViewSite().getPage().removePartListener(_partListener);

      super.dispose();
   }

   private void enableControls() {

      final boolean isLogAvailable = TourLogManager.getLogs().size() > 0;

      _action_CopyIntoClipboard.setEnabled(isLogAvailable);
   }

   private void fillToolbar() {

      /*
       * fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_action_CopyIntoClipboard);
      tbm.add(_action_Reset);
   }

   private String getStateImage_NoBrowser(final TourLogState state) {

// SET_FORMATTING_OFF

      switch (state) {

      case IMPORT_OK:                  return STATE_OK;
      case IMPORT_ERROR:               return STATE_ERROR;
      case IMPORT_EXCEPTION:           return STATE_EXCEPTION;
      case INFO:                       return STATE_INFO;
      case EASY_IMPORT_COPY:           return STATE_COPY;

      case EASY_IMPORT_DELETE_BACKUP:
      case TOUR_DELETED:               return STATE_DELETE;

      case EASY_IMPORT_DELETE_DEVICE:  return STATE_DELETE;
      case TOUR_SAVED:                 return STATE_SAVE;

// SET_FORMATTING_ON

      default:
      }
      return UI.EMPTY_STRING;
   }

   private String getStateImage_WithBrowser(final TourLogState state) {

// SET_FORMATTING_OFF

      switch (state) {

      case IMPORT_OK:                  return js_SetStyleBgImage(_imageUrl_StateOK);
      case IMPORT_ERROR:               return js_SetStyleBgImage(_imageUrl_StateError);
      case IMPORT_EXCEPTION:           return js_SetStyleBgImage(_imageUrl_StateError);
      case INFO:                       return js_SetStyleBgImage(_imageUrl_StateInfo);
      case EASY_IMPORT_COPY:           return js_SetStyleBgImage(_imageUrl_StateCopy);

      case EASY_IMPORT_DELETE_BACKUP:
      case TOUR_DELETED:               return js_SetStyleBgImage(_imageUrl_StateDeleteBackup);

      case EASY_IMPORT_DELETE_DEVICE:  return js_SetStyleBgImage(_imageUrl_StateDeleteDevice);
      case TOUR_SAVED:                 return js_SetStyleBgImage(_imageUrl_StateSave);

// SET_FORMATTING_ON

      default:
      }

      return UI.EMPTY_STRING;
   }

   private boolean httpAction(final String location) {

      if (location.toLowerCase().startsWith("http://") || //$NON-NLS-1$
            location.toLowerCase().startsWith("https://")) { //$NON-NLS-1$
         WEB.openUrl(location);
         return true;
      }
      return false;
   }

   private void initUI() {

      /*
       * Webpage css
       */
      try {

         final File webFile = WEB.getResourceFile(WEB_RESOURCE_TOUR_IMPORT_LOG_CSS);
         final String cssFromFile = Util.readContentFromFile(webFile.getAbsolutePath());

         _tourLogCSS = UI.EMPTY_STRING

               + "<style>" + NL //              //$NON-NLS-1$
               + WEB.createCSS_Scrollbar()
               + cssFromFile
               + "</style>" + NL //             //$NON-NLS-1$
         ;

      } catch (IOException | URISyntaxException e) {
         TourLogManager.logEx(e);
      }

   }

   private boolean isBrowserAvailable() {

      return _browser != null && _browser.isDisposed() == false;
   }

   public boolean isDisposed() {

      return _pageBook == null || _pageBook.isDisposed();
   }

   private String js_SetStyleBgImage(final String imageUrl) {

      return "td.style.backgroundImage=\"url('" + imageUrl + "')\";" + NL; //$NON-NLS-1$ //$NON-NLS-2$
   }

   private void onAction_ClearView() {

      TourLogManager.clear();

      enableControls();
   }

   /**
    * Copy log text into clipboard
    */
   private void onAction_CopyLogIntoClipboard() {

      final StringBuilder sb = new StringBuilder();
      final CopyOnWriteArrayList<TourLog> allTourLogs = TourLogManager.getLogs();

      for (int logIndex = 0; logIndex < allTourLogs.size(); logIndex++) {

         final TourLog tourLog = allTourLogs.get(logIndex);

         if (logIndex > 0) {
            sb.append(UI.NEW_LINE);
         }

         sb.append(createNoBrowserText(tourLog, getStateImage_NoBrowser(tourLog.state)));
      }

      final String logText = sb.toString();

      if (logText.length() > 0) {

         final Display display = Display.getDefault();
         final TextTransfer textTransfer = TextTransfer.getInstance();

         final Clipboard clipBoard = new Clipboard(display);
         {
            clipBoard.setContents(

                  new Object[] { logText },
                  new Transfer[] { textTransfer });
         }
         clipBoard.dispose();

         final IStatusLineManager statusLineMgr = UI.getStatusLineManager();
         if (statusLineMgr != null) {

            // show info that data are copied
            // "The log were copied into the clipboard"
            statusLineMgr.setMessage(Messages.Tour_Log_Info_TourLogWasCopied);

            // cleanup message
            display.timerExec(3000, () -> statusLineMgr.setMessage(null));
         }
      }
   }

   private void onBrowser_Completed() {

      _isBrowserCompleted = true;

      // a redraw MUST be done otherwise nothing is displayed
      _browser.setRedraw(true);

      // show already logged items
      final CopyOnWriteArrayList<TourLog> importLogs = TourLogManager.getLogs();
      for (final TourLog importLog : importLogs) {
         addLog(importLog);
      }
   }

   private void onLocation_Changing(final LocationEvent event) {

      if (httpAction(event.location)) {

         // keep current page when an action is performed, OTHERWISE the current page will disappear or is replaced :-(
         event.doit = false;
      }

   }

   private void restoreState() {

      _isNewUI = TourbookPlugin.getPrefStore().getBoolean(ITourbookPreferences.IMPORT_IS_NEW_UI);
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

      if (_isNewUI) {

         createUI_NewUI();

         final boolean isBrowserAvailable = _browser != null && _browser.isDisposed() == false;

         // set dashboard page
         _pageBook.showPage(isBrowserAvailable//
               ? _page_WithBrowser
               : _page_NoBrowser);

         if (isBrowserAvailable == false) {
            return;
         }

         updateUI_InitBrowser();

      } else {

         _pageBook.showPage(_page_NoBrowser);

         /*
          * Show already available log entries
          */
         for (final TourLog tourLog : TourLogManager.getLogs()) {

            final String noBrowserText = createNoBrowserText(tourLog, getStateImage_NoBrowser(tourLog.state));

            addLog_NoBrowser(noBrowserText);
         }
      }

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
