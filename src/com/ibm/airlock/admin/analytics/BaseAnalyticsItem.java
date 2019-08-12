package com.ibm.airlock.admin.analytics;

import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.Rule;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;

public abstract class BaseAnalyticsItem {
	public static final Logger logger = Logger.getLogger(BaseAnalyticsItem.class.getName());
	
	protected Rule rule; //required in update
	protected Double rolloutPercentage = null; //required in update
	protected Date lastModified = null; // required in update. forbidden in create
		
	public BaseAnalyticsItem() {		
		rule = new Rule();
		rule.setRuleString("true");
		rolloutPercentage = 100.0;
		lastModified = new Date();
	}

	public Rule getRule() {
		return rule;
	}
	
	public void setRule(Rule rule) {
		this.rule = rule;
	}
	
	public Double getRolloutPercentage() {
		return rolloutPercentage;
	}
	
	public void setRolloutPercentage(Double rolloutPercentage) {
		this.rolloutPercentage = rolloutPercentage;
	}
	
	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}		
	
	public void fromJSON(JSONObject input) throws JSONException {
		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
			long timeInMS = input.getLong(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_RULE) && input.get(Constants.JSON_FEATURE_FIELD_RULE)!=null) {
			rule = new Rule();
			rule.fromJSON(input.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE));
		}
		
		if (input.containsKey(Constants.JSON_FEATURE_FIELD_PERCENTAGE) && input.get(Constants.JSON_FEATURE_FIELD_PERCENTAGE)!=null) {
			rolloutPercentage = input.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE);
		}				
									
	}	
	
	public JSONObject toJson(OutputJSONMode mode) throws JSONException {
		JSONObject res = new JSONObject();
		
		res.put(Constants.JSON_FEATURE_FIELD_RULE, rule.toJson(OutputJSONMode.ADMIN));
		res.put(Constants.JSON_FEATURE_FIELD_PERCENTAGE, rolloutPercentage);
		
		if (mode == OutputJSONMode.ADMIN || mode == OutputJSONMode.DISPLAY) { 		
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
		}
		
		return res;
	}
	
	public ValidationResults validateBaseAnalyticsItemJSON(JSONObject analyticsItemObj, ServletContext context, UserInfo userInfo, Action action) {

		try {
			//rule 
			if (!analyticsItemObj.containsKey(Constants.JSON_FEATURE_FIELD_RULE) || analyticsItemObj.getString(Constants.JSON_FEATURE_FIELD_RULE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_RULE), Status.BAD_REQUEST);
			}

			analyticsItemObj.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE); //validate legal json
			Rule tmpRule = new Rule();
			tmpRule.fromJSON(analyticsItemObj.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE));

			//empty,null or missing ruleString is legal
			
			//rolloutPercentage
			if (!analyticsItemObj.containsKey(Constants.JSON_FEATURE_FIELD_PERCENTAGE) && analyticsItemObj.get(Constants.JSON_FEATURE_FIELD_PERCENTAGE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_PERCENTAGE), Status.BAD_REQUEST);					
			}
			double p = analyticsItemObj.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE); //validate that is Double value
			ValidationResults vr = Utilities.validatePercentage(p);
			if (vr!=null)
				return vr;			
			
			if (action == Action.ADD) {
				//modification date => should not appear in feature creation
				if (analyticsItemObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && analyticsItemObj.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
					return new ValidationResults("The lastModified field cannot be specified during experiment creation.", Status.BAD_REQUEST);
				}								
			}
			else {
				//modification date must appear in update
				if (!analyticsItemObj.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || analyticsItemObj.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
					return new ValidationResults(Strings.lastModifiedIsMissing, Status.BAD_REQUEST);
				}				
	
				//verify that given modification date is not older that current modification date
				long givenModoficationDate = analyticsItemObj.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
				Date givenDate = new Date(givenModoficationDate);
				if (givenDate.before(lastModified)) {
					return new ValidationResults(String.format(Strings.itemChangedByAnotherUser, "item"), Status.CONFLICT);			
				}
			}

		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}
	
	public String updateAnalyticsItem (JSONObject updatedAnalyticsItemJSON, ServletContext context) throws JSONException {
		
		//seasonId should not be updated
		boolean wasChanged = false;
		StringBuilder updateDetails = new StringBuilder();

		//Rule
		JSONObject updatedRuleJSON = updatedAnalyticsItemJSON.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE);
		Rule updatedRule = new Rule();
		updatedRule.fromJSON(updatedRuleJSON);
		if (!rule.equals(updatedRule)) {
			updateDetails.append(" 'rule' changed from \n" + rule.toJson(OutputJSONMode.ADMIN).toString() + "\nto\n" + updatedRule.toJson(OutputJSONMode.ADMIN).toString() + "\n");
			rule = updatedRule;
			wasChanged = true;			
		}

		//rolloutPercentage
		double updatedRolloutPercentage = updatedAnalyticsItemJSON.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE);
		if (rolloutPercentage  != updatedRolloutPercentage) {
			updateDetails.append(" 'rolloutPercentage' changed from " + rolloutPercentage + " to " + updatedRolloutPercentage + "\n");
			rolloutPercentage = updatedRolloutPercentage;
				
			wasChanged = true;
		}
			
		if (wasChanged) {
			lastModified = new Date();
		}
		
		return updateDetails.toString();
	}
	
}
