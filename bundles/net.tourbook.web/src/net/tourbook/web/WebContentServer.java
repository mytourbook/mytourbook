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
package net.tourbook.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.tourbook.common.ReplacingOutputStream;
import net.tourbook.common.UI;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

/*
 * Part of this code is copied (23.11.2014) from
 * http://www.microhowto.info/howto/serve_web_pages_using_an_embedded_http_server_in_java.html
 */
public class WebContentServer {

   private static final char    NL                       = UI.NEW_LINE;

   private static final boolean IS_DEBUG_PORT            = false;
   private static final int     NUMBER_OF_SERVER_THREADS = 1;

   // logs: time, url
   private static boolean LOG_URL  = false;
   private static boolean LOG_DOJO = false;

   // logs: header
   private static boolean LOG_HEADER = false;

   // logs: xhr
   public static boolean       LOG_XHR                      = false;

   private static boolean      IS_LOGGING                   = LOG_URL || LOG_XHR || LOG_HEADER || LOG_DOJO;

   private static final String MTHTML_CUSTOM_JS             = "CUSTOM_JS";                                 //$NON-NLS-1$
   private static final String MTHTML_DOJO_SEARCH           = "DOJO_SEARCH";                               //$NON-NLS-1$
   private static final String MTHTML_LOCALE                = "LOCALE";                                    //$NON-NLS-1$

   private static final String MTHTML_MESSAGE_LOADING       = "MESSAGE_LOADING";                           //$NON-NLS-1$
   private static final String MTHTML_MESSAGE_SEARCH_TITLE  = "MESSAGE_SEARCH_TITLE";                      //$NON-NLS-1$

   private static final String SEARCH_CSS                   = "search.css";                                //$NON-NLS-1$

   private static final String ROOT_FILE_PATH_NAME          = "/";                                         //$NON-NLS-1$
   private static final String URI_INNER_PROTOCOL_FILE      = "/file:";                                    //$NON-NLS-1$
   private static final String REQUEST_PATH_TOURBOOK        = "/tourbook";                                 //$NON-NLS-1$

   private static final String XHR_HEADER_KEY               = "X-requested-with";                          //$NON-NLS-1$
   private static final String XHR_HEADER_VALUE             = "XMLHttpRequest";                            //$NON-NLS-1$

   private static final String ICON_RESOURCE_REQUEST        = "/$MT-ICON$/";                               //$NON-NLS-1$

   /**
    * A § character is used as identifier, otherwise when using a $ character it would fail as it
    * tries to replace it in the output stream. The § character must be kept in the filepath until
    * it is resolved in the webserver -> Complicated stuff with all these replacements but it is
    * very flexible :-)
    */
   private static final String ICON_RESOURCE_REQUEST_COMMON = "/§MT-ICON-COMMON§/";                        //$NON-NLS-1$

   private static final String CSS_TAG__IS_THEMED           = "$CSS_TAG__IS_THEMED$";                      //$NON-NLS-1$

   private static final String DOJO_ROOT                    = "/dojo/";                                    //$NON-NLS-1$
   private static final String DOJO_DIJIT                   = "/dijit/";                                   //$NON-NLS-1$
   private static final String DOJO_DGRID                   = "/dgrid/";                                   //$NON-NLS-1$
   private static final String DOJO_DSTORE                  = "/dstore/";                                  //$NON-NLS-1$
   private static final String DOJO_PUT_SELECTOR            = "/put-selector/";                            //$NON-NLS-1$
   private static final String DOJO_XSTYLE                  = "/xstyle/";                                  //$NON-NLS-1$

   public static final String  SERVER_URL;

   /**
    * Is <code>true</code> when an external browser is used, <code>false</code> when the embeddet IE
    * browser is used.
    */
   private static boolean      _isUsingEmbeddedBrowser;

   //
   private static Map<String, XHRHandler> _allXHRHandler           = new HashMap<>();
   private static Map<String, Object>     _mtHtmlReplacementValues = new HashMap<>();
   private static Map<String, Object>     _cssReplacementValues;

   /**
    * Possible alternative: https://github.com/NanoHttpd/nanohttpd
    */
   private static HttpServer              _server;
   private static final int               _serverPort;

   private static IconRequestHandler      _iconRequestHandler;
   private static InetSocketAddress       _inetAddress;

   static {

      if (IS_DEBUG_PORT) {
         _serverPort = 24114;
      } else {
         _serverPort = PortFinder.findFreePort();
      }

      final InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
      _inetAddress = new InetSocketAddress(loopbackAddress, _serverPort);

      SERVER_URL = WEB.PROTOCOL_HTTP + loopbackAddress.getHostAddress() + ':' + _serverPort;

//
// This font is disabled because it is not easy to read it.
//
//      /*
//       * Set css font to the same as the whole app.
//       */
//      final FontData dlgFont = JFaceResources.getDialogFont().getFontData()[0];
//
//      final float fontHeight = dlgFont.getHeight() * 1.0f;
//      final String fontSize = String.format(Locale.US, "%.1f", fontHeight);
//
//      final String cssFont = ""//
//            + "<style>                                             " + NL
//            + "body                                                " + NL
//            + "{                                                " + NL
//            + ("   font-family:   " + dlgFont.getName() + ", sans-serif;      " + NL )
//            + ("   font-size:      " + fontSize + "pt;                     " + NL )
//
//            /*
//             * IE do not set the font weight correctly, 599 is too light compared the external
//             * IE, 600 is heavier than the external IE, 400 is by definition the default.
//             */
//            + "      font-weight:   400;                              " + NL
//            + "}                                                " + NL
//            + "</STYLE>                                             " + NL ;

      // Steps when switching between DEBUG and RELEASE build:
      // =====================================================
      // - Set DEBUG flag in WEB.java
      // - Run ant file Create-Dojo-Bundle.xml when RELEASE build is enabled.
      // - Restart app.

      final String dojoSearch = WEB.IS_DEBUG

            ? UI.EMPTY_STRING

                  // DEBUG build

                  + "   <link rel='stylesheet' href='mt-dojo.css'>" + NL //                  //$NON-NLS-1$
                  + "   <link rel='stylesheet' href='search.css'>" + NL //                   //$NON-NLS-1$

                  + "   <script src='/dojo/dojo.js'></script>" + NL //                       //$NON-NLS-1$

            : UI.EMPTY_STRING

                  // RELEASE build

                  + "   <link rel='stylesheet' href='mt-dojo.css.jgz'>" + NL //              //$NON-NLS-1$

                  // use css without compression to support the dark theme
                  + "   <link rel='stylesheet' href='search.css'>" + NL //                   //$NON-NLS-1$

                  + "   <script src='/dojo/dojo.js.jgz'></script>" + NL //                   //$NON-NLS-1$
                  + "   <script src='/tourbook/search/SearchApp.js.jgz'></script>" + NL //   //$NON-NLS-1$
      ;

      /*
       * Get valid locale, invalid locale will cause errors of not supported Dojo files.
       */
      final String localeLanguage = Locale.getDefault().getLanguage();
      String dojoLocale = WEB.DEFAULT_LANGUAGE;

      for (final String supportedLanguage : WEB.SUPPORTED_LANGUAGES) {
         if (supportedLanguage.equals(localeLanguage)) {
            dojoLocale = supportedLanguage;
            break;
         }
      }

      /*
       * Tags which are replaced when HTML page is loaded
       */
      _mtHtmlReplacementValues.put(MTHTML_CUSTOM_JS, createCustomJS());
      _mtHtmlReplacementValues.put(MTHTML_DOJO_SEARCH, dojoSearch);
      _mtHtmlReplacementValues.put(MTHTML_LOCALE, dojoLocale);

      try {

         // these text must be converted into UTF-8 otherwise they are displayed unusable

         _mtHtmlReplacementValues.put(MTHTML_MESSAGE_LOADING, Messages.Web_Page_ContentLoading.getBytes(UI.UTF_8));
         _mtHtmlReplacementValues.put(MTHTML_MESSAGE_SEARCH_TITLE, Messages.Web_Page_Search_Title.getBytes(UI.UTF_8));

      } catch (final UnsupportedEncodingException e) {
         e.printStackTrace();
      }
   }

   private static class DefaultHandler implements HttpHandler {

      @Override
      public void handle(final HttpExchange httpExchange) throws IOException {
         WebContentServer.handle(httpExchange);
      }
   }

   /**
    * @param xhrKey
    * @param xhrHandler
    * @return Returns the previous handler or <code>null</code> when a handler is not available.
    */
   public static XHRHandler addXHRHandler(final String xhrKey, final XHRHandler xhrHandler) {

      return _allXHRHandler.put(xhrKey, xhrHandler);
   }

   private static String createCustomJS() {

      final String js = UI.EMPTY_STRING

            + "<script>" + NL //$NON-NLS-1$
            + "   var mt_IsUsingEmbeddedBrowser = " + Boolean.toString(_isUsingEmbeddedBrowser) + NL //$NON-NLS-1$
            + "</script>" + NL //$NON-NLS-1$
      ;

      return js;
   }

   private static void handle(final HttpExchange httpExchange) {

      final long start = System.nanoTime();

      final StringBuilder log = new StringBuilder();

      try {

         boolean isResourceUrl = false;

         final File rootFile = WEB.getFile(ROOT_FILE_PATH_NAME);
         final String rootPath = rootFile.getCanonicalFile().getPath();

         final URI requestURI = httpExchange.getRequestURI();
         String requestUriPath = requestURI.getPath();

         final Headers requestHeaders = httpExchange.getRequestHeaders();
         final Set<Entry<String, List<String>>> headerEntries = requestHeaders.entrySet();

         final String xhrValue = requestHeaders.getFirst(XHR_HEADER_KEY);
         final boolean isXHR = XHR_HEADER_VALUE.equals(xhrValue);

         final boolean isIconRequest = requestUriPath.startsWith(ICON_RESOURCE_REQUEST);
         final boolean isIconCommonRequest = requestUriPath.startsWith(ICON_RESOURCE_REQUEST_COMMON);

         final int iconRequestLength = isIconCommonRequest
               ? ICON_RESOURCE_REQUEST_COMMON.length()
               : ICON_RESOURCE_REQUEST.length();

         boolean isDojoRequest = false;

         if (WEB.IS_DEBUG

               && (requestUriPath.startsWith(DOJO_ROOT)
                     || requestUriPath.startsWith(DOJO_DIJIT)
                     || requestUriPath.startsWith(DOJO_DGRID)
                     || requestUriPath.startsWith(DOJO_DSTORE)
                     || requestUriPath.startsWith(DOJO_PUT_SELECTOR)
                     || requestUriPath.startsWith(DOJO_XSTYLE))) {

            isDojoRequest = true;
            requestUriPath = WEB.DOJO_TOOLKIT_FOLDER + requestUriPath;
         }

         /*
          * Log request
          */
         if (isDojoRequest) {

            if (LOG_DOJO) {
               log.append(requestUriPath);
            }

         } else if (isXHR) {

            if (LOG_XHR) {
               log.append(requestUriPath);
               logParameter(httpExchange, log);
            }

         } else {

            if (LOG_URL) {
               log.append(requestUriPath);
            }
            if (LOG_HEADER && isXHR) {
               logHeader(log, headerEntries);
            }
         }

         /*
          * Handle request
          */
         if (isIconRequest || isIconCommonRequest) {

            // remove icon marker
            String iconFilename = requestUriPath.substring(iconRequestLength, requestUriPath.length());

            // replace themed marker
            if (iconFilename.contains(CSS_TAG__IS_THEMED)) {

               final CharSequence themePostfix = UI.IS_DARK_THEME

                     ? ThemeUtil.DARK_THEME_POSTFIX
                     : UI.EMPTY_STRING;

               iconFilename = iconFilename.replace(CSS_TAG__IS_THEMED, themePostfix);
            }

            handle_Icon(httpExchange, iconFilename, log, isIconCommonRequest);

         } else if (isXHR) {

            // XHR request

            handle_XHR(httpExchange, requestUriPath, log);

         } else {

            String requestedOSPath = null;

            if (isDojoRequest) {

               // Dojo requests

               isResourceUrl = true;
               requestedOSPath = WEB.DEBUG_PATH_DOJO + requestUriPath;

            } else if (requestUriPath.startsWith(REQUEST_PATH_TOURBOOK)) {

               // Tourbook widget requests

               isResourceUrl = true;
               requestedOSPath = rootPath + requestUriPath;

            } else if (requestUriPath.startsWith(URI_INNER_PROTOCOL_FILE)) {

               isResourceUrl = true;
               requestedOSPath = requestUriPath.substring(URI_INNER_PROTOCOL_FILE.length());

            } else {

               // default request

               requestedOSPath = rootPath + requestUriPath;
            }

            if (requestedOSPath != null) {

               final File file = new File(requestedOSPath).getCanonicalFile();

               if (LOG_URL) {
//                  log.append("\t-->\t" + file.toString());
               }

               if (!file.getPath().startsWith(rootPath) && isResourceUrl == false) {

                  // Suspected path traversal attack: reject with 403 error

                  handle_403(httpExchange, file);

               } else if (!file.isFile()) {

                  // Object does not exist or is not a file: reject with 404 error

                  handle_404(httpExchange, file, requestUriPath);

               } else {

                  // Object exists and is a file: accept with response code 200

                  handle_File(httpExchange, file);
               }
            }
         }

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {

         if (log.length() > 0 && IS_LOGGING) {

            final String msg = String.format("%s %5.1f ms  %-16s [%s] %s", //$NON-NLS-1$
                  UI.timeStampNano(),
                  (float) (System.nanoTime() - start) / 1000000,
                  Thread.currentThread().getName(),
                  WebContentServer.class.getSimpleName(),
                  log);

            System.out.println(msg);
         }
      }
   }

   private static void handle_403(final HttpExchange httpExchange, final File file) {

      OutputStream os = null;

      try {

         final String response = "403 (Forbidden)" + NL; //$NON-NLS-1$
         httpExchange.sendResponseHeaders(403, response.length());

         os = httpExchange.getResponseBody();
         os.write(response.getBytes());

         StatusUtil.logError(response + " " + file.getPath()); //$NON-NLS-1$

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {
         Util.close(os);
      }
   }

   private static void handle_404(final HttpExchange httpExchange, final File file, final String requestUriPath) {

      OutputStream os = null;

      try {

         final String response = String.format("%s\n404 (Not Found)" + NL, requestUriPath);//$NON-NLS-1$
         httpExchange.sendResponseHeaders(404, response.length());

         os = httpExchange.getResponseBody();
         os.write(response.getBytes());

         StatusUtil.logError(response + " " + file.getPath()); //$NON-NLS-1$

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {
         Util.close(os);
      }
   }

   private static void handle_File(final HttpExchange httpExchange, final File file) {

      OutputStream outputStream = null;

      ReplacingOutputStream replacingOutputStream = null;

      try (FileInputStream fs = new FileInputStream(file)) {

         final String extension = WEB.setResponseHeaderContentType(httpExchange, file);

         httpExchange.sendResponseHeaders(200, 0);

         outputStream = httpExchange.getResponseBody();

         if (extension.equals(WEB.FILE_EXTENSION_MTHTML)) {

            // replaces values in .mthtml files

            replacingOutputStream = new ReplacingOutputStream(outputStream, _mtHtmlReplacementValues);

            int byteValue;

            while ((byteValue = fs.read()) != -1) {
               replacingOutputStream.write(byteValue);
            }

         } else if (file.getName().equals(SEARCH_CSS)) {

            // replace values in search.css file

// for debugging - START

//            _cssReplacementValues.clear();
//            _cssReplacementValues = null;
//            setupCssReplacements();

// for debugging - END

            replacingOutputStream = new ReplacingOutputStream(outputStream, _cssReplacementValues);

            int c;

            while ((c = fs.read()) != -1) {
               replacingOutputStream.write(c);
            }

         } else {

            final byte[] buffer = new byte[0x10000];
            int count = 0;
            while ((count = fs.read(buffer)) >= 0) {
               outputStream.write(buffer, 0, count);
            }
         }

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {

         if (Util.close(outputStream) == false) {
            StatusUtil.logError(String.format("File: '%s'", file.toString())); //$NON-NLS-1$
         }

         Util.close(replacingOutputStream);
      }
   }

   private static void handle_Icon(final HttpExchange httpExchange,
                                   final String iconFilename,
                                   final StringBuilder log,
                                   final boolean isIconCommonRequest) {

      try {

         if (_iconRequestHandler == null) {

            StatusUtil.logError("IconRequestHandler is not set for " + iconFilename); //$NON-NLS-1$

         } else {

            _iconRequestHandler.handleIconRequest(httpExchange, iconFilename, log, isIconCommonRequest);
         }

      } catch (final Exception e) {
         StatusUtil.log(e);
      }
   }

   private static void handle_XHR(final HttpExchange httpExchange, final String requestUriPath, final StringBuilder log) {

      try {

         if (LOG_XHR) {

//            reqBody = httpExchange.getRequestBody();
//
//            final StringBuilder sb = new StringBuilder();
//            final byte[] buffer = new byte[0x10000];
//
//            while (reqBody.read(buffer) != -1) {
//               sb.append(buffer);
//            }
//
//            // log content
//            log.append("\nXHR-" + NL );
//            log.append(sb.toString());
//            log.append("\n-XHR" + NL );
         }

         final String xhrKey = requestUriPath;

         final XHRHandler xhrHandler = _allXHRHandler.get(xhrKey);

         if (xhrHandler == null) {
            StatusUtil.logError("XHR handler is not set for " + xhrKey);//$NON-NLS-1$
         } else {
            xhrHandler.handleXHREvent(httpExchange, log);
         }

      } catch (final Exception e) {
         StatusUtil.log(e);
      }
   }

   private static void logHeader(final StringBuilder log, final Set<Entry<String, List<String>>> headerEntries) {

      log.append(NL);

      for (final Entry<String, List<String>> entry : headerEntries) {
         log.append(String.format("%-20s %s" + NL, entry.getKey(), entry.getValue()));//$NON-NLS-1$
      }
   }

   private static void logParameter(final HttpExchange httpExchange, final StringBuilder log) {

      // get parameters from url query string

      @SuppressWarnings("unchecked")
      final Map<String, Object> params = (Map<String, Object>) httpExchange.getAttribute(RequestParameterFilter.ATTRIBUTE_PARAMETERS);

      if (params.size() > 0) {
         log.append("\tparams: " + params);//$NON-NLS-1$
      }
   }

   /**
    * @param xhrKey
    * @return Returns the previous handler or <code>null</code> when a handler is not available.
    */
   public static XHRHandler removeXHRHandler(final String xhrKey) {

      return _allXHRHandler.remove(xhrKey);
   }

   public static void setIconRequestHandler(final IconRequestHandler iconRequestHandler) {

      _iconRequestHandler = iconRequestHandler;
   }

   public static void setIsUsingEmbeddedBrowser(final boolean isUsingEmbeddedBrowser) {

      _isUsingEmbeddedBrowser = isUsingEmbeddedBrowser;

      _mtHtmlReplacementValues.put(MTHTML_CUSTOM_JS, createCustomJS());
   }

   private static void setupCssReplacements() {

      if (_cssReplacementValues != null) {
         return;
      }

      _cssReplacementValues = new HashMap<>();

// SET_FORMATTING_OFF

      _cssReplacementValues.put("A__COLOR",                                UI.IS_DARK_THEME ? "749DFF" : "24f");           //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      _cssReplacementValues.put("BODY__COLOR",                             ThemeUtil.getThemedCss_DefaultForeground());    //$NON-NLS-1$
      _cssReplacementValues.put("BODY__BACKGROUND_COLOR",                  ThemeUtil.getThemedCss_DefaultBackground());    //$NON-NLS-1$
      _cssReplacementValues.put("BODY__SCROLLBAR",                         UI.IS_DARK_THEME                                //$NON-NLS-1$
                                                                              ? WEB.CSS_CONTENT__BODY_SCROLLBAR__DARK
                                                                              : UI.EMPTY_STRING);

      _cssReplacementValues.put("DRGID_CONTENT__COLOR",                    ThemeUtil.getThemedCss_DefaultForeground());    //$NON-NLS-1$
      _cssReplacementValues.put("DRGID_CONTENT__BACKGROUND_COLOR",         ThemeUtil.getThemedCss_DefaultBackground());    //$NON-NLS-1$

      _cssReplacementValues.put("DRGID_ROW__BACKGROUND_COLOR",             UI.IS_DARK_THEME ? "444" : "eee");              //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      _cssReplacementValues.put("DRGID_SELECTED__BACKGROUND_COLOR",        UI.IS_DARK_THEME ? "555" : "ddd");              //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      _cssReplacementValues.put("DRGID_SELECTED_HOVER__BACKGROUND_COLOR",  UI.IS_DARK_THEME ? "666" : "ccc");              //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      _cssReplacementValues.put("DIJIT_BUTTON_HOVER__COLOR",               UI.IS_DARK_THEME ? "ddd" : "666");              //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      _cssReplacementValues.put("DIJIT_BUTTON_HOVER__BACKGROUND_COLOR",    UI.IS_DARK_THEME ? "666" : "ddd");              //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      _cssReplacementValues.put("DOM_SEARCH_INPUT_CONTAINER",              UI.IS_DARK_THEME ? "444" : "f4f4f4");           //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      _cssReplacementValues.put("DOM_APP_STATUS",                          UI.IS_DARK_THEME ? "aaa" : "666");              //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      _cssReplacementValues.put("CSS_TAG__IS_THEMED",                      UI.IS_DARK_THEME                                //$NON-NLS-1$
                                                                              ? ThemeUtil.DARK_THEME_POSTFIX
                                                                              : UI.EMPTY_STRING);
// SET_FORMATTING_ON

   }

   public static void start() {

      if (_server != null) {
         return;
      }

      setupCssReplacements();

      try {

         _server = HttpServer.create(_inetAddress, 0);

         final HttpContext context = _server.createContext("/", new DefaultHandler());//$NON-NLS-1$

         // convert uri query parameters into a "parameters" map
         context.getFilters().add(new RequestParameterFilter());

         // ensure that the server is running in another thread
         final ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_SERVER_THREADS);
         _server.setExecutor(executor);

         _server.start();

         StatusUtil.logInfo("Started WebContentServer " + SERVER_URL);//$NON-NLS-1$

      } catch (final IOException e) {
         StatusUtil.showStatus(e);
      }
   }

   public static void stop() {

      if (_server != null) {

         _server.stop(0);
         _server = null;

         StatusUtil.logInfo("Stopped WebContentServer " + SERVER_URL);//$NON-NLS-1$
      }
   }

}
