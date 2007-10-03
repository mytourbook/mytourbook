/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
	protected static final int		XUNIT_TEXT_POS_LEFT		= 0;
	protected static final int		XUNIT_TEXT_POS_CENTER	= 1;

	public static final int			BAR_POS_LEFT			= 0;
	public static final int			BAR_POS_CENTER			= 1;

	private ChartDataXSerie			xData;
	private ChartDataXSerie			xData2nd;
	private ChartDataYSerie			yData;

	private Rectangle[]				ySliderHitRect;

	private String					fXTitle;

	/**
	 * management for the bar graph
	 */
	private Rectangle[][]			barRectangles;
	private Rectangle[][]			barFocusRectangles;

	private int						barRectangleWidth;
	private int						fDevBarRectangleXPos;

	/**
	 * Contains all unit labels and their positions for the x axis
	 */
	private ArrayList<ChartUnit>	xUnits					= new ArrayList<ChartUnit>();
	private int						xUnitTextPos			= XUNIT_TEXT_POS_LEFT;

	/**
	 * List with all unit labels and positions for the y axis
	 */
	private ArrayList<ChartUnit>	yUnits					= new ArrayList<ChartUnit>();

	// scaling from graph value to device value
	private float					scaleX;
	private float					scaleY;

	private int						devMarginTop;
	private int						devXTitelBarHeight;
	private int						devMarkerBarHeight;
	private int						devSliderBarHeight;

	// graph position
	private int						devYTop;
	private int						devYBottom;

	/**
	 * virtual graph width in dev (pixel) units
	 */
	private int						devGraphWidth;
	private int						devGraphHeight;

	private int						devSliderHeight;

	/**
	 * graph value for the bottom of the graph
	 */
	private int						graphYBottom;
	private int						graphYTop;

	private int						barPosition				= BAR_POS_LEFT;

	private int						fChartType;

	public ChartDrawingData(int chartType) {
		fChartType = chartType;
	}

	/**
	 * @return Returns the barFocusRectangles.
	 */
	public Rectangle[][] getBarFocusRectangles() {
		return barFocusRectangles;
	}

	/**
	 * @return Returns the barPosition, this can be set to BAR_POS_LEFT, BAR_POS_CENTER
	 */
	public int getBarPosition() {
		return barPosition;
	}

	/**
	 * @return Returns the barRectanglePos.
	 */
	public int getDevBarRectangleXPos() {
		return fDevBarRectangleXPos;
	}

	/**
	 * @return Returns the barRectangles.
	 */
	public Rectangle[][] getBarRectangles() {
		return barRectangles;
	}

	/**
	 * @return Returns the barRectangleWidth.
	 */
	public int getBarRectangleWidth() {
		return barRectangleWidth;
	}

	public int getChartType() {
		return fChartType;
	}

	public int getDevGraphHeight() {
		return devGraphHeight;
	}

	/**
	 * virtual graph width in dev (pixel) units
	 */
	public int getDevGraphWidth() {
		return devGraphWidth;
	}

	/**
	 * @return Returns the devMarginTop.
	 */
	public int getDevMarginTop() {
		return devMarginTop;
	}

	/**
	 * @return Returns the devMarkerBarHeight.
	 */
	public int getDevMarkerBarHeight() {
		return devMarkerBarHeight;
	}

	/**
	 * @return Returns the devSliderBarHeight.
	 */
	public int getDevSliderBarHeight() {
		return devSliderBarHeight;
	}

	public int getDevSliderHeight() {
		return devSliderHeight;
	}

	/**
	 * @return Returns the devTitelBarHeight.
	 */
	public int getDevXTitelBarHeight() {
		return devXTitelBarHeight;
	}

	/**
	 * @return Returns the bottom of the chart in dev units
	 */
	public int getDevYBottom() {
		return devYBottom;
	}

	/**
	 * @return Returns the y position for the title
	 */
	public int getDevYTitle() {
		return getDevYBottom()
				- getDevGraphHeight()
				- getDevSliderBarHeight()
				- getDevXTitelBarHeight();
	}

	/**
	 * @return Returns the top of the chart in dev units
	 */
	public int getDevYTop() {
		return devYTop;
	}

	/**
	 * @return Returns the bottom of the chart in graph units
	 */
	public int getGraphYBottom() {
		return graphYBottom;
	}

	/**
	 * @return Returns the top of the chart in graph units
	 */
	protected int getGraphYTop() {
		return graphYTop;
	}

	public float getScaleX() {
		return scaleX;
	}

	public float getScaleY() {
		return scaleY;
	}

	/**
	 * @return Returns the xData.
	 */
	public ChartDataXSerie getXData() {
		return xData;
	}

	/**
	 * @return Returns the xData2nd.
	 */
	public ChartDataXSerie getXData2nd() {
		return xData2nd;
	}

	public String getXTitle() {
		return fXTitle;
	}

	/**
	 * @return Returns the xUnits.
	 */
	public ArrayList<ChartUnit> getXUnits() {
		return xUnits;
	}

	/**
	 * @return Returns the xUnitTextPos.
	 */
	public int getXUnitTextPos() {
		return xUnitTextPos;
	}

	/**
	 * @return Returns the ChartDataXSerie for the y-axis
	 */
	public ChartDataYSerie getYData() {
		return yData;
	}

	/**
	 * @return Returns the ySliderHitRect.
	 */
	public Rectangle[] getYSliderHitRect() {
		return ySliderHitRect;
	}

	/**
	 * @return Returns the yUnits.
	 */
	public ArrayList<ChartUnit> getYUnits() {
		return yUnits;
	}

	/**
	 * @param barFocusRectangles
	 *        The barFocusRectangles to set.
	 */
	public void setBarFocusRectangles(Rectangle[][] barFocusRectangles) {
		this.barFocusRectangles = barFocusRectangles;
	}

	/**
	 * @param barPosition
	 *        The barPosition to set.
	 */
	public void setBarPosition(int barPosition) {
		this.barPosition = barPosition;
	}

	/**
	 * @param barRectanglePos
	 *        The barRectanglePos to set.
	 */
	public void setDevBarRectangleXPos(int barRectanglePos) {
		fDevBarRectangleXPos = barRectanglePos;
	}

	/**
	 * @param barRectangles
	 *        The barRectangles to set.
	 */
	public void setBarRectangles(Rectangle[][] barRectList) {
		this.barRectangles = barRectList;
	}

	/**
	 * @param barRectangleWidth
	 *        The barRectangleWidth to set.
	 */
	public void setBarRectangleWidth(int barRectangleWidth) {
		this.barRectangleWidth = barRectangleWidth;
	}

	public void setDevGraphHeight(int heightDev) {
		this.devGraphHeight = heightDev;
	}

	public void setDevGraphWidth(int devGraphWidth) {
		this.devGraphWidth = devGraphWidth;
	}

	/**
	 * @param devMarginTop
	 *        The devMarginTop to set.
	 */
	public void setDevMarginTop(int devMarginTop) {
		this.devMarginTop = devMarginTop;
	}

	/**
	 * @param devMarkerBarHeight
	 *        The devMarkerBarHeight to set.
	 */
	public void setDevMarkerBarHeight(int devMarkerBarHeight) {
		this.devMarkerBarHeight = devMarkerBarHeight;
	}

	/**
	 * @param devSliderBarHeight
	 *        The devSliderBarHeight to set.
	 */
	public void setDevSliderBarHeight(int devSliderBarHeight) {
		this.devSliderBarHeight = devSliderBarHeight;
	}

	public void setDevSliderHeight(int devSliderHeight) {
		this.devSliderHeight = devSliderHeight;
	}

	/**
	 * @param devTitelBarHeight
	 *        The devTitelBarHeight to set.
	 */
	void setDevXTitelBarHeight(int devTitelBarHeight) {
		this.devXTitelBarHeight = devTitelBarHeight;
	}

	public void setDevYBottom(int devY) {
		this.devYBottom = devY;
	}

	/**
	 * @param devYTop
	 *        The devYTop to set.
	 */
	protected void setDevYTop(int devYTop) {
		this.devYTop = devYTop;
	}

	public void setGraphYBottom(int yGraphMin) {
		this.graphYBottom = yGraphMin;
	}

	/**
	 * @param graphYTop
	 *        The graphYTop to set.
	 */
	protected void setGraphYTop(int graphYTop) {
		this.graphYTop = graphYTop;
	}

	public void setScaleX(float scaleX) {
		this.scaleX = scaleX;
	}

	public void setScaleY(float scaleY) {
		this.scaleY = scaleY;
	}

	/**
	 * @param xData
	 *        The xData to set.
	 */
	public void setXData(ChartDataXSerie xData) {
		this.xData = xData;
	}

	/**
	 * @param data2nd
	 *        The xData2nd to set.
	 */
	public void setXData2nd(ChartDataXSerie data2nd) {
		xData2nd = data2nd;
	}

	public void setXTitle(String title) {
		fXTitle = title;
	}

	/**
	 * set the position of the unit text, this can be XUNIT_TEXT_POS_LEFT or XUNIT_TEXT_POS_CENTER
	 * 
	 * @param unitTextPos
	 *        The xUnitTextPos to set.
	 */
	public void setXUnitTextPos(int unitTextPos) {
		xUnitTextPos = unitTextPos;
	}

	/**
	 * @param data
	 *        The yData to set.
	 */
	public void setYData(ChartDataYSerie data) {
		this.yData = data;
	}

	/**
	 * @param sliderHitRect
	 *        The ySliderHitRect to set.
	 */
	public void setYSliderHitRect(Rectangle[] sliderHitRect) {
		ySliderHitRect = sliderHitRect;
	}
}
