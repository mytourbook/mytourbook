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
import net.tourbook.web.RequestParameterFilter;
import net.tourbook.web.WEB;
import net.tourbook.web.WebContentServer;
import net.tourbook.web.XHRHandler;

import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.eclipse.swt.browser.Browser;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class SearchWebService implements XHRHandler {

	private static final String	REQUEST_HEADER_RANGE		= "Range";

	static final String			SEARCH_FOLDER				= "/tourbook/search/";	//$NON-NLS-1$

	/**
	 * This will handle all search actions.
	 */
	private static final String	XHR_SEARCH_INPUT_HANDLER	= "/xhrSearch";		//$NON-NLS-1$

	private static final String	XHR_PARAM_ACTION			= "action";			//$NON-NLS-1$
	private static final String	XHR_PARAM_SEARCH_TEXT		= "searchText";		//$NON-NLS-1$

	private static final String	XHR_ACTION_PROPOSALS		= "proposals";			//$NON-NLS-1$
	private static final String	XHR_ACTION_SEARCH			= "search";			//$NON-NLS-1$

	private static final String	ITEM_KEY_ID					= "id";				//$NON-NLS-1$
	private static final String	ITEM_KEY_NAME				= "name";				//$NON-NLS-1$

	private Browser				_browser;

	SearchWebService(final Browser browser) {

		// ensure web server is started
		WebContentServer.start();

		WebContentServer.addXHRHandler(XHR_SEARCH_INPUT_HANDLER, this);

		_browser = browser;

//		http://127.0.0.1:24114/tourbook/search/test-dgrid.html

//		final String searchUrl = WEB.SERVER_URL + SEARCH_FOLDER + "search.html";
		final String searchUrl = WEB.SERVER_URL + SEARCH_FOLDER + "test-dgrid.html";

		_browser.setUrl(searchUrl);
	}

	private String doAction_Proposals(final Map<String, Object> params) throws UnsupportedEncodingException {

		final JSONObject jsonResponse = new JSONObject();

		List<LookupResult> proposals = null;
		final Object xhrSearchText = params.get(XHR_PARAM_SEARCH_TEXT);

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

	private String doAction_Search(	final Map<String, Object> params,
									final HttpExchange httpExchange,
									final StringBuilder log) throws UnsupportedEncodingException {

		final Headers headers = httpExchange.getRequestHeaders();

		final JSONArray allItems = new JSONArray();

		final String xhrSearchText = (String) params.get(XHR_PARAM_SEARCH_TEXT);
		final String range = headers.getFirst(REQUEST_HEADER_RANGE);

		SearchResult searchResult = null;
		int searchPosFrom = 0;

		if (xhrSearchText != null && range != null) {

			final String[] ranges = range.substring("items=".length()).split("-");
			searchPosFrom = Integer.valueOf(ranges[0]);
			final int searchPosTo = Integer.valueOf(ranges[1]);

			log.append("range: " + searchPosFrom + "-" + searchPosTo + "\t" + headers.entrySet());

			String searchText = URLDecoder.decode(xhrSearchText, WEB.UTF_8);
			if (searchText.endsWith(UI.SYMBOL_STAR) == false) {

				// Append a * otherwise nothing is found
				searchText += UI.SYMBOL_STAR;
			}

			searchResult = FTSearchManager.searchByPosition(searchText, searchPosFrom, searchPosTo);

			for (final SearchResultItem searchItem : searchResult.items) {

				final int docId = searchItem.docId;
				final int docSource = searchItem.docSource;
				final String markerId = searchItem.markerId;
				final String tourId = searchItem.tourId;

				final boolean isTour = docSource == FTSearchManager.DOC_SOURCE_TOUR;
				final boolean isMarker = docSource == FTSearchManager.DOC_SOURCE_TOUR_MARKER;
				final boolean isWayPoint = docSource == FTSearchManager.DOC_SOURCE_WAY_POINT;

				final JSONObject item = new JSONObject();

				final StringBuilder content = new StringBuilder();
				if (searchItem.title != null) {
					content.append(searchItem.title);
				}
				if (searchItem.description != null) {
					if (content.length() > 0) {
						content.append("<br>");
					}
					content.append(searchItem.description);
				}

				item.put(ITEM_KEY_ID, searchItem.docId);
				item.put(ITEM_KEY_NAME, content);

				allItems.put(item);
			}
		}

		/*
		 * This is very important otherwise nothing is displayed
		 */
		if (searchResult != null) {

			final Headers responseHeaders = httpExchange.getResponseHeaders();
			String contentRange;
			if (searchResult.items.size() == 0) {
				contentRange = "0-0/0";
			} else {
				contentRange = searchPosFrom
						+ "-"
						+ (searchPosFrom + allItems.length() - 1)
						+ "/"
						+ searchResult.totalHits;
			}
			responseHeaders.set("Content-Range", "items " + contentRange);
		}

		final String response = allItems.toString();

		return response;
	}

	@Override
	public void handleXHREvent(final HttpExchange httpExchange, final StringBuilder log) throws IOException {

		// get parameters from url query string
		@SuppressWarnings("unchecked")
		final Map<String, Object> params = (Map<String, Object>) httpExchange
				.getAttribute(RequestParameterFilter.ATTRIBUTE_PARAMETERS);

		String response = UI.EMPTY_STRING;
		final Object action = params.get(XHR_PARAM_ACTION);

		if (XHR_ACTION_SEARCH.equals(action)) {

			response = doAction_Search(params, httpExchange, log);

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
