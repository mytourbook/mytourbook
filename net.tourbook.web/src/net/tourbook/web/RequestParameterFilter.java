/**
 * Source: https://leonardom.wordpress.com/2009/08/06/getting-parameters-from-httpexchange/<br>
 * Date:  25.11.2014
 */
package net.tourbook.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

/**
 * Puts query parameters into a map which can be retrieved with
 * {@link HttpExchange#getAttribute(String)} and "parameters" as the parameter.
 */
public class RequestParameterFilter extends Filter {

	private static final String	REQUEST_METHOD_POST		= "post"; //$NON-NLS-1$
	public static final String	ATTRIBUTE_PARAMETERS	= "parameters"; //$NON-NLS-1$

	private static final String	REGEX_VALUE_SEP			= "[=]"; //$NON-NLS-1$
	private static final String	REGEX_PARAMETER_SEP		= "[&]"; //$NON-NLS-1$

	@Override
	public String description() {
		return "Parses the requested URI for parameters";//$NON-NLS-1$
	}

	@Override
	public void doFilter(final HttpExchange exchange, final Chain chain) throws IOException {

		parseGetParameters(exchange);
		parsePostParameters(exchange);

		chain.doFilter(exchange);
	}

	private void parseGetParameters(final HttpExchange exchange) throws UnsupportedEncodingException {

		final Map<String, Object> parameters = new HashMap<String, Object>();

		final URI requestedUri = exchange.getRequestURI();
		final String query = requestedUri.getRawQuery();

		parseQuery(query, parameters);

		exchange.setAttribute(ATTRIBUTE_PARAMETERS, parameters);
	}

	private void parsePostParameters(final HttpExchange exchange) throws IOException {

		if (REQUEST_METHOD_POST.equalsIgnoreCase(exchange.getRequestMethod())) {

			@SuppressWarnings("unchecked")
			final Map<String, Object> parameters = (Map<String, Object>) exchange.getAttribute(ATTRIBUTE_PARAMETERS);

			final InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), WEB.UTF_8);
			final BufferedReader br = new BufferedReader(isr);
			final String query = br.readLine();

			parseQuery(query, parameters);
		}
	}

	@SuppressWarnings("unchecked")
	private void parseQuery(final String query, final Map<String, Object> parameters)
			throws UnsupportedEncodingException {

		if (query != null) {

			final String pairs[] = query.split(REGEX_PARAMETER_SEP);

			for (final String pair : pairs) {

				final String param[] = pair.split(REGEX_VALUE_SEP);

				String key = null;
				String value = null;

				if (param.length > 0) {
					key = URLDecoder.decode(param[0], WEB.UTF_8);
				}

				if (param.length > 1) {
					value = URLDecoder.decode(param[1], WEB.UTF_8);
				}

				if (parameters.containsKey(key)) {
					final Object obj = parameters.get(key);
					if (obj instanceof List<?>) {
						final List<String> values = (List<String>) obj;
						values.add(value);
					} else if (obj instanceof String) {
						final List<String> values = new ArrayList<String>();
						values.add((String) obj);
						values.add(value);
						parameters.put(key, values);
					}
				} else {
					parameters.put(key, value);
				}
			}
		}
	}
}
