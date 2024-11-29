import javax.swing.*;
import java.awt.*;
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
    private JPanel roomPanel;
    private JPanel leftWrapperPanel;
    private ServerSocket serverSocket;
    private JTextArea t_display;
    private JButton b_connect, b_disconnect, b_exit;
    private Thread acceptThread = null;
    private Vector<ClientHandler> users = new Vector<ClientHandler>();
    private HashMap<Integer, ArrayList<String>> RoomNumUid = new HashMap<Integer, ArrayList<String>>();
    private UnoGameServerGUI unoGameServerGUI;
    private int viewingRoomNumber = 0;
    private int roomCount = 0;
    private HashMap<Integer, ArrayList<String>> ReadyMap = new HashMap<Integer, ArrayList<String>>();

    public ServerGUI(int port) {
        super("Uno Game");
        this.port = port;
        initializeGUI();
        startServerThread();
        setVisible(true);
    }

    // GUI 초기화
    private void initializeGUI() {
        this.setSize(870, 830);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);

        serverPanel = new JPanel(new BorderLayout());
        buildGUI();

        RoomNumUid.put(0, new ArrayList<String>());
        updateParticipantsPanel();

        setLayout(new BorderLayout());
        add(serverPanel, BorderLayout.EAST);

        leftWrapperPanel = createLeftWrapperPanel();
        add(leftWrapperPanel, BorderLayout.CENTER);
    }

    // 왼쪽 패널을 감싸는 외부 패널 생성
    private JPanel createLeftWrapperPanel() {
        leftWrapperPanel = new JPanel();
        leftWrapperPanel.setLayout(new BoxLayout(leftWrapperPanel, BoxLayout.Y_AXIS));
        leftWrapperPanel.add(createLeftPanel());
        return leftWrapperPanel;
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());

        JPanel leftTopPanel = new JPanel(new BorderLayout());
        leftTopPanel.add(createImageLabel(), BorderLayout.CENTER);

        roomPanel = new JPanel();
        roomPanel.setLayout(new BoxLayout(roomPanel, BoxLayout.Y_AXIS));
        roomPanel.setBorder(BorderFactory.createTitledBorder("방 목록"));

        leftPanel.add(leftTopPanel, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(roomPanel), BorderLayout.CENTER);

        return leftPanel;
    }

    // 이미지 라벨 생성
    private JLabel createImageLabel() {
        JLabel imageLabel = new JLabel();
        ImageIcon imageIcon = new ImageIcon("assets/UNO.PNG");
        Image scaledImage = imageIcon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(scaledImage));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setBorder(BorderFactory.createEmptyBorder(80, 0, 80, 0));
        return imageLabel;
    }

    // 방 추가 메서드
    private void addRoom() {
        int roomNumber = roomPanel.getComponentCount();
        JPanel singleRoomPanel = new JPanel(new BorderLayout());
        singleRoomPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        singleRoomPanel.setPreferredSize(new Dimension(550, 50));
        singleRoomPanel.setMaximumSize(new Dimension(550, 50));

        RoomNumUid.put(roomNumber + 1, new ArrayList<>());
        ReadyMap.put(roomNumber + 1, new ArrayList<>());

        JLabel roomLabel = new JLabel("방 " + (roomNumber + 1) + " (" + RoomNumUid.get(roomNumber + 1).size() + "/4)", SwingConstants.CENTER);
        JButton joinButton = new JButton("관전");

        joinButton.addActionListener(e -> handleRoomJoin(roomNumber + 1));

        singleRoomPanel.add(roomLabel, BorderLayout.CENTER);
        singleRoomPanel.add(joinButton, BorderLayout.EAST);

        roomPanel.add(singleRoomPanel);
        roomPanel.revalidate();
        roomPanel.repaint();
    }

    // 방 참가 처리
    private void handleRoomJoin(int roomNumber) {
        if (RoomNumUid.get(roomNumber).size() < 4) {
            printDisplay("현재 room " + roomNumber + "이 시작되지 않았습니다");
        } else {
            UnoGameViewing(roomNumber);
            t_display.setText("");
            updateParticipantsPanel();
        }
    }

    // GUI 구성 메서드
    private void buildGUI() {
        participantsPanel = new JPanel(new GridLayout(0, 1));
        participantsPanel.setBorder(BorderFactory.createTitledBorder("참가자 리스트"));
        JScrollPane scrollPane = new JScrollPane(participantsPanel);
        scrollPane.setPreferredSize(new Dimension(-1, 200));

        serverPanel.add(scrollPane, BorderLayout.NORTH);
        serverPanel.add(createDisplayPanel(), BorderLayout.CENTER);
        serverPanel.add(createControlPanel(), BorderLayout.SOUTH);
    }

    // 참가자 목록 갱신
    private void updateParticipantsPanel() {
        participantsPanel.removeAll();
        for (String uid : RoomNumUid.get(viewingRoomNumber)) {
            participantsPanel.add(new JLabel("UID: " + uid));
        }
        revalidate();
        repaint();
    }

    // 서버 시작 및 클라이언트 연결 처리
    private void startServerThread() {
        acceptThread = new Thread(() -> startServer());
        acceptThread.start();
    }

    // 서버 시작
    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            printDisplay("서버가 시작됐습니다." + getLocalAddr());
            while (acceptThread == Thread.currentThread()) {
                Socket clientSocket = serverSocket.accept();
                handleClientConnection(clientSocket);
            }
        } catch (IOException e) {
            printDisplay("서버 종료");
        }
    }

    // 클라이언트 연결 처리
    private void handleClientConnection(Socket clientSocket) {
        String clientAddr = clientSocket.getInetAddress().getHostAddress();
        t_display.append("클라이언트 연결: " + clientAddr + "\n");
        ClientHandler handler = new ClientHandler(clientSocket);
        users.add(handler);
        handler.start();
    }

    // 로컬 IP 주소 얻기
    private String getLocalAddr() {
        try {
            InetAddress local = InetAddress.getLocalHost();
            return local.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "";
        }
    }

    // 텍스트 디스플레이 패널 생성
    private JPanel createDisplayPanel() {
        t_display = new JTextArea();
        t_display.setEditable(false);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(t_display), BorderLayout.CENTER);
        return panel;
    }

    // 서버 제어 버튼 생성
    private JPanel createControlPanel() {
        b_connect = createButton("서버 시작", e -> startServerThread(), false);
        b_disconnect = createButton("서버 종료", e -> disconnect(), true);
        b_exit = createButton("종료하기", e -> exitServer(), false);

        JPanel panel = new JPanel(new GridLayout(0, 3));
        panel.add(b_connect);
        panel.add(b_disconnect);
        panel.add(b_exit);

        return panel;
    }

    // 버튼 생성 메서드
    private JButton createButton(String text, ActionListener action, boolean enabled) {
        JButton button = new JButton(text);
        button.setEnabled(enabled);
        button.addActionListener(action);
        return button;
    }

    // 메시지 출력
    private void printDisplay(String message) {
        t_display.append(message + "\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());
    }

    // 서버 종료 처리
    private void disconnect() {
        try {
            acceptThread = null;
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            System.err.println("서버 닫기 오류: " + e.getMessage());
        }
    }

    // 서버 종료 후 시스템 종료
    private void exitServer() {
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            System.err.println("서버 종료 오류: " + e.getMessage());
        }
        System.exit(0);
    }

    private void UnoGameViewing(int roomNumber) {
        viewingRoomNumber = roomNumber;
        remove(leftWrapperPanel);
        unoGameServerGUI = new UnoGameServerGUI(unoGame);
        add(unoGameServerGUI, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private void UnoGameUpdate() {
        remove(unoGameServerGUI);
        unoGameServerGUI = new UnoGameServerGUI(unoGame);
        add(unoGameServerGUI, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private void joinRoom(String uid, int roomNumber) {
        RoomNumUid.get(0).remove(uid);
        RoomNumUid.get(roomNumber).add(uid);
    }

    // 클라이언트 핸들러 클래스
    private class ClientHandler extends Thread {
        private final Socket clientSocket;
        private ObjectOutputStream out;
        private String uid;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        private void receiveMessages(Socket cs) {
            try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(cs.getInputStream()));
                 ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(cs.getOutputStream()))) {

                this.out = out;
                GamePacket msg;

                while ((msg = (GamePacket) in.readObject()) != null) {
                    processMessage(msg);
                }

            } catch (IOException | ClassNotFoundException e) {
                handleError(e);
            } finally {
                closeSocket(cs);
            }
        }

        private void processMessage(GamePacket msg) {
            int mode = msg.getMode();
            String userID = msg.getUserID();

            switch (mode) {
                case GamePacket.MODE_LOGIN:
                    handleLogin(userID);
                    break;
                case GamePacket.MODE_LOGOUT:
                    handleLogout();
                    break;
                case GamePacket.MODE_TX_STRING:
                    handleMessage(msg);
                    break;
                case GamePacket.MODE_TX_IMAGE:
                    handleImageMessage(msg);
                    break;
                case GamePacket.MODE_ROOM_ADD:
                    handleRoomAdd();
                    break;
                case GamePacket.MODE_ROOM_JOIN:
                    handleRoomJoin(msg);
                    break;
                case GamePacket.MODE_ROOM_READY:
                    handleReady(msg);
                    break;
                case GamePacket.MODE_UNO_UPDATE:
                    handleUnoUpdate(msg);
                    break;
                default:
                    break;
            }
        }

        private void handleLogin(String userID) {
            uid = userID;
            RoomNumUid.get(0).add(uid);
            sendRoomCount(uid);

            printDisplay("새 참가자: " + uid);
            printDisplay("현재 참가자 수: " + users.size());
            updateParticipantsPanel();
        }

        private void handleLogout() {
            RoomNumUid.get(0).remove(uid);
            updateParticipantsPanel();
            users.remove(this);
            printDisplay(uid + " 퇴장. 현재 참가자 수: " + users.size());
        }

        private void handleMessage(GamePacket msg) {
            String message = uid + ": " + msg.getMessage();
            printDisplay(message);
            broadcasting(msg);
        }

        private void handleImageMessage(GamePacket msg) {
            printDisplay(uid + ": " + msg.getMessage());
            broadcasting(msg);
        }

        private void handleRoomAdd() {
            addRoom();
            roomCount++;

            printDisplay(uid + ": 방 추가 요청");
            printDisplay("[방 목록 업데이트]");
            broadcastingRoomUpdate();
        }

        private void handleRoomJoin(GamePacket msg) {
            printDisplay(uid + ": " + msg.getRoomNum() + "방 입장");
            joinRoom(msg.getUserID(), msg.getRoomNum());

            broadcastingRoomJoin(msg.getRoomNum());

            updateParticipantsPanel();
        }

        private void handleReady(GamePacket msg) {
            ArrayList<String> playerList = ReadyMap.get(msg.getRoomNum());
            playerList.add(msg.getUserID());
            ReadyMap.put(msg.getRoomNum(),playerList);

            broadcastingReady(msg.getRoomNum(), ReadyMap.get(msg.getRoomNum()));

            if (ReadyMap.get(msg.getRoomNum()).size() == 4) {
                unoGame = new UnoGame();
                unoGame.setPlayers(RoomNumUid.get(msg.getRoomNum()));
                unoGame.startGame();

                broadcastingUnoStart(msg.getRoomNum());
            }
            updateParticipantsPanel();
        }

        private void handleUnoUpdate(GamePacket msg) {
            printDisplay(uid + ": 플레이 완료");
            unoGame = msg.getUno();
            UnoGameUpdate();
            broadcastingUnoUpdate(msg.getRoomNum());
        }

        private void sendJoinRoom(int roomNum, String uid, HashMap<Integer, ArrayList<String>> roomState) {
            send(new GamePacket(uid, GamePacket.MODE_ROOM_JOIN, roomState, ReadyMap.get(roomNum), roomNum));
        }

        private void sendRoomCount(String uid) {
            send(new GamePacket(uid, GamePacket.MODE_ROOM_COUNT, roomCount, 0));
        }

        private void send(GamePacket msg) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                System.err.println("클라이언트 전송 오류: " + e.getMessage());
            }
        }

        private void broadcasting(GamePacket msg) {
            for (ClientHandler client : users) {
                client.send(msg);
            }
        }

        private void broadcastingReady(int roomNum, ArrayList<String> readyList) {
            for (ClientHandler client : users) {
                client.sendReady(roomNum, readyList);
            }
        }

        private void broadcastingUnoStart(int roomNum) {
            for (ClientHandler client : users) {
                client.sendUnoStart(roomNum);
            }
        }

        private void broadcastingUnoUpdate(int roomNum) {
            for (ClientHandler client : users) {
                client.sendUnoUpdate(roomNum);
            }
        }

        private void broadcastingRoomJoin(int roomNum) {
            for (ClientHandler client : users) {
                client.sendJoinRoom(roomNum, client.uid, RoomNumUid);
            }
        }

        private void broadcastingRoomUpdate() {
            for (ClientHandler client : users) {
                client.sendRoomCount(client.uid);
            }
        }

        private void sendReady(int roomNum, ArrayList<String> readyList) {
            send(new GamePacket(uid, GamePacket.MODE_ROOM_READY, RoomNumUid, readyList, roomNum));
        }

        private void sendUnoStart(int roomNum) {
            send(new GamePacket(uid, GamePacket.MODE_UNO_START, unoGame, roomNum));
        }

        private void sendUnoUpdate(int roomNum) {
            send(new GamePacket(uid, GamePacket.MODE_UNO_UPDATE, unoGame, roomNum));
        }

        private void closeSocket(Socket cs) {
            try {
                cs.close();
            } catch (IOException e) {
                System.err.println("서버 닫기 오류: " + e.getMessage());
            }
        }

        private void handleError(Exception e) {
            users.remove(this);
            System.err.println("서버 처리 오류: " + e.getMessage());
        }

        @Override
        public void run() {
            receiveMessages(clientSocket);
        }
    }

    public static void main(String[] args) {
        int port = 54321;
        new ServerGUI(port);
    }
}