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
//    private int port;
    private JPanel serverPanel;
    private JPanel participantsPanel;
    private JPanel imagePanel;
    private ServerSocket serverSocket;
    private JTextArea t_display;
    private JButton b_connect, b_disconnect, b_exit;
    private Thread acceptThread = null;
    private Vector<ClientHandler> users = new Vector<ClientHandler>();
    private List<String> playersUid = new ArrayList<>();
    private int maxPlayers = 4;  // 최대 플레이어 수 설정
    private UnoGameServerGUI unoGameServerGUI;

    private int roomNum;

    public ServerGUI(int roomNum) {
        super("Uno Game");
//        this.port = port;
        this.roomNum = roomNum;
        this.setSize(870, 830);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);

        serverPanel = new JPanel(new BorderLayout());
        buildGUI();

        updateParticipantsPanel();

        setLayout(new BorderLayout());
        // 채팅 패널을 오른쪽에 추가
        add(serverPanel, BorderLayout.EAST);  // 채팅 패널 추가

        // 상단 이미지 영역: 비율 증가 및 중앙 정렬
        JLabel imageLabel = new JLabel();
        ImageIcon imageIcon = new ImageIcon("assets/UNO.PNG");
        Image scaledImage = imageIcon.getImage().getScaledInstance(400, -1, Image.SCALE_SMOOTH); // 이미지 크기를 더 키움
        imageLabel.setIcon(new ImageIcon(scaledImage));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER); // 수평 중앙 정렬
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);   // 수직 중앙 정렬

        // 이미지 패널 추가 및 여백 포함
        imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBorder(BorderFactory.createEmptyBorder(80, 0, 100, 0)); // 상단과 하단에 20px 여백 추가
        imagePanel.add(imageLabel, BorderLayout.CENTER);

        add(imagePanel, BorderLayout.CENTER); // 이미지 패널 추가

//        acceptThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                startServer();
//            }
//        });
//        acceptThread.start();

        setVisible(false);
    }

    public void openOrCloseServerGUI(){
        this.setVisible(!this.isVisible());
    }

    private void buildGUI() {
        // 참가자 패널을 북쪽에 추가
        participantsPanel = new JPanel(new GridLayout(0, 1)); // 세로로 리스트가 쌓이도록 설정
        participantsPanel.setBorder(BorderFactory.createTitledBorder("참가자 리스트"));
        serverPanel.add(participantsPanel, BorderLayout.NORTH);

        serverPanel.add(createDisplayPanel(), BorderLayout.CENTER);
        serverPanel.add(createControlPanel(), BorderLayout.SOUTH);
    }

    // 참가자 목록에 추가할 메서드
    private void updateParticipantsPanel() {
        participantsPanel.removeAll(); // 기존 참가자 목록을 삭제

        // 최대 4명의 플레이어를 위한 패널을 설정
        for (int i = 0; i < maxPlayers; i++) {
            JPanel playerPanel = new JPanel(new BorderLayout());
            //playerPanel.setPreferredSize(new Dimension(350, 60)); // 패널 크기 조정 (너비 350, 높이 60)

            // 플레이어 이름을 위한 라벨 설정
            JLabel playerLabel = new JLabel();
            playerLabel.setFont(new Font("Arial", Font.BOLD, 18)); // 폰트 크기와 스타일 설정
            playerLabel.setVerticalAlignment(SwingConstants.CENTER); // 세로 중앙 정렬

            // UID가 있을 경우, 초록색 배경으로 변경하고 UID를 표시
            if (i < playersUid.size()) {
                playerLabel.setText("Player " + (i + 1) + ": " + playersUid.get(i));
                playerLabel.setForeground(Color.WHITE); // 텍스트 색상은 흰색
                playerPanel.setBackground(Color.GREEN); // 참가자가 있으면 초록색으로 변경
            } else {
                playerLabel.setText("Player " + (i + 1) + ": ");
                playerLabel.setForeground(Color.BLACK); // 참가자가 없다면 텍스트 색상은 검정
            }

            // 패널에 라벨 추가
            playerPanel.add(playerLabel, BorderLayout.CENTER);

            // 각 playerPanel에 상단 마진 추가 (예: 10px)
            playerPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));  // 상단에만 10px 마진


            // 참가자 패널에 패널 추가
            participantsPanel.add(playerPanel);
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

//    private void startServer() {
//        Socket clientSocket = null;
//        try {
//            serverSocket = new ServerSocket(port);
//            printDisplay("서버가 시작됐습니다." + getLocalAddr());
//            while (acceptThread == Thread.currentThread()) { // 클라이언트 접속 기다림
//                clientSocket = serverSocket.accept();
//                String cAddr = clientSocket.getInetAddress().getHostAddress();
//                t_display.append("클라이언트 연결:" + cAddr + "\n");
//                ClientHandler cHandler = new ClientHandler(clientSocket);
//                users.add(cHandler);
//                cHandler.start();
//            }
//        } catch (SocketException e) {
//            printDisplay("서버 소캣 종료");
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (clientSocket != null) clientSocket.close();
//                if (serverSocket != null) serverSocket.close();
//            } catch (IOException e) {
//                System.err.println("서버 닫기 오류 > " + e.getMessage());
//                System.exit(-1);
//            }
//        }
//    }

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
//                acceptThread = new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        startServer();
//                    }
//                });
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
        b_disconnect.setEnabled(false);

        return panel;
    }


    private void printDisplay(String message) {
        t_display.append(message + "\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());
    }

    /*private void sendMessage(String inputText) {
        if (inputText.isEmpty()) return; // 입력창 비었으면 아무것도 안 함

        else {
            try {
                ((BufferedWriter) out).write(inputText + '\n');
                out.flush();
            } catch (NumberFormatException e) { // 정수 아니면 오류
                System.err.println("정수가 아님! " + e.getMessage());
                return;
            } catch (IOException e) {
                System.err.println("클라이언트 쓰기 오류 > " + e.getMessage());
                System.exit(-1);
            }
            t_display.append("나: " + inputText + "\n");
            //t_input.setText(""); // 보낸 후 입력창은 비우기
        }
    }*/

    private void disconnect() {
        try {
            acceptThread = null;
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("서버 닫기 오류 > " + e.getMessage());
            System.exit(-1);
        }
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
                        playersUid.add(uid);
                        printDisplay("새 참가자: " + uid);
                        printDisplay("현재 참가자 수: " + users.size());
                        updateParticipantsPanel(); // 참가자 목록 갱신

                        // 현재 유저 수 확인후 실행
                        if (users.size() == maxPlayers && unoGame == null) {
                            remove(imagePanel);
                            unoGame = new UnoGame();
                            unoGame.setPlayers(playersUid);
                            // 우노 게임 패널
                            unoGameServerGUI = new UnoGameServerGUI(unoGame);
                            add(unoGameServerGUI, BorderLayout.CENTER); // centerPanel을 중앙에 추가
                            unoGameServerGUI.gameStartUp();

                            broadcastingUnoStart();

                            revalidate(); // 레이아웃을 갱신
                            repaint(); // 화면을 새로 그리기
                        }
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

                playersUid.remove(uid);
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

//        private void sendMessage(String msg) {
//            send(new ChatMsg(uid, ChatMsg.MODE_LOGIN, msg));
//        }

        private void sendUnoStart() {
            send(new ChatMsg(uid, ChatMsg.MODE_UNO_START, unoGame, roomNum));
        }
        private void sendUnoUpdate() {
            send(new ChatMsg(uid, ChatMsg.MODE_UNO_UPDATE, unoGame, roomNum));
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

//    public static void main(String[] args) {
//        int port = 54321;
//        ServerGUI server = new ServerGUI(port);
//    }
}