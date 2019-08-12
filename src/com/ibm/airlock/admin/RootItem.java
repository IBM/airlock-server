package com.ibm.airlock.admin;

import java.util.HashMap;
import java.util.UUID;

import javax.servlet.ServletContext;

import java.util.LinkedList;
import java.util.Map;

public class RootItem extends BaseAirlockItem {

	public RootItem() {
		type = Type.ROOT;
		featuresItems = new LinkedList<BaseAirlockItem>();
		entitlementItems = new LinkedList<BaseAirlockItem>();
	}

	@Override
	protected BaseAirlockItem newInstance() // derived classes will override
	{
		return new RootItem();
	}

	@Override
	public BaseAirlockItem duplicate(String minVersion, UUID newSeasonId, UUID parentId, Map<String, BaseAirlockItem> airlockItemsDB, 
			HashMap<String, String> oldToDuplicatedFeaturesId, ServletContext context, boolean duplicateSubFeatures, boolean createNewId,
			ValidationCache tester) {
		RootItem res = new RootItem();
		
		res.setLastModified(lastModified);
		
		if (createNewId) {
			res.setUniqueId(UUID.randomUUID());
		} else {
			res.setUniqueId(uniqueId);			
		}
		oldToDuplicatedFeaturesId.put(uniqueId.toString(), res.getUniqueId().toString());			
		
		if (newSeasonId!=null) {
			res.setSeasonId(newSeasonId);
		} 
		else {
			res.setSeasonId(seasonId);
		}
		
		res.setParent(parentId);

		if (duplicateSubFeatures) {
			for (int i=0;i<featuresItems.size(); i++) {
				BaseAirlockItem newAirlockItem = featuresItems.get(i).duplicate(minVersion, newSeasonId, res.getUniqueId(), airlockItemsDB, oldToDuplicatedFeaturesId, context, duplicateSubFeatures, createNewId, tester);
				res.getFeaturesItems().add(newAirlockItem);					
				if (airlockItemsDB!=null)
					airlockItemsDB.put(newAirlockItem.getUniqueId().toString(), newAirlockItem);						
			}
			
			for (int i=0;i<entitlementItems.size(); i++) {
				BaseAirlockItem newAirlockItem = entitlementItems.get(i).duplicate(minVersion, newSeasonId, res.getUniqueId(), airlockItemsDB, oldToDuplicatedFeaturesId, context, duplicateSubFeatures, createNewId, tester);
				res.getEntitlementItems().add(newAirlockItem);					
				if (airlockItemsDB!=null)
					airlockItemsDB.put(newAirlockItem.getUniqueId().toString(), newAirlockItem);						
			}
		}

		if (airlockItemsDB!=null)
			airlockItemsDB.put(res.getUniqueId().toString(), res);	
		
		return res;
	}
	
}
