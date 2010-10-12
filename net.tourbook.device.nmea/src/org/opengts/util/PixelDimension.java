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
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;

import org.opengts.util.*;

public class PixelDimension
    implements Cloneable
{

    // ------------------------------------------------------------------------

    private int width  = 0;
    private int height = 0;
    
    public PixelDimension(int w, int h)
    {
        this.setWidth( w);
        this.setHeight(h);
    }
    
    public PixelDimension(PixelDimension pd)
    {
        this.setWidth( (pd != null)? pd.getWidth()  : 0);
        this.setHeight((pd != null)? pd.getHeight() : 0);
    }

    // ------------------------------------------------------------------------

    /* copy */
    public Object clone()
    {
        return new PixelDimension(this);
    }

    // ------------------------------------------------------------------------

    public void setWidth(int w)
    {
        this.width = w;
    }

    public int getWidth()
    {
        return this.width;
    }

    // ------------------------------------------------------------------------

    public void setHeight(int h)
    {
        this.height = h;
    }

    public int getHeight()
    {
        return this.height;
    }

    // ------------------------------------------------------------------------

    public boolean isValid()
    {
        return (this.width > 0) && (this.height > 0);
    }

    // ------------------------------------------------------------------------

}