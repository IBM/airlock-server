package tests.restapi.validations.utilities;

import tests.restapi.ProductsRestApi;
import tests.restapi.UtilitiesRestApi;

public class UtilityCreateValidateMissingFields {
	protected String seasonID;
	protected String utilityID;
	protected String filePath;
	protected UtilitiesRestApi u;
	protected ProductsRestApi p;
	protected String productID;
	protected String m_url;
	protected String utility;
	private String sessionToken = "";
	/*		
	@BeforeClass
	@Parameters({"url", "configPath", "sessionToken", "productsToDeleteFile"})
	public void init(String url, String configPath, @Optional String sToken, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		u = new UtilitiesRestApi();
		u.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		if (sToken != null)
			sessionToken = sToken;
	AirlockUtils baseUtils = new AirlockUtils(m_url, configPath, sessionToken, productsToDeleteFile);
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		

	}
/*
	@Test 
	public void emptyUtility() throws Exception{
		Properties props = getPropValues(filePath + "utilities" + File.separator + "utility1.txt");
		props.remove("utility");
		addUtility(props);
	}
	
	@Test
	public void emptyStage() throws Exception{
		Properties props = getPropValues(filePath + "utilities" + File.separator + "utility1.txt");
		props.remove("stage");
		addUtility(props);
	}


	@Test
	public void emptyMinAppVersion() throws Exception{
		Properties props = getPropValues(filePath + "utilities" + File.separator + "utility1.txt");
		props.remove("minAppVersion");
		addUtility(props);
	}
	
	
	private void addUtility(Properties utilProps) throws IOException, JSONException{
		String response = u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}

	public Properties getPropValues(String configPath) throws IOException {
		Properties props = new Properties();
		try {
			
			String propFileName = configPath;
			InputStream inputStream = new FileInputStream(propFileName);
			props.load(inputStream);
			
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		} 
		return props;
	}

	

	@AfterTest
	private void reset(){
		p.deleteProduct(productID, sessionToken);
	}
	*/
}
