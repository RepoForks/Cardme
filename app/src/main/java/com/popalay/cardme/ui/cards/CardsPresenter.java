package com.popalay.cardme.ui.cards;

import android.content.Context;

import com.arellomobile.mvp.InjectViewState;
import com.popalay.cardme.App;
import com.popalay.cardme.business.cards.CardsInteractor;
import com.popalay.cardme.data.events.AddCardEvent;
import com.popalay.cardme.data.models.Card;
import com.popalay.cardme.ui.removablelistitem.RemovableListItemPresenter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import javax.inject.Inject;

import io.card.payment.CreditCard;
import rx.Completable;
import rx.android.schedulers.AndroidSchedulers;

@InjectViewState
public class CardsPresenter extends RemovableListItemPresenter<Card, CardsView> {

    @Inject CardsInteractor cardsInteractor;
    @Inject Context context;

    public CardsPresenter() {
        App.appComponent().inject(this);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(sticky = true)
    public void onAddCardShortCutEvent(AddCardEvent event) {
        EventBus.getDefault().removeStickyEvent(AddCardEvent.class);
        onAddClick();
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();

        cardsInteractor.getCards()
                .compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getViewState()::setItems, this::handleBaseError);
    }

    public void onAddClick() {
        getViewState().startCardScanning();
    }

    public void onCardScanned(CreditCard card) {
        getViewState().addCardDetails(card);
    }

    public void onCardClick(Card card) {
        cardsInteractor.incCardUsage(card)
                .compose(bindToLifecycle().forCompletable())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> getViewState().shareCardNumber(card.getNumber()), this::handleBaseError);
    }

    @Override
    protected Completable removeItem(Card item) {
        return cardsInteractor.removeCard(item);
    }

    @Override
    protected Completable saveItem(Card item) {
        return cardsInteractor.save(item);
    }
}