package com.ibm.airlock.admin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.BranchStatus;
import com.ibm.airlock.Constants.REQUEST_ITEM_TYPE;

public class MergeBranch
{
	@SuppressWarnings("serial")
	public static class MergeException extends Exception
	{
		public MergeException(String error) {
			super(error);			
		}
	}

	public static Map<String, BaseAirlockItem> getItemMap(BaseAirlockItem in, boolean useId)
	{
		HashMap<String, BaseAirlockItem> out = new HashMap<String, BaseAirlockItem>();
		mapItem(in, out, useId);
		return out;
	}
	
	public static Map<String, BaseAirlockItem> addToItemMap(BaseAirlockItem in, boolean useId, Map<String, BaseAirlockItem> map)
	{
		mapItem(in, map, useId);
		return map;
	}
	
	static void mapItem(BaseAirlockItem in, Map<String, BaseAirlockItem> out, boolean useId)
	{
		String key = useId ? getId(in) : getName(in);
		out.put(key, in);

		List<BaseAirlockItem> items = in.getFeaturesItems();
		if (items != null)
			for (BaseAirlockItem item : items)
			{
				mapItem(item, out, useId);
			}

		items = in.getConfigurationRuleItems();
		if (items != null)
			for (BaseAirlockItem item : items)
			{
				mapItem(item, out, useId);
			}
		
		items = in.getOrderingRuleItems();
		if (items != null)
			for (BaseAirlockItem item : items)
			{
				mapItem(item, out, useId);
			}
		
		items = in.getEntitlementItems();
		if (items != null)
			for (BaseAirlockItem item : items)
			{
				mapItem(item, out, useId);
			}
		
		items = in.getPurchaseOptionsItems();
		if (items != null)
			for (BaseAirlockItem item : items)
			{
				mapItem(item, out, useId);
			}
	}

	public static String getName(BaseAirlockItem bai)
	{
		switch (bai.getType())
		{
		case ROOT :
			return Constants.ROOT_FEATURE;
		
		case MUTUAL_EXCLUSION_GROUP:
		case CONFIG_MUTUAL_EXCLUSION_GROUP:
		case ORDERING_RULE_MUTUAL_EXCLUSION_GROUP:
		case ENTITLEMENT_MUTUAL_EXCLUSION_GROUP:
		case PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP:
			return "mx." + getId(bai);

		default:
			DataAirlockItem dai = (DataAirlockItem) bai;
			return dai.getNamespace() + "." + dai.getName();
		}
	}
	public static String getId(BaseAirlockItem bai)
	{
		return bai.getUniqueId().toString();
	}

	public static BaseAirlockItem merge(BaseAirlockItem master, Branch branch, Constants.REQUEST_ITEM_TYPE itemsType) throws MergeException
	{
		try {
			BaseAirlockItem out = BaseAirlockItem.getClone(master);

			// map each name to a node
			Map<String,BaseAirlockItem> nameMap = getItemMap(out, false);
	
			// mark old nodes for deletion
			LinkedList<BaseAirlockItem> branchItems = itemsType==REQUEST_ITEM_TYPE.FEATURES ? branch.getBranchFeatures() : branch.getBranchPurchases();
			for (BaseAirlockItem override : branchItems)
			{
				markForDeletion(override, nameMap, BaseAirlockItem.Type.FEATURE);
				markForDeletion(override, nameMap, BaseAirlockItem.Type.CONFIGURATION_RULE);
				markForDeletion(override, nameMap, BaseAirlockItem.Type.ORDERING_RULE);
				markForDeletion(override, nameMap, BaseAirlockItem.Type.ENTITLEMENT);
				markForDeletion(override, nameMap, BaseAirlockItem.Type.PURCHASE_OPTIONS);
			}

			// replace and add items
			if (itemsType.equals(Constants.REQUEST_ITEM_TYPE.FEATURES)) {
				for (BaseAirlockItem override : branch.getBranchFeatures())
				{
					override = BaseAirlockItem.getClone(override);
					if (isRoot(override))
					{
						mergeRootNames(override, out, itemsType);
						reconcileChildren(override, nameMap, BaseAirlockItem.Type.FEATURE);
						//mapItem(override, nameMap, BaseAirlockItem.Type.CONFIGURATION_RULE);
						nameMap.put(getName(override), override);
						out = override;
					}
					else
						overrideItem(out, override, nameMap);
				}
			}
			
			if (itemsType.equals(Constants.REQUEST_ITEM_TYPE.ENTITLEMENTS)) {
				for (BaseAirlockItem override : branch.getBranchPurchases())
				{
					override = BaseAirlockItem.getClone(override);
					if (isRoot(override))
					{
						mergeRootNames(override, out, itemsType);
						reconcileChildren(override, nameMap, BaseAirlockItem.Type.ENTITLEMENT);
						reconcileChildren(override, nameMap, BaseAirlockItem.Type.PURCHASE_OPTIONS);
						nameMap.put(getName(override), override);
						out = override;
					}
					else
						overrideItem(out, override, nameMap);
				}
			}
			// remove any remaining references to old features that have been overridden or moved.
			// needed for cases such as ROOT--> A--> B changed to ROOT--> B--> A
			removeResiduals(out, null, BaseAirlockItem.Type.FEATURE);
			removeResiduals(out, null, BaseAirlockItem.Type.CONFIGURATION_RULE);
			removeResiduals(out, null, BaseAirlockItem.Type.ORDERING_RULE);
			removeResiduals(out, null, BaseAirlockItem.Type.ENTITLEMENT);
			removeResiduals(out, null, BaseAirlockItem.Type.PURCHASE_OPTIONS);
			return out;
		}
		catch (Exception e)
		{
			throw new MergeException("Merge error: " + e.getMessage());
		}
	}

	static boolean isRoot(BaseAirlockItem item)
	{
		return item.branchFeatureParentName == null || item.branchFeatureParentName.isEmpty();
	}
	static void mergeRootNames(BaseAirlockItem override, BaseAirlockItem root, Constants.REQUEST_ITEM_TYPE itemsType)
	{
		// if the override of the root is checked out, the child names are taken as is
		// i.e. the children are frozen; new additions to the master root will not appear in the result.
		if (override.branchStatus == BranchStatus.CHECKED_OUT)
			return;

		// else, the override child names are extended by merging them with the root's current child names.
		// i.e. children added to the master root after the branch was created will appear in the result

		// get child names from master
		LinkedList<String> newNames = new LinkedList<String>();
		TreeSet<String> nameSet = new TreeSet<String>();
			
		if (itemsType.equals(REQUEST_ITEM_TYPE.FEATURES)) {
			if (root.featuresItems != null)
				for (BaseAirlockItem child : root.featuresItems)
				{
					if (child.getBranchStatus().equals(BranchStatus.TEMPORARY))
						continue; // marked for deletion
					String childName = getName(child);
					newNames.add(childName);
					nameSet.add(childName);
				}
	
			// get child names from override, append the additional ones to the new name list
			for (String overrideName : override.branchFeaturesItems)
			{
				if (!nameSet.contains(overrideName))
					newNames.add(overrideName);
			}
			// replace name list in override
			override.branchFeaturesItems = newNames;
		}
		else {
			//PURCHASES
			if (root.entitlementItems != null)
				for (BaseAirlockItem child : root.entitlementItems)
				{
					if (child.getBranchStatus().equals(BranchStatus.TEMPORARY))
						continue; // marked for deletion
					String childName = getName(child);
					newNames.add(childName);
					nameSet.add(childName);
				}
	
			// get child names from override, append the additional ones to the new name list
			for (String overrideName : override.branchEntitlementItems)
			{
				if (!nameSet.contains(overrideName))
					newNames.add(overrideName);
			}
			// replace name list in override
			override.branchEntitlementItems = newNames;
		}
	}
	
	static void overrideItem(BaseAirlockItem out, BaseAirlockItem override, Map<String,BaseAirlockItem> nameMap) throws Exception
	{
		BaseAirlockItem parent = nameMap.get(override.branchFeatureParentName);
		if (parent == null)
			throw new Exception ("parent does not exist: " + override.branchFeatureParentName);

		String overrideName = getName(override);
		BaseAirlockItem original = nameMap.get(overrideName);

		if (override.branchStatus != BranchStatus.NEW)
		{
			if (original == null)
				throw new Exception ("override item not found in master: " + overrideName);

			original.branchStatus = BranchStatus.TEMPORARY; // just in case it was NONE rather than CHECKED_OUT
		}

		BaseAirlockItem.Type type = baseType(override);
		LinkedList<BaseAirlockItem> source = parent.getItemList(type);
		if (source == null)
			source = new LinkedList<BaseAirlockItem>(); // // is it OK to start with empty parent?

		// find feature in parent and replace it, or add it as new
		LinkedList<BaseAirlockItem> newChildren = new LinkedList<BaseAirlockItem>();
		boolean found = false;

		for (BaseAirlockItem child : source)
		{
			if (getName(child).equals(overrideName))
			{
				newChildren.add(override);
				found = true;
			}
			else
			{
				newChildren.add(child);
			}
		}
		if (!found)
			newChildren.add(override);

		// point parent to the replacement
		parent.setItemList(type, newChildren);

		// resolve children of the override
		reconcileChildren(override, nameMap, BaseAirlockItem.Type.FEATURE);
		reconcileChildren(override, nameMap, BaseAirlockItem.Type.CONFIGURATION_RULE);
		reconcileChildren(override, nameMap, BaseAirlockItem.Type.ORDERING_RULE);
		reconcileChildren(override, nameMap, BaseAirlockItem.Type.ENTITLEMENT);
		reconcileChildren(override, nameMap, BaseAirlockItem.Type.PURCHASE_OPTIONS);
		

		// update the name map
		// mapItem(override, nameMap, false);
		nameMap.put(overrideName, override);
	}

	static BaseAirlockItem.Type baseType(BaseAirlockItem item)
	{
		switch (item.getType())
		{
		case CONFIGURATION_RULE:
		case CONFIG_MUTUAL_EXCLUSION_GROUP:
			return BaseAirlockItem.Type.CONFIGURATION_RULE;

		case ORDERING_RULE:
		case ORDERING_RULE_MUTUAL_EXCLUSION_GROUP:
			return BaseAirlockItem.Type.ORDERING_RULE;
			
		case ENTITLEMENT:
		case ENTITLEMENT_MUTUAL_EXCLUSION_GROUP:
			return BaseAirlockItem.Type.ENTITLEMENT;

		case PURCHASE_OPTIONS:
		case PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP:
			return BaseAirlockItem.Type.PURCHASE_OPTIONS;
			
		default:
			return BaseAirlockItem.Type.FEATURE;
		}
	}

	static void reconcileChildren(BaseAirlockItem override, Map<String,BaseAirlockItem> nameMap, BaseAirlockItem.Type type) throws Exception
	{
		LinkedList<String> names = override.getBranchList(type);
		LinkedList<BaseAirlockItem> source = override.getItemList(type);
		if (names == null || names.isEmpty())
			return;

		Map<String,BaseAirlockItem> overrideKids = new HashMap<String,BaseAirlockItem>();
		if (source != null)
			for (BaseAirlockItem child : source)
				overrideKids.put(getName(child), child);

		LinkedList<BaseAirlockItem> newKids = new LinkedList<BaseAirlockItem>();
		for (String childName : names)
		{
			// look for the child in the override first, then look in the master
			BaseAirlockItem child = overrideKids.get(childName);
			if (child == null)
			{
				child = nameMap.get(childName);
				if (child == null)
					throw new Exception("missing node name " + childName);

				// we can't just put the child as-is - it may have moved from a parent that isn't overridden and still points to it.
				// that old link needs to be identified in removeResiduals. so we clone the child, put the duplicate in the
				// override, and mark the original for deletion. the clone is shallow since we only need to override the status.

				BaseAirlockItem newChild = BaseAirlockItem.getShallowClone(child);
				child.branchStatus = BranchStatus.TEMPORARY;
				child = newChild;
			}
			newKids.add(child);
		}

		override.setItemList(type, newKids);

		// recurse on the override's children
		for (BaseAirlockItem child : overrideKids.values())
		{
			reconcileChildren(child, nameMap, BaseAirlockItem.Type.FEATURE);
			reconcileChildren(child, nameMap, BaseAirlockItem.Type.CONFIGURATION_RULE);
			reconcileChildren(child, nameMap, BaseAirlockItem.Type.ORDERING_RULE);
			reconcileChildren(child, nameMap, BaseAirlockItem.Type.ENTITLEMENT);
			reconcileChildren(child, nameMap, BaseAirlockItem.Type.PURCHASE_OPTIONS);
		}
		for (int i = 0; i < newKids.size(); ++i)
		{
			BaseAirlockItem child = newKids.get(i);
			nameMap.put(getName(child), child);
		}
	}

	// find old nodes that are being replaced and mark them for deletion
	static void  markForDeletion(BaseAirlockItem override, Map<String,BaseAirlockItem> nameMap, BaseAirlockItem.Type type) throws Exception
	{
		if (override.branchStatus == BranchStatus.CHECKED_OUT)
		{
			String key = getName(override);
			BaseAirlockItem original = nameMap.get(key);
			if (original != null) // throw exception?
				original.branchStatus = BranchStatus.TEMPORARY;
		}

		LinkedList<BaseAirlockItem> source = override.getItemList(type);
		if (source != null)
			for (BaseAirlockItem feature : source)
			{
				markForDeletion(feature, nameMap, BaseAirlockItem.Type.FEATURE);
				markForDeletion(feature, nameMap, BaseAirlockItem.Type.CONFIGURATION_RULE);
				markForDeletion(feature, nameMap, BaseAirlockItem.Type.ORDERING_RULE);
				markForDeletion(feature, nameMap, BaseAirlockItem.Type.ENTITLEMENT);
				markForDeletion(feature, nameMap, BaseAirlockItem.Type.PURCHASE_OPTIONS);
			}
	}
	// remove all nodes marked for deletion that are accessible from the root.
	// also update the parentage
	static void removeResiduals(BaseAirlockItem item, UUID parentId, BaseAirlockItem.Type type) throws Exception
	{
		item.setParent(parentId);
		LinkedList<BaseAirlockItem> source = item.getItemList(type);
		if (source == null || source.isEmpty())
			return;

		UUID id = item.getUniqueId();
		LinkedList<BaseAirlockItem> newChildren = new LinkedList<BaseAirlockItem>();
		for (BaseAirlockItem child : source)
		{
			if (child.branchStatus != BranchStatus.TEMPORARY)
			{
				newChildren.add(child);
				removeResiduals(child, id, BaseAirlockItem.Type.FEATURE);
				removeResiduals(child, id, BaseAirlockItem.Type.CONFIGURATION_RULE);
				removeResiduals(child, id, BaseAirlockItem.Type.ORDERING_RULE);
				removeResiduals(child, id, BaseAirlockItem.Type.ENTITLEMENT);
				removeResiduals(child, id, BaseAirlockItem.Type.PURCHASE_OPTIONS);
			}
		}

		if (source.size() != newChildren.size())
			item.setItemList(type, newChildren);
	}

	static void validateBranch(Branch branch) throws Exception
	{
		for (BaseAirlockItem override : branch.getBranchFeatures())
		{
			validateOverride(override, BaseAirlockItem.Type.FEATURE, true);
			validateOverride(override, BaseAirlockItem.Type.CONFIGURATION_RULE, true);
			validateOverride(override, BaseAirlockItem.Type.ORDERING_RULE, true);
		}
		
		for (BaseAirlockItem override : branch.getBranchPurchases())
		{
			validateOverride(override, BaseAirlockItem.Type.ENTITLEMENT, true);
			validateOverride(override, BaseAirlockItem.Type.PURCHASE_OPTIONS, true);
			validateOverride(override, BaseAirlockItem.Type.CONFIGURATION_RULE, true);
		}
	}
	
	static void validateOverride(BaseAirlockItem override, BaseAirlockItem.Type type, boolean topNode) throws Exception
	{
		LinkedList<BaseAirlockItem> source = override.getItemList(type);
		if (source == null)
			return;

		// not checking top node - it can contain new items that are not in the name list
		boolean skipTest = topNode && (override.branchStatus != BranchStatus.CHECKED_OUT);
		if (!skipTest)
		{
			LinkedList<String> names = override.getBranchList(type);
			TreeSet<String> nameSet = (names == null) ? new TreeSet<String>() : new TreeSet<String>(names);

			for (BaseAirlockItem child : source)
			{
				String childName = getName(child);
				if (!nameSet.contains(childName))
					throw new Exception ("BaseAirlockItem '" + childName + "' is checked out or new, but not found in name list");
			}
		}

		for (BaseAirlockItem item : source)
		{
			validateOverride(item, BaseAirlockItem.Type.FEATURE, false);
			validateOverride(item, BaseAirlockItem.Type.CONFIGURATION_RULE, false);
			validateOverride(item, BaseAirlockItem.Type.ORDERING_RULE, false);
			validateOverride(item, BaseAirlockItem.Type.ENTITLEMENT, false);
			validateOverride(item, BaseAirlockItem.Type.PURCHASE_OPTIONS, false);
		}
	}
}
