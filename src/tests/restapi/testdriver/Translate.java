package tests.restapi.testdriver;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.engine.Version;
import com.ibm.airlock.utilities.ConvertTranslations;

import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.testdriver.TestDriver;
import tests.restapi.testdriver.TestDriver.Config;
import tests.restapi.testdriver.TestDriver.Instruction;

public class Translate
{
	TestDriver driver;
	Config config;

	Translate(TestDriver driver)
	{
		this.driver = driver;
		this.config = driver.config;
	}

	static class TData
	{
		String key;
		String fallback;
		String minAppVersion;
		String stage;
		String source;
		TreeMap<String,String> trans = new TreeMap<String,String>();
	}

	//--------------------------
	void addStrings(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1 && instruction.parameters.length != 2)
			throw new Exception(instruction.action.toString() + " requires 1 or 2 parameters (strings-file [ ; output-string-ids-file ] )");

		String path = driver.composePath(instruction.parameters[0]);
		JSONObject json = Utilities.readJson(path);

		String output = null;
		JSONArray ids = null;
		if (instruction.parameters.length == 2)
		{
			output = driver.composePath(instruction.parameters[1]);
			ids = new JSONArray();
		}

		JSONArray allStrings = json.getJSONArray("strings");
		for (int i = 0; i < allStrings.size(); ++i)
		{
			JSONObject singleStr = allStrings.getJSONObject(i);
			driver.removeFields(singleStr);
			String str = singleStr.toString();

			try {
				RestClientUtils.RestCallResults res = RestClientUtils.sendPost(config.translationsUrl + "seasons/" + config.seasonId + "/strings", str, config.sessionToken);
				String id = driver.parseResultId(res.message, null);
				if (ids != null)
					ids.add(id);
			}
			catch (Exception e)
			{
				String msg = e.getLocalizedMessage();
				if (msg.contains("already exists"))
					System.out.println("warning: skipping existing string " + str);
				else
					throw new Exception(("Failed to create string:" + singleStr.write() + "\n" + msg));
			}
		}
		if (ids != null)
		{
			JSONObject obj = new JSONObject();
			obj.put("stringIds", ids);
			String str = obj.write(true);
			Utilities.writeString(str, output);
		}
	}

	// add or update list of strings
	void updateStrings(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1 && instruction.parameters.length != 2)
			throw new Exception(instruction.action.toString() + " requires 1 or 2 parameters (strings-file [ ; output-string-ids-file ] )");

		String path = driver.composePath(instruction.parameters[0]);
		JSONObject json = Utilities.readJson(path);

		String output = null;
		JSONArray ids = null;
		if (instruction.parameters.length == 2)
		{
			output = driver.composePath(instruction.parameters[1]);
			ids = new JSONArray();
		}
		
		JSONObject existing;
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(config.translationsUrl + "seasons/" + config.seasonId + "/strings", config.sessionToken);
			existing = driver.parseError(res.message, null);
		}
		catch (Exception e) {
			existing = new JSONObject();
		}

		TreeMap<String, JSONObject> map = new TreeMap<String, JSONObject>();
		JSONArray oldStrings = existing.getJSONArray("strings");
		for (int i = 0; i < oldStrings.size(); ++i)
		{
			JSONObject singleStr = oldStrings.getJSONObject(i);
			String key = singleStr.getString("key");
			map.put(key, singleStr);
		}

		JSONArray newStrings = json.getJSONArray("strings");
		for (int i = 0; i < newStrings.size(); ++i)
		{
			JSONObject newItem = newStrings.getJSONObject(i);
			driver.removeFields(newItem);

			String key = newItem.getString("key");
			JSONObject oldItem = map.get(key);
			RestClientUtils.RestCallResults res;

			try {
				String stringId;
				if (oldItem == null)
				{
					res = RestClientUtils.sendPost(config.translationsUrl + "seasons/" + config.seasonId + "/strings", newItem.toString(), config.sessionToken);
					stringId = driver.parseResultId(res.message, null);
				}
				else
				{
					Utilities.mergeJson(oldItem, newItem);
					stringId = driver.getId(oldItem);
					res = RestClientUtils.sendPut(config.translationsUrl + "seasons/strings/" + stringId, oldItem.toString(), config.sessionToken);
					driver.parseError(res.message, null);
				}
				if (ids != null)
					ids.add(stringId);
			}
			catch (Exception e) {
				throw new Exception(("Failed to add or update string: " + newItem.write() + "\n" + e.toString()));
			}
		}

		if (ids != null)
		{
			JSONObject obj = new JSONObject();
			obj.put("stringIds", ids);
			String str = obj.write(true);
			Utilities.writeString(str, output);
		}
	}
	// this will add a new translation or update an existing translation
	void addTranslations(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 2)
			throw new Exception(instruction.action.toString() + " requires 2 parameters ( locale ; translations_file_path )");

		String locale = instruction.parameters[0];
		String path = driver.composePath(instruction.parameters[1]);
		String translation = Utilities.readString(path);
		String parms = (config.version.i < Version.v2_5.i) ? "" : "?stage=" + config.stage; // used only in GET

		String url = config.translationsUrl + config.seasonId + "/translate/" + locale;
		JSONObject existing;
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(url + parms, config.sessionToken);
			existing = driver.parseError(res.message, null);
		}
		catch (Exception e) {
			 existing = null;
		}

		try {
			RestClientUtils.RestCallResults res;
			if (existing == null)
				res = RestClientUtils.sendPost(url, translation, config.sessionToken);
			else
				res =  RestClientUtils.sendPut(url, translation, config.sessionToken);
			if (res.code != 200)
				driver.parseError(res.message, null);
		}
		catch (Exception e) {
			throw new Exception("Failed to create translations:\n" + e.getLocalizedMessage());
		}
	}
	void getLocales(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 0)
			throw new Exception(instruction.action.toString() + " does not take parameters");

		String url = config.translationsUrl + config.seasonId + "/supportedlocales";
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(url, config.sessionToken);
		JSONObject json = driver.parseError(res.message, "getLocales");
		System.out.println(json.write(true));
	}
	void addLocale(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("missing locale");

		String url = config.translationsUrl + config.seasonId + "/supportedlocales/" + instruction.parameters[0];
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(url, "", config.sessionToken);
		if (res.code != 200)
			driver.parseError(res.message, "addLocale");
	}

	String parseIds(String list) throws Exception
	{
		String[] ids = list.split(TestDriver.ITEM_SEPARATOR);
		if (ids.length < 1)
			throw new Exception("invalid list of ids " + list);

		StringBuilder b = new StringBuilder();
		for (String id : ids)
		{
			String sep = (b.length() == 0) ? "?ids=" : "&ids=";
			b.append(sep);
			b.append(id);
		}
		return b.toString();
	}

	void newStrings(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("missing output file");

		String path = driver.composePath(instruction.parameters[0]);

		String url = config.translationsUrl + "seasons/" + config.seasonId + "/newstringsfortranslation";
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(url, config.sessionToken);
		JSONObject json = driver.parseError(res.message, "newStrings");
		String str = json.write(true);
		Utilities.writeString(str, path);
		System.out.println(str);
	}

	void stringStatus(Instruction instruction) throws Exception
	{
		String parms = "";
		if (instruction.parameters.length > 0)
			 parms = parseIds(instruction.parameters[0]);

		String url = config.translationsUrl + "seasons/" + config.seasonId + "/strings/statuses" + parms;
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(url, config.sessionToken);
		JSONObject json = driver.parseError(res.message, "stringStatus");
		System.out.println(json.write(true));
	}
	void translationSummary(Instruction instruction) throws Exception
	{
		String parms = "";
		if (instruction.parameters.length > 0)
			 parms = parseIds(instruction.parameters[0]);

		String url = config.translationsUrl + "seasons/" + config.seasonId + "/translate/summary" + parms;
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(url, config.sessionToken);
		JSONObject json = driver.parseError(res.message, "translationSummary");
		System.out.println(json.write(true));
	}
	void stringsInUse(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("missing feature name or id");

		String id = instruction.parameters[0];

		// id may be a name, convert it
		JSONObject features = driver.getFeatures();
		JSONObject fjson =  driver.findName(features, id);
		String fId = driver.getId(fjson);
		//String fName = driver.getName(fjson);

		String url = config.translationsUrl + "seasons/" + fId + "/stringsinuse";
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(url, config.sessionToken);
		JSONObject json = driver.parseError(res.message, "stringsInUse");
		System.out.println(json.write(true));
	}
	void overrideTranslate(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 3)
			throw new Exception(instruction.action.toString() + " requires 2 parameters ( string-id; locale; input-file-path.json ) ");

		String id = instruction.parameters[0];
		String locale = instruction.parameters[1];
		String path = driver.composePath(instruction.parameters[2]);
		String body = Utilities.readString(path);

		String url = config.translationsUrl + "seasons/" + id + "/overridetranslate/" + locale;
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(url, body, config.sessionToken);
		if (res.code != 200)
			driver.parseError(res.message, "overrideTranslate");
	}
	void prepareTranslations(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 3)
			throw new Exception(instruction.action.toString() + " requires 3 parameters ( original.json; out_strings.json; out_translations_folder )");

		String original_json = driver.composePath(instruction.parameters[0]);
		String out_english = driver.composePath(instruction.parameters[1]);
		String out_folder = driver.composePath(instruction.parameters[2]);

		JSONObject original = Utilities.readJson(original_json);
		JSONArray other = original.getJSONArray("strings");

		File outFolder = new File(out_folder);
		if (!outFolder.isDirectory())
			outFolder.mkdir();

		ArrayList<TData> arr = new ArrayList<TData>();
		Set<String> languages = new TreeSet<String>();
		languages.add("en");

		for (int i = 0; i < other.length(); ++i)
		{
			JSONObject one = other.getJSONObject(i);
			TData t = new TData();

			t.key = one.getString("key");
			t.source = one.getString("value");
			t.stage = one.getString("stage");
			t.minAppVersion = one.optString("minAppVersion");
			t.fallback = one.optString("internationalFallback");

			JSONObject trans = one.getJSONObject("translations");
			@SuppressWarnings("unchecked")
			Set<String> keys = trans.keySet();
			for (String key : keys)
			{
				languages.add(key);
				JSONObject lang = trans.getJSONObject(key);
				String value = lang.getString("translatedValue");
				t.trans.put(key, value);
			}
			t.trans.put("en", t.source);
			arr.add(t);
		}

		// write source
		JSONArray outEng = new JSONArray();
		for (TData t : arr)
		{
			JSONObject one = new JSONObject();
			one.put("key", t.key);
			one.put("value", t.source);
			if (t.fallback != null)
				one.put("internationalFallback", t.fallback);
			one.put("stage", t.stage);
			one.put("minAppVersion", t.minAppVersion);
			outEng.add(one);
		}

		JSONObject json = new JSONObject();
		json.put("strings", outEng);
		String content = json.write(true);
		Utilities.writeString(content, out_english);

		TreeMap<String,TreeMap<String,String>> allTranslations = new TreeMap<String,TreeMap<String,String>>();
		for (String lang : languages)
		{
			TreeMap<String,String> map = new TreeMap<String,String>();
			allTranslations.put(lang, map);
			for (TData t : arr)
			{
				String translation = t.trans.get(lang);
				if (translation != null)
					map.put(t.key, translation);
			}
		}

		for (String lang : languages)
		{
			File output = new File(outFolder, lang + ".json");
			PrintWriter writer = new PrintWriter(output, "UTF-8");
			TreeMap<String,String> map = allTranslations.get(lang);
			ConvertTranslations.printSmartling(writer, map, false);
			writer.close();

			output = new File(outFolder, lang + "_simple.json");
			writer = new PrintWriter(output, "UTF-8");
			writer.println("{\n\"strings\":");
			ConvertTranslations.printSimple(writer, map, false);
			writer.println("}");
			writer.close();
		}
	}

	void getTranslations(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("missing output folder");

		String sfolder = driver.composePath(instruction.parameters[0]);
		File folder = new File(sfolder);
		if (!folder.isDirectory())
			folder.mkdir();

		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(config.translationsUrl + "seasons/" + config.seasonId + "/strings?mode=INCLUDE_TRANSLATIONS", config.sessionToken);
		JSONObject json = driver.parseError(res.message, "translations");

		JSONArray arr = json.getJSONArray("supportedLanguages");
		Set<String> supported = new TreeSet<String>();
		for (int i = 0; i < arr.length(); ++i)
		{
			String locale = arr.getString(i);
			if (!locale.equals(Constants.DEFAULT_LANGUAGE))
				supported.add(locale);
		}

		JSONArray strings = json.getJSONArray("strings");
		TreeMap<String, TreeMap<String,String>> allTranslations = new TreeMap<String, TreeMap<String,String>>();
		TreeMap<String, TreeSet<String>> missing = new TreeMap<String, TreeSet<String>>();
		boolean inProduction = config.stage.equals("PRODUCTION");
		
		for (int i = 0; i < strings.length(); ++i)
		{
			JSONObject string = strings.getJSONObject(i);

			String stage = string.getString("stage");
			if (inProduction && !stage.equals("PRODUCTION"))
				continue;

			String key = string.getString("key");
			//String value = string.getString("value");
			JSONObject translations = string.getJSONObject("translations");

			@SuppressWarnings("unchecked")
			Set<String> locales = translations.keySet();
			for (String locale : locales) // does not include the default language
			{
				JSONObject translation = translations.getJSONObject(locale);
				String translatedValue = translation.getString("translatedValue");

				TreeMap<String,String> tt = allTranslations.get(locale);
				if (tt == null)
				{
					tt = new TreeMap<String,String>();
					allTranslations.put(locale, tt);
				}
				tt.put(key, translatedValue);
			}

			TreeSet<String> untranslated = getDiff(supported, locales);
			if (!untranslated.isEmpty())
				missing.put(key, untranslated);
		}

		boolean toUpper = false;
		for (String locale : allTranslations.keySet())
		{
			File outFile = new File(folder, locale + ".json");
			PrintWriter writer = new PrintWriter(outFile, "UTF-8");

			TreeMap<String,String> map = allTranslations.get(locale);
			ConvertTranslations.printSmartling(writer, map, toUpper);
			writer.close();
		}
		
		File outMissing = new File(folder, "MISSING.txt");
		PrintWriter writer = new PrintWriter(outMissing, "UTF-8");
		for (String key : missing.keySet())
		{
			writer.println("key: " + key + ", missing: " + missing.get(key));
		}
		writer.close();
	}
	TreeSet<String> getDiff(Set<String> supported, Set<String> translated)
	{
		TreeSet<String> out = new TreeSet<String>(supported);
		out.removeAll(translated);
		return out;
	}

	enum StrStatus { MARK, REVIEW, SEND }
	static final int chunkSize = 20;
	void setStringStatus(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 2)
			throw new Exception(instruction.action.toString() + " requires 2 parameters ( translate-ids-file; MARK | REVIEW | SEND )");

		String idsFile = driver.composePath(instruction.parameters[0]);
		StrStatus mode = StrStatus.valueOf(instruction.parameters[1]);
		if (mode == null)
			throw new Exception("invalid mode " + mode);

		String path =	(mode == StrStatus.MARK) ? "markfortranslation" :
			(mode == StrStatus.SEND) ? "sendtotranslation" : "completereview";

		String body = Utilities.readString(idsFile);
		JSONObject ids = new JSONObject(body);
		JSONArray array = ids.getJSONArray(Constants.JSON_FIELD_STRING_IDS);
		if (array.isEmpty())
			throw new Exception("no string ids in input");

		boolean legacy = (config.version.i < Version.v3_0.i);
		if (legacy)
		{
			for (int i = 0; i < array.length(); i += chunkSize)
				setStringStatusChunk(array, path, i, i + chunkSize);
		}
		else
		{
			String url = config.translationsUrl + "seasons/" + config.seasonId + "/" + path;
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(url, body, config.sessionToken);
			if (res.code != 200)
				driver.parseError(res.message, path);
		}
	}

	void setStringStatusChunk(JSONArray array, String path, int from, int to) throws Exception
	{
		if (to > array.length())
			to = array.length();

		StringBuilder b = new StringBuilder();
		for (int i = from; i < to; ++i)
		{
			String id = array.getString(i);
			String sep = (b.length() == 0) ? "?ids=" : "&ids=";
			b.append(sep);
			b.append(id);
		}
		String parms = b.toString();

		String url = config.translationsUrl + "seasons/" + config.seasonId + "/" + path + parms;
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(url, "", config.sessionToken);
		if (res.code != 200)
			driver.parseError(res.message, path);
	}
	void getStringByStatus(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 3)
			throw new Exception(instruction.action.toString() + " requires 3 parameters ( StringStatus(enum) ; full-output-path ; string-ids-path )");

		String type = instruction.parameters[0];
		String out_full = driver.composePath(instruction.parameters[1]);
		String out_ids = driver.composePath(instruction.parameters[2]);

		Constants.StringStatus status =  Constants.StringStatus.valueOf(type);
		if (status == null || status == Constants.StringStatus.NONE)
			throw new Exception("invalid status " + status);

		String url = config.translationsUrl + "seasons/" + config.seasonId + "/strings/" + status;
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(url, config.sessionToken);
		JSONObject json = driver.parseError(res.message, "stringStatus");

		String full = json.write(true);
		Utilities.writeString(full, out_full);
		
		JSONArray array = json.getJSONArray("strings");
		JSONArray ids = new JSONArray();
		for (int i = 0; i < array.length(); ++i)
		{
			JSONObject obj = array.getJSONObject(i);
			String id = obj.getString("uniqueId");
			ids.add(id);
		}

		JSONObject obj = new JSONObject();
		obj.put("stringIds", ids);
		String str = obj.write(true);
		Utilities.writeString(str, out_ids);
	}

	//----------------------------------------------------------------------------------------------------------
	// tests url
	enum LogType { trace, fine }
	void setTranslationLog(Instruction instruction) throws Exception
	{
		String usage = "usage: " + instruction.action.toString() + " ; [trace | fine] ; [true | false] ";
		if (instruction.parameters.length != 2)
			throw new Exception(usage);

		LogType type = LogType.valueOf(instruction.parameters[0]);
		Boolean on = Boolean.parseBoolean(instruction.parameters[1]);
		if (type == null || on == null)
			throw new Exception(usage);

		String url = config.testUrl + "translations?" + type.toString() + "=" + on.toString();
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(url, "", config.sessionToken);
		driver.parseError(res.message, "setTranslationLog");
	}
	void getTranslationLog(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception(instruction.action.toString() + " requires output log path");

		String out = driver.composePath(instruction.parameters[0]);

		String url = config.testUrl + "translations";
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(url, config.sessionToken);
		JSONObject json = driver.parseError(res.message, "getTranslationLog");
		String content = json.write(true);
		Utilities.writeString(content, out);
		System.out.println(content);
	}

	void wakeupTranslations(Instruction instruction) throws Exception
	{
		String url = config.testUrl + "translations/wakeup";
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(url, "", config.sessionToken);
		driver.parseError(res.message, "wakeupTranslations");
	}
}

/*
	void userRollout(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("user random number not provided");
		String str = instruction.parameters[0];
		int num;
		try {
			num = Integer.parseInt(str);
		}
		catch (Exception e) {
			throw new Exception("user random number '" + str + "'");
		}
		System.out.println("Rollout status for user random number " + num);
		System.out.println(Percentile.printRandomUserNumber(num));

		JSONObject features = getFeatures().getJSONObject(Constants.JSON_FIELD_ROOT);
		RolloutChecker checker = new RolloutChecker(num);
		checker.run(features, null);
	}
	class RolloutChecker extends Visit
	{
		int num;
		RolloutChecker(int userRandomNumber) {
			num = userRandomNumber;
		}

		protected Object visit(JSONObject obj, Object state) throws Exception
		{
			BaseAirlockItem.Type type = getNodeType(obj);
			if (type != Type.FEATURE && type != Type.CONFIGURATION_RULE)
				return null;

			String b64 = obj.optString(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP);
			double percent = obj.optDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE, 100.0);
			Percentile p = config.pfactory.create(b64);
			String status = p.isAccepted(num) ? "is ON:  " : "is OFF: ";
			System.out.println(status +  getName(obj) + ", percent " +  percent + "%, effective percent " + p.getEffectivePercentage() + "%");
			return null;
		}
	}

	void validatePercentage(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1 && instruction.parameters.length != 2)
			throw new Exception(instruction.action.toString() + " requires 1 or two parameters ( feature.json [ ; gold.json ] )");

		String path = composePath(instruction.parameters[0]);
		System.out.println("validating " + path);

		JSONObject json = Utilities.readJson(path).getJSONObject("root");
		PercentValidator validator = new PercentValidator();
		validator.run(json, null);
		System.out.println("validation finished");

		if (instruction.parameters.length == 2)
		{
			path = composePath(instruction.parameters[1]);
			System.out.println("comparing to gold " + path);

			JSONObject gold = Utilities.readJson(path);
			gold = gold.getJSONObject("root");

			Map<String,JSONObject> inputIndex = indexByName(json, false);
			CompareGold comparer = new CompareGold(inputIndex);
			comparer.run(gold, null);
			System.out.println("comparison finished");
		}
	}
	class PercentValidator extends Visit
	{
		protected Object visit(JSONObject obj, Object state) throws Exception
		{
			Percentile parent = (Percentile) state;

			BaseAirlockItem.Type type = getNodeType(obj);
			if (type != Type.FEATURE && type != Type.CONFIGURATION_RULE)
				return parent; // pass parent unchanged to lower nodes

			// features and configurations calculate new bitmap
			double percent = obj.optDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE, 100.0);
			String b64 = obj.optString(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP);

			Percentile old = config.pfactory.create(b64);
			double oldActualPercent = old.getEffectivePercentage();

			Percentile newCalc = (parent == null) ? config.pfactory.create(percent) : config.pfactory.create(parent, percent);
			double newActualPercent = newCalc.getEffectivePercentage();

			if (newActualPercent != oldActualPercent)
				throw new Exception("Invalid actual percentage in node " + getName(obj) + ". Should be " + newActualPercent + " but found " + oldActualPercent);

			if (parent != null)
			{
				int[] parentBits = parent.getContent(Range.PERCENT);
				int[] childBits = old.getContent(Range.PERCENT);

				TreeSet<Integer> parentSet = new TreeSet<Integer>();
				for (int n : parentBits)
					parentSet.add(n);

				for (int z : childBits)
				{
					if (!parentSet.contains(z))
						throw new Exception("Invalid percentage bits in node " + getName(obj) + ". Child bits are not a subset of the parent bits");
				}
			}
			return old; // pass current percentile as a parent to the lower nodes
		}
	}

	class CompareGold extends Visit
	{
		Map<String,JSONObject> map;
		CompareGold(Map<String,JSONObject> map) {
			this.map = map;
		}

		protected Object visit(JSONObject gold_child, Object state) throws Exception
		{
			BaseAirlockItem.Type type = getNodeType(gold_child);
			if (type == Type.FEATURE || type == Type.CONFIGURATION_RULE)
			{
				String name = getName(gold_child);
				JSONObject test_child = map.get(name);
	
				if (test_child == null)
					System.out.println("No matching node found for gold feature " + name);
				else
				{
					double gold_percent = gold_child.optDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE, 100.0);
					double test_percent = test_child.optDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE, 100.0);
	
					String gold_bitmap = gold_child.optString(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP);
					String test_bitmap = test_child.optString(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP);
	
					double gold_actual = config.pfactory.create(gold_bitmap).getEffectivePercentage();
					double test_actual = config.pfactory.create(test_bitmap).getEffectivePercentage();
	
					if (gold_percent != test_percent)
						System.out.println("Incompatible percents in feature " + name + ". Gold percent " + gold_percent + ", test percent " + test_percent);
	
					if (gold_actual != test_actual)
						System.out.println("Incompatible actual (internal) percents in feature " + name + ". Gold percent " + gold_actual + ", test percent " + test_actual);
				}
			}
			return null;
		}
	}
*/

