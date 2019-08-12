package tests.restapi.testsmartling;

import java.util.ArrayList;
import java.util.Date;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class StringState
{
	enum LifeCycle { NEW, WAIT_FOR_CREATION, WAIT_FOR_TRANSLATION, PARTIALLY_TRANSLATED, TRANSLATED, ERROR };
	LifeCycle cycle;
	String text;
	String variant;
	String instruction;
	// callback here?
	String hash;
	String createProcessId;
	Date created;
	TranslationState translationState = null;

	public StringState(String text, String variant, String instruction)
	{
		this.text = text;
		this.variant = variant; // can be null
		this.instruction = instruction; // can be null
		cycle = LifeCycle.NEW;
	}
	
	JSONObject toJSON()
	{
		JSONObject json = new JSONObject();
//		json.put("")
		return json;
	}
	
	public static ArrayList<StringState> arrayFromJSON(JSONObject json, LifeCycle cycle) throws JSONException
	{
		String processId = json.optString("processUid"); // can be null
		JSONArray items = json.getJSONArray("items");
		ArrayList<StringState> out = new ArrayList<StringState>();
		for (int i = 0; i < items.length(); ++i)
		{
			JSONObject item = items.getJSONObject(i);
			StringState str = stringFromJSON(item, processId);
			str.cycle = cycle;
			if (cycle == LifeCycle.NEW)
				str.created = new Date(System.currentTimeMillis());
			out.add(str);
		}
		return out;
	}
	public static StringState stringFromJSON(JSONObject json, String processId) throws JSONException
	{
		String text = json.getString("stringText");
		String hash = json.optString("hashcode");
		String instruction = json.optString("instruction");
		String variant = json.optString("variant");

		StringState str = new StringState(text, variant, instruction);
		str.createProcessId = processId;
		str.hash = hash;
		return str;
	}
}
