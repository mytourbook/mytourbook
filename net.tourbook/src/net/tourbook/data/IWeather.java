package net.tourbook.data;

import net.tourbook.Messages;
import net.tourbook.ui.UI;

public interface IWeather {

	public static final String[]	windDirectionText	= new String[] {
			Messages.Weather_WindDirection_N,
			Messages.Weather_WindDirection_NE,
			Messages.Weather_WindDirection_E,
			Messages.Weather_WindDirection_SE,
			Messages.Weather_WindDirection_S,
			Messages.Weather_WindDirection_SW,
			Messages.Weather_WindDirection_W,
			Messages.Weather_WindDirection_NW			};

	/**
	 * <pre>
	 * 
	 * source: wikipedia
	 * 
	 * Bft	Description				km/h 		mph
	 * 0	Calm 					< 1 		< 1
	 * 1	Light air			 	1.1 - 5.5 	1 - 3
	 * 2	Light breeze 			5.6 - 11 	4 - 7
	 * 3	Gentle breeze 			12 - 19 	8 - 12
	 * 4	Moderate breeze 		20 - 28 	13 - 17
	 * 5	Fresh breeze 			29 - 38 	18 - 24
	 * 6	Strong breeze 			39 - 49 	25 - 30
	 * 7	High wind, ...		 	50 - 61 	31 - 38
	 * 8	Gale, Fresh gale 		62 - 74 	39 - 46
	 * 9	Strong gale 			75 - 88 	47 - 54
	 * 10	Storm[6], Whole gale 	89 - 102 	55 - 63
	 * 11	Violent storm 			103 - 117 	64 - 72
	 * 12	Hurricane-force 	 	>= 118 		>= 73
	 * 
	 * </pre>
	 */
	public static final String[]	windSpeedText		= new String[] {
			Messages.Weather_WindSpeed_Bft00,
			Messages.Weather_WindSpeed_Bft01,
			Messages.Weather_WindSpeed_Bft02,
			Messages.Weather_WindSpeed_Bft03,
			Messages.Weather_WindSpeed_Bft04,
			Messages.Weather_WindSpeed_Bft05,
			Messages.Weather_WindSpeed_Bft06,
			Messages.Weather_WindSpeed_Bft07,
			Messages.Weather_WindSpeed_Bft08,
			Messages.Weather_WindSpeed_Bft09,
			Messages.Weather_WindSpeed_Bft10,
			Messages.Weather_WindSpeed_Bft11,
			Messages.Weather_WindSpeed_Bft12			};

	public static final String[]	windSpeedTextShort	= new String[] {
			Messages.Weather_WindSpeed_Bft00_Short,
			Messages.Weather_WindSpeed_Bft01_Short,
			Messages.Weather_WindSpeed_Bft02_Short,
			Messages.Weather_WindSpeed_Bft03_Short,
			Messages.Weather_WindSpeed_Bft04_Short,
			Messages.Weather_WindSpeed_Bft05_Short,
			Messages.Weather_WindSpeed_Bft06_Short,
			Messages.Weather_WindSpeed_Bft07_Short,
			Messages.Weather_WindSpeed_Bft08_Short,
			Messages.Weather_WindSpeed_Bft09_Short,
			Messages.Weather_WindSpeed_Bft10_Short,
			Messages.Weather_WindSpeed_Bft11_Short,
			Messages.Weather_WindSpeed_Bft12_Short		};

	/**
	 * Wind speed in km/h
	 */
	public static final int[]		windSpeedKmh		= new int[] { 0, // 0 bft
			5, //	1 bft
			11, //	2
			19, //	3
			28, //	4
			38, //	5
			49, //  6
			61, //  7
			74, //  8
			88, //  9
			102, // 10
			117, // 11
			118, // 12
														};

	public static final int[]		windSpeedMph		= new int[] { 0, // 0 bft
			3, //  1 bft
			7, //  2
			12, // 3
			17, // 4
			24, // 5
			30, // 6
			38, // 7
			46, // 8
			54, // 9
			63, // 10
			72, // 11
			73, // 12
														};

	public static final String		cloudIsNotDefined	= Messages.Weather_Clounds_IsNotDefined;

	/*
	 * cloudText and cloudDbValue must be in synch
	 */

	/**
	 * Text for the weather
	 */
	public static final String[]	cloudText			= new String[] {
			cloudIsNotDefined,
			Messages.Weather_Clounds_Sunny,
			Messages.Weather_Clounds_Clouny,
			Messages.Weather_Clounds_Clouds,
			Messages.Weather_Clounds_ScatteredShowers,
			Messages.Weather_Clounds_Rain,
			Messages.Weather_Clounds_Lightning,
			Messages.Weather_Clounds_Snow,
			Messages.Weather_Clounds_SevereWeatherAlert
														//
														};

	/**
	 * Icons for the weather
	 */
	public static final String[]	cloudIcon			= new String[] {
			UI.IMAGE_EMPTY_16,
			UI.WEATHER_ID_SUNNY,
			UI.WEATHER_ID_CLOUDY,
			UI.WEATHER_ID_CLOUDS,
			UI.WEATHER_ID_SHOWERES_SCATTERED,
			UI.WEATHER_ID_RAIN,
			UI.WEATHER_ID_LIGHTNING,
			UI.WEATHER_ID_SNOW,
			UI.WEATHER_ID_SEVERE_WEATHER_ALERT,
														//
														};

}
