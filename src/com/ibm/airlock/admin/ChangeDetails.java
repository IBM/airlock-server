package com.ibm.airlock.admin;

import com.ibm.airlock.Constants;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

/**
 * Created by amitaim on 08/03/2017.
 */
public class ChangeDetails {
    protected StringBuilder updateDetails = new StringBuilder("");
    protected BaseAirlockItem item = null;
    protected Boolean isProductionChange = null;
    protected ArrayList<DataAirlockItem> m_subfeatures = null;

    public ChangeDetails(String _details,BaseAirlockItem _item){
        updateDetails.append(_details);
        item = _item;
    }

    public ChangeDetails(String _details,BaseAirlockItem _item,Boolean isProduction ){
        updateDetails.append(_details);
        item = _item;
        isProductionChange = isProduction;
    }

    public BaseAirlockItem getItem(){
        return item;
    }

    public String getUpdateDetails(){
        return updateDetails.toString();
    }

    public void addUpdateDetails(String details){
        updateDetails.append(details);
    }


    public void setSubfeatures(ArrayList<DataAirlockItem> subfeatures){
        m_subfeatures = subfeatures;
    }
    public ArrayList<String> getFollowers(ServletContext context,Map<String, BaseAirlockItem> airlockItemsDB){
        Map<String, ArrayList<String>> followersFeaturesDB = (Map<String, ArrayList<String>>)context.getAttribute(Constants.FOLLOWERS_FEATURES_DB_PARAM_NAME);
        ArrayList<String> allFollowers = new ArrayList<>();
        if(m_subfeatures != null){
            for (int i = 0; i < m_subfeatures.size();++i){
                FeatureItem subFeatureItem = getFirstParentFeature(m_subfeatures.get(i),airlockItemsDB);
                ArrayList<String> subFeatureFollowers  =followersFeaturesDB.get(subFeatureItem.getUniqueId().toString());
                if(subFeatureFollowers != null) {
                    allFollowers.addAll(subFeatureFollowers);
                }
            }
        }
        FeatureItem featureItem = getFirstParentFeature(airlockItemsDB);
        if(featureItem == null){
            return allFollowers;
        }
        ArrayList<String> followers = followersFeaturesDB.get(featureItem.getUniqueId().toString());
        if(allFollowers.size() == 0) {
            return followers;
        }
        else {
            if(followers!= null) {
                allFollowers.addAll(followers);
            }
            return allFollowers;
        }
    }

    public Boolean isProduction(){
        return isProductionChange;
    }

    public UUID getUniqueId(){
        return item.getUniqueId();
    }

    public FeatureItem getFirstParentFeature(Map<String, BaseAirlockItem> airlockItemsDB){
        return getFirstParentFeature(item,airlockItemsDB);
    }
    public FeatureItem getFirstParentFeature(BaseAirlockItem baseItem,Map<String, BaseAirlockItem> airlockItemsDB){
         if(baseItem instanceof FeatureItem){
            return ((FeatureItem) baseItem);
        }
        try {
            BaseAirlockItem parent = airlockItemsDB.get(baseItem.getParent().toString());
            //while (parent.getType() != BaseAirlockItem.Type.FEATURE) {
            while (!(parent instanceof FeatureItem)) {
                parent = airlockItemsDB.get(parent.getParent().toString());
            }
            return ((FeatureItem) parent);
        }catch (Exception e){
            return null;
        }
    }

    public String changeToString(){
        String name = "";
        if(item instanceof DataAirlockItem  ){
            name = ", " + item.getNameSpaceDotName();
        }
        String header = "   In " + item.getObjTypeStrByType() +" "+  getUniqueId()+ name +" : \n";
        return header + updateDetails.toString();
    }
}
