package com.oakonell.ticstacktoe.ui.game;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.AudioManager;
import android.media.SoundPool;
import android.preference.PreferenceManager;

import com.oakonell.ticstacktoe.R;

/**
 * Sound pool wrapper for dealing with sounds.
 * 
 * Concept based on
 * http://www.droidnova.com/creating-sound-effects-in-android-part-1,570.html
 */
public class SoundManager {
	private static final int NUM_CONCURRENT_STREAMS = 2;

	private SoundPool mSoundPool;
	private Map<Object, Integer> mSoundPoolMap;
	private AudioManager mAudioManager;
	private Context mContext;

	private boolean playFx;
	private OnSharedPreferenceChangeListener prefListener;

	public SoundManager(Context theContext) {
		mContext = theContext;
		mSoundPool = new SoundPool(NUM_CONCURRENT_STREAMS,
				AudioManager.STREAM_MUSIC, 0);
		mSoundPoolMap = new HashMap<Object, Integer>();
		mAudioManager = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(theContext);
		final String soundFxPrefKey = theContext
				.getString(R.string.pref_sound_fx_key);
		playFx = preferences.getBoolean(soundFxPrefKey, true);
		prefListener = new OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				if (soundFxPrefKey.equals(key)) {
					playFx = sharedPreferences
							.getBoolean(soundFxPrefKey, false);
				}
			}
		};
		preferences.registerOnSharedPreferenceChangeListener(prefListener);
	}

	public void addSound(Object key, int soundID) {
		mSoundPoolMap.put(key, mSoundPool.load(mContext, soundID, 1));
	}

	public int playSound(Object key) {
		return playSound(key, false);
	}

	public int playSound(Object key, boolean loop) {
		if (!playFx) {
			return 0;
		}
		float streamVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		streamVolume = streamVolume
				/ mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		Integer soundId = mSoundPoolMap.get(key);
		if (soundId == null) {
			return 0;
		}
		return mSoundPool.play(soundId, streamVolume, streamVolume, 1,
				loop ? -1 : 0, 1f);
	}

	public void stopSound(int streamId) {
		if (!playFx) {
			return;
		}
		mSoundPool.stop(streamId);
	}

	public void release() {
		mSoundPool.release();
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		preferences.unregisterOnSharedPreferenceChangeListener(prefListener);
	}

	public boolean shouldPlayFx() {
		return playFx;
	}
}
