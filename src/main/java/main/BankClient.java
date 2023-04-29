package main;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BankClient extends Thread {
    private int id;

    public BankClient(int id) {
        this.id = id;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        List<BankClient> threads = new ArrayList<>();
        int threadCount = 10;
        for (int i = 0 ; i < threadCount; i++) {
            threads.add(new BankClient(i));
        }
        for (int i = 0 ; i < threadCount; i++) {

            threads.get(i).start();
        }
        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).join();
        }



    }

    public void run() {
        InetSocketAddress addr = new InetSocketAddress("192.168.43.72", 23 + id);

        //  selectable channel for stream-oriented connecting sockets
        SocketChannel socketChannel = null;
        try {
            socketChannel = SocketChannel.open(addr);
            System.out.println(String.format("Connecting to Server on port %d...", 23 + id));

            int n = 0;
            int sourceAccount = 0;
            int destinationAccount = 1;
            int amount = 0;
            int accountsSize = 120000;
            ByteBuffer allocate = ByteBuffer.allocate(100);
            int limit = 1000;
            ByteBufferArrayOutputStream byteArrayOutputStream = new ByteBufferArrayOutputStream(40);
            while (true) {
//            System.out.println("Writing transaction");
                amount = new Random().nextInt(75);
                byteArrayOutputStream.reset();
                DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                try {
                    dataOutputStream.writeInt(sourceAccount);
                    dataOutputStream.writeInt(destinationAccount);
                    dataOutputStream.writeLong(amount);

                    socketChannel.write(ByteBuffer.wrap(byteArrayOutputStream.toByteArray()));

                    allocate.rewind();
                    socketChannel.read(allocate);
                    n++;
                    sourceAccount = (sourceAccount + 1) % accountsSize;
                    destinationAccount = (destinationAccount + 1) % accountsSize;
                }
                catch (IOException io) {
                    System.out.println("broken connection");
                    break;
                }

            }

            ByteBufferArrayOutputStream byteArrayOutputStream2 = new ByteBufferArrayOutputStream(1024);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(byteArrayOutputStream2);
            try {
                outputStreamWriter.write(1);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            // close(): Closes this channel.
            // If the channel has already been closed then this method returns immediately.
            // Otherwise it marks the channel as closed and then invokes the implCloseChannel method in order to complete the close operation.
            socketChannel.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
