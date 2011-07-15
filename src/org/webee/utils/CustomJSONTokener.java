package org.webee.utils;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

public class CustomJSONTokener {

    private int 	character;
	private boolean eof;
    private int 	index;
    private int 	line;
    private char 	previous;
    private Reader 	reader;
    private boolean usePrevious;

    public CustomJSONTokener(Reader reader) {
        this.reader = reader.markSupported() ? 
        		reader : new BufferedReader(reader);
        this.eof = false;
        this.usePrevious = false;
        this.previous = 0;
        this.index = 0;
        this.character = 1;
        this.line = 1;
    }
    
    public CustomJSONTokener(String s) {
        this(new StringReader(s));
    }

    public void back() throws Exception {
        if (usePrevious || index <= 0) {
            throw new Exception("Stepping back two steps is not supported");
        }
        this.index -= 1;
        this.character -= 1;
        this.usePrevious = true;
        this.eof = false;
    }


    public static int dehexchar(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return c - ('A' - 10);
        }
        if (c >= 'a' && c <= 'f') {
            return c - ('a' - 10);
        }
        return -1;
    }
    
    public boolean end() {
    	return eof && !usePrevious;    	
    }

    public boolean more() throws Exception {
        next();
        if (end()) {
            return false;
        } 
        back();
        return true;
    }

    public char next() throws Exception {
        int c;
        if (this.usePrevious) {
        	this.usePrevious = false;
            c = this.previous;
        } else {
	        try {
	            c = this.reader.read();
	        } catch (IOException exception) {
	            throw new Exception(exception);
	        }
	
	        if (c <= 0) { // End of stream
	        	this.eof = true;
	        	c = 0;
	        } 
        }
    	this.index += 1;
    	if (this.previous == '\r') {
    		this.line += 1;
    		this.character = c == '\n' ? 0 : 1;
    	} else if (c == '\n') {
    		this.line += 1;
    		this.character = 0;
    	} else {
    		this.character += 1;
    	}
    	this.previous = (char) c;
        return this.previous;
    }

    public char next(char c) throws Exception {
        char n = next();
        if (n != c) {
            throw syntaxError("Expected '" + c + "' and instead saw '" +
                    n + "'");
        }
        return n;
    }

     public String next(int n) throws Exception {
         if (n == 0) {
             return "";
         }

         char[] chars = new char[n];
         int pos = 0;

         while (pos < n) {
             chars[pos] = next();
             if (end()) {
                 throw syntaxError("Substring bounds error");                 
             }
             pos += 1;
         }
         return new String(chars);
     }

    public char nextClean() throws Exception {
        for (;;) {
            char c = next();
            if (c == 0 || c > ' ') {
                return c;
            }
        }
    }

    public String nextString(char quote) throws Exception {
        char c;
        StringBuffer sb = new StringBuffer();
        for (;;) {
            c = next();
            switch (c) {
            case 0:
            case '\n':
            case '\r':
                throw syntaxError("Unterminated string");
            case '\\':
                c = next();
                switch (c) {
                case 'b':
                    sb.append('\b');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 'u':
                    sb.append((char)Integer.parseInt(next(4), 16));
                    break;
                case '"':
                case '\'':
                case '\\':
                case '/':
                	sb.append(c);
                	break;
                default:
                    throw syntaxError("Illegal escape.");
                }
                break;
            default:
                if (c == quote) {
                    return sb.toString();
                }
                sb.append(c);
            }
        }
    }

    public String nextTo(char delimiter) throws Exception {
        StringBuffer sb = new StringBuffer();
        for (;;) {
            char c = next();
            if (c == delimiter || c == 0 || c == '\n' || c == '\r') {
                if (c != 0) {
                    back();
                }
                return sb.toString().trim();
            }
            sb.append(c);
        }
    }

    public String nextTo(String delimiters) throws Exception {
        char c;
        StringBuffer sb = new StringBuffer();
        for (;;) {
            c = next();
            if (delimiters.indexOf(c) >= 0 || c == 0 ||
                    c == '\n' || c == '\r') {
                if (c != 0) {
                    back();
                }
                return sb.toString().trim();
            }
            sb.append(c);
        }
    }

    public Object nextValue() throws Exception {
        char c = nextClean();
        String string;

        switch (c) {
            case '"':
            case '\'':
                return nextString(c);
            case '{':
                back();
                return new CustomJSONObject(this);
            case '[':
                back();
                return new CustomJSONArray(this);
        }

        StringBuffer sb = new StringBuffer();
        while (c >= ' ' && ",:]}/\\\"[{;=#".indexOf(c) < 0) {
            sb.append(c);
            c = next();
        }
        back();

        string = sb.toString().trim();
        if (string.equals("")) {
            throw syntaxError("Missing value");
        }
        return CustomJSONObject.stringToValue(string);
    }

    public char skipTo(char to) throws Exception {
        char c;
        try {
            int startIndex = this.index;
            int startCharacter = this.character;
            int startLine = this.line;
            reader.mark(Integer.MAX_VALUE);
            do {
                c = next();
                if (c == 0) {
                    reader.reset();
                    this.index = startIndex;
                    this.character = startCharacter;
                    this.line = startLine;
                    return c;
                }
            } while (c != to);
        } catch (IOException exc) {
            throw new Exception(exc);
        }

        back();
        return c;
    }
    
    public Exception syntaxError(String message) {
        return new Exception(message + toString());
    }

 
}