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


public class AddMutuallyExclusiveConfigurationWithFeature {
	protected String seasonID;
	protected String parentID;
	protected String childID1;
	protected String childID2;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
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
	@Test (description = "Create mutually exclusive group of configuration rules and add it a regular feature - not allowed")
	public void createMutuallyExclusiveWithFeature() throws JSONException{
		try {
			String parent = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
			parentID = f.addFeature(seasonID, parent, featureID, sessionToken);
			
			String child1 = FileUtils.fileToString(filePath+"configuration_rule1.txt", "UTF-8", false);
			childID1 = f.addFeature(seasonID, child1, parentID, sessionToken);
			String child2 = FileUtils.fileToString(filePath+"feature2.txt", "UTF-8", false);
			
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


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
