package model;

/**
 * Created by Alexander on 25/11/2017.
 */
public interface RestMethod {
    String LOGIN = "/login";
    String LOGOUT = "/logout";
    String GET_USERS = "/users";
    String GET_USER = "/users/";
    String GET_MESSAGES = "/messages";
    String POST_MESSAGE = "/messages";
}
