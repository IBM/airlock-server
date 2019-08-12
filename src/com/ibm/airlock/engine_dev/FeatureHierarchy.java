package com.ibm.airlock.engine_dev;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.FeatureItem;
import com.ibm.airlock.admin.Utilities;

public class FeatureHierarchy
{
	public static class HierarchyInfo
	{
		public String featureId = null;
		public String parentId = null;
		public ArrayList<String> childrenIds = new  ArrayList<String>();

		public String toString()
		{
			return "featureId: " + featureId + ", parentId: "  + parentId + ", children:" + childrenIds;
		}
	}

	public static Map<String,HierarchyInfo> getFeatureHierarchy(JSONObject features, boolean byName) throws JSONException
	{
		JSONObject root = features.getJSONObject(Constants.JSON_FIELD_ROOT);
		HierarchyInfo info = new HierarchyInfo();
		info.parentId = null;

		Map<String,HierarchyInfo> out = new TreeMap<String,HierarchyInfo>();
		descend(root, info, out, byName);
		return out;
	}

	static void descend(JSONObject current, HierarchyInfo info, Map<String,HierarchyInfo> out, boolean byName) throws JSONException
	{
		JSONArray children = getChildren(current);

		if (isMutex(current)) // gather all children into higher node
		{
			for (int i = 0; i < children.size(); ++i) {
				descend(children.getJSONObject(i), info, out, byName);
			}
			return;
		}

		if (byName && info.parentId == null)
			info.featureId = "<root>";
		else
			info.featureId =  getId(current, byName);

		for (int i = 0; i < children.size(); ++i)
		{
			JSONObject child = children.getJSONObject(i);
			String childId = getId(child, byName);
			info.childrenIds.add(childId);

			HierarchyInfo childInfo = new HierarchyInfo();
			childInfo.parentId = info.featureId;
			descend(child, childInfo, out, byName);
		}

		out.put(info.featureId, info);
	}
	
	static JSONArray getChildren(JSONObject obj) throws JSONException
	{
		JSONArray out = obj.optJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
		if (out == null) out = new JSONArray();
		return out;
	}
	static boolean isMutex(JSONObject obj)
	{
		String str = obj.optString(Constants.JSON_FEATURE_FIELD_TYPE, FeatureItem.Type.FEATURE.toString());
		FeatureItem.Type type = FeatureItem.Type.valueOf(str);
		return (type == FeatureItem.Type.MUTUAL_EXCLUSION_GROUP);
	}
	static String getId(JSONObject obj, boolean byName)
	{
		return obj.optString(byName ? Constants.JSON_FIELD_NAME : Constants.JSON_FIELD_UNIQUE_ID, "<unknown>");
	}

	//-------------------------------------------------------------------
	public static void main(String[] args)
	{
		try {
			assert (args.length > 0);

			JSONObject features = Utilities.readJson(args[0]);
			boolean byName = (args.length > 1 && args[1].equals("extractByName"));

			Map<String,HierarchyInfo> results = getFeatureHierarchy(features, byName);
			 for (HierarchyInfo e : results.values())
			 {
				 System.out.println(e);
				 
			 }
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
