package com.mihraya.app.feature.more

import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.serialization.Serializable

val ZERO: BigDecimal = BigDecimal.ZERO
fun amount(text: String): BigDecimal? = when {
    text.isBlank() -> ZERO
    !text.matches(Regex("[0-9]{1,15}(\\.[0-9]{0,6})?")) -> null
    else -> text.toBigDecimalOrNull()
}
enum class Nisab(val grams: BigDecimal) { Gold(BigDecimal("87.48")), Silver(BigDecimal("612.36")) }
enum class MetalUnit(val label: String, val grams: BigDecimal) {
    Grams("Grams", BigDecimal.ONE), TroyOunces("Troy ounces", BigDecimal("31.1034768")), Tola("Tola", BigDecimal("11.6638038"))
}
@Serializable
data class ZakatInput(
    val cash: String = "", val bank: String = "", val gold: String = "", val silver: String = "",
    val investments: String = "", val business: String = "", val receivables: String = "", val other: String = "",
    val deductions: String = "", val goldPrice: String = "", val silverPrice: String = "",
    val method: Nisab = Nisab.Silver, val currency: String = "USD", val unit: MetalUnit = MetalUnit.Grams,
    val goldKarat: Int = 24, val includeJewelry: Boolean = true, val jewelryGold: String = "", val jewelrySilver: String = "",
    val hawl: Boolean = false, val goldSource: String = "Manual entry", val silverSource: String = "Manual entry",
    val goldUpdated: Long = 0, val silverUpdated: Long = 0,
)
data class ZakatResult(val assets: BigDecimal, val deductions: BigDecimal, val net: BigDecimal, val nisab: BigDecimal, val meetsNisab: Boolean, val due: BigDecimal)
object ZakatCalculator {
    fun calculate(input: ZakatInput): ZakatResult? {
        val monetary = listOf(input.cash, input.bank, input.investments, input.business, input.receivables, input.other, input.deductions)
            .map { amount(it) ?: return null }
        val gold = amount(input.gold) ?: return null
        val silver = amount(input.silver) ?: return null
        val jewelryGold = if (input.includeJewelry) amount(input.jewelryGold) ?: return null else ZERO
        val jewelrySilver = if (input.includeJewelry) amount(input.jewelrySilver) ?: return null else ZERO
        val gp = amount(input.goldPrice) ?: return null
        val sp = amount(input.silverPrice) ?: return null
        if ((input.method == Nisab.Gold || gold + jewelryGold > ZERO) && gp <= ZERO) return null
        if ((input.method == Nisab.Silver || silver + jewelrySilver > ZERO) && sp <= ZERO) return null
        if (input.goldKarat !in listOf(18, 21, 22, 24)) return null
        val goldValue = ((gold + jewelryGold) * input.unit.grams * BigDecimal(input.goldKarat) * gp)
            .divide(BigDecimal(24), 12, RoundingMode.HALF_UP)
        val silverValue = (silver + jewelrySilver) * input.unit.grams * sp
        val assets = monetary.take(6).fold(ZERO, BigDecimal::add) + goldValue + silverValue
        val deductions = monetary.last()
        val net = (assets - deductions).max(ZERO)
        val threshold = input.method.grams * if (input.method == Nisab.Gold) gp else sp
        val meets = net >= threshold
        return ZakatResult(assets, deductions, net, threshold, meets,
            if (meets && input.hawl) (net * BigDecimal("0.025")).setScale(2, RoundingMode.HALF_UP) else ZERO)
    }
}
