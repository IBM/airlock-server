package com.ibm.airlock.engine_dev;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.ScriptInvoker;
import com.ibm.airlock.engine.ScriptInvoker.InvokerException;
import com.ibm.airlock.engine_dev.RuntimeMergeBranch.MergeException;

public class Experiments extends ClientEngine
{
	static final String MASTER_DEFAULT = "Default";

	public static class BranchInfo
	{
		String experiment, branch, variant;
		public BranchInfo(String e, String b, String v)
		{
			experiment = e; branch = b; variant = v; // null branch means "use the master as-is"
		}
		public String getExperiment() { return experiment; }
		public String getBranch() { return branch; }
		public String getVariant() { return variant; }

		public String toString()
		{
			return "Experiment: " + experiment + ", variant: " + variant + ", branch: " + branch;
		}
	}

	public static List<BranchInfo> calculateExperiments(JSONObject json, JSONObject userContext, List<String> profileGroups, 
			String productVersion, Map<String,Integer> featureRandomNumber, Environment env)
					throws JSONException, InvokerException
	{
		AdditionalData additionalData = init(profileGroups, productVersion, featureRandomNumber, env);
		ScriptInvoker invoker = initInvoker(additionalData.version, userContext);
		return findExperiments(json, invoker, additionalData);
	}

	public static JSONObject calculateExperimentsAndFeatures(JSONObject json, JSONObject userContext, List<String> profileGroups, 
			JSONObject fallback, String productVersion, Map<String,Integer> featureRandomNumber, Environment env)
					throws JSONException, InvokerException, MergeException
	{
		AdditionalData additionalData = init(profileGroups, productVersion, featureRandomNumber, env);
		ScriptInvoker invoker = initInvoker(additionalData.version, userContext);

		List<BranchInfo> branchInfo = findExperiments(json, invoker, additionalData);

		JSONObject runtimeTree = calculateRuntimeTree(json, branchInfo);

		return calculateFeatures(runtimeTree, invoker, additionalData, fallback);
	}
	public static JSONObject calculateOneBranch(String branchName, JSONObject json, JSONObject userContext, List<String> profileGroups, 
			JSONObject fallback, String productVersion, Map<String,Integer> featureRandomNumber, Environment env)
					throws JSONException, InvokerException, MergeException
	{
		AdditionalData additionalData = init(profileGroups, productVersion, featureRandomNumber, env);
		ScriptInvoker invoker = initInvoker(additionalData.version, userContext);

		List<BranchInfo> branchInfo = new ArrayList<BranchInfo>();
		branchInfo.add(new BranchInfo("<BranchTest>", branchName, branchName));

		JSONObject runtimeTree = calculateRuntimeTree(json, branchInfo);
		return calculateFeatures(runtimeTree, invoker, additionalData, fallback);
	}
	static List<BranchInfo> findExperiments(JSONObject parent, ScriptInvoker invoker, AdditionalData additionalData) throws JSONException
	{
		ArrayList<BranchInfo> out = new ArrayList<BranchInfo>();
		JSONObject experimentRoot = parent.getJSONObject(Constants.JSON_FIELD_EXPERIMENTS);
		JSONArray experiments = experimentRoot.getJSONArray(Constants.JSON_FIELD_EXPERIMENTS);
		int maxExperiments = experimentRoot.getInt(Constants.JSON_FIELD_MAX_EXPERIMENTS_ON);
		TreeMap<String,Fallback> fallback = new TreeMap<String,Fallback>(); // Experiment rules do not have fallbacks

		for (int i = 0; i < experiments.size(); ++i)
		{
			JSONObject experiment = experiments.getJSONObject(i);
			String experimentName = experiment.getString(Constants.JSON_FIELD_NAME);
			Result result = doFeature(experiment, invoker, additionalData, fallback);

			if (additionalData.env.isTraced())
				additionalData.env.addTrace(experimentName + ": " + result.shortString());

			if (result.accept == false)
				continue;

			String variantName = null; //MASTER_DEFAULT; // default when all variants fail
			String branchName = null;  // default when all variants fail

			additionalData.checkMinVersion = false; // variants do not check minAppVersion

			JSONArray variants = experiment.getJSONArray(Constants.JSON_FIELD_VARIANTS);
			for (int j = 0; j < variants.size(); ++j)
			{
				JSONObject variant = variants.getJSONObject(j);
				variantName = variant.getString(Constants.JSON_FIELD_NAME);

				Result result2 = doFeature(variant, invoker, additionalData, fallback);
				if (additionalData.env.isTraced())
					additionalData.env.addTrace(experimentName + "." + variantName + ": " + result2.shortString());

				if (result2.accept)
				{
					branchName = variant.getString(Constants.JSON_FIELD_BRANCH_NAME);
					break;
				}
			}
			additionalData.checkMinVersion = true;

			// If the experiment rule succeeded but all variant rules failed, 
			// (or if a successful variant wants to use the master)
			// put in a null branch, meaning the experiment should use the master as-is
			//if (variantName.equals(MASTER_DEFAULT))
			//	branchName = null;

			if (branchName == null)
				continue;

			out.add(new BranchInfo(experimentName, branchName, variantName));
			break;
			//if (out.size() >= maxExperiments)
			//	break;
		}
		return out;
	}

	static public JSONObject calculateRuntimeTree(JSONObject masterAndBranches, List<BranchInfo> branchInfo) throws JSONException, MergeException
	{
		if (branchInfo.isEmpty())
		{
			// clone just the top layer, return the rest as-is
			JSONObject shallowClone = (JSONObject) Utilities.cloneJson(masterAndBranches, false);
			shallowClone.put(Constants.JSON_FIELD_EXPERIMENT, "<none>");
			shallowClone.put(Constants.JSON_FIELD_VARIANT, MASTER_DEFAULT);
			return shallowClone;
		}

		JSONObject master = masterAndBranches.getJSONObject(Constants.JSON_FIELD_ROOT);
		JSONObject clonedMaster = (JSONObject) Utilities.cloneJson(master, true);
		Map<String,JSONObject> nameMap = RuntimeMergeBranch.getItemMap(clonedMaster, false);

		// the master's analytics are already merged into the experiments, just get the data from the experiments
		TreeSet<String> barInfo = new TreeSet<String>();
		TreeSet<String> inputAnalytics = new TreeSet<String>();
		TreeSet<String> nameAnalytics = new TreeSet<String>();
		Map<String,Set<String>> attrAnalytics = new HashMap<String,Set<String>>();

		for (BranchInfo info : branchInfo)
		{
			barInfo.add("EXPERIMENT_" + info.experiment);
			barInfo.add("VARIANT_" + info.experiment + "_" + info.variant);

			if (info.branch != null) // otherwise, use the master as-is
			{
				JSONObject branch = findBranch(masterAndBranches, info.branch);
				clonedMaster = RuntimeMergeBranch.merge(clonedMaster, nameMap, branch);
			}

			// the experiment data is used even running with the default master
			JSONObject experiment = findExperiment(masterAndBranches, info.experiment);
			addAnalytics(experiment, Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS, inputAnalytics);
			addAnalytics(experiment, Constants.JSON_FIELD_FEATURES_CONFIGS_FOR_ANALYTICS, nameAnalytics);
			addAttrAnalytics(experiment, attrAnalytics);
		}

		JSONObject out = new JSONObject();
		out.put(Constants.JSON_FIELD_ROOT, clonedMaster);

		out.put(Constants.JSON_FIELD_EXPERIMENT_LIST, new JSONArray(barInfo)); // for BAR reports
		BranchInfo info = branchInfo.get(0); // additional BAR requirement - report the first experiment found
		out.put(Constants.JSON_FIELD_EXPERIMENT, info.experiment);
		out.put(Constants.JSON_FIELD_VARIANT, info.variant);

		out.put(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS, new JSONArray(inputAnalytics));
		copyAnalytics(nameMap, nameAnalytics);
		copyAnalytics(nameMap, attrAnalytics);
		return out;
	}

	static JSONObject findBranch(JSONObject json, String branchName) throws JSONException
	{
		JSONArray branches = json.getJSONArray(Constants.JSON_FIELD_BRANCHES);
		for (int i = 0; i < branches.size(); ++i)
		{
			JSONObject branch = branches.getJSONObject(i);
			String name = branch.getString(Constants.JSON_FIELD_NAME);
			if (name.equals(branchName))
				return branch;
		}
		throw new JSONException("missing branch name " + branchName);
	}
	static JSONObject findExperiment(JSONObject masterAndBranches, String experimentName) throws JSONException
	{
		JSONObject experimentRoot = masterAndBranches.getJSONObject(Constants.JSON_FIELD_EXPERIMENTS);
		JSONArray experiments = experimentRoot.getJSONArray(Constants.JSON_FIELD_EXPERIMENTS);
		for (int i = 0; i < experiments.size(); ++i)
		{
			JSONObject experiment = experiments.getJSONObject(i);
			String name = experiment.getString(Constants.JSON_FIELD_NAME);
			if (name.equals(experimentName))
				return experiment;
		}
		throw new JSONException("missing experiment name " + experimentName);
	}
	// merge analytics names from all experiments
	static void addAnalytics(JSONObject experiment, String key, TreeSet<String> analytics) throws JSONException
	{
		JSONObject json = experiment.optJSONObject(Constants.JSON_FIELD_ANALYTICS);
		if (json == null) return;
		JSONArray array = json.optJSONArray(key);
		if (array == null) return;

		for (int i = 0; i < array.size(); ++i)
			analytics.add(array.getString(i));
	}
	// merge analytics values from all experiments. using Map<featureName, Set<flatAttrValue>>
	static void addAttrAnalytics(JSONObject experiment, Map<String,Set<String>> attrAnalytics)  throws JSONException
	{
		JSONObject json = experiment.optJSONObject(Constants.JSON_FIELD_ANALYTICS);
		if (json == null) return;
		JSONArray array = json.optJSONArray(Constants.JSON_FIELD_FEATURES_ATTRIBUTES_FOR_ANALYTICS);
		if (array == null) return;

		for (int i = 0; i < array.size(); ++i)
		{
			JSONObject obj = array.getJSONObject(i);

			String name = obj.getString(Constants.JSON_FIELD_NAME);
			JSONArray attributes = obj.getJSONArray(Constants.JSON_FIELD_ATTRIBUTES);

			Set<String> items = attrAnalytics.get(name);
			if (items == null)
			{
				items = new TreeSet<String>();
				attrAnalytics.put(name, items);
			}

			for (int j = 0; j < attributes.size(); ++j)
				items.add(attributes.getString(j));
		}
	}

	// copy analytics names into runtime
	static void copyAnalytics(Map<String,JSONObject> nameMap, TreeSet<String> nameAnalytics) throws JSONException
	{
		for (String name : nameAnalytics)
		{
			JSONObject obj = nameMap.get(name); // the map may contain features which are missing from the runtime
			if (obj != null)
				obj.put(Constants.JSON_FIELD_SEND_TO_ANALYTICS, true);
		}
	}
	// copy analytics attributes into runtime
	static void copyAnalytics(Map<String,JSONObject> nameMap, Map<String,Set<String>> attrAnalytics) throws JSONException
	{
		for (Map.Entry<String,Set<String>> ent : attrAnalytics.entrySet())
		{
			String name = ent.getKey();
			Set<String> data = ent.getValue();

			JSONObject obj = nameMap.get(name); // the map may contain features which are missing from the runtime
			if (obj != null)
				obj.put(Constants.JSON_FIELD_ATTRIBUTES_FOR_ANALYTICS, new JSONArray(data));
		}
	}

	//-----------------------------------------------------------

/*	public static void main(String[] args) throws PercentileException
	{
		if (args.length != 7 && args.length != 8)
		{
			System.out.println("usage: Experiments features groups context functions translations minAppVer randUser [fallbacks]");
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

			List<String> profileGroups = Json2List(groups);
			JSONObject contextObject = VerifyRule.createContextObject(context, functions, translations);

			JSONObject fallbacks = null;
			fallbacks = (args.length == 8) ? Utilities.readJson(args[7]) : null;

			Environment env = new Environment();
			env.setServerVersion("3");
			env.put(Environment.DEBUG_MODE, "true");
			JSONObject results = calculateExperimentsAndFeatures(features, contextObject, profileGroups, fallbacks, minAppVer, userRandomNumber, env);

			String formatted = results.write(true);
			System.out.println(formatted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
*/
}
