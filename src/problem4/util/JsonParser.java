package problem4.util;

import java.util.*;

/**
 * 라이브러리 없이 사용하는 미니 JSON 파서 유틸리티
 * ※ 채점을 위해 메서드 시그니처를 수정하지 마세요.
 */
public class JsonParser {

    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
    public static String getStr(String json, String key) {
        // "key":"value" 패턴 탐색
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
    public static int getInt(String json, String key) {
        // "key":숫자 패턴 탐색
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return -1;
        start += search.length();
        // 숫자가 끝나는 위치 탐색 (,  } ] 공백 중 하나)
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == ',' || c == '}' || c == ']' || c == ' ') break;
            end++;
        }
        try {
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
    public static boolean getBool(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return false;
        start += search.length();
        return json.startsWith("true", start);
    }

    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
    public static String[] getStrArray(String json, String key) {
        // "key":["v1","v2"] 패턴 탐색
        String search = "\"" + key + "\":[";
        int start = json.indexOf(search);
        if (start == -1) return new String[0];
        start += search.length();
        int end = json.indexOf("]", start);
        if (end == -1) return new String[0];

        String arrayContent = json.substring(start, end).trim();
        if (arrayContent.isEmpty()) return new String[0];

        // 큰따옴표 안의 값만 추출
        List<String> result = new ArrayList<>();
        int i = 0;
        while (i < arrayContent.length()) {
            int q1 = arrayContent.indexOf("\"", i);
            if (q1 == -1) break;
            int q2 = arrayContent.indexOf("\"", q1 + 1);
            if (q2 == -1) break;
            result.add(arrayContent.substring(q1 + 1, q2));
            i = q2 + 1;
        }
        return result.toArray(new String[0]);
    }

    // =====================================================================
    // JSON 빌더 유틸
    // =====================================================================

    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
    public static String build(Object... keyValues) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) sb.append(",");
            String k = (String) keyValues[i];
            Object v = keyValues[i + 1];
            sb.append("\"").append(k).append("\":");
            if (v instanceof String) {
                sb.append("\"").append(v).append("\"");
            } else {
                // Number, Boolean, 이미 JSON 문자열인 경우 그대로
                sb.append(v);
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
