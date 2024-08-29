/*******************************************************************************
 * Copyright (C) 2005, 2024 Wolfgang Schramm and Contributors
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
/**
 * @author Alfred Barten
 */
package net.tourbook.common.color;

import org.eclipse.swt.graphics.Image;

public class Map3ProfileImage extends ProfileImage implements Cloneable {

   @Override
   public Image createImage(final int width, final int height, final boolean isHorizontal) {

      // this seems to be not used anymore

      return null;
   }

   @Override
   public int getRGB(final long value) {

      // this seems to be not used anymore

      return 0xFC76FF;
   }

}
