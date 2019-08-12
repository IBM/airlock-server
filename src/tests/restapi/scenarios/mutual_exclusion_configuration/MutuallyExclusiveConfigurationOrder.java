package tests.restapi.scenarios.mutual_exclusion_configuration;

import java.io.IOException;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;


public class MutuallyExclusiveConfigurationOrder {
	protected String seasonID;
	protected String seasonID2;
	protected String parentID;
	protected String childID1;
	protected String childID2;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected SeasonsRestApi s;	
	protected String productID;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
 		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		String parent = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, parent, "ROOT", sessionToken);
	}
	
	/**
	 * Create mutually exclusive group
	 */
	@Test (description = "Create mutually exclusive group of configuration rules")
	public void testCreateMutuallyExclusiveFromJson() throws JSONException{
		try {
			String parent = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
			parentID = f.addFeature(seasonID, parent, featureID, sessionToken);
			
			String child1 = FileUtils.fileToString(filePath+"configuration_rule1.txt", "UTF-8", false);
			childID1 = f.addFeature(seasonID, child1, parentID, sessionToken);
			String child2 = FileUtils.fileToString(filePath+"configuration_rule2.txt", "UTF-8", false);
			childID2 = f.addFeature(seasonID, child2, parentID, sessionToken);
			
			parent = f.getFeature(parentID, sessionToken);
			JSONObject json = new JSONObject(parent);
			JSONArray children = json.getJSONArray("configurationRules");
			if (children.size() == 2){
				Assert.assertEquals(children.getJSONObject(0).get("uniqueId"), childID1, "The first child is incorrect");
				Assert.assertEquals(children.getJSONObject(1).get("uniqueId"), childID2, "The second child is incorrect");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Test (dependsOnMethods="testCreateMutuallyExclusiveFromJson", description = "Change configuration rules order in mutually exclusive group")
	public void testChangeOrder() throws JSONException, IOException{
		String parent = f.getFeature(parentID, sessionToken);
		JSONObject json = new JSONObject(parent);
		json.remove("configurationRules");
		JSONArray children = new JSONArray();
		JSONObject child2 = new JSONObject(f.getFeature(childID2, sessionToken));
		children.put(child2);
		JSONObject child1 = new JSONObject(f.getFeature(childID1, sessionToken));
		children.put(child1);
		json.put("configurationRules", children);
		String response = f.updateFeature(seasonID, parentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "MIX was not updated after features reorder"+response);
		
		parent = f.getFeature(parentID, sessionToken);
		json = new JSONObject(parent);
		children = json.getJSONArray("configurationRules");
		if (children.size() == 2){
			Assert.assertEquals(children.getJSONObject(0).get("uniqueId"), childID2, "The first child is incorrect");
			Assert.assertEquals(children.getJSONObject(1).get("uniqueId"), childID1, "The second child is incorrect");
		}

	}
	
	@Test (dependsOnMethods="testChangeOrder", description = "Create a new season")
	public void createSeason() throws IOException{
		String season = "{\"minVersion\":\"7.5\"}";
		seasonID2 = s.addSeason(productID, season, sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The second season was not created" + seasonID2);
	}
	
	@Test (dependsOnMethods="createSeason", description = "Change configuration rules order in the first season")
	public void testChangeOrderFirstSeason() throws JSONException, IOException{
		String parent = f.getFeature(parentID, sessionToken);
		JSONObject json = new JSONObject(parent);
		json.remove("configurationRules");
		JSONArray children = new JSONArray();
		JSONObject child2 = new JSONObject(f.getFeature(childID2, sessionToken));
		JSONObject child1 = new JSONObject(f.getFeature(childID1, sessionToken));
		children.put(child1);
		children.put(child2);
		json.put("configurationRules", children);
		String response = f.updateFeature(seasonID, parentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "MIX was not updated after features reorder"+response);
		
		parent = f.getFeature(parentID, sessionToken);
		json = new JSONObject(parent);
		children = json.getJSONArray("configurationRules");
		if (children.size() == 2){
			Assert.assertEquals(children.getJSONObject(0).get("uniqueId"), childID1, "The first child is incorrect");
			Assert.assertEquals(children.getJSONObject(1).get("uniqueId"), childID2, "The second child is incorrect");
		}

	}
	
	@Test (dependsOnMethods="testChangeOrderFirstSeason", description = "Change configuration rules order in the second season")
	public void testChangeOrderSecondSeason() throws JSONException, IOException{
		JSONArray features = f.getFeaturesBySeason(seasonID2, sessionToken);
		JSONObject parentFeature = features.getJSONObject(0);
		JSONObject parentMIX = parentFeature.getJSONArray("configurationRules").getJSONObject(0);
		String parentID2 = parentMIX.getString("uniqueId");
		
		
		String parent = f.getFeature(parentID2, sessionToken);
		JSONObject json = new JSONObject(parent);
		

		JSONArray children = new JSONArray();
		JSONObject child2 = json.getJSONArray("configurationRules").getJSONObject(0);
		JSONObject child1 = json.getJSONArray("configurationRules").getJSONObject(1);
		children.put(child1);
		children.put(child2);
		json.remove("configurationRules");
		json.put("configurationRules", children);
		String response = f.updateFeature(seasonID2, parentID2, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "MIX was not updated after features reorder"+response);
		
		parent = f.getFeature(parentID, sessionToken);
		json = new JSONObject(parent);
		children = json.getJSONArray("configurationRules");
		if (children.size() == 2){
			Assert.assertEquals(children.getJSONObject(0).get("uniqueId"), childID1, "The first child is incorrect");
			Assert.assertEquals(children.getJSONObject(1).get("uniqueId"), childID2, "The second child is incorrect");
		}

	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
