package org.stringframework.dispatcher;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.stringframework.core.Controller;

public class FilterDispatcher implements Filter {

	private FilterConfig config = null;
	private Map<String, String> configMap = null;

	public void init(FilterConfig filterConfig) throws ServletException {
		this.config = filterConfig;
		this.loadConfiguration();
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		String requestURI = req.getRequestURI();

		if ("true".equals(this.get("string.debug"))) {
			this.loadConfiguration();
		}

		if (shouldDispatch(requestURI)) {
			doDispatch(request, response, chain);
		} else {
			chain.doFilter(request, response);
		}
	}

	private void doDispatch(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		Map<String, String> params = this.getParameterMap(req);
		String contextPath = req.getContextPath();
		String requestURI = req.getRequestURI();

		String applicationDefaultController = this
				.get("string.applicationDefaultController");
		String applicationDefaultAction = this
				.get("string.applicationDefaultAction");
		String defaultAction = this.get("string.defaultAction");
		String debug = this.get("string.debug");
		String errorPage404 = this.get("string.errorPage404");
		String errorPage500 = this.get("string.errorPage500");
		String suffix = this.get("string.suffix");

		if (!contextPath.trim().equals("")) {
			requestURI = requestURI.substring(contextPath.length());
		}

		String action = "";
		String controller = "";
		String controllerClass = "";
		String controllerPath = "";

		if (("".equals(requestURI) || "/".equals(requestURI))
				&& applicationDefaultAction != null) {
			controller = applicationDefaultController;
			action = applicationDefaultAction;
			controllerClass = this.get("string.controllerRoot") + "."
					+ controller + "Controller";
			controllerPath = controller.toLowerCase();
		} else {
			requestURI = requestURI
					.substring(0, requestURI.lastIndexOf(suffix));
			String[] paths = requestURI.split("/");

			if (paths.length == 2) {
				if (requestURI.endsWith("/")) {
					controller = paths[1].replaceFirst(paths[1].charAt(0) + "",
							(paths[1].charAt(0) + "").toUpperCase());
					action = defaultAction;
				} else {
					controller = applicationDefaultController.replaceFirst(
							applicationDefaultController.charAt(0) + "",
							(applicationDefaultController.charAt(0) + "")
									.toUpperCase());
					action = paths[1];
				}
				controllerClass = this.get("string.controllerRoot") + "."
						+ controller + "Controller";
				controllerPath = controller.toLowerCase();

			} else {
				StringBuffer cp = new StringBuffer();
				int cFlag = paths.length - 2;
				int aFlag = paths.length - 1;

				if (requestURI.endsWith("/")) {
					cFlag = paths.length - 1;
				}

				for (int i = 1; i < cFlag; i++) {
					cp.append(paths[i]).append(".");
				}
				controller = paths[cFlag].replaceFirst(paths[cFlag].charAt(0)
						+ "", (paths[cFlag].charAt(0) + "").toUpperCase());
				controllerClass = this.get("string.controllerRoot") + "." + cp
						+ controller + "Controller";
				controllerPath = cp.toString().replaceAll("\\.", "\\/")
						+ controller.toLowerCase();

				if (requestURI.endsWith("/")) {
					action = defaultAction;
				} else {
					action = paths[aFlag];
				}
			}
		}
		Class<?> c;
		Controller obj;

		try {
			c = Class.forName(controllerClass);
			obj = (Controller) c.newInstance();
			obj.params = params;
			obj.request = req;
			obj.response = res;
			obj.session = req.getSession();
			obj.path = requestURI;

			// before filter
			Method m;
			try {
				m = c.getMethod("beforeFilter", new Class[] {});
				m.invoke(obj, new Object[] {});
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (obj.redirect != null) {
				res.sendRedirect(obj.redirect);
				return;
			}

			// action
			m = c.getMethod(action, new Class[] {});
			m.invoke(obj, new Object[] {});

			// render
			req.setAttribute("theme", obj.theme);
			req.setAttribute("layout", obj.layout);
			req.setAttribute("title", obj.title==null?this.get("string.applicationName"):obj.title);
			req.setAttribute("action", action);
			req.setAttribute("controller", controller);
			req.setAttribute("params", obj.params);

			String view = ("/" + this.get("string.viewRoot") + "/" + obj.theme
					+ "/" + controllerPath + "/" + action + ".jsp")
					.toLowerCase();
			String layout = "/" + this.get("string.viewRoot") + "/" + obj.theme
					+ "/layout/" + obj.layout + ".jsp";
			if (obj.redirect != null) {
				res.sendRedirect(obj.redirect);
			} else {
				if (obj.view != null) {
					if (obj.view.startsWith("/")) {
						view = ("/" + this.get("string.viewRoot") + "/"
								+ obj.theme + obj.view + ".jsp").toLowerCase();
					} else {
						view = ("/" + this.get("string.viewRoot") + "/"
								+ obj.theme + "/" + controllerPath + "/"
								+ obj.view + ".jsp").toLowerCase();
					}
				}

				req.setAttribute("view", view);
				req.getRequestDispatcher(layout).forward(request, response);
			}
		} catch (ClassNotFoundException e) {
			if ("true".equals(debug)) {
				if (e.getCause() != null) {
					e.getCause().printStackTrace();
					res.getWriter().write(e.getCause().toString());
				} else {
					e.printStackTrace();
					res.getWriter().write(e.toString());
				}
				res.flushBuffer();
			} else {
				req.getRequestDispatcher("/" + errorPage404).forward(request,
						response);
			}
		} catch (InstantiationException e) {
			if ("true".equals(debug)) {
				if (e.getCause() != null) {
					e.getCause().printStackTrace();
					res.getWriter().write(e.getCause().toString());
				} else {
					e.printStackTrace();
					res.getWriter().write(e.toString());
				}
				res.flushBuffer();
			} else {
				req.getRequestDispatcher("/" + errorPage500).forward(request,
						response);
			}
		} catch (IllegalAccessException e) {
			if ("true".equals(debug)) {
				if (e.getCause() != null) {
					e.getCause().printStackTrace();
					res.getWriter().write(e.getCause().toString());
				} else {
					e.printStackTrace();
					res.getWriter().write(e.toString());
				}
				res.flushBuffer();
			} else {
				req.getRequestDispatcher("/" + errorPage500).forward(request,
						response);
			}
		} catch (SecurityException e) {
			if ("true".equals(debug)) {
				if (e.getCause() != null) {
					e.getCause().printStackTrace();
					res.getWriter().write(e.getCause().toString());
				} else {
					e.printStackTrace();
					res.getWriter().write(e.toString());
				}
				res.flushBuffer();
			} else {
				req.getRequestDispatcher("/" + errorPage500).forward(request,
						response);
			}
		} catch (NoSuchMethodException e) {
			if ("true".equals(debug)) {
				if (e.getCause() != null) {
					e.getCause().printStackTrace();
					res.getWriter().write(e.getCause().toString());
				} else {
					e.printStackTrace();
					res.getWriter().write(e.toString());
				}
				res.flushBuffer();
			} else {
				req.getRequestDispatcher("/" + errorPage404).forward(request,
						response);
			}
		} catch (IllegalArgumentException e) {
			if ("true".equals(debug)) {
				if (e.getCause() != null) {
					e.getCause().printStackTrace();
					res.getWriter().write(e.getCause().toString());
				} else {
					e.printStackTrace();
					res.getWriter().write(e.toString());
				}
				res.flushBuffer();
			} else {
				req.getRequestDispatcher("/" + errorPage404).forward(request,
						response);
			}
		} catch (InvocationTargetException e) {
			if ("true".equals(debug)) {
				if (e.getCause() != null) {
					e.getCause().printStackTrace();
					res.getWriter().write(e.getCause().toString());
				} else {
					e.printStackTrace();
					res.getWriter().write(e.toString());
				}
				res.flushBuffer();
			} else {
				req.getRequestDispatcher("/" + errorPage500).forward(request,
						response);
			}
		} catch (Exception e) {
			if ("true".equals(debug)) {
				if (e.getCause() != null) {
					e.getCause().printStackTrace();
					res.getWriter().write(e.getCause().toString());
				} else {
					e.printStackTrace();
					res.getWriter().write(e.toString());
				}
				res.flushBuffer();
			} else {
				req.getRequestDispatcher("/" + errorPage404).forward(request,
						response);
			}
		}
	}

	private Map<String, String> getParameterMap(HttpServletRequest request) {
		Map<String, String> newParams = new HashMap<String, String>();
		Map params = request.getParameterMap();
		for (Object key : params.keySet()) {
			String name = (String) key;
			String[] values = (String[]) params.get(name);
			String value = values.length > 0 ? values[0].trim() : "";
			try {
				if ("true".equals(this.get("string.uriEncodingEnabled"))) {
					value = new String(value.getBytes("iso-8859-1"), this
							.get("string.uriEncoding"));
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			newParams.put(name, value);
		}
		return newParams;
	}

	private boolean shouldDispatch(String requestURI) {
		String suffix = this.get("string.suffix");

		if ("".equals(requestURI) || "/".equals(requestURI)) {
			if ("true".equals(this
					.get("string.applicationDefaultControllerEnabled")))
				return true;
			else
				return false;
		} else {
			if ("".equals(suffix)) {
				if (requestURI.indexOf(".") == -1)
					return true;
			} else {
				if (requestURI.lastIndexOf(suffix) != -1
						&& (requestURI.length() == requestURI
								.lastIndexOf(suffix)
								+ suffix.length())) {
					return true;
				}
			}
		}

		return false;
	}

	private String get(String key) {
		return this.configMap.get(key);
	}

	private void loadConfiguration() {
		this.configMap = new HashMap<String, String>();
		configMap.put("string.applicationDefaultName", "");
		configMap.put("string.applicationDefaultControllerEnabled", "true");
		configMap.put("string.applicationDefaultController", "Default");
		configMap.put("string.applicationDefaultAction", "index");
		configMap.put("string.suffix", "");
		configMap.put("string.debug", "true");
		configMap.put("string.viewRoot", "view");
		configMap.put("string.controllerRoot", "app.controller");
		configMap.put("string.errorPage404", "404.jsp");
		configMap.put("string.errorPage500", "500.jsp");

		Properties properties = new Properties();
		try {
			String configFilePath = this.getClass().getResource("/").getPath()
					+ "string.properties";
			properties.load(new InputStreamReader(new FileInputStream(
					configFilePath), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Enumeration enu = properties.propertyNames();
		while (enu.hasMoreElements()) {
			String key = (String) enu.nextElement();
			if (properties.getProperty(key) != null)
				configMap.put(key, properties.getProperty(key));
		}
	}

	public void destroy() {

	}

}
