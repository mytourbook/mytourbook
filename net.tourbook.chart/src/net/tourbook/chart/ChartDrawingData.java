/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.chart;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Rectangle;

public class ChartDrawingData {

	// position for the x-axis unit text
	protected static final int		X_UNIT_TEXT_POS_LEFT	= 0;
	protected static final int		X_UNIT_TEXT_POS_CENTER	= 1;

	public static final int			BAR_POS_LEFT			= 0;
	public static final int			BAR_POS_CENTER			= 1;
//	public static final int			BAR_POS_CENTER_UNIT_TICK	= 2;							// center bar in the unit tick

	private ChartDataXSerie			_xData;
	private ChartDataXSerie			_xData2nd;
	private ChartDataYSerie			_yData;

	private Rectangle[]				_ySliderHitRect;

	private String					_xTitle;

	/**
	 * management for the bar graph
	 */
	private Rectangle[][]			_barRectangles;
	private Rectangle[][]			_barFocusRectangles;

	private int						_barRectangleWidth;
	private int						_devBarRectangleXPos;

	/**
	 * Contains all unit labels and their positions for the x axis
	 */
	private ArrayList<ChartUnit>	_xUnits					= new ArrayList<ChartUnit>();
	private int						_xUnitTextPos			= X_UNIT_TEXT_POS_LEFT;

	/**
	 * List with all unit labels and positions for the y axis
	 */
	private ArrayList<ChartUnit>	_yUnits					= new ArrayList<ChartUnit>();

	// scaling from graph value to device value
	private float					_scaleX;
	private float					_scaleY;

	/**
	 * scaling the the x unit
	 */
	private float					_scaleUnitX				= Float.MIN_VALUE;

	private int						_devMarginTop;
	private int						_devXTitelBarHeight;
	private int						_devMarkerBarHeight;
	private int						_devSliderBarHeight;

	// graph position
	private int						_devYTop;
	private int						_devYBottom;

	/**
	 * virtual graph width in dev (pixel) units
	 */
	public int						devVirtualGraphWidth;

	/**
	 * graph height in dev (pixel) units, each graph has the same height
	 */
	public int						devGraphHeight;

	private int						_devSliderHeight;

	/**
	 * graph value for the bottom of the graph
	 */
	private int						_graphYBottom;
	private int						_graphYTop;

	private int						_barPosition			= BAR_POS_LEFT;

	private int						_chartType;

	private String					_errorMessage;

	private boolean					_isXUnitOverlapChecked	= false;

	private boolean					_isDrawVerticalGrid		= true;
	private boolean[]				_isDrawUnits			= null;

	public ChartDrawingData(final int chartType) {
		_chartType = chartType;
	}

	/**
	 * @return Returns the barFocusRectangles.
	 */
	public Rectangle[][] getBarFocusRectangles() {
		return _barFocusRectangles;
	}

	/**
	 * @return Returns the barPosition,<br>
	 *         this can be set to BAR_POS_LEFT, BAR_POS_CENTER
	 */
	public int getBarPosition() {
		return _barPosition;
	}

	/**
	 * @return Returns the barRectangles.
	 */
	public Rectangle[][] getBarRectangles() {
		return _barRectangles;
	}

	/**
	 * @return Returns the barRectangleWidth.
	 */
	public int getBarRectangleWidth() {
		return _barRectangleWidth;
	}

	public int getChartType() {
		return _chartType;
	}

	/**
	 * @return Returns the barRectanglePos.
	 */
	public int getDevBarRectangleXPos() {
		return _devBarRectangleXPos;
	}

//	public int getDevGraphHeight() {
//		return devGraphHeight;
//	}
//
//	/**
//	 * virtual graph width in dev (pixel) units
//	 */
//	int getDevGraphWidth() {
//		return devGraphWidth;
//	}

	/**
	 * @return Returns the devMarginTop.
	 */
	public int getDevMarginTop() {
		return _devMarginTop;
	}

	/**
	 * @return Returns the devMarkerBarHeight.
	 */
	public int getDevMarkerBarHeight() {
		return _devMarkerBarHeight;
	}

	/**
	 * @return Returns the devSliderBarHeight.
	 */
	public int getDevSliderBarHeight() {
		return _devSliderBarHeight;
	}

	public int getDevSliderHeight() {
		return _devSliderHeight;
	}

	/**
	 * @return Returns the devTitelBarHeight.
	 */
	public int getDevXTitelBarHeight() {
		return _devXTitelBarHeight;
	}

	/**
	 * @return Returns the bottom of the chart in dev units
	 */
	public int getDevYBottom() {
		return _devYBottom;
	}

	/**
	 * @return Returns the y position for the title
	 */
	public int getDevYTitle() {
		return getDevYBottom() - devGraphHeight - getDevSliderBarHeight() - getDevXTitelBarHeight();
	}

	/**
	 * @return Returns the top of the chart in dev units
	 */
	public int getDevYTop() {
		return _devYTop;
	}

	public String getErrorMessage() {
		return _errorMessage;
	}

	/**
	 * @return Returns the bottom of the chart in graph units
	 */
	public int getGraphYBottom() {
		return _graphYBottom;
	}

	/**
	 * @return Returns the top of the chart in graph units
	 */
	public int getGraphYTop() {
		return _graphYTop;
	}

	public float getScaleUnitX() {
		return _scaleUnitX;
	}

	public float getScaleX() {
		return _scaleX;
	}

	public float getScaleY() {
		return _scaleY;
	}

	/**
	 * @return Returns the xData.
	 */
	public ChartDataXSerie getXData() {
		return _xData;
	}

	/**
	 * @return Returns the xData2nd.
	 */
	public ChartDataXSerie getXData2nd() {
		return _xData2nd;
	}

	public String getXTitle() {
		return _xTitle;
	}

	/**
	 * @return Returns the units for the x-axis.
	 */
	public ArrayList<ChartUnit> getXUnits() {
		return _xUnits;
	}

	/**
	 * @return Returns the xUnitTextPos.
	 */
	public int getXUnitTextPos() {
		return _xUnitTextPos;
	}

	/**
	 * @return Returns the ChartDataXSerie for the y-axis
	 */
	public ChartDataYSerie getYData() {
		return _yData;
	}

	/**
	 * @return Returns the ySliderHitRect.
	 */
	public Rectangle[] getYSliderHitRect() {
		return _ySliderHitRect;
	}

	/**
	 * @return Returns the yUnits.
	 */
	public ArrayList<ChartUnit> getYUnits() {
		return _yUnits;
	}

	/**
	 * @return Returns an array for each unit tick. Unit tick is drawn when set to <code>true</code>
	 *         .
	 *         <p>
	 *         When <code>null</code> is returned all units are drawn.
	 */
	public boolean[] isDrawUnits() {
		return _isDrawUnits;
	}

	public boolean isDrawVerticalGrid() {
		return _isDrawVerticalGrid;
	}

	/**
	 * @return Returns <code>true</code> when the x-axis unit visibility is checked that it do not
	 *         overlap the previous unit label.
	 */
	public boolean isXUnitOverlapChecked() {
		return _isXUnitOverlapChecked;
	}

	/**
	 * @param barFocusRectangles
	 *            The barFocusRectangles to set.
	 */
	public void setBarFocusRectangles(final Rectangle[][] barFocusRectangles) {
		this._barFocusRectangles = barFocusRectangles;
	}

//	public void setDevGraphHeight(final int heightDev) {
//		this.devGraphHeight = heightDev;
//	}
//
//	public void setDevGraphWidth(final int devGraphWidth) {
//		this.devGraphWidth = devGraphWidth;
//	}

	/**
	 * @param barPosition
	 *            The barPosition to set.
	 */
	public void setBarPosition(final int barPosition) {
		this._barPosition = barPosition;
	}

	/**
	 * @param barRectangles
	 *            The barRectangles to set.
	 */
	public void setBarRectangles(final Rectangle[][] barRectList) {
		this._barRectangles = barRectList;
	}

	/**
	 * @param barRectangleWidth
	 *            The barRectangleWidth to set.
	 */
	public void setBarRectangleWidth(final int barRectangleWidth) {
		this._barRectangleWidth = barRectangleWidth;
	}

	/**
	 * @param barRectanglePos
	 *            The barRectanglePos to set.
	 */
	public void setDevBarRectangleXPos(final int barRectanglePos) {
		_devBarRectangleXPos = barRectanglePos;
	}

	/**
	 * @param devMarginTop
	 *            The devMarginTop to set.
	 */
	public void setDevMarginTop(final int devMarginTop) {
		this._devMarginTop = devMarginTop;
	}

	/**
	 * @param devMarkerBarHeight
	 *            The devMarkerBarHeight to set.
	 */
	public void setDevMarkerBarHeight(final int devMarkerBarHeight) {
		this._devMarkerBarHeight = devMarkerBarHeight;
	}

	/**
	 * @param devSliderBarHeight
	 *            The devSliderBarHeight to set.
	 */
	public void setDevSliderBarHeight(final int devSliderBarHeight) {
		this._devSliderBarHeight = devSliderBarHeight;
	}

	public void setDevSliderHeight(final int devSliderHeight) {
		this._devSliderHeight = devSliderHeight;
	}

	/**
	 * @param devTitelBarHeight
	 *            The devTitelBarHeight to set.
	 */
	void setDevXTitelBarHeight(final int devTitelBarHeight) {
		this._devXTitelBarHeight = devTitelBarHeight;
	}

	public void setDevYBottom(final int devY) {
		this._devYBottom = devY;
	}

	/**
	 * @param devYTop
	 *            The devYTop to set.
	 */
	protected void setDevYTop(final int devYTop) {
		this._devYTop = devYTop;
	}

	public void setErrorMessage(final String errorMessage) {
		_errorMessage = errorMessage;
	}

	public void setGraphYBottom(final int yGraphMin) {
		this._graphYBottom = yGraphMin;
	}

	/**
	 * @param graphYTop
	 *            The graphYTop to set.
	 */
	protected void setGraphYTop(final int graphYTop) {
		this._graphYTop = graphYTop;
	}

	public void setIsDrawUnit(final boolean[] isDrawUnits) {
		_isDrawUnits = isDrawUnits;
	}

	public void setIsDrawVerticalGrid(final boolean isDrawVerticalGrid) {
		_isDrawVerticalGrid = isDrawVerticalGrid;
	}

	public void setIsXUnitOverlapChecked(final boolean isXUnitOverlapChecked) {
		_isXUnitOverlapChecked = isXUnitOverlapChecked;
	}

	/**
	 * Set scaling for the x-axis unit
	 * 
	 * @param scaleXUnit
	 */
	public void setScaleUnitX(final float scaleXUnit) {
		this._scaleUnitX = scaleXUnit;
	}

	/**
	 * Set scaling for the x-axis values
	 * 
	 * @param scaleX
	 */
	public void setScaleX(final float scaleX) {
		this._scaleX = scaleX;
	}

	public void setScaleY(final float scaleY) {
		this._scaleY = scaleY;
	}

	/**
	 * @param xData
	 *            The xData to set.
	 */
	public void setXData(final ChartDataXSerie xData) {
		this._xData = xData;
	}

	/**
	 * @param data2nd
	 *            The xData2nd to set.
	 */
	public void setXData2nd(final ChartDataXSerie data2nd) {
		_xData2nd = data2nd;
	}

	public void setXTitle(final String title) {
		_xTitle = title;
	}

	/**
	 * set the position of the unit text, this can be X_UNIT_TEXT_POS_LEFT or X_UNIT_TEXT_POS_CENTER
	 * 
	 * @param unitTextPos
	 *            The xUnitTextPos to set.
	 */
	public void setXUnitTextPos(final int unitTextPos) {
		_xUnitTextPos = unitTextPos;
	}

	/**
	 * @param data
	 *            The yData to set.
	 */
	public void setYData(final ChartDataYSerie data) {
		this._yData = data;
	}

	/**
	 * @param sliderHitRect
	 *            The ySliderHitRect to set.
	 */
	public void setYSliderHitRect(final Rectangle[] sliderHitRect) {
		_ySliderHitRect = sliderHitRect;
	}

}
