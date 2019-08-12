package com.ibm.airlock.admin.operations;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;

public class Webhooks {
	private class CallWebhookRunner implements Runnable {

	    private String url;
	    private JSONObject jsonContent;

	    
	    public CallWebhookRunner(String url, JSONObject jsonContent) {
	    	this.url = url;
	    	this.jsonContent = jsonContent;
	    }

	    public void run() {
	        try {
	        	if (jsonContent != null) {
	        		Utilities.sendPost(url, jsonContent);
	        	}
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	}
	// index by uniqueId
	Map<String, Webhook> webhooksById = new ConcurrentHashMap<String, Webhook>();
	private LinkedList<Webhook> webhooksList = new LinkedList<Webhook>();
	//replace if exists
	public void addWebhook(Webhook hook) {
		webhooksById.put(hook.getUniqueId().toString(), hook);	
		webhooksList.add(hook);
	}
	
	public void removeWebhook(Webhook hook) {
		webhooksById.remove(hook.getUniqueId().toString());
		webhooksList.remove(hook);
	}
	
	public Webhook get(String id) {
		return webhooksById.get(id);
	}
	
	public JSONObject toJSON() throws JSONException{
		JSONObject res = new JSONObject();
		JSONArray hooksArr = new JSONArray();
		for (Webhook hook:webhooksList) {
			hooksArr.add(hook.toJson());	
		}
		res.put (Constants.JSON_FIELD_WEBHOOK_ARRAY, hooksArr);
		
		return res;
	}
		
	public void fromJSON (JSONObject webhooksJSON) throws JSONException {
		JSONArray hooksArr = webhooksJSON.getJSONArray(Constants.JSON_FIELD_WEBHOOK_ARRAY);
		
		//build webhooks map
		Map<String, Webhook> webhooksMap = new ConcurrentHashMap<String, Webhook>();
		LinkedList<Webhook> hooksList = new LinkedList<Webhook>();
		for (int i=0; i<hooksArr.size(); i++) {
			JSONObject webhookJSON = hooksArr.getJSONObject(i);
			Webhook currWebhook = new Webhook();
			currWebhook.fromJSON(webhookJSON);
			webhooksMap.put(currWebhook.getUniqueId().toString(), currWebhook);
			hooksList.add(currWebhook);
			
		}
		webhooksById = webhooksMap;
		webhooksList = hooksList;
	}
	
	public ValidationResults validateWebhookJSON(JSONObject webhookObj, Webhook webhook, ServletContext context) {
		return webhook.validateWebhookJSON(webhookObj, webhooksById, context);
	}
	
	public void notifyChanges(AirlockChange change, ServletContext context) {
		Collection<Webhook> relevantHooks = getRelevantHooks(change);
		for (Webhook webhook: relevantHooks) {
			sendWebhook(webhook, change, context);
		}
	}
	
	private Collection<Webhook> getRelevantHooks(AirlockChange change) {
		LinkedList<Webhook> toRet = new LinkedList<Webhook>();
		for (Webhook currHook : webhooksList) {
			if (currHook.getProducts() == null) {
				toRet.add(currHook);
			} else if (change.getProduct() != null) {
				for (String prodId: currHook.getProducts()) {
					if (prodId.equals(change.getProduct().getUniqueId().toString())) {
						toRet.add(currHook);
						break;
					}
				}
			}
		}
		return toRet;
	}
	
	private void sendWebhook(Webhook hook, AirlockChange change, ServletContext context) {
		if (!Webhooks.isProductsMatch(change, hook)) {
			// this webhook is not relevant for this change
			return;
		}
		try {
			JSONObject content = new JSONObject();
			if (change.getProduct() != null) {
				JSONObject prod = new JSONObject();
				prod.put(Constants.JSON_FIELD_UNIQUE_ID, change.getProduct().getUniqueId().toString());
				prod.put(Constants.JSON_FIELD_NAME, change.getProduct().getName());
				content.put(Constants.JSON_FIELD_WEBHOOK_OUTPUT_PRODUCT, prod);
			}
			if (change.getSeason() != null) {
				JSONObject season = new JSONObject();
				season.put(Constants.JSON_FIELD_UNIQUE_ID, change.getSeason().getUniqueId().toString());
				season.put(Constants.JSON_FIELD_NAME, Utilities.getSeasonString(change.getSeason()));
				content.put(Constants.JSON_FIELD_WEBHOOK_OUTPUT_SEASON, season);
			}
			if (change.getBranch() != null) {
				JSONObject branch = new JSONObject();
				branch.put(Constants.JSON_FIELD_UNIQUE_ID, change.getBranch().getUniqueId().toString());
				branch.put(Constants.JSON_FIELD_NAME, change.getBranch().getName());
				content.put(Constants.JSON_FIELD_WEBHOOK_OUTPUT_BRANCH, branch);
			}
			
			//time
			content.put(Constants.JSON_FIELD_WEBHOOK_OUTPUT_TIME, new Date().getTime());
			JSONArray filesArr = new JSONArray();
			for (AirlockChangeContent changeContent : change.getFiles()) {
				if (Webhooks.isAdminRuntimeValid(changeContent, hook)
						&& Webhooks.isMinStageValid(changeContent, hook)) {
					JSONObject fileObj = new JSONObject();
					fileObj.append(Constants.JSON_FIELD_WEBHOOK_OUTPUT_FILE_NAME, changeContent.getFileName());
					if (changeContent.getJson() != null) {
						fileObj.append(Constants.JSON_FIELD_WEBHOOK_OUTPUT_FILE_CONTENT, changeContent.getJson());
					} else {
						fileObj.append(Constants.JSON_FIELD_WEBHOOK_OUTPUT_FILE_CONTENT, changeContent.getData());
					}
					filesArr.add(fileObj);
				}
			}
			
			if (filesArr.length() > 0) {
				content.put(Constants.JSON_FIELD_WEBHOOK_OUTPUT_FILES, filesArr);
				String urlString = hook.getUrl();
				CallWebhookRunner runner = new CallWebhookRunner(urlString, content);
				Thread sendThread = new Thread(runner);
				sendThread.start();
			} else {
				// do not send the webhook
			}
			

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private static boolean isMinStageValid(AirlockChangeContent content, Webhook webhook) {
		//return true if minStage of the change is relevant for that webhook
		return webhook.getMinStage() == Stage.DEVELOPMENT ||
				content.getMinStage() == Stage.PRODUCTION ||
				webhook.getMinStage() == content.getMinStage();
	}
	
	private static boolean isAdminRuntimeValid(AirlockChangeContent changeContent, Webhook hook) {
		// return true if sendAdmin/sendRuntime of the webhook is relevant for that change
		return (changeContent.isAdmin() && hook.getSendAdmin()) ||
				(changeContent.isRuntime() && hook.getSendRuntime());
	}
	
	private static boolean isProductsMatch(AirlockChange change, Webhook hook) {
		// return true if this is a global webhook or that the product of the change is in the products list of the webhook
		if (hook.getProducts() == null || change.getProduct() == null) {
			return true;
		}
		for (String prodID : hook.getProducts()) {
			if (prodID.equals(change.getProduct().getUniqueId().toString())) {
				return true;
			}
		}
		return false;
	}
	
	static public Webhooks get(ServletContext context)
	{
		return (Webhooks) context.getAttribute(Constants.WEBHOOKS_PARAM_NAME);
	}

}
