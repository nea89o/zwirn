package moe.nea.zwirn;

import java.util.Stack;

public class GoodStringReader {
    final String source;
    Stack<Integer> stack = new Stack<>();
    int index = 0;

    public GoodStringReader(String source) {
        this.source = source;
    }

    public void push() {
        stack.push(index);
    }

    public void reset() {
        index = stack.pop();
    }

    public void discard() {
        stack.pop();
    }

    public char nextChar() {
        return source.charAt(index++);
    }

    public String readUntil(char... cs) {
        int minI = -1;
        for (char c : cs) {
            int i = source.indexOf(c, index);
            if (i < 0) continue;
            minI = minI < 0 ? i : Math.min(minI, i);
        }
        if (minI < 0) return null;
        int startIndex = index;
        index = minI + 1;
        return source.substring(startIndex, index - 1);
    }

    public char peekChar() {
        return source.charAt(index);
    }
}
