package com.ibm.airlock.admin.translations;

import java.util.TreeMap;

// this class maps Airlock locales to Smartling locale IDs
public class SmartlingLocales {

	static TreeMap<String,String> map = new TreeMap<String,String>();

	static // default mapping
	{
		map.put("en", "en");
		map.put("en-us", "en-US");
		map.put("ar", "ar"); // arabic
		map.put("ca", "ca-ES"); // catalan
		map.put("cs", "cs-CZ"); // czech
		map.put("da", "da-DK"); // denmark
		map.put("de", "de-DE"); // germany
		map.put("el", "el-GR"); // greece
		map.put("en_au", "en-AU"); // australia
		map.put("en_gb", "en-GB"); // uk
		map.put("en-rgb", "en-GB"); // uk
		map.put("es", "es"); // spain
		map.put("fa", "fa-IR"); // persian
		map.put("fi", "fi-FI"); // finland
		map.put("fr", "fr-FR"); // france
		map.put("fr-rca", "fr-FR");
		map.put("fr_ca", "fr-CA"); // canada
		map.put("he", "he-IL"); // israel
		map.put("iw", "he-IL"); // israel
		map.put("hi", "hi-IN"); // hindi
		map.put("hr", "hr-HR"); // croatia
		map.put("hu", "hu-HU"); // hungary
		map.put("id", "id-ID"); // indonesia
		map.put("in", "id-ID"); // also indonesia?
		map.put("it", "it"); // italy
		map.put("ja", "ja-JP"); // japan
		map.put("ko", "ko-KR"); // korea
		map.put("ms", "ms-MY"); // malasia
		map.put("nl", "nl-NL"); // netherlands
		map.put("no", "no-NO"); // norway
		// we map bokomal to regular norwegian
		// map.put("nb", "nb-NO"); // norway bokmal
		map.put("nb", "no-NO"); // norway bokmal
		map.put("pl", "pl-PL"); // poland  CHANGED FROM ("pl", "pl");
		map.put("pt-rbr", "pt-BR"); // brasil
		map.put("pt_br", "pt-BR"); // brasil
		map.put("pt", "pt-PT"); // portugal
		map.put("ro", "ro-RO"); // romania
		map.put("ru", "ru-RU"); // russia
		map.put("sk", "sk-SK"); // slovakia
		map.put("sv", "sv-SE"); // sweden
		map.put("th", "th"); // thailand CHANGED FROM ("th", "th-TH")
		map.put("tr", "tr"); // turkey CHANGED FROM ("tr", "tr-TR")
		map.put("uk", "uk-UA"); // ukraine
		map.put("vi", "vi-VN"); // vietnam
		map.put("zh_cn", "zh-CN"); // china
		map.put("zh_tw", "zh-TW"); // taiwan
		map.put("zh-rcn", "zh-CN"); // china
		map.put("zh-rtw", "zh-TW"); // taiwan
		map.put("zh_hans", "zh-CN"); //  china
		map.put("zh_hant", "zh-TW"); // taiwan
	}

	// not synchronized since it's called once during initialization
	public static void updateMap(String content) throws Exception
	{
		TreeMap<String,String> newmap = new TreeMap<String,String>();
		String[] lines = content.split("\n");
		for (String line : lines)
		{
			String[] items = line.trim().split("\\s+");
			if (items.length == 0)
				continue;
			if (items.length != 2)
				throw new Exception("invalid smartling locale mapping: " + line);
			newmap.put(items[0].toLowerCase(), items[1]);
		}
		if (newmap.isEmpty())
			throw new Exception("empty smartling locale mapping");

		map = newmap;
	}
	public static String get(String key)
	{
		return map.get(key.toLowerCase());
	}
}
