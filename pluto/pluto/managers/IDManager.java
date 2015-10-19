package pluto.managers;

import java.util.UUID;

public class IDManager {

	/**
	 * @return unique string hex id
	 */
	public static String getID() {
		String id = UUID.randomUUID().toString();
		StringBuilder result = new StringBuilder(id.length());
		for (int i = 0; i < id.length(); i++) {
			char c = id.charAt(i);
			if (c != '-') {
				if (c >= 'a' && c <= 'z') {
					result.append((char) (c + 'A' - 'a'));
				} else {
					result.append(c);
				}
			}
		}
		return result.toString();
	}
}
