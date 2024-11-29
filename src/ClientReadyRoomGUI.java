import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Objects;

public class ClientReadyRoomGUI extends JPanel {

    private ArrayList<String> Players;
    private ArrayList<String> ReadyPlayers;
    private int roomNumber;
    private ClientGUI uc;

    public ClientReadyRoomGUI(ClientGUI uc, int roomNumber, ArrayList<String> players, ArrayList<String> readyPlayers) {
        this.uc = uc;
        this.roomNumber = roomNumber;
        this.ReadyPlayers = readyPlayers;
        this.Players = players;
        setLayout(new BorderLayout());
        createReadyRoomPanel();
    }

    private void createReadyRoomPanel() {
        // 네모와 버튼을 가로로 배치하는 패널
        JPanel boxesPanel = new JPanel(new GridLayout(2, 4, 10, 10)); // 1x4 그리드
        for (int i = 0; i < 4; i++) {
            // 개별 네모와 버튼을 포함하는 패널 생성
            JPanel boxPanel = new JPanel(new BorderLayout());
            JLabel uidPanel;

            // Players 리스트의 크기가 i보다 크면 해당 플레이어 이름을 표시
            if (Players.size() > i) {
                uidPanel = new JLabel(Players.get(i)); // 플레이어 이름 설정
            } else {
                uidPanel = new JLabel(); // 플레이어가 없는 경우 빈 JLabel
            }

            uidPanel.setPreferredSize(new Dimension(100, 30));
            boxPanel.add(uidPanel, BorderLayout.NORTH);

            // 네모 생성
            JPanel box = new JPanel();
            box.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            box.setPreferredSize(new Dimension(100, 100));

            // ReadyPlayers에 해당 플레이어가 있는지 확인
            if (Players.size() > i && ReadyPlayers.contains(Players.get(i))) {
                box.setBackground(Color.GREEN); // 레디 플레이어는 초록색 배경
            } else {
                box.setBackground(Color.WHITE); // 레디하지 않은 플레이어는 흰색 배경
            }

            boxPanel.add(box, BorderLayout.CENTER);
            // 메인 패널에 추가
            boxesPanel.add(boxPanel);
        }

        // 버튼 생성
        JButton readyButton = new JButton("READY");
        readyButton.setPreferredSize(new Dimension(100, 40));

        if (ReadyPlayers.contains(uc.uid)) { // 이미 레디한 상태일 경우
            readyButton.setEnabled(false); // 버튼 비활성화
        } else {
            readyButton.setEnabled(true); // 레디하지 않은 상태일 경우 활성화
        }

        readyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                uc.sendReady(roomNumber); // 'READY' 버튼을 클릭하면 레디 상태를 서버에 전송
                readyButton.setEnabled(false); // 버튼 비활성화
                revalidate();
                repaint();
            }
        });

        readyButton.setEnabled(true);

        add(boxesPanel, BorderLayout.CENTER);
        add(readyButton, BorderLayout.SOUTH);
    }

}
