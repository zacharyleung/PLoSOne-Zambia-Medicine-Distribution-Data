package com.zacleung.util;

import java.util.Scanner;

public abstract class MyString {
	public static String addToStartOfEachLine(String input, String addend) {
		Scanner scanner = new Scanner(input);
		StringBuilder sb = new StringBuilder();
		while (scanner.hasNextLine()) {
			sb.append(String.format("%s%s%n", addend, scanner.nextLine()));
		}
		return sb.toString();
	}
}
