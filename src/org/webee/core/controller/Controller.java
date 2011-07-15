package org.webee.core.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class Controller {
	public String path;
	public String redirectPath = null;
	public String viewPath = null;
	public String layout = "default";
	public String theme = "default";
	public Map<String, String> params;
	//public Session session = new Session();
	//public Request request = new Request();
	public HttpServletRequest request;
	public HttpServletResponse response;
	public HttpSession session;

	public void beforeFilter() {

	}

	public void redirect(String path) {
		this.redirectPath = path;
	}

	public void render(String path) {
		this.viewPath = path;
	}
}
