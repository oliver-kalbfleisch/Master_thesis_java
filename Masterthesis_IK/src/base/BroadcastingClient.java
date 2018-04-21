package base;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class BroadcastingClient {

	public static void broadcast(String broadcastMessage, InetAddress address) throws IOException {
		try (DatagramSocket socket = new DatagramSocket();) {

			socket.setBroadcast(true);

			byte[] buffer = broadcastMessage.getBytes();

			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 6666);
			System.out.println("sent data!");
			socket.send(packet);
		}

	}
}
