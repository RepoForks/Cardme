package com.popalay.cardme.presentation.screens.home

import android.app.PendingIntent
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.databinding.DataBindingUtil
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.tech.NfcF
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBarDrawerToggle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import com.popalay.cardme.R
import com.popalay.cardme.business.ShortcutInteractor
import com.popalay.cardme.data.models.Card
import com.popalay.cardme.databinding.ActivityHomeBinding
import com.popalay.cardme.presentation.base.BaseActivity
import com.popalay.cardme.presentation.base.navigation.CustomNavigator
import com.popalay.cardme.presentation.screens.*
import com.popalay.cardme.presentation.screens.addcard.AddCardActivity
import com.popalay.cardme.presentation.screens.adddebt.AddDebtActivity
import com.popalay.cardme.presentation.screens.carddetails.CardDetailsActivity
import com.popalay.cardme.presentation.screens.cards.CardsFragment
import com.popalay.cardme.presentation.screens.debts.DebtsFragment
import com.popalay.cardme.presentation.screens.holderdetails.HolderDetailsActivity
import com.popalay.cardme.presentation.screens.holders.HoldersFragment
import com.popalay.cardme.presentation.screens.settings.SettingsActivity
import com.popalay.cardme.presentation.screens.trash.TrashActivity
import com.popalay.cardme.utils.extensions.findFragmentByType
import com.popalay.cardme.utils.extensions.setSelectedItem
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import io.card.payment.CardIOActivity
import org.cryse.widget.persistentsearch.PersistentSearchView
import org.cryse.widget.persistentsearch.SearchItem
import org.cryse.widget.persistentsearch.SearchSuggestionsBuilder
import ru.terrakok.cicerone.commands.Command
import ru.terrakok.cicerone.commands.Forward
import shortbread.Shortcut
import javax.inject.Inject


class HomeActivity : BaseActivity(), HasSupportFragmentInjector {

    @Inject lateinit var factory: ViewModelProvider.Factory
    @Inject lateinit var androidInjector: DispatchingAndroidInjector<Fragment>
    @Inject lateinit var shortcutInteractor: ShortcutInteractor

    private lateinit var b: ActivityHomeBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var viewModelFacade: HomeViewModelFacade

    private var adapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var filters: Array<IntentFilter>? = null
    private var techLists: Array<Array<String>>? = null

    companion object {

        fun getIntent(context: Context) = Intent(context, HomeActivity::class.java)

    }

    override var navigator = object : CustomNavigator(this, R.id.host) {

        override fun createFragment(screenKey: String, data: Any?) = when (screenKey) {
            SCREEN_CARDS -> {
                b.bottomBar.setSelectedItem(R.id.cards, false)
                CardsFragment.newInstance()
            }
            SCREEN_HOLDERS -> {
                b.bottomBar.setSelectedItem(R.id.holders, false)
                HoldersFragment.newInstance()
            }
            SCREEN_DEBTS -> {
                b.bottomBar.setSelectedItem(R.id.debts, false)
                DebtsFragment.newInstance()
            }
            else -> null
        }?.also { b.searchView.closeSearch() }

        override fun createActivityIntent(screenKey: String, data: Any?) = when (screenKey) {
            SCREEN_HOME -> HomeActivity.getIntent(activity)
            SCREEN_HOLDER_DETAILS -> HolderDetailsActivity.getIntent(activity, data as String)
            SCREEN_ADD_CARD -> AddCardActivity.getIntent(activity, data as Card)
            SCREEN_SCAN_CARD -> Intent(activity, CardIOActivity::class.java)
            SCREEN_SETTINGS -> SettingsActivity.getIntent(activity)
            SCREEN_ADD_DEBT -> AddDebtActivity.getIntent(activity)
            SCREEN_TRASH -> TrashActivity.getIntent(activity)
            SCREEN_CARD_DETAILS -> CardDetailsActivity.getIntent(activity, data as String)
            else -> null
        }?.also { b.searchView.closeSearch() }

        override fun setupActivityTransactionAnimation(command: Command, activityIntent: Intent): Bundle? {
            if (command is Forward && command.screenKey == SCREEN_ADD_DEBT) {
                return findFragmentByType<DebtsFragment>()?.createAddDebtTransition(activityIntent)
            } else if (command is Forward && command.screenKey == SCREEN_CARD_DETAILS) {
                return findFragmentByType<CardsFragment>()?.createCardDetailsTransition(activityIntent)
            }
            return super.setupActivityTransactionAnimation(command, activityIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = DataBindingUtil.setContentView<ActivityHomeBinding>(this, R.layout.activity_home)
        ViewModelProviders.of(this, factory).get(HomeViewModel::class.java).let {
            b.vm = it
            viewModelFacade = it
        }
        initNfcListening()
        initUI()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    override fun onNewIntent(intent: Intent) {
        processIntent(intent)
    }

    public override fun onResume() {
        super.onResume()
        adapter?.enableForegroundDispatch(this, pendingIntent, filters, techLists)
    }

    override fun onPause() {
        super.onPause()
        adapter?.disableForegroundDispatch(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CardsFragment.SCAN_REQUEST_CODE) {
            findFragmentByType<CardsFragment>()?.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toggle.onConfigurationChanged(newConfig)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        b.searchView.setStartPositionFromMenuItem(menu.findItem(R.id.action_search).actionView)
        return true
    }

    override fun onBackPressed() {
        if (b.drawerLayout.isDrawerOpen(Gravity.START)) {
            b.drawerLayout.closeDrawers()
        } else if (b.searchView.isSearching) {
            b.searchView.closeSearch()
        } else super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) return true
        when (item.itemId) {
            R.id.action_search -> b.searchView.openSearch()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun supportFragmentInjector() = androidInjector

    private fun initUI() {
        setSupportActionBar(b.toolbar)
        toggle = ActionBarDrawerToggle(this, b.drawerLayout,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        toggle.isDrawerIndicatorEnabled = true
        b.drawerLayout.addDrawerListener(toggle)

        //TODO move to the search interactor
        b.searchView.setSuggestionBuilder(object : SearchSuggestionsBuilder {
            override fun buildSearchSuggestion(maxCount: Int, query: String?): MutableCollection<SearchItem> {
                return arrayListOf(
                        SearchItem("Card", "32847297492"),
                        SearchItem("Holder", "Denis Nikiforov", SearchItem.TYPE_SEARCH_ITEM_SUGGESTION),
                        SearchItem("Holder", "Hjjj Sdkfj", SearchItem.TYPE_SEARCH_ITEM_SUGGESTION),
                        SearchItem("Card", "32847297492", SearchItem.TYPE_SEARCH_ITEM_SUGGESTION)
                )
            }

            override fun buildEmptySearchSuggestion(maxCount: Int): MutableCollection<SearchItem> = arrayListOf()

        })

        b.searchView.setSearchListener(object : PersistentSearchView.SearchListener {
            override fun onSearchExit() {

            }

            override fun onSearchCleared() {

            }

            override fun onSearchEditClosed() {

            }

            override fun onSearchTermChanged(term: String?) {

            }

            override fun onSearchEditBackPressed() = true

            override fun onSearchEditOpened() {

            }

            override fun onSearch(query: String?) {

            }

        })
    }

    //TODO create specific class
    private fun initNfcListening() {
        adapter = NfcAdapter.getDefaultAdapter(this)
        pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP), 0)
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        try {
            ndef.addDataType("application/" + packageName)
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("fail", e)
        }
        filters = arrayOf(ndef)
        techLists = arrayOf((arrayOf<String>(NfcF::class.java.name)))
    }

    private fun processIntent(intent: Intent) {
        val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        val msg = rawMsgs [0] as NdefMessage
        val bytes = msg.records[0].payload
        viewModelFacade.onNfcMessageRead(bytes)
        getIntent().action = null
    }

    // Shortcuts
    @Shortcut(id = "SHORTCUT_ADD_CARD", icon = R.drawable.ic_shortcut_add_card, rank = 0, shortLabelRes = R.string.shortcut_add_card)
    fun addCardShortcut() {
        shortcutInteractor.applyShortcut(ShortcutInteractor.Shortcut.ADD_CARD)
    }

    @Shortcut(id = "SHORTCUT_DEBTS", icon = R.drawable.ic_shortcut_debts, rank = 2, shortLabelRes = R.string.shortcut_debts)
    fun debtsShortcut() {
        shortcutInteractor.applyShortcut(ShortcutInteractor.Shortcut.DEBTS)
    }
}
