package day2;

import java.util.*;
import java.io.*;

/**
 * =====================================================================
 * [문제 6] 사무실 네트워크 복구 (BFS) - 예상 소요 시간: 60분
 * =====================================================================
 *
 * N개의 서버가 있고, 일부 서버 쌍이 네트워크 케이블로 연결되어 있습니다.
 * 재난으로 일부 서버가 다운되었을 때, 아래 쿼리들을 처리하세요.
 *
 * -------------------------------------------------------------------
 * [입력 형식]
 *
 * 첫째 줄: N M       (서버 수, 케이블 수, 1 ≤ N ≤ 500, 0 ≤ M ≤ 5000)
 * 이후 M줄: u v      (서버 u와 v가 연결됨, 양방향, 1-indexed)
 * 다음 줄: K         (쿼리 수)
 * 이후 K줄: 쿼리
 *
 * -------------------------------------------------------------------
 * [쿼리 종류]
 *
 * 1) DOWN server=<번호>
 *    - 해당 서버를 다운 처리합니다.
 *    - 다운된 서버는 이후 탐색에서 제외됩니다.
 *    - 이미 다운된 서버에 DOWN을 하면 무시합니다.
 *
 * 2) UP server=<번호>
 *    - 다운된 서버를 복구합니다.
 *    - 정상 서버에 UP을 하면 무시합니다.
 *
 * 3) REACHABLE from=<번호> to=<번호>
 *    - from 서버에서 to 서버까지 정상 서버들만 거쳐 도달 가능한지 출력
 *    - 가능하면 "YES", 불가능하면 "NO"
 *    - from 또는 to 자체가 다운된 경우 "NO"
 *
 * 4) COUNT from=<번호>
 *    - from 서버에서 도달 가능한 정상 서버의 수 출력 (from 자신 제외)
 *    - from이 다운된 경우 0 출력
 *
 * -------------------------------------------------------------------
 * [입력 예시]
 *   5 5
 *   1 2
 *   2 3
 *   3 4
 *   4 5
 *   1 3
 *   4
 *   REACHABLE from=1 to=5
 *   DOWN server=3
 *   REACHABLE from=1 to=5
 *   COUNT from=1
 *
 * [출력 예시]
 *   YES
 *   NO
 *   1
 *
 * -------------------------------------------------------------------
 * [힌트]
 * - 인접 리스트: List<List<Integer>> 또는 Map<Integer, List<Integer>>
 * - BFS: ArrayDeque<Integer> 사용, visited 배열로 중복 방지
 * - DOWN된 서버: boolean[] down 배열
 * - REACHABLE은 BFS 도중 to를 만나면 즉시 YES 반환 가능
 * - COUNT는 BFS로 방문한 노드 수 - 1 (자신 제외)
 * =====================================================================
 */
public class Problem6 {

    // ※ 채점을 위해 아래 필드를 수정하지 마세요.
    static int N, M;
    static List<List<Integer>> graph;
    static boolean[] down;

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder sb = new StringBuilder();
        StringTokenizer st;

        // 1. N, M 입력
        st = new StringTokenizer(br.readLine());
        N = Integer.parseInt(st.nextToken());
        M = Integer.parseInt(st.nextToken());

        // 여기에 코드를 작성하세요.
        // 2. 그래프 초기화 (1-indexed이므로 크기 N+1)


        // 3. 간선 입력


        // 4. 다운 상태 배열 초기화


        // 5. 쿼리 처리
        int K = Integer.parseInt(br.readLine().trim());
        for (int i = 0; i < K; i++) {
            String line = br.readLine().trim();
            String[] tokens = line.split(" ");
            String cmd = tokens[0];

            if ("DOWN".equals(cmd)) {
                // TODO

            } else if ("UP".equals(cmd)) {
                // TODO

            } else if ("REACHABLE".equals(cmd)) {
                // TODO: bfsReachable() 호출 후 결과 sb에 추가

            } else if ("COUNT".equals(cmd)) {
                // TODO: bfsCount() 호출 후 결과 sb에 추가

            }
        }

        System.out.print(sb);
    }

    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
    static boolean bfsReachable(int from, int to) {
        // 여기에 코드를 작성하세요.
        // from 또는 to가 다운이면 false
        // BFS로 from → to 도달 가능 여부 반환
        return false;
    }

    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
    static int bfsCount(int from) {
        // 여기에 코드를 작성하세요.
        // from이 다운이면 0
        // BFS로 from에서 도달 가능한 정상 서버 수 반환 (from 자신 제외)
        return 0;
    }

    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
    static Map<String, String> parseParams(String[] tokens) {
        Map<String, String> map = new HashMap<>();
        for (int i = 1; i < tokens.length; i++) {
            String[] kv = tokens[i].split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }
}
