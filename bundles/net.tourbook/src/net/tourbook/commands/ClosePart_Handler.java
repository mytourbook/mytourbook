/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package net.tourbook.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

/*
 * Source:  https://bugs.eclipse.org/bugs/show_bug.cgi?id=77014
 * and      org.eclipse.ui.internal.handlers.ClosePartHandler
 */

/**
 * Provide a Handler for the Close Part command. This can then be bound to
 * whatever keybinding the user prefers.
 *
 * @since 3.3
 */
public class ClosePart_Handler extends AbstractHandler {

   @Override
   public Object execute(final ExecutionEvent event) throws ExecutionException {

      final IWorkbenchPart part = HandlerUtil.getActivePartChecked(event);
      final IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

      if (part instanceof IEditorPart) {

         window.getActivePage().closeEditor((IEditorPart) part, true);

      } else if (part instanceof IViewPart) {

         window.getActivePage().hideView((IViewPart) part);
      }

      return null;
   }

}
