package me.rtsvk.utility.smarthome;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

public class HTTP {

	public static class Request {

		private String rawRequest;
		private String method;
		private String file;
		private String protocolVersion;
		private Map<String, String> requestProperties;
		private Map<String, Object> requestBody;

		public Request(String request) throws ParseException {

			//request = request.replace("\n", "NL");
			//request = request.replace("\r", "CR");

			this.rawRequest = request + ""; // request clone
			System.out.println("Incoming request:");
			//System.out.println(this.rawRequest);

			String[] req = request.split("\r\n\r\n");
			String[] header = req[0].split("\r\n");
			String[] data = header[0].split(" ");

			this.method = data.length > 0 ? data[0] : "GET";
			this.file = data.length > 1 ? data[1] : "/";
			this.protocolVersion = data.length > 2 ? data[2] : "HTTP/1.1";

			// handle request properties
			this.requestProperties = new HashMap<>();
			for (int i = 1; i < header.length; i++) {
				//System.out.println(header[i]);
				String[] _data = header[i].split(": ");
				if (_data.length > 1) this.requestProperties.put(_data[0].toLowerCase(), _data[1].toLowerCase());
			}

			// if there is a non-empty POST, handle it
			if (this.method.equals("POST")) {

				if (this.requestProperties.get("content-length") == null) return;
				if (this.requestProperties.get("content-length").equals("0")) return;

				System.out.println("Request body [DEBUG]: " + req[1]);

				// default form data format:
				if (this.requestProperties.get("content-type").startsWith("application/x-www-form-urlencoded")) {
					this.requestBody = new HashMap<>();
					String[] postData = req[1].split("&");
					for (String elm : postData) {
						//System.out.println(elm);
						String[] _data = elm.split("=");
						this.requestBody.put(_data[0], _data[1]);
					}
				}

				// JSON format
				else if (this.requestProperties.get("content-type").startsWith("application/json")){
					this.requestBody = new HashMap<>();
					System.out.println(req[1]);
					JSONObject obj = (JSONObject)(new JSONParser().parse(req[1]));
					obj.keySet().forEach(key -> this.requestBody.put(key.toString(), obj.get(key.toString())));
				}
			}
		}

		public void print(PrintStream writer) {
			writer.println("Raw request: " + this.getRawRequest());
			writer.println("Method: " + this.getMethod());
			writer.println("File: " + this.getFile());
			writer.println("HTTP version: " + this.getProtocolVersion());
			writer.println("Request body: ");
			if (this.getRequestBody() != null)
				this.getRequestBody()
						.keySet()
						.forEach(e -> writer.println(e + " = " + this.getRequestParameter(e).toString()));
			else writer.println("Body null");
		}

		public String getMethod() {
			return method;
		}

		public String getFile() {
			return file;
		}

		public String getProtocolVersion() {
			return protocolVersion;
		}

		public String getRawRequest() {
			return rawRequest;
		}

		public Map<String, Object> getRequestBody() {
			return requestBody;
		}

		public Object getRequestParameter(String param) {
			return requestBody.get(param);
		}

		public Object getRequestProperty(String param) {
			return requestProperties.get(param);
		}
	}

	public static class Response {

		public static final String HTTP_OK = "200 OK";
		public static final String HTTP_BAD_REQUEST = "400 Bad Request";
		public static final String HTTP_FORBIDDEN = "403 Forbidden";
		public static final String HTTP_NOT_FOUND = "404 Not Found";
		public static final String HTTP_INTERNAL_SERVER_ERROR = "500 Internal Server Error";
		public static final String HTTP_NOT_IMPLEMENTED = "501 Not Implemented";
		public static final String HTTP_VERSION_NOT_SUPPORTED = "501 HTTP Version Not Supported";


		private String protocolVersion;
		private String responseCode;
		private Map<String, Object> responseParameters;
		private String responseBody;

		public Response(String protocolVersion, String responseCode, Map<String, Object> responseParameters, String responseBody) {
			this.protocolVersion = protocolVersion;
			this.responseCode = responseCode;
			this.responseParameters = responseParameters;
			this.responseBody = responseBody;
		}

		public static Response OK() throws IOException {
			// read file
			StringBuilder sb = new StringBuilder();
			try (FileInputStream fis = new FileInputStream("index.html")) {
				byte[] buf = new byte[128];
				while (fis.available() > 0) {
					int read = fis.read(buf);
					sb.append(new String(buf, 0, read));
				}
			}

			return prepResponse(Response.HTTP_OK, sb.toString());
		}

		public static Response BadRequest() {
			return BadRequest("Wrong or unsupported data format!");
		}

		public static Response BadRequest(String message) {
			return prepResponse(Response.HTTP_BAD_REQUEST, message);
		}

		public static Response Forbidden() {
			return Forbidden("File not found!");
		}

		public static Response Forbidden(String message) {
			return prepResponse(Response.HTTP_FORBIDDEN, message);
		}

		public static Response NotFound() {
			return NotFound("File not found!");
		}

		public static Response NotFound(String message) {
			return prepResponse(Response.HTTP_NOT_FOUND, message);
		}

		public static Response InternalServerError() {
			return InternalServerError("Unspecified error");
		}

		public static Response InternalServerError(String message) {
			return prepResponse(Response.HTTP_INTERNAL_SERVER_ERROR, message);
		}

		public static Response NotImplemented() {
			return NotImplemented("Method not supported!");
		}

		public static Response NotImplemented(String message) {
			return prepResponse(Response.HTTP_NOT_IMPLEMENTED, message);
		}

		public static Response HttpVersionNotSupported() {
			return HttpVersionNotSupported("HTTP version not supported");
		}

		public static Response HttpVersionNotSupported(String message) {
			return prepResponse(Response.HTTP_VERSION_NOT_SUPPORTED, message);
		}

		public static Response prepResponse(String responseCode, String message) {
			return prepResponse(responseCode, message, "text/html", false);
		}

		public static Response prepResponse(String responseCode, String message, String contentType, boolean raw) {
			String body = (raw ? "" : "<h1>" + responseCode + "</h1><b>") + message;
			Map<String, Object> params = new HashMap<>();
			params.put("Content-Type", contentType);
			params.put("Connection", "close");
			params.put("Content-length", body.length());

			return new HTTP.Response(
					"HTTP/1.1",
					responseCode,
					params,
					body
			);
		}

		public String build() {
			StringBuilder sb = new StringBuilder();

			// do the magic here
			sb.append(this.protocolVersion).append(" ").append(this.responseCode).append("\r\n");
			if (this.responseParameters != null)
				this.responseParameters.keySet().forEach(e ->
					sb.append(e)
						.append(": ")
						.append(this.responseParameters.get(e))
						.append("\r\n")
				);
			sb.append("\r\n");
			if (this.responseBody != null) sb.append(this.responseBody).append("\r\n");

			return sb.toString();
		}

		public void setProtocolVersion(String version) {
			this.protocolVersion = version;
		}

		public void setResponseCode(String responseCode) {
			this.responseCode = responseCode;
		}

		public void setResponseParameters(Map<String, Object> params) {
			this.responseParameters = params;
		}

		public void setResponseBody(String body) {
			this.responseBody = body;
		}
	}
}
