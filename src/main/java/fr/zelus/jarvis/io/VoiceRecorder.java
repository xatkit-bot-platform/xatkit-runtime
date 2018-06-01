package fr.zelus.jarvis.io;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisException;

import javax.sound.sampled.*;
import java.util.Observable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceRecorder extends Observable {

    private AudioFormat format;

    private DataLine.Info targetInfo;
    private TargetDataLine targetLine;

    protected static int BUFFER_SIZE = 10000;

    private ExecutorService service = Executors.newSingleThreadExecutor();
    private boolean isRecording;

    public VoiceRecorder(AudioFormat format) {
        this.format = format;
        targetInfo = new DataLine.Info(TargetDataLine.class, format);
        isRecording = false;
        try {
            targetLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
            targetLine.open(format);
            targetLine.start();
        } catch(LineUnavailableException e) {
            throw new JarvisException("Cannot open audio line, check that your microphone is plugged", e);
        }
    }

    public VoiceRecorder() {
        this(new AudioFormat(44100, 16, 1, true, false));
    }

    public void startRecording() {
        if(isRecording) {
            throw new JarvisException("Cannot start recording, the VoiceRecorder is already recording");
        } else {
            isRecording = true;
            service.submit(new InternalRecorder());
        }
    }

    private class InternalRecorder implements Runnable {

        @Override
        public void run() {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesReadCount;
            while(true) {
                //if(VoiceRecorder.this.countObservers() > 0) {
                    // Do not listen if there is no observers
                    bytesReadCount = targetLine.read(buffer, 0, buffer.length);
                    setChanged();
                    notifyObservers(buffer);
                //}
            }
        }
    }
}
