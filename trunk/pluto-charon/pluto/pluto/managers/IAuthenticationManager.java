package pluto.managers;

public interface IAuthenticationManager {
	public static final String DOMAIN_CONTROLLER_NAME = "domain.controller.name";

	/**
	 * call this to initialize the manager
	 */
	public void init();

	/**
	 * @param username
	 * @param password
	 * @return true if user and password are valid for the predefined domain
	 */
	boolean checkAuthentication(String username, String password);

	/**
	 * 
	 * @param username
	 * @param userGroup
	 * @return true if username is assigned to a userGroup
	 */
	boolean checkAutorization(String username);
}
