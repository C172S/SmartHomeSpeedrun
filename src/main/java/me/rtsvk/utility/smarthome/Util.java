package me.rtsvk.utility.smarthome;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class Util {

	private static Map<String, GPIO> gpios;

	public static JSONObject readConfigFile(String fileName) throws IOException, ParseException {

		File file = new File(fileName);
		JSONParser parser = new JSONParser();

		if (file.exists())
			return (JSONObject) parser.parse(new FileReader(file));

		else {
			System.out.println("Config not found, generating...");
			Map<String, Object> defVals = new HashMap<>();

			// create a demo object to show the object format and add it to a JSON array
			Map<String, Object> gpio = new HashMap<>();
			gpio.put("name", "demo_pin");
			gpio.put("pin", 16);
			gpio.put("mode", GPIO.MODE_OUTPUT);
			JSONArray gpios = new JSONArray();
			gpios.add(gpio);

			// create a random string to act as the AES encryption key
			int leftLimit = 97; // letter 'a'
			int rightLimit = 122; // letter 'z'
			int targetStringLength = 256; // key length
			Random random = new Random();
			String key = random.ints(leftLimit, rightLimit + 1)
					.limit(targetStringLength)
					.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
					.toString();

			// build the JSON object
			defVals.put("gpio", gpios);
			defVals.put("key", key);

			FileWriter fw = new FileWriter(file);
			fw.write(new JSONObject(defVals).toJSONString());
			fw.close();

			return new JSONObject(defVals);
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

	public static void loadGpios(JSONArray pins) {
		Map<String, GPIO> _gpios = new HashMap<>();
		Iterator i = pins.iterator();
		while (i.hasNext()) {
			JSONObject obj = (JSONObject) i.next();
			_gpios.put(obj.get("name").toString(), new GPIO(
					Integer.parseInt(obj.get("pin").toString()),
					obj.get("mode").toString()
			));
		}
		gpios = _gpios;
	}

	public static Map<String, GPIO> getGpios() {
		return gpios;
	}

	public static String platform() {
		return System.getProperty("os.name");
	}
}
