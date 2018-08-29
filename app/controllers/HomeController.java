package controllers;

import play.mvc.*;

import play.api.Environment;
import play.data.*;
import play.db.ebean.Transactional;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import models.*;
import models.users.*;

import views.html.*;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {

    private FormFactory formFactory;
    private Environment env;

    @Inject
    public HomeController(FormFactory f, Environment e) {
        this.formFactory = f;
        this.env = e;
    }


    private User getCurrentUser() {
		User u = User.getLoggedIn(session().get("email"));
		return u;
	}

    public Result home() {
        return ok(home.render(getCurrentUser()));
    }

    public Result contact() {
        return ok(contact.render(getCurrentUser()));
    }

    public Result about() {
        return ok(about.render(getCurrentUser()));
    }


}