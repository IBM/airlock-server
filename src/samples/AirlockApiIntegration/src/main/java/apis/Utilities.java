package apis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Utilities {

    public static class RestCallResults {
        public String message;
        public int code;

        public RestCallResults(String msg, int code) {
            this.message = msg;
            this.code = code;
        }
    }

    public static RestCallResults sendPost(String urlString, String parameters, String sessionToken) throws IOException {

        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("POST");
        if (sessionToken!=null)
            con.setRequestProperty ("sessionToken", sessionToken);

        con.setDoOutput(true);
        con.getOutputStream().write(parameters.getBytes("UTF-8"));
        return buildResult(con);
    }

    public static RestCallResults sendDelete(String urlString, String sessionToken) throws IOException {

        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("DELETE");
        con.setDoOutput(true);

        if (sessionToken != null)
            con.setRequestProperty ("sessionToken", sessionToken);

        return buildResult(con);
    }

    public static RestCallResults sendGet(String url, String sessionToken) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("GET");
        if (sessionToken != null)
            con.setRequestProperty ("sessionToken", sessionToken);

        return buildResult(con);
    }

    public static RestCallResults sendPut(String urlString, String parameters, String sessionToken) throws IOException{

        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("PUT");
        if (sessionToken != null)
            con.setRequestProperty ("sessionToken", sessionToken);

        con.setDoOutput(true);
        con.getOutputStream().write(parameters.getBytes("UTF-8"));
        return buildResult(con);
    }


    private static RestCallResults buildResult(HttpURLConnection con)  throws IOException
    {
        int responseCode = con.getResponseCode();

        InputStream inp;
        if ((responseCode >= 200 && responseCode <= 299) || con.getErrorStream() == null)
            inp = con.getInputStream();
        else
            inp = con.getErrorStream();

        String out;
        if (inp == null)
            out = "Response Code : " + responseCode;
        else
            out = Utilities.streamToString(inp);

        return new RestCallResults (out, responseCode);
    }

    private static String streamToString(InputStream is)
    {
        java.util.Scanner s = new java.util.Scanner(is, "UTF-8");
        s.useDelimiter("\\A");
        String out = s.hasNext() ? s.next() : "";
        s.close();
        return out;
    }

    public static String getUniquId(String object) {
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(object).getAsJsonObject();

        return jsonObject.get("uniqueId").getAsString();
    }

    public static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
