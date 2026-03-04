import java.util.*;
import java.io.*;

/**
 * =====================================================================
 * [문제 2] 창고 로봇 시뮬레이션 (구현/시뮬레이션) - 예상 소요 시간: 70분
 * =====================================================================
 *
 * N×M 격자 창고에서 로봇이 명령어에 따라 이동하며 박스를 수집합니다.
 * 규칙을 정확히 구현하여 최종 상태를 출력하세요.
 *
 * -------------------------------------------------------------------
 * [입력 형식]
 *
 * 첫째 줄: N M          (격자 크기, 1 ≤ N,M ≤ 20)
 * 둘째 줄: SR SC        (로봇 시작 위치, 0-indexed)
 * 셋째 줄: 격자 초기 상태 (N줄, 공백으로 구분된 M개 숫자)
 *           0 = 빈 칸, 양수 = 박스 (숫자가 박스 개수)
 * 다음 줄: 명령어 수 K
 * 이후 K줄: 명령어 (U/D/L/R 중 하나)
 *
 * -------------------------------------------------------------------
 * [이동 규칙]
 * 1. 로봇은 명령어 방향으로 한 칸 이동합니다.
 * 2. 격자 밖으로 나가는 이동은 무시합니다. (이동 안 함)
 * 3. 이동 후 해당 칸에 박스가 있으면 전부 수집합니다. (해당 칸은 0이 됩니다)
 * 4. 이동을 무시한 경우에도 박스 수집 시도는 하지 않습니다.
 *
 * -------------------------------------------------------------------
 * [출력 형식]
 * 첫째 줄: 로봇이 수집한 총 박스 수
 * 둘째 줄: 최종 로봇 위치 (행 열, 0-indexed)
 * 셋째 줄~: 최종 격자 상태 (N줄, 공백으로 구분, 원본 형식 유지)
 *
 * -------------------------------------------------------------------
 * [입력 예시]
 *   4 4
 *   0 0
 *   0 3 0 2
 *   1 0 4 0
 *   0 2 0 1
 *   0 0 0 0
 *   6
 *   R
 *   D
 *   R
 *   U
 *   L
 *   D
 *
 * [출력 예시]
 *   9
 *   1 1
 *   0 0 0 2
 *   0 0 4 0
 *   0 0 0 1
 *   0 0 0 0
 *
 * -------------------------------------------------------------------
 * [힌트]
 * - 방향 배열: int[] dr = {-1,1,0,0}, dc = {0,0,-1,1} (U,D,L,R 순서)
 * - 격자 범위 체크: 0 <= nr < N && 0 <= nc < M
 * - 처리 순서: 이동 가능 여부 확인 → 이동 → 박스 수집
 * =====================================================================
 */
public class Problem2 {

    // ※ 채점을 위해 아래 필드와 메서드 시그니처를 수정하지 마세요.
    static int N, M;
    static int[][] grid;
    static int robotR, robotC;
    static int collected = 0;

    // 방향: U, D, L, R
    static int[] dr = {-1, 1, 0, 0};
    static int[] dc = {0, 0, -1, 1};

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        StringTokenizer st;
        StringBuilder sb = new StringBuilder();

        // 1. N, M 입력
        st = new StringTokenizer(br.readLine());
        N = Integer.parseInt(st.nextToken());
        M = Integer.parseInt(st.nextToken());

        // 2. 로봇 시작 위치 입력
        st = new StringTokenizer(br.readLine());
        robotR = Integer.parseInt(st.nextToken());
        robotC = Integer.parseInt(st.nextToken());

        // 3. 격자 초기 상태 입력
        grid = new int[N][M];
        for (int i = 0; i < N; i++) {
            st = new StringTokenizer(br.readLine());
            for (int j = 0; j < M; j++) {
                grid[i][j] = Integer.parseInt(st.nextToken());
            }
        }

        // 4. 명령어 처리
        int K = Integer.parseInt(br.readLine().trim());
        for (int i = 0; i < K; i++) {
            String cmd = br.readLine().trim();
            switch (cmd) {
                case "U": move(0); break;
                case "D": move(1); break;
                case "L": move(2); break;
                case "R": move(3); break;
            }
        }

        // 5. 출력
        sb.append(collected).append("\n");
        sb.append(robotR).append(" ").append(robotC).append("\n");
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                if (j > 0) sb.append(" ");
                sb.append(grid[i][j]);
            }
            sb.append("\n");
        }

        System.out.print(sb);
    }

    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
    static void move(int dirIndex) {
        // 1. 다음 위치 계산
        int nr = robotR + dr[dirIndex];
        int nc = robotC + dc[dirIndex];

        // 2. 범위 벗어나면 이동 안 함
        if (nr < 0 || nr >= N || nc < 0 || nc >= M) return;

        // 3. 이동
        robotR = nr;
        robotC = nc;

        // 4. 박스 수집
        collected += grid[robotR][robotC];
        grid[robotR][robotC] = 0;
    }
}
