package plan.config;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.context.annotation.ComponentScan;
import plan.integration.akka.SpringAkkaExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(
        basePackages = {
                "plan"
        }
)
public class AkkaConfig {
    
    private final ApplicationContext applicationContext;

    private final SpringAkkaExtension springAkkaExtension;

    @Autowired
    public AkkaConfig(ApplicationContext applicationContext, SpringAkkaExtension springAkkaExtension) {
        this.applicationContext = applicationContext;
        this.springAkkaExtension = springAkkaExtension;
    }
    
    @Bean
    public ActorSystem actorSystem() {
        ActorSystem system = ActorSystem.create("akka-actor-model-system", akkaConfiguration());
        springAkkaExtension.initialize(applicationContext);
        return system;
    }

    @Bean
    public Config akkaConfiguration() {
        return ConfigFactory.load();
    }
}
