import java.util.*;
import java.io.*;

/**
 * =====================================================================
 * [문제 1] 학생 성적 관리 시스템 (문자열 파싱 + Map) - 예상 소요 시간: 60분
 * =====================================================================
 *
 * 당신은 학생 성적 관리 프로그램을 만들어야 합니다.
 * 표준 입력으로 명령어 문자열이 주어지며, 각 명령어를 순서대로 처리한 뒤
 * PRINT 명령어가 오면 조건에 맞게 결과를 출력해야 합니다.
 *
 * -------------------------------------------------------------------
 * [명령어 형식]
 *
 * 1) ADD name=<이름> subject=<과목> score=<점수>
 *    - 해당 학생의 과목 점수를 등록합니다.
 *    - 같은 학생, 같은 과목의 점수가 이미 있으면 덮어씁니다.
 *    - 이름과 과목은 영문 소문자, 점수는 0~100 정수입니다.
 *
 * 2) DELETE name=<이름> subject=<과목>
 *    - 해당 학생의 해당 과목 점수를 삭제합니다.
 *    - 존재하지 않는 경우 무시합니다.
 *
 * 3) PRINT order=<정렬기준>
 *    - 현재 등록된 모든 학생의 평균 점수를 출력합니다.
 *    - order=avg_desc  : 평균 점수 내림차순 (같으면 이름 오름차순)
 *    - order=name_asc  : 이름 오름차순
 *    - 출력 형식: "<이름> <평균점수>" (평균은 소수점 첫째 자리, 반올림)
 *    - 등록된 학생이 없으면 "EMPTY"를 출력합니다.
 *
 * -------------------------------------------------------------------
 * [입력 예시]
 *   ADD name=alice subject=math score=90
 *   ADD name=alice subject=english score=80
 *   ADD name=bob subject=math score=70
 *   ADD name=alice subject=math score=95
 *   DELETE name=bob subject=science
 *   PRINT order=avg_desc
 *   DELETE name=alice subject=english
 *   PRINT order=name_asc
 *
 * [출력 예시]
 *   alice 87.5
 *   bob 70.0
 *   alice 95.0
 *   bob 70.0
 *
 * -------------------------------------------------------------------
 * [힌트]
 * - 자료구조: Map<String, Map<String, Integer>> (학생명 -> 과목 -> 점수)
 * - 파싱: split(" ") 후 각 토큰을 split("=", 2)
 * - 평균 계산: values()를 stream 없이 for-each로 합산
 * - 정렬: List<Map.Entry> 또는 별도 클래스 만들어서 Comparator 적용
 * =====================================================================
 */
public class Problem1 {

    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder sb = new StringBuilder();
        String line;

        // 여기에 코드를 작성하세요.
        // 데이터 저장 구조 선언


        while ((line = br.readLine()) != null && !line.isEmpty()) {
            line = line.trim();
            if (line.startsWith("ADD")) {
                // TODO: ADD 명령어 처리

            } else if (line.startsWith("DELETE")) {
                // TODO: DELETE 명령어 처리

            } else if (line.startsWith("PRINT")) {
                // TODO: PRINT 명령어 처리 후 sb에 append

            }
        }

        System.out.print(sb);
    }

    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
    static Map<String, String> parseParams(String[] tokens) {
        // 여기에 코드를 작성하세요.
        // "key=value" 형태의 토큰 배열을 파싱해서 Map으로 반환
        return null;
    }
}
