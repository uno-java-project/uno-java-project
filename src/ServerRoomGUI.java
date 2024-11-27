import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public class ServerRoomGUI extends JFrame {
    private HashMap<Integer, Vector<String>> roomUsers = new HashMap<>();

    private JLabel[] roomLabels = new JLabel[8]; // 각 방의 라벨을 저장
    private ServerGUI[] servers = new ServerGUI[8];

    private int port;
    private ServerSocket serverSocket;
    private JTextArea t_display;
    private JButton b_connect, b_disconnect, b_exit;
    private Thread acceptThread = null;
    private Vector<ClientHandler> users = new Vector<ClientHandler>();
    private int getPortForRoom(int roomNumber) {
        return port + roomNumber; // 방 번호에 따라 포트를 설정
    }

    public ServerRoomGUI(int port) {
        super("WithCharServer");
        this.port = port;
        buildGUI();
        this.setBounds(100, 100, 800, 600);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true); //this는 전부 필수 아니지만 있는 게 나음
    } // 생성자

    private void buildGUI() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftPanel(), createRightPanel());
        splitPane.setDividerLocation(300);
        add(splitPane, BorderLayout.CENTER);
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
            JButton joinButton = new JButton("관전");
            joinButton.addActionListener(e -> joinRoom(roomNumber));

            singleRoomPanel.add(roomLabel, BorderLayout.CENTER);
            singleRoomPanel.add(joinButton, BorderLayout.EAST);
            roomPanel.add(singleRoomPanel);
        }

        leftPanel.add(roomPanel, BorderLayout.CENTER);
        return leftPanel;
    }

    private void joinRoom(int roomNumber) {
        servers[roomNumber-1].openOrCloseServerGUI();
    }

    private JPanel createRightPanel() {
        JPanel createLeftPanel = new JPanel(new BorderLayout());
        createLeftPanel.add(createDisplayPanel(), BorderLayout.CENTER);
        createLeftPanel.add(createControlPanel(), BorderLayout.SOUTH);

        return createLeftPanel;
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
        printDisplay("메인 서버가 시작됐습니다. " + getLocalAddr());

        for (int roomNumber = 1; roomNumber <= 8; roomNumber++) {
            servers[roomNumber-1] = new ServerGUI(getPortForRoom(roomNumber));
            printDisplay("방 " + roomNumber + " 서버 시작.");
            //printDisplay("방 " + roomNumber + " 서버 시작. 포트: " + getPortForRoom(roomNumber));
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
        b_disconnect.setEnabled(false); // 처음엔 비활성화
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


    private synchronized void printDisplay(String message) {
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

                // 사용자 목록 저장
                Vector<String> currentUsers = new Vector<>();

                while ((msg = (ChatMsg) in.readObject()) != null) {
                    if (msg.mode == ChatMsg.MODE_LOGIN) {
                        uid = msg.userID;
                        currentUsers.add(uid); // 현재 사용자를 추가
                        printDisplay("새 참가자: " + uid);
                        printDisplay("현재 참가자: " + currentUsers);
                        printDisplay("현재 참가자 수: " + currentUsers.size());
                        continue;
                    } else if (msg.mode == ChatMsg.MODE_LOGOUT) {
                        currentUsers.remove(uid); // 퇴장 시 사용자 제거
                        printDisplay(uid + " 퇴장.");
                        printDisplay("현재 참가자: " + currentUsers);
                        printDisplay("현재 참가자 수: " + currentUsers.size());
                        break;
                    } else if (msg.mode == ChatMsg.MODE_TX_STRING) {
                        message = uid + ": " + msg.message;
                        printDisplay(message);
                        broadcasting(msg);
                    } else if (msg.mode == ChatMsg.MODE_TX_IMAGE) {
                        printDisplay(uid + ": " + msg.message);
                        broadcasting(msg);
                    } else if (msg.mode == ChatMsg.MODE_TX_STRING) {
                        message = uid + ": " + msg.message;

                        printDisplay(message);
                        broadcasting(msg);
                        ;
                    }
                }

                users.removeElement(this);
                printDisplay(uid + " 연결 종료. 현재 총 연결된 사용자 수: " + users.size());
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

        private void broadcasting(ChatMsg msg) {
            for (ClientHandler c : users) {
                c.send(msg);
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
        new ServerRoomGUI(port);
    }
}