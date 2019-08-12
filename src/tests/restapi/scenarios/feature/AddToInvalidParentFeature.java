package tests.restapi.scenarios.feature;

import java.io.IOException;

import org.apache.wink.json4j.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;


public class AddToInvalidParentFeature {
	protected String seasonID;
	protected String productID;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		if (sToken != null)
			sessionToken = sToken;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}



	/**
	 * Add feature to non-existing parent id
	 * 
	 */
	@Test (description = "Add feature to a non-existing parent id")
	public void testAddNonExistingParent() throws JSONException{
		try {
			String feature = FileUtils.fileToString(filePath+"feature1.txt", "UTF-8", false);
			String response = f.addFeature(seasonID, feature, "e2d4efc6-90cf-40f7-a4aa-111aaa11a1a1", sessionToken);
			 Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Add feature to null parent
	 * 
	 */
	@Test (description  = "Add feature to null parent")
	public void testAddNullParent() throws JSONException{
		try {
			String feature = FileUtils.fileToString(filePath+"feature1.txt", "UTF-8", false);
			String response = f.addFeature(seasonID, feature, null, sessionToken);
			 Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Add feature to empty parent
	 * 
	 */
	@Test (description = "Add feature to empty parent")
	public void testAddEmptyParent() throws JSONException{
		try {
			String feature = FileUtils.fileToString(filePath+"feature1.txt", "UTF-8", false);
			String response = f.addFeature(seasonID, feature, "", sessionToken);
			 Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Add feature to empty parent
	 * 
	 */
	@Test (description = "Add feature with non-empty features field array")
	public void testAddParentWithNonEmptyFeatures() throws JSONException{
		try {
			String feature = FileUtils.fileToString(filePath+"invalid-parent-feature.txt", "UTF-8", false);
			String response = f.addFeature(seasonID, feature, "ROOT", sessionToken);
			 Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@AfterTest
	private void reset(){
		baseUtils.reset( productID, sessionToken);

	}
}
