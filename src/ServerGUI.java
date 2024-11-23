import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.Vector;

public class ServerGUI extends JPanel {
    private int port;
    private ServerSocket serverSocket;
    private JTextArea t_display;
    private JButton b_connect, b_disconnect, b_exit;
    private Thread acceptThread = null;
    private Vector<ClientHandler> users = new Vector<ClientHandler>();
    private int maxPlayers = 4;  // 최대 플레이어 수 설정

    public ServerGUI(int port) {
//        super("WithCharServer");
        this.port = port;
        this.setLayout(new BorderLayout());
        buildGUI();
        this.setBounds(100, 200, 400, 300);

//        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        this.setVisible(true); //this는 전부 필수 아니지만 있는 게 나음
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

                // 접속할 때마다 현재 유저 수 확인
                if (users.size() >= maxPlayers + 1) {
                    // 최대 인원 초과 시, 클라이언트에게 메시지 전송하고 연결 종료
                    ChatMsg msg = new ChatMsg("Server", ChatMsg.MODE_TX_STRING, "최대 인원 수 초과. 접속을 종료합니다.");
                    out.writeObject(msg);
                    out.flush();
                    cs.close();
                    return; // 클라이언트 접속 종료
                }

                String message;
                ChatMsg msg;
                while ((msg = (ChatMsg) in.readObject()) != null) {
                    if (msg.mode == ChatMsg.MODE_LOGIN) {
                        uid = msg.userID;
                        printDisplay("새 참가자: " + uid);
                        printDisplay("현재 참가자 수: " + users.size());
                        continue;
                    }
                    else if (msg.mode == ChatMsg.MODE_LOGOUT) {
                        break;
                    }
                    else if (msg.mode == ChatMsg.MODE_TX_STRING) {
                        message = uid + ": " + msg.message;

                        printDisplay(message);
                        broadcasting(msg);
                        ;
                    }
                    else if (msg.mode == ChatMsg.MODE_TX_IMAGE) {
                        printDisplay(uid + ": " + msg.message);
                        broadcasting(msg);
                    }
                }

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

//    public static void main(String[] args) {
//        int port = 54321;
//        ServerGUI server = new ServerGUI(port);
//    }
}