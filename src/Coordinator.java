import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * @author Diana-Alexandra Crintea
 * University of Southampton
*/

public class Coordinator {

    private Integer port;
    private Integer parts;
    private Set<String> options;
    private HashMap<Integer,Socket> participants = new HashMap<>();
    private int count =0;
    private int outputCount=0;
    private HashMap<String,Set<Integer>> outcomes = new HashMap<>();
    private ServerSocket serverSocket;
    private boolean check = true;
    private String removed;
    private boolean send = true;


    public static void main(String[] args) {

        int port = Integer.parseInt(args[0]);
        int parts = Integer.parseInt(args[1]);
        Set<String> options = new HashSet<>();

        for (int i = 2; i < args.length; i++) {
            options.add(args[i]);
        }

        // create new Coordinator
        new Coordinator(port, parts, options);


    }

    public Coordinator(Integer port, Integer parts, Set<String> options){

        this.port = port;
        this.parts = parts;
        this.options = options;

        // if the number of participants is 0, then there is no one to vote
        if (parts == 0){
            System.out.println("There is no participant to vote!");
        }else
            // if there is only one option in the list, there is no need to start the voting session
            if (options.size()== 1){
                System.out.println("There is no point in voting as there is only one vote option - " + options.toString());
            } else {

                // print information for each round
                System.out.println("_____________________________________________");
                System.out.println("COORDINATOR " + port + " is starting a new round...");
                System.out.println("List of options : " + options.toString());
                System.out.println("Number of expected participants : " + parts);
                System.out.println("---------------------------------------------");

                try {
                    // open ServerSocket which accepts sockets from participants
                    serverSocket = new ServerSocket(this.port);

                    // each participant connects exactly twice
                    while (count < 2 * parts) {
                        Socket socket = serverSocket.accept();
                        count++;
                        // start thread that communicates with the participant
                        new CoordinatorThread(socket).start();
                    }

                } catch (IOException e) {
                }
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

                        System.out.println("All participants have joined.");

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

//                        if (!message.contains("none")) {

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

                        // stop looking for OUTCOMEs after a while
                        new TimeOut().start();


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

        if (outputCount==parts){
            return "All participants have sent their outcome.";
        } else if (outputCount == 1){
            return "Only one participant has sent his outcome, the rest of " + (parts-outputCount) + " failed.";
        } else if (outputCount == 0){
            return "All participants failed!";
        } else {
            return "Only " + outputCount + " participants have sent their outcome, the rest of " + (parts-outputCount) + " failed.";
        }
    }

    // this method returns a message regarding the final outcome
    public String getOutcomeMessage() {
        if (outcomes.isEmpty()) {
            return "All participants failed in sending an outcome.";
        } else if (outcomes.size()==1){
            for (Map.Entry<String, Set<Integer>> p : outcomes.entrySet()) {
                return "The participants " + p.getValue().toString() + " agreed on the outcome " + p.getKey() + ".";
            }
        }
        return "The participants did not agree.";
    }

    // this class describes a thread which waits for 5 seconds for the Coordinator to receive outputs
    // then it either closes the voting session or starts a new round
    private class TimeOut extends Thread {

        @Override
        public void run() {
            super.run();
            try {
                sleep(5000);
            } catch (InterruptedException e) {
            }

            try {
                serverSocket.close();
            } catch (IOException e) {
            }

            // check makes sure that this is executed only once
            if (check) {
                check = false;

                // informative messages regarding the outcome are printed
                System.out.println(getErrorMessage());
                System.out.println(getOutcomeMessage());

                // if there is no outcome (i.e. all participants fail) then exit
                if (outputCount == 0){
                    System.exit(0);
                }

                // if the outcome is null (i.e. the participants did not meet a majority) then remove an option from the list and restart the game
                if (getOutcome().contains("null")) {
                    System.out.println("Majority not met! A new round will begin");
                    options.remove(removed);
                    System.out.println("Option " + removed + " has been removed from the options list.");
                    new Coordinator(port, outputCount, options);
                }
            }
        }
    }
}
