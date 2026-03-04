import java.util.*;
import java.io.*;

public class Problem1 {

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder sb = new StringBuilder();
        String line;

        // 학생명 -> (과목 -> 점수)
        Map<String, Map<String, Integer>> data = new LinkedHashMap<>();

        while ((line = br.readLine()) != null && !line.isEmpty()) {
            line = line.trim();
            String[] tokens = line.split(" ");

            if (line.startsWith("ADD")) {
                Map<String, String> params = parseParams(tokens);
                String name    = params.get("name");
                String subject = params.get("subject");
                int score      = Integer.parseInt(params.get("score"));

                data.computeIfAbsent(name, k -> new HashMap<>())
                        .put(subject, score);

            } else if (line.startsWith("DELETE")) {
                Map<String, String> params = parseParams(tokens);
                String name    = params.get("name");
                String subject = params.get("subject");

                if (data.containsKey(name)) {
                    data.get(name).remove(subject);
                    // 과목이 하나도 없으면 학생도 제거
                    if (data.get(name).isEmpty()) {
                        data.remove(name);
                    }
                }

            } else if (line.startsWith("PRINT")) {
                Map<String, String> params = parseParams(tokens);
                String order = params.get("order");

                if (data.isEmpty()) {
                    sb.append("EMPTY\n");
                    continue;
                }

                // 학생별 평균 계산
                List<String[]> results = new ArrayList<>();
                for (Map.Entry<String, Map<String, Integer>> entry : data.entrySet()) {
                    String name = entry.getKey();
                    Map<String, Integer> subjects = entry.getValue();

                    double sum = 0;
                    for (int score : subjects.values()) {
                        sum += score;
                    }
                    double avg = sum / subjects.size();

                    // 소수점 첫째 자리 반올림
                    double rounded = Math.round(avg * 10) / 10.0;
                    results.add(new String[]{name, String.valueOf(rounded)});
                }

                // 정렬
                results.sort((a, b) -> {
                    if ("avg_desc".equals(order)) {
                        double avgA = Double.parseDouble(a[1]);
                        double avgB = Double.parseDouble(b[1]);
                        if (avgB != avgA) return Double.compare(avgB, avgA); // 평균 내림차순
                        return a[0].compareTo(b[0]);                          // 이름 오름차순
                    } else { // name_asc
                        return a[0].compareTo(b[0]);
                    }
                });

                for (String[] r : results) {
                    // 출력 형식: "alice 87.5" (끝이 .0이어도 그대로)
                    sb.append(r[0]).append(" ").append(r[1]).append("\n");
                }
            }
        }

        System.out.print(sb);
    }

    // ※ 채점을 위해 아래 메서드 시그니처를 수정하지 마세요.
    static Map<String, String> parseParams(String[] tokens) {
        Map<String, String> map = new HashMap<>();
        // tokens[0]은 명령어(ADD/DELETE/PRINT)이므로 1번부터 파싱
        for (int i = 1; i < tokens.length; i++) {
            String[] kv = tokens[i].split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }
}