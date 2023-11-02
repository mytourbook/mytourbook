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
package net.tourbook.nutrition;

import org.eclipse.jface.viewers.ISelection;

public class Product implements ISelection {

//	public enum Type {Area, Way, Node};

//	private Type type;

   private String name;



   public String getName() {
      return name;
   }


//	/**
//	 * @return Return the recommended zoom level or -1 when the zoom level should not be changed
//	 */
//	public int getRecommendedZoom() {
//		return recommendedZoom;
//	}



   @Override
   public boolean isEmpty() {
      return false;
   }

//	public void setType(Type type) {
//		this.type = type;
//	}
//
//	public void setType(final String type) {
//
//		if (Type.Way.toString().toLowerCase().equals(type.toLowerCase())) {
//			this.type = Type.Way;
//		}
//		else if (Type.Node.toString().toLowerCase().equals(type.toLowerCase())) {
//			this.type = Type.Node;
//		}
//		else {
//			throw new IllegalArgumentException("Not a valid type: " + type);
//		}
//	}



   public void setName(final String name) {
      this.name = name;
   }




   @Override
   public String toString() {

      final StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(getName());



      return stringBuilder.toString();
   }
}
