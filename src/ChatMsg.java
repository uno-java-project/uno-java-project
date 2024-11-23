import javax.swing.*;
import java.io.Serializable;

public class ChatMsg implements Serializable {
    public final static int MODE_LOGIN = 0x1;
    public final static int MODE_LOGOUT = 0x2;
    public final static int MODE_TX_STRING = 0x10;
    public final static int MODE_TX_FILE = 0x20;
    public final static int MODE_TX_IMAGE = 0x40;
    public final static int MODE_UNO_DATA = 0x50;


    String userID;
    int mode;
    String message;
    ImageIcon image;
    long size;
    UnoGame uno;

    public ChatMsg(String userID, int code, String message, ImageIcon image, UnoGame uno) {
        this.userID = userID;
        this.mode = code;
        this.message = message;
        this.image = image;
        this.uno = uno;
    }

    public ChatMsg(String userID, int code, String message, ImageIcon image) {
        this(userID, code, message, image, null);
    }
    public ChatMsg(String userID, int code) {
        this(userID, code, null, null);
    }

    public ChatMsg(String userID, int code, String message) {
        this(userID, code, message, null);
    }
    public ChatMsg(String userID, int code, UnoGame uno) {
        this(userID, code, null, null, uno);
    }


}
