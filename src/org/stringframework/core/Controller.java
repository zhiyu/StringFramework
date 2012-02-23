package org.stringframework.core;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class Controller {
	public String path;
	public String redirect = null;
	public String view = null;
	public String layout = "default";
	public String theme = "default";
	public String title = null;
	public Map<String, String> params;
	public HttpServletRequest request;
	public HttpServletResponse response;
	public HttpSession session;

	public void beforeFilter() {

	}

	public void redirect(String path) {
		this.redirect = path;
	}

	public void render(String path) {
		this.view = path;
	}
}
