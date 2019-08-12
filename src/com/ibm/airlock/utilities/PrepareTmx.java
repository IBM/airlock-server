package com.ibm.airlock.utilities;

import java.io.PrintWriter;
import java.util.Set;
//import java.util.UUID;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.translations.SmartlingLocales;

import org.apache.commons.lang.StringEscapeUtils;

public class PrepareTmx
{
	static final String H1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	static final String H2 = "<!DOCTYPE tmx SYSTEM \"tmx14.dtd\">\n<tmx version=\"1.4\">";
	static final String H3 = "<header creationtool=\"PrepareTmx\" creationtoolversion=\"1.1\" srclang=\"en-US\" datatype=\"xml\" segtype=\"block\" adminlang=\"en-US\" o-tmf=\"Airlock\" />";
	
	public static void main(String[] args)
	{
		if (args.length != 2 && args.length != 3)
		{
			System.out.println("usage: original.json output.tmx [useKey]");
			return;
		}

		try {
			boolean useKey = (args.length == 3 && args[2].equals("useKey"));
			run(args[0], args[1], useKey);
			System.out.println("DONE");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void run(String original, String tmx, boolean useKey) throws Exception
	{
		JSONObject json = Utilities.readJson(original);
		JSONArray strings = json.getJSONArray("strings");
		PrintWriter out = new PrintWriter(tmx, "UTF-8");

		 out.println(H1);
		 out.println(H2);
		 out.println(H3);
		 out.println("<body>");
		 for (int i = 0; i < strings.length(); ++i)
		 {
			JSONObject string = strings.getJSONObject(i);
			printString(string, out, useKey);
		 }

		 out.println("</body>\n</tmx>");
		 out.close();
	}
	static void printString(JSONObject one, PrintWriter out, boolean useKey) throws Exception
	{
		String id = one.getString("uniqueId");
		String source = one.getString("value");
		String variant = useKey ? one.optString("key") : one.optString("variant");
		JSONObject trans = one.getJSONObject("translations");

		out.print("<tu tuid=\"");
		//out.print(UUID.randomUUID().toString());
		out.print(id);
		out.println("\" segtype=\"block\">");

		out.println("<prop type=\"x-segment-id\">0</prop>");

		if (variant != null && !variant.isEmpty())
		{
			out.print("<prop type=\"x-smartling-string-variant\">");
			out.print(StringEscapeUtils.escapeXml(variant));
			out.println("</prop>");
		}
		printItem("en-US", source, out);

		@SuppressWarnings("unchecked")
		Set<String> keys = trans.keySet();
		for (String key : keys)
		{
			JSONObject lang = trans.getJSONObject(key);
			String value = lang.getString("translatedValue");
			String locale = SmartlingLocales.get(key);
			printItem(locale, value, out);
		}
		out.println("</tu>");
	}
	static void printItem(String locale, String value, PrintWriter out) throws Exception
	{
		out.print("<tuv xml:lang=\"");
		out.print(locale);
		out.print("\"><seg>");
		out.print(StringEscapeUtils.escapeXml(value));
		out.println("</seg></tuv>");
	}
}
