package com.wrox.chat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.websocket.Session;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ChatSession
{
    private long sessionId;
    private String userName;
    private Session user;
    private String partnerName;
    private Session partner;
    private ChatMessage creationMessage;
    private final List<ChatMessage> chatLog = new ArrayList<>();

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName){
        this.userName = userName;
    }

    public Session getUser(){
        return user;
    }

    public void setUser(Session user){
        this.user = user;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public Session getPartner() {
        return partner;
    }

    public void setPartner(Session partner) {
        this.partner = partner;
    }

    public ChatMessage getCreationMessage() {
        return creationMessage;
    }

    public void setCreationMessage(ChatMessage creationMessage) {
        this.creationMessage = creationMessage;
    }

    @JsonIgnore
    public void log(ChatMessage message) {
        this.chatLog.add(message);
    }

    @JsonIgnore
    public void writeChatLog(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        try(FileOutputStream stream = new FileOutputStream(file))
        {
            mapper.writeValue(stream, this.chatLog);
        }
    }
}
