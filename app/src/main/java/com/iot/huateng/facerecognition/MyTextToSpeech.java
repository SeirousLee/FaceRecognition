package com.iot.huateng.facerecognition;

import android.annotation.SuppressLint;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.Engine;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.content.Intent;
import android.content.ContentResolver;
import android.provider.Settings.Secure;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * seemore
 *
 */
@SuppressLint("NewApi")
public abstract class MyTextToSpeech {

    public static final String INTENT_ACTION_TTS_SERVICE = "android.intent.action.TTS_SERVICE";
    private TextToSpeech mTts;
    private static final int MAX_TTS_FAILURES = 3;

    private boolean isStop = false;

    /** A list of installed TTS engines. */
    private final LinkedList<String> mInstalledTtsEngines = new LinkedList<String>();

    /** The package name of the system TTS engine. */
    private String mSystemTtsEngine;

    /** The package name of the preferred TTS engine. */
    private String mDefaultTtsEngine;

    /** A temporary TTS used for switching engines. */
    private TextToSpeech mTempTts;

    /** The engine loading into the temporary TTS. */
    private String mTempTtsEngine;

    private String mTtsEngine;

    /** The number of time the current TTS has failed. */
    private int mTtsFailures;

    private boolean isInitialized;

    private Context mContext;

    public abstract void speakCompletedCallBack(String callback);

    public MyTextToSpeech(Context context) {
        mContext = context;
        updateDefaultEngine();
    }

    @SuppressLint("NewApi")
    public void speak(String text) {
        if(text != null && mTts != null && isInitialized) {
            //mTts.setOnUtteranceCompletedListener(mUtteranceCompletedListener);
            mTts.setOnUtteranceProgressListener(mUtteranceProgressListener);
            HashMap myHashAlarm = new HashMap();
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
            if(isStop) {
                mTts.speak("", TextToSpeech.QUEUE_FLUSH, myHashAlarm);
            } else {
                mTts.speak(text, TextToSpeech.QUEUE_ADD, myHashAlarm);
            }
        }
    }

    @SuppressLint("NewApi")
    public void speak(String text, int number) {
        if(text != null && mTts != null && isInitialized) {
            //mTts.setOnUtteranceCompletedListener(mUtteranceCompletedListener);
            HashMap myHashAlarm = new HashMap();
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, String.valueOf(number));
            if(isStop) {
                mTts.speak("", TextToSpeech.QUEUE_FLUSH, myHashAlarm);
            } else {
                mTts.speak(text, TextToSpeech.QUEUE_ADD, myHashAlarm);
            }
        }
    }

    public void setStop(boolean mstop) {
        isStop = mstop;
    }

    public void shutdown() {
        if (mTts != null) {
            mTts.shutdown();
        }
    }

    private final TextToSpeech.OnInitListener mTtsChangeListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            handleTtsInitialized(status);
        }
    };

    /** Hands off utterance completed processing. */
    //@SuppressWarnings("deprecation")
    private final OnUtteranceCompletedListener mUtteranceCompletedListener = new OnUtteranceCompletedListener() {
        @Override
        public void onUtteranceCompleted(String utteranceId) {
        }
    };

    @SuppressLint("NewApi")
    private final UtteranceProgressListener mUtteranceProgressListener = new UtteranceProgressListener() {

        @Override
        public void onDone(String text) {
            // TODO Auto-generated method stub
            Log.d("ALEC", "TextToSpeech==========11==========&" + text);
            speakCompletedCallBack(text);
        }

        @Override
        public void onError(String arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStart(String arg0) {
            // TODO Auto-generated method stub

        }

    };

    private void reloadInstalledTtsEngines() {
        final PackageManager pm = mContext.getPackageManager();
        mInstalledTtsEngines.clear();
        if (Build.VERSION.SDK_INT >= 14) {
            reloadInstalledTtsEngines_ICS(pm);
        }
    }

    private void reloadInstalledTtsEngines_ICS(PackageManager pm) {
        final Intent intent = new Intent(INTENT_ACTION_TTS_SERVICE);
        final List<ResolveInfo> resolveInfos = pm.queryIntentServices(intent,
                PackageManager.GET_SERVICES);

        for (ResolveInfo resolveInfo : resolveInfos) {
            final ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            final ApplicationInfo appInfo = serviceInfo.applicationInfo;
            final String packageName = serviceInfo.packageName;
            final boolean isSystemApp = ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);

            mInstalledTtsEngines.add(serviceInfo.packageName);

            if (isSystemApp) {
                mSystemTtsEngine = packageName;
            }
        }
    }

    public void updateDefaultEngine() {
        // Always refresh the list of available engines, since the user may have
        // installed a new TTS and then switched to it.
        reloadInstalledTtsEngines();

        // This may be null if the user hasn't specified an engine.
        mDefaultTtsEngine = Secure
                .getString(mContext.getContentResolver(), Secure.TTS_DEFAULT_SYNTH);
        // Always switch engines when the system default changes.
        setTtsEngine(mDefaultTtsEngine, true);
    }

    @SuppressLint("NewApi")
    private void handleTtsInitialized(int status) {
        if (mTempTts == null) {
            return;
        }

        final TextToSpeech tempTts = mTempTts;
        final String tempTtsEngine = mTempTtsEngine;

        mTempTts = null;
        mTempTtsEngine = null;

        if (status != TextToSpeech.SUCCESS) {
            attemptTtsFailover(tempTtsEngine);
            return;
        }
        if (status == TextToSpeech.SUCCESS)
            isInitialized = true;
        else
            isInitialized = false;

        mTts = tempTts;
        mTts.setOnUtteranceProgressListener(mUtteranceProgressListener);
        mTtsEngine = tempTtsEngine;

    }

    private void attemptTtsFailover(String failedEngine) {
        mTtsFailures++;

        // If there is only one installed engine, or if the current engine
        // hasn't failed enough times, just restart the current engine.
        if ((mInstalledTtsEngines.size() <= 1)
                || (mTtsFailures < MAX_TTS_FAILURES)) {
            setTtsEngine(failedEngine, false);
            return;
        }

        // Move the engine to the back of the list.
        if (failedEngine != null) {
            mInstalledTtsEngines.remove(failedEngine);
            mInstalledTtsEngines.addLast(failedEngine);
        }

        // Try to use the first available TTS engine.
        final String nextEngine = mInstalledTtsEngines.getFirst();

        setTtsEngine(nextEngine, true);
    }

    @SuppressLint("NewApi")
    private void setTtsEngine(String engine, boolean resetFailures) {
        if (resetFailures) {
            mTtsFailures = 0;
        }
        mTempTtsEngine = engine;
        mTempTts = new TextToSpeech(mContext, mTtsChangeListener, engine);
        mTempTts.setOnUtteranceCompletedListener(mUtteranceCompletedListener);
    }

}
