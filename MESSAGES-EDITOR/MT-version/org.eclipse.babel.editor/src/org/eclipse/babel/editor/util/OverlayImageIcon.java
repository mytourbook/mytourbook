/*******************************************************************************
 * Copyright (c) 2007 Pascal Essiembre.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Pascal Essiembre - initial API and implementation
 ******************************************************************************/
package org.eclipse.babel.editor.util;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

/**
 * 
 * @author Pascal Essiembre
 */
public class OverlayImageIcon extends CompositeImageDescriptor {

    /** Constant for overlaying icon in top left corner. */
    public static final int TOP_LEFT = 0;
    /** Constant for overlaying icon in top right corner. */
    public static final int TOP_RIGHT = 1;
    /** Constant for overlaying icon in bottom left corner. */
    public static final int BOTTOM_LEFT = 2;
    /** Constant for overlaying icon in bottom right corner. */
    public static final int BOTTOM_RIGHT = 3;

    private Image baseImage;
    private Image overlayImage;
    private int location;
    private Point imgSize;

    /**
     * Constructor.
     * @param baseImage background image
     * @param overlayImage the image to put on top of background image
     * @param location in which corner to put the icon
     */
    public OverlayImageIcon(Image baseImage, Image overlayImage, int location) {
        super();
        this.baseImage = baseImage;
        this.overlayImage = overlayImage;
        this.location = location;
        this.imgSize = new Point(
                baseImage.getImageData().width, 
                baseImage.getImageData().height);
    }

    /**
     * @see org.eclipse.jface.resource.CompositeImageDescriptor
     *         #drawCompositeImage(int, int)
     */
    protected void drawCompositeImage(int width, int height) {
        // Draw the base image
        drawImage(baseImage.getImageData(), 0, 0); 
        ImageData imageData = overlayImage.getImageData();
        switch(location) {
            // Draw on the top left corner
            case TOP_LEFT:
                drawImage(imageData, 0, 0);
                break;
            
            // Draw on top right corner  
            case TOP_RIGHT:
                drawImage(imageData, imgSize.x - imageData.width, 0);
                break;
            
            // Draw on bottom left  
            case BOTTOM_LEFT:
                drawImage(imageData, 0, imgSize.y - imageData.height);
                break;
            
            // Draw on bottom right corner  
            case BOTTOM_RIGHT:
                drawImage(imageData, imgSize.x - imageData.width,
                        imgSize.y - imageData.height);
                break;
            
        }
    }

    /**
     * @see org.eclipse.jface.resource.CompositeImageDescriptor#getSize()
     */
    protected Point getSize() {
        return imgSize;
    }

}
