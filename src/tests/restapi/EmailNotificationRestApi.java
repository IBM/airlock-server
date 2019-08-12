package tests.restapi;

import java.io.IOException;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;

//import com.ibm.airlock.admin.serialize.S3DataSerializer;

import tests.com.ibm.qautils.RestClientUtils;
import tests.com.ibm.qautils.RestClientUtils.RestCallResults;

public class EmailNotificationRestApi
{
	protected String m_url;
	protected String m_test_url;

	public EmailNotificationRestApi(String url) throws IOException
	{
		setUrl(url);
	}

	public void setUrl(String url) throws IOException
	{
		int pos = url.lastIndexOf("/api/admin");
		if (pos == -1)
			throw new IOException("url must end with /api/admin");

		m_url = url;
		m_test_url = url.substring(0, pos) + "/api/test";
	}

	public void startTest(String sessionToken)
	{
		try {
			RestCallResults res = RestClientUtils.sendPut(m_test_url + "/TestMails?start=true", "", sessionToken);
			if (res.code != 200)
				throw new Exception(res.message);
		}
		catch (Exception e)
		{
			Assert.fail("exception thrown from notification startTest call. Message: " + e.getMessage());
		}
	}
	public void stopTest(String sessionToken)
	{
		try {
			RestCallResults res = RestClientUtils.sendPut(m_test_url + "/TestMails?start=false", "", sessionToken);
			if (res.code != 200)
				throw new Exception(res.message);
		}
		catch (Exception e)
		{
			Assert.fail("exception thrown from notification stopTest call. Message: " + e.getMessage());
		}
	}

	public JSONObject getNotificationsFile(String sessionToken)
	{
		try {
			RestCallResults res = RestClientUtils.sendGet(m_test_url + "/TestMails", sessionToken);
			if (res.code == 400)
				return new JSONObject();
			if (res.code != 200)
				throw new Exception(res.message);
			return parseResult(res);
		}
		catch (Exception e)
		{
			Assert.fail("exception thrown from getNotificationsFile call. Message: " + e.getMessage());
			return null;
		}
	}
	public void deleteNotificationsFile(String sessionToken) // if it exists
	{
		try {
			RestCallResults res = RestClientUtils.sendDelete(m_test_url + "/TestMails", sessionToken);
			if (res.code != 200 && res.code != 400) //deleted or doesn't exist
				throw new Exception(res.message);
		}
		catch (Exception e)
		{
			Assert.fail("exception thrown from deleteNotificationsFile call. Message: " + e.getMessage());
		}
	}
	public JSONObject getProductNotifications(String productId, String sessionToken)
	{
		try {
			RestCallResults res = RestClientUtils.sendGet(m_url + "/products/" + productId, sessionToken);
			return parseResult(res);
		}
		catch (Exception e)
		{
			Assert.fail("exception thrown from notification API call. Message: " + e.getMessage());
			return null;
		}
	}
	public int addProductNotification(String productId, String sessionToken)
	{
		try {
			RestCallResults res = RestClientUtils.sendPost(m_url + "/products/" + productId, "", sessionToken);
			return res.code;
		}
		catch (Exception e)
		{
			Assert.fail("exception thrown from notification API call. Message: " + e.getMessage());
			return -1;
		}
	}
	public int deleteProductNotification(String productId, String sessionToken)
	{
		try {
			RestCallResults res = RestClientUtils.sendDelete(m_url + "/products/" + productId, sessionToken);
			return res.code;
		}
		catch (Exception e)
		{
			Assert.fail("exception thrown from notification API call. Message: " + e.getMessage());
			return -1;
		}
	}
	public JSONObject getFeatureNotifications(String featureId, String sessionToken)
	{
		try {
			RestCallResults res = RestClientUtils.sendGet(m_url + "/products/seasons/features/" + featureId + "/follow", sessionToken);
			return parseResult(res);
		}
		catch (Exception e)
		{
			Assert.fail("exception thrown from notification API call. Message: " + e.getMessage());
			return null;
		}
	}
	public int addFeatureNotification(String featureId, String sessionToken)
	{
		try {
			RestCallResults res = RestClientUtils.sendPost(m_url + "/products/seasons/features/" + featureId + "/follow", "", sessionToken);
			return res.code;
		}
		catch (Exception e)
		{
			Assert.fail("exception thrown from notification API call.  Message: " + e.getMessage());
			return -1;
		}
	}
	public int deleteFeatureNotification(String featureId, String sessionToken)
	{
		try {
			RestCallResults res = RestClientUtils.sendDelete(m_url + "/products/seasons/features/" + featureId + "/follow", sessionToken);
			return res.code;
		}
		catch (Exception e)
		{
			Assert.fail("exception thrown from notification API call. Message: " + e.getMessage());
			return -1;
		}
	}
	JSONObject parseResult(RestClientUtils.RestCallResults res)
	{

		JSONObject response = null;
		try {
			response = new JSONObject(res.message);
		}
		catch (JSONException e) {
			Assert.fail("unexpected result from notification API call. code: " + res.code + ", message: " + e.getMessage());
		}

		String error = response.optString("error");
		if (error != null)
		{
			Assert.fail("An error was returned from notification API call. code: " + res.code + ", message: " + error);
		}

		return response;
	}
}
