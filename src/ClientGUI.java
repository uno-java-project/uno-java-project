import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.*;

public class ClientGUI extends JFrame {
    private String serverAddress;
    private int serverPort;
    JTextField t_userID;
    JTextField t_hostAddr;
    JTextField t_portNum;

    private JButton b_start, b_exit;
    private String uid;

    public ClientGUI(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        buildGUI();
        this.setBounds(0, 0, 800, 800);
        this.setTitle("UNO Login");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    private void buildGUI() {
        this.add(createLeftPanel(), BorderLayout.CENTER);
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

    private JPanel createInfoPanel() {
        // GridLayout을 사용하여 세로로 정렬
        JPanel p = new JPanel(new GridLayout(3, 2, 5, 5)); // 3행 2열, 컴포넌트 간 여백 설정

        t_userID = new JTextField(7);
        t_hostAddr = new JTextField(12);
        t_portNum = new JTextField(5);
        t_userID.setText("guest" + getLocalAddr().split("\\.")[3]);
        t_hostAddr.setText(this.serverAddress);
        t_portNum.setText(String.valueOf(this.serverPort));

        p.add(new JLabel("아이디:"));
        p.add(t_userID);
        p.add(new JLabel("서버주소:"));
        p.add(t_hostAddr);
        p.add(new JLabel("포트번호:"));
        p.add(t_portNum);
        t_portNum.setEditable(false);

        return p;
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());

        // 상단 이미지 영역: 비율 증가 및 중앙 정렬
        JLabel imageLabel = new JLabel();
        ImageIcon imageIcon = new ImageIcon("assets/UNO.PNG");
        Image scaledImage = imageIcon.getImage().getScaledInstance(400, -1, Image.SCALE_SMOOTH); // 이미지 크기를 더 키움
        imageLabel.setIcon(new ImageIcon(scaledImage));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER); // 수평 중앙 정렬
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);   // 수직 중앙 정렬

        // 이미지 패널 추가 및 여백 포함
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBorder(BorderFactory.createEmptyBorder(200, 0, 100, 0)); // 상단과 하단에 여백 추가
        imagePanel.add(imageLabel, BorderLayout.CENTER);

        leftPanel.add(imagePanel, BorderLayout.NORTH); // 이미지 패널 추가

        // 하단 패널: InfoPanel과 ControlPanel을 좌우로 배치
        JPanel lowerPanel = new JPanel(new GridLayout(1, 2, 10, 10)); // 좌우로 나누는 레이아웃
        lowerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // 패딩 추가

        // InfoPanel과 ControlPanel 추가
        lowerPanel.add(createInfoPanel());  // 왼쪽: InfoPanel
        lowerPanel.add(createControlPanel()); // 오른쪽: ControlPanel

        // 하단 패널을 LeftPanel의 남쪽에 추가
        leftPanel.add(lowerPanel, BorderLayout.SOUTH);

        return leftPanel;
    }

    private JPanel createControlPanel() {
        b_start = new JButton("접속하기");

        b_start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ClientGUI.this.serverAddress = t_hostAddr.getText();
                ClientGUI.this.serverPort = Integer.parseInt(t_portNum.getText());
                ClientGUI.this.uid = t_userID.getText();

                System.out.println(uid + serverAddress + serverPort);

                //SwingUtilities.invokeLater(() -> new UnoGameClient(uid, serverAddress, serverPort));
                SwingUtilities.invokeLater(() -> new ClientRoomGUI(uid, serverAddress, serverPort));

                // 현재 LoginGUI 닫기
                ClientGUI.this.dispose();
            }
        });

        b_exit = new JButton("종료하기");
        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.exit(0);
            }
        });

        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(b_start);
        panel.add(b_exit);

        return panel;
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 54321;

        ClientGUI client = new ClientGUI(serverAddress, serverPort);
    }
}