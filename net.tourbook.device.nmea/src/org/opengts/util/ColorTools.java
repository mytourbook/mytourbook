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
//  2006/02/19  Martin D. Flynn
//      - Initial release
//  2006/04/17  Martin D. Flynn
//      - Add additional keywords to "parseColor"
//  2008/12/01  Martin D. Flynn
//      - Added 'RGB' class
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.color.*;

/**
*** Color handling and conversion tools
**/

public class ColorTools
{

    // ------------------------------------------------------------------------

    /**
    *** RGB class.
    **/
    public static class RGB
    {
        private int R   = 0;
        private int G   = 0;
        private int B   = 0;
        private int A   = -1;
        public RGB(int R, int G, int B) {
            this.R = R & 0xFF;
            this.G = G & 0xFF;
            this.B = B & 0xFF;
        }
        public int getRGB() {
            return (this.R << 16) | (this.G << 8) | (this.B << 0);
        }
        public String toString() {
            return ColorTools.toHexString(this.getRGB(), false);
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Creates a new Color that is lighter than the specified Color
    *** @param c       The Color to make lighter
    *** @param percent The percent to make lighter
    *** @return The new 'ligher' Color
    **/
    public static Color lighter(Color c, float percent)
    {
        float p = _bound(percent);
        float comp[] = c.getColorComponents(null);
        for (int i = 0; i < comp.length; i++) {
            comp[i] = _bound(comp[i] + ((1.0F - comp[i]) * p));
        }
        ColorSpace cs = c.getColorSpace();
        return new Color(cs, comp, _toFloat(c.getAlpha()));
    }

    /**
    *** Creates a new Color that is darker than the specified Color
    *** @param c       The Color to make darker
    *** @param percent The percent to make darker
    *** @return The new 'darker' Color
    **/
    public static Color darker(Color c, float percent)
    {
        float p = _bound(percent);
        float comp[] = c.getColorComponents(null);
        for (int i = 0; i < comp.length; i++) {
            comp[i] = _bound(comp[i] - (comp[i] * p));
        }
        return new Color(c.getColorSpace(), comp, _toFloat(c.getAlpha()));
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates a new Color that is the average of the 2 specified Colors, based
    *** on a 50% weighting from the first Color to the second Color.
    *** @param color1  The first Color which will be averaged
    *** @param color2  The second Color which will be averaged
    *** @return The new 'averaged' Color
    **/
    public static Color mix(Color color1, Color color2)
    {
        if (color1 == null) {
            return color2;
        } else
        if (color2 == null) {
            return color1;
        } else {
            float rgb1[] = color1.getRGBColorComponents(null);
            float rgb2[] = color2.getRGBColorComponents(null);
            float rgb[]  = new float[3];
            for (int i = 0; i < 3; i++) {
                rgb[i] = (rgb1[i] + rgb2[i]) / 2.0F;
            }
            return new Color(rgb[0], rgb[1], rgb[2]);
        }
    }

    /**
    *** Creates a new Color that is a mix of the 2 specified Colors, based on
    *** the specified 'weighting' from the first Color to the second Color.
    *** @param color1  The first Color which will be averaged
    *** @param color2  The second Color which will be averaged
    *** @param weight  The 'weighting' from the first Color to the second Color.
    ***                A 'weight' of '0.0' will produce the first Color.  
    ***                A 'weight' of '1.0' will produce the second Color.
    *** @return The new 'averaged' Color
    **/
    public static Color mix(Color color1, Color color2, float weight)
    {
        if (color1 == null) {
            return color2;
        } else
        if (color2 == null) {
            return color1;
        } else {
            float rgb1[] = color1.getRGBColorComponents(null);
            float rgb2[] = color2.getRGBColorComponents(null);
            float rgb[]  = new float[3];
            for (int i = 0; i < 3; i++) {
                rgb[i] = _bound(rgb1[i] + ((rgb2[i] - rgb1[i]) * weight));

            }
            return new Color(rgb[0], rgb[1], rgb[2]);
        }
    }

    /**
    *** Creates a new Color that is a mix of the 2 specified Colors, based on
    *** the specified 'weighting' from the first Color to the second Color.
    *** @param color1  The first Color which will be averaged
    *** @param color2  The second Color which will be averaged
    *** @param weight  The 'weighting' from the first Color to the second Color.
    ***                A 'weight' of '0.0' will produce the first Color.  
    ***                A 'weight' of '1.0' will produce the second Color.
    *** @return The new 'averaged' Color
    **/
    public static Color mix(Color color1, Color color2, double weight)
    {
        return ColorTools.mix(color1, color2, (float)weight);
    }

    // ------------------------------------------------------------------------

    /**
    *** Parses the Color String representation into a Color instance.
    *** @param color  The color String representation to parse
    *** @param dft    The default Color returned if unable to parse a Color from the String
    *** @return The parsed Color instance.
    **/
    public static Color parseColor(String color, Color dft)
    {
        if ((color == null) || color.equals("")) {
            return dft;
        } else
        if (color.equalsIgnoreCase("black")) {
            return Color.black;
        } else
        if (color.equalsIgnoreCase("blue")) {
            return Color.blue;
        } else
        if (color.equalsIgnoreCase("cyan")) {
            return Color.cyan;
        } else
        if (color.equalsIgnoreCase("darkGray")) {
            return Color.darkGray;
        } else
        if (color.equalsIgnoreCase("gray")) {
            return Color.gray;
        } else
        if (color.equalsIgnoreCase("green")) {
            return Color.green;
        } else
        if (color.equalsIgnoreCase("lightGray")) {
            return Color.lightGray;
        } else
        if (color.equalsIgnoreCase("magenta")) {
            return Color.magenta;
        } else
        if (color.equalsIgnoreCase("orange")) {
            return Color.orange;
        } else
        if (color.equalsIgnoreCase("pink")) {
            return Color.pink;
        } else
        if (color.equalsIgnoreCase("red")) {
            return Color.red;
        } else
        if (color.equalsIgnoreCase("white")) {
            return Color.white;
        } else
        if (color.equalsIgnoreCase("yellow")) {
            return Color.yellow;
        } else {
            String c = color.startsWith("#")? color.substring(1) : color;
            byte rgb[] = StringTools.parseHex(c, null);
            if (rgb != null) {
                int r = (rgb.length > 0)? ((int)rgb[0] & 0xFF) : 0;
                int g = (rgb.length > 1)? ((int)rgb[1] & 0xFF) : 0;
                int b = (rgb.length > 2)? ((int)rgb[2] & 0xFF) : 0;
                int a = (rgb.length > 3)? ((int)rgb[3] & 0xFF) : 0;
                return (rgb.length > 3)? new Color(r,g,b,a) : new Color(r,g,b);
            } else {
                return dft;
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a hex String representation of the specified Color.
    *** @param color  The Color to convert to a String representation
    *** @return The Color String representation
    **/
    public static String toHexString(Color color)
    {
        return ColorTools.toHexString(color, true);
    }

    /**
    *** Returns a hex String representation of the specified Color.
    *** @param color  The Color to convert to a String representation
    *** @return The Color String representation
    **/
    public static String toHexString(Color color, boolean inclHash)
    {
        if (color != null) {
            return ColorTools.toHexString(color.getRGB(), inclHash);
        } else {
            return "";
        }
    }

    /**
    *** Returns a hex String representation of the specified Color.
    *** @param color  The RGB color to convert to a String representation
    *** @return The color String representation
    **/
    public static String toHexString(RGB color, boolean inclHash)
    {
        if (color != null) {
            return ColorTools.toHexString(color.getRGB(), inclHash);
        } else {
            return "";
        }
    }

    /**
    *** Returns a hex String representation of the specified Color.
    *** @param color  The RGB value to convert to a String representation
    *** @return The color String representation
    **/
    public static String toHexString(int color, boolean inclHash)
    {
        String v = Integer.toHexString(color | 0xFF000000).substring(2);
        return inclHash? ("#" + v) : v;
    }

    // ------------------------------------------------------------------------

    /**
    *** Converts the specified 0..255 color value to a float
    *** @param colorVal The 0..255 color value
    *** @return The float value
    **/
    private static float _toFloat(int colorVal)
    {
        return _bound((float)colorVal / 255.0F);
    }

    /**
    *** Performs bounds checking on the specified 0..255 color value
    *** @param colorVal  The color value to bounds check
    *** @return A color value that is guaranteed to be within 0..255
    **/
    private static int _bound(int colorVal)
    {
        if (colorVal <   0) { colorVal =   0; }
        if (colorVal > 255) { colorVal = 255; }
        return colorVal;
    }

    /**
    *** Performs bounds checking on the specified 0..1 color value
    *** @param colorVal  The color value to bounds check
    *** @return A color value that is guaranteed to be within 0..1 (inclusive)
    **/
    private static double _bound(double colorVal)
    {
        if (colorVal < 0.0) { colorVal = 0.0; }
        if (colorVal > 1.0) { colorVal = 1.0; }
        return colorVal;
    }

    /**
    *** Performs bounds checking on the specified 0..1 color value
    *** @param colorVal  The color value to bounds check
    *** @return A color value that is guaranteed to be within 0..1 (inclusive)
    **/
    private static float _bound(float colorVal)
    {
        if (colorVal < 0.0F) { colorVal = 0.0F; }
        if (colorVal > 1.0F) { colorVal = 1.0F; }
        return colorVal;
    }

    // ------------------------------------------------------------------------

    //public static void main(String argv[])
    //{
    //    RTConfig.setCommandLineArgs(argv);
    //    String data = (argv.length > 0)? argv[0] : "010203";
    //    Color c = parseColor(data, null);
    //    Print.logInfo("Color: " + c);
    //    Print.logInfo("Color: " + ColorTools.toHexString(c));
    //}

}
