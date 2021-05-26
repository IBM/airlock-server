package com.ibm.airlock.admin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletContext;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.amazonaws.services.cloudformation.model.AlreadyExistsException;
import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.engine.Environment;

public class BranchesCollection {
	
	private Season season = null;
	
	//map between the branch name and the branch itself 
	private HashMap<String, Branch> branchesMap = new HashMap<String, Branch>();
	
	//list of branch to maintain order in toJson 
	private LinkedList<Branch> branchesList = new LinkedList<Branch>();
		
		
	public HashMap<String, Branch> getBranchesMap() {
		return branchesMap;
	}
	
	public LinkedList<Branch> getBranchesList() {
		return branchesList;
	}

	public void setBranchesMap(HashMap<String, Branch> branchesMap) {
		this.branchesMap = branchesMap;
	}
	
	public BranchesCollection(Season season) {
		this.season = season;
	}
	
	public Branch getBranchByName(String branchName) {
		return branchesMap.get(branchName);
	}
	
	public void removeBranch(String branchName) {
		branchesMap.remove(branchName);
		for (int i=0; i<branchesList.size(); i++) {
			if (branchesList.get(i).getName().equals(branchName))
				branchesList.remove(i);
		}
	}
	
	public void addBranch(Branch branch) {
		if (branchesMap.containsKey(branch.getName())) {
			throw new AlreadyExistsException(branch.getName() + " already exists for version range");
		}
		branchesMap.put(branch.getName(), branch);
		branchesList.add(branch);
	}
	
	public JSONObject toJson(OutputJSONMode outputMode, ServletContext context, Environment env, boolean returnFeatures, boolean addDummyMasterBranch, boolean returnPurchases) throws JSONException{
		JSONObject res = new JSONObject();
		
		JSONArray branchesArr = new JSONArray();
		if (addDummyMasterBranch) {
			branchesArr.add(Branch.getDummyMasterBranchObject(season));
		}
		
		for (Branch branch : branchesList) {
			branchesArr.add(branch.toJson(outputMode, context, env, returnFeatures, true, returnPurchases));
		}
		
		res.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, season.getUniqueId()==null?null:season.getUniqueId().toString());
		res.put(Constants.JSON_FIELD_BRANCHES, branchesArr);

		return res; 
	}
	
	//for each branch return creator, creationDat and the last date a feature in this branch was modified.
	//the returned list is sorted by modification date
	public JSONObject toBranchesUsageJson(ServletContext context) throws JSONException {
		JSONObject res = new JSONObject();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		TreeMap<Date,JSONObject> sortedByModificationDate = new TreeMap<>();
		JSONArray branchesData = new JSONArray();
		for (Branch branch : branchesList) {
			JSONObject branchObj = new JSONObject();
			branchObj.put(Constants.JSON_FIELD_NAME, branch.getName());
			branchObj.put(Constants.JSON_FIELD_UNIQUE_ID, branch.getUniqueId().toString());
			branchObj.put(Constants.JSON_FIELD_NAME, branch.getName());
			branchObj.put(Constants.JSON_FEATURE_FIELD_CREATOR, branch.getCreator());
			branchObj.put(Constants.JSON_FEATURE_FIELD_CREATION_DATE, formatter.format(branch.getCreationDate()));
			
			Date lastBranchModificationDate = branch.getBranchlatestModificationDate();
			branchObj.put(Constants.JSON_FIELD_BRANCH_MODIFICATION_DATE, formatter.format(lastBranchModificationDate));
			
			String exp = branch.isPartOfExperiment(context);
			branchObj.put(Constants.JSON_FIELD_IS_PART_OF_EXPERIMENT, exp!=null);
			if (exp!=null) {
				branchObj.put(Constants.JSON_FIELD_EXPERIMENT, exp);
			}
			sortedByModificationDate.put(lastBranchModificationDate, branchObj);
			
		}
		
		Set<Date> keys = sortedByModificationDate.keySet();
		for (Date key:keys) {
			branchesData.add(sortedByModificationDate.get(key));
		}
		
		res.put(Constants.JSON_FIELD_BRANCHES, branchesData);
		return res;
	}
	
	public int size() {
		return branchesList.size();
	}

	public void fromJSON(JSONArray input, Map<String, Branch> branchesDB, Environment env, ServletContext context) throws JSONException {
		for (int i=0; i<input.length(); i++ ) {
			JSONObject branchJSON = input.getJSONObject(i);
			Branch branch = new Branch(season.getUniqueId());
			branch.fromJSON(branchJSON, /*airlockItemsDB,*/ env, season, context);
			addBranch(branch);
			if (branchesDB!=null)
				branchesDB.put(branch.getUniqueId().toString(), branch);
		}		
		
	}

	public BranchesCollection duplicateForNewSeason (Season newSeason, HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context) throws JSONException {
		BranchesCollection res = new BranchesCollection(newSeason);
		@SuppressWarnings("unchecked")
		Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
		
		for (Branch b:branchesList) {
			Branch dupBranch = b.duplicateForNewSeason(newSeason, oldToDuplicatedFeaturesId, context); 
			res.addBranch(dupBranch);			
			branchesDB.put(dupBranch.getUniqueId().toString(), dupBranch);	
		}
		return res;
	}

	public void replaceBranch(Branch destinationBranchCopy) {
		
		branchesMap.put(destinationBranchCopy.getName(), destinationBranchCopy);
		for (int i=0; i<branchesList.size(); i++) {
			if (branchesList.get(i).getName().equals(destinationBranchCopy.getName())) {
				branchesList.remove(i);
				branchesList.add(i, destinationBranchCopy);
				break;
			}
		}
		
	}
	
}
