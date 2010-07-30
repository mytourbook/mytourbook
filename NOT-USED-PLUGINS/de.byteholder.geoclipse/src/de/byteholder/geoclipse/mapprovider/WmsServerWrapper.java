/*******************************************************************************
 * Copyright (C) 2005, 2009 Wolfgang Schramm and Contributors
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package de.byteholder.geoclipse.mapprovider;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.geotools.data.wms.WebMapServer;
import org.geotools.ows.ServiceException;

import de.byteholder.geoclipse.Messages;

public class WmsServerWrapper {

	/**
	 * Connects to the capability url and returns the wms server or null when connection fails or
	 * when other errors occures. <br>
	 * <br> {@link #getWmsError()} returns the error or <code>null</code> when no error occured
	 * 
	 * @param capsUrl
	 * @return Returns a {@link WebMapServer}
	 * @throws Exception
	 */
	public static WebMapServer getWmsServer(final String capsUrl) throws Exception {

		// these variables must be final to be accessible in the runnable

		final WebMapServer[] wmsServer = { null };
		final Exception[] exception = { null };
		final String message[] = { null };

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				try {

					wmsServer[0] = new WebMapServer(new URL(capsUrl));

				} catch (final MalformedURLException e) {

					exception[0] = e;
					message[0] = NLS.bind(Messages.DBG028_Wms_Server_Error_MalformedUrl, e.getMessage(), capsUrl);

				} catch (final FileNotFoundException e) {

					exception[0] = e;
					message[0] = NLS.bind(Messages.DBG029_Wms_Server_Error_FileNotFound, e.getMessage(), capsUrl);

				} catch (final UnknownHostException e) {

					exception[0] = e;
					message[0] = NLS.bind(
							Messages.DBG030_Wms_Server_Error_CannotConnectToServer,
							e.getMessage(),
							capsUrl);

				} catch (final IOException e) {

					exception[0] = e;
					message[0] = NLS.bind(Messages.DBG031_Wms_Server_Error_IoExeption, e.getMessage(), capsUrl);

				} catch (final ServiceException e) {

					exception[0] = e;
					message[0] = NLS.bind(Messages.DBG032_Wms_Server_Error_ServiceExeption, e.getMessage(), capsUrl);

				} catch (final Exception e) {
					exception[0] = e;
					message[0] = NLS.bind(Messages.DBG033_Wms_Server_Error_OtherExeption, e.getMessage(), capsUrl);
				}
			}
		});

		if (exception[0] != null) {
			throw new Exception(message[0] + Messages.Wms_ServerError_Info, exception[0]);
		}

		return wmsServer[0];
	}
}
