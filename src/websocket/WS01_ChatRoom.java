package websocket;

import java.util.*;
import java.io.*;

/**
 * =====================================================================
 * [WebSocket 예제 1] 기본 채팅방 - 난이도: ⭐ (입문)
 * =====================================================================
 *
 * WebSocket의 가장 기본 패턴입니다.
 * - 세션 관리 (onOpen / onClose)
 * - 인증 (AUTH)
 * - 브로드캐스트 (모든 세션에 메시지 전송)
 *
 * -------------------------------------------------------------------
 * [처리할 이벤트]
 *
 * OPEN <sessionId>
 *   - 세션을 등록합니다 (아직 미인증 상태)
 *
 * CLOSE <sessionId>
 *   - 세션을 제거합니다
 *   - 인증된 사용자였다면 다른 모든 세션에 퇴장 메시지 브로드캐스트
 *   - PUSH: {"type":"NOTICE","msg":"<이름> has left"}
 *
 * <sessionId> {"type":"AUTH","name":"<닉네임>"}
 *   - 닉네임 등록. 이미 사용 중인 닉네임이면:
 *     {"type":"RESP","status":"ERR","code":"NAME_TAKEN"}
 *   - 성공 시: 본인에게 {"type":"RESP","status":"OK"}
 *   - 성공 시: 다른 모든 세션에 {"type":"NOTICE","msg":"<이름> has joined"}
 *
 * <sessionId> {"type":"CHAT","msg":"<내용>"}
 *   - 미인증 세션: {"type":"RESP","status":"ERR","code":"UNAUTHORIZED"}
 *   - 인증된 경우: 모든 세션(본인 포함)에 브로드캐스트
 *     {"type":"CHAT","from":"<이름>","msg":"<내용>"}
 *
 * <sessionId> {"type":"WHO"}
 *   - 현재 접속 중인 닉네임 목록을 이름 오름차순으로 응답
 *   - {"type":"RESP","status":"OK","users":["alice","bob",...]}
 *
 * -------------------------------------------------------------------
 * [입력 예시]
 *   OPEN s1
 *   OPEN s2
 *   s1 {"type":"AUTH","name":"alice"}
 *   s2 {"type":"AUTH","name":"bob"}
 *   s1 {"type":"CHAT","msg":"hello"}
 *   s2 {"type":"WHO"}
 *   CLOSE s1
 *
 * [출력 예시]
 *   [EMIT → s1] {"type":"RESP","status":"OK"}
 *   [EMIT → s2] {"type":"NOTICE","msg":"alice has joined"}
 *   [EMIT → s2] {"type":"RESP","status":"OK"}
 *   [EMIT → s1] {"type":"NOTICE","msg":"bob has joined"}
 *   [EMIT → s1] {"type":"CHAT","from":"alice","msg":"hello"}
 *   [EMIT → s2] {"type":"CHAT","from":"alice","msg":"hello"}
 *   [EMIT → s2] {"type":"RESP","status":"OK","users":["alice","bob"]}
 *   [EMIT → s2] {"type":"NOTICE","msg":"alice has left"}
 *
 * -------------------------------------------------------------------
 * [핵심 학습 포인트]
 * 1. 세션 Map 구조:  Map<String, String> sessionToName
 * 2. 사용 중 닉네임: Set<String> usedNames
 * 3. 브로드캐스트:   sessionToName.keySet() 전체 순회하며 emit()
 * 4. 본인 제외 브로드캐스트: if (!sid.equals(sessionId)) emit(...)
 * =====================================================================
 */
public class WS01_ChatRoom {

    // ※ 채점을 위해 아래 필드를 수정하지 마세요.
    static Map<String, String> sessionToName = new LinkedHashMap<>(); // 세션 → 닉네임(null=미인증)
    static Set<String> usedNames = new HashSet<>();
    static List<String> emitLog = new ArrayList<>();

    // =========================================================

    static void emit(String sessionId, String json) {
        emitLog.add(sessionId + "|" + json);
        System.out.println("[EMIT → " + sessionId + "] " + json);
    }

    static void broadcast(String json) {
        // 여기에 코드를 작성하세요.
        // 모든 세션에 emit
    }

    static void broadcastExcept(String excludeSession, String json) {
        // 여기에 코드를 작성하세요.
        // excludeSession 제외 모든 세션에 emit
    }

    // =========================================================

    static void onOpen(String sessionId) {
        // 여기에 코드를 작성하세요.
    }

    static void onClose(String sessionId) {
        // 여기에 코드를 작성하세요.
        // 1. 닉네임 꺼내기
        // 2. usedNames에서 제거
        // 3. sessionToName에서 제거
        // 4. 인증된 사용자였으면 퇴장 NOTICE 브로드캐스트
    }

    static void onMessage(String sessionId, String json) {
        String type = getStr(json, "type");

        if ("AUTH".equals(type)) {
            // 여기에 코드를 작성하세요.

        } else if ("CHAT".equals(type)) {
            // 여기에 코드를 작성하세요.

        } else if ("WHO".equals(type)) {
            // 여기에 코드를 작성하세요.
            // 현재 닉네임 목록을 오름차순 정렬해서 JSON 배열로 만들기
        }
    }

    // =========================================================
    // 간단한 JSON 헬퍼 (직접 구현해보세요)
    // =========================================================

    static String getStr(String json, String key) {
        // 여기에 코드를 작성하세요.
        return null;
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
