<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" exclude-result-prefixes="fo">
	<xsl:output method="xml" version="1.0" omit-xml-declaration="no" indent="yes" />
	<!-- translations-->
	<xsl:param name="lang.Tour_Print_Tour" select="''" />
	<xsl:param name="lang.Tour_Print_Start" select="''" />
	<xsl:param name="lang.Tour_Print_Start_Location" select="''" />
	<xsl:param name="lang.Tour_Print_End_Location" select="''" />
	<xsl:param name="lang.Tour_Print_Time_Distance_Speed" select="''" />
	<xsl:param name="lang.Tour_Print_Tour_Time" select="''" />
	<xsl:param name="lang.Tour_Print_Tour_Pausing_Time" select="''" />
	<xsl:param name="lang.Tour_Print_Tour_Moving_Time" select="''" />
	<xsl:param name="lang.Tour_Print_Distance" select="''" />
	<xsl:param name="lang.Tour_Print_Maximum_Speed" select="''" />
	<xsl:param name="lang.Tour_Print_Personal" select="''" />
	<xsl:param name="lang.Tour_Print_Rest_Pulse" select="''" />
	<xsl:param name="lang.Tour_Print_Maximum_Pulse" select="''" />
	<xsl:param name="lang.Tour_Print_Average_Pulse" select="''" />
	<xsl:param name="lang.Tour_Print_Calories" select="''" />
	<xsl:param name="lang.Tour_Print_Average_Cadence" select="''" />
	<xsl:param name="lang.Tour_Print_Altitude" select="''" />
	<xsl:param name="lang.Tour_Print_Highest_Altitude" select="''" />
	<xsl:param name="lang.Tour_Print_Meters_Up" select="''" />
	<xsl:param name="lang.Tour_Print_Meters_Down" select="''" />
	<xsl:param name="lang.Tour_Print_Tour_Markers" select="''" />
	<xsl:param name="lang.Tour_Print_No_Markers_Found" select="''" />

	<!-- parameters passed from java code into the TransFormer, useful for pre-formatting data in java or configuring i18n -->
	<xsl:param name="paperSize" select="''" />
	<xsl:param name="paperOrientation" select="''" />
	<xsl:param name="isPrintMarkers" select="''" />
	<xsl:param name="isPrintDescription" select="''" />
	<xsl:param name="startDate" select="''" />	
	<xsl:param name="unitAltitude" select="''" />
	<xsl:param name="unitDistance" select="''" />
	<xsl:param name="unitTemperature" select="''" />			
	<xsl:param name="unitLabelDistance" select="''" />
	<xsl:param name="unitLabelSpeed" select="''" />
	<xsl:param name="unitLabelAltitude" select="''" />
	<xsl:param name="unitLabelTemperature" select="''" />
	<xsl:param name="unitLabelHeartBeat" select="''" />
	<xsl:param name="unitLabelCadence" select="''" />
	<xsl:param name="unitLabelCalories" select="''" />
			
	<xsl:template name="FormatTime">
		<xsl:param name="theTime" />
		<xsl:variable name="hh">
      		<xsl:value-of select="floor($theTime div 3600)" />
    	</xsl:variable>
		<xsl:variable name="mm">
      		<xsl:value-of select="floor(($theTime mod 3600) div 60)" />
    	</xsl:variable>
		<xsl:variable name="ss">
      		<xsl:value-of select="($theTime mod 3600) mod 60" />
    	</xsl:variable>
    	<xsl:value-of select="$hh"/>
    	<xsl:value-of select="':'"/>
 		<xsl:if test="(string-length($mm) &lt; 2)">
      		<xsl:value-of select="0"/>
    	</xsl:if>    	
     	<xsl:value-of select="$mm"/>
    	<xsl:value-of select="':'"/>
 		<xsl:if test="(string-length($ss) &lt; 2)">
      		<xsl:value-of select="0"/>
    	</xsl:if>    	
     	<xsl:value-of select="$ss"/>
	</xsl:template>
			
	<!-- ========================= -->
	<!-- root element: TourData -->
	<!-- ========================= -->
	<xsl:template match="TourData">
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
			<fo:layout-master-set>
			
				
				<fo:simple-page-master master-name="simplePaper" margin-top="2cm" margin-bottom="2cm" margin-left="2cm" margin-right="2cm">
					<xsl:if test="$paperSize = 'A4'">
						<xsl:attribute name="page-width">
							<xsl:if test="$paperOrientation = 'Portrait'">21cm</xsl:if>
							<xsl:if test="$paperOrientation = 'Landscape'">29.7cm</xsl:if>
	  					</xsl:attribute>
						<xsl:attribute name="page-height">
							<xsl:if test="$paperOrientation = 'Portrait'">29.7cm</xsl:if>
							<xsl:if test="$paperOrientation = 'Landscape'">21cm</xsl:if>
	  					</xsl:attribute>
					</xsl:if>
					<xsl:if test="$paperSize = 'Letter'">
						<xsl:attribute name="page-width">
							<xsl:if test="$paperOrientation = 'Portrait'">8.5in</xsl:if>
							<xsl:if test="$paperOrientation = 'Landscape'">11in</xsl:if>
	  					</xsl:attribute>
						<xsl:attribute name="page-height">
							<xsl:if test="$paperOrientation = 'Portrait'">11in</xsl:if>
							<xsl:if test="$paperOrientation = 'Landscape'">8.5in</xsl:if>
	  					</xsl:attribute>
					</xsl:if>	
					<fo:region-body />
				</fo:simple-page-master>
			
				
			</fo:layout-master-set>
			<fo:page-sequence master-reference="simplePaper">
				<fo:flow flow-name="xsl-region-body">

				<fo:block border-color="black" border-style="solid" border-width=".3mm"  margin-bottom="5pt" font-size="16pt" font-weight="bold" background-color="#E1E1E1" text-align="center">
					Tour: <xsl:value-of select="tourTitle" />
				</fo:block>
				
				
				<fo:block margin-bottom="5pt" font-size="10pt">
					<fo:table border-style="solid" border-width="0.5pt" border-color="black" table-layout="fixed" width="100%">
						<fo:table-column column-width="25%" />
						<fo:table-column column-width="75%" />
						<fo:table-header>
	    					<fo:table-row>
	    						<fo:table-cell border-style="solid" border-width="0.5pt" border-color="black" text-align="center" vertical-align="middle" number-columns-spanned="2" background-color="#E1E1E1">
	    							<fo:block text-align="center" vertical-align="middle">
	    								<xsl:value-of select="$lang.Tour_Print_Tour" />
	    							</fo:block>
	    						</fo:table-cell>	
	    					</fo:table-row>
	    				</fo:table-header>
						<fo:table-body>
							<xsl:if test="$isPrintDescription">
							<!-- tour description -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" number-columns-spanned="2" padding="2pt">
									<fo:block text-align="left" vertical-align="top" white-space-collapse="false" linefeed-treatment="preserve"><xsl:value-of select="tourDescription" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							</xsl:if>
							<!-- tour start date -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top"><xsl:value-of select="$lang.Tour_Print_Start" />:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="$startDate" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- tour start location -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top"><xsl:value-of select="$lang.Tour_Print_Start_Location" />:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="tourStartPlace" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- tour end location -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top"><xsl:value-of select="$lang.Tour_Print_End_Location" />:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="tourEndPlace" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
						</fo:table-body>
					</fo:table>
				</fo:block>
				
				<!-- insert TourMarkes content if needed -->
				<xsl:if test="$isPrintMarkers">
					<xsl:apply-templates select="//TourMarkers"/> 
				</xsl:if>
				
				<fo:block padding="0pt" margin-bottom="5pt" font-size="10pt">
					<fo:table border-style="solid" border-width="0.5pt" border-color="black" table-layout="fixed" width="100%">
						<fo:table-column column-width="25%" />
						<fo:table-column column-width="75%" />
						<fo:table-header>
	    					<fo:table-row>
	    						<fo:table-cell border-style="solid" border-width="0.5pt" border-color="black" text-align="center" vertical-align="middle" number-columns-spanned="2" background-color="#E1E1E1">
	    							<fo:block text-align="center" vertical-align="middle">
	    								<xsl:value-of select="$lang.Tour_Print_Time_Distance_Speed" />
	    							</fo:block>
	    						</fo:table-cell>	
	    					</fo:table-row>
	    				</fo:table-header>
						<fo:table-body>
							<!-- tour time -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top"><xsl:value-of select="$lang.Tour_Print_Tour_Time" />:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left">
										<xsl:call-template name="FormatTime">
	          								<xsl:with-param name="theTime" select="tourRecordingTime"/>
	        							</xsl:call-template>									
									</fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- tour moving time -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top"><xsl:value-of select="$lang.Tour_Print_Tour_Pausing_Time" />:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left">
										<xsl:call-template name="FormatTime">
	          								<xsl:with-param name="theTime" select="tourRecordingTime - tourDrivingTime"/>
	        							</xsl:call-template>									
									</fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- tour moving time -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top"><xsl:value-of select="$lang.Tour_Print_Tour_Moving_Time" />:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left">
										<xsl:call-template name="FormatTime">
	          								<xsl:with-param name="theTime" select="tourDrivingTime"/>
	        							</xsl:call-template>
	        						</fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- tour distance -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top"><xsl:value-of select="$lang.Tour_Print_Distance" />:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left" white-space-collapse="false" white-space="pre"><xsl:value-of select="format-number(tourDistance div 1000 div $unitDistance, '#.##')" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelDistance" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- maximum speed -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top"><xsl:value-of select="$lang.Tour_Print_Maximum_Speed" />:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="format-number(maxSpeed div $unitDistance, '#.##')" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelSpeed" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
						</fo:table-body>
					</fo:table>
				</fo:block>
				
				
				<fo:block padding="0pt" margin-bottom="5pt" font-size="10pt">
					<fo:table border-style="solid" border-width="0.5pt" border-color="black" table-layout="fixed" width="100%">
						<fo:table-column column-width="25%" />
						<fo:table-column column-width="75%" />
						<fo:table-header>
	    					<fo:table-row>
	    						<fo:table-cell border-style="solid" border-width="0.5pt" border-color="black" text-align="center" vertical-align="middle" number-columns-spanned="2" background-color="#E1E1E1">
	    							<fo:block text-align="center" vertical-align="middle">
	    								<xsl:value-of select="$lang.Tour_Print_Personal" />
	    							</fo:block>
	    						</fo:table-cell>	
	    					</fo:table-row>
	    				</fo:table-header>
						<fo:table-body>
							<!-- rest pulse -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top"><xsl:value-of select="$lang.Tour_Print_Rest_Pulse" />:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="restPulse" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelHeartBeat" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- maximum pulse -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top"><xsl:value-of select="$lang.Tour_Print_Maximum_Pulse" />:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="maxPulse" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelHeartBeat" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- average pulse -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top"><xsl:value-of select="$lang.Tour_Print_Average_Pulse" />:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="avgPulse" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelHeartBeat" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- calories -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top"><xsl:value-of select="$lang.Tour_Print_Calories" />:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="calories" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelCalories" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- average cadence -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top"><xsl:value-of select="$lang.Tour_Print_Average_Cadence" />:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="avgCadence" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelCadence" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
						</fo:table-body>
					</fo:table>
				</fo:block>
				
				
				<fo:block padding="0pt" margin-bottom="5pt" font-size="10pt">
					<fo:table border-style="solid" border-width="0.5pt" border-color="black" table-layout="fixed" width="100%">
						<fo:table-column column-width="25%" />
						<fo:table-column column-width="75%" />
						<fo:table-header>
	    					<fo:table-row>
	    						<fo:table-cell border-style="solid" border-width="0.5pt" border-color="black" text-align="center" vertical-align="middle" number-columns-spanned="2" background-color="#E1E1E1">
	    							<fo:block text-align="center" vertical-align="middle">
	    								<xsl:value-of select="$lang.Tour_Print_Altitude" />
	    							</fo:block>
	    						</fo:table-cell>	
	    					</fo:table-row>
	    				</fo:table-header>
						<fo:table-body>
							<!-- highest altitude -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top"><xsl:value-of select="$lang.Tour_Print_Highest_Altitude" />:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="format-number(maxAltitude div $unitAltitude, '#.##')" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelAltitude" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- tour meters up -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top"><xsl:value-of select="$lang.Tour_Print_Meters_Up" />:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="format-number(tourAltUp div $unitAltitude, '#.##')" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelAltitude" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- tour meters down -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top"><xsl:value-of select="$lang.Tour_Print_Meters_Down" />:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="format-number(tourAltDown div $unitAltitude, '#.##')" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelAltitude" /></fo:block>
								</fo:table-cell>
							</fo:table-row>	
						</fo:table-body>
					</fo:table>
				</fo:block>
				
				
				</fo:flow>				
			</fo:page-sequence>
		</fo:root>
	</xsl:template>
	
	<!-- ========================= -->
	<!-- element: TourMarkers -->
	<!-- ========================= -->
	<xsl:template match="TourMarkers">

			<fo:block padding="0pt" margin-bottom="5pt" font-size="10pt">
				<fo:table border-style="solid" border-width="0.5pt" border-color="black" table-layout="fixed" width="100%">
					<fo:table-column column-width="15%" />
					<fo:table-column column-width="15%" />
					<fo:table-column column-width="70%" />
					<fo:table-header>
	   					<fo:table-row>
	   						<fo:table-cell border-style="solid" border-width="0.5pt" border-color="black" text-align="center" vertical-align="middle" number-columns-spanned="3" background-color="#E1E1E1">
	   							<fo:block text-align="center" vertical-align="middle">
	   								<xsl:value-of select="$lang.Tour_Print_Tour_Markers" />
	   							</fo:block>
	   						</fo:table-cell>	
	   					</fo:table-row>
	   				</fo:table-header>
	   				<fo:table-body>
					<xsl:choose> 
						<xsl:when test="count(TourMarker) > 0">
	
								<xsl:for-each select="TourMarker">
			      					<xsl:sort select="distance" data-type="number" order="ascending"/>		
									<!-- tour marker -->
									<fo:table-row>
										<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
											<fo:block text-align="left" vertical-align="top" white-space-collapse="false" white-space="pre">
												<xsl:call-template name="FormatTime">
	          										<xsl:with-param name="theTime" select="time"/>
	        									</xsl:call-template>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
											<fo:block text-align="left" vertical-align="top" white-space-collapse="false" white-space="pre">								
												<xsl:value-of select="format-number(distance div 1000 div $unitDistance, '#.##')" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelDistance" />
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
											<fo:block text-align="left"><xsl:value-of select="label" /></fo:block>
										</fo:table-cell>
									</fo:table-row>
			    				</xsl:for-each>
													
						</xsl:when> 
						<xsl:otherwise> 
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt" number-columns-spanned="3">
									<fo:block text-align="left"><xsl:value-of select="$lang.Tour_Print_No_Markers_Found" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
						</xsl:otherwise> 
					</xsl:choose> 
					</fo:table-body>
				</fo:table>
			</fo:block>	
		
	</xsl:template>
		
</xsl:stylesheet>