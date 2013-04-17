
package demo;

import static java.lang.System.exit;
import static java.lang.System.out;
import static org.modeshape.jcr.RepositoryConfiguration.read;

import java.util.concurrent.ExecutionException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.infinispan.schematic.document.ParsingException;
import org.modeshape.jcr.ConfigurationException;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.NoSuchRepositoryException;
import org.modeshape.jcr.api.JcrTools;

public class Main {

    static Main me = new Main();

    ModeShapeEngine engine = new ModeShapeEngine();

    JcrRepository repo;

    private final String repoName = "repo";

    private final static String repoConfig = "/repository.json";

    public static void main(String[] args) {
        try {
            me.start();
            Session session = me.repo.login();

            setup: {
                session.getRootNode().addNode("nonfederated");
                new JcrTools(true).uploadFileAndBlock(session, Main.class
                        .getResource("/thing2"), "/nonfederated");
            }

            action: {
                final Workspace ws = session.getWorkspace();
                ws.copy("/p1/thing1", "/nonfederated/thing1");
                session.removeItem("/p1/thing1");
                ws.copy("/nonfederated/thing2", "/p1/thing2");
                session.removeItem("/nonfederated/thing2");
            }

            session.save();
            session.logout();
            session = me.repo.login();

            tests: {
                assert !session.nodeExists("/p1/thing1") : "Shouldn't find /p1/thing1!";
                assert session.nodeExists("/nonfederated/thing1") : "Should find /nonfederated/thing1!";
                assert !session.nodeExists("/nonfederated/thing2") : "Shouldn't find /nonfederated/thing2!";
                assert session.nodeExists("/p1/thing2") : "Should find /p1/thing2!";
            }

            session.save();
            session.logout();
            me.stop();

        } catch (Exception e) {
            e.printStackTrace();
            exit(1);
        }
        exit(0);

    }

    private void start() throws ConfigurationException, ParsingException,
            RepositoryException {
        engine.start();
        out.println("Engine started...");
        engine.deploy(read(this.getClass().getResource(repoConfig)));
        out.println("Repo deployed...");
        engine.startRepository(repoName);
        out.println("Repo started...");
        repo = engine.getRepository(repoName);
    }

    private void stop() throws NoSuchRepositoryException, InterruptedException,
            ExecutionException {
        // block waiting for all repos to shutdown
        out.println("Engine stopping...");
        engine.shutdown().get();
        out.println("Engine stopped.");
    }

}
