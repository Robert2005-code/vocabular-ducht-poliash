package com.robert.vocabulardutchpolish;

import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.Locale;

@CapacitorPlugin(name = "NativeTTS")
public class TtsPlugin extends Plugin implements OnInitListener {

    private TextToSpeech tts;
    private boolean ready = false;
    private PluginCall pendingCall = null;

    @Override
    public void load() {
        tts = new TextToSpeech(getContext(), this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("nl", "NL"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.ENGLISH);
            }
            ready = true;
            if (pendingCall != null) {
                doSpeak(pendingCall);
                pendingCall = null;
            }
        } else if (pendingCall != null) {
            pendingCall.reject("TTS init failed");
            pendingCall = null;
        }
    }

    @PluginMethod
    public void speak(PluginCall call) {
        if (tts == null) {
            call.reject("TTS unavailable");
            return;
        }
        if (!ready) {
            if (pendingCall != null) {
                pendingCall.resolve();
            }
            pendingCall = call;
            return;
        }
        doSpeak(call);
    }

    private void doSpeak(PluginCall call) {
        if (tts == null) {
            call.reject("TTS unavailable");
            return;
        }
        String text = call.getString("text", "");
        String lang = call.getString("lang", "nl-NL");
        float rate = call.getFloat("rate", 0.85f);
        float pitch = call.getFloat("pitch", 1.0f);

        try {
            String[] parts = (lang != null ? lang : "nl-NL").split("[-_]");
            Locale locale = parts.length >= 2 ? new Locale(parts[0], parts[1]) : new Locale(parts[0]);
            int result = tts.setLanguage(locale);
            if ((result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) && parts.length >= 2) {
                result = tts.setLanguage(new Locale(parts[0]));
            }
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(new Locale("nl", "NL"));
            }
        } catch (Exception e) {
            tts.setLanguage(new Locale("nl", "NL"));
        }

        tts.setSpeechRate(rate);
        tts.setPitch(pitch);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "wordra_tts");
        call.resolve();
    }

    @PluginMethod
    public void stop(PluginCall call) {
        if (tts != null) tts.stop();
        call.resolve();
    }

    @Override
    protected void handleOnDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
