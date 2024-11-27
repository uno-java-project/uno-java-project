import javax.swing.*;
import java.awt.*;

public class UnoGameServer extends JFrame {
    int port = 54321;
    private JPanel gameRoomPanel;  // 게임방 패널들 (각각의 게임방을 표시하는 패널이 추가될 곳)

    public UnoGameServer() {
        setTitle("Uno Game");
        setSize(870, 830);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // 채팅 패널을 오른쪽에 추가
        ServerGUI serverGUI = new ServerGUI(port);
        add(serverGUI, BorderLayout.EAST);  // 채팅 패널 추가
        
//        // 우노 게임 패널
//        UnoGameServerGUI unoGameServerGUI = new UnoGameServerGUI();
//        add(unoGameServerGUI, BorderLayout.CENTER); // centerPanel을 중앙에 추가
        

        setVisible(true);
    }

}