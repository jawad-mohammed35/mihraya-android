package com.mihraya.app.feature.more

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.mihraya.app.core.designsystem.components.*
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.feature.settings.SettingsScaffold
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Date

fun copyMore(context: Context, text: String) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Mihraya", text))
}
fun shareMore(context: Context, text: String) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share with"))
}
@Composable
internal fun ToolText(text: String, accent: Boolean = false) {
    Text(text, style = Mihraya.type.body, color = if (accent) Mihraya.colors.accent else Mihraya.colors.textSecondary)
}
@Composable
internal fun ToolTitle(text: String) { Text(text, style = Mihraya.type.title, color = Mihraya.colors.textPrimary) }
@Composable
internal fun ToolCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { ToolTitle(title); content() }
    }
}
@Composable
fun ZakatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { MoreStore(context) }
    var draft by rememberSaveable { mutableStateOf(Json.encodeToString(ZakatInput.serializer(), store.initialZakat())) }
    val input = remember(draft) { Json.decodeFromString<ZakatInput>(draft) }
    fun change(value: ZakatInput) { draft = Json.encodeToString(ZakatInput.serializer(), value) }
    var savedAt by remember { mutableLongStateOf(store.savedAt()) }
    var message by rememberSaveable { mutableStateOf("") }
    var reset by rememberSaveable { mutableStateOf(false) }
    var currencySheet by remember { mutableStateOf(false) }
    var pendingCurrency by rememberSaveable { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val result = remember(input) { ZakatCalculator.calculate(input) }
    val formatter = remember(input.currency) { NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(input.currency) } }
    fun money(value: BigDecimal) = formatter.format(value)
    val summary = result?.let {
        "Mihraya · Zakat estimate (${input.currency})\nAssets: ${money(it.assets)}\nEligible deductions: ${money(it.deductions)}\nNet Zakatable wealth: ${money(it.net)}\n${input.method} Nisab: ${money(it.nisab)}\nMeets Nisab: ${if (it.meetsNisab) "Yes" else "No"}\nHawl met: ${if (input.hawl) "Yes" else "Not confirmed"}\nEstimated Zakat due (2.5%): ${money(it.due)}\nGold: ${input.goldPrice.ifBlank { "Not entered" }} ${input.currency}/g · ${input.goldSource} · ${if (input.goldUpdated > 0) Date(input.goldUpdated) else "undated"}\nSilver: ${input.silverPrice.ifBlank { "Not entered" }} ${input.currency}/g · ${input.silverSource} · ${if (input.silverUpdated > 0) Date(input.silverUpdated) else "undated"}\nGold purity: ${input.goldKarat}K; personal jewelry included: ${input.includeJewelry}. Investment input is the user-assessed eligible portion. Deductions are user-assessed short-term obligations.\nEstimate only. Rules vary by school and circumstances; consult a qualified scholar."
    }
    SettingsScaffold("Zakat Calculator", onBack) {
        ToolCard("Your annual estimate") {
            Text(result?.let { money(it.due) } ?: "—", style = Mihraya.type.headline, color = Mihraya.colors.accent)
            when {
                result == null -> ToolText("Enter required prices and valid amounts.")
                !input.hawl -> ToolText("Hawl not confirmed")
                !result.meetsNisab -> ToolText("Below Nisab")
            }
            SecondaryButton("Currency · ${input.currency}", { currencySheet = true }, Modifier.fillMaxWidth())
            SegmentedControl(Nisab.entries.map { "${it.name} Nisab" }, input.method.ordinal, { change(input.copy(method = Nisab.entries[it])); store.preference(input.copy(method = Nisab.entries[it])) })
            ToolText("Gold: 87.48 g · Silver: 612.36 g of pure metal. Select the standard advised by your scholar.")
        }
        ToolCard("Metal prices · ${input.currency} per gram") {
            ToolText("Enter pure-metal prices from your chosen source. No market price is assumed.")
            MoneyField("Gold price / gram", input.goldPrice) { change(input.copy(goldPrice = it, goldSource = "Manual entry", goldUpdated = System.currentTimeMillis())) }
            QuoteNote(input.goldSource, input.goldUpdated)
            MoneyField("Silver price / gram", input.silverPrice) { change(input.copy(silverPrice = it, silverSource = "Manual entry", silverUpdated = System.currentTimeMillis())) }
            QuoteNote(input.silverSource, input.silverUpdated)
            if (input.currency == "USD") {
                PrimaryButton("Refresh market quotes", {
                    refreshing = true
                    scope.launch {
                        try {
                            val quotes = coroutineScope { val gold = async { MetalPrices.fetch("XAU") }; val silver = async { MetalPrices.fetch("XAG") }; gold.await() to silver.await() }
                            // Do not overwrite edits or a currency change made while the request was running.
                            if (Json.decodeFromString<ZakatInput>(draft) == input) {
                                change(input.copy(goldPrice = quotes.first.pricePerGram, silverPrice = quotes.second.pricePerGram,
                                    goldSource = MetalPrices.SOURCE, silverSource = MetalPrices.SOURCE, goldUpdated = quotes.first.updated, silverUpdated = quotes.second.updated))
                                message = "Quotes received. Check the source timestamps below each price."
                            } else message = "Inputs changed. Refresh again to apply market prices."
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { message = "Market pricing unavailable. Existing prices are unchanged; enter prices manually." }
                        finally { refreshing = false }
                    }
                }, Modifier.fillMaxWidth(), enabled = !refreshing, loading = refreshing)
            } else ToolText("Market refresh is available in USD. Enter prices manually in ${input.currency}; amounts are never converted automatically.")
        }
        ToolCard("Cash & eligible assets · ${input.currency}") {
            MoneyField("Cash on hand", input.cash) { change(input.copy(cash = it)) }
            MoneyField("Bank balances & savings", input.bank) { change(input.copy(bank = it)) }
            MoneyField("Eligible investment portion", input.investments) { change(input.copy(investments = it)) }
            ToolText("For trading holdings, usually use market value. For long-term shares or funds, enter the Zakatable portion advised by your scholar or fund provider; do not count it again as cash or business assets.")
            MoneyField("Business inventory / trade goods", input.business) { change(input.copy(business = it)) }
            MoneyField("Recoverable money owed to you", input.receivables) { change(input.copy(receivables = it)) }
            MoneyField("Other eligible assets", input.other) { change(input.copy(other = it)) }
        }
        ToolCard("Gold & silver holdings") {
            UnitPicker(input.unit) { change(input.copy(unit = it)) }
            ToolText("Changing the unit reinterprets entered weights. Silver weights must be fine-silver equivalents (weight × purity). Gold weights use the selected karat.")
            SegmentedControl(listOf("18K", "21K", "22K", "24K"), listOf(18,21,22,24).indexOf(input.goldKarat), { change(input.copy(goldKarat = listOf(18,21,22,24)[it])) })
            MoneyField("Gold bullion / investment weight", input.gold) { change(input.copy(gold = it)) }
            MoneyField("Silver bullion / investment weight", input.silver) { change(input.copy(silver = it)) }
            SettingsSwitchRow("Include personal jewelry", input.includeJewelry, { change(input.copy(includeJewelry = it)) })
            ToolText("Scholars differ on personal-use jewelry. Choose the treatment you follow. Jewelry weights below are separate from bullion; use the same gold purity or enter an equivalent weight.")
            if (input.includeJewelry) {
                MoneyField("Personal gold jewelry weight", input.jewelryGold) { change(input.copy(jewelryGold = it)) }
                MoneyField("Personal silver jewelry weight", input.jewelrySilver) { change(input.copy(jewelrySilver = it)) }
            }
        }
        ToolCard("Deductions & holding period") {
            MoneyField("Eligible short-term deductions · ${input.currency}", input.deductions) { change(input.copy(deductions = it)) }
            ToolText("Enter immediately payable debts and qualifying expenses only, following your chosen scholarly guidance. Do not deduct an entire mortgage or other long-term loan automatically. Do not count an expense twice.")
            SettingsSwitchRow("Hawl requirement has been met", input.hawl, { change(input.copy(hawl = it)) })
            ToolText("Confirm one lunar year where required for these assets. Asset-specific rules and changes in wealth can affect Hawl; this tool does not determine them automatically.")
        }
        ToolCard("Calculation breakdown") {
            if (result == null) ToolText("Enter a positive price for your Nisab metal and each metal you hold. All inputs must be nonnegative numbers, up to 15 whole digits and 6 decimal places.")
            else {
                ToolText("Total eligible assets: ${money(result.assets)}")
                ToolText("Eligible deductions: ${money(result.deductions)}")
                ToolText("Net Zakatable wealth: ${money(result.net)}", true)
                ToolText("${input.method} Nisab threshold: ${money(result.nisab)}")
                ToolText(if (result.meetsNisab) "Meets Nisab" else "Below Nisab", true)
                ToolTitle("Estimated Zakat due: ${money(result.due)}")
                ToolText(if (!input.hawl) "Hawl is not confirmed, so no amount is shown as due." else "Net wealth × 2.5%, if Nisab is met. Rounded to two decimal places.")
                SecondaryButton("Copy summary", { copyMore(context, summary!!) }, Modifier.fillMaxWidth())
                SecondaryButton("Share summary", { shareMore(context, summary!!) }, Modifier.fillMaxWidth())
            }
            PrimaryButton("Save calculation locally", { store.saveZakat(input); savedAt = store.savedAt(); message = "Calculation saved on this device." }, Modifier.fillMaxWidth())
            if (savedAt > 0) SecondaryButton("Load saved calculation", { val saved = store.loadZakat(); change(saved); store.preference(saved); message = "Saved calculation restored." }, Modifier.fillMaxWidth())
            if (savedAt > 0) ToolText("Last saved: ${Date(savedAt)}. Unsaved edits do not replace your saved calculation.")
            SecondaryButton("Reset inputs", { reset = true }, Modifier.fillMaxWidth())
            if (message.isNotBlank()) ToolText(message, true)
        }
    }
    if (reset) MihrayaDialog(title = "Reset current inputs?", message = "Your saved calculation and Nisab/currency preferences remain available. Save after resetting only if you want to replace the saved calculation.", onDismiss = { reset = false }, confirmText = "Reset", onConfirm = {
        change(ZakatInput(method = input.method, currency = input.currency)); message = "Current inputs cleared."; reset = false
    })
    if (currencySheet) OptionSheet("Currency", listOf("USD", "EUR", "GBP", "CAD", "INR", "SAR", "AED").map { SheetOption(it, it) }, input.currency,
        { if (it != input.currency) pendingCurrency = it; currencySheet = false }, { currencySheet = false })
    pendingCurrency?.let { currency ->
        MihrayaDialog(title = "Change currency to $currency?", message = "Current inputs and prices will be cleared to avoid mixing currencies. No automatic conversion is performed. Your saved calculation remains until you save again.", onDismiss = { pendingCurrency = null }, confirmText = "Change", onConfirm = {
            val updated = ZakatInput(method = input.method, currency = currency); change(updated); store.preference(updated); pendingCurrency = null
        })
    }
}
@Composable
private fun MoneyField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        MihrayaTextField(value, { if (it.length <= 22) onChange(it) }, label, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        if (amount(value) == null) ToolText("Use a nonnegative number with a decimal point, without commas.")
    }
}
@Composable
private fun QuoteNote(source: String, updated: Long) {
    // Recheck freshness while the screen stays open, including crossing the 24-hour boundary.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(updated) { while (true) { now = System.currentTimeMillis(); delay(60_000) } }
    ToolText(if (updated == 0L) "$source · no price entered" else "$source · ${Date(updated)} · ${if (source == "Manual entry") "user-entered" else if (MetalPrices.isFresh(updated, now)) "recent market quote (not streaming)" else "dated quote — refresh or replace before relying on it"}")
}
@Composable
private fun UnitPicker(unit: MetalUnit, onSelect: (MetalUnit) -> Unit) {
    var open by remember { mutableStateOf(false) }
    SecondaryButton("Weight unit · ${unit.label}", { open = true }, Modifier.fillMaxWidth())
    if (open) OptionSheet("Weight unit", MetalUnit.entries.map { SheetOption(it, it.label) }, unit, { onSelect(it); open = false }, { open = false })
}
