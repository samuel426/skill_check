import java.util.*;
import java.io.*;

/**
 * =====================================================================
 * [문제 3] 팀 빌딩 (정렬 + 우선순위 + 예외처리) - 예상 소요 시간: 50분
 * =====================================================================
 *
 * 회사에서 사원 목록을 받아 팀을 구성하려 합니다.
 * 입력된 사원 정보를 조건에 맞게 정렬/필터링하여 팀을 출력하세요.
 *
 * -------------------------------------------------------------------
 * [입력 형식]
 *
 * 첫째 줄: 사원 수 N (1 ≤ N ≤ 1000)
 * 이후 N줄: <이름> <부서> <연차> <점수>
 *   - 이름: 영문 소문자 (중복 없음)
 *   - 부서: "dev", "design", "pm" 중 하나
 *   - 연차: 1~20 정수
 *   - 점수: 0~100 정수
 * 마지막 줄: 팀 구성 조건
 *   "SELECT <부서> TOP <K>"  →  해당 부서에서 상위 K명 선발
 *   "SELECT ALL TOP <K>"     →  전체에서 상위 K명 선발
 *
 * -------------------------------------------------------------------
 * [선발 우선순위]
 * 1순위: 점수 내림차순
 * 2순위: 연차 내림차순
 * 3순위: 이름 오름차순
 *
 * -------------------------------------------------------------------
 * [출력 형식]
 * 선발된 K명을 순서대로 한 줄씩 출력:
 *   "<순위>. <이름> (<부서>, <연차>년차, <점수>점)"
 * 선발 가능한 인원이 K보다 적으면 가능한 만큼만 출력합니다.
 *
 * -------------------------------------------------------------------
 * [입력 예시]
 *   6
 *   alice dev 5 88
 *   bob design 3 92
 *   carol pm 7 88
 *   dave dev 5 88
 *   eve design 10 75
 *   frank dev 2 92
 *   SELECT dev TOP 3
 *
 * [출력 예시]
 *   1. frank (dev, 2년차, 92점)
 *   2. alice (dev, 5년차, 88점)
 *   3. dave (dev, 5년차, 88점)
 *
 * [다른 예시]
 *   SELECT ALL TOP 3
 * [출력]
 *   1. bob (design, 3년차, 92점)
 *   2. frank (dev, 2년차, 92점)
 *   3. alice (dev, 5년차, 88점)
 *
 * -------------------------------------------------------------------
 * [힌트]
 * - Employee 클래스(또는 int[] 배열)로 사원 정보 저장
 * - Comparator 체이닝: 점수 내림차순 → 연차 내림차순 → 이름 오름차순
 * - SELECT ALL vs SELECT <부서> 분기 처리
 * =====================================================================
 */
public class Problem3 {

    // ※ 채점을 위해 아래 클래스 필드를 수정하지 마세요.
    static class Employee {
        String name;
        String dept;
        int year;
        int score;

        Employee(String name, String dept, int year, int score) {
            this.name = name;
            this.dept = dept;
            this.year = year;
            this.score = score;
        }
    }

    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder sb = new StringBuilder();

        int N = Integer.parseInt(br.readLine().trim());
        List<Employee> employees = new ArrayList<>();

        for (int i = 0; i < N; i++) {
            StringTokenizer st = new StringTokenizer(br.readLine());
            String name = st.nextToken();
            String dept = st.nextToken();
            int year    = Integer.parseInt(st.nextToken());
            int score   = Integer.parseInt(st.nextToken());
            employees.add(new Employee(name, dept, year, score));
        }

        // condition 파싱: "SELECT <부서|ALL> TOP <K>"
        StringTokenizer st = new StringTokenizer(br.readLine());
        st.nextToken(); // "SELECT" 버림
        String deptFilter = st.nextToken(); // "ALL" 또는 부서명
        st.nextToken(); // "TOP" 버림
        int K = Integer.parseInt(st.nextToken());

        // 필터링
        List<Employee> filtered = new ArrayList<>();
        for (Employee e : employees) {
            if ("ALL".equals(deptFilter) || deptFilter.equals(e.dept)) {
                filtered.add(e);
            }
        }

        // 정렬
        filtered.sort(getComparator());

        // 상위 K명 출력
        int limit = Math.min(K, filtered.size());
        for (int i = 0; i < limit; i++) {
            Employee e = filtered.get(i);
            sb.append(i + 1).append(". ")
              .append(e.name).append(" (")
              .append(e.dept).append(", ")
              .append(e.year).append("년차, ")
              .append(e.score).append("점)\n");
        }

        System.out.print(sb);
    }

    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
    static Comparator<Employee> getComparator() {
        // 1순위: 점수 내림차순
        // 2순위: 연차 내림차순
        // 3순위: 이름 오름차순
        return (a, b) -> {
            if (a.score != b.score) return b.score - a.score;
            if (a.year  != b.year)  return b.year  - a.year;
            return a.name.compareTo(b.name);
        };
    }
}
