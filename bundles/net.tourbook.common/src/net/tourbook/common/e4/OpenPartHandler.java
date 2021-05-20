/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.common.e4;

import javax.inject.Named;

import net.tourbook.common.util.StatusUtil;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

public class OpenPartHandler {

   @Execute
   public void execute(final EPartService partService, @Optional @Named("net.tourbook.commandparameter.partId") final String partId) {

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] execute()")
//				+ ("\tpartId: " + partId)
////				+ ("\t: " + )
//		);
//// TODO remove SYSTEM.OUT.PRINTLN

      final MPart part = partService.showPart(partId, PartState.ACTIVATE);

      if (part == null) {
         StatusUtil.showStatus(new IllegalArgumentException(String.format("Part is not available with id '%s'", partId))); //$NON-NLS-1$
//			throw new IllegalArgumentException(String.format("Part is not available with id '%s'", partId));
      }
   }

}
