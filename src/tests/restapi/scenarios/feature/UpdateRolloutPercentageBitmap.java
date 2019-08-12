package tests.restapi.scenarios.feature;



public class UpdateRolloutPercentageBitmap {
/*	protected String seasonID;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected String productID;
	private String sessionToken = "";
	protected Notification notification;
	
	@BeforeClass
	@Parameters({"url", "configPath", "sessionToken", "productsToDeleteFile", "notify"})
	public void init(String url, String configPath, @Optional String sToken, String productsToDeleteFile, String notify) throws IOException{
		filePath = configPath + "feature2.txt";
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		if (sToken != null)
			sessionToken = sToken;
		AirlockUtils baseUtils = new AirlockUtils(url, configPath, sessionToken, productsToDeleteFile);
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

		boolean isOn = "true".equals(notify);
		notification = new Notification(isOn, url, sToken);
		notification.startTest();
		notification.clearMailFile();
		notification.followProduct(productID);
	}


//rolloutPercentageBitmap is obsolete
	

	@Test (description = "Update rolloutPercentage and rolloutPercentageBitmap should be updated automatically")
	public void testUpdateRolloutPercentageBitmap() throws JSONException, IOException{

			String feature = FileUtils.fileToString(filePath, "UTF-8", false);
			String featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
			feature = f.getFeature(featureID, sessionToken);
			JSONObject json = new JSONObject(feature);
			String oldRolloutPercentageBitmap = json.getString("rolloutPercentageBitmap");
			json.put("rolloutPercentage", 30);
			featureID = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
			String updatedFeature = f.getFeature(featureID, sessionToken);
			JSONObject newJson = new JSONObject(updatedFeature);
			String newRolloutPercentageBitmap = newJson.getString("rolloutPercentageBitmap");
			Assert.assertNotEquals(newRolloutPercentageBitmap, oldRolloutPercentageBitmap, "RolloutPercentageBitmap was not updated");

		
	}

	
	@AfterTest
	private void reset(){
		p.deleteProduct(productID, sessionToken);
		if (notification.isOn())
		{
			JSONObject notificationOutput = notification.getMailFile();
			// do something with it (parse or compare to gold)
			notification.stopTest();
		}
	}*/

}
