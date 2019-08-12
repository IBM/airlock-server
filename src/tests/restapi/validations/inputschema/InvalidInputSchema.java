package tests.restapi.validations.inputschema;

import java.io.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.FolderUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.InputSchemaRestApi;;

public class InvalidInputSchema {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected InputSchemaRestApi is;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		is = new InputSchemaRestApi();
		is.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

		
	}
	
	
	@Test(dataProvider = "fileReader", description = "Add invalid schema")
	public void updateInvalidInputSchema(String fileName, String schemaBody) throws Exception {
		String schema = is.getInputSchema(seasonID, sessionToken);
		JSONObject schemaJson = new JSONObject(schema);

		try {
			schemaJson.put("inputSchema", new JSONObject(schemaBody));
			String result = is.updateInputSchema(seasonID, schemaJson.toString(), sessionToken);

			Assert.assertTrue(result.contains("error"), "Invalid schema from file " + fileName + " was added to season");

		} catch (JSONException e) {
				Assert.assertTrue(true, "Invalid schema was processed correctly");
		}
				
	}
	

	
	@DataProvider (name="fileReader")
	private Iterator<Object[]> fileReader(){
				
		ArrayList<File> files =  FolderUtils.allFilesFromFolder(filePath + "/invalidInputSchema");
		List<Object[]> dataToBeReturned = new ArrayList<Object[]>();

		
			for (int i=0;i<files.size();i++){
				File file = files.get(i);
				
				try {
					String body = FileUtils.fileToString(file.getAbsolutePath(), "UTF-8", false);;
					dataToBeReturned.add(new Object[] { file.getName(), body });
					
					//dataToBeReturned.add(new Object[] {body });

				} catch (IOException e) {

					System.err.println("An exception was thrown when trying to read data file.\n Message:"+e.getMessage());
				}
			}
			return dataToBeReturned.iterator(); 

	}
	


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
