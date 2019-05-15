import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Coordinator {

    private Integer port;
    private Integer parts;
    private Set<String> options;
    private HashMap<Integer,Socket> participants = new HashMap<>();
    private int count =0;

    private class CoordinatorThread extends Thread{

        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        CoordinatorThread(Socket socket) throws IOException {

            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

        }

        public void run(){

            try {
                String message = in.readLine();

                if(message.contains("JOIN")){
                    System.out.println(message);
                    String[] protocol = message.split(" ");
                    participants.put(Integer.valueOf(protocol[1]),socket);

                }

                if(participants.size() == parts){

                    String votes = "VOTE_OPTIONS";

                    for (String opt : options){
                        votes = votes + " " + opt;
                    }

                    for (Socket s : participants.values()){
                        String details = "DETAILS";
                        out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()),true);

                        for (Map.Entry<Integer,Socket> p : participants.entrySet()){
                            if (!s.equals(p.getValue())){
                                details = details + " " + p.getKey();
                            }
                        }

                        out.println(details);
                        out.println(votes);
                    }

                }



            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


    public Coordinator(Integer port, Integer parts, Set<String> options) throws IOException {

        this.port = port;
        this.parts = parts;
        this.options = options;

        ServerSocket serverSocket;


            serverSocket = new ServerSocket(this.port);
            System.out.println("Starting Coordinator...");

            while (count<parts){
                Socket socket = serverSocket.accept();
                count++;
                new CoordinatorThread(socket).start();
            }



    }

    public static void main(String[] args) throws IOException {

        int port = Integer.parseInt(args[0]);
        int parts = Integer.parseInt(args[1]);
        Set<String> options = new HashSet<>();

        for (int i = 2; i < args.length; i++) {
            options.add(args[i]);
        }

        new Coordinator(port, parts,options);

    }

}
