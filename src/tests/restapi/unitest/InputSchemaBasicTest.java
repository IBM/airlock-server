package tests.restapi.unitest;

import tests.restapi.InputSchemaRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.ProductsRestApi;

public class InputSchemaBasicTest {
	
	protected String seasonID;
	protected String productID;
	protected String schemaID;
	protected String filePath;
	protected String schema;
	protected SeasonsRestApi s;
	protected ProductsRestApi p;
	protected InputSchemaRestApi is;
	private String sessionToken = "";
/*	
	@BeforeClass
	@Parameters({"url", "configPath", "sessionToken", "productsToDeleteFile"})
	public void init(String url, String configPath, @Optional String sToken, String productsToDeleteFile) throws IOException{
		seasonID = "";
		filePath = configPath ;
		p = new ProductsRestApi();
		p.setURL(url);
		if (sToken != null)
			sessionToken = sToken;
		AirlockUtils baseUtils = new AirlockUtils(url, filePath, sessionToken, productsToDeleteFile);
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		s = new SeasonsRestApi();
		s.setURL(url);
		String season = FileUtils.fileToString(filePath + "season1.txt", "UTF-8", false);
		seasonID = s.addSeason(productID, season, sessionToken);
		
		is = new InputSchemaRestApi();
		is.setURL(url);
		schema = FileUtils.fileToString(filePath + "inputSchema1.txt", "UTF-8", false);
	}
	
	@Test
	public void addSchema() throws IOException{
		schemaID = is.addInputSchema(seasonID, schema, sessionToken);
	}
	
	
	@Test (dependsOnMethods = "addSchema")
	public void getInputSchema() throws Exception{
		schema = is.getInputSchema(schemaID, sessionToken);
	}
	
	@Test (dependsOnMethods = "getInputSchema")
	public void updateInputSchema() throws Exception{
		JSONObject json = new JSONObject(schema);
		json.put("name", "newSchemaName");
		is.updateInputSchema(schemaID, json.toString(), sessionToken);
	}
	
	@Test (dependsOnMethods = "updateInputSchema")
	public void getAllSchemas() throws Exception{
		is.getAllInputSchemas(seasonID, sessionToken);
	}
	
	@Test (dependsOnMethods = "getAllSchemas")
	public void deleteSchema() throws Exception{
		is.deleteInputSchema(schemaID, sessionToken);
	}
	
	@AfterTest 
	public void reset(){
		//s.reset();
		p.deleteProduct(productID, sessionToken);
 			
	}
*/
}
