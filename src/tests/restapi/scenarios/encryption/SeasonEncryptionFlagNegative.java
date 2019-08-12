package tests.restapi.scenarios.encryption;

import org.apache.wink.json4j.JSONArray;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

public class SeasonEncryptionFlagNegative
{
    protected String seasonID1;
    protected String seasonID2;
    
    protected String productID;
    protected String featureID;
    protected String filePath;
    protected FeaturesRestApi f;
    protected ProductsRestApi p;
    protected SeasonsRestApi s;
    protected TranslationsRestApi translationsApi;
    protected StringsRestApi str;
    protected UtilitiesRestApi u;
    protected BranchesRestApi br;
    private OperationRestApi operApi;
	
	
    private String sessionToken = "";
    protected AirlockUtils baseUtils;
    protected String m_url;
    private JSONObject seasonJson; 
    

    @BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "operationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String operationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
    		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		operApi = new OperationRestApi();
		operApi.setURL(operationsUrl);
		
		str = new StringsRestApi();
		str.setURL(translationsUrl);
		translationsApi = new TranslationsRestApi();
        translationsApi.setURL(translationsUrl);
        u = new UtilitiesRestApi();
        u.setURL(m_url);
        br = new BranchesRestApi();
		br.setURL(m_url);
		
		if (sToken != null)
			sessionToken = sToken;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		String seasonStr = FileUtils.fileToString(configPath + "season1.txt", "UTF-8", false);
		seasonJson = new JSONObject(seasonStr);
    	}

    //Negative tests:
    //1. cannot set runtimeEncryption flag to true if product doesn't include RUNTIME_ENCRYPTION capability
    //2. cannot remove encryption from product if one of its seasons support runtimeEncryption
    //3. cannot remove encryption from server if one of its seasons support runtimeEncryptionv(list the products)
    //4. cannot call resetEncKey for season that doesn't support runtime encryption
    //5. cannot call getEncKey for season that doesn't support runtime encryption
    
    @Test(description = "remove RUNTIME_ENCRYPTION capability from product and try to create/update season with runtimeEncryption=true")
    public void setSeasonRuntimeEncryptionWhenNotInProd() throws Exception {
    		//remove RUNTIME_ENCRYPTION capability from product
    		JSONArray capabilities = getCapabilitiesInProduct(productID, sessionToken);
    		Assert.assertTrue(capabilityIncluded(capabilities, "RUNTIME_ENCRYPTION"), "RUNTIME_ENCRYPTION is not included in product");
            
    		capabilities.remove("RUNTIME_ENCRYPTION"); 
    		String response = setCapabilitiesInProduct(productID, capabilities, sessionToken);
    		Assert.assertFalse(response.contains("error"), "Removed required RUNTIME_ENCRYPTION from capabilities");
    		
    		capabilities = getCapabilitiesInProduct(productID, sessionToken);
    		Assert.assertFalse(capabilityIncluded(capabilities, "RUNTIME_ENCRYPTION"), "RUNTIME_ENCRYPTION is included in product");
                		
    		//try to create season with runtimeEncryption
    		seasonID1 = s.addSeasonSpecifyEncryption(productID, seasonJson.toString(), true, sessionToken);
    		Assert.assertTrue(seasonID1.contains("error") && seasonID1.contains("RUNTIME_ENCRYPTION"),  "create season with runtimeEncryption when product does not include RUNTIME_ENCRYPTION capability: " + seasonID1);
    			
    		//try to create season without runtimeEncryption
    		seasonID1 = s.addSeasonSpecifyEncryption(productID, seasonJson.toString(), false, sessionToken);
    		Assert.assertFalse(seasonID1.contains("error") ,  "create season with runtimeEncryption when product does not include RUNTIME_ENCRYPTION capability: " + seasonID1);
    		
    		//try to update season to runtimeEncryption
    		String seasonTmp = s.getSeason(productID, seasonID1, sessionToken);
		JSONObject json = new JSONObject(seasonTmp);		
		json.put("runtimeEncryption", true);
    		response = s.updateSeason(seasonID1, json.toString(), sessionToken);
    		Assert.assertTrue(response.contains("error") && response.contains("RUNTIME_ENCRYPTION"),  "update season to runtimeEncryption when product does not include RUNTIME_ENCRYPTION capability: " + seasonID1);
    }
    
    @Test(dependsOnMethods="setSeasonRuntimeEncryptionWhenNotInProd", description = "remove RUNTIME_ENCRYPTION capability from product that has encrypted seasons")
    public void removeEncryptionCapabilityFromProdWithEncSeasons() throws Exception {
    		//add RUNTIME_ENCRYPTION capability to product
		JSONArray capabilities = getCapabilitiesInProduct(productID, sessionToken);
		Assert.assertFalse(capabilityIncluded(capabilities, "RUNTIME_ENCRYPTION"), "RUNTIME_ENCRYPTION is not included in product");
        
		capabilities.add("RUNTIME_ENCRYPTION"); 
		String response = setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Removed required RUNTIME_ENCRYPTION from capabilities");
		
		capabilities = getCapabilitiesInProduct(productID, sessionToken);
		Assert.assertTrue(capabilityIncluded(capabilities, "RUNTIME_ENCRYPTION"), "RUNTIME_ENCRYPTION is included in product");
    
		//create season with runtimeEncryption
		seasonJson.put("minVersion", "10");
		seasonID2 = s.addSeasonSpecifyEncryption(productID, seasonJson.toString(), true, sessionToken);
		Assert.assertFalse(seasonID2.contains("error") , "fail to create season with runtimeEncryption when product includes RUNTIME_ENCRYPTION capability: " + seasonID2);

		//try to remove RUNTIME_ENCRYPTION capability from product
		capabilities.remove("RUNTIME_ENCRYPTION"); 
		response = setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("RUNTIME_ENCRYPTION"), "Can remove RUNTIME_ENCRYPTION from  prod capabilities");
		
		String seasonTmp = s.getSeason(productID, seasonID1, sessionToken);
		JSONObject json = new JSONObject(seasonTmp);		
		Assert.assertFalse(json.getBoolean("runtimeEncryption"), "season1 not encrypted");
		
		seasonTmp = s.getSeason(productID, seasonID2, sessionToken);
		json = new JSONObject(seasonTmp);		
		Assert.assertTrue(json.getBoolean("runtimeEncryption"), "season2 encrypted");
    }

    @Test(dependsOnMethods="removeEncryptionCapabilityFromProdWithEncSeasons", description = "remove RUNTIME_ENCRYPTION capability from server when exist encrypted seasons")
    public void removeEncryptionCapabilityFromServerWithEncSeasons() throws Exception {
    		//add RUNTIME_ENCRYPTION capability to product
		JSONArray capabilities = getGlobalCapabilities(sessionToken);
		Assert.assertTrue(capabilityIncluded(capabilities, "RUNTIME_ENCRYPTION"), "RUNTIME_ENCRYPTION is not included in server");
        
		capabilities.remove("RUNTIME_ENCRYPTION"); 
		String response = setGlobalCapabilities(capabilities, sessionToken);
		Assert.assertTrue(response.contains("error"), "Removed RUNTIME_ENCRYPTION from capabilities when encrypted seasons exists");
		
		String prodStr = productID = p.getProduct(productID);
		String prodName = new JSONObject(prodStr).getString("name");
		Assert.assertTrue(response.contains(prodName), "Removed RUNTIME_ENCRYPTION from capabilities when encrypted seasons exists - prod name missing");
    }

    
    @Test(dependsOnMethods="removeEncryptionCapabilityFromServerWithEncSeasons", description = "call encryption api for non encryped api")
    public void callEncryptionAPIforNonEncryptedSeason() throws Exception {
    		//Non encryption season
    		String response = s.getEncryptionKey(seasonID1, sessionToken);
    		Assert.assertTrue(response.contains("error"), "can call getEncryptionKey for non encrypted season");
    		
    		response = s.resetEncryptionKey(seasonID1, sessionToken);
    		Assert.assertTrue(response.contains("error"), "can call resetEncryptionKey for non encrypted season");
    		
    		//Encryption season
    		response = s.getEncryptionKey(seasonID2, sessionToken);
    		Assert.assertFalse(response.contains("error"), "cannot call getEncryptionKey for encrypted season");
    		
    		response = s.resetEncryptionKey(seasonID2, sessionToken);
    		Assert.assertFalse(response.contains("error"), "cannot call resetEncryptionKey for encrypted season");
    		
    		
    		
    }

    
    private boolean capabilityIncluded(JSONArray capabilitie, String capability) throws JSONException {
    		for (int i=0; i<capabilitie.size(); i++) {
    			if (capabilitie.getString(i).equals(capability)) {
    				return true;
    			}
    		}
    		return false;
    }
    private String setCapabilitiesInProduct(String productID, JSONArray capabilites, String sessionToken) throws JSONException{
		String product = p.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product);
		json.remove("seasons");
		json.put("capabilities", capabilites);
		return p.updateProduct(productID, json.toString(), sessionToken);
	}
	
	private JSONArray getCapabilitiesInProduct(String productID, String sessionToken) throws JSONException{
		String product = p.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product);
		return json.getJSONArray("capabilities");
	}
	
	 private String setGlobalCapabilities(JSONArray capabilitesArr, String sessionToken) throws Exception{

		 String capabilitiesStr = operApi.getCapabilities(sessionToken);
			
		if (capabilitiesStr.contains("error")){
			Assert.fail("Can't get capabilities " + capabilitiesStr);
		}

		JSONObject json = new JSONObject(capabilitiesStr);
		json.put("capabilities", capabilitesArr); 
		return operApi.setCapabilities(json.toString(), sessionToken);
	}
		
	 private JSONArray getGlobalCapabilities(String sessionToken) throws Exception{
		String capabilities = operApi.getCapabilities(sessionToken);
		
		if (capabilities.contains("error")){
			Assert.fail("Can't get capabilities " + capabilities);
		}
		
		return new JSONObject(capabilities).getJSONArray("capabilities");
	}
		
	
	
    @AfterTest
	private void reset(){
    		baseUtils.reset(productID, sessionToken);
	}
}
