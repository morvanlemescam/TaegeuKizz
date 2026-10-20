package com.taekwondo.qcm1erdan;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.Locale;

public class ResultActivity extends AppCompatActivity {

    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        TextView textScore = findViewById(R.id.textScore);
        TextView textTime = findViewById(R.id.textTime);
        TextView textStatus = findViewById(R.id.textStatus);
        Button btnRetry = findViewById(R.id.btnRetry);
        MaterialButton btnHome = findViewById(R.id.btnHome);
        LinearLayout layoutQuestionsContainer = findViewById(R.id.layoutQuestionsContainer);

        int score = getIntent().getIntExtra("SCORE", 0);
        int questionCount = getIntent().getIntExtra("QUESTION_COUNT", 10);
        int totalPoints = getIntent().getIntExtra("TOTAL_POINTS", questionCount * 2);
        long elapsedMs = getIntent().getLongExtra("ELAPSED_TIME_MS", 0);
        int passingScore = totalPoints / 2;
        boolean isVocalMode = getIntent().getBooleanExtra("IS_VOCAL_MODE", false);
        ArrayList<String> selectedBlocs = getIntent().getStringArrayListExtra("SELECTED_BLOCS");

        textScore.setText("Note globale : " + score + " / " + totalPoints);
        textTime.setText("⏱️ Temps écoulé : " + formatElapsedTime(elapsedMs));

        String statusSpeech;
        if (score >= passingScore) {
            textStatus.setText("MODULE B VALIDÉ !\nFélicitations, vous avez la moyenne (" + score + " / " + totalPoints + ").");
            textStatus.setTextColor(ContextCompat.getColor(this, R.color.color_correct));
            statusSpeech = "Module B validé ! Félicitations, vous avez au moins 50 pourcent.";
        } else {
            textStatus.setText("MODULE B NON VALIDÉ\n(Minimum requis : " + passingScore + " / " + totalPoints + " - 50%)");
            textStatus.setTextColor(ContextCompat.getColor(this, R.color.color_incorrect));
            statusSpeech = "Module B non validé. Minimum requis : " + passingScore + " points sur " + totalPoints + ".";
        }

        btnRetry.setOnClickListener(v -> {
            Intent intent = new Intent(ResultActivity.this, QuizActivity.class);
            intent.putExtra("QUESTION_COUNT", questionCount);
            if (selectedBlocs != null) {
                intent.putStringArrayListExtra("SELECTED_BLOCS", selectedBlocs);
            }
            startActivity(intent);
            finish();
        });

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(ResultActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        ArrayList<Question> questions = (ArrayList<Question>) getIntent().getSerializableExtra("QUESTIONS");
        int[] userAnswers = getIntent().getIntArrayExtra("USER_ANSWERS");

        if (questions != null && userAnswers != null && questions.size() == userAnswers.length) {
            displayQuestionResults(layoutQuestionsContainer, questions, userAnswers);
        }

        if (isVocalMode) {
            textToSpeech = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(Locale.FRENCH);
                    String speech = "Quiz terminé en " + formatElapsedTimeForSpeech(elapsedMs) + ". Votre note globale est de " + score + " sur " + totalPoints + ". " + statusSpeech;
                    textToSpeech.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "RESULT_SUMMARY_UTTERANCE");

                    if (questions != null && userAnswers != null) {
                        speakWrongAnswersAnalysis(questions, userAnswers);
                    }
                }
            });
        }
    }

    private String formatElapsedTime(long elapsedMs) {
        long seconds = (elapsedMs / 1000) % 60;
        long minutes = (elapsedMs / (1000 * 60)) % 60;
        long hours = elapsedMs / (1000 * 60 * 60);

        if (hours > 0) {
            return String.format(Locale.FRENCH, "%d h %02d min %02d s", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format(Locale.FRENCH, "%02d min %02d s", minutes, seconds);
        } else {
            return String.format(Locale.FRENCH, "%d s", seconds);
        }
    }

    private String formatElapsedTimeForSpeech(long elapsedMs) {
        long seconds = (elapsedMs / 1000) % 60;
        long minutes = (elapsedMs / (1000 * 60)) % 60;
        long hours = elapsedMs / (1000 * 60 * 60);

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append(hours > 1 ? " heures " : " heure ");
        }
        if (minutes > 0) {
            sb.append(minutes).append(minutes > 1 ? " minutes " : " minute ");
        }
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append(seconds > 1 ? " secondes" : " seconde");
        }
        return sb.toString().trim();
    }

    private void speakWrongAnswersAnalysis(ArrayList<Question> questions, int[] userAnswers) {
        int wrongCount = 0;
        for (int i = 0; i < questions.size(); i++) {
            if (userAnswers[i] != questions.get(i).getIndexReponseCorrecte()) {
                wrongCount++;
            }
        }

        if (wrongCount == 0) {
            textToSpeech.speak("Félicitations ! Vous avez fait un sans-faute, aucune erreur à analyser.", TextToSpeech.QUEUE_ADD, null, "NO_ERRORS_UTTERANCE");
            return;
        }

        textToSpeech.speak("Voici l'analyse de vos " + wrongCount + " réponses incorrectes.", TextToSpeech.QUEUE_ADD, null, "WRONG_ANALYSIS_HEADER");

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            int userAnswer = userAnswers[i];
            int correctIndex = q.getIndexReponseCorrecte();

            if (userAnswer != correctIndex) {
                String userAnsText = (userAnswer >= 0 && userAnswer < q.getOptions().length)
                        ? "Réponse " + getOptionLetter(userAnswer) + " : " + q.getOptions()[userAnswer]
                        : "aucune réponse";

                String correctAnsText = "Réponse " + getOptionLetter(correctIndex) + " : " + q.getOptions()[correctIndex];

                String analysisText = "Question " + (i + 1) + ". " + q.getIntitule() + ". " +
                        "Vous avez répondu : " + userAnsText + ". " +
                        "La bonne réponse était : " + correctAnsText + ". ";

                textToSpeech.speak(analysisText, TextToSpeech.QUEUE_ADD, null, "WRONG_Q_" + i);
            }
        }
    }

    private String getOptionLetter(int index) {
        switch (index) {
            case 0: return "A";
            case 1: return "B";
            case 2: return "C";
            case 3: return "D";
            default: return "";
        }
    }

    private void displayQuestionResults(LinearLayout container, ArrayList<Question> questions, int[] userAnswers) {
        LayoutInflater inflater = LayoutInflater.from(this);

        int correctColor = ContextCompat.getColor(this, R.color.color_correct);
        int correctBgColor = ContextCompat.getColor(this, R.color.color_correct_bg);
        int correctStrokeColor = ContextCompat.getColor(this, R.color.color_correct_stroke);

        int incorrectColor = ContextCompat.getColor(this, R.color.color_incorrect);
        int incorrectBgColor = ContextCompat.getColor(this, R.color.color_incorrect_bg);
        int incorrectStrokeColor = ContextCompat.getColor(this, R.color.color_incorrect_stroke);

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            int userAnswer = userAnswers[i];
            boolean isCorrect = (userAnswer == q.getIndexReponseCorrecte());

            View itemView = inflater.inflate(R.layout.item_result_question, container, false);

            MaterialCardView cardView = (MaterialCardView) itemView;
            ImageView imgStatus = itemView.findViewById(R.id.imgStatus);
            TextView textQuestionNum = itemView.findViewById(R.id.textQuestionNum);
            TextView textStatusBadge = itemView.findViewById(R.id.textStatusBadge);
            TextView textQuestionTitle = itemView.findViewById(R.id.textQuestionTitle);
            TextView textUserAnswer = itemView.findViewById(R.id.textUserAnswer);
            TextView textCorrectAnswer = itemView.findViewById(R.id.textCorrectAnswer);
            TextView textExplication = itemView.findViewById(R.id.textExplication);

            textQuestionNum.setText("Question " + (i + 1) + " / " + questions.size() + " • " + q.getBloc().toUpperCase());
            textQuestionTitle.setText(q.getIntitule());

            String userAnsText = (userAnswer >= 0 && userAnswer < q.getOptions().length)
                    ? getOptionLetter(userAnswer) + ") " + q.getOptions()[userAnswer]
                    : "Aucune réponse";

            String correctAnsText = getOptionLetter(q.getIndexReponseCorrecte()) + ") " + q.getOptions()[q.getIndexReponseCorrecte()];

            if (isCorrect) {
                imgStatus.setImageResource(R.drawable.ic_check);
                textStatusBadge.setText("CORRECT (+2 pts)");
                textStatusBadge.setTextColor(correctColor);
                cardView.setStrokeColor(correctStrokeColor);
                cardView.setCardBackgroundColor(correctBgColor);

                textUserAnswer.setText("✓ Votre réponse : " + userAnsText);
                textUserAnswer.setTextColor(correctColor);
                textCorrectAnswer.setVisibility(View.GONE);
            } else {
                imgStatus.setImageResource(R.drawable.ic_cross);
                textStatusBadge.setText("INCORRECT (0 pt)");
                textStatusBadge.setTextColor(incorrectColor);
                cardView.setStrokeColor(incorrectStrokeColor);
                cardView.setCardBackgroundColor(incorrectBgColor);

                textUserAnswer.setText("✗ Votre réponse : " + userAnsText);
                textUserAnswer.setTextColor(incorrectColor);

                textCorrectAnswer.setVisibility(View.VISIBLE);
                textCorrectAnswer.setText("✓ Bonne réponse : " + correctAnsText);
                textCorrectAnswer.setTextColor(correctColor);
            }

            if (q.getExplication() != null && !q.getExplication().trim().isEmpty()) {
                textExplication.setVisibility(View.VISIBLE);
                textExplication.setText("💡 Explication : " + q.getExplication());
            } else {
                textExplication.setVisibility(View.GONE);
            }

            container.addView(itemView);
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}