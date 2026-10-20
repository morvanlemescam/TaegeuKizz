package com.taekwondo.qcm1erdan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuizActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 101;

    private TextView textProgress, textBloc, textQuestion;
    private Chronometer chronometer;
    private ProgressBar progressBar;
    private RadioGroup radioGroupOptions;
    private RadioButton optionA, optionB, optionC, optionD;
    private Button btnValidate;
    private MaterialButton btnVocalMode;

    private List<Question> quizQuestions;
    private int[] userAnswers;
    private int currentQuestionIndex = 0;
    private int scorePoints = 0;

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;

    private boolean isVocalMode = false;
    private boolean isTtsInitialized = false;
    private boolean isListening = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        int requestedQuestionCount = getIntent().getIntExtra("QUESTION_COUNT", 10);
        ArrayList<String> selectedBlocs = getIntent().getStringArrayListExtra("SELECTED_BLOCS");

        textProgress = findViewById(R.id.textProgress);
        chronometer = findViewById(R.id.chronometer);
        textBloc = findViewById(R.id.textBloc);
        textQuestion = findViewById(R.id.textQuestion);
        progressBar = findViewById(R.id.progressBar);
        radioGroupOptions = findViewById(R.id.radioGroupOptions);
        optionA = findViewById(R.id.optionA);
        optionB = findViewById(R.id.optionB);
        optionC = findViewById(R.id.optionC);
        optionD = findViewById(R.id.optionD);
        btnValidate = findViewById(R.id.btnValidate);
        btnVocalMode = findViewById(R.id.btnVocalMode);

        quizQuestions = QuestionBank.getRandomQuiz(this, requestedQuestionCount, selectedBlocs);

        if (quizQuestions.isEmpty()) {
            Toast.makeText(this, "Erreur de chargement du fichier JSON ou aucun thème sélectionné", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        progressBar.setMax(quizQuestions.size());
        userAnswers = new int[quizQuestions.size()];

        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();

        initTtsAndStt();

        displayQuestion();

        btnVocalMode.setOnClickListener(v -> {
            if (!isVocalMode) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
                } else {
                    enableVocalMode();
                }
            } else {
                disableVocalMode();
            }
        });

        btnValidate.setOnClickListener(v -> {
            int selectedId = radioGroupOptions.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Veuillez sélectionner une réponse", Toast.LENGTH_SHORT).show();
                if (isVocalMode) {
                    speakFeedbackAndRetry("Veuillez choisir une réponse. Dites A, B, C ou D.");
                }
                return;
            }

            int selectedIndex = -1;
            if (selectedId == R.id.optionA) selectedIndex = 0;
            else if (selectedId == R.id.optionB) selectedIndex = 1;
            else if (selectedId == R.id.optionC) selectedIndex = 2;
            else if (selectedId == R.id.optionD) selectedIndex = 3;

            userAnswers[currentQuestionIndex] = selectedIndex;

            Question currentQ = quizQuestions.get(currentQuestionIndex);
            if (selectedIndex == currentQ.getIndexReponseCorrecte()) {
                scorePoints += 2;
            }

            currentQuestionIndex++;
            if (currentQuestionIndex < quizQuestions.size()) {
                displayQuestion();
                if (isVocalMode) {
                    handler.postDelayed(this::readCurrentQuestionAloud, 400);
                }
            } else {
                finishQuiz();
            }
        });
    }

    private void initTtsAndStt() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.FRENCH);
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsInitialized = true;
                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {}

                        @Override
                        public void onDone(String utteranceId) {
                            if ("QUESTION_UTTERANCE".equals(utteranceId) && isVocalMode) {
                                handler.post(QuizActivity.this::startListeningForAnswer);
                            } else if ("FEEDBACK_RETRY_UTTERANCE".equals(utteranceId) && isVocalMode) {
                                handler.post(QuizActivity.this::startListeningForAnswer);
                            } else if ("ANSWER_FEEDBACK_UTTERANCE".equals(utteranceId) && isVocalMode) {
                                handler.post(() -> btnValidate.performClick());
                            }
                        }

                        @Override
                        public void onError(String utteranceId) {}
                    });
                }
            }
        });

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRENCH.toString());
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fr-FR");
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { isListening = true; }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() { isListening = false; }

                @Override
                public void onError(int error) {
                    isListening = false;
                    if (isVocalMode) {
                        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            speakFeedbackAndRetry("Je n'ai pas compris. Dites A, B, C ou D.");
                        } else if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                            handler.postDelayed(QuizActivity.this::startListeningForAnswer, 500);
                        }
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    isListening = false;
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        processSpokenAnswer(matches);
                    } else if (isVocalMode) {
                        speakFeedbackAndRetry("Je n'ai pas compris. Dites A, B, C ou D.");
                    }
                }

                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }
    }

    private void enableVocalMode() {
        isVocalMode = true;
        btnVocalMode.setText("Vocal : ON");
        btnVocalMode.setIconResource(R.drawable.ic_mic);
        Toast.makeText(this, "Mode Vocal activé. Écoute automatique.", Toast.LENGTH_SHORT).show();

        if (isTtsInitialized) {
            readCurrentQuestionAloud();
        }
    }

    private void disableVocalMode() {
        isVocalMode = false;
        btnVocalMode.setText("Mode Vocal");
        btnVocalMode.setIconResource(R.drawable.ic_mic_off);
        Toast.makeText(this, "Mode Vocal désactivé.", Toast.LENGTH_SHORT).show();

        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
        if (speechRecognizer != null && isListening) {
            try {
                speechRecognizer.cancel();
            } catch (Exception ignored) {}
            isListening = false;
        }
    }

    private void readCurrentQuestionAloud() {
        if (!isTtsInitialized || !isVocalMode) return;

        Question q = quizQuestions.get(currentQuestionIndex);
        String textToSpeak = "Question " + (currentQuestionIndex + 1) + " sur " + quizQuestions.size() + ". " +
                q.getIntitule() + ". " +
                "Réponse A : " + q.getOptions()[0] + ". " +
                "Réponse B : " + q.getOptions()[1] + ". " +
                "Réponse C : " + q.getOptions()[2] + ". " +
                "Réponse D : " + q.getOptions()[3] + ". " +
                "Quelle est votre réponse ?";

        speakAloud(textToSpeak, "QUESTION_UTTERANCE");
    }

    private void speakFeedbackAndRetry(String feedback) {
        if (!isTtsInitialized || !isVocalMode) return;
        speakAloud(feedback, "FEEDBACK_RETRY_UTTERANCE");
    }

    private void speakAloud(String text, String utteranceId) {
        if (textToSpeech != null && isTtsInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        }
    }

    private void startListeningForAnswer() {
        if (!isVocalMode || speechRecognizer == null) return;
        try {
            if (isListening) {
                speechRecognizer.cancel();
                isListening = false;
            }
            speechRecognizer.startListening(recognizerIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processSpokenAnswer(List<String> matches) {
        if (matches == null || matches.isEmpty()) {
            if (isVocalMode) {
                speakFeedbackAndRetry("Je n'ai pas compris. Dites A, B, C ou D.");
            }
            return;
        }

        Question q = quizQuestions.get(currentQuestionIndex);
        int selectedIndex = -1;

        for (String match : matches) {
            selectedIndex = matchSpokenTextToOption(match, q);
            if (selectedIndex != -1) {
                break;
            }
        }

        if (selectedIndex != -1) {
            selectOptionAndValidate(selectedIndex);
        } else {
            if (isVocalMode) {
                speakFeedbackAndRetry("Je n'ai pas compris. Dites A, B, C ou D.");
            }
        }
    }

    private int matchSpokenTextToOption(String rawSpeech, Question q) {
        if (rawSpeech == null) return -1;

        String text = rawSpeech.toLowerCase(Locale.FRENCH)
                .replaceAll("[,!?:;\\-'\"_\\.]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (text.isEmpty()) return -1;

        String cleanText = text
                .replaceAll("^(c'est|c est|je choisis|je dis|je vote pour|je pense|la|le|l'|l )\\s+", "")
                .replaceAll("^(réponse|reponse|option|choix)\\s+", "")
                .trim();

        if (cleanText.equals("a") || cleanText.equals("ah") || cleanText.equals("1") || cleanText.equals("un") || cleanText.equals("premier") || cleanText.equals("premiere") || cleanText.equals("première")) {
            return 0;
        }
        if (cleanText.equals("b") || cleanText.equals("be") || cleanText.equals("bé") || cleanText.equals("2") || cleanText.equals("deux") || cleanText.equals("deuxieme") || cleanText.equals("deuxième") || cleanText.equals("second") || cleanText.equals("seconde")) {
            return 1;
        }
        if (cleanText.equals("c") || cleanText.equals("ce") || cleanText.equals("cé") || cleanText.equals("se") || cleanText.equals("s'est") || cleanText.equals("3") || cleanText.equals("trois") || cleanText.equals("troisieme") || cleanText.equals("troisième")) {
            return 2;
        }
        if (cleanText.equals("d") || cleanText.equals("de") || cleanText.equals("dé") || cleanText.equals("des") || cleanText.equals("4") || cleanText.equals("quatre") || cleanText.equals("quatrieme") || cleanText.equals("quatrième")) {
            return 3;
        }

        if (cleanText.matches("^(a|ah)\\b.*")) return 0;
        if (cleanText.matches("^(b|bé|be)\\b.*")) return 1;
        if (cleanText.matches("^(c|cé|ce)\\b.*")) return 2;
        if (cleanText.matches("^(d|dé|de)\\b.*")) return 3;

        if (cleanText.length() >= 4) {
            for (int i = 0; i < 4; i++) {
                String optText = q.getOptions()[i].toLowerCase(Locale.FRENCH)
                        .replaceAll("[,!?:;\\-'\"_\\.]", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
                if (optText.length() >= 4) {
                    if (cleanText.contains(optText) || optText.contains(cleanText)) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    private void selectOptionAndValidate(int selectedIndex) {
        switch (selectedIndex) {
            case 0: optionA.setChecked(true); break;
            case 1: optionB.setChecked(true); break;
            case 2: optionC.setChecked(true); break;
            case 3: optionD.setChecked(true); break;
        }

        String letter = (selectedIndex == 0) ? "A" : (selectedIndex == 1) ? "B" : (selectedIndex == 2) ? "C" : "D";

        if (isVocalMode) {
            speakAloud("Réponse " + letter + " sélectionnée.", "ANSWER_FEEDBACK_UTTERANCE");
        }
    }

    private void displayQuestion() {
        radioGroupOptions.clearCheck();
        Question q = quizQuestions.get(currentQuestionIndex);

        textProgress.setText("Question " + (currentQuestionIndex + 1) + " / " + quizQuestions.size());
        progressBar.setProgress(currentQuestionIndex + 1);

        textBloc.setText(q.getBloc().toUpperCase());
        textQuestion.setText(q.getIntitule());

        optionA.setText("A) " + q.getOptions()[0]);
        optionB.setText("B) " + q.getOptions()[1]);
        optionC.setText("C) " + q.getOptions()[2]);
        optionD.setText("D) " + q.getOptions()[3]);
    }

    private void finishQuiz() {
        chronometer.stop();
        long elapsedMs = SystemClock.elapsedRealtime() - chronometer.getBase();

        if (textToSpeech != null) textToSpeech.stop();
        if (speechRecognizer != null) speechRecognizer.stopListening();

        Intent intent = new Intent(QuizActivity.this, ResultActivity.class);
        intent.putExtra("SCORE", scorePoints);
        intent.putExtra("QUESTION_COUNT", quizQuestions.size());
        intent.putExtra("TOTAL_POINTS", quizQuestions.size() * 2);
        intent.putExtra("ELAPSED_TIME_MS", elapsedMs);
        intent.putExtra("QUESTIONS", new ArrayList<>(quizQuestions));
        intent.putExtra("USER_ANSWERS", userAnswers);
        intent.putExtra("IS_VOCAL_MODE", isVocalMode);
        ArrayList<String> selectedBlocs = getIntent().getStringArrayListExtra("SELECTED_BLOCS");
        if (selectedBlocs != null) {
            intent.putStringArrayListExtra("SELECTED_BLOCS", selectedBlocs);
        }
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableVocalMode();
            } else {
                Toast.makeText(this, "La permission micro est nécessaire pour utiliser le mode vocal.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }
}