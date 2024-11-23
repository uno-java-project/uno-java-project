package pro;

import javax.swing.*;
import java.io.Serializable;

public class ChatMsg implements Serializable {
    private static final long serialVersionUID = 1L; // 고정된 UID 설정
    public final static int MODE_LOGIN = 0x1;
    public final static int MODE_LOGOUT = 0x2;
    public final static int MODE_TX_STRING = 0x10;
    public final static int MODE_TX_FILE = 0x20;
    public final static int MODE_TX_IMAGE = 0x40;
    public final static int MODE_ROOM_STATUS = 0x50;
    public final static int MODE_ROOM_JOIN = 0x51; // 방 포트 정보 전달
    String userID;
    int mode;
    String message;
    ImageIcon image;
    long size;

    public ChatMsg(String userID, int mode, String message, ImageIcon image, long size) {
        this.userID = userID;
        this.mode = mode;
        this.message = message;
        this.image = image;
        this.size = size;
    }

    public ChatMsg(String userID, int code, String message, ImageIcon image) {
        this(userID, code, message, image, 0);
    }
    public ChatMsg(String userID, int code) {
        this(userID, code, null, null);
    }

    public ChatMsg(String userID, int code, String message) {
        this(userID, code, message, null);
    }
    public ChatMsg(String userID, int code, ImageIcon image) {
        this(userID, code, null, image);
    }
    public ChatMsg(String userID, int code, String filename, long size) {
        this(userID, code, filename, null, size);
    }
}
