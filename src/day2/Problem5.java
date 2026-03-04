package day2;

import java.util.*;
import java.io.*;

/**
 * =====================================================================
 * [문제 5] 채널 메시지 통계 (Map/Set + 정렬) - 예상 소요 시간: 50분
 * =====================================================================
 *
 * 슬랙과 유사한 메신저 시스템의 로그가 주어집니다.
 * 각 이벤트를 처리하고 REPORT 명령어가 오면 통계를 출력하세요.
 *
 * -------------------------------------------------------------------
 * [명령어 형식]
 *
 * 1) JOIN channel=<채널명> user=<사용자명>
 *    - 해당 사용자가 채널에 입장합니다.
 *    - 이미 입장한 경우 무시합니다.
 *
 * 2) LEAVE channel=<채널명> user=<사용자명>
 *    - 해당 사용자가 채널에서 퇴장합니다.
 *    - 채널에 없는 경우 무시합니다.
 *
 * 3) SEND channel=<채널명> user=<사용자명> msg=<내용>
 *    - 채널에 입장한 사용자만 메시지 전송 가능합니다.
 *    - 채널에 없는 사용자의 SEND는 무시합니다.
 *    - 메시지 내용은 공백 없는 단어 하나입니다.
 *
 * 4) REPORT type=<통계유형>
 *    - type=top_sender   : 전체 채널 통틀어 메시지를 가장 많이 보낸 사용자
 *                          상위 3명을 "이름 횟수" 형식으로 출력
 *                          (같으면 이름 오름차순, 3명 미만이면 있는 만큼만)
 *    - type=active_ch    : 현재 가장 많은 사용자가 입장해 있는 채널 이름 출력
 *                          (같으면 채널명 오름차순, 채널 없으면 "NONE")
 *    - type=msg_count    : 채널별 총 메시지 수를 메시지 수 내림차순으로 출력
 *                          형식: "<채널명> <메시지수>"
 *                          (같으면 채널명 오름차순, 메시지 없는 채널은 제외)
 *
 * -------------------------------------------------------------------
 * [입력 예시]
 *   JOIN channel=general user=alice
 *   JOIN channel=general user=bob
 *   JOIN channel=random user=alice
 *   SEND channel=general user=alice msg=hello
 *   SEND channel=general user=bob msg=hi
 *   SEND channel=general user=alice msg=bye
 *   SEND channel=random user=alice msg=test
 *   LEAVE channel=general user=bob
 *   JOIN channel=random user=carol
 *   REPORT type=top_sender
 *   REPORT type=active_ch
 *   REPORT type=msg_count
 *
 * [출력 예시]
 *   alice 4
 *   bob 1
 *   general 3
 *   random 1
 *   NONE (active_ch는 general=1명, random=2명이므로 random)
 *
 * ※ 위 예시 출력은 의도적으로 일부 틀리게 작성되어 있습니다.
 *    직접 계산해서 맞는 출력을 구하세요.
 *
 * -------------------------------------------------------------------
 * [힌트]
 * - 채널별 현재 입장 사용자: Map<String, Set<String>>
 * - 사용자별 총 메시지 수:   Map<String, Integer>
 * - 채널별 총 메시지 수:     Map<String, Integer>
 * - REPORT마다 필요한 데이터만 정렬 (전체를 미리 정렬할 필요 없음)
 * =====================================================================
 */
public class Problem5 {

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder sb = new StringBuilder();
        String line;

        // 여기에 코드를 작성하세요.
        // 자료구조 선언


        while ((line = br.readLine()) != null && !line.isEmpty()) {
            line = line.trim();
            String[] tokens = line.split(" ");
            String cmd = tokens[0];

            if ("JOIN".equals(cmd)) {
                // TODO

            } else if ("LEAVE".equals(cmd)) {
                // TODO

            } else if ("SEND".equals(cmd)) {
                // TODO

            } else if ("REPORT".equals(cmd)) {
                String type = tokens[1].split("=")[1];

                if ("top_sender".equals(type)) {
                    // TODO: 전체 사용자 메시지 수 내림차순, 상위 3명

                } else if ("active_ch".equals(type)) {
                    // TODO: 현재 입장자 수가 가장 많은 채널 1개

                } else if ("msg_count".equals(type)) {
                    // TODO: 채널별 메시지 수 내림차순

                }
            }
        }

        System.out.print(sb);
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
