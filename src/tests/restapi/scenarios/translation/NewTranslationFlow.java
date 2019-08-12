package tests.restapi.scenarios.translation;

import com.ibm.airlock.Strings;
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
public class NewTranslationFlow {

    protected String seasonID;
    protected String seasonID2;
    protected String stringID;
    protected String stringID2;
    protected String stringID3;
    protected String stringID4;
    protected String filePath;
    protected String str;
    protected String str2;
    protected String str3;
    protected String str4;
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
    }
    @Test (dependsOnMethods="addString",  description = "Check that the string was added to a list of all strings in season")
    public void validateStringInSeason() throws Exception{

        String response = stringsApi.getAllStrings(seasonID, sessionToken);
        JSONObject json = new JSONObject(response);
        JSONArray allStrings = json.getJSONArray("strings");
        Assert.assertTrue(allStrings.length()==4, "String was not added to a list of all strings in season");

        //check first string is in
        str = stringsApi.getString(stringID, sessionToken);
        JSONObject jsonStr = new JSONObject(str);
        JSONObject firstString = (JSONObject)allStrings.get(0);
        Assert.assertTrue(firstString.getString("key").equals(jsonStr.getString("key")), "Incorrect string key");
        Assert.assertTrue(firstString.getString("value").equals(jsonStr.getString("value")), "Incorrect string value");
        Assert.assertTrue(firstString.getString("translationInstruction").equals("short"), "Incorrect string translationInstruction");

        //check second string is in
        str2 = stringsApi.getString(stringID2, sessionToken);
        JSONObject jsonStr2 = new JSONObject(str2);
        JSONObject secondString = (JSONObject)allStrings.get(1);
        Assert.assertTrue(secondString.getString("key").equals(jsonStr2.getString("key")), "Incorrect string key");
        Assert.assertTrue(secondString.getString("value").equals(jsonStr2.getString("value")), "Incorrect string value");
        Assert.assertTrue(secondString.get("translationInstruction") == null, "Incorrect string translationInstruction");

        //check third string is in
        str3 = stringsApi.getString(stringID3, sessionToken);
        JSONObject jsonStr3 = new JSONObject(str3);
        JSONObject thirdString = (JSONObject)allStrings.get(2);
        Assert.assertTrue(thirdString.getString("key").equals(jsonStr3.getString("key")), "Incorrect string key");
        Assert.assertTrue(thirdString.getString("value").equals(jsonStr3.getString("value")), "Incorrect string value");
        Assert.assertTrue(thirdString.getString("translationInstruction").equals("long"), "Incorrect string translationInstruction");

        //check fourth string is in
        str4 = stringsApi.getString(stringID4, sessionToken);
        JSONObject jsonStr4 = new JSONObject(str4);
        JSONObject fourthString = (JSONObject)allStrings.get(3);
        Assert.assertTrue(fourthString.getString("key").equals(jsonStr4.getString("key")), "Incorrect string key");
        Assert.assertTrue(fourthString.getString("value").equals(jsonStr4.getString("value")), "Incorrect string value");
        Assert.assertTrue(fourthString.get("translationInstruction") == null, "Incorrect string translationInstruction");
    }

    @Test(dependsOnMethods="validateStringInSeason",description = "add supported locale")
    public void addSupportedLocal() throws Exception{
        String response = translationsApi.addSupportedLocales(seasonID,"en",sessionToken);
        Assert.assertTrue(response.contains("error"),"could add english");
        // response = translationsApi.addSupportedLocales(seasonID,"sdfsdf",sessionToken);
        //Assert.assertTrue(response.contains("error"),"could add weird");
        response = translationsApi.addSupportedLocales(seasonID,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add french");
        response = translationsApi.addSupportedLocales(seasonID,"fr",sessionToken);
        Assert.assertTrue(response.contains("error"),"could add twice");
        response = translationsApi.getSupportedLocales(seasonID,sessionToken);
        Assert.assertTrue(response.contains("\"fr\""),"missing locale");
        //try to translate should fail
        String translationFr = FileUtils.fileToString(filePath + "strings/translationFR.txt", "UTF-8", false);
        response = translationsApi.updateTranslation(seasonID,"fr",translationFr,sessionToken);
        Assert.assertTrue(response.contains("in the NEW_STRING status. Only strings in IN_TRANSLATION and TRANSLATION_COMPLETE statuses can recieve translation values"),"could translate french");
    }
    @Test(dependsOnMethods="addSupportedLocal",description = "add supported locale")
    public void removeSupportedLocal() throws Exception{
        String response = translationsApi.removeSupportedLocales(seasonID,"en",sessionToken);
        Assert.assertTrue(response.contains("error"),"could remove english");
        // response = translationsApi.addSupportedLocales(seasonID,"sdfsdf",sessionToken);
        //Assert.assertTrue(response.contains("error"),"could add weird");
        response = translationsApi.removeSupportedLocales(seasonID,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not remove french");
        response = translationsApi.removeSupportedLocales(seasonID,"fr",sessionToken);
        Assert.assertTrue(response.contains("error"),"could remove twice");
        response = translationsApi.getSupportedLocales(seasonID,sessionToken);
        Assert.assertTrue(!response.contains("\"fr\""),"not removed locale");
        //restore french
        response = translationsApi.addSupportedLocales(seasonID,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add french");
    }

    @Test (dependsOnMethods="removeSupportedLocal",  description = "mark for translation")
    public void markForTranslation() throws Exception{
        String response = translationsApi.markForTranslation(seasonID,new String[]{},sessionToken);
        Assert.assertTrue(response.contains(Strings.idsMissing),"could mark nothing");
        response = translationsApi.markForTranslation(seasonID,new String[]{stringID},sessionToken);
        Assert.assertTrue(response.equals(""),"could not mark string: "+stringID);
        response = translationsApi.markForTranslation(seasonID,new String[]{stringID},sessionToken);
        Assert.assertTrue(response.contains("is not in NEW_STRING status and therefore cannot be markForTranslation"),"could mark twice string: "+stringID);
        response = translationsApi.markForTranslation(seasonID,new String[]{stringID2,stringID3},sessionToken);
        Assert.assertTrue(response.equals(""),"could not mark strings: "+stringID2+" "+stringID3);
        response = translationsApi.markForTranslation(seasonID,new String[]{stringID,stringID4},sessionToken);
        Assert.assertTrue(response.contains("is not in NEW_STRING status and therefore cannot be markForTranslation"),"could mark twice string: "+stringID);
    }

    @Test (dependsOnMethods="markForTranslation",  description = "mark for translation")
    public void reviewForTranslation() throws Exception{
        String response = translationsApi.reviewForTranslation(seasonID,new String[]{},sessionToken);
        Assert.assertTrue(response.contains(Strings.idsMissing),"could review nothing");
        response = translationsApi.reviewForTranslation(seasonID,new String[]{stringID},sessionToken);
        Assert.assertTrue(response.equals(""),"could not review string: "+stringID);
        response = translationsApi.reviewForTranslation(seasonID,new String[]{stringID},sessionToken);
        Assert.assertTrue(response.contains("is not in READY_FOR_TRANSLATION status and therefore cannot be reviewedForTranslation"),"could mark twice string: "+stringID);
        response = translationsApi.reviewForTranslation(seasonID,new String[]{stringID2,stringID3},sessionToken);
        Assert.assertTrue(response.equals(""),"could not mark strings: "+stringID2+" "+stringID3);
        response = translationsApi.reviewForTranslation(seasonID,new String[]{stringID,stringID4},sessionToken);
        Assert.assertTrue(response.contains("is not in READY_FOR_TRANSLATION status and therefore cannot be reviewedForTranslation"),"could mark twice string: "+stringID);

    }

    @Test (dependsOnMethods="reviewForTranslation",  description = "get new string for translation")
    public void getNewTranslation() throws Exception{
        String response = translationsApi.getNewStringsForTranslation(seasonID,new String[]{},sessionToken);
        JSONObject jsonResponse = new JSONObject(response);
        Assert.assertTrue(jsonResponse.containsKey("translationKey3"),"missing string for translation");
        Assert.assertTrue(jsonResponse.getJSONObject("translationKey3").getString("instruction").equals("long"),"missing translationInstruction");
        Assert.assertTrue(!jsonResponse.containsKey("translationKey4"),"too many string for translation");
        response = translationsApi.getNewStringsForTranslation(seasonID,new String[]{stringID},sessionToken);
        jsonResponse = new JSONObject(response);
        Assert.assertTrue(jsonResponse.containsKey("translationKey1"),"missing string for translation");
        Assert.assertTrue(jsonResponse.getJSONObject("translationKey1").get("instruction").equals("short"),"missing translationInstruction");
        Assert.assertTrue(!jsonResponse.containsKey("translationKey2"),"too many string for translation");
        String response2 = translationsApi.getNewStringsForTranslation(seasonID,new String[]{stringID},sessionToken);
        Assert.assertTrue(response.equals(response2),"get is not the same for: "+stringID);
        response = translationsApi.getNewStringsForTranslation(seasonID,new String[]{stringID2,stringID3},sessionToken);
        jsonResponse = new JSONObject(response);
        Assert.assertTrue(jsonResponse.containsKey("translationKey2"),"missing string for translation");
        Assert.assertTrue(jsonResponse.getJSONObject("translationKey2").getString("instruction").equals("long"),"missing translationInstruction");
        Assert.assertTrue(!jsonResponse.containsKey("translationKey3"),"too many string for translation");
    }

    @Test (dependsOnMethods="getNewTranslation",  description = "send new string for translation")
    public void sendToTranslation() throws Exception{
        String response = translationsApi.sendToTranslation(seasonID,new String[]{},sessionToken);
        Assert.assertTrue(response.contains(Strings.idsMissing),"could mark nothing");
        response = translationsApi.sendToTranslation(seasonID,new String[]{stringID},sessionToken);
        Assert.assertTrue(response.equals(""),"could not send string: "+stringID);
        response = translationsApi.sendToTranslation(seasonID,new String[]{stringID},sessionToken);
        Assert.assertTrue(response.contains("is not in REVIEWED_FOR_TRANSLATION status and therefore cannot be sendToTranslation"),"could send twice string: "+stringID);
        //try to remark should fail
        response = translationsApi.markForTranslation(seasonID,new String[]{stringID},sessionToken);
        Assert.assertTrue(response.contains("is not in NEW_STRING status and therefore cannot be markForTranslation"),"could mark twice string: "+stringID);
        //try to rereviewed should fail
        response = translationsApi.reviewForTranslation(seasonID,new String[]{stringID},sessionToken);
        Assert.assertTrue(response.contains("is not in READY_FOR_TRANSLATION status and therefore cannot be reviewedForTranslation"),"could mark twice string: "+stringID);
        response = translationsApi.sendToTranslation(seasonID,new String[]{stringID2,stringID3},sessionToken);
        Assert.assertTrue(response.equals(""),"could not send strings: "+stringID2+" "+stringID3);
        response = translationsApi.sendToTranslation(seasonID,new String[]{stringID,stringID4},sessionToken);
        Assert.assertTrue(response.contains("is not in REVIEWED_FOR_TRANSLATION status and therefore cannot be sendToTranslation"),"could send twice string: "+stringID);
    }

    @Test (dependsOnMethods="sendToTranslation",  description = "override translation")
    public void overrideTranslation() throws Exception{
        //override
        String response = translationsApi.overrideTranslate(stringID,"fr","",sessionToken);
        Assert.assertTrue(response.equals(""),"could not  override string: "+stringID);
        JSONObject allFrTransStrings = getAllLocalString(translationsApi,"fr","DEVELOPMENT");
        checkIfStringExist(stringID,allFrTransStrings,"");

        response = translationsApi.overrideTranslate(stringID,"fr","salut",sessionToken);
        Assert.assertTrue(response.equals(""),"could not  override string: "+stringID);
        allFrTransStrings = getAllLocalString(translationsApi,"fr","DEVELOPMENT");
        checkIfStringExist(stringID,allFrTransStrings,"salut");

        response = translationsApi.overrideTranslate(stringID,"fr","bouh",sessionToken);
        Assert.assertTrue(response.equals(""),"could not reoverride string: "+stringID);
        allFrTransStrings = getAllLocalString(translationsApi,"fr","DEVELOPMENT");
        checkIfStringExist(stringID,allFrTransStrings,"bouh");

        response = translationsApi.overrideTranslate(stringID,"es","hola",sessionToken);
        Assert.assertTrue(response.contains(Strings.localNotSupported),"could override in unsupported locale string: "+stringID);

        String translationFr = FileUtils.fileToString(filePath + "strings/translationFR.txt", "UTF-8", false);
        response = translationsApi.updateTranslation(seasonID,"fr",translationFr,sessionToken);
        Assert.assertTrue(response.equals(""),"could not translate french");
        allFrTransStrings = getAllLocalString(translationsApi,"fr","DEVELOPMENT");
        checkIfStringExist(stringID,allFrTransStrings,"bouh");
        checkIfStringExist(stringID2,allFrTransStrings,"Salut");

        //try to retranslate
        response = translationsApi.updateTranslation(seasonID,"fr",translationFr,sessionToken);
        Assert.assertTrue(response.equals(""),"could not translate french");
        allFrTransStrings = getAllLocalString(translationsApi,"fr","DEVELOPMENT");
        checkIfStringExist(stringID,allFrTransStrings,"bouh");
        checkIfStringExist(stringID2,allFrTransStrings,"Salut");

        //some more negative
        response = translationsApi.markForTranslation(seasonID,new String[]{stringID},sessionToken);
        Assert.assertTrue(response.contains("is not in NEW_STRING status and therefore cannot be markForTranslation"),"could mark override string: "+stringID);
        response = translationsApi.reviewForTranslation(seasonID,new String[]{stringID},sessionToken);
        Assert.assertTrue(response.contains("is not in READY_FOR_TRANSLATION status and therefore cannot be reviewedForTranslation"),"could send override string: "+stringID);
        response = translationsApi.sendToTranslation(seasonID,new String[]{stringID},sessionToken);
        Assert.assertTrue(response.contains("is not in REVIEWED_FOR_TRANSLATION status and therefore cannot be sendToTranslation"),"could send override string: "+stringID);

        response = translationsApi.markForTranslation(seasonID,new String[]{stringID2},sessionToken);
        Assert.assertTrue(response.contains("is not in NEW_STRING status and therefore cannot be markForTranslation"),"could mark override string: "+stringID2);
        response = translationsApi.reviewForTranslation(seasonID,new String[]{stringID2},sessionToken);
        Assert.assertTrue(response.contains("is not in READY_FOR_TRANSLATION status and therefore cannot be reviewedForTranslation"),"could send override string: "+stringID);
        response = translationsApi.sendToTranslation(seasonID,new String[]{stringID2},sessionToken);
        Assert.assertTrue(response.contains("is not in REVIEWED_FOR_TRANSLATION status and therefore cannot be sendToTranslation"),"could send twice string: "+stringID2);

        //override after translate
        response = translationsApi.overrideTranslate(stringID,"fr","salut",sessionToken);
        Assert.assertTrue(response.equals(""),"could not override translated string: "+stringID);
        allFrTransStrings = getAllLocalString(translationsApi,"fr","DEVELOPMENT");
        checkIfStringExist(stringID,allFrTransStrings,"salut");

        response = translationsApi.overrideTranslate(stringID2,"fr","bonjour",sessionToken);
        Assert.assertTrue(response.equals(""),"could not override translated string: "+stringID2);
        allFrTransStrings = getAllLocalString(translationsApi,"fr","DEVELOPMENT");
        checkIfStringExist(stringID2,allFrTransStrings,"bonjour");

    }

    @Test (dependsOnMethods="overrideTranslation",  description = "override translation")
    public void cancelOverride() throws Exception {

        String response = translationsApi.cancelOverride(stringID,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not cancel override string: "+stringID);
        JSONObject allFrTransStrings = getAllLocalString(translationsApi,"fr","DEVELOPMENT");
        checkIfStringExist(stringID,allFrTransStrings,"bouh");

        response = translationsApi.cancelOverride(stringID2,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not cancel override string: "+stringID2);
        allFrTransStrings = getAllLocalString(translationsApi,"fr","DEVELOPMENT");
        checkIfStringExist(stringID2,allFrTransStrings,"Salut");

        response = translationsApi.cancelOverride(stringID2,"fr",sessionToken);
        Assert.assertTrue(response.contains("The specified string does not have an override value for the specified locale"),"could  cancel override twice: "+stringID2);
        response = translationsApi.cancelOverride(stringID2,"jp",sessionToken);
        Assert.assertTrue(response.contains(Strings.localNotSupported),"could  cancel override twice: "+stringID2);
    }
    @Test (dependsOnMethods="cancelOverride",  description = "override translation")
    public void oldFlow() throws Exception{
        String response = translationsApi.stringForTranslation(seasonID,sessionToken);
        JSONObject jsonResponse = new JSONObject(response);
        Assert.assertTrue(jsonResponse.getJSONObject("translationKey1").getString("instruction").equals("short"),"invalid instruction"+stringID);
        Assert.assertTrue(!jsonResponse.getJSONObject("translationKey2").containsKey("instruction"),"invalid instruction"+stringID);
        Assert.assertTrue(jsonResponse.getJSONObject("translationKey3").getString("instruction").equals("long"),"invalid instruction"+stringID);
        Assert.assertTrue(!jsonResponse.getJSONObject("translationKey4").containsKey("instruction"),"invalid instruction"+stringID);
    }

    @Test (dependsOnMethods="oldFlow",  description = "duplicate translation")
    public void duplicateAndUpdate() throws Exception{
        String translationFr = FileUtils.fileToString(filePath + "strings/translationFR.txt", "UTF-8", false);
        String response = translationsApi.updateTranslation(seasonID,"fr",translationFr,sessionToken);
        String season = FileUtils.fileToString(filePath + "season2.txt", "UTF-8", false);
        SeasonsRestApi s = new SeasonsRestApi();
        seasonID2 = s.addSeason(productID, season, sessionToken);
        String translationFr2 = FileUtils.fileToString(filePath + "strings/translationFR6.txt", "UTF-8", false);
        response = translationsApi.updateTranslation(seasonID,"fr",translationFr2,sessionToken);
        String stringsInSeason = stringsApi.getAllStrings(seasonID2, sessionToken);
        JSONObject stringsInSeasonJson = new JSONObject(stringsInSeason);
        JSONArray strings = stringsInSeasonJson.getJSONArray("strings");
        String uniqueId = null;
        for(int i  = 0; i<strings.size();++i){
            if(strings.getJSONObject(i).getString("key").equals("app.hello")){
                uniqueId = strings.getJSONObject(i).getString("uniqueId");
                break;
            }
        }
        Assert.assertTrue(uniqueId!= null, "Strings not copied between season");
        String str1 = stringsApi.getString(uniqueId, "INCLUDE_TRANSLATIONS", sessionToken);
        JSONObject str1Json = new JSONObject(str1);
        Assert.assertTrue(str1Json.getJSONObject("translations").getJSONObject("fr").getString("translatedValue").equals("Bonjour"), "Translation for string1 in french was updated");

    }

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

    private void checkIfStringExist(String id,JSONObject allTransStrings,String expectedValue) throws Exception{
        String str = stringsApi.getString(id, sessionToken);
        JSONObject jsonStr = new JSONObject(str);
        Assert.assertTrue(allTransStrings.has(jsonStr.getString("key")), "The string key was not found in translations");
        if(expectedValue == null)
            expectedValue = jsonStr.getString("value");
        Assert.assertTrue(allTransStrings.getString(jsonStr.getString("key")).equals(expectedValue), "The string value was not found in translations");
    }


    @AfterTest
    private void reset(){
        baseUtils.reset(productID, sessionToken);
    }
}
