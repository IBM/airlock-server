package com.ibm.airlock.admin.translations;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.StringStatus;
import com.ibm.airlock.TranslationServices;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.engine.Environment;


/*{
    "smartling" : {
        "translate_mode" : "custom",
        "translate_paths": ["* /translation"],
        "source_key_paths" :  ["{*}/translation"],
        "placeholder_format_custom": ["\\{([^\\}]*)\\}"], 
        "variants_enabled" : true
     },
     "translationKey1": {
        "stringId": "Lightning Title",
        "translation": "Lightning will strike within {([context.weather.lightning.distanceKm])} KM"
     },
     "translationKey2": {
        "stringId": "Ski",
        "translation": "The ski season opens when the temperature in the morning falls under {([context.weather.observations[1].temperature])}C"
     },
    "translationKey3": {
        "note": "Flu conditions",
        "stringId": "Flu",
        "translation": "There severity is {([context.weather.alerts[1].severityCode])} for the phenomenon {([context.weather.alerts[1].phenomenaCode])}  "
    },
    "timestampSentToTranslation":123
}
 */

//The file that is sent to translation and return from translation
public class TranslatedStrings {

	private static final String INSTRUCTIONS_FIELD = "smartling";

	public class TranslatedString {
		private String stringId = ""; //key
		private String translation = ""; //value

		public TranslatedString (String stringId, String translation) {
			this.stringId = stringId;
			this.translation = translation;
		}

		public TranslatedString () {			
		}

		public String getStringId() {
			return stringId;
		}

		public void setStringId(String stringId) {
			this.stringId = stringId;
		}

		public String getTranslation() {
			return translation;
		}

		public void setTranslation(String translation) {
			this.translation = translation;
		}

		public void fromJSON (JSONObject input) throws JSONException {			
			if (input.containsKey(Constants.JSON_FIELD_STRING_ID) && input.get(Constants.JSON_FIELD_STRING_ID)!=null) { 
				stringId = input.getString(Constants.JSON_FIELD_STRING_ID);
			}

			if (input.containsKey(Constants.JSON_FIELD_TRANSLATION) && input.get(Constants.JSON_FIELD_TRANSLATION)!=null) { 
				translation = input.getString(Constants.JSON_FIELD_TRANSLATION);
			}
		}

		public ValidationResults validateTranslatedStringJSON(JSONObject transStringObj) {
			try {

				//attribute
				if (!transStringObj.containsKey(Constants.JSON_FIELD_STRING_ID) || transStringObj.getString(Constants.JSON_FIELD_STRING_ID) == null || transStringObj.getString(Constants.JSON_FIELD_STRING_ID).isEmpty()) {
					return new ValidationResults("The stringId field is missing.", Status.BAD_REQUEST);
				}

				//tarnslation
				if (!transStringObj.containsKey(Constants.JSON_FIELD_TRANSLATION) || transStringObj.getString(Constants.JSON_FIELD_TRANSLATION) == null || transStringObj.getString(Constants.JSON_FIELD_TRANSLATION).isEmpty()) {
					return new ValidationResults("The translation field is missing.", Status.BAD_REQUEST);
				}


			} catch (JSONException jsne) {
				return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
			}
			catch (ClassCastException cce) {
				return new ValidationResults("Illegal TranslatedString JSON: " + cce.getMessage(), Status.BAD_REQUEST);
			}
			return null;
		}
	}

	//map between key to TranslatedString
	private HashMap<String, TranslatedString> translatedStringsMap = new HashMap<String, TranslatedString>();

	public HashMap<String, TranslatedString> getTranslatedStringsMap() {
		return translatedStringsMap;
	}

	//return error string upon error else return null
	public String addTranslatedString(String key, String value) {
		if (translatedStringsMap.get(key)!=null) {
			return "A translated string with the '" + key + "' stringId already exists."; 
		}
		TranslatedString newStr = new TranslatedString(key, value);
		translatedStringsMap.put(key, newStr);
		return null;
	}

	//at this stage we are after validate so all mandatory fields are in and the json formate is correct.
	public void fromJSON(JSONObject input) throws JSONException {
		if (input != null) {
			@SuppressWarnings("unchecked")
			Set<Map.Entry<String, JSONObject>> entries = input.entrySet();
			for (Map.Entry<String, JSONObject> entry:entries) {
				if (entry.getKey().equals(INSTRUCTIONS_FIELD))
					continue;
				TranslatedString ts = new TranslatedString();
				ts.fromJSON(entry.getValue());
				translatedStringsMap.put(ts.getStringId(), ts);
			}				
		}

	}

	public ValidationResults validateTranslatedStringsJSON(JSONObject transStringsObj, OriginalStrings originalStrings, Environment env) {
		try {

			Map<String, Boolean> entryKeysMap = new HashMap<String, Boolean>();
			Map<String, Boolean> stringIdsMap = new HashMap<String, Boolean>();
			
			//stringId should be unique + entry.getKey should be unique
			//not checking if string exists - maybe was deleted - just ignoring
			
			@SuppressWarnings("unchecked")
			Set<Map.Entry<String, JSONObject>> entries = transStringsObj.entrySet();				
			for (Map.Entry<String, JSONObject> entry:entries) {	
				if (entry.getKey().equals(INSTRUCTIONS_FIELD))
					continue;
				
				if (entryKeysMap.get(entry.getKey()) != null) {
					return new ValidationResults("Entry " + entry.getKey() + " appears more than once.", Status.BAD_REQUEST);
				}
				entryKeysMap.put(entry.getKey(), true);

				TranslatedString ts = new TranslatedString();
				ValidationResults vr = ts.validateTranslatedStringJSON(entry.getValue());
				if (vr!=null)
					return vr;

				ts.fromJSON(entry.getValue());
				if (stringIdsMap.get(ts.getStringId()) != null) {
					return new ValidationResults("stringId " + ts.getStringId() + " appears more than once.", Status.BAD_REQUEST);
				}
				
				if (TranslationServices.isTranslationStatusesSupport(env)) {
					//only strings in status IN_TRANSLATION or TRANSLATION_COMPLETE can recieve translations
					OriginalString origStr = originalStrings.getOriginalStringByKey(ts.getStringId());
					if (origStr != null) { //If string does not exists - ignore probably deleted
						if (origStr.getStatus()!=StringStatus.IN_TRANSLATION && origStr.getStatus()!=StringStatus.TRANSLATION_COMPLETE) {
							return new ValidationResults("The string " + ts.getStringId() + " is in the " + origStr.getStatus().toString() + " status. Only strings in " + StringStatus.IN_TRANSLATION.toString() + " and " + StringStatus.TRANSLATION_COMPLETE.toString() + " statuses can recieve translation values." , Status.BAD_REQUEST);
						}
					}
				}
				
				stringIdsMap.put(ts.getStringId(), true);

			}								

		}
		catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal TranslatedStrings JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}

	public JSONObject toStringsObject() throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_MOST_RECENT_TRANSLATION, (new Date()).getTime()); //should be part of the translation file and be taken from ther
		JSONObject stringsJSON = new JSONObject();
		Set<String> keys = translatedStringsMap.keySet();
		for (String key:keys) {
			stringsJSON.put (key, translatedStringsMap.get(key).getTranslation());
		}

		res.put(Constants.JSON_FIELD_STRINGS, stringsJSON);
		return res;
	}	
}
