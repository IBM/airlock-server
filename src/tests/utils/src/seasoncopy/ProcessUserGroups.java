package tests.utils.src.seasoncopy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;

import tests.com.ibm.qautils.RestClientUtils;

public class ProcessUserGroups {
	static final String PARAM_SEPARATOR = "\\s*;\\s*";
	private static final String GET_USERGROUPS_FROM_SERVER = "GET_USERGROUPS_FROM_SERVER";
	private static final String GET_USERGROUPS_FROM_LIST = "GET_USERGROUPS_FROM_LIST";
	private static final String PUT_USERGROUPS_TO_SERVER = "PUT_USERGROUPS_TO_SERVER";
	private static final String GET_USERGROUPS_FROM_FEATURE_TREE = "GET_USERGROUPS_FROM_FEATURE_TREE";
	private static Properties properties = new Properties();
	private static String action = "";
	private static String seasonFile = "";
	private static String seasonId = "";
	private static String url = "";
	private static String putUserGroupsUrl = "";
	private static List<String> internalUserGroups = new ArrayList<String>();
	private static String sessionToken = "";
	private static String outputFile = "";

	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		if (args.length != 1)
		{
			System.out.println("usage: ProcessUserGroups ProcessUserGroups-configuration-file");
			return;
		}
		
		String configPath = args[0];
		properties = new Properties();
		try {
			properties.load(new InputStreamReader(new FileInputStream(args[0]), "UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		action = properties.getProperty("action");
		if (action == null)
			throw new Exception("action missing in config file");
		
		String[] params = action.split(PARAM_SEPARATOR);
		List<String> actions = Arrays.asList(params);
		
		if (actions.contains(GET_USERGROUPS_FROM_SERVER))
			getUserGroupsFromServer();
		
		if (actions.contains(GET_USERGROUPS_FROM_LIST))
			getUserGroupsFromList();
		
		if (action.contains(GET_USERGROUPS_FROM_FEATURE_TREE))
			getUserGroupsFromFeatureTree();
		
		if (actions.contains(PUT_USERGROUPS_TO_SERVER))
			putUserGroupsToServer();
		
		System.out.println("DONE!");
	}
	
	//get user groups from server
	private static void getUserGroupsFromServer() throws Exception{
		
		if (properties.getProperty("url") != null)
			url = properties.getProperty("url");
		else
			throw new Exception("server url missing in config file");
		
		if (properties.getProperty("sessionToken") != null)
			sessionToken = properties.getProperty("sessionToken");
		
		if (properties.getProperty("outputFile") != null && properties.getProperty("outputFile") != "")
			outputFile = properties.getProperty("outputFile");
		else 
			System.out.println("Output file for user groups list was not provided. Proceeding without it");
		
		
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(url + "/usergroups", sessionToken);
		String response = res.message;
		JSONObject json = new JSONObject(res.message);
		JSONArray userGroups = json.getJSONArray("internalUserGroups");
		for(int i = 0; i < userGroups.length(); i++){
		    internalUserGroups.add(userGroups.getString(i));
		}
		
		
		if (outputFile != "")
			printUserGroups(outputFile);
	}
	
	//get user groups from specific product sitting on a server
	private static void getUserGroupsFromFeatureTree() throws Exception {

		if (properties.getProperty("url") != null)
			url = properties.getProperty("url");
		else
			throw new Exception("server url missing in config file");

		if (properties.getProperty("seasonId") != null)
			seasonId = properties.getProperty("seasonId");
		else
			throw new Exception("seasonId missing in config file");

		if (properties.getProperty("sessionToken") != null)
			sessionToken = properties.getProperty("sessionToken");

		
		if (properties.getProperty("outputFile") != null && properties.getProperty("outputFile") != "")
			outputFile = properties.getProperty("outputFile");
		else 
			System.out.println("Output file for user groups list was not provided. Proceeding without it");

		JSONArray featuresArray = getFeaturesBySeason(url, seasonId, sessionToken);

		getUserGroupsFromFeature(featuresArray);

		if (outputFile != "")
			printUserGroups(outputFile);
	}
	
	//get user groups from a list of features saved on disk
	private static void getUserGroupsFromList() throws Exception {
		if (properties.getProperty("seasonFile") != null)
			seasonFile = properties.getProperty("seasonFile");
		else
			throw new Exception("seasonFile missing in config file");
		
		if (properties.getProperty("outputFile") != null && properties.getProperty("outputFile") != "")
			outputFile = properties.getProperty("outputFile");
		else 
			System.out.println("Output file for user groups list was not provided. Proceeding without it");

		
			String jsonData = readFile(seasonFile);
			JSONObject seasonJson = new JSONObject(jsonData);
			JSONArray featuresArray = seasonJson.getJSONObject("root").getJSONArray("features");

			getUserGroupsFromFeature(featuresArray);

			if (outputFile != "")
				printUserGroups(outputFile);
	}
	
	private static void putUserGroupsToServer() throws Exception{
		
		if (properties.getProperty("putUserGroupsUrl") != null)
			putUserGroupsUrl = properties.getProperty("putUserGroupsUrl");
		else
			throw new Exception("target server url missing in config file");
		
		if (properties.getProperty("sessionToken") != null)
			sessionToken = properties.getProperty("sessionToken");

		
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(putUserGroupsUrl + "/usergroups", sessionToken);
			String response = res.message;
			JSONArray currentGroups = new JSONObject(response).getJSONArray("internalUserGroups");
			List<String> currGroupsList = new ArrayList<String>();
			for (int i=0; i<currentGroups.length(); i++){
				currGroupsList.add(currentGroups.getString(i));
			}
			
			for (String group : internalUserGroups){
				if (!currGroupsList.contains(group)) {
					currGroupsList.add(group);
				} 
			}

	 
			JSONObject json = new JSONObject(response);
			json.remove("internalUserGroups");
			
			json.put("internalUserGroups", currGroupsList);
			
			try {
				res = RestClientUtils.sendPut( url + "/usergroups", json.toString(), sessionToken);
				if (res.code != 200)
					System.out.println("Could not update user groups. Some feature may not be created " + res.message);
			} catch (Exception e){
				System.out.println("Could not update user groups. Some feature may not be created " + e.getLocalizedMessage());
			}
			
		
		}
	
		private static String readFile(String filename) {
		    String result = "";
		    try {
		        @SuppressWarnings("resource")
				BufferedReader br = new BufferedReader(new FileReader(filename));
		        StringBuilder sb = new StringBuilder();
		        String line = br.readLine();
		        while (line != null) {
		            sb.append(line);
		            line = br.readLine();
		        }
		        result = sb.toString();
		    } catch(Exception e) {
		    	System.out.println("File not found: "  + filename);
		    }
		    return result;
		}
		
		private static void getUserGroupsFromFeature(JSONArray featuresArray) throws JSONException{
			for (int i = 0; i< featuresArray.length(); i++) {
				JSONObject feature = featuresArray.getJSONObject(i);
				
				//get sub-features array
				JSONArray subFeatures = new JSONArray();
				if (feature.containsKey("features")) {
					subFeatures = feature.getJSONArray("features");
				}
				
				//get configuration rules array
				JSONArray configurationRules = new JSONArray();			
				if (feature.containsKey("configurationRules"))
					configurationRules = feature.getJSONArray("configurationRules");

				if (feature.containsKey("internalUserGroups")){		//for mutual-exclusion groups
					JSONArray userGroups = feature.getJSONArray("internalUserGroups");
					for(int k = 0; k < userGroups.length();k++){
						if (!internalUserGroups.contains(userGroups.getString(k))){
							internalUserGroups.add(userGroups.getString(k));
						}	
					}
				}	
				
				if (configurationRules.size() != 0) 	
					getUserGroupsFromFeature(configurationRules);

				if (subFeatures.size() != 0)
					getUserGroupsFromFeature(subFeatures);
		
			}
		}

		
		private static void printUserGroups(String outputFile){
			try {
				FileWriter fw = new FileWriter(outputFile, true);
			    BufferedWriter bw = new BufferedWriter(fw);
			    PrintWriter out = new PrintWriter(bw);
			    
			    for (String group : internalUserGroups){
			    	out.println(group);
			    	
				}
			    out.close();

			} catch (IOException e) {
			    System.out.println("cannot write usergroups to file: " + e.getLocalizedMessage());
			}
		}
		
		private static JSONArray getFeaturesBySeason(String url, String seasonID, String sessionToken){
			JSONArray features=new JSONArray();
			try {
			 	
				RestClientUtils.RestCallResults res = RestClientUtils.sendGet(url +"/products/seasons/" + seasonID + "/features", sessionToken);
				String response = res.message;
			 	JSONObject jsonResponse = new JSONObject(response);
			 	JSONObject season = jsonResponse.getJSONObject("root");
				features = season.getJSONArray("features");
			 			
			} catch (Exception e) {
				Assert.fail("An exception was thrown when trying  to get a list of all features in season. Message: "+e.getMessage()) ;
			}
			return features;
		}
}
