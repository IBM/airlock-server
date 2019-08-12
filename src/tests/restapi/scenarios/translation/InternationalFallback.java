package tests.restapi.scenarios.translation;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

public class InternationalFallback {
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
	private String MISSING_TRANSLATION = "Local translations do not exist.";

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

		str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID = t.addString(seasonID, str, sessionToken);
		str2 = FileUtils.fileToString(filePath + "strings/string2.txt", "UTF-8", false);
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
	public void checkInternationalFallback() throws Exception{
		TranslationsRestApi trans = new TranslationsRestApi();
		trans.setURL(translationsUrl);
		//add spanish
		String esTranslation = FileUtils.fileToString(filePath + "strings/translationFallbackES.txt", "UTF-8", false);
		String markMessage = trans.markForTranslation(seasonID ,new String[]{stringID,stringID2},sessionToken);
		String reviewedMessage = trans.reviewForTranslation(seasonID ,new String[]{stringID,stringID2},sessionToken);
		String sendMessage = trans.sendToTranslation(seasonID ,new String[]{stringID,stringID2},sessionToken);
		String translationMessage = trans.addTranslation(seasonID,"es",esTranslation,sessionToken);
		Assert.assertTrue(translationMessage.equals(""));

		//check first value is tranlated and second value is fallback
		JSONObject allEsTransStrings = getAllLocalString(trans,"es");
		checkIfStringExist(stringID,allEsTransStrings,"Hola");
		checkIfStringExist(stringID2,allEsTransStrings,"Hello");

		//add french
		String frTranslation = FileUtils.fileToString(filePath + "strings/translationFallbackFR.txt", "UTF-8", false);
		translationMessage = trans.addTranslation(seasonID,"fr",frTranslation,sessionToken);
		Assert.assertTrue(translationMessage.equals(""));

		//check both value are tranlated
		JSONObject allFrTransStrings = getAllLocalString(trans,"fr");
		checkIfStringExist(stringID,allFrTransStrings,"Bonjour");
		checkIfStringExist(stringID2,allFrTransStrings,"Salut");

		//check english get values
		JSONObject allEnTransStrings = getAllLocalString(trans,"en");
		checkIfStringExist(stringID,allEnTransStrings);
		checkIfStringExist(stringID2,allEnTransStrings);

		//add third string with fallback
		String fallback = FileUtils.fileToString(filePath + "/strings/string4.txt", "UTF-8", false);
		String fallbackID = t.addString(seasonID, fallback, sessionToken);
		fallback = t.getString(fallbackID, sessionToken);
		Assert.assertFalse((fallback.contains("error")));

		//check added in both language and english get value
		allEsTransStrings = getAllLocalString(trans,"es");
		checkIfStringExist(fallbackID,allEsTransStrings,"back");
		allFrTransStrings = getAllLocalString(trans,"fr");
		checkIfStringExist(fallbackID,allFrTransStrings,"back");
		allEnTransStrings = getAllLocalString(trans,"en");
		checkIfStringExist(fallbackID,allEnTransStrings,"fall");

		//add string without fallback
		String noFallback = FileUtils.fileToString(filePath + "/strings/stringNoFallback.txt", "UTF-8", false);
		String noFallbackID = t.addString(seasonID, noFallback, sessionToken);
		noFallback = t.getString(noFallbackID, sessionToken);
		Assert.assertFalse((noFallback.contains("error")));

		//check added with value
		allEsTransStrings = getAllLocalString(trans,"es");
		checkDontExist(noFallbackID,allEsTransStrings);
		allFrTransStrings = getAllLocalString(trans,"fr");
		checkDontExist(noFallbackID,allFrTransStrings);
		allEnTransStrings = getAllLocalString(trans,"en");
		checkIfStringExist(noFallbackID,allEnTransStrings,"noFallback");

	}

	@Test (dependsOnMethods="checkInternationalFallback",  description = "Check that the string was added to a list of all strings in translations")
	public void checkUpdateFallback() throws Exception{
		TranslationsRestApi trans = new TranslationsRestApi();
		trans.setURL(translationsUrl);

		//update neither value and fallback
		str2 = t.getString(stringID2,sessionToken);
		str2 = str2.replace("Mary","Bob");
		stringID2 = t.updateString(stringID2, str2, sessionToken);
		//check no changes
		JSONObject allFrTransStrings = getAllLocalString(trans,"fr");
		checkIfStringExist(stringID2,allFrTransStrings,"Salut");
		JSONObject allEsTransStrings = getAllLocalString(trans,"es");
		checkIfStringExist(stringID2,allEsTransStrings,"Hello");
		JSONObject allEnTransStrings = getAllLocalString(trans,"en");
		checkIfStringExist(stringID2,allEnTransStrings);

		//check last modified
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getFileModificationDate("translations/strings__enPRODUCTION.json",m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "production file should not changed");
		responseProd = RuntimeDateUtilities.getFileModificationDate("translations/strings__frPRODUCTION.json",m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "production file should not changed");
		responseProd = RuntimeDateUtilities.getFileModificationDate("translations/strings__esPRODUCTION.json",m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "production file should not exist");

		//update string 2 value
		str2 = t.getString(stringID2,sessionToken);
		str2 = str2.replace("Hi","Hey");
		stringID2 = t.updateString(stringID2, str2, sessionToken);
		// check that french and spanish did not change
		allFrTransStrings = getAllLocalString(trans,"fr");
		checkIfStringExist(stringID2,allFrTransStrings,"Salut");
		allEsTransStrings = getAllLocalString(trans,"es");
		checkIfStringExist(stringID2,allEsTransStrings,"Hello");
		allEnTransStrings = getAllLocalString(trans,"en");
		checkIfStringExist(stringID2,allEnTransStrings);

		//update string 2 fallback
		str2 = t.getString(stringID2,sessionToken);
		str2 = str2.replace("Hello","Heya");
		stringID2 = t.updateString(stringID2, str2, sessionToken);
		// check that french did not change and spanish changed to new fallback
		allFrTransStrings = getAllLocalString(trans,"fr");
		checkIfStringExist(stringID2,allFrTransStrings,"Salut");
		allEsTransStrings = getAllLocalString(trans,"es");
		checkIfStringExist(stringID2,allEsTransStrings,"Heya");
		allEnTransStrings = getAllLocalString(trans,"en");
		checkIfStringExist(stringID2,allEnTransStrings);

		//update string 2 value and fallback
		str2 = t.getString(stringID2,sessionToken);
		str2 = str2.replace("Heya","HelloNew");
		str2 = str2.replace("Hey","Hi");
		stringID2 = t.updateString(stringID2, str2, sessionToken);
		// check that french did not change and spanish changed to new fallback
		allFrTransStrings = getAllLocalString(trans,"fr");
		checkIfStringExist(stringID2,allFrTransStrings,"Salut");
		allEsTransStrings = getAllLocalString(trans,"es");
		checkIfStringExist(stringID2,allEsTransStrings,"HelloNew");
		allEnTransStrings = getAllLocalString(trans,"en");
		checkIfStringExist(stringID2,allEnTransStrings);

		//remove fallback from string 2
		str2 = t.getString(stringID2,sessionToken);
		JSONObject strObj = new JSONObject(str2);
		strObj.put("internationalFallback",  JSONObject.NULL);
		str2 = strObj.toString();
		stringID2 = t.updateString(stringID2, str2, sessionToken);
		String str3 = t.getString(stringID2,sessionToken);
		//check that french did not change and spanish to value
		allFrTransStrings = getAllLocalString(trans,"fr");
		checkIfStringExist(stringID2,allFrTransStrings,"Salut");
		allEsTransStrings = getAllLocalString(trans,"es");
		checkDontExist(stringID2,allEsTransStrings);
		allEnTransStrings = getAllLocalString(trans,"en");
		checkIfStringExist(stringID2,allEnTransStrings);

		//update string 2 value
		str2 = t.getString(stringID2,sessionToken);
		str2 = str2.replace("Hi","Hey");
		stringID2 = t.updateString(stringID2, str2, sessionToken);
		//check that french did not change and spanish to new value
		allFrTransStrings = getAllLocalString(trans,"fr");
		checkIfStringExist(stringID2,allFrTransStrings,"Salut");
		allEsTransStrings = getAllLocalString(trans,"es");
		checkDontExist(stringID2,allEsTransStrings);
		allEnTransStrings = getAllLocalString(trans,"en");
		checkIfStringExist(stringID2,allEnTransStrings);

		//add fallback to string 2
		str2 = t.getString(stringID2,sessionToken);
		strObj = new JSONObject(str2);
		strObj.put("internationalFallback","newFallback");
		str2 = strObj.toString();
		stringID2 = t.updateString(stringID2, str2, sessionToken);
		//check that french did not change and spanish changed to new fallback
		allFrTransStrings = getAllLocalString(trans,"fr");
		checkIfStringExist(stringID2,allFrTransStrings,"Salut");
		allEsTransStrings = getAllLocalString(trans,"es");
		checkIfStringExist(stringID2,allEsTransStrings,"newFallback");
		allEnTransStrings = getAllLocalString(trans,"en");
		checkIfStringExist(stringID2,allEnTransStrings);
	}

	private JSONObject getAllLocalString(TranslationsRestApi trans, String local) {
		try {
			String allTranslations = trans.getTranslation(seasonID, local, "DEVELOPMENT", sessionToken);
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
