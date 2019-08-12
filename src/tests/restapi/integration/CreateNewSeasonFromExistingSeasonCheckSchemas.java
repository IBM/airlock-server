package tests.restapi.integration;

import tests.restapi.AirlockUtils;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class CreateNewSeasonFromExistingSeasonCheckSchemas {
	protected String seasonID1;
	protected String seasonID2;
	protected String schemaID1;
	protected String schemaID2;
	protected String schemaID3;
	protected String productID;
	protected String config;
	protected InputSchemaRestApi is;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected AirlockUtils baseUtils;
	private String sessionToken = "";

/*
	@BeforeClass
	@Parameters({"url", "configPath", "sessionToken", "productsToDeleteFile"})
	public void init(String url, String configPath, @Optional String sToken, String productsToDeleteFile){
		config = configPath;
		if (sToken != null)
			sessionToken = sToken;
		p = new ProductsRestApi();
		s = new SeasonsRestApi();
		is = new InputSchemaRestApi();
		p.setURL(url);
		s.setURL(url);
		is.setURL(url);
		baseUtils = new AirlockUtils(url, configPath, sessionToken, productsToDeleteFile);

	}
	
	//
	@Test (description = "Add product, season and 2 schemas")
	public void addComponents() throws Exception{
		String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "name");
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		productID = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID);
		String season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
		seasonID1 = s.addSeason(productID, season, sessionToken);
		String schema1 = FileUtils.fileToString(config + "inputSchema1.txt", "UTF-8", false);
		schemaID1 = is.addInputSchema(seasonID1, schema1.toString(), sessionToken);
		String schema2 = FileUtils.fileToString(config + "inputSchema2.txt", "UTF-8", false);
		schemaID2 = is.addInputSchema(seasonID1, schema2.toString(), sessionToken);
	}
	
	
	*/
		
	/* create a new season - it should copy the previous season
	 * validate that 2 schemas exist in the new season
	 * 
	 */
	
	/*
	@Test (dependsOnMethods="addComponents", description = "Create new season and check its schemas")
	public void createNewSeason() throws Exception{
		String season2 = FileUtils.fileToString(config + "season2.txt", "UTF-8", false);
		seasonID2 = s.addSeason(productID, season2, sessionToken);
		
		String oldSchemas = is.getAllInputSchemas(seasonID1, sessionToken);
		String newSchemas = is.getAllInputSchemas(seasonID2, sessionToken);
		
		JSONObject oldJson = new JSONObject(oldSchemas);
		JSONObject newJson = new JSONObject(newSchemas);
		
		JSONArray oldSchemasArr = oldJson.getJSONArray("inputSchemas");
		JSONArray newSchemasArr = newJson.getJSONArray("inputSchemas");
		
		Assert.assertTrue(oldSchemasArr.size() == newSchemasArr.size(), "The number of schemas in seasons is different.");
		boolean compare = compareSchemas(oldSchemasArr, newSchemasArr);
		Assert.assertTrue(compare, "The schemas are different in 2 seasons");
		
		season2 = s.getSeason(productID, seasonID2, sessionToken);
		JSONObject season2Json = new JSONObject(season2);
		Assert.assertTrue(newSchemasArr.getJSONObject(0).getString("minAppVersion").equals(season2Json.getString("minVersion")), "The schema's minAppVersion is different from the new season's minVersion");
		
	}
	
	private boolean compareSchemas(JSONArray oldSchemasArr, JSONArray newSchemasArr) throws JSONException{
		boolean compare = true;
		for (int i= 0; i < oldSchemasArr.size(); i++){
			if (!oldSchemasArr.getJSONObject(i).getString("name").equals(newSchemasArr.getJSONObject(i).getString("name")))
				compare = false;
			
			if (!oldSchemasArr.getJSONObject(i).getJSONObject("inputSchema").equals(newSchemasArr.getJSONObject(i).getJSONObject("inputSchema")))
				compare = false;
			
		}
		
		return compare;
	}
	
	
	@AfterTest 
	private void reset(){
		p.deleteProduct(productID, sessionToken);
	}
	
	*/
}
