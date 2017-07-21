package com.popalay.cardme.presentation.screens.debts

import com.jakewharton.rxrelay2.PublishRelay
import com.popalay.cardme.business.debts.DebtsInteractor
import com.popalay.cardme.data.models.Debt
import com.popalay.cardme.presentation.base.BaseViewModel
import com.popalay.cardme.presentation.base.navigation.CustomRouter
import com.popalay.cardme.utils.extensions.setTo
import com.popalay.cardme.utils.recycler.DiffObservableList
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DebtsViewModel @Inject constructor(
        router: CustomRouter,
        debtsInteractor: DebtsInteractor
) : BaseViewModel() {

    var debts = DiffObservableList<Debt>()

    val addDebtClickPublisher: PublishRelay<Boolean> = PublishRelay.create<Boolean>()

    val onSwiped: PublishRelay<Int> = PublishRelay.create()
    val onUndoSwipe: PublishRelay<Boolean> = PublishRelay.create()

    init {
        debtsInteractor.getDebts()
                .observeOn(AndroidSchedulers.mainThread())
                .setTo(debts)
                .subscribeBy(this::handleBaseError)
                .addTo(disposables)

        onSwiped
                .map(debts::get)
                .flatMapSingle { debtsInteractor.remove(it).toSingle { it } }
                .flatMap { debt -> onUndoSwipe.filter { it }.map { debt } }
                .debounce(1L, TimeUnit.MILLISECONDS)
                .flatMapCompletable(debtsInteractor::save)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(this::handleBaseError)
                .addTo(disposables)
    }
}
