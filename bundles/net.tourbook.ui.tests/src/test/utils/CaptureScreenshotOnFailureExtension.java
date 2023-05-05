/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package utils;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

public class CaptureScreenshotOnFailureExtension implements TestWatcher {

   private String constructFilename(final ExtensionContext context) {
      return "./target/" //$NON-NLS-1$
            + context.getClass().getCanonicalName() + "." //$NON-NLS-1$
            + context.getTestMethod().get().getName() + ".png"; //$NON-NLS-1$
   }

   @Override
   public void testFailed(final ExtensionContext context, final Throwable cause) {
      final String fileName = constructFilename(context);
      new SWTWorkbenchBot().captureScreenshot(fileName);
   }
}
