package pro;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Vector;

public class ServerGUI extends JFrame {
    private HashMap<Integer, Vector<String>> roomUsers = new HashMap<>();
    private int port;
    private ServerSocket serverSocket;
    private JTextArea t_display;
    private JButton b_connect, b_disconnect, b_exit;
    private Thread acceptThread = null;
    private Vector<ClientHandler> users = new Vector<ClientHandler>();
    private int getPortForRoom(int roomNumber) {
        return 54321 + roomNumber; // 방 번호에 따라 포트를 설정
    }

    public ServerGUI(int port) {
        super("WithCharServer");
        this.port = port;
        buildGUI();
        this.setBounds(100, 200, 400, 300);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true); //this는 전부 필수 아니지만 있는 게 나음
    } // 생성자

    private void buildGUI() {
        add(createDisplayPanel(), BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.SOUTH);

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
    public void startRoomServer(int roomNumber) {
        new Thread(() -> {
            int roomPort = getPortForRoom(roomNumber);
            try (ServerSocket roomServerSocket = new ServerSocket(roomPort)) {
                printDisplay("방 " + roomNumber + " 서버 시작. 포트: " + roomPort);

                while (true) {
                    printDisplay("방 " + roomNumber + " 클라이언트 연결 대기 중...");
                    Socket roomClient = roomServerSocket.accept();
                    printDisplay("방 " + roomNumber + "에 클라이언트 연결됨");
                    // 클라이언트 처리
                    handleClientInRoom(roomClient, roomNumber);
                }
            } catch (IOException e) {
                printDisplay("방 서버 오류: " + e.getMessage());
            }
        }).start();
    }

    private void handleClientInRoom(Socket clientSocket, int roomNumber) {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

            ChatMsg msg = (ChatMsg) in.readObject();
            if (msg.mode == ChatMsg.MODE_ROOM_JOIN) {
                // 방 사용자 목록 업데이트
                roomUsers.computeIfAbsent(roomNumber, k -> new Vector<>()).add(msg.userID);

                // 현재 방 상태 메시지 생성
                int currentCount = roomUsers.get(roomNumber).size();
                String roomStatus = "방 " + roomNumber + " (" + currentCount + "/4)";

                printDisplay("방 " + roomNumber + " 상태: " + roomStatus);

                // 클라이언트에게 방 상태 전송
                out.writeObject(new ChatMsg("SERVER", ChatMsg.MODE_ROOM_STATUS, roomStatus));
                out.flush();
            }
        } catch (IOException | ClassNotFoundException e) {
            printDisplay("방 " + roomNumber + " 클라이언트 처리 오류: " + e.getMessage());
        }
    }



    private void broadcastToAllClients(ChatMsg msg) {
        for (ClientHandler c : users) {
            c.send(msg); // 각 클라이언트에게 전송
        }
    }


    private void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            printDisplay("메인 서버가 시작됐습니다. " + getLocalAddr());

            for (int roomNumber = 1; roomNumber <= 4; roomNumber++) {
                printDisplay("방 " + roomNumber + " 서버 준비 중...");
                startRoomServer(roomNumber);
            }

            while (acceptThread == Thread.currentThread()) {
                Socket clientSocket = serverSocket.accept();
                printDisplay("클라이언트 연결됨: " + clientSocket.getInetAddress().getHostAddress());
                ClientHandler cHandler = new ClientHandler(clientSocket);
                users.add(cHandler);
                cHandler.start();
            }
        } catch (IOException e) {
            printDisplay("서버 시작 오류: " + e.getMessage());
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
        ServerGUI server = new ServerGUI(port);
    }
}