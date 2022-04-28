package me.rtsvk.utility.smarthome;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class GPIO {

	public static final String MODE_OUTPUT = "op";
	public static final String MODE_INPUT = "ip";

	private String mode;
	private int pin;
	private int value;

	public GPIO(int pin, String mode) {
		this.pin = pin;
		this.mode(mode);
		this.write(0);
	}

	public GPIO(int pin, String mode, int initialValue){
		this.pin = pin;
		this.mode(mode);
		this.write(initialValue);
	}

	public String mode() {
		return this.mode;
	}

	public boolean mode(String mode) {
		this.mode = mode;
		try {
			Runtime.getRuntime().exec("raspi-gpio set " + this.pin + " " + this.mode);
			System.out.println("[GPIO " + this.pin + "]: Mode set to " + this.mode);
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}

	public boolean write(int value) {
		try {
			if (this.mode.equals(MODE_INPUT)) return false;
			this.value = value;
			System.out.println("[GPIO " + this.pin + "]: Value set to " + value);
			Runtime.getRuntime().exec("raspi-gpio set " + this.pin + " " + (value == 1 ? "dh" : "dl"));
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}

	public int read(){
		try {
			if (this.mode.equals(MODE_OUTPUT)) return this.value;
			Process proc = Runtime.getRuntime().exec("raspi-gpio get " + this.pin);
			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String result = reader.readLine();
			reader.close();
			String[] out = result.split(" ");
			this.value = Integer.parseInt(out[2].substring(6));
			return this.value;
		}
		catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}
}
