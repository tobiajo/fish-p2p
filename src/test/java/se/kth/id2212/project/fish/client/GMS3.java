package se.kth.id2212.project.fish.client;

/**
 * Created by marcus on 05/01/16.
 */
public class GMS3 {
    public static void main(String[] args) {

        GMS gms = new GMS(9000,new ClientAddress("127.0.0.1", 9000));
        gms.run();

    }
}
