package com.ibm.airlock.engine;

import com.ibm.airlock.engine.ScriptInvoker.Output;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.engine.ScriptInvoker.InvokerException;

public class VerifyStream
{
/*	@Deprecated
	public static void validateStreamFilter(StreamsScriptInvoker invoker, String filter) throws InvokerException
	{
		Output output = invoker.evaluateFilter(filter, "dummyEventName");
		switch (output.result)
		{
			case ERROR: throw new InvokerException(output.error);
			default:
		}
	}*/

	public static void validateStreamFilter(StreamsScriptInvoker invoker, String filter, JSONArray events) throws InvokerException, JSONException
	{
		for (int i = 0; i < events.size(); ++i)
		{
			JSONObject event = events.getJSONObject(i);
			String eventName = event.getString("name");
			Output output = invoker.evaluateFilter(filter, event);
			if (output.result == ScriptInvoker.Result.ERROR)
				throw new InvokerException("Filter fails for event " + eventName + ": " +  output.error);
		}
	}

	// filter out events that don't match the filter
	public static JSONArray evaluateStreamFilter(StreamsScriptInvoker invoker, String filter, JSONArray events) throws JSONException, InvokerException
	{
		events = (JSONArray)Utilities.cloneJson(events, true);
		JSONArray out = new JSONArray();		
		for (int i = 0; i < events.size(); ++i)
		{
			JSONObject event = events.getJSONObject(i);
			Output output = invoker.evaluateFilter(filter, event);

			switch (output.result)
			{
			case ERROR: throw new InvokerException(output.error);
			case TRUE: out.add(event); break;
			case FALSE: break;
			}
		}
		return out;
	}

	// convert filtered events to a format useful for intelliSense (a merged instance for each event)
	public static JSONObject getIntelliSenseData(JSONArray events) throws JSONException
	{
		JSONObject out = new JSONObject();
		for (int i = 0; i < events.size(); ++i)
		{
			JSONObject event = events.getJSONObject(i);
			String eventName = event.getString(Constants.JSON_FIELD_NAME);
			JSONObject data = event.getJSONObject(Constants.JSON_FIELD_EVENT_DATA);
			if (out.containsKey(eventName))
			{
				JSONObject oldData = out.getJSONObject(eventName);
				Utilities.mergeJson(data, oldData);
			}
			out.put(eventName, data);
		}
		return out;
	}
	// convert filtered events to a format useful for intelliSense (a single merged instance for all relevant events)
	public static JSONObject getIntelliSenseData(JSONArray events, boolean forFilter) throws JSONException
	{
		events = (JSONArray)Utilities.cloneJson(events, true);
		JSONObject join = new JSONObject();
		for (int i = 0; i < events.size(); ++i)
		{
			JSONObject event = events.getJSONObject(i);
			Utilities.mergeJson(join, event);
		}

		JSONObject out = new JSONObject();
		if (forFilter)
			out.put("event", join);
		else
		{
			JSONArray array = new JSONArray();
			array.add(join);
			out.put("events", array);
		}

		return out;
	}
	public static JSONObject evaluateProcessor(StreamsScriptInvoker invoker, String processor, JSONArray events) throws InvokerException
	{
		return invoker.evaluateProcessor(processor, events.toString());
	}

	public static JSONObject evaluateFilterAndProcessor(StreamsScriptInvoker invoker, String filter, String processor, JSONArray allevents) throws InvokerException, JSONException
	{
		JSONArray filteredEvents = evaluateStreamFilter(invoker, filter, allevents);
		return evaluateProcessor(invoker, processor, filteredEvents);
	}
}
