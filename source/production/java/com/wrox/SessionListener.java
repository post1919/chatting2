package com.wrox;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

@WebListener
public class SessionListener implements HttpSessionListener, HttpSessionIdListener
{
    private SimpleDateFormat formatter =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss a");

    @Override
    public void sessionCreated(HttpSessionEvent e)
    {
        System.out.println(this.date() + ": Session " + e.getSession().getId() +
                " created.");
        SessionRegistry.addSession(e.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent e)
    {
        System.out.println(this.date() + ": Session " + e.getSession().getId() +
                " destroyed.");
        SessionRegistry.removeSession(e.getSession());
    }

    @Override
    public void sessionIdChanged(HttpSessionEvent e, String oldSessionId)
    {
        System.out.println(this.date() + ": Session ID " + oldSessionId +
                " changed to " + e.getSession().getId());
        SessionRegistry.updateSessionId(e.getSession(), oldSessionId);
    }

    private String date()
    {
        return this.formatter.format(new Date());
    }
}
