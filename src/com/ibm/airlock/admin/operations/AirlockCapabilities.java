package com.ibm.airlock.admin.operations;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.AirlockCapability;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;

public class AirlockCapabilities {
	private Set<AirlockCapability> capabilities = new TreeSet<AirlockCapability>();//mandatory in update	
	private Date lastModified = null; //mandatory in update

	public Set<AirlockCapability> getCapabilities() {
		return capabilities;
	}
	public void setCapabilities(Set<AirlockCapability> capabilities) {
		this.capabilities = capabilities;
	}
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject res = new JSONObject();

		res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified == null?null:lastModified.getTime());
		res.put(Constants.JSON_FIELD_CAPABILITIES, Utilities.capabilitieslistToJsonArray(capabilities));

		return res;			
	}

	public void fromJSON (JSONObject CapabilitiesJSON) throws JSONException {		
		if (CapabilitiesJSON.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && CapabilitiesJSON.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
			long timeInMS = CapabilitiesJSON.getLong(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}

		capabilities.clear();
		if (CapabilitiesJSON.containsKey(Constants.JSON_FIELD_CAPABILITIES) && CapabilitiesJSON.get(Constants.JSON_FIELD_CAPABILITIES)!=null) {				
			JSONArray capabilitiesArr = CapabilitiesJSON.getJSONArray(Constants.JSON_FIELD_CAPABILITIES);
			for (int i=0; i<capabilitiesArr.size(); i++) {
				String capabilityStr = capabilitiesArr.getString(i);
				AirlockCapability capability = Utilities.strToAirlockCapability(capabilityStr);
				if (capability == null)
					throw new JSONException("invalid capability " + capability);
				capabilities.add(capability);
			}
		}		
	}

	public static ValidationResults validateCapabilitiesList(JSONArray capabilities, Set<AirlockCapability> baseCapabilities) {
		try {
			if (capabilities == null || capabilities.isEmpty()) {
				return new ValidationResults("No capabilities specified", Status.BAD_REQUEST);
			}

			TreeSet<AirlockCapability> capabilitiesSet = new TreeSet<>();		
			for (int i=0; i<capabilities.size(); i++) {
				String capabilityStr=capabilities.getString(i);
				
				AirlockCapability capability = Utilities.strToAirlockCapability(capabilityStr);
				if (capability == null) {
					return new ValidationResults("Illegal capability '" + capabilityStr + "'", Status.BAD_REQUEST);
				}
				
				if (capabilitiesSet.contains(capability)) {
					return new ValidationResults("Capability '" + capabilityStr + "' appears more than once.", Status.BAD_REQUEST);
				}
				
				capabilitiesSet.add(capability);
			}
			
			if (!capabilitiesSet.contains(AirlockCapability.FEATURES)) {				
				return new ValidationResults("FEATURES capability is mandatory.", Status.BAD_REQUEST);
			}
			if (capabilitiesSet.contains(AirlockCapability.EXPERIMENTS) && !capabilitiesSet.contains(AirlockCapability.BRANCHES)) {
				return new ValidationResults("Cannot enable EXPERIMENTS when BRANCHES are disabled", Status.BAD_REQUEST);
			}
			
			if (baseCapabilities!=null) {
				if (!baseCapabilities.containsAll(capabilitiesSet)) {
					return new ValidationResults("One or more capabilities are not enabled in the server.", Status.BAD_REQUEST);
				}
			}
		} catch (JSONException je) {
			return new ValidationResults(je.getMessage(), Status.BAD_REQUEST);
		}

		

		return null;
	}		

	public ValidationResults validateCapabilitiesJSON (JSONObject capabilitiesJSON, ServletContext context) {	
		try {
			//always update
			//modification date must appear in update
			if (!capabilitiesJSON.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || capabilitiesJSON.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
				return new ValidationResults(Strings.lastModifiedIsMissing, Status.BAD_REQUEST);
			}				

			//verify that given modification date is not older that current modification date
			long givenModoficationDate = capabilitiesJSON.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
			Date givenDate = new Date(givenModoficationDate);
			if (givenDate.before(lastModified)) {
				return new ValidationResults(String.format(Strings.itemChangedByAnotherUser, "api key"), Status.CONFLICT);			
			}

			//capabilities
			if (!capabilitiesJSON.containsKey(Constants.JSON_FIELD_CAPABILITIES) || capabilitiesJSON.get(Constants.JSON_FIELD_CAPABILITIES) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_CAPABILITIES), Status.BAD_REQUEST);
			}

			JSONArray capabilitiesArr = capabilitiesJSON.getJSONArray(Constants.JSON_FIELD_CAPABILITIES);
			ValidationResults res = validateCapabilitiesList(capabilitiesArr, null);
			if (res!=null)
				return res;
			
			
			//if reducing RUNTIME_ENCRYPTION encryption and some season is configured to encrypt runtime files - throw an error
			boolean orgCapabilitiesRuntimeEncryptionIncluded = capabilities.contains(AirlockCapability.RUNTIME_ENCRYPTION);
			boolean newCapabilitiesRuntimeEncryptionIncluded = new TreeSet<String>(capabilitiesArr).contains(AirlockCapability.RUNTIME_ENCRYPTION.toString());
			if (orgCapabilitiesRuntimeEncryptionIncluded && !newCapabilitiesRuntimeEncryptionIncluded) {
				String prodsWithEncryptedSeasons =  getProductsWithEncryptedSeasons(context);
				if (!prodsWithEncryptedSeasons.isEmpty()) {
					return new ValidationResults(String.format(Strings.failReduceRuntimeEncryptionCapability, prodsWithEncryptedSeasons), Status.BAD_REQUEST);
				}
			}
		}catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}

		return null;
	}
	
	private String getProductsWithEncryptedSeasons(ServletContext context) {
		StringBuilder sb = new StringBuilder();
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

		Set<String> ids = productsDB.keySet();
		for (String id:ids) {
			Product p = productsDB.get(id);
		
			if (p.containSeasonWithRuntimeEncryption()) {
				sb.append(p.getName() + ", ");
			}
		}
		return sb.toString();
	}
	
	public String updateCapabilitiesJSON (JSONObject capabilitiesJSON) throws JSONException {
		//the only capabilities field that can be updated

		JSONArray updatedCapabilities = capabilitiesJSON.getJSONArray(Constants.JSON_FIELD_CAPABILITIES);
		String updateDetails = updateCapabilities(updatedCapabilities, capabilities);
		boolean wasChanged = !updateDetails.isEmpty();

		if (wasChanged) {
			lastModified = new Date();
		}
		
		return updateDetails;
	}
	
	public static String updateCapabilities(JSONArray updateCapabiltiesArr, Set<AirlockCapability> capabilities) throws JSONException {
		StringBuilder updateDetails = new StringBuilder();
		
		Set<AirlockCapability> newCapabilities = new TreeSet<AirlockCapability>();
		for (int i = 0; i < updateCapabiltiesArr.size(); ++i)
		{
			String capabilityStr = updateCapabiltiesArr.getString(i);
			AirlockCapability capability = Utilities.strToAirlockCapability(capabilityStr);
			if (capability == null)
				throw new JSONException("Invalid capability " + capabilityStr);
			newCapabilities.add(capability);
		}

		if (!newCapabilities.equals(capabilities))
		{
			updateDetails.append(" 'capabilities' changed from " + capabilities + " to " + newCapabilities + "\n");
			capabilities.clear();
			capabilities.addAll(newCapabilities);			
		}		
		
		return updateDetails.toString();
	}
	
	//if no product was changed return null
	//if some product was updated - return string with the updated details
	public String updateProductWithReducdedCapabilities(Set<AirlockCapability> prevCapabilities, ServletContext context) {
		boolean someProdUpdated = false;
		StringBuilder sb = new StringBuilder();
		prevCapabilities.removeAll(capabilities);
		Set<AirlockCapability> reducedCapabilities = prevCapabilities;
		if (reducedCapabilities.size() > 0) {
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
			Set<String> productsIds = productsDB.keySet();
			for (String prodId:productsIds) {
				Product prod = productsDB.get(prodId);
				for (AirlockCapability reducedCapability:reducedCapabilities) {
					if (prod.getCapabilities().contains(reducedCapability)) {
						someProdUpdated = true;
						prod.getCapabilities().remove(reducedCapability);
						sb.append("The  " + reducedCapability.toString() + " capability was removed from product " + prod.getName() + ", " + prod.getUniqueId().toString() + "\n");
					}
				}
				
			}
		}
		if (someProdUpdated) {
			return sb.toString();
		}
		return null;
	}
	
	public boolean contains(AirlockCapability capability) {
		return capabilities.contains(capability);
	}
}
