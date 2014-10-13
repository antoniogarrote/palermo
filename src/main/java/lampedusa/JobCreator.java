package lampedusa;

import palermo.PalermoServer;

import java.util.Random;

public class JobCreator {

    public static void main(String[] args) throws InterruptedException {
        Configuration configuration = new Configuration();
        PalermoServer palermo = new PalermoServer(
                configuration.getPalermoHost(),
                configuration.getPalermoPort(),
                configuration.getPalermoExchange());

        while(true) {
            Thread.sleep(new Random().nextInt(20000));
            palermo.enqueue("java",LampedusaJavaJob.class,null);
        }
    }
}
