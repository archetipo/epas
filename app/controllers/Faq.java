package controllers;

import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class Faq extends Controller {

    public static void faq() {
        render();
    }

}
