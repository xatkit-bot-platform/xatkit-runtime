package com.xatkit;

/**
 * This class is used to run existing bots, and should not contain test cases.
 */
public class BotTest {

    public static void main(String[] args) {
        Xatkit.main(new String[]{"C:\\Users\\gwend\\Dropbox\\jarvis-demo-resources\\github-demo\\github-demo.properties"});
        try {
            Thread.sleep(10000000);
        }catch(InterruptedException e) {
            e.printStackTrace();
        }
    }
}
