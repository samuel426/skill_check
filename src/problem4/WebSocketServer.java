package problem4;

import problem4.domain.Document;
import problem4.domain.Notification;
import problem4.util.JsonParser;

import java.util.*;

/**
 * =====================================================================
 * [문제 4] 전자결재 WebSocket 서버 (CRUD + 상태머신 + 실시간 알림)
 * =====================================================================
 *
 * 다우기술 과제테스트 스타일: 실제 소켓 없이 onMessage() 핸들러만 구현합니다.
 * 채점기가 onOpen / onMessage / onClose 를 순서대로 호출하고,
 * emit()으로 전송된 JSON 문자열을 순서대로 캡처해 채점합니다.
 *
 * -------------------------------------------------------------------
 * [처리해야 할 요청 type 목록]
 *
 *  인증
 *    AUTH           → 세션에 empNo 등록
 *
 *  Document CRUD
 *    DOC_CREATE     → 문서 생성 (DRAFT, ver=1)
 *    DOC_READ       → 단건 조회 (권한: 작성자 or 결재자)
 *    DOC_UPDATE     → 초안 수정 (DRAFT + 작성자 + ver 일치)
 *    DOC_DELETE     → 초안 삭제 (DRAFT + 작성자 + ver 일치, soft delete)
 *
 *  결재 흐름
 *    DOC_SUBMIT     → 상신 (DRAFT→IN_REVIEW, 첫 결재자에게 PUSH)
 *    DOC_APPROVE    → 승인 (현재 결재자만, 마지막이면 APPROVED + 작성자 PUSH)
 *    DOC_REJECT     → 반려 (현재 결재자만, REJECTED + 작성자 PUSH)
 *
 *  알림 관리
 *    NOTI_LIST      → 내 알림 목록 (createdAt 내림차순, onlyUnread 지원)
 *    NOTI_READ      → 읽음 처리 (수신자만)
 *    NOTI_DELETE    → 삭제 (수신자만)
 *
 * -------------------------------------------------------------------
 * [공통 에러 코드]
 *  UNAUTHORIZED   - 인증 안 된 세션
 *  FORBIDDEN      - 권한 없음
 *  NOT_FOUND      - 존재하지 않는 docId / notiId
 *  CONFLICT       - 낙관적 락 ver 불일치
 *  BAD_STATUS     - 현재 status에서 불가능한 작업
 *  BAD_REQUEST    - 필수 필드 누락 등
 *
 * -------------------------------------------------------------------
 * [JSON 프로토콜 요약]
 *
 * AUTH 요청/응답:
 *   REQ:  {"type":"AUTH","requestId":"r1","employeeNumber":"E1024"}
 *   RESP: {"type":"RESP","requestId":"r1","status":"OK"}
 *
 * DOC_CREATE:
 *   REQ:  {"type":"DOC_CREATE","requestId":"r2","title":"휴가 신청","content":"...","approvers":["E2001","E2002"]}
 *   RESP: {"type":"RESP","requestId":"r2","status":"OK","docId":1,"ver":1}
 *
 * DOC_READ:
 *   RESP: {"type":"RESP","requestId":"r3","status":"OK","doc":{...전체 필드...}}
 *
 * DOC_UPDATE:
 *   REQ:  {"type":"DOC_UPDATE","requestId":"r4","docId":1,"ver":1,"title":"...","content":"..."}
 *   RESP: {"type":"RESP","requestId":"r4","status":"OK","ver":2}
 *   ERR:  {"type":"RESP","requestId":"r4","status":"ERR","code":"CONFLICT","curVer":2}
 *
 * DOC_SUBMIT:
 *   RESP: {"type":"RESP","requestId":"r5","status":"OK","ver":3,"statusAfter":"IN_REVIEW","currentIdx":0}
 *   PUSH: {"type":"PUSH","event":"NOTI_CREATED","noti":{...}}  → 첫 결재자에게
 *
 * DOC_APPROVE (중간 결재):
 *   RESP: {"type":"RESP","requestId":"r6","status":"OK","ver":4}
 *   PUSH: 다음 결재자에게 DOC_NEXT_APPROVER 알림
 *
 * DOC_APPROVE (최종 결재):
 *   RESP: {"type":"RESP","requestId":"r6","status":"OK","ver":4}
 *   PUSH: 작성자에게 DOC_APPROVED 알림
 *
 * DOC_REJECT:
 *   RESP: {"type":"RESP","requestId":"r6","status":"OK","ver":4}
 *   PUSH: 작성자에게 DOC_REJECTED 알림
 *
 * NOTI_LIST:
 *   RESP: {"type":"RESP","requestId":"r7","status":"OK","notifications":[...]}
 *
 * NOTI_READ / NOTI_DELETE:
 *   RESP: {"type":"RESP","requestId":"r8","status":"OK"}
 *
 * -------------------------------------------------------------------
 * [미니 예시 시퀀스]
 *   s1 AUTH E1024
 *   s1 DOC_CREATE → docId=1, ver=1
 *   s1 DOC_UPDATE(ver=1) → ver=2
 *   s1 DOC_SUBMIT(ver=2) → ver=3 + E2001에 PUSH
 *   s2 AUTH E2001
 *   s2 DOC_APPROVE(ver=3) → ver=4 + E2002에 PUSH
 *   s3 AUTH E2002
 *   s3 DOC_REJECT(ver=4) → ver=5 + E1024에 PUSH(반려)
 * =====================================================================
 */
public class WebSocketServer {

    // =========================================================
    // ※ 채점을 위해 아래 필드들을 수정하지 마세요.
    // =========================================================

    /** 세션ID → 인증된 empNo (인증 전이면 null) */
    Map<String, String> sessionToEmp = new HashMap<>();

    /** empNo → 접속 중인 세션ID 목록 (다중 기기 지원) */
    Map<String, List<String>> empToSessions = new HashMap<>();

    /** docId → Document */
    Map<Integer, Document> documents = new HashMap<>();

    /** notiId → Notification */
    Map<Long, Notification> notifications = new HashMap<>();

    /** 자동 증가 ID */
    int nextDocId = 1;
    long nextNotiId = 1000L;

    /** emit() 호출 결과를 순서대로 기록 (채점용) */
    List<String> emitLog = new ArrayList<>();

    // =========================================================
    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
    // =========================================================

    /** 특정 세션에 JSON 전송 (채점기가 캡처함) */
    void emit(String sessionId, String json) {
        emitLog.add(sessionId + "|" + json);
        System.out.println("[EMIT → " + sessionId + "] " + json);
    }

    /** 같은 empNo의 모든 세션에 PUSH 전송 */
    void emitToEmployee(String empNo, String json) {
        List<String> sessions = empToSessions.get(empNo);
        if (sessions == null) return;
        for (String sid : sessions) {
            emit(sid, json);
        }
    }

    // =========================================================

    /** 세션 연결 이벤트 */
    void onOpen(String sessionId) {
        sessionToEmp.put(sessionId, null);
    }

    /** 세션 종료 이벤트 */
    void onClose(String sessionId) {
        String empNo = sessionToEmp.get(sessionId);
        if (empNo != null) {
            List<String> sessions = empToSessions.get(empNo);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) empToSessions.remove(empNo);
            }
        }
        sessionToEmp.remove(sessionId);
    }

    /** 메시지 수신 이벤트 — 여기가 핵심 구현 영역 */
    void onMessage(String sessionId, String json) {
        String type      = JsonParser.getStr(json, "type");
        String requestId = JsonParser.getStr(json, "requestId");

        if ("AUTH".equals(type)) {
            handleAuth(sessionId, json, requestId);
            return;
        }

        // 인증 체크: 인증 안 된 세션은 UNAUTHORIZED 반환
        String empNo = sessionToEmp.get(sessionId);
        if (empNo == null) {
            emit(sessionId, JsonParser.build(
                    "type", "RESP",
                    "requestId", requestId,
                    "status", "ERR",
                    "code", "UNAUTHORIZED"
            ));
            return;
        }

        // 여기에 코드를 작성하세요.
        // type에 따라 각 핸들러 메서드 호출
        switch (type) {
            case "DOC_CREATE":  handleDocCreate(sessionId, json, requestId, empNo);  break;
            case "DOC_READ":    handleDocRead(sessionId, json, requestId, empNo);    break;
            case "DOC_UPDATE":  handleDocUpdate(sessionId, json, requestId, empNo);  break;
            case "DOC_DELETE":  handleDocDelete(sessionId, json, requestId, empNo);  break;
            case "DOC_SUBMIT":  handleDocSubmit(sessionId, json, requestId, empNo);  break;
            case "DOC_APPROVE": handleDocApprove(sessionId, json, requestId, empNo); break;
            case "DOC_REJECT":  handleDocReject(sessionId, json, requestId, empNo);  break;
            case "NOTI_LIST":   handleNotiList(sessionId, json, requestId, empNo);   break;
            case "NOTI_READ":   handleNotiRead(sessionId, json, requestId, empNo);   break;
            case "NOTI_DELETE": handleNotiDelete(sessionId, json, requestId, empNo); break;
            default:
                emit(sessionId, JsonParser.build(
                        "type","RESP","requestId",requestId,"status","ERR","code","BAD_REQUEST"
                ));
        }
    }

    // =========================================================
    // 핸들러 메서드
    // =========================================================

    void handleAuth(String sessionId, String json, String requestId) {
        String empNo = JsonParser.getStr(json, "employeeNumber");
        if (empNo == null || empNo.isEmpty()) {
            emit(sessionId, JsonParser.build(
                    "type","RESP","requestId",requestId,"status","ERR","code","BAD_REQUEST"
            ));
            return;
        }
        sessionToEmp.put(sessionId, empNo);
        empToSessions.computeIfAbsent(empNo, k -> new ArrayList<>()).add(sessionId);
        emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","OK"));
    }

    void handleDocCreate(String sessionId, String json, String requestId, String empNo) {
        String title      = JsonParser.getStr(json, "title");
        String content    = JsonParser.getStr(json, "content");
        String[] approvers = JsonParser.getStrArray(json, "approvers");

        // 유효성 검사
        if (title == null || title.isEmpty() || title.length() > 50
                || approvers.length < 1 || approvers.length > 5) {
            emit(sessionId, JsonParser.build(
                    "type","RESP","requestId",requestId,"status","ERR","code","BAD_REQUEST"
            ));
            return;
        }
        if (content == null) content = "";

        Document doc = new Document(nextDocId++, title, content, empNo, approvers, System.currentTimeMillis());
        documents.put(doc.docId, doc);

        emit(sessionId, JsonParser.build(
                "type","RESP","requestId",requestId,"status","OK",
                "docId", doc.docId,
                "ver", doc.ver
        ));
    }

    void handleDocRead(String sessionId, String json, String requestId, String empNo) {
        int docId = JsonParser.getInt(json, "docId");
        Document doc = documents.get(docId);

        if (doc == null || "DELETED".equals(doc.status)) {
            emit(sessionId, JsonParser.build(
                    "type","RESP","requestId",requestId,"status","ERR","code","NOT_FOUND"
            ));
            return;
        }
        if (!doc.authorEmpNo.equals(empNo) && !isApprover(doc, empNo)) {
            emit(sessionId, JsonParser.build(
                    "type","RESP","requestId",requestId,"status","ERR","code","FORBIDDEN"
            ));
            return;
        }

        emit(sessionId, "{\"type\":\"RESP\",\"requestId\":\"" + requestId
                + "\",\"status\":\"OK\",\"doc\":" + docToJson(doc) + "}");
    }

    void handleDocUpdate(String sessionId, String json, String requestId, String empNo) {
        int docId = JsonParser.getInt(json, "docId");
        int ver   = JsonParser.getInt(json, "ver");
        Document doc = documents.get(docId);

        if (doc == null || "DELETED".equals(doc.status)) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","NOT_FOUND")); return;
        }
        if (!doc.authorEmpNo.equals(empNo)) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","FORBIDDEN")); return;
        }
        if (!"DRAFT".equals(doc.status)) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","BAD_STATUS")); return;
        }
        if (ver != doc.ver) {
            emit(sessionId, "{\"type\":\"RESP\",\"requestId\":\"" + requestId
                    + "\",\"status\":\"ERR\",\"code\":\"CONFLICT\",\"curVer\":" + doc.ver + "}");
            return;
        }

        String newTitle   = JsonParser.getStr(json, "title");
        String newContent = JsonParser.getStr(json, "content");
        if (newTitle   != null) doc.title   = newTitle;
        if (newContent != null) doc.content = newContent;
        doc.ver++;
        doc.updatedAt = System.currentTimeMillis();

        emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","OK","ver",doc.ver));
    }

    void handleDocDelete(String sessionId, String json, String requestId, String empNo) {
        int docId = JsonParser.getInt(json, "docId");
        int ver   = JsonParser.getInt(json, "ver");
        Document doc = documents.get(docId);

        if (doc == null || "DELETED".equals(doc.status)) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","NOT_FOUND")); return;
        }
        if (!doc.authorEmpNo.equals(empNo)) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","FORBIDDEN")); return;
        }
        if (!"DRAFT".equals(doc.status)) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","BAD_STATUS")); return;
        }
        if (ver != doc.ver) {
            emit(sessionId, "{\"type\":\"RESP\",\"requestId\":\"" + requestId
                    + "\",\"status\":\"ERR\",\"code\":\"CONFLICT\",\"curVer\":" + doc.ver + "}");
            return;
        }

        doc.status = "DELETED";
        doc.ver++;
        doc.updatedAt = System.currentTimeMillis();

        emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","OK","ver",doc.ver));
    }

    void handleDocSubmit(String sessionId, String json, String requestId, String empNo) {
        int docId = JsonParser.getInt(json, "docId");
        int ver   = JsonParser.getInt(json, "ver");
        Document doc = documents.get(docId);

        if (doc == null || "DELETED".equals(doc.status)) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","NOT_FOUND")); return;
        }
        if (!doc.authorEmpNo.equals(empNo)) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","FORBIDDEN")); return;
        }
        if (!"DRAFT".equals(doc.status)) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","BAD_STATUS")); return;
        }
        if (ver != doc.ver) {
            emit(sessionId, "{\"type\":\"RESP\",\"requestId\":\"" + requestId
                    + "\",\"status\":\"ERR\",\"code\":\"CONFLICT\",\"curVer\":" + doc.ver + "}");
            return;
        }

        doc.status     = "IN_REVIEW";
        doc.currentIdx = 0;
        doc.ver++;
        doc.updatedAt  = System.currentTimeMillis();

        emit(sessionId, "{\"type\":\"RESP\",\"requestId\":\"" + requestId
                + "\",\"status\":\"OK\",\"ver\":" + doc.ver
                + ",\"statusAfter\":\"IN_REVIEW\",\"currentIdx\":0}");

        // 첫 결재자에게 알림 PUSH
        createNotification(doc.approvers[0], "DOC_SUBMITTED", doc.docId,
                "문서 #" + doc.docId + " 결재 요청");
    }

    void handleDocApprove(String sessionId, String json, String requestId, String empNo) {
        int docId = JsonParser.getInt(json, "docId");
        int ver   = JsonParser.getInt(json, "ver");
        Document doc = documents.get(docId);

        if (doc == null || "DELETED".equals(doc.status)) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","NOT_FOUND")); return;
        }
        if (!"IN_REVIEW".equals(doc.status)) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","BAD_STATUS")); return;
        }
        if (!doc.approvers[doc.currentIdx].equals(empNo)) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","FORBIDDEN")); return;
        }
        if (ver != doc.ver) {
            emit(sessionId, "{\"type\":\"RESP\",\"requestId\":\"" + requestId
                    + "\",\"status\":\"ERR\",\"code\":\"CONFLICT\",\"curVer\":" + doc.ver + "}");
            return;
        }

        doc.ver++;
        doc.updatedAt = System.currentTimeMillis();

        if (doc.currentIdx + 1 < doc.approvers.length) {
            // 다음 결재자 존재 → 인덱스 증가
            doc.currentIdx++;
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","OK","ver",doc.ver));
            createNotification(doc.approvers[doc.currentIdx], "DOC_NEXT_APPROVER", doc.docId,
                    "문서 #" + doc.docId + " 결재 요청");
        } else {
            // 최종 승인
            doc.status = "APPROVED";
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","OK","ver",doc.ver));
            createNotification(doc.authorEmpNo, "DOC_APPROVED", doc.docId,
                    "문서 #" + doc.docId + " 최종 승인");
        }
    }

    void handleDocReject(String sessionId, String json, String requestId, String empNo) {
        int docId = JsonParser.getInt(json, "docId");
        int ver   = JsonParser.getInt(json, "ver");
        Document doc = documents.get(docId);

        if (doc == null || "DELETED".equals(doc.status)) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","NOT_FOUND")); return;
        }
        if (!"IN_REVIEW".equals(doc.status)) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","BAD_STATUS")); return;
        }
        if (!doc.approvers[doc.currentIdx].equals(empNo)) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","FORBIDDEN")); return;
        }
        if (ver != doc.ver) {
            emit(sessionId, "{\"type\":\"RESP\",\"requestId\":\"" + requestId
                    + "\",\"status\":\"ERR\",\"code\":\"CONFLICT\",\"curVer\":" + doc.ver + "}");
            return;
        }

        doc.status = "REJECTED";
        doc.ver++;
        doc.updatedAt = System.currentTimeMillis();

        emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","OK","ver",doc.ver));
        createNotification(doc.authorEmpNo, "DOC_REJECTED", doc.docId,
                "문서 #" + doc.docId + " 반려");
    }

    void handleNotiList(String sessionId, String json, String requestId, String empNo) {
        int limit       = JsonParser.getInt(json, "limit");
        boolean onlyUnread = JsonParser.getBool(json, "onlyUnread");
        if (limit <= 0) limit = 20;

        // 내 알림 필터링
        List<Notification> myNotis = new ArrayList<>();
        for (Notification n : notifications.values()) {
            if (n.empNo.equals(empNo)) {
                if (!onlyUnread || !n.read) {
                    myNotis.add(n);
                }
            }
        }

        // createdAt 내림차순 정렬
        myNotis.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));

        // limit 적용 후 JSON 배열 직렬화
        StringBuilder arr = new StringBuilder("[");
        int count = Math.min(limit, myNotis.size());
        for (int i = 0; i < count; i++) {
            if (i > 0) arr.append(",");
            arr.append(notiToJson(myNotis.get(i)));
        }
        arr.append("]");

        emit(sessionId, "{\"type\":\"RESP\",\"requestId\":\"" + requestId
                + "\",\"status\":\"OK\",\"notifications\":" + arr + "}");
    }

    void handleNotiRead(String sessionId, String json, String requestId, String empNo) {
        long notiId = JsonParser.getInt(json, "notiId");
        Notification noti = notifications.get(notiId);

        if (noti == null) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","NOT_FOUND")); return;
        }
        if (!noti.empNo.equals(empNo)) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","FORBIDDEN")); return;
        }

        noti.read = true;
        emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","OK"));
    }

    void handleNotiDelete(String sessionId, String json, String requestId, String empNo) {
        long notiId = JsonParser.getInt(json, "notiId");
        Notification noti = notifications.get(notiId);

        if (noti == null) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","NOT_FOUND")); return;
        }
        if (!noti.empNo.equals(empNo)) {
            emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","ERR","code","FORBIDDEN")); return;
        }

        notifications.remove(notiId);
        emit(sessionId, JsonParser.build("type","RESP","requestId",requestId,"status","OK"));
    }

    // =========================================================
    // 헬퍼 메서드
    // =========================================================

    /**
     * 알림 생성 + 저장 + 대상자 모든 세션에 PUSH
     *
     * 예시 PUSH JSON:
     * {"type":"PUSH","event":"NOTI_CREATED","noti":{
     *   "notiId":1001,"empNo":"E2001","type":"DOC_SUBMITTED",
     *   "refDocId":1,"message":"문서 #1 결재 요청","read":false
     * }}
     */
    Notification createNotification(String targetEmpNo, String type, int refDocId, String message) {
        Notification noti = new Notification(
                nextNotiId++, targetEmpNo, type, refDocId, message, System.currentTimeMillis()
        );
        notifications.put(noti.notiId, noti);

        String pushJson = "{\"type\":\"PUSH\",\"event\":\"NOTI_CREATED\",\"noti\":"
                + notiToJson(noti) + "}";
        emitToEmployee(targetEmpNo, pushJson);

        return noti;
    }

    /**
     * Document를 JSON 문자열로 직렬화
     * approvers 배열도 포함해야 합니다.
     *
     * 예: {"docId":1,"title":"휴가 신청","content":"...","authorEmpNo":"E1024",
     *       "approvers":["E2001","E2002"],"status":"DRAFT","currentIdx":-1,"ver":1}
     */
    String docToJson(Document doc) {
        // approvers 배열 → JSON 배열 문자열
        StringBuilder approversJson = new StringBuilder("[");
        for (int i = 0; i < doc.approvers.length; i++) {
            if (i > 0) approversJson.append(",");
            approversJson.append("\"").append(doc.approvers[i]).append("\"");
        }
        approversJson.append("]");

        return "{\"docId\":" + doc.docId
                + ",\"title\":\"" + doc.title + "\""
                + ",\"content\":\"" + doc.content + "\""
                + ",\"authorEmpNo\":\"" + doc.authorEmpNo + "\""
                + ",\"approvers\":" + approversJson
                + ",\"status\":\"" + doc.status + "\""
                + ",\"currentIdx\":" + doc.currentIdx
                + ",\"ver\":" + doc.ver
                + "}";
    }

    /**
     * Notification을 JSON 문자열로 직렬화
     */
    String notiToJson(Notification noti) {
        return "{\"notiId\":" + noti.notiId
                + ",\"empNo\":\"" + noti.empNo + "\""
                + ",\"type\":\"" + noti.type + "\""
                + ",\"refDocId\":" + noti.refDocId
                + ",\"message\":\"" + noti.message + "\""
                + ",\"read\":" + noti.read
                + "}";
    }

    /**
     * 특정 empNo가 해당 문서의 결재자 목록에 포함되는지 확인
     */
    boolean isApprover(Document doc, String empNo) {
        for (String a : doc.approvers) {
            if (a.equals(empNo)) return true;
        }
        return false;
    }
}
