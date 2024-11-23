//2071334 임승택

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketAddress;

public class WithTalk extends JFrame {
	private JTextField t_input;
	private JTextField t_userID;
	private JTextField t_hostAddr;
	private JTextField t_portNum;
	
	private JTextPane t_display;
	private DefaultStyledDocument document;
	
	private JButton b_connect;
	private JButton b_disconnect;
	private JButton b_send;
	private JButton b_exit;
	private JButton b_select;
	
	private String serverAddress;
	private int serverPort;
	private String uid;
	
	private Socket socket;
	private ObjectOutputStream out;
	
	private Thread receiveThread = null;

	public WithTalk(String serverAddress, int serverPort) {
		super("2071334 WithTalk");

		this.serverAddress = serverAddress;
		this.serverPort = serverPort;

		setSize(500, 300);
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		buildGUI();

		setVisible(true);
	}

	private void buildGUI() {
		JScrollPane centerPanel = new JScrollPane(createDisplayPanel());
		JPanel southPanel = new JPanel(new GridLayout(3, 0));

		southPanel.add(createInputPanel());
		southPanel.add(createInfoPanel());
		southPanel.add(createControlPanel());

		this.add("Center", centerPanel);
		this.add("South", southPanel);
	}

	private JPanel createDisplayPanel() {
		JPanel displayPanel = new JPanel();
		displayPanel.setLayout(new BorderLayout());
		
		document = new DefaultStyledDocument();
		t_display = new JTextPane(document);
		t_display.setEditable(false);
		displayPanel.add("Center", t_display);

		return displayPanel;
	}

	private JPanel createInputPanel() {
		JPanel InputPanel = new JPanel();
		InputPanel.setLayout(new BorderLayout());
		t_input = new JTextField();
		t_input.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendMessage();
			}
		});

		b_send = new JButton("보내기");
		b_send.setEnabled(false);

		b_send.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendMessage();
			}
		});
		
		b_select = new JButton("선택하기");
		b_select.setEnabled(false);
		b_select.addActionListener(new ActionListener() {
			JFileChooser chooser = new JFileChooser();
			
			public void actionPerformed(ActionEvent e) {
				FileNameExtensionFilter filter = new FileNameExtensionFilter(
						"JPG & GIF & PNG Images", "jpg", "gif", "png");
				chooser.setFileFilter(filter);
				
				int ret = chooser.showOpenDialog(WithTalk.this);
				if(ret != JFileChooser.APPROVE_OPTION) {
					JOptionPane.showMessageDialog(WithTalk.this,  "파일을 선택하지 않았습니다.");
					return;
				}
				
		        t_input.setText(chooser.getSelectedFile().getAbsolutePath());
		        sendImage();
			}
		});  
		
		InputPanel.add("Center", t_input);

		JPanel p_button = new JPanel(new GridLayout(1,0));
		p_button.add(b_select);
		p_button.add(b_send);
		InputPanel.add(p_button, BorderLayout.EAST);

		return InputPanel;
	}

	private JPanel createInfoPanel() {
		JPanel InfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		t_userID = new JTextField(7);
		t_hostAddr = new JTextField(12);
		t_portNum = new JTextField(5);

		InfoPanel.add(new JLabel("아이디: "));
		InfoPanel.add(t_userID);

		InfoPanel.add(new JLabel("서버주소: "));
		InfoPanel.add(t_hostAddr);

		InfoPanel.add(new JLabel("포트번호: "));
		InfoPanel.add(t_portNum);

		t_userID.setText("guest" + getLocalAddr().split("\\.")[3]);
		t_hostAddr.setText(serverAddress);
		t_portNum.setText(String.valueOf(serverPort));

		return InfoPanel;
	}

	private JPanel createControlPanel() {
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new GridLayout(0, 3));
		b_connect = new JButton("접속하기");
		b_disconnect = new JButton("접속 끊기");
		b_disconnect.setEnabled(false);
		b_exit = new JButton("종료하기");

		b_connect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				try {
					connectToSever();
					sendUserID();
				} catch (UnknownHostException e1) {
					printDisplay("서버 주소와 포트번호를 확인하세요: "+ e1.getMessage());
					return;
				} catch (IOException e1) {
					printDisplay("서버와 연결 오류: "+ e1.getMessage());
					return;
				}

				b_connect.setEnabled(false);
				b_disconnect.setEnabled(true);
				b_select.setEnabled(true);

				t_input.setEnabled(true);
				b_send.setEnabled(true);
				b_exit.setEnabled(false);

				t_userID.setEditable(false);
				t_hostAddr.setEditable(false);
				t_portNum.setEditable(false);
			}
		});

		b_disconnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				b_send.setEnabled(false);
				b_disconnect.setEnabled(false);
				b_connect.setEnabled(true);
				b_exit.setEnabled(true);
				b_select.setEnabled(false);

				disconnect();
			}
		});

		b_exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		controlPanel.add(b_connect);
		controlPanel.add(b_disconnect);
		controlPanel.add(b_exit);
		return controlPanel;
	}

	private void printDisplay(String msg) {
		int len = t_display.getDocument().getLength();
		
		try {
			document.insertString(len, msg + "\n", null);
		}catch(BadLocationException e) {
			e.printStackTrace();
		}
		
		t_display.setCaretPosition(len);
	}

	private String getLocalAddr() {
		InetAddress local = null;
		String addr = "";
		try {
			local = InetAddress.getLocalHost();
			addr = local.getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return addr;
	}
	
	private void printDisplay(ImageIcon icon) {
		t_display.setCaretPosition(t_display.getDocument().getLength());
		
		if(icon.getIconWidth() > 400) {
			Image img = icon.getImage();
			Image changeImg = img.getScaledInstance(400, -1, Image.SCALE_SMOOTH);
			icon = new ImageIcon(changeImg);
		}
		
		t_display.insertIcon(icon);
		
		printDisplay("");
		t_input.setText("");
	}

	private void connectToSever() throws UnknownHostException, IOException {
		socket = new Socket();
		SocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
		socket.connect(sa, 3000);
		
		out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
	
		
		receiveThread = new Thread(new Runnable() {
			
			private ObjectInputStream in;
			
			private void receiveMessage() {
				try {
					ChatMsg inMsg = (ChatMsg)in.readObject();
					if (inMsg == null) {
						printDisplay("서버 연결 종료");
						disconnect();
						return;
					}
					
					switch(inMsg.mode) {
					case ChatMsg.MODE_TX_STRING:
						printDisplay(inMsg.userID + ": " + inMsg.message);
						break;
					case ChatMsg.MODE_TX_IMAGE:
						printDisplay(inMsg.userID + ": " + inMsg.message);
						printDisplay(inMsg.image);
						break;
					}
					
				} catch (IOException e) {
					printDisplay("연결을 종료했습니다.");
				} catch (ClassNotFoundException e) {
					printDisplay("잘못된 객체가 전달되었습니다.");
				}
			}
			
			@Override
			public void run() {
				try {
					in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
				}catch(IOException e) {
					printDisplay("입력 스트림이 열리지 않음");
				}
				while (receiveThread == Thread.currentThread()) {
					receiveMessage();
				}
			}
		});
		receiveThread.start();
	}

	private void disconnect() {
		send(new ChatMsg(uid, ChatMsg.MODE_LOGOUT));
		try {
			receiveThread = null;
			socket.close();
		} catch (IOException e) {
			System.err.println("클라이언트 닫기 오류> " + e.getMessage());
			System.exit(-1);
		}
	}

	private void send(ChatMsg msg) {
		try {
			out.writeObject(msg);
			out.flush();
		} catch (IOException e) {
			System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
		}
	}
	
	private void sendMessage() {
		String message = t_input.getText(); // 텍스트 필드에 문자열 가져옴
		if(message.isEmpty()) return;
		send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING, message));	
		t_input.setText("");
	}

	private void sendUserID() {
		uid = t_userID.getText();
		send(new ChatMsg(uid, ChatMsg.MODE_LOGIN));
	}
	
	private void sendImage() {
		String filename = t_input.getText().strip();
		if (filename.isEmpty())
			return;

		File file = new File(filename);
		if (!file.exists()) {
			printDisplay(">> 파일이 존재하지 않습니다;" + filename + "\n");
			return;
		}
		
		ImageIcon icon = new ImageIcon(filename);
		send(new ChatMsg(uid, ChatMsg.MODE_TX_IMAGE, file.getName(),icon));
		
		t_input.setText("");
	}
	
//	private void sendFile(File file) {
//	    if (!file.exists()) {
//	        printDisplay("파일이 존재하지 않습니다: " + file.getAbsolutePath());
//	        return;
//	    }
//
//	    try {
//	    	long fileSize = file.length();
//	        byte[] fileData = Files.readAllBytes(file.toPath());
//	        send(new ChatMsg(uid, ChatMsg.MODE_TX_FILE, file.getName(),null, fileSize));
//
//	        printDisplay("파일 전송: " + file.getName());
//	    } catch (IOException e) {
//	        printDisplay("파일 읽기 오류: " + e.getMessage());
//	    }
//	}
	

	public static void main(String[] args) {
		String serverAddress = "localhost";
		int serverPort = 54321;

		new WithTalk(serverAddress, serverPort);
	}
}