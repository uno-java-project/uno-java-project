import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClientGUI extends JFrame {
    private String serverAddress;
    private int serverPort;
    private ObjectOutputStream out;
    private JPanel leftWrapperPanel;
    private JPanel roomPanel;
    private JPanel readyRoomPanel;

    private JButton b_exit, b_select, b_disconnect;;
    private JTextPane t_display;
    private JTextField t_input;
    private DefaultStyledDocument document;
    Socket socket;
    private Thread receiveThread = null;
    public String uid;
    private JPanel currentUNOGUI;
    public int myRoomNumber = 0;
    private int roomCount;

    private HashMap<Integer, ArrayList<String>> RoomNumUid = new HashMap<Integer, ArrayList<String>>();
    public ArrayList<String> ReadyList = new ArrayList<>();

    public ClientGUI(String uid, String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.uid = uid;

        buildGUI();


        try {
            connectToServer();
            sendUserID();
        } catch (UnknownHostException e1) {
            printDisplay("서버 주소와 포트번호를 확인하세요: "+ e1.getMessage());
            return;
        } catch (IOException e1) {
            printDisplay("서버와 연결 오류: "+ e1.getMessage());
            return;
        }

        this.setBounds(0, 0, 1000, 800);
        this.setTitle("WithTalk");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    private void buildGUI() {
        // 왼쪽 패널을 감싸는 외부 패널 생성
        leftWrapperPanel = new JPanel();
        leftWrapperPanel.setLayout(new BoxLayout(leftWrapperPanel, BoxLayout.Y_AXIS));  // 세로로 배치
        leftWrapperPanel.add(createLeftPanel());  // createLeftPanel()을 내부에 추가

        this.add(leftWrapperPanel, BorderLayout.CENTER);  // leftWrapperPanel을 WEST에 추가
        this.add(createRightPanel(), BorderLayout.EAST);
    }

    public void printDisplay(String msg) {
        t_display.setCaretPosition(t_display.getDocument().getLength());
        int len = t_display.getDocument().getLength();
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
        send(new GamePacket(uid, GamePacket.MODE_TX_IMAGE, file.getName(), icon, myRoomNumber));
        t_input.setText("");
    }

    public void send(GamePacket msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.out.println("클라 오류" + e.getMessage());
        }
    }

    private void updateRoom() {
        // 기존 방 패널 초기화
        roomPanel.removeAll(); // 기존 방 목록 삭제

        // 방 생성 로직
        for (int i = 0; i < roomCount; i++) {
            JPanel singleRoomPanel = new JPanel(new BorderLayout());
            singleRoomPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

            // 각 방의 크기를 고정 (예: 550x50 크기로 설정)
            singleRoomPanel.setPreferredSize(new Dimension(550, 50));  // 방 크기 고정
            singleRoomPanel.setMaximumSize(new Dimension(550, 50));  // 방 크기 고정

            // 방 번호는 1부터 시작하도록
            int roomNumber = i + 1;  // 1부터 시작하는 방 번호

            // 방의 UID 리스트를 RoomNumUid에 추가 (초기값은 빈 리스트)
            RoomNumUid.put(roomNumber, new ArrayList<String>());

            // 방 이름을 레이블로 설정
            JLabel roomLabel = new JLabel("방 " + roomNumber + " (" + RoomNumUid.get(roomNumber).size() + "/4)", SwingConstants.CENTER);

            // 참가 버튼 생성
            JButton joinButton = new JButton("참가");

            // 참가 버튼 클릭 시 해당 방 번호로 이동 (myRoomNumber에 값 할당)
            joinButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    myRoomNumber = roomNumber;
                    sendJoinRoom(myRoomNumber);
                    remove(leftWrapperPanel);
                    readyRoomPanel = new ClientReadyRoomGUI(ClientGUI.this, roomNumber, RoomNumUid.get(roomNumber), ReadyList);
                    add(readyRoomPanel, BorderLayout.CENTER);
                    revalidate();
                    repaint();
                }
            });

            // 방 레이블과 버튼을 방 패널에 추가
            singleRoomPanel.add(roomLabel, BorderLayout.CENTER);
            singleRoomPanel.add(joinButton, BorderLayout.EAST);

            // 새 방을 roomPanel에 추가
            roomPanel.add(singleRoomPanel);
        }

        // 레이아웃 갱신 (새로 추가된 방을 반영)
        roomPanel.revalidate();
        roomPanel.repaint();
    }

//    private JLabel waitingLabel(){
//        // 상단 이미지 영역: 비율 증가 및 중앙 정렬
//        JLabel imageLabel = new JLabel();
//        ImageIcon imageIcon = new ImageIcon("assets/waiting.png");
//        Image scaledImage = imageIcon.getImage().getScaledInstance(710, 800, Image.SCALE_SMOOTH); // 이미지 크기를 더 키움
//        imageLabel.setIcon(new ImageIcon(scaledImage));
////        imageLabel.setHorizontalAlignment(SwingConstants.CENTER); // 수평 중앙 정렬
////        imageLabel.setVerticalAlignment(SwingConstants.CENTER);   // 수직 중앙 정렬
//
//        return imageLabel;
//    }

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
        roomPanel = new JPanel();
        roomPanel.setLayout(new BoxLayout(roomPanel, BoxLayout.Y_AXIS));  // 세로로 배치
        roomPanel.setBorder(BorderFactory.createTitledBorder("방 목록"));

        addRoomButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendAddRoom(uid);
                updateRoom();
            }
        });

        leftPanel.add(leftTopPanel, BorderLayout.NORTH); // 버튼 패널을 상단에 추가
        leftPanel.add(new JScrollPane(roomPanel), BorderLayout.CENTER); // 방 목록을 중앙에 추가

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
        panel.setPreferredSize(new Dimension(270, 800));

        JPanel displayPanel = createDisplayPanel();
        panel.add(displayPanel, BorderLayout.CENTER); // 중앙에 배치해 가장 큰 영역 할당

        // Input 필드 및 버튼 패널 구성
        JPanel inputPanel = new JPanel(new BorderLayout());

        // 텍스트 입력 필드
        t_input = new JTextField(15);
        t_input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        inputPanel.add(t_input, BorderLayout.NORTH); // 입력 필드는 상단에 배치

        // 버튼 패널 (보내기, 선택하기 버튼)
        JPanel p_button = new JPanel(new GridLayout(1, 3, 5, 5)); // 가로로 두 개 버튼 배치
        b_exit = new JButton("종료하기");
        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.exit(0);
            }
        });

        b_disconnect = new JButton("접속 끊기");
        b_disconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnect();
            }
        });

        b_select = new JButton("선택하기");
        b_select.addActionListener(new ActionListener() {
            JFileChooser chooser = new JFileChooser();

            @Override
            public void actionPerformed(ActionEvent e) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "JPG & GIF & PNG Images",
                        "jpg", "gif", "png");
                chooser.setFileFilter(filter);

                int ret = chooser.showOpenDialog(ClientGUI.this);
                if (ret != JFileChooser.APPROVE_OPTION) {
                    JOptionPane.showMessageDialog(ClientGUI.this, "파일을 선택하지 않았습니다");
                    return;
                }
                t_input.setText(chooser.getSelectedFile().getAbsolutePath());
                sendImage();
            }
        });

        // 버튼들을 버튼 패널에 추가
        p_button.add(b_select);
        p_button.add(b_disconnect);
        p_button.add(b_exit);

        inputPanel.add(p_button, BorderLayout.SOUTH); // 버튼 패널은 입력 필드 아래에 배치

        // Input 패널 전체를 Right Panel의 하단에 추가
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void sendMessage() {
        String message = t_input.getText();
        if (message.isEmpty()) return;

        send(new GamePacket(uid, GamePacket.MODE_TX_STRING, message, myRoomNumber));

        t_input.setText(""); // 보낸 후 입력창은 비우기
    }

    private void sendUserID() {
        send(new GamePacket(uid, GamePacket.MODE_LOGIN, myRoomNumber));
    }

    public void sendUnoUpdate(String uid, UnoGame unoGame){
        send(new GamePacket(uid, GamePacket.MODE_UNO_UPDATE, unoGame, myRoomNumber));
    }

    public void sendAddRoom(String uid){
        send(new GamePacket(uid, GamePacket.MODE_ROOM_ADD, myRoomNumber));
    }

    public void sendJoinRoom(int joinRoomNum){
        send(new GamePacket(uid, GamePacket.MODE_ROOM_JOIN, joinRoomNum));
    }

    public void sendReady(int readyRoomNum){
        send(new GamePacket(uid, GamePacket.MODE_ROOM_READY, readyRoomNum));
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

    private void connectToServer() throws IOException {
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
        b_exit.setEnabled(true);
    }
    private void receiveMessage(ObjectInputStream in) {
        try {
            GamePacket inMsg = (GamePacket) in.readObject();
            if (inMsg == null) {
                disconnect();
                printDisplay("서버 연결 끊김");
                return;
            }

            // 메시지 모드에 따라 분기
            switch (inMsg.getMode()) {  // getter 사용
                case GamePacket.MODE_TX_STRING:
                    printDisplay(inMsg.getUserID() + ": " + inMsg.getMessage());
                    break;

                case GamePacket.MODE_TX_IMAGE:
                    printDisplay(inMsg.getUserID() + ": " + inMsg.getMessage());
                    printDisplay(inMsg.getImage());  // 이미지 출력
                    break;

                case GamePacket.MODE_ROOM_JOIN:
                    if (inMsg.getRoomNum() == myRoomNumber){
                        printDisplay(inMsg.getUserID() + " 참가");
                        if (inMsg.getRoomState() != null && inMsg.getReadyList() != null) {
                            RoomNumUid = inMsg.getRoomState();
                            ReadyList = inMsg.getReadyList();

                            if (readyRoomPanel != null) {
                                remove(readyRoomPanel);  // 기존 패널이 있다면 제거
                            }

                            readyRoomPanel = new ClientReadyRoomGUI(this, myRoomNumber,RoomNumUid.get(myRoomNumber),ReadyList);
                            add(readyRoomPanel);
                        }
                    }
                    break;

                case GamePacket.MODE_ROOM_COUNT:
                    if (0 == myRoomNumber){
                        // 방이 업데이트 되었을 때 처리
                        printDisplay("방이 업데이트 되었습니다.");
                        roomCount = inMsg.getRoomCount();  // getter 사용
                        System.out.printf("현재 방 수: %d\n", roomCount);
                        updateRoom();  // 방 리스트나 UI 갱신 함수 호출
                    }
                    break;
                case GamePacket.MODE_ROOM_READY:
                    if (inMsg.getRoomNum() == myRoomNumber){
                        printDisplay(inMsg.getReadyList().size() + "");

                        // RoomNumUid와 ReadyList가 null이 아닌지 확인
                        if (inMsg.getRoomState() != null && inMsg.getReadyList() != null) {
                            RoomNumUid = inMsg.getRoomState();
                            ReadyList = inMsg.getReadyList();

                            // myRoomNumber가 RoomNumUid에 존재하는지 확인
                            if (RoomNumUid.size() > myRoomNumber && RoomNumUid.get(myRoomNumber) != null && currentUNOGUI == null) {
                                // 이전 GUI 컴포넌트가 있다면 제거
                                if (readyRoomPanel != null) {
                                    remove(readyRoomPanel);  // 기존 패널이 있다면 제거
                                }

                                // 새로운 ClientReadyRoomGUI 생성 및 추가
                                readyRoomPanel = new ClientReadyRoomGUI(this, myRoomNumber, RoomNumUid.get(myRoomNumber), ReadyList);
                                add(readyRoomPanel);

                                revalidate();
                                repaint();
                            } else {
                                // RoomNumUid에 문제가 있을 경우 처리
                                printDisplay("Error: Invalid Room Number or RoomNumUid is not initialized correctly.");
                            }
                        } else {
                            // RoomState 또는 ReadyList가 null인 경우
                            printDisplay("Error: Room State or Ready List is not available.");
                        }
                    }
                    break;
                case GamePacket.MODE_UNO_START:
                    // 게임 시작 요청 처리
                    if (inMsg.getRoomNum() == myRoomNumber) {  // getter 사용
                        printDisplay("게임이 시작됩니다.");

                        // 이전 GUI 컴포넌트가 있다면 제거
                        if (readyRoomPanel != null) {
                            remove(readyRoomPanel);
                        }

                        if (currentUNOGUI != null) {
                            remove(currentUNOGUI);
                        }


                        // 새로운 UnoGame GUI 추가
                        currentUNOGUI = new UnoGameClientGUI(inMsg.getUno(), uid, this);  // getter 사용
                        add(currentUNOGUI, BorderLayout.CENTER);
                        revalidate();
                        repaint();
                    }
                    break;

                case GamePacket.MODE_UNO_UPDATE:
                    if (inMsg.getRoomNum() == myRoomNumber){
                        // UNO 게임 상태 업데이트 처리
                        printDisplay(uid + "의 턴이 종료되었습니다.");
                        // 이전 GUI 컴포넌트 제거
                        remove(currentUNOGUI);
                        currentUNOGUI = new UnoGameClientGUI(inMsg.getUno(), uid, this);  // getter 사용
                        add(currentUNOGUI, BorderLayout.CENTER);
                        revalidate();
                        repaint();
                    }
                    break;

                default:
                    printDisplay("알 수 없는 메시지 모드: " + inMsg.getMode());
                    break;
            }
        } catch (IOException e) {
            printDisplay("연결이 종료되었습니다: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            printDisplay("잘못된 객체 형식이 전달되었습니다: " + e.getMessage());
        }
    }


    private void disconnect() {
        send(new GamePacket(uid, GamePacket.MODE_LOGOUT, 0));
        try {
            receiveThread = null;
            socket.close();
        } catch (IOException e) {
            System.err.println("클라이언트 닫기 오류 > " + e.getMessage());
            System.exit(-1);
        }
    }
}