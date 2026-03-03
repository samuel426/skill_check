package problem4.domain;

/**
 * 알림 도메인 클래스
 *
 * ※ 채점을 위해 아래 클래스와 필드를 수정하지 마세요.
 */
public class Notification {
    public long notiId;
    public String empNo;         // 수신자 사번
    public String type;          // "DOC_SUBMITTED" | "DOC_APPROVED" | "DOC_REJECTED" | "DOC_NEXT_APPROVER"
    public int refDocId;
    public String message;       // 길이 <= 200
    public boolean read;         // 기본 false
    public long createdAt;

    public Notification(long notiId, String empNo, String type,
                        int refDocId, String message, long now) {
        this.notiId = notiId;
        this.empNo = empNo;
        this.type = type;
        this.refDocId = refDocId;
        this.message = message;
        this.read = false;
        this.createdAt = now;
    }
}
