package tests.restapi.scenarios.operations;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class ValidateDocumentlinks {
	protected String seasonID;
	protected String productID;
	protected String filePath;
	protected String m_url;
	protected SeasonsRestApi s;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String documentlinks;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	@Test
	public void getDocumentlinks() throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/seasons/" + seasonID + "/documentlinks", sessionToken);
		documentlinks = res.message;
		try{
			JSONObject json = new JSONObject(documentlinks);
		}catch(Exception e){
			Assert.fail("Failed to get documentlinks" + e.getMessage());
		}
	}
	
	//cannot access constants and defaults directly since reside in private container
	@Test (dependsOnMethods = "getDocumentlinks")
	public void validateDocumentlinks() throws JSONException{
		JSONObject json = new JSONObject(documentlinks);
		//validate defaults file
		String file = json.getString("defaultsFile");
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(file, sessionToken);
			String content = res.message;
			boolean val = validateResponse(content); 
			//Assert.assertTrue(val, "Invalid defaults file."); //the error is since the defaults file is in private storage blob container
		} catch (Exception e) {
			Assert.fail("Can't get the defaults file.");
		}
	
/*		//validate static runtime dev
		String file = json.getString("staticRuntimeDevelopment");
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(file, sessionToken);
			String content = res.message;
			boolean val = validateResponse(content);
			Assert.assertTrue(val, "Invalid staticRuntimeDevelopment file.");
		} catch (Exception e) {
			Assert.fail("Can't get the staticRuntimeDevelopment file.");
		}
		
		//validate static runtime prod
		file = json.getString("staticRuntimeProduction");
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(file, sessionToken);
			String content = res.message;
			boolean val = validateResponse(content);
			Assert.assertTrue(val, "Invalid staticRuntimeProduction file.");
		} catch (Exception e) {
			Assert.fail("Can't get the staticRuntimeProduction file.");
		}*/

		//validate constants		
		JSONArray platforms = json.getJSONArray("platforms");
		for (int i=0; i<platforms.size(); i++){
			JSONObject platformObj = platforms.getJSONObject(i);
			//String platform = platformObj.getString("platform");
			JSONArray links = platformObj.getJSONArray("links");
			for (int j=0; j<links.size(); j++){
				JSONObject link = links.getJSONObject(j);
				String constantsFilePath = link.getString("link");
				try {
					RestClientUtils.RestCallResults res = RestClientUtils.sendGet(constantsFilePath, sessionToken);
					String constantsContent = res.message;
					//Assert.assertTrue(!constantsContent.equals(""), "Invalid constants file." + constantsFilePath); //the error is since the constants file is in private storage blob container
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Assert.fail("Can't get the constants file." + constantsFilePath);
				}
			}
		}
				
	}
	
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}
	
	public boolean validateResponse(String response){
		try {
			JSONObject obj = new JSONObject(response);
			return true;
		} catch(Exception e) {
			return false;
		}
		
	}
}
