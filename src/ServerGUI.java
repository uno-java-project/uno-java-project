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
    private HashMap<Integer, List<String>> RoomNumUid = new HashMap<Integer, List<String>>();
    private HashMap<Integer, List<String>> ReadyProgress = new HashMap<Integer, List<String>>();
    private UnoGameServerGUI unoGameServerGUI;
    private int viewingRoomNumber = 0;
    private int roomCount = 0;

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

    private void deleteRoom(int roomNumber) {
        if (roomNumber > 0 && roomNumber <= roomCount) {
            roomPanel.remove(roomNumber - 1); // 방 UI 제거 (인덱스는 0부터 시작)
            roomPanel.revalidate();
            roomPanel.repaint();
        } else {
            printDisplay("삭제하려는 방 번호가 유효하지 않습니다: " + roomNumber);
        }
    }


    // 방 추가 메서드
    private void addRoom() {
        int roomNumber = roomPanel.getComponentCount();
        JPanel singleRoomPanel = new JPanel(new BorderLayout());
        singleRoomPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        singleRoomPanel.setPreferredSize(new Dimension(550, 50));
        singleRoomPanel.setMaximumSize(new Dimension(550, 50));

        ReadyProgress.put(roomNumber, new ArrayList<String>());

        RoomNumUid.put(roomNumber + 1, new ArrayList<>());
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
        if (ReadyProgress.get(roomNumber).size() < 4) {
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
        unoGameServerGUI = new UnoGameServerGUI(unoGame, viewingRoomNumber);
        add(unoGameServerGUI, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private void UnoGameUpdate() {
        if(unoGameServerGUI!=null){
            remove(unoGameServerGUI);
        }
        unoGameServerGUI = new UnoGameServerGUI(unoGame, viewingRoomNumber);

        if(viewingRoomNumber!=0){
            add(unoGameServerGUI, BorderLayout.CENTER);
        }

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
                    handleRoomReady(msg);
                    break;
                case GamePacket.MODE_UNO_UPDATE:
                    handleUnoUpdate(msg);
                    break;
                case GamePacket.MODE_UNO_GAME_OVER:
                    handleGameOver(msg);
                    break;
                case GamePacket.MODE_ROOM_DELETE:
                    int roomToDelete = msg.getRoomNum();

                    // 유효한 방 번호인지 확인
                    if (roomToDelete > 0 && roomToDelete <= roomCount) {
                        deleteRoom(roomToDelete); // 방 삭제
                        handleRoomDelete(roomToDelete); // 방 삭제 후 후속 처리
                    } else {
                        printDisplay("잘못된 방 번호 요청: " + roomToDelete);
                    }
                    break;

                default:
                    break;

            }
        }
        private void handleGameOver(GamePacket msg) {
            String winner = msg.getUserID();
            int roomNumber = msg.getRoomNum();

            System.out.println("서버에서 게임 종료 패킷 수신: 방 번호 = " + roomNumber + ", 승자 = " + winner);

            printDisplay("방 " + roomNumber + "에서 게임이 종료되었습니다. 승자는 " + winner + "입니다!");
            broadcastingMessages(roomNumber, "게임이 종료되었습니다! 승리자는 " + winner + "입니다!");
            resetRoom(roomNumber); // 방 상태 초기화
        }



        private void updateGUIOnGameOver(int roomNumber, String winner) {
            if (unoGameServerGUI != null) {
                unoGameServerGUI.updateOnGameOver(roomNumber, winner);
            }
        }

        private void resetRoom(int roomNumber) {
            // ReadyProgress와 RoomNumUid를 초기화하여 방을 리셋
            ReadyProgress.put(roomNumber, new ArrayList<>());
            RoomNumUid.put(roomNumber, new ArrayList<>());
            unoGame = null; // 게임 상태 초기화
            broadcastRoomInfo(roomNumber);
        }
        private void handleLogin(String userID) {
            uid = userID;
            RoomNumUid.get(0).add(uid);
            sendRoomCount();

            printDisplay("새 참가자: " + uid);
            printDisplay("현재 참가자 수: " + users.size());

            String broadMsg = uid + "님이 로그인 하였습니다.";
            broadcastingMessages(0, broadMsg);

            int participantsCount;

            for (int i = RoomNumUid.size() - 1; i > 0; i--) {
                participantsCount = RoomNumUid.get(i).size();
                sendRoomInfo(i, participantsCount);
            }

            updateParticipantsPanel();
        }

        private void handleLogout() {
            RoomNumUid.get(0).remove(uid);
            updateParticipantsPanel();
            users.remove(this);
            printDisplay(uid + " 퇴장. 현재 참가자 수: " + users.size());
        }

        private void handleRoomReady(GamePacket msg) {
            int roomNumber = msg.getRoomNum();
            String userID = msg.getUserID();

            // 레디 상태 업데이트
            if (ReadyProgress.get(roomNumber).contains(userID)) {
                ReadyProgress.get(roomNumber).remove(userID); // 레디 취소
            } else {
                ReadyProgress.get(roomNumber).add(userID); // 레디 추가
            }

            // 모든 클라이언트로 레디 상태 전송
            int readyCount = ReadyProgress.get(roomNumber).size();
            int joinCount = RoomNumUid.get(roomNumber).size();
            broadcastingReady(roomNumber, readyCount, joinCount, ReadyProgress.get(roomNumber)); // 레디 상태 리스트 추가

            // 게임 시작 조건 확인
            if (readyCount == 4) {
                unoGame = new UnoGame();
                unoGame.setPlayers(RoomNumUid.get(roomNumber));
                unoGame.startGame();

                broadcastingUnoStart(roomNumber);
            }
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
        private void handleRoomDelete(int roomNumber) {
            // 방 번호 재정렬
            for (int i = roomNumber; i < roomCount; i++) {
                RoomNumUid.put(i, RoomNumUid.remove(i + 1)); // 다음 방을 현재 방 번호로 이동
                ReadyProgress.put(i, ReadyProgress.remove(i + 1)); // 준비 상태도 이동
            }
            RoomNumUid.remove(roomCount); // 마지막 방 제거
            ReadyProgress.remove(roomCount);
            roomCount--; // 방 개수 감소

            // 클라이언트로 방 갱신 정보 브로드캐스트
            broadcastingRoomUpdate();
            printDisplay("방 " + roomNumber + "이 삭제되었습니다.");
        }



        private void handleRoomAdd() {
            addRoom();
            roomCount++;

            printDisplay(uid + ": 방 삭제 요청");
            printDisplay("방이 삭제 되었습니다.");

            broadcastingRoomUpdate();

            int participantsCount = RoomNumUid.get(roomCount).size();
            sendRoomInfo(roomCount, participantsCount);
        }
        private void broadcastRoomInfo(int roomNumber) {
            int currentParticipants = RoomNumUid.get(roomNumber).size();
            for (ClientHandler client : users) {
                client.sendRoomInfo(roomNumber, currentParticipants);

            }
        }

        private void sendRoomInfo(int roomNumber, int participantsCount) {
            send(new GamePacket(uid, GamePacket.MODE_ROOM_INFO, null, null, null, 0, 0, 0, roomNumber, participantsCount));
        }

        private void handleRoomJoin(GamePacket msg) {
            int roomNumber = msg.getRoomNum();
            String broadMsg = uid + "님이  " + msg.getRoomNum() + "번 방에 입장하였습니다.";
            broadcastingMessages(msg.getRoomNum(), broadMsg);
            printDisplay(uid + "님이  " + msg.getRoomNum() + "번 방에 입장하였습니다.");

            joinRoom(msg.getUserID(), msg.getRoomNum());

            // ReadyProgress.get()이 null일 경우, 빈 리스트를 반환하도록 처리
            List<String> readyList = ReadyProgress.get(msg.getRoomNum());
            if (readyList == null) {
                readyList = new ArrayList<>(); // 빈 리스트로 초기화
                ReadyProgress.put(msg.getRoomNum(), readyList); // 맵에 새로운 리스트 추가
            }

            Integer joinProgress = RoomNumUid.get(msg.getRoomNum()).size();
            Integer readyProgress = ReadyProgress.get(msg.getRoomNum()).size();


            broadcastingJoin(readyProgress, joinProgress, msg.getRoomJoin());

            // 참가자 수 업데이트 후 메인 클라이언트에 전송
            broadcastRoomInfo(roomNumber);

            // 참가자 목록 UI 갱신
            updateRoomLabel(roomNumber);
            updateParticipantsPanel();
        }

        private void updateRoomLabel(int roomNumber) {
            JPanel singleRoomPanel = (JPanel) roomPanel.getComponent(roomNumber - 1); // 방 번호는 1부터 시작
            JLabel roomLabel = (JLabel) singleRoomPanel.getComponent(0); // 라벨은 첫 번째 컴포넌트로 가정

            int currentParticipants = RoomNumUid.get(roomNumber).size();
            roomLabel.setText("방 " + roomNumber + " (" + currentParticipants + "/4)");

            roomPanel.revalidate();
            roomPanel.repaint();
        }

        private void handleUnoUpdate(GamePacket msg) {
            printDisplay(uid + "님 플레이 완료");
            unoGame = msg.getUno();
            UnoGameUpdate();
            broadcastingUnoUpdate(msg.getRoomNum());
        }

        private void sendRoomCount() {
            send(new GamePacket(uid, GamePacket.MODE_ROOM_COUNT, null, null, null, roomCount, 0, 0, 0, 0));
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

        private void broadcastingJoin(Integer readyProgress, Integer joinProgress, int roomNum) {
            for (ClientHandler client : users) {
                client.sendJoin(readyProgress, joinProgress, roomNum);
            }
        }
        private void broadcastingDelete(Integer readyProgress, Integer joinProgress, int roomNum) {
            for (ClientHandler client : users) {
                client.sendDelete(readyProgress, joinProgress, roomNum);
            }
        }
        private void broadcastingMessages(int roomNum, String msg) {
            for (ClientHandler client : users) {
                    System.out.println("클라이언트로 메시지 전송: " + msg + " (방 번호 = " + roomNum + ")");
                    client.send(new GamePacket(null, GamePacket.MODE_BROAD_STRING, msg, roomNum));
            }
        }

        private void broadcastingUnoStart(int roomNum) {
            for (ClientHandler client : users) {
                client.sendUnoStart(roomNum);
            }
        }

        private void broadcastingReady(int roomNum, int ready, int joinRoom, List<String> readyUsers) {
            for (ClientHandler client : users) {
                client.sendReady(roomNum, ready, joinRoom);
            }
        }


        private void broadcastingUnoUpdate(int roomNum) {
            for (ClientHandler client : users) {
                client.sendUnoUpdate(roomNum);
            }
        }

        private void broadcastingRoomUpdate() {
            for (ClientHandler client : users) {
                client.send(new GamePacket(null, GamePacket.MODE_ROOM_COUNT, null, null, null, roomCount, 0, 0, 0, 0));
            }
        }



        private void sendMessages(String msg, int roomNum){
            send(new GamePacket(uid, GamePacket.MODE_BROAD_STRING ,msg ,roomNum));
        }

        private void sendJoin(Integer readyProgress, Integer joinProgress, int roomNum){
            send(new GamePacket(uid, GamePacket.MODE_ROOM_JOIN, readyProgress, joinProgress ,roomNum));
        }
        private void sendDelete(Integer readyProgress, Integer joinProgress, int roomNum){
            send(new GamePacket(uid, GamePacket.MODE_ROOM_DELETE, readyProgress, joinProgress ,roomNum));
        }
        private void sendUnoStart(int roomNum) {
            send(new GamePacket(uid, GamePacket.MODE_UNO_START, unoGame, roomNum));
        }

        private void sendReady(int roomNum, Integer ready, Integer joinRoom) {
            send(new GamePacket(uid, GamePacket.MODE_ROOM_READY, ready, joinRoom, roomNum));
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