import javax.swing.*;
import java.io.Serializable;

public class ChatMsg implements Serializable {
    public final static int MODE_LOGIN = 0x1;
    public final static int MODE_LOGOUT = 0x2;
    public final static int MODE_TX_STRING = 0x10;
    public final static int MODE_TX_IMAGE = 0x30;
    public final static int MODE_UNO_START = 0x50;
    public final static int MODE_UNO_UPDATE = 0x51;

    public final static int MODE_ROOM_ADD = 0x40; // 방 추가 전달
    public final static int MODE_ROOM_COUNT = 0x41; // 방 포트 정보 전달
    public final static int MODE_ROOM_JOIN = 0x42; // 방 포트 정보 전달


    String userID;
    int mode;
    String message;
    ImageIcon image;
    UnoGame uno;
    int roomCount;
    int roomNum;

    public ChatMsg(String userID, int code, String message, ImageIcon image, UnoGame uno, int roomCount, int roomNum) {
        this.userID = userID;
        this.mode = code;
        this.message = message;
        this.image = image;
        this.uno = uno;
        this.roomCount = roomCount;
    }

    public ChatMsg(String userID, int code, String message, ImageIcon image, int roomNum) {
        this(userID, code, message, image, null, 0, roomNum);
    }
    public ChatMsg(String userID, int code, int roomNum) {
        this(userID, code, null, null, roomNum);
    }

    public ChatMsg(String userID, int code, int roomCount, int roomNum) {
        this(userID, code, null, null, null, roomCount, roomNum);
    }

    public ChatMsg(String userID, int code, String message, int roomNum) {
        this(userID, code, message, null, roomNum);
    }
    public ChatMsg(String userID, int code, UnoGame uno,  int roomNum) {
        this(userID, code, null, null, uno, 0, roomNum);
    }


}
