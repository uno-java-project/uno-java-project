import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class ServerRoomGUI extends JFrame {
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;
    private JTextPane t_display;
    private Thread receiveThread;
    private String uid = "Guest"; // 기본 사용자 ID
    private DefaultStyledDocument document;
    private String serverAddress;
    private int serverPort;
    private JLabel[] roomLabels = new JLabel[8]; // 각 방의 라벨을 저장

    public ServerRoomGUI(String serverAddress, int serverPort) {
        super("Server Room GUI");
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;

        buildGUI(); // GUI 초기화
        setBounds(100, 100, 800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        // 서버 연결 시도
        connectToServer(serverAddress, serverPort);
    }
    private void buildGUI() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftPanel(), createRightPanel());
        splitPane.setDividerLocation(300);
        add(splitPane, BorderLayout.CENTER);
    }
    private void ReceiveThread() {
        receiveThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) { // 스레드 중단 요청 확인
                    ChatMsg msg = (ChatMsg) in.readObject();
                    if (msg != null) {
                        handleIncomingMessage(msg);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                SwingUtilities.invokeLater(() -> printDisplay("서버 연결이 끊어졌습니다: " + e.getMessage()));
            } finally {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close(); // 소켓 닫기
                    }
                } catch (IOException e) {
                    printDisplay("소켓 닫기 오류: " + e.getMessage());
                }
            }
        });
        receiveThread.start();
    }


    private void connectToServer(String serverAddress, int serverPort) {
        new Thread(() -> {
            try {
                socket = new Socket(serverAddress, serverPort);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                SwingUtilities.invokeLater(() -> printDisplay("서버에 연결되었습니다!"));

                receiveThread = new Thread(() -> {
                    try {
                        while (true) {
                            ChatMsg msg = (ChatMsg) in.readObject();
                            if (msg != null) {
                                handleIncomingMessage(msg);
                            }
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        SwingUtilities.invokeLater(() -> printDisplay("서버 연결이 끊어졌습니다: " + e.getMessage()));
                    }
                });
                receiveThread.start();
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> printDisplay("서버에 연결할 수 없습니다: " + e.getMessage()));
            }
        }).start();
    }
    private int getPortForRoom(int roomNumber) {
        return 54321 + roomNumber; // 방 번호에 따라 포트 설정
    }
    private void handleIncomingMessage(ChatMsg msg) {
        SwingUtilities.invokeLater(() -> {
            switch (msg.mode) {
                case ChatMsg.MODE_ROOM_STATUS:
                    updateRoomLabels(msg.message); // 방 상태 업데이트
                    break;
                default:
                    printDisplay("알 수 없는 메시지: " + msg.message);
            }
        });
    }

    private void updateRoomLabels(String message) {
        String[] lines = message.split("\n"); // 메시지를 줄 단위로 분리
        for (String line : lines) {
            if (line.startsWith("방")) {
                String[] parts = line.split(" "); // "방 1 (1/4)" -> ["방", "1", "(1/4)"]
                int roomNumber = Integer.parseInt(parts[1]) - 1; // 배열 인덱스는 0부터 시작
                roomLabels[roomNumber].setText(line); // 해당 방의 라벨 업데이트
            }
        }
    }




    private void printDisplay(String msg) {
        SwingUtilities.invokeLater(() -> {
            try {
                document.insertString(document.getLength(), msg + "\n", null);
                t_display.setCaretPosition(t_display.getDocument().getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void send(ChatMsg msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            printDisplay("메시지 전송 오류: " + e.getMessage());
        }
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel roomPanel = new JPanel(new GridLayout(8, 1, 5, 5));
        roomPanel.setBorder(BorderFactory.createTitledBorder("방 목록"));

        for (int i = 0; i < 8; i++) {
            int roomNumber = i + 1; // 방 번호는 1부터 시작
            JPanel singleRoomPanel = new JPanel(new BorderLayout());
            singleRoomPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

            JLabel roomLabel = new JLabel("방 " + roomNumber + " (0/4)", SwingConstants.CENTER);
            roomLabels[i] = roomLabel; // 배열에 라벨 저장
            JButton joinButton = new JButton("참가");
            joinButton.addActionListener(e -> joinRoom(roomNumber, roomLabel));

            singleRoomPanel.add(roomLabel, BorderLayout.CENTER);
            singleRoomPanel.add(joinButton, BorderLayout.EAST);
            roomPanel.add(singleRoomPanel);
        }

        leftPanel.add(roomPanel, BorderLayout.CENTER);
        return leftPanel;
    }


    private void joinRoom(int roomNumber, JLabel roomLabel) {
        new Thread(() -> {
            try {
                // 기존 연결 닫기
                if (socket != null && !socket.isClosed()) {
                    try {
                        receiveThread.interrupt(); // 기존 수신 스레드 종료 요청
                        socket.close(); // 기존 소켓 닫기
                    } catch (IOException e) {
                        printDisplay("기존 소켓 닫기 오류: " + e.getMessage());
                    }
                }

                // 새 소켓 연결
                int roomPort = getPortForRoom(roomNumber);
                socket = new Socket(serverAddress, roomPort);
                out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

                // 서버에 입장 메시지 전송
                send(new ChatMsg(uid, ChatMsg.MODE_ROOM_JOIN, "방 " + roomNumber + " 입장"));

                // UI 업데이트
                SwingUtilities.invokeLater(() -> roomLabel.setText("방 " + roomNumber + " (연결됨)"));

                // 새 수신 스레드 시작
                if (receiveThread != null && receiveThread.isAlive()) {
                    receiveThread.interrupt(); // 이전 수신 스레드 중단 요청
                }
                ReceiveThread();

                printDisplay("방 " + roomNumber + "에 입장했습니다.");
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> printDisplay("방 입장 오류: " + e.getMessage()));
            }
        }).start();
    }








    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 메시지 표시
        document = new DefaultStyledDocument(); // DefaultStyledDocument 초기화
        t_display = new JTextPane(document);    // JTextPane에 문서 연결
        t_display.setEditable(false);           // 편집 불가능 설정
        panel.add(new JScrollPane(t_display), BorderLayout.CENTER); // 스크롤 추가

        // 입력 및 버튼 구성
        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextField inputField = new JTextField();
        JButton sendButton = new JButton("보내기");
        sendButton.addActionListener(e -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING, message));
                inputField.setText("");
            }
        });

        JButton fileButton = new JButton("파일 전송");
        fileButton.addActionListener(e -> selectFile());

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        buttonPanel.add(sendButton);
        buttonPanel.add(fileButton);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        panel.add(inputPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void selectFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "png", "gif"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            send(new ChatMsg(uid, ChatMsg.MODE_TX_IMAGE, file.getName(), new ImageIcon(file.getAbsolutePath())));
        }
    }

}
