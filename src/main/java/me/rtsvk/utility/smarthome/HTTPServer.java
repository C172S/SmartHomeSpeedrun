package me.rtsvk.utility.smarthome;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class HTTPServer implements Runnable {

	private Socket client;
	private Map<String, GPIO> gpioMap;
	private String key;

	public HTTPServer(Socket client, Map<String, GPIO> gpioMap, String key) {
		this.client = client;
		this.gpioMap = gpioMap;
		this.key = key;
	}

	@Override
	public void run() {

		InputStream in = null;
		PrintWriter writer = null;

		try {
			in = this.client.getInputStream();
			writer = new PrintWriter(this.client.getOutputStream());

			StringBuilder req = new StringBuilder();
			System.out.println("Incoming request from " + this.client.getRemoteSocketAddress());

			byte[] buffer = new byte[256];
			while (in.available() > 0) {
				int read = in.read(buffer);
				req.append(new String(buffer, 0, read));
			}

			System.out.println("Read, processing...");

			HTTP.Request request = new HTTP.Request(req.toString());
			request.print(System.out);

			// check protocol version
			if (!request.getProtocolVersion().equals("HTTP/1.1"))
				writer.println(HTTP.Response.HttpVersionNotSupported().build());

			// request has no body at all
			/*if (request.getRequestBody() == null)
				writer.println(HTTP.Response.BadRequest().build());

			// check auth key
			else if (request.getRequestParameter("key") == null)
				writer.println(HTTP.Response.BadRequest().build());

			else if (!request.getRequestParameter("key").equals(this.key))
				writer.println(HTTP.Response.Forbidden().build());

			// if all is good, proceed to process the request and send the response
			else */if (request.getMethod().equals("POST")) { // POST method handling
				try {
					switch (request.getFile().substring(1)) {

						case "control":
							String gpio = request.getRequestParameter("pin").toString();
							String state = request.getRequestParameter("state").toString();
							GPIO pin = this.gpioMap.get(gpio);
							if (pin.mode().equals(GPIO.MODE_OUTPUT)){
								pin.write(Integer.parseInt(state));
								writer.println(HTTP.Response.OK().build());
							}
							else writer.println(HTTP.Response.BadRequest("Selected pin is not an output!").build());
							break;

						case "add_control":
							writer.println(HTTP.Response.NotFound("Coming soon").build());
							break;

						case "reload":
							JSONObject obj = Util.readConfigFile("config.json");
							Util.loadGpios((JSONArray) obj.get("gpio"));
							writer.println(HTTP.Response.OK().build());
							break;

						default:
							writer.println(HTTP.Response.NotFound("File '" + request.getFile() + "' was not found!").build());
							break;
						}
					}
					catch (NullPointerException e) {
						e.printStackTrace();
						HTTP.Response response = HTTP.Response.InternalServerError(e.getMessage());
						writer.println(response.build());
					}
			}
			else if (request.getMethod().equals("GET")) { // GET method handling

				switch (request.getFile().substring(1)) {
					case "output":
						JSONArray pins = new JSONArray();
						this.gpioMap.keySet().forEach(e -> {
							GPIO _pin = this.gpioMap.get(e);
							if (_pin.mode().equals(GPIO.MODE_OUTPUT)) {
								Map<String, Object> json = new HashMap<>();
								json.put("pin", e);
								json.put("value", _pin.read());
								pins.add(json);
							}
						});

						System.out.println(pins.toJSONString());
						HTTP.Response response = HTTP.Response.prepResponse(
								HTTP.Response.HTTP_OK,
								pins.toJSONString(),
								"application/json",
								true
						);

						writer.println(response.build());
						break;

					default:
						writer.println(HTTP.Response.OK().build());
						break;
				}

			}

			else   // method not supported
				writer.println(HTTP.Response.NotImplemented("Method " + request.getMethod() + " is not supported!").build());

			writer.flush();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		finally {
			try {
				in.close();
				writer.close();
				client.close();
			}
			catch (Exception e) {
				System.out.println("error closing");
			}
		}
	}
}
