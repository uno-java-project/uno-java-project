import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnoGameGUI extends JPanel {

    private static final String[] COLORS = {"Red", "Green", "Blue", "Yellow"};
    private static final String[] VALUES = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "Skip", "Reverse", "Draw2"};
    
    private List<String> deck;
    private List<String> player1List, player2List, player3List, player4List;  // 각 플레이어의 카드 리스트
    private String topCard;  // 덱에서 뽑은 top card
    private JPanel gamePanel;
    private JLabel remainingCardsLabel;  // 덱에 남은 카드 수를 표시할 레이블
    private String[] turn = {"Player 1", "Player 2", "Player 3", "Player 4"};  // 플레이어 순서 배열

    public UnoGameGUI() {
    	setLayout(new BorderLayout()); // 기존의 레이아웃 설정
    	setPreferredSize(new Dimension(615, 830));
    	
        // 게임 패널 설정
        gamePanel = new JPanel();
        gamePanel.setLayout(new GridLayout(6, 1)); // 한 행에 6개 항목을 표시
        add(gamePanel, BorderLayout.CENTER);

        // 덱에 남은 카드 수를 표시할 레이블 추가
        remainingCardsLabel = new JLabel("남은 카드 수 : 0");
        remainingCardsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(remainingCardsLabel, BorderLayout.NORTH);
        
        // 덱을 초기화하고 카드 섞기
        deck = new ArrayList<>();
        
        // 각 플레이어 카드 리스트 초기화
        player1List = new ArrayList<>();
        player2List = new ArrayList<>();
        player3List = new ArrayList<>();
        player4List = new ArrayList<>();
        
        updateRemainingCardsLabel();
        // 플레이어들의 카드 표시
        updateGamePanel();

        // 게임 시작 버튼
        JButton startButton = new JButton("게임 시작");
        startButton.addActionListener(e -> dealCards());
        add(startButton, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void dealCards() {
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

        // 남은 카드 수 업데이트
        updateRemainingCardsLabel();

        // 플레이어들의 카드 표시
        updateGamePanel();
    }

    private void updateRemainingCardsLabel() {
        // 덱에 남은 카드 수 표시
        remainingCardsLabel.setText("남은 카드 수 : " + deck.size());
    }

    private void updateGamePanel() {
        gamePanel.removeAll();  // 기존 내용 제거
        
        // 각 플레이어의 카드 표시        
        gamePanel.add(displayPlayerCards(player1List, 1));
        gamePanel.add(displayPlayerCards(player2List, 2));
        gamePanel.add(displayPlayerCards(player3List, 3));
        gamePanel.add(displayPlayerCards(player4List, 4));
        
        // 턴 패널 생성 및 중앙에 배치
        JPanel borderPanel = new JPanel(new BorderLayout());
        borderPanel.add(displayTopCardPanel(), BorderLayout.WEST);
        
        // displayTurnPanel을 중앙에 배치
        borderPanel.add(displayTurnPanel(), BorderLayout.CENTER);
        
        // 오른쪽에 버튼들 배치
        borderPanel.add(displayActionButtons(), BorderLayout.EAST);  // 동쪽에 버튼 패널 추가
        
        gamePanel.add(borderPanel);
        
        gamePanel.add(displayNumberOfCardsPanel());

        // 게임 패널 재배치
        gamePanel.revalidate();
        gamePanel.repaint();
    }
    
    private JPanel displayActionButtons() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(3, 1));  // 3개의 버튼을 세로로 배치 (3행, 1열)
        buttonPanel.setPreferredSize(new Dimension(200, 100)); // 패널 크기 설정

        // Draw, Next, Reverse 버튼들 생성
        JButton nextButton = new JButton("Next");
        JButton skipButton = new JButton("Skip");
        JButton reverseButton = new JButton("Reverse");

        // 각 버튼에 액션 리스너 추가
        nextButton.addActionListener(e -> nextTurn());
        
        // 스킵
        skipButton.addActionListener(e -> {
            nextTurn();  // 첫 번째 턴 건너뛰기
            nextTurn();  // 두 번째 턴 건너뛰기
        });
        
        // Reverse 버튼의 기능 구현 (turn 배열을 뒤집음)
        reverseButton.addActionListener(e -> reverseTurn());
        
        
        // 버튼을 버튼 패널에 추가
        buttonPanel.add(nextButton);
        buttonPanel.add(skipButton);
        buttonPanel.add(reverseButton);

        return buttonPanel;
    }

    private JPanel displayPlayerCards(List<String> playerList, int playerIndex) {
        JPanel playerPanel = new JPanel();
        playerPanel.setBorder(BorderFactory.createTitledBorder("Player " + playerIndex));
        playerPanel.setLayout(new BorderLayout());

        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new FlowLayout(FlowLayout.LEFT));  // 카드 버튼들을 왼쪽에 배치
        for (String card : playerList) {
            // 카드 이름을 색상과 값으로 나누어 표시
            String[] cardParts = card.split(" ");
            String color = cardParts[0]; // 색상
            String value = cardParts[1]; // 값

            // JButton 생성하여 카드로 표시
            JButton cardButton = new JButton(value);  // 카드 값만 텍스트로 표시
            cardButton.setPreferredSize(new Dimension(90, 30));  // 버튼 크기 조정

            // 카드 색상에 맞게 배경색 설정
            cardButton.setBackground(getColorForCard(color));
            
            // 색상에 따른 글자 색 설정
            if (color.equals("Green") || color.equals("Yellow")) {
                cardButton.setForeground(Color.BLACK);  // Green과 Yellow는 글자 색을 검은색으로 설정
            } else {
                cardButton.setForeground(Color.WHITE);  // 나머지 색은 흰색 글자
            }

            // 카드 클릭 시 이벤트 처리
            final String currentCard = card;
            cardButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // 카드 클릭 시 처리할 코드
                    playCard(currentCard, playerIndex - 1);  // playerIndex를 0부터 시작하는 인덱스로 변경
                }
            });

            cardPanel.add(cardButton);
        }

        // 오른쪽에 세로로 버튼 3개 추가 (GridLayout으로 변경)
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(3, 1)); // 3개의 버튼을 세로로 배치 (3행, 1열)
        buttonPanel.setPreferredSize(new Dimension(100, 100)); // 패널 크기 설정

        // 버튼들 추가 (예: Draw, Next, UNO)
        JButton drawButton = new JButton("Draw");
        JButton nextButton = new JButton("Next");
        JButton unoButton = new JButton("UNO");

        // 버튼에 액션 리스너 추가 (기본적인 처리 방식)
        drawButton.addActionListener(e -> drawCard(playerIndex - 1));
        nextButton.addActionListener(e -> System.out.println("player next action"));
        unoButton.addActionListener(e -> System.out.println("UNO action"));

        // 버튼을 버튼 패널에 추가
        buttonPanel.add(drawButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(unoButton);
        
        // 플레이어 패널에 카드 패널과 버튼 패널 추가
        playerPanel.add(cardPanel, BorderLayout.CENTER);
        playerPanel.add(buttonPanel, BorderLayout.EAST);  // 오른쪽에 버튼 패널 배치
        
        return playerPanel;
    }
    

    private JPanel displayTopCardPanel() {
        if (topCard != null) {
            JPanel topCardPanel = new JPanel();
            topCardPanel.setBorder(BorderFactory.createTitledBorder("Top Card"));
            
            // topCard를 JButton으로 만들기
            String[] topCardParts = topCard.split(" ");
            String topColor = topCardParts[0];
            String topValue = topCardParts[1];

            // topCard 버튼 생성
            JButton topCardButton = new JButton(topValue);  
            topCardButton.setPreferredSize(new Dimension(130, 90));  // 버튼 크기 조정
            topCardButton.setBackground(getColorForCard(topColor)); // 카드 색상 설정

            // 글자 색 설정 (Yellow와 Green은 검은색, 나머지는 흰색)
            if (topColor.equals("Yellow") || topColor.equals("Green")) {
                topCardButton.setForeground(Color.BLACK);  // 글자 색을 검은색으로 설정
            } else {
                topCardButton.setForeground(Color.WHITE);  // 나머지 색상은 흰색
            }

            topCardPanel.add(topCardButton);

            return topCardPanel;
        }
        return new JPanel();
    }

    private void playCard(String card, int playerIndex) {
        String[] cardParts = card.split(" ");
        String color = cardParts[0];
        String value = cardParts[1];

        // topCard와 비교: 색상 또는 숫자가 일치하면 카드 플레이
        String[] topCardParts = topCard.split(" ");
        String topColor = topCardParts[0];
        String topValue = topCardParts[1];

        if (color.equals(topColor) || value.equals(topValue)) {
            // 카드가 일치하면 플레이하고, 해당 카드 삭제
            if (playerIndex == 0) {
                player1List.remove(card);
            } else if (playerIndex == 1) {
                player2List.remove(card);
            } else if (playerIndex == 2) {
                player3List.remove(card);
            } else {
                player4List.remove(card);
            }

            // topCard를 현재 플레이된 카드로 교체
            topCard = card;

            // 게임 화면 갱신
            updateGamePanel();
        } else {
            JOptionPane.showMessageDialog(this, "이 카드는 플레이할 수 없습니다. 색상 또는 숫자가 일치하지 않습니다.");
        }
    }

    private void drawCard(int playerIndex) {
        // 덱에서 한 장의 카드를 뽑아 해당 플레이어에게 추가
        if (!deck.isEmpty()) {
            String drawnCard = deck.remove(0);
            if (playerIndex == 0) {
                player1List.add(drawnCard);
            } else if (playerIndex == 1) {
                player2List.add(drawnCard);
            } else if (playerIndex == 2) {
                player3List.add(drawnCard);
            } else {
                player4List.add(drawnCard);
            }

            // 남은 카드 수 업데이트
            updateRemainingCardsLabel();

            // 게임 화면 갱신
            updateGamePanel();
        }
    }
    
    private JPanel displayTurnPanel() {
        JPanel turnPanel = new JPanel();
        turnPanel.setLayout(new GridLayout(turn.length + 1, 1));  // 각 플레이어를 세로로 나열
        
        JLabel nowTurn = new JLabel("현제 차례 : " + turn[0]);
        turnPanel.add(nowTurn);
        

        for (String player : turn) {
            JLabel playerLabel = new JLabel(player);
            playerLabel.setHorizontalAlignment(SwingConstants.CENTER);
            turnPanel.add(playerLabel);
        }

        return turnPanel;
    }

    private void nextTurn() {
        // 턴 변경: turn 배열에서 첫 번째 아이템을 맨 뒤로 보냄
        String firstPlayer = turn[0];
        System.arraycopy(turn, 1, turn, 0, turn.length - 1);  // 배열에서 첫 번째 요소를 뺀 나머지를 한 칸씩 앞으로
        turn[turn.length - 1] = firstPlayer;  // 첫 번째 플레이어를 배열의 마지막에 넣기

        // 게임 화면 업데이트
        updateGamePanel();
    }
    
    private void reverseTurn() {
        // turn 배열을 뒤집음
        List<String> turnList = new ArrayList<>(List.of(turn));
        Collections.reverse(turnList);
        turn = turnList.toArray(new String[0]);  // 배열로 다시 변환

        // 게임 화면 업데이트
        updateGamePanel();
    }
    
    private JPanel displayNumberOfCardsPanel() {
        JPanel numberOfCardsPanel = new JPanel();
        numberOfCardsPanel.setBorder(BorderFactory.createTitledBorder("Number of Cards"));

        // 각 플레이어의 남은 카드 수를 표시하는 레이블 생성
        JLabel player1CardsLabel = new JLabel("Player 1: " + player1List.size() + " cards");
        JLabel player2CardsLabel = new JLabel("Player 2: " + player2List.size() + " cards");
        JLabel player3CardsLabel = new JLabel("Player 3: " + player3List.size() + " cards");
        JLabel player4CardsLabel = new JLabel("Player 4: " + player4List.size() + " cards");

        // 레이블들을 numberOfCardsPanel에 추가
        numberOfCardsPanel.add(player1CardsLabel);
        numberOfCardsPanel.add(player2CardsLabel);
        numberOfCardsPanel.add(player3CardsLabel);
        numberOfCardsPanel.add(player4CardsLabel);

        // 레이아웃 설정 (세로로 배치)
        numberOfCardsPanel.setLayout(new BoxLayout(numberOfCardsPanel, BoxLayout.Y_AXIS));

        return numberOfCardsPanel;
    }

    private Color getColorForCard(String color) {
        // 색상에 맞는 배경색을 반환
        switch (color) {
            case "Red": return Color.RED;
            case "Green": return Color.GREEN;
            case "Blue": return Color.BLUE;
            case "Yellow": return Color.YELLOW;
            default: return Color.GRAY;  // 기본 색상
        }
    }

    public static void main(String[] args) {
        JFrame gameFrame = new JFrame();
        gameFrame.setSize(615, 830);
        gameFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        gameFrame.add(new UnoGameGUI());

        gameFrame.setVisible(true);
    }
}



