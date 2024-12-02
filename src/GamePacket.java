import javax.swing.*;
import java.io.Serializable;

public class GamePacket implements Serializable {
    // 메시지 모드를 정의하는 상수들
    public static final int MODE_LOGIN = 0x1;
    public static final int MODE_LOGOUT = 0x2;

    public static final int MODE_TX_STRING = 0x10;
    public static final int MODE_BROAD_STRING = 0x11;

    public static final int MODE_TX_IMAGE = 0x30;

    public static final int MODE_ROOM_ADD = 0x40;  // 방 추가 정보
    public static final int MODE_ROOM_COUNT = 0x41;  // 방 수와 포트 정보
    public static final int MODE_ROOM_JOIN = 0x42;  // 방 입장 요청
    public static final int MODE_ROOM_READY = 0x43;
    public static final int MODE_ROOM_INFO = 0x44;

    public static final int MODE_UNO_START = 0x50;
    public static final int MODE_UNO_UPDATE = 0x51;

    // 필드들
    private String userID;
    private int mode;
    private String message;
    private ImageIcon image;
    private UnoGame uno;
    private int roomCount;
    private Integer roomReady;
    private Integer roomJoin;
    private int roomNum;
    private int participantsCount;

    // 모든 필드를 처리할 수 있는 생성자
    public GamePacket(String userID, int mode, String message, ImageIcon image, UnoGame uno, int roomCount, Integer roomReady, Integer roomJoin, int roomNum, int participantsCount) {
        this.userID = userID;
        this.mode = mode;
        this.message = message;
        this.image = image;
        this.uno = uno;
        this.roomCount = roomCount;
        this.roomReady = roomReady;
        this.roomJoin = roomJoin;
        this.roomNum = roomNum;
        this.participantsCount = participantsCount;
    }

    // 방 번호와 참가자 수를 전달하는 생성자
    public GamePacket(String userID, int mode, int roomNum, int participantsCount) {
        this(userID, mode, null, null, null, 0, 0, 0, roomNum, participantsCount);
    }

    // 메시지와 방 번호만 전달하는 생성자
    public GamePacket(String userID, int mode, String message, int roomNum) {
        this(userID, mode, message, null, null, 0, 0, 0, roomNum, 0);
    }

    // 메시지와 이미지만 전달하는 생성자
    public GamePacket(String userID, int mode, String message, ImageIcon image, int roomNum) {
        this(userID, mode, message, image, null, 0, 0, 0, roomNum, 0);
    }

    // 방 번호와 UnoGame 객체만 전달하는 생성자
    public GamePacket(String userID, int mode, UnoGame uno, int roomNum) {
        this(userID, mode, null, null, uno, 0, 0, 0, roomNum, 0);
    }



    // 방 레디/참가 정보와 방 번호만 전달하는 생성자
    public GamePacket(String userID, int mode, Integer roomReady, Integer roomJoin, int roomNum) {
        this(userID, mode, null, null, null, 0, roomReady, roomJoin, roomNum, 0);
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

    public Integer getRoomReady() {
        return roomReady;
    }

    public Integer getRoomJoin() {
        return roomJoin;
    }

    public int getRoomNum() {
        return roomNum;
    }

    public int getParticipantsCount() {
        return participantsCount;
    }

    // Setter for participantsCount
    public void setParticipantsCount(int participantsCount) {
        this.participantsCount = participantsCount;
    }

}
