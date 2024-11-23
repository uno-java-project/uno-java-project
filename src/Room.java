import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Room {
    private static final int MAIN_PORT = 54321; // 메인 서버 포트
    private static final int[] roomPorts = {54322, 54323, 54324, 54325}; // 방 포트
    private final Map<Integer, Integer> roomStatus = new HashMap<>(); // <방 번호, 현재 인원>
    private final Vector<ClientHandler> users = new Vector<>(); // 방 클라이언트 관리

    public Room() {
        initializeRooms();
        startMainServer();
        startRoomServers();
    }

    private void initializeRooms() {
        for (int i = 1; i <= roomPorts.length; i++) {
            roomStatus.put(i, 0); // 방 번호 1부터 시작, 초기 인원은 0명
        }
    }

    private void startMainServer() {
        new Thread(() -> {
            try (ServerSocket mainSocket = new ServerSocket(MAIN_PORT)) {
                System.out.println("메인 서버가 포트 " + MAIN_PORT + "에서 시작되었습니다.");

                while (true) {
                    Socket clientSocket = mainSocket.accept();
                    System.out.println("클라이언트 연결됨: " + clientSocket.getInetAddress());
                    new Thread(() -> handleClientConnection(clientSocket)).start();
                }
            } catch (IOException e) {
                System.err.println("메인 서버 오류: " + e.getMessage());
            }
        }).start();
    }

    private void handleClientConnection(Socket clientSocket) {
        try (ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

            // 클라이언트 요청 처리
            ChatMsg msg = (ChatMsg) in.readObject();
            if (msg.mode == ChatMsg.MODE_LOGIN) {
                int roomNumber = Integer.parseInt(msg.message); // 요청된 방 번호
                int roomPort = getRoomPort(roomNumber);

                if (roomPort != -1) {
                    out.writeObject(new ChatMsg("SERVER", ChatMsg.MODE_ROOM_PORT, String.valueOf(roomPort)));
                    System.out.println("클라이언트를 방 " + roomNumber + " (포트 " + roomPort + ")로 리다이렉트.");
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("클라이언트 연결 처리 오류: " + e.getMessage());
        }
    }

    private int getRoomPort(int roomNumber) {
        if (roomNumber >= 1 && roomNumber <= roomPorts.length) {
            return roomPorts[roomNumber - 1];
        }
        return -1; // 잘못된 방 번호
    }

    private void startRoomServers() {
        for (int port : roomPorts) {
            new Thread(() -> {
                try (ServerSocket roomSocket = new ServerSocket(port)) {
                    System.out.println("방 서버가 포트 " + port + "에서 시작되었습니다.");
                    while (true) {
                        Socket clientSocket = roomSocket.accept();
                        System.out.println("방 서버 클라이언트 연결: " + clientSocket.getInetAddress() + " (포트 " + port + ")");
                        ClientHandler clientHandler = new ClientHandler(clientSocket, port);
                        synchronized (users) {
                            users.add(clientHandler);
                        }
                        clientHandler.start();
                    }
                } catch (IOException e) {
                    System.err.println("포트 " + port + "에서 방 서버 오류: " + e.getMessage());
                }
            }).start();
        }
    }

    private class ClientHandler extends Thread {
        private final Socket clientSocket;
        private final int assignedPort;
        private int currentRoom = -1;

        public ClientHandler(Socket clientSocket, int assignedPort) {
            this.clientSocket = clientSocket;
            this.assignedPort = assignedPort;
        }

        @Override
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

                ChatMsg msg;
                while ((msg = (ChatMsg) in.readObject()) != null) {
                    if (msg.mode == ChatMsg.MODE_TX_STRING) {
                        broadcastMessage(msg);
                    } else if (msg.mode == ChatMsg.MODE_LOGOUT) {
                        handleLogout();
                        break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("클라이언트 처리 오류: " + e.getMessage());
            } finally {
                handleLogout();
            }
        }

        private void broadcastMessage(ChatMsg msg) {
            System.out.println("방 " + currentRoom + ": " + msg.message);
            synchronized (users) {
                for (ClientHandler client : users) {
                    if (client.assignedPort == this.assignedPort) {
                        try {
                            client.sendMessage(msg);
                        } catch (IOException e) {
                            System.err.println("메시지 전송 오류: " + e.getMessage());
                        }
                    }
                }
            }
        }

        private void handleLogout() {
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
        }

        private void sendMessage(ChatMsg msg) throws IOException {
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.writeObject(msg);
            out.flush();
        }
    }

    public static void main(String[] args) {
        new Room();
    }
}
