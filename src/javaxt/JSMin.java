package javaxt;
import java.io.*;

//******************************************************************************
//**  JSMin
//******************************************************************************
/**
 *   Used to minify javascript by removing comments and unnecessary whitespaces
 *   from JavaScript files. Adapted from Douglas Crockford's JSMin C code:
 *   https://github.com/douglascrockford/JSMin
 *
 ******************************************************************************/

public class JSMin {

    private static final int EOF = -1;
    private static int theA;
    private static int theB;
    private static int theLookahead = EOF;
    private static int theX = EOF;
    private static int theY = EOF;

    private InputStream stdin;
    private ByteArrayOutputStream stdout;

    public JSMin(String src) throws Exception {
        stdin = new ByteArrayInputStream(src.getBytes("UTF-8"));
        stdout = new ByteArrayOutputStream();
        jsmin();
        stdout.close();
    }

    public String toString() {
        try{
            return stdout.toString("UTF-8");
        }
        catch(Exception e){
            return null;
        }
    }


    private int get() throws Exception {
        int c = theLookahead;
        theLookahead = EOF;
        if (c == EOF) {
            c = getc(stdin);
        }
        if (c >= ' ' || c == '\n' || c == EOF) {
            return c;
        }
        if (c == '\r') {
            return '\n';
        }
        return ' ';
    }


    private int peek() throws Exception {
        theLookahead = get();
        return theLookahead;
    }

    private int next() throws Exception {
        int c = get();
        if  (c == '/') {
            switch (peek()) {
            case '/':
                for (;;) {
                    c = get();
                    if (c <= '\n') {
                        break;
                    }
                }
                break;
            case '*':
                get();
                while (c != ' ') {
                    switch (get()) {
                    case '*':
                        if (peek() == '/') {
                            get();
                            c = ' ';
                        }
                        break;
                    case EOF:
                        error("Unterminated comment.");
                    }
                }
                break;
            }
        }
        theY = theX;
        theX = c;
        return c;
    }

    private void action(int d) throws Exception {
        switch (d) {
        case 1:
            putc(theA, stdout);
            if (
                (theY == '\n' || theY == ' ') &&
                (theA == '+' || theA == '-' || theA == '*' || theA == '/') &&
                (theB == '+' || theB == '-' || theB == '*' || theB == '/')
            ) {
                putc(theY, stdout);
            }
        case 2:
            theA = theB;
            if (theA == '\'' || theA == '"' || theA == '`') {
                for (;;) {
                    putc(theA, stdout);
                    theA = get();
                    if (theA == theB) {
                        break;
                    }
                    if (theA == '\\') {
                        putc(theA, stdout);
                        theA = get();
                    }
                    if (theA == EOF) {
                        error("Unterminated string literal.");
                    }
                }
            }
        case 3:
            theB = next();
            if (theB == '/' && (
                theA == '(' || theA == ',' || theA == '=' || theA == ':' ||
                theA == '[' || theA == '!' || theA == '&' || theA == '|' ||
                theA == '?' || theA == '+' || theA == '-' || theA == '~' ||
                theA == '*' || theA == '/' || theA == '{' || theA == '\n'
            )) {
                putc(theA, stdout);
                if (theA == '/' || theA == '*') {
                    putc(' ', stdout);
                }
                putc(theB, stdout);
                for (;;) {
                    theA = get();
                    if (theA == '[') {
                        for (;;) {
                            putc(theA, stdout);
                            theA = get();
                            if (theA == ']') {
                                break;
                            }
                            if (theA == '\\') {
                                putc(theA, stdout);
                                theA = get();
                            }
                            if (theA == EOF) {
                                error("Unterminated set in Regular Expression literal.");
                            }
                        }
                    } else if (theA == '/') {
                        switch (peek()) {
                        case '/':
                        case '*':
                            error("Unterminated set in Regular Expression literal.");
                        }
                        break;
                    } else if (theA =='\\') {
                        putc(theA, stdout);
                        theA = get();
                    }
                    if (theA == EOF) {
                        error("Unterminated Regular Expression literal.");
                    }
                    putc(theA, stdout);
                }
                theB = next();
            }
        }
    }


    private void jsmin() throws Exception {

        if (peek() == 0xEF) {
            get();
            get();
            get();
        }

        theA = '\n';
        action(3);
        while (theA != EOF) {
            switch (theA) {
            case ' ':
                action(isAlphanum(theB) ? 1 : 2);
                break;
            case '\n':
                switch (theB) {
                case '{':
                case '[':
                case '(':
                case '+':
                case '-':
                case '!':
                case '~':
                    action(1);
                    break;
                case ' ':
                    action(3);
                    break;
                default:
                    action(isAlphanum(theB) ? 1 : 2);
                }
                break;
            default:
                switch (theB) {
                case ' ':
                    action(isAlphanum(theA) ? 1 : 3);
                    break;
                case '\n':
                    switch (theA) {
                    case '}':
                    case ']':
                    case ')':
                    case '+':
                    case '-':
                    case '"':
                    case '\'':
                    case '`':
                        action(1);
                        break;
                    default:
                        action(isAlphanum(theA) ? 1 : 3);
                    }
                    break;
                default:
                    action(1);
                    break;
                }
            }
        }
    }


    private static int getc(InputStream is) throws Exception {
        return is.read();
    }

    private static void putc(int c, OutputStream out) throws Exception {
        out.write(c);
    }


    private static void error(String err) throws Exception{
        throw new Exception(err);
    }

    private static boolean isAlphanum(int c) {
        return c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c == '_' || c == '$' || c == '\\' || c > 126 ? true : false;
    }
}