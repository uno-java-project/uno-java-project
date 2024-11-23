import java.util.*;

public class UnoGame {

    private static final String[] COLORS = {"Red", "Green", "Blue", "Yellow"};
    private static final String[] VALUES = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "Skip", "Reverse", "Draw2"};

    private List<String> deck;
    private List<String> player1List, player2List, player3List, player4List;
    private String topCard;
    private String[] turn;

    public UnoGame() {
        turn = new String[] {"Player 1", "Player 2", "Player 3", "Player 4"};

        // 초기화
        deck = new ArrayList<>();

        // 각 플레이어 카드 리스트 초기화
        player1List = new ArrayList<>();
        player2List = new ArrayList<>();
        player3List = new ArrayList<>();
        player4List = new ArrayList<>();

    }

    public void dealCards() {
        // 덱을 초기화하고 카드 섞기
        deck = new ArrayList<>();

        // 각 플레이어 카드 리스트 초기화
        player1List = new ArrayList<>();
        player2List = new ArrayList<>();
        player3List = new ArrayList<>();
        player4List = new ArrayList<>();

        // 덱 생성
        for (String color : COLORS) {
            for (String value : VALUES) {
                deck.add(color + " " + value);
                deck.add(color + " " + value);  // 각 카드는 2번씩 존재
            }
        }

        // 덱 섞기
        Collections.shuffle(deck);

        // 4명의 플레이어에게 7장씩 나누어 주기
        for (int i = 0; i < 7; i++) {  // 각 플레이어에게 7장
            player1List.add(deck.remove(0));
            player2List.add(deck.remove(0));
            player3List.add(deck.remove(0));
            player4List.add(deck.remove(0));
        }

        // 덱에서 한 장의 카드를 뽑아서 topCard 설정
        if (!deck.isEmpty()) {
            topCard = deck.remove(0);
        }
    }

    public List<String> getDeck(){
        return deck;
    }

    public String getTopCard() {
        return topCard;
    }

    public List<String> getPlayerCards(int playerIndex) {
        switch (playerIndex) {
            case 1: return player1List;
            case 2: return player2List;
            case 3: return player3List;
            case 4: return player4List;
            default: return new ArrayList<>();
        }
    }

    public String[] getTurn() {
        return turn;
    }

    public boolean playCard(String card, int playerIndex) {
        String[] cardParts = card.split(" ");
        String color = cardParts[0];
        String value = cardParts[1];

        String[] topCardParts = topCard.split(" ");
        String topColor = topCardParts[0];
        String topValue = topCardParts[1];

        if (color.equals(topColor) || value.equals(topValue)) {
            // 카드가 일치하면 플레이하고, 해당 카드 삭제
            switch (playerIndex) {
                case 1: player1List.remove(card); break;
                case 2: player2List.remove(card); break;
                case 3: player3List.remove(card); break;
                case 4: player4List.remove(card); break;
            }

            // topCard 갱신
            topCard = card;
            return true;
        } else {
            return false;
        }
    }

    public void drawCard(int playerIndex) {
        if (!deck.isEmpty()) {
            String drawnCard = deck.remove(0);
            switch (playerIndex) {
                case 1: player1List.add(drawnCard); break;
                case 2: player2List.add(drawnCard); break;
                case 3: player3List.add(drawnCard); break;
                case 4: player4List.add(drawnCard); break;
            }
        }
    }

    public void nextTurn() {
        // 턴 변경: turn 배열에서 첫 번째 아이템을 맨 뒤로 보냄
        String firstPlayer = turn[0];
        System.arraycopy(turn, 1, turn, 0, turn.length - 1);  // 배열에서 첫 번째 요소를 뺀 나머지를 한 칸씩 앞으로
        turn[turn.length - 1] = firstPlayer;  // 첫 번째 플레이어를 배열의 마지막에 넣기
    }

    public void jumpTurn() {
        nextTurn();
        nextTurn();
    }

    public void reverseTurn() {
        // turn 배열을 뒤집음
        List<String> turnList = new ArrayList<>(List.of(turn));
        Collections.reverse(turnList);
        turn = turnList.toArray(new String[0]);  // 배열로 다시 변환
    }

    public int getRemainingCardCount() {
        return deck.size();
    }
}