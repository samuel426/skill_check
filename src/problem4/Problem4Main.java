package problem4;

import java.util.*;
import java.io.*;

/**
 * =====================================================================
 * [문제 4] 채점용 실행 진입점
 *
 * 입력 형식:
 *   각 줄: <sessionId> <JSON>
 *   특수 명령: OPEN <sessionId>  / CLOSE <sessionId>
 *
 * 출력:
 *   emit() 이 호출될 때마다 "[EMIT → sessionId] json" 형태로 출력됩니다.
 *   (WebSocketServer.emit() 메서드 내부에서 println 처리됨)
 *
 * -------------------------------------------------------------------
 * [입력 예시]
 *   OPEN s1
 *   s1 {"type":"AUTH","requestId":"r1","employeeNumber":"E1024"}
 *   s1 {"type":"DOC_CREATE","requestId":"r2","title":"휴가 신청","content":"3/10~3/12","approvers":["E2001","E2002"]}
 *   s1 {"type":"DOC_UPDATE","requestId":"r3","docId":1,"ver":1,"content":"3/11~3/12 수정"}
 *   s1 {"type":"DOC_SUBMIT","requestId":"r4","docId":1,"ver":2}
 *   OPEN s2
 *   s2 {"type":"AUTH","requestId":"r5","employeeNumber":"E2001"}
 *   s2 {"type":"DOC_APPROVE","requestId":"r6","docId":1,"ver":3}
 *   OPEN s3
 *   s3 {"type":"AUTH","requestId":"r7","employeeNumber":"E2002"}
 *   s3 {"type":"DOC_REJECT","requestId":"r8","docId":1,"ver":4}
 *   s1 {"type":"NOTI_LIST","requestId":"r9","limit":10,"onlyUnread":true}
 *   CLOSE s1
 *   CLOSE s2
 *   CLOSE s3
 *
 * [예상 출력]
 *   [EMIT → s1] {"type":"RESP","requestId":"r1","status":"OK"}
 *   [EMIT → s1] {"type":"RESP","requestId":"r2","status":"OK","docId":1,"ver":1}
 *   [EMIT → s1] {"type":"RESP","requestId":"r3","status":"OK","ver":2}
 *   [EMIT → s1] {"type":"RESP","requestId":"r4","status":"OK","ver":3,"statusAfter":"IN_REVIEW","currentIdx":0}
 *   [EMIT → s2] {"type":"RESP","requestId":"r5","status":"OK"}
 *   [EMIT → s2] {"type":"PUSH","event":"NOTI_CREATED","noti":{...DOC_SUBMITTED...}}  ← s2 AUTH 전 알림이 있어도 AUTH 후 전송 없음 (이미 저장됨)
 *   [EMIT → s2] {"type":"RESP","requestId":"r6","status":"OK","ver":4}
 *   [EMIT → s3] {"type":"PUSH","event":"NOTI_CREATED","noti":{...DOC_NEXT_APPROVER...}}  ← s2 APPROVE 시 s3 미접속이라 PUSH 없음
 *   [EMIT → s3] {"type":"RESP","requestId":"r7","status":"OK"}
 *   [EMIT → s3] {"type":"RESP","requestId":"r8","status":"OK","ver":5}
 *   [EMIT → s1] {"type":"PUSH","event":"NOTI_CREATED","noti":{...DOC_REJECTED...}}
 *   [EMIT → s1] {"type":"RESP","requestId":"r9","status":"OK","notifications":[...]}
 * =====================================================================
 */
public class Problem4Main {

    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        WebSocketServer server = new WebSocketServer();
        String line;

        while ((line = br.readLine()) != null && !line.isEmpty()) {
            line = line.trim();
            if (line.startsWith("OPEN ")) {
                String sessionId = line.substring(5).trim();
                server.onOpen(sessionId);
            } else if (line.startsWith("CLOSE ")) {
                String sessionId = line.substring(6).trim();
                server.onClose(sessionId);
            } else {
                // "<sessionId> <json>" 형식
                int spaceIdx = line.indexOf(' ');
                if (spaceIdx == -1) continue;
                String sessionId = line.substring(0, spaceIdx);
                String json = line.substring(spaceIdx + 1);
                server.onMessage(sessionId, json);
            }
        }
    }
}
