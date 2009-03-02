/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.ext.srtm;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;


@SuppressWarnings("unchecked")
public class RGBVertex implements Comparable{

   RGB rgb;
   long elev;
   
   public RGBVertex() {
      elev = 0;
      rgb = new RGB(255, 255, 255); // WHITE
   }
   
   public RGBVertex(int red, int green, int blue, long elev) {
	   if ((red > 255) || (red < 0) ||
		   (green > 255) || (green < 0) ||
		   (blue > 255) || (blue < 0))
		   SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	   
	   this.elev = elev;
	   rgb = new RGB(red, green, blue);
   }

   public RGB getRGB() {
      return rgb;
   }

   public long getElevation() {
      return elev;
   }

   public void setRGB(RGB rgb) {
      this.rgb = rgb;
   }

   public void setElev(long l) {
      elev = l;
   }
   
   public int compareTo(Object anotherRGBVertex) throws ClassCastException {
       if (!(anotherRGBVertex instanceof RGBVertex))
         throw new ClassCastException(Messages.rgv_vertex_class_cast_exception);
       long anotherElev = ((RGBVertex) anotherRGBVertex).getElevation();
         
       if (this.elev < anotherElev) return (-1);
       if (this.elev > anotherElev) return 1;
       return 0;    
   }
   
   public String toString() {
	   return ""+elev+","+rgb.red+","+rgb.green+","+rgb.blue+";"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
   }

   public static void main(String[] args) {
   }
}
