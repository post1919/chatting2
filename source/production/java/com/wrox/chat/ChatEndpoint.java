package com.wrox.chat;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.HandshakeResponse;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

@ServerEndpoint(value = "/chat/{sessionId}"
, encoders = JsonConvertor.class
, decoders = JsonConvertor.class
, configurator = ChatEndpoint.EndpointConfigurator.class)
public class ChatEndpoint implements HttpSessionListener {

	private static final String HTTP_SESSION_PROPERTY = "com.castingn.ws.HTTP_SESSION";
    private static final String WS_SESSION_PROPERTY = "com.castingn.http.WS_SESSION";
    private static long sessionIdSequence = 1L;
    private static final Object             sessionIdSequenceLock = new Object();
    private static final Map<Long, ChatSession>      chatSessions = new Hashtable<>();
    private static final Map<Session, ChatSession>       sessions = new Hashtable<>();
    private static final Map<Session, HttpSession>   httpSessions = new Hashtable<>();
    public  static final List<ChatSession>        pendingSessions = new ArrayList<>();

	/**
	 * <pre>
	 * 연결
	 * </pre>
	 * @Method Name : onOpen
	 * @param session
	 * @param sessionId
	 */
	@OnOpen
    public void onOpen(Session session, @PathParam("sessionId") long sessionId){

		System.out.println("=> ChatEndpoint.onOpen()");

        HttpSession httpSession = (HttpSession)session.getUserProperties().get(ChatEndpoint.HTTP_SESSION_PROPERTY);

        try {
        	if(httpSession == null || httpSession.getAttribute("username") == null) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "You are not logged in!"));
                return;
            }

        	String username = (String)httpSession.getAttribute("username");
            session.getUserProperties().put("username", username);
            ChatMessage message = new ChatMessage();
            message.setTimestamp(OffsetDateTime.now());
            message.setUser(username);
            ChatSession chatSession;

            if(sessionId < 1){
                message.setType(ChatMessage.Type.STARTED);
                message.setContent(username + " 님이 참여했습니다.");

                chatSession = new ChatSession();

                synchronized(ChatEndpoint.sessionIdSequenceLock){
                    chatSession.setSessionId(ChatEndpoint.sessionIdSequence++);
                }

                chatSession.setUser(session);
                chatSession.setUserName(username);
                chatSession.setCreationMessage(message);
                ChatEndpoint.pendingSessions.add(chatSession);
                ChatEndpoint.chatSessions.put(chatSession.getSessionId(), chatSession);

            } else {
                message.setType(ChatMessage.Type.JOINED);
                message.setContent(username + " 님이 참여했습니다.");

                chatSession = ChatEndpoint.chatSessions.get(sessionId);
                chatSession.setPartner(session);
                chatSession.setPartnerName(username);

                ChatEndpoint.pendingSessions.remove(chatSession);

                session.getBasicRemote().sendObject(chatSession.getCreationMessage());
                session.getBasicRemote().sendObject(message);
            }

            ChatEndpoint.sessions.put(session, chatSession);
            ChatEndpoint.httpSessions.put(session, httpSession);

            this.getSessionsFor(httpSession).add(session);

            chatSession.log(message);
            chatSession.getUser().getBasicRemote().sendObject(message);

            //session.getBasicRemote().sendObject(toJSON("참가 했습니다."));

        } catch (IOException | EncodeException ex) {
            //log.log(Level.SEVERE, null, ex);
        }
    }

	/**
	 * <pre>
	 * 메세지등록
	 * </pre>
	 * @Method Name : onMessage
	 * @param session
	 * @param message
	 */
	@OnMessage
    public void onMessage(Session session, ChatMessage message) {

    	System.out.println("=> ChatEndpoint.onMessage()");
    	System.out.println(message.toString());

        ChatSession charSession = ChatEndpoint.sessions.get(session);
        Session other = this.getOtherSession(charSession, session);

        if(charSession != null && other != null) {
        	charSession.log(message);

            try {
                session.getBasicRemote().sendObject(message);
                other.getBasicRemote().sendObject(message);
            } catch(IOException | EncodeException e) {
                this.onError(session, e);
            }
        }
    }

    /**
     * <pre>
     * 
     * </pre>
     * @Method Name : onClose
     * @param session
     * @param reason
     */
    @OnClose
    public void onClose(Session session, CloseReason reason) {

    	System.out.println("=> ChatEndpoint.onClose()");

        if(reason.getCloseCode() == CloseReason.CloseCodes.NORMAL_CLOSURE) {
            ChatMessage message = new ChatMessage();
            message.setUser((String)session.getUserProperties().get("username"));
            message.setType(ChatMessage.Type.LEFT);
            message.setTimestamp(OffsetDateTime.now());
            message.setContent(message.getUser() + " left the chat.");

            try {
                Session other = this.close(session, message);
                if(other != null) other.close();

            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * <pre>
     * 에러
     * </pre>
     * @Method Name : onError
     * @param session
     * @param e
     */
    @OnError
    public void onError(Session session, Throwable e) {
    	System.out.println("=> ChatEndpoint.onError()");

        ChatMessage message = new ChatMessage();
        message.setUser((String)session.getUserProperties().get("username"));
        message.setType(ChatMessage.Type.ERROR);
        message.setTimestamp(OffsetDateTime.now());
        message.setContent(message.getUser() + " left the chat due to an error.");

        try {
            Session other = this.close(session, message);
            if(other != null) { 
            	other.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, e.toString()));
            }

            e.printStackTrace();

        } catch(IOException ignore) {
        	ignore.printStackTrace();

        } finally {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, e.toString()));
            } catch(IOException ignore) { }
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSessionListener#sessionDestroyed(javax.servlet.http.HttpSessionEvent)
     */
    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
    	System.out.println("=> ChatEndpoint.sessionDestroyed()");
    			
        HttpSession httpSession = event.getSession();

        if(httpSession.getAttribute(WS_SESSION_PROPERTY) != null) {
            ChatMessage message = new ChatMessage();
            message.setUser((String)httpSession.getAttribute("username"));
            message.setType(ChatMessage.Type.LEFT);
            message.setTimestamp(OffsetDateTime.now());
            message.setContent(message.getUser() + " logged out.");

            for( Session session : new ArrayList<>(this.getSessionsFor(httpSession)) ) {
                try {
                    session.getBasicRemote().sendObject(message);
                    Session other = this.close(session, message);
                    if(other != null) other.close();

                } catch(IOException | EncodeException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        session.close();
                    } catch(IOException ignore) { }
                }
            }
        }
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
    	System.out.println("=> ChatEndpoint.sessionCreated()");
    	/* do nothing */ 
	}

    @SuppressWarnings("unchecked")
    private synchronized ArrayList<Session> getSessionsFor(HttpSession session){
    	
    	System.out.println("=> ChatEndpoint.getSessionsFor()");
    	
        try {
            if(session.getAttribute(WS_SESSION_PROPERTY) == null) {
                session.setAttribute(WS_SESSION_PROPERTY, new ArrayList<>());
            }

            return (ArrayList<Session>)session.getAttribute(WS_SESSION_PROPERTY);

        } catch(IllegalStateException e) {
            return new ArrayList<>();
        }
    }

    /**
     * <pre>
     * 연결끊기
     * </pre>
     * @Method Name : close
     * @param session
     * @param message
     * @return
     */
    private Session close(Session session, ChatMessage message) {
    	
    	System.out.println("=> ChatEndpoint.close()");
    	
        ChatSession chatSession = ChatEndpoint.sessions.get(session);
        Session other           = this.getOtherSession(chatSession, session);
        ChatEndpoint.sessions.remove(session);
        HttpSession httpSession = ChatEndpoint.httpSessions.get(session);

        if(httpSession != null) {
            this.getSessionsFor(httpSession).remove(session);
        }

        //자신 연결종료
        if(chatSession != null) {
            chatSession.log(message);
            ChatEndpoint.pendingSessions.remove(chatSession);
            ChatEndpoint.chatSessions.remove(chatSession.getSessionId());

            try {
                chatSession.writeChatLog(new File("chat." + chatSession.getSessionId() + ".log"));
            } catch(Exception e) {
                System.err.println("Could not write chat log.");
                e.printStackTrace();
            }
        }

        //상대방 연결종료
        if(other != null){
            ChatEndpoint.sessions.remove(other);
            httpSession = ChatEndpoint.httpSessions.get(other);
            if(httpSession != null) this.getSessionsFor(httpSession).remove(session);

            try {
                other.getBasicRemote().sendObject(message);
            } catch(IOException | EncodeException e) {
                e.printStackTrace();
            }
        }

        return other;
    }

    
    /**
     * <pre>
     * 상대방 세션가져옴
     * </pre>
     * @Method Name : getOtherSession
     * @param createSesstion
     * @param session
     * @return
     */
    private Session getOtherSession(ChatSession createSesstion, Session session){
    	
    	System.out.println("=> ChatEndpoint.getOtherSession()");
    	
        return createSesstion == null ? null : (session == createSesstion.getUser() ? createSesstion.getPartner() : createSesstion.getUser());
    }

    public static class EndpointConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig config,
                                    HandshakeRequest request,
                                    HandshakeResponse response){
        	
        	System.out.println("=> ChatEndpoint.EndpointConfigurator.modifyHandshake()");
        	
            super.modifyHandshake(config, request, response);

            config.getUserProperties().put(ChatEndpoint.HTTP_SESSION_PROPERTY, request.getHttpSession());
        }
    }

    //String => ChatEndpoint.JSON 변환
    private JsonObject toJSON(String message){
    	
    	System.out.println("=> ChatEndpoint.toJSON()");
    	
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("content",message);
        job.add("uuid", UUID.randomUUID().toString());
        return job.build();
    }
}
