import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Coordinator{

    protected int port,parts,participantsCount=0;
    protected Set<String> opts;
    protected ServerSocket server;
    protected ExecutorService pool;

    public Coordinator(int port, int parts, Set<String> opts) throws IOException {
        this.port = port;
        this.parts = parts;
        this.opts = opts;

        server = new ServerSocket(port);
        pool = Executors.newFixedThreadPool(parts);

        while (true){
            System.out.println("Start server...");

            while (true){
                Socket socket = server.accept();
                participantsCount++;
                pool.execute(new CoordinatorThread(socket));
            }
        }
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(args[0]);
        int parts = Integer.parseInt(args[1]);
        Set<String> opts = new HashSet<>();

        for (int i = 2; i<args.length; i++){
            opts.add(args[i]);
        }

        new Coordinator(port,parts,opts);
    }


    private class CoordinatorThread extends Thread{

        Socket socket;
        PrintWriter out;
        BufferedReader in;

        public CoordinatorThread(Socket socket) throws IOException {
            this.socket = socket;

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        public void run(){
            try {
                String message = in.readLine();
                String[] protocol = message.split(" ");
                String type = protocol[0];
                System.out.println(type);

                switch (type){
                    case "JOIN":
                        System.out.println("Participant "+ protocol[1] +" has joined");
                        break;
                    case "DETAILS":
                        for (int i=1; i<protocol.length; i++){
                            System.out.println("Sent details for participant "+protocol[i]);
                        }
                        break;
                    case "VOTE_OPTIONS":
                        System.out.print("Vote options: ");
                        for (int i=1; i<protocol.length; i++){
                            System.out.print(protocol[i]+" ");
                        }
                        break;
                    case "OUTCOME":
                        System.out.print("Outcome: "+protocol[1]+" from participants: ");
                        for (int i=2; i<protocol.length;i++){
                            System.out.print(protocol[i]+" ");
                        }
                        break;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
