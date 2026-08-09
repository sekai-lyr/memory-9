package com.sekai.sekai_form.model;

import java.util.List;

public class ChatResponse {
    private String reply;
    private String motion;
    private String imageUrl;

    public ChatResponse() {}
    public ChatResponse(String reply, String motion) {
        this.reply = reply; this.motion = motion;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public String getMotion() { return motion; }
    public void setMotion(String motion) { this.motion = motion; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public static class Message {
        private String role;
        private Object content;
        public Message() {}
        public Message(String role, String content) { this.role = role; this.content = content; }
        public Message(String role, Object content) { this.role = role; this.content = content; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Object getContent() { return content; }
        public void setContent(Object content) { this.content = content; }
    }

    public static class AiRequest {
        private String model;
        private List<Message> messages;
        private Integer maxTokens;
        private Double temperature;
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<Message> getMessages() { return messages; }
        public void setMessages(List<Message> messages) { this.messages = messages; }
        public Integer getMaxTokens() { return maxTokens; }
        public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
    }

    public static class AiResponse {
        private List<Choice> choices;
        public List<Choice> getChoices() { return choices; }
        public void setChoices(List<Choice> choices) { this.choices = choices; }
    }

    public static class Choice {
        private Message message;
        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }
    }
}
