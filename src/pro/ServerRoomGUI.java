package pro;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerRoomGUI extends JFrame {
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;
    private JTextPane t_display;
    private Thread receiveThread;
    private String uid ; // 기본 사용자 ID
    private DefaultStyledDocument document;
    private String serverAddress;
    private int serverPort;
    private JLabel[] roomLabels = new JLabel[8]; // 각 방의 라벨을 저장

    public ServerRoomGUI(String serverAddress, int serverPort, String uid) {
        super("Server Room GUI");
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.uid = uid; // uid 저장

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

    private synchronized void ReceiveThread() {
        if (receiveThread != null && receiveThread.isAlive()) {
            receiveThread.interrupt(); // Stop previous thread if active
        }
        receiveThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    ChatMsg msg = (ChatMsg) in.readObject();
                    if (msg != null) {
                        handleIncomingMessage(msg);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                SwingUtilities.invokeLater(() -> printDisplay("서버 연결이 끊어졌습니다: " + e.getMessage()));
            } finally {
                cleanupResources();
            }
        });
        receiveThread.start();
    }

    private void cleanupResources() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close(); // Close socket
            }
            if (receiveThread != null && receiveThread.isAlive()) {
                receiveThread.interrupt(); // Interrupt thread
            }
        } catch (IOException e) {
            printDisplay("자원 정리 중 오류: " + e.getMessage());
        }
    }


    private void connectToServer(String serverAddress, int serverPort) {
        new Thread(() -> {
            try {
                socket = new Socket(serverAddress, serverPort);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                send(new ChatMsg(uid, ChatMsg.MODE_LOGIN, "로그인"));
                ReceiveThread();
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> printDisplay("서버에 연결할 수 없습니다: " + e.getMessage()));
            }
        }).start();
    }





    private int getPortForRoom(int roomNumber) {
        return 54321 + roomNumber; // 방 번호에 따라 포트 설정
    }


    private void updateRoomLabels(String message) {
        // 메시지 형식: "방 1 (2/4)"
        String[] parts = message.split(" "); // "방 1 (2/4)" -> ["방", "1", "(2/4)"]
        int roomNumber = Integer.parseInt(parts[1]) - 1; // 배열 인덱스는 0부터 시작
        SwingUtilities.invokeLater(() -> roomLabels[roomNumber].setText(message)); // 해당 방의 라벨 업데이트
    }


    private void handleIncomingMessage(ChatMsg msg) {
        SwingUtilities.invokeLater(() -> {
            switch (msg.mode) {
                case ChatMsg.MODE_ROOM_STATUS:
                    updateRoomLabels(msg.message); // 서버로부터 받은 방 상태 업데이트
                    break;
                default:
                    printDisplay("알 수 없는 메시지: " + msg.message);
            }
        });
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
            System.out.println("전송 메시지: " + msg.message);
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
                int roomPort = getPortForRoom(roomNumber);
                printDisplay("방 " + roomNumber + " 연결 시도 중. 포트: " + roomPort);

                // 서버 연결
                socket = new Socket(serverAddress, roomPort);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // 방 입장 메시지 전송
                send(new ChatMsg(uid, ChatMsg.MODE_ROOM_JOIN, "방 " + roomNumber + " 입장"));
                printDisplay("방 " + roomNumber + "에 메시지 전송 완료");

                // 서버로부터 응답 수신
                ChatMsg response = (ChatMsg) in.readObject();
                if (response != null && response.mode == ChatMsg.MODE_ROOM_STATUS) {
                    printDisplay("서버 응답: " + response.message);
                    updateRoomLabels(response.message); // 방 상태 업데이트
                }
            } catch (IOException | ClassNotFoundException e) {
                printDisplay("방 입장 오류: " + e.getMessage());
            }
        }).start();
    }


    private void requestRoomStatus() {
        send(new ChatMsg(uid, ChatMsg.MODE_ROOM_STATUS, "방 상태 요청"));
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
