package net.johnsonlau.englishnumber;

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements OnInitListener {
	private static final int REQ_TTS_STATUS_CHECK = 0;
	private boolean mTtsIsReady = false;
	private TextToSpeech mTts;

	private String mRandomNumberString;
	private int mLevel = 1;
	private int mPoints = 1; // so that next time user click "Listen again"
								// would set the points to 0
	private int mListenTimes = 0;

	private EditText mInput;
	private Button mLevel1Button;
	private Button mLevel2Button;
	private Button mLevel3Button;
	private Button mLevel4Button;
	private Button mAnswerButton;
	private Button mClearButton;
	private Button mSubmitButton;
	private Button mListenButton;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		checkTts();
		initMembers();
		bindEvents();

		nextNumber(false);
	}

	// == initialization methods
	// ===============================================================

	private void checkTts() {
		// ensure TTS is ready
		Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, REQ_TTS_STATUS_CHECK);
	}

	private void initMembers() {
		mInput = (EditText) findViewById(R.id.main_input);
		mLevel1Button = (Button) findViewById(R.id.main_level1_btn);
		mLevel2Button = (Button) findViewById(R.id.main_level2_btn);
		mLevel3Button = (Button) findViewById(R.id.main_level3_btn);
		mLevel4Button = (Button) findViewById(R.id.main_level4_btn);
		mAnswerButton = (Button) findViewById(R.id.main_answer_btn);
		mClearButton = (Button) findViewById(R.id.main_clear_btn);
		mSubmitButton = (Button) findViewById(R.id.main_submit_btn);
		mListenButton = (Button) findViewById(R.id.main_listen_btn);
	}

	private void bindEvents() {
		this.mLevel1Button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setToLevel(1, mLevel1Button);
			}
		});
		this.mLevel2Button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setToLevel(2, mLevel2Button);
			}
		});
		this.mLevel3Button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setToLevel(3, mLevel3Button);
			}
		});
		this.mLevel4Button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setToLevel(4, mLevel4Button);
			}
		});
		this.mAnswerButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				mInput.setText(mRandomNumberString);
				mPoints -= getUpPoints();
				updateSubmitButtonText();
			}
		});
		this.mClearButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				mInput.setText("");
			}
		});
		this.mSubmitButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if (mInput.getText().toString().trim()
						.equals(mRandomNumberString)) {
					mPoints += getUpPoints();
					mInput.setText("");
					nextNumber(true);
				} else {
					mPoints -= getDownPoints();
					Toast("No, wrong answer.");
				}
				updateSubmitButtonText();
			}
		});
		this.mListenButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				mListenTimes++;
				mPoints -= getDownPoints();
				speak(mRandomNumberString);
				updateSubmitButtonText();
			}
		});
	}

	// == override methods
	// =====================================================================

	public void onInit(int status) {
		// TTS Engine is initialized
		if (status == TextToSpeech.SUCCESS) {
			// set voice language
			int result = mTts.setLanguage(Locale.US);

			// if it bad voice data
			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				mTtsIsReady = false;
				Log.i(Config.LOG_TAG, "TTS Language is not available.");
			} else {
				mTtsIsReady = true;
			}
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQ_TTS_STATUS_CHECK) {
			switch (resultCode) {
			// TTS Engine is available
			case TextToSpeech.Engine.CHECK_VOICE_DATA_PASS: {
				mTts = new TextToSpeech(this, this);
			}
				break;

			// miss or bad voice data
			case TextToSpeech.Engine.CHECK_VOICE_DATA_BAD_DATA:
			case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_DATA:
			case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_VOLUME: {
				// install voice data
				Intent dataIntent = new Intent();
				dataIntent
						.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(dataIntent);
			}
				break;

			// fail to check
			case TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL:
			default:
				Log.i(Config.LOG_TAG, "TTS is not available.");
				break;
			}
		}
	}

	protected void onPause() {
		super.onPause();

		stopTTS();
	}

	protected void onDestroy() {
		super.onDestroy();

		shutdownTTS();
	}

	// == helpers
	// =============================================================================

	private void setToLevel(int level, Button levelButton) {
		mLevel = level;
		mPoints = 0;
		mListenTimes = -1; // so that next time user click "Listen again" would
							// set the times to 0
		mInput.setText("");
		updateSubmitButtonText();

		mPoints += getDownPoints(); // so that next time user click
									// "Listen again" would set the points to 0

		nextNumber(false);

		resetLevelButtonsColor();

		Resources resources = MainActivity.this.getResources();
		levelButton.setBackgroundDrawable(resources
				.getDrawable(R.drawable.button_selector2));
	}

	private void resetLevelButtonsColor() {
		Resources resources = MainActivity.this.getResources();
		mLevel1Button.setBackgroundDrawable(resources
				.getDrawable(R.drawable.button_selector4));
		mLevel2Button.setBackgroundDrawable(resources
				.getDrawable(R.drawable.button_selector4));
		mLevel3Button.setBackgroundDrawable(resources
				.getDrawable(R.drawable.button_selector4));
		mLevel4Button.setBackgroundDrawable(resources
				.getDrawable(R.drawable.button_selector4));
	}

	private void stopTTS() {
		if (mTts != null) {
			mTts.stop();
		}
	}

	private void shutdownTTS() {
		if (mTts != null) {
			mTts.shutdown();
		}
	}

	private void speak(String msg) {
		if (mTtsIsReady) {
			mTts.speak(msg, TextToSpeech.QUEUE_FLUSH, null);
		}
	}

	private void nextNumber(boolean speak) {
		if (mPoints >= 100) {
			String text = "re-listen: " + String.valueOf(mListenTimes);
			mInput.setText(text);

			String speech = "Level " + String.valueOf(mLevel)
					+ " is done. You re-listened " + String.valueOf(mListenTimes)
					+ " times.";
			speak(speech);
		} else {
			mRandomNumberString = getRandomNumberString();
			if (speak) {
				speak(mRandomNumberString);
			}
		}
	}

	private void Toast(String text) {
		Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
	}

	private String getRandomNumberString() {
		if (mLevel == 1) {
			return getLever1RandomNumberString();
		}
		if (mLevel == 2) {
			return getLever2RandomNumberString();
		}
		if (mLevel == 3) {
			return getLever3RandomNumberString();
		}
		if (mLevel == 4) {
			return getLever4RandomNumberString();
		} else {
			return "error";
		}
	}

	private String getLever1RandomNumberString() {
		return Long.toString((long) (Math.random() * 100));
	}

	private String getLever2RandomNumberString() {
		return Long.toString((long) (Math.random() * 1000));
	}

	private String getLever3RandomNumberString() {
		return Long.toString((long) (Math.random() * 100000));
	}

	private String getLever4RandomNumberString() {
		return Long.toString((long) (Math.random() * 10000000000l));
	}

	private void updateSubmitButtonText() {
		String text = getResources().getString(R.string.main_submit_btn) + " ("
				+ String.valueOf(mPoints) + ")";
		mSubmitButton.setText(text);
	}

	private int getUpPoints() {
		int result = 0;

		if (mLevel == 1) {
			result = 2;
		} else if (mLevel == 2) {
			result = 2;
		} else if (mLevel == 3) {
			result = 4;
		} else {
			result = 4;
		}

		return result;
	}

	private int getDownPoints() {
		int result = 0;

		if (mLevel == 1) {
			result = 1;
		} else if (mLevel == 2) {
			result = 1;
		} else if (mLevel == 3) {
			result = 2;
		} else {
			result = 2;
		}

		return result;
	}

}
