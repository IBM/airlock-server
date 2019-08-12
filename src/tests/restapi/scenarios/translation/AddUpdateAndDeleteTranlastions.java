package tests.restapi.scenarios.translation;


import com.ibm.airlock.Strings;
import org.apache.wink.json4j.JSONArray;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

public class AddUpdateAndDeleteTranlastions {
	protected String seasonID;
	protected String stringID;
	protected String stringID2;
	protected String stringID3;
	protected String filePath;
	protected String str;
	protected String str2;
	protected String str3;
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
		str3 = FileUtils.fileToString(filePath + "/strings/string3.txt", "UTF-8", false);
		stringID3 =t.addString(seasonID, str3, sessionToken);
	}
	
	@Test (dependsOnMethods="addString",  description = "Check that the string was added to a list of all strings in season")
	public void validateStringInSeason() throws Exception{
		
		String response = t.getAllStrings(seasonID, sessionToken);
		JSONObject json = new JSONObject(response);
		JSONArray allStrings = json.getJSONArray("strings");
		Assert.assertTrue(allStrings.length()==3, "String was not added to a list of all strings in season");

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

		//check third string is in
		str3 = t.getString(stringID3, sessionToken);
		JSONObject jsonStr3 = new JSONObject(str3);
		JSONObject thirdString = (JSONObject)allStrings.get(2);
		Assert.assertTrue(thirdString.getString("key").equals(jsonStr3.getString("key")), "Incorrect string key");
		Assert.assertTrue(thirdString.getString("value").equals(jsonStr3.getString("value")), "Incorrect string value");

	}
	
	
	@Test (dependsOnMethods="validateStringInSeason",  description = "Check that the string was added to a list of all strings in translations")
	public void validateStringInTranslations() throws Exception{
		
		TranslationsRestApi trans = new TranslationsRestApi();
		trans.setURL(translationsUrl);

		//Validate French and Spanish translation are missing
		JSONObject allFrTransStrings = getAllLocalString(trans,"fr","DEVELOPMENT");
		Assert.assertTrue(allFrTransStrings.get("error").equals(MISSING_TRANSLATION));
		JSONObject allEsTransStrings = getAllLocalString(trans,"es","DEVELOPMENT");
		Assert.assertTrue(allEsTransStrings.get("error").equals(MISSING_TRANSLATION));
		//production
		allFrTransStrings = getAllLocalString(trans,"fr","PRODUCTION");
		Assert.assertTrue(allFrTransStrings.get("error").equals(MISSING_TRANSLATION));
		allEsTransStrings = getAllLocalString(trans,"es","PRODUCTION");
		Assert.assertTrue(allEsTransStrings.get("error").equals(MISSING_TRANSLATION));

		//Validate string exist in English
		JSONObject allEnTransStrings = getAllLocalString(trans,"en","DEVELOPMENT");
		checkIfStringExist(stringID,allEnTransStrings);
		checkIfStringExist(stringID2,allEnTransStrings);
		checkIfStringExist(stringID3,allEnTransStrings);
		allEnTransStrings = getAllLocalString(trans,"en","PRODUCTION");
		checkDontExist(stringID,allEnTransStrings);
		checkDontExist(stringID2,allEnTransStrings);
		checkIfStringExist(stringID3,allEnTransStrings);

	}

	@Test (dependsOnMethods="validateStringInTranslations",  description = "Check that the string was translated and added to a list of all strings in translations")
	public void stringTranslate() throws Exception{

		TranslationsRestApi trans = new TranslationsRestApi();
		trans.setURL(translationsUrl);

		String translation = trans.stringForTranslation(seasonID, sessionToken);
		//Translate to French
		String expectedTranslation = FileUtils.fileToString(filePath + "strings/translationExpected.txt", "UTF-8", false);
		//Assert.assertEquals(expectedTranslation,translation);
		Assert.assertEquals(expectedTranslation.replace("\n", "").replace("\r", ""), translation.replace("\n", "").replace("\r", "")); 

		expectedTranslation = expectedTranslation.replace("Hello [[[1]]]","Bonjour [[[1]]]");
		expectedTranslation = expectedTranslation.replace("Hi [[[1]]] [[[2]]]","Salut [[[1]]] [[[2]]]");
		String markMessage = trans.markForTranslation(seasonID ,new String[]{stringID,stringID2,stringID3},sessionToken);
		String reviewedMessage = trans.reviewForTranslation(seasonID ,new String[]{stringID,stringID2,stringID3},sessionToken);
		String sendMessage = trans.sendToTranslation(seasonID ,new String[]{stringID,stringID2,stringID3},sessionToken);
		String translationMessage = trans.addTranslation(seasonID,"fr",expectedTranslation,sessionToken);
		Assert.assertTrue(translationMessage.equals(""));

		String supported = t.getSupportedLocales(seasonID,sessionToken);
		Assert.assertTrue(supported.contains("[\"en\",\"fr\"]")&& !supported.contains("\"es\""));
		//translation should still be missing in spanish
		JSONObject allEsTransStrings = getAllLocalString(trans,"es","DEVELOPMENT");
		Assert.assertTrue(allEsTransStrings.get("error").equals(MISSING_TRANSLATION));
		allEsTransStrings = getAllLocalString(trans,"es","PRODUCTION");
		Assert.assertTrue(allEsTransStrings.get("error").equals(MISSING_TRANSLATION));


		JSONObject allEnTransStrings = getAllLocalString(trans,"en","DEVELOPMENT");
		JSONObject allFrTransStrings = getAllLocalString(trans,"fr","DEVELOPMENT");
		checkIfStringExist(stringID,allEnTransStrings);
		checkIfStringExist(stringID2,allEnTransStrings);
		checkIfStringExist(stringID3,allEnTransStrings);
		checkIfStringExist(stringID,allFrTransStrings,"Bonjour [[[1]]]");
		checkIfStringExist(stringID2,allFrTransStrings,"Salut [[[1]]] [[[2]]]");
		checkIfStringExist(stringID3,allFrTransStrings,"Hello");

		allEnTransStrings = getAllLocalString(trans,"en","PRODUCTION");
		allFrTransStrings = getAllLocalString(trans,"fr","PRODUCTION");
		checkDontExist(stringID,allEnTransStrings);
		checkDontExist(stringID2,allEnTransStrings);
		checkIfStringExist(stringID3,allEnTransStrings);
		checkDontExist(stringID,allFrTransStrings);
		checkDontExist(stringID2,allFrTransStrings);
		checkIfStringExist(stringID3,allFrTransStrings,"Hello");
	}

	@Test (dependsOnMethods="stringTranslate",  description = "Update translation and check if succeed")
	public void translationUpdate() throws Exception {
		TranslationsRestApi trans = new TranslationsRestApi();
		trans.setURL(translationsUrl);

		String str4 = FileUtils.fileToString(filePath + "/strings/string4.txt", "UTF-8", false);
		String stringID4 =t.addString(seasonID, str4, sessionToken);
		//Update french translation
		String frTranslation = FileUtils.fileToString(filePath + "strings/translationFR.txt", "UTF-8", false);
		frTranslation = frTranslation.replace("Bonjour","SALUT");
		String translationMessage = trans.updateTranslation(seasonID,"fr",frTranslation,sessionToken);
		Assert.assertTrue(translationMessage.equals(""));

		JSONObject allFrTransStrings = getAllLocalString(trans,"fr","DEVELOPMENT");
		checkIfStringExist(stringID,allFrTransStrings,"SALUT");
		checkIfStringExist(stringID2,allFrTransStrings,"Salut");
		checkIfStringExist(stringID3,allFrTransStrings,"Hello");
		checkIfStringExist(stringID4,allFrTransStrings,"back");

		allFrTransStrings = getAllLocalString(trans,"fr","PRODUCTION");
		checkDontExist(stringID,allFrTransStrings);
		checkDontExist(stringID2,allFrTransStrings);
		checkIfStringExist(stringID3,allFrTransStrings,"Hello");
		checkIfStringExist(stringID4,allFrTransStrings,"back");


		//try to update Spanish. should fail because it is missing
		String esTranslation = FileUtils.fileToString(filePath + "strings/translationES.txt", "UTF-8", false);
		translationMessage = trans.updateTranslation(seasonID,"es",esTranslation,sessionToken);
		Assert.assertTrue(translationMessage.contains(MISSING_TRANSLATION));

		String supported = t.getSupportedLocales(seasonID,sessionToken);
		Assert.assertTrue(supported.contains("[\"en\",\"fr\"]")&& !supported.contains("\"es\""));

	}

	@Test (dependsOnMethods="translationUpdate",  description = "check that we have international fallback")
	public void deleteTranslation() throws Exception {
		//delete the first string
		t.deleteString(stringID,sessionToken);
		String deleted = t.getString(stringID, sessionToken);
		Assert.assertTrue(deleted.contains("String not found"));

		TranslationsRestApi trans = new TranslationsRestApi();
		trans.setURL(translationsUrl);

		//first one should be deleted an second still exist
		JSONObject allFrTransStrings = getAllLocalString(trans,"fr","DEVELOPMENT");
		Assert.assertFalse(allFrTransStrings.has("app.hello"), "The string key should not exist in translations");
		Assert.assertTrue(allFrTransStrings.has("app.hi"), "The string key was not found in translations");
		JSONObject allEnTransStrings = getAllLocalString(trans,"en","DEVELOPMENT");
		Assert.assertFalse(allEnTransStrings.has("app.hello"), "The string key should not exist in translations");
		Assert.assertTrue(allEnTransStrings.has("app.hi"), "The string key was not found in translations");

		//production should stay the same
		allFrTransStrings = getAllLocalString(trans,"fr","PRODUCTION");
		Assert.assertTrue(allFrTransStrings.has("app.helloProd"), "The string key should not exist in translations");
		Assert.assertTrue(allFrTransStrings.has("app.fallback"), "The string key was not found in translations");
		allEnTransStrings = getAllLocalString(trans,"en","PRODUCTION");
		Assert.assertTrue(allEnTransStrings.has("app.helloProd"), "The string key should not exist in translations");
		Assert.assertTrue(allEnTransStrings.has("app.fallback"), "The string key was not found in translations");

		//check last modified
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getFileModificationDate("translations/strings__enPRODUCTION.json",m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "production file should not changed");
		responseProd = RuntimeDateUtilities.getFileModificationDate("translations/strings__frPRODUCTION.json",m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "production file should not changed");
		responseProd = RuntimeDateUtilities.getFileModificationDate("translations/strings__esPRODUCTION.json",m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==403 || responseProd.code ==404, "production file should not exist");

		//Translate to Spanish
		String esTranslation = FileUtils.fileToString(filePath + "strings/translationES.txt", "UTF-8", false);
		String translationMessage = trans.addTranslation(seasonID,"es",esTranslation,sessionToken);
		Assert.assertTrue(translationMessage.equals(""));

		//only second should exist
		JSONObject allEsTransStrings = getAllLocalString(trans,"es","DEVELOPMENT");
		Assert.assertFalse(allEsTransStrings.has("app.hello"), "The string key should not exist in translations");
		checkIfStringExist(stringID2,allEsTransStrings,"Ho");

		//check spanish is now supported
		String supported = t.getSupportedLocales(seasonID,sessionToken);
		Assert.assertTrue(supported.contains("[\"en\",\"fr\",\"es\"]"));

	}

	@Test (dependsOnMethods="deleteTranslation",  description = "upgrade To Production")
	public void upgradeToProd() throws Exception {
		//upgrade String 2
		str2 = t.getString(stringID2,sessionToken);
		str2 = str2.replace("DEVELOPMENT","PRODUCTION");
		stringID2 = t.updateString(stringID2, str2, sessionToken);

		TranslationsRestApi trans = new TranslationsRestApi();
		trans.setURL(translationsUrl);

		//should be in both
		JSONObject allFrTransStrings = getAllLocalString(trans,"fr","DEVELOPMENT");
		checkIfStringExist(stringID2,allFrTransStrings,"Salut");
		allFrTransStrings = getAllLocalString(trans,"fr","PRODUCTION");
		checkIfStringExist(stringID2,allFrTransStrings,"Salut");

	}
	@Test (dependsOnMethods="upgradeToProd",  description = "downgrade To Development")
	public void DowngradeToDev() throws Exception {
		//downgrade String 3
		str3 = t.getString(stringID3,sessionToken);
		str3 = str3.replace("PRODUCTION","DEVELOPMENT");
		stringID3 = t.updateString(stringID3, str3, sessionToken);

		TranslationsRestApi trans = new TranslationsRestApi();
		trans.setURL(translationsUrl);

		//should be in both
		JSONObject allFrTransStrings = getAllLocalString(trans,"fr","DEVELOPMENT");
		checkIfStringExist(stringID3,allFrTransStrings,"Hello");
		allFrTransStrings = getAllLocalString(trans,"fr","PRODUCTION");
		checkDontExist(stringID3,allFrTransStrings);

	}

	//TODO add upgrade downgrade tests
	private JSONObject getAllLocalString(TranslationsRestApi trans, String local,String stage) {
		try {
			String allTranslations = trans.getTranslation(seasonID, local, stage, sessionToken);
			JSONObject jsonTrans = new JSONObject(allTranslations);
			if(jsonTrans.has("strings")){
				JSONObject allTransStrings = jsonTrans.getJSONObject("strings");
				return allTransStrings;
			}
			else return jsonTrans;
		} catch (Exception e) {
			Assert.fail("failed to get translastion");
		}
		return null;
	}

	private void checkIfStringExist(String id,JSONObject allTransStrings) throws Exception {
		checkIfStringExist(id,allTransStrings,null);
	}
	private void checkIfStringExist(String id,JSONObject allTransStrings,String expectedValue) throws Exception{
		String str = t.getString(id, sessionToken);
		JSONObject jsonStr = new JSONObject(str);
		Assert.assertTrue(allTransStrings.has(jsonStr.getString("key")), "The string key was not found in translations");
		if(expectedValue == null)
			expectedValue = jsonStr.getString("value");
		Assert.assertTrue(allTransStrings.getString(jsonStr.getString("key")).equals(expectedValue), "The string value was not found in translations");
	}

	private void checkDontExist(String id,JSONObject allTransStrings) throws Exception{
		String str = t.getString(id, sessionToken);
		JSONObject jsonStr = new JSONObject(str);
		Assert.assertFalse(allTransStrings.has(jsonStr.getString("key")), "The string key was not found in translations");
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
