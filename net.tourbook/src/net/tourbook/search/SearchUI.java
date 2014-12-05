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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.web.ParameterFilter;
import net.tourbook.web.WEB;
import net.tourbook.web.WebContentServer;
import net.tourbook.web.XHRHandler;

import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.eclipse.swt.browser.Browser;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;

public class SearchUI implements XHRHandler {

	static final String			SEARCH_FOLDER			= "/search/";	//$NON-NLS-1$

	/**
	 * This will handle all search actions.
	 */
	private static final String	XHR_SEARCH_HANDLER		= "/xhrSearch"; //$NON-NLS-1$

	private static final String	XHR_KEY_ACTION			= "ACTION";	//$NON-NLS-1$
	private static final String	XHR_ACTION_PROPOSALS	= "Proposals";	//$NON-NLS-1$
	private static final String	XHR_ACTION_SEARCH		= "Search";	//$NON-NLS-1$

	private static final String	XHR_SEARCH_TEXT			= "searchText"; //$NON-NLS-1$

	private static final String	ITEM_KEY_ID				= "id";		//$NON-NLS-1$
	private static final String	ITEM_KEY_NAME			= "name";		//$NON-NLS-1$

	private Browser				_browser;

	SearchUI(final Browser browser) {

		// ensure web server is started
		WebContentServer.start();

		WebContentServer.addXHRHandler(XHR_SEARCH_HANDLER, this);

		_browser = browser;

		final String searchUrl = WEB.SERVER_URL + SEARCH_FOLDER + "search.html";

		_browser.setUrl(searchUrl);
	}

	private String doAction_Proposals(final Map<String, Object> params) throws UnsupportedEncodingException {

		final JSONObject jsonResponse = new JSONObject();

		List<LookupResult> proposals = null;
		final Object xhrSearchText = params.get(XHR_SEARCH_TEXT);

		if (xhrSearchText instanceof String) {

			final String searchText = URLDecoder.decode((String) xhrSearchText, WEB.UTF_8);

			proposals = FTSearchManager.getProposals(searchText);
		}

		if (proposals.size() == 0) {
			return UI.EMPTY_STRING;
		}

		final JSONArray allItems = new JSONArray();
		for (final LookupResult lookupResult : proposals) {

			final String proposal = lookupResult.key.toString();

			final JSONObject item = new JSONObject();
			item.put(ITEM_KEY_ID, proposal);
			item.put(ITEM_KEY_NAME, proposal);

			allItems.put(item);
		}

		jsonResponse.put("items", allItems);

		final String response = jsonResponse.toString();

		return response;
	}

	private String doAction_Search(final Map<String, Object> params) {

//		final SearchResult searchResult = null;
//		if (xhrSearchText instanceof String) {
//			searchResult = FTSearchManager.search((String) xhrSearchText, 0, 100);
//		}
//
//		if (searchResult != null && searchResult.items.size() == 0 && searchResult.error == null) {
//			jsonResponse.put("isEmpty", true);
//		}
		return UI.EMPTY_STRING;
	}

	@Override
	public void handleXHREvent(final HttpExchange httpExchange, final StringBuilder log) throws IOException {

		// get parameters from url query string
		@SuppressWarnings("unchecked")
		final Map<String, Object> params = (Map<String, Object>) httpExchange
				.getAttribute(ParameterFilter.ATTRIBUTE_PARAMETERS);

		log.append("\tparams: " + params);

		String response = UI.EMPTY_STRING;
		final Object action = params.get(XHR_KEY_ACTION);

		if (XHR_ACTION_SEARCH.equals(action)) {

			response = doAction_Search(params);

		} else if (XHR_ACTION_PROPOSALS.equals(action)) {

			response = doAction_Proposals(params);
		}

		writeRespone(httpExchange, response);
	}

	void setFocus() {
		_browser.setFocus();
	}

	private void writeRespone(final HttpExchange httpExchange, final String response) {

		OutputStream os = null;

		try {

//			response.setContentType("application/json;charset=UTF-8");
			final byte[] convertedResponse = response.getBytes(WEB.UTF_8);

			httpExchange.sendResponseHeaders(200, convertedResponse.length);

			os = httpExchange.getResponseBody();
			os.write(convertedResponse);

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {
			Util.close(os);
		}
	}

}
