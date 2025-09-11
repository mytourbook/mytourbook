/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.curves;

import com.jhlabs.image.CurveValues;
import com.jhlabs.image.CurvesFilter;

import java.awt.image.BufferedImage;

import net.tourbook.common.UI;

import pixelitor.filters.Filter;
import pixelitor.filters.levels.Channel;

/**
 * Filter that applies tone curves adjustments for RGB and individual color channels.
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurvesFilter extends Filter /* extends FilterWithGUI */ {

   private static final char NL = UI.NEW_LINE;

   private CurvesFilter      _curvesFilter;   // jhlabs curves filter
   private final ToneCurves  _toneCurves;     // the curve adjustments

   public ToneCurvesFilter() {

      _toneCurves = new ToneCurves();
   }

   public ToneCurves getCurves() {

      return _toneCurves;
   }

   public CurvesFilter getCurvesFilter() {

      return _curvesFilter;
   }

   @Override
   public BufferedImage transform(final BufferedImage src, final BufferedImage dest) {

      if (_curvesFilter == null) {

         _curvesFilter = new CurvesFilter("filtername"); //$NON-NLS-1$
      }

      if (_toneCurves == null) {

         return src;
      }

      final ToneCurve toneCurve = _toneCurves.getCurve(Channel.RGB);
      final CurveValues curveValues = toneCurve.curveValues;

      _curvesFilter.setCurve(curveValues);

      return _curvesFilter.filter(src, dest);
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "ToneCurvesFilter" + NL //$NON-NLS-1$

            + " _curvesFilter = " + _curvesFilter + NL //$NON-NLS-1$
            + " _toneCurves   = " + _toneCurves + NL //$NON-NLS-1$

      ;
   }
}
