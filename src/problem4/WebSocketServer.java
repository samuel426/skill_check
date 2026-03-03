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
        // 여기에 코드를 작성하세요.
        // empToSessions에서 empNo의 세션 목록을 꺼내 각각 emit() 호출
        // 해당 empNo가 현재 미접속 상태면 그냥 무시 (알림 객체는 저장됨)
    }

    // =========================================================

    /** 세션 연결 이벤트 */
    void onOpen(String sessionId) {
        // 여기에 코드를 작성하세요.
        sessionToEmp.put(sessionId, null);
    }

    /** 세션 종료 이벤트 */
    void onClose(String sessionId) {
        // 여기에 코드를 작성하세요.
        // 1. sessionToEmp에서 empNo 꺼내기
        // 2. empToSessions에서 해당 세션 제거
        // 3. sessionToEmp에서 sessionId 제거
    }

    /** 메시지 수신 이벤트 — 여기가 핵심 구현 영역 */
    void onMessage(String sessionId, String json) {
        String type = JsonParser.getStr(json, "type");
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
            case "DOC_CREATE":  handleDocCreate(sessionId, json, requestId, empNo); break;
            case "DOC_READ":    handleDocRead(sessionId, json, requestId, empNo);   break;
            case "DOC_UPDATE":  handleDocUpdate(sessionId, json, requestId, empNo); break;
            case "DOC_DELETE":  handleDocDelete(sessionId, json, requestId, empNo); break;
            case "DOC_SUBMIT":  handleDocSubmit(sessionId, json, requestId, empNo); break;
            case "DOC_APPROVE": handleDocApprove(sessionId, json, requestId, empNo); break;
            case "DOC_REJECT":  handleDocReject(sessionId, json, requestId, empNo); break;
            case "NOTI_LIST":   handleNotiList(sessionId, json, requestId, empNo);  break;
            case "NOTI_READ":   handleNotiRead(sessionId, json, requestId, empNo);  break;
            case "NOTI_DELETE": handleNotiDelete(sessionId, json, requestId, empNo); break;
            default:
                emit(sessionId, JsonParser.build(
                        "type","RESP","requestId",requestId,"status","ERR","code","BAD_REQUEST"
                ));
        }
    }

    // =========================================================
    // 핸들러 메서드 (각 TODO 구현)
    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
    // =========================================================

    void handleAuth(String sessionId, String json, String requestId) {
        // 여기에 코드를 작성하세요.
        // 1. json에서 "employeeNumber" 파싱
        // 2. sessionToEmp에 저장
        // 3. empToSessions에 sessionId 추가 (없으면 새 리스트 생성)
        // 4. OK 응답 emit
    }

    void handleDocCreate(String sessionId, String json, String requestId, String empNo) {
        // 여기에 코드를 작성하세요.
        // 1. title, content, approvers 파싱 (title 1~50, approvers 1~5명 검증)
        // 2. Document 생성 후 documents에 저장
        // 3. {"type":"RESP","requestId":"...","status":"OK","docId":N,"ver":1} emit
    }

    void handleDocRead(String sessionId, String json, String requestId, String empNo) {
        // 여기에 코드를 작성하세요.
        // 1. docId 파싱
        // 2. documents에서 조회 → 없으면 NOT_FOUND
        // 3. status == "DELETED" → NOT_FOUND
        // 4. 권한 체크: authorEmpNo == empNo || approvers에 empNo 포함 → 아니면 FORBIDDEN
        // 5. doc 전체 필드를 JSON으로 직렬화해서 emit
        // 힌트: approvers 배열을 JSON 배열 문자열로 변환하는 헬퍼 만들기
    }

    void handleDocUpdate(String sessionId, String json, String requestId, String empNo) {
        // 여기에 코드를 작성하세요.
        // 1. docId, ver, title(선택), content(선택) 파싱
        // 2. 문서 조회 → 없거나 DELETED → NOT_FOUND
        // 3. 권한: authorEmpNo != empNo → FORBIDDEN
        // 4. status != "DRAFT" → BAD_STATUS
        // 5. ver != doc.ver → CONFLICT + curVer 반환
        // 6. 필드 업데이트 + ver++ + updatedAt 갱신
        // 7. OK + 새 ver 반환
    }

    void handleDocDelete(String sessionId, String json, String requestId, String empNo) {
        // 여기에 코드를 작성하세요.
        // handleDocUpdate와 유사하지만 status를 "DELETED"로 변경 (soft delete)
    }

    void handleDocSubmit(String sessionId, String json, String requestId, String empNo) {
        // 여기에 코드를 작성하세요.
        // 1. docId, ver 파싱
        // 2. 문서 조회 + 권한 + DRAFT 체크 + ver 체크
        // 3. status = "IN_REVIEW", currentIdx = 0, ver++ 갱신
        // 4. 작성자에게 OK RESP emit (statusAfter, currentIdx 포함)
        // 5. createNotification() 호출 → 첫 결재자(approvers[0])에게 "DOC_SUBMITTED" PUSH
    }

    void handleDocApprove(String sessionId, String json, String requestId, String empNo) {
        // 여기에 코드를 작성하세요.
        // 1. docId, ver 파싱
        // 2. 문서 조회, status == IN_REVIEW 체크, ver 체크
        // 3. 권한: approvers[currentIdx] == empNo 아니면 FORBIDDEN
        // 4. ver++ 갱신
        // 5. 분기:
        //    a) currentIdx + 1 < approvers.length
        //       → currentIdx++ 유지(IN_REVIEW), 다음 결재자에게 DOC_NEXT_APPROVER PUSH
        //    b) 마지막 결재자
        //       → status = "APPROVED", 작성자에게 DOC_APPROVED PUSH
        // 6. 승인자에게 OK RESP emit
    }

    void handleDocReject(String sessionId, String json, String requestId, String empNo) {
        // 여기에 코드를 작성하세요.
        // handleDocApprove와 유사하지만 status = "REJECTED"
        // 작성자에게 DOC_REJECTED PUSH
    }

    void handleNotiList(String sessionId, String json, String requestId, String empNo) {
        // 여기에 코드를 작성하세요.
        // 1. limit(기본 20), onlyUnread(기본 false) 파싱
        // 2. notifications.values() 중 noti.empNo == empNo 필터
        // 3. onlyUnread == true면 !noti.read 추가 필터
        // 4. createdAt 내림차순 정렬
        // 5. limit 개수만큼 잘라서 JSON 배열로 직렬화 emit
    }

    void handleNotiRead(String sessionId, String json, String requestId, String empNo) {
        // 여기에 코드를 작성하세요.
        // 1. notiId 파싱
        // 2. 알림 조회 → 없으면 NOT_FOUND
        // 3. noti.empNo != empNo → FORBIDDEN
        // 4. noti.read = true
        // 5. OK 응답
    }

    void handleNotiDelete(String sessionId, String json, String requestId, String empNo) {
        // 여기에 코드를 작성하세요.
        // 1. notiId 파싱
        // 2. 알림 조회 → 없으면 NOT_FOUND
        // 3. noti.empNo != empNo → FORBIDDEN
        // 4. notifications에서 제거
        // 5. OK 응답
    }

    // =========================================================
    // 헬퍼 메서드
    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
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
        // 여기에 코드를 작성하세요.
        // 1. Notification 객체 생성 (nextNotiId++ 사용)
        // 2. notifications에 저장
        // 3. PUSH JSON 만들어서 emitToEmployee(targetEmpNo, ...) 호출
        // 4. 생성된 Notification 반환
        return null;
    }

    /**
     * Document를 JSON 문자열로 직렬화
     * approvers 배열도 포함해야 합니다.
     *
     * 예: {"docId":1,"title":"휴가 신청","content":"...","authorEmpNo":"E1024",
     *       "approvers":["E2001","E2002"],"status":"DRAFT","currentIdx":-1,"ver":1}
     */
    String docToJson(Document doc) {
        // 여기에 코드를 작성하세요.
        // approvers 배열 → ["E2001","E2002"] 형태로 변환 후 전체 JSON 조립
        return null;
    }

    /**
     * Notification을 JSON 문자열로 직렬화
     */
    String notiToJson(Notification noti) {
        // 여기에 코드를 작성하세요.
        return null;
    }

    /**
     * 특정 empNo가 해당 문서의 결재자 목록에 포함되는지 확인
     */
    boolean isApprover(Document doc, String empNo) {
        // 여기에 코드를 작성하세요.
        return false;
    }
}
