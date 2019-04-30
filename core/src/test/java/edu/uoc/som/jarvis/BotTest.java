package edu.uoc.som.jarvis;

/**
 * This class is used to run existing bots, and should not contain test cases.
 */
public class BotTest {

    public static void main(String[] args) {
        Jarvis.main(new String[]{"<path to the Jarvis properties file>"});
        try {
            Thread.sleep(10000000);
        }catch(InterruptedException e) {
            e.printStackTrace();
        }
    }
}
