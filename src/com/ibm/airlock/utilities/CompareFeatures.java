package com.ibm.airlock.utilities;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.Utilities;

public class CompareFeatures
{
	static class FeatureInfo extends TreeMap<String,String>
	{
		private static final long serialVersionUID = 1L;
		public void print()
		{
			for (Map.Entry<String,String> e : this.entrySet())
				System.out.println("\t\t" + e.getKey() + " : " + e.getValue());
		}
	}
	static class Pair
	{
		FeatureInfo season1, season2;
		Pair(FeatureInfo s1, FeatureInfo s2) {
			season1 = s1; season2 = s2;
		}
	}

	public static void main(String[] args)
	{
		if (args.length != 2 && args.length != 3)
		{
			System.out.println("usage: CompareFeatures season1.json season2.json [ignore]");
			return;
		}

		try {
			boolean ignoreIds = (args.length == 3 && args[2].equals("ignore"));
			compare(args[0], args[1], ignoreIds);
			System.out.println("done");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void compare(String path1, String path2, boolean ignoreIds) throws Exception
	{
		System.out.println("season 1: " + path1);
		System.out.println("season 2: " + path2);
		if (ignoreIds)
			System.out.println("[Ignoring Ids and dates]");
		System.out.println();

		TreeSet<String> ingoreSet = new TreeSet<String>();
		if (ignoreIds)
		{
			ingoreSet.add("creationDate");
			ingoreSet.add("lastModified");
			ingoreSet.add("seasonId");
			ingoreSet.add("uniqueId");
		}

		TreeMap<String, FeatureInfo> season1 = processSeason(path1, ingoreSet);
		TreeMap<String, FeatureInfo> season2 = processSeason(path2, ingoreSet);

		TreeMap<String,Pair> pair = new TreeMap<String,Pair>();
		// add items found in s1
		for (String key : season1.keySet())
		{
			FeatureInfo s1 = season1.get(key);
			FeatureInfo s2 = season2.get(key); // s2 can be null
			pair.put(key, new Pair(s1,s2));
		}
		// add items not found in s1
		for (String key : season2.keySet())
		{
			FeatureInfo s1 = season1.get(key);
			FeatureInfo s2 = season2.get(key);
			if (s1 == null)
				pair.put(key, new Pair(null, s2));
		}
		for (Map.Entry<String, Pair> e : pair.entrySet())
		{
			System.out.println();
			System.out.println(e.getKey());
			printDiff(e.getValue());
		}
	}
	static TreeMap<String, FeatureInfo> processSeason(String path, TreeSet<String> ingoreSet) throws Exception
	{
		TreeMap<String, FeatureInfo> out = new TreeMap<String, FeatureInfo>();
		JSONObject features = Utilities.readJson(path);
		JSONObject root = features.getJSONObject("root");
		scanTree(root, "", ingoreSet, out);
		return out;
	}
	static void scanTree(JSONObject obj, String path, TreeSet<String> ingoreSet, Map<String,FeatureInfo> out) throws Exception
	{
		String str = obj.getString(Constants.JSON_FEATURE_FIELD_TYPE);
		BaseAirlockItem.Type type = BaseAirlockItem.Type.valueOf(str);

		String name;
		if (type == BaseAirlockItem.Type.ROOT)
			name = "root";
		else if (type == BaseAirlockItem.Type.CONFIG_MUTUAL_EXCLUSION_GROUP || type == BaseAirlockItem.Type.MUTUAL_EXCLUSION_GROUP)
			name = "mx";
		else
			name = obj.optString(Constants.JSON_FEATURE_FIELD_NAMESPACE, "?") + "." + obj.optString(Constants.JSON_FIELD_NAME, "?");

		path = path + "/" + name;

		JSONArray features = null;
		JSONArray configurationRules = null;
		FeatureInfo item = new FeatureInfo();

		@SuppressWarnings("unchecked")
		Set<String> keys = obj.keySet();
		for (String key : keys)
		{
			if (ingoreSet.contains(key))
				continue;

			if (key.equals("configurationRules"))
				configurationRules = obj.getJSONArray(key);
			else if (key.equals("features"))
				features = obj.getJSONArray(key);
			/*else if (key.equals("additionalInfo") || key.equals("internalUserGroups"))
			{
				JSONArray arr =  obj.getJSONArray(key);
				item.put(key, arr.toString());
			}*/
			else if (key.equals("rule"))
			{
				JSONObject one = obj.getJSONObject(key);
				boolean force = one.getBoolean("force");
				String ruleString = one.getString("ruleString");
				item.put("rule.force", force ? "true" : "false");
				item.put("rule.ruleString", ruleString);
			}
			else
			{
				Object one = obj.get(key);
				String val = (one == null) ? "" : one.toString();
				item.put(key, val);
			}
		}
		out.put(path, item);

		doChildren(features, path, ingoreSet, out);
		doChildren(configurationRules, path, ingoreSet, out);
	}

	static void doChildren(JSONArray array, String path, TreeSet<String> ingoreSet, Map<String,FeatureInfo> out) throws Exception
	{
		if (array != null)
			for (int i =0; i < array.length(); ++i)
			{
				JSONObject obj = array.getJSONObject(i);
				String newPath = path + "/[" + i + "]";
				scanTree(obj, newPath, ingoreSet, out);
			}
	}

	static void printDiff(Pair pair)
	{
		if (pair.season2 == null)
		{
			System.out.println("\t>>> season 1 >>>");
			pair.season1.print();
			return;
		}
		if (pair.season1 == null)
		{
			System.out.println("\t>>> season 2 >>>");
			pair.season2.print();
			return;
		}

		FeatureInfo d1 = getDiff(pair.season1, pair.season2);
		if (!d1.isEmpty())
		{
			System.out.println("\t>>> season 1 >>>");
			d1.print();
		}
		FeatureInfo d2 = getDiff(pair.season2, pair.season1);
		if (!d2.isEmpty())
		{
			System.out.println("\t>>> season 2 >>>");
			d2.print();
		}
	}
	static FeatureInfo getDiff(FeatureInfo s1, FeatureInfo s2)
	{
		FeatureInfo out = new FeatureInfo();

		for (String key : s1.keySet())
		{
			String val1 = s1.get(key);
			String val2 = s2.get(key);
			if (val2 == null || !val2.equals(val1))
				out.put(key, val1);
		}
		return out;
	}
}
