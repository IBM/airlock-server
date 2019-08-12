package com.ibm.airlock.engine_dev;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.engine.AirlockVersionComparator;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.Percentile;
import com.ibm.airlock.engine.Percentile.PercentileException;
import com.ibm.airlock.engine.RhinoScriptInvoker;
import com.ibm.airlock.engine.RhinoScriptInvokerV2;
import com.ibm.airlock.engine.ScriptInvoker;
import com.ibm.airlock.engine.ScriptInvoker.InvokerException;
import com.ibm.airlock.engine.VerifyRule;
import com.ibm.airlock.engine.Version;


// a stripped-down version of the rule engine to run on a mobile device
public class ClientEngine
{
	static final String RULE_OK = "";
	static final String RULE_MUTEX = "Mutex"; // internal use only

	static final int DefaultLegacyNumber = 50; // for testing
	public static final String LegacyRandomKey = "_LegacyRandomKey_";

	//---------------------------
	// helper classes
	static public class Result
	{
		boolean accept;
		String trace;
		JSONObject attributes;

		public Result(boolean accept, String trace)
		{
			this.accept = accept;
			this.trace = trace;
			attributes = new JSONObject();
		}
		public String toString() {
			return (accept ? "Status: ON" : "Status: OFF") + ", Attributes: " + attributes.toString() + ", Trace: " + trace;
		}
		public String shortString() {
			return (accept ? "Status: ON" : "Status: OFF" + ", Trace: " + trace );
		}
	}
	static public class Fallback
	{
		boolean accept;
		JSONObject attributes;

		public Fallback(boolean accept, JSONObject attributes)
		{
			this.accept = accept;
			this.attributes = attributes;
		}
	}

	static class ConfigResult
	{
		JSONObject attributes;
		ArrayList<String> appliedRules;
		String disabledBy = null;
	}

	static class AdditionalData
	{
		TreeSet<String> profileGroups;
		Map<String,Integer> featureRandomNumber;
		String productVersion;
		Version version;
		int legacyRandomNumber;
		boolean checkMinVersion;
		boolean printGold;
		boolean debugMode;
		Environment env = null;
		TreeMap<String,JSONArray> reorderedRules;
		TreeMap<String,TreeSet<String>> appliedOrderingRules;

		AdditionalData(List<String> profileGroups, String productVersion, Map<String,Integer> featureRandomNumber)
		{
			this.profileGroups = new TreeSet<String>(profileGroups);
			this.productVersion = productVersion;
			this.featureRandomNumber = (featureRandomNumber != null) ? featureRandomNumber : new TreeMap<String,Integer>();
			this.checkMinVersion = true;

			Integer legacy = this.featureRandomNumber.get(LegacyRandomKey);
			this.legacyRandomNumber = (legacy != null) ? legacy : DefaultLegacyNumber ;

			reorderedRules = new TreeMap<String, JSONArray>();
			appliedOrderingRules = new TreeMap<String,TreeSet<String>>();
		}
	}
	@SuppressWarnings("serial")
	static class ScriptError extends Exception
	{
		ScriptError(String str) {
			super(str);
		}
	}
	//---------------------------------------------------------------------
	// main class

	public static JSONObject calculateFeatures(JSONObject features, JSONObject userContext, List<String> profileGroups, 
			JSONObject fallback, String productVersion, Map<String,Integer> featureRandomNumber, Environment env)
					throws JSONException, InvokerException
	{
		AdditionalData additionalData = init(profileGroups, productVersion, featureRandomNumber, env);
		ScriptInvoker invoker = initInvoker(additionalData.version, userContext);
		return calculateFeatures(features, invoker, additionalData, fallback);
	}
	static AdditionalData init(List<String> profileGroups, String productVersion, Map<String,Integer> featureRandomNumber, Environment env)
	{
		AdditionalData additionalData = new AdditionalData(profileGroups, productVersion, featureRandomNumber);
		additionalData.env = env;
		additionalData.version = env.getVersion();

		String value = env.getProperty(Environment.PRINT_GOLD);
		additionalData.printGold = "true".equals(value); // null is OK

		value = env.getProperty(Environment.DEBUG_MODE);
		additionalData.debugMode = "true".equals(value);
		return additionalData;
	}
	static ScriptInvoker initInvoker(Version version, JSONObject userContext) throws JSONException, InvokerException
	{
		return (version.i < Version.v2_5.i) ? new RhinoScriptInvokerV2(userContext) : new RhinoScriptInvoker(userContext);
	}

	static JSONObject calculateFeatures(JSONObject features, ScriptInvoker invoker, AdditionalData additionalData, JSONObject fallback)
					throws JSONException, InvokerException
	{
		Map<String,Result> results = new TreeMap<String,Result>();
		JSONObject root = features.getJSONObject(Constants.JSON_FIELD_ROOT);

		Map<String,Fallback> fallbacks = (fallback == null) ? new TreeMap<String,Fallback>() : getFallbacks(fallback);
		doFeatureGroup(root, invoker, additionalData, -1, fallbacks, results);

		return embedResults(features, results, additionalData);
	}

	static int doFeatureGroup( JSONObject parent, ScriptInvoker invoker, AdditionalData additionalData, int mutexConstraint,
			Map<String, Fallback> fallback, Map<String, Result> out) throws JSONException
	 {
        if (parent == null)
            return 0;

        JSONArray array = getChildren(parent);
        if (array == null || array.length() == 0)
            return 0;

        int groupMutexCount = mutexCount(parent);
        boolean groupIsMutex = (groupMutexCount > 0);
        boolean hasReOrderingRules = hasReOrderingRule(parent);
        TreeMap<String, Result> featureCalculatedResults = null;

        if (hasReOrderingRules)
        {
        	// first we need to calculate all the rules of this level in their original order.
        	// this is because rule #1 may introduce a global variable used by rule #2
        	featureCalculatedResults = new TreeMap<String, Result>();

            for (int i = 0; i < array.length(); ++i)
            {
                JSONObject obj = array.getJSONObject(i);
                String featureName = extendedName(obj);
                Result result = doFeature(obj, invoker, additionalData, fallback);
                featureCalculatedResults.put(featureName, result);
            }

            try {
            	array = applyRuleBasedOrder(parent, invoker, additionalData);
            }
            catch (Exception e)
            {
            	 out.put(getId(parent), new Result(false, Strings.RuleReorderFailed + e.getMessage()));
            }

            // trace reordered rules here
         	JSONArray reorderTrace = new JSONArray();
        	for (int i = 0; i < array.size(); ++i) {
        		reorderTrace.add(extendedName(array.getJSONObject(i)));
        	}
        	additionalData.reorderedRules.put(extendedName(parent), reorderTrace); // trace reordered rules here
        }

        if (groupIsMutex) {
            // reconcile with parent's constraint. (use the child if parent constraint is not set, or is more lenient)
            if (mutexConstraint < 0 || mutexConstraint > groupMutexCount)
                mutexConstraint = groupMutexCount;
            // else keep the parent's constraint (may be zero if parent is exhausted)
        }
        else
            mutexConstraint = -1; // not set

        int successes = 0;
        for (int i = 0; i < array.length(); ++i)
        {
            JSONObject obj = array.getJSONObject(i);

            Result result;
            if (groupIsMutex && mutexConstraint <= 0)
            {
            	result = new Result(false, Strings.RuleSkipped);
            }
            else if (hasReOrderingRules)
            {
            	String featureName = extendedName(obj);
            	result = featureCalculatedResults.get(featureName);
            	assert(result != null);
            	//reorderTrace.add(featureName);
            }
            else
            	result = doFeature(obj, invoker, additionalData, fallback);

            if (result.accept == false)
            {
                propagateFail(obj, result, out); // pass false to all children
            }
            else
            {
                boolean childIsMutex = mutexCount(obj) > 0;
                if (!childIsMutex) // child is not a mutex, count it as success
                {
                    ++successes;
                    out.put(getId(obj), result);

                    if (groupIsMutex)
                        --mutexConstraint; // must be done here before descending into subtree
                }

                int sub_mutex_success = doFeatureGroup(obj, invoker, additionalData, mutexConstraint, fallback, out); // success: descend & check children
                if (childIsMutex)
                {
                    successes += sub_mutex_success;
                    if (groupIsMutex)
                        mutexConstraint -= sub_mutex_success;
                }
            }
        }

        // mutex nodes notify caller about number of successes (they are added to parent mutex success count)
        // non-mutex nodes notify 1 or zero.
        if (!groupIsMutex && successes > 1) {
            successes = 1;
        }
        return successes;
	 }

	static Result doFeature(JSONObject obj, ScriptInvoker invoker, AdditionalData additionalData, Map<String,Fallback> fallback) throws JSONException
	{
		if (mutexCount(obj) > 0)
			return new Result(true, RULE_MUTEX); // not added to output, but marked as successful to process its children

		Result res = checkPreconditions(obj, additionalData);
		if (!res.accept)
			return res; // precondition failure

		// a missing/empty rule returns true
		String trigger = getRuleString(obj);
		ScriptInvoker.Output out = invoker.evaluate(trigger);
		Fallback theFallback = getFallback(obj, fallback);

		switch (out.result)
		{
		case ERROR:
		{
			Result result = new Result(theFallback.accept, Strings.RuleErrorFallback + ". Error trace: " + out.error);
			if (theFallback.accept) // try to calculate configuration anyhow since rule is on by default
			{
				try {
					ConfigResult cr = calculateAttributes(obj, invoker, additionalData);
					result.attributes = cr.attributes;
					result.trace = result.trace + "\nConfigurations: " + cr.appliedRules.toString();
					disableFeatureFromConfiguration(result, cr); // some configurations will turn off the feature explicitly
				}
				catch (Exception e)
				{
					result = new Result(theFallback.accept, Strings.RuleAndConfigurationError + ". Error trace: " + out.error);
					result.attributes = theFallback.attributes;
				}
			}
			return result;
		}

		case TRUE:
		{
			Result result = new Result(true, RULE_OK);
			try {
				//String name = getId(obj);
				ConfigResult cr = calculateAttributes(obj, invoker, additionalData);
				result.attributes = cr.attributes;
				result.trace = "Configurations: " + cr.appliedRules.toString();
				disableFeatureFromConfiguration(result, cr); // some configurations will turn off the feature explicitly
			}
			catch (Exception e)
			{
				if (theFallback.accept == false)
				{
					result = new Result(false, Strings.RuleConfigurationTurnoff + ". Error trace: " + e.toString());
				}
				else
				{
					result.attributes = theFallback.attributes;
					result.trace = Strings.RuleConfigurationFallback + ". Error trace: " + e.toString();
				}
			}
			return result;
		}
		case FALSE:
		default:
			return new Result(false, Strings.RuleFail);
		}
	}

	static Result checkPreconditions(JSONObject obj, AdditionalData additionalData)
	{
		if (!isEnabled(obj))
			return new Result(false, Strings.RuleDisabled);

		if (checkPercentage(obj, additionalData) == false)
			return new Result(false, Strings.RulePercentage);

		if (additionalData.checkMinVersion)
		{
			// the minAppVersion of the feature should be <= to the current productVersion
			// minAppVersion is now mandatory, so fail if missing
			String minAppVersion = getMinAppVersion(obj);
			if (minAppVersion == null || AirlockVersionComparator.compare(minAppVersion, additionalData.productVersion) > 0)
				return new Result(false, Strings.RuleVersioned);
		}

		// for development users, check that the feature belongs to the right user group
		if (getStage(obj) == Stage.DEVELOPMENT)
		{
			TreeSet<String> featureGroups = getUserGroups(obj);
			featureGroups.retainAll(additionalData.profileGroups);
			if (featureGroups.size() == 0)
				return new Result(false, Strings.RuleUserGroup);
		}

		return new Result(true, RULE_OK); // all preconditions are met
	}

	// propagate a fail down to all children
	static void propagateFail(JSONObject obj, Result result, Map<String,Result> out) throws JSONException
	{
		if (mutexCount(obj) == 0)
			out.put(getId(obj), result);

		JSONArray children = getChildren(obj);
		if (children == null || children.length() == 0)
			return;

		for (int i = 0; i < children.length(); ++i)
		{
			Result parentFail = new Result(false, Strings.RuleParentFailed);
			propagateFail(children.getJSONObject(i), parentFail, out);
		}
	}

	///---------------------------

	static JSONArray applyRuleBasedOrder(JSONObject obj, ScriptInvoker invoker, AdditionalData additionalData) throws Exception
	{
        JSONArray array = getChildren(obj);
        if (array == null || array.length() == 0)
            return array;

        ConfigResult cr = calculateRuleBasedChildrenOrder(obj, invoker, additionalData);

        String name = extendedName(obj);
        TreeSet<String> rules = additionalData.appliedOrderingRules.get(name);
        if (rules == null)
        {
        	rules = new TreeSet<String>();
        	additionalData.appliedOrderingRules.put(name, rules);
        }
        rules.addAll(cr.appliedRules);

        JSONArray reordered = reorderChildren(array, cr.attributes);
        return reordered;
 //       obj.put(Constants.JSON_FEATURE_FIELD_FEATURES, reordered); // modifies the feature tree
    }
    static ConfigResult calculateRuleBasedChildrenOrder(JSONObject obj, ScriptInvoker invoker, AdditionalData additionalData) throws Exception
    {
		// start with defaults
		ConfigResult out = new ConfigResult();
		out.attributes = getDefaultConfiguration(obj);
		out.appliedRules = new ArrayList<String>();
		out.appliedRules.add(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG);

		// override defaults
		evaluateConfigurationArray(obj, invoker, additionalData, out, -1, Constants.JSON_FEATURE_FIELD_ORDERING_RULES, BaseAirlockItem.Type.ORDERING_RULE);
		return out;
	}

    static class Weighted implements Comparable<Weighted> // decorate each node with its weight
    {
    	JSONObject obj;
    	Double weight;
    	Weighted(JSONObject o, Double d) {
    		obj = o; weight = d;
    	}
		@Override
		public int compareTo(Weighted other) {
			return other.weight.compareTo(this.weight); // high scores first
		}
    };

    static Weighted findWeight(JSONObject obj, final JSONObject weights)
    {
    	String name = extendedName(obj);
    	double weight;

    	try {
			weight = Double.parseDouble(weights.optString(name));
		}
		catch (Exception e) // null pointer and NumberFormatException
    	{
			weight = 0;
		}

    	return new Weighted(obj, weight);
    }

    static JSONArray reorderChildren(JSONArray array, final JSONObject weights) throws Exception
    {
        List<Weighted> jsonValues = new ArrayList<Weighted>();
        for (int i = 0; i < array.length(); ++i)
        {
        	JSONObject obj = array.getJSONObject(i);
        	jsonValues.add(findWeight(obj, weights));
        }
        Collections.sort(jsonValues);
   
        JSONArray sortedJsonArray = new JSONArray();
        for (int i = 0; i < array.length(); ++i) {
            sortedJsonArray.put(jsonValues.get(i).obj);
        }
        return sortedJsonArray;
    }
	//---------------------------
	static String getRuleString(JSONObject json)
	{
		json = json.optJSONObject(Constants.JSON_FEATURE_FIELD_RULE);
		return  (json == null) ? null : json.optString(Constants.JSON_RULE_FIELD_RULE_STR);
	}
	static boolean isUncached(JSONObject json)
	{
		return json.optBoolean(Constants.JSON_FEATURE_FIELD_NO_CACHED_RES, false);
	}

	static BaseAirlockItem.Type getNodeType(JSONObject obj)
	{
		String str = obj.optString(Constants.JSON_FEATURE_FIELD_TYPE, BaseAirlockItem.Type.FEATURE.toString());
		return Utilities.valueOf(BaseAirlockItem.Type.class, str); // null on error
	}

	static int mutexCount(JSONObject obj)
	{
		BaseAirlockItem.Type type = getNodeType(obj);
		switch (type)
		{
		case MUTUAL_EXCLUSION_GROUP:
		case CONFIG_MUTUAL_EXCLUSION_GROUP:
		case ORDERING_RULE_MUTUAL_EXCLUSION_GROUP:
			return obj.optInt(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON, 1);

		default: return 0;
		}
	}

	public static String getId(JSONObject obj) // using namespace.name instead of id
	{
		String name = obj.optString(Constants.JSON_FIELD_NAME, "<unknown>");
		return getNamespace(obj) + "." + name;
	}
	static String getNamespace(JSONObject obj)
	{
		String namespace = obj.optString(Constants.JSON_FEATURE_FIELD_NAMESPACE);
		if (namespace != null)
			return namespace;
		namespace = obj.optString(Constants.JSON_FIELD_EXPERIMENT_NAME);
		if (namespace != null)
			return Constants.JSON_FIELD_EXPERIMENTS + "." + namespace;

		return obj.containsKey(Constants.JSON_FIELD_VARIANTS) ? Constants.JSON_FIELD_EXPERIMENTS :  "<unknown>";
	}
	static String getMutexId(JSONObject obj) // using mx.uniqueId
	{
		String name = obj.optString(Constants.JSON_FIELD_UNIQUE_ID, "<unknown>"); 
		return "mx." + name;
	}
    static String extendedName(JSONObject obj)
    {
    	return (mutexCount(obj) > 0) ? getMutexId(obj) : getId(obj);
    }
	static String getMinAppVersion(JSONObject obj)
	{
		String out = obj.optString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER); // features use this
		if (out == null)
			out = obj.optString(Constants.JSON_SEASON_FIELD_MIN_VER); // experiments use this
		return out;
	}
	static JSONArray getChildren(JSONObject obj) throws JSONException
	{
		return obj.optJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
	}
	static boolean checkPercentage(JSONObject obj, AdditionalData data)
	{
		if (data.version.i < Version.v2_5.i) // legacy bitmap
		{
			String b64 = obj.optString(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP, "").trim();
			if (b64.isEmpty())
				return false;

			try {
				Percentile percentile = new Percentile(b64);
				return percentile.isAccepted(data.legacyRandomNumber);
			}
			catch (Exception e) {
				return false;
			}
		}
		else // random feature map
		{
			double threshold = obj.optDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE, 100.0);
			if (threshold >= 100.)
				return true;
			if (threshold <= 0.)
				return false;

			Integer userFeatureRand = data.featureRandomNumber.get(getId(obj));
			if (userFeatureRand == null)
				return false; // return true?

			return userFeatureRand <= threshold * 10000;
		}
	}

	static boolean hasReOrderingRule(JSONObject obj) throws JSONException
	{
		JSONArray array = obj.optJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES);
		return (array != null && array.length() > 0);
	}
	static boolean isEnabled(JSONObject obj)
	{
		return obj.optBoolean(Constants.JSON_FEATURE_FIELD_ENABLED);
	}
	static Stage getStage(JSONObject obj)
	{
		String str = obj.optString(Constants.JSON_FEATURE_FIELD_STAGE, "");
		return Utilities.valueOf(Stage.class, str); // null on error
	}

	static Fallback getFallback(JSONObject obj, Map<String,Fallback> fallback) throws JSONException
	{
		Fallback out;
		boolean avoidCache = isUncached(obj);

		if (avoidCache == false)
		{
			out = fallback.get(getId(obj));
			if (out != null)
				return out;
			// if requested cached fallback does not exist, use default fallback
		}

		boolean accept = obj.optBoolean(Constants.JSON_FEATURE_FIELD_DEF_VAL, false);
		JSONObject attributes = getDefaultConfiguration(obj);
		return new Fallback(accept, attributes);
	}

	static TreeSet<String> getUserGroups(JSONObject obj)
	{
		JSONArray array;
		try {
			array = obj.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS);
		} catch (JSONException e) {
			array = new JSONArray();
		}
		
		TreeSet<String> out = new TreeSet<String>();
		for (int i = 0; i < array.length(); ++i)
		{
			String str = array.optString(i);
			if (str != null && !str.isEmpty())
				out.add(str);
		}
		return out;
	}

	static ConfigResult calculateAttributes(JSONObject obj, ScriptInvoker invoker, AdditionalData additionalData) throws ScriptError, JSONException
	{
		// start with defaults
		ConfigResult out = new ConfigResult();
		out.attributes = getDefaultConfiguration(obj);
		out.appliedRules = new ArrayList<String>();
		out.appliedRules.add(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG);

		// override defaults
		evaluateConfigurationArray(obj, invoker, additionalData, out, -1, Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES, BaseAirlockItem.Type.CONFIGURATION_RULE);
		return out;
	}

	static JSONObject getDefaultConfiguration(JSONObject obj) throws JSONException 
	{
		String config = obj.optString(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG, "");
		if (config.isEmpty())
			return new JSONObject();

		try {
			return new JSONObject(config);
		}
		catch (JSONException e) {
			throw new JSONException("The default configuration is not valid. Unlike output configurations in configuration rules, the default configuration must be a static JSON that does not contain context variables or JavaScript functions.\n" + config);
		}
	}

	static int evaluateConfigurationArray(JSONObject obj, ScriptInvoker invoker, AdditionalData additionalData, ConfigResult out, int mxCounter, String parentKey, BaseAirlockItem.Type childKey) throws ScriptError, JSONException
	{
		JSONArray array = obj.optJSONArray(parentKey);
		if (array == null || array.isEmpty())
			return 0;

		int groupMutexCount = mutexCount(obj);
		boolean groupIsMutex = (groupMutexCount > 0);

		if (groupIsMutex)
		{
			 // reconcile with parent's constraint. (use the child if parent constraint is not set, or is more lenient)
			if (mxCounter < 0 || mxCounter > groupMutexCount) 
				mxCounter = groupMutexCount;
			// else keep the parent's constraint (may be zero if parent is exhausted)
		}
		else
			mxCounter = -1; // not set

		int successes = 0;
		for (int i = 0; i < array.length(); ++i)
		{
			if (groupIsMutex && mxCounter <= 0)
				break;

			JSONObject child = array.getJSONObject(i);
			BaseAirlockItem.Type childType = getNodeType(child);

			boolean childIsMutex = true;
			//if (childType == BaseAirlockItem.Type.CONFIGURATION_RULE || childType == BaseAirlockItem.Type.ORDERING_RULE)
			if (childType == childKey)
			{
				childIsMutex = false;
				int childSuccess = evaluateConfigurationItem(child, invoker, additionalData, out); // 0 or 1
				if (childSuccess == 0)
					continue; // do not descend into children, skip to next brother

				successes += 1;
				if (groupIsMutex)
					mxCounter -= 1;
			}

			// descend into children if node is a mutex or a successful configuration
			int sub_mutex_success = evaluateConfigurationArray(child, invoker, additionalData, out, mxCounter, parentKey, childKey);
			if (childIsMutex)
			{
				successes += sub_mutex_success;
				if (groupIsMutex)
					mxCounter -= sub_mutex_success;
			}
		}

		if (!groupIsMutex && successes > 1) // none-mutex nodes return 1 or 0 successes
			successes = 1;
		return successes;
	}

	static int evaluateConfigurationItem(JSONObject obj, ScriptInvoker invoker, AdditionalData additionalData, ConfigResult out) throws ScriptError, JSONException
	{
		Result res = checkPreconditions(obj, additionalData);
		if (!res.accept)
			return 0; // precondition not met

		String attributesString = obj.optString(Constants.JSON_FEATURE_FIELD_CONFIGURATION, "");
		String trigger = getRuleString(obj);

		JSONObject attributes = evaluateConfigurationScript(invoker, trigger, attributesString);
		if (attributes == null)
			return 0; // configuration rule not met

		String configName = getId(obj);
		boolean enabled = attributes.optBoolean(Constants.ENABLE_FEATURE, true);
		if (!enabled)
			out.disabledBy = configName;

		Utilities.mergeJson(out.attributes, attributes);
		out.appliedRules.add(configName);
		return 1;
	}

	static JSONObject evaluateConfigurationScript(ScriptInvoker invoker, String trigger, String attributesString) throws ScriptError
	{
		try {
			// evaluate rule and configuration together. This allows the configuration to act on variables introduced in the rule
			return invoker.evaluateRuleAndConfiguration(trigger, attributesString); // returns null if not triggered
		}
		catch (Exception e)
		{
			throw new ScriptError("Error evaluating configuration.\nTrigger: " + trigger + "\nAttributes: " + attributesString + "\nError: " + e.toString());
		}
	}
	static void disableFeatureFromConfiguration(Result result, ConfigResult cr)
	{
		// enableFeature is TRUE by default.
		// configuration can explicitly set enableFeature to FALSE
		if (cr.disabledBy != null)
		{
			String message = Strings.RuleFeatureTurnoff + " (" + cr.disabledBy + ")";

			result.accept = false;
			result.attributes = new JSONObject();
			if (result.trace.isEmpty())
				result.trace = message;
			else
				result.trace = result.trace + "\n" + message;
		}
	}

	public static Map<String,Fallback> getFallbacks(JSONObject defaults) throws JSONException
	{
		Map<String,Fallback> out = new TreeMap<String,Fallback>();
		JSONObject root = defaults.getJSONObject(Constants.JSON_FIELD_ROOT);
		extractDefaults(root, out);
		return out;
	}
	static void extractDefaults(JSONObject obj, Map<String,Fallback> out) throws JSONException
	{
		 BaseAirlockItem.Type type = getNodeType(obj);
		 if (type == BaseAirlockItem.Type.FEATURE)
		 {
			boolean accept = obj.optBoolean(Constants.JSON_FEATURE_FIELD_DEF_VAL, false);
			JSONObject attributes = getDefaultConfiguration(obj);
			String id = getId(obj);
			out.put(id, new Fallback(accept, attributes));
		 }
		 JSONArray children = getChildren(obj);
		 if (children != null)
			 for (int i = 0; i < children.length(); ++i)
				 extractDefaults(children.getJSONObject(i), out);
	}
	//---------------------------------------------------------------------
	public static JSONObject embedResults(JSONObject features, Map<String,Result> resultMap, AdditionalData additionalData) throws JSONException
	{
		JSONObject root = features.getJSONObject(Constants.JSON_FIELD_ROOT);
		JSONArray childResults = embedChildren(root, resultMap, additionalData);

		JSONObject featuresOut = new JSONObject();
		featuresOut.put(Constants.JSON_FEATURE_FIELD_FEATURES, childResults);

		JSONObject out = new JSONObject();
		out.put(Constants.JSON_FIELD_ROOT, featuresOut);

		// get analytics and experiments from the merged feature tree
		copyOptional(features, out, Constants.JSON_FIELD_EXPERIMENT_LIST);
		copyOptional(features, out, Constants.JSON_FIELD_EXPERIMENT);
		copyOptional(features, out, Constants.JSON_FIELD_VARIANT);
		copyOptional(features, out, Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS);
		return out;
	}
	static void copyOptional(JSONObject in, JSONObject out, String key) throws JSONException
	{
		Object obj = in.opt(key);
		if (obj != null)
			out.put(key, obj);
	}
	static JSONArray embedChildren(JSONObject obj, Map<String,Result> resultMap, AdditionalData additionalData) throws JSONException
	{
		JSONArray out = new JSONArray();
		JSONArray children = getChildren(obj);
		if (children != null && hasReOrderingRule(obj))
		{
			String parentName = extendedName(obj);
			JSONArray reordered = additionalData.reorderedRules.get(parentName);
			if (reordered != null) // some reordering rules were not reached, skip them
			{
				TreeMap<String, JSONObject> map = new TreeMap<String, JSONObject>();
				for (int i = 0; i < children.size(); ++i)
				{
					JSONObject child = children.getJSONObject(i);
					map.put(extendedName(child), child);
				}
				children.clear();
				for (int i = 0; i < reordered.size(); ++i)
				{
					String name = reordered.getString(i);
					JSONObject child = map.get(name);
					if (child == null)
						throw new JSONException("missing reordered data for " + name);
					children.add(child);
				}
			}

		}
		if (children != null)
		{
			for (int i = 0; i < children.length(); ++i)
			{
				JSONObject node = embedOneChild(children.getJSONObject(i), resultMap, additionalData);
				out.add(node);
			}
		}
		return out;
	}
	static JSONObject embedOneChild(JSONObject obj, Map<String,Result> resultMap, AdditionalData additionalData) throws JSONException
	{
		String type = obj.optString(Constants.JSON_FEATURE_FIELD_TYPE, BaseAirlockItem.Type.FEATURE.toString());
		JSONArray children = embedChildren(obj, resultMap, additionalData);
		String id = extendedName(obj);

		JSONObject node = new JSONObject();
		node.put(Constants.JSON_FEATURE_FIELD_TYPE, type);
		node.put(Constants.JSON_FEATURE_FIELD_FEATURES, children);

		boolean sendIt = obj.containsKey(Constants.JSON_FIELD_SEND_TO_ANALYTICS);

//		if (sendIt) // always adding this
//		{
			JSONArray reordered = additionalData.reorderedRules.get(id);
			if (reordered != null)
			{
				node.put(Constants.JSON_FIELD_REORDERED_CHILDREN, reordered); // appears on both features and mx nodes
				// node.put("featureIsReported", true); // temporary
			}
//		}

		int mutCount = mutexCount(obj);

		if (mutCount > 0)
		{
			node.put(Constants.JSON_FEATURE_FIELD_MAX_FEATURES_ON, mutCount);
			// in "gold" mode remove the mx UUID for easier comparisons
			node.put(Constants.JSON_FIELD_NAME, additionalData.printGold ? "mx" : id);
		}
		else
		{
			Result result = resultMap.get(id);
			assert (result != null); // shouldn't happen since we created the result map from the same feature tree

			node.put(Constants.JSON_FIELD_NAME, id); // this includes the namespace (namespace.name)
			node.put("isON", result.accept);
			node.put("resultTrace", result.trace);
			if (additionalData.debugMode)
				node.put("featureAttributes", result.attributes); // for a more readable output
			else
				node.put("featureAttributes", result.attributes.toString()); // Rachel's parser expects a string instead of JSON

			// print analytics data for accepted attributes
			if (additionalData.version.i >= Version.v2_5.i) // && result.accept)  // requested change: putting out this field even when the rule fails
			{
				if (sendIt)
				{
					Boolean val = obj.getBoolean(Constants.JSON_FIELD_SEND_TO_ANALYTICS);
					node.put("featureIsReported", val); // if user selected it for analytics return true even if the trigger is false
				}

				// you can report on attributes even if you don't report their feature
				{
					JSONObject names = getReportedConfigurationNames(result.trace, obj);
					node.put("reportedConfigurationNames", names);

					JSONObject values = getReportedConfigurationValues(result.attributes, obj);
					node.put("reportedConfigurationValues", values); 

					TreeSet<String> appliedReorderingRules = additionalData.appliedOrderingRules.get(id);
					if (appliedReorderingRules != null)
					{
						node.put("appliedReorderingRules", new JSONArray(appliedReorderingRules));
						JSONObject reported = getReportedOrderingNames(appliedReorderingRules, obj);
						node.put("reportedOrderingNames", reported);
					}
				}
			}
		}

		return node;
	}

	static JSONObject getReportedConfigurationNames(String trace, JSONObject json) throws JSONException
	{
		String header = "Configurations: [";
		String[] items;
		int pos1 = trace.indexOf(header);
		if (pos1 >= 0) pos1 += header.length();
		int pos2 = trace.indexOf("]");
		if (pos1 < 0 || pos2 <= pos1)
		{
			items = new String[0];
		}
		else
		{
			String traceList = trace.substring(pos1, pos2);
			items = traceList.split("\\s*,\\s*");
		}
		Set<String> tracedNames = new TreeSet<String>(Arrays.asList(items));

		Set<String> whiteListed = new TreeSet<String>();
		getConfigNames(json, whiteListed);

		JSONObject out = new JSONObject();
		for (String configName : whiteListed)
		{
			// mark configuration names which are white-listed and contributed to the final attributes as TRUE
			boolean sent = tracedNames.contains(configName);
			out.put(configName, sent);
		}
		//out.put(Constants.JSON_FEATURE_FIELD_DEFAULT_CONFIG, true); // default configuration is always accepted
		return out;
	}

	static JSONObject getReportedOrderingNames(Set<String> orderedNames, JSONObject json) throws JSONException
	{
		Set<String> whiteListed = new TreeSet<String>();
		getOrderingNames(json, whiteListed);

		JSONObject out = new JSONObject();
		for (String configName : whiteListed)
		{
			// mark configuration names which are white-listed and contributed to the final attributes as TRUE
			boolean sent = orderedNames.contains(configName);
			out.put(configName, sent);
		}
		return out;
	}
	// gather white-listed configuration names
	static void getConfigNames(JSONObject obj, Set<String> configs) throws JSONException
	{
		if (getNodeType(obj) == BaseAirlockItem.Type.CONFIGURATION_RULE)
		{
			boolean send = obj.optBoolean(Constants.JSON_FIELD_SEND_TO_ANALYTICS, false);
			if (send)
				configs.add(getId(obj));
		}

		JSONArray array = obj.optJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
		if (array != null)
			for (int i = 0; i < array.size(); ++i)
				getConfigNames(array.getJSONObject(i), configs);
	}
	// gather white-listed ordering names
	static void getOrderingNames(JSONObject obj, Set<String> configs) throws JSONException
	{
		if (getNodeType(obj) == BaseAirlockItem.Type.ORDERING_RULE)
		{
			boolean send = obj.optBoolean(Constants.JSON_FIELD_SEND_TO_ANALYTICS, false);
			if (send)
				configs.add(getId(obj));
		}

		JSONArray array = obj.optJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES);
		if (array != null)
			for (int i = 0; i < array.size(); ++i)
				getOrderingNames(array.getJSONObject(i), configs);
	}
	static JSONObject getReportedConfigurationValues(JSONObject attributes, JSONObject obj) throws JSONException
	{
		JSONObject out = new JSONObject();

		JSONArray whitelist = obj.optJSONArray(Constants.JSON_FIELD_ATTRIBUTES_FOR_ANALYTICS);
		if (whitelist == null)
			return out;

		TreeMap<String,String> flat = new TreeMap<String,String>();
		flatten(attributes, null, flat);

		for (int i = 0; i < whitelist.size(); ++i)
		{
			String key = whitelist.getString(i);  // the white-list key is flattened
			String value = flat.get(key);
			if (value != null)
				out.put(key, value);
		}
		return out;
	}
	public static void flatten(Object obj, String path, Map<String,String> out) throws JSONException
	{
		if (obj instanceof JSONObject)
		{
			String prefix = (path == null) ? "" : path + ".";
			JSONObject attr = (JSONObject) obj;
			@SuppressWarnings("unchecked")
			Set<String> keys = attr.keySet();
			for (String str : keys)
			{
				String full = prefix + str;
				flatten(attr.get(str), full, out);
			}
		}
		else if (obj instanceof JSONArray)
		{
			String prefix = (path == null) ? "[" : path + "[";
			JSONArray arr = (JSONArray) obj;
			for (int i = 0; i < arr.size(); ++i)
			{
				String full = prefix + i + "]";
				flatten(arr.get(i), full, out);
			}
		}
		else
			out.put(path, obj.toString());
	}
	//-------------------------------------------------------------------
	// generate a new feature-random-map, based on a previous feature-random-map and the legacy random number
	public static Map<String,Integer> calculateFeatureRandomNumbers(JSONObject features, int legacyUserRandomNumber,
			Map<String,Integer> inputfeatureRandomMap) throws JSONException
	{
		Map<String,Integer> out = new TreeMap<String,Integer>();
		JSONObject root = features.getJSONObject(Constants.JSON_FIELD_ROOT);
		inspectPercentage(root, inputfeatureRandomMap, out, legacyUserRandomNumber);

		JSONObject experiments = features.optJSONObject(Constants.JSON_FIELD_EXPERIMENTS);
		if (experiments != null)
		{
			JSONArray array = experiments.optJSONArray(Constants.JSON_FIELD_EXPERIMENTS);
			if (array != null)
				for (int i = 0; i < array.size(); ++i)
					inspectPercentage(array.getJSONObject(i), inputfeatureRandomMap, out, legacyUserRandomNumber);
		}

		JSONArray branches = features.optJSONArray(Constants.JSON_FIELD_BRANCHES);
		if (branches != null)
			for (int i = 0; i < branches.size(); ++i)
				addBranchFeatures(branches.getJSONObject(i), inputfeatureRandomMap, out); // has no legacy

		return out;
	}

	static void addBranchFeatures(JSONObject branch, Map<String,Integer> in, Map<String,Integer> out) throws JSONException
	{
		JSONArray features = branch.getJSONArray("features");
		for (int i = 0; i < features.size(); ++i)
		{
			JSONObject feature = features.getJSONObject(i);
			String branchStatus = feature.getString("branchStatus");
			if (branchStatus.equals("NEW"))
			{
				String name = getId(feature);
				Integer previousRandom = in.get(name);

				if (previousRandom != null)
					out.put(name, previousRandom);
				else if (!out.containsKey(name))
					out.put(name, anyRandom());
			}
			// check its children too
			addBranchFeatures(feature, in, out);
		}
	}
	static void inspectPercentage(JSONObject obj, Map<String,Integer> in, Map<String,Integer> out, int legacyUserRandomNumber) throws JSONException
	{
		BaseAirlockItem.Type type = getNodeType(obj);
		if (type == BaseAirlockItem.Type.FEATURE || type == BaseAirlockItem.Type.CONFIGURATION_RULE || type == BaseAirlockItem.Type.ORDERING_RULE)
		{
			String name = getId(obj);
			Integer previousRandom = in.get(name);
			if (previousRandom != null)
			{
				out.put(name, previousRandom);
			}
			else
			{
				double threshold = obj.optDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE, 100.0);
				String b64 = obj.optString(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP, "").trim();
				// 0 or 100 percentages will not use the bitmap
				if (threshold <= 0 || threshold >= 100 || b64.isEmpty())
				{
					// allocate random [1 to 1 million inclusive]
					out.put(name, anyRandom());
				}
				else // legacy percentage, fake a random to capture previous behavior
				{
					boolean isOn;
					double bitmapPercentage;
					try {
						Percentile percentile = new Percentile(b64);
						isOn = percentile.isAccepted(legacyUserRandomNumber);
						bitmapPercentage = percentile.getEffectivePercentage();
					}
					catch (Exception e)
					{
						//throw new JSONException("invalid rollout bitmap for item " + name); // throw this?
						isOn = false;
						bitmapPercentage = threshold;
					}

					// sometimes the bitmap percentage is different than the given percentage.
					// there may be a valid reason (inheritance of bitmaps), or the console may
					// have changed the given percentage after upgrading to 2.5. The second case is more
					// problematic, so we prefer to accommodate it by ignoring the bitmap in some cases.

					if (isOn && threshold < bitmapPercentage) // the percentage has decreased for a tested user
						out.put(name, anyRandom());
					else if (!isOn && threshold > bitmapPercentage) // the percentage has increased for an untested user
						out.put(name, anyRandom());
					else
						out.put(name, constrainedRandom(threshold, isOn));
				}
			}
		}

		JSONArray array = obj.optJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
		if (array != null)
		{
			for (int i = 0; i < array.length(); ++i)
			{
				inspectPercentage(array.getJSONObject(i), in, out, legacyUserRandomNumber);
			}
		}
		array = obj.optJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
		if (array != null)
		{
			for (int i = 0; i < array.length(); ++i)
			{
				inspectPercentage(array.getJSONObject(i), in, out, legacyUserRandomNumber);
			}
		}
		array = obj.optJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES);
		if (array != null)
		{
			for (int i = 0; i < array.length(); ++i)
			{
				inspectPercentage(array.getJSONObject(i), in, out, legacyUserRandomNumber);
			}
		}
		array = obj.optJSONArray(Constants.JSON_FIELD_VARIANTS);
		if (array != null)
		{
			for (int i = 0; i < array.length(); ++i)
			{
				inspectPercentage(array.getJSONObject(i), in, out, legacyUserRandomNumber);
			}
		}
	}
	static int anyRandom() // allocate any random [1 to 1 million inclusive]
	{
		return new Random().nextInt(1000000) + 1;
	}
	static int constrainedRandom(double threshold, boolean isOn) // allocate a random that will simulate the legacy threshold
	{
		int splitPoint = (int) Math.floor(threshold * 10000); // 1 through 999999 inclusive
		if (splitPoint < 1) splitPoint = 1;
		else if (splitPoint > 999999) splitPoint = 999999; // just in case

		if (isOn) // select a user random number smaller than the split point
			return new Random().nextInt(splitPoint) + 1;
		else// select a user random number bigger than the split point

			return new Random().nextInt(1000000 - splitPoint) + splitPoint + 1;
	}
	
	static boolean rejectNotification(long minInterval, long maxNotifications, List<Long> firedHistory)
	{
        return minInterval > 0 && maxNotifications > -1 && notificationsShownForInterval(minInterval, firedHistory) >= maxNotifications;
        	 
	}
	static int notificationsShownForInterval(long minInterval, List<Long> firedHistory)
	{
		long currentTime = System.currentTimeMillis()/1000;
		int counter = 0;
		for (int index = firedHistory.size()-1 ; index >= 0; --index)
		{
			if (firedHistory.get(index) > (currentTime - minInterval))
			counter++;
		}
		return counter;
	}
	//-------------------------------------------------------------------

	public static void main(String[] args) throws PercentileException
	{
//		Percentile p = new Percentile("BgICBAERACJQ2FABBA==");
		Percentile p = new Percentile("AAABBAAAQANoAABAAg==");
		System.out.println(p.printBits());

		
		if (args.length != 8)
		{
			System.out.println("usage: ClientEngine features groups context functions translations minAppVer randUser fallbacks");
			return;
		}
		try {

			JSONObject features = Utilities.readJson(args[0]);
			JSONObject groups = Utilities.readJson(args[1]);

			String context = Utilities.readString(args[2]);
			String functions = Utilities.readString(args[3]);
			String translations = Utilities.readString(args[4]);

			String minAppVer = args[5];
			JSONObject thresholds = Utilities.readJson(args[6]);
			Map<String,Integer> userRandomNumber = Json2Thresholds(thresholds);
//			int userRandomNumber = Integer.parseInt(args[6]); 

			List<String> profileGroups = Json2List(groups);
			JSONObject contextObject = VerifyRule.createContextObject(context, functions, translations);

			//Map<String,Fallback> fallbacks = new TreeMap<String,Fallback>();
			JSONObject defaults = Utilities.readJson(args[7]);
			//fallbacks = getFallbacks(defaults);


			//interferes with generation of gold files
			//long startTime = System.currentTimeMillis();
			//...
			//long endTime = System.currentTimeMillis();
			//System.out.println("calculateFeatures took " + (endTime - startTime) + " milliseconds");

			Environment env = new Environment();
			env.setServerVersion("3");
			env.put(Environment.DEBUG_MODE, "true");
			JSONObject results = calculateFeatures(features, contextObject, profileGroups, defaults, minAppVer, userRandomNumber, env);

			String formatted = results.write(true);
			System.out.println(formatted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	static List<String> Json2List(JSONObject groups) throws JSONException
	{
		ArrayList<String> out = new ArrayList<String>();
		JSONArray array = groups.getJSONArray("groups");
		for (int i = 0; i < array.length(); ++i)
			out.add(array.getString(i));

		return out;
	}

	public static Map<String,Integer> Json2Thresholds(JSONObject json) throws JSONException
	{
		TreeMap<String,Integer> out = new TreeMap<String,Integer>();
		@SuppressWarnings("unchecked")
		Set<String> keys = json.keySet();
		for (String key : keys)
		{
			int value = json.getInt(key);
			out.put(key, value);
		}
		return out;
	}
	public static JSONObject thresholds2Json(Map<String,Integer> map) throws JSONException
	{
		JSONObject out = new JSONObject();
		for (Map.Entry<String,Integer> e : map.entrySet())
		{
			out.put(e.getKey(), e.getValue());
		}
		return out;
	}
}
