package tests.restapi.scenarios.mutual_exclusion_features;

import java.io.IOException;

import java.util.List;

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

public class UpdateMutuallyExclusiveToRoot {

	protected String productID;
	protected String seasonID;
	protected String featureID1;
	protected String featureID2;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String feature;
	protected List<Integer> actualResult;
	protected String childID;
	protected String m_url;
	private String sessionToken = "";


	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

		f = new FeaturesRestApi();
		f.setURL(m_url);
		feature = FileUtils.fileToString(configPath + "feature-mutual.txt", "UTF-8", false);
		featureID1 = f.addFeature(seasonID, feature, "ROOT", sessionToken); //parent feature1
		feature = FileUtils.fileToString(configPath + "feature-mutual.txt", "UTF-8", false);
		featureID2 = f.addFeature(seasonID, feature, "ROOT", sessionToken);	//parent feature2
		feature = FileUtils.fileToString(configPath + "feature1.txt", "UTF-8", false);
		childID = f.addFeature(seasonID, feature, featureID2, sessionToken); // add child to feature1
	}

	/*
	 * This test validates that a feature can be moved to a different mutually exclusive group
	 */
	@Test (description = "This test validates that a feature can be moved to a different mutually exclusive group")
	public void addFeature() throws IOException, JSONException {
		//move child from parent2 to parent1
		feature = f.getFeature(featureID1, sessionToken);
		JSONArray children = new JSONArray();
		String child = f.getFeature(childID, sessionToken);
		JSONObject json = new JSONObject(child);
		children.add(json);
		JSONObject obj = new JSONObject(feature);
		obj.put("features", children);
		String response = f.updateFeature(seasonID, featureID1, obj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}

	@Test (dependsOnMethods = "addFeature", description="check that the child was added to parent1 and removed from parent2")
	public void validateParents() throws JSONException{
		//check that the child was added to parent1 and removed from parent2
		String parent = f.getFeature(featureID1, sessionToken);
		JSONObject json = new JSONObject(parent);
		JSONArray children = json.getJSONArray("features");
		Assert.assertEquals(children.size(), 1, "A child was not added to parent1");

		parent = f.getFeature(featureID2, sessionToken);
		json = new JSONObject(parent);
		children = json.getJSONArray("features");
		Assert.assertEquals(children.size(), 0, "A child was not removed from parent2");
	}

	/*
	 * This test validates that a feature can be moved from a mutually exclusive group to the ROOT
	 */

	@Test (dependsOnMethods = "validateParents", description = "This test validates that a feature can be moved from a mutually exclusive group to the ROOT")
	public void moveToRoot() throws Exception{
		//move child from parent1 to root
		String parent1 = f.getFeature(featureID1, sessionToken);
		JSONObject jsonParent1 = new JSONObject(parent1);
		jsonParent1.put("features", new JSONArray());

		String child = f.getFeature(childID, sessionToken);
		String parent2 = f.getFeature(featureID2, sessionToken);
		JSONObject childJson = new JSONObject(child);
		JSONObject jsonParent2 = new JSONObject(parent2);

		//retrieve season's root uniqueId and a list of features
		//retrieve season's root uniqueId and a list of features
		String rootUniqueId = f.getRootId(seasonID, sessionToken);
		String root = f.getFeature(rootUniqueId, sessionToken);
		JSONObject seasonJson = new JSONObject(root);
		JSONArray newFeatures = new JSONArray();
		newFeatures.put(jsonParent1);
		newFeatures.put(jsonParent2);
		newFeatures.put(childJson);
		seasonJson.put("features", newFeatures);


		String response = f.updateFeature(seasonID, rootUniqueId, seasonJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}

	@Test (dependsOnMethods = "moveToRoot", description = "Check that the feature was moved from MIX to ROOT")
	public void validateRoot() throws JSONException{
		String parent1 = f.getFeature(featureID1, sessionToken);
		JSONObject json = new JSONObject(parent1);
		JSONArray children = json.getJSONArray("features");
		Assert.assertEquals(children.size(), 0, "A child was not removed from parent2");

		JSONArray rootFeatures = f.getFeaturesBySeason(seasonID, sessionToken);
		Assert.assertEquals(rootFeatures.size(), 3, "Root children are incorrect");
	}


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
