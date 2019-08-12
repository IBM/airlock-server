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


public class AddAndDeleteMutuallyExclusiveConfigurationRule {
	protected String seasonID;
	protected String parentID;
	protected String childID1;
	protected String childID2;
	protected String featureID;
	protected String filePath;
	protected String mutualGroupFile;
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
	@Test (description = "Create and validate mutually exclusive group with 2 features ")
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
			Assert.assertEquals(children.size(), 2, "Sub-features were not assigned to a mutually exclusive parent.");

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Test (dependsOnMethods="testCreateMutuallyExclusiveFromJson", description = "Delete one feature from mutually exclusive group ")
	public void testDeleteChild() throws JSONException{
		int response = f.deleteFeature(childID2, sessionToken);
		Assert.assertEquals(response, 200, "Child feature was not removed.");
		String parent = f.getFeature(parentID, sessionToken);
		JSONObject json = new JSONObject(parent);
		JSONArray children = json.getJSONArray("configurationRules");
		Assert.assertEquals(children.size(), 1, "Sub-feature was not removed from the list of mutual exclusion");


	}

	@Test (dependsOnMethods="testDeleteChild", description = "Delete mutually exclusive group with its sub-feature ")
	public void testDeleteParent() throws JSONException{
		int response = f.deleteFeature(parentID, sessionToken);
		Assert.assertEquals(response, 200, "Parent feature was not removed.");
		String testChild = f.getFeature(childID1, sessionToken);
		//TODO; should not look at the error string but at the response code that should be 404
		Assert.assertEquals(testChild, "{\"error\":\"Airlock item not found.\"}"/*"404"*/, "Sub-feature was not removed from the list of mutual exclusion");

	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
