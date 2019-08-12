package tests.restapi.scenarios.translation;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;


/**
 * Created by amitaim on 29/03/2017.
 */
public class StatutesAndSummary {

    protected String seasonID;
    protected String stringID;
    protected String stringID2;
    protected String stringID3;
    protected String stringID4;
    protected String stringID5;
    protected String filePath;
    protected String str;
    protected String str2;
    protected String str3;
    protected String str4;
    protected String str5;
    protected StringsRestApi stringsApi;
    protected TranslationsRestApi translationsApi;
    private AirlockUtils baseUtils;
    protected ProductsRestApi p;
    protected String productID;
    protected String m_url;
    private String sessionToken = "";
    //private String translationsUrl;

    @BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
        m_url = url;
        filePath = configPath;
        stringsApi = new StringsRestApi();
        stringsApi.setURL(translationsUrl);
        translationsApi = new TranslationsRestApi();
        translationsApi.setURL(translationsUrl);
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
        stringID = stringsApi.addString(seasonID, str, sessionToken);
        str2 = FileUtils.fileToString(filePath + "/strings/string2.txt", "UTF-8", false);
        stringID2 =stringsApi.addString(seasonID, str2, sessionToken);
        str3 = FileUtils.fileToString(filePath + "/strings/string3.txt", "UTF-8", false);
        stringID3 =stringsApi.addString(seasonID, str3, sessionToken);
        str4 = FileUtils.fileToString(filePath + "/strings/string4.txt", "UTF-8", false);
        stringID4 =stringsApi.addString(seasonID, str4, sessionToken);
        str5 = FileUtils.fileToString(filePath + "/strings/string6.txt", "UTF-8", false);
        stringID5 =stringsApi.addString(seasonID, str5, sessionToken);
    }
    @Test (dependsOnMethods="addString",  description = "Check that the string was added to a list of all strings in season")
    public void validateStringInSeason() throws Exception{

        String response = stringsApi.getAllStrings(seasonID, sessionToken);
        JSONObject json = new JSONObject(response);
        JSONArray allStrings = json.getJSONArray("strings");
        Assert.assertTrue(allStrings.length()==5, "String was not added to a list of all strings in season");

        //check first string is in
        str = stringsApi.getString(stringID, sessionToken);
        JSONObject jsonStr = new JSONObject(str);
        JSONObject firstString = (JSONObject)allStrings.get(0);
        Assert.assertTrue(firstString.getString("key").equals(jsonStr.getString("key")), "Incorrect string key");
        Assert.assertTrue(firstString.getString("value").equals(jsonStr.getString("value")), "Incorrect string value");

        //check second string is in
        str2 = stringsApi.getString(stringID2, sessionToken);
        JSONObject jsonStr2 = new JSONObject(str2);
        JSONObject secondString = (JSONObject)allStrings.get(1);
        Assert.assertTrue(secondString.getString("key").equals(jsonStr2.getString("key")), "Incorrect string key");
        Assert.assertTrue(secondString.getString("value").equals(jsonStr2.getString("value")), "Incorrect string value");

        //check third string is in
        str3 = stringsApi.getString(stringID3, sessionToken);
        JSONObject jsonStr3 = new JSONObject(str3);
        JSONObject thirdString = (JSONObject)allStrings.get(2);
        Assert.assertTrue(thirdString.getString("key").equals(jsonStr3.getString("key")), "Incorrect string key");
        Assert.assertTrue(thirdString.getString("value").equals(jsonStr3.getString("value")), "Incorrect string value");

        //check fourth string is in
        str4 = stringsApi.getString(stringID4, sessionToken);
        JSONObject jsonStr4 = new JSONObject(str4);
        JSONObject fourthString = (JSONObject)allStrings.get(3);
        Assert.assertTrue(fourthString.getString("key").equals(jsonStr4.getString("key")), "Incorrect string key");
        Assert.assertTrue(fourthString.getString("value").equals(jsonStr4.getString("value")), "Incorrect string value");

        //check fifth string is in
        str5 = stringsApi.getString(stringID5, sessionToken);
        JSONObject jsonStr5 = new JSONObject(str5);
        JSONObject fifth = (JSONObject)allStrings.get(4);
        Assert.assertTrue(fifth.getString("key").equals(jsonStr5.getString("key")), "Incorrect string key");
        Assert.assertTrue(fifth.getString("value").equals(jsonStr5.getString("value")), "Incorrect string value");

    }

    @Test (dependsOnMethods="validateStringInSeason",  description = "mark for translation")
    public void checkStatusesAndSummary() throws Exception{
        Thread.sleep(1000);
        String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
        Thread.sleep(1000);

        String response = translationsApi.addSupportedLocales(seasonID,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add french");
        //check default and original file updated

        Thread.sleep(1000);//wait a bit for amazon to create the files
        RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getFileModificationDate("AirlockDefaults.json",m_url, productID, seasonID, dateFormat, sessionToken);
        Assert.assertTrue(responseProd.code !=304, "default file should have changed");
        responseProd = RuntimeDateUtilities.getFileModificationDate("translations/original.json",m_url, productID, seasonID, dateFormat, sessionToken);
        Assert.assertTrue(responseProd.code !=304, "original file should have changed");
        responseProd = RuntimeDateUtilities.getFileModificationDate("translations/strings__frPRODUCTION.json",m_url, productID, seasonID, dateFormat,sessionToken);
        Assert.assertTrue(responseProd.code == 200 && responseProd.code != 404, "production file should have been created");
        responseProd = RuntimeDateUtilities.getFileModificationDate("translations/strings__frDEVELOPMENT.json",m_url, productID, seasonID, dateFormat,sessionToken);
        Assert.assertTrue(responseProd.code == 200 && responseProd.code != 404, "development file should have been created");

        response = translationsApi.removeSupportedLocales(seasonID,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add french");
        //check default and original file updated

        Thread.sleep(1000);//wait a bit for amazon to create the files
        responseProd = RuntimeDateUtilities.getFileModificationDate("AirlockDefaults.json",m_url, productID, seasonID, dateFormat, sessionToken);
        Assert.assertTrue(responseProd.code !=304, "default file should have changed");
        responseProd = RuntimeDateUtilities.getFileModificationDate("translations/original.json",m_url, productID, seasonID, dateFormat, sessionToken);
        Assert.assertTrue(responseProd.code !=304, "original file should have changed");
        responseProd = RuntimeDateUtilities.getFileModificationDate("translations/strings__frPRODUCTION.json",m_url, productID, seasonID, dateFormat,sessionToken);
        Assert.assertTrue(responseProd.code == 403 || responseProd.code ==404, "production file should have been delete");
        responseProd = RuntimeDateUtilities.getFileModificationDate("translations/strings__frDEVELOPMENT.json",m_url, productID, seasonID, dateFormat,sessionToken);
        Assert.assertTrue(responseProd.code == 403 || responseProd.code ==404, "development file should have been deleted");

        response = translationsApi.addSupportedLocales(seasonID,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add french");
        //check default and original file updated

        Thread.sleep(1000);//wait a bit for amazon to create the files
        responseProd = RuntimeDateUtilities.getFileModificationDate("AirlockDefaults.json",m_url, productID, seasonID, dateFormat, sessionToken);
        Assert.assertTrue(responseProd.code !=304, "default file should have changed");
        responseProd = RuntimeDateUtilities.getFileModificationDate("translations/original.json",m_url, productID, seasonID, dateFormat, sessionToken);
        Assert.assertTrue(responseProd.code !=304, "original file should have changed");
        responseProd = RuntimeDateUtilities.getFileModificationDate("translations/strings__frPRODUCTION.json",m_url, productID, seasonID, dateFormat,sessionToken);
        Assert.assertTrue(responseProd.code != 403 && responseProd.code != 404, "production file should have been created");
        responseProd = RuntimeDateUtilities.getFileModificationDate("translations/strings__frDEVELOPMENT.json",m_url, productID, seasonID, dateFormat,sessionToken);
        Assert.assertTrue(responseProd.code != 403 && responseProd.code != 404, "development file should have been created");

        dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
        response = translationsApi.markForTranslation(seasonID,new String[]{stringID,stringID2,stringID3,stringID4},sessionToken);
        Assert.assertTrue(response.equals(""),"could not mark for translation");
        //check original updated
        responseProd = RuntimeDateUtilities.getFileModificationDate("translations/original.json",m_url, productID, seasonID, dateFormat, sessionToken);
        Assert.assertTrue(responseProd.code !=304, "production file should have changed");


        dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
        response = translationsApi.reviewForTranslation(seasonID,new String[]{stringID,stringID2,stringID3},sessionToken);
        Assert.assertTrue(response.equals(""),"could not reviewed for translation");
        //check original updated
        responseProd = RuntimeDateUtilities.getFileModificationDate("translations/original.json",m_url, productID, seasonID, dateFormat, sessionToken);
        Assert.assertTrue(responseProd.code !=304, "production file should have changed");

        dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
        response = translationsApi.sendToTranslation(seasonID,new String[]{stringID,stringID2},sessionToken);
        Assert.assertTrue(response.equals(""),"could not send to translation");

        //check original updated
        responseProd = RuntimeDateUtilities.getFileModificationDate("translations/original.json",m_url, productID, seasonID, dateFormat, sessionToken);
        Assert.assertTrue(responseProd.code !=304, "production file should have changed");

        String translationFr = FileUtils.fileToString(filePath + "strings/translationFR5.txt", "UTF-8", false);
        response = translationsApi.updateTranslation(seasonID,"fr",translationFr,sessionToken);
        Assert.assertTrue(response.equals(""),"could not translate french");

        //add file checked
        responseProd = RuntimeDateUtilities.getFileModificationDate("translations/strings__frPRODUCTION.json",m_url, productID, seasonID, dateFormat,sessionToken);
        Assert.assertTrue(responseProd.code == 304, "production file should have not change");
        responseProd = RuntimeDateUtilities.getFileModificationDate("translations/strings__frDEVELOPMENT.json",m_url, productID, seasonID, dateFormat,sessionToken);
        Assert.assertTrue(responseProd.code != 304, "development file should have change");

        //check statuses
        response = translationsApi.getStringStatuses(seasonID,sessionToken);
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray statusArray = (JSONArray) jsonResponse.get("stringsStatuses");
        checkStatus(statusArray,0,"TRANSLATION_COMPLETE");
        checkStatus(statusArray,1,"IN_TRANSLATION");
        checkStatus(statusArray,2,"REVIEWED_FOR_TRANSLATION");
        checkStatus(statusArray,3,"READY_FOR_TRANSLATION");
        checkStatus(statusArray,4,"NEW_STRING");

        //check summary
        response = translationsApi.getTranslationSummary(seasonID,new String[]{},sessionToken);
        JSONArray summaryArray = new JSONObject(response).getJSONArray("translationSummary");
        checkStatusAndTranslation(summaryArray,0,"TRANSLATION_COMPLETE",0,"TRANSLATED");
        checkStatusAndTranslation(summaryArray,1,"IN_TRANSLATION",0,"IN_TRANSLATION");
        checkStatusAndTranslation(summaryArray,2,"REVIEWED_FOR_TRANSLATION",0,"NOT_TRANSLATED");
        checkStatusAndTranslation(summaryArray,3,"READY_FOR_TRANSLATION",0,"NOT_TRANSLATED");
        checkStatusAndTranslation(summaryArray,4,"NEW_STRING",0,"NOT_TRANSLATED");

        //checkAllString
        response = stringsApi.getAllStrings(seasonID,"INCLUDE_TRANSLATIONS",sessionToken);
        JSONArray stringArray = new JSONObject(response).getJSONArray("strings");
        checkStatusAndTranslation(stringArray,0,"TRANSLATION_COMPLETE","fr","TRANSLATED");
        checkStatusAndTranslation(stringArray,1,"IN_TRANSLATION","fr",null);
        checkStatusAndTranslation(stringArray,2,"REVIEWED_FOR_TRANSLATION","fr",null);
        checkStatusAndTranslation(stringArray,3,"READY_FOR_TRANSLATION","fr",null);
        checkStatusAndTranslation(stringArray,4,"NEW_STRING","fr",null);

        //check one at time string
        response = stringsApi.getString(stringID,"INCLUDE_TRANSLATIONS",sessionToken);
        JSONObject jsonresponse = new JSONObject(response);
        Assert.assertTrue(jsonresponse.getString("status").equals("TRANSLATION_COMPLETE"),"wrong translation status for String");
        String translationStatus = jsonresponse.getJSONObject("translations").getJSONObject("fr").getString("translationStatus");
        Assert.assertTrue(translationStatus.equals("TRANSLATED"),"wrong translation status for String");

        response = stringsApi.getString(stringID2,"INCLUDE_TRANSLATIONS",sessionToken);
        jsonresponse = new JSONObject(response);
        Assert.assertTrue(jsonresponse.getString("status").equals("IN_TRANSLATION"),"wrong translation status for String");
        Assert.assertTrue(!jsonresponse.getJSONObject("translations").containsKey("fr"),"wrong translation status for String");

        response = stringsApi.getString(stringID3,"INCLUDE_TRANSLATIONS",sessionToken);
        jsonresponse = new JSONObject(response);
        Assert.assertTrue(jsonresponse.getString("status").equals("REVIEWED_FOR_TRANSLATION"),"wrong translation status for String");
        Assert.assertTrue(!jsonresponse.getJSONObject("translations").containsKey("fr"),"wrong translation status for String");

        response = stringsApi.getString(stringID4,"INCLUDE_TRANSLATIONS",sessionToken);
        jsonresponse = new JSONObject(response);
        Assert.assertTrue(jsonresponse.getString("status").equals("READY_FOR_TRANSLATION"),"wrong translation status for String");
        Assert.assertTrue(!jsonresponse.getJSONObject("translations").containsKey("fr"),"wrong translation status for String");

        //check by status

        response = translationsApi.getStringsByStatuses(seasonID,"NEW_STRING", "BASIC", sessionToken);
        jsonresponse = new JSONObject(response);
        str5 = stringsApi.getString(stringID5, sessionToken);
        JSONObject jsonStr5 = new JSONObject(str5);
        Assert.assertTrue(jsonresponse.getJSONArray("strings").getJSONObject(0).getString("key").equals(jsonStr5.getString("key")),"wrong string by status NEW_STRING");

        response = translationsApi.getStringsByStatuses(seasonID,"READY_FOR_TRANSLATION", "BASIC", sessionToken);
        jsonresponse = new JSONObject(response);
        str4 = stringsApi.getString(stringID4, sessionToken);
        JSONObject jsonStr4 = new JSONObject(str4);
        Assert.assertTrue(jsonresponse.getJSONArray("strings").getJSONObject(0).getString("key").equals(jsonStr4.getString("key")),"wrong string by status READY_FOR_TRANSLATION");

        response = translationsApi.getStringsByStatuses(seasonID,"REVIEWED_FOR_TRANSLATION", "BASIC", sessionToken);
        jsonresponse = new JSONObject(response);
        str3 = stringsApi.getString(stringID3, sessionToken);
        JSONObject jsonStr3 = new JSONObject(str3);
        Assert.assertTrue(jsonresponse.getJSONArray("strings").getJSONObject(0).getString("key").equals(jsonStr3.getString("key")),"wrong string by status REVIEWED_FOR_TRANSLATION");

        response = translationsApi.getStringsByStatuses(seasonID,"IN_TRANSLATION", "BASIC", sessionToken);
        jsonresponse = new JSONObject(response);
        str2 = stringsApi.getString(stringID2, sessionToken);
        JSONObject jsonStr2 = new JSONObject(str2);
        Assert.assertTrue(jsonresponse.getJSONArray("strings").getJSONObject(0).getString("key").equals(jsonStr2.getString("key")),"wrong string by status IN_TRANSLATION");

        response = translationsApi.getStringsByStatuses(seasonID,"TRANSLATION_COMPLETE", "BASIC", sessionToken);
        jsonresponse = new JSONObject(response);
        str = stringsApi.getString(stringID, sessionToken);
        JSONObject jsonStr = new JSONObject(str);
        Assert.assertTrue(jsonresponse.getJSONArray("strings").getJSONObject(0).getString("key").equals(jsonStr.getString("key")),"wrong string by status TRANSLATION_COMPLETE");
        response = translationsApi.getStringsByStatuses(seasonID,"TRANSLATION_COMPLETE", "INCLUDE_TRANSLATIONS", sessionToken);
        jsonresponse = new JSONObject(response);
        Assert.assertTrue(jsonresponse.getJSONArray("strings").getJSONObject(0).getJSONObject("translations").getJSONObject("fr").getString("translatedValue").equals("Bonjour"),"wrong translation for string by status TRANSLATION_COMPLETE");

    }

    @Test (dependsOnMethods="checkStatusesAndSummary",  description = "checkStatusesAndSummary after new locale")
    public void checkStatusesAndSummaryNewLocale() throws Exception {
        //add spanish
        Thread.sleep(1000);
        String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
        Thread.sleep(1000);

        String response = translationsApi.addSupportedLocales(seasonID, "es", sessionToken);
        Assert.assertTrue(response.equals(""), "could not add spanish");

        //check files updated and created
        Thread.sleep(1000);//wait a bit for amazon to create the files
        RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getFileModificationDate("AirlockDefaults.json",m_url, productID, seasonID, dateFormat, sessionToken);
        Assert.assertTrue(responseProd.code !=304, "default file should have changed");
        responseProd = RuntimeDateUtilities.getFileModificationDate("translations/original.json",m_url, productID, seasonID, dateFormat, sessionToken);
        Assert.assertTrue(responseProd.code !=304, "original file should have changed");
        responseProd = RuntimeDateUtilities.getFileModificationDate("translations/strings__esPRODUCTION.json",m_url, productID, seasonID, dateFormat,sessionToken);
        Assert.assertTrue(responseProd.code != 403  && responseProd.code != 404, "production file should have been created");
        responseProd = RuntimeDateUtilities.getFileModificationDate("translations/strings__esDEVELOPMENT.json",m_url, productID, seasonID, dateFormat,sessionToken);
        Assert.assertTrue(responseProd.code != 403 && responseProd.code != 404, "development file should have been created");


        //re check statuses
        response = translationsApi.getStringStatuses(seasonID, sessionToken);
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray statusArray = (JSONArray) jsonResponse.get("stringsStatuses");
        checkStatus(statusArray, 0, "IN_TRANSLATION");
        checkStatus(statusArray, 1, "IN_TRANSLATION");
        checkStatus(statusArray, 2, "REVIEWED_FOR_TRANSLATION");
        checkStatus(statusArray, 3, "READY_FOR_TRANSLATION");
        checkStatus(statusArray, 4, "NEW_STRING");

        response = translationsApi.getTranslationSummary(seasonID, new String[]{}, sessionToken);
        JSONArray summaryArray = new JSONObject(response).getJSONArray("translationSummary");
        //recheck french
        checkStatusAndTranslation(summaryArray, 0, "IN_TRANSLATION", 0, "TRANSLATED");
        checkStatusAndTranslation(summaryArray, 1, "IN_TRANSLATION", 0, "IN_TRANSLATION");
        checkStatusAndTranslation(summaryArray, 2, "REVIEWED_FOR_TRANSLATION", 0, "NOT_TRANSLATED");
        checkStatusAndTranslation(summaryArray, 3, "READY_FOR_TRANSLATION", 0, "NOT_TRANSLATED");
        checkStatusAndTranslation(summaryArray, 4, "NEW_STRING", 0, "NOT_TRANSLATED");
        //checkspanish
        checkTranslation(summaryArray, 0, 1, "IN_TRANSLATION");
        checkTranslation(summaryArray, 1, 1, "IN_TRANSLATION");
        checkTranslation(summaryArray, 2, 1, "NOT_TRANSLATED");
        checkTranslation(summaryArray, 3, 1, "NOT_TRANSLATED");
        checkTranslation(summaryArray, 4, 1, "NOT_TRANSLATED");

    }
    @Test (dependsOnMethods="checkStatusesAndSummaryNewLocale",  description = "checkStatusesAndSummary after override")
    public void checkStatusesAndSummaryOverride() throws Exception{

        //override one and translate
        String response = translationsApi.overrideTranslate(stringID,"es","hola",sessionToken);
        Assert.assertTrue(response.equals(""),"could not override string: "+stringID);
        response = translationsApi.getTranslationSummary(seasonID,new String[]{},sessionToken);
        JSONArray summaryArray = new JSONObject(response).getJSONArray("translationSummary");
        checkStatusAndTranslation(summaryArray,0,"TRANSLATION_COMPLETE",0,"TRANSLATED");
        checkTranslation(summaryArray,0,1,"OVERRIDE");

        String translationEs = FileUtils.fileToString(filePath + "strings/translationES.txt", "UTF-8", false);
        response = translationsApi.updateTranslation(seasonID,"es",translationEs,sessionToken);
        Assert.assertTrue(response.equals(""),"could not translate spanish");
        response = translationsApi.getTranslationSummary(seasonID,new String[]{},sessionToken);
        summaryArray = new JSONObject(response).getJSONArray("translationSummary");
        checkStatusAndTranslation(summaryArray,0,"TRANSLATION_COMPLETE",0,"TRANSLATED");
        checkTranslation(summaryArray,0,1,"OVERRIDE");

        //override all
        response = translationsApi.overrideTranslate(stringID,"fr","hola",sessionToken);
        Assert.assertTrue(response.equals(""),"could not cancel override string: "+stringID);
        response = translationsApi.overrideTranslate(stringID2,"fr","hola",sessionToken);
        Assert.assertTrue(response.equals(""),"could not cancel override string: "+stringID2);
        response = translationsApi.overrideTranslate(stringID3,"fr","hola",sessionToken);
        Assert.assertTrue(response.equals(""),"could not cancel override string: "+stringID3);
        response = translationsApi.overrideTranslate(stringID4,"fr","hola",sessionToken);
        Assert.assertTrue(response.equals(""),"could not cancel override string: "+stringID4);
        response = translationsApi.overrideTranslate(stringID5,"fr","hola",sessionToken);
        Assert.assertTrue(response.equals(""),"could not cancel override string: "+stringID5);

        response = translationsApi.getTranslationSummary(seasonID,new String[]{},sessionToken);
        summaryArray = new JSONObject(response).getJSONArray("translationSummary");
        checkStatusAndTranslation(summaryArray,0,"TRANSLATION_COMPLETE",0,"OVERRIDE");
        checkStatusAndTranslation(summaryArray,1,"TRANSLATION_COMPLETE",0,"OVERRIDE");
        checkStatusAndTranslation(summaryArray,2,"REVIEWED_FOR_TRANSLATION",0,"OVERRIDE");
        checkStatusAndTranslation(summaryArray,3,"READY_FOR_TRANSLATION",0,"OVERRIDE");
        checkStatusAndTranslation(summaryArray,4,"NEW_STRING",0,"OVERRIDE");
    }

    @Test (dependsOnMethods="checkStatusesAndSummaryOverride",  description = "checkStatusesAndSummary after override")
    public void checkStatusesAndSummaryCancelOverride() throws Exception{
        //cancel override all
        String response = translationsApi.cancelOverride(stringID,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not cancel override string: "+stringID);
        response = translationsApi.cancelOverride(stringID2,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not cancel override string: "+stringID2);
        response = translationsApi.cancelOverride(stringID3,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not cancel override string: "+stringID3);
        response = translationsApi.cancelOverride(stringID4,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not cancel override string: "+stringID4);
        response = translationsApi.cancelOverride(stringID5,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not cancel override string: "+stringID5);

        response = translationsApi.getTranslationSummary(seasonID,new String[]{},sessionToken);
        JSONArray summaryArray = new JSONObject(response).getJSONArray("translationSummary");
        checkStatusAndTranslation(summaryArray,0,"TRANSLATION_COMPLETE",0,"TRANSLATED");
        checkStatusAndTranslation(summaryArray,1,"IN_TRANSLATION",0,"IN_TRANSLATION");
        checkStatusAndTranslation(summaryArray,2,"REVIEWED_FOR_TRANSLATION",0,"NOT_TRANSLATED");
        checkStatusAndTranslation(summaryArray,3,"READY_FOR_TRANSLATION",0,"NOT_TRANSLATED");
        checkStatusAndTranslation(summaryArray,4,"NEW_STRING",0,"NOT_TRANSLATED");
    }
    @Test (dependsOnMethods="checkStatusesAndSummaryCancelOverride",  description = "checkStatusesAndSummary after override")
    public void checkStatusesAndSummaryRemoveLocale() throws Exception{

        String response = translationsApi.overrideTranslate(stringID,"fr","hola",sessionToken);
        Assert.assertTrue(response.equals(""),"could not cancel override string: "+stringID);
        response = translationsApi.getTranslationSummary(seasonID,new String[]{},sessionToken);
        JSONArray summaryArray = new JSONObject(response).getJSONArray("translationSummary");
        checkStatusAndTranslation(summaryArray,0,"TRANSLATION_COMPLETE",0,"OVERRIDE");

        response = translationsApi.removeSupportedLocales(seasonID,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not remove french");

        response = translationsApi.getTranslationSummary(seasonID,new String[]{},sessionToken);
        summaryArray = new JSONObject(response).getJSONArray("translationSummary");
        // only es translation
        checkStatusAndTranslation(summaryArray,0,"TRANSLATION_COMPLETE",0,"OVERRIDE");
        checkStatusAndTranslation(summaryArray,1,"TRANSLATION_COMPLETE",0,"TRANSLATED");
        checkStatusAndTranslation(summaryArray,2,"REVIEWED_FOR_TRANSLATION",0,"NOT_TRANSLATED");
        checkStatusAndTranslation(summaryArray,3,"READY_FOR_TRANSLATION",0,"NOT_TRANSLATED");
        checkStatusAndTranslation(summaryArray,4,"NEW_STRING",0,"NOT_TRANSLATED");

        response = translationsApi.addSupportedLocales(seasonID,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not readd french");

        response = translationsApi.getTranslationSummary(seasonID,new String[]{},sessionToken);
        summaryArray = new JSONObject(response).getJSONArray("translationSummary");
        //now french is second in the translations
        checkStatusAndTranslation(summaryArray,0,"IN_TRANSLATION",1,"IN_TRANSLATION");
        checkStatusAndTranslation(summaryArray,1,"IN_TRANSLATION",1,"IN_TRANSLATION");
        checkStatusAndTranslation(summaryArray,2,"REVIEWED_FOR_TRANSLATION",1,"NOT_TRANSLATED");
        checkStatusAndTranslation(summaryArray,3,"READY_FOR_TRANSLATION",1,"NOT_TRANSLATED");
        checkStatusAndTranslation(summaryArray,4,"NEW_STRING",1,"NOT_TRANSLATED");

    }

    @Test (dependsOnMethods="checkStatusesAndSummaryRemoveLocale",  description = "checkStatusesAndSummary after update")
    public void checkStatusesAndSummaryAfterUpdate() throws Exception {
        str = stringsApi.getString(stringID, sessionToken);
        str = str.replace("Hello","HELLO");
        String response = stringsApi.updateString(stringID,str,sessionToken);
        Assert.assertTrue(!response.contains("error"),"could not update string: "+stringID);
        Assert.assertTrue(new JSONObject(stringsApi.getString(stringID, sessionToken)).getString("translationInstruction").equals("short"), "Incorrect string translationInstruction");
        str2 = stringsApi.getString(stringID2, sessionToken);
        str2 = str2.replace("Hi","HI");
        response = stringsApi.updateString(stringID2,str2,sessionToken);
        Assert.assertTrue(!response.contains("error"),"could not update string: "+stringID2);
        Assert.assertTrue(new JSONObject(stringsApi.getString(stringID2, sessionToken)).get("translationInstruction") == null, "Incorrect string translationInstruction");
        str3 = stringsApi.getString(stringID3, sessionToken);
        str3 = str3.replace("Mary","bob");
        response = stringsApi.updateString(stringID3,str3,sessionToken);
        Assert.assertTrue(!response.contains("error"),"could not update string: "+stringID3);
        Assert.assertTrue(new JSONObject(stringsApi.getString(stringID3, sessionToken)).getString("translationInstruction").equals("long"), "Incorrect string translationInstruction");
        str4 = stringsApi.getString(stringID4, sessionToken);
        str4 = str4.replace("NewsHello","NewHello");
        response = stringsApi.updateString(stringID4,str4,sessionToken);
        Assert.assertTrue(!response.contains("error"),"could not update string: "+stringID4);
        response = translationsApi.getTranslationSummary(seasonID,new String[]{},sessionToken);
        JSONArray summaryArray = new JSONObject(response).getJSONArray("translationSummary");
        checkStatusAndTranslation(summaryArray,0,"NEW_STRING",0,"NOT_TRANSLATED");
        checkStatusAndTranslation(summaryArray,1,"NEW_STRING",0,"NOT_TRANSLATED");
        checkStatusAndTranslation(summaryArray,2,"REVIEWED_FOR_TRANSLATION",0,"NOT_TRANSLATED");
        checkStatusAndTranslation(summaryArray,3,"READY_FOR_TRANSLATION",0,"NOT_TRANSLATED");
        checkStatusAndTranslation(summaryArray,4,"NEW_STRING",0,"NOT_TRANSLATED");

    }

    private void checkStatus(JSONArray statusArray,int statusIndex,String expectedStatus)throws Exception{
        JSONObject stringObject = (JSONObject)statusArray.get(statusIndex);
        String status = stringObject.getString("status");
        Assert.assertTrue(status.equals(expectedStatus),"wrong status for String");

    }
    private void checkTranslation(JSONArray summaryArray,int summaryIndex,int translationIndex,String expectedTranslation)throws Exception{
        JSONObject stringObject = (JSONObject)summaryArray.get(summaryIndex);
        String translationStatus = stringObject.getJSONArray("translations").getJSONObject(translationIndex).getString("translationStatus");
        Assert.assertTrue(translationStatus.equals(expectedTranslation),"wrong translation status for String");
    }
    private void checkTranslation(JSONArray summaryArray,int summaryIndex,String locale,String expectedTranslation)throws Exception{
        JSONObject stringObject = (JSONObject)summaryArray.get(summaryIndex);
        JSONObject translations = stringObject.getJSONObject("translations");
        if(translations.containsKey(locale)) {
            String translationStatus = translations.getJSONObject(locale).getString("translationStatus");
            Assert.assertTrue(translationStatus.equals(expectedTranslation), "wrong translation status for String");
        }
        else {
            if(expectedTranslation != null){
                Assert.fail("expecting translation: " +expectedTranslation);
            }
        }
    }
    private void checkStatusAndTranslation(JSONArray summaryArray,int summaryIndex,String expectedStatus,int translationIndex,String expectedTranslation)throws Exception{
        checkStatus(summaryArray,summaryIndex,expectedStatus);
        checkTranslation(summaryArray,summaryIndex,translationIndex,expectedTranslation);
    }
    private void checkStatusAndTranslation(JSONArray summaryArray,int summaryIndex,String expectedStatus,String locale,String expectedTranslation)throws Exception{
        checkStatus(summaryArray,summaryIndex,expectedStatus);
        checkTranslation(summaryArray,summaryIndex,locale,expectedTranslation);
    }

    @AfterTest
    private void reset(){
        baseUtils.reset(productID, sessionToken);
    }
}
