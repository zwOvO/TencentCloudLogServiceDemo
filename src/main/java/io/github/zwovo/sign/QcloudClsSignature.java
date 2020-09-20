package io.github.zwovo.sign;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacUtils;

public class QcloudClsSignature {

    public static final String LINE_SEPARATOR = "\n";
    public static final String Q_SIGN_ALGORITHM_KEY = "q-sign-algorithm";
    public static final String Q_SIGN_ALGORITHM_VALUE = "sha1";
    public static final String Q_AK = "q-ak";
    public static final String Q_SIGN_TIME = "q-sign-time";
    public static final String Q_KEY_TIME = "q-key-time";
    public static final String Q_HEADER_LIST = "q-header-list";
    public static final String Q_URL_PARAM_LIST = "q-url-param-list";
    public static final String Q_SIGNATURE = "q-signature";
    public static final String DEFAULT_ENCODING = "UTF-8";

    private static Map<String, String> buildSignHeaders(Map<String, String> originHeaders) {
        Map<String, String> signHeaders = new HashMap<String, String>();
        for (Entry<String, String> headerEntry : originHeaders.entrySet()) {
            String key = headerEntry.getKey();
            if (key.equalsIgnoreCase("content-type") || key.equalsIgnoreCase("content-md5")
                  || key.equalsIgnoreCase("host") || key.startsWith("x") || key.startsWith("X")) {
                String lowerKey = key.toLowerCase();
                String value = headerEntry.getValue();
                signHeaders.put(lowerKey, value);
            }
        }

        return signHeaders;
    }

    private static String buildSignMemberStr(Map<String, String> signHeaders) {
        StringBuilder strBuilder = new StringBuilder();
        boolean seenOne = false;
        for (String key : signHeaders.keySet()) {
            if (!seenOne) {
                seenOne = true;
            } else {
                strBuilder.append(";");
            }
            strBuilder.append(key.toLowerCase());
        }
        return strBuilder.toString();
    }

    private static String formatMapToStr(Map<String, String> kVMap) throws UnsupportedEncodingException {
        StringBuilder strBuilder = new StringBuilder();
        boolean seeOne = false;
        for (Entry<String, String> entry : kVMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String lowerKey = key.toLowerCase();
            String encodeKey = URLEncoder.encode(lowerKey, DEFAULT_ENCODING).replace("*", "%2A");
            String encodedValue = "";
            if (value != null) {
                encodedValue = URLEncoder.encode(value, DEFAULT_ENCODING).replace("*", "%2A");
            }
            if (!seeOne) {
                seeOne = true;
            } else {
                strBuilder.append("&");
            }
            strBuilder.append(encodeKey).append("=").append(encodedValue);
        }
        return strBuilder.toString();
    }

    private static String buildTimeStr(long expireMillsecond) {
        StringBuilder strBuilder = new StringBuilder();
        long startTime = System.currentTimeMillis() / 1000 - 60;
        long endTime = (System.currentTimeMillis() + expireMillsecond) / 1000;
        strBuilder.append(startTime).append(";").append(endTime);
        //return "1510109254;1510109314";
        return strBuilder.toString();
    }

    public static String buildSignature(String secretId, String secretKey, String method, String path,
            Map<String, String> paramMap, Map<String, String> headerMap, long expireMillsecond)
                    throws UnsupportedEncodingException {

        Map<String, String> signHeaders = buildSignHeaders(headerMap);
        TreeMap<String, String> sortedSignHeaders = new TreeMap<String, String>();
        TreeMap<String, String> sortedParams = new TreeMap<String, String>();

        sortedSignHeaders.putAll(signHeaders);
        sortedParams.putAll(paramMap);

        String qHeaderListStr = buildSignMemberStr(sortedSignHeaders);
        String qUrlParamListStr = buildSignMemberStr(sortedParams);
        String qKeyTimeStr, qSignTimeStr;
        qKeyTimeStr = qSignTimeStr = buildTimeStr(expireMillsecond);
        String signKey = HmacUtils.hmacSha1Hex(secretKey, qKeyTimeStr);
        String formatMethod = method.toString().toLowerCase();
        String formatUri = path;
        String formatParameters = formatMapToStr(sortedParams);
        String formatHeaders = formatMapToStr(sortedSignHeaders);

        String formatStr = new StringBuilder().append(formatMethod).append(LINE_SEPARATOR)
                .append(formatUri).append(LINE_SEPARATOR).append(formatParameters)
                .append(LINE_SEPARATOR).append(formatHeaders).append(LINE_SEPARATOR).toString();
        //System.out.println(formatStr + "\n");
        String hashFormatStr = DigestUtils.sha1Hex(formatStr);
        String stringToSign = new StringBuilder().append(Q_SIGN_ALGORITHM_VALUE)
                .append(LINE_SEPARATOR).append(qSignTimeStr).append(LINE_SEPARATOR)
                .append(hashFormatStr).append(LINE_SEPARATOR).toString();
        //System.out.println(stringToSign + "\n");
        String signature = HmacUtils.hmacSha1Hex(signKey, stringToSign);

        String authoriationStr = new StringBuilder().append(Q_SIGN_ALGORITHM_KEY).append("=")
                .append(Q_SIGN_ALGORITHM_VALUE).append("&").append(Q_AK).append("=")
                .append(secretId).append("&").append(Q_SIGN_TIME).append("=")
                .append(qSignTimeStr).append("&").append(Q_KEY_TIME).append("=").append(qKeyTimeStr)
                .append("&").append(Q_HEADER_LIST).append("=").append(qHeaderListStr).append("&")
                .append(Q_URL_PARAM_LIST).append("=").append(qUrlParamListStr).append("&")
                .append(Q_SIGNATURE).append("=").append(signature).toString();
        return authoriationStr;
    }

    public static void main(String args[]) {
        Map<String, String> paramMap = new HashMap();
        Map<String, String> headerMap = new HashMap();
        paramMap.put("logset_id", "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx");
        headerMap.put("Host", "ap-shanghai.cls.myqcloud.com");
        headerMap.put("User-Agent", "AuthSDK");
        try {
            System.out.println(QcloudClsSignature.buildSignature("AKIDc9YlmrBcFk4C8sbmXQ8i65XXXXXXXXXX", "LUSE4nPK1d4tX5SHyXv6tZXXXXXXXXXX",
                "GET", "/logset", paramMap, headerMap, 300000));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        paramMap.clear();
        headerMap.put("Content-Type", "application/json");
        headerMap.put("Content-MD5", "f9c7fc33c7eab68dfa8a52508d1f4659");
        try {
            System.out.println(QcloudClsSignature.buildSignature("AKIDc9YlmrBcFk4C8sbmXQ8i65XXXXXXXXXX", "LUSE4nPK1d4tX5SHyXv6tZXXXXXXXXXX",
                    "PUT", "/logset", paramMap, headerMap, 300000));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

}
