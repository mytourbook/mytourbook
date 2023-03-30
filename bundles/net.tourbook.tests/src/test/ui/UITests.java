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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.tourbook.tag.TagManager;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.Test;

import utils.FilesUtils;

public class UITests {

   public static final String FILES_PATH = FilesUtils.rootPath + "ui/files/"; //$NON-NLS-1$

   @Test
   void testResize_ShouldReturnImageWithNewSize() {

      final String imageFilePath = FILES_PATH + "mytourbook-256x256.png"; //$NON-NLS-1$

      final Image originalImage = new Image(Display.getDefault(), imageFilePath);

      assertAll(
            () -> assertNotNull(originalImage),
            () -> assertEquals(256, originalImage.getBounds().height),
            () -> assertEquals(256, originalImage.getBounds().width));

      final Image resizedImage = TagManager.prepareTagImage(imageFilePath);

      assertAll(
            () -> assertNotNull(resizedImage),
            () -> assertEquals(100, resizedImage.getBounds().height),
            () -> assertEquals(100, resizedImage.getBounds().width));
   }
}
