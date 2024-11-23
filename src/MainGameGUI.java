import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class MainGameGUI extends JFrame {
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;
    private JTextPane t_display;
    private Thread receiveThread;
    private String uid = "Guest"; // 기본 사용자 ID
    private DefaultStyledDocument document;
    public MainGameGUI(String serverAddress, int serverPort) {
        super("Server Room GUI");
        buildGUI();
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

    private void connectToServer(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

            printDisplay("서버에 연결되었습니다!");

            // 수신 스레드 시작
            receiveThread = new Thread(() -> {
                try {
                    while (true) {
                        ChatMsg msg = (ChatMsg) in.readObject();
                        if (msg != null) {
                            handleIncomingMessage(msg);
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    printDisplay("서버 연결이 끊어졌습니다: " + e.getMessage());
                }
            });
            receiveThread.start();
        } catch (IOException e) {
            printDisplay("서버에 연결할 수 없습니다: " + e.getMessage());
        }
    }

    private void handleIncomingMessage(ChatMsg msg) {
        SwingUtilities.invokeLater(() -> {
            switch (msg.mode) {
                case ChatMsg.MODE_TX_STRING:
                    printDisplay(msg.userID + ": " + msg.message);
                    break;
                case ChatMsg.MODE_ROOM_STATUS:
                    printDisplay("방 상태 업데이트: " + msg.message);
                    break;
                case ChatMsg.MODE_TX_IMAGE:
                    printDisplay(msg.userID + " 이미지 수신: " + msg.message);
                    break;
                default:
                    printDisplay("알 수 없는 메시지 형식");
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

        for (int i = 1; i <= 8; i++) {
            int roomNumber = i;
            JPanel singleRoomPanel = new JPanel(new BorderLayout());
            singleRoomPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

            JLabel roomLabel = new JLabel("방 " + roomNumber + " (0/4)", SwingConstants.CENTER);
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
        try {
            String[] statusParts = roomLabel.getText().split("\\(")[1].replace(")", "").split("/");
            int currentUsers = Integer.parseInt(statusParts[0]);
            int maxUsers = Integer.parseInt(statusParts[1]);

            if (currentUsers >= maxUsers) {
                JOptionPane.showMessageDialog(this, "방이 가득 찼습니다.", "입장 불가", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 방 입장
            printDisplay("방 " + roomNumber + "에 입장합니다...");
            send(new ChatMsg(uid, ChatMsg.MODE_LOGIN, "방 " + roomNumber + "에 입장"));

            // 방 상태 업데이트
            roomLabel.setText("방 " + roomNumber + " (" + (currentUsers + 1) + "/" + maxUsers + ")");
        } catch (Exception e) {
            printDisplay("방 입장 오류: " + e.getMessage());
        }
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 메시지 표시
        document = new DefaultStyledDocument();
        t_display = new JTextPane(document);
        t_display.setEditable(false);
        panel.add(new JScrollPane(t_display), BorderLayout.CENTER);

        // 입력 및 버튼
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerRoomGUI("localhost", 54321));
    }
}
