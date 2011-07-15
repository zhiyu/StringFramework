package org.webee.core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webee.core.controller.Controller;
import org.webee.utils.CustomJSONObject;


public class Dispatcher implements Filter {

	private FilterConfig config;
	private String defaultController = "default";
	private String defaultAction = "index";
	private String exclude = "";
	private String debug = "true";
	private String error404 = "404.jsp";
	private String error500 = "500.jsp";

	public void init(FilterConfig filterConfig) throws ServletException {
		this.config = filterConfig;
		try {
			CustomJSONObject js = new CustomJSONObject(config
					.getInitParameter("config"));
			if (js.getString("defaultController") != null)
				this.defaultController = js.getString("defaultController");
			if (js.getString("defaultAction") != null)
				this.defaultAction = js.getString("defaultAction");
			if (js.getString("exclude") != null)
				this.exclude = js.getString("exclude");
			if (js.getString("debug") != null)
				this.debug = js.getString("debug");
			if (js.get("errorPages") != null) {
				if (((CustomJSONObject) js.get("errorPages")).getString("404") != null)
					this.error404 = ((CustomJSONObject) js.get("errorPages"))
							.getString("404");
				if (((CustomJSONObject) js.get("errorPages")).getString("500") != null)
					this.error500 = ((CustomJSONObject) js.get("errorPages"))
							.getString("500");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		Map<String, String> params = this.getParameterMap(req);
		String contextPath = req.getContextPath();
		String requestURI = req.getRequestURI();

		if (contextPath.length() > 0) {
			requestURI = requestURI.substring(contextPath.length());
		}

		if (("".equals(requestURI) || "/".equals(requestURI))
				&& defaultAction != null) {
			if(contextPath.lastIndexOf("/")==(contextPath.length()-1)){
			    res.sendRedirect(contextPath + defaultController + "/"
					+ defaultAction);
			}else{
				res.sendRedirect(contextPath + "/" + defaultController + "/"
						+ defaultAction);
			}
		} else {
			if (isOpen(requestURI, exclude)) {
				chain.doFilter(request, response);
			} else {
				String[] paths = requestURI.split("/");
				String action = "";
				String controller = "";
				String controllerClass = "";
				String controllerPath = "";

				if (paths.length == 2) {
					if (requestURI.endsWith("/")) {
						controller = paths[1].replaceFirst(paths[1].charAt(0)
								+ "", (paths[1].charAt(0) + "").toUpperCase());
						action = "index";
					} else {
						controller = this.defaultController.replaceFirst(
								this.defaultController.charAt(0) + "",
								(this.defaultController.charAt(0) + "")
										.toUpperCase());
						action = paths[1];
					}
					controllerClass = "app.controller." + controller
							+ "Controller";
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
					controller = paths[cFlag].replaceFirst(paths[cFlag]
							.charAt(0)
							+ "", (paths[cFlag].charAt(0) + "").toUpperCase());
					controllerClass = "app.controller." + cp + controller
							+ "Controller";
					controllerPath = cp.toString().replaceAll("\\.", "\\/")
							+ controller.toLowerCase();

					if (requestURI.endsWith("/")) {
						action = "index";
					} else {
						action = paths[aFlag];
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

					// // set session attributes
					// Enumeration names = req.getSession()
					// .getAttributeNames();
					// while (names.hasMoreElements()) {
					// String name = (String) names.nextElement();
					// obj.session.setAttribute(name, req.getSession()
					// .getAttribute(name));
					// }
					//
					// // set request attributes
					// names = req.getAttributeNames();
					// while (names.hasMoreElements()) {
					// String name = (String) names.nextElement();
					// obj.request.setAttribute(name, req.getSession()
					// .getAttribute(name));
					// }

					// before filter
					Method m;
					try {
						m = c.getMethod("beforeFilter", new Class[] {});
						m.invoke(obj, new Object[] {});
					} catch (Exception e) {
						e.printStackTrace();
					}

					if (obj.redirectPath != null) {
						res.sendRedirect(obj.redirectPath);
						return;
					}

					// action
					m = c.getMethod(action, new Class[] {});
					m.invoke(obj, new Object[] {});

					// before render
					try {
						m = c.getMethod("beforeRender", new Class[] {});
						m.invoke(obj, new Object[] {});
					} catch (Exception e) {
						e.printStackTrace();
					}

					if (obj.redirectPath != null) {
						res.sendRedirect(obj.redirectPath);
						return;
					}
					// // set session attributes
					// Set<String> oNames = obj.session.getAttributeNames();
					// Iterator<String> itr = oNames.iterator();
					// while (itr.hasNext()) {
					// String name = (String) itr.next();
					// req.getSession().setAttribute(name,
					// obj.session.getAttribute(name));
					// }
					//
					// // set request attributes
					// oNames = obj.request.getAttributeNames();
					// itr = oNames.iterator();
					// while (itr.hasNext()) {
					// String name = (String) itr.next();
					// req.setAttribute(name, obj.request
					// .getAttribute(name));
					// }

					// render
					req.setAttribute("root", "/view");
					req.setAttribute("theme", obj.theme);
					req.setAttribute("layout", obj.layout);
					req.setAttribute("view", action);
					req.setAttribute("controller", controller);
					req.setAttribute("params", obj.params);

					String viewPath = ("/view/" + obj.theme + "/"
							+ controllerPath + "/" + action + ".jsp")
							.toLowerCase();
					if (obj.redirectPath != null) {
						res.sendRedirect(obj.redirectPath);
					} else {
						if (obj.viewPath != null) {
							if (obj.viewPath.startsWith("/")) {
								viewPath = ("/view/" + obj.theme + obj.viewPath + ".jsp")
										.toLowerCase();

							} else {
								viewPath = ("/view/" + obj.theme + "/"
										+ controllerPath + "/" + obj.viewPath + ".jsp")
										.toLowerCase();
							}
						}

						req.setAttribute("viewPath", viewPath);
						req.getRequestDispatcher(
								"/view/" + obj.theme + "/layout/" + obj.layout
										+ ".jsp").forward(request, response);
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
						req.getRequestDispatcher("/"+error404).forward(request,
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
						req.getRequestDispatcher("/"+error500).forward(request,
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
						req.getRequestDispatcher("/"+error500).forward(request,
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
						req.getRequestDispatcher("/"+error500).forward(request,
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
						req.getRequestDispatcher("/"+error404).forward(request,
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
						req.getRequestDispatcher("/"+error500).forward(request,
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
						req.getRequestDispatcher("/"+error500).forward(request,
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
						req.getRequestDispatcher("/"+error404).forward(request,
								response);
					}
				}
			}
		}
	}

	public boolean isOpen(String path, String exclude) {
		if (exclude != null && !"".equals(exclude)) {
			String[] paths = exclude.split(",");
			for (int i = 0; i < paths.length; i++) {
				if (path.endsWith(paths[i]))
					return true;
			}
		}
		return false;
	}

	public Map<String, String> getParameterMap(HttpServletRequest request) {
		Map<String, String> newParams = new HashMap<String, String>();
		Map params = request.getParameterMap();
		for (Object key : params.keySet()) {
			String name = (String) key;
			String[] values = (String[]) params.get(name);
			String value = values.length > 0 ? values[0].trim() : "";
			String contentType = request.getContentType();
			try {
				if (!(contentType != null
						&& contentType.toUpperCase().contains("URLENCODED") && contentType
						.toUpperCase().contains("CHARSET"))) {
					value = new String(value.getBytes("iso-8859-1"), "UTF-8");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			newParams.put(name, value);
		}
		return newParams;
	}

	public void destroy() {

	}

}
