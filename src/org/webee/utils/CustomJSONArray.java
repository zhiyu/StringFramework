package org.webee.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class CustomJSONArray {
    private ArrayList myArrayList;
    public CustomJSONArray() {
        this.myArrayList = new ArrayList();
    }

    public CustomJSONArray(CustomJSONTokener x) throws Exception {
        this();
        if (x.nextClean() != '[') {
            throw x.syntaxError("A JSONArray text must start with '['");
        }
        if (x.nextClean() != ']') {
	        x.back();
	        for (;;) {
	            if (x.nextClean() == ',') {
	                x.back();
	                this.myArrayList.add(CustomJSONObject.NULL);
	            } else {
	                x.back();
	                this.myArrayList.add(x.nextValue());
	            }
	            switch (x.nextClean()) {
	            case ';':
	            case ',':
	                if (x.nextClean() == ']') {
	                    return;
	                }
	                x.back();
	                break;
	            case ']':
	            	return;
	            default:
	                throw x.syntaxError("Expected a ',' or ']'");
	            }
	        }
        }
    }

    public CustomJSONArray(String source) throws Exception {
        this(new CustomJSONTokener(source));
    }

    public CustomJSONArray(Collection collection) {
		this.myArrayList = new ArrayList();
		if (collection != null) {
			Iterator iter = collection.iterator();
			while (iter.hasNext()) {
                this.myArrayList.add(CustomJSONObject.wrap(iter.next()));  
			}
		}
    }

    public CustomJSONArray(Object array) throws Exception {
        this();
        if (array.getClass().isArray()) {
            int length = Array.getLength(array);
            for (int i = 0; i < length; i += 1) {
                this.put(CustomJSONObject.wrap(Array.get(array, i)));
            }
        } else {
            throw new Exception(
"JSONArray initial value should be a string or collection or array.");
        }
    }
    
    public Object get(int index) throws Exception {
        Object object = opt(index);
        if (object == null) {
            throw new Exception("JSONArray[" + index + "] not found.");
        }
        return object;
    }

    public boolean getBoolean(int index) throws Exception {
        Object object = get(index);
        if (object.equals(Boolean.FALSE) ||
                (object instanceof String &&
                ((String)object).equalsIgnoreCase("false"))) {
            return false;
        } else if (object.equals(Boolean.TRUE) ||
                (object instanceof String &&
                ((String)object).equalsIgnoreCase("true"))) {
            return true;
        }
        throw new Exception("JSONArray[" + index + "] is not a boolean.");
    }

    public double getDouble(int index) throws Exception {
        Object object = get(index);
        try {
            return object instanceof Number ?
                ((Number)object).doubleValue() :
                Double.parseDouble((String)object);
        } catch (Exception e) {
            throw new Exception("JSONArray[" + index +
                "] is not a number.");
        }
    }

    public int getInt(int index) throws Exception {
        Object object = get(index);
        try {
            return object instanceof Number ?
                ((Number)object).intValue() :
                Integer.parseInt((String)object);
        } catch (Exception e) {
            throw new Exception("JSONArray[" + index +
                "] is not a number.");
        }
    }

    public CustomJSONArray getJSONArray(int index) throws Exception {
        Object object = get(index);
        if (object instanceof CustomJSONArray) {
            return (CustomJSONArray)object;
        }
        throw new Exception("JSONArray[" + index +
                "] is not a JSONArray.");
    }

    public CustomJSONObject getJSONObject(int index) throws Exception {
        Object object = get(index);
        if (object instanceof CustomJSONObject) {
            return (CustomJSONObject)object;
        }
        throw new Exception("JSONArray[" + index +
            "] is not a JSONObject.");
    }

    public long getLong(int index) throws Exception {
        Object object = get(index);
        try {
            return object instanceof Number ?
                ((Number)object).longValue() :
                Long.parseLong((String)object);
        } catch (Exception e) {
            throw new Exception("JSONArray[" + index +
                "] is not a number.");
        }
    }

    public String getString(int index) throws Exception {
        Object object = get(index);
        return object == CustomJSONObject.NULL ? null : object.toString();
    }

    public boolean isNull(int index) {
        return CustomJSONObject.NULL.equals(opt(index));
    }

    public int length() {
        return this.myArrayList.size();
    }

    public Object opt(int index) {
        return (index < 0 || index >= length()) ?
            null : this.myArrayList.get(index);
    }

    public boolean optBoolean(int index)  {
        return optBoolean(index, false);
    }

    public boolean optBoolean(int index, boolean defaultValue)  {
        try {
            return getBoolean(index);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public double optDouble(int index) {
        return optDouble(index, Double.NaN);
    }

    public double optDouble(int index, double defaultValue) {
        try {
            return getDouble(index);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public int optInt(int index) {
        return optInt(index, 0);
    }

    public int optInt(int index, int defaultValue) {
        try {
            return getInt(index);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public CustomJSONArray optJSONArray(int index) {
        Object o = opt(index);
        return o instanceof CustomJSONArray ? (CustomJSONArray)o : null;
    }

    public CustomJSONObject optJSONObject(int index) {
        Object o = opt(index);
        return o instanceof CustomJSONObject ? (CustomJSONObject)o : null;
    }

    public long optLong(int index) {
        return optLong(index, 0);
    }

    public long optLong(int index, long defaultValue) {
        try {
            return getLong(index);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public String optString(int index) {
        return optString(index, "");
    }

    public String optString(int index, String defaultValue) {
        Object object = opt(index);
        return object != null ? object.toString() : defaultValue;
    }

    public CustomJSONArray put(boolean value) {
        put(value ? Boolean.TRUE : Boolean.FALSE);
        return this;
    }

    public CustomJSONArray put(Collection value) {
        put(new CustomJSONArray(value));
        return this;
    }

    public CustomJSONArray put(double value) throws Exception {
        Double d = new Double(value);
        CustomJSONObject.testValidity(d);
        put(d);
        return this;
    }

    public CustomJSONArray put(int value) {
        put(new Integer(value));
        return this;
    }

    public CustomJSONArray put(long value) {
        put(new Long(value));
        return this;
    }

    public CustomJSONArray put(Map value) {
        put(new CustomJSONObject(value));
        return this;
    }

    public CustomJSONArray put(Object value) {
        this.myArrayList.add(value);
        return this;
    }

    public CustomJSONArray put(int index, boolean value) throws Exception {
        put(index, value ? Boolean.TRUE : Boolean.FALSE);
        return this;
    }

    public CustomJSONArray put(int index, Collection value) throws Exception {
        put(index, new CustomJSONArray(value));
        return this;
    }

    public CustomJSONArray put(int index, double value) throws Exception {
        put(index, new Double(value));
        return this;
    }

    public CustomJSONArray put(int index, int value) throws Exception {
        put(index, new Integer(value));
        return this;
    }

    public CustomJSONArray put(int index, long value) throws Exception {
        put(index, new Long(value));
        return this;
    }

    public CustomJSONArray put(int index, Map value) throws Exception {
        put(index, new CustomJSONObject(value));
        return this;
    }

    public CustomJSONArray put(int index, Object value) throws Exception {
        CustomJSONObject.testValidity(value);
        if (index < 0) {
            throw new Exception("JSONArray[" + index + "] not found.");
        }
        if (index < length()) {
            this.myArrayList.set(index, value);
        } else {
            while (index != length()) {
                put(CustomJSONObject.NULL);
            }
            put(value);
        }
        return this;
    }
    
    public Object remove(int index) {
    	Object o = opt(index);
        this.myArrayList.remove(index);
        return o;
    }

    public CustomJSONObject toJSONObject(CustomJSONArray names) throws Exception {
        if (names == null || names.length() == 0 || length() == 0) {
            return null;
        }
        CustomJSONObject jo = new CustomJSONObject();
        for (int i = 0; i < names.length(); i += 1) {
            jo.put(names.getString(i), this.opt(i));
        }
        return jo;
    }
}