package problem4.util;

import java.util.*;

/**
 * =====================================================================
 * 라이브러리 없이 사용하는 미니 JSON 파서 유틸리티
 * 시험 환경에서 Jackson 같은 외부 라이브러리가 없을 때 사용합니다.
 *
 * ※ 채점을 위해 메서드 시그니처를 수정하지 마세요.
 * =====================================================================
 */
public class JsonParser {

    /**
     * JSON 문자열에서 String 필드를 추출합니다.
     * 예: getStr("{\"type\":\"AUTH\",\"empNo\":\"E1024\"}", "type") → "AUTH"
     *
     * ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
     */
    public static String getStr(String json, String key) {
        // 여기에 코드를 작성하세요.
        // 힌트: "\"key\":\"" 를 찾은 뒤 다음 '"' 위치까지 substring
        return null;
    }

    /**
     * JSON 문자열에서 int 필드를 추출합니다.
     * 예: getInt("{\"docId\":1,\"ver\":2}", "docId") → 1
     *
     * ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
     */
    public static int getInt(String json, String key) {
        // 여기에 코드를 작성하세요.
        // 힌트: "\"key\":" 를 찾은 뒤 숫자가 끝나는 위치(,  } 등) 까지 substring → parseInt
        return -1;
    }

    /**
     * JSON 문자열에서 boolean 필드를 추출합니다.
     *
     * ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
     */
    public static boolean getBool(String json, String key) {
        // 여기에 코드를 작성하세요.
        return false;
    }

    /**
     * JSON 문자열에서 String 배열 필드를 추출합니다.
     * 예: getStrArray("{\"approvers\":[\"E2001\",\"E2002\"]}", "approvers")
     *     → ["E2001", "E2002"]
     *
     * ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
     */
    public static String[] getStrArray(String json, String key) {
        // 여기에 코드를 작성하세요.
        // 힌트: "[" ~ "]" 구간 추출 후 큰따옴표 안 토큰만 모으기
        return new String[0];
    }

    // =====================================================================
    // JSON 빌더 유틸 (응답 JSON 직접 만들 때 사용)
    // =====================================================================

    /**
     * Key-Value 쌍들을 받아 JSON 객체 문자열을 반환합니다.
     * 값이 String이면 따옴표로 감싸고, Number/Boolean이면 그대로 넣습니다.
     *
     * 사용 예:
     *   JsonParser.build("type","RESP", "requestId","r1", "status","OK")
     *   → {"type":"RESP","requestId":"r1","status":"OK"}
     *
     * ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
     */
    public static String build(Object... keyValues) {
        // 여기에 코드를 작성하세요.
        // keyValues는 (key, value, key, value ...) 쌍으로 옵니다.
        // value가 String이면 "value", Number/Boolean이면 value 그대로
        return null;
    }
}
