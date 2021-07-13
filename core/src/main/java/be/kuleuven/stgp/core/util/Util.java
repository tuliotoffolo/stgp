package be.kuleuven.stgp.core.util;

import java.io.*;

public class Util {

    public static void safePrintf(PrintStream output, String format, Object... args) {
        if (output != null) {
            output.printf(format, args);
        }
    }

    public static void safePrintln(PrintStream output, String string) {
        if (output != null) {
            output.println(string);
        }
    }
}
