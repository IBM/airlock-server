package com.ibm.airlock.admin.streams;

import java.util.ArrayList;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class AddStreamsToSchema
{
	public static void merge(JSONObject mainSchema, JSONObject streamsJson, boolean streamsAreRequired) throws Exception
	{
		// for now assume all streams in streamsJson will actually appear in the context, as full or null objects
		JSONArray required = new JSONArray();
		JSONObject properties = new JSONObject();

		JSONArray streams = streamsJson.getJSONArray("streams");
		for (int i = 0; i < streams.length(); ++i)
		{
			JSONObject stream = streams.getJSONObject(i);
			String stage = stream.getString("stage");
			String minApp = stream.getString("minAppVersion");
			String name = stream.getString("name");

			 // the schema name replaces spaces and dots with underscores
			String nameInSchema = name.replace(" ", "_").replace(".",  "_");

			//JSONObject json = stream.getJSONObject("resultsSchema");
			// temporary code, should always by JSON
			JSONObject json;
			Object obj = stream.get("resultsSchema");
			if (obj instanceof JSONObject)
				json = (JSONObject) obj;
			else if (obj instanceof String)
			{
				String str = ((String)obj).trim();
				json = str.isEmpty() ? new  JSONObject() :  new JSONObject(str);
			//	Utilities.writeString(json.write(true), "c:/json.json");
			}
			else
				throw new Exception("resultsSchema has unexpected type");

			if (!json.isEmpty())
				insertMinApp(json, minApp, stage);
			properties.put(nameInSchema, json);
			if (streamsAreRequired)
				required.add(nameInSchema);
		}

		JSONArray type = new JSONArray();
		type.add("object");
		type.add("null");

		JSONObject newStreams = new JSONObject();
		newStreams.put("type", type);
		newStreams.put("properties", properties);
		if (streamsAreRequired)
			newStreams.put("required", required);

		// now insert the new streams object into the schema
		if (mainSchema.containsKey("inputSchema")) // accept both wrapped and unwrapped schemas
			mainSchema = mainSchema.getJSONObject("inputSchema");

		JSONObject mainProps = mainSchema.getJSONObject("properties");
		JSONObject context = mainProps.getJSONObject("context");

		JSONObject subProps = context.optJSONObject("properties");
		if (subProps == null)
		{
			subProps = new JSONObject();
			context.put("properties", subProps);
		}
		subProps.put("streams", newStreams);

/*		top streams level is already required in schema
		if (streamsAreRequired)
		{
			JSONArray req = context.optJSONArray("required");
			if (req == null)
			{
				req = new JSONArray();
				context.put("required", req);
			}
			req.add("streams");
		}
*/
	}

	// propagate the stage/minApp down to the leaves
	static void insertMinApp(Object obj, String minApp, String stage) throws JSONException
	{
		if (obj instanceof JSONArray)
		{
			JSONArray array = (JSONArray) obj;
			for (int i = 0; i < array.length(); ++i)
				insertMinApp(array.get(i), minApp, stage);
		}
		else if (obj instanceof JSONObject)
		{
			JSONObject json = (JSONObject) obj;
			if (json.containsKey("properties"))
			{
				json = json.getJSONObject("properties");
				@SuppressWarnings("unchecked")
				ArrayList<Object> children = new ArrayList<Object>(json.values()); // making a copy to be safe
				for (Object child : children)
					insertMinApp(child, minApp, stage);

				/* TreeSet<String> keys = new TreeSet<String>(json.keySet());
				   for (String key : keys)
					insertMinApp(json.get(key), minApp, stage);
				*/
			}
			else // reached bottom leaf
			{
				json.put("stage", stage);
				json.put("minAppVersion", minApp);
			}
		}
	}
/* 
	import com.ibm.airlock.admin.Utilities;
	public static void main(String[] args)
	{
		try {
			JSONObject a = Utilities.readJson("C:/Develop/Weather/AirlockInputShema.json");
			JSONObject b = Utilities.readJson("C:/client/streams/streams_IOS.json");
			AddStreamsToSchema.merge(a, b, true);
			Utilities.writeString(a.toString(true), "C:/Develop/Weather/merged.json");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}*/
}
