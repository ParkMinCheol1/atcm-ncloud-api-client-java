package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class NcloudApi {
    public static final Logger logger = Logger.getLogger(NcloudApi.class);

    public static String apiDomain;
    public static String accessKey;
    public static String secretKey;

    public void readConfig() {
        try {
            String content = new String(Files.readAllBytes(Paths.get("config.json")));
            JSONObject jsonObject = new JSONObject(content);

            apiDomain = jsonObject.getJSONObject("DEFAULT").getString("API_DOMAIN");
            accessKey = jsonObject.getJSONObject("DEFAULT").getString("ACCESS_KEY");
            secretKey = jsonObject.getJSONObject("DEFAULT").getString("SECRET_KEY");

        } catch (IOException e) {
            logger.debug("readConfig error!!");
            e.printStackTrace();
        }
    }

    //현재 시각
    public static long getTimestamp() {
        return System.currentTimeMillis();
    }

    private String makeSignature(String method, String uri, String timestamp) {
        readConfig();
        try{
            byte[] secretKeyBytes = secretKey.getBytes("UTF-8");
            SecretKeySpec signingKey = new SecretKeySpec(secretKeyBytes, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            String message = method + " " + uri + "\n" + timestamp + "\n" + accessKey;
            byte[] messageBytes = message.getBytes("UTF-8");
            byte[] rawHmac = mac.doFinal(messageBytes);
            return Base64.getEncoder().encodeToString(rawHmac);

        }catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private Map<String, String> commonHeader(String signature, String timestamp) {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-ncp-apigw-timestamp", timestamp);
        headers.put("x-ncp-iam-access-key", accessKey);
        headers.put("x-ncp-apigw-signature-v2", signature);
        return headers;
    }

    private static String createQueryString(JSONObject data) {
        try {
            StringBuilder queryString = new StringBuilder();

            for (String key : data.keySet()) {
                Object value = data.get(key);

                if (value != null) {
                    String encodedKey = URLEncoder.encode(key, "UTF-8");
                    String encodedValue = URLEncoder.encode(value.toString(), "UTF-8");

                    if (queryString.length() > 0) {
                        queryString.append("&");
                    }

                    queryString.append(encodedKey).append("=").append(encodedValue);
                }
            }
            return queryString.toString();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String httpConnection (String method, String url, Map<String, String> headers) {
        String result = "";

        try{
            // URL 객체 생성
            URL apiUrl = new URL(url);
            // HttpURLConnection 객체 생성
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            // HTTP 메소드 설정
            connection.setRequestMethod(method);
            // 헤더 추가
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            // 응답 내용 읽기
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder responseContent = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                responseContent.append(line);
            }
            reader.close();
            result = String.valueOf(responseContent);
            // 연결 종료
            connection.disconnect();

        } catch (Exception e){
            logger.debug("httpConnection error");
        }
        return result;
    }

    public String getServerInstanceList() {

        /**
         *  인스턴스 정보 조회
         */
        String result = "";

        try{
            String regionCode = "FKR";
            String method = "GET";
            String api = "/vserver/v2/getServerInstanceList?regionCode=" + regionCode + "&responseFormatType=json";

            String timestamp = String.valueOf(getTimestamp());
            String signature = makeSignature(method, api, timestamp);

            Map<String, String> headers = commonHeader(signature, timestamp);

            String url = apiDomain + api;

            result = httpConnection(method, url, headers);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public String createServerInstances(JSONObject jsonObject) {
        /**
         *  서버 인스턴스 (VM)를 생성
         */

        String result = "";

        try{
            String method = "GET";
            String api = "/vserver/v2/createServerInstances?" + createQueryString(jsonObject);

            String timestamp = String.valueOf(getTimestamp());
            String signature = makeSignature(method, api, timestamp);

            Map<String, String> headers = commonHeader(signature, timestamp);

            String url = apiDomain + api;

            result = httpConnection(method, url, headers);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }


    public String getServerInstanceDetail (String serverInstanceNo) {
        /**
         *  VPC 리스트를 조회
         */
        String result = "";

        try{
            String method = "GET";
            String api = "/vserver/v2/getServerInstanceDetail?serverInstanceNo=" + serverInstanceNo + "&regionCode=FKR&responseFormatType=json";

            String timestamp = String.valueOf(getTimestamp());
            String signature = makeSignature(method, api, timestamp);

            Map<String, String> headers = commonHeader(signature, timestamp);

            String url = apiDomain + api;

            result = httpConnection(method, url, headers);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return  result;
    }



}
