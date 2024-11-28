import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

public class ServerGUI extends JFrame {
    private UnoGame unoGame;
    private int port;
    private JPanel serverPanel;
    private JPanel participantsPanel;
    private JPanel leftWrapperPanel;
    private ServerSocket serverSocket;
    private JTextArea t_display;
    private JButton b_connect, b_disconnect, b_exit;
    private Thread acceptThread = null;
    private Vector<ClientHandler> users = new Vector<ClientHandler>();
    private HashMap<Integer, List<String>> RoomNumUid = new HashMap<Integer, List<String>>();
    private UnoGameServerGUI unoGameServerGUI;
    private int viewingRoomNumber = 0;

    public ServerGUI(int port) {
        super("Uno Game");
        this.port = port;
        this.setSize(870, 830);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);

        serverPanel = new JPanel(new BorderLayout());
        buildGUI();

        RoomNumUid.put(0, new ArrayList<String>());

        updateParticipantsPanel();

        setLayout(new BorderLayout());
        // 채팅 패널을 오른쪽에 추가
        add(serverPanel, BorderLayout.EAST);  // 채팅 패널 추가

        // 왼쪽 패널을 감싸는 외부 패널 생성
        leftWrapperPanel = new JPanel();
        leftWrapperPanel.setLayout(new BoxLayout(leftWrapperPanel, BoxLayout.Y_AXIS));  // 세로로 배치
        leftWrapperPanel.add(createLeftPanel());  // createLeftPanel()을 내부에 추가

        add(leftWrapperPanel, BorderLayout.CENTER);  // leftWrapperPanel을 WEST에 추가



        acceptThread = new Thread(new Runnable() {
            @Override
            public void run() {
                startServer();
            }
        });
        acceptThread.start();

        setVisible(true);
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());

        JPanel leftTopPanel = new JPanel(new BorderLayout());

        // 이미지 영역 추가
        JLabel imageLabel = new JLabel();
        ImageIcon imageIcon = new ImageIcon("assets/UNO.PNG");
        Image scaledImage = imageIcon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH); // 이미지 크기 조정
        imageLabel.setIcon(new ImageIcon(scaledImage));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER); // 수평 중앙 정렬
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);   // 수직 중앙 정렬
        imageLabel.setBorder(BorderFactory.createEmptyBorder(80, 0, 80, 0)); // 여백 설정
        leftTopPanel.add(imageLabel, BorderLayout.CENTER);

        // 방 추가 버튼
        JButton addRoomButton = new JButton("방 추가");
        leftTopPanel.add(addRoomButton, BorderLayout.SOUTH);

        // BoxLayout을 사용하여 세로로 방 배치
        JPanel roomPanel = new JPanel();
        roomPanel.setLayout(new BoxLayout(roomPanel, BoxLayout.Y_AXIS));  // 세로로 배치
        roomPanel.setBorder(BorderFactory.createTitledBorder("방 목록"));

        addRoomButton.addActionListener(e -> addRoom(roomPanel));  // 버튼 클릭 시 방 추가


        leftPanel.add(leftTopPanel, BorderLayout.NORTH); // 버튼 패널을 상단에 추가
        leftPanel.add(new JScrollPane(roomPanel), BorderLayout.CENTER); // 방 목록을 중앙에 추가

        return leftPanel;
    }

    // 방을 동적으로 추가하는 메서드
    private void addRoom(JPanel roomPanel) {
        // 방 번호 계산 (현재 방 갯수 + 1)
        int roomNumber = roomPanel.getComponentCount();  // 방 번호를 자동으로 증가시킴

        JPanel singleRoomPanel = new JPanel(new BorderLayout());
        singleRoomPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // 각 방의 크기를 고정 (예: 250x50 크기로 설정)
        singleRoomPanel.setPreferredSize(new Dimension(550, 50));  // 방 크기 고정

        // BoxLayout에서 크기 고정을 위해 강제로 레이아웃 갱신
        singleRoomPanel.setMaximumSize(new Dimension(550, 50));  // 방 크기 고정

        RoomNumUid.put(roomNumber+1, new ArrayList<String>());
        JLabel roomLabel = new JLabel("방 " + (roomNumber + 1) + " (" + RoomNumUid.get(roomNumber+1).size() + "/4)", SwingConstants.CENTER);
        JButton joinButton = new JButton("관전");

        joinButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if(RoomNumUid.get(roomNumber+1).isEmpty()){
                    printDisplay("현재 room " + (roomNumber + 1) + "에 유저가 없습니다.");
                }else {
                    UnoGameViewing(roomNumber + 1);
                }
            }
        });

        singleRoomPanel.add(roomLabel, BorderLayout.CENTER);
        singleRoomPanel.add(joinButton, BorderLayout.EAST);

        roomPanel.add(singleRoomPanel);  // 새 방 추가

        // 레이아웃 갱신 (패널에 새로 추가된 방을 반영)
        roomPanel.revalidate();
        roomPanel.repaint();
    }

    private void buildGUI() {

        // 참가자 패널을 생성하여 스크롤 패널로 감싸기
        participantsPanel = new JPanel(new GridLayout(0, 1)); // 세로로 리스트가 쌓이도록 설정
        participantsPanel.setBorder(BorderFactory.createTitledBorder("참가자 리스트"));

        // 참가자 목록을 스크롤 가능한 패널로 변환
        JScrollPane scrollPane = new JScrollPane(participantsPanel);
        scrollPane.setPreferredSize(new Dimension(-1, 200)); // 스크롤 패널 크기 설정

        serverPanel.add(scrollPane, BorderLayout.NORTH);

        serverPanel.add(createDisplayPanel(), BorderLayout.CENTER);
        serverPanel.add(createControlPanel(), BorderLayout.SOUTH);
    }

    // 참가자 목록에 추가할 메서드
    private void updateParticipantsPanel() {
        participantsPanel.removeAll(); // 기존 참가자 목록을 삭제

        // playersUid에 있는 모든 UID를 리스트에 표시
        for (int i = 0; i < RoomNumUid.get(viewingRoomNumber).size(); i++) {
            JLabel uidLabel = new JLabel();

            // UID만 표시
            uidLabel.setText("UID: " + RoomNumUid.get(viewingRoomNumber).get(i));
            uidLabel.setForeground(Color.BLACK); // 텍스트 색상은 검정

            // UID 라벨의 크기 고정 (너비 350, 높이 40)
            uidLabel.setPreferredSize(new Dimension(-1, 40));

            // 참가자 패널에 UID 추가
            participantsPanel.add(uidLabel);
        }

        revalidate(); // 패널을 갱신하여 최신 상태 반영
        repaint(); // 화면을 새로 그리기
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

    private void startServer() {
        Socket clientSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            printDisplay("서버가 시작됐습니다." + getLocalAddr());
            while (acceptThread == Thread.currentThread()) { // 클라이언트 접속 기다림
                clientSocket = serverSocket.accept();
                String cAddr = clientSocket.getInetAddress().getHostAddress();
                t_display.append("클라이언트 연결:" + cAddr + "\n");
                ClientHandler cHandler = new ClientHandler(clientSocket);
                users.add(cHandler);
                cHandler.start();
            }
        } catch (SocketException e) {
            printDisplay("서버 소캣 종료");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null) clientSocket.close();
                if (serverSocket != null) serverSocket.close();
            } catch (IOException e) {
                System.err.println("서버 닫기 오류 > " + e.getMessage());
                System.exit(-1);
            }
        }
    }

    private JPanel createDisplayPanel() { // 최상단 JTextArea
        t_display = new JTextArea();
        t_display.setEditable(false);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(t_display), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createControlPanel() { // 제일 밑단 종료 버튼

        b_connect = new JButton("서버 시작");
        b_connect.setEnabled(false); // 처음엔 비활성화
        b_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                acceptThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startServer();
                    }
                });
                acceptThread.start();
                //접속 끊기 전에는 종료하거나 다시 접속하기 불가
                b_connect.setEnabled(false);
                b_disconnect.setEnabled(true);
                b_exit.setEnabled(false);

            }
        });

        b_disconnect = new JButton("서버 종료");
        b_disconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                disconnect();
                b_connect.setEnabled(true);
                b_disconnect.setEnabled(false);
                b_exit.setEnabled(true);
            }
        });

        b_exit = new JButton("종료하기");
        b_exit.setEnabled(false); // 처음엔 비활성화
        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    if (serverSocket != null) serverSocket.close();
                } catch (IOException e) {
                    System.err.println("서버 닫기 오류 > " + e.getMessage());
                }
                System.exit(-1);
            }
        });

        JPanel panel = new JPanel(new GridLayout(0, 3));

        panel.add(b_connect);
        panel.add(b_disconnect);
        panel.add(b_exit);

        return panel;
    }


    private void printDisplay(String message) {
        t_display.append(message + "\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());
    }

    private void disconnect() {
        try {
            acceptThread = null;
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("서버 닫기 오류 > " + e.getMessage());
            System.exit(-1);
        }
    }

    private void UnoGameViewing(int roomNumber) {
        viewingRoomNumber = roomNumber;
        remove(leftWrapperPanel);
        unoGame = new UnoGame();
        unoGame.setPlayers(RoomNumUid.get(viewingRoomNumber));
        // 우노 게임 패널
        unoGameServerGUI = new UnoGameServerGUI(unoGame);
        add(unoGameServerGUI, BorderLayout.CENTER);
//        unoGameServerGUI.gameStartUp();

//        broadcastingUnoStart();

        revalidate(); // 레이아웃을 갱신
        repaint(); // 화면을 새로 그리기
    }

    private void UnoGameUpdate() {
        remove(unoGameServerGUI);

        // 우노 게임 패널
        unoGameServerGUI = new UnoGameServerGUI(unoGame);
        add(unoGameServerGUI, BorderLayout.CENTER); // centerPanel을 중앙에 추가

        revalidate(); // 레이아웃을 갱신
        repaint(); // 화면을 새로 그리기
    }

    private class ClientHandler extends Thread {
        private final Socket clientSocket;
        private ObjectOutputStream out;
        private String uid;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;

        }

        private void receiveMessages(Socket cs) {
            try {
                ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(cs.getInputStream()));
                out = new ObjectOutputStream(new BufferedOutputStream(cs.getOutputStream()));


                String message;
                ChatMsg msg;
                while ((msg = (ChatMsg) in.readObject()) != null) {
                    if (msg.mode == ChatMsg.MODE_LOGIN) {
                        uid = msg.userID;

                        List<String> playersUid = RoomNumUid.get(0);
                        playersUid.add(uid);
                        RoomNumUid.put(0,playersUid);

                        printDisplay("새 참가자: " + uid);
                        printDisplay("현재 참가자 수: " + users.size());
                        updateParticipantsPanel(); // 참가자 목록 갱신
                        continue;
                    }
                    else if (msg.mode == ChatMsg.MODE_LOGOUT) {
                        break;
                    }
                    else if (msg.mode == ChatMsg.MODE_TX_STRING) {
                        message = uid + ": " + msg.message;

                        printDisplay(message);
                        broadcasting(msg);
                    }
                    else if (msg.mode == ChatMsg.MODE_TX_IMAGE) {
                        printDisplay(uid + ": " + msg.message);
                        broadcasting(msg);
                    }
                    else if (msg.mode == ChatMsg.MODE_UNO_UPDATE){
                        printDisplay(uid + ": 플레이 완료");
                        unoGame = msg.uno;

                        UnoGameUpdate();
                        broadcastingUnoUpdate();
                    }
                }

                RoomNumUid.get(0).remove(uid);
                updateParticipantsPanel(); // 참가자 목록 갱신
                users.removeElement(this);
                printDisplay(uid + " 퇴장. 현재 참가자 수: " + users.size());
            } catch (IOException e) {
                users.removeElement(this);
                System.err.println("서버 읽기 오류: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    cs.close();
                } catch (IOException e) {
                    System.err.println("서버 닫기 오류: " + e.getMessage());
                    System.exit(-1);
                }
            }
        }

        private void send(ChatMsg msg) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                System.err.println("클라이언트 일반 전송 오류>" + e.getMessage());
            }
        }

        private void sendMessage(String msg) {
            send(new ChatMsg(uid, ChatMsg.MODE_LOGIN, msg));
        }

        private void sendUnoStart() {
            send(new ChatMsg(uid, ChatMsg.MODE_UNO_START, unoGame));
        }
        private void sendUnoUpdate() {
            send(new ChatMsg(uid, ChatMsg.MODE_UNO_UPDATE, unoGame));
        }

        private void broadcasting(ChatMsg msg) {
            for (ClientHandler c : users) {
                c.send(msg);
            }
        }

        private void broadcastingUnoUpdate() {
            for (ClientHandler c : users) {
                c.sendUnoUpdate();
            }
        }

        private void broadcastingUnoStart() {
            for (ClientHandler c : users) {
                c.sendUnoStart();
            }
        }

        @Override
        public void run() {
            // 특정 소캣에 대해서 receiveMessages
            receiveMessages(clientSocket);
        }
    }

    public static void main(String[] args) {
        int port = 54321;
        ServerGUI server = new ServerGUI(port);
    }
}