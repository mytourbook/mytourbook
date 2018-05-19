/*
 * Original: org.oscim.renderer.LocationRenderer
 */
package net.tourbook.map25.layer.tourtrack;

import static org.oscim.backend.GLAdapter.*;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GL;
import org.oscim.backend.canvas.Color;
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

	private static final long	ANIM_RATE				= 50;
	private static final long	INTERVAL				= 2000;

	private static final float	CIRCLE_SIZE				= 50;
	private static final int	COLOR1					= 0xff00ffcc;
	private static final int	COLOR2					= 0xff3333cc;
	private static final int	SHOW_ACCURACY_ZOOM		= 16;

	private final Map			mMap;
	private final Layer			mLayer;
	private final float			mScale;

	private String				_shaderFile;
	private int					_shaderProgram;
	private int					_shader_VertexPosition;
	private int					_shader_MatrixPosition;
	private int					_shader_Scale;
	private int					_shader_Phase;
	private int					_shader_Direction;
	private int					_shader_Color;
	private int					_shader_Mode;

	private final Point			mScreenPoint			= new Point();
	private final Box			mBBox					= new Box();

	private boolean				_isInitialized;

	private final float[]		mColors[]				= new float[2][4];
	private final Point			mIndicatorPosition[]	= { new Point(), new Point() };

	/**
	 * When <code>true</code> the location is visible in the viewport otherwise it is outside of the
	 * viewport
	 */
	private final boolean		_isLocationVisible[]	= new boolean[2];

	private final Point			_locationLatLon[]		= {
			new Point(Double.NaN, Double.NaN),
			new Point(Double.NaN, Double.NaN)
	};

	private boolean				_isAnimationEnabled;
	private long				mAnimStart;

	private Callback			mCallback;

	private double				mRadius;
	private int					mShowAccuracyZoom		= SHOW_ACCURACY_ZOOM;

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

		mMap = map;
		mLayer = layer;
		mScale = scale;

		final float color1 = Color.aToFloat(COLOR1);
		mColors[0][0] = color1 * Color.rToFloat(COLOR1);
		mColors[0][1] = color1 * Color.gToFloat(COLOR1);
		mColors[0][2] = color1 * Color.bToFloat(COLOR1);
		mColors[0][3] = color1;

		final float color2 = Color.aToFloat(COLOR2);
		mColors[1][0] = color2 * Color.rToFloat(COLOR2);
		mColors[1][1] = color2 * Color.gToFloat(COLOR2);
		mColors[1][2] = color2 * Color.bToFloat(COLOR2);
		mColors[1][3] = color2;
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
				mMap.postDelayed(this, Math.min(ANIM_RATE, diff));

				if (_isLocationVisible[0] == false || _isLocationVisible[1] == false) {
					mMap.render();
				}

				lastRun = System.currentTimeMillis();
			}
		};

		mAnimStart = System.currentTimeMillis();
		mMap.postDelayed(action, ANIM_RATE);
	}

	private float animPhase() {
		return (float) ((MapRenderer.frametime - mAnimStart) % INTERVAL) / INTERVAL;
	}

	private boolean init() {

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
		float radius = CIRCLE_SIZE * mScale;

		for (int locationIndex = 0; locationIndex < 2; locationIndex++) {

			boolean isViewShed = false;

			animate(true);

			final boolean isLocationVisible = _isLocationVisible[locationIndex];
			if (isLocationVisible == false

			/* || pos.zoomLevel < SHOW_ACCURACY_ZOOM */) {

				//animate(true);

			} else {

				if (viewPortPosition.zoomLevel >= mShowAccuracyZoom) {
					radius = (float) (mRadius * viewPortPosition.scale);
				}
				radius = Math.max(CIRCLE_SIZE * mScale, radius);

				isViewShed = true;
				//animate(false);
			}
			gl.uniform1f(_shader_Scale, radius);

			final Point locationPosition = mIndicatorPosition[locationIndex];
			final double x = locationPosition.x - viewPortPosition.x;
			final double y = locationPosition.y - viewPortPosition.y;
			final double tileScale = Tile.SIZE * viewPortPosition.scale;

			viewPort.mvp.setTransScale((float) (x * tileScale), (float) (y * tileScale), 1);
			viewPort.mvp.multiplyMM(viewPort.viewproj, viewPort.mvp);
			viewPort.mvp.setAsUniform(_shader_MatrixPosition);

			if (isViewShed == false) {

				float phase = Math.abs(animPhase() - 0.5f) * 2;
				//phase = Interpolation.fade.apply(phase);
				phase = Interpolation.swing.apply(phase);

				gl.uniform1f(_shader_Phase, 0.8f + phase * 0.2f);

			} else {
				gl.uniform1f(_shader_Phase, 1);
			}

			if (isViewShed && isLocationVisible) {

				if (mCallback != null && mCallback.hasRotation()) {

					float rotation = mCallback.getRotation();
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

			GLUtils.glUniform4fv(_shader_Color, 1, mColors[locationIndex]);

			gl.drawArrays(GL.TRIANGLE_STRIP, 0, 4);
		}
	}

	public void setCallback(final Callback callback) {
		mCallback = callback;
	}

	public void setColor(final int color) {

		final float color1 = Color.aToFloat(COLOR1);
		mColors[0][0] = color1 * Color.rToFloat(COLOR1);
		mColors[0][1] = color1 * Color.gToFloat(COLOR1);
		mColors[0][2] = color1 * Color.bToFloat(COLOR1);
		mColors[0][3] = color1;

		final float color2 = Color.aToFloat(COLOR2);
		mColors[1][0] = color2 * Color.rToFloat(COLOR2);
		mColors[1][1] = color2 * Color.gToFloat(COLOR2);
		mColors[1][2] = color2 * Color.bToFloat(COLOR2);
		mColors[1][3] = color2;
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

		mRadius = radius;
	}

	public void setShader(final String shaderFile) {

		_shaderFile = shaderFile;
		_isInitialized = false;
	}

	public void setShowAccuracyZoom(final int showAccuracyZoom) {
		mShowAccuracyZoom = showAccuracyZoom;
	}

	@Override
	public void update(final GLViewport v) {

		if (!_isInitialized) {
			init();
			_isInitialized = true;
		}

		if (mLayer.isEnabled() == false) {
			setReady(false);
			return;
		}

		/*
		 * if (!v.changed() && isReady()) return;
		 */

		setReady(true);

		final int width = mMap.getWidth();
		final int height = mMap.getHeight();

		// clamp location to a position that can be
		// savely translated to screen coordinates
		v.getBBox(mBBox, 0);

		for (int locationIndex = 0; locationIndex < 2; locationIndex++) {

			double x = _locationLatLon[locationIndex].x;
			double y = _locationLatLon[locationIndex].y;

			if (!mBBox.contains(_locationLatLon[locationIndex])) {
				x = FastMath.clamp(x, mBBox.xmin, mBBox.xmax);
				y = FastMath.clamp(y, mBBox.ymin, mBBox.ymax);
			}

			// get position of Location in pixel relative to screen center
			v.toScreenPoint(x, y, mScreenPoint);

			x = mScreenPoint.x + width / 2;
			y = mScreenPoint.y + height / 2;

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
			v.fromScreenPoint(x, y, mIndicatorPosition[locationIndex]);
		}
	}
}
