package tests.restapi.scenarios.inputSample;

import com.ibm.airlock.Strings;
import org.apache.wink.json4j.JSONException;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.*;



public class getInputSample {
    protected String seasonID;
    protected String productID;
    protected String filePath;
    protected ProductsRestApi p;
    protected InputSchemaRestApi schema;
    private String sessionToken = "";
    protected AirlockUtils baseUtils;
    protected String murl;



    @BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
        filePath = configPath;
        murl=url;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
        p = new ProductsRestApi();
        p.setURL(url);
        schema = new InputSchemaRestApi();
        schema.setURL(url);

        productID = baseUtils.createProduct();
        baseUtils.printProductToFile(productID);
        seasonID = baseUtils.createSeason(productID);

    }


    @Test(description = "invalid params")
    private void invalidCalls(){
        try {
            // invalid stage
            RestClientUtils.RestCallResults res = RestClientUtils.sendGet(murl+"/products/seasons/" + seasonID + "/inputsample?minappversion=1.1.1&generationmode=MAXIMAL", sessionToken);
            Assert.assertTrue(res.message.contains(Strings.stageMissing));
            String result = schema.getInputSample(seasonID, "DEV", "1.1.1", sessionToken, "MAXIMAL", 0.7);
            Assert.assertTrue(result.contains("The legal values are"));
            //missingMinAppVersion
            res = RestClientUtils.sendGet(murl+"/products/seasons/" + seasonID + "/inputsample?stage=DEVELOPMENT&generationmode=MAXIMAL", sessionToken);
            Assert.assertTrue(res.message.contains(  Strings.minAppMissing));
            //invalid generationmode
            res = RestClientUtils.sendGet(murl+"/products/seasons/" + seasonID + "/inputsample?stage=DEVELOPMENT&minappversion=1.1.1", sessionToken);
            Assert.assertTrue(res.message.contains( Strings.generationModeMissing));
            result = schema.getInputSample(seasonID, "DEVELOPMENT", "1.1.1", sessionToken, "MAX", 0.7);
            Assert.assertTrue(result.contains("The legal values are"));
            //invalid randomize
            result = schema.getInputSample(seasonID, "DEVELOPMENT", "1.1.1", sessionToken, "MAXIMAL", 1.7);
            Assert.assertTrue(result.contains("invalid randomizer 1.7"));
        }catch (Exception e){
            Assert.fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "invalidCalls",  description = "test min app version field")
    private void minAppVersion(){
        try {
            String sch = schema.getInputSchema(seasonID, sessionToken);
            JSONObject jsonSchema = new JSONObject(sch);
            String schemaBody = FileUtils.fileToString(filePath + "validInputSchema/inputSchemaForUtilities.txt", "UTF-8", false);
            jsonSchema.put("inputSchema", new JSONObject(schemaBody));
            schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
            String result = schema.getInputSample(seasonID, "DEVELOPMENT", "1.1.1", sessionToken, "MAXIMAL", 0.7);
            boolean found = isKeyValInJson (new JSONObject(result), "context.testData.dsx.mode", "Lorem ipsum laborum dolor");
            Assert.assertFalse(found);
            result = schema.getInputSample(seasonID, "DEVELOPMENT", "1.1.2", sessionToken, "MAXIMAL", 0.7);
            found = isKeyValInJson (new JSONObject(result), "context.testData.dsx.mode", "Lorem ipsum laborum dolor");
            JSONObject sample = new JSONObject(result);
            Assert.assertTrue(found);
        }catch (Exception e){
            Assert.fail(e.getMessage());
        }
    }
    
    private boolean isKeyValInJson(JSONObject jsonObject, String key, String value) throws JSONException {
		String[] fields = key.split("\\.");
		boolean found = true;
		String foundValue = null;
		for (int i=0; i<fields.length; i++) {
			if (!jsonObject.containsKey(fields[i])) {
				return false;
			}
			
			if (i<fields.length-1)
				jsonObject = jsonObject.getJSONObject(fields[i]);			
			else //if (i==fields.length-1)
				foundValue = jsonObject.getString(fields[i]);
		}
		
		if (value!=null) {
			if ( foundValue == null || !value.equals(foundValue)) {
				return false;
			}
		}
		
		return found;
	}


	@Test(dependsOnMethods = "minAppVersion",  description = "test prod dev field")
    private void prodDev(){
        try {
            String result = schema.getInputSample(seasonID, "DEVELOPMENT", "1.1.2", sessionToken, "MAXIMAL", 0.7);
            boolean found = isKeyValInJson (new JSONObject(result), "context.testData.dsx.mode", "Lorem ipsum laborum dolor");
            Assert.assertTrue(found);
           // Assert.assertTrue(result.contains("\"dsx\":{\"mode\":\"Lorem ipsum laborum dolor\""));
            result = schema.getInputSample(seasonID, "PRODUCTION", "1.1.2", sessionToken, "MAXIMAL", 0.7);
            found = isKeyValInJson (new JSONObject(result), "context.testData.dsx.mode", "Lorem ipsum laborum dolor");
            Assert.assertFalse(found);
            //Assert.assertFalse(result.contains("\"dsx\":{\"mode\":\"Lorem ipsum laborum dolor\""));
            result = schema.getInputSample(seasonID, "DEVELOPMENT", "1.1.2", sessionToken, "MAXIMAL", 0.7);
            found = isKeyValInJson (new JSONObject(result), "context.testData.dsx.teaserTitle", "Lorem ipsum laborum dolor");
            Assert.assertTrue(found);
            //Assert.assertTrue(result.contains("\"teaserTitle\":\"Lorem ipsum laborum dolor\""));
            result = schema.getInputSample(seasonID, "PRODUCTION", "1.1.2", sessionToken, "MAXIMAL", 0.7);
            found = isKeyValInJson (new JSONObject(result), "context.testData.dsx.teaserTitle", "Lorem ipsum laborum dolor");
            Assert.assertTrue(found);
            //Assert.assertTrue(result.contains("\"teaserTitle\":\"Lorem ipsum laborum dolor\""));
        }catch (Exception e){
            Assert.fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "prodDev",  description = "test min max generation")
    private void minMax(){
        try {
            String result = schema.getInputSample(seasonID, "DEVELOPMENT", "1.1.2", sessionToken, "MAXIMAL", 0.7);
            Assert.assertTrue(result.contains("\"effectiveDateTime\":\"4188-03-18T04:26:40.000Z\""));
            result = schema.getInputSample(seasonID, "DEVELOPMENT", "1.1.2", sessionToken, "MINIMAL", 0.7);
            Assert.assertFalse(result.contains("\"effectiveDateTime\":\"4188-03-18T04:26:40.000Z\""));
        }catch (Exception e){
            Assert.fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "minMax",  description = "test min max generation")
    private void randomize(){
        try {
            String result = schema.getInputSample(seasonID, "DEVELOPMENT", "1.1.2", sessionToken, "MAXIMAL", 0.4);
            String result2 = schema.getInputSample(seasonID, "DEVELOPMENT", "1.1.2", sessionToken, "MAXIMAL", 0.4);
            Assert.assertTrue(result.equals(result2));
            result2 = schema.getInputSample(seasonID, "DEVELOPMENT", "1.1.2", sessionToken, "MAXIMAL", 0.4);
            Assert.assertTrue(result.equals(result2));
            result2 = schema.getInputSample(seasonID, "DEVELOPMENT", "1.1.2", sessionToken, "MAXIMAL", 0.5);
            Assert.assertFalse(result.equals(result2));
            result = schema.getInputSample(seasonID, "DEVELOPMENT", "1.1.2", sessionToken, "MAXIMAL", 0.5);
            Assert.assertTrue(result.equals(result2));
            //full random
            result = schema.getInputSample(seasonID, "DEVELOPMENT", "1.1.2", sessionToken, "MAXIMAL", -1.0);
            result2 = schema.getInputSample(seasonID, "DEVELOPMENT", "1.1.2", sessionToken, "MAXIMAL", -1.0);
            Assert.assertFalse(result.equals(result2));
            //default
            RestClientUtils.RestCallResults res = RestClientUtils.sendGet(murl+"/products/seasons/" + seasonID + "/inputsample?stage=DEVELOPMENT&minappversion=1.1.1&generationmode=MAXIMAL", sessionToken);
            result = res.message;
            res = RestClientUtils.sendGet(murl+"/products/seasons/" + seasonID + "/inputsample?stage=DEVELOPMENT&minappversion=1.1.1&generationmode=MAXIMAL", sessionToken);
            result2 = res.message;
            Assert.assertTrue(result.equals(result2));
            res = RestClientUtils.sendGet(murl+"/products/seasons/" + seasonID + "/inputsample?stage=DEVELOPMENT&minappversion=1.1.1&generationmode=MAXIMAL", sessionToken);
            result2 = res.message;
            Assert.assertTrue(result.equals(result2));
        }catch (Exception e){
            Assert.fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "randomize",  description = "test min max generation")
    private void update(){
        try {
            String result = schema.getInputSample(seasonID, "DEVELOPMENT", "1.1.1", sessionToken, "MAXIMAL", 0.7);
            String sch = schema.getInputSchema(seasonID, sessionToken);
            JSONObject jsonSchema = new JSONObject(sch);
            String schemaBody = FileUtils.fileToString(filePath + "validInputSchema/inputSchemaForUtilities.txt", "UTF-8", false);
            schemaBody = schemaBody.replace("1.1.2","1.1.1");
            jsonSchema.put("inputSchema", new JSONObject(schemaBody));
            schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
            String result2 = schema.getInputSample(seasonID, "DEVELOPMENT", "1.1.1", sessionToken, "MAXIMAL", 0.7);
            Assert.assertFalse(result.equals(result2));

        }catch (Exception e){
            Assert.fail(e.getMessage());
        }
    }


    @AfterTest
    private void reset(){
    	baseUtils.reset(productID, sessionToken);
    }
}
