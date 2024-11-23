import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;

public class UnoGameClient extends JFrame {
    private String serverAddress;
    private int serverPort;
    private ObjectOutputStream out;

    private JButton b_exit, b_select, b_disconnect;;
    private JTextPane t_display;
    private JTextField t_input;
    private DefaultStyledDocument document;
    Socket socket;
    private Thread receiveThread = null;
    private String uid;

    public UnoGameClient(String uid, String serverAddress, int serverPort) {
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
        this.add(createRightPanel(), BorderLayout.EAST);
        this.add(createLeftPanel(), BorderLayout.CENTER);
    }

    private void printDisplay(String msg) {
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
        send(new ChatMsg(uid, ChatMsg.MODE_TX_IMAGE, file.getName(), icon));
        t_input.setText("");
    }

    private void send(ChatMsg msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.out.println("클라 오류" + e.getMessage());
        }
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());


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

                int ret = chooser.showOpenDialog(UnoGameClient.this);
                if (ret != JFileChooser.APPROVE_OPTION) {
                    JOptionPane.showMessageDialog(UnoGameClient.this, "파일을 선택하지 않았습니다");
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

        send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING, message));

        t_input.setText(""); // 보낸 후 입력창은 비우기
    }

    private void sendUserID() {
        send(new ChatMsg(uid, ChatMsg.MODE_LOGIN));
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

    private void connectToServer() throws UnknownHostException, IOException {
        socket = new Socket();
        SocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
        socket.connect(sa, 3000);
        out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        receiveThread = new Thread(new Runnable() {
            private ObjectInputStream in;

            private void receiveMessage() {
                try {
                    ChatMsg inMsg = (ChatMsg) in.readObject();
                    if (inMsg == null) {
                        disconnect();
                        printDisplay("서버 연결 끊김");
                        return;
                    }
                    switch (inMsg.mode) {
                        case ChatMsg.MODE_TX_STRING:
                            printDisplay(inMsg.userID + ":" + inMsg.message);
                            break;
                        case ChatMsg.MODE_TX_IMAGE:
                            printDisplay(inMsg.userID + ":" + inMsg.message);
                            printDisplay(inMsg.image);
                            break;
                    }
                } catch (IOException e) {
                    printDisplay("연결 종류");
                } catch (ClassNotFoundException e) {
                    printDisplay("잘못된 객체가 전달되었습니다");
                }
            }

            @Override
            public void run() {
                try {
                    in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

                } catch (IOException e) {
                    printDisplay("입력 스트림이 열리지 않음");
                }
                while (receiveThread == Thread.currentThread()) {
                    receiveMessage();
                }
            }
        });
        receiveThread.start();
        b_select.setEnabled(true);
        b_exit.setEnabled(true);
    }
    private void disconnect() {
        send(new ChatMsg(uid, ChatMsg.MODE_LOGOUT));
        try {
            receiveThread = null;
            socket.close();
        } catch (IOException e) {
            System.err.println("클라이언트 닫기 오류 > " + e.getMessage());
            System.exit(-1);
        }
    }
}