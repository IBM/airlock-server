package com.ibm.airlock.engine_dev;

import java.io.IOException;
import java.io.StringWriter;
import java.util.TreeMap;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.engine.Percentile.PercentileException;

public class SeasonMapper
{
	TreeMap<String,RuleEngine> season2RuleEngine = new TreeMap<String,RuleEngine>();
	TreeMap<String,String> season2RuleEngineJson = new TreeMap<String,String>();
	
	synchronized public RuleEngine getRuleEngine(String seasonID)
	{
		return season2RuleEngine.get(seasonID); // null if missing
	}
	synchronized public String getRuleEngineJson(String seasonID)
	{
		return season2RuleEngineJson.get(seasonID); // null if missing
	}

	synchronized public void addRuleEngine(String featureListJson, boolean javascriptRules) throws IOException, JSONException, EngineException, PercentileException
	{
		JSONObject obj = new JSONObject(featureListJson);
		String seasonID = obj.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
		RuleEngine.Flavour flavour = javascriptRules ? RuleEngine.Flavour.JAVASCRIPT_RULES : RuleEngine.Flavour.NATIVE_RULES;

		RuleEngine engine = loadEngineFromFeatureList(obj, flavour);

		StringWriter writer = new StringWriter();
		engine.toJSON(writer);
		String serialized = writer.toString();

		season2RuleEngine.put(seasonID, engine);
		season2RuleEngineJson.put(seasonID, serialized);
	}

	synchronized public boolean removeRuleEngine(String seasonID)
	{
		if (season2RuleEngine.containsKey(seasonID))
		{
			season2RuleEngine.remove(seasonID);
			season2RuleEngineJson.remove(seasonID);
			return true;
		}
		return false;
	}

	public static RuleEngine loadEngineFromFeatureList(JSONObject json, RuleEngine.Flavour flavour) throws JSONException, EngineException, PercentileException
	{
		String version = "1.0"; // TBD
		String season = json.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);

		JSONObject root = json.getJSONObject(Constants.JSON_FIELD_ROOT);
		JSONArray array = root.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);

		RuleEngine engine = new RuleEngine(season, flavour, version);
		RuleEngine.Group group = new RuleEngine.Group("Top Group", RuleEngine.Group.Type.REGULAR);

		doFeatureArray(group, array, flavour);
		engine.addGroup(group);
		return engine;
	}

	static void doFeatureArray(RuleEngine.Group group, JSONArray array, RuleEngine.Flavour flavour) throws JSONException, EngineException, PercentileException
	{
		for (int i = 0; i < array.length(); ++i)
		{
			JSONObject jobj = array.getJSONObject(i);
			RuleEngine.FeatureData rf = doFeature(jobj, flavour);
			group.addFeature(rf);
		}
	}
	static RuleEngine.FeatureData doFeature(JSONObject json, RuleEngine.Flavour flavour) throws JSONException, EngineException, PercentileException
	{
		String description = json.optString(Constants.JSON_FIELD_DESCRIPTION, "");
		JSONArray children = json.optJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES, null);
		String uniqueID = json.optString(Constants.JSON_FIELD_UNIQUE_ID, "unknown feature id");
		String featureName = json.optString(Constants.JSON_FIELD_NAME, "unknown feature name");
		String minVersion = json.optString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER, "");
		String bitmap = json.optString(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP, "");
		String trigger = getRuleString(json, "true"); // if no rule given, assume it always returns true

		RuleEngine.FeatureData rf = new RuleEngine.FeatureData(description, featureName, uniqueID, minVersion, bitmap, trigger, flavour);

		if (children != null && children.size() > 0)
		{
			String ftype = json.getString(Constants.JSON_FEATURE_FIELD_TYPE);
			RuleEngine.Group.Type gtype;

			switch (BaseAirlockItem.Type.valueOf(ftype))
			{
			case MUTUAL_EXCLUSION_GROUP:	gtype = RuleEngine.Group.Type.MUTUAL_EXCLUSION; break;
			default:						gtype = RuleEngine.Group.Type.REGULAR; break;
			}

			RuleEngine.Group group = new RuleEngine.Group("Group under " + featureName, gtype);
			doFeatureArray(group, children, flavour);
			rf.setSubgroup(group);
		}
		return rf;
	}
	static String getRuleString(JSONObject json, String dflt)
	{
		json = json.optJSONObject(Constants.JSON_FEATURE_FIELD_RULE);
		if (json == null)
			return dflt;
		return json.optString(Constants.JSON_RULE_FIELD_RULE_STR, dflt);
	}

}
