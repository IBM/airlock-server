package com.ibm.airlock.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.Utilities;

public class ConvertTranslations
{
	static final String PREFIX = "values-";
	static final String STRING_FILE = "strings.xml";
	static final String STRING_TAG = "string";
	static final String NAME_ATTR = "name";
	static final String L_BRACKET = "[[[";
	static final String R_BRACKET = "]]]";
	static final String PLACEHOLDER_TAG = "xliff:g";

	static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	static Pattern pattern = Pattern.compile("\\%(\\d+)\\$");

	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.out.println("usage: ConvertTranslations propertiesFile");
			return;
		}

		try {
			new ConvertTranslations().run(args[0]);
			System.out.println("DONE");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	void run(String properties) throws Exception
	{
		Utilities.Props props = new Utilities.Props();
		props.load(properties);

		String inputFolder = props.get("inputFolder");
		String outputFolder = props.get("outputFolder");
		boolean smartlingFormat = props.get("smartlingFormat").equals("true");
		boolean toUpper = props.get("toUpper").equals("true");

		String mapIds = props.getOptional("mapIds"); // optional filtering and conversion of translation IDs
		Map<String,String[]> ids = loadIds(mapIds);

		File in = new File(inputFolder);
		File out = new File(outputFolder);
		if (!in.isDirectory())
			throw new Exception("missing input folder " + inputFolder);

		if (!out.isDirectory())
			out.mkdir();

		for( File f : in.listFiles())
		{
			doSubfolder(f, out, ids, smartlingFormat, toUpper);
		}
	}

	void doSubfolder(File subFile, File out, Map<String,String[]> ids, boolean smartlingFormat, boolean toUpper) throws Exception
	{
		File xml = null;
		String folder = subFile.getName();

		if (subFile.isDirectory() && folder.startsWith(PREFIX))
			xml = findXml(subFile);

		if (xml == null)
			System.out.println("skipping folder " + folder);
		else
		{
			String localeName = folder.substring(PREFIX.length());
			System.out.println("processing " + localeName);
			Map<String,String> map = extractXml(xml, ids);
			printMap(out, map, localeName, smartlingFormat, toUpper);
			printStats(out, map, localeName);
		}
	}
	File findXml(File subFile)
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
	Map<String,String> extractXml(File xml,  Map<String,String[]> ids) throws Exception
	{
		Map<String,String> out = new LinkedHashMap<String,String>(); // to preserve order
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(xml);
		NodeList tags = doc.getElementsByTagName(STRING_TAG);

		for (int i = 0; i < tags.getLength(); ++i)
		{
			addString(tags.item(i), out,  ids);
		}
		return out;
	}
	void addString(Node node, Map<String,String> out,  Map<String,String[]> ids)
	{
		String origId = null;
		 if (node.hasAttributes())
		 {
            Attr attr = (Attr) node.getAttributes().getNamedItem(NAME_ATTR);
            if (attr != null)
            	origId = attr.getValue();
		 }
		 if (origId == null)
		 {
				System.out.println("warning: skipping string tag without name");
				return;
		 }
		 String[] translateId = getAlternativeIds(origId, ids);
		 if (translateId == null)
			 return;

		StringBuilder b = new StringBuilder();

		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling())
		{
			switch (child.getNodeType())
			{
			case Node.TEXT_NODE:
			case Node.CDATA_SECTION_NODE:
				b.append(child.getTextContent());
				break;

			case Node.ELEMENT_NODE:
				int placeholder = getPlaceholder(child);
				if (placeholder > 0)
				{
					b.append(L_BRACKET);
					b.append(placeholder);
					b.append(R_BRACKET);
				}

			default:
			}
		}

		String val = b.toString(); // empty strings are valid
		val = removeBackslash(val); // TEMPORARY!

		for (int i = 0; i < translateId.length; ++i)
			out.put(translateId[i], val);
	}

	// TODO TEMPORARY: fix invalid backslash
	// for now assume at most one such error
	String removeBackslash(String in)
	{
		in = in.replace("\\\"", "\"");
		in = in.replace("\\'", "'");
		//in = in.replace("/\"", "\"");
		//in = in.replace("/'", "'");
		return in;
	}

	class Sorter implements Comparable<Sorter>
	{
		String key, value;
		Sorter(String key, String value) {
			this.key = key; this.value = value;
		}
		// longer texts come first
		public int compareTo(Sorter o) {
			return o.value.length() - this.value.length();
		}
	}
	void printStats(File out, Map<String,String> map, String localeName) throws Exception
	{
		File outFile = new File(out, localeName + ".stats.txt");
		printStats(outFile, map);
	}
	void printStats(File outFile, Map<String,String> map) throws Exception
	{
		ArrayList<Sorter> arr = new ArrayList<Sorter>();
		for (Map.Entry<String,String> e : map.entrySet())
		{
			arr.add(new Sorter(e.getKey(), e.getValue()));
		}
		Collections.sort(arr);

		PrintWriter writer = new PrintWriter(outFile, "UTF-8");
		for (Sorter item : arr)
		{
			writer.println("length: " + item.value.length() + ", key: " + item.key + ", value: " + item.value);
		}
		writer.close();
	}
	void printMap(File outDir, Map<String,String> map, String localeName, boolean smartlingFormat, boolean toUpper) throws Exception
	{
		File outFile = new File(outDir, localeName + ".json");
		printMap(outFile, map, smartlingFormat, toUpper);
	}
	void printMap(File outFile, Map<String,String> map,  boolean smartlingFormat, boolean toUpper) throws Exception
	{
		PrintWriter writer = new PrintWriter(outFile, "UTF-8");
		if (smartlingFormat)
			printSmartling(writer, map, toUpper);
		else
			printSimple(writer, map, toUpper);
		writer.close();
	}
	static public void printSimple(PrintWriter writer, Map<String,String> map, boolean toUpper)
	{
		writer.println("{");
		boolean firstime = true;
		for (Map.Entry<String,String> item : map.entrySet())
		{
			if (firstime)
				firstime = false;
			else
				writer.println(",");

			writer.write(Utilities.escapeJson(item.getKey()));
			writer.write(" : ");
			String value = item.getValue();
			if (toUpper)
				value = value.toUpperCase();
			writer.write(Utilities.escapeJson(value));
		}
		writer.println("\n}");
	}
	public static class SmartlingData
	{
		public String key, value, instruction, variant;

		public SmartlingData(String key, String value, String instruction, String variant)
		{
			this.key = key;
			this.value = value;
			this.instruction = instruction;
			this.variant = variant;
		}
	}
	public static void printSmartling(PrintWriter writer, Map<String,String> map, boolean toUpper)  
	{
		ArrayList<SmartlingData> data = new ArrayList<SmartlingData>();
		for (Map.Entry<String,String> item : map.entrySet())
		{
			data.add(new SmartlingData(item.getKey(), item.getValue(), null, null));
		}
		printSmartling(writer, data, toUpper) ;
		
	}
	public static void printSmartling(PrintWriter writer, ArrayList<SmartlingData> data, boolean toUpper)  
	{

/* this is no good - it uses a hash map and mangles the order
  	static JSONArray jarray(String value)
	{
		JSONArray out = new JSONArray();
		out.add(value);
		return out;
	}
		JSONObject root = new JSONObject();

		JSONObject smartling = new JSONObject();
		smartling.put("translate_mode", "custom");
		smartling.put("translate_paths", jarray("* /translation"));
		smartling.put("source_key_paths", jarray("{*}/translation"));
		smartling.put("placeholder_format_custom", jarray(Constants.SMARTLING_PLACEHOLDER_FORMAT));
		smartling.put("variants_enabled", true);
		root.put("smartling", smartling);

		int counter = 0;
		for (Map.Entry<String,String> item : map.entrySet())
		{
			JSONObject json = new JSONObject();
			json.put("stringId", item.getKey());
			json.put("translation", item.getValue());
			String key = "translationKey" + (++counter);
			root.put(key, json);
		}
		writer.print(root.write(true));
*/
		writer.println("{");
		writer.println("\"smartling\" : {");
		writer.println("\"translate_mode\" : \"custom\",");
		writer.println("\"translate_paths\": [\"*/translation\"],");
		writer.println("\"source_key_paths\": [\"{*}/translation\"],");
		writer.println("\"placeholder_format_custom\": [" + Utilities.escapeJson(Constants.SMARTLING_PLACEHOLDER_FORMAT) + "],");
		writer.println("\"variants_enabled\": true");		
		
		if (data.size() == 0) 
			writer.println("}");
		else
			writer.println("},");

		int counter = 0;
		for (SmartlingData item : data)
		{
			if (counter != 0)
				writer.println(",");
			++counter;

			writer.println("\"translationKey" + counter + "\": {");
			writer.println("\"stringId\" : " + Utilities.escapeJson(item.key) + ",");

			if (item.instruction != null)
				writer.println("\"instruction\" : " + Utilities.escapeJson(item.instruction) + ",");
			if (item.variant != null)
				writer.println("\"variant\" : " + Utilities.escapeJson(item.variant) + ",");

			if (toUpper) item.value = item.value.toUpperCase();
			writer.println("\"translation\" : " + Utilities.escapeJson(item.value));
			writer.print("}");
		}
		writer.println("\n}");
	}

	int getPlaceholder(Node node)
	{
		if (!node.getNodeName().equals(PLACEHOLDER_TAG))
			return 0;

		String value = node.getTextContent();
		Matcher m = pattern.matcher(value);
		if (!m.find())
			return 0;

		String find = m.group(1);
		return Integer.parseInt(find);
	}

	ArrayList<String[]> getLines(String mapIds) throws Exception
	{
		String line;
		ArrayList<String[]> out = new ArrayList<String[]>();
		BufferedReader br = new BufferedReader(new FileReader(mapIds));

		while ((line = br.readLine()) != null)
		{
			line = line.trim();
			if (line.isEmpty())
				continue;

			String[] items = line.split("\\s+");
			if (items.length < 2)
			{
				System.out.println("warning: skipping invalid mapping: " + line);
				continue;
			}
			out.add(items);
		}
		br.close();
		return out;
	}
	Map<String,String[]> loadIds(String mapIds) throws Exception
	{
		if (mapIds == null)
			return null;

		ArrayList<String[]> lines = getLines(mapIds);
		checkIds(lines);

		HashMap<String,String[]> out = new HashMap<String,String[]>();

		for (String[] items : lines)
		{
			String from = items[0];
			String[] to = new String[items.length-1];
			for (int i = 0; i < to.length; ++i)
				to[i] = items[i+1];

			out.put(from,to);
		}
		return out;
	}
	
	void checkIds(ArrayList<String[]> lines) throws Exception
	{
		Map<String,Set<String>> v1 = new TreeMap<String,Set<String>>();
		Map<String,Set<String>> v2 = new TreeMap<String,Set<String>>();

		for (String[] items : lines)
		{
			String from = items[0];
			TreeSet<String> to = new TreeSet<String>();
			for (int i = 1; i < items.length; ++i)
				to.add(items[i]);

			Set<String> val1 = v1.get(from);
			if (val1 == null)
				val1 = to;
			else
				val1.addAll(to);
			v1.put(from,val1);

			for (String str : to)
			{
				Set<String> val2 = v2.get(str);
				if (val2 == null)
					val2 = new TreeSet<String>();
				val2.add(from);
				v2.put(str, val2);
			}
		}
		System.out.println("looking for duplicates in id map...");
		printDuplicates(v1);
		printDuplicates(v2);
	}
	void printDuplicates(Map<String,Set<String>> map)
	{
		for (Map.Entry<String,Set<String>> ent : map.entrySet())
		{
			String key = ent.getKey();
			Set<String> set = ent.getValue();
			if (set.size() != 1)
				System.out.println("key: " + key + ", values: " + set);
		}
	}
	String[] getAlternativeIds(String origId, Map<String,String[]> ids)
	{
		String[] out;
		if (ids == null)
		{
			out = new String[1];
			out[0] = origId;
		}
		else
			out = ids.get(origId);

		return out;
	}
}

