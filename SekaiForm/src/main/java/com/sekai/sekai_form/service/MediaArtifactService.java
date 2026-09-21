package com.sekai.sekai_form.service;

import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MediaArtifactService {
    private final Path root = Paths.get("data", "agent-media").toAbsolutePath().normalize();
    private final Map<String, Path> artifacts = new ConcurrentHashMap<>();

    public String createAudioUrl(String text) throws IOException {
        Files.createDirectories(root);
        String id = UUID.randomUUID().toString();
        Path target = root.resolve(id + ".wav").normalize();
        if (!target.startsWith(root)) throw new IOException("invalid artifact");
        Files.write(target, createWave(text));
        artifacts.put(id, target);
        return "/api/live2d/agent-media/" + id;
    }

    public Path resolve(String id) {
        Path path = artifacts.get(id);
        if (path == null || !path.startsWith(root) || !Files.exists(path)) return null;
        return path;
    }

    private byte[] createWave(String text) throws IOException {
        int sampleRate = 16000;
        int frames = Math.max(1, Math.min(12, text == null ? 1 : text.length())) * sampleRate / 8;
        byte[] pcm = new byte[frames * 2];
        for (int i = 0; i < frames; i++) {
            double frequency = 440 + ((i / (sampleRate / 8)) % 5) * 35;
            short sample = (short) (Math.sin(2 * Math.PI * frequency * i / sampleRate) * 2500);
            pcm[i * 2] = (byte) (sample & 0xff);
            pcm[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
        }
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        try (AudioInputStream input = new AudioInputStream(new ByteArrayInputStream(pcm), format, frames);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            AudioSystem.write(input, javax.sound.sampled.AudioFileFormat.Type.WAVE, output);
            return output.toByteArray();
        }
    }
}
