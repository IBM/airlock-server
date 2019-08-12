package tests.restapi.scenarios.translation;


import com.ibm.airlock.Strings;
import org.apache.wink.json4j.JSONArray;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.*;

public class DuplicateSeason {
	protected String seasonID;
	protected String stringID;
	protected String stringID2;
	protected String filePath;
	protected String str;
	protected String str2;
	protected StringsRestApi t;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private String translationsUrl;
	private String MISSING_TRANSLATION = Strings.translationDoesNotExist;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String tUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		translationsUrl = tUrl;
		filePath = configPath;
		t = new StringsRestApi();
		t.setURL(translationsUrl);
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}



	@Test (description = "Add string value")
	public void addString() throws Exception{

		str = FileUtils.fileToString(filePath + "/strings/string1.txt", "UTF-8", false);
		stringID = t.addString(seasonID, str, sessionToken);
		str2 = FileUtils.fileToString(filePath + "/strings/string2.txt", "UTF-8", false);
		stringID2 =t.addString(seasonID, str2, sessionToken);
	}

	@Test (dependsOnMethods="addString",  description = "Check that the string was added to a list of all strings in season")
	public void validateStringInSeason() throws Exception{

		String response = t.getAllStrings(seasonID, sessionToken);
		JSONObject json = new JSONObject(response);
		JSONArray allStrings = json.getJSONArray("strings");
		Assert.assertTrue(allStrings.length()==2, "String was not added to a list of all strings in season");

		//check first string is in
		str = t.getString(stringID, sessionToken);
		JSONObject jsonStr = new JSONObject(str);
		JSONObject firstString = (JSONObject)allStrings.get(0);
		Assert.assertTrue(firstString.getString("key").equals(jsonStr.getString("key")), "Incorrect string key");
		Assert.assertTrue(firstString.getString("value").equals(jsonStr.getString("value")), "Incorrect string value");

		//check second string is in
		str2 = t.getString(stringID2, sessionToken);
		JSONObject jsonStr2 = new JSONObject(str2);
		JSONObject secondString = (JSONObject)allStrings.get(1);
		Assert.assertTrue(secondString.getString("key").equals(jsonStr2.getString("key")), "Incorrect string key");
		Assert.assertTrue(secondString.getString("value").equals(jsonStr2.getString("value")), "Incorrect string value");

	}


	@Test (dependsOnMethods="validateStringInSeason",  description = "Check that the string was added to a list of all strings in translations")
	public void duplicateSeason() throws Exception{
		TranslationsRestApi trans = new TranslationsRestApi();
		trans.setURL(translationsUrl);
		//add spanish
		String esTranslation = FileUtils.fileToString(filePath + "strings/translationFallbackES.txt", "UTF-8", false);
		String markMessage = trans.markForTranslation(seasonID ,new String[]{stringID,stringID2},sessionToken);
		String reviewedMessage = trans.reviewForTranslation(seasonID ,new String[]{stringID,stringID2},sessionToken);
		String sendMessage = trans.sendToTranslation(seasonID ,new String[]{stringID,stringID2},sessionToken);
		String translationMessage = trans.addTranslation(seasonID,"es",esTranslation,sessionToken);
		Assert.assertTrue(translationMessage.equals(""));

		//add french
		String frTranslation = FileUtils.fileToString(filePath + "strings/translationFallbackFR.txt", "UTF-8", false);
		translationMessage = trans.addTranslation(seasonID,"fr",frTranslation,sessionToken);
		Assert.assertTrue(translationMessage.equals(""));

		SeasonsRestApi seasonApi = new SeasonsRestApi();
		String seasonJson = FileUtils.fileToString(filePath + "/season2.txt", "UTF-8", false);
		String seasonID2 = seasonApi.addSeason(productID,seasonJson, sessionToken);
		String allFrTranslations = trans.getTranslation(seasonID, "fr", "DEVELOPMENT", sessionToken);
		String allFrTranslations2= trans.getTranslation(seasonID2, "fr", "DEVELOPMENT", sessionToken);
		Assert.assertTrue(allFrTranslations.equals(allFrTranslations2));

		String allEsTranslations = trans.getTranslation(seasonID, "es", "DEVELOPMENT", sessionToken);
		String allEsTranslations2 = trans.getTranslation(seasonID2, "es", "DEVELOPMENT",  sessionToken);
		Assert.assertTrue(allEsTranslations.equals(allEsTranslations2));

		String allEnTranslations = trans.getTranslation(seasonID, "en", "DEVELOPMENT",  sessionToken);
		String allEnTranslations2 = trans.getTranslation(seasonID2, "en", "DEVELOPMENT", sessionToken);
		Assert.assertTrue(allEnTranslations.equals(allEnTranslations2));

		//check japanese still missing
		String allJpTranslations = trans.getTranslation(seasonID, "jp", "DEVELOPMENT", sessionToken);
		Assert.assertTrue(allJpTranslations.contains(MISSING_TRANSLATION));

		//same but with PRODUCTION
		allFrTranslations = trans.getTranslation(seasonID, "fr", "PRODUCTION", sessionToken);
		allFrTranslations2= trans.getTranslation(seasonID2, "fr", "PRODUCTION", sessionToken);
		Assert.assertTrue(allFrTranslations.equals(allFrTranslations2));

		allEsTranslations = trans.getTranslation(seasonID, "es", "PRODUCTION", sessionToken);
		allEsTranslations2 = trans.getTranslation(seasonID2, "es", "PRODUCTION",  sessionToken);
		Assert.assertTrue(allEsTranslations.equals(allEsTranslations2));

		allEnTranslations = trans.getTranslation(seasonID, "en", "PRODUCTION",  sessionToken);
		allEnTranslations2 = trans.getTranslation(seasonID2, "en", "PRODUCTION", sessionToken);
		Assert.assertTrue(allEnTranslations.equals(allEnTranslations2));

		//check japanese still missing
		allJpTranslations = trans.getTranslation(seasonID, "jp", "PRODUCTION", sessionToken);
		Assert.assertTrue(allJpTranslations.contains(MISSING_TRANSLATION));


		//check new product don't have translation
		String product = FileUtils.fileToString(filePath + "/product2.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		String productID2 = p.addProduct(product, sessionToken);
		String seasonID3 = seasonApi.addSeason(productID2,seasonJson, sessionToken);
		allEsTranslations = trans.getTranslation(seasonID3, "es", "DEVELOPMENT", sessionToken);
		Assert.assertTrue(allEsTranslations.contains(MISSING_TRANSLATION));
		allEsTranslations = trans.getTranslation(seasonID3, "es", "PRODUCTION", sessionToken);
		Assert.assertTrue(allEsTranslations.contains(MISSING_TRANSLATION));
		p.deleteProduct(productID2, sessionToken);
	}


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
