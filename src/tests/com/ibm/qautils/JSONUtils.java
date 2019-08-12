package tests.com.ibm.qautils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class JSONUtils {
	
	/**
	 * Parse the given json string into a key-value hash map.
	 * Assume the json is simple and doesn't contain arrays or objects
	 * @param json
	 * @return
	 */
	public static HashMap<String, String> jsonSimpleStringToHashMap(String json){
		
		HashMap<String, String> jsonMap = new HashMap<String, String>() ;
		String[] pairs = json.split(",");
		
		for (int i=0;i<pairs.length;i++){
			String[] keyValue = pairs[i].split(":");
			jsonMap.put(keyValue[0].replace("{", ""), keyValue[1].replace("}", "")) ;
		}
		
		return jsonMap ;
	}
	
	/**
	 * Parse the given json string into a list.
	 * Assume the json is simple and doesn't contain arrays or objects
	 * @param json
	 * @return
	 */
	public static ArrayList<String[]> jsonSimpleStringToList(String json){
		
		ArrayList<String[]> jsonList = new ArrayList<String[]>() ;
		String[] pairs = json.split(",");
		
		for (int i=0;i<pairs.length;i++){
			String[] keyValue = pairs[i].split(":");
			jsonList.add(keyValue);
		}
		
		return jsonList ;
	}
	
	public static boolean equals(JSONObject js1, JSONObject js2, String[] ignoreKeys) throws JSONException {
		
	    if (js1 == null || js2 == null) {
	        return (js1 == js2);
	    }

	    List<String> l1 =  Arrays.asList(JSONObject.getNames(js1));
	    Collections.sort(l1);
	    List<String> l2 =  Arrays.asList(JSONObject.getNames(js2));
	    Collections.sort(l2);
	    if (!l1.equals(l2)) {
	        return false;
	    }
	    for (String key : l1) {
	    	if (ArrayUtils.isIn(ignoreKeys, key, false)) continue;
	        Object val1 = js1.get(key);
	        Object val2 = js2.get(key);
	        if (val1 instanceof JSONObject) {
	            if (!(val2 instanceof JSONObject)) {
	                return false;
	            }
	            if (!equals((JSONObject)val1, (JSONObject)val2, ignoreKeys)) {
	                return false;
	            }
	        }

	        if (val1 == null) {
	            if (val2 != null) {
	                return false;
	            }
	        }  else if (!val1.equals(val2)) {
	            return false;
	        }
	    }
	    
	    return true;
	}
	
	/*
	public static boolean jsonObjsAreEqual (JSONObject js1, JSONObject js2) throws JSONException {
	    if (js1 == null || js2 == null) {
	        return (js1 == js2);
	    }

	    List<String> l1 =  Arrays.asList(JSONObject.getNames(js1));
	    Collections.sort(l1);
	    List<String> l2 =  Arrays.asList(JSONObject.getNames(js2));
	    Collections.sort(l2);
	    if (!l1.equals(l2)) {
	        return false;
	    }
	    for (String key : l1) {
	    	if (key.equals("uniqueId") || key.equals("lastModified") || key.equals("minAppVersion")  || key.equals("creationDate") || key.equals("seasonId"))
	    		continue;
	        Object val1 = js1.get(key);
	        Object val2 = js2.get(key);
	        if (val1 instanceof JSONObject) {
	            if (!(val2 instanceof JSONObject)) {
	                return false;
	            }
	            if (!jsonObjsAreEqual((JSONObject)val1, (JSONObject)val2)) {
	                return false;
	            }
	        }

	        if (val1 == null) {
	            if (val2 != null) {
	                return false;
	            }
	        }  else if (!val1.equals(val2)) {
	            return false;
	        }
	    }
	    
	    return true;
	}*/
	
	
	public static String generateUniqueString(String json, int length, String fieldName){
		JSONObject obj = new JSONObject();
		try {
			obj = new JSONObject(json);
			String value = RandomStringUtils.randomAlphabetic(length).toUpperCase();
			obj.put("name", value);
			obj.put("codeIdentifier", value);
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return obj.toString();
	}
	
}
