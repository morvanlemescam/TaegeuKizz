package com.taekwondo.qcm1erdan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "QcmPrefs";
    private static final String KEY_SELECTED_BLOCS = "SelectedBlocsSet";

    private TextView textBlocsSummary;
    private Map<String, Integer> blocQuestionCounts;
    private final Set<String> selectedBlocs = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialCardView cardExpress = findViewById(R.id.cardExpress);
        MaterialCardView cardExam = findViewById(R.id.cardExam);
        MaterialCardView cardFilterBlocs = findViewById(R.id.cardFilterBlocs);
        MaterialButton btnOpenBlocsDialog = findViewById(R.id.btnOpenBlocsDialog);
        textBlocsSummary = findViewById(R.id.textBlocsSummary);

        blocQuestionCounts = QuestionBank.getBlocQuestionCounts(this);

        loadSelectedBlocsFromPrefs();

        updateBlocsSummaryText();

        View.OnClickListener openDialogListener = v -> showSelectBlocsDialog();
        cardFilterBlocs.setOnClickListener(openDialogListener);
        btnOpenBlocsDialog.setOnClickListener(openDialogListener);

        cardExpress.setOnClickListener(v -> startQuiz(10));
        cardExam.setOnClickListener(v -> startQuiz(30));
    }

    private void loadSelectedBlocsFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> savedSet = prefs.getStringSet(KEY_SELECTED_BLOCS, null);

        selectedBlocs.clear();
        if (savedSet != null && !savedSet.isEmpty()) {
            for (String savedBloc : savedSet) {
                if (blocQuestionCounts.containsKey(savedBloc)) {
                    selectedBlocs.add(savedBloc);
                }
            }
        }

        if (selectedBlocs.isEmpty()) {
            selectedBlocs.addAll(blocQuestionCounts.keySet());
        }
    }

    private void saveSelectedBlocsToPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putStringSet(KEY_SELECTED_BLOCS, new HashSet<>(selectedBlocs)).apply();
    }

    private void updateBlocsSummaryText() {
        int selectedCount = selectedBlocs.size();
        int totalBlocs = blocQuestionCounts.size();
        int totalAvailableQuestions = 0;

        for (String b : selectedBlocs) {
            Integer count = blocQuestionCounts.get(b);
            if (count != null) {
                totalAvailableQuestions += count;
            }
        }

        if (selectedCount == totalBlocs) {
            textBlocsSummary.setText("Tous les thèmes sélectionnés (" + totalBlocs + " thèmes, " + totalAvailableQuestions + " questions)");
        } else {
            textBlocsSummary.setText(selectedCount + " / " + totalBlocs + " thèmes sélectionnés (" + totalAvailableQuestions + " questions disponibles)");
        }
    }

    private void showSelectBlocsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_blocs, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        CheckBox checkSelectAll = dialogView.findViewById(R.id.checkSelectAll);
        LinearLayout layoutBlocsContainer = dialogView.findViewById(R.id.layoutBlocsContainer);
        Button btnConfirmBlocs = dialogView.findViewById(R.id.btnConfirmBlocs);

        List<CheckBox> checkBoxList = new ArrayList<>();
        Set<String> tempSelected = new HashSet<>(selectedBlocs);

        checkSelectAll.setChecked(tempSelected.size() == blocQuestionCounts.size());

        for (Map.Entry<String, Integer> entry : blocQuestionCounts.entrySet()) {
            String blocName = entry.getKey();
            int qCount = entry.getValue();

            CheckBox cb = new CheckBox(this);
            cb.setText(blocName + " (" + qCount + " q.)");
            cb.setTextSize(14f);
            cb.setChecked(tempSelected.contains(blocName));
            cb.setPadding(0, 8, 0, 8);

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    tempSelected.add(blocName);
                } else {
                    tempSelected.remove(blocName);
                }
            });

            checkBoxList.add(cb);
            layoutBlocsContainer.addView(cb);
        }

        checkSelectAll.setOnClickListener(v -> {
            boolean isChecked = checkSelectAll.isChecked();
            tempSelected.clear();
            if (isChecked) {
                tempSelected.addAll(blocQuestionCounts.keySet());
            }
            for (CheckBox cb : checkBoxList) {
                cb.setChecked(isChecked);
            }
        });

        btnConfirmBlocs.setOnClickListener(v -> {
            if (tempSelected.isEmpty()) {
                Toast.makeText(this, "Veuillez sélectionner au moins un thème !", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedBlocs.clear();
            selectedBlocs.addAll(tempSelected);
            saveSelectedBlocsToPrefs();
            updateBlocsSummaryText();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void startQuiz(int questionCount) {
        if (selectedBlocs.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner au moins un thème !", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MainActivity.this, QuizActivity.class);
        intent.putExtra("QUESTION_COUNT", questionCount);
        intent.putStringArrayListExtra("SELECTED_BLOCS", new ArrayList<>(selectedBlocs));
        startActivity(intent);
    }
}