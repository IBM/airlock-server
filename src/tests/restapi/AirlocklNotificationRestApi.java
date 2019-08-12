package tests.restapi;

import java.io.IOException;


import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import tests.com.ibm.qautils.RestClientUtils;


public class AirlocklNotificationRestApi
{
	protected String m_url;

	public void setUrl(String url) throws IOException
	{
		m_url = url;
	}
	
	//GET /admin/products/seasons/{season-id}/notifications
	public String getAllNotifications(String seasonID, String sessionToken){
		String response="";
 		try { 			
 			RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonID + "/notifications", sessionToken);
 			response = res.message;
  			
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to get notifications. Message: "+e.getMessage()) ;
		}
 		return response;
	}
	
	//PUT /admin/products/seasons/{season-id}/notifications
	public String updateAllNotifications(String seasonID, String content, String sessionToken){
		String response="";
 		try { 			
 			RestClientUtils.RestCallResults res= RestClientUtils.sendPut(m_url+"/products/seasons/" + seasonID + "/notifications", content, sessionToken);
 			response = res.message;
  			
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to update all notifications. Message: "+e.getMessage()) ;
		}
 		return response;
	}
	
	//POST /admin/products/seasons/{season-id}/notifications
	public String createNotification(String seasonID, String notificationBody, String sessionToken) throws IOException{
		String notificaitonID = "";
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonID + "/notifications", notificationBody, sessionToken);
		notificaitonID = parseNotificationId(res.message);
		return notificaitonID;
		
	}
	
	
	///admin/products/seasons/notifications/{notification-id} - delete , update, get
	
	public String getNotification(String notificationID, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/seasons/notifications/" + notificationID, sessionToken);
			response = res.message;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to get airlock notification. Message: "+e.getMessage()) ;
		}
			return response;
	}
	
	public int deleteNotification(String notificationID, String sessionToken){
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/products/seasons/notifications/" + notificationID, sessionToken);
			return res.code;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to delete airlock notification. Message: "+e.getMessage()) ;
		}
			return -1;
	}
	
	public String updateNotification(String notificationID, String notification, String sessionToken){
		String responseID="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons/notifications/" + notificationID, notification, sessionToken);
			responseID = parseNotificationId(res.message);
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to update airlock notification. Message: "+e.getMessage()) ;
		}
			return responseID;
	}
	
	/*
	 *not it use
	 *
	public String getNotificationSchema(String seasonID, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonID + "/notificationschema/" , sessionToken);
			response = res.message;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to get airlock notification schema. Message: "+e.getMessage()) ;
		}
			return response;
	}
	
	//PUT /admin/products/seasons/{season-id}/notificationschema
	public String updateNotificationSchema(String seasonID, String schema, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons/" + seasonID + "/notificationschema/" , schema, sessionToken);
			response = res.message;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to get airlock notification schema. Message: "+e.getMessage()) ;
		}
			return response;
	}
	*/
	
	//GET /admin/products/seasons/{season-id}/notifications/outputsample
	public String getNotificationOutputSample(String seasonID, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonID + "/notifications/outputsample" , sessionToken);
			response = res.message;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to get airlock notification output sample. Message: "+e.getMessage()) ;
		}
			return response;
	}
	
	private String parseNotificationId(String notification){
		String notificationID = "";
		JSONObject response = null;
		try {
			response = new JSONObject(notification);
			if (response.containsKey("error")){
				notificationID = notification;
			} else {
				notificationID = (String)response.get("uniqueId");
			}
		} catch (JSONException e) {
			notificationID = "Invalid response in AirlockNotification: " + response;
		}

		return notificationID;

	}

}
