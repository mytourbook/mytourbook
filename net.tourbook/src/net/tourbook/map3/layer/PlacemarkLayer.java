/*******************************************************************************
 * Copyright (c) 2006 Vladimir Silva and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Silva - initial API and implementation
 *******************************************************************************/

package net.tourbook.map3.layer;


import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.font.TextAttribute;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

//import org.apache.log4j.Logger;

//import org.eclipse.plugin.worldwind.contrib.Messages;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.IconLayer;
import gov.nasa.worldwind.render.Annotation;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.render.IconRenderer;
import gov.nasa.worldwind.render.UserFacingIcon;
import gov.nasa.worldwind.render.WWIcon;

/**
 * A WW layer to represent a KML Placemark or point on earth.
 * @author Owner
 *
 */
public class PlacemarkLayer extends IconLayer  
{
//	private static final Logger logger = Logger.getLogger(PlacemarkLayer.class);
	
	// Default icon path
	static final String defaultIconPath = "worldwind/contrib/layers/placemark.png"; 
	
	// Annotation attributes
	private static AnnotationAttributes attribs = new AnnotationAttributes();

	private IconRenderer renderer = new IconRenderer();

	//private String description;
	
	static {
    	attribs.setTextColor(Color.BLACK);
    	attribs.setBackgroundColor(Color.WHITE);
    	attribs.setBorderColor(Color.BLACK);
	}

	/**
	 * Placemark icon w/ annotation
	 * @author Owner
	 *
	 */
	public static class PlacemarkIcon extends UserFacingIcon 
	{
		GlobeAnnotation bubble;
		String name;
		String description;
		
		public PlacemarkIcon(String name, String iconPath, Position iconPosition, String description) {
			super(iconPath, iconPosition);
			
			this.name 			= name;
			this.description 	= description;
			
			if ( description != null)
				bubble = new GlobeAnnotation(description, iconPosition, attribs);
		}
		
		public Annotation getAnnotation(){
			return bubble;
		}
	}
	
	/**
	 * Contructor
	 */
	public PlacemarkLayer() {
		super();
	}

    @Override
    protected void doRender(DrawContext dc)
    {
    	Collection<WWIcon> icons = (Collection<WWIcon>)getIcons();
    	
    	renderer.render(dc, icons);
    	for (WWIcon icon : icons) 
    	{
    		final Annotation a = ((PlacemarkIcon)icon).getAnnotation();
    		if ( a != null )
    			a.render(dc);
		}
    }

    @Override
    protected void doPick(DrawContext dc, Point pickPoint) {
    	super.doPick(dc, pickPoint);
    	
    	Collection<WWIcon> icons =  (Collection<WWIcon>)getIcons();
    	
    	for (WWIcon icon : icons) 
    	{
    		final Annotation a = ((PlacemarkIcon)icon).getAnnotation();
    		
    		// TODO: WW 0.6?? pick annotation
//    		if (a != null)
//    			a.pick(dc, pickPoint);
		}
    }
    
    
    public void addIcon(PlacemarkIcon icon) { 
    	super.addIcon(icon);
    }
    
    public void addDefaultIcon(String name, Position iconPosition, String description) 
    {
    	//WWIcon icon = new UserFacingIcon(defaultIconPath, iconPosition);
    	WWIcon icon = new PlacemarkIcon(name ,defaultIconPath, iconPosition, description);
    	
    	icon.setToolTipText(name);
    	icon.setToolTipFont(makeToolTipFont());
    	icon.setToolTipTextColor(java.awt.Color.BLACK);
    	addIcon(icon);
    	
    	//this.description = description;
    }

    /*
     * Deafult font
     */
    private Font makeToolTipFont()
    {
        HashMap<TextAttribute, Object> fontAttributes = new HashMap<TextAttribute, Object>();

        fontAttributes.put(TextAttribute.BACKGROUND, java.awt.Color.YELLOW); //new java.awt.Color(0.4f, 0.4f, 0.4f, 1f));
        return Font.decode("Arial-12").deriveFont(fontAttributes);
    }
   
    /**
     * Get kml of the first icon only for now....
     * @return
     */
    public String toKML ()
    {
    	Iterator<WWIcon> iter =  getIcons().iterator();
    	StringBuffer buff 		= new StringBuffer();
    	
//    	while ( iter.hasNext()) 
//    	{
//    		PlacemarkIcon icon = (PlacemarkIcon)iter.next();
//    	
//	    	if ( icon == null ) {
//	    		logger.error("No placemark icon for layer " + getName());
//	    		continue;
//	    	}
//    		
//	    	Position pos = icon.getPosition();
//    	
//	    	buff.append("<Placemark><name>" + icon.name +  "</name>" + Messages.NL
//	    			+ (icon.description != null 
//	    					? "<description><![CDATA[" + icon.description + "]]>" 
//	    							+ "</description>" + Messages.NL
//	    					: "" )
//	    			+ "<Point><coordinates>" + pos.getLongitude().degrees
//	    				+ "," + pos.getLatitude().degrees 
//	    				+ "</coordinates></Point>" + Messages.NL
//	    			+ "</Placemark>" + Messages.NL);
//    	}
    	return buff.toString();
    }
}
