package me.rtsvk.utility.smarthome;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.*;

public class Main {

    public static final int PORT = 8080;

	public static void main(String[] args) throws IOException, ParseException {

        System.out.println("Running on platform: " + Util.platform());

        if (args.length > 0) {
	        System.out.println("Sending...");
            Util.alert("http://raspberrypi.local/", args[0]);
	        System.out.println("Exiting...");
            return;
        }

		System.out.println("Loading config file...");
        JSONObject jsonCfg = Util.readConfigFile("config.json");
        System.out.println("Success!");

        String key = jsonCfg.get("key").toString();
        Util.loadGpios((JSONArray) jsonCfg.get("gpio"));

        try {
            ServerSocket serverConnect = new ServerSocket(PORT);
            System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");

            // discovery server
            new Thread(new DiscoveryServer(PORT)).start();

            // we listen until user halts server execution
            while (true) {
                HTTPServer myServer = new HTTPServer(serverConnect.accept(), Util.getGpios(), key);

                // create dedicated thread to manage the client connection
                Thread thread = new Thread(myServer);
                thread.start();
            }

        } catch (IOException e) {
            System.err.println("Server Connection error : " + e.getMessage());
        }
    }
}
