package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Scanner;

public class MultiplexingThread extends Thread {

    public static void main(String[] args) throws FileNotFoundException, URISyntaxException {
        MultiplexingThread multiplexingThread = new MultiplexingThread();
        multiplexingThread.load("statemachine.pipeline");

    }

    public void load(String name) throws FileNotFoundException, URISyntaxException {
        URL res = getClass().getClassLoader().getResource(name);
        File file = Paths.get(res.toURI()).toFile();
        Scanner reader = new Scanner(file);

        StringBuilder programString = new StringBuilder();
        while (reader.hasNextLine()) {
            programString.append("\n").append(reader.nextLine());
        }

        String programString1 = programString.toString();
        System.out.println(programString1);
        MultiplexedAST ast = new MultiplexingProgramParser(programString1).parse();
    }
}
