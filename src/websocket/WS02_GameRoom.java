package websocket;

import java.util.*;
import java.io.*;

/**
 * =====================================================================
 * [WebSocket 예제 2] 게임 룸 매칭 서버 - 난이도: ⭐⭐ (중급)
 * =====================================================================
 *
 * Problem 4의 핵심 패턴인 "방 상태 관리 + 참가자별 PUSH"를 연습합니다.
 * - 방(Room) 생성/입장/퇴장
 * - 방 상태머신: WAITING → PLAYING → FINISHED
 * - 이벤트 발생 시 방 안의 모든 참가자에게 PUSH
 *
 * -------------------------------------------------------------------
 * [도메인]
 * Room: roomId(자동발급), hostSession, players(Set<String> sessionId),
 *       status("WAITING"/"PLAYING"/"FINISHED"), maxPlayers(2~4)
 *
 * -------------------------------------------------------------------
 * [처리할 이벤트]
 *
 * OPEN / CLOSE <sessionId>  : 기본 세션 관리
 *   - CLOSE 시 해당 세션이 방에 있으면 자동 LEAVE 처리
 *
 * {"type":"CREATE","maxPlayers":<N>}
 *   - 새 방 생성, 요청자가 자동으로 방에 입장 + 호스트
 *   - RESP: {"type":"RESP","status":"OK","roomId":<id>}
 *
 * {"type":"JOIN","roomId":<id>}
 *   - WAITING 방에만 입장 가능
 *   - 방이 가득 찼으면: {"type":"RESP","status":"ERR","code":"ROOM_FULL"}
 *   - 성공 시: 방 전체에 PUSH {"type":"PLAYER_JOINED","sessionId":"...","count":<현재인원>}
 *   - 인원이 maxPlayers에 도달하면 자동으로 게임 시작:
 *     방 전체에 PUSH {"type":"GAME_START","roomId":<id>,"players":[...세션목록...]}
 *
 * {"type":"LEAVE","roomId":<id>}
 *   - PLAYING 중엔 나갈 수 없음: {"type":"RESP","status":"ERR","code":"CANNOT_LEAVE"}
 *   - 성공 시: 남은 플레이어들에게 PUSH {"type":"PLAYER_LEFT","sessionId":"...","count":<남은인원>}
 *   - 방에 아무도 없으면 방 삭제
 *   - 나간 사람이 호스트였으면 남은 사람 중 첫 번째가 호스트 승계
 *
 * {"type":"FINISH","roomId":<id>,"winner":<sessionId>}
 *   - 호스트만 FINISH 가능
 *   - PLAYING 상태에서만 가능
 *   - status = "FINISHED"
 *   - 방 전체에 PUSH {"type":"GAME_OVER","winner":"<sessionId>"}
 *
 * {"type":"ROOM_LIST"}
 *   - WAITING 상태 방 목록 반환
 *   - RESP: {"type":"RESP","status":"OK","rooms":[{"roomId":1,"count":1,"maxPlayers":2}, ...]}
 *   - roomId 오름차순 정렬
 *
 * -------------------------------------------------------------------
 * [핵심 학습 포인트]
 * 1. emitToRoom(): 방 참가자 전원에게 PUSH
 * 2. 상태머신 게이트: WAITING/PLAYING/FINISHED 각 단계에서 허용 동작 다름
 * 3. 호스트 승계 패턴: Set을 iterator()로 첫 번째 원소 꺼내기
 * 4. 자동 트리거: 인원이 maxPlayers에 도달하면 GAME_START 자동 발생
 * =====================================================================
 */
public class WS02_GameRoom {

    // ※ 채점을 위해 아래 클래스를 수정하지 마세요.
    static class Room {
        int roomId;
        String hostSession;
        Set<String> players; // 참가자 sessionId 목록
        String status;       // "WAITING" | "PLAYING" | "FINISHED"
        int maxPlayers;

        Room(int roomId, String hostSession, int maxPlayers) {
            this.roomId      = roomId;
            this.hostSession = hostSession;
            this.players     = new LinkedHashSet<>();
            this.status      = "WAITING";
            this.maxPlayers  = maxPlayers;
        }
    }

    // ※ 채점을 위해 아래 필드를 수정하지 마세요.
    static Set<String> sessions = new HashSet<>();
    static Map<String, Integer> sessionToRoom = new HashMap<>(); // 세션 → 방ID
    static Map<Integer, Room> rooms = new LinkedHashMap<>();
    static int nextRoomId = 1;
    static List<String> emitLog = new ArrayList<>();

    // =========================================================

    static void emit(String sessionId, String json) {
        emitLog.add(sessionId + "|" + json);
        System.out.println("[EMIT → " + sessionId + "] " + json);
    }

    static void emitToRoom(int roomId, String json) {
        // 여기에 코드를 작성하세요.
        // rooms.get(roomId).players 순회하며 emit
    }

    static void emitToRoomExcept(int roomId, String excludeSession, String json) {
        // 여기에 코드를 작성하세요.
    }

    // =========================================================

    static void onOpen(String sessionId) {
        sessions.add(sessionId);
    }

    static void onClose(String sessionId) {
        // 여기에 코드를 작성하세요.
        // 방에 있었다면 자동 LEAVE 처리 (단, PLAYING 중이면 강제 퇴장)
        sessions.remove(sessionId);
    }

    static void onMessage(String sessionId, String json) {
        String type    = getStr(json, "type");
        String reqId   = getStr(json, "requestId");

        switch (type != null ? type : "") {
            case "CREATE":    handleCreate(sessionId, json, reqId);   break;
            case "JOIN":      handleJoin(sessionId, json, reqId);     break;
            case "LEAVE":     handleLeave(sessionId, json, reqId);    break;
            case "FINISH":    handleFinish(sessionId, json, reqId);   break;
            case "ROOM_LIST": handleRoomList(sessionId, reqId);       break;
        }
    }

    // =========================================================

    static void handleCreate(String sessionId, String json, String reqId) {
        // 여기에 코드를 작성하세요.
        // maxPlayers 파싱 (없으면 기본값 2, 범위: 2~4)
        // Room 생성, players에 자신 추가, sessionToRoom 등록
    }

    static void handleJoin(String sessionId, String json, String reqId) {
        // 여기에 코드를 작성하세요.
        // 1. roomId 파싱
        // 2. 방 존재 + WAITING 체크
        // 3. 가득 찬지 체크 (players.size() >= maxPlayers)
        // 4. 입장 처리
        // 5. PLAYER_JOINED PUSH
        // 6. 가득 찼으면 GAME_START PUSH + status = "PLAYING"
    }

    static void handleLeave(String sessionId, String json, String reqId) {
        // 여기에 코드를 작성하세요.
        // 1. PLAYING 체크 → CANNOT_LEAVE
        // 2. 퇴장 처리
        // 3. 남은 사람들에게 PLAYER_LEFT PUSH
        // 4. 방이 비면 삭제
        // 5. 호스트가 나갔으면 승계
    }

    static void handleFinish(String sessionId, String json, String reqId) {
        // 여기에 코드를 작성하세요.
        // 1. 호스트 체크
        // 2. PLAYING 체크
        // 3. status = "FINISHED"
        // 4. GAME_OVER PUSH
    }

    static void handleRoomList(String sessionId, String reqId) {
        // 여기에 코드를 작성하세요.
        // WAITING 방만, roomId 오름차순으로 JSON 배열 만들어서 응답
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
