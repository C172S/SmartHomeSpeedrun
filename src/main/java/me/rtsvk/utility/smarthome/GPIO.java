package me.rtsvk.utility.smarthome;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class GPIO {

	public static final String MODE_OUTPUT = "op";
	public static final String MODE_INPUT = "ip";

	private String mode;
	private int pin;

	public GPIO(int pin, String mode) throws IOException {
		this.pin = pin;
		this.mode(mode);
	}

	public GPIO(int pin, String mode, int initialValue) throws IOException {
		this.pin = pin;
		this.mode(mode);
		this.write(initialValue);
	}

	public String mode() {
		return this.mode;
	}

	public void mode(String mode) throws IOException {
		Runtime.getRuntime().exec("raspi-gpio set " + pin + " " + mode);
		this.mode = mode;
	}

	public boolean write(int value) {
		try {
			if (this.mode.equals(MODE_INPUT)) return false;
			Runtime.getRuntime().exec("raspi-gpio set " + this.pin + " " + (value == 1 ? "dh" : "dl"));
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}

	public int read() throws IOException {
		if (this.mode.equals(MODE_OUTPUT)) return -1;
		Process proc = Runtime.getRuntime().exec("raspi-gpio get " + this.pin);
		BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		String result = reader.readLine();
		reader.close();
		String[] out = result.split(" ");
		return Integer.parseInt(out[2].substring(6));
	}
}
