import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClientReadyRoomGUI extends JPanel {

    private int roomNumber;
    private ClientGUI uc;
    private int readyProgress;
    private boolean isReady = false; // 초기 상태는 레디가 아님

    public ClientReadyRoomGUI(ClientGUI uc, int roomNumber, int readyProgress) {
        this.uc = uc;
        this.roomNumber = roomNumber;
        this.readyProgress = readyProgress;
        setLayout(new BorderLayout());
        createReadyRoomPanel();
    }

    private void createReadyRoomPanel() {
        // 네모와 버튼을 가로로 배치하는 패널
        JPanel boxesPanel = new JPanel(new GridLayout(2, 4, 10, 10)); // 1x4 그리드
        for (int i = 0; i < 4; i++) {
            // 개별 네모와 버튼을 포함하는 패널 생성
            JPanel boxPanel = new JPanel(new BorderLayout());
            JLabel uidPanel = new JLabel("Player" + i); // 플레이어가 없는 경우 빈 JLabel

            uidPanel.setPreferredSize(new Dimension(100, 30));
            boxPanel.add(uidPanel, BorderLayout.NORTH);

            // 네모 생성
            JPanel box = new JPanel();
            box.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            box.setPreferredSize(new Dimension(100, 100));

            // ReadyPlayers에 해당 플레이어가 있는지 확인
            if(readyProgress > i){
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
        readyButton.setBackground(Color.GREEN); // READY 상태일 때 배경을 초록색으로 변경


        readyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (!isReady) {
                    readyButton.setText("CANCEL"); // 레디 상태일 때 버튼 텍스트를 CANCEL로 변경
                    readyButton.setBackground(Color.RED); // CANCEL 상태일 때 배경을 빨간색으로 변경
                    uc.sendReady(roomNumber); // 레디 상태를 서버에 전송
                    isReady = true;
                } else {
                    readyButton.setText("READY"); // 레디 상태가 아닐 때 버튼 텍스트를 READY로 변경
                    readyButton.setBackground(Color.GREEN); // READY 상태일 때 배경을 초록색으로 변경
                    uc.sendReady(roomNumber); // CANCEL 상태를 서버에 전송
                    isReady = false;
                }
                revalidate();
                repaint();
            }
        });

        add(boxesPanel, BorderLayout.CENTER);
        add(readyButton, BorderLayout.SOUTH);
    }

    // 외부에서 readyProgress를 변경하는 메서드 추가
    public void setReadyProgress(int newReadyProgress) {
        this.readyProgress = newReadyProgress;
        updatePanel();
    }

    private void updatePanel() {
        // 네모와 버튼을 다시 그리도록 트리거
        for (int i = 0; i < 4; i++) {
            JPanel boxPanel = (JPanel) ((JPanel) getComponent(0)).getComponent(i); // boxesPanel에서 각 boxPanel을 가져옴
            JPanel box = (JPanel) boxPanel.getComponent(1); // boxPanel에서 네모 패널 가져옴

            if (readyProgress > i) {
                box.setBackground(Color.GREEN); // 레디 플레이어는 초록색 배경
            } else {
                box.setBackground(Color.WHITE); // 레디하지 않은 플레이어는 흰색 배경
            }
        }
        revalidate();
        repaint();
    }
}