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

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.EnumMap;

import pixelitor.filters.levels.Channel;

/**
 * Manages RGB and individual color channel curves.
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurves {

   private static final int                  CURVE_PADDING   = 10;
   private static final int                  AXIS_PADDING    = 20;
   private static final int                  AXIS_SIZE       = 10;
   private static final int                  GRID_DENSITY    = 4;
   private static final BasicStroke          GRID_STROKE     = new BasicStroke(1);

   private final EnumMap<Channel, ToneCurve> curvesByChannel = new EnumMap<>(Channel.class);

   private Channel                           activeChannel;
   private int                               panelWidth      = 295;
   private int                               panelHeight     = 295;
   private int                               curveWidth      = 255;
   private int                               curveHeight     = 255;

   public ToneCurves() {

      // Initializes curves for each channel, setting RGB as the default active channel
      curvesByChannel.put(Channel.RGB, new ToneCurve(Channel.RGB));
      curvesByChannel.put(Channel.RED, new ToneCurve(Channel.RED));
      curvesByChannel.put(Channel.GREEN, new ToneCurve(Channel.GREEN));
      curvesByChannel.put(Channel.BLUE, new ToneCurve(Channel.BLUE));

      setActiveChannel(Channel.RGB);
   }

   /**
    * Draws the tone curve grid, scales, and curves for all channels.
    */
   public void draw(final Graphics2D g) {

//    final boolean darkTheme = Themes.getActive().isDark();

      g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

      // clear background
//    Colors.fillWith(darkTheme ? Color.BLACK : Color.WHITE, g, panelWidth, panelHeight);

      // Apply padding, adjust for y-axis inversion
      final var origTransform = g.getTransform();
      g.translate(CURVE_PADDING + AXIS_PADDING, CURVE_PADDING);
      g.translate(0, curveHeight);
      g.scale(1.0, -1.0);

      drawGrid(g);
      drawDiagonal(g);
      drawScales(g);
//    drawCurves(g, darkTheme);

      g.setTransform(origTransform);
   }

   /**
    * Draws the curves for each channel.
    */
   private void drawCurves(final Graphics2D g, final boolean darkTheme) {

      // draw the inactive curves first...
      for (final var entry : curvesByChannel.entrySet()) {
         if (entry.getKey() != activeChannel) {
            entry.getValue().draw(g, darkTheme);
         }
      }

      // ...then the active curve on top.
      curvesByChannel.get(activeChannel).draw(g, darkTheme);
   }

   /**
    * Draws the diagonal reference line for the
    * bottom-left to the top-right of the curve area.
    */
   private void drawDiagonal(final Graphics2D g) {

      g.setColor(Color.GRAY);
      g.setStroke(GRID_STROKE);
      g.drawLine(0, 0, curveWidth, curveHeight);
   }

   /**
    * Draws the tone curve grid lines in alternating light and dark colors.
    */
   private void drawGrid(final Graphics2D g) {

      final Path2D lightPath2D = new Path2D.Float();
      final Path2D darkPath2D = new Path2D.Float();

      final float gridWidth = (float) curveWidth / GRID_DENSITY;
      final float gridHeight = (float) curveHeight / GRID_DENSITY;

      for (int i = 0; i <= GRID_DENSITY; i++) {

         final Path2D path2D = i % 2 == 0 ? darkPath2D : lightPath2D;

         // horizontal
         path2D.moveTo(0, i * gridHeight);
         path2D.lineTo(curveWidth, i * gridHeight);

         // vertical
         path2D.moveTo(i * gridWidth, 0);
         path2D.lineTo(i * gridWidth, curveHeight);
      }

      g.setStroke(GRID_STROKE);

      g.setColor(Color.LIGHT_GRAY);
      g.draw(lightPath2D);

      g.setColor(Color.GRAY);
      g.draw(darkPath2D);
   }

   /**
    * Draws the gradient scales along the horizontal and
    * vertical axes based on the active channel.
    */
   private void drawScales(final Graphics2D g) {

      final Color gradientEndColor = (activeChannel == Channel.RGB)
            ? Color.WHITE
            : activeChannel.getDrawColor(true, false);

      // Horizontal gradient
      final var rectHor = new Rectangle2D.Float(0, -AXIS_PADDING, curveWidth, AXIS_SIZE);
      final var gradientHor = new GradientPaint(0, 0, Color.BLACK, curveWidth, 0, gradientEndColor);
      g.setPaint(gradientHor);
      g.fill(rectHor);
      g.setColor(Color.LIGHT_GRAY);
      g.draw(rectHor);

      // Vertical gradient
      final var rectVer = new Rectangle2D.Float(-AXIS_PADDING, 0, AXIS_SIZE, curveHeight);
      final var gradientVer = new GradientPaint(0, 0, Color.BLACK, 0, curveHeight, gradientEndColor);
      g.setPaint(gradientVer);
      g.fill(rectVer);
      g.setColor(Color.LIGHT_GRAY);
      g.draw(rectVer);
   }

   public Channel getActiveChannel() {
      return activeChannel;
   }

   public ToneCurve getActiveCurve() {
      return curvesByChannel.get(activeChannel);
   }

   public ToneCurve getCurve(final Channel channel) {

      return curvesByChannel.get(channel);
   }

   /**
    * Converts a point from user input coordinates
    * to normalized curve coordinates.
    */
   public void normalizePoint(final Point2D.Float p) {

      p.x -= CURVE_PADDING + AXIS_PADDING;
      p.y -= CURVE_PADDING;

      p.y = curveHeight - p.y;

      p.x /= curveWidth;
      p.y /= curveHeight;
   }

   public void reset() {

      for (final var entry : curvesByChannel.entrySet()) {
         entry.getValue().reset();
      }
   }

   public void setActiveChannel(final Channel channel) {

      if (activeChannel != channel) {

         if (activeChannel != null) {

            curvesByChannel.get(activeChannel).setActive(false);
         }

         curvesByChannel.get(channel).setActive(true);
         activeChannel = channel;
      }
   }

   /**
    * Resizes the tone curve area according to the given new panel width and height
    */
   public void setSize(final int newPanelWidth, final int newPanelHeight) {

      this.panelWidth = newPanelWidth;
      this.panelHeight = newPanelHeight;

      curveWidth = newPanelWidth - 2 * CURVE_PADDING - AXIS_PADDING;
      curveHeight = newPanelHeight - 2 * CURVE_PADDING - AXIS_PADDING;

      for (final var entry : curvesByChannel.entrySet()) {
         entry.getValue().setSize(curveWidth, curveHeight);
      }
   }
}
