import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ClientGameGUI extends JPanel {
    private UnoGame unoGame;
    private JPanel gamePanel;
    private JLabel remainingCardsLabel;  // 덱에 남은 카드 수를 표시할 레이블

    public ClientGameGUI(UnoGame unoGame) {
        setLayout(new BorderLayout()); // 기존의 레이아웃 설정
        setPreferredSize(new Dimension(615, 830));

        // 게임 패널 설정
        gamePanel = new JPanel();
        gamePanel.setLayout(new BorderLayout());  // 전체 화면을 BorderLayout으로 설정
        add(gamePanel, BorderLayout.CENTER);

        // 덱에 남은 카드 수를 표시할 레이블 추가
        remainingCardsLabel = new JLabel("남은 카드 수 : 0");
        remainingCardsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(remainingCardsLabel, BorderLayout.NORTH);

        this.unoGame = unoGame;

        updateGamePanel();

        // 게임 시작 버튼
        JButton startButton = new JButton("게임 시작");
        startButton.addActionListener(e -> gameStartUp());
        add(startButton, BorderLayout.SOUTH);

        setVisible(true);
    }

    public void gameStartUp() {
        unoGame.startGame();

        // 플레이어들의 카드 표시
        updateGamePanel();
    }

    private void updateRemainingCardsLabel() {
        // 덱에 남은 카드 수 표시
        remainingCardsLabel.setText("남은 카드 수 : " + unoGame.getDeck().size());
    }

    private void updateGamePanel() {
        // 남은 카드 수 업데이트
        updateRemainingCardsLabel();

        gamePanel.removeAll();  // 기존 내용 제거

        // 상단에 플레이어 덱 (Player 1, 2, 3, 4) 배치
        JPanel playersPanel = new JPanel();
        playersPanel.setLayout(new BorderLayout());  // 전체 게임 화면을 BorderLayout으로 배치
        playersPanel.add(displayPlayerCards(unoGame.getPlayerCards(1), 1), BorderLayout.NORTH); // Player 1 덱
        playersPanel.add(displayPlayerCards(unoGame.getPlayerCards(2), 2), BorderLayout.SOUTH); // Player 2 덱
        playersPanel.add(displayPlayerCards(unoGame.getPlayerCards(3), 3), BorderLayout.WEST);  // Player 3 덱
        playersPanel.add(displayPlayerCards(unoGame.getPlayerCards(4), 4), BorderLayout.EAST);  // Player 4 덱

        gamePanel.add(playersPanel, BorderLayout.CENTER);  // 플레이어 덱을 게임 패널 중앙에 배치

        // Top Card 표시 (중앙)
        gamePanel.add(displayTopCardPanel(), BorderLayout.CENTER);

        // 플레이어 1의 버튼들
        gamePanel.add(displayActionButtons(), BorderLayout.EAST);  // 플레이어 1 버튼은 오른쪽에 배치

        // 게임 패널 재배치
        gamePanel.revalidate();
        gamePanel.repaint();
    }

    private JPanel displayActionButtons() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(3, 1));  // 3개의 버튼을 세로로 배치 (3행, 1열)
        buttonPanel.setPreferredSize(new Dimension(200, 100)); // 패널 크기 설정

        // Draw, Next, UNO 버튼들 생성
        JButton drawButton = new JButton("Draw");
        JButton nextButton = new JButton("Next");
        JButton unoButton = new JButton("UNO");

        // 각 버튼에 액션 리스너 추가
        drawButton.addActionListener(e -> drawCardUpdate(1));  // 플레이어 1이 Draw 버튼을 클릭한 경우
        nextButton.addActionListener(e -> nextTurnUpdate());  // 턴을 넘기는 버튼
        unoButton.addActionListener(e -> System.out.println("UNO action"));  // UNO 버튼 처리

        // 버튼을 버튼 패널에 추가
        buttonPanel.add(drawButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(unoButton);

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
                    playCardUpdate(currentCard, playerIndex);
                }
            });

            cardPanel.add(cardButton);
        }

        playerPanel.add(cardPanel, BorderLayout.CENTER);  // 카드 패널을 중앙에 배치

        return playerPanel;
    }

    private JPanel displayTopCardPanel() {
        if (unoGame.getTopCard() != null) {
            JPanel topCardPanel = new JPanel();
            topCardPanel.setBorder(BorderFactory.createTitledBorder("Top Card"));

            // topCard를 JButton으로 만들기
            String[] topCardParts = unoGame.getTopCard().split(" ");
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

    private void playCardUpdate(String card, int playerIndex) {
        if (!unoGame.playCard(card, playerIndex)) {
            JOptionPane.showMessageDialog(this, "이 카드는 플레이할 수 없습니다. 색상 또는 숫자가 일치하지 않습니다.");
        }
        updateGamePanel();
    }

    private void drawCardUpdate(int playerIndex) {
        // 덱에서 한 장의 카드를 뽑아 해당 플레이어에게 추가
        unoGame.drawCard(playerIndex);

        // 게임 화면 갱신
        updateGamePanel();
    }

    private void nextTurnUpdate() {
        unoGame.nextTurn();

        // 게임 화면 업데이트
        updateGamePanel();
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
        UnoGame uno = new UnoGame();
        gameFrame.add(new ClientGameGUI(uno));

        gameFrame.setVisible(true);
    }
}

