package tests.restapi.scenarios.stream_utilities;

import java.io.File;

import java.io.IOException;




import java.io.StringReader;
import java.util.Properties;

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
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.UtilitiesRestApi;

public class StreamUtilitiesInRuntime {
	protected String seasonID;
	protected String devFeatureID;
	protected String prodFeatureID;
	protected String filePath;
	protected String utilityIDDev;
	protected String utilityIDProd;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected UtilitiesRestApi u;
	private FeaturesRestApi f;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		p = new ProductsRestApi();
		s = new SeasonsRestApi();
		u = new UtilitiesRestApi();
		f = new FeaturesRestApi();
		s.setURL(m_url);
		u.setURL(m_url);
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	


	@Test (description="Create a utiltiy in development")
	public void createUtilityInDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilityIDDev = u.addUtility(seasonID, utilProps, UtilitiesRestApi.STREAM_UTILITY, sessionToken);
		Assert.assertFalse(utilityIDDev.contains("error"), "Test should pass, but instead failed: " + utilityIDDev );
		
		
		f.setSleep();
		//check if files were changed
		RuntimeRestApi.DateModificationResults responseDevStream = RuntimeDateUtilities.getDevelopmentStreamUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDevStream.code ==200, "Runtime development stream utility file was not updated");
		String utlitiesList = responseDevStream.message;
		Assert.assertTrue(utlitiesList.contains("isTrue()"), "New utility doesn't appear in the stream development  file");
		RuntimeRestApi.DateModificationResults responseProdStream = RuntimeDateUtilities.getProductionStreamUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProdStream.code ==304, "Runtime production stream utility file was changed");
		
		//regular utilites not changed
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development utility file was updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production utility file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods = "createUtilityInDev", description="Create a utility in production")
	public void createUtilityInProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility2.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilityIDProd = u.addUtility(seasonID, utilProps, UtilitiesRestApi.STREAM_UTILITY, sessionToken);
		Assert.assertFalse(utilityIDProd.contains("error"), "Test should pass, but instead failed: " + utilityIDProd );
		
		//check if files were changed
		f.setSleep();
		
		RuntimeRestApi.DateModificationResults responseDevStream = RuntimeDateUtilities.getDevelopmentStreamUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDevStream.code ==200, "Runtime development stream utility file was not updated");
		RuntimeRestApi.DateModificationResults responseProdStream = RuntimeDateUtilities.getProductionStreamUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProdStream.code ==200, "Runtime production stream utility file was not updated");

		//regular utilites not changed
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development utility file was updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production utility file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		
		//check development utilities file content
		String utlitiesList = responseDevStream.message;
		Assert.assertTrue(utlitiesList.contains("isFalse()"), "New utility doesn't appear in the development stream file");
		utlitiesList = responseProdStream.message;
		Assert.assertTrue(utlitiesList.contains("isFalse()"), "New utility doesn't appear in the production stream file");
	}
	
	@Test (dependsOnMethods = "createUtilityInProd", description="Move utility from dev to production")
	public void moveUtilityToProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();

		String utility = u.getUtility(utilityIDDev, sessionToken);
		JSONObject uJson = new JSONObject(utility);
		uJson.put("stage", "PRODUCTION");
		String response  =u.updateUtility(utilityIDDev, uJson, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDevStream = RuntimeDateUtilities.getDevelopmentStreamUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDevStream.code ==200, "Runtime development stream utility file was not updated");
		RuntimeRestApi.DateModificationResults responseProdStream = RuntimeDateUtilities.getProductionStreamUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProdStream.code ==200, "Runtime production stream utility file was not changed");

		//regular utilites not changed
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development utility file was updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production utility file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		
		//check development utilities file content
		String utlitiesList = responseDevStream.message;
		Assert.assertTrue(utlitiesList.contains("isTrue()"), "Updated utility doesn't appear in the development stream file");
		utlitiesList = responseProdStream.message;
		Assert.assertTrue(utlitiesList.contains("isTrue()"), "Updated utility doesn't appear in the production stream file");
	}
	
	@Test (dependsOnMethods = "moveUtilityToProd", description="Update utility in production")
	public void updateUtilityInProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String utility = u.getUtility(utilityIDDev, sessionToken);
		JSONObject uJson = new JSONObject(utility);
		uJson.put("utility", "function isTrue(){return \"updated utility\";}");
		String response  =u.updateUtility(utilityIDDev, uJson, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDevStream = RuntimeDateUtilities.getDevelopmentStreamUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDevStream.code ==200, "Runtime development stream utility file was not updated");
		RuntimeRestApi.DateModificationResults responseProdStream = RuntimeDateUtilities.getProductionStreamUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProdStream.code ==200, "Runtime production stream utility file was not changed");

		//regular utilites not changed
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development utility file was updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production utility file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		
		//check development utilities file content
		String utlitiesList = responseDevStream.message;
		Assert.assertTrue(utlitiesList.contains("updated utility"), "Updated utility doesn't appear in the development streams file");
		utlitiesList = responseProdStream.message;
		Assert.assertTrue(utlitiesList.contains("updated utility"), "Updated utility doesn't appear in the production streams file");

	}
	
	@Test (dependsOnMethods = "updateUtilityInProd", description="Move utility from production to development")
	public void moveUtilityToDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String utility = u.getUtility(utilityIDDev, sessionToken);
		JSONObject uJson = new JSONObject(utility);
		uJson.put("stage", "DEVELOPMENT");
		String response  =u.updateUtility(utilityIDDev, uJson, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDevStream = RuntimeDateUtilities.getDevelopmentStreamUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDevStream.code ==200, "Runtime development stream utility file was not updated");
		RuntimeRestApi.DateModificationResults responseProdStream = RuntimeDateUtilities.getProductionStreamUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProdStream.code ==200, "Runtime production stream utility file was not changed");

		//regular utilites not changed
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development utility file was updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production utility file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		
		//check development utilities file content
		String utlitiesList = responseDevStream.message;
		Assert.assertTrue(utlitiesList.contains("isTrue()"), "Updated utility doesn't appear in the development streams file");
		utlitiesList = responseProdStream.message;
		Assert.assertFalse(utlitiesList.contains("isTrue()"), "Updated utility appears in the production streams file");
	}
	
	@Test (dependsOnMethods = "moveUtilityToDev", description="Update utility in development")
	public void updateUtilityInDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String utility = u.getUtility(utilityIDDev, sessionToken);
		JSONObject uJson = new JSONObject(utility);
		uJson.put("utility", "function isTrue(){return \"second update\";}");
		String response  =u.updateUtility(utilityIDDev, uJson, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDevStream = RuntimeDateUtilities.getDevelopmentStreamUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDevStream.code ==200, "Runtime development stream utility file was not updated");
		RuntimeRestApi.DateModificationResults responseProdStream = RuntimeDateUtilities.getProductionStreamUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProdStream.code ==304, "Runtime production stream utility file was changed");

		//regular utilites not changed
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development utility file was updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production utility file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
	
		//check development utilities file content
		String utlitiesList = responseDevStream.message;
		Assert.assertTrue(utlitiesList.contains("second update"), "Updated utility doesn't appear in the development streams file");

	}
	
	@Test (dependsOnMethods = "updateUtilityInDev", description="Delete utility in development")
	public void deleteUtilityInDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();

		int response  =u.deleteUtility(utilityIDDev, sessionToken);
		Assert.assertTrue(response==200, "Utility in development was not deleted" );
	
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDevStream = RuntimeDateUtilities.getDevelopmentStreamUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDevStream.code ==200, "Runtime development stream utility file was not updated");
		RuntimeRestApi.DateModificationResults responseProdStream = RuntimeDateUtilities.getProductionStreamUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProdStream.code ==304, "Runtime production stream utility file was changed");
			
		//check development utilities file content
		String utlitiesList = responseDevStream.message;
		Assert.assertFalse(utlitiesList.contains("second update"), "Deleted utility appear in the development file");

		//regular utilites not changed
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development utility file was updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production utility file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
	
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
