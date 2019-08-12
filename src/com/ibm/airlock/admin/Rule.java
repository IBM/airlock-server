package com.ibm.airlock.admin;


import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.engine.VerifyRule;

public class Rule {
	public static final Logger logger = Logger.getLogger(Rule.class.getName());

	private String ruleString = null;
	private boolean force = false;

	public Rule()
	{}
	public Rule(Rule other)
	{
		ruleString = other.ruleString;
		force = other.force;
	}

	public String getRuleString() {
		return ruleString;
	}

	public void setRuleString(String ruleString) {
		this.ruleString = ruleString;
	}

	public boolean getForce() {
		return force;
	}

	public JSONObject toJson(OutputJSONMode mode) throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_RULE_FIELD_RULE_STR, ruleString);
		//	if (mode == OutputJSONMode.ADMIN)
		//		res.put(Constants.JSON_RULE_FIELD_FORCE, force);
		return res;
	}

	public void fromJSON (JSONObject input) throws JSONException {
		if (input.containsKey(Constants.JSON_RULE_FIELD_RULE_STR)) 
			ruleString = (String)input.get(Constants.JSON_RULE_FIELD_RULE_STR); 										

		if (input.containsKey(Constants.JSON_RULE_FIELD_FORCE) && input.get(Constants.JSON_RULE_FIELD_FORCE)!=null) 
			force = (Boolean)input.get(Constants.JSON_RULE_FIELD_FORCE);
	}	

	public static Rule duplicteForNextSeason(Rule src) {
		Rule res = new Rule();
		res.setRuleString(src.getRuleString());
		return res;
	}

	public boolean equals (Rule otherRule) {
		if (ruleString == null && otherRule.getRuleString() == null)
			return true;

		if (ruleString == null || otherRule.getRuleString() == null)
			return false;

		return ruleString.equals(otherRule.getRuleString());
	}

	public ValidationResults validateRule(Stage stage, String minAppVersion, Season season, ServletContext context,
			ValidationCache tester,
			UserInfo userInfo) {

		if (force == true && !validAdmin(userInfo)) {
			return new ValidationResults("Only a user with the Administrator role can create or update a feature or configuration without validation.", Status.BAD_REQUEST); 
		}

		try {
			ValidationCache.Info info = tester.getInfo(context, season, stage, minAppVersion);
			VerifyRule.fullRuleEvaluation(ruleString, info.minimalInvoker, info.maximalInvoker);
		} catch (Exception e) {
			if (force) { //dont return error - log warning
				logger.warning("The 'force' field is on, so the following invalid rule is allowed: " + e.getMessage());
				return null;
			}
			else
				return new ValidationResults(e.getMessage(),  Status.BAD_REQUEST);
		}
		
		return null;
	}
	
	public ValidationResults validateRuleWithAdditionalContext(Stage stage, String minAppVersion, Season season, ServletContext context,
			ValidationCache tester, String extraContext,
			UserInfo userInfo) {

		if (force == true && !validAdmin(userInfo)) {
			return new ValidationResults("Only a user with the Administrator role can create or update a feature or configuration without validation.", Status.BAD_REQUEST); 
		}

		try {
			ValidationCache.Info info = tester.getInfo(context, season, stage, minAppVersion);
			VerifyRule.validateRuleWithAdditionalContext(ruleString, info.minimalContext, info.maximalContext, extraContext, info.javascriptFunctions, info.translations);
		} catch (Exception e) {
			if (force) { //dont return error - log warning
				logger.warning("The 'force' field is on, so the following invalid rule is allowed: " + e.getMessage());
				return null;
			}
			else
				return new ValidationResults(e.getMessage(),  Status.BAD_REQUEST);
		}
		
		return null;
	}
	static boolean validAdmin(UserInfo userInfo)
	{

		return userInfo == null || userInfo.getRoles().contains(Constants.RoleType.Administrator);
	}
}
