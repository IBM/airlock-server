package tests.restapi;

import java.io.IOException;

import org.apache.wink.json4j.JSONObject;

public class EmailNotification
{
	EmailNotificationRestApi api;
	String sessionToken;
	boolean isOn;

	public EmailNotification(boolean isOn, String url, String sessionToken) throws IOException
	{
		this.isOn = (isOn && sessionToken != null);
		this.sessionToken = sessionToken;

		if (isOn)
			api = new EmailNotificationRestApi(url);
	}
	public boolean isOn()
	{
		return isOn;
	}
	public void followProduct(String productId)
	{
		if (isOn)
			api.addProductNotification(productId, sessionToken);
	}
	public void followFeature(String featureId)
	{
		if (isOn)
			api.addFeatureNotification(featureId, sessionToken);
	}

	public void startTest()
	{
		if (isOn)
			api.startTest(sessionToken);
	}
	public void stopTest()
	{
		if (isOn)
			api.stopTest(sessionToken);
	}
	public void clearMailFile()
	{
		if (isOn)
			api.deleteNotificationsFile(sessionToken);
	}
	public JSONObject getMailFile()
	{
		if (isOn)
			return api.getNotificationsFile(sessionToken);
		else
			return null;
	}
}
