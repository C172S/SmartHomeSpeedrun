package me.rtsvk.utility.smarthome;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

public class DiscoveryServer implements Runnable {

	private int port;

	public DiscoveryServer(int port) {
		this.port = port;
	}

	@Override
	public void run() {
		System.out.println("Discovery thread starting...");
		try {
			DatagramSocket udp = new DatagramSocket(this.port);
			while (true){
				byte[] buffer = new byte[32];
				DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
				udp.receive(packet);
				buffer = packet.getData();
				String data = new String(buffer, StandardCharsets.UTF_8).trim();

				if (data.equals("DISCOVER")) {
					String respStr = "" + this.port;
					byte[] response = respStr.getBytes(StandardCharsets.UTF_8);
					System.out.println("received discovery broadcast");
					System.out.println(packet.getSocketAddress());
					udp.send(new DatagramPacket(response, 0, response.length, packet.getSocketAddress()));
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
