package utils;
import pluto.managers.IAuthenticationManager;

public class LocalAuthentication implements IAuthenticationManager {
	public void init() {}

	public boolean checkAuthentication(String username, String password) {
		return true;
	}

	public boolean checkAutorization(String username) {
		return true;
	}

}