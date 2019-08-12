package com.ibm.airlock.utilities;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.admin.Utilities;

// generate a JSON object with enum constants based on an input schema

public class GenerateConstants {

	static void addSource(JSONObject output, JSONObject input, String name, String[] path) throws JSONException
	{
		JSONObject json = input;
		for (int i = 0; i < path.length; ++i)
		{
			json = json.getJSONObject(path[i]);
		}

		String theDefalt = json.optString("default");
		JSONArray theEnums = json.optJSONArray("enum");
		if (theEnums == null)
			throw new JSONException("input JSON path does not point to 'enum' array");

		JSONObject source = new JSONObject();
		if (theDefalt != null)
			source.put(theDefalt, theDefalt);

		for (int j = 0; j < theEnums.length(); ++j)
		{
			String theEnum = theEnums.getString(j);
			source.put(theEnum, theEnum);
		}

		output.put(name, source);
	}

	public static void main(String[] args)
	{
		try {
			String schema = Utilities.readString("C:/client/josemina/AirlockInputShema_profile.json");
			JSONObject input = new JSONObject(schema);
			JSONObject output = new JSONObject();

			String[] path = {"definitions", "localeDef"};
			addSource(output, input, "locale", path);
			
			String[] path2 = {"definitions", "cardinalDirectionDef"};
			addSource(output, input, "direction", path2);

			String[] path3 = {"definitions", "forecastPartDef", "properties", "dayPart"};
			addSource(output, input, "dayPart", path3);
			String[] path4 = {"definitions", "forecastPartDef", "properties", "precipType"};
			addSource(output, input, "precipitationType", path4);
			String[] path5 = {"definitions", "forecastPartDef", "properties", "thunderEnum"};
			addSource(output, input, "thunderType", path5);

			String[] path6 = {"definitions", "localeCountryCodeDef"};
			addSource(output, input, "countryCode", path6);
			String[] path7 = {"definitions", "localeLanguageDef"};
			addSource(output, input, "language", path7);

			String[] path8 = {"definitions", "significanceCodeDef"};
			addSource(output, input, "significanceCode", path8);

			String[] path9 = {"enums", "ageRange"};
			addSource(output, input, "ageRange", path9);
			String[] path10 = {"enums", "unit"};
			addSource(output, input, "unit", path10);
			String[] path11 = {"enums", "status"};
			addSource(output, input, "status", path11);
			String[] path12 = {"enums", "product"};
			addSource(output, input, "product", path12);
			String[] path13 = {"enums", "gender"};
			addSource(output, input, "gender", path13);
			
			System.out.println(output.write(true));

			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
