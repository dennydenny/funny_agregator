package fa.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Hashtable;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLParser {
	
	private static final Logger LOG = LoggerFactory.getLogger(XMLParser.class);
	private static Map<String, String> settings;

	public static Map<String, String> ParseConfig() {

		settings = new Hashtable<String, String>();
		
	    try {
	    	//ClassLoader classLoader = ClassLoader.getSystemClassLoader();
	    	//File file = new File(classLoader.getResource("./config.xml").getFile());
	    	File file = new File("./config.xml");
	    	DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance()
	                             .newDocumentBuilder();
	    	Document doc = dBuilder.parse(file);
	    	readNode(doc.getChildNodes());
	    	return settings;
	    }
	    catch (FileNotFoundException filenotfound)
	    {
	    	LOG.error("Не удалось найти файл конфигурации. Возникла ошибка: " + filenotfound.getMessage());
	    }
	    catch (Exception e) {
	    	LOG.error("Возникла ошибки при парсинге XML: " + e.getMessage());
	    }
		return settings;
	}

	private static void readNode(NodeList nodeList) {

	    for (int count = 0; count < nodeList.getLength(); count++) {
	
		Node tempNode = nodeList.item(count);
	
		// make sure it's element node.
		if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
			
			if (tempNode.hasChildNodes()) {
				// loop again if has child nodes
				readNode(tempNode.getChildNodes());
			}
			if (tempNode.getNodeName().equals("configuration")) return;
			
			settings.put(tempNode.getNodeName(), tempNode.getTextContent());
		}
	    }
	}
}