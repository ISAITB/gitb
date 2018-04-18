package com.gitb.messaging.layer;

import com.gitb.core.*;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.*;
import com.gitb.messaging.model.*;
import com.gitb.messaging.model.tcp.ITransactionListener;
import com.gitb.messaging.model.tcp.ITransactionReceiver;
import com.gitb.messaging.model.tcp.ITransactionSender;
import com.gitb.messaging.model.udp.IDatagramListener;
import com.gitb.messaging.model.udp.IDatagramReceiver;
import com.gitb.messaging.model.udp.IDatagramSender;
import com.gitb.messaging.server.IMessagingServer;
import com.gitb.messaging.server.tcp.TCPMessagingServer;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import com.gitb.utils.ActorUtils;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created by serbay on 9/25/14.
 *
 * Abstract Messaging Handler class that provides several utilities to handle the session and transaction configuration
 */
public abstract class AbstractMessagingHandler implements IMessagingHandler {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMessagingHandler.class);

	@Override
    public InitiateResponse initiate(List<ActorConfiguration> actorConfigurations) {
        try {
            SessionManager sessionManager = SessionManager.getInstance();

	        validateActorConfigurations(actorConfigurations);

            return sessionManager.createSession(this, getMessagingServer(), actorConfigurations);
        } catch (GITBEngineInternalError e) {
            throw e;
        } catch (Exception e) {
            throw new GITBEngineInternalError(e);
        }
    }

    @Override
    public void beginTransaction(String sessionId, String transactionId, String from, String to, List<Configuration> configurations) {
        try {
            SessionManager sessionManager = SessionManager.getInstance();

            SessionContext sessionContext = sessionManager.getSession(sessionId);

            List<TransactionContext> transactions = sessionContext.beginTransaction(transactionId, from, to, configurations);

            for(TransactionContext transactionContext : transactions) {
                ITransactionReceiver transactionReceiver = getReceiver(sessionContext, transactionContext);
                ITransactionSender transactionSender = getSender(sessionContext, transactionContext);
                IDatagramReceiver datagramReceiver = getDatagramReceiver(sessionContext, transactionContext);
                IDatagramSender datagramSender = getDatagramSender(sessionContext, transactionContext);

                if(transactionReceiver != null) {
                    transactionContext.setParameter(ITransactionReceiver.class, transactionReceiver);
                }
                if(transactionSender != null) {
                    transactionContext.setParameter(ITransactionSender.class, transactionSender);
                }
                if(datagramReceiver != null) {
                    transactionContext.setParameter(IDatagramReceiver.class, datagramReceiver);
                }
                if(datagramSender != null) {
                    transactionContext.setParameter(IDatagramSender.class, datagramSender);
                }
            }

        } catch (IOException e) {
            throw new GITBEngineInternalError(e);
        }
    }

    @Override
    public MessagingReport sendMessage(String sessionId, String transactionId, List<Configuration> configurations, Message message) {
        SessionManager sessionManager = SessionManager.getInstance();

        SessionContext sessionContext = sessionManager.getSession(sessionId);
        List<TransactionContext> transactions = sessionContext.getTransactions(transactionId);

        if(transactions.size() == 0) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Messages can not be sent before creating transactions"));
        } else if(transactions.size() == 2) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "'send' command functionality is not available between 2 SUTs, please take a look at 'listen' command"));
        }

        TransactionContext transactionContext = transactions.get(0);

        try {
	        validateSendConfigurations(configurations);
	        validateInputs(message);

            ITransactionSender transactionSender = transactionContext.getParameter(ITransactionSender.class);
	        IDatagramSender datagramSender = transactionContext.getParameter(IDatagramSender.class);

            Message sentMessage = null;
	        if(transactionSender != null) {
		        sentMessage = transactionSender.send(configurations, message);
	        } else if(datagramSender != null) {
		        sentMessage = datagramSender.send(configurations, message);
	        } else {
		        throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "No senders are defined for ["+getModuleDefinition().getId()+"]"));
	        }

            Collection<Exception> nonCriticalErrors = transactionContext.getNonCriticalErrors();
            if(!nonCriticalErrors.isEmpty()) {
                return onError(sentMessage, nonCriticalErrors);
            } else {
                return onSuccess(sentMessage);
            }
        } catch (Exception e) {
            return onError(new GITBEngineInternalError(e));
        } finally {
            transactionContext.clearNonCriticalErrors();
        }
    }

    @Override
    public MessagingReport receiveMessage(String sessionId, String transactionId, String callId, List<Configuration> configurations, Message inputs, List<Thread> messagingThreads) {
        Thread receiveThread = new Thread(new ReceiveRunner(sessionId, transactionId, callId, configurations, inputs, this));
        messagingThreads.add(receiveThread);
        receiveThread.start();
        return CallbackManager.getInstance().waitForCallback(sessionId, callId);
    }

    @Override
    public MessagingReport listenMessage(String sessionId, String transactionId, String from, String to, List<Configuration> configurations, Message inputs) {
        SessionManager sessionManager = SessionManager.getInstance();

        SessionContext sessionContext = sessionManager.getSession(sessionId);
        List<TransactionContext> transactions = sessionContext.getTransactions(transactionId);

        if(transactions.size() == 0) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Messages can not be listened before creating transactions"));
        } else if(transactions.size() == 1) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "'listen' command functionality is only available between 2 SUTs, please take a look at 'send' or 'receive' commands"));
        }

        String fromActorId = ActorUtils.extractActorId(from);
        String fromEndpointName = ActorUtils.extractEndpointName(from);

        String toActorId = ActorUtils.extractActorId(to);
        String toEndpointName = ActorUtils.extractEndpointName(to);

        Message incomingMessage = null;
        Message outgoingMessage = null;

        TransactionContext receiverTransactionContext = null;
        TransactionContext senderTransactionContext = null;

        for(TransactionContext transactionContext : transactions) {
            if((fromEndpointName == null && fromActorId.equals(transactionContext.getWith().getActor()))
                    || (fromEndpointName != null && fromActorId.equals(transactionContext.getWith().getActor()) && fromEndpointName.equals(transactionContext.getWith().getEndpoint()))) {
                receiverTransactionContext = transactionContext;
            } else if((toEndpointName == null && toActorId.equals(transactionContext.getWith().getActor()))
                    || (toEndpointName != null && toActorId.equals(transactionContext.getWith().getActor()) && toEndpointName.equals(transactionContext.getWith().getEndpoint()))) {
                senderTransactionContext = transactionContext;
            }
        }

        if(receiverTransactionContext == null || senderTransactionContext == null) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Could not find the transaction for one of the SUTs."));
        }

        ITransactionListener transactionListener = getListener(sessionContext, receiverTransactionContext, senderTransactionContext);
        IDatagramListener datagramListener = getDatagramListener(sessionContext, receiverTransactionContext, senderTransactionContext);

        IListener listener = null;
        if(transactionListener != null) {
            listener = transactionListener;
        } else if(datagramListener != null) {
            listener = datagramListener;
        } else {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "No listener is defined for the messaging handler ["+getModuleDefinition().getId()+"]"));
        }

        try {
            incomingMessage = listener.listen(configurations, inputs);
        } catch (Exception e) {
            if(e instanceof GITBEngineInternalError) {
                return onError((GITBEngineInternalError) e);
            } else {
                return onError(new GITBEngineInternalError(e));
            }
        }

        return onSuccess(incomingMessage);
    }

    @Override
    public void endTransaction(String sessionId, String transactionId) {
        SessionManager sessionManager = SessionManager.getInstance();

        SessionContext sessionContext = sessionManager.getSession(sessionId);

        List<TransactionContext> transactions = sessionContext.endTransaction(transactionId);

        for(TransactionContext transactionContext : transactions) {
            try {
                Collection<Object> parameters = transactionContext.getParameters();

                for(Object parameter : parameters) {
                    if(parameter instanceof IReceiver) {
                        ((IReceiver) parameter).onEnd();
                    } else if(parameter instanceof ISender) {
                        ((ISender) parameter).onEnd();
                    }
                }

            } catch (Exception e) {
                throw new GITBEngineInternalError(e);
            }
        }

    }

	public ITransactionReceiver getReceiver(SessionContext sessionContext, TransactionContext transactionContext) throws IOException {
		return null;
	}

	public ITransactionSender getSender(SessionContext sessionContext, TransactionContext transactionContext) {
		return null;
	}

    public ITransactionListener getListener(SessionContext sessionContext, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        return null;
    }

	public IDatagramReceiver getDatagramReceiver(SessionContext sessionContext, TransactionContext transactionContext) {
		return null;
	}

	public IDatagramSender getDatagramSender(SessionContext sessionContext, TransactionContext transactionContext) {
		return null;
	}

    public IDatagramListener getDatagramListener(SessionContext sessionContext, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        return null;
    }

	protected IMessagingServer getMessagingServer() throws IOException {
		return TCPMessagingServer.getInstance();
	}

    @Override
    public void endSession(String sessionId) {
        try {
            SessionManager sessionManager = SessionManager.getInstance();

            sessionManager.endSession(sessionId);
        } catch (UnknownHostException e) {
            throw new GITBEngineInternalError(e);
        }
    }

    private void checkRequiredTypedParameters(List<TypedParameter> parameters, Message message) {
        // name -> type pairs
        Map<String, String> requiredInputs = new HashMap<>();

        for(TypedParameter parameter : parameters) {
            if(parameter.getUse() == UsageEnumeration.R) {
                requiredInputs.put(parameter.getName(), parameter.getType());
            }
        }

        for(Map.Entry<String, String> requiredInput : requiredInputs.entrySet()) {
            if(!message.getFragments().containsKey(requiredInput.getKey())) {
                throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.MISSING_CONFIGURATION, "'"+requiredInput.getKey()+"' fragment is required."));
            } else if(!message.getFragments().get(requiredInput.getKey()).getType().equals(requiredInput.getValue())) {
                throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_CONFIGURATION, "'"+requiredInput.getKey()+"' should have a type of '"+requiredInput.getValue()+"'"));
            }
        }
    }

    private void checkRequiredParameters(List<Parameter> parameters, List<Configuration> configurations) {
        Set<String> requiredActorConfigurations = new HashSet<>();
        for(Parameter parameter : parameters) {
            if(parameter.getUse() == UsageEnumeration.R) {
                requiredActorConfigurations.add(parameter.getName());
            }
        }
        for(Configuration configuration : configurations) {
            if(requiredActorConfigurations.isEmpty()) {
                break;
            }
            if(requiredActorConfigurations.contains(configuration.getName())) {
                requiredActorConfigurations.remove(configuration.getName());
            }
        }

        if(!requiredActorConfigurations.isEmpty()) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.MISSING_CONFIGURATION, "["+ StringUtils.join(requiredActorConfigurations, ',')+"] actor configurations are required."));
        }
    }

	protected void validateActorConfigurations(List<ActorConfiguration> actorConfigurations) {
		MessagingModule module = getModuleDefinition();

		for(ActorConfiguration actorConfiguration : actorConfigurations) {
            checkRequiredParameters(module.getActorConfigs().getParam(), actorConfiguration.getConfig());
		}
	}

	protected void validateInputs(Message message) {
		MessagingModule module = getModuleDefinition();

		checkRequiredTypedParameters(module.getInputs().getParam(), message);
	}

	protected void validateOutputs(Message message) {
        MessagingModule module = getModuleDefinition();

        checkRequiredTypedParameters(module.getOutputs().getParam(), message);
	}

	protected void validateReceiveConfigurations(List<Configuration> configurations) {
        MessagingModule module = getModuleDefinition();

        if(module.getReceiveConfigs() != null) {
            checkRequiredParameters(module.getReceiveConfigs().getParam(), configurations);
        }
	}

	protected void validateSendConfigurations(List<Configuration> configurations) {
        MessagingModule module = getModuleDefinition();

        if(module.getSendConfigs() != null){
            checkRequiredParameters(module.getSendConfigs().getParam(), configurations);
        }
	}

    protected MessagingReport onError(GITBEngineInternalError error) {
        logger.error("An error occurred", error);
        return MessagingHandlerUtils.generateErrorReport(error);
    }

    protected MessagingReport onError(Message message, Collection<Exception> nonCriticalErrors) {
        return MessagingHandlerUtils.generateErrorReport(message, nonCriticalErrors);
    }

    protected MessagingReport onSuccess(Message message) {
        return MessagingHandlerUtils.generateSuccessReport(message);
    }

	static class ReceiveRunner implements Runnable {

        private final String sessionId;
        private final AbstractMessagingHandler handler;
	    private final String transactionId;
	    private final List<Configuration> configurations;
	    private final Message inputs;
        private final String callId;

        ReceiveRunner(String sessionId, String transactionId, String callId, List<Configuration> configurations, Message inputs, AbstractMessagingHandler handler) {
            this.handler = handler;
            this.transactionId = transactionId;
            this.configurations = configurations;
            this.inputs = inputs;
            this.sessionId = sessionId;
            this.callId = callId;
        }

        private MessagingReport doReceiveMessage(String transactionId, List<Configuration> configurations, Message inputs) {
            SessionManager sessionManager = SessionManager.getInstance();
            SessionContext sessionContext = sessionManager.getSession(sessionId);
            List<TransactionContext> transactions = sessionContext.getTransactions(transactionId);

            if(transactions.size() == 0) {
                throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Messages can not be received before creating transactions"));
            } else if(transactions.size() == 2) {
                throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "'receive' command functionality is not available between 2 SUTs, please take a look at 'listen' command"));
            }
            TransactionContext transactionContext = transactions.get(0);

            try {
                handler.validateReceiveConfigurations(configurations);

                ITransactionReceiver transactionReceiver = transactionContext.getParameter(ITransactionReceiver.class);
                IDatagramReceiver datagramReceiver = transactionContext.getParameter(IDatagramReceiver.class);

                Message message;
                if(transactionReceiver != null) {
                    message = transactionReceiver.receive(configurations, inputs);
                } else if(datagramReceiver != null) {
                    message = datagramReceiver.receive(configurations, inputs);
                } else {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "No receivers are defined for ["+handler.getModuleDefinition().getId()+"]"));
                }

                handler.validateOutputs(message);

                Collection<Exception> nonCriticalErrors = transactionContext.getNonCriticalErrors();
                if(!nonCriticalErrors.isEmpty()) {
                    return handler.onError(message, nonCriticalErrors);
                } else {
                    return handler.onSuccess(message);
                }
            } catch (Exception e) {
                return handler.onError(new GITBEngineInternalError(e));
            } finally {
                transactionContext.clearNonCriticalErrors();
            }
        }

        @Override
        public void run() {
            MessagingReport report = doReceiveMessage(transactionId, configurations, inputs);
            CallbackManager.getInstance().callbackReceived(sessionId, callId, report);
        }
    }

}
