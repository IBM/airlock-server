package tests.restapi.production_data_tests;

import java.io.IOException;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.FeaturesRestApi;
import tests.restapi.SeasonsRestApi;

public class AdsConfigUpdateExceedsAnalyticsQuota {
	protected String seasonID;
	protected String productID;
	protected String filePath;
	protected SeasonsRestApi s;
	protected FeaturesRestApi f;
	protected String sessionToken = "";
	protected String m_url;
	protected String m_fromVersion;
	private JSONObject body = new JSONObject();
	private String configID;

	
	@BeforeClass
	@Parameters({"url", "configPath", "sessionToken"})
	public void init(String url, String configPath, @Optional String sToken) throws JSONException{
		m_url = url;
		if (sToken != null)
			sessionToken = sToken;
		
		f = new FeaturesRestApi();
		f.setURL(m_url);
		
		configID = "c5b92fa1-5a92-4ddf-84de-60ed907aaa48";
		//seasonID = "6f8cba0d-8e46-4c6b-8b70-9bac3cc5a085";
	}
	
	@Test
	public void updateConfiguration() throws JSONException, IOException{
		String config = f.getFeature(configID, sessionToken);
		String configuration = "{\"application\":\"androidflagship\",\"version\":\"6.0.0\",\"scatterShotEnabled\":true}";
		JSONObject newConfiguration = new JSONObject(configuration);
		JSONObject json = new JSONObject(config);
		json.put("configuration", newConfiguration);
		String response = f.updateFeature(seasonID, configID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Failed to update configuration: " + response);
		
	}
}
