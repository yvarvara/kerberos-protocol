package com.company;

import java.io.*;

public class Logger {
    private static final String filename = "log.txt";
    private static RandomAccessFile file;

    static  {
        try {
            file = new RandomAccessFile(filename, "rw");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(String string) {
        try {
            file.seek(file.length());
            file.writeBytes(string + '\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}