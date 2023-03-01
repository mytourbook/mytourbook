/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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
package ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.tourbook.ui.UI;

import org.eclipse.swt.graphics.Image;
import org.junit.jupiter.api.Test;

import utils.FilesUtils;

public class UITests {

   public static final String FILES_PATH = FilesUtils.rootPath + "photo/files/"; //$NON-NLS-1$

   @Test
   void testResize_ShouldReturnImageWithNewSize() {

      final Image resizedImage = UI.prepareTagImage(FILES_PATH + "Black Diamond Trail Back Poles.png");

      assertNotNull(resizedImage);
      assertEquals(70, resizedImage.getBounds().height);
      assertEquals(70, resizedImage.getBounds().width);
   }
}
