package pro;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;

public class LoginGUI extends JFrame {
    private String serverAddress;
    private int serverPort;
    private ObjectOutputStream out;
    JTextField t_userID;
    JTextField t_hostAddr;
    JTextField t_portNum;

    private JButton b_start, b_exit, b_send, b_select;
    private JTextPane t_display;
    private JTextField t_input;
    private DefaultStyledDocument document;
    Socket socket;
    private Thread receiveThread = null;
    private String uid;

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

    private void printDisplay(String msg) {
        t_display.setCaretPosition(t_display.getDocument().getLength());
        int len = t_display.getDocument().getLength();
        /*try {
            document.insertString(len, msg + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        t_display.setCaretPosition(len);*/
        try {
            document.insertString(len, msg + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        t_display.setCaretPosition(len);


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

    private void sendUserID() {
        uid = t_userID.getText();
        send(new ChatMsg(uid, ChatMsg.MODE_LOGIN));
    }
    private String getLocalAddr() {
        InetAddress local = null;
        String addr = "";
        try {
            local = InetAddress.getLocalHost();
            addr = local.getHostAddress();
            System.out.println(addr);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return addr;
    }
    private void send(ChatMsg msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.out.println("클라 오류" + e.getMessage());
        }
    }
    private JPanel createInfoPanel() {
        // GridLayout을 사용하여 세로로 정렬
        JPanel p = new JPanel(new GridLayout(3, 2, 5, 5)); // 3행 2열, 컴포넌트 간 여백 설정

        t_userID = new JTextField(7);
        t_hostAddr = new JTextField(12);
        t_portNum = new JTextField(5);
        t_userID.setText("guest" + getLocalAddr().split("\\.")[3]);
        t_hostAddr.setText(this.serverAddress);
        t_portNum.setText(String.valueOf(this.serverPort));

        p.add(new JLabel("아이디:"));
        p.add(t_userID);
        p.add(new JLabel("서버주소:"));
        p.add(t_hostAddr);
        p.add(new JLabel("포트번호:"));
        p.add(t_portNum);

        return p;
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
        imagePanel.setBorder(BorderFactory.createEmptyBorder(200, 0, 100, 0)); // 상단과 하단에 여백 추가
        imagePanel.add(imageLabel, BorderLayout.CENTER);

        leftPanel.add(imagePanel, BorderLayout.NORTH); // 이미지 패널 추가

        // 하단 패널: InfoPanel과 ControlPanel을 좌우로 배치
        JPanel lowerPanel = new JPanel(new GridLayout(1, 2, 10, 10)); // 좌우로 나누는 레이아웃
        lowerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // 패딩 추가

        // InfoPanel과 ControlPanel 추가
        lowerPanel.add(createInfoPanel());  // 왼쪽: InfoPanel
        lowerPanel.add(createControlPanel()); // 오른쪽: ControlPanel

        // 하단 패널을 LeftPanel의 남쪽에 추가
        leftPanel.add(lowerPanel, BorderLayout.SOUTH);

        return leftPanel;
    }


    private JPanel createDisplayPanel() {
        JPanel p = new JPanel(new BorderLayout());
        document = new DefaultStyledDocument();
        t_display = new JTextPane(document);

        t_display.setEditable(false);
        p.add(new JScrollPane(t_display), BorderLayout.CENTER);

        return p;
    }
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel displayPanel = createDisplayPanel();
        panel.add(displayPanel, BorderLayout.CENTER); // 중앙에 배치해 가장 큰 영역 할당
        // Input 필드 및 버튼 패널 구성
        JPanel inputPanel = new JPanel(new BorderLayout());
        // 텍스트 입력 필드
        t_input = new JTextField(30);
        t_input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        inputPanel.add(t_input, BorderLayout.NORTH); // 입력 필드는 상단에 배치

        return panel;
    }

    private void sendMessage() {
        String message = t_input.getText();
        if (message.isEmpty()) return;

        send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING, message));

        t_input.setText(""); // 보낸 후 입력창은 비우기

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
    private JPanel createControlPanel() {
        b_start = new JButton("접속하기");

        b_start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                LoginGUI.this.serverAddress = t_hostAddr.getText();
                LoginGUI.this.serverPort = Integer.parseInt(t_portNum.getText());

                try {
                    connectToServer();
                    sendUserID();

                    // ServerRoomGUI 실행 (uid 전달)
                    SwingUtilities.invokeLater(() -> {
                        try {
                            new ServerRoomGUI(serverAddress, serverPort, uid);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    // 현재 LoginGUI 닫기
                    LoginGUI.this.dispose();
                } catch (Exception e) {
                    printDisplay("오류 발생: " + e.getMessage());
                }
            }
        });


        b_exit = new JButton("종료하기");
        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.exit(0);
            }
        });

        JPanel panel = new JPanel(new GridLayout(1, 3));
        panel.add(b_start);
        panel.add(b_exit);

        return panel;
    }

    private void connectToServer() throws UnknownHostException, IOException {
        socket = new Socket();
        SocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
        socket.connect(sa, 3000);
        out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        //in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        receiveThread = new Thread(new Runnable() {
            private ObjectInputStream in;

            private void receiveMessage() {
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

            @Override
            public void run() {
                try {
                    in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

                } catch (IOException e) {
                    printDisplay("입력 스트림이 열리지 않음");
                }
                while (receiveThread == Thread.currentThread()) {
                    receiveMessage();
                }
            }
        });
        receiveThread.start();
        b_start.setEnabled(true);
        b_exit.setEnabled(true);
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
        b_start.setEnabled(true);
        b_exit.setEnabled(false);
        b_exit.setEnabled(true);
    }


    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 54321;

        LoginGUI client = new LoginGUI(serverAddress, serverPort);
    }
}
