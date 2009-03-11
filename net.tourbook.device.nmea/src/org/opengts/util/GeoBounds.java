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
//  2008/08/15  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;

import org.opengts.util.*;

public class GeoBounds
    implements Cloneable
{
    
    // ------------------------------------------------------------------------

    private double          maxLat      =  -90.0;
    private double          maxLon      = -180.0;
    private double          minLat      =   90.0;
    private double          minLon      =  180.0;

    /**
    *** Constructor
    **/
    public GeoBounds()
    {
        //
    }

    /**
    *** Constructor
    *** @param gp  An array points
    **/
    public GeoBounds(GeoPointProvider... gpp)
    {
        this.extendByPoint(gpp);
    }

    /**
    *** Constructor
    *** @param gp  An array circle center points
    *** @param radiusM  The circle radius
    **/
    public GeoBounds(double radiusM, GeoPointProvider... gpp)
    {
        this.extendByCircle(radiusM, gpp);
    }

    /**
    *** Copy constructor
    *** @param gb  The other GeoBounds to copy
    **/
    public GeoBounds(GeoBounds gb)
    {
        if (gb != null) {
            this.setMinLatitude(gb.getMinLatitude());
            this.setMaxLatitude(gb.getMaxLatitude());
            this.setMinLongitude(gb.getMinLongitude());
            this.setMaxLongitude(gb.getMaxLongitude());
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a copy of this GeoBounds
    *** @return a copy of this GeoBounds
    **/
    public Object clone()
    {
        return new GeoBounds(this);
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the maximum latitude
    *** @param maxLat  The maximum latitude
    **/
    public void setMaxLatitude(double maxLat)
    {
        this.maxLat = maxLat;
    }

    /**
    *** Gets the maximum latitude
    *** @return  The maximum latitude
    **/
    public double getMaxLatitude()
    {
        return this.maxLat;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the minimum latitude
    *** @param minLat  The minimum latitude
    **/
    public void setMinLatitude(double minLat)
    {
        this.minLat = minLat;
    }

    /**
    *** Gets the minimum latitude
    *** @return  The minimum latitude
    **/
    public double getMinLatitude()
    {
        return this.minLat;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the maximum longitude
    *** @param maxLon  The maximum longitude
    **/
    public void setMaxLongitude(double maxLon)
    {
        this.maxLon = maxLon;
    }

    public double getMaxLongitude()
    {
        return this.maxLon;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the minimum longitude
    *** @param minLon  The minimum longitude
    **/
    public void setMinLongitude(double minLon)
    {
        this.minLon = minLon;
    }

    /**
    *** Gets the minimum longitude
    *** @return  The minimum longitude
    **/
    public double getMinLongitude()
    {
        return this.minLon;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this bounds is valid
    *** @return True if this bounds is valid
    **/
    public boolean isValid()
    {
        if (!GeoPoint.isValid(this.getMinLatitude(), this.getMinLongitude())) {
            return false;
        } else
        if (!GeoPoint.isValid(this.getMaxLatitude(), this.getMaxLongitude())) {
            return false;
        } else
        if (this.getMinLatitude() > this.getMaxLatitude()) {
            return false;
        } else
        if (this.getMinLongitude() > this.getMaxLongitude()) {
            return false;
        } else {
            return true;
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Extend the bounds by the specified GeoPoint
    *** @param gp  The point added to the bounds
    **/
    public void extendByPoint(GeoPointProvider gpp)
    {
        if (gpp != null) {
            GeoPoint gp = gpp.getGeoPoint();
            double lat = gp.getLatitude();
            double lon = gp.getLongitude();
            if (lat > this.getMaxLatitude() ) { this.setMaxLatitude( lat); }
            if (lat < this.getMinLatitude() ) { this.setMinLatitude( lat); }
            if (lon > this.getMaxLongitude()) { this.setMaxLongitude(lon); }
            if (lon < this.getMinLongitude()) { this.setMinLongitude(lon); }
        }
    }

    /**
    *** Extend the bounds by the specified GeoPoint
    *** @param gp  The point added to the bounds
    **/
    public void extendByPoint(GeoPointProvider... gp)
    {
        if (!ListTools.isEmpty(gp)) {
            for (int i = 0; i < gp.length; i++) {
                this.extendByPoint(gp[i]);
            }
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Extend the bounds by the specified point/radius circle
    *** @param radiusM  The radius in meters
    *** @param gp       The center points of the circles
    **/
    public void extendByCircle(double radiusM, GeoPointProvider gpp)
    {
        if ((radiusM > 0.0) && (gpp != null)) {
            GeoPoint gp = gpp.getGeoPoint();
            if (gp.isValid()) {
                // could stand some optimization
                this.extendByPoint(gp.getHeadingPoint(radiusM,   0.0)); // top
                this.extendByPoint(gp.getHeadingPoint(radiusM,  90.0)); // right
                this.extendByPoint(gp.getHeadingPoint(radiusM, 180.0)); // bottom
                this.extendByPoint(gp.getHeadingPoint(radiusM, 270.0)); // left
            }
        }
    }

    /**
    *** Extend the bounds by the specified point/radius circle
    *** @param radiusM  The radius in meters
    *** @param gp       The center points of the circles
    **/
    public void extendByCircle(double radiusM, GeoPointProvider... gpp)
    {
        if ((radiusM > 0.0) && !ListTools.isEmpty(gpp)) {
            for (int i = 0; i < gpp.length; i++) {
                this.extendByCircle(radiusM, gpp[i]);
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the average center of the bounds
    *** @return The GepPoint representing the center of the bounds
    **/
    public GeoPoint getCenter()
    {
        double avgLat = (this.getMinLatitude() + this.getMaxLatitude()) / 2.0;
        double avgLon = (this.getMinLongitude() + this.getMaxLongitude()) / 2.0;
        return new GeoPoint(avgLat, avgLon);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Calculate/return the best meters-per-pixel
    *** @param W   The map pixel width
    *** @param H   The map pixel height
    *** @return The best meters-per-pixel zoom level
    **/
    public double getMetersPerPixel(int W, int H)
    {
        if ((W > 0) && (H > 0)) {
            double wMPP = this.getDeltaLongitudeMeters() / W;
            double hMPP = this.getDeltaLatitudeMeters()  / H;
            return (wMPP > hMPP)? wMPP : hMPP;
        } else {
            return 0.0;
        }
    }

    /**
    *** Gets the width of the bounds (in delta meters)
    *** @return The bounds width (ie. delta meters)
    **/
    public double getDeltaLongitudeMeters()
    {
        if (Math.abs(this.getMinLatitude()) < Math.abs(this.getMaxLatitude())) {
            GeoPoint gp1 = new GeoPoint(this.getMinLatitude(), this.getMinLongitude());
            GeoPoint gp2 = new GeoPoint(this.getMinLatitude(), this.getMaxLongitude());
            return gp1.metersToPoint(gp2);
        } else {
            GeoPoint gp1 = new GeoPoint(this.getMaxLatitude(), this.getMinLongitude());
            GeoPoint gp2 = new GeoPoint(this.getMaxLatitude(), this.getMaxLongitude());
            return gp1.metersToPoint(gp2);
        }
    }

    /**
    *** Gets the width of the bounds (in delta meters)
    *** @return The bounds width (ie. delta meters)
    **/
    public double getDeltaLatitudeMeters()
    {
        GeoPoint gp1 = new GeoPoint(this.getMinLatitude(), this.getMinLongitude());
        GeoPoint gp2 = new GeoPoint(this.getMaxLatitude(), this.getMinLongitude());
        return gp1.metersToPoint(gp2);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /**
    *** Return the pixel location of the specified GeoPoint within the specified dimensions
    *** @param gp  The GeoPoint
    *** @param W   The pixel width
    *** @param H   The pixel height
    *** @return The PixPoint location, or null if the point falls outside the bounds
    **/
    public PixelPoint getPixelLocation(GeoPoint gp, int W, int H)
    {
        
        /* null/invalid arguments */
        if ((gp == null) || !gp.isValid()) {
            return null;
        }
        
        /* location */
        double lat = gp.getLatitude();
        double lon = gp.getLongitude();
        
        /* outside bounds? */
        if ((lat < this.getMinLatitude()) && (lat > this.getMaxLatitude())) {
            return null;
        } else
        if ((lon < this.getMinLongitude()) && (lon > this.getMaxLongitude())) {
            return null;
        }
        
        /* calculate pixel location */
        // 0,0 is top,left
        double dX = (lon - this.getMinLongitude()) / this.getDeltaLatitude();
        double dY = (this.getMaxLatitude() - lat)  / this.getDeltaLongitude();
        return new PixelPoint((int)Math.round(dY * H), (int)Math.round(dX * W));
        
    }

    /**
    *** Gets the height of the bounds
    *** @return The bounds height (ie. delta latitude)
    **/
    public double getDeltaLatitude()
    {
        return this.getMaxLatitude() - this.getMinLatitude();
    }

    /**
    *** Gets the width of the bounds
    *** @return The bounds width (ie. delta longitude)
    **/
    public double getDeltaLongitude()
    {
        return this.getMaxLongitude() - this.getMinLongitude();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Return a String representation of this GeoBounds
    *** @return A String representation
    **/
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("MaxLat/Lon:");
        sb.append(this.getMaxLatitude()).append("/").append(this.getMaxLongitude());
        sb.append(",");
        sb.append("MinLat/Lon:");
        sb.append(this.getMinLatitude()).append("/").append(this.getMinLongitude());
       return sb.toString();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        
        GeoPoint gp = new GeoPoint(39.0, -100.0);
        double radiusM = 1000.0;
        
        GeoBounds gb = new GeoBounds();
        gb.extendByCircle(radiusM, gp);
        Print.logInfo("GeoBounds: " + gb);
        
    }
}