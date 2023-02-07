/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.web.WEB;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
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

   public static final String             ID                               = "net.tourbook.tour.TourLogView";        //$NON-NLS-1$

   private static final char              NL                               = UI.NEW_LINE;

   private static final String            STATE_COPY                       = "COPY";                                 //$NON-NLS-1$
   private static final String            STATE_DELETE                     = "DELETE";                               //$NON-NLS-1$
   private static final String            STATE_ERROR                      = "ERROR";                                //$NON-NLS-1$
   private static final String            STATE_EXCEPTION                  = "EXCEPTION";                            //$NON-NLS-1$
   private static final String            STATE_INFO                       = "INFO";                                 //$NON-NLS-1$
   private static final String            STATE_OK                         = "OK";                                   //$NON-NLS-1$
   private static final String            STATE_SAVE                       = "SAVE";                                 //$NON-NLS-1$

   private static final String            STATE_IS_UI_COLORFULL            = "STATE_IS_UI_COLORFULL";                //$NON-NLS-1$

   static final String                    CSS_LOG_INFO                     = "info";                                 //$NON-NLS-1$
   private static final String            CSS_LOG_ITEM                     = "logItem";                              //$NON-NLS-1$
   private static final String            CSS_LOG_SUB_ITEM                 = "subItem";                              //$NON-NLS-1$
   public static final String             CSS_LOG_TITLE                    = "title";                                //$NON-NLS-1$

   private static final String            DOM_ID_LOG                       = "logs";                                 //$NON-NLS-1$
   private static final String            WEB_RESOURCE_TOUR_IMPORT_LOG_CSS = "tour-import-log.css";                  //$NON-NLS-1$

   private static final int               MAX_BROWSER_ITEMS                = 1000;

   private static final IPreferenceStore  _prefStore                       = TourbookPlugin.getPrefStore();
   private static final IDialogSettings   _state                           = TourbookPlugin.getState(ID);

   private IPartListener2                 _partListener;
   private IPropertyChangeListener        _prefChangeListener;

   private Action                         _action_CopyIntoClipboard;
   private Action                         _action_Clear;
   private Action_ToggleSimpleOrColor     _action_ToggleSimpleOrColor;

   private boolean                        _isBrowserContentSet;
   private boolean                        _isBrowserCompleted;
   private boolean                        _isUIColorfull;

   private String                         _tourLogCSS;
   private String                         _noBrowserLog                    = UI.EMPTY_STRING;

   private String                         _imageUrl_StateCopy              = getIconUrl(Images.State_Copy);
   private String                         _imageUrl_StateDeleteDevice      = getIconUrl(Images.State_Deleted_Device);
   private String                         _imageUrl_StateDeleteBackup      = getIconUrl(Images.State_Deleted_Backup);
   private String                         _imageUrl_StateError             = getIconUrl(Images.State_Error);
   private String                         _imageUrl_StateInfo              = getIconUrl(Images.State_Info);
   private String                         _imageUrl_StateOK                = getIconUrl(Images.State_OK);
   private String                         _imageUrl_StateSave              = getIconUrl(Images.State_Save);

   private long                           _lastLogTime;
   private AtomicInteger                  _tourLog_RunningId               = new AtomicInteger();
   private ConcurrentLinkedQueue<TourLog> _tourLog_Queue                   = new ConcurrentLinkedQueue<>();

   /*
    * UI controls
    */
   private Browser   _browser;
   private PageBook  _pageBook;

   private Composite _page_NoBrowser;
   private Composite _page_WithBrowser;

   private Text      _txtNoBrowser;

   private Font      _zoomFont;

   private class Action_ClearView extends Action {

      public Action_ClearView() {

         setText(Messages.Tour_Log_Action_Clear_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_RemoveAll));
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_RemoveAll_Disabled));
      }

      @Override
      public void run() {
         onAction_ClearView();
      }
   }

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

   private class Action_ToggleSimpleOrColor extends Action {

      public Action_ToggleSimpleOrColor() {

         setText(Messages.Tour_Log_Action_TourLogLayout_Tooltip);
         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourLog_Layout_Color));
      }

      @Override
      public void run() {
         onAction_ToggleSimpleOrColorUI();
      }
   }

   void addLog(final TourLog tourLog) {

      _tourLog_Queue.add(tourLog);

      if (isBrowserAvailable() && _isBrowserCompleted == false) {

         // this occurs when the view is opening but not yet ready
         return;
      }

      final long now = System.currentTimeMillis();

      final int runnableRunningId = _tourLog_RunningId.incrementAndGet();

      Display.getDefault().asyncExec(new Runnable() {

         private int __runningId = runnableRunningId;

         @Override
         public void run() {

            if (_pageBook.isDisposed()) {
               return;
            }

            if (now - _lastLogTime > 1000) {

               // display at least at every interval

               addLog_10_UpdateUI();

               _lastLogTime = System.currentTimeMillis();

            } else {

               // display when idle

               final int currentId = _tourLog_RunningId.get();

               if (__runningId != currentId) {

                  // a newer runnable is created -> ignore this UI update

                  return;
               }

               addLog_10_UpdateUI();

               _lastLogTime = System.currentTimeMillis();
            }
         }
      });
   }

   private void addLog_10_UpdateUI() {

      List<TourLog> allLogItems;

      // get current log items and cleanup queue
      synchronized (_tourLog_Queue) {

         if (_tourLog_Queue.isEmpty()) {
            return;
         }

         allLogItems = _tourLog_Queue.stream().collect(Collectors.toList());
         _tourLog_Queue.clear();
      }

      /*
       * Create log text
       */
      final StringBuilder logsWithJs = new StringBuilder();
      final ArrayList<String> allSimple = new ArrayList<>();

      allLogItems.forEach(tourLog_InQueue -> {

         logsWithJs.append(createLogMessage_Js(tourLog_InQueue));

         allSimple.add(createLogMessage_NoJs(tourLog_InQueue, getStateImage_NoBrowser(tourLog_InQueue.state)));
      });

      if (isBrowserAvailable() && _isUIColorfull) {

         // show browser log text

         _browser.execute(logsWithJs.toString());

         // scroll to the bottom
         _browser.execute(UI.EMPTY_STRING

//             debugger do not work
//             + "debugger" + NL //$NON-NLS-1$

               + "var html = document.documentElement" + NL //$NON-NLS-1$
               + "var scrollHeight = html.scrollHeight" + NL //$NON-NLS-1$
               + "html.scrollTop = scrollHeight" + NL //$NON-NLS-1$
         );

         final boolean isLogToConsole = true;

         // log the log text to the console
         if (isLogToConsole) {

            for (final String logText : allSimple) {
               final String convertedMessage = WEB.convertHTML_Into_JavaLineBreaks(logText);
               System.out.println("[TourLog] " + convertedMessage);//$NON-NLS-1$
            }
         }

      } else {

         // show simple log text

         final StringBuilder sb = new StringBuilder();

         final boolean[] isFirst = { false };

         allSimple.forEach(logMessage -> {

            if (isFirst[0] == false) {
               isFirst[0] = true;
            } else {
               sb.append(UI.NEW_LINE);
            }

            final String convertedMessage = WEB.convertHTML_Into_JavaLineBreaks(logMessage);

            sb.append(convertedMessage);

         });

         addLog_20_NoBrowser(sb.toString());
      }

      enableControls();
   }

   private void addLog_20_NoBrowser(final String newLogText) {

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

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.FONT_LOGGING_IS_MODIFIED)) {

            // update font

            _txtNoBrowser.setFont(net.tourbook.ui.UI.getLogFont());
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   /**
    * Clear logs.
    */
   void clear() {

      _noBrowserLog = UI.EMPTY_STRING;
      _txtNoBrowser.setText(UI.EMPTY_STRING);

      updateUI_InitBrowser();
   }

   private void createActions() {

      _action_Clear = new Action_ClearView();
      _action_CopyIntoClipboard = new Action_CopyLogValuesIntoClipboard();
      _action_ToggleSimpleOrColor = new Action_ToggleSimpleOrColor();
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

   private String createLogMessage_Js(final TourLog tourLog) {

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

         tdContent = "td.appendChild(document.createTextNode('" + message + "'))"         + NL; //$NON-NLS-1$ //$NON-NLS-2$

      } else {

         tdContent = UI.EMPTY_STRING

               + "var span = document.createElement('SPAN')"                              + NL //$NON-NLS-1$
               + "span.innerHTML='" + message + "'"                                       + NL //$NON-NLS-1$ //$NON-NLS-2$
               + "td.appendChild(span)"                                                   + NL  //$NON-NLS-1$
         ;
      }


      final String js = UI.EMPTY_STRING

            + "var tr = document.createElement('TR')"                                     + NL //$NON-NLS-1$
            + "tr.className='row'"                                                        + NL //$NON-NLS-1$

            // time
            + "var td = document.createElement('TD')"                                     + NL //$NON-NLS-1$
            + "td.appendChild(document.createTextNode('" + tourLog.time + "'))"           + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "tr.appendChild(td)"                                                        + NL //$NON-NLS-1$

            // thread name
            + "var td = document.createElement('TD')"                                     + NL //$NON-NLS-1$
            + "td.className='column logItem'"                                             + NL //$NON-NLS-1$
            + "td.appendChild(document.createTextNode('[" + tourLog.threadName + "]'))"   + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "tr.appendChild(td)"                                                        + NL //$NON-NLS-1$

            // log number
            + "var td = document.createElement('TD')"                                     + NL //$NON-NLS-1$
            + "td.className='column'"                                                     + NL //$NON-NLS-1$
            + "td.appendChild(document.createTextNode('" + tourLog.logNumber + "'))"      + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "tr.appendChild(td)"                                                        + NL //$NON-NLS-1$

            // state (icon)
            + "var td = document.createElement('TD')"                                     + NL //$NON-NLS-1$
            + "td.className='column icon'"                                                + NL //$NON-NLS-1$
            + getStateImage_WithBrowser(tourLog.state)
            + "tr.appendChild(td)"                                                        + NL //$NON-NLS-1$

            // message
            + "var td = document.createElement('TD')"                                     + NL //$NON-NLS-1$
            + "td.className='column " + subItem + UI.SPACE1 + css + "'"                   + NL //$NON-NLS-1$ //$NON-NLS-2$
            + tdContent

            + "tr.appendChild(td)"                                                        + NL //$NON-NLS-1$

            + "var logTable = document.getElementById(\"" + DOM_ID_LOG + "\")"            + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "var htmlSectionElement = logTable.tBodies[0]"                              + NL //$NON-NLS-1$

            // reduce number of log rows to reduce cpu cycles, when too many rows,
            // time can be > 100ms for the js code, this code is now not more than 10ms
            + "var allRows = htmlSectionElement.rows"                                     + NL //$NON-NLS-1$
            + "var numRows = allRows.length"                                              + NL //$NON-NLS-1$
            + "if (numRows > " + MAX_BROWSER_ITEMS + ") {"                                          + NL //$NON-NLS-1$ //$NON-NLS-2$

            // delete top row
            + "   htmlSectionElement.deleteRow(0)"                                        + NL //$NON-NLS-1$
            + "}"                                                                         + NL //$NON-NLS-1$

            + "htmlSectionElement.appendChild(tr)"                                        + NL //$NON-NLS-1$

            + NL
            + NL
      ;

// SET_FORMATTING_ON

      return js;
   }

   private String createLogMessage_NoJs(final TourLog tourLog, final String stateNoBrowser) {

      final String subIndent = tourLog.isSubLogItem
            ? UI.SPACE3
            : UI.EMPTY_STRING;

      final String separator = "";//"\t"; //$NON-NLS-1$

      final String logMessagePrefix = String.format(UI.EMPTY_STRING

            + "%s" + separator //            time              //$NON-NLS-1$
            + " [%-25s]" + separator //      thread name       //$NON-NLS-1$
            + " %-5d" + separator //         log number        //$NON-NLS-1$
            + " %-5s" + separator //         state icon        //$NON-NLS-1$
            + " %s" + separator //           indent            //$NON-NLS-1$
            ,

            tourLog.time,
            tourLog.threadName,
            tourLog.logNumber,
            stateNoBrowser, // text instead of an icon
            subIndent);

      final int prefixLength = logMessagePrefix.length();
      final String linePrefix = UI.SPACE1.repeat(prefixLength + 3);

      final String logMessageRaw = tourLog.message;
      final String logMessageWithSpaces = logMessageRaw.replaceAll(

            WEB.HTML_ELEMENT_BR,
            WEB.HTML_ELEMENT_BR + linePrefix);
      /*
       * Replace new line breaks '<br>' with prefixed spaces
       */

      final String logMessage = String.format(UI.EMPTY_STRING

            + "%s" //                        message prefix    //$NON-NLS-1$
            + "   %s" + separator //         message           //$NON-NLS-1$
            ,

            logMessagePrefix,
            logMessageWithSpaces);

      return logMessage;
   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI();

      createActions();

      createUI(parent);
      restoreState();

      fillToolbar();

      addPartListener();
      addPrefListener();

      updateUI();

      enableControls();
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);

      _page_NoBrowser = createUI_20_NoBrowser(_pageBook);
   }

   private void createUI_10_NewUI() {

      if (_page_WithBrowser == null) {

         _page_WithBrowser = new Composite(_pageBook, SWT.NONE);

         GridLayoutFactory.fillDefaults().applyTo(_page_WithBrowser);
         {
            createUI_12_Browser(_page_WithBrowser);
         }
      }
   }

   private void createUI_12_Browser(final Composite parent) {

      try {

         try {

            // use default browser
            _browser = new Browser(parent, SWT.NONE);

            // initial setup
            _browser.setRedraw(false);

         } catch (final Exception e) {

            // use WebKit browser for Linux when default browser fails

            _browser = new Browser(parent, SWT.WEBKIT);
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

                  onBrowser_LocationChanging(event);
               }
            });
         }

      } catch (final SWTError e) {

         addLog_20_NoBrowser(NLS.bind(Messages.UI_Label_BrowserCannotBeCreated_Error, e.getMessage()));
      }
   }

   private Composite createUI_20_NoBrowser(final Composite parent) {

      final Color bgColor = Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND);

      final Composite container = new Composite(parent, SWT.NONE);
      container.setBackground(bgColor);
      container.setFont(net.tourbook.ui.UI.getLogFont());
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         _txtNoBrowser = new Text(container, SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
         _txtNoBrowser.setFont(net.tourbook.ui.UI.getLogFont());
         _txtNoBrowser.setBackground(bgColor);
         _txtNoBrowser.addMouseWheelListener(this::onMouseWheel);
         GridDataFactory.fillDefaults()
               .grab(true, true)
               .align(SWT.FILL, SWT.FILL)
               .applyTo(_txtNoBrowser);
      }

      return container;
   }

   @Override
   public void dispose() {

      if (_zoomFont != null) {
         _zoomFont.dispose();
      }

      getViewSite().getPage().removePartListener(_partListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      super.dispose();
   }

   private void enableControls() {

      final boolean isLogAvailable = TourLogManager.getLogs().size() > 0;

      _action_Clear.setEnabled(isLogAvailable);
      _action_CopyIntoClipboard.setEnabled(isLogAvailable);
   }

   private void fillToolbar() {

      /*
       * fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_action_CopyIntoClipboard);
      tbm.add(_action_ToggleSimpleOrColor);
      tbm.add(_action_Clear);
   }

   private String getStateImage_NoBrowser(final TourLogState state) {

// SET_FORMATTING_OFF

      switch (state) {

      case OK:                         return STATE_OK;
      case ERROR:                      return STATE_ERROR;
      case EXCEPTION:                  return STATE_EXCEPTION;
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

      case OK:                         return js_SetStyleBgImage(_imageUrl_StateOK);
      case ERROR:                      return js_SetStyleBgImage(_imageUrl_StateError);
      case EXCEPTION:                  return js_SetStyleBgImage(_imageUrl_StateError);
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
      final File webFile = WEB.getResourceFile(WEB_RESOURCE_TOUR_IMPORT_LOG_CSS);
      final String cssFromFile = Util.readContentFromFile(webFile.getAbsolutePath());

      _tourLogCSS = UI.EMPTY_STRING

            + "<style>" + NL //              //$NON-NLS-1$
            + WEB.createCSS_Scrollbar()
            + cssFromFile
            + "</style>" + NL //             //$NON-NLS-1$
      ;
   }

   private boolean isBrowserAvailable() {

      return _browser != null && _browser.isDisposed() == false;
   }

   public boolean isDisposed() {

      return _pageBook == null || _pageBook.isDisposed();
   }

   private String js_SetStyleBgImage(final String imageUrl) {

      return "td.style.backgroundImage=\"url('" + imageUrl + "')\"" + NL; //$NON-NLS-1$ //$NON-NLS-2$
   }

   private void onAction_ClearView() {

      TourLogManager.clear();

      _isBrowserContentSet = false;

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

//         if (tourLog.message.contains("Invalid thread")) {
//
//            int a = 0;
//            a++;
//         }

         final String logMessage = createLogMessage_NoJs(tourLog, getStateImage_NoBrowser(tourLog.state));
         final String logMessage_WithLineBreaks = WEB.convertHTML_Into_JavaLineBreaks(logMessage);

         sb.append(logMessage_WithLineBreaks);
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

   private void onAction_ToggleSimpleOrColorUI() {

      _isUIColorfull = !_isUIColorfull;

      updateUI_ToggleSimpleOrColor();

      if (_isUIColorfull && _page_WithBrowser != null && _isBrowserContentSet) {

         _pageBook.showPage(_page_WithBrowser);

      } else {

         _noBrowserLog = UI.EMPTY_STRING;

         updateUI();
      }

   }

   private void onBrowser_Completed() {

      _isBrowserCompleted = true;

      // a redraw MUST be done otherwise nothing is displayed
      _browser.setRedraw(true);

      // show already logged items
      final CopyOnWriteArrayList<TourLog> importLogs = TourLogManager.getLogs();

      final TourLog[] allImportLogs = importLogs.toArray(new TourLog[importLogs.size()]);

      final int numLogs = allImportLogs.length;
      final int logIndexStart = Math.max(0, numLogs - MAX_BROWSER_ITEMS);

      for (int logIndex = logIndexStart; logIndex < numLogs; logIndex++) {
         addLog(allImportLogs[logIndex]);
      }

      _isBrowserContentSet = numLogs > 0;
   }

   private void onBrowser_LocationChanging(final LocationEvent event) {

      if (httpAction(event.location)) {

         // keep current page when an action is performed, OTHERWISE the current page will disappear or is replaced :-(
         event.doit = false;
      }
   }

   private void onMouseWheel(final MouseEvent mouseEvent) {

      if (UI.isCtrlKey(mouseEvent)) {

         // enlarge/reduce font size

         final Font txtFont = _txtNoBrowser.getFont();
         final FontData fontData = txtFont.getFontData()[0];

         final int fontHeight_OLD = fontData.getHeight();
         int fontHeight_NEW = fontHeight_OLD

               // adjust to mouse wheel direction
               + (mouseEvent.count > 0 ? 1 : -1);

         fontHeight_NEW = Math.max(1, Math.min(fontHeight_NEW, 100));

         if (fontHeight_OLD != fontHeight_NEW) {

            // fontsize has changed

            fontData.setHeight(fontHeight_NEW);

            // dispose old font
            if (_zoomFont != null) {
               _zoomFont.dispose();
            }

            _zoomFont = new Font(_txtNoBrowser.getDisplay(), fontData);

            _txtNoBrowser.setFont(_zoomFont);
         }
      }
   }

   private void restoreState() {

      _isUIColorfull = Util.getStateBoolean(_state, STATE_IS_UI_COLORFULL, true);
   }

   @PersistState
   private void saveState() {

      // keep selected tours
      _state.put(STATE_IS_UI_COLORFULL, _isUIColorfull);
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

      if (_isUIColorfull) {

         createUI_10_NewUI();

         final boolean isBrowserAvailable = _browser != null && _browser.isDisposed() == false;

         // set log page
         _pageBook.showPage(isBrowserAvailable

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
         final StringBuilder sb = new StringBuilder();
         final CopyOnWriteArrayList<TourLog> allTourLogs = TourLogManager.getLogs();

         for (int logIndex = 0; logIndex < allTourLogs.size(); logIndex++) {

            final TourLog tourLog = allTourLogs.get(logIndex);

            if (logIndex > 0) {
               sb.append(UI.NEW_LINE);
            }

            final String logMessage = createLogMessage_NoJs(tourLog, getStateImage_NoBrowser(tourLog.state));
            final String logMessage_WithLineBreaks = WEB.convertHTML_Into_JavaLineBreaks(logMessage);

            sb.append(logMessage_WithLineBreaks);
         }

         addLog_20_NoBrowser(sb.toString());
      }

      updateUI_ToggleSimpleOrColor();
   }

   private void updateUI_InitBrowser() {

      if (_browser == null || _browser.isDisposed()) {
         return;
      }

      final String html = createHTML();

      _isBrowserCompleted = false;

      _browser.setText(html);
   }

   private void updateUI_ToggleSimpleOrColor() {

      if (_isUIColorfull) {

         _action_ToggleSimpleOrColor.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourLog_Layout_Color));

      } else {

         _action_ToggleSimpleOrColor.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourLog_Layout_Simple));
      }
   }

}
