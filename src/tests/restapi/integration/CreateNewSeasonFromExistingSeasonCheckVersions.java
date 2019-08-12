package tests.restapi.integration;

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
import tests.restapi.SeasonsRestApi;

public class CreateNewSeasonFromExistingSeasonCheckVersions {
	protected String seasonID1;
	protected String seasonID2;
	protected String seasonID3;
	protected String featureID1;
	protected String featureID2;
	protected String featureID3;
	protected String productID;
	protected String config;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected AirlockUtils baseUtils;
	private String sessionToken = "";


	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		config = configPath;

		p = new ProductsRestApi();
		s = new SeasonsRestApi();
		f = new FeaturesRestApi();

		p.setURL(url);
		s.setURL(url);
		f.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

	}
	

	@Test (description = "Add product and season1 with minVer=0.8")
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
		featureID1 = JSONUtils.generateUniqueString(feature1, 5, "name");
		Assert.assertFalse(featureID1.contains("error"), "Test should pass, but instead failed: " + featureID1 );
	}
	
	
	
		
	/* create season2 with minVer=1.4:
	 * season1MinVer = 0.8, season1MaxVer=1.4
	 * season2MinVer = 1.4, season1MaxVer=?
	 * 
	 */
	@Test (dependsOnMethods="addComponents", description ="Add season2 with minVer=1.4")
	public void createSecondSeason() throws Exception{
		String basicSeason = FileUtils.fileToString(config + "season2.txt", "UTF-8", false);
		seasonID2 = s.addSeason(productID, basicSeason, sessionToken);
		String season1 = s.getSeason(productID, seasonID1, sessionToken);
		JSONObject season1Json = new JSONObject(season1);
		String season2 = s.getSeason(productID, seasonID2, sessionToken);
		JSONObject season2Json = new JSONObject(season2);
		validateSeasonVersionNumbers(season1Json, season2Json);
	}
	
	/* create season3 with minVer=2.6:
	 * season1MinVer = 0.8, season1MaxVer=1.4
	 * season2MinVer = 1.4, season1MaxVer=2.6
	 * season3MinVer = 2.6, season1MaxVer=null
	 */
	@Test (dependsOnMethods="createSecondSeason", description ="Add season3 with minVer=2.6 and validate seasons' maxNumbers")
	public void createThirdSeason() throws Exception{
		String basicSeason = FileUtils.fileToString(config + "season3.txt", "UTF-8", false);
		seasonID3 = s.addSeason(productID, basicSeason, sessionToken);
		String season2 = s.getSeason(productID, seasonID2, sessionToken);
		JSONObject season2Json = new JSONObject(season2);
		String season3 = s.getSeason(productID, seasonID3, sessionToken);
		JSONObject season3Json = new JSONObject(season3);
		validateSeasonVersionNumbers(season2Json, season3Json);
	}
	
	//1. You cannot change last season's maxVesrion - it must be null.
	@Test (dependsOnMethods="createThirdSeason", description="Add maxVersion to the last season" )
	public void checkLastMaxVersion() throws Exception{
		String season3 = s.getSeason(productID, seasonID3, sessionToken);
		JSONObject season3Json = new JSONObject(season3);
		season3Json.put("maxVersion", "5.0");
		String response = s.updateSeason(seasonID3, season3Json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	//2. minVersion cannot be lower than prev season's minVersion.
	@Test (dependsOnMethods="checkLastMaxVersion", description="minVersion cannot be lower than prev season's minVersion")
	public void checkMinVersion() throws Exception{
		String season3 = s.getSeason(productID, seasonID3, sessionToken);
		JSONObject season3Json = new JSONObject(season3);
		season3Json.put("minVersion", "1.2");
		String response = s.updateSeason(seasonID3, season3Json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	//3. maxVersion cannot be higher than next season's maxVersion
	@Test (dependsOnMethods="checkMinVersion", description = "maxVersion cannot be higher than next season's maxVersion")
	public void checkMaxVersion() throws Exception{
		String season2 = s.getSeason(productID, seasonID2, sessionToken);
		JSONObject season2Json = new JSONObject(season2);
		season2Json.put("maxVersion", "3.0");
		String response = s.updateSeason(seasonID3, season2Json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	//4. updating minVersion updates prev season's maxVersion (if not first season)
	@Test (dependsOnMethods="checkMaxVersion", description = "updating minVersion updates prev season's maxVersion")
	public void updateMinVersion() throws Exception{
		String season2 = s.getSeason(productID, seasonID2, sessionToken);
		JSONObject season2Json = new JSONObject(season2);
		season2Json.put("minVersion", "1.5");
		String response = s.updateSeason(seasonID2, season2Json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
		
		//check previous season's maxVersion
		String season1 = s.getSeason(productID, seasonID1, sessionToken);
		JSONObject season1Json = new JSONObject(season1);
		Assert.assertTrue(season1Json.getString("maxVersion").equals("1.5"), "The previous season's maxVersion was not updated");
	}
	
	//5. updating maxVestion updates next season's minVersion (if not last season)
	@Test (dependsOnMethods="updateMinVersion", description = "updating maxVestion updates next season's minVersion")
	public void updateMaxVersion() throws Exception{
		String season2 = s.getSeason(productID, seasonID2, sessionToken);
		JSONObject season2Json = new JSONObject(season2);
		season2Json.put("maxVersion", "1.6");
		String response = s.updateSeason(seasonID2, season2Json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
		
		//check next season's minVersion
		String season3 = s.getSeason(productID, seasonID3, sessionToken);
		JSONObject season3Json = new JSONObject(season3);
		Assert.assertTrue(season3Json.getString("minVersion").equals("1.6"), "The next season's minVersion was not updated");
	}
	
	//6. delete the last season, the previous season't maxVersion must be changed to null
	@Test (dependsOnMethods="updateMaxVersion", description = "delete the last season, the previous season't maxVersion must be changed to null")
	public void deleteLastSeason() throws Exception{
		s.deleteSeason(seasonID3, sessionToken);
		String season2 = s.getSeason(productID, seasonID2, sessionToken);
		JSONObject season2Json = new JSONObject(season2);	
		if (!season2Json.isNull("maxVersion"))
			Assert.fail("The last season's maxVersion was not converted to null");
		
	}

	
	@AfterTest ()
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
	
	private void validateSeasonVersionNumbers(JSONObject season1, JSONObject season2) throws JSONException{
		
		Assert.assertNotNull(season1.getString("maxVersion"), "maxVersion for season1 was not set.");
		String maxVer = season1.getString("maxVersion");
		String minVer = season2.getString("minVersion");
		if (!maxVer.equals(minVer)){
			Assert.fail("minVersion of season2 is not equal to maxVersion of season1");
		} 
		
		if ( !season2.isNull("maxVersion")) {
			Assert.fail("maxVersion of season2 should be set to null");
		} 
		
	}
}
