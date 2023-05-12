/*
 * Original: org.oscim.renderer.LocationRenderer
 */
package net.tourbook.map25.renderer;

import static org.oscim.backend.GLAdapter.gl;

import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.layer.tourtrack.Map25TrackConfig;

import org.eclipse.swt.graphics.RGB;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GL;
import org.oscim.core.Box;
import org.oscim.core.MapPosition;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.LayerRenderer;
import org.oscim.renderer.MapRenderer;
import org.oscim.utils.FastMath;
import org.oscim.utils.math.Interpolation;

public class LocationRenderer extends LayerRenderer {

   private static final long ANIM_RATE                    = 50;
   private static final long ANIMATION_DURATION           = 2000;                        // milliseconds

   private static final int  SHOW_ACCURACY_ZOOM           = 16;

   private final Map         _map;
   private final Layer       _layer;

   private String            _shaderFile;
   private int               _shaderProgram;
   private int               _shader_a_pos;
   private int               _shader_u_mvp;
   private int               _shader_u_scale;
   private int               _shader_u_phase;
   private int               _shader_u_dir;
   private int               _shader_u_color;
   private int               _shader_u_mode;

   private Callback          _render_Callback;
   private final float       _render_Scale;
   private double            _render_Radius;
   private float             _render_CircleSize           = 50;
   private int               _render_ShowAccuracyZoom     = SHOW_ACCURACY_ZOOM;

   private final float[]     _render_Colors[]             = new float[2][4];
   private final Point       _render_IndicatorPositions[] = { new Point(), new Point() };

   private final Point       _screenPoint                 = new Point();
   private final Box         _viewportBBox                = new Box();

   private boolean           _isShaderInitialized;

   /**
    * When <code>true</code> the location is visible in the viewport otherwise it is outside of the
    * viewport
    */
   private final boolean     _isLocationVisible[]         = new boolean[2];

   /**
    * Projected lat/lon -> 0...1
    */
   private final Point       _locationLatLon[]            = {
         new Point(Double.NaN, Double.NaN),
         new Point(Double.NaN, Double.NaN)
   };

   private boolean           _isAnimationEnabled;
   private long              _animStart;

   private boolean           _isLocationModified;

   public interface Callback {

      float getRotation();

      /**
       * Usually true, can be used with e.g. Android Location.hasBearing().
       */
      boolean hasRotation();
   }

   public LocationRenderer(final Map map, final Layer layer) {
      this(map, layer, CanvasAdapter.getScale());
   }

   public LocationRenderer(final Map map, final Layer layer, final float scale) {

      _map = map;
      _layer = layer;

      _render_Scale = scale;

      updateConfig();
   }

   public void animate(final boolean isEnabled) {

      if (_isAnimationEnabled == isEnabled) {
         return;
      }

      _isAnimationEnabled = isEnabled;

      if (isEnabled == false) {
         return;
      }

      final Runnable animationRunnable = new Runnable() {

         private long __lastRun;

         @Override
         public void run() {

            if (!_isAnimationEnabled) {
               return;
            }

            final long timeDiff = System.currentTimeMillis() - __lastRun;

            _map.postDelayed(this, Math.min(ANIM_RATE, timeDiff));

            if (_isLocationVisible[0] == false || _isLocationVisible[1] == false) {
               _map.render();
            }

            __lastRun = System.currentTimeMillis();
         }
      };

      _animStart = System.currentTimeMillis();
      _map.postDelayed(animationRunnable, ANIM_RATE);
   }

   private float animPhase() {

      return (float) ((MapRenderer.frametime - _animStart) % ANIMATION_DURATION) / ANIMATION_DURATION;
   }

   private boolean initShader() {

      final int program = GLShader.loadShader(_shaderFile != null ? _shaderFile : "location_1"); //$NON-NLS-1$
      if (program == 0) {
         return false;
      }

// SET_FORMATTING_OFF

      _shaderProgram       = program;

      _shader_a_pos        = gl.getAttribLocation (program, "a_pos"); //$NON-NLS-1$
      _shader_u_mvp        = gl.getUniformLocation(program, "u_mvp"); //$NON-NLS-1$
      _shader_u_phase      = gl.getUniformLocation(program, "u_phase"); //$NON-NLS-1$
      _shader_u_scale      = gl.getUniformLocation(program, "u_scale"); //$NON-NLS-1$
      _shader_u_dir        = gl.getUniformLocation(program, "u_dir"); //$NON-NLS-1$
      _shader_u_color      = gl.getUniformLocation(program, "u_color"); //$NON-NLS-1$
      _shader_u_mode       = gl.getUniformLocation(program, "u_mode"); //$NON-NLS-1$

// SET_FORMATTING_ON

      return true;
   }

   @Override
   public void render(final GLViewport viewPort) {

      GLState.useProgram(_shaderProgram);
      GLState.blend(true);
      GLState.test(false, false);

      GLState.enableVertexArrays(_shader_a_pos, -1);
      MapRenderer.bindQuadVertexVBO(_shader_a_pos/* , true */);

      final MapPosition viewPort_MapPosition = viewPort.pos;
      float radius = _render_CircleSize * _render_Scale;

      for (int locationIndex = 0; locationIndex < 2; locationIndex++) {

         boolean isViewShed = false;

// reduce CPU cycles, this could be improved by making it customizable
//			animate(true);

         final boolean isLocationVisible = _isLocationVisible[locationIndex];
         if (isLocationVisible) {

            if (viewPort_MapPosition.zoomLevel >= _render_ShowAccuracyZoom) {
               radius = (float) (_render_Radius * viewPort_MapPosition.scale);
            }
            radius = Math.max(_render_CircleSize * _render_Scale, radius);

            isViewShed = true;
         }
         gl.uniform1f(_shader_u_scale, radius);

         final Point locationPosition = _render_IndicatorPositions[locationIndex];
         final double diffX = locationPosition.x - viewPort_MapPosition.x;
         final double diffY = locationPosition.y - viewPort_MapPosition.y;
         final double tileScale = Tile.SIZE * viewPort_MapPosition.scale;

         final float scaledDiffX = (float) (diffX * tileScale);
         final float scaledDiffY = (float) (diffY * tileScale);

         viewPort.mvp.setTransScale(scaledDiffX, scaledDiffY, 1);
         viewPort.mvp.multiplyMM(viewPort.viewproj, viewPort.mvp);
         viewPort.mvp.setAsUniform(_shader_u_mvp);

         if (isViewShed) {

            gl.uniform1f(_shader_u_phase, 1);

         } else {

            float phase = Math.abs(animPhase() - 0.5f) * 2;

            //phase = Interpolation.fade.apply(phase);
            phase = Interpolation.swing.apply(phase);

            gl.uniform1f(_shader_u_phase, 0.8f + phase * 0.2f);

         }

         if (isViewShed && isLocationVisible) {

            if (_render_Callback != null && _render_Callback.hasRotation()) {

               float rotation = _render_Callback.getRotation();
               rotation -= 90;
               gl.uniform2f(_shader_u_dir,
                     (float) Math.cos(Math.toRadians(rotation)),
                     (float) Math.sin(Math.toRadians(rotation)));
               gl.uniform1i(_shader_u_mode, 1); // With bearing

            } else {

               gl.uniform2f(_shader_u_dir, 0, 0);
               gl.uniform1i(_shader_u_mode, 0); // Without bearing
            }

         } else {

            // Outside screen

            gl.uniform1i(_shader_u_mode, -1);
         }

         GLUtils.glUniform4fv(_shader_u_color, 1, _render_Colors[locationIndex]);

         gl.drawArrays(GL.TRIANGLE_STRIP, 0, 4);
      }
   }

   public void setCallback(final Callback callback) {
      _render_Callback = callback;
   }

   public void setLocation(final double longitudeX,
                           final double latitudeY,
                           final double longitudeX2,
                           final double latitudeY2,
                           final double radius) {

      _locationLatLon[0].x = longitudeX;
      _locationLatLon[0].y = latitudeY;

      _locationLatLon[1].x = longitudeX2;
      _locationLatLon[1].y = latitudeY2;

      _render_Radius = radius;

      _isLocationModified = true;
   }

   public void setShader(final String shaderFile) {

      _shaderFile = shaderFile;
      _isShaderInitialized = false;
   }

   public void setShowAccuracyZoom(final int showAccuracyZoom) {
      _render_ShowAccuracyZoom = showAccuracyZoom;
   }

   @Override
   public void update(final GLViewport viewport) {

      if (_isShaderInitialized == false) {
         initShader();
         _isShaderInitialized = true;
      }

      if (_layer.isEnabled() == false) {

         // layer is hidden

         setReady(false);
         return;
      }

      // optimize performance
      if (viewport.changed() == false && _isLocationModified == false) {
         return;
      }

      _isLocationModified = false;

      setReady(true);

      final int mapWidth = _map.getWidth();
      final int mapHeight = _map.getHeight();

      // clamp location to a position that can be savely translated to screen coordinates
      viewport.getBBox(_viewportBBox, 0);

      for (int locationIndex = 0; locationIndex < 2; locationIndex++) {

         double x = _locationLatLon[locationIndex].x;
         double y = _locationLatLon[locationIndex].y;

         // clamp location to viewport
         if (!_viewportBBox.contains(_locationLatLon[locationIndex])) {
            x = FastMath.clamp(x, _viewportBBox.xmin, _viewportBBox.xmax);
            y = FastMath.clamp(y, _viewportBBox.ymin, _viewportBBox.ymax);
         }

         // get position of location in pixel relative to screen center
         viewport.toScreenPoint(x, y, _screenPoint);

         x = _screenPoint.x + mapWidth / 2;
         y = _screenPoint.y + mapHeight / 2;

         // clip position to screen boundaries
         int visible = 0;

         if (x > mapWidth - 5) {
            x = mapWidth;
         } else if (x < 5) {
            x = 0;
         } else {
            visible++;
         }

         if (y > mapHeight - 5) {
            y = mapHeight;
         } else if (y < 5) {
            y = 0;
         } else {
            visible++;
         }

         _isLocationVisible[locationIndex] = (visible == 2);

         // set location indicator position
         viewport.fromScreenPoint(x, y, _render_IndicatorPositions[locationIndex]);
      }
   }

   public void updateConfig() {

      final Map25TrackConfig config = Map25ConfigManager.getActiveTourTrackConfig();

      _render_CircleSize = config.sliderLocation_Size;

      final RGB leftColor = config.sliderLocation_Left_Color;
      final RGB rightColor = config.sliderLocation_Right_Color;
      final float opacity = config.sliderLocation_Opacity;

      _render_Colors[0][0] = leftColor.red / 255f;
      _render_Colors[0][1] = leftColor.green / 255f;
      _render_Colors[0][2] = leftColor.blue / 255f;
      _render_Colors[0][3] = opacity / 100f;

      _render_Colors[1][0] = rightColor.red / 255f;
      _render_Colors[1][1] = rightColor.green / 255f;
      _render_Colors[1][2] = rightColor.blue / 255f;
      _render_Colors[1][3] = opacity / 100f;

      _map.render();
   }
}
