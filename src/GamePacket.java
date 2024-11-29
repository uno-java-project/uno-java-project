import javax.swing.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GamePacket implements Serializable {
    // 메시지 모드를 정의하는 상수들
    public static final int MODE_LOGIN = 0x1;
    public static final int MODE_LOGOUT = 0x2;
    public static final int MODE_TX_STRING = 0x10;
    public static final int MODE_TX_IMAGE = 0x30;

    public static final int MODE_ROOM_ADD = 0x40;  // 방 추가 정보
    public static final int MODE_ROOM_COUNT = 0x41;  // 방 수와 포트 정보
    public static final int MODE_ROOM_JOIN = 0x42;  // 방 입장 요청
    public static final int MODE_ROOM_READY = 0x43;  // 게임 레디

    public static final int MODE_UNO_START = 0x50;
    public static final int MODE_UNO_UPDATE = 0x51;

    // 필드들
    private String userID;
    private int mode;
    private String message;
    private ImageIcon image;
    private UnoGame uno;
    private Integer roomCount;
    private ArrayList<String> readyList;
    private HashMap<Integer, ArrayList<String>> roomState;
    private int roomNum;

    // 기본 생성자 (모든 필드를 처리할 수 있음)
    public GamePacket(String userID, int mode, String message, ImageIcon image, UnoGame uno, int roomCount, ArrayList<String> readyList,HashMap<Integer, ArrayList<String>> roomState, int roomNum) {
        this.userID = userID;
        this.mode = mode;
        this.message = message;
        this.image = image;
        this.uno = uno;
        this.roomCount = roomCount;
        this.readyList = readyList;
        this.roomState = roomState;
        this.roomNum = roomNum;
    }

    // 메시지와 이미지만 전달하는 생성자
    public GamePacket(String userID, int mode, String message, ImageIcon image, int roomNum) {
        this(userID, mode, message, image, null, 0, null, null,roomNum);
    }

    // 방 번호만 전달하는 생성자
    public GamePacket(String userID, int mode, int roomNum) {
        this(userID, mode, null, null, null, 0, null, null, roomNum);
    }

    // 방 수와 방 번호만 전달하는 생성자
    public GamePacket(String userID, int mode, int roomCount, int roomNum) {
        this(userID, mode, null, null, null, roomCount, null, null, roomNum);
    }

    // 메시지와 방 번호만 전달하는 생성자
    public GamePacket(String userID, int mode, String message, int roomNum) {
        this(userID, mode, message, null, null, 0, null,null, roomNum);
    }

    // UnoGame 객체와 방 번호만 전달하는 생성자
    public GamePacket(String userID, int mode, UnoGame uno, int roomNum) {
        this(userID, mode, null, null, uno, 0, null, null, roomNum);
    }

    public GamePacket(String userID, int mode, HashMap<Integer, ArrayList<String>> roomState, ArrayList<String> ReadyList, int roomNum){
        this(userID, mode, null, null, null, 0, ReadyList, roomState, roomNum);
    }

    // Getter와 Setter (필요시 사용 가능)
    public String getUserID() {
        return userID;
    }

    public int getMode() {
        return mode;
    }

    public String getMessage() {
        return message;
    }

    public ImageIcon getImage() {
        return image;
    }

    public UnoGame getUno() {
        return uno;
    }

    public int getRoomCount() {
        return roomCount;
    }

    public ArrayList<String> getReadyList() { return readyList; }

    public HashMap<Integer, ArrayList<String>> getRoomState() { return roomState; }

    public int getRoomNum() {
        return roomNum;
    }
}
