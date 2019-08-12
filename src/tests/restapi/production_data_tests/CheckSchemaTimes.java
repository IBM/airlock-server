package tests.restapi.production_data_tests;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;

public class CheckSchemaTimes {

	    protected String feature;
	    protected String filePath;
	    protected InputSchemaRestApi schema;
	    private String sessionToken = "";
	    protected String murl;
	    private String m_testUrl;
	    protected ProductsRestApi p;

		private String productID ;
		private String sourceSeasonID;
		private String productName;


	    @BeforeClass
		@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "productId", "seasonId", "productName"})
		public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String prId, String seasId, String prName) throws Exception{
	        filePath = configPath;
	        m_testUrl = url.replace("/api/admin", "/api/test/import");
	        murl=url;
			productID = prId;
			sourceSeasonID = seasId;
			productName = prName;
	        if (sToken != null)
	            sessionToken = sToken;
			p = new ProductsRestApi();
			p.setURL(murl);
	        schema = new InputSchemaRestApi();
	        schema.setURL(murl);
	    }
	    
		@Test (description = "copy product to server")
		public void copy() throws Exception{
			String response  = p.getProduct(productID, sessionToken);
			if (response.contains("Product not found")){	//product  doesn't exists
			
				JSONObject body = new JSONObject();
				body.put("path", "vicky/PRODUCTION_DATA/" + productID + "/" + sourceSeasonID);		
				body.put("productName", productName);
				body.put("productId", productID);
				body.put("seasonId", sourceSeasonID);
				body.put("minVersion", "7.15");
				RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_testUrl, body.toString(), sessionToken);
				Assert.assertTrue(res.code==200, "Product was not copied");
			}
			
		}
	    
		@Test (dependsOnMethods="copy", description="Add old Android input schema to the season")
		public void addSchema() throws Exception{
			long startTime = System.currentTimeMillis();
			System.out.println("started: " + startTime);
			
			String sch = schema.getInputSchema(sourceSeasonID, sessionToken);
	        JSONObject jsonSchema = new JSONObject(sch);
	        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_Android_3.0.json", "UTF-8", false);
	        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
	        String response = schema.updateInputSchema(sourceSeasonID, jsonSchema.toString(), sessionToken);
	        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
	        
	        long estimatedTime = System.currentTimeMillis() - startTime;
	        System.out.println("Elapsed time for current schema for Android: " + estimatedTime +  " milliseconds");
		}
				
/*		@Test (dependsOnMethods="addSchema", description="Add new android input schema to the season")
		public void addNewSchema() throws Exception{
			long startTime = System.currentTimeMillis();
			
			String sch = schema.getInputSchema(AndroidSeasonId, sessionToken);
	        JSONObject jsonSchema = new JSONObject(sch);
	        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/AndroidInputSchema_ver25.txt", "UTF-8", false);
	        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
	        String response = schema.updateInputSchema(AndroidSeasonId, jsonSchema.toString(), sessionToken);
	        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
	        
	        long estimatedTime = System.currentTimeMillis() - startTime;
	        System.out.println("Elapsed time for new schema  for Android: " + estimatedTime);
		}
	
		
		@Test (description="Add old iOS input schema to the season")
		public void addIOSSchema() throws Exception{
			long startTime = System.currentTimeMillis();
			
			String sch = schema.getInputSchema(AndroidSeasonId, sessionToken);
	        JSONObject jsonSchema = new JSONObject(sch);
	        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/AndroidInputSchema_ver25.txt", "UTF-8", false);
	        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
	        String response = schema.updateInputSchema(iOSSeasonId, jsonSchema.toString(), sessionToken);
	        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
	        
	        long estimatedTime = System.currentTimeMillis() - startTime;
	        System.out.println("Elapsed time for current schema for Android: " + estimatedTime);
		}
		
		@Test (dependsOnMethods="addIOSSchema", description="Add new iOS input schema to the season")
		public void addNewIOSSchema() throws Exception{
			long startTime = System.currentTimeMillis();
			
			String sch = schema.getInputSchema(AndroidSeasonId, sessionToken);
	        JSONObject jsonSchema = new JSONObject(sch);
	        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/AndroidInputSchema_ver25.txt", "UTF-8", false);
	        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
	        String response = schema.updateInputSchema(iOSSeasonId, jsonSchema.toString(), sessionToken);
	        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
	        
	        long estimatedTime = System.currentTimeMillis() - startTime;
	        System.out.println("Elapsed time for new schema  for Android: " + estimatedTime);
		}
		
*/

}
