import java.net.Socket;

public class Participant {

    protected int cport,pport,timeout,failurecond;

    public Participant(int cport, int pport, int timeout, int failurecond){
        this.cport = cport;
        this.pport = pport;
        this.timeout = timeout;
        this.failurecond = failurecond;

    }

    public static void main(String[] args){
        int cport = Integer.parseInt(args[0]);
        int pport = Integer.parseInt(args[1]);
        int timeout = Integer.parseInt(args[2]);
        int failurecond = Integer.parseInt(args[3]);
    }

    public class ParticipantThread extends Thread{

        public ParticipantThread(){}
    }
}
