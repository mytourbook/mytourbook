package net.tourbook.cloud.dropbox;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.IOAuth2Constants;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.eclipse.jface.preference.IPreferenceStore;

public class TokensRetrievalHandler implements HttpHandler {

   private String           _codeVerifier;

   private IPreferenceStore _prefStore = Activator.getDefault().getPreferenceStore();
   private String           _authorizationCode;

   public TokensRetrievalHandler(final String codeVerifier) {
      _codeVerifier = codeVerifier;
   }

   public String getAuthorizationCode() {
      return _authorizationCode;
   }

   @Override
   public void handle(final HttpExchange httpExchange) throws IOException {

      DropboxTokens tokens = new DropboxTokens();
      if ("GET".equals(httpExchange.getRequestMethod())) { //$NON-NLS-1$

         tokens = handleGetRequest(httpExchange);
      }

      handleResponse(httpExchange);

      if (StringUtils.hasContent(tokens.getAccess_token())) {

         _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN_EXPIRES_IN, tokens.getExpires_in());
         _prefStore.setValue(Preferences.DROPBOX_REFRESHTOKEN, tokens.getRefresh_token());
         _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN_ISSUE_DATETIME, System.currentTimeMillis());
         _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN, tokens.getAccess_token());
      }
   }

   private DropboxTokens handleGetRequest(final HttpExchange httpExchange) {

      final char[] separators = { '#', '&', '?' };

      final String response = httpExchange.getRequestURI().toString();

      String authorizationCode = UI.EMPTY_STRING;
      final List<NameValuePair> params = URLEncodedUtils.parse(response, StandardCharsets.UTF_8, separators);
      for (final NameValuePair param : params) {
         if (param.getName().equals(IOAuth2Constants.PARAM_CODE)) {
            authorizationCode = param.getValue();
            break;
         }
      }

      DropboxTokens newTokens = new DropboxTokens();
      if (StringUtils.isNullOrEmpty(authorizationCode)) {
         return newTokens;
      }

      //get tokens from the authorization code
      newTokens = DropboxClient.getTokens(authorizationCode, false, UI.EMPTY_STRING, _codeVerifier);

      return newTokens;
   }

   private void handleResponse(final HttpExchange httpExchange) throws IOException {

      final OutputStream outputStream = httpExchange.getResponseBody();

      final StringBuilder htmlBuilder = new StringBuilder();
      htmlBuilder.append("<html><body><h1>" + Messages.Html_CloseBrowser_Text + "</h1></body></html>"); //$NON-NLS-1$ //$NON-NLS-2$

      // this line is a must
      httpExchange.sendResponseHeaders(200, htmlBuilder.length());

      try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
         writer.write(htmlBuilder.toString());
         outputStream.flush();
      }
   }
}
