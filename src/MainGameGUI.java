/*
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class MainGameGUI {
    private String serverAddress;
    private int serverPort;
    private ObjectOutputStream out;
    JTextField t_userID;
    JTextField t_hostAddr;
    JTextField t_portNum;

    private JButton b_start, b_exit, b_send, b_select;
    private JTextPane t_display;
    private JTextField t_input;

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel displayPanel = createDisplayPanel();
        panel.add(displayPanel, BorderLayout.CENTER); // 중앙에 배치해 가장 큰 영역 할당

        // Input 필드 및 버튼 패널 구성
        JPanel inputPanel = new JPanel(new BorderLayout());

        // 텍스트 입력 필드
        t_input = new JTextField(30);
        t_input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        inputPanel.add(t_input, BorderLayout.NORTH); // 입력 필드는 상단에 배치

        // 버튼 패널 (보내기, 선택하기 버튼)
        JPanel p_button = new JPanel(new GridLayout(1, 2, 5, 5)); // 가로로 두 개 버튼 배치
        b_send = new JButton("보내기");
        b_send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
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

                int ret = chooser.showOpenDialog(LoginGUI.this);
                if (ret != JFileChooser.APPROVE_OPTION) {
                    JOptionPane.showMessageDialog(LoginGUI.this, "파일을 선택하지 않았습니다");
                    return;
                }
                t_input.setText(chooser.getSelectedFile().getAbsolutePath());
                sendImage();
            }
        });

        // 버튼들을 버튼 패널에 추가
        p_button.add(b_select);
        p_button.add(b_send);

        inputPanel.add(p_button, BorderLayout.SOUTH); // 버튼 패널은 입력 필드 아래에 배치

        // Input 패널 전체를 Right Panel의 하단에 추가
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

}
*/
