package tests.com.ibm.qautils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlUtils {

	public static ArrayList<String> extractFromXmlByXPath(String xmlFilePath, String xpathString) throws IOException, XPathExpressionException{
		
		//xmls are utf-9
		String content = FileUtils.fileToString(xmlFilePath, "UTF-8", false);
	
		ArrayList<String> results = new ArrayList<String>();
		
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();

		InputSource source = new InputSource(new StringReader(content));

		NodeList nodes = (NodeList) xpath.evaluate(xpathString, source,XPathConstants.NODESET);
		
		//TODO nodelise iteration should come here
		if (nodes!=null){
			for (int i=0;i<nodes.getLength();i++){
				Node current = nodes.item(i);
				if (current.getNodeType() == Node.ELEMENT_NODE){
					Element el = (Element) current ;
					results.add(el.getTextContent());
				}
			}
		}
		
		return results ;
	}
}
