package tests.restapi.proddevsplit;

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
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;

public class SeasonChangeVersions {
	protected String seasonID1;
	protected String seasonID2;
	protected String featureID1;
	protected String featureInSeason2;
	protected String productID;
	protected String config;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected AirlockUtils baseUtils;
	private String sessionToken = "";
	protected String m_url;


	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		config = configPath;
		p = new ProductsRestApi();
		s = new SeasonsRestApi();
		f = new FeaturesRestApi();
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		p.setURL(url);
		s.setURL(url);
		f.setURL(url);


	}
	

	@Test (description = "Add product in production stage and season1 with minVer=0.8")
	public void addComponents() throws Exception{
		String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "name");
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		productID = p.addProduct(product, sessionToken);		
		baseUtils.printProductToFile(productID);
		
		String season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
		seasonID1 = s.addSeason(productID, season, sessionToken);
		Assert.assertFalse(seasonID1.contains("error"), "Test should pass, but instead failed: " + seasonID1 );
		
		String feature1 = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature1);
		json.put("stage", "PRODUCTION");
		String response  = f.addFeature(seasonID1, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	
	
		
	/* create season2 with minVer=1.4:
	 * season1MinVer = 0.8, season1MaxVer=1.4
	 * season2MinVer = 1.4, season1MaxVer=?
	 * 
	 */
	@Test (dependsOnMethods="addComponents", description ="Add season2 with minVer=1.4")
	public void createSecondSeason() throws Exception{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID1, sessionToken);
		
		String season2 = FileUtils.fileToString(config + "season2.txt", "UTF-8", false);
		seasonID2 = s.addSeason(productID, season2, sessionToken);

		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");	
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID2, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");
		
		Assert.assertTrue(validateSeasonVersionNumbersInProduction(dateFormat), "Incorrect season min/maxVersion in production file");
		Assert.assertTrue(validateSeasonVersionNumbersInDevelopment(dateFormat), "Incorrect season min/maxVersion in development file");
	}
	

	
	// updating minVersion updates prev season's maxVersion (if not first season)
	@Test (dependsOnMethods="createSecondSeason", description = "updating minVersion updates prev season's maxVersion")
	public void updateMinVersion() throws Exception{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID2, sessionToken);
		
		String season2 = s.getSeason(productID, seasonID2, sessionToken);
		JSONObject season2Json = new JSONObject(season2);
		season2Json.put("minVersion", "1.5");
		String response = s.updateSeason(seasonID2, season2Json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");	
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID2, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");
		
		Assert.assertTrue(validateSeasonVersionNumbersInProduction(dateFormat), "Incorrect season min/maxVersion in production file");
		Assert.assertTrue(validateSeasonVersionNumbersInDevelopment(dateFormat), "Incorrect season min/maxVersion in development file");
	}
	
	// updating maxVestion updates next season's minVersion (if not last season)
	@Test (dependsOnMethods="updateMinVersion", description = "updating maxVestion updates next season's minVersion")
	public void updateMaxVersion() throws Exception{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID2, sessionToken);
		
		String season1 = s.getSeason(productID, seasonID1, sessionToken);
		JSONObject season1Json = new JSONObject(season1);
		season1Json.put("maxVersion", "1.6");
		String response = s.updateSeason(seasonID1, season1Json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");	
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID2, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");
		
		Assert.assertTrue(validateSeasonVersionNumbersInProduction(dateFormat), "Incorrect season min/maxVersion in production file");
		Assert.assertTrue(validateSeasonVersionNumbersInDevelopment(dateFormat), "Incorrect season min/maxVersion in development file");
		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		featureInSeason2 = features.getJSONObject(0).getString("uniqueId");
	}
	
	// delete the last season, the previous season't maxVersion must be changed to null
	@Test (dependsOnMethods="updateMaxVersion", description = "delete the last season, the previous season't maxVersion must be changed to null")
	public void deleteLastSeason() throws Exception{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID2, sessionToken);
		
		String feature = f.getFeature(featureInSeason2, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		String response = f.updateFeature(seasonID2, featureInSeason2, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not updated");
		
		int responseCode = s.deleteSeason(seasonID2, sessionToken);
		Assert.assertTrue(responseCode==200, "Season was not deleted");
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev2 = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev2.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");	
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID1, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");

		JSONObject rootSeason1 = new JSONObject(responseDev2.message);
		Assert.assertTrue((rootSeason1.isNull("maxVersion")), "Season1 maxVersion was not updated in development file");
		
		JSONObject rootSeason1Prod = new JSONObject(responseProd.message);
		Assert.assertTrue(rootSeason1Prod.isNull("maxVersion"), "Season1 maxVersion was not updated in production file");

	}

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
	
	private boolean validateSeasonVersionNumbersInProduction(String dateFormat) throws JSONException, IOException{
		boolean versionCorrect = false;
		RuntimeRestApi.DateModificationResults responseSeason1 = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		RuntimeRestApi.DateModificationResults responseSeason2 = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		JSONObject rootSeason1 = new JSONObject(responseSeason1.message);
		JSONObject rootSeason2 = new JSONObject(responseSeason2.message);
		if (rootSeason2.getString("minVersion").equals(rootSeason1.getString("maxVersion")))
			versionCorrect = true;
		
		return versionCorrect;
	}
	
	private boolean validateSeasonVersionNumbersInDevelopment(String dateFormat) throws JSONException, IOException{
		boolean versionCorrect = false;
		RuntimeRestApi.DateModificationResults responseSeason1 = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		RuntimeRestApi.DateModificationResults responseSeason2 = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		JSONObject rootSeason1 = new JSONObject(responseSeason1.message);
		JSONObject rootSeason2 = new JSONObject(responseSeason2.message);
		if (rootSeason2.getString("minVersion").equals(rootSeason1.getString("maxVersion")))
			versionCorrect = true;
		
		return versionCorrect;
	}
}
