package fyi.teddy.android.grocery.data

enum class StandardUnit(val label: String) {
    PCS("pcs"),
    LBS("lbs"),
    OZ("oz"),
    G("g"),
    KG("kg"),
    ML("ml"),
    L("L"),
    CANS("cans"),
    PACKS("packs"),
    BOTTLES("bottles"),
    BAGS("bags");

    companion object {
        val labels: List<String> = entries.map { it.label }
    }
}
