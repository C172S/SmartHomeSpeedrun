package me.rtsvk.utility.smarthome;

import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public class HTTPServer implements Runnable {

	private Socket client;
	private Map<String, GPIO> gpioMap;

	public HTTPServer(Socket client, Map<String, GPIO> gpioMap) {
		this.client = client;
		this.gpioMap = gpioMap;
	}

	@Override
	public void run() {

		InputStream in = null;
		PrintWriter writer = null;

		try {
			in = this.client.getInputStream();
			writer = new PrintWriter(this.client.getOutputStream());

			StringBuilder req = new StringBuilder();
			System.out.println("Reading...");

			byte[] buffer = new byte[256];
			while (in.available() > 0) {
				int read = in.read(buffer);
				req.append(new String(buffer, 0, read));
			}

			System.out.println("Read, processing...");

			HTTP.Request request = new HTTP.Request(req.toString());
			request.print(System.out);

			if (!request.getProtocolVersion().equals("HTTP/1.1"))
				writer.println(HTTP.Response.HttpVersionNotSupported().build());

			if (request.getMethod().equals("POST")) { // POST method handling

				// received data format is not supported or there were no data at all
				if (request.getRequestBody() == null)
					writer.println(HTTP.Response.BadRequest().build());

				else {
					try {
						switch (request.getFile()) {
							case "/control":
								String gpio = request.getRequestParameter("pin").toString();
								String state = request.getRequestParameter("state").toString();
								GPIO pin = this.gpioMap.get(gpio);

								if (pin.mode().equals(GPIO.MODE_OUTPUT)){
									pin.write(Integer.parseInt(state));
									writer.println(HTTP.Response.OK().build());
								}
								else writer.println(HTTP.Response.BadRequest("Selected pin is not an output!").build());

								break;

							default:
								writer.println(HTTP.Response.NotFound("File '" + request.getFile() + "' was not found!"));
								break;
						}
					}
					catch (NullPointerException e) {
						e.printStackTrace();
						HTTP.Response response = HTTP.Response.InternalServerError(e.getMessage());
						writer.println(response.build());
					}
				}
			}
			else if (request.getMethod().equals("GET"))  // GET method handling
				writer.println(HTTP.Response.OK().build());

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
