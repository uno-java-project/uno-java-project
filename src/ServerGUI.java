import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ServerGUI extends JFrame {
    private final int[] ports = {54321, 54322, 54323, 54324}; // 방 포트
    private JTextArea t_display;
    private JButton b_connect, b_disconnect, b_exit;
    private Thread acceptThread = null;
    private final Vector<ClientHandler> users = new Vector<>();
    private final Map<Integer, Integer> roomStatus = new HashMap<>(); // <방 번호, 현재 인원>

    // 방 초기화
    private void initializeRooms() {
        for (int i = 2; i <= ports.length; i++) {
            roomStatus.put(i, 0); // 초기 인원은 0명
        }
    }

    public ServerGUI() {
        super("WithCharServer");
        buildGUI();
        this.setBounds(100, 200, 400, 300);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    private void buildGUI() {
        add(createDisplayPanel(), BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.SOUTH);
    }

    private JPanel createDisplayPanel() {
        t_display = new JTextArea();
        t_display.setEditable(false);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(t_display), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createControlPanel() {
        b_connect = new JButton("서버 시작");
        b_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                acceptThread = new Thread(() -> {
                    initializeRooms(); // 방 상태 초기화
                    startMultiRoomServer(); // 멀티룸 서버 시작
                });
                acceptThread.start();
                b_connect.setEnabled(false);
                b_disconnect.setEnabled(true);
                b_exit.setEnabled(false);
            }
        });

        b_disconnect = new JButton("서버 종료");
        b_disconnect.setEnabled(false);
        b_disconnect.addActionListener(e -> disconnect());

        b_exit = new JButton("종료하기");
        b_exit.addActionListener(e -> System.exit(0));

        JPanel panel = new JPanel(new GridLayout(1, 3));
        panel.add(b_connect);
        panel.add(b_disconnect);
        panel.add(b_exit);
        return panel;
    }

    private void startMultiRoomServer() {
        for (int port : ports) { // ports 배열에 있는 모든 포트에 대해 서버 실행
            new Thread(() -> {
                try (ServerSocket roomSocket = new ServerSocket(port)) {
                    printDisplay("포트 " + port + "에서 서버가 시작되었습니다.");
                    while (true) {
                        Socket clientSocket = roomSocket.accept();
                        String cAddr = clientSocket.getInetAddress().getHostAddress();
                        printDisplay("클라이언트 연결 (포트 " + port + "): " + cAddr);
                        ClientHandler clientHandler = new ClientHandler(clientSocket, port);
                        synchronized (users) {
                            users.add(clientHandler);
                        }
                        clientHandler.start();
                    }
                } catch (IOException e) {
                    printDisplay("포트 " + port + "에서 서버 오류: " + e.getMessage());
                }
            }).start();
        }
    }

    private void disconnect() {
        try {
            acceptThread = null;
            synchronized (users) {
                for (ClientHandler client : users) {
                    client.closeConnection();
                }
                users.clear();
            }
            printDisplay("서버가 종료되었습니다.");
            b_connect.setEnabled(true);
            b_disconnect.setEnabled(false);
        } catch (Exception e) {
            printDisplay("서버 종료 오류: " + e.getMessage());
        }
    }

    private void printDisplay(String message) {
        t_display.append(message + "\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());
    }

    private class ClientHandler extends Thread {
        private final Socket clientSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private int currentRoom = -1; // 클라이언트가 현재 입장한 방 (-1은 미입장 상태)
        private final int assignedPort;

        public ClientHandler(Socket clientSocket, int assignedPort) {
            this.clientSocket = clientSocket;
            this.assignedPort = assignedPort;
        }

        @Override
        public void run() {
            try {
                in = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                out = new ObjectOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
                ChatMsg msg;
                while ((msg = (ChatMsg) in.readObject()) != null) {
                    switch (msg.mode) {
                        case ChatMsg.MODE_LOGIN -> handleLogin(msg);
                        case ChatMsg.MODE_LOGOUT -> handleLogout();
                        case ChatMsg.MODE_TX_STRING -> broadcastMessage(msg);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                printDisplay("클라이언트 처리 오류: " + e.getMessage());
            } finally {
                handleClientDisconnection();
            }
        }

        private void handleLogin(ChatMsg msg) {
            currentRoom = getRoomNumber(assignedPort);
            synchronized (roomStatus) {
                roomStatus.put(currentRoom, roomStatus.getOrDefault(currentRoom, 0) + 1);
            }
            printDisplay(msg.userID + "님이 방 " + currentRoom + "에 입장했습니다.");
            broadcastRoomStatus();
        }

        private void handleLogout() {
            handleClientDisconnection();
        }

        private void broadcastMessage(ChatMsg msg) {
            printDisplay("방 " + currentRoom + ": " + msg.message);
            synchronized (users) {
                for (ClientHandler client : users) {
                    if (client.currentRoom == this.currentRoom) {
                        client.sendMessage(msg);
                    }
                }
            }
        }

        private void handleClientDisconnection() {
            synchronized (roomStatus) {
                if (currentRoom != -1) {
                    int currentCount = roomStatus.getOrDefault(currentRoom, 0);
                    roomStatus.put(currentRoom, Math.max(0, currentCount - 1));
                    currentRoom = -1;
                }
            }
            synchronized (users) {
                users.remove(this);
            }
            broadcastRoomStatus();
            closeConnection();
        }

        private void broadcastRoomStatus() {
            StringBuilder statusMessage = new StringBuilder();
            synchronized (roomStatus) {
                for (Map.Entry<Integer, Integer> entry : roomStatus.entrySet()) {
                    statusMessage.append("방 ").append(entry.getKey()).append(" (")
                            .append(entry.getValue()).append("/4)\n");
                }
            }
            ChatMsg roomStatusMsg = new ChatMsg("SERVER", ChatMsg.MODE_ROOM_STATUS, statusMessage.toString());
            synchronized (users) {
                for (ClientHandler client : users) {
                    client.sendMessage(roomStatusMsg);
                }
            }
        }

        private void sendMessage(ChatMsg msg) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                printDisplay("메시지 전송 오류: " + e.getMessage());
            }
        }

        public void closeConnection() {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                printDisplay("클라이언트 소켓 닫기 오류: " + e.getMessage());
            }
        }

        private int getRoomNumber(int port) {
            for (int i = 0; i < ports.length; i++) {
                if (ports[i] == port) {
                    return i + 1;
                }
            }
            return -1; // 포트와 방 번호가 매칭되지 않는 경우
        }
    }

    public static void main(String[] args) {
        new ServerGUI();
    }
}
