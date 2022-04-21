package me.rtsvk.utility.smarthome;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static final int PORT = 8080;
    public static final int DISCOVERY_PORT = 8081;

	public static void main(String[] args) throws IOException, ParseException {

        if (args.length > 0) {
            alert("http://192.138.1.31/", args[0]);
            return;
        }

        JSONObject jsonCfg = (JSONObject)(new JSONParser().parse(new FileReader("config.json")));

        JSONArray pins = (JSONArray) jsonCfg.get("gpio");
        Map<String, GPIO> gpios = new HashMap<>();

        pins.forEach(e -> {
            try {
                JSONObject obj = ((JSONObject)e);
                gpios.put(obj.get("name").toString(), new GPIO(
                   Integer.parseInt(obj.get("pin").toString()),
                   obj.get("mode").toString(),
                        0
                ));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        try {
            ServerSocket serverConnect = new ServerSocket(PORT);
            System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");

            new Thread(() -> {
	            System.out.println("Discovery thread starting...");
	            try {
		            DatagramSocket udp = new DatagramSocket(DISCOVERY_PORT);
		            while (true){
			            byte[] buffer = new byte[32];
			            DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
			            udp.receive(packet);
			            buffer = packet.getData();
			            String data = new String(buffer, StandardCharsets.UTF_8).trim();

			            if (data.equals("DISCOVER")) {
			            	String respStr = "" + PORT;
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
            }).start();

            // we listen until user halts server execution
            while (true) {
                HTTPServer myServer = new HTTPServer(serverConnect.accept(), gpios);

                // create dedicated thread to manage the client connection
                Thread thread = new Thread(myServer);
                thread.start();
            }

        } catch (IOException e) {
            System.err.println("Server Connection error : " + e.getMessage());
        }
    }

    public static void alert(String url, String message) throws IOException {

        JSONObject obj = new JSONObject();
        if (message != null) obj.put("content", message);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        try(OutputStream os = conn.getOutputStream()) {
            byte[] input = obj.toJSONString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        conn.getInputStream().close();
        conn.disconnect();
    }
}
