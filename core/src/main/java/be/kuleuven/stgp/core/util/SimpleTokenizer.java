package be.kuleuven.stgp.core.util;

import java.util.*;

public class SimpleTokenizer {

    public final String str;
    public final char[] separators;

    private int start, end = -1;

    public SimpleTokenizer(String str) {
        this.str = str;
        this.separators = new char[]{ ' ', '\t', '\n' };
    }

    public SimpleTokenizer(String str, String separators) {
        this.str = str;
        this.separators = separators.toCharArray();
    }

    public boolean hasToken() {
        start = end + 1;
        while (start < str.length() && isSeparator(str.charAt(start))) start++;
        if (start < str.length()) {
            end = start - 1;
            return true;
        }
        return false;
    }

    public double nextDouble() {
        while (start < str.length()) {
            try {
                String token = nextToken().trim();
                return Double.parseDouble(token);
            }
            catch (Exception ignored) {}
        }
        throw new NoSuchElementException();
    }

    public int nextInt() {
        while (start < str.length()) {
            try {
                String token = nextToken().trim();
                return Integer.parseInt(token);
            }
            catch (Exception ignored) {}
        }
        throw new NoSuchElementException();
    }

    public String nextToken() {
        skipToken();
        return str.substring(start, end);
    }

    public void skipToken() {
        start = end + 1;
        while (start < str.length() && isSeparator(str.charAt(start))) start++;
        end = start + 1;
        while (end < str.length() && !isSeparator(str.charAt(end))) end++;
    }

    private boolean isSeparator(char c) {
        for (char separator : separators)
            if (c == separator)
                return true;
        return false;
    }
}
