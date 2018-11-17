package net.tourbook.preferences;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

// http://www.vogella.com/tutorials/JavaXML/article.html

public class MapsforgeStyleParser {
	static final String ID = "id";
	static final String XML_LAYER = "layer";
	static final String VISIBLE = "visible";
	static  Boolean Style = false;


	@SuppressWarnings({ "unchecked", "null" })
	public List<Item> readConfig(String configFile) {

		List<Item> items = new ArrayList<Item>();
		try {
			// First, create a new XMLInputFactory
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			// Setup a new eventReader
			InputStream in = new FileInputStream(configFile);
			XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
			// read the XML document
			Item item = null;

			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();
				if (event.isStartElement()) {
					StartElement startElement = event.asStartElement();
					// If we have an item(layer) element, we create a new item
					if (startElement.getName().getLocalPart().equals(XML_LAYER)) {
						Style = false;
						item = new Item();
						Iterator<Attribute> attributes = startElement
								.getAttributes();
						while (attributes.hasNext()) {
							Attribute attribute = attributes.next();
							//System.out.println("Att Name + Value: " + attribute.getName() + " = " + attribute.getValue());
							if (attribute.getName().toString().equals(ID)) {
								item.setXmlLayer(attribute.getValue());
							}
							if (attribute.getName().toString().equals(VISIBLE)) {
								item.setXmlLayer(attribute.getValue());
								Style = true;
							}
						}
					}
				}
				// If we reach the end of an item element, we add it to the list
				if (event.isEndElement()) {
					EndElement endElement = event.asEndElement();
					if (endElement.getName().getLocalPart().equals(XML_LAYER) && Style) {
						items.add(item);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		return items;
	}

}


class Item {
   private String xmlLayer;
   
   public String getXmlLayer() {
      return xmlLayer;
  }  
   public void setXmlLayer(String xmlLayer) {
      this.xmlLayer = xmlLayer;
  } 

   @Override
   public String toString() {
       return "Item [xmlLAyer=" + xmlLayer + "]";
   }
}