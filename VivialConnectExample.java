/*
 * VivialConnectExample.java
 * 
 * Compile and run the example from the command line like:
 *
 * javac VivialConnectExample.java
 * java VivialConnectExample GET|POST
 *
 */

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.net.ProtocolException;

import java.util.Map;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.SimpleTimeZone;
import java.util.Date;

import java.text.SimpleDateFormat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VivialConnectExample {

    public static final String API_KEY = "";
    public static final String API_SECRET = "";
    public static final String ACCOUNT_ID = "";
    public static final String BASE_URL = "https://api.vivialconnect.net/api/v1.0";

    public static final String ISO_8601_FORMAT = "yyyyMMdd'T'HHmmss'Z'";
    public static final String HTTP_DATE_FORMAT = "E, dd MMM YYYY HH:mm:ss z";

    public VivialConnectExample() {}

    public String request(URL endpoint, String method,
                          Map<String, String> headers,
                          String body) throws IOException, ProtocolException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) endpoint.openConnection();
            connection.setRequestMethod(method);
            if (headers != null) {
                for (String headerKey : headers.keySet()) {
                    connection.setRequestProperty(headerKey, headers.get(headerKey));
                }
            }
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            if (body != null) {
                DataOutputStream wr = new DataOutputStream(
                        connection.getOutputStream());
                wr.writeBytes(body);
                wr.flush();
                wr.close();
            }

            InputStream is;
            try {
                is = connection.getInputStream();
            } catch (IOException e) {
                is = connection.getErrorStream();
            }

            BufferedReader rd = new BufferedReader(
                new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\n');
            }
            rd.close();
            return response.toString();   
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public String createSignature(URL endpoint, String method,
                                  Map<String, String> headers,
                                  Map<String, String> queryParameters,
                                  String body, String requestTimeStamp)
        throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {

        body = (body == null) ? "" : body;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(body.getBytes("UTF-8"));
        byte[] contentHash = md.digest();

        String canonicalizedHeaderNames = this.getCanonicalizeHeaderNames(headers);
        String canonicalizedHeaders = this.getCanonicalizedHeaderString(headers);
        String canonicalizedQueryParameters = this.getCanonicalizedQueryString(queryParameters);
        String canonicalRequest = method + "\n" +
            requestTimeStamp + "\n" +
            this.getCanonicalizedResourcePath(endpoint) + "\n" +
            canonicalizedQueryParameters + "\n" +
            canonicalizedHeaders + "\n" +
            canonicalizedHeaderNames + "\n" +
            this.toHex(contentHash);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
            VivialConnectExample.API_SECRET.getBytes(), "HmacSHA256"));
        byte[] signature = mac.doFinal(canonicalRequest.getBytes("UTF-8"));

        return "HMAC " + VivialConnectExample.API_KEY + ":" +
            this.toHex(signature);
    }

    public String getCanonicalizeHeaderNames(Map<String, String> headers) {
        List<String> sortedHeaders = new ArrayList<String>();
        sortedHeaders.addAll(headers.keySet());
        Collections.sort(sortedHeaders, String.CASE_INSENSITIVE_ORDER);
        StringBuilder buffer = new StringBuilder();
        for (String header : sortedHeaders) {
            if (buffer.length() > 0) buffer.append(";");
            buffer.append(header.toLowerCase());
        }
        return buffer.toString();
    }

    private String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (int i = 0; i < data.length; i++) {
            String hex = Integer.toHexString(data[i]);
            if (hex.length() == 1) {
                sb.append("0");
            } else if (hex.length() == 8) {
                hex = hex.substring(6);
            }
            sb.append(hex);
        }
        return sb.toString().toLowerCase(Locale.getDefault());
    }

    private String getCanonicalizedHeaderString(Map<String, String> headers) {
        List<String> sortedHeaders = new ArrayList<String>();
        sortedHeaders.addAll(headers.keySet());
        Collections.sort(sortedHeaders, String.CASE_INSENSITIVE_ORDER);
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < sortedHeaders.size(); i++) {
            String key = sortedHeaders.get(i);
            buffer.append(key.toLowerCase() + ":" + headers.get(key));
            if(i < sortedHeaders.size() - 1) {
                buffer.append("\n");    
            }
        }
        return buffer.toString();
    }

    private String getCanonicalizedResourcePath(URL endpoint)
        throws UnsupportedEncodingException{
        if (endpoint == null) {
            return "/";
        }
        String path = endpoint.getPath();
        if (path == null || path.isEmpty()) {
            return "/";
        }
        String encodedPath = this.urlEncode(path, true);
        if (encodedPath.startsWith("/")) {
            return encodedPath;
        } else {
            return "/".concat(encodedPath);
        }
    }

    private String getCanonicalizedQueryString(Map<String, String> parameters)
        throws UnsupportedEncodingException {
        if (parameters == null || parameters.isEmpty()) {
            return "";
        }
        SortedMap<String, String> sorted = new TreeMap<String, String>();

        Iterator<Map.Entry<String, String>> pairs = parameters.entrySet().iterator();
        while (pairs.hasNext()) {
            Map.Entry<String, String> pair = pairs.next();
            String key = pair.getKey();
            String value = pair.getValue();
            sorted.put(this.urlEncode(key, false), this.urlEncode(value, false));
        }

        StringBuilder builder = new StringBuilder();
        pairs = sorted.entrySet().iterator();
        while (pairs.hasNext()) {
            Map.Entry<String, String> pair = pairs.next();
            builder.append(pair.getKey());
            builder.append("=");
            builder.append(pair.getValue());
            if (pairs.hasNext()) {
                builder.append("&");
            }
        }
        return builder.toString();
    }

    private String urlEncode(String url, boolean keepSlash)
        throws UnsupportedEncodingException {
        String encoded = URLEncoder.encode(url, "UTF-8");
        if (keepSlash)
            encoded = encoded.replace("%2F", "/");
        return encoded;
    }

    public static void main(String[] args) throws Exception {
        String method = args.length > 0 ? args[0] : "GET";

        URL endpoint = new URL(VivialConnectExample.BASE_URL + "/accounts/" +
            VivialConnectExample.ACCOUNT_ID + "/messages.json");
        Date now = new Date();

        String message = "Test message from Vivial Connect Java example";
        String fromNumber = "+11234567890";
        String toNumber = "+19876543210";

        String body = null;

        VivialConnectExample example = new VivialConnectExample();

        SimpleDateFormat iso8601 = new SimpleDateFormat(
            VivialConnectExample.ISO_8601_FORMAT);
        iso8601.setTimeZone(new SimpleTimeZone(0, "GMT"));
        SimpleDateFormat httpDate = new SimpleDateFormat(
            VivialConnectExample.HTTP_DATE_FORMAT);
        httpDate.setTimeZone(new SimpleTimeZone(0, "GMT"));
        
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Date", httpDate.format(now));
        headers.put("Accept", "application/json");

        if (method.equals("POST")) {
            headers.put("Content-Type", "application/json");
            StringBuffer buff = new StringBuffer();
            buff.append("{\"message\": {");
            buff.append("\"body\": \"").append(message).append("\", ");
            buff.append("\"from_number\": \"").append(fromNumber).append("\", ");
            buff.append("\"to_number\": \"").append(toNumber).append("\" ");
            buff.append("} }");
            body = buff.toString();
        }

        String hostHeader = endpoint.getHost();
        int port = endpoint.getPort();
        if (port > -1) {
            hostHeader.concat(":" + Integer.toString(port));
        }
        headers.put("Host", hostHeader);

        String requestTimeStamp = iso8601.format(now);
        String authorization = example.createSignature(endpoint, method,
            headers, null, body, requestTimeStamp);

        String canonicalizedHeaderNames = example.getCanonicalizeHeaderNames(headers);
        headers.put("X-Auth-SignedHeaders", canonicalizedHeaderNames);

        headers.put("Authorization", authorization);        
        headers.put("X-Auth-Date", requestTimeStamp);

        String response = example.request(endpoint, method, headers, body);
        System.out.println(response);
    }
}
