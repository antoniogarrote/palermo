package lampedusa;

import com.mongodb.MongoClient;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;


public class Configuration {

    private final HashMap<String, Object> configuration;
    private MongoClient client;

    public Configuration() {
        String environment = System.getenv("ENV");
        if(environment == null)
            environment = "development";

        this.configuration = parseConfiguration(environment);
    }


    // Mongo configuration
    public HashMap<String,Object> getMongoConfiguration() {
        return (HashMap<String, Object>) configuration.get("mongo");
    }
    public String getMongoHost() { return (String) getMongoConfiguration().get("host"); }
    public Integer getMongoPort() { return (Integer) getMongoConfiguration().get("port"); }
    public String getMongoDatabase() { return (String) getMongoConfiguration().get("database"); }
    public String getMongoCollection() { return (String) getMongoConfiguration().get("collection"); }

    // Palermo configuration
    public HashMap<String,Object> getPalermoConfiguration() {
        return (HashMap<String, Object>) configuration.get("palermo");
    }
    public String getPalermoHost() { return (String) getPalermoConfiguration().get("host"); }
    public Integer getPalermoPort() { return (Integer) getPalermoConfiguration().get("port"); }
    public String getPalermoExchange() { return (String) getPalermoConfiguration().get("exchange"); }

    private HashMap<String,Object> parseConfiguration(String environment) {
        InputStream is = this.getClass().getResourceAsStream("/configuration.yml");

        Yaml yamlParser = new Yaml();
        HashMap<String,Object> configuration = (HashMap<String, Object>) yamlParser.load(is);

        return (HashMap<String,Object>) configuration.get(environment);
    }

}
