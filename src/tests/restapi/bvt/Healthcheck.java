package tests.restapi.bvt;

import java.net.HttpURLConnection;
import java.net.URL;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;


public class Healthcheck {
	protected String m_url;
	protected String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"operationsUrl", "url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String operationsUrl, String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
 		m_url = operationsUrl + "/healthcheck"; 
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

	}
	
	@Test	
	public void checkHealthcheck() throws Exception{
		boolean response = sendGet(m_url, sessionToken);
		Assert.assertTrue(response, "Healthcheck failed");
	}
	
	
	public static  boolean sendGet(String url, String sessionToken) throws Exception {

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		if (sessionToken != null)
			con.setRequestProperty ("sessionToken", sessionToken);

		if (200 <= con.getResponseCode() && con.getResponseCode() <= 299) {
			return true;
		} else {
			return false;
		}
				

	}
}
