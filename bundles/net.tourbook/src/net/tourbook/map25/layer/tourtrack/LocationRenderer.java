/*
 * Original: org.oscim.renderer.LocationRenderer
 */
package net.tourbook.map25.layer.tourtrack;

import static org.oscim.backend.GLAdapter.*;

import net.tourbook.map25.Map25ConfigManager;

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

	private static final long	ANIM_RATE						= 50;
	private static final long	INTERVAL						= 2000;

	private static final int	SHOW_ACCURACY_ZOOM				= 16;

	private final Map			_map;
	private final Layer			_layer;

	private String				_shaderFile;
	private int					_shaderProgram;
	private int					_shader_VertexPosition;
	private int					_shader_MatrixPosition;
	private int					_shader_Scale;
	private int					_shader_Phase;
	private int					_shader_Direction;
	private int					_shader_Color;
	private int					_shader_Mode;

	private Callback			_render_Callback;
	private final float			_render_Scale;
	private double				_render_Radius;
	private float				_render_CircleSize				= 50;
	private int					_render_ShowAccuracyZoom		= SHOW_ACCURACY_ZOOM;

	private final float[]		_render_Colors[]				= new float[2][4];
	private final Point			_render_IndicatorPositions[]	= { new Point(), new Point() };

	private final Point			_screenPoint					= new Point();
	private final Box			_viewportBBox					= new Box();

	private boolean				_isShaderInitialized;

	/**
	 * When <code>true</code> the location is visible in the viewport otherwise it is outside of the
	 * viewport
	 */
	private final boolean		_isLocationVisible[]			= new boolean[2];

	private final Point			_locationLatLon[]				= {
			new Point(Double.NaN, Double.NaN),
			new Point(Double.NaN, Double.NaN)
	};

	private boolean				_isAnimationEnabled;
	private long				_animStart;

	private boolean				_isLocationModified;

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

		final Runnable action = new Runnable() {
			private long lastRun;

			@Override
			public void run() {
				if (!_isAnimationEnabled) {
					return;
				}

				final long diff = System.currentTimeMillis() - lastRun;
				_map.postDelayed(this, Math.min(ANIM_RATE, diff));

				if (_isLocationVisible[0] == false || _isLocationVisible[1] == false) {
					_map.render();
				}

				lastRun = System.currentTimeMillis();
			}
		};

		_animStart = System.currentTimeMillis();
		_map.postDelayed(action, ANIM_RATE);
	}

	private float animPhase() {
		return (float) ((MapRenderer.frametime - _animStart) % INTERVAL) / INTERVAL;
	}

	private boolean initShader() {

		final int program = GLShader.loadShader(_shaderFile != null ? _shaderFile : "location_1");
		if (program == 0) {
			return false;
		}

		_shaderProgram = program;
		_shader_VertexPosition = gl.getAttribLocation(program, "a_pos");
		_shader_MatrixPosition = gl.getUniformLocation(program, "u_mvp");
		_shader_Phase = gl.getUniformLocation(program, "u_phase");
		_shader_Scale = gl.getUniformLocation(program, "u_scale");
		_shader_Direction = gl.getUniformLocation(program, "u_dir");
		_shader_Color = gl.getUniformLocation(program, "u_color");
		_shader_Mode = gl.getUniformLocation(program, "u_mode");

		return true;
	}

	@Override
	public void render(final GLViewport viewPort) {

		GLState.useProgram(_shaderProgram);
		GLState.blend(true);
		GLState.test(false, false);

		GLState.enableVertexArrays(_shader_VertexPosition, -1);
		MapRenderer.bindQuadVertexVBO(_shader_VertexPosition/* , true */);

		final MapPosition viewPortPosition = viewPort.pos;
		float radius = _render_CircleSize * _render_Scale;

		for (int locationIndex = 0; locationIndex < 2; locationIndex++) {

			boolean isViewShed = false;

			animate(true);

			final boolean isLocationVisible = _isLocationVisible[locationIndex];
			if (isLocationVisible) {

				if (viewPortPosition.zoomLevel >= _render_ShowAccuracyZoom) {
					radius = (float) (_render_Radius * viewPortPosition.scale);
				}
				radius = Math.max(_render_CircleSize * _render_Scale, radius);

				isViewShed = true;
			}
			gl.uniform1f(_shader_Scale, radius);

			final Point locationPosition = _render_IndicatorPositions[locationIndex];
			final double x = locationPosition.x - viewPortPosition.x;
			final double y = locationPosition.y - viewPortPosition.y;
			final double tileScale = Tile.SIZE * viewPortPosition.scale;

			viewPort.mvp.setTransScale((float) (x * tileScale), (float) (y * tileScale), 1);
			viewPort.mvp.multiplyMM(viewPort.viewproj, viewPort.mvp);
			viewPort.mvp.setAsUniform(_shader_MatrixPosition);

			if (isViewShed) {

				gl.uniform1f(_shader_Phase, 1);

			} else {

				float phase = Math.abs(animPhase() - 0.5f) * 2;

				//phase = Interpolation.fade.apply(phase);
				phase = Interpolation.swing.apply(phase);

				gl.uniform1f(_shader_Phase, 0.8f + phase * 0.2f);

			}

			if (isViewShed && isLocationVisible) {

				if (_render_Callback != null && _render_Callback.hasRotation()) {

					float rotation = _render_Callback.getRotation();
					rotation -= 90;
					gl.uniform2f(_shader_Direction,
							(float) Math.cos(Math.toRadians(rotation)),
							(float) Math.sin(Math.toRadians(rotation)));
					gl.uniform1i(_shader_Mode, 1); // With bearing

				} else {

					gl.uniform2f(_shader_Direction, 0, 0);
					gl.uniform1i(_shader_Mode, 0); // Without bearing
				}

			} else {

				// Outside screen

				gl.uniform1i(_shader_Mode, -1);
			}

			GLUtils.glUniform4fv(_shader_Color, 1, _render_Colors[locationIndex]);

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

		final int width = _map.getWidth();
		final int height = _map.getHeight();

		// clamp location to a position that can be savely translated to screen coordinates
		viewport.getBBox(_viewportBBox, 0);

		for (int locationIndex = 0; locationIndex < 2; locationIndex++) {

			double x = _locationLatLon[locationIndex].x;
			double y = _locationLatLon[locationIndex].y;

			if (!_viewportBBox.contains(_locationLatLon[locationIndex])) {
				x = FastMath.clamp(x, _viewportBBox.xmin, _viewportBBox.xmax);
				y = FastMath.clamp(y, _viewportBBox.ymin, _viewportBBox.ymax);
			}

			// get position of location in pixel relative to screen center
			viewport.toScreenPoint(x, y, _screenPoint);

			x = _screenPoint.x + width / 2;
			y = _screenPoint.y + height / 2;

			// clip position to screen boundaries
			int visible = 0;

			if (x > width - 5) {
				x = width;
			} else if (x < 5) {
				x = 0;
			} else {
				visible++;
			}

			if (y > height - 5) {
				y = height;
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

	void updateConfig() {

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
	}
}
