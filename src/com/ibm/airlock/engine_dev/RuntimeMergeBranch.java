package com.ibm.airlock.engine_dev;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.BranchStatus;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.Utilities;

// Similar to admin.MergeBranch but operates on JSON
public class RuntimeMergeBranch
{
	@SuppressWarnings("serial")
	public static class MergeException extends Exception
	{
		public MergeException(String error) {
			super(error);			
		}
	}

	public static Map<String,JSONObject> getItemMap(JSONObject in, boolean useId) throws JSONException
	{
		Map<String, JSONObject> out = new HashMap<String, JSONObject>();
		mapItem(in, out, useId);
		return out;
	}
	static void mapItem(JSONObject in, Map<String, JSONObject> out, boolean useId) throws JSONException
	{
		String key = useId ? getId(in) : getName(in);
		out.put(key, in);

		JSONArray array =  in.optJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
		if (array != null)
			for (int i = 0; i < array.length(); ++i)
			{
				mapItem(array.getJSONObject(i), out, useId);
			}

		array =  in.optJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
		if (array != null)
			for (int i = 0; i < array.length(); ++i)
			{
				mapItem(array.getJSONObject(i), out, useId);
			}
		
		array =  in.optJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES);
		if (array != null)
			for (int i = 0; i < array.length(); ++i)
			{
				mapItem(array.getJSONObject(i), out, useId);
			}
	}
	
	static BaseAirlockItem.Type getNodeType(JSONObject obj) throws JSONException
	{
		String str = obj.getString(Constants.JSON_FEATURE_FIELD_TYPE);
		return Utilities.valueOf(BaseAirlockItem.Type.class, str); // null on error
	}
	static BranchStatus getBranchStatus(JSONObject obj) throws JSONException
	{
		String str = obj.getString(Constants.JSON_FIELD_BRANCH_STATUS);
		return Utilities.valueOf(BranchStatus.class, str); // null on error
	}
	static void setBranchStatus(JSONObject obj, BranchStatus status) throws JSONException
	{
		obj.put(Constants.JSON_FIELD_BRANCH_STATUS, status.toString());
	}

	static String getName(JSONObject obj) throws JSONException
	{
		switch (getNodeType(obj))
		{
		case ROOT :
			return Constants.ROOT_FEATURE;

		case MUTUAL_EXCLUSION_GROUP:
		case CONFIG_MUTUAL_EXCLUSION_GROUP:
		case ORDERING_RULE_MUTUAL_EXCLUSION_GROUP:
			return "mx." + getId(obj);

		default:
			String name = obj.getString(Constants.JSON_FIELD_NAME);
			String namespace = obj.getString(Constants.JSON_FEATURE_FIELD_NAMESPACE); 
			return namespace + "." + name;
		}
	}
	static String getId(JSONObject obj) throws JSONException
	{
		return obj.getString(Constants.JSON_FIELD_UNIQUE_ID); 
	}
	
	public static JSONObject merge(JSONObject clonedMaster, Map<String,JSONObject> nameMap, JSONObject branch) throws MergeException
	{
		try {

			//JSONObject clonedMaster = (JSONObject) Utilities.cloneJson(master, true);  (moved outside of loop)
			JSONArray branchItems = branch.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);

			// map each name to a node (moved outside of loop)
			// Map<String,JSONObject> nameMap = getItemMap(clonedMaster, false);
	
			// mark old nodes for deletion
			for (int i = 0; i < branchItems.length(); ++i)
			{
				JSONObject override = branchItems.getJSONObject(i);
				markForDeletion(override, nameMap);
			}

			// replace and add items
			for (int i = 0; i < branchItems.length(); ++i)
			{
				JSONObject override = branchItems.getJSONObject(i);
				override =(JSONObject) Utilities.cloneJson(override, true);
				if (isRoot(override))
				{
					mergeRootNames(override, clonedMaster);
					resolveChildren(override, nameMap, Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS, Constants.JSON_FEATURE_FIELD_FEATURES);
					//mapItem(override, nameMap, false);
					nameMap.put(getName(override), override);
					clonedMaster = override;
				}
				else
					overrideItem(clonedMaster, override, nameMap);
			}

			// remove any remaining references to old features that have been overridden or moved.
			// needed for cases such as ROOT--> A--> B changed to ROOT--> B--> A
			removeResiduals(clonedMaster);
			return clonedMaster;
		}
		catch (Exception e)
		{
			throw new MergeException("Merge error: " + e.getMessage());
		}
	}

	static boolean isRoot(JSONObject override) throws Exception
	{
		String parentName = override.optString(Constants.JSON_FIELD_BRANCH_FEATURE_PARENT_NAME);
		return parentName == null || parentName.isEmpty();
	}
	static void mergeRootNames(JSONObject override, JSONObject clonedMaster) throws Exception
	{
		// if the override of the root is checked out, the child names are taken as is
		// i.e. the children are frozen; new additions to the master root will not appear in the result.
		if (getBranchStatus(override) == BranchStatus.CHECKED_OUT)
			return;

		// else, the override child names are extended by merging them with the root's current child names.
		// i.e. children added to the master root after the branch was created will appear in the result

		// get child names from master
		JSONArray newNames = new JSONArray();
		TreeSet<String> nameSet = new TreeSet<String>();
		JSONArray source = clonedMaster.optJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);

		if (source != null)
			for (int i = 0; i < source.size(); ++i)
			{
				JSONObject child = source.getJSONObject(i);
				if (getBranchStatus(child) == BranchStatus.TEMPORARY)
					continue; // marked for deletion

				String childName = getName(child);
				newNames.add(childName);
				nameSet.add(childName);
			}

		// get child names from override, append the additional ones to the new name list
		JSONArray overrideNames = override.getJSONArray(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS);
		for (int i = 0; i < overrideNames.size(); ++i)
		{
			String overrideName = overrideNames.getString(i);
			if (!nameSet.contains(overrideName))
				newNames.add(overrideName);
				
		}
		// replace name list in override
		override.put(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS, newNames);
	}

	static void overrideItem(JSONObject out, JSONObject override, Map<String,JSONObject> nameMap) throws Exception
	{
		String parentName = override.getString(Constants.JSON_FIELD_BRANCH_FEATURE_PARENT_NAME);
		JSONObject parent = nameMap.get(parentName);
		if (parent == null)
			throw new Exception ("parent does not exist: " + parentName);

		String overrideName = getName(override);
		JSONObject original = nameMap.get(overrideName);

		if (original == null)
		{
			// this happens when an overridden DEVELOPMENT node is filtered out in a PRODUCTION master. Treat the overriding node as NEW
			if (getBranchStatus(override) == BranchStatus.CHECKED_OUT)
				setBranchStatus(override, BranchStatus.NEW); // the override should contain all fields, not just a delta
		}
		else
		{
			setBranchStatus(original, BranchStatus.TEMPORARY);  // mark for deletion in case it was NONE rather than CHECKED_OUT
		}

		boolean isConfig = isConfigType(override);
		String key = isConfig ? Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES : Constants.JSON_FEATURE_FIELD_FEATURES;

		JSONArray source = parent.optJSONArray(key);
		if (source == null)
			source = new JSONArray(); // is it OK to start with empty parent?

		// find feature in parent and replace it, or add it as new
		JSONArray newChildren = new JSONArray();
		boolean found = false;

		for (int i = 0; i < source.length(); ++i)
		{
			JSONObject child = source.getJSONObject(i);
			if (getName(child).equals(overrideName))
			{
				newChildren.add(override);
				found = true;
			}
			else
			{
				newChildren.add(child);
			}
		}
		if (!found)
			newChildren.add(override);

		// point parent to the replacement
		parent.put(key, newChildren);

		// resolve children of the override
		resolve(override, nameMap);

		// update the name map
		// mapItem(override, nameMap, false);
		nameMap.put(overrideName, override);
	}

	static boolean isConfigType(JSONObject item) throws JSONException
	{
		switch (getNodeType(item))
		{
		case CONFIGURATION_RULE:
		case CONFIG_MUTUAL_EXCLUSION_GROUP:
		case ORDERING_RULE:
		case ORDERING_RULE_MUTUAL_EXCLUSION_GROUP:
			return true;
		default:
			return false;
		}
	}

	static void resolve(JSONObject override, Map<String,JSONObject> nameMap) throws Exception
	{
		mergeDelta(override, nameMap);
		resolveChildren(override, nameMap, Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS, Constants.JSON_FEATURE_FIELD_FEATURES);
		resolveChildren(override, nameMap, Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS, Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
		resolveChildren(override, nameMap, Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS, Constants.JSON_FEATURE_FIELD_ORDERING_RULES);
	}
	static void resolveChildren(JSONObject override, Map<String,JSONObject> nameMap, String nameKey, String childKey) throws Exception
	{		
		JSONArray names = override.optJSONArray(nameKey);
		JSONArray source = override.optJSONArray(childKey);
		if (names == null || names.isEmpty())
			return;

		Map<String,JSONObject> overrideKids = new HashMap<String,JSONObject>();
		if (source != null)
			for (int i = 0; i < source.length(); ++i)
			{
				JSONObject child = source.getJSONObject(i);
				overrideKids.put(getName(child), child);
			}

		JSONArray newKids = new JSONArray();
		for (int i = 0; i < names.size(); ++i)
		{
			String childName = names.getString(i);

			// look for the child in the override first, then look in the master
			JSONObject child = overrideKids.get(childName);
			if (child == null)
			{
				child = nameMap.get(childName);
				if (child == null) // this happens when a DEVELOPMENT node is filtered out in a PRODUCTION master. remove it from the list of children
					continue;

				// we can't just put the child as-is - it may have moved from a parent that isn't overridden and still points to it.
				// that old link needs to be identified in removeResiduals. so we clone the child, put the duplicate in the
				// override, and mark the original for deletion. the clone is shallow since we only need to override the status.

				JSONObject newChild = (JSONObject) Utilities.cloneJson(child, false);
				setBranchStatus(child, BranchStatus.TEMPORARY);
				child = newChild;
			}
			newKids.add(child);
		}

		override.put(childKey, newKids);

		// recurse on the override's children
		for (JSONObject child : overrideKids.values())
		{
			resolve(child, nameMap);
		}

		for (int i = 0; i < newKids.size(); ++i)
		{
			JSONObject child = newKids.getJSONObject(i);
			nameMap.put(getName(child), child);
		}
	}
	// find old nodes that are being replaced and mark them for deletion
	static void markForDeletion(JSONObject override, Map<String,JSONObject> nameMap) throws JSONException
	{
		if (getBranchStatus(override) == BranchStatus.CHECKED_OUT)
		{
			String key = getName(override);
			JSONObject original = nameMap.get(key);
			if (original != null)
			{
				setBranchStatus(original, BranchStatus.TEMPORARY); // mark the original node for deletion
			}
			else // this happens when an overridden DEVELOPMENT node is filtered out in a PRODUCTION master. Treat the overriding node as NEW
			{
				setBranchStatus(override, BranchStatus.NEW); // the override should contain all fields, not just a delta
			}
		}
		markChildren(override, nameMap, Constants.JSON_FEATURE_FIELD_FEATURES);
		markChildren(override, nameMap, Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
		markChildren(override, nameMap, Constants.JSON_FEATURE_FIELD_ORDERING_RULES);
		
	}
	static void markChildren(JSONObject override, Map<String,JSONObject> nameMap, String childKey) throws JSONException
	{
		JSONArray array = override.optJSONArray(childKey);
		if (array != null)
			for (int i = 0; i < array.size(); ++i)
			{
				JSONObject feature = array.getJSONObject(i);
				markForDeletion(feature, nameMap);
			}
	}
	static void removeResiduals(JSONObject item) throws JSONException
	{
		removeChidren(item, Constants.JSON_FEATURE_FIELD_FEATURES);
		removeChidren(item, Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
		removeChidren(item, Constants.JSON_FEATURE_FIELD_ORDERING_RULES);
	}
	static void removeChidren(JSONObject item, String childKey) throws JSONException
	{
		JSONArray array = item.optJSONArray(childKey);
		if (array == null || array.isEmpty())
			return;

		JSONArray newChildren = new JSONArray();
		for (int i = 0; i < array.length(); ++i)
		{
			JSONObject feature = array.getJSONObject(i);
			if (getBranchStatus(feature) != BranchStatus.TEMPORARY)
			{
				newChildren.add(feature);
				removeResiduals(feature);
			}
		}

		if (array.size() != newChildren.size())
			item.put(childKey, newChildren);
	}

	// copy missing items from the original to the checkout
	static void mergeDelta(JSONObject override, Map<String,JSONObject> nameMap) throws JSONException
	{
		String nodeName = getName(override);
		JSONObject original = nameMap.get(nodeName);
		if (getBranchStatus(override) != BranchStatus.CHECKED_OUT || original == null)
			return;

		@SuppressWarnings("unchecked")
		Set<String> keys = original.keySet();
		for (String key : keys)
		{
			if (key.equals(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES) ||key.equals(Constants.JSON_FEATURE_FIELD_FEATURES)||key.equals(Constants.JSON_FEATURE_FIELD_ORDERING_RULES))
				continue;

			if (!override.containsKey(key))
			{
				Object obj = original.get(key);
				override.put(key, obj);
			}
		}
		//mergeAnalytics(override, original);
	}
/*	static void mergeAnalytics(JSONObject override, JSONObject original) throws JSONException
	{
		TreeSet<String> analytics = new TreeSet<String>();
		addAnalytics(original, analytics);
		addAnalytics(override, analytics);
		override.put(Constants.JSON_FIELD_ATTRIBUTES_FOR_ANALYTICS, new JSONArray(analytics));
	}
	static void addAnalytics(JSONObject json, TreeSet<String> analytics) throws JSONException
	{
		JSONArray array = json.optJSONArray(Constants.JSON_FIELD_ATTRIBUTES_FOR_ANALYTICS);
		if (array != null)
			for (int i = 0; i < array.size(); ++i)
				analytics.add(array.getString(i));
	}*/
}
