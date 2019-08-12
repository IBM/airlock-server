package tests.restapi.authentication;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import tests.restapi.AirlockUtils;

public class TestAnalyticsRoles {
	private String analyticsEditorUser;
	private String analyticsEditorPassword;
	private String analyticsViewerUser;
	private String analyticsViewerPassword;
	protected String m_appName = "backend_dev";
	private String m_url;
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "seasonVersion", "analyticsViewer", "analyticsViewerPass", "analyticsEditor", "analyticsEditorPass"})
	public void init(String url, String configPath, @Optional String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, @Optional String seasonVersion, String analyticsViewer, String analyticsViewerPass, String analyticsEditor, String analyticsEditorPass) throws IOException{

		m_url = url+"/authentication";
		analyticsEditorUser = analyticsEditor;
		analyticsEditorPassword = analyticsEditorPass;
		analyticsViewerUser = analyticsViewer;
		analyticsViewerPassword = analyticsViewerPass;
		m_appName = appName;
		baseUtils = new AirlockUtils(url);
	}
	
	@Test(description="analyticsViewer role")
	public void analyticsViewer() throws IOException{
		String viewerToken = baseUtils.setNewJWTToken(analyticsViewerUser, analyticsViewerPassword, m_appName);
		int respCode = sendRequest(m_url+"/analyticsviewer", viewerToken, "GET");
		Assert.assertEquals(respCode, 200, "Incorrect response code1");
		
		respCode = sendRequest(m_url+"/analyticsviewer", "", "GET");
		Assert.assertEquals(respCode, 401, "Incorrect response code2");
		
		respCode = sendRequest(m_url+"/analyticsviewer", viewerToken, "POST");
		Assert.assertEquals(respCode, 401, "Incorrect response code3");
		
		respCode = sendRequest(m_url+"/analyticsviewer", viewerToken, null);
		Assert.assertEquals(respCode, 200, "Incorrect response code4");
	}
	
	
	@Test(description="analyticsEditor role")
	public void analyticsEditor() throws IOException{
		String editorToken = baseUtils.setNewJWTToken(analyticsEditorUser, analyticsEditorPassword, m_appName);
		
		int respCode = sendRequest(m_url+"/analyticsviewer", editorToken, "POST");
		Assert.assertEquals(respCode, 200, "Incorrect response code3");
		
		respCode = sendRequest(m_url+"/analyticsviewer", editorToken, "GET");
		Assert.assertEquals(respCode, 200, "Incorrect response code1");
		
		respCode = sendRequest(m_url+"/analyticsviewer", "", "GET");
		Assert.assertEquals(respCode, 401, "Incorrect response code2");
		
		respCode = sendRequest(m_url+"/analyticsviewer", editorToken, null);
		Assert.assertEquals(respCode, 200, "Incorrect response code4");
	}
	
	@Test(description="validation sessionToken")
	public void validateToken() throws IOException{
		String viewerToken = baseUtils.setNewJWTToken(analyticsViewerUser, analyticsViewerPassword, m_appName);
		int respCode = sendRequest(m_url+"/validate", viewerToken, "GET");
		Assert.assertEquals(respCode, 200, "Incorrect response code1");
		
		viewerToken = viewerToken + "stam";
		respCode = sendRequest(m_url+"/validate", viewerToken, "GET");
		Assert.assertEquals(respCode, 401, "Incorrect response code1");
	}
	
	private int sendRequest(String url, String sessionToken, String method) throws IOException{
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");
		if (sessionToken != null)
			con.setRequestProperty ("sessionToken", sessionToken);
		if (method != null)
			con.setRequestProperty ("X-Original-Method", method);
	
		return con.getResponseCode();

	}
}
