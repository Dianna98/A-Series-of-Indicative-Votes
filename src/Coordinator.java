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

    public static void main(String[] args) {

        int port = Integer.parseInt(args[0]);
        int parts = Integer.parseInt(args[1]);
        Set<String> options = new HashSet<>();

        for (int i = 2; i < args.length; i++) {
            options.add(args[i]);
        }

        // create new Coordinator
        new Coordinator(port, parts,options);

    }

    public Coordinator(Integer port, Integer parts, Set<String> options){

        this.port = port;
        this.parts = parts;
        this.options = options;

        // print information for each round
        System.out.println("_____________________________________________");
        System.out.println("COORDINATOR " + port + " is starting a new round...");
        System.out.println("List of options : "+ options.toString());
        System.out.println("Number of expected participants : " + parts);
        System.out.println("---------------------------------------------");

        try {
            // open ServerSocket which accepts sockets from participants
            serverSocket = new ServerSocket(this.port);

            while (count<2*parts){
                Socket socket = serverSocket.accept();
                count++;
                // start thread that communicates with the participant
                new CoordinatorThread(socket).start();
            }

        } catch (IOException e) {
        }
    }

    // this class describes the main thread that assures correspondence with participants
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

                    // get JOIN messages
                    if (message.contains("JOIN")) {
                        System.out.println(message);
                        String[] protocol = message.split(" ");

                        // add participant in the participants set
                        participants.put(Integer.valueOf(protocol[1]), socket);

                        System.out.println("PARTICIPANT " + protocol[1] + " has joined.");
                    }

                    // when all participants joined the game, send them DETAILS and VOTE_OPTIONS
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

                    // get OUTCOME messages
                    if (message.contains("OUTCOME")) {
                        outputCount++;
                        String[] outcome = message.split(" ");

                        if (!message.contains("none")) {

                            // if the outcome is coming from an active participant then print the message
                            System.out.println(message);
                            Set<Integer> ports = new HashSet<>();

                            // if the outcome is a tie save the option to be removed and add the participants to the list
                            if (outcome[1].contains("null")) {

                                removed = outcome[2];
                                for (int i = 3; i < outcome.length; i++) {
                                    ports.add(Integer.parseInt(outcome[i]));
                                }

                            } else {
                                // if the outcome is decided, add the participants to the list
                                for (int i = 2; i < outcome.length; i++) {
                                    ports.add(Integer.parseInt(outcome[i]));
                                }

                            }
                            outcomes.put(outcome[1], ports);

                        } else {
                            // if the outcome comes from a failed participant, add it to the list of failed participants
                            failed.add(Integer.parseInt(outcome[2]));
                        }
                    }

                    // if the number of outputs received is the same as the number of registered participants then stop looking for messages
                    if (outputCount==parts){
                        stop = true;
                        break;
                    }
                }

                // if the number of outputs is meet and if is the first time executing the structure
                if (stop && check) {

                    check = false;

                    // print information about the round and the outcome
                    System.out.println(getErrorMessage());
                    System.out.println(getOutcomeMessage());

                    // if the outcome is null (i.e. the participants did not meet a majority) then remove an option from the list and restart the game
                    if (getOutcome().contains("null")){
                        System.out.println("Majority not met! A new round will begin");
                        serverSocket.close();
                        options.remove(removed);
                        System.out.println("Option " + removed + " has been removed from the options list.");
                        new Coordinator(port,parts-failed.size(),options);
                    }
                }


            } catch (IOException e) {
            }

        }

    }

    // this method returns the final outcome
    public String getOutcome(){
        if(outcomes.size()==1){
            for (String o : outcomes.keySet()) {
                return o;
            }
        }
        return null;
    }

    // this method returns the final message regarding failed participants
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

    // this method returns a message regarding the final outcome
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

}
