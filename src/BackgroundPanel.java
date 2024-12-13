import javax.swing.*;
import java.awt.*;

public class BackgroundPanel extends JPanel {
    private Image backgroundImage;

    public BackgroundPanel(String imagePath) {
        this.backgroundImage = new ImageIcon(imagePath).getImage();
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 배경 이미지 그리기
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
