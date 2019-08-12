package com.ibm.airlock.admin.operations;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Strings;
import javax.ws.rs.core.Response.Status;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.Utilities.ProductErrorPair;


public class Webhook {
	public static final Logger logger = Logger.getLogger(Webhook.class.getName());
	
	
	private String name = null;
	private UUID uniqueId = null;
	private Date lastModified = null; 
	private String[] products = null;
	private String url = null;
	private Stage minStage = null;
	private boolean sendRuntime = false;
	private boolean sendAdmin = false;
	protected Date creationDate = null; //nc + u (not changed)
	protected String creator = null;	//c+u (creator not changed)
	//creator, creation date
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public UUID getUniqueId() {
		return uniqueId;
	}
	
	public void setUniqueId(UUID uniqueId) {
		this.uniqueId = uniqueId;
	}
		
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	
	public JSONObject toJson() throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_NAME, name);
		res.put(Constants.JSON_FEATURE_FIELD_CREATION_DATE, creationDate.getTime()); 			
		res.put(Constants.JSON_FEATURE_FIELD_CREATOR, creator);
		res.put(Constants.JSON_FIELD_WEBHOOK_URL, url);
		res.put(Constants.JSON_FIELD_UNIQUE_ID, uniqueId==null?null:uniqueId.toString());
		res.put(Constants.JSON_FIELD_WEBHOOK_SEND_ADMIN, sendAdmin);
		res.put(Constants.JSON_FIELD_WEBHOOK_SEND_RUNTIME, sendRuntime);
		res.put(Constants.JSON_FIELD_WEBHOOK_STAGE, minStage.toString());	
		if (products!=null && products.length > 0) {
			res.put(Constants.JSON_FIELD_WEBHOOK_PRODUCTS, products);	
		} else {
			String val = null;
			res.put(Constants.JSON_FIELD_WEBHOOK_PRODUCTS, val);
		}
		res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
		
		return res;
	}
	
	public void fromJSON (JSONObject input) throws JSONException {
		name = (String)input.get(Constants.JSON_FIELD_NAME);
		
		if (input.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && input.get(Constants.JSON_FIELD_UNIQUE_ID) != null) {
			String sStr = (String)input.get(Constants.JSON_FIELD_UNIQUE_ID);			
			uniqueId = UUID.fromString(sStr);		
		}
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) && input.get(Constants.JSON_FEATURE_FIELD_CREATOR)!=null)
			creator = input.getString(Constants.JSON_FEATURE_FIELD_CREATOR).trim();
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && input.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) { 
			long timeInMS = input.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);
			creationDate = new Date(timeInMS);			
		} else {
			creationDate = new Date();
		}
		
		if (input.containsKey(Constants.JSON_FIELD_WEBHOOK_SEND_ADMIN)) 
			sendAdmin = input.getBoolean(Constants.JSON_FIELD_WEBHOOK_SEND_ADMIN);
		
		if (input.containsKey(Constants.JSON_FIELD_WEBHOOK_URL)) 
			url = (String)input.get(Constants.JSON_FIELD_WEBHOOK_URL);
		
		if (input.containsKey(Constants.JSON_FIELD_WEBHOOK_SEND_RUNTIME)) 
			sendRuntime = input.getBoolean(Constants.JSON_FIELD_WEBHOOK_SEND_RUNTIME);
		
		if (input.containsKey(Constants.JSON_FIELD_WEBHOOK_STAGE) && input.get(Constants.JSON_FIELD_WEBHOOK_STAGE)!=null)
			minStage = Utilities.strToStage(input.getString(Constants.JSON_FIELD_WEBHOOK_STAGE));
		
		if (input.containsKey(Constants.JSON_FIELD_WEBHOOK_PRODUCTS) && input.get(Constants.JSON_FIELD_WEBHOOK_PRODUCTS)!=null) {
			products = Utilities.jsonArrToStringArr(input.getJSONArray(Constants.JSON_FIELD_WEBHOOK_PRODUCTS));						
		} else {
			products = null;
		}
		
		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
			long timeInMS = (Long)input.get(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}
	}
	
	
	//Return a string with update details.
	//If nothing was changed - return empty string 
	public String updateWebhook(JSONObject updatedWebhookData) throws JSONException {

		StringBuilder updateDetails = new StringBuilder();
		boolean wasChanged = false;
		String updatedName = updatedWebhookData.getString(Constants.JSON_FIELD_NAME);
		if (!updatedName.equals(name)) {
			updateDetails.append("'name' changed from " + name + " to " + updatedName + ";	");
			name = updatedName;
			wasChanged = true;			
		}
		
		//optional fields
		if (updatedWebhookData.containsKey(Constants.JSON_FIELD_WEBHOOK_SEND_ADMIN) && updatedWebhookData.get(Constants.JSON_FIELD_WEBHOOK_SEND_ADMIN)!=null) {
			boolean updatedSendAdmin = updatedWebhookData.getBoolean(Constants.JSON_FIELD_WEBHOOK_SEND_ADMIN);
			if (sendAdmin != updatedSendAdmin) {
				updateDetails.append("'sendAdmin' changed from " + (sendAdmin?"true":"false") + " to " + (updatedSendAdmin? "true": "false") + "\n");
				sendAdmin = updatedSendAdmin;
				wasChanged = true; 
			}
		}
		
		if (updatedWebhookData.containsKey(Constants.JSON_FIELD_WEBHOOK_SEND_RUNTIME) && updatedWebhookData.get(Constants.JSON_FIELD_WEBHOOK_SEND_RUNTIME)!=null) {
			boolean updatedSendRuntime = updatedWebhookData.getBoolean(Constants.JSON_FIELD_WEBHOOK_SEND_RUNTIME);
			if (sendRuntime != updatedSendRuntime) {
				updateDetails.append("'sendRuntime' changed from " + (sendRuntime?"true":"false") + " to " + (updatedSendRuntime? "true": "false") + "\n");
				sendRuntime = updatedSendRuntime;
				wasChanged = true; 
			}
		}
		if (updatedWebhookData.containsKey(Constants.JSON_FIELD_WEBHOOK_STAGE) && updatedWebhookData.get(Constants.JSON_FIELD_WEBHOOK_STAGE)!=null) {
			Stage updatedminStage = Utilities.strToStage(updatedWebhookData.getString(Constants.JSON_FIELD_WEBHOOK_STAGE));
			if (minStage != updatedminStage) {
				updateDetails.append("'minStage' changed from " + (minStage.toString()) + " to " + (updatedminStage.toString()) + "\n");
				minStage = updatedminStage;
				wasChanged = true; 
			}
		}
		if (updatedWebhookData.containsKey(Constants.JSON_FIELD_WEBHOOK_URL) && updatedWebhookData.get(Constants.JSON_FIELD_WEBHOOK_URL)!=null) {
			String updatedURL = updatedWebhookData.getString(Constants.JSON_FIELD_WEBHOOK_URL);
			if (!url.equals(updatedURL)) {
				updateDetails.append("'url' changed from " + url + " to " + updatedURL + "\n");
				url = updatedURL;
				wasChanged = true; 
			}
		}
		if (updatedWebhookData.containsKey(Constants.JSON_FIELD_WEBHOOK_PRODUCTS) && updatedWebhookData.get(Constants.JSON_FIELD_WEBHOOK_PRODUCTS)!=null) {
			JSONArray updatedProducts = updatedWebhookData.getJSONArray(Constants.JSON_FIELD_WEBHOOK_PRODUCTS);
			if (products == null || !Utilities.stringArrayCompareIgnoreOrder(updatedProducts,products)) {
				updateDetails.append("'products' changed from " + Arrays.toString(products) + " to " +  Arrays.toString(Utilities.jsonArrToStringArr(updatedProducts)) + "\n");
				products = Utilities.jsonArrToStringArr(updatedProducts);
				wasChanged = true;
			}
		} else {
			products = null;
		}
		
		
		if (wasChanged) {
			lastModified = new Date();
		}

		if (updateDetails.length()!=0) {
			updateDetails.insert(0,"Webhook changes: ");
		}
		return updateDetails.toString();
	}
	
	public ValidationResults validateWebhookJSON(JSONObject webhookObj, Map<String, Webhook> webhooksDB, ServletContext context) {
		Action action = Action.ADD;
		try {
			if (webhookObj.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && webhookObj.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing webhook otherwise create a new webhook
				action = Action.UPDATE;
			}						
			
			//name
			if (!webhookObj.containsKey(Constants.JSON_FIELD_NAME) || webhookObj.get(Constants.JSON_FIELD_NAME) == null || webhookObj.getString(Constants.JSON_FIELD_NAME).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_NAME), Status.BAD_REQUEST);
			}
							
			String validateNameErr = Utilities.validateName(webhookObj.getString(Constants.JSON_FIELD_NAME));
			if(validateNameErr!=null) {
				return new ValidationResults(validateNameErr, Status.BAD_REQUEST);
			}
			
			//URL
			if (!webhookObj.containsKey(Constants.JSON_FIELD_WEBHOOK_URL) || webhookObj.get(Constants.JSON_FIELD_WEBHOOK_URL) == null || webhookObj.getString(Constants.JSON_FIELD_WEBHOOK_URL).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_WEBHOOK_URL), Status.BAD_REQUEST);
			}
			try {
				URL url = new URL(webhookObj.getString(Constants.JSON_FIELD_WEBHOOK_URL));
			} catch (MalformedURLException e) {
				// bad URL
				return new ValidationResults(String.format(Strings.malformedUrl, webhookObj.getString(Constants.JSON_FIELD_WEBHOOK_URL)), Status.BAD_REQUEST);
			}
			//minStage
			if (!webhookObj.containsKey(Constants.JSON_FIELD_WEBHOOK_STAGE) || webhookObj.get(Constants.JSON_FIELD_WEBHOOK_STAGE) == null || webhookObj.getString(Constants.JSON_FIELD_WEBHOOK_STAGE).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_WEBHOOK_STAGE), Status.BAD_REQUEST);
			}
			//sendAdmin
			if (!webhookObj.containsKey(Constants.JSON_FIELD_WEBHOOK_SEND_ADMIN) || webhookObj.get(Constants.JSON_FIELD_WEBHOOK_SEND_ADMIN) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_WEBHOOK_SEND_ADMIN), Status.BAD_REQUEST);
			}
			boolean bool;
			try {
				bool = (boolean)webhookObj.get(Constants.JSON_FIELD_WEBHOOK_SEND_ADMIN);
			} catch (ClassCastException e) {
				return new ValidationResults(String.format(Strings.nonBooleanField, Constants.JSON_FIELD_WEBHOOK_SEND_ADMIN), Status.BAD_REQUEST);
			}
			//sendRuntime
			if (!webhookObj.containsKey(Constants.JSON_FIELD_WEBHOOK_SEND_RUNTIME) || webhookObj.get(Constants.JSON_FIELD_WEBHOOK_SEND_RUNTIME) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_WEBHOOK_SEND_RUNTIME), Status.BAD_REQUEST);
			}
			try {
				bool = (boolean)webhookObj.get(Constants.JSON_FIELD_WEBHOOK_SEND_RUNTIME);
			} catch (ClassCastException e) {
				return new ValidationResults(String.format(Strings.nonBooleanField, Constants.JSON_FIELD_WEBHOOK_SEND_RUNTIME), Status.BAD_REQUEST);
			}
			//creator
			if (!webhookObj.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) || webhookObj.getString(Constants.JSON_FEATURE_FIELD_CREATOR) == null || webhookObj.getString(Constants.JSON_FEATURE_FIELD_CREATOR).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_CREATOR), Status.BAD_REQUEST);
			}
			
			//validate name uniqueness
			String updatedName = webhookObj.getString(Constants.JSON_FIELD_NAME);
			Set<String> webhookIds = webhooksDB.keySet();
			for (String hookId:webhookIds) {
				Webhook webhook = webhooksDB.get(hookId);
				if (uniqueId==null || !hookId.equals(uniqueId.toString())) {  //in update - skip yourself
					if (webhook.getName().equalsIgnoreCase(updatedName)) {
						return new ValidationResults("A webhook with the specified name already exists.", Status.BAD_REQUEST);
					}
				}
			}
			
			
			//products - optional field
			if (webhookObj.containsKey(Constants.JSON_FIELD_WEBHOOK_PRODUCTS) && webhookObj.get(Constants.JSON_FIELD_WEBHOOK_PRODUCTS) != null) {				
				JSONArray prodsArr = webhookObj.getJSONArray(Constants.JSON_FIELD_WEBHOOK_PRODUCTS);
				if (prodsArr.length() == 0) {
					return new ValidationResults("Cannot have empty products list", Status.BAD_REQUEST);
				}
				for (int i=0; i<prodsArr.length(); i++) {
					String prodId  = prodsArr.getString(i);
					ProductErrorPair prodErrPair = Utilities.getProduct(context, prodId);
					if (prodErrPair.error != null) {			
						return new ValidationResults("Product with id "+prodId+" does not exist", Status.BAD_REQUEST);
					}
				}
			}				
			
			if (action == Action.ADD) {
				//modification date => should not appear in feature creation
				if (webhookObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && webhookObj.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
					return new ValidationResults("The lastModified field cannot be specified during product creation.", Status.BAD_REQUEST);
				}
				
				//creation date => should not appear in branch creation
				if (webhookObj.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && webhookObj.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) {
					return new ValidationResults("The creationDate field cannot be specified during creation.", Status.BAD_REQUEST);
				}
			}
			else { //update				
				
				//creation date must appear
				if (!webhookObj.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) || webhookObj.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)==null) {
					return new ValidationResults("The creationDate field is missing. This field must be specified during update.", Status.BAD_REQUEST);
				}
				//verify that legal long
				long creationdateLong = webhookObj.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);

				//verify that was not changed
				if (!creationDate.equals(new Date(creationdateLong))) {
					return new ValidationResults("creationDate cannot be changed during update", Status.BAD_REQUEST);
				}
				
				//creator must exist and not be changed
				String creatorStr = webhookObj.getString(Constants.JSON_FEATURE_FIELD_CREATOR);
				if (!creator.equals(creatorStr)) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_CREATOR), Status.BAD_REQUEST);
				}
				
				//modification date must appear
				if (!webhookObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || webhookObj.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
					return new ValidationResults("The lastModified field is missing. This field must be specified during product update.", Status.BAD_REQUEST);
				}
				
				//verify that given modification date is not older that current modification date
				long givenModoficationDate = webhookObj.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
				Date givenDate = new Date(givenModoficationDate);
				if (givenDate.before(lastModified)) {
					return new ValidationResults("The webhook was changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
				}
			}
			
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal webhook JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}
		
		return null;
		
	}
	
	public String[] getProducts() {
		return products;
	}
	public void setProducts(String[] products) {
		this.products = products;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	public Boolean getSendRuntime() {
		return sendRuntime;
	}
	public void setSendRuntime(Boolean sendRuntime) {
		this.sendRuntime = sendRuntime;
	}
	public Boolean getSendAdmin() {
		return sendAdmin;
	}
	public void setSendAdmin(Boolean sendAdmin) {
		this.sendAdmin = sendAdmin;
	}
	
	public Stage getMinStage() {
		return this.minStage;
	}
		
}
