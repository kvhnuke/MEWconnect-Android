package com.myetherwallet.mewconnect.feature.buy.fragment

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.util.TypedValue
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import com.myetherwallet.mewconnect.R
import com.myetherwallet.mewconnect.core.di.ApplicationComponent
import com.myetherwallet.mewconnect.core.extenstion.formatMoney
import com.myetherwallet.mewconnect.core.extenstion.toStringWithoutZeroes
import com.myetherwallet.mewconnect.core.extenstion.viewModel
import com.myetherwallet.mewconnect.core.persist.prefenreces.PreferencesManager
import com.myetherwallet.mewconnect.core.ui.fragment.BaseViewModelFragment
import com.myetherwallet.mewconnect.feature.buy.activity.BuyWebViewActivity
import com.myetherwallet.mewconnect.feature.buy.viewmodel.BuyViewModel
import kotlinx.android.synthetic.main.fragment_buy.*
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Created by BArtWell on 12.09.2018.
 */

private const val CURRENCY_USD = "USD"
private const val CURRENCY_ETH = "ETH"
private const val ETH_DECIMALS = 8
private val LIMIT_MIN = BigDecimal(50)
private val LIMIT_MAX = BigDecimal(20000)
private const val EXTRA_STOCK_PRICE = "stock_price"

class BuyFragment : BaseViewModelFragment() {

    companion object {

        fun newInstance(stockPrice: BigDecimal): BuyFragment {
            val fragment = BuyFragment()
            val arguments = Bundle()
            arguments.putSerializable(EXTRA_STOCK_PRICE, stockPrice)
            fragment.arguments = arguments
            return fragment
        }
    }

    @Inject
    lateinit var preferences: PreferencesManager
    private lateinit var viewModel: BuyViewModel

    private var textSizeMin = 0f
    private var textSizeMax = 0f
    private var isInUsd = false
    private var price = BigDecimal.ZERO

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = viewModel(viewModelFactory)

        textSizeMin = resources.getDimension(R.dimen.text_size_fixed_20sp)
        textSizeMax = resources.getDimension(R.dimen.text_size_fixed_48sp)

        buy_toolbar.setNavigationIcon(R.drawable.ic_action_close)
        buy_toolbar.setNavigationOnClickListener(View.OnClickListener { close() })
        buy_toolbar.setTitle(R.string.buy_title)
        buy_toolbar.inflateMenu(R.menu.buy)
        buy_toolbar.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener {
            if (it.itemId == R.id.buy_history) {
                addFragment(HistoryFragment.newInstance())
                true
            } else {
                false
            }
        })

        price = arguments!!.getSerializable(EXTRA_STOCK_PRICE) as BigDecimal

        buy_button_1.setOnClickListener { addDigit(1) }
        buy_button_2.setOnClickListener { addDigit(2) }
        buy_button_3.setOnClickListener { addDigit(3) }
        buy_button_4.setOnClickListener { addDigit(4) }
        buy_button_5.setOnClickListener { addDigit(5) }
        buy_button_6.setOnClickListener { addDigit(6) }
        buy_button_7.setOnClickListener { addDigit(7) }
        buy_button_8.setOnClickListener { addDigit(8) }
        buy_button_9.setOnClickListener { addDigit(9) }
        buy_button_point.setOnClickListener { addPoint() }
        buy_button_0.setOnClickListener { addDigit(0) }
        buy_button_delete.setOnClickListener { delete() }

        buy_button_delete.setOnLongClickListener {
            populateMainValue(BigDecimal.ZERO)
            true
        }

        buy_toggle_currency.setOnClickListener {
            isInUsd = !isInUsd
            val tmp = buy_sum_1.text
            setRawText(buy_sum_2.text.toString())
            buy_sum_2.text = tmp
            populateSecondValue()
        }

        buy_button.setOnClickListener {
            buy_loading.visibility = VISIBLE
            viewModel.load(BigDecimal(getCurrentValue()),
                    if (isInUsd) CURRENCY_USD else CURRENCY_ETH,
                    preferences.getCurrentWalletPreferences().getWalletAddress(),
                    preferences.applicationPreferences.getInstallTime(),
                    {
                        startActivity(BuyWebViewActivity.createIntent(requireContext(), it.url, it.getEncodedPostData()))
                        buy_loading.visibility = GONE
                    },
                    {
                        Toast.makeText(context, R.string.buy_loading_error, Toast.LENGTH_LONG).show()
                    })
        }

        populateMainValue(BigDecimal.ZERO)
    }

    private fun populateMainValue(value: BigDecimal) {
        if (isInUsd) {
            setRawText(value.formatMoney(2))
        } else {
            setRawText(value.toStringWithoutZeroes())
        }
        populateSecondValue()
    }

    private fun populateSecondValue() {
        val text = getCurrentValue()
        if (isInUsd) {
            buy_sum_2.text = BigDecimal(text).divide(price, ETH_DECIMALS, RoundingMode.HALF_UP).formatMoney(ETH_DECIMALS)
        } else {
            buy_sum_2.text = BigDecimal(text).multiply(price).formatMoney(2)
        }
        populateCurrency()
    }

    private fun getCurrentValue() = buy_sum_1.text.toString()

    private fun populateCurrency() {
        if (isInUsd) {
            buy_currency_1.text = CURRENCY_USD
            buy_symbol_1.text = "$"
            buy_currency_2.text = CURRENCY_ETH
            buy_symbol_2.text = ""
        } else {
            buy_currency_1.text = CURRENCY_ETH
            buy_symbol_1.text = ""
            buy_currency_2.text = ""
            buy_symbol_2.text = "$"
        }
        setupBuyButton()
    }

    private fun setupBuyButton() {
        var currentValue = BigDecimal(getCurrentValue())
        if (!isInUsd) {
            currentValue = currentValue.multiply(price)
        }
        if (currentValue < LIMIT_MIN) {
            buy_button.setText(R.string.buy_minimum_warning)
            buy_button.isEnabled = false
        } else {
            buy_button.setText(R.string.buy_button)
            buy_button.isEnabled = true
        }
    }

    private fun delete() {
        var text = getCurrentValue()
        val length = text.length
        text = text.substring(0, length - 1)
        if (text.isEmpty()) {
            populateMainValue(BigDecimal.ZERO)
        } else {
            setRawText(text)
            populateSecondValue()
        }
    }

    private fun addPoint() {
        val text = getCurrentValue()
        val value = BigDecimal(text)
        if (value < LIMIT_MAX) {
            if (value.unscaledValue() == BigInteger.ZERO) {
                setRawText("0.")
                populateSecondValue()
            } else if (!text.contains(".")) {
                setRawText("$text.")
            }
        }
    }

    private fun setRawText(value: String) {
        buy_sum_1.text = value
        val delta = (textSizeMax - textSizeMin) * (value.length / 18f)
        val textSize = textSizeMax - delta
        buy_symbol_1.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        buy_sum_1.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
    }

    private fun addDigit(digit: Int) {
        var text = getCurrentValue()
        if (BigDecimal(text).unscaledValue() == BigInteger.ZERO && !text.contains(".")) {
            text = ""
        }
        val length = text.length
        val newValue = text + digit
        if (isInUsd) {
            if (length >= 3 && text.substring(length - 3, length - 2) == ".") { // If already has 2 decimals
                return
            }
            if (BigDecimal(newValue) > LIMIT_MAX) {
                populateMainValue(LIMIT_MAX)
                return
            }
        } else {
            if (length >= ETH_DECIMALS + 1 && text.substring(length - ETH_DECIMALS - 1, length - ETH_DECIMALS) == ".") { // If already has 18 decimals
                return
            }
            val limit = LIMIT_MAX.divide(price, ETH_DECIMALS, RoundingMode.HALF_UP)
            if (BigDecimal(newValue) > limit) {
                populateMainValue(limit)
                return
            }
        }
        setRawText(newValue)
        populateSecondValue()
    }

    override fun inject(appComponent: ApplicationComponent) {
        appComponent.inject(this)
    }

    override fun layoutId() = R.layout.fragment_buy
}