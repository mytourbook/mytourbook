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
package net.tourbook.importdata;

import java.io.File;
import java.util.List;

import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * Device which can transfer data
 */
public abstract class ExternalDevice {

   protected static final int RECEIVE_TIMEOUT   = 600;

   public String              name;
   public String              deviceId;
   public String              visibleName;

   public boolean             isBuildNewFileNames = true;

   public abstract IRunnableWithProgress createImportRunnable(String portName, List<File> receivedFiles);

   public abstract boolean isImportCanceled();
}
