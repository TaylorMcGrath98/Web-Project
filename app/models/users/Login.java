package models.users;

import play.data.validation.Constraints.Validate;
import play.data.validation.Constraints.Validatable;

// Login class 'backs' the login form
@Validate
public class Login implements Validatable<String> {

  private String email;
  private String password;

	// Validate method - invoked during error checking
	// after form based on a Login object has been submitted
	@Override
	public String validate() {
		
		// Call the static authenticate method in User
		if (User.authenticate(email, password) == null) {
		  return "Invalid user or password";
		}
		return null;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}

