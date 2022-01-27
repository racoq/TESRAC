package tesrac.utils;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Contains methods for the XML files handling
 * @author JBecho
 *
 */
public class XMLUtils {

	/**
	 * Get a Document from the xml file in the given path
	 * @param path - the path to the xml file
	 * @return Document
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document getXMLDoc(String path) throws ParserConfigurationException, SAXException, IOException {
		File reportXml = new File(path);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(reportXml);
		doc.getDocumentElement().normalize();
		return doc;
	}
	
	/**
	 * Returns the child of the given node with the given tagname
	 * It is only used when it is certain that there will be one and only one child 
	 * with that tagname
	 * @param node - the parent Node
	 * @param tagname - the tag name of the child
	 * @return Node child
	 */
	public static Node getUniqueChild(Node node, String tagname) {
		Node metrics = node.getFirstChild();
		while(!metrics.getNodeName().equals(tagname)) {
			metrics = metrics.getNextSibling();
		}
		return metrics;
	}
}
