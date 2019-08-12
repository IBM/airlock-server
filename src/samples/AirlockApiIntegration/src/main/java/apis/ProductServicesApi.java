package apis;

import com.google.gson.*;
import netscape.javascript.JSObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProductServicesApi {
    String url = null;

    public ProductServicesApi(String url) {
        this.url = url + "/admin/products";
    }

    public String createProduct(String productName, String token) throws IOException {
        JsonObject content = new JsonObject();
        content.addProperty("name", productName);
        content.addProperty("codeIdentifier", productName);

        Utilities.RestCallResults res = Utilities.sendPost(url, content.toString(), token);
        if (res.code != 200) {
            System.err.println("createProduct fail: " + res.code + ", " + res.message);
            return null;
        }

        return Utilities.getUniquId(res.message);
    }

    public String createSeason(String minVersion, String productId, String token) throws IOException {
        JsonObject content = new JsonObject();
        content.addProperty("minVersion", minVersion);

        Utilities.RestCallResults res = Utilities.sendPost(url + "/" + productId + "/seasons/ ", content.toString(), token);
        if (res.code != 200) {
            System.err.println("createSeason fail: " + res.code + ", " + res.message);
            return null;
        }

        return Utilities.getUniquId(res.message);
    }

    public String getEncryptionKey(String seasonId, String token) throws IOException {
        Utilities.RestCallResults res = Utilities.sendGet(url + "/seasons/" + seasonId + "/encryptionkey", token);
        if (res.code != 200) {
            System.err.println("getEncryptionKry fail: " + res.code + ", " + res.message);
            return null;
        }

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(res.message).getAsJsonObject();

        return jsonObject.get("encryptionKey").getAsString();
    }

    public String writeDefaultsFile(String seasonId, String token) throws IOException {
        Utilities.RestCallResults res = Utilities.sendGet(url + "/seasons/" + seasonId + "/defaults", token);
        if (res.code != 200) {
            System.err.println("getDefaultsFile fail: " + res.code + ", " + res.message);
            return null;
        }

        String defaultsFileContent = res.message;


        //transferring to json to correct json indentation
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(defaultsFileContent).getAsJsonObject();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonStr = gson.toJson(jsonObject);

        File file = new File("out/AirlockDefaults.json");
        FileWriter fw = new FileWriter(file);
        fw.write(jsonStr);
        fw.close();

        return file.getAbsolutePath();
    }

    private String writeConstantsFile(String seasonId, String token, String platform, String fileName) throws IOException {
        Utilities.RestCallResults res = Utilities.sendGet(url + "/seasons/" + seasonId + "/constants?platform=" + platform, token);
        if (res.code != 200) {
            System.err.println("writeConstantFiles fail: " + res.code + ", " + res.message);
            return null;
        }

        String iOSconstantsFileContent = res.message;

        File file = new File(fileName);
        FileWriter fw = new FileWriter(file);
        fw.write(iOSconstantsFileContent);
        fw.close();

        return file.getAbsolutePath();
    }

    public String writeConstantFiles(String seasonId, String token) throws IOException {
        StringBuilder sb = new StringBuilder();
        String constantsFilesPath = writeConstantsFile(seasonId, token, "iOS", "out/AirlockConstants.swift");
        if (constantsFilesPath == null) {
            return null;
        }
        sb.append("\nConstants file for iOS: " + constantsFilesPath+"\n");

        constantsFilesPath = writeConstantsFile(seasonId, token, "Android", "out/AirlockConstants.java");
        if (constantsFilesPath == null) {
            return null;
        }
        sb.append("Constants file for Android: " + constantsFilesPath+"\n");

        constantsFilesPath = writeConstantsFile(seasonId, token, "c_sharp", "out/AirlockConstants.cs");
        if (constantsFilesPath == null) {
            return null;
        }
        sb.append("Constants file for c_sharp: " + constantsFilesPath+"\n");

        return sb.toString();
    }

    public String createFeature(String seasonId, String parent, String token,
                             String name, String namespace, String stage, String creator,
                             JsonArray internalUserGroups , boolean enabled,
                             String description, String rule, String minAppVersion, String owner,
                             int rolloutPercentage, boolean defaultIfAirlockSystemIsDown,
                             JsonObject configurationSchema, JsonObject defaultConfiguration
                    ) throws IOException {
        JsonObject content = new JsonObject();
        content.addProperty("type", "FEATURE");
        content.addProperty("name", name);
        content.addProperty("namespace", namespace);
        content.addProperty("stage", stage);
        content.addProperty("creator", creator);
        content.add("internalUserGroups", internalUserGroups);
        content.addProperty("enabled", enabled);
        content.addProperty("description", description);
        content.addProperty("minAppVersion", minAppVersion);
        content.addProperty("owner", owner);
        content.addProperty("rolloutPercentage", rolloutPercentage);
        content.addProperty("defaultIfAirlockSystemIsDown", defaultIfAirlockSystemIsDown);
        content.add("configurationSchema", configurationSchema);
        content.addProperty("defaultConfiguration", defaultConfiguration.toString());


        JsonObject ruleJson = new JsonObject();
        ruleJson.addProperty("ruleString", rule);
        content.add("rule", ruleJson);


        Utilities.RestCallResults res = Utilities.sendPost(url + "/seasons/" + seasonId + "/branches/MASTER/features?parent="+parent, content.toString(), token);
        if (res.code != 200) {
            System.err.println("createFeature fail: " + res.code + ", " + res.message);
            return null;
        }

        return Utilities.getUniquId(res.message);
    }

    public String createConfigurationRule(String seasonId, String parent, String token,
                                String name, String namespace, String stage, String creator,
                                JsonArray internalUserGroups , boolean enabled,
                                String description, String rule, String minAppVersion, String owner,
                                int rolloutPercentage, boolean defaultIfAirlockSystemIsDown,
                                String configuration) throws IOException {

        JsonObject content = new JsonObject();
        content.addProperty("type", "CONFIGURATION_RULE");
        content.addProperty("name", name);
        content.addProperty("namespace", namespace);
        content.addProperty("stage", stage);
        content.addProperty("creator", creator);
        content.add("internalUserGroups", internalUserGroups);
        content.addProperty("enabled", enabled);
        content.addProperty("description", description);
        content.addProperty("minAppVersion", minAppVersion);
        content.addProperty("owner", owner);
        content.addProperty("rolloutPercentage", rolloutPercentage);
        content.addProperty("defaultIfAirlockSystemIsDown", defaultIfAirlockSystemIsDown);
        content.addProperty("configuration", configuration);

        JsonObject ruleJson = new JsonObject();
        ruleJson.addProperty("ruleString", rule);
        content.add("rule", ruleJson);


        Utilities.RestCallResults res = Utilities.sendPost(url + "/seasons/" + seasonId + "/branches/MASTER/features?parent="+parent, content.toString(), token);
        if (res.code != 200) {
            System.err.println("createConfigurationRule fail: " + res.code + ", " + res.message);
            return null;
        }

        return Utilities.getUniquId(res.message);
    }

    public int deleteProduct(String productId, String token) throws IOException {
        int responseCode = -1;

        Utilities.RestCallResults res = Utilities.sendDelete(url + "/" + productId, token);
        responseCode = res.code;

        if (res.code != 200) {
            System.err.println("deleteProduct fail: " + res.code + ", " + res.message);
        }

        return responseCode;
    }

    public JsonObject getInputSchema(String seasonId, String sessionToken) throws IOException{
        Utilities.RestCallResults res = Utilities.sendGet(url+"/seasons/" + seasonId + "/inputschema", sessionToken);

        if (res.code != 200) {
            System.err.println("getInputSchema fail: " + res.code + ", " + res.message);
            return null;
        }

        JsonParser parser = new JsonParser();
        return parser.parse(res.message).getAsJsonObject();
    }

    public String updateInputSchema(String seasonId, JsonObject schemaJson, String sessionToken) throws IOException{
        Utilities.RestCallResults res = Utilities.sendPut(url+"/seasons/" + seasonId + "/inputschema", schemaJson.toString(), sessionToken);

        if (res.code != 200) {
            System.err.println("updateInputSchema fail: " + res.code + ", " + res.message);
            return null;
        }

        String schema = res.message;
        return schema;
    }
}