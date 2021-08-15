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

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

import net.tourbook.common.CommonActivator;

public interface IconRequestHandler {

   /**
    * Handle icon image requests, e.g. /$MT-ICON$/photo-tooltip.png
    *
    * @param httpExchange
    * @param requestUriPath
    * @param log
    * @param isIconCommonRequest
    *           When <code>true</code> the the request is for a resource from the
    *           {@link CommonActivator} plugin, otherwise from {@link TourbookPlugin}
    * @throws IOException
    */
   public void handleIconRequest(HttpExchange httpExchange,
                                 String iconFileName,
                                 StringBuilder log,
                                 boolean isIconCommonRequest)
         throws IOException;
}
