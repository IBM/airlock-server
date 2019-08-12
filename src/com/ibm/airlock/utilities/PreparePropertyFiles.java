package com.ibm.airlock.utilities;

import java.io.File;
import java.io.PrintWriter;
import java.util.TreeSet;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.translations.SmartlingLocales;

public class PreparePropertyFiles
{
	public static void main(String[] args)
	{
		if (args.length != 2)
		{
			System.out.println("usage: original.json outputFolder");
			return;
		}

		try {
			run(args[0], args[1]);
			System.out.println("DONE");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	static void run(String inputFile, String outputFolder) throws Exception
	{
		JSONObject json = Utilities.readJson(inputFile);
		File out = new File(outputFolder);
		if (!out.isDirectory())
			out.mkdir();

		JSONArray strings = json.getJSONArray("strings");
		TreeSet<String> locales = getLocales(strings);
		for (String locale : locales)
		{
			doLocale(strings, out, locale);
		}
	}

	@SuppressWarnings("unchecked")
	static TreeSet<String> getLocales(JSONArray strings) throws Exception
	{
		TreeSet<String> out = new TreeSet<String>();
		out.add("en-us");

		 for (int i = 0; i < strings.length(); ++i)
		 {
			JSONObject one = strings.getJSONObject(i);
			JSONObject trans = one.optJSONObject("translations");
			out.addAll(trans.keySet());
		 }
		return out;
	}
	static void doLocale(JSONArray strings, File out, String locale)  throws Exception
	{
		String smartlingLocale = SmartlingLocales.get(locale);

		String fileName = smartlingLocale.replace('-', '_') + ".properties";
		File outFile = new File(out, fileName);
		PrintWriter writer = new PrintWriter(outFile, "UTF-8");
		
		 for (int i = 0; i < strings.length(); ++i)
		 {
			JSONObject one = strings.getJSONObject(i);
			String key = one.optString("key");
			String translation = null;
			 
			if (locale.equals("en-us"))
			{
				translation = one.getString("value");
			}
			else
			{
				JSONObject trans = one.optJSONObject("translations");
				if (trans == null)
					continue;
				JSONObject lang = trans.optJSONObject(locale);
				if (lang == null)
					continue;

				translation = lang.getString("translatedValue");
			}
			if (key != null && translation != null)
				writer.println(key + "=" + translation);
		 }
		 writer.close();
	}
}
