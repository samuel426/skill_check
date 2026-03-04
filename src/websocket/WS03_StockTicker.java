package websocket;

import java.util.*;
import java.io.*;

/**
 * =====================================================================
 * [WebSocket 예제 3] 실시간 주식 시세 구독 - 난이도: ⭐⭐⭐ (고급)
 * =====================================================================
 *
 * Problem 4의 "구독(Subscribe) 패턴 + 조건부 PUSH"를 연습합니다.
 * 실무에서 WebSocket을 가장 많이 쓰는 패턴 중 하나입니다.
 *
 * -------------------------------------------------------------------
 * [처리할 이벤트]
 *
 * OPEN / CLOSE <sessionId>  : 기본 세션 관리
 *   - CLOSE 시 해당 세션의 모든 구독 자동 해제
 *
 * {"type":"AUTH","userId":"<id>"}
 *   - userId로 세션 인증
 *
 * {"type":"SUBSCRIBE","symbol":"<종목코드>"}
 *   - 해당 종목을 구독합니다.
 *   - 이미 구독 중이면 무시합니다.
 *   - RESP: {"type":"RESP","status":"OK","symbol":"<code>","lastPrice":<가격 or null>}
 *     (lastPrice: 한 번도 TICK이 없었으면 null, 있었으면 마지막 가격)
 *
 * {"type":"UNSUBSCRIBE","symbol":"<종목코드>"}
 *   - 해당 종목 구독 해제
 *   - RESP: {"type":"RESP","status":"OK"}
 *
 * {"type":"TICK","symbol":"<종목코드>","price":<가격>,"volume":<거래량>}
 *   - 시세 업데이트 이벤트 (외부 시스템이 보내는 이벤트라고 가정)
 *   - 인증 불필요 (시스템 이벤트)
 *   - lastPrice 갱신
 *   - 해당 종목을 구독 중인 모든 세션에 PUSH:
 *     {"type":"PRICE_UPDATE","symbol":"<code>","price":<가격>,"volume":<거래량>,"change":<변동폭>}
 *     (change = 이번 price - 직전 price, 첫 TICK이면 change = 0)
 *
 * {"type":"ALERT_SET","symbol":"<종목코드>","above":<가격>}
 *   - 해당 종목이 above 가격을 초과하면 본인에게 PUSH:
 *     {"type":"ALERT","symbol":"<code>","price":<현재가>,"threshold":<above>}
 *   - 알림은 조건 충족 시 1회만 발생 후 자동 삭제됩니다.
 *   - 같은 종목에 ALERT_SET을 여러 번 하면 마지막 설정으로 덮어씁니다.
 *
 * {"type":"MY_SUBS"}
 *   - 현재 구독 중인 종목 목록과 마지막 가격 응답
 *   - RESP: {"type":"RESP","status":"OK","subs":[{"symbol":"AAPL","lastPrice":150},...]  }
 *   - symbol 오름차순 정렬
 *
 * -------------------------------------------------------------------
 * [핵심 학습 포인트]
 * 1. 구독 역인덱스: Map<String, Set<String>> symbolToSessions (종목 → 구독 세션들)
 * 2. 세션별 구독 목록: Map<String, Set<String>> sessionToSymbols (세션 → 구독 종목들)
 *    → 둘 다 유지해야 CLOSE 시 빠르게 구독 해제 가능
 * 3. TICK 시 조건부 알림: 알림 설정을 Map<String, Map<String,Integer>> alertMap 으로 관리
 *    (세션 → (종목 → 임계가격))
 * 4. lastPrice: Map<String, Integer> 로 종목별 마지막 가격 관리
 * 5. change 계산: TICK마다 이전 가격과 비교
 *
 * -------------------------------------------------------------------
 * [입력 예시]
 *   OPEN s1
 *   OPEN s2
 *   s1 {"type":"AUTH","userId":"user1"}
 *   s2 {"type":"AUTH","userId":"user2"}
 *   s1 {"type":"SUBSCRIBE","symbol":"AAPL"}
 *   s2 {"type":"SUBSCRIBE","symbol":"AAPL"}
 *   s1 {"type":"SUBSCRIBE","symbol":"GOOG"}
 *   s1 {"type":"ALERT_SET","symbol":"AAPL","above":155}
 *   s1 {"type":"TICK","symbol":"AAPL","price":150,"volume":1000}
 *   s1 {"type":"TICK","symbol":"AAPL","price":160,"volume":2000}
 *   s1 {"type":"MY_SUBS"}
 *   s2 {"type":"UNSUBSCRIBE","symbol":"AAPL"}
 *   s1 {"type":"TICK","symbol":"AAPL","price":155,"volume":500}
 *
 * [출력 예시]
 *   [EMIT → s1] {"type":"RESP","status":"OK","symbol":"AAPL","lastPrice":null}
 *   [EMIT → s2] {"type":"RESP","status":"OK","symbol":"AAPL","lastPrice":null}
 *   [EMIT → s1] {"type":"RESP","status":"OK","symbol":"GOOG","lastPrice":null}
 *   [EMIT → s1] {"type":"PRICE_UPDATE","symbol":"AAPL","price":150,"volume":1000,"change":0}
 *   [EMIT → s2] {"type":"PRICE_UPDATE","symbol":"AAPL","price":150,"volume":1000,"change":0}
 *   [EMIT → s1] {"type":"PRICE_UPDATE","symbol":"AAPL","price":160,"volume":2000,"change":10}
 *   [EMIT → s2] {"type":"PRICE_UPDATE","symbol":"AAPL","price":160,"volume":2000,"change":10}
 *   [EMIT → s1] {"type":"ALERT","symbol":"AAPL","price":160,"threshold":155}
 *   [EMIT → s1] {"type":"RESP","status":"OK","subs":[{"symbol":"AAPL","lastPrice":160},{"symbol":"GOOG","lastPrice":null}]}
 *   [EMIT → s2] {"type":"RESP","status":"OK"}
 *   [EMIT → s1] {"type":"PRICE_UPDATE","symbol":"AAPL","price":155,"volume":500,"change":-5}
 *   (s2는 이미 구독 해제했으므로 PUSH 안 받음)
 *   (알림은 이미 1회 발동됐으므로 재발동 없음)
 * =====================================================================
 */
public class WS03_StockTicker {

    // ※ 채점을 위해 아래 필드를 수정하지 마세요.
    static Map<String, String> sessionToUser  = new HashMap<>();
    static Map<String, Set<String>> symbolToSessions = new HashMap<>(); // 종목 → 구독 세션들
    static Map<String, Set<String>> sessionToSymbols = new HashMap<>(); // 세션 → 구독 종목들
    static Map<String, Integer> lastPrice = new HashMap<>();            // 종목 → 마지막 가격
    // alertMap: 세션 → (종목 → 임계가격)
    static Map<String, Map<String, Integer>> alertMap = new HashMap<>();
    static List<String> emitLog = new ArrayList<>();

    // =========================================================

    static void emit(String sessionId, String json) {
        emitLog.add(sessionId + "|" + json);
        System.out.println("[EMIT → " + sessionId + "] " + json);
    }

    // =========================================================

    static void onOpen(String sessionId) {
        sessionToUser.put(sessionId, null);
        sessionToSymbols.put(sessionId, new HashSet<>());
        alertMap.put(sessionId, new HashMap<>());
    }

    static void onClose(String sessionId) {
        // 여기에 코드를 작성하세요.
        // 1. 이 세션이 구독 중이던 모든 종목에서 세션 제거 (symbolToSessions 역인덱스 정리)
        // 2. sessionToSymbols에서 제거
        // 3. alertMap에서 제거
        // 4. sessionToUser에서 제거
    }

    static void onMessage(String sessionId, String json) {
        String type = getStr(json, "type");

        if ("AUTH".equals(type)) {
            // 여기에 코드를 작성하세요.

        } else if ("SUBSCRIBE".equals(type)) {
            // 여기에 코드를 작성하세요.
            // 1. symbol 파싱
            // 2. 이미 구독 중이면 무시
            // 3. symbolToSessions, sessionToSymbols 양쪽 다 추가
            // 4. lastPrice.getOrDefault → null이면 "null", 있으면 숫자로 응답

        } else if ("UNSUBSCRIBE".equals(type)) {
            // 여기에 코드를 작성하세요.

        } else if ("TICK".equals(type)) {
            handleTick(sessionId, json);

        } else if ("ALERT_SET".equals(type)) {
            // 여기에 코드를 작성하세요.
            // alertMap.get(sessionId).put(symbol, above)

        } else if ("MY_SUBS".equals(type)) {
            // 여기에 코드를 작성하세요.
            // sessionToSymbols.get(sessionId) → symbol 오름차순 정렬 → JSON 배열
        }
    }

    static void handleTick(String sessionId, String json) {
        // 여기에 코드를 작성하세요.
        // 1. symbol, price, volume 파싱
        // 2. change = price - lastPrice.getOrDefault(symbol, price)  (첫 TICK이면 0)
        // 3. lastPrice 갱신
        // 4. 구독 중인 모든 세션에 PRICE_UPDATE PUSH
        // 5. 알림 체크:
        //    alertMap의 각 세션에서 해당 symbol의 임계가격이 있고 price > threshold이면
        //    해당 세션에 ALERT PUSH + 알림 삭제 (1회성)
    }

    // =========================================================
    // JSON 헬퍼
    // =========================================================

    static String getStr(String json, String key) {
        String search = "\"" + key + "\":\"";
        int s = json.indexOf(search);
        if (s == -1) return null;
        s += search.length();
        int e = json.indexOf("\"", s);
        return e == -1 ? null : json.substring(s, e);
    }

    static int getInt(String json, String key) {
        String search = "\"" + key + "\":";
        int s = json.indexOf(search);
        if (s == -1) return -1;
        s += search.length();
        int e = s;
        while (e < json.length() && ",}] ".indexOf(json.charAt(e)) == -1) e++;
        try { return Integer.parseInt(json.substring(s, e).trim()); }
        catch (NumberFormatException ex) { return -1; }
    }

    // =========================================================

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = br.readLine()) != null && !line.isEmpty()) {
            line = line.trim();
            if (line.startsWith("OPEN "))       onOpen(line.substring(5).trim());
            else if (line.startsWith("CLOSE ")) onClose(line.substring(6).trim());
            else {
                int sp = line.indexOf(' ');
                if (sp != -1) onMessage(line.substring(0, sp), line.substring(sp + 1));
            }
        }
    }
}
