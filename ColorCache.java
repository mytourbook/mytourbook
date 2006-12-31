package net.tourbook.ui;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorCache {

	private Display					fDisplay;

	private HashMap<String, Color>	fColors	= new HashMap<String, Color>();

	public ColorCache() {
		fDisplay = Display.getCurrent();
	}

	public void dispose() {
		for (Iterator i = fColors.values().iterator(); i.hasNext();) {
			((Color) i.next()).dispose();
		}
		fColors.clear();
	}

	public Color get(String colorKey) {
		return fColors.get(colorKey);
	}

	public Color put(String colorKey, RGB rgb) {

		// check if key already exists
		if (fColors.containsKey(colorKey)) {
			return fColors.get(colorKey);
		}
		
		Color color = new Color(fDisplay, rgb);

		fColors.put(colorKey, color);

		return color;
	}

}
