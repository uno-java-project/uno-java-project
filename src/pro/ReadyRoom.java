package pro;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ReadyRoom extends JFrame {
    private int roomNumber; // 방 번호
    private String uid; // 사용자 ID
    private String serverAddress; // 서버 주소
    private int serverPort; // 방 포트 번호

    private JLabel[] userLabels = new JLabel[4]; // 최대 4명의 사용자
    private boolean[] readyStates = new boolean[4]; // 각 사용자의 레디 상태
    private JButton[] readyButtons = new JButton[4]; // 각 사용자의 레디 버튼
    private int currentUserCount = 0; // 현재 입장한 사용자 수
    private JButton startGameButton;

    public ReadyRoom(int roomNumber, String uid, String serverAddress, int serverPort) {
        this.roomNumber = roomNumber;
        this.uid = uid;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;

        buildGUI();
        this.setBounds(100, 100, 600, 400);
        this.setTitle("Ready Room - 방 " + roomNumber);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    private void buildGUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 사용자 목록 패널
        JPanel userPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        userPanel.setBorder(BorderFactory.createTitledBorder("사용자 목록"));

        // 사용자 슬롯 초기화
        for (int i = 0; i < 4; i++) {
            JPanel singleUserPanel = new JPanel(new BorderLayout());
            singleUserPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

            JLabel userLabel = new JLabel("User Slot " + (i + 1) + " (대기 중)", SwingConstants.CENTER);
            userLabels[i] = userLabel;

            JButton readyButton = new JButton("레디");
            readyButtons[i] = readyButton;
            readyButton.setEnabled(false); // 초기에는 버튼 비활성화
            int userIndex = i;
            readyButton.addActionListener(e -> toggleReady(userIndex, userLabel, readyButton));

            singleUserPanel.add(userLabel, BorderLayout.CENTER);
            singleUserPanel.add(readyButton, BorderLayout.EAST);
            userPanel.add(singleUserPanel);
        }

        mainPanel.add(userPanel, BorderLayout.CENTER);

        // 하단 패널 (게임 시작 버튼 및 종료 버튼)
        JPanel controlPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        startGameButton = new JButton("게임 시작");
        startGameButton.setEnabled(false); // 모든 사용자가 레디 상태가 될 때까지 비활성화
        startGameButton.addActionListener(e -> startGame());

        JButton exitButton = new JButton("나가기");
        exitButton.addActionListener(e -> System.exit(0));

        controlPanel.add(startGameButton);
        controlPanel.add(exitButton);

        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        this.add(mainPanel);
    }

    private void toggleReady(int userIndex, JLabel userLabel, JButton readyButton) {
        readyStates[userIndex] = !readyStates[userIndex]; // 레디 상태 토글
        if (readyStates[userIndex]) {
            userLabel.setText("User Slot " + (userIndex + 1) + " (Ready)");
            readyButton.setText("취소");
        } else {
            userLabel.setText("User Slot " + (userIndex + 1) + " (대기 중)");
            readyButton.setText("레디");
        }

        checkAllReady();
    }

    private void checkAllReady() {
        for (int i = 0; i < currentUserCount; i++) {
            if (!readyStates[i]) {
                startGameButton.setEnabled(false);
                return;
            }
        }
        startGameButton.setEnabled(true);
    }

    private void startGame() {
        JOptionPane.showMessageDialog(this, "게임이 시작됩니다!");
        // 게임 시작 로직 추가 가능
    }

    public void addUser(String userName) {
        if (currentUserCount >= 4) {
            JOptionPane.showMessageDialog(this, "최대 4명만 입장할 수 있습니다.");
            return;
        }

        userLabels[currentUserCount].setText(userName + " (대기 중)");
        readyButtons[currentUserCount].setEnabled(true); // 사용자가 추가되면 레디 버튼 활성화
        currentUserCount++;
    }
}
