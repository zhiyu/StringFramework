package org.stringframework.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Request {
	Map<String, Object> attributes = new HashMap<String, Object>();

	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}

	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	public Set<String> getAttributeNames() {
		return attributes.keySet();
	}

}