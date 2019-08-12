package tests.restapi.proddevsplit;

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
import tests.restapi.StringsRestApi;
import tests.restapi.TranslationsRestApi;


public class StringsInNewSeason {
	protected String seasonID;
	protected String devFeatureID;
	protected String prodFeatureID;
	protected String filePath;
	protected String stringID1;
	protected String stringID2;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected StringsRestApi str;
	private FeaturesRestApi f;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private TranslationsRestApi translationsApi;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		p = new ProductsRestApi();
		s = new SeasonsRestApi();
		str = new StringsRestApi();
		f = new FeaturesRestApi();
		translationsApi = new TranslationsRestApi();
		s.setURL(m_url);
		s.setURL(m_url);
		p.setURL(m_url);
		translationsApi.setURL(translationsUrl);
		str.setURL(translationsUrl);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	
	@Test (description="Create string in development")
	public void createString() throws Exception{

		String string1 = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID1 = str.addString(seasonID, string1, sessionToken);
		
		String string2 = FileUtils.fileToString(filePath + "strings/string3.txt", "UTF-8", false);
		stringID2 = str.addString(seasonID, string2, sessionToken);

		//		add french locale 
		String response = translationsApi.addSupportedLocales(seasonID,"fr",sessionToken);
	     Assert.assertFalse(response.contains("error"),"could not add french locale");

	}
	

	@Test (dependsOnMethods = "createString", description="Add translations")
	public void addTranslations() throws Exception{


	    String  response = translationsApi.markForTranslation(seasonID,new String[]{stringID1},sessionToken);
	     Assert.assertFalse(response.contains("error"),"could not mark string for translation ");
	    response = translationsApi.reviewForTranslation(seasonID,new String[]{stringID1},sessionToken);
	    Assert.assertFalse(response.contains("error"),"could not review string");
	    response = translationsApi.sendToTranslation(seasonID,new String[]{stringID1},sessionToken);
	    Assert.assertFalse(response.contains("error"),"could not send for translation");

		String dateFormat = f.setDateFormat();
	    //add translation for string in dev
	   String translationFr = FileUtils.fileToString(filePath + "strings/translationFR.txt", "UTF-8", false);
	   response = translationsApi.updateTranslation(seasonID,"fr",translationFr,sessionToken);
	   Assert.assertTrue(response.equals(""),"could not translate french");


		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentTranslationDateModification(m_url, "en", productID,seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development strings file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionTranslationDateModification(m_url, "en", productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production string file was changed");

		responseDev = RuntimeDateUtilities.getDevelopmentTranslationDateModification(m_url, "fr", productID,seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development strings file was not updated");
		responseProd = RuntimeDateUtilities.getProductionTranslationDateModification(m_url, "fr", productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production string file was changed");

		response = translationsApi.markForTranslation(seasonID,new String[]{stringID2},sessionToken);
		Assert.assertFalse(response.contains("error"),"could not mark string for translation ");
		response = translationsApi.reviewForTranslation(seasonID,new String[]{stringID2},sessionToken);
		Assert.assertFalse(response.contains("error"),"could not review string");
		response = translationsApi.sendToTranslation(seasonID,new String[]{stringID2},sessionToken);
		Assert.assertFalse(response.contains("error"),"could not send for translation");

		dateFormat = f.setDateFormat();
		//add translation for string in prod
		String translationFr2 = FileUtils.fileToString(filePath + "strings/translationFRProd.txt", "UTF-8", false);
		response = translationsApi.updateTranslation(seasonID,"fr",translationFr2,sessionToken);
		Assert.assertTrue(response.equals(""),"could not translate french");

		f.setSleep();
		responseDev = RuntimeDateUtilities.getDevelopmentTranslationDateModification(m_url, "en", productID,seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development strings file was changed");
		responseProd = RuntimeDateUtilities.getProductionTranslationDateModification(m_url, "en", productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production string file was changed");

		responseDev = RuntimeDateUtilities.getDevelopmentTranslationDateModification(m_url, "fr", productID,seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development strings file was not updated");
		responseProd = RuntimeDateUtilities.getProductionTranslationDateModification(m_url, "fr", productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production string file was not updated");

		dateFormat = f.setDateFormat();
		//move from dev to prod
		String strJson = str.getString(stringID1, sessionToken);
		JSONObject json = new JSONObject(strJson);
		json.put("stage", "PRODUCTION");
		str.updateString(stringID1, json.toString(), sessionToken);
		f.setSleep();
		responseDev = RuntimeDateUtilities.getDevelopmentTranslationDateModification(m_url, "en", productID,seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development strings file was not updated");
		responseProd = RuntimeDateUtilities.getProductionTranslationDateModification(m_url, "en", productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production string file was not updated");

		responseDev = RuntimeDateUtilities.getDevelopmentTranslationDateModification(m_url, "fr", productID,seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development strings file was not updated");
		responseProd = RuntimeDateUtilities.getProductionTranslationDateModification(m_url, "fr", productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production string file was not updated");

		dateFormat = f.setDateFormat();
		//move from prod to dev
		strJson = str.getString(stringID2, sessionToken);
		json = new JSONObject(strJson);
		json.put("stage", "DEVELOPMENT");
		str.updateString(stringID2, json.toString(), sessionToken);
		f.setSleep();
		responseDev = RuntimeDateUtilities.getDevelopmentTranslationDateModification(m_url, "en", productID,seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development strings file was not updated");
		responseProd = RuntimeDateUtilities.getProductionTranslationDateModification(m_url, "en", productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production string file was not updated");

		responseDev = RuntimeDateUtilities.getDevelopmentTranslationDateModification(m_url, "fr", productID,seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development strings file was not updated");
		responseProd = RuntimeDateUtilities.getProductionTranslationDateModification(m_url, "fr", productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production string file was not updated");

		dateFormat = f.setDateFormat();
		int responseCode = str.deleteString(stringID2,sessionToken);
		Assert.assertTrue(responseCode == 200,"could not delete string");
		f.setSleep();
		responseDev = RuntimeDateUtilities.getDevelopmentTranslationDateModification(m_url, "en", productID,seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development strings file was not updated");
		responseProd = RuntimeDateUtilities.getProductionTranslationDateModification(m_url, "en", productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production string file was changed");

		responseDev = RuntimeDateUtilities.getDevelopmentTranslationDateModification(m_url, "fr", productID,seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development strings file was not updated");
		responseProd = RuntimeDateUtilities.getProductionTranslationDateModification(m_url, "fr", productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production string file was changed");
		
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
