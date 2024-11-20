import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginGUI extends JFrame {
    private String serverAddress;
    private int serverPort;
    private JTextField t_userID;
    private JTextField t_hostAddr;
    private JButton b_start, b_exit, b_login;
    private JTextPane t_display;
    private JTextField t_input;
    private DefaultStyledDocument document;

    public LoginGUI(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        buildGUI();
        this.setBounds(0, 0, 800, 800);
        this.setTitle("WithTalk");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    private void buildGUI() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftPanel(), createRightPanel());

        splitPane.setResizeWeight(0.7); // 비율: 왼쪽 70%, 오른쪽 30%

        this.add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());

        // 상단 이미지 영역: 비율 증가 및 중앙 정렬
        JLabel imageLabel = new JLabel();
        ImageIcon imageIcon = new ImageIcon("assets/UNO.PNG");
        Image scaledImage = imageIcon.getImage().getScaledInstance(400, -1, Image.SCALE_SMOOTH); // 이미지 크기를 더 키움
        imageLabel.setIcon(new ImageIcon(scaledImage));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER); // 수평 중앙 정렬
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);   // 수직 중앙 정렬

        // 이미지 패널 추가 및 여백 포함
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBorder(BorderFactory.createEmptyBorder(200, 0, 100, 0)); // 상단과 하단에 20px 여백 추가
        imagePanel.add(imageLabel, BorderLayout.CENTER);

        leftPanel.add(imagePanel, BorderLayout.NORTH); // 이미지 패널 추가

        // 하단 영역: 로그인 및 버튼
        JPanel lowerPanel = new JPanel(new GridLayout(1, 2, 10, 10)); // 좌우로 나눔

        // 로그인 정보 입력 영역
        JPanel loginPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10)); // 패딩 추가

        loginPanel.add(new JLabel("User Name:"));
        t_userID = new JTextField();
        loginPanel.add(t_userID);

        loginPanel.add(new JLabel("IP Address:"));
        t_hostAddr = new JTextField();
        loginPanel.add(t_hostAddr);

        // 버튼 패널
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 5, 5)); // 세로로 나눔

        b_start = new JButton("게임 접속");
        b_start.addActionListener(this::onStartClicked);

        b_exit = new JButton("게임 종료");
        b_exit.addActionListener(e -> System.exit(0));

        buttonPanel.add(b_start);
        buttonPanel.add(b_exit);

        // 하단 패널에 좌측: 로그인, 우측: 버튼 패널 추가
        lowerPanel.add(loginPanel);
        lowerPanel.add(buttonPanel);

        // 하단 패널을 전체 레이아웃의 남쪽에 추가
        leftPanel.add(lowerPanel, BorderLayout.SOUTH);
        return leftPanel;
    }



    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());

        // 텍스트 표시 영역
        document = new DefaultStyledDocument();
        t_display = new JTextPane(document);
        t_display.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(t_display);
        rightPanel.add(scrollPane, BorderLayout.CENTER);

        // 입력 패널
        JPanel inputPanel = new JPanel(new BorderLayout());
        t_input = new JTextField();
        inputPanel.add(t_input, BorderLayout.CENTER);

        JButton b_send = new JButton("보내기");
        b_send.addActionListener(e -> sendMessage());
        inputPanel.add(b_send, BorderLayout.EAST);

        rightPanel.add(inputPanel, BorderLayout.SOUTH);
        return rightPanel;
    }

    private void onLoginClicked(ActionEvent e) {
        String username = t_userID.getText();
        String ipAddress = t_hostAddr.getText();
        try {
            document.insertString(document.getLength(), "로그인: " + username + " / IP: " + ipAddress + "\n", null);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    private void onStartClicked(ActionEvent e) {
        try {
            document.insertString(document.getLength(), "게임 접속 시도...\n", null);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = t_input.getText();
        if (message.isEmpty()) return;
        try {
            document.insertString(document.getLength(), "You: " + message + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        t_input.setText("");
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 54321;

        LoginGUI client = new LoginGUI(serverAddress, serverPort);
    }
}
