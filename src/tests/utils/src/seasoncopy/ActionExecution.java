package tests.utils.src.seasoncopy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import tests.com.ibm.qautils.RestClientUtils;


public class ActionExecution {
	protected Properties properties;
	private static final String ALL_FEATURES = "all"; 
	private static final String ADD_FEATURES_TO_NEW_SEASON = "addFeaturesToNewSeason";
	private static final String ADD_FEATURES_TO_EXISTING_SEASON = "addFeaturesToExistingSeason";
	private static final String ADD_SUB_FEATURES = "addSubFeatures";
	private static final String ADD_FEATURES_TO_SEASON_WITH_GUID = "addFeaturesToSeasonWithGUID";
	private static List<String> listOfFeatures = new ArrayList<String>();
	private static String product = "";
	private static String action = "";
	private static String seasonId = "";
	private static String seasonMinVer = "";
	private static String features = "";
	private static String seasonFile = "";
	private static String url = "";
	private static String translationsUrl = "";
	private static String internalUserGroups = "";
	private static String sessionToken = "";
	private static JSONObject seasonJson;
	private static String parentId = "";
	private static String utilities = "";
	private static String translations = "";
	private static String inputSchema = "";
	private static String locale = "en";
	private static String strings = "";
	private static String productWithGUID = "";
	private static String utilityMinVersion = "";
	private static String utilityStage = "";

	public ActionExecution(Properties props) {
		properties = props;
	}
	
	public void doExecute(){
		validateProperties();
		processActionParameters();
	}
	
	 
	
	public void validateProperties() {
		if (properties.getProperty("product") != null)
			product = properties.getProperty("product");
		
		if (properties.getProperty("productWithGUID") != null)
			productWithGUID = properties.getProperty("productWithGUID");
		
		if (properties.getProperty("action") != null)
			action = properties.getProperty("action");
		
		if (properties.getProperty("seasonId") != null)
			seasonId = properties.getProperty("seasonId");
		
		if (properties.getProperty("features") != null)
			features = properties.getProperty("features");
		
		if (properties.getProperty("seasonMinVer") != null)
			seasonMinVer = properties.getProperty("seasonMinVer");
		
		if (properties.getProperty("seasonMinVer") != null)
			seasonFile = properties.getProperty("seasonFile");
		
		if (properties.getProperty("url") != null)
			url = properties.getProperty("url");
		
		if (properties.getProperty("translationsUrl") != null)
			translationsUrl = properties.getProperty("translationsUrl");
		
		
		if (properties.getProperty("internalUserGroups") != null)
			internalUserGroups = properties.getProperty("internalUserGroups");
		
		if (properties.getProperty("sessionToken") != null)
			sessionToken = properties.getProperty("sessionToken");
		
		if (properties.getProperty("parentId") != null)
			parentId = properties.getProperty("parentId");
		
		if (properties.getProperty("utilities") != null)
			utilities = properties.getProperty("utilities");
		
		if (properties.getProperty("utilityMinVersion") != null)
			utilityMinVersion = properties.getProperty("utilityMinVersion");
		
		if (properties.getProperty("utilityStage") != null)
			utilityStage = properties.getProperty("utilityStage");
		
		if (properties.getProperty("translations") != null)
			translations = properties.getProperty("translations");
		
		if (properties.getProperty("inputSchema") != null)
			inputSchema = properties.getProperty("inputSchema");
		
		if (properties.getProperty("locale") != null)
			locale = properties.getProperty("locale");
		
		if (properties.getProperty("strings") != null)
			strings = properties.getProperty("strings");
		
		
		
	}
	
	public void processActionParameters() {
		
		if (url.equals("")){
			System.out.println("URL of the new environment was not provided.");
			return;
		}
		
		if (seasonFile.equals("")){
			System.out.println("Season file to copy was not provided.");
			return;
		} else {			
			try {
				String jsonData = readFile(seasonFile);
				seasonJson = new JSONObject(jsonData);

			}catch (JSONException e) {
				System.out.println("Invalid json in season file "  + seasonFile);
				return;
			}
		}
		


		
		if (!internalUserGroups.equals("")){
			String[] userGroups = internalUserGroups.split(",");
			try {
				createUserGroups(userGroups);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.getLocalizedMessage();
			}
			
		}
				
		JSONArray featuresToCopy = new JSONArray();
		try {
			featuresToCopy = getFeaturesToCopy();
		} catch (JSONException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		
		if (action.equals(ADD_FEATURES_TO_NEW_SEASON)){
			
			if (seasonMinVer.equals("")){
				System.out.println("Season's minVersion was not provided.");
				return;
			}
			
			try {
				//existing product
				//create a new season using productID and copy features to it
				UUID.fromString(product);
				String seasonID = createSeason(product, seasonMinVer);
				createAssets(seasonID);
				copyFeatures(seasonID, featuresToCopy, "ROOT");
			}catch (Exception e){
				if (product.equals("")) {
					System.out.println("Provide either a product id to use an existing product or a new product name.");
				} else	{
					//new product + new season
					try {
						String productID = createProduct(product);
						String seasonID = createSeason(productID, seasonMinVer);
						try {
							createAssets(seasonID);
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						copyFeatures(seasonID, featuresToCopy, "ROOT");
					} catch (JSONException e1) {
						System.out.println("Can't process json: " + e1.getLocalizedMessage());
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

				}	
			}
		} else if (action.equals(ADD_FEATURES_TO_EXISTING_SEASON)){
			try {
				UUID.fromString(seasonId);
				createAssets(seasonId);
				//copy features to config seasonId
				copyFeatures(seasonId, featuresToCopy, "ROOT");
			}catch (Exception e){
				//invalid season id
				System.out.println("Invalid season id: " + seasonId);
			}
			
		} else if (action.equals(ADD_SUB_FEATURES)) {
			
			if (parentId.equals("") || seasonId.equals("")){
				System.out.println("Provide parent feature id and season id and try again.");
				return;
			} else {
				try {
					try {
						createAssets(seasonId);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					try {
						featuresToCopy = getFeaturesToCopy();
					} catch (JSONException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					copyFeatures(seasonId, featuresToCopy, parentId);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
		} else if (action.equals(ADD_FEATURES_TO_SEASON_WITH_GUID)) {
			//use provided GUID to create product or/and season
			try {
				//valid productId
				UUID.fromString(productWithGUID);
				productWithGUID = createProductWithGUID(product, productWithGUID);
				try {
					//valid seasonID
					UUID.fromString(seasonId);
					String seasonID = createSeasonWithGUID(productWithGUID, seasonMinVer, seasonId);
					createAssets(seasonID);
					copyFeatures(seasonID, featuresToCopy, "ROOT");
				} catch (Exception ex) {
					try {
						//for given productID create a new season with minVer
						String seasonID = createSeason(productWithGUID, seasonMinVer);
						createAssets(seasonID);
						copyFeatures(seasonID, featuresToCopy, "ROOT");
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						System.out.println("Cannot create a new season for existing product: " + product + "\r\n" + e1.getLocalizedMessage());
					}					
				}
			} catch (Exception e) {
				System.out.println("Invalid product GUID. Cannot proceed with selected action: " + ADD_FEATURES_TO_SEASON_WITH_GUID);
			}
					
		
		
		} else {
			System.out.println("Unknown action: " + action);
			return;
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
	
	String readString(String filepath) throws Exception
	{
		Path path = Paths.get(filepath);
		byte[] data = Files.readAllBytes(path);
		return new String(data, "UTF-8");
	}
	
	private String createProduct(String productName) throws JSONException, IOException{
		JSONObject product = new JSONObject();
		product.put("name", productName);
		product.put("codeIdentifier", productName);
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(url+"/products", product.toString(), sessionToken);
		String result = res.message;
		String productID = parseResultId(result, productName);
		return productID;
	}
	
	
	private String createProductWithGUID(String productName, String productId) throws JSONException, IOException{
		JSONObject product = new JSONObject();
		product.put("name", productName);
		product.put("codeIdentifier", productName);
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(url+"/products/" + productId, product.toString(), sessionToken);
		String result = res.message;
		String productID = parseResultId(result, productName);
		return productID;
	}

	
	private String createSeason(String productID, String minVersion) throws JSONException, IOException{
		JSONObject season = new JSONObject();
		season.put("minVersion", minVersion);
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(url+"/products/" + productID + "/seasons/", season.toString(), sessionToken);
		String result = res.message;
		String seasonID = parseResultId(result, "");
		return seasonID;
	}
	
	private String createSeasonWithGUID(String productId, String minVersion, String seasonId) throws JSONException, IOException{
		JSONObject season = new JSONObject();
		season.put("minVersion", minVersion);
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(url+"/products/" + productId + "/seasons/" + seasonId, season.toString(), sessionToken);
		String result = res.message;
		String seasonID = parseResultId(result, "");
		return seasonID;
	}
	
	private String createFeature(String seasonID, String featureJson, String parentID) throws JSONException, IOException{
		JSONObject jsonF = new JSONObject(featureJson);
		String featureName = "";
		if(jsonF.containsKey("name"))
			featureName = jsonF.getString("name");
		else
			featureName = "mutual exclusion group";
		
		RestClientUtils.RestCallResults res =RestClientUtils.sendPost(url+"/products/seasons/" + seasonID + "/features?parent=" + parentID, featureJson, sessionToken);
		String result = res.message;
		
		String featureID = parseResultId(result, featureName);
		return featureID;
	}
	
	
	private String parseResultId(String result, String itemName){
		String uniqueID = "";
		JSONObject response = null;;
		try {
			response = new JSONObject(result);
			uniqueID = (String)response.get("uniqueId");
			System.out.println("Added feature: " + itemName);
		} catch (JSONException e) {

			System.out.println("An exception was thrown when trying  to add a component " + itemName +   " . Message: "+ result) ;
			uniqueID = result;			
		}

		return uniqueID;

	}

	
	private void copyFeatures(String seasonId, JSONArray featuresToCopy, String parentId) throws JSONException{

		for (int i = 0; i< featuresToCopy.length(); i++) {
			String parent = parentId;
			JSONObject feature = featuresToCopy.getJSONObject(i);
			
			//get sub-features array
			JSONArray subFeatures = new JSONArray();
			if (feature.containsKey("features")) {
				subFeatures = feature.getJSONArray("features");
			}
			
			//get configuration rules array
			JSONArray configurationRules = new JSONArray();			
			if (feature.containsKey("configurationRules"))
				configurationRules = feature.getJSONArray("configurationRules");

			
			
			//check if user groups in feature exist and create groups if necessary
			if (feature.containsKey("internalUserGroups")) {
				JSONArray userGroups = feature.getJSONArray("internalUserGroups");
				String[] newUserGroups = toStringArray(userGroups);
				try {
					createUserGroups(newUserGroups);
				} catch (Exception e) {
					System.out.println("Cannot create user groups: "  + e.getLocalizedMessage());
					
				}
			}
			
						
			//remove fields that are not allowed for feature creation
			feature = removeFields(feature);
			
			//create the feature
			try {
				parent = createFeature(seasonId, feature.toString(), parent);
				if (configurationRules.size() != 0) {	
					copyFeatures(seasonId, configurationRules, parent);
				}
			} catch (IOException e) {
				System.out.println("Can't create feature " + feature.getString("name") + ". The reason: " + e.getLocalizedMessage());
			}
			
			//create sub-features with this parent 
			if (subFeatures.size() != 0){ 
				copyFeatures(seasonId, subFeatures, parent);
			}

			
		} 
		
	
	}
	

	
	private JSONObject removeFields(JSONObject feature){
		feature.remove("uniqueId");
		feature.remove("seasonId");
		feature.remove("rolloutPercentageBitmap");
		feature.remove("lastModified");
		feature.remove("creationDate");
		feature.remove("features");
		feature.remove("configurationRules");
		return feature;
	}
	
	
	private JSONArray getFeaturesToCopy() throws JSONException{
		JSONArray jsonArray = new JSONArray();
		//for action addSubFeatures
		if (action.equals(ADD_SUB_FEATURES)){
			jsonArray.add(seasonJson);
		} else {
		//for all other actions
			jsonArray = seasonJson.getJSONObject("root").getJSONArray("features");
		}
		JSONArray featuresToCopy = new JSONArray();
		if (!features.toLowerCase().equals(ALL_FEATURES)){				//a list of specific features
			String[] f = features.split(",");
			listOfFeatures=Arrays.asList(f);

			for (int i=0; i < jsonArray.length(); i++){
				JSONObject feature = new JSONObject(jsonArray.getJSONObject(i));				
				if (feature.containsKey("name")) {
					if (listOfFeatures.contains(feature.getString("name"))){
						featuresToCopy.add(feature);
					}
				}	
			}
			return featuresToCopy;
		} else {
			return jsonArray;		//all features in season
		}

		
	}
	

	
	private void createUserGroups(String[] newGroups) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(url + "/usergroups", sessionToken);
		String response = res.message;
		JSONArray currentGroups = new JSONObject(response).getJSONArray("internalUserGroups");
		List<String> currGroupsList = new ArrayList<String>();
		for (int i=0; i<currentGroups.length(); i++){
			currGroupsList.add(currentGroups.getString(i));
		}
		
		List<String> allGroups = new ArrayList<String>();
		allGroups = Arrays.asList(newGroups);
		
		for (String group : allGroups){
			if (!currGroupsList.contains(group)) {
				currGroupsList.add(group);
			} 
		}

 
		JSONObject json = new JSONObject(response);
		json.remove("internalUserGroups");
		
		json.put("internalUserGroups", currGroupsList);
		
		try {
			RestClientUtils.sendPut( url + "/usergroups", json.toString(), sessionToken);
		} catch (Exception e){
			System.out.println("Could not update user groups. Some feature may not be created");
		}
		
	
	}
	
	
	private static String[] toStringArray(JSONArray array) {
	    if(array==null)
	        return null;

	    String[] arr=new String[array.length()];
	    for(int i=0; i<arr.length; i++) {
	        arr[i]=array.optString(i);
	    }
	    return arr;
	}
	
	
	private  void createAssets(String seasonID) throws Exception{
		
		
		if (strings.equals("")){
			System.out.println("Strings file was not provided - creating features without strings");
		} else if (translationsUrl.equals("")){
			System.out.println("Cannot proceed adding strings: translationsUrl parameter was not provided.");
			return;
		} else {			
			String response = "";
			
			String stringsBody = readString(strings);
			JSONObject json = new JSONObject(stringsBody);
			
			JSONArray allStrings = json.getJSONArray("strings");
			
			for(int i=0; i <allStrings.size(); i++ ){
				JSONObject singleStr = allStrings.getJSONObject(i);
				singleStr = removeFields(singleStr);

				try {
					RestClientUtils.RestCallResults res =RestClientUtils.sendPost(translationsUrl +"seasons/" + seasonID + "/strings", singleStr.toString(), sessionToken);
					response = res.message;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("Failed to create string:" + singleStr.getString("key") + "\r\n" + e.getLocalizedMessage());
					return;
				}
			}	
		}
		
		
		
		if (utilities.equals("")){
			System.out.println("Utilities file was not provided - using default utilites");
		} else {
			/*Properties utilityProps = SeasonCopyProperties.getPropValues(utilities);
			String minAppVersion = utilityProps.getProperty("minAppVersion");
			String stage = utilityProps.getProperty("stage");
			String utility = utilityProps.getProperty("utility"); 
			*/
			//String utility = readFile(utilities);			
			String utility = readString(utilities);
			
			try {
				RestClientUtils.RestCallResults res = RestClientUtils.sendPost(url +"/products/seasons/" + seasonID + "/utilities?stage="+utilityStage+"&minappversion="+utilityMinVersion, utility, sessionToken);
				String response = res.message;
				
			} catch (IOException e) {
				System.out.println("An exception was thrown when trying  to add a utility. Message: "+e.getLocalizedMessage()) ;
				return;
			}


		}
		
	
		if (inputSchema.equals("")){
			System.out.println("inputSchema file was not provided - creating features without inputSchema");
		} else {
			String schemaBody = readString(inputSchema);
			String response = "";
			RestClientUtils.RestCallResults res =RestClientUtils.sendGet(url+"/products/seasons/" + seasonID + "/inputschema", sessionToken);
			String schema = res.message;
			JSONObject schemaJson = new JSONObject(schema);
			schemaJson.put("inputSchema", new JSONObject(schemaBody));
			try {
				res = RestClientUtils.sendPut(url+"/products/seasons/" + seasonID + "/inputschema", schemaJson.toString(), sessionToken);
				response = res.message;
				if (response.contains("error")){
					System.out.println("Failed to create inputSchema. Response code: " + response);
					return;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Failed to create inputSchema. Response code: " + response + "\r\n" + e.getLocalizedMessage());
				return;
			}
		}
		
		
			
			if (!translations.equals("")){
			String translationBody = readString(translations);
			String response="";
			try {
				RestClientUtils.RestCallResults res =RestClientUtils.sendPost(translationsUrl +"/" + seasonID + "/translate/" + locale, translationBody, sessionToken);
				response = res.message;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Failed to create translations. Response code: " + response + "\r\n" + e.getLocalizedMessage());
				return;
			}
		}	
		

	}
	

}
