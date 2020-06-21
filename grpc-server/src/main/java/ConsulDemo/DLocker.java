package ConsulDemo;

import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.SessionClient;
import com.orbitz.consul.model.session.ImmutableSession;
import com.orbitz.consul.model.session.Session;
import com.orbitz.consul.model.session.SessionCreatedResponse;

public class DLocker {

    SessionClient sessionClient;
    KeyValueClient keyValueClient;
    Session session;

    public DLocker(Consul consul) {
        this.sessionClient = consul.sessionClient();
        this.keyValueClient = consul.keyValueClient();

         session = ImmutableSession.builder()
                .name("my-service-lock")
                .ttl("300s")
                .node("foobar")
                .lockDelay("15s").build();

        //SessionCreatedResponse response =  sessionClient.createSession(session);
    }

    public boolean lock(String v) {
        SessionCreatedResponse response =  sessionClient.createSession(session);
        return keyValueClient.acquireLock(v,  response.getId());
    }
}
