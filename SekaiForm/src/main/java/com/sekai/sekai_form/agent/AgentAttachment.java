package com.sekai.sekai_form.agent;

import java.util.Arrays;

/** An attachment held only for the lifetime of an agent request. */
public final class AgentAttachment {
    private final String id;
    private final String name;
    private final String mediaType;
    private final String modality;
    private final byte[] content;

    public AgentAttachment(String id, String name, String mediaType, String modality, byte[] content) {
        this.id = id;
        this.name = name == null ? "attachment" : name;
        this.mediaType = mediaType == null ? "application/octet-stream" : mediaType;
        this.modality = modality == null ? "FILE" : modality;
        this.content = content == null ? new byte[0] : Arrays.copyOf(content, content.length);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getMediaType() { return mediaType; }
    public String getModality() { return modality; }
    public byte[] getContent() { return Arrays.copyOf(content, content.length); }
    public boolean isImage() { return modality.equalsIgnoreCase("IMAGE") || mediaType.startsWith("image/"); }
    public boolean isAudio() { return modality.equalsIgnoreCase("AUDIO") || mediaType.startsWith("audio/"); }
}
