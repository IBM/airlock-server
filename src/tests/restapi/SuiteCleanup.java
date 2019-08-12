package tests.restapi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import tests.com.ibm.qautils.RestClientUtils;

public class SuiteCleanup {

	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private AirlocklNotificationRestApi notifApi;
	protected StreamsRestApi streamApi;
	
	private ExperimentsRestApi experimentApi;
	private String sessionToken = "";
	private String productsToDelete; 
	private boolean deleteAll = false;
	private String excludeProductsDeleteList;
	private AirlockUtils baseUtils;
	
	@AfterSuite
	@Parameters({"url", "analyticsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "excludeProductsDelete", "deleteAll", "productsToDeleteFile"})
	public void cleanup(String url, String analyticsUrl, String configPath, @Optional String sToken, String userName, String userPassword, String appName, @Optional String excludeProductsDelete, @Optional String deleteAllProducts, String productsToDeleteFile) throws IOException{
		m_url = url;
		productsToDelete = productsToDeleteFile;
		baseUtils = new AirlockUtils(m_url, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);	
		sessionToken = baseUtils.sessionToken;

		if (deleteAllProducts != null)
			deleteAll = Boolean.parseBoolean(deleteAllProducts);
		
		if (excludeProductsDelete != null)
			excludeProductsDeleteList = excludeProductsDelete;
		
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		experimentApi = new ExperimentsRestApi();
		experimentApi.setURL(analyticsUrl);
		notifApi = new AirlocklNotificationRestApi();
		notifApi.setUrl(url);
		streamApi = new StreamsRestApi();
		streamApi.setURL(url);
		
		if(deleteAll)
			deleteAllProducts();
		else
			deleteByList();
		
		/*
		try {
			deleteUserGroups();
		} catch (Exception e) {
			System.out.println("Can't delete user groups: " + e.getLocalizedMessage());
		}
		*/
		
		System.clearProperty("seasonVersion");
				
	}
	
	public void deleteUserGroups() throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/usergroups",sessionToken);
		String userGroups = res.message;
		JSONObject json = new JSONObject(userGroups);
		JSONArray groups = new JSONArray();
		groups.put("QA");
		groups.put("DEV");
		json.put("internalUserGroups", groups);
		RestClientUtils.sendPut(m_url + "/usergroups", json.toString(), sessionToken);
	}
	
	private void deleteProduct(String productID) throws Exception{
		String prod = p.getProduct(productID, sessionToken);		
		
		//check that product exists
		JSONObject product = new JSONObject();
		try {
			product = new JSONObject(prod);
			if (product.containsKey("error"))
				return; //product does not exist
					
		} catch(Exception e){
			//product doesn't exist
			return;
		}
		
		System.out.println("Working on product: " + product.getString("name"));
		
		//convert experiments and variants to development stage
		if (product.containsKey("experiments")) {
			if (product.getJSONObject("experiments").getJSONArray("experiments") != null &&product.getJSONObject("experiments").getJSONArray("experiments").size() > 0)
			findExperimentsInProduction(product);
		}
		
		//convert features and configurations to development stage
		try {
			JSONArray seasons = product.getJSONArray("seasons");
			if (seasons !=null && seasons.size() > 0) {
				for (int i=0; i<seasons.size(); i++){
					//for each season get a list of features
					JSONObject season = seasons.getJSONObject(i);
					String seasonId = season.getString("uniqueId");
					JSONArray features = f.getFeaturesBySeason(seasonId, sessionToken);
					findFeatureInProduction(seasonId, features);	//convert all features in production to development
					
					
					//convert notifications to development					
					String allNotif = notifApi.getAllNotifications(seasonId, sessionToken);
					JSONObject allNotifJson = new JSONObject(allNotif);
					
					JSONArray notifArr = allNotifJson.getJSONArray("notifications");
					if (notifArr!=null) {
						for (int j=0; j<notifArr.length(); j++) {
							JSONObject notificationJSON = notifArr.getJSONObject(j);
							if (notificationJSON.getString("stage").equals("PRODUCTION")) {
								notificationJSON.put("stage", "DEVELOPMENT");
								notifApi.updateNotification(notificationJSON.getString("uniqueId"), notificationJSON.toString(), sessionToken);
							}
						}
					}
					
					//convert streams to development
					String allStreams = streamApi.getAllStreams(seasonId, sessionToken);
					JSONObject allStreamsJson = new JSONObject(allStreams);
					JSONArray streamsArr = allStreamsJson.getJSONArray("streams");
					if (streamsArr!=null) {
						for (int j=0; j<streamsArr.length(); j++) {
							JSONObject streamJSON = streamsArr.getJSONObject(j);
							if (streamJSON.getString("stage").equals("PRODUCTION")) {
								streamJSON.put("stage", "DEVELOPMENT");
								streamApi.updateStream(streamJSON.getString("uniqueId"), streamJSON.toString(), sessionToken);
							}
						}
					}
				}
			}
		} catch (JSONException je) {
			System.out.println("Product: " + productID + " Error: " + je.getLocalizedMessage());
		}
		
		
		System.out.println("Deleting product: " + product.getString("name"));
		int response = p.deleteProduct(productID, sessionToken);
		if (response != 200)
			System.out.println("Cannot delete product: " + product.getString("name") + " " + productID);
	}
	
	
	private void findFeatureInProduction(String seasonId, JSONArray features) throws JSONException, IOException{
		for (int j=0; j<features.size(); j++){
			JSONObject feature = features.getJSONObject(j);
			String temp = f.getFeature(feature.getString("uniqueId"), sessionToken);
				feature = new JSONObject(temp);

					
				JSONArray configurationRules = new JSONArray();
				if (feature.containsKey("configurationRules"))
					configurationRules = feature.getJSONArray("configurationRules");
				
				if (configurationRules.size() != 0) {
					findFeatureInProduction(seasonId, configurationRules);				
				}
				
				JSONArray orderingRules = new JSONArray();
				if (feature.containsKey("orderingRules"))
					orderingRules = feature.getJSONArray("orderingRules");
				
				if (orderingRules.size() != 0) {
					findFeatureInProduction(seasonId, orderingRules);				
				}

				
				JSONArray subFeatures = new JSONArray();
				if (feature.containsKey("features"))
					subFeatures = feature.getJSONArray("features");				
				if (subFeatures.size() != 0) {
					findFeatureInProduction(seasonId, subFeatures);
				
				}
				if (feature.has("stage") && feature.getString("stage").equals("PRODUCTION")){
					//change stage to development
					String updatedFeature = f.getFeature(feature.getString("uniqueId"), sessionToken);
					feature = new JSONObject(updatedFeature);
					String featureId = feature.getString("uniqueId");
					feature.put("stage", "DEVELOPMENT");
					f.updateFeature(seasonId, featureId, feature.toString(), sessionToken);
				}
		}	
	}
	
	private void deleteAllProducts(){
		String productsList = p.getAllProducts(sessionToken);
		List<String> excludeProducts = new ArrayList<String>() ;
		if(excludeProductsDeleteList != null && excludeProductsDeleteList != "" ) {
			String[] tempProduct = excludeProductsDeleteList.split(",");
			for(int i=0; i<tempProduct.length; i++)
				tempProduct[i] = tempProduct[i].trim();
			excludeProducts=Arrays.asList(tempProduct);
		}	

		try {
			System.out.println("Start cleaning the test environment...");
			JSONObject json = new JSONObject(productsList);
			JSONArray products = json.getJSONArray("products");
			for (int i = 0; i<products.size(); i++){
				JSONObject product = products.getJSONObject(i); // !excludeProducts.contains(product.getString("name")
				
				if (product.containsKey("name") &&  !excludeProducts.contains(product.getString("name"))) {
					String productID = product.getString("uniqueId");
					int response = p.deleteProduct(productID, sessionToken);
					if (response != 200) {
						try {
							deleteProduct(productID);
						} catch (Exception e) {
							System.out.println("Can't delete product " + product.getString("name") + ": " + e.getLocalizedMessage());
						}
					}	
				}
			}
		} catch (JSONException e) {
			System.out.println("Can't clean the environment after tests: " + e.getLocalizedMessage());
		}
		
	}
	
	
	private void deleteByList() throws IOException{

		
		
		File file = new File(productsToDelete); 
		if (file.exists()) {
			
			FileInputStream fis = new FileInputStream(productsToDelete);
			 
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		 
			String productID = null;
			while ((productID = br.readLine()) != null) {
				try {
				//first get product and check if uniqueId exists, only then delete
					deleteProduct(productID);
				} catch (Exception e) {
					System.out.println("Can't delete product " + productID + ": " + e.getLocalizedMessage());
				}
			}
		 
			br.close();
			
			file.delete();
		}
		
		
		
	}
	
	private void findExperimentsInProduction(JSONObject product) throws JSONException{
		try {
		JSONArray experiments = product.getJSONObject("experiments").getJSONArray("experiments");
			if (experiments !=null && experiments.size() > 0) {
				for (int i=0; i<experiments.size(); i++){
					JSONObject experiment = experiments.getJSONObject(i);
					String experimentId = experiment.getString("uniqueId");
					JSONArray variants = experiment.getJSONArray("variants");
					//find variants in production and update them to development
					for (int k=0; k<variants.size(); k++){
						JSONObject variant = variants.getJSONObject(k);
						
						if (variant.getString("stage").equals("PRODUCTION")){	//if variant stage=production, convert to development
							String variantId = variant.getString("uniqueId");
							String var = experimentApi.getVariant(variantId, sessionToken);
							JSONObject jsonVar = new JSONObject(var);
							jsonVar.put("stage", "DEVELOPMENT");
							String response = experimentApi.updateVariant(variantId, jsonVar.toString(), sessionToken);
						}
					}
					
					//if experiment stage=production, convert to development
					if (experiment.getString("stage").equals("PRODUCTION")){
						String exp = experimentApi.getExperiment(experimentId, sessionToken);
						JSONObject jsonExp = new JSONObject(exp);
						jsonExp.put("stage", "DEVELOPMENT");
						jsonExp.put("enabled", false);
						String response = experimentApi.updateExperiment(experimentId, jsonExp.toString(), sessionToken);
					}
				}
			}
		} catch (Exception e){
			System.out.println("Product: " + product.getString("uniqueId") + " Error: " + e.getLocalizedMessage());
		}
	}
	
}
