package com.ibm.airlock.admin.dataimport;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.airlytics.JobStatus;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class DataImportItem {

    private static final String OLD_TABLE = "user_features";

    private UUID productId = null;
    private UUID uniqueId = null;
    private String name = null; //c+u
    private Date creationDate = null; //nc + u (not changed)
    private String creator = null;	//c+u (creator not changed)
    private Date lastModified = null; // required in update. forbidden in create
    private String description = null; //opt in c+u (if missing or null in update don't change)
    //file path
    private String s3File;
    //status
    private JobStatus status;
    private String statusMessage;
    private String detailedMessage;
    private Long successfulImports;
    private List<String> affectedColumns;
    private String targetTable = OLD_TABLE; // default value for backwards compatibility    
    private Boolean overwrite = Boolean.TRUE; // whether the data in the table should be replaced or updated

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getS3File() {
        return s3File;
    }

    public void setS3File(String s3File) {
        this.s3File = s3File;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public void setDetailedMessage(String detailedMessage) {
        this.detailedMessage = detailedMessage;
    }

    public Long getSuccessfulImports() {
        return successfulImports;
    }

    public void setSuccessfulImports(Long successfulImports) {
        this.successfulImports = successfulImports;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTargetTable() {
		return targetTable;
	}

	public void setTargetTable(String targetTable) {
		this.targetTable = targetTable;
	}

	public Boolean getOverwrite() {
		return overwrite;
	}

	public void setOverwrite(Boolean overwrite) {
		this.overwrite = overwrite;
	}

	public DataImportItem(UUID productId) {

    }

    public JSONObject toJson() throws JSONException {
        JSONObject res = new JSONObject();

        res.put(Constants.JSON_FIELD_UNIQUE_ID, uniqueId==null?null:uniqueId.toString());
        res.put(Constants.JSON_FIELD_PRODUCT_ID, productId==null?null:productId.toString());
        res.put(Constants.JSON_FIELD_DESCRIPTION, description);
        res.put(Constants.JSON_FIELD_NAME, name);
        res.put(Constants.JSON_FEATURE_FIELD_CREATION_DATE, creationDate.getTime());
        res.put(Constants.JSON_FEATURE_FIELD_CREATOR, creator);
        res.put(Constants.JSON_FIELD_DATA_IMPORT_S3_FILE, s3File);
        res.put(Constants.JSON_FIELD_DATA_IMPORT_OVERWRITE, overwrite);
        res.put(Constants.JSON_FIELD_DATA_IMPORT_TARGET_TABLE, targetTable);
        res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
        res.put(Constants.JSON_FIELD_DATA_IMPORT_STATUS, status != null ? status.toString() : null);
        res.put(Constants.JSON_FIELD_DATA_IMPORT_STATUS_DETAILS, detailedMessage);
        res.put(Constants.JSON_FIELD_DATA_IMPORT_STATUS_MESSAGE, statusMessage);
        res.put(Constants.JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS, successfulImports);
        res.put(Constants.JSON_FIELD_DATA_IMPORT_STATUS_AFFECTED_COLUMNS, affectedColumns != null ? new JSONArray(affectedColumns) : null);
        return res;
    }

    public void fromJSON(JSONObject input) throws JSONException {
        if (input.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && input.get(Constants.JSON_FIELD_PRODUCT_ID) != null) {
            String sStr = input.getString(Constants.JSON_FIELD_PRODUCT_ID);
            productId = UUID.fromString(sStr);
        }

        if (input.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && input.get(Constants.JSON_FIELD_UNIQUE_ID) != null) {
            String sStr = input.getString(Constants.JSON_FIELD_UNIQUE_ID);
            uniqueId = UUID.fromString(sStr);
        }

        if (input.containsKey(Constants.JSON_FIELD_NAME) && input.get(Constants.JSON_FIELD_NAME)!=null)
            name = input.getString(Constants.JSON_FIELD_NAME);

        if (input.containsKey(Constants.JSON_FIELD_DESCRIPTION) && input.get(Constants.JSON_FIELD_DESCRIPTION)!=null)
            description = input.getString(Constants.JSON_FIELD_DESCRIPTION).trim();


        if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) && input.get(Constants.JSON_FEATURE_FIELD_CREATOR)!=null)
            creator = input.getString(Constants.JSON_FEATURE_FIELD_CREATOR).trim();

        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_S3_FILE) && input.get(Constants.JSON_FIELD_DATA_IMPORT_S3_FILE)!=null)
            s3File = input.getString(Constants.JSON_FIELD_DATA_IMPORT_S3_FILE).trim();
        
        if (!input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_OVERWRITE) && !input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_TARGET_TABLE)) { // this is an old JSON, before introduction of overwrite flag
        	overwrite = Boolean.FALSE; // old default
        	targetTable = OLD_TABLE;
        } else { // overwrite and target table must be explicitly present
        	overwrite = Boolean.TRUE; // new default
        	targetTable = null;
        	
        	if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_OVERWRITE) && input.get(Constants.JSON_FIELD_DATA_IMPORT_OVERWRITE)!=null)
        		overwrite = input.getBoolean(Constants.JSON_FIELD_DATA_IMPORT_OVERWRITE);

	        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_TARGET_TABLE) && input.get(Constants.JSON_FIELD_DATA_IMPORT_TARGET_TABLE)!=null)
	        	targetTable = input.getString(Constants.JSON_FIELD_DATA_IMPORT_TARGET_TABLE).trim();
        }


        if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && input.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) {
            long timeInMS = input.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);
            creationDate = new Date(timeInMS);
        } else {
            creationDate = new Date();
        }


        if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
            long timeInMS = input.getLong(Constants.JSON_FIELD_LAST_MODIFIED);
            lastModified = new Date(timeInMS);
        }  else {
            lastModified = new Date();
        }

        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS) && input.get(Constants.JSON_FIELD_DATA_IMPORT_STATUS)!=null)
            status = JobStatus.fromString(input.getString(Constants.JSON_FIELD_DATA_IMPORT_STATUS).trim());

        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS_MESSAGE) && input.get(Constants.JSON_FIELD_DATA_IMPORT_STATUS_MESSAGE)!=null)
            statusMessage = input.getString(Constants.JSON_FIELD_DATA_IMPORT_STATUS_MESSAGE).trim();

        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS_DETAILS) && input.get(Constants.JSON_FIELD_DATA_IMPORT_STATUS_DETAILS)!=null)
            detailedMessage = input.getString(Constants.JSON_FIELD_DATA_IMPORT_STATUS_DETAILS).trim();

        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS) && input.get(Constants.JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS)!=null)
            successfulImports = input.getLong(Constants.JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS);

        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS_AFFECTED_COLUMNS) && input.get(Constants.JSON_FIELD_DATA_IMPORT_STATUS_AFFECTED_COLUMNS)!=null)
            affectedColumns = this.jsonArrayToList(input.getJSONArray(Constants.JSON_FIELD_DATA_IMPORT_STATUS_AFFECTED_COLUMNS));

    }

    public ValidationResults validateDataImportJSON(JSONObject jobJSON) throws JSONException {
        try {
            Constants.Action action = Constants.Action.ADD;
            if (jobJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && jobJSON.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
                //if JSON contains uniqueId - update an existing feature otherwise create a new feature
                action = Constants.Action.UPDATE;
            }

            String objName = jobJSON.getString(Constants.JSON_FIELD_NAME);
            String validateNameErr = Utilities.validateName(objName);
            if(validateNameErr!=null) {
                return new ValidationResults(validateNameErr, Response.Status.BAD_REQUEST);
            }

            //successfulImports - if exists, verify its a long
            if (jobJSON.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS) && jobJSON.get(Constants.JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS) != null) {
                jobJSON.getLong(Constants.JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS);
            }


            //creator
            if (!jobJSON.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) || jobJSON.getString(Constants.JSON_FEATURE_FIELD_CREATOR) == null || jobJSON.getString(Constants.JSON_FEATURE_FIELD_CREATOR).isEmpty()) {
                return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_CREATOR), Response.Status.BAD_REQUEST);
            }

            //s3File
            if (!jobJSON.containsKey(Constants.JSON_FIELD_DATA_IMPORT_S3_FILE) || jobJSON.getString(Constants.JSON_FIELD_DATA_IMPORT_S3_FILE) == null || jobJSON.getString(Constants.JSON_FIELD_DATA_IMPORT_S3_FILE).isEmpty()) {
                return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_DATA_IMPORT_S3_FILE), Response.Status.BAD_REQUEST);
            }
            
            if (jobJSON.containsKey(Constants.JSON_FIELD_DATA_IMPORT_OVERWRITE) || jobJSON.containsKey(Constants.JSON_FIELD_DATA_IMPORT_TARGET_TABLE)) {
            	
            	if(jobJSON.containsKey(Constants.JSON_FIELD_DATA_IMPORT_OVERWRITE) && jobJSON.get(Constants.JSON_FIELD_DATA_IMPORT_OVERWRITE) == null) {
            		return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_DATA_IMPORT_OVERWRITE), Response.Status.BAD_REQUEST);
            	}
            	
            	if (!jobJSON.containsKey(Constants.JSON_FIELD_DATA_IMPORT_TARGET_TABLE) || jobJSON.getString(Constants.JSON_FIELD_DATA_IMPORT_TARGET_TABLE) == null || jobJSON.getString(Constants.JSON_FIELD_DATA_IMPORT_TARGET_TABLE).isEmpty()) {
                    return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_DATA_IMPORT_TARGET_TABLE), Response.Status.BAD_REQUEST);
                }
            }

            if (action == Constants.Action.ADD){
                //creation date => should not appear in branch creation
                if (jobJSON.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && jobJSON.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) {
                    return new ValidationResults("The creationDate field cannot be specified during creation.", Response.Status.BAD_REQUEST);
                }
            } else {
                //creation date must appear
                if (!jobJSON.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) || jobJSON.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)==null) {
                    return new ValidationResults("The creationDate field is missing. This field must be specified during update.", Response.Status.BAD_REQUEST);
                }

                //verify that legal long
                long creationdateLong = jobJSON.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);

                //verify that was not changed
                if (!creationDate.equals(new Date(creationdateLong))) {
                    return new ValidationResults("creationDate cannot be changed during update", Response.Status.BAD_REQUEST);
                }

                //creator must exist and not be changed
                String creatorStr = jobJSON.getString(Constants.JSON_FEATURE_FIELD_CREATOR);
                if (!creator.equals(creatorStr)) {
                    return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_CREATOR), Response.Status.BAD_REQUEST);
                }
            }



            //status
            if (jobJSON.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS) && jobJSON.get(Constants.JSON_FIELD_DATA_IMPORT_STATUS) != null && !jobJSON.getString(Constants.JSON_FIELD_DATA_IMPORT_STATUS).isEmpty()) {
                if (JobStatus.fromString(jobJSON.getString(Constants.JSON_FIELD_DATA_IMPORT_STATUS).trim())==null) {
                    return new ValidationResults(String.format(Strings.fieldStatus, jobJSON.getString(Constants.JSON_FIELD_DATA_IMPORT_STATUS).trim()), Response.Status.BAD_REQUEST);
                }
            }
        } catch (JSONException jsne) {
            return new ValidationResults(jsne.getMessage(), Response.Status.BAD_REQUEST);
        }
        return null;


    }

    public ValidationResults validateDataImportStatusJSON(JSONObject updatedJobJSON, ServletContext context) throws JSONException {
        //status
        if (updatedJobJSON.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS) && updatedJobJSON.getString(Constants.JSON_FIELD_DATA_IMPORT_STATUS) != null) {
            if (JobStatus.fromString(updatedJobJSON.getString(Constants.JSON_FIELD_DATA_IMPORT_STATUS))==null) {
                return new ValidationResults(String.format(Strings.fieldStatus, updatedJobJSON.getString(Constants.JSON_FIELD_DATA_IMPORT_STATUS)), Response.Status.BAD_REQUEST);
            }
        }

        if (updatedJobJSON.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS) && updatedJobJSON.get(Constants.JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS) != null) {
            try {
                updatedJobJSON.getLong(Constants.JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS);
            } catch (JSONException e) {
                return new ValidationResults(Strings.fieldSuccessfulImports, Response.Status.BAD_REQUEST);
            }
        }
        return null;
    }

    public String updateDataImportStatus(JSONObject input, Date now) throws JSONException {
        StringBuilder updateDetails = new StringBuilder();
        boolean wasChanged = false;
        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS) && input.get(Constants.JSON_FIELD_DATA_IMPORT_STATUS)!=null) {
            JobStatus updateStatus = JobStatus.fromString(input.getString(Constants.JSON_FIELD_DATA_IMPORT_STATUS));
            if (status == null || !updateStatus.toString().equals(status.toString())) {
                updateDetails.append("'status' changed from " + status + " to " + updateStatus + ";	");
                status = updateStatus;
                wasChanged = true;
            }
        }
        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS_MESSAGE) && input.get(Constants.JSON_FIELD_DATA_IMPORT_STATUS_MESSAGE)!=null) {
            String updateStatusMessage = input.getString(Constants.JSON_FIELD_DATA_IMPORT_STATUS_MESSAGE);
            if (statusMessage == null || !updateStatusMessage.equals(statusMessage)) {
                updateDetails.append("'statusMessage' changed from " + statusMessage + " to " + updateStatusMessage + ";	");
                statusMessage = updateStatusMessage;
                wasChanged = true;
            }
        }

        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS_DETAILS) && input.get(Constants.JSON_FIELD_DATA_IMPORT_STATUS_DETAILS)!=null) {
            String updateStatusDetails = input.getString(Constants.JSON_FIELD_DATA_IMPORT_STATUS_DETAILS);
            if (detailedMessage == null || !updateStatusDetails.equals(detailedMessage)) {
                updateDetails.append("'detailedMessage' changed from " + detailedMessage + " to " + updateStatusDetails + ";	");
                detailedMessage = updateStatusDetails;
                wasChanged = true;
            }
        }

        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS_AFFECTED_COLUMNS) && input.get(Constants.JSON_FIELD_DATA_IMPORT_STATUS_AFFECTED_COLUMNS)!=null) {
            List<String> updateAffectedColumnsDetails = this.jsonArrayToList(input.getJSONArray(Constants.JSON_FIELD_DATA_IMPORT_STATUS_AFFECTED_COLUMNS));
            if (affectedColumns == null || !updateAffectedColumnsDetails.equals(affectedColumns)) {
                updateDetails.append("'affectedColumns' changed from " + affectedColumns + " to " + updateAffectedColumnsDetails + ";	");
                affectedColumns = updateAffectedColumnsDetails;
                wasChanged = true;
            }
        }

        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS) && input.get(Constants.JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS)!=null) {
            long updatedImports = input.getLong(Constants.JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS);
            if (successfulImports == null || updatedImports != successfulImports) {
                updateDetails.append("'successfulImports' changed from " + successfulImports + " to " + updatedImports + ";	");
                successfulImports = updatedImports;
                wasChanged = true;
            }
        }
        if (wasChanged) {
            lastModified = now;

        }

        return updateDetails.toString();
    }

    private List<String> jsonArrayToList(JSONArray jArray) throws JSONException {
        List<String> listdata = new ArrayList<String>();
        if (jArray != null) {
            for (int i=0;i<jArray.length();i++){
                listdata.add(jArray.getString(i));
            }
        }
        return listdata;
    }

    public List<String> getAffectedColumns() {
        return affectedColumns;
    }

    public void setAffectedColumns(List<String> affectedColumns) {
        this.affectedColumns = affectedColumns;
    }
}
