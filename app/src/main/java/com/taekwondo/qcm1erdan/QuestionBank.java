package com.taekwondo.qcm1erdan;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QuestionBank {

    public static List<Question> loadQuestionsFromJSON(Context context) {
        List<Question> questions = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open("questions.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String bloc = obj.optString("bloc", "Culture & Terminologie");
                String intitule = obj.getString("intitule");
                JSONArray optsArray = obj.getJSONArray("options");
                String[] options = new String[optsArray.length()];
                for (int j = 0; j < optsArray.length(); j++) {
                    options[j] = optsArray.getString(j);
                }
                int index = obj.getInt("index");
                String explication = obj.getString("explication");

                questions.add(new Question(bloc, intitule, options, index, explication));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return questions;
    }

    public static Map<String, Integer> getBlocQuestionCounts(Context context) {
        List<Question> fullList = loadQuestionsFromJSON(context);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Question q : fullList) {
            String bloc = q.getBloc();
            Integer current = counts.get(bloc);
            counts.put(bloc, (current == null ? 0 : current) + 1);
        }
        return counts;
    }

    public static List<Question> getRandomQuiz(Context context, int count, List<String> selectedBlocs) {
        List<Question> fullList = loadQuestionsFromJSON(context);
        List<Question> filteredList = new ArrayList<>();

        if (selectedBlocs != null && !selectedBlocs.isEmpty()) {
            for (Question q : fullList) {
                if (selectedBlocs.contains(q.getBloc())) {
                    filteredList.add(q);
                }
            }
        } else {
            filteredList = fullList;
        }

        Collections.shuffle(filteredList);
        return filteredList.subList(0, Math.min(count, filteredList.size()));
    }

    public static List<Question> getRandomQuiz(Context context, int count) {
        return getRandomQuiz(context, count, null);
    }

    public static List<Question> getRandomQuiz(Context context) {
        return getRandomQuiz(context, 10, null);
    }
}