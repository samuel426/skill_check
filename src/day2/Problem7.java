package day2;

import java.util.*;
import java.io.*;

/**
 * =====================================================================
 * [문제 7] 재고 경매 시스템 (복합 - 파싱 + Map + 우선순위큐) - 예상 소요 시간: 70분
 * =====================================================================
 *
 * 실전형 복합 문제입니다. 경매 시스템을 구현하세요.
 *
 * -------------------------------------------------------------------
 * [도메인]
 *
 * - 상품(Item): itemId(자동발급, 1부터), name, startPrice, currentPrice,
 *               sellerId, bidderId(최고 입찰자), status("OPEN"/"CLOSED")
 * - 입찰(Bid):  itemId, bidderId, price
 *
 * -------------------------------------------------------------------
 * [명령어]
 *
 * 1) LIST_ITEM seller=<판매자> name=<상품명> price=<시작가>
 *    - 상품을 경매에 등록합니다. status = "OPEN"
 *    - 출력: "LISTED <itemId>"
 *
 * 2) BID item=<itemId> bidder=<입찰자> price=<입찰가>
 *    - 입찰 조건:
 *      a) status == "OPEN"
 *      b) price > currentPrice
 *      c) bidder != sellerId (판매자는 자기 상품에 입찰 불가)
 *    - 성공 시: currentPrice 갱신, bidderId 갱신
 *    - 출력: "BID_OK <itemId> <newPrice>" 또는 "BID_FAIL <itemId>"
 *
 * 3) CLOSE item=<itemId>
 *    - 판매자만 닫을 수 있습니다. (seller 정보는 명령어에 없음 → 별도 처리 불필요,
 *      이 문제에서는 누구나 CLOSE 가능하다고 가정합니다)
 *    - status = "CLOSED"
 *    - bidderId가 있으면: "SOLD <itemId> <bidderId> <finalPrice>"
 *    - bidderId가 없으면: "UNSOLD <itemId>"
 *
 * 4) SEARCH keyword=<키워드>
 *    - name에 keyword가 포함된 OPEN 상품을 검색합니다.
 *    - currentPrice 오름차순 정렬, 같으면 itemId 오름차순
 *    - 출력: "<itemId> <name> <currentPrice>" 형식으로 한 줄씩
 *    - 없으면 "NO_RESULT"
 *
 * 5) TOP_BIDDER
 *    - 지금까지 BID_OK를 가장 많이 성공한 입찰자 top 3 출력
 *    - 형식: "<입찰자> <성공횟수>"
 *    - 같으면 이름 오름차순, 3명 미만이면 있는 만큼
 *
 * -------------------------------------------------------------------
 * [입력 예시]
 *   LIST_ITEM seller=tom name=laptop price=500
 *   LIST_ITEM seller=jane name=keyboard price=30
 *   LIST_ITEM seller=tom name=laptop_stand price=20
 *   BID item=1 bidder=alice price=600
 *   BID item=1 bidder=bob price=550
 *   BID item=1 bidder=bob price=700
 *   BID item=2 bidder=alice price=35
 *   BID item=2 bidder=tom price=40
 *   CLOSE item=1
 *   SEARCH keyword=laptop
 *   TOP_BIDDER
 *
 * [출력 예시]
 *   LISTED 1
 *   LISTED 2
 *   LISTED 3
 *   BID_OK 1 600
 *   BID_FAIL 1
 *   BID_OK 1 700
 *   BID_OK 2 35
 *   BID_FAIL 2
 *   SOLD 1 bob 700
 *   2 keyboard 35
 *   3 laptop_stand 20
 *   bob 2
 *   alice 1
 *
 * -------------------------------------------------------------------
 * [힌트]
 * - Item 클래스 또는 int[] 배열로 상품 관리
 * - Map<Integer, Item> items (itemId → Item)
 * - Map<String, Integer> bidSuccessCount (입찰자 → 성공 횟수)
 * - SEARCH: items.values() 필터 → 정렬 → 출력
 * - TOP_BIDDER: bidSuccessCount.entrySet() → List로 변환 → 정렬
 * =====================================================================
 */
public class Problem7 {

    // ※ 채점을 위해 아래 클래스를 수정하지 마세요.
    static class Item {
        int itemId;
        String name;
        int startPrice;
        int currentPrice;
        String sellerId;
        String bidderId;   // null이면 입찰자 없음
        String status;     // "OPEN" | "CLOSED"

        Item(int itemId, String name, int startPrice, String sellerId) {
            this.itemId       = itemId;
            this.name         = name;
            this.startPrice   = startPrice;
            this.currentPrice = startPrice;
            this.sellerId     = sellerId;
            this.bidderId     = null;
            this.status       = "OPEN";
        }
    }

    // ※ 채점을 위해 아래 필드를 수정하지 마세요.
    static int nextItemId = 1;
    static Map<Integer, Item> items = new LinkedHashMap<>();
    static Map<String, Integer> bidSuccessCount = new HashMap<>();

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null && !line.isEmpty()) {
            line = line.trim();
            String[] tokens = line.split(" ");
            String cmd = tokens[0];

            if ("LIST_ITEM".equals(cmd)) {
                // TODO: 파라미터 파싱 → Item 생성 → "LISTED <itemId>" 출력

            } else if ("BID".equals(cmd)) {
                // TODO: 입찰 처리 → BID_OK 또는 BID_FAIL 출력

            } else if ("CLOSE".equals(cmd)) {
                // TODO: 경매 종료 → SOLD 또는 UNSOLD 출력

            } else if ("SEARCH".equals(cmd)) {
                // TODO: keyword 포함 OPEN 상품 검색 → 정렬 → 출력

            } else if ("TOP_BIDDER".equals(cmd)) {
                // TODO: 입찰 성공 횟수 top 3 출력

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
