package com.ibm.airlock.admin.analytics;

import java.util.Date;

import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.ValidationResults;

public class ExperimentRange {
	Date start = null;
	Date end = null;

	public ExperimentRange() {
	}

	public ExperimentRange(Date start, Date end) {
		this.start = start;
		this.end = end;
	}
	public void fromJSON(JSONObject input) throws JSONException {
		if (input.containsKey(Constants.JSON_FIELD_START) && input.get(Constants.JSON_FIELD_START)!=null) {
			long timeInMS = input.getLong(Constants.JSON_FIELD_START);
			start = new Date(timeInMS);
		}

		if (input.containsKey(Constants.JSON_FIELD_END) && input.get(Constants.JSON_FIELD_END)!=null) {
			long timeInMS = input.getLong(Constants.JSON_FIELD_END);
			end = new Date(timeInMS);
		}
		else {
			end = null;
		}
	}

	public boolean equals(ExperimentRange other) {
		Date otherStart = other.start;
		Date otherEnd = other.end;
		if (!start.equals(otherStart))
			return false;

		if (otherEnd == null && end == null)
			return true;

		if (otherEnd == null || end == null)
			return false;

		if (!end.equals(otherEnd))
			return false;

		return true;
	}

	public static ValidationResults validateRangeJson(JSONObject rangeJson) {
		try {
			//start - mandatory
			if (!rangeJson.containsKey(Constants.JSON_FIELD_START) || rangeJson.getString(Constants.JSON_FIELD_START) == null ) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_START), Status.BAD_REQUEST);
			}

			long start = rangeJson.getLong(Constants.JSON_FIELD_START); //validate legal long
			new Date(start); //validate legal date
			Long end = null;

			if (rangeJson.containsKey(Constants.JSON_FIELD_END) && rangeJson.get(Constants.JSON_FIELD_END) != null) {
				end = rangeJson.getLong(Constants.JSON_FIELD_END);
				new Date(end); //validate legal date
			}

			if (end != null && end<=start) {
				return new ValidationResults("The start field should be smaller than the end field.", Status.BAD_REQUEST);
			}
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		return null;
	}

	public JSONObject toJson() throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_START, start.getTime());
		res.put(Constants.JSON_FIELD_END, end == null?null:end.getTime());
		return res;
	}

}
