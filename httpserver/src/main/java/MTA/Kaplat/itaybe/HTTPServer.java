package MTA.Kaplat.itaybe;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collections;

@SpringBootApplication
public class HTTPServer
{
    public static void main( String[] args )
    {
        SpringApplication app = new SpringApplication(HTTPServer.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", "9583"));
        app.run(args);
    }
}
