package org.gonnot.imtp;
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
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.Agent;
import net.codjo.agent.AgentContainer;
import net.codjo.agent.ContainerConfiguration;
import net.codjo.agent.JadeWrapper;
import net.codjo.agent.behaviour.OneShotBehaviour;
import net.codjo.agent.test.DummyAgent;
import net.codjo.agent.test.OneShotStep;
import net.codjo.agent.test.Story;
import net.codjo.agent.test.Story.ConnectionType;
import net.codjo.test.common.LogString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static net.codjo.agent.MessageTemplate.matchContent;
import static net.codjo.agent.test.AgentAssert.log;
import static net.codjo.agent.test.MessageBuilder.message;

@RunWith(Parameterized.class)
public class ReleaseTest {
    public static final String[] RELOADED_PACKAGES = {"org.gonnot", "jade", "net.codjo"};
    public static final String[] RELOADED_EXCEPTION = {"jade.util.Logger" /* jade issue */};
    public static final String MESSAGE_CONTENT = "hello";
    private Story story = new Story(ConnectionType.DEFAULT_CONNECTION);
    private LogString log = new LogString();
    private AgentContainer peripheralContainer;
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
    public void testMainContainerWithAnEmptyPeripheralContainer() throws Exception {
        final Object inOtherClassLoader = newInstance(ReleaseTest.class.getName());

        story.record()
              .addAction(executeMethod("startEmptyPeripheralContainer", inOtherClassLoader));

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
            executeMethod("stopPeripheralContainer", inOtherClassLoader).run();
        }
    }


    @Test
    public void testPlatformListenerUsingAMS() throws Exception {
        final Object inOtherClassLoader = newInstance(ReleaseTest.class.getName());

        story.record().addAction(new net.codjo.agent.test.AgentContainerFixture.Runnable() {
            public void run() throws Exception {
                Thread.sleep(200); // to avoid meta-reset-events in the log
            }
        });
        story.record().startAgent("listener", agentWith(new LogAMSBehaviour()));

        story.record().addAction(executeMethod("startPeripheralContainer", inOtherClassLoader));
        story.record().addAssert(log(log, contains("added-container(one-peripheral-container)")));

        story.record().addAction(executeMethod("stopPeripheralContainer", inOtherClassLoader));
        story.record().addAssert(log(log, contains("removed-container(one-peripheral-container)")));

        story.execute();

        log.assertContent(pattern("meta-reset-events(Meta_Reset-Events), "
                                  + "added-container(Main-Container), "
                                  + "born-agent(.*), "
                                  + "born-agent(.*), "
                                  + "born-agent(.*), "
                                  + "born-agent(.*), "
                                  + "added-container(one-peripheral-container), "
                                  + "born-agent(sender), "
                                  + "removed-agent(sender), "
                                  + "removed-container(one-peripheral-container)"));

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

        story.record().addAction(executeMethod("startPeripheralContainer", inOtherClassLoader));
        story.record().addAssert(log(log, contains("bornAgent(sender.*:35700/JADE)")));

        story.record().addAction(executeMethod("stopPeripheralContainer", inOtherClassLoader));
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
              .addAction(executeMethod("startPeripheralContainer", inOtherClassLoader));

        try {
            story.execute();
        }
        finally {
            executeMethod("stopPeripheralContainer", inOtherClassLoader).run();
        }
    }


    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    public void startPeripheralContainer() throws Exception {
        System.out.println("### Start Peripheral Container ------------------------");

        startPeripheralContainer("localhost", AgentContainer.CONTAINER_PORT, "one-peripheral-container");
    }


    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    public void startEmptyPeripheralContainer() throws Exception {
        System.out.println("### Start Peripheral Container ------------------------");

        ContainerConfiguration configuration = new ContainerConfiguration("localhost",
                                                                          AgentContainer.CONTAINER_PORT,
                                                                          "one-peripheral-container");
        installTestedIMTP(configuration);

        peripheralContainer = AgentContainer.createContainer(configuration);

        peripheralContainer.start();
    }


    public void startPeripheralContainer(String serverHost, int serverPort, String containerName) throws Exception {
        ContainerConfiguration configuration = new ContainerConfiguration(serverHost, serverPort, containerName);
        installTestedIMTP(configuration);

        peripheralContainer = AgentContainer.createContainer(configuration);

        peripheralContainer.start();

        peripheralContainer.acceptNewAgent("sender", new DummyAgent(new OneShotBehaviour() {
            @Override
            protected void action() {
                getAgent().send(message(Performative.PROPOSE).to("receiver").withContent(MESSAGE_CONTENT).get());
            }
        })).start();
    }


    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    public void stopPeripheralContainer() throws Exception {
        System.out.println("### Stop Peripheral Container ------------------------");
        if (peripheralContainer != null) {
            peripheralContainer.stop();
        }
        peripheralContainer = null;
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


    private net.codjo.agent.test.AgentContainerFixture.Runnable executeMethod(final String methodName,
                                                                              final Object inOtherClassLoader) {
        return new net.codjo.agent.test.AgentContainerFixture.Runnable() {
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


        private Class handleWeirdBehaviour(String name, LinkageError linkageError) throws ClassNotFoundException {
            if (linkageError.getMessage().contains("duplicate class definition")) {
                return super.loadClass(name);
            }
            else {
                throw linkageError;
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
        @SuppressWarnings({"unchecked", "InnerClassTooDeeplyNested"})
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
