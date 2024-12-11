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
    private ClientReadyRoomGUI waitingPanel;

    private JButton b_exit, b_select, b_disconnect;;
    private JTextPane t_display;
    private JTextField t_input;
    private DefaultStyledDocument document;
    Socket socket;
    private Thread receiveThread = null;
    private String uid;
    private JPanel currentUNOGUI;
    public int myRoomNumber = 0;
    private int roomCount = 0;
    private ArrayList<Integer> deletedRooms;


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
    //이모티콘용 sendimg
    private void sendImage(ImageIcon image) {
        if (image == null) {
            printDisplay(">> 이미지를 전송할 수 없습니다: 이미지가 null입니다.");
            return;
        }
        Image scaledImage = image.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
        ImageIcon resizedIcon = new ImageIcon(scaledImage);
        //이모지 사이즈 조절
        send(new GamePacket(uid, GamePacket.MODE_TX_IMAGE, "이모지", resizedIcon, myRoomNumber));
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

            // 방 번호는 1부터 시작하도록
            int roomNumber = i + 1;  // 1부터 시작하는 방 번호

//            // deletedRooms에 해당 방 번호가 포함되어 있으면 그리지 않음
//            if (deletedRooms.contains(roomNumber)) {
//                continue;  // 해당 방은 그리지 않고 다음 방으로 넘어감
//            }

            JPanel singleRoomPanel = new JPanel(new BorderLayout());
            singleRoomPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

            // 각 방의 크기를 고정 (예: 550x50 크기로 설정)
            singleRoomPanel.setPreferredSize(new Dimension(550, 50));  // 방 크기 고정
            singleRoomPanel.setMaximumSize(new Dimension(550, 50));  // 방 크기 고정

            // 방 이름을 레이블로 설정
            JLabel roomLabel = new JLabel("방 " + roomNumber , SwingConstants.CENTER);

            // 참가 버튼 생성
            JButton joinButton = new JButton("참가");

            // 참가 버튼 클릭 시 해당 방 번호로 이동 (myRoomNumber에 값 할당)
            joinButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    t_display.setText(""); // 보낸 후 입력창은 비우기

                    remove(leftWrapperPanel);
                    myRoomNumber = roomNumber;
                    sendJoinRoom(uid, myRoomNumber);
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
        // 이모티콘 선택 버튼 추가
        JButton b_emoji = new JButton("이모티콘");
        b_emoji.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 이모티콘 선택을 위한 팝업 메뉴 생성
                JPopupMenu emojiMenu = new JPopupMenu();

                // assets 폴더에 있는 이모티콘 파일 로드
                String[] emojiFiles = {"happy.png", "sad.png","cry.png","heeng.png"}; // 사용할 이미지 파일명
                for (String emojiFile : emojiFiles) {
                    // 이모티콘 이미지를 로드
                    ImageIcon emojiIcon = new ImageIcon("assets/" + emojiFile);
                    Image scaledImage = emojiIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH); // 크기 조정
                    ImageIcon scaledIcon = new ImageIcon(scaledImage);

                    // 각 이모티콘에 대한 메뉴 아이템 생성
                    JMenuItem emojiItem = new JMenuItem(emojiFile, scaledIcon);
                    emojiItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            // 선택된 이모티콘을 전송
                            t_input.setText("이모티콘: " + emojiFile); // 선택한 이모티콘 표시
                            sendImage(new ImageIcon("assets/" + emojiFile)); // 이미지 전송
                        }
                    });

                    emojiMenu.add(emojiItem); // 메뉴에 추가
                }

                // 버튼 아래에 팝업 표시
                emojiMenu.show(b_emoji, b_emoji.getWidth() / 2, b_emoji.getHeight() / 2);
            }
        });

// 이모티콘 버튼을 입력 필드 오른쪽에 배치
        inputPanel.add(b_emoji, BorderLayout.EAST);

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
    // 로그인 시 사용자 ID와 현재 방 번호를 전송
    private void sendUserID() {
        send(new GamePacket(uid, GamePacket.MODE_LOGIN, null, null, null, 0, 0, 0, myRoomNumber, 0, null));
    }

    // UNO 업데이트 정보를 방 번호와 함께 전
    public void sendUnoUpdate(String uid, UnoGame unoGame) {
        send(new GamePacket(uid, GamePacket.MODE_UNO_UPDATE, null, null, unoGame, 0, 0, 0, myRoomNumber, 0, null));
    }

    // 방 추가 요청 시 사용자 ID와 현재 방 번호를 전송
    public void sendAddRoom(String uid) {
        send(new GamePacket(uid, GamePacket.MODE_ROOM_ADD, null, null, null, 0, 0, 0, myRoomNumber, 0, null));
    }

    // 방 입장 요청 시 사용자 ID와 입장할 방 번호를 전송
    public void sendJoinRoom(String uid, int joinRoomNum) {
        send(new GamePacket(uid, GamePacket.MODE_ROOM_JOIN, null, null, null, 0, 0, joinRoomNum, joinRoomNum, 0, null));
    }

    // 준비 상태를 서버로 전송 (참여 인원 정보 포함 가능)
    public void sendReady(int roomNumber) {
        send(new GamePacket(uid, GamePacket.MODE_ROOM_READY, null, null, null, 0, roomNumber, 0, roomNumber, 0, null));
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
            int readyPro;
            int joinPro;
            GamePacket inMsg = (GamePacket) in.readObject();
            if (inMsg == null) {
                disconnect();
                printDisplay("서버 연결 끊김");
                return;
            }

            // 메시지 모드에 따라 분기
            switch (inMsg.getMode()) {  // getter 사용
                case GamePacket.MODE_BROAD_STRING:
                    if(inMsg.getRoomNum() == myRoomNumber) {
                        printDisplay(inMsg.getMessage());
                    }
                    break;
                case GamePacket.MODE_TX_STRING:
                    if(inMsg.getRoomNum() == myRoomNumber) {
                        printDisplay(inMsg.getUserID() + ": " + inMsg.getMessage());
                    }
                    break;

                case GamePacket.MODE_TX_IMAGE:
                    if(inMsg.getRoomNum() == myRoomNumber) {
                        printDisplay(inMsg.getUserID() + ": " + inMsg.getMessage());
                        printDisplay(inMsg.getImage());  // 이미지 출력
                    }
                    break;

                case GamePacket.MODE_ROOM_COUNT:
                    if(0 == myRoomNumber) {
                        deletedRooms = inMsg.getDeletedRooms();
                        roomCount = inMsg.getRoomCount();  // getter 사용
                        updateRoom();  // 방 리스트나 UI 갱신 함수 호출
                    }
                    break;

                case GamePacket.MODE_ROOM_JOIN:
                    if(inMsg.getRoomNum() == myRoomNumber) {
                        printDisplay("방 참가에 성공하였습니다.");
                        readyPro = inMsg.getRoomReady();
                        joinPro = inMsg.getRoomJoin();

                        if(waitingPanel == null) {
                            waitingPanel = new ClientReadyRoomGUI(this, myRoomNumber, readyPro, joinPro);
                            add(waitingPanel, BorderLayout.CENTER);
                            revalidate();
                            repaint();
                        }else {
                            waitingPanel.setReadyProgress(readyPro, joinPro);
                        }

                        printDisplay("방 " + inMsg.getRoomNum() + ": 현재 참가자 수 " + joinPro);

                        // 현재 방 번호와 같으면 ClientReadyRoomGUI에 반영
                        if (waitingPanel != null && inMsg.getRoomNum() == myRoomNumber) {
                            waitingPanel.handleRoomInfo(inMsg);
                        }
                    }
                    break;

                case GamePacket.MODE_ROOM_READY:
                    if(inMsg.getRoomNum() == myRoomNumber){
                        readyPro = inMsg.getRoomReady();
                        joinPro = inMsg.getRoomJoin();

                        if (waitingPanel != null) {
                            waitingPanel.setReadyProgress(readyPro, joinPro);
                            revalidate();
                            repaint();
                        }
                    }
                    break;


                case GamePacket.MODE_UNO_START:
                    // 게임 시작 요청 처리
                    if (inMsg.getRoomNum() == myRoomNumber) {  // getter 사용
                        printDisplay("게임이 시작됩니다.");

                        // 이전 GUI 컴포넌트가 있다면 제거
                        if (waitingPanel != null) {
                            remove(waitingPanel);
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
//                        printDisplay("턴이 종료되었습니다.");

                        if (waitingPanel != null) {
                            remove(waitingPanel);
                        }

                        // 이전 GUI 컴포넌트 제거
                        remove(currentUNOGUI);
                        currentUNOGUI = new UnoGameClientGUI(inMsg.getUno(), uid, this);  // getter 사용
                        add(currentUNOGUI, BorderLayout.CENTER);
                        revalidate();
                        repaint();
                    }
                    break;

                case GamePacket.MODE_ROOM_INFO:
                    int roomNumber = inMsg.getRoomNum();
                    int participantsCount = inMsg.getParticipantsCount();

                    if(myRoomNumber == 0){
                        updateRoomParticipants(roomNumber, participantsCount);
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

    private void updateRoomParticipants(int roomNumber, int participantsCount) {
        for (Component comp : roomPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel singleRoomPanel = (JPanel) comp;
                JLabel roomLabel = (JLabel) singleRoomPanel.getComponent(0);

                // 방 번호를 확인하여 레이블 텍스트 업데이트
                if (roomLabel.getText().contains("방 " + roomNumber)) {
                    roomLabel.setText("방 " + roomNumber + " (" + participantsCount + "/4)");
                    break;
                }
            }
        }

        roomPanel.revalidate();
        roomPanel.repaint();
    }

    private void disconnect() {
        send(new GamePacket(uid, GamePacket.MODE_LOGOUT, null, null, null, 0, 0, 0, 0, 0, null)); // LOGOUT 패킷 전송
        try {
            receiveThread = null; // 수신 스레드 종료
            socket.close(); // 소켓 닫기
        } catch (IOException e) {
            System.err.println("클라이언트 닫기 오류 > " + e.getMessage());
            System.exit(-1); // 오류 발생 시 비정상 종료
        }
    }

}