import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public class ClientReadyRoomGUI extends JFrame {
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
    private JTextField t_input;
    JButton b_select, b_send;

    public ClientReadyRoomGUI(String uid, String serverAddress, int serverPort){
        super("Server Room GUI");
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.uid = uid; // uid 저장

        buildGUI(); // GUI 초기화

  /*      try {
            connectToServer();
            sendUserID();
        } catch (UnknownHostException e1) {
            printDisplay("서버 주소와 포트번호를 확인하세요: "+ e1.getMessage());
            return;
        } catch (IOException e1) {
            printDisplay("서버와 연결 오류: "+ e1.getMessage());
            return;
        }*/

        setBounds(100, 100, 800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void sendImage() {
        String filename = t_input.getText().strip();
        if (filename.isEmpty()) return;

        File file = new File(filename);
        if (!file.exists()) {
            printDisplay(">> 파일이 존재하지 않습니다 : " + filename);
            return;
        }
        ImageIcon icon = new ImageIcon(filename);
        send(new ChatMsg(uid, ChatMsg.MODE_TX_IMAGE, file.getName(), icon));
        t_input.setText("");
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

    private void connectToServer() throws UnknownHostException, IOException {
        socket = new Socket();
        SocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
        socket.connect(sa, 3000);
        out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        receiveThread = new Thread(new Runnable() {
            private ObjectInputStream in;

            @Override
            public void run() {
                try {
                    in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

                } catch (IOException e) {
                    printDisplay("입력 스트림이 열리지 않음");
                }
                while (receiveThread == Thread.currentThread()) {
                    receiveMessage(in);
                }
            }
        });
        receiveThread.start();
        b_select.setEnabled(true);
    }
    private void receiveMessage(ObjectInputStream in) {
        try {
            ChatMsg inMsg = (ChatMsg) in.readObject();
            if (inMsg == null) {
                disconnect();
                printDisplay("서버 연결 끊김");
                return;
            }
            switch (inMsg.mode) {
                case ChatMsg.MODE_TX_STRING:
                    printDisplay(inMsg.userID + ":" + inMsg.message);
                    break;
                case ChatMsg.MODE_TX_IMAGE:
                    printDisplay(inMsg.userID + ":" + inMsg.message);
                    printDisplay(inMsg.image);
                    break;
            }
        } catch (IOException e) {
            printDisplay("연결 종류");
        } catch (ClassNotFoundException e) {
            printDisplay("잘못된 객체가 전달되었습니다");
        }
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Message display area (from WithTalk)
        document = new DefaultStyledDocument();
        t_display = new JTextPane(document);
        t_display.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(t_display);

        // Add the message display area to the top
        panel.add(scrollPane, BorderLayout.CENTER);

        // Create the input panel (use a separate method)
        JPanel inputPanel = createInputPanel();

        // Add the input panel to the bottom
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        t_input = new JTextField(30);

        // Add action for pressing Enter
        t_input.addActionListener(e -> sendMessage());

        b_send = new JButton("보내기");
        b_send.addActionListener(e -> sendMessage());

        b_select = new JButton("선택하기");
        b_select.addActionListener(new ActionListener() {
            JFileChooser chooser = new JFileChooser();

            @Override
            public void actionPerformed(ActionEvent e) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "JPG & GIF & PNG Images",
                        "jpg", "gif", "png");
                chooser.setFileFilter(filter);

                int ret = chooser.showOpenDialog(ClientReadyRoomGUI.this); // Adjusted to use ServerRoomGUI context
                if (ret != JFileChooser.APPROVE_OPTION) {
                    JOptionPane.showMessageDialog(ClientReadyRoomGUI.this, "파일을 선택하지 않았습니다");
                    return;
                }
                t_input.setText(chooser.getSelectedFile().getAbsolutePath());
                sendImage();
            }
        });

        panel.add(t_input, BorderLayout.CENTER);

        JPanel p_button = new JPanel(new GridLayout(1, 0));
        p_button.add(b_select);
        p_button.add(b_send);

        panel.add(p_button, BorderLayout.EAST);

        t_input.setEnabled(true);
        b_select.setEnabled(true);
        b_send.setEnabled(true);

        return panel;
    }


    private void sendMessage() {
        String message = t_input.getText().trim();
        if (message.isEmpty()) return;

        send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING, message));
        t_input.setText(""); // Clear input field after sending
    }

    private int getPortForRoom(int roomNumber) {
        return 54321 + roomNumber; // 방 번호에 따라 포트 설정
    }

    private void handleIncomingMessage(ChatMsg msg) {
        SwingUtilities.invokeLater(() -> {
            switch (msg.mode) {
                case ChatMsg.MODE_ROOM_STATUS:
                    printDisplay("방 상태 업데이트 수신: " + msg.message);
                    updateRoomLabels(msg.message); // 방 상태 업데이트
                    break;
                case ChatMsg.MODE_ROOM_JOIN:
                    // 입장 거부 메시지 처리
                    if (msg.message.contains("입장이 거부")) {
                        printDisplay("서버 응답: " + msg.message);
                    }
                    break;
                default:
                    printDisplay("알 수 없는 메시지: " + msg.message);
            }
        });
    }


    private void updateRoomLabels(String message) {
        // 메시지 형식: "방 1 (2/4)"
        String[] parts = message.split(" "); // "방 1 (2/4)" -> ["방", "1", "(2/4)"]
        int roomNumber = Integer.parseInt(parts[1]) - 1; // 배열 인덱스는 0부터 시작
        SwingUtilities.invokeLater(() -> roomLabels[roomNumber].setText(message)); // 해당 방의 라벨 업데이트
    }

    private void printDisplay(ImageIcon icon) {
        t_display.setCaretPosition(t_display.getDocument().getLength());

        if (icon.getIconWidth() > 400) {
            Image img = icon.getImage();
            Image changeImg = img.getScaledInstance(400, -1, Image.SCALE_SMOOTH);
            icon = new ImageIcon(changeImg);
        }
        t_display.insertIcon(icon);
        printDisplay("");
        t_input.setText("");
    }
    private void printDisplay(String msg) {
        t_display.setCaretPosition(t_display.getDocument().getLength());
        int len = t_display.getDocument().getLength();
        try {
            document.insertString(len, msg + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        t_display.setCaretPosition(len);
    }

    private void sendUserID() {
        send(new ChatMsg(uid, ChatMsg.MODE_LOGIN));
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


    private static JPanel createLeftPanel() {
        // BorderLayout을 사용한 기본 패널
        JPanel leftPanel = new JPanel(new BorderLayout());

        // 네모와 버튼을 가로로 배치하는 패널
        JPanel boxesPanel = new JPanel(new GridLayout(2, 4, 10, 10)); // 1x4 그리드
        for (int i = 0; i < 4; i++) {
            // 개별 네모와 버튼을 포함하는 패널 생성
            JPanel boxPanel = new JPanel(new BorderLayout());

            // 네모 생성
            JPanel box = new JPanel();
            box.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            box.setPreferredSize(new Dimension(100, 100));
            boxPanel.add(box, BorderLayout.CENTER);

            // 버튼 생성
            JButton readyButton = new JButton("READY " );
            readyButton.setPreferredSize(new Dimension(100, 40));
            boxPanel.add(readyButton, BorderLayout.SOUTH);

            // 메인 패널에 추가
            boxesPanel.add(boxPanel);
        }
        leftPanel.add(boxesPanel, BorderLayout.CENTER);



        return leftPanel;
    }



    private void joinRoom(int roomNumber) {
        new UnoGameClient(uid, serverAddress, serverPort + roomNumber);
    }

    private void disconnect() {
        send(new ChatMsg(uid, ChatMsg.MODE_LOGOUT));
        try {
            receiveThread = null;
            socket.close();
        } catch (IOException e) {
            System.err.println("클라이언트 닫기 오류 > " + e.getMessage());
            System.exit(-1);
        }

    }

    private void selectFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "png", "gif"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            send(new ChatMsg(uid, ChatMsg.MODE_TX_IMAGE, file.getName(), new ImageIcon(file.getAbsolutePath())));
        }
    }
/*    public static void main(String[] args) {


        int port = 54321;
        new ClientReadyRoomGUI(port);    }*/
}
