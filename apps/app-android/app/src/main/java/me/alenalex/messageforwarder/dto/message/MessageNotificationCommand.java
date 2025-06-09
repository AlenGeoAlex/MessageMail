package me.alenalex.messageforwarder.dto.message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import me.alenalex.messageforwarder.AppContainer;

public class MessageNotificationCommand {

    private String from;
    private String message;
    private String on;

    public String from() {
        return from;
    }

    public MessageNotificationCommand setFrom(String from) {
        this.from = from;
        return this;
    }

    public String message() {
        return message;
    }

    public MessageNotificationCommand setMessage(String message) {
        this.message = message;
        return this;
    }

    public String on() {
        return on;
    }

    public MessageNotificationCommand setOn(String on) {
        this.on = on;
        return this;
    }

    public MessageNotificationCommand setOnNow(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.on = sdf.format(new Date());
        return this;
    }

    public String toJson(){
        if(this.from == null || this.message == null || this.on == null)
            throw new IllegalArgumentException("All fields must be set");
        
        return AppContainer.container()
                .moshi()
                .adapter(MessageNotificationCommand.class)
                .toJson(this);
    }
}
