package com.ibm.airlock.admin.airlytics.entities;

import java.util.Date;
import java.util.UUID;

import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;

public abstract class BaseAirlyticsItem {
	protected String name = null; //c+u
	protected UUID uniqueId = null; //nc + u
	protected String description = null; //opt in c+u (if missing or null in update don't change)
	protected Date creationDate = null; //nc + u (not changed)
	protected String creator = null;	//c+u (creator not changed)
	protected Date lastModified = null; // nc + u
	
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
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	public String getCreator() {
		return creator;
	}
	public void setCreator(String creator) {
		this.creator = creator;
	}
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	
	public JSONObject toJson() throws JSONException {
		JSONObject res = new JSONObject();
		
		res.put(Constants.JSON_FIELD_UNIQUE_ID, uniqueId==null?null:uniqueId.toString());
		res.put(Constants.JSON_FIELD_NAME, name);
		res.put(Constants.JSON_FIELD_DESCRIPTION, description);
		res.put(Constants.JSON_FEATURE_FIELD_CREATION_DATE, creationDate.getTime()); 			
		res.put(Constants.JSON_FEATURE_FIELD_CREATOR, creator);
		res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
		
		return res;
	}
	
	public void fromJSON (JSONObject input) throws JSONException {
		if (input.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && input.get(Constants.JSON_FIELD_UNIQUE_ID) != null) {
			String sStr = input.getString(Constants.JSON_FIELD_UNIQUE_ID);			
			uniqueId = UUID.fromString(sStr);		
		}	
		
		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
			long timeInMS = input.getLong(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}
		
		name = input.getString(Constants.JSON_FIELD_NAME);			

		if (input.containsKey(Constants.JSON_FIELD_DESCRIPTION) && input.get(Constants.JSON_FIELD_DESCRIPTION)!=null) 
			description = input.getString(Constants.JSON_FIELD_DESCRIPTION).trim();

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) && input.get(Constants.JSON_FEATURE_FIELD_CREATOR)!=null)
			creator = input.getString(Constants.JSON_FEATURE_FIELD_CREATOR).trim();

		if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && input.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) { 
			long timeInMS = input.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);
			creationDate = new Date(timeInMS);			
		} else {
			creationDate = new Date();
		}
	}
	
	//return null if valid, ValidationResults otherwise
	public ValidationResults validateJSON(JSONObject input) {
		Action action = Action.ADD;
		try {
			if (input.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && input.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing object otherwise create a new one
				action = Action.UPDATE;
			}
			
			//name
			if (!input.containsKey(Constants.JSON_FIELD_NAME) || input.getString(Constants.JSON_FIELD_NAME) == null || input.getString(Constants.JSON_FIELD_NAME).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_NAME), Status.BAD_REQUEST);
			}

			String validateNameErr = Utilities.validateName(input.getString(Constants.JSON_FIELD_NAME));
			if(validateNameErr!=null) {
				return new ValidationResults(validateNameErr, Status.BAD_REQUEST);
			}
			
			//creator
			if (!input.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) || input.getString(Constants.JSON_FEATURE_FIELD_CREATOR) == null || input.getString(Constants.JSON_FEATURE_FIELD_CREATOR).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_CREATOR), Status.BAD_REQUEST);
			}
			
			if (action == Action.ADD) {		
				//modification date => should not appear in object creation
				if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
					return new ValidationResults("The lastModified field cannot be specified during creation.", Status.BAD_REQUEST);
				}
				
				//creation date => should not appear in object creation
				if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && input.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) {
					return new ValidationResults("The creationDate field cannot be specified during creation.", Status.BAD_REQUEST);
				}
			}
			else { //action == Action.UPDATE
				//modification date must appear
				if (!input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || input.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
					return new ValidationResults("The lastModified field is missing. This field must be specified during feature update.", Status.BAD_REQUEST);
				}
				
				//creator must exist and not be changed
				String creatorStr = input.getString(Constants.JSON_FEATURE_FIELD_CREATOR);
				if (!creator.equals(creatorStr)) {
					return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_CREATOR), Status.BAD_REQUEST);
				}
				
				//verify that given modification date is not older that current modification date
				long givenModoficationDate = input.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
				Date givenDate = new Date(givenModoficationDate);
				if (givenDate.before(lastModified)) {
					return new ValidationResults("Item was changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
				}	
				
				//creation date must appear
				if (!input.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) || input.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)==null) {
					return new ValidationResults("The creationDate field is missing. This field must be specified during update.", Status.BAD_REQUEST);
				}

			}
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		
		return null;
	}
	public String update(JSONObject input) throws JSONException {
		boolean wasChanged = false;
        StringBuilder updateDetails = new StringBuilder();
        
        if (input.containsKey(Constants.JSON_FIELD_NAME) && input.get(Constants.JSON_FIELD_NAME)!=null) {
            String updateName = input.getString(Constants.JSON_FIELD_NAME);
            if (!updateName.equals(name)) {
                updateDetails.append("'name' changed from " + name + " to " + updateName + ";	");
                name = updateName;
                wasChanged = true;
            }
        }

        if (input.containsKey(Constants.JSON_FIELD_DESCRIPTION) && input.get(Constants.JSON_FIELD_DESCRIPTION)!=null) {
            String updateDescription = input.getString(Constants.JSON_FIELD_DESCRIPTION);
            if (!updateDescription.equals(description)) {
                updateDetails.append("'description' changed from " + description + " to " + updateDescription + ";	");
                description = updateDescription;
                wasChanged = true;
            }
        }

        if (wasChanged) {
            lastModified = new Date();
        }

        return updateDetails.toString();
	}
}
