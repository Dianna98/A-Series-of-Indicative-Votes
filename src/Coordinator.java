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
    private int outputCount=0;
    private HashMap<String,Set<Integer>> outcomes = new HashMap<>();
    private Set<Integer> failed = new HashSet<>();
    private boolean stop = false;
    private ServerSocket serverSocket;
    private boolean check = true;
    private String removed;


    private class CoordinatorThread extends Thread{

        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        CoordinatorThread(Socket socket) {

            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            } catch (IOException e) {
            }
        }

        public void run(){
            boolean send = true;
            String message;

            try {
                while ((message = in.readLine())!=null) {

                    if (message.contains("JOIN")) {
                        System.out.println(message);
                        String[] protocol = message.split(" ");
                        participants.put(Integer.valueOf(protocol[1]), socket);

                    }

                    if ((participants.size() == parts) && send) {

                        send = false;

                        String votes = "VOTE_OPTIONS";

                        for (String opt : options) {
                            votes = votes + " " + opt;
                        }

                        for (Socket s : participants.values()) {
                            String details = "DETAILS";
                            out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true);

                            for (Map.Entry<Integer, Socket> p : participants.entrySet()) {
                                if (!s.equals(p.getValue())) {
                                    details = details + " " + p.getKey();
                                }
                            }

                            out.println(details);
                            out.println(votes);
                        }

                    }

                    if (message.contains("OUTCOME")) {
                        outputCount++;
                        String[] outcome = message.split(" ");

                        if (!message.contains("none")) {
                            System.out.println(message);
                            Set<Integer> ports = new HashSet<>();
                            if (outcome[1].contains("null")) {
                                removed = outcome[2];
                                for (int i = 3; i < outcome.length; i++) {
                                    ports.add(Integer.parseInt(outcome[i]));
                                }
                            } else {
                                for (int i = 2; i < outcome.length; i++) {
                                    ports.add(Integer.parseInt(outcome[i]));
                                }
                            }
                            outcomes.put(outcome[1], ports);
                        } else {
                            failed.add(Integer.parseInt(outcome[2]));
                        }
                    }

                    if (outputCount==parts){
                        stop = true;
                        break;
                    }
                }

                if (stop && check) {
                    //sleep(1000);

                    check = false;

                    System.out.println(getErrorMessage());
                    System.out.println(getOutcomeMessage());

                    if (getOutcome().contains("null")){
                        System.out.println("Majority not met!");
                        serverSocket.close();
                        options.remove(removed);
                        new Coordinator(port,parts-failed.size(),options);
                    }
                }


            } catch (IOException e) {
            }

        }

    }

    public String getOutcome(){
        if(outcomes.size()==1){
            for (String o : outcomes.keySet()) {
                return o;
            }
        }
        return null;
    }

    public String getErrorMessage(){
        if (failed.isEmpty()){
            return "All participants have sent their outcome.";
        } else{
            String message = "The following participant(s) failed in sending their outcome :";
            for (Integer p : failed){
                message = message + " " + p;
            }
            return message;
        }
    }

    public String getOutcomeMessage() {
        if (outcomes.isEmpty()) {
            return "All participants failed in sending an outcome.";
        } else if (outcomes.size()==1){
            for (Map.Entry<String, Set<Integer>> p : outcomes.entrySet()) {
                return "The participants " + p.getValue().toString() + " agreed on the outcome " + p.getKey();
            }
        }
        return "The participants did not agree.";
    }


    public Coordinator(Integer port, Integer parts, Set<String> options){

        this.port = port;
        this.parts = parts;
        this.options = options;

        System.out.println("Starting a new round...");
        System.out.println("COORDINATOR " + port);
        System.out.println("List of options : "+ options.toString());

        try {
            serverSocket = new ServerSocket(this.port);

            while (count<2*parts){
                Socket socket = serverSocket.accept();
                count++;
                new CoordinatorThread(socket).start();
            }

        } catch (IOException e) {
        }
    }

    public static void main(String[] args) {

        int port = Integer.parseInt(args[0]);
        int parts = Integer.parseInt(args[1]);
        Set<String> options = new HashSet<>();

        for (int i = 2; i < args.length; i++) {
            options.add(args[i]);
        }


        new Coordinator(port, parts,options);

    }

}
