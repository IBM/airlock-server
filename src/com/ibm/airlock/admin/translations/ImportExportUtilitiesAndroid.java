package com.ibm.airlock.admin.translations;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportExportUtilitiesAndroid extends ImportExportUtilities{

    static final String PREFIX = "values-";
    static final String STRING_FILE = "strings.xml";
    static final String STRING_TAG = "string";
    static final String NAME_ATTR = "name";


    static // default mapping
    {
        localeMap.put("no", "nb");
        //localeMap.put("nb", "no");
        localeMap.put("pt-rBR", "pt_BR");
        localeMap.put("pt_BR", "pt-rBR");
        localeMap.put("zh-rCN", "zh_CN");
        localeMap.put("zh_CN", "zh-rCN");
        localeMap.put("zh-rTW", "zh_TW");
        localeMap.put("zh_TW", "zh-rTW");
        localeMap.put("id", "in");
        localeMap.put("in", "id");
        localeMap.put("en-rGB", "en_GB");
        localeMap.put("en_GB", "en-rGB");
        localeMap.put("en-rAU", "en_AU");
        localeMap.put("en_AU", "en-rAU");
        localeMap.put("fr-rCA", "fr_CA");
        localeMap.put("fr_CA", "fr-rCA");
    }

    static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    static Pattern pattern = Pattern.compile("\\%(\\d+)\\$");
    static Pattern pattern2 = Pattern.compile("\\%[sSdfb]");

    public boolean doDefaultSubfolder(File inputFolder, HashMap<String,OriginalString> originalStringMap,boolean keepData, boolean preserveFormat) throws Exception{
        logger.info("processing default" );
        File subFile = null;
        for (File f : inputFolder.listFiles()) {
            if (f.isDirectory() && f.getName().endsWith("values")) {
               subFile = f;
               break;
            }
        }
        if(subFile == null){
            return false;
        }
        File xml = findXml(subFile);
        if(xml == null){
            throw new Exception("missing default values string.xml ");
        }
        Map<String,String> map = extractXml(xml,keepData, preserveFormat);
        //TODO arrays
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            OriginalString importedString = new OriginalString(key, value);
            originalStringMap.put(key,importedString);
        }
        return true;
    }

    public void doLocaleSubfolder(File subFile, HashMap<String,OriginalString> originalStringMap, LinkedList<String> supportedLanguages,boolean keepData, boolean preserveFormat) throws Exception{
        File xml = null;
        String folder = subFile.getName();

        if (subFile.isDirectory() && folder.startsWith(PREFIX))
            xml = findXml(subFile);

        if (xml == null)
            logger.info("skipping folder " + folder);
        else
        {
            String localeName = folder.substring(PREFIX.length());
            String mappedLocaleName = mapLocale(localeName);
            if(mappedLocaleName == null){
                logger.info("unsuported locale " + localeName);
                return;
            }
            logger.info("processing " + localeName);
            Map<String,String> map = extractXml(xml,keepData, preserveFormat);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if(originalStringMap.containsKey(entry.getKey())) {
                    originalStringMap.get(entry.getKey()).addStringTranslationForLocale(mappedLocaleName, entry.getValue(), supportedLanguages);
                    if(mappedLocaleName.equals("nb")){ // special treatment for norvergian
                        originalStringMap.get(entry.getKey()).addStringTranslationForLocale("no", entry.getValue(), supportedLanguages);
                    }
                }

            }
        }
    }

    public File exportToZipFile(String seasonId,LinkedList<OriginalString> originalStrings) throws Exception{
        HashMap<String,Document> localeFileMap = new HashMap<>();
        String separator = "/";
        String fileName ="temp"+separator+"export"+separator+seasonId;
        String zipName = "temp"+separator+"export"+separator+"exportedStrings.zip";
        File newFile = new File(fileName);
        String RESOURCESTAG = "resources";
        String STRINGTAG = "string";
        String NAMEATTR = "name";
        if(!newFile.exists())
            newFile.mkdirs();
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // root elements
        Document valueDoc = docBuilder.newDocument();
        Element rootElement = valueDoc.createElement(RESOURCESTAG);
        valueDoc.appendChild(rootElement);

        //rootElement.setAttribute("xmlns:tools","http://schemas.android.com/tools");
        //rootElement.setAttribute(" xmlns:xliff","urn:oasis:names:tc:xliff:document:1.2");

        for(int i = 0 ; i<originalStrings.size();++i) {
            OriginalString oriString = originalStrings.get(i);

            //default
            Element stringElem = valueDoc.createElement(STRINGTAG);
            stringElem.setAttribute(NAMEATTR, oriString.getKey());
            String value = removePlaceHolders(oriString.getValue());
            value = restoreBackslash(value);
            if(value.startsWith("<![CDATA[")){
                stringElem.appendChild(valueDoc.createCDATASection(value.substring(9,value.length()-3)));
            }
            else {
                stringElem.appendChild(valueDoc.createTextNode(value));
            }
            rootElement.appendChild(stringElem);

            //translation
            OriginalString.StringTranslations stringTranslations = oriString.getStringTranslations();
            Iterator<Map.Entry<String,OriginalString.StringTranslations.LocaleTranslationData>> iter = stringTranslations.stringTranslationsMap.entrySet().iterator();
            while (iter.hasNext()){
                Map.Entry<String,OriginalString.StringTranslations.LocaleTranslationData> entry = iter.next();
                Document localeDoc = getFileForLocale(entry.getKey(),localeFileMap);
                Node rootLocaleElement = localeDoc.getElementsByTagName(RESOURCESTAG).item(0);
                Element stringLocaleElem = localeDoc.createElement(STRINGTAG);
                stringLocaleElem.setAttribute(NAMEATTR, oriString.getKey());
                String localeValue = removePlaceHolders(entry.getValue().getTranslatedValue());
                stringLocaleElem.appendChild(localeDoc.createTextNode(localeValue));
                rootLocaleElement.appendChild(stringLocaleElem);
            }
        }
        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        //default
        DOMSource source = new DOMSource(valueDoc);
        newFile = new File(fileName,"values");
        if(!newFile.exists())
            newFile.mkdirs();
        File stringsFile = new File(fileName,"values/strings.xml");
        StreamResult result = new StreamResult(stringsFile);
        transformer.transform(source, result);

        //translations
        Iterator<Map.Entry<String,Document>> iter2 = localeFileMap.entrySet().iterator();
        while (iter2.hasNext()){
            Map.Entry<String,Document> entry = iter2.next();
            source = new DOMSource(entry.getValue());
            newFile = new File(fileName,"/values-"+mapLocale(entry.getKey()));
            if(!newFile.exists())
                newFile.mkdirs();
            stringsFile = new File(fileName,"/values-"+mapLocale(entry.getKey())+"/strings.xml");
            result = new StreamResult(stringsFile);
            transformer.transform(source, result);
        }
        zip(zipName,fileName);
        return new File(zipName);
    }

    public String removePlaceHolders(String in) {
        return in.replace(L_BRACKET,"%").replace(R_BRACKET,"$s");
    }

    public Document getFileForLocale(String locale, HashMap<String,Document> mapLocaleXml) throws ParserConfigurationException{
        if(mapLocaleXml.containsKey(locale)){
            return  mapLocaleXml.get(locale);
        }
        else {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document localeDoc = docBuilder.newDocument();
            Element rootElement = localeDoc.createElement("resources");
            localeDoc.appendChild(rootElement);
            mapLocaleXml.put(locale,localeDoc);
            return localeDoc;
        }
    }

    public  String mapLocale(String localeName){
        if(localeMap.containsKey(localeName)){
            return localeMap.get(localeName);
        }
        return localeName;
    }

    public  File findXml(File subFile)
    {
        File[] sons = subFile.listFiles();
        if (sons == null)
            return null;

        for (int i = 0; i < sons.length; ++i)
        {
            if (sons[i].getName().equals(STRING_FILE) && sons[i].isFile())
                return sons[i];
        }
        return null;
    }
    public  Map<String,String> extractXml(File xml,boolean keepData, boolean preserveFormat) throws Exception
    {
        Map<String,String> out = new LinkedHashMap<>(); // to preserve order
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xml);
        NodeList tags = doc.getElementsByTagName(STRING_TAG);

        for (int i = 0; i < tags.getLength(); ++i)
        {
            addString(tags.item(i), out,keepData, preserveFormat);
        }
        return out;
    }
    public String getFormattedString(String str, boolean preserveFormat) {
    	String text = str;
    	if (!preserveFormat && str!=null) {
        	text = fixPlaceholders(text);
            text = fixNumberPlaceholder(text);
        }
    	return text;
    }
    public void addString(Node node, Map<String,String> out,boolean keepData, boolean preserveFormat)
    {
        String id = null;
        if (node.hasAttributes())
        {
            Attr attr = (Attr) node.getAttributes().getNamedItem(NAME_ATTR);
            if (attr != null)
                id = attr.getValue();
        }
        if (id == null)
        {
            logger.info("warning: skipping string tag without name");
            return;
        }
        StringBuilder b = new StringBuilder();

        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling())
        {

            String text = child.getTextContent();
            if(text.contains("@string/")){
                continue;
            }
            if (!preserveFormat) {
            	text = fixPlaceholders(text);
                text = fixNumberPlaceholder(text);
            }
            if(child.getNodeName().equals("#cdata-section") && keepData == true){
                text = "<![CDATA[" +text + "]]>";
            }
            if(child.getNodeName().equals("xliff:g" ) && keepData == true){
                StringBuilder attributes = new StringBuilder();
                int size = child.getAttributes().getLength();
                for(int i = 0; i<size; ++i){
                    Node attr = child.getAttributes().item(i);
                    attributes.append(attr.getNodeName() + "=\""+ attr.getNodeValue() +"\" ");
                }
                text = "<xliff:g " + attributes.toString() +text + "</xliff:g>";
            }
            b.append(text);
        }

        String val = b.toString(); // empty strings are valid
        val = removeBackslash(val);
        out.put(id, val);
    }

    public String fixPlaceholders(String text) {
        String[] splitText = text.split(pattern2.pattern());
        if (splitText.length == 1)
            return text;
        StringBuilder sb = new StringBuilder();
        int count = 1;
        for (int i = 0; i < splitText.length - 1; ++i) {
            sb.append(splitText[i] + L_BRACKET + count + R_BRACKET);
            count++;
        }
        sb.append(splitText[splitText.length - 1]);
        return sb.toString();
    }

    public  String fixNumberPlaceholder(String text)
    {
        String substring = text;
        Matcher m = pattern.matcher(substring);
        StringBuilder sb = new StringBuilder();
        while(m.find()) {
            int index = substring.indexOf("%");
            String placeholder = m.group(1);
            sb.append(substring.substring(0,index));
            sb.append(L_BRACKET);
            sb.append(placeholder);
            sb.append(R_BRACKET);
            substring = substring.substring(index + 3 + placeholder.length());// 3 is % + $x
            m = pattern.matcher(substring);
        }
        sb.append(substring);
        return sb.toString();
    }
}
