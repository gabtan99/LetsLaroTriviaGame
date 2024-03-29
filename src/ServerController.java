import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class ServerController {

	// Maximum Segment Size - Quantity of data from the application layer in the segment
	public static final int MSS = 4;
	// Window size - Number of packets sent without acking
	public static final int WINDOW_SIZE = 2;
	// Time (ms) before REsending all the non-acked packets
	public static final int TIMER = 30;
	// PORT
	public final static int PORT = 7331;
	// Buffer size
	private final static int BUFFER = 1024;


	public Button startBtn;
	public TextField playerTF;
	public TextField questionTF;
	public ListView pListView;
	// MAX players
	private int MAXPLAYER;

	private static final int COUNTDOWN = 15;
	private static final int WAITRESULT = 3;
	private static final int PROCESSINGTIME = 1;

	private DatagramSocket socket;
	private ArrayList<InetAddress> clientAddresses;
	private ArrayList<Integer> clientPorts;
	private List<Player> playerList;
	private HashSet<String> existingClients;
	private TriviaGame game;

	public ServerController(Stage primaryStage) throws Exception {

		FXMLLoader loader = new FXMLLoader(getClass().getResource("serverTemplate.fxml"));
		loader.setController(this);

		StageManager sm = new StageManager(primaryStage);
		sm.loadScene(loader);
		sm.setWindowName("Let's Laro Server");
	}

	private void initServer (int nQuestions) throws Exception {

		socket = new DatagramSocket(PORT);
		clientAddresses = new ArrayList();
		clientPorts = new ArrayList();
		existingClients = new HashSet();
		playerList = new ArrayList<>();

		game = new TriviaGame(nQuestions);

		initLobby();
	}

	private void initLobby()  {

		Thread t = new Thread() {
			public void run () {
				System.out.println("Waiting for enough players to connect.");

				while (existingClients.size() < MAXPLAYER) {

					byte[] buf = new byte[BUFFER];

					try {
						DatagramPacket packet = new DatagramPacket(buf, buf.length);
						socket.receive(packet);

						Player noob = registerPlayer(buf);

						InetAddress clientAddress = packet.getAddress();
						int clientPort = packet.getPort();

						if (noob != null) {
							String id = clientAddress.toString() + "," + clientPort;
							if (!existingClients.contains(id)) {
								existingClients.add(id);
								clientPorts.add(clientPort);
								clientAddresses.add(clientAddress);
								playerList.add(noob);
								game.connectPlayer(noob);
								updatePlayerList();
								sendConnectConfirmation(clientAddress, clientPort);
							}
						}
						else {
							sendConnectionError(clientAddress, clientPort);
						}

					} catch (Exception e) {
						System.err.println(e);
					}
				}
				try {
					castGameStart();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		t.setDaemon(true);
		t.start();

	}

	private void castGameStart() throws Exception {

		System.out.println("Game will begin\n\n");

		game.startGame();

		System.out.println("Max number of questions: " + game.getnQuestions());

		for (int i = 0; i<existingClients.size(); i++) {
			String start = "START";
			byte buf[] = Serializer.toBytes(start);
			sendPacket(clientAddresses.get(i), clientPorts.get(i), buf);
		}

		castGameProper();

	}

	private void castGameProper() throws Exception {

		while (playerList.size() > 0) {

			game.askQuestion();

			for (int i = 0; i < playerList.size(); i++) {
				GameState playerstate = game.getGameState(playerList.get(i));
				byte[] state = Serializer.toBytes(playerstate);
				sendPacket(clientAddresses.get(i), clientPorts.get(i), state);
			}


			if (!game.isGameDone()) {
				TimeUnit.SECONDS.sleep(COUNTDOWN+WAITRESULT+PROCESSINGTIME);
				for (int i = 0; i < playerList.size(); i++) {
					requestAnswer(clientAddresses.get(i), clientPorts.get(i), playerList.get(i).getName());
					PlayerResponse response = (PlayerResponse) Serializer.toObject(receivePacket());
					recordScore(response);
				}
			}
			else {
				break;
			}


			System.out.println("\n---------------------------------------------------------------------\n");
		}

		castGameEnd();
		Platform.exit();
		System.exit(0);
	}

	private void requestAnswer(InetAddress address, int port, String username)  throws Exception{
		byte[] dude = Serializer.toBytes(username);
		sendPacket(address, port, dude);
	}

	private void castGameEnd () {
		game.endGame();
		System.out.println("Game has ended");
	}

	private Player registerPlayer(byte[] buf) {

		String content = new String(buf);
		content = content.trim();

		// verify if new player
		for (Player p: playerList) {
			if (p.getName().equalsIgnoreCase(content)) {
				return null;
			}
		}

		Player noob = new Player(content);

		return noob;
	}

	private void recordScore(PlayerResponse response) throws Exception{

		if (response.getAnswer()!= null) {
			game.checkAnswer(response.getAnswer(), response.getPlayer());
		}
		else {
			if (game.disconnectPlayer(response.getPlayer().getName())) {

				for(int i= 0; i<playerList.size(); i++) {
					if (playerList.get(i).getName().equals(response.getPlayer().getName())) {
						clientAddresses.remove(i);
						clientPorts.remove(i);
					}
				}
				playerList = game.getPlayersList();

				updatePlayerList();

				if (playerList.size() == 0) {
					castGameEnd();
					System.exit(0);
				}


			}
		}
	}

	private void sendConnectConfirmation (InetAddress address, int port) throws Exception {
		String msg = "CONNECTED";
		byte buf[] = Serializer.toBytes(msg);
		sendPacket(address, port, buf);
	}

	private void sendConnectionError (InetAddress address, int port) throws Exception {
		String msg = "ERROR";
		byte buf[] = Serializer.toBytes(msg);
		sendPacket(address, port, buf);
	}

	private byte[] receivePacket () throws Exception{
		byte[] receivedData = new byte[ClientController.MSS + 83];
		int waitingFor = 0;
		ArrayList<RDTPacket> received = new ArrayList<RDTPacket>();
		boolean end = false;

		while(!end){

			System.out.println("Waiting for packet");

			// Receive packet
			DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);
			socket.receive(receivedPacket);

			// Unserialize to a RDTPacket object
			RDTPacket packet = (RDTPacket) Serializer.toObject(receivedPacket.getData());

			System.out.println("Packet with sequence number " + packet.getSeq() + " received (last: " + packet.isLast() + " )");

			if(packet.getSeq() == waitingFor && packet.isLast()){

				waitingFor++;
				received.add(packet);

				System.out.println("Last packet received");

				end = true;

			}else if(packet.getSeq() == waitingFor){
				waitingFor++;
				received.add(packet);
				System.out.println("Packed stored in buffer");
			}else{
				System.out.println("Packet discarded (not in order)");
			}

			// Create an RDTAck object
			RDTAck ackObject = new RDTAck(waitingFor);

			// Serialize
			byte[] ackBytes = Serializer.toBytes(ackObject);


			DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, receivedPacket.getAddress(), receivedPacket.getPort());

			// Send with some probability of loss

			socket.send(ackPacket);

		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );

		for(RDTPacket p : received){
			for(byte b: p.getData()){
				outputStream.write(b);
			}
		}

		byte[] obj = outputStream.toByteArray();

		return obj;

	}

	private void sendPacket (InetAddress address, int port, byte[] object) throws Exception {

		// Sequence number of the last packet sent (rcvbase)
		int lastSent = 0;

		// Sequence number of the last acked packet
		int waitingForAck = 0;

		System.out.println("Data size: " + object.length + " bytes");

		// Last packet sequence number
		int lastSeq = (int) Math.ceil( (double) object.length / MSS);

		System.out.println("Number of packets to send: " + lastSeq);

		DatagramSocket toReceiver = new DatagramSocket();

		// ServerController address
		InetAddress receiverAddress = InetAddress.getByName("localhost");

		// List of all the packets sent
		ArrayList<RDTPacket> sent = new ArrayList<RDTPacket>();

		while(true){

			// Sending loop
			while(lastSent - waitingForAck < WINDOW_SIZE && lastSent < lastSeq){

				// Array to store part of the bytes to send
				byte[] filePacketBytes = new byte[MSS];

				// Copy segment of data bytes to array
				filePacketBytes = Arrays.copyOfRange(object, lastSent*MSS, lastSent*MSS + MSS);

				// Create RDTPacket object
				RDTPacket rdtPacketObject = new RDTPacket(lastSent, filePacketBytes, (lastSent == lastSeq-1) ? true : false);

				// Serialize the RDTPacket object
				byte[] sendData = Serializer.toBytes(rdtPacketObject);

				// Create the packet
				DatagramPacket packet = new DatagramPacket(sendData, sendData.length, address, port );

				System.out.println("Sending packet with sequence number " + lastSent +  " and size " + sendData.length + " bytes");

				// Add packet to the sent list
				sent.add(rdtPacketObject);

				// Send with some probability of loss
				toReceiver.send(packet);


				// Increase the last sent
				lastSent++;

			} // End of sending while

			// Byte array for the ACK sent by the receiver
			byte[] ackBytes = new byte[40];

			// Creating packet for the ACK
			DatagramPacket ack = new DatagramPacket(ackBytes, ackBytes.length);

			try{
				// If an ACK was not received in the time specified (continues on the catch clausule)
				toReceiver.setSoTimeout(TIMER);

				// Receive the packet
				toReceiver.receive(ack);

				// Unserialize the RDTAck object
				RDTAck ackObject = (RDTAck) Serializer.toObject(ack.getData());

				System.out.println("Received ACK for " + ackObject.getPacket());

				// If this ack is for the last packet, stop the sender (Note: gbn has a cumulative acking)
				if(ackObject.getPacket() == lastSeq){
					break;
				}

				waitingForAck = Math.max(waitingForAck, ackObject.getPacket());

			}catch(SocketTimeoutException e){
				// then send all the sent but non-acked packets

				for(int i = waitingForAck; i < lastSent; i++){

					// Serialize the RDTPacket object
					byte[] sendData = Serializer.toBytes(sent.get(i));

					// Create the packet
					DatagramPacket packet = new DatagramPacket(sendData, sendData.length, address, port );

					// Send with some probability
					toReceiver.send(packet);

				}
			}


		}
		System.out.println("Finished transmission");

	}

	public void startServer(ActionEvent actionEvent) {


		try {

			MAXPLAYER = Integer.parseInt(playerTF.getText());
			int nQuestions = Integer.parseInt(questionTF.getText());

			if (MAXPLAYER > 0 && nQuestions > 0) {
				try {
					initServer(nQuestions);
				} catch (Exception e) {
					e.printStackTrace();
				}

				playerTF.setEditable(false);
				questionTF.setEditable(false);
				startBtn.setText("SERVER IS RUNNING");
				startBtn.setDisable(true);
			}
		}
		catch (Exception e) {
			System.out.println("Invalid input");
		}

	}

	private void updatePlayerList () {

		pListView.getItems().clear();

		for (int i = 0; i<playerList.size(); i++) {
			HBox entry = new HBox();

			Label name = new Label(playerList.get(i).getName());
			Label ip = new Label(clientAddresses.get(i).getHostAddress() + ":" + clientPorts.get(i));

			name.setPrefWidth(100);
			ip.setPrefWidth(130);

			entry.getChildren().addAll(name, ip);

			pListView.getItems().add(entry);
		}
	}

}
