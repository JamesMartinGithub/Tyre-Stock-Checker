package com.example.stockcheck.model;

import java.util.ArrayList;

/**
 * Singleton container of ArrayList of tyres, for access between activities.
 */
public class TyreContainer {

    private static volatile TyreContainer instance;
    private ArrayList<Tyre> tyreList;

    private TyreContainer() {

    }

    public static TyreContainer getInstance() {
        if (instance == null) {
            synchronized (TyreContainer.class) {
                if (instance == null) {
                    instance = new TyreContainer();
                }
            }
        }
        return instance;
    }

    public void SetTyreList(ArrayList<Tyre> newList) {
        tyreList = newList;
    }

    public ArrayList<Tyre> GetTyreList() {
        return tyreList;
    }

}
