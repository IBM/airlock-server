package tests.utils.src;


import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;

import tests.restapi.scenarios.capabilities.TestAllApi;

public class FindLocalyticsFeatures {
	public static void main(String[] args) {
		if(args.length!=5) {
			System.out.println("Usage: removeUnusedBranches server-url productID apiKey apiKeyPasswork changePercantage");
			System.exit(-1);
		}
		
		String serverUrl = args[0];
		String productID = args[1];
		String key = args[2];
		String keyPassword = args[3];
		boolean changePercantage = Boolean.valueOf(args[4]);
		
		String adminUrl = serverUrl + "/api/admin";
		String operationsUrl = serverUrl + "/api/ops";
		String translationsUrl = serverUrl + "/api/translations";
		String analyticsUrl = serverUrl + "/api/analytics";
		
		//the UUIDs of the configuration to keep
		String[] configurationsToKeepArray = new String[]{};
		Set<String> configurationsToKeepSet = new HashSet<String>(); 
		for (int j = 0; j<configurationsToKeepArray.length; j++) {
			configurationsToKeepSet.add(configurationsToKeepArray[j]);
		}
		
		try {
			TestAllApi allApis = new TestAllApi(adminUrl, operationsUrl,translationsUrl,analyticsUrl, "");
			String sessionToken = allApis.startSessionFromKey(key, keyPassword);
			if (sessionToken.contains("error")) {
				System.err.println("Failed to generate session token from key: " + sessionToken);
				return;
			}
			
			System.out.println("Feature name; Feature id; Configuartion name; Configuartion id" );
			
			JSONArray seasonsArray = allApis.getSeasonsPerProduct(productID, sessionToken);
			for (int i=0; i<seasonsArray.length(); i++) {
				String seasonID = seasonsArray.getJSONObject(i).getString("uniqueId");
				System.out.println("seasonID = " + seasonID);
				
				String response = allApis.getFeaturesBySeasonFromBranch(seasonID, "MASTER", sessionToken);
				JSONObject seasonObj = new JSONObject(response);
				
				JSONObject root = seasonObj.getJSONObject("root");
				
				checkFeature(root, changePercantage, allApis, sessionToken, "ROOT", "ROOT");
			}
			
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
		System.out.print("END!!!!");
	}
	
	private static void checkFeature(JSONObject airlockObj, boolean changePercentage, TestAllApi allApis, String sessionToken, String featureName, String featureId) throws JSONException, IOException {
		//go over the feature's configuration rules. If one of the contains the string "service":"LOCALYTICS" print it/move to 1%
		if (airlockObj.containsKey("configurationRules")) { //root does not contain cr array
			JSONArray configurationRulesArr = airlockObj.getJSONArray("configurationRules");
			for (int i=0; i<configurationRulesArr.size(); i++) {
				if (configurationRulesArr.getJSONObject(i).containsKey("configuration")) { //cr mtx does not contains configuration field
					String config = configurationRulesArr.getJSONObject(i).getString("configuration");
					config = config.trim();
					//config = config.replace(" ", "");
					if (config.contains("\"service\": \"LOCALYTICS\",")) {
						String confName = configurationRulesArr.getJSONObject(i).getString("name");
						System.out.println(featureName + "; " + featureId + ";" + configurationRulesArr.getJSONObject(i).getString("name") + ";" + configurationRulesArr.getJSONObject(i).getString("uniqueId"));
						
						if (changePercentage && (!featureName.equals("Video Summary Event") && !confName.equals("Localytics Video Summary Beacon"))) {
							updateConfigurationRulePercentage(configurationRulesArr.getJSONObject(i), allApis, sessionToken);
						}
					}
				}
				checkFeature(configurationRulesArr.getJSONObject(i), changePercentage, allApis, sessionToken, featureName, featureId);
			}
		}
		
		if (airlockObj.containsKey("features")) {
			JSONArray featuresArr = airlockObj.getJSONArray("features");
			for (int i=0; i<featuresArr.size(); i++) {
				String currentFeatureName = featureName;
				if (featuresArr.getJSONObject(i).containsKey("name")) { //mtx does not contains name field
					currentFeatureName = featuresArr.getJSONObject(i).getString("name");
				}
				
				checkFeature (featuresArr.getJSONObject(i), changePercentage, allApis, sessionToken, currentFeatureName, featuresArr.getJSONObject(i).getString("uniqueId"));
			}
		}
		
	}

	private static void updateConfigurationRulePercentage(JSONObject jsonObject, TestAllApi allApis, String sessionToken) throws JSONException, IOException {
		String crId = jsonObject.getString("uniqueId");
		String seasonId = jsonObject.getString("seasonId");
			
		String configurationRule = allApis.getFeatureFromBranch(crId, "MASTER", sessionToken);
		JSONObject json = new JSONObject(configurationRule);
		json.put("rolloutPercentage", 100);
		String response = allApis.updateFeatureInBranch(seasonId, "MASTER", crId, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "update configuration rule failed: " + response );	
		
		configurationRule = allApis.getFeatureFromBranch(crId, "MASTER", sessionToken);
		JSONObject crObj = new JSONObject(configurationRule);
		Assert.assertTrue(crObj.getInt("rolloutPercentage") == 100, "rolloutPercentage was not updated in cr: " + crId);	
	}
	
}
