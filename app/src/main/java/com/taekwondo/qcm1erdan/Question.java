package com.taekwondo.qcm1erdan;

import java.io.Serializable;

public class Question implements Serializable {
    private String bloc;
    private String intitule;
    private String[] options;
    private int indexReponseCorrecte;
    private String explication;

    public Question(String bloc, String intitule, String[] options, int indexReponseCorrecte, String explication) {
        this.bloc = bloc;
        this.intitule = intitule;
        this.options = options;
        this.indexReponseCorrecte = indexReponseCorrecte;
        this.explication = explication;
    }

    public String getBloc() { return bloc; }
    public String getIntitule() { return intitule; }
    public String[] getOptions() { return options; }
    public int getIndexReponseCorrecte() { return indexReponseCorrecte; }
    public String getExplication() { return explication; }
}
