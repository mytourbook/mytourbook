/*
 * This is copied from okhttp3.debugging.HttpLoggingInterceptor.class
 * 
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tourbook.map.vtm;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;
import okio.BufferedSource;

/**
 * An OkHttp interceptor which logs request and response information. Can be applied as an
 * {@linkplain OkHttpClient#interceptors() application interceptor} or as a
 * {@linkplain OkHttpClient#networkInterceptors() network interceptor}.
 * <p>
 * The format of the logs created by this class should not be considered stable and may change
 * slightly between releases. If you need a stable logging format, use your own interceptor.
 */
public final class HttpLoggingInterceptorMT implements Interceptor {

	private static final String		FORMAT_HEADER	= "   %-30s%s";

	private static final Charset	UTF8			= Charset.forName("UTF-8");

	private static final Logger		log				= LoggerFactory.getLogger(HttpLoggingInterceptorMT.class);

	private volatile Level			level			= Level.NONE;

	private AtomicInteger			_logId			= new AtomicInteger();

	public enum Level {

		/** No logs. */
		NONE,

		/**
		 * Logs request and response lines.
		 * <p>
		 * Example:
		 * 
		 * <pre>
		 * {@code
		 * --> POST /greeting http/1.1 (3-byte body)
		 *
		 * <-- 200 OK (22ms, 6-byte body)
		 * }
		 * </pre>
		 */
		BASIC,

		/**
		 * Logs request and response lines and their respective headers.
		 * <p>
		 * Example:
		 * 
		 * <pre>
		 * {@code
		 * --> POST /greeting http/1.1
		 * Host: example.com
		 * Content-Type: plain/text
		 * Content-Length: 3
		 * --> END POST
		 *
		 * <-- 200 OK (22ms)
		 * Content-Type: plain/text
		 * Content-Length: 6
		 * <-- END HTTP
		 * }
		 * </pre>
		 */
		HEADERS,

		/**
		 * Logs request and response lines and their respective headers and bodies (if present).
		 * <p>
		 * Example:
		 * 
		 * <pre>
		 * {@code
		 * --> POST /greeting http/1.1
		 * Host: example.com
		 * Content-Type: plain/text
		 * Content-Length: 3
		 *
		 * Hi?
		 * --> END POST
		 *
		 * <-- 200 OK (22ms)
		 * Content-Type: plain/text
		 * Content-Length: 6
		 *
		 * Hello!
		 * <-- END HTTP
		 * }
		 * </pre>
		 */
		BODY
	}

	/**
	 * Returns true if the body in question probably contains human readable text. Uses a small
	 * sample of code points to detect unicode control characters commonly used in binary file
	 * signatures.
	 */
	static boolean isPlaintext(final Buffer buffer) {

		try {
			final Buffer prefix = new Buffer();
			final long byteCount = buffer.size() < 64 ? buffer.size() : 64;
			buffer.copyTo(prefix, 0, byteCount);
			for (int i = 0; i < 16; i++) {
				if (prefix.exhausted()) {
					break;
				}
				final int codePoint = prefix.readUtf8CodePoint();
				if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
					return false;
				}
			}
			return true;

		} catch (final EOFException e) {

			return false; // Truncated UTF-8 sequence.
		}
	}

	private boolean bodyEncoded(final Headers headers) {
		final String contentEncoding = headers.get("Content-Encoding");
		return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
	}

	public Level getLevel() {
		return level;
	}

	@Override
	public Response intercept(final Chain chain) throws IOException {

		final Level level = this.level;

		final Request request = chain.request();
		if (level == Level.NONE) {
			return chain.proceed(request);
		}

		final String logId = Integer.toString(_logId.incrementAndGet()) + " ";

		final boolean isLogBody = level == Level.BODY;
		final boolean isLogHeaders = isLogBody || level == Level.HEADERS;

		final RequestBody requestBody = request.body();
		final boolean hasRequestBody = requestBody != null;

		final Connection connection = chain.connection();
		final Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;
		String requestStartMessage = "--> " + request.method() + ' ' + request.url() + ' ' + protocol;
		if (!isLogHeaders && hasRequestBody) {
			requestStartMessage += " (" + requestBody.contentLength() + "-byte body)";
		}
		log.debug(logId + requestStartMessage);

		if (isLogHeaders) {

			if (hasRequestBody) {
				// Request body headers are only present when installed as a network interceptor. Force
				// them to be included (when available) so there values are known.
				if (requestBody.contentType() != null) {
					log.debug(logId + "Content-Type: " + requestBody.contentType());
				}
				if (requestBody.contentLength() != -1) {
					log.debug(logId + "Content-Length: " + requestBody.contentLength());
				}
			}

			final Headers headers = request.headers();
			for (int i = 0, count = headers.size(); i < count; i++) {

				final String name = headers.name(i);

				// Skip headers from the request body as they are explicitly logged above.
				if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
					log.debug(logId + String.format(FORMAT_HEADER, headers.name(i), headers.value(i)));
				}
			}

//			if (!isLogBody || !hasRequestBody) {
//				log.debug(logId + "--> END " + request.method());
//			} else if (bodyEncoded(request.headers())) {
//				log.debug(logId + "--> END " + request.method() + " (encoded body omitted)");
//			} else {
//				final Buffer buffer = new Buffer();
//				requestBody.writeTo(buffer);
//
//				Charset charset = UTF8;
//				final MediaType contentType = requestBody.contentType();
//				if (contentType != null) {
//					charset = contentType.charset(UTF8);
//				}
//
//				log.debug(logId + "");
//				if (isPlaintext(buffer)) {
//					log.debug(logId + buffer.readString(charset));
//					log.debug(logId + "--> END " + request.method()
//							+ " (" + requestBody.contentLength() + "-byte body)");
//				} else {
//					log.debug(logId + "--> END " + request.method() + " (binary "
//							+ requestBody.contentLength() + "-byte body omitted)");
//				}
//			}
		}

		final long startNs = System.nanoTime();
		Response response;
		try {
			response = chain.proceed(request);
		} catch (final Exception e) {
			log.debug(logId + "<-- HTTP FAILED: " + e);
			throw e;
		}

		final long tookMy = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startNs);

		final ResponseBody responseBody = response.body();
		final long contentLength = responseBody.contentLength();
		final String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";

		final String logBody = !isLogHeaders ? ", " + bodySize + " body" : "";

		log.debug(logId + "<-- " + response.code() + ' ' + response.message() + ' '
				+ response.request().url() + " (" + tookMy + " my" + logBody + ')');

		if (isLogHeaders) {

			final Headers headers = response.headers();
			for (int i = 0, count = headers.size(); i < count; i++) {
				log.debug(logId + String.format(FORMAT_HEADER, headers.name(i), headers.value(i)));
			}

			if (!isLogBody || !HttpHeaders.hasBody(response)) {
				log.debug(logId + "<-- END HTTP");
			} else if (bodyEncoded(response.headers())) {
				log.debug(logId + "<-- END HTTP (encoded body omitted)");
			} else {
				final BufferedSource source = responseBody.source();
				source.request(Long.MAX_VALUE); // Buffer the entire body.
				final Buffer buffer = source.buffer();

				Charset charset = UTF8;
				final MediaType contentType = responseBody.contentType();
				if (contentType != null) {
					charset = contentType.charset(UTF8);
				}

				if (!isPlaintext(buffer)) {
					log.debug(logId + "");
					log.debug(logId + "<-- END HTTP (binary " + buffer.size() + "-byte body omitted)");
					return response;
				}

				if (contentLength != 0) {
					log.debug(logId + "");
					log.debug(logId + buffer.clone().readString(charset));
				}

				log.debug(logId + "<-- END HTTP (" + buffer.size() + "-byte body)");
			}
		}

		return response;
	}

	/** Change the level at which this interceptor logs. */
	public HttpLoggingInterceptorMT setLevel(final Level level) {

		if (level == null) {
			throw new NullPointerException("level == null. Use Level.NONE instead.");
		}

		this.level = level;

		return this;
	}
}
