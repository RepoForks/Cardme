package com.popalay.cardme.data.repositories;

import com.github.popalay.rxrealm.RxRealm;
import com.popalay.cardme.data.models.Card;
import com.popalay.cardme.data.models.Holder;

import java.util.List;

import io.realm.Sort;
import rx.Completable;
import rx.Observable;
import rx.Single;

public class CardRepository implements ICardRepository {

    @Override public Completable save(Card card) {
        return Completable.fromAction(() -> RxRealm.generateObjectId(card, (realm, id) -> {
            if (card.getId() == 0) {
                card.setId(id);
            }
            final Holder realmHolder = realm.where(Holder.class).equalTo(Holder.NAME, card.getHolder().getName())
                    .findFirst();
            if (realmHolder != null) {
                card.setHolder(realmHolder);
            } else {
                final Number num = realm.where(Holder.class).max(Holder.ID);
                final long nextID = num != null ? num.longValue() + 1L : 0L;
                card.getHolder().setId(nextID);
            }
            card.getHolder().setCardsCount(card.getHolder().getCardsCount() + 1);
            realm.copyToRealmOrUpdate(card);
        }));
    }

    @Override public Completable update(List<Card> cards) {
        return Completable.fromAction(() -> RxRealm.doTransactional(realm -> realm.copyToRealmOrUpdate(cards)));
    }

    @Override public Observable<List<Card>> getAll() {
        return RxRealm.listenList(realm -> realm.where(Card.class)
                .findAllSorted(Card.ID, Sort.DESCENDING, Card.USAGE, Sort.DESCENDING)
                .sort(Card.USAGE, Sort.DESCENDING));
    }

    @Override public Observable<List<Card>> getAllByHolder(long holderId) {
        return RxRealm.listenList(realm -> realm.where(Card.class)
                .equalTo(Card.HOLDER_ID, holderId)
                .findAllSorted(Card.ID, Sort.DESCENDING)
                .sort(Card.USAGE, Sort.DESCENDING));
    }

    @Override public Completable remove(final Card card) {
        return Completable.fromAction(() -> RxRealm.doTransactional(realm -> {
            realm.where(Card.class).equalTo(Card.ID, card.getId()).findAll().deleteAllFromRealm();
        }));
    }

    @Override public Single<Card> getByFormattedNumber(String formattedNumber) {
        return RxRealm.getElement(realm -> realm.where(Card.class)
                .equalTo(Card.FORMATTED_NUMBER, formattedNumber)
                .findFirst());
    }
}
