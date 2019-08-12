package com.ibm.airlock.engine;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;

// run a method on a node and all its children
public abstract class Visit
{
	protected static final int FEATURES = 1;
	protected static final int CONFIGURATIONS = 2;
	protected static final int ORDERING_RULES = 4;
	protected static final int ENTITLEMENTS = 1;
	protected static final int PURCHSE_OPTION = 1;
	protected int visiting = FEATURES | CONFIGURATIONS | ORDERING_RULES | ENTITLEMENTS | PURCHSE_OPTION; // by default visit both features and configurations

	protected abstract Object visit(JSONObject obj, Object state) throws Exception;

	public void run(JSONObject obj, Object state) throws Exception
	{
		// optional: get a state object from parent and create a new one
		state = visit(obj, state);

		if ((visiting & CONFIGURATIONS) != 0)
		{
			JSONArray array = obj.optJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
			if (array != null)
			{
				for (int i = 0; i < array.length(); ++i)
				{
					// pass a visited node's state to all its children
					run(array.getJSONObject(i), state);
				}
			}
		}

		if ((visiting & FEATURES) != 0)
		{
			JSONArray array = obj.optJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
			if (array != null)
			{
				for (int i = 0; i < array.length(); ++i)
				{
					run(array.getJSONObject(i), state);
				}
			}
		}
		
		if ((visiting & ORDERING_RULES) != 0)
		{
			JSONArray array = obj.optJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES);
			if (array != null)
			{
				for (int i = 0; i < array.length(); ++i)
				{
					run(array.getJSONObject(i), state);
				}
			}
		}
		
		if ((visiting & ENTITLEMENTS) != 0)
		{
			JSONArray array = obj.optJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
			if (array != null)
			{
				for (int i = 0; i < array.length(); ++i)
				{
					run(array.getJSONObject(i), state);
				}
			}
		}
		
		if ((visiting & PURCHSE_OPTION) != 0)
		{
			JSONArray array = obj.optJSONArray(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS);
			if (array != null)
			{
				for (int i = 0; i < array.length(); ++i)
				{
					run(array.getJSONObject(i), state);
				}
			}
		}
	}
}