/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package net.tourbook.map3.layer;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.geom.coords.MGRSCoord;
import gov.nasa.worldwind.geom.coords.UTMCoord;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.OGLTextRenderer;
import gov.nasa.worldwind.util.OGLUtil;
import gov.nasa.worldwind.util.WWMath;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.swing.Timer;

import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * @author jparsons
 * @version $Id: StatusLayer.java 1171 2013-02-11 21:45:02Z dcollins $
 */
/*
 * this is a copy from /gov/nasa/worldwindx/examples/util/StatusLayer.java - 2013-11-14
 */
/**
 * Renders statusbar information as a layer. Used ScalebarLayer and StatusBar as template
 */
//TODO
//  3. move some methods duplicated in statusbar to a utility class
//  6. add ability to put status text on top of window
public class StatusLayer extends AbstractLayer implements PositionListener, RenderingListener {

	public final static String	ID					= "net.tourbook.map3.layer.StatusLayer";		//$NON-NLS-1$

	public final static String	UNIT_METRIC			= "gov.nasa.worldwind.StatusLayer.Metric";
	public final static String	UNIT_IMPERIAL		= "gov.nasa.worldwind.StatusLayer.Imperial";

	private String				iconFilePath_bg		= "images/dot-clockwise-32.png";
	private Color				color				= Color.white;									//text color
//	private Font				defaultFont			= Font.decode("Arial-BOLD-12");
	private Font				defaultFont			= Font.decode("Arial-12");

	protected WorldWindow		_ww;

	protected String			latDisplay			= "";
	protected String			lonDisplay			= "";
	protected String			elevDisplay			= "";
	protected String			altDisplay			= "";
	private String				noNetwork			= "";
	private String				elevationUnit		= UNIT_METRIC;
	private boolean				showNetworkStatus	= true;
	private AtomicBoolean		isNetworkAvailable	= new AtomicBoolean(true);
	private boolean				activatedDownload	= false;
	private int					bgWidth;
	private int					bgHeight;
	private double				iconScale			= .5d;											//adjust icon size
	private Texture				iconTexture;
	private double				rotated				= 0.0d;
	private Color				backColor			= new Color(0f, 0f, 0f, 0.4f);
	protected int				coordDecimalPlaces	= 4;
	static int					rotationIncrement	= 60;

	// Draw it as ordered with an eye distance of 0 so that it shows up in front of most other things.
	private OrderedIcon			orderedImage		= new OrderedIcon();
	private final float[]		compArray			= new float[4];

	protected Position			previousPos;

	private class OrderedIcon implements OrderedRenderable {
		public double getDistanceFromEye() {
			return 0;
		}

		public void pick(final DrawContext dc, final Point pickPoint) {
			StatusLayer.this.draw(dc);
		}

		public void render(final DrawContext dc) {
			StatusLayer.this.draw(dc);
		}
	}

	public static class StatusMGRSLayer extends StatusLayer {
		private void handleCursorPositionChange(final PositionEvent event) {
			final Position newPos = event.getPosition();
			if (newPos != null) {
				//merge lat & lon into one field to display MGRS in lon field
				final String las = makeAngleDescription("Lat", newPos.getLatitude(), coordDecimalPlaces)
						+ " "
						+ makeAngleDescription("Lon", newPos.getLongitude(), coordDecimalPlaces);
				final String els = makeCursorElevationDescription(getEventSource()
						.getModel()
						.getGlobe()
						.getElevation(newPos.getLatitude(), newPos.getLongitude()));
				String los;
				try {
					final MGRSCoord MGRS = MGRSCoord.fromLatLon(
							newPos.getLatitude(),
							newPos.getLongitude(),
							getEventSource().getModel().getGlobe());
					los = MGRS.toString();
				} catch (final Exception e) {
					los = "";
				}
				latDisplay = las;
				lonDisplay = los;
				elevDisplay = els;

				if ((previousPos != null)
						&& (previousPos.getLatitude().compareTo(newPos.getLatitude()) != 0)
						&& (previousPos.getLongitude().compareTo(newPos.getLongitude()) != 0)) {
					this._ww.redraw();
				}
			} else {
				latDisplay = "";
				lonDisplay = Logging.getMessage("term.OffGlobe");
				elevDisplay = "";
			}
		}

		@Override
		public void moved(final PositionEvent event) {
			this.handleCursorPositionChange(event);
		}
	}

	public static class StatusUTMLayer extends StatusLayer {
		private void handleCursorPositionChange(final PositionEvent event) {
			final Position newPos = event.getPosition();
			if (newPos != null) {
				//merge lat & lon into one field to display UMT coordinates in lon field
				final String las = makeAngleDescription("Lat", newPos.getLatitude(), coordDecimalPlaces)
						+ " "
						+ makeAngleDescription("Lon", newPos.getLongitude(), coordDecimalPlaces);
				final String els = makeCursorElevationDescription(getEventSource()
						.getModel()
						.getGlobe()
						.getElevation(newPos.getLatitude(), newPos.getLongitude()));
				String los;
				try {
					final UTMCoord UTM = UTMCoord.fromLatLon(
							newPos.getLatitude(),
							newPos.getLongitude(),
							getEventSource().getModel().getGlobe());
					los = UTM.toString();
				} catch (final Exception e) {
					los = "";
				}
				latDisplay = las;
				lonDisplay = los;
				elevDisplay = els;

				if ((previousPos != null)
						&& (previousPos.getLatitude().compareTo(newPos.getLatitude()) != 0)
						&& (previousPos.getLongitude().compareTo(newPos.getLongitude()) != 0)) {
					this._ww.redraw();
				}
			} else {
				latDisplay = "";
				lonDisplay = Logging.getMessage("term.OffGlobe");
				elevDisplay = "";
			}
		}

		@Override
		public void moved(final PositionEvent event) {
			this.handleCursorPositionChange(event);
		}
	}

	public StatusLayer() {
		setPickEnabled(false);

		final Timer downloadTimer = new Timer(300, new ActionListener() {
			public void actionPerformed(final java.awt.event.ActionEvent actionEvent) {
				if (!showNetworkStatus) {
					activatedDownload = false;
					noNetwork = "";
					return;
				}

				if (!isNetworkAvailable.get()) {
					noNetwork = Logging.getMessage("term.NoNetwork");
					return;
				} else {
					noNetwork = "";
				}

				if (isNetworkAvailable.get() && WorldWind.getRetrievalService().hasActiveTasks()) {
					activatedDownload = true;
					bumpRotation();
					if (_ww != null) {
						_ww.redraw(); //smooth graphic
					}
				} else {
					if (activatedDownload && (_ww != null)) {
						_ww.redraw(); //force a redraw to clear downloading graphic
					}
					activatedDownload = false;
				}
			}
		});
		downloadTimer.start();

		final Timer netCheckTimer = new Timer(10000, new ActionListener() {
			public void actionPerformed(final java.awt.event.ActionEvent actionEvent) {
				if (!showNetworkStatus) {
					return;
				}

				final Thread t = new Thread(new Runnable() {
					public void run() {
						isNetworkAvailable.set(!WorldWind.getNetworkStatus().isNetworkUnavailable());
					}
				});
				t.start();
			}
		});
		netCheckTimer.start();
	}

	private void bumpRotation() {
		if (rotated > rotationIncrement) {
			rotated = rotated - rotationIncrement;
		} else {
			rotated = 360;
		}
	}

	@Override
	public void doPick(final DrawContext dc, final Point pickPoint) {
		// Delegate drawing to the ordered renderable list
		dc.addOrderedRenderable(this.orderedImage);
	}

	// Rendering
	@Override
	public void doRender(final DrawContext dc) {
		dc.addOrderedRenderable(this.orderedImage);
	}

	// Rendering
	public void draw(final DrawContext dc) {

//		final long start = System.nanoTime();

		final GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
		boolean attribsPushed = false;
		boolean modelviewPushed = false;
		boolean projectionPushed = false;
		try {
			gl.glPushAttrib(GL2.GL_DEPTH_BUFFER_BIT
					| GL2.GL_COLOR_BUFFER_BIT
					| GL2.GL_ENABLE_BIT
					| GL2.GL_TRANSFORM_BIT
					| GL2.GL_VIEWPORT_BIT
					| GL2.GL_CURRENT_BIT);
			attribsPushed = true;

			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			gl.glDisable(GL.GL_DEPTH_TEST);

			// Load a parallel projection with xy dimensions (viewportWidth, viewportHeight)
			// into the GL projection matrix.
			final java.awt.Rectangle viewport = dc.getView().getViewport();
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPushMatrix();
			projectionPushed = true;
			gl.glLoadIdentity();

			final String label = String.format("%s   %s   %s   %s", altDisplay, latDisplay, lonDisplay, elevDisplay);
			Dimension size = getTextRenderSize(dc, label);
			if (size.width < viewport.getWidth()) //todo more accurate add size of graphic
			{
				final double maxwh = size.width > size.height ? size.width : size.height;
				gl.glOrtho(0d, viewport.width, 0d, viewport.height, -0.6 * maxwh, 0.6 * maxwh);

				gl.glMatrixMode(GL2.GL_MODELVIEW);
				gl.glPushMatrix();
				modelviewPushed = true;
				gl.glLoadIdentity();

				final int iconHeight = 16;
				if (backColor != null) {
					drawFilledRectangle(
							dc,
							new Vec4(0, 0, 0),
							new Dimension((int) viewport.getWidth(), Math.max((int) size.getHeight(), iconHeight)),
							this.backColor);
				}
				final int verticalSpacing = 2;
				drawLabel(dc, label, new Vec4(1, verticalSpacing, 0), this.color);

				if (noNetwork.length() > 0) {
					size = getTextRenderSize(dc, noNetwork);
					final double x = viewport.getWidth() - size.getWidth();
					drawLabel(dc, noNetwork, new Vec4(x, verticalSpacing, 0), Color.RED);

				} else if (activatedDownload) {

					//draw background image
					if (iconTexture == null) {
						initBGTexture(dc);
					}

					final double width = this.getScaledBGWidth();
					final double height = this.getScaledBGHeight();

					if (iconTexture != null) {
						gl.glTranslated(viewport.getWidth() - width, 0, 0d);
						gl.glTranslated(width / 2, height / 2, 0);
						gl.glRotated(rotated, 0d, 0d, 1d);
						gl.glTranslated(-width / 2, -height / 2, 0);

						if (iconTexture != null) {
							gl.glEnable(GL.GL_TEXTURE_2D);
							iconTexture.bind(gl);
							gl.glColor4d(1d, 1d, 1d, this.getOpacity());
							gl.glEnable(GL.GL_BLEND);
							gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
							final TextureCoords texCoords = iconTexture.getImageTexCoords();
							gl.glScaled(width, height, 1d);
							dc.drawUnitQuad(texCoords);
							gl.glDisable(GL.GL_TEXTURE_2D);
							gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
						}
					}
				}
			}
		} finally {
			if (projectionPushed) {
				gl.glMatrixMode(GL2.GL_PROJECTION);
				gl.glPopMatrix();
			}
			if (modelviewPushed) {
				gl.glMatrixMode(GL2.GL_MODELVIEW);
				gl.glPopMatrix();
			}
			if (attribsPushed) {
				gl.glPopAttrib();
			}
		}

//		System.out.println((UI.timeStampNano() + " " + this.getClass().getName() + " \t")
//				+ (((float) (System.nanoTime() - start) / 1000000) + " ms"));
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	private void drawFilledRectangle(	final DrawContext dc,
										final Vec4 origin,
										final Dimension dimension,
										final Color color) {
		final GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
		gl.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha());
		gl.glBegin(GL2.GL_POLYGON);
		gl.glVertex3d(origin.x, origin.y, 0);
		gl.glVertex3d(origin.x + dimension.getWidth(), origin.y, 0);
		gl.glVertex3d(origin.x + dimension.getWidth(), origin.y + dimension.getHeight(), 0);
		gl.glVertex3d(origin.x, origin.y + dimension.getHeight(), 0);
		gl.glVertex3d(origin.x, origin.y, 0);
		gl.glEnd();
	}

	// Draw the label
	private void drawLabel(final DrawContext dc, final String text, final Vec4 screenPoint, final Color textColor) {
		final int x = (int) screenPoint.x();
		final int y = (int) screenPoint.y();

		final TextRenderer textRenderer = OGLTextRenderer.getOrCreateTextRenderer(
				dc.getTextRendererCache(),
				this.defaultFont);
		textRenderer.begin3DRendering();
		textRenderer.setColor(this.getBackgroundColor(textColor));
		textRenderer.draw(text, x + 1, y - 1);
		textRenderer.setColor(textColor);
		textRenderer.draw(text, x, y);
		textRenderer.end3DRendering();
	}

	public Color getBackColor() {
		return backColor;
	}

	// Compute background color for best contrast
	private Color getBackgroundColor(final Color color) {
		Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), compArray);
		if (compArray[2] > 0.5) {
			return new Color(0, 0, 0, 0.7f);
		} else {
			return new Color(1, 1, 1, 0.7f);
		}
	}

	public int getCoordSigDigits() {
		return coordDecimalPlaces;
	}

	public Font getDefaultFont() {
		return defaultFont;
	}

	protected WorldWindow getEventSource() {
		return _ww;
	}

	private double getScaledBGHeight() {
		return this.bgHeight * this.iconScale;
	}

	private double getScaledBGWidth() {
		return this.bgWidth * this.iconScale;
	}

	private Dimension getTextRenderSize(final DrawContext dc, final String text) {
		final TextRenderer textRenderer = OGLTextRenderer.getOrCreateTextRenderer(
				dc.getTextRendererCache(),
				this.defaultFont);
		final Rectangle2D nameBound = textRenderer.getBounds(text);

		return nameBound.getBounds().getSize();
	}

	private void handleCursorPositionChange(final PositionEvent event) {
		final Position newPos = event.getPosition();
		if (newPos != null) {
			latDisplay = makeAngleDescription("Lat", newPos.getLatitude(), coordDecimalPlaces);
			lonDisplay = makeAngleDescription("Lon", newPos.getLongitude(), coordDecimalPlaces);
			elevDisplay = makeCursorElevationDescription(_ww
					.getModel()
					.getGlobe()
					.getElevation(newPos.getLatitude(), newPos.getLongitude()));

			//Need to force an extra draw.  without this the displayed value lags the actual when just moving cursor
			if ((previousPos != null)
					&& (previousPos.getLatitude().compareTo(newPos.getLatitude()) != 0)
					&& (previousPos.getLongitude().compareTo(newPos.getLongitude()) != 0)) {
				this._ww.redraw();
			}
		} else {
			latDisplay = "";
			lonDisplay = Logging.getMessage("term.OffGlobe");
			elevDisplay = "";
		}

		previousPos = newPos;
	}

	private void initBGTexture(final DrawContext dc) {
		try {
			InputStream iconStream = this.getClass().getResourceAsStream("/" + iconFilePath_bg);
			if (iconStream == null) {
				final File iconFile = new File(iconFilePath_bg);
				if (iconFile.exists()) {
					iconStream = new FileInputStream(iconFile);
				}
			}

			final TextureData textureData = OGLUtil.newTextureData(dc.getGL().getGLProfile(), iconStream, false);
			iconTexture = TextureIO.newTexture(textureData);
			iconTexture.bind(dc.getGL());
			this.bgWidth = iconTexture.getWidth();
			this.bgHeight = iconTexture.getHeight();
		} catch (final IOException e) {
			final String msg = Logging.getMessage("layers.IOExceptionDuringInitialization");
			Logging.logger().severe(msg);
			throw new WWRuntimeException(msg, e);
		}
	}

	protected String makeAngleDescription(final String label, final Angle angle, final int places) {
		return String.format("%s %s", label, angle.toDecimalDegreesString(places));
	}

	protected String makeCursorElevationDescription(final double metersElevation) {
		final String elev = Logging.getMessage("term.Elev");
		if (UNIT_IMPERIAL.equals(elevationUnit)) {
			return String.format("%s %,d feet", elev, (int) Math.round(WWMath.convertMetersToFeet(metersElevation)));
		} else {
			// Default to metric units.
			return String.format("%s %,d meters", elev, (int) Math.round(metersElevation));
		}
	}

	protected String makeEyeAltitudeDescription(final double metersAltitude) {
		final String altitude = Logging.getMessage("term.Altitude");
		if (UNIT_IMPERIAL.equals(elevationUnit)) {
			return String.format("%s %,d mi", altitude, (int) Math.round(WWMath.convertMetersToMiles(metersAltitude)));
		} else {
			// Default to metric units.

			if (metersAltitude >= 10000) {
				return String.format("%s %,d km", altitude, (int) Math.round(metersAltitude / 1e3));
			} else {
				return String.format("%s %,.1f km", altitude, metersAltitude / 1000);
			}

		}
	}

	public void moved(final PositionEvent event) {
		this.handleCursorPositionChange(event);
	}

	public void setBackColor(final Color backColor) {
		if (backColor == null) {
			final String msg = Logging.getMessage("nullValue.ColorIsNull");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		this.backColor = backColor;
	}

	public void setCoordDecimalPlaces(final int coordDecimalPlaces) {
		this.coordDecimalPlaces = coordDecimalPlaces;
	}

	public void setDefaultFont(final Font font) {
		if (font == null) {
			final String msg = Logging.getMessage("nullValue.FontIsNull");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		this.defaultFont = font;
	}

	public void setElevationUnits(final String units) {
		elevationUnit = units;
	}

	public void setEventSource(final WorldWindow newEventSource) {
		if (newEventSource == null) {
			final String msg = Logging.getMessage("nullValue.WorldWindow");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		if (this._ww != null) {
			this._ww.removePositionListener(this);
			this._ww.removeRenderingListener(this);
		}

		newEventSource.addPositionListener(this);
		newEventSource.addRenderingListener(this);

		this._ww = newEventSource;
	}

	public void stageChanged(final RenderingEvent event) {
		if (!event.getStage().equals(RenderingEvent.BEFORE_BUFFER_SWAP)) {
			return;
		}

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (_ww.getView() != null && _ww.getView().getEyePosition() != null) {
					altDisplay = makeEyeAltitudeDescription(_ww.getView().getEyePosition().getElevation());
				} else {
					altDisplay = (Logging.getMessage("term.Altitude"));
				}
			}
		});
	}

	@Override
	public String toString() {
		return Logging.getMessage("layers.StatusLayer.Name");
	}
}
