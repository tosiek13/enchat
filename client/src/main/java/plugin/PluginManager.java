package plugin;

import app_info.IPluginCommandContainer;
import app_info.State;
import controller.ControllerManager;
import io.display.IDisplay;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import messages.MessageId;
import network.MessageSignPair;
import network.NetworkMessageIncome;
import network.SendException;
import package_forwarder.MessageIncomeBuffer;
import package_forwarder.PackageForwarder;
import rsa.PublicKeyInfo;
import rsa.RSA;
import util.ExceptionUtils;
import util.StringFormatter;

/**
 * @author robert
 */
public class PluginManager
{
    private final PackageForwarder packageForwarder;
    private final MessageIncomeBuffer messageIncomeBuffer;
    private final IPluginCommandContainer pluginCommandContainer;
    private ControllerManager controllerManager;

    private String passwordHash;

    public PluginManager(MessageIncomeBuffer mmessageIncomeBuffer,
            IPluginCommandContainer ppluginCommandContainer,
            PackageForwarder ppackageForwarder)
    {
        // unchanging reference to some core objects
        messageIncomeBuffer = mmessageIncomeBuffer;
        pluginCommandContainer = ppluginCommandContainer;
        packageForwarder = ppackageForwarder;

        // iterate over all plugins and set plugin manager in them
        for (IPlugin plugin : pluginCommandContainer.getAllPlugins())
        {
            plugin.setPluginManager(this);
        }
    }

    public void setInterlocutorPublicKeyInfo(
            PublicKeyInfo interlocutorPublicKeyInfo)
    {
        packageForwarder.setInterlocutorPublicKeyInfo(
                interlocutorPublicKeyInfo);
    }

    public void setAppEnd()
    {
        controllerManager.setAppEnd();
    }

    public State getAppState()
    {
        return controllerManager.getAppState();
    }

    public void updatePlugin(int id, String[] parameters)
    {
        IPlugin plugin = pluginCommandContainer.getPluginById(id);
        plugin.update(MessageId.ErrorId.OK.getIntRepresentation(),
                parameters);
    }

    public void send(int id, String[] parameters)
    {
        try
        {
            packageForwarder.send(id, parameters);
        }
        catch (SendException ex)
        {
            // TODELETE
            //throw new RuntimeException(ex);
            // odkomentować
            exceptionOccured(ex);
        }
    }

    public String getPasswordHash()
    {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash)
    {
        this.passwordHash = passwordHash;
    }

    public void connect() throws Exception
    {
        packageForwarder.connect();
    }

    public void disconnect()
    {
        packageForwarder.disconnect();
    }

    public void setMsg(String msg, boolean error)
    {
        controllerManager.setMsg(msg, error);
    }

    public void setDisplay(int callerId, IDisplay newDisplay)
    {
        controllerManager.setDisplay(callerId, newDisplay);
    }

    public void setAppState(State newAppState)
    {
        controllerManager.setAppState(newAppState);
    }

    public void updateControllerError(int error)
    {
        controllerManager.updateControllerError(error);
    }

    public void setControllerManager(ControllerManager ccontrollerManager)
    {
        controllerManager = ccontrollerManager;
    }

    public void switchDisplayToMain()
    {
        controllerManager.switchDisplayToMain();
    }

    /**
     * Metoda wywoływana za każdym obiegiem pętli głównej programu. Sprawdza czy
     * są jakieś nowe paczki od serwera w MessageIncomeBuffer i jeżeli tak to
     * przekazuje je do odpowiedniego pluginu.
     */
    public void update()
    {
        // sprawdzamy czy nie wystąpił żaden error
        if (messageIncomeBuffer.isException())
        {
            Exception ex = messageIncomeBuffer.getException();
            // TODELETE
            //throw new RuntimeException(ex);
            // odkomentować
            // jeżeli tak to wywołujemy odpowiednią funkcję
            exceptionOccured(ex);
        }

        // jeżeli są nowe wiadomości od serwera
        else if (messageIncomeBuffer.isAvailable())
        {
            // pobieramy wiadomości
            List<NetworkMessageIncome> messages = messageIncomeBuffer.get();

            // iterujemy po wszystkich wiadomościach
            for (NetworkMessageIncome message : messages)
            {
                // pobieramy wszystkie paczki z danej wiadomości
                List<MessageSignPair> pairs = message.getMessageSignPair();

                // tworzymy listę na parametry
                List<String> parameters = new ArrayList<>();

                // iterujemy po paczkach
                for (MessageSignPair pair : pairs)
                {
                    String param = "";

                    try
                    {
                        param = new String(pair.getMessage(),
                                RSA.STRING_CODING);
                    }
                    catch (Exception ex)
                    {
                        throw new RuntimeException("Something goes wrong "
                                + "in PluginManager.update when converting"
                                + " message to string", ex);
                    }

                    parameters.add(param);
                }

                // pobieramy id wiadomości
                int id = message.getId();
                // pobieramy error wiadomości
                int error = message.getError();
                // jeżeli zły MessageId to rzuci wyjątek runtime exceptions
                // (sprawdza tylko czy prawidłowy)
                MessageId messageId = MessageId.createMessageId(id);
                // jeżeli zły ErrorId to rzuci wyjątkiem runtime excpetion
                // (sprawdza tylko czy prawidłowy)
                MessageId.ErrorId errorId = messageId.createErrorId(error);
                // zapisujemy informację o paczce do loga
                Logger logger = Logger.getLogger("MainLogger");
                String logMsg = "Paczka <id>: " + Integer.toString(id)
                        + "(" + messageId.toString() + ")"
                        + " <error>: " + Integer.toString(error)
                        + "(" + errorId.toString() + ")"
                        + " <parameters>: "
                        + StringFormatter.convertListToString(parameters);
                logger.info(logMsg);
                // pobieramy plugin
                IPlugin plugin = pluginCommandContainer.getPluginById(id);
                plugin.update(error, parameters.toArray(new String[0]));
            }
        }
    }

    private void exceptionOccured(Exception ex)
    {
        // zapisujemy stack trace do loga
        Logger logger = Logger.getLogger("MainLogger");
        logger.warning(ExceptionUtils.getStackTraceString(ex));
        // zmieniamy stan aplikacji na not_connected
        // (package_forwarder gwarantuje, że w przypadku wyjątku połączenie
        // z serwerem zostaje przerwane i całość wraca do stanu sprzed
        // połączenia)
        controllerManager.setAppState(State.NOT_CONNECTED);
        String msg = "Nastąpiło przerwanie połączenia z serwerem w wyniku "
                + "wystąpienia wyjątku";
        setMsg(msg, true);
        // resetujemy wszystkie pluginy
        for (IPlugin plugin : pluginCommandContainer.getAllPlugins())
        {
            plugin.reset();
        }
    }
}
