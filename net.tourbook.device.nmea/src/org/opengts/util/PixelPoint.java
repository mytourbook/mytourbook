// ----------------------------------------------------------------------------
// Copyright 2006-2009, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Change History:
//  2008/08/08  Martin D. Flynn
//     -Initial release
//  2008/08/15  Martin D. Flynn
//     -Added coordinate transformation 
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;

import org.opengts.util.*;

public class PixelPoint
    implements Cloneable
{

    // ------------------------------------------------------------------------

    /**
    *** A private function performing the 'square' of the argument
    *** @param X  The argument to 'square'
    *** @return The square of X (ie. 'X' raised to the 2nd power)
    **/
    private static double SQ(double X) { return X * X; }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private double dX = 0.0;
    private double dY = 0.0;
    private double dZ = 0.0;    // not fully used

    /**
    *** Constructor
    **/
    public PixelPoint()
    {
        this(0.0, 0.0, 0.0);
    }

    /**
    *** Constructor
    *** @param x  X coordinate
    *** @param y  Y coordinate
    **/
    public PixelPoint(int x, int y)
    {
        this(x, y, 0);
    }

    /**
    *** Constructor
    *** @param x  X coordinate
    *** @param y  Y coordinate
    *** @param z  Z coordinate
    **/
    public PixelPoint(int x, int y, int z)
    {
        this.setX(x);
        this.setY(y);
        this.setZ(z);
    }

    /**
    *** Constructor
    *** @param x  X coordinate
    *** @param y  Y coordinate
    **/
    public PixelPoint(double x, double y)
    {
        this(x, y, 0.0);
    }

    /**
    *** Constructor
    *** @param x  X coordinate
    *** @param y  Y coordinate
    *** @param z  Z coordinate
    **/
    public PixelPoint(double x, double y, double z)
    {
        this.setX(x);
        this.setY(y);
        this.setZ(z);
    }

    /**
    *** Copy Constructor
    *** @param pp  The PixelPoint to copy
    **/
    public PixelPoint(PixelPoint pp)
    {
        this.setX((pp != null)? pp.getX() : 0.0);
        this.setY((pp != null)? pp.getY() : 0.0);
        this.setZ((pp != null)? pp.getZ() : 0.0);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a copy of this PixelPoint
    *** @return A copy of this PixelPoint object
    **/
    public Object clone()
    {
        return new PixelPoint(this);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the X coordinate value
    *** @param x  The X coordinate value
    **/
    public void setX(int x)
    {
        this.dX = (double)x;
    }

    /**
    *** Sets the X coordinate value
    *** @param x  The X coordinate value
    **/
    public void setX(double x)
    {
        this.dX = x;
    }

    /**
    *** Gets the X coordinate value
    *** @return The X coordinate value
    **/
    public double getX()
    {
        return this.dX;
    }

    /**
    *** Gets the X coordinate value as an integer
    *** @return The X coordinate integer value
    **/
    public int getIntX()
    {
        return (int)Math.round(this.dX);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Y coordinate value
    *** @param y  The Y coordinate value
    **/
    public void setY(int y)
    {
        this.dY = (double)y;
    }

    /**
    *** Sets the Y coordinate value
    *** @param y  The Y coordinate value
    **/
    public void setY(double y)
    {
        this.dY = y;
    }

    /**
    *** Gets the Y coordinate value
    *** @return The Y coordinate value
    **/
    public double getY()
    {
        return this.dY;
    }

    /**
    *** Gets the Y coordinate value as an integer
    *** @return The Y coordinate integer value
    **/
    public int getIntY()
    {
        return (int)Math.round(this.dY);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Z coordinate value
    *** @param z  The Z coordinate value
    **/
    public void setZ(int z)
    {
        this.dZ = (double)z;
    }

    /**
    *** Sets the Z coordinate value
    *** @param z  The Z coordinate value
    **/
    public void setZ(double z)
    {
        this.dZ = z;
    }

    /**
    *** Gets the Z coordinate value
    *** @return The Z coordinate value
    **/
    public double getZ()
    {
        return this.dZ;
    }

    /**
    *** Gets the Z coordinate value as an integer
    *** @return The Z coordinate integer value
    **/
    public int getIntZ()
    {
        return (int)Math.round(this.dZ);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the distance from this PixelPoint to the specified PixelPoint.
    *** @param pp  The other PixelPoint
    *** @return The distance to the other PixelPoint
    **/
    public double distanceToPixel(PixelPoint pp)
    {
        double deltaX = this.getX() - pp.getX();
        double deltaY = this.getY() - pp.getY();
        double deltaZ = this.getZ() - pp.getZ();
        return Math.sqrt(SQ(deltaX) + SQ(deltaY) + SQ(deltaZ));
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // xn = a11*xm + a12*ym + a13*zm + a14
    // yn = a21*xm + a22*ym + a23*zm + a24
    // zn = a31*xm + a32*ym + a33*zm + a34

    /**
    *** Transforms this PixelPoint frame to world/parent 2D coordinates.
    *** @param thetaDeg  The degrees rotations in the world/parent coordinate frame.
    *** @return The new PixelPoint in world/parent 2D coordinates
    **/
    public PixelPoint toWorldCoordinates(double thetaDeg)
    {
        //  |  cos sin 0 |
        //  | -sin cos 0 |
        //  |   0   0  1 |
        double X        = this.getX();
        double Y        = this.getY();
        double thetaRad = thetaDeg * (Math.PI / 180.0);
        double cosTheta = Math.cos(thetaRad);
        double sinTheta = Math.sin(thetaRad);
        double newX     = (X *  cosTheta) + (Y * sinTheta);
        double newY     = (X * -sinTheta) + (Y * cosTheta);
        return new PixelPoint(newX, newY, this.getZ());
    }

    /**
    *** Transforms this PixelPoint frame (in world/parent coordinates) to a new coordinate
    *** frame rotated by the specified number of degrees.
    *** @param thetaDeg  The degrees rotations in the world/parent coordinate frame.
    *** @return The new PixelPoint in world/parent coordinates
    **/
    public PixelPoint fromWorldCoordinates(double thetaDeg)
    {
        return this.toWorldCoordinates(-thetaDeg);
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Format double value 
    *** @param v  The double value
    *** @param d  The number of decimal places
    **/
    private static String _format(double v, int d)
    {
        if (d < 0) {
            return String.valueOf(v);
        } else
        if (d == 0) {
            return String.valueOf((int)Math.round(v));
        } else {
            String fmt = "0." + StringTools.replicateString("0",d);
            return StringTools.format(v,fmt);
        }
    }
    
    /**
    *** Returns a string representation of this PixelPoint 
    *** @param dec  The number of decimal places
    *** @return The string representation of this PixelPoint 
    **/
    public String toString(int dec)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        sb.append(_format(this.getX(),dec));
        sb.append(",");
        sb.append(_format(this.getY(),dec));
        sb.append("]");
        return sb.toString();
    }

    /**
    *** Returns a string representation of this PixelPoint 
    *** @return The string representation of this PixelPoint 
    **/
    public String toString()
    {
        return this.toString(-1);
    }
   
   // ------------------------------------------------------------------------
   // ------------------------------------------------------------------------

   public static void main(String argv[])
   {
       RTConfig.setCommandLineArgs(argv);
       
       double X = RTConfig.getDouble("X",0.0);
       double Y = RTConfig.getDouble("Y",0.0);
       double T = RTConfig.getDouble("T",0.0);
       PixelPoint pp = new PixelPoint(X,Y);
       
       Print.sysPrintln(pp.toWorldCoordinates(T).toString(1));
   }

    // ------------------------------------------------------------------------

}