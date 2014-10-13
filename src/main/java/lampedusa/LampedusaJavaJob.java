package lampedusa;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import palermo.job.PalermoJob;

import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LampedusaJavaJob implements PalermoJob {

    private MongoClient client;

    @Override
    public void process(Object o) throws Exception {
        try {
            DBCollection times = openMongo(new Configuration());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
            Date now = new Date();
            String iso8601 = sdf.format(now);

            DBObject document = new BasicDBObject("iso8601", iso8601)
                    .append("unix", now.getTime() * 1000)
                    .append("date", now);

            times.insert(document);

        } catch(Exception ex) {
            throw ex;
        } finally {
            closeMongo();
        }
    }


    public DBCollection openMongo(Configuration configuration) throws UnknownHostException {
        client = new MongoClient(configuration.getMongoHost(), configuration.getMongoPort());
        return client.getDB(configuration.getMongoDatabase()).getCollection(configuration.getMongoCollection());
    }

    public void closeMongo() {
        if(client != null)
            client.close();
    }


}
