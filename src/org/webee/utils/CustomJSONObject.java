package org.webee.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CustomJSONObject {

	private static final class Null {

		protected final Object clone() {
			return this;
		}

		public boolean equals(Object object) {
			return object == null || object == this;
		}

		public String toString() {
			return "null";
		}
	}

	private Map map;

	public static final Object NULL = new Null();

	public CustomJSONObject() {
		this.map = new HashMap();
	}

	public CustomJSONObject(CustomJSONObject jo, String[] names) {
		this();
		for (int i = 0; i < names.length; i += 1) {
			try {
				putOnce(names[i], jo.opt(names[i]));
			} catch (Exception ignore) {
			}
		}
	}

	public CustomJSONObject(CustomJSONTokener x) throws Exception {
		this();
		char c;
		String key;

		if (x.nextClean() != '{') {
			throw x.syntaxError("A JSONObject text must begin with '{'");
		}
		for (;;) {
			c = x.nextClean();
			switch (c) {
			case 0:
				throw x.syntaxError("A JSONObject text must end with '}'");
			case '}':
				return;
			default:
				x.back();
				key = x.nextValue().toString();
			}

			// The key is followed by ':'. We will also tolerate '=' or '=>'.

			c = x.nextClean();
			if (c == '=') {
				if (x.next() != '>') {
					x.back();
				}
			} else if (c != ':') {
				throw x.syntaxError("Expected a ':' after a key");
			}
			putOnce(key, x.nextValue());

			// Pairs are separated by ','. We will also tolerate ';'.

			switch (x.nextClean()) {
			case ';':
			case ',':
				if (x.nextClean() == '}') {
					return;
				}
				x.back();
				break;
			case '}':
				return;
			default:
				throw x.syntaxError("Expected a ',' or '}'");
			}
		}
	}

	public CustomJSONObject(Map map) {
		this.map = new HashMap();
		if (map != null) {
			Iterator i = map.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry e = (Map.Entry) i.next();
				Object value = e.getValue();
				if (value != null) {
					this.map.put(e.getKey(), wrap(value));
				}
			}
		}
	}

	public CustomJSONObject(Object bean) {
		this();
		populateMap(bean);
	}

	public CustomJSONObject(Object object, String names[]) {
		this();
		Class c = object.getClass();
		for (int i = 0; i < names.length; i += 1) {
			String name = names[i];
			try {
				putOpt(name, c.getField(name).get(object));
			} catch (Exception ignore) {
			}
		}
	}

	public CustomJSONObject(String source) throws Exception {
		this(new CustomJSONTokener(source));
	}

	public static String doubleToString(double d) {
		if (Double.isInfinite(d) || Double.isNaN(d)) {
			return "null";
		}

		// Shave off trailing zeros and decimal point, if possible.

		String string = Double.toString(d);
		if (string.indexOf('.') > 0 && string.indexOf('e') < 0
				&& string.indexOf('E') < 0) {
			while (string.endsWith("0")) {
				string = string.substring(0, string.length() - 1);
			}
			if (string.endsWith(".")) {
				string = string.substring(0, string.length() - 1);
			}
		}
		return string;
	}

	public Object get(String key) throws Exception {
		if (key == null) {
			throw new Exception("Null key.");
		}
		Object object = opt(key);
		if (object == null) {
			throw new Exception("JSONObject[" + quote(key) + "] not found.");
		}
		return object;
	}

	public boolean getBoolean(String key) throws Exception {
		Object object = get(key);
		if (object.equals(Boolean.FALSE)
				|| (object instanceof String && ((String) object)
						.equalsIgnoreCase("false"))) {
			return false;
		} else if (object.equals(Boolean.TRUE)
				|| (object instanceof String && ((String) object)
						.equalsIgnoreCase("true"))) {
			return true;
		}
		throw new Exception("JSONObject[" + quote(key) + "] is not a Boolean.");
	}

	public double getDouble(String key) throws Exception {
		Object object = get(key);
		try {
			return object instanceof Number ? ((Number) object).doubleValue()
					: Double.parseDouble((String) object);
		} catch (Exception e) {
			throw new Exception("JSONObject[" + quote(key)
					+ "] is not a number.");
		}
	}

	public int getInt(String key) throws Exception {
		Object object = get(key);
		try {
			return object instanceof Number ? ((Number) object).intValue()
					: Integer.parseInt((String) object);
		} catch (Exception e) {
			throw new Exception("JSONObject[" + quote(key) + "] is not an int.");
		}
	}

	public CustomJSONObject getJSONObject(String key) throws Exception {
		Object object = get(key);
		if (object instanceof CustomJSONObject) {
			return (CustomJSONObject) object;
		}
		throw new Exception("JSONObject[" + quote(key)
				+ "] is not a JSONObject.");
	}

	public long getLong(String key) throws Exception {
		Object object = get(key);
		try {
			return object instanceof Number ? ((Number) object).longValue()
					: Long.parseLong((String) object);
		} catch (Exception e) {
			throw new Exception("JSONObject[" + quote(key) + "] is not a long.");
		}
	}

	public static String[] getNames(CustomJSONObject jo) {
		int length = jo.length();
		if (length == 0) {
			return null;
		}
		Iterator iterator = jo.keys();
		String[] names = new String[length];
		int i = 0;
		while (iterator.hasNext()) {
			names[i] = (String) iterator.next();
			i += 1;
		}
		return names;
	}

	public static String[] getNames(Object object) {
		if (object == null) {
			return null;
		}
		Class klass = object.getClass();
		Field[] fields = klass.getFields();
		int length = fields.length;
		if (length == 0) {
			return null;
		}
		String[] names = new String[length];
		for (int i = 0; i < length; i += 1) {
			names[i] = fields[i].getName();
		}
		return names;
	}

	public String getString(String key) throws Exception {
		Object object = get(key);
		return object == NULL ? null : object.toString();
	}

	public boolean has(String key) {
		return this.map.containsKey(key);
	}

	public CustomJSONObject increment(String key) throws Exception {
		Object value = opt(key);
		if (value == null) {
			put(key, 1);
		} else if (value instanceof Integer) {
			put(key, ((Integer) value).intValue() + 1);
		} else if (value instanceof Long) {
			put(key, ((Long) value).longValue() + 1);
		} else if (value instanceof Double) {
			put(key, ((Double) value).doubleValue() + 1);
		} else if (value instanceof Float) {
			put(key, ((Float) value).floatValue() + 1);
		} else {
			throw new Exception("Unable to increment [" + quote(key) + "].");
		}
		return this;
	}

	public boolean isNull(String key) {
		return CustomJSONObject.NULL.equals(opt(key));
	}

	public Iterator keys() {
		return this.map.keySet().iterator();
	}

	public int length() {
		return this.map.size();
	}

	public static String numberToString(Number number) throws Exception {
		if (number == null) {
			throw new Exception("Null pointer");
		}
		testValidity(number);

		// Shave off trailing zeros and decimal point, if possible.

		String string = number.toString();
		if (string.indexOf('.') > 0 && string.indexOf('e') < 0
				&& string.indexOf('E') < 0) {
			while (string.endsWith("0")) {
				string = string.substring(0, string.length() - 1);
			}
			if (string.endsWith(".")) {
				string = string.substring(0, string.length() - 1);
			}
		}
		return string;
	}

	public Object opt(String key) {
		return key == null ? null : this.map.get(key);
	}

	public boolean optBoolean(String key) {
		return optBoolean(key, false);
	}

	public boolean optBoolean(String key, boolean defaultValue) {
		try {
			return getBoolean(key);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public double optDouble(String key) {
		return optDouble(key, Double.NaN);
	}

	public double optDouble(String key, double defaultValue) {
		try {
			return getDouble(key);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public int optInt(String key) {
		return optInt(key, 0);
	}

	public int optInt(String key, int defaultValue) {
		try {
			return getInt(key);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public CustomJSONObject optJSONObject(String key) {
		Object object = opt(key);
		return object instanceof CustomJSONObject ? (CustomJSONObject) object : null;
	}

	public long optLong(String key) {
		return optLong(key, 0);
	}

	public long optLong(String key, long defaultValue) {
		try {
			return getLong(key);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public String optString(String key) {
		return optString(key, "");
	}

	public String optString(String key, String defaultValue) {
		Object object = opt(key);
		return NULL.equals(object) ? defaultValue : object.toString();
	}

	private void populateMap(Object bean) {
		Class klass = bean.getClass();
		boolean includeSuperClass = klass.getClassLoader() != null;

		Method[] methods = (includeSuperClass) ? klass.getMethods() : klass
				.getDeclaredMethods();
		for (int i = 0; i < methods.length; i += 1) {
			try {
				Method method = methods[i];
				if (Modifier.isPublic(method.getModifiers())) {
					String name = method.getName();
					String key = "";
					if (name.startsWith("get")) {
						if (name.equals("getClass")
								|| name.equals("getDeclaringClass")) {
							key = "";
						} else {
							key = name.substring(3);
						}
					} else if (name.startsWith("is")) {
						key = name.substring(2);
					}
					if (key.length() > 0
							&& Character.isUpperCase(key.charAt(0))
							&& method.getParameterTypes().length == 0) {
						if (key.length() == 1) {
							key = key.toLowerCase();
						} else if (!Character.isUpperCase(key.charAt(1))) {
							key = key.substring(0, 1).toLowerCase()
									+ key.substring(1);
						}

						Object result = method.invoke(bean, (Object[]) null);
						if (result != null) {
							map.put(key, wrap(result));
						}
					}
				}
			} catch (Exception ignore) {
			}
		}
	}
	
	public CustomJSONObject put(String key, boolean value) throws Exception {
		put(key, value ? Boolean.TRUE : Boolean.FALSE);
		return this;
	}
	
	public CustomJSONObject put(String key, double value) throws Exception {
		put(key, new Double(value));
		return this;
	}

	public CustomJSONObject put(String key, int value) throws Exception {
		put(key, new Integer(value));
		return this;
	}

	public CustomJSONObject put(String key, long value) throws Exception {
		put(key, new Long(value));
		return this;
	}

	public CustomJSONObject put(String key, Map value) throws Exception {
		put(key, new CustomJSONObject(value));
		return this;
	}

	public CustomJSONObject put(String key, Object value) throws Exception {
		if (key == null) {
			throw new Exception("Null key.");
		}
		if (value != null) {
			testValidity(value);
			this.map.put(key, value);
		} else {
			remove(key);
		}
		return this;
	}

	public CustomJSONObject putOnce(String key, Object value) throws Exception {
		if (key != null && value != null) {
			if (opt(key) != null) {
				throw new Exception("Duplicate key \"" + key + "\"");
			}
			put(key, value);
		}
		return this;
	}

	public CustomJSONObject putOpt(String key, Object value) throws Exception {
		if (key != null && value != null) {
			put(key, value);
		}
		return this;
	}

	public static String quote(String string) {
		if (string == null || string.length() == 0) {
			return "\"\"";
		}

		char b;
		char c = 0;
		String hhhh;
		int i;
		int len = string.length();
		StringBuffer sb = new StringBuffer(len + 4);

		sb.append('"');
		for (i = 0; i < len; i += 1) {
			b = c;
			c = string.charAt(i);
			switch (c) {
			case '\\':
			case '"':
				sb.append('\\');
				sb.append(c);
				break;
			case '/':
				if (b == '<') {
					sb.append('\\');
				}
				sb.append(c);
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\r':
				sb.append("\\r");
				break;
			default:
				if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
						|| (c >= '\u2000' && c < '\u2100')) {
					hhhh = "000" + Integer.toHexString(c);
					sb.append("\\u" + hhhh.substring(hhhh.length() - 4));
				} else {
					sb.append(c);
				}
			}
		}
		sb.append('"');
		return sb.toString();
	}

	/**
	 * Remove a name and its value, if present.
	 * 
	 * @param key
	 *            The name to be removed.
	 * @return The value that was associated with the name, or null if there was
	 *         no value.
	 */
	public Object remove(String key) {
		return this.map.remove(key);
	}

	/**
	 * Try to convert a string into a number, boolean, or null. If the string
	 * can't be converted, return the string.
	 * 
	 * @param string
	 *            A String.
	 * @return A simple JSON value.
	 */
	public static Object stringToValue(String string) {
		if (string.equals("")) {
			return string;
		}
		if (string.equalsIgnoreCase("true")) {
			return Boolean.TRUE;
		}
		if (string.equalsIgnoreCase("false")) {
			return Boolean.FALSE;
		}
		if (string.equalsIgnoreCase("null")) {
			return CustomJSONObject.NULL;
		}

		/*
		 * If it might be a number, try converting it. We support the
		 * non-standard 0x- convention. If a number cannot be produced, then the
		 * value will just be a string. Note that the 0x-, plus, and implied
		 * string conventions are non-standard. A JSON parser may accept
		 * non-JSON forms as long as it accepts all correct JSON forms.
		 */

		char b = string.charAt(0);
		if ((b >= '0' && b <= '9') || b == '.' || b == '-' || b == '+') {
			if (b == '0' && string.length() > 2
					&& (string.charAt(1) == 'x' || string.charAt(1) == 'X')) {
				try {
					return new Integer(Integer
							.parseInt(string.substring(2), 16));
				} catch (Exception ignore) {
				}
			}
			try {
				if (string.indexOf('.') > -1 || string.indexOf('e') > -1
						|| string.indexOf('E') > -1) {
					return Double.valueOf(string);
				} else {
					Long myLong = new Long(string);
					if (myLong.longValue() == myLong.intValue()) {
						return new Integer(myLong.intValue());
					} else {
						return myLong;
					}
				}
			} catch (Exception ignore) {
			}
		}
		return string;
	}

	/**
	 * Throw an exception if the object is a NaN or infinite number.
	 * 
	 * @param o
	 *            The object to test.
	 * @throws Exception
	 *             If o is a non-finite number.
	 */
	public static void testValidity(Object o) throws Exception {
		if (o != null) {
			if (o instanceof Double) {
				if (((Double) o).isInfinite() || ((Double) o).isNaN()) {
					throw new Exception(
							"JSON does not allow non-finite numbers.");
				}
			} else if (o instanceof Float) {
				if (((Float) o).isInfinite() || ((Float) o).isNaN()) {
					throw new Exception(
							"JSON does not allow non-finite numbers.");
				}
			}
		}
	}

	public static Object wrap(Object object) {
		try {
			if (object == null) {
				return NULL;
			}
			if (object instanceof CustomJSONObject || object instanceof CustomJSONArray
					|| NULL.equals(object) || object instanceof Byte
					|| object instanceof Character || object instanceof Short
					|| object instanceof Integer || object instanceof Long
					|| object instanceof Boolean || object instanceof Float
					|| object instanceof Double || object instanceof String) {
				return object;
			}

			if (object instanceof Collection) {
				return new CustomJSONArray((Collection) object);
			}
			if (object.getClass().isArray()) {
				return new CustomJSONArray(object);
			}
			if (object instanceof Map) {
				return new CustomJSONObject((Map) object);
			}
			Package objectPackage = object.getClass().getPackage();
			String objectPackageName = (objectPackage != null ? objectPackage
					.getName() : "");
			if (objectPackageName.startsWith("java.")
					|| objectPackageName.startsWith("javax.")
					|| object.getClass().getClassLoader() == null) {
				return object.toString();
			}
			return new CustomJSONObject(object);
		} catch (Exception exception) {
			return null;
		}
	}
}