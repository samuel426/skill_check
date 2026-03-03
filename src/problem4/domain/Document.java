package problem4.domain;

/**
 * 전자결재 문서 도메인 클래스
 *
 * ※ 채점을 위해 아래 클래스와 필드를 수정하지 마세요.
 */
public class Document {
    public int docId;
    public String title;
    public String content;
    public String authorEmpNo;
    public String[] approvers;   // 결재자 사번 목록 (1~5명)
    public String status;        // "DRAFT" | "IN_REVIEW" | "APPROVED" | "REJECTED" | "DELETED"
    public int currentIdx;       // 현재 결재자 인덱스 (-1: 결재 전)
    public int ver;
    public long createdAt;
    public long updatedAt;

    public Document(int docId, String title, String content,
                    String authorEmpNo, String[] approvers, long now) {
        this.docId = docId;
        this.title = title;
        this.content = content;
        this.authorEmpNo = authorEmpNo;
        this.approvers = approvers;
        this.status = "DRAFT";
        this.currentIdx = -1;
        this.ver = 1;
        this.createdAt = now;
        this.updatedAt = now;
    }
}
