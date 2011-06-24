package org.gonnot.imtp;
import com.agf.agent.AclMessage.Performative;
import com.agf.agent.Agent;
import com.agf.agent.AgentContainer;
import com.agf.agent.ContainerConfiguration;
import com.agf.agent.JadeWrapper;
import com.agf.agent.behaviour.OneShotBehaviour;
import com.agf.agent.test.DummyAgent;
import com.agf.agent.test.OneShotStep;
import com.agf.agent.test.Story;
import com.agf.agent.test.Story.ConnectionType;
import com.agf.test.common.LogString;
import jade.domain.introspection.AMSSubscriber;
import jade.domain.introspection.AddedContainer;
import jade.domain.introspection.AddedMTP;
import jade.domain.introspection.BornAgent;
import jade.domain.introspection.ChangedAgentOwnership;
import jade.domain.introspection.DeadAgent;
import jade.domain.introspection.Event;
import jade.domain.introspection.FrozenAgent;
import jade.domain.introspection.IntrospectionVocabulary;
import jade.domain.introspection.MovedAgent;
import jade.domain.introspection.RemovedContainer;
import jade.domain.introspection.RemovedMTP;
import jade.domain.introspection.ResetEvents;
import jade.domain.introspection.ResumedAgent;
import jade.domain.introspection.SuspendedAgent;
import jade.domain.introspection.ThawedAgent;
import jade.wrapper.PlatformController.Listener;
import jade.wrapper.PlatformEvent;
import java.io.File;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static com.agf.agent.MessageTemplate.matchContent;
import static com.agf.agent.test.AgentAssert.log;
import static com.agf.agent.test.MessageBuilder.message;

@RunWith(Parameterized.class)
public class ReleaseTest {
    public static final String[] RELOADED_PACKAGES = {"org.gonnot", "jade", "com.agf"};
    public static final String[] RELOADED_EXCEPTION = {"jade.util.Logger" /* jade issue */};
    public static final String MESSAGE_CONTENT = "hello";
    private Story story = new Story(ConnectionType.DEFAULT_CONNECTION);
    private LogString log = new LogString();
    private AgentContainer clientContainer;
    private String imtpUsed;


    public ReleaseTest(String imtpUsed) {
        this.imtpUsed = imtpUsed;
    }


    @Parameters
    public static Collection<Object[]> imtpToBeTested() {
        return Arrays.asList(new Object[][]{
//              {jade.imtp.rmi.RMIIMTPManager.class.getName()},
              {WebSocketIMTPManager.class.getName()}
        });
    }


    @Before
    public void setUp() throws Exception {
        story.doSetUp();
        installTestedIMTP(story.getConfiguration());
    }


    @After
    public void tearDown() throws Exception {
        story.doTearDown();
    }


    @Test
    public void testOnlyMainContainer() throws Exception {

        story.record()
              .startTester("receiver")
              .receiveMessage()
              .assertReceivedMessage(matchContent(MESSAGE_CONTENT))
              .replyWith(Performative.AGREE, "thanks");

        story.record()
              .startTester("sender")
              .send(message(Performative.PROPOSE).to("receiver").withContent(MESSAGE_CONTENT))
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchContent("thanks"));

        story.execute();
    }


    @Test
    public void testMainContainerWithAnEmptyClientContainer() throws Exception {
        final Object inOtherClassLoader = newInstance(ReleaseTest.class.getName());

        story.record()
              .addAction(executeMethod("startEmptyClient", inOtherClassLoader));

        story.record()
              .startTester("receiver")
              .receiveMessage()
              .assertReceivedMessage(matchContent(MESSAGE_CONTENT))
              .replyWith(Performative.AGREE, "thanks");

        story.record()
              .startTester("sender")
              .send(message(Performative.PROPOSE).to("receiver").withContent(MESSAGE_CONTENT))
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchContent("thanks"));

        try {
            story.execute();
        }
        finally {
            executeMethod("stopClient", inOtherClassLoader).run();
        }
    }


    @Test
    public void testPlatformListenerUsingAMS() throws Exception {
        final Object inOtherClassLoader = newInstance(ReleaseTest.class.getName());

        story.record().addAction(new com.agf.agent.test.AgentContainerFixture.Runnable() {
            public void run() throws Exception {
                Thread.sleep(200); // to avoid meta-reset-events in the log
            }
        });
        story.record().startAgent("listener", agentWith(new LogAMSBehaviour()));

        story.record().addAction(executeMethod("startClient", inOtherClassLoader));
        story.record().addAssert(log(log, contains("added-container(one-client-node)")));

        story.record().addAction(executeMethod("stopClient", inOtherClassLoader));
        story.record().addAssert(log(log, contains("removed-container(one-client-node)")));

        story.execute();

        log.assertContent(pattern("meta-reset-events(Meta_Reset-Events), "
                                  + "added-container(Main-Container), "
                                  + "born-agent(.*), "
                                  + "born-agent(.*), "
                                  + "born-agent(.*), "
                                  + "born-agent(.*), "
                                  + "added-container(one-client-node), "
                                  + "born-agent(sender), "
                                  + "removed-agent(sender), "
                                  + "removed-container(one-client-node)"));

        log.assertContent(contains("born-agent(ams)"));
        log.assertContent(contains("born-agent(df)"));
        log.assertContent(contains("born-agent(unit-test-director)"));
        log.assertContent(contains("born-agent(listener)"));
    }


    @Test
    public void testPlatformListener() throws Exception {
        final Object inOtherClassLoader = newInstance(ReleaseTest.class.getName());

        story.record()
              .startTester("receiver")
              .perform(new OneShotStep() {
                  public void run(Agent agent) throws Exception {
                      JadeWrapper.unwrapp(agent.getAgentContainer())
                            .getPlatformController()
                            .addPlatformListener(new LogPlatformEvent());
                  }
              });

        story.record().addAction(executeMethod("startClient", inOtherClassLoader));
        story.record().addAssert(log(log, contains("bornAgent(sender.*:35700/JADE)")));

        story.record().addAction(executeMethod("stopClient", inOtherClassLoader));
        story.record().addAssert(log(log, contains("deadAgent(sender.*:35700/JADE)")));

        story.execute();
    }


    @Test
    public void testOneMessageBetweenTwoContainers() throws Exception {
        Object inOtherClassLoader = newInstance(ReleaseTest.class.getName());

        story.record()
              .startTester("receiver")
              .receiveMessage()
              .assertReceivedMessage(matchContent(MESSAGE_CONTENT));

        story.record()
              .addAction(executeMethod("startClient", inOtherClassLoader));

        try {
            story.execute();
        }
        finally {
            executeMethod("stopClient", inOtherClassLoader).run();
        }
    }


    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    public void startClient() throws Exception {
        System.out.println("### Start client node ------------------------");

        startClient("localhost", AgentContainer.CONTAINER_PORT, "one-client-node");
    }


    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    public void startEmptyClient() throws Exception {
        System.out.println("### Start client node ------------------------");

        ContainerConfiguration configuration = new ContainerConfiguration("localhost",
                                                                          AgentContainer.CONTAINER_PORT,
                                                                          "one-client-node");
        installTestedIMTP(configuration);

        clientContainer = AgentContainer.createContainer(configuration);

        clientContainer.start();
    }


    public void startClient(String serverHost, int serverPort, String containerName) throws Exception {
        ContainerConfiguration configuration = new ContainerConfiguration(serverHost, serverPort, containerName);
        installTestedIMTP(configuration);

        clientContainer = AgentContainer.createContainer(configuration);

        clientContainer.start();

        clientContainer.acceptNewAgent("sender", new DummyAgent(new OneShotBehaviour() {
            @Override
            protected void action() {
                getAgent().send(message(Performative.PROPOSE).to("receiver").withContent(MESSAGE_CONTENT).get());
            }
        })).start();
    }


    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    public void stopClient() throws Exception {
        System.out.println("### Stop client node ------------------------");
        if (clientContainer != null) {
            clientContainer.stop();
        }
        clientContainer = null;
    }


    public Object newInstance(String name) throws Exception {
        Class aClass = new ClassLoaderFixture().loadClass(name);
        Constructor constructor = aClass.getConstructor(String.class);

        return constructor.newInstance(imtpUsed);
    }


    private Pattern contains(String pattern) {
        return pattern(".*" + pattern + ".*");
    }


    private Pattern pattern(String pattern) {
        return Pattern.compile(pattern.replaceAll("\\(", "\\\\(")
                                     .replaceAll("\\)", "\\\\)"));
    }


    private static boolean isException(String name) {
        for (String exception : RELOADED_EXCEPTION) {
            if (name.startsWith(exception)) {
                return true;
            }
        }
        return false;
    }


    private Agent agentWith(LogAMSBehaviour behaviour) {
        Agent agent = new Agent();
        JadeWrapper.unwrapp(agent).addBehaviour(behaviour);
        return agent;
    }


    private com.agf.agent.test.AgentContainerFixture.Runnable executeMethod(final String methodName,
                                                                            final Object inOtherClassLoader) {
        return new com.agf.agent.test.AgentContainerFixture.Runnable() {
            public void run() throws Exception {
                inOtherClassLoader.getClass().getMethod(methodName).invoke(inOtherClassLoader);
            }
        };
    }


    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    private void installTestedIMTP(ContainerConfiguration configuration) {
        System.out.println("Using imtp: " + imtpUsed);
        configuration.setParameter("mtps", null);
        configuration.setParameter("imtp", imtpUsed);
    }


    private static class ClassLoaderFixture extends URLClassLoader {
        ClassLoaderFixture() throws MalformedURLException {
            super(parseClassPath());
        }


        @Override
        public Class loadClass(String name) throws ClassNotFoundException {
            for (String toBeReloaded : RELOADED_PACKAGES) {
                if (name.startsWith(toBeReloaded) && !isException(name)) {
                    try {
                        return findClass(name);
                    }
                    catch (LinkageError e) {
                        return handleWeirdBehaviour(name, e);
                    }
                }
            }
            return super.loadClass(name);
        }


        private Class handleWeirdBehaviour(String name, LinkageError e) throws ClassNotFoundException {
            if (e.getMessage().contains("duplicate class definition")) {
                return super.loadClass(name);
            }
            else {
                throw e;
            }
        }


        private static URL[] parseClassPath() throws MalformedURLException {
            return parsePath(System.getProperty("java.class.path"));
        }


        private static URL[] parsePath(String classPath) throws MalformedURLException {
            StringTokenizer tokenizer = new StringTokenizer(classPath, java.io.File.pathSeparator);
            List<URL> paths = new ArrayList<URL>();
            while (tokenizer.hasMoreTokens()) {
                paths.add(new File(tokenizer.nextToken()).toURL());
            }
            return paths.toArray(new URL[paths.size()]);
        }
    }
    private class LogPlatformEvent implements Listener {
        public void bornAgent(PlatformEvent anEvent) {
            log.call("bornAgent", anEvent.getAgentGUID());
        }


        public void deadAgent(PlatformEvent anEvent) {
            log.call("deadAgent", anEvent.getAgentGUID());
        }


        public void startedPlatform(PlatformEvent anEvent) {
            log.call("startedPlatform");
        }


        public void suspendedPlatform(PlatformEvent anEvent) {
            log.call("suspendedPlatform");
        }


        public void resumedPlatform(PlatformEvent anEvent) {
            log.call("resumedPlatform");
        }


        public void killedPlatform(PlatformEvent anEvent) {
            log.call("killedPlatform");
        }
    }
    private class LogAMSBehaviour extends AMSSubscriber {
        @SuppressWarnings({"unchecked"})
        @Override
        protected void installHandlers(Map handlersTable) {

            // Fill the event handler table.

            handlersTable.put(IntrospectionVocabulary.META_RESETEVENTS, new EventHandler() {
                public void handle(Event ev) {
                    ResetEvents re = (ResetEvents)ev;
                    log.call("meta-reset-events", re.getName());
                }
            });

            handlersTable.put(IntrospectionVocabulary.ADDEDCONTAINER, new EventHandler() {
                public void handle(Event ev) {
                    AddedContainer ac = (AddedContainer)ev;
                    log.call("added-container", ac.getContainer().getName());
                }
            });

            handlersTable.put(IntrospectionVocabulary.REMOVEDCONTAINER, new EventHandler() {
                public void handle(Event ev) {
                    RemovedContainer rc = (RemovedContainer)ev;
                    log.call("removed-container", rc.getContainer().getName());
                }
            });

            handlersTable.put(IntrospectionVocabulary.BORNAGENT, new EventHandler() {
                public void handle(Event ev) {
                    BornAgent ba = (BornAgent)ev;
                    log.call("born-agent", ba.getAgent().getLocalName());
                }
            });

            handlersTable.put(IntrospectionVocabulary.DEADAGENT, new EventHandler() {
                public void handle(Event ev) {
                    DeadAgent da = (DeadAgent)ev;
                    log.call("removed-agent", da.getAgent().getLocalName());
                }
            });

            handlersTable.put(IntrospectionVocabulary.SUSPENDEDAGENT, new EventHandler() {
                public void handle(Event ev) {
                    SuspendedAgent sa = (SuspendedAgent)ev;
                    log.call("suspended-agent", sa.getAgent().getName());
                }
            });

            handlersTable.put(IntrospectionVocabulary.RESUMEDAGENT, new EventHandler() {
                public void handle(Event ev) {
                    ResumedAgent ra = (ResumedAgent)ev;
                    log.call("resumed-agent", ra.getAgent().getName());
                }
            });

            handlersTable.put(IntrospectionVocabulary.FROZENAGENT, new EventHandler() {
                public void handle(Event ev) {
                    FrozenAgent fa = (FrozenAgent)ev;
                    log.call("resumed-agent", fa.getAgent().getName());
                }
            });

            handlersTable.put(IntrospectionVocabulary.THAWEDAGENT, new EventHandler() {
                public void handle(Event ev) {
                    ThawedAgent ta = (ThawedAgent)ev;
                    log.call("thaw-agent", ta.getAgent().getName());
                }
            });

            handlersTable.put(IntrospectionVocabulary.CHANGEDAGENTOWNERSHIP, new EventHandler() {
                public void handle(Event ev) {
                    ChangedAgentOwnership cao = (ChangedAgentOwnership)ev;
                    log.call("changed-agent-ownership", cao.getAgent().getName());
                }
            });

            handlersTable.put(IntrospectionVocabulary.MOVEDAGENT, new EventHandler() {
                public void handle(Event ev) {
                    MovedAgent ma = (MovedAgent)ev;
                    log.call("moved-agent", ma.getAgent().getName());
                }
            });

            handlersTable.put(IntrospectionVocabulary.ADDEDMTP, new EventHandler() {
                public void handle(Event ev) {
                    AddedMTP amtp = (AddedMTP)ev;
                    log.call("added-mtp", amtp.getProto());
                }
            });

            handlersTable.put(IntrospectionVocabulary.REMOVEDMTP, new EventHandler() {
                public void handle(Event ev) {
                    RemovedMTP rmtp = (RemovedMTP)ev;
                    log.call("added-mtp", rmtp.getProto());
                }
            });

//            handlersTable.put(IntrospectionVocabulary.PLATFORMDESCRIPTION, new EventHandler() {
//                public void handle(Event ev) {
//                    PlatformDescription pd = (PlatformDescription)ev;
//                    log.call("platform-description", pd.getPlatform().getName());
//                }
//            });
        }
    }
}
