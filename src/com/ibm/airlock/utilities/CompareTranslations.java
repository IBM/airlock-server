package com.ibm.airlock.utilities;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.admin.Utilities;

public class CompareTranslations extends ConvertTranslations
{
	Map<String,String> git2AirlockLocales = new TreeMap<String,String>();
	Map<String,String> airlock2GitLocales = new TreeMap<String,String>();

	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.out.println("usage: CompareTranslations propertiesFile");
			return;
		}

		try {
			new CompareTranslations().run(args[0]);
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
		String missingFolder = props.get("missingFolder");
		String inputJson = props.get("inputJson");
		String report = props.get("report");
		String reportIgnoreCase = props.get("reportIgnoreCase");
		String localeMap = props.get("localeMap"); // airlock locale to git locale
		boolean toUpper = props.get("toUpper").equals("true");
		String mapIds = props.get("mapIds"); // conversion of translation IDs
		Map<String,String[]> ids = loadIds(mapIds);

		File in = new File(inputFolder);
		if (!in.isDirectory())
			throw new Exception("missing input folder " + inputFolder);

		File outFolder = new File(missingFolder);
		if (!outFolder.isDirectory())
			outFolder.mkdir();

		ArrayList<String[]> lines = getLines(localeMap);
		for (String[] items : lines)
		{
			airlock2GitLocales.put(items[0], items[1]);
			git2AirlockLocales.put(items[1], items[0]);
		}

		Map<String,Map<String,String>> allTranslations = new TreeMap<String,Map<String,String>>();
		Map<String,Map<String,String>> fromGit = new TreeMap<String,Map<String,String>>();
		Map<String,Map<String,String>> missing = new TreeMap<String,Map<String,String>>();

		for( File f : in.listFiles())
		{
			doSubfolder(f, fromGit, ids);
		}

		JSONObject json = Utilities.readJson(inputJson);
		jsonToMap(json, allTranslations);

		PrintWriter writer = new PrintWriter(report, "UTF-8");
		printComparison(writer, fromGit, allTranslations, missing, true);
		writer.close();

		writer = new PrintWriter(reportIgnoreCase, "UTF-8");
		printComparison(writer, fromGit, allTranslations, null, false);
		writer.close();

		for (Map.Entry<String,Map<String,String>> ent : missing.entrySet())
		{
			String locale = ent.getKey();
			Map<String,String> map = ent.getValue();
			if (map.isEmpty())
				continue;

			File out = new File(outFolder, locale + ".json");
			writer = new PrintWriter(out, "UTF-8");
			printSmartling(writer, map, toUpper);
			writer.close();
		}
	}

	void doSubfolder(File subFile, Map<String,Map<String,String>> out, Map<String,String[]> ids) throws Exception
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
			System.out.println("processing git " + localeName);
			Map<String,String> map = extractXml(xml, ids);
			out.put(localeName, map);
		}
	}
	void jsonToMap(JSONObject json, Map<String,Map<String,String>> map) throws JSONException
	{
		JSONArray strings = json.getJSONArray("strings");

		for (int i = 0; i < strings.length(); ++i)
		{
			JSONObject string = strings.getJSONObject(i);

			String key = string.getString("key");
			//String value = string.getString("value");
			JSONObject translations = string.getJSONObject("translations");

			@SuppressWarnings("unchecked")
			Set<String> locales = translations.keySet();
			for (String locale : locales) // does not include the default language
			{
				JSONObject translation = translations.getJSONObject(locale);
				String translatedValue = translation.getString("translatedValue");
				copyTranslation(map, locale, key, translatedValue);
			}
		}
	}
	void printComparison(PrintWriter writer, Map<String,Map<String,String>> fromGit,
			Map<String,Map<String,String>> allTranslations, Map<String,Map<String,String>> missing, boolean caseSensitive)
	{
		Set<String> locales = new TreeSet<String>(fromGit.keySet());
//		locales.addAll(allTranslations.keySet());
		for (String locale : allTranslations.keySet())
		{
			String gitLocale = airlock2GitLocales.get(locale);
			if (gitLocale == null)
				gitLocale = locale;
			locales.add(gitLocale);
		}

		for (String locale : locales)
		{
			String airlockLocale = git2AirlockLocales.get(locale);
			writer.println("\n\n>>> LOCALE: " + locale + " (" + airlockLocale + ")");
			Map<String,String> git = fromGit.get(locale);
			Map<String,String> jso = (airlockLocale == null) ? null : allTranslations.get(airlockLocale);

			if (git == null)
			{
				writer.println("locale does not exist in GIT");
				continue;
			}
			if (jso == null)
			{
				writer.println("locale does not exist in JSON");
				if (missing != null)
					missing.put(locale, git); // copy all translations from GIT
				continue;
			}

			Set<String> keys = new TreeSet<String>(git.keySet());
			keys.addAll(jso.keySet());

			for (String key : keys)
			{
				String gitval = git.get(key);
				String jsoval = jso.get(key);
				if (isEqual(gitval, jsoval, caseSensitive))
					continue;

				writer.println("key " + key + " differs:");
				writer.println("   in GIT : " + gitval);
				writer.println("   in JSON: " + jsoval);

				if (jsoval == null)
				{
					writer.println("   ++AIRLOCK NO TRANSLATION++");
					if (missing != null)
						copyTranslation(missing, locale, key, gitval);
				}
				else if (gitval != null)
				{
					if (gitval.length() > jsoval.length())
						writer.println("   ++GIT TRANSLATION IS LARGER++");
					else
						writer.println("   ++GIT TRANSLATION IS SMALL/SAME SIZE++");
				}
			}
		}
	}
	boolean isEqual(String a, String b, boolean caseSensitive)
	{
		if (a == null || b == null) // one of them is not null
			return false;

		if (caseSensitive)
			return a.equals(b);

		return a.compareToIgnoreCase(b) == 0;
	}
	void copyTranslation(Map<String,Map<String,String>> all, String locale, String key, String value)
	{
		Map<String,String> map = all.get(locale);
		if (map == null)
		{
			map = new TreeMap<String,String>();
			all.put(locale, map);
		}
		map.put(key, value);
	}
}
