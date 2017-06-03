package com.popalay.cardme.data.repositories;

import com.popalay.cardme.data.models.Debt;
import com.popalay.cardme.data.models.Holder;

import java.util.List;

import rx.Completable;
import rx.Observable;

public interface IDebtRepository {

    Completable save(Debt debt);

    Observable<List<Debt>> getAll();

    Observable<List<Debt>> getAllByHolder(long holderId);

    Observable<Holder> get(long id);

    Completable remove(Debt debt);
}
