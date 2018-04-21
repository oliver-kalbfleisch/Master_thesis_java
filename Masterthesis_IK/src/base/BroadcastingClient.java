package base;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class BroadcastingClient {
	private static DatagramSocket socket = null;

	/*public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		int counter = 0;
		TimeUnit.SECONDS.sleep(10);
		while (counter < 30) {
			try {
				//Use network specifc broadcast addresss
				broadcast("go", InetAddress.getByName("192.168.1.255"));
				System.out.println("broadcasting " + counter);
				counter++;
				TimeUnit.SECONDS.sleep(5);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
*/
	public static void broadcast(String broadcastMessage, InetAddress address) throws IOException {
		socket = new DatagramSocket();
		socket.setBroadcast(true);

		byte[] buffer = broadcastMessage.getBytes();

		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 6666);
		System.out.println("sent data!");
		socket.send(packet);
		socket.close();
	}
}
