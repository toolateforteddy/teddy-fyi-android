package fyi.teddy.android.grocery.ui.components

/**
 * Maps a grocery item's name to a glyph, so "Bananas" shows up as 🍌 rather than as one
 * more line of text in a grid of identical tiles.
 *
 * This is a plain lookup on purpose. The app can categorise items with an on-device
 * model ([fyi.teddy.android.grocery.domain.ai.GroceryCategorizer]), but that model is
 * optional and usually absent, and a tile that only sometimes has a picture on it is
 * worse than one that never does. Unrecognised items fall back to their aisle's icon in
 * [ItemLeadingMark], so every tile still gets a mark.
 *
 * Keywords are matched against the words of the item name, so "Chicken Breast",
 * "chicken" and "2 lbs chicken" all land on the same glyph. Order matters: the first
 * entry whose keyword matches wins, so keep specific items above general ones.
 */
private val ITEM_GLYPHS: List<Pair<String, Set<String>>> = listOf(
    // Produce
    "🍌" to setOf("banana", "bananas"),
    "🍎" to setOf("apple", "apples"),
    "🍓" to setOf("strawberry", "strawberries"),
    "🫐" to setOf("blueberry", "blueberries", "raspberry", "raspberries", "blackberry", "blackberries"),
    "🍇" to setOf("grape", "grapes"),
    "🍊" to setOf("orange", "oranges", "clementine", "clementines", "tangerine", "mandarin"),
    "🍋" to setOf("lemon", "lemons", "lime", "limes"),
    "🍑" to setOf("peach", "peaches", "nectarine", "apricot"),
    "🍐" to setOf("pear", "pears"),
    "🍉" to setOf("watermelon", "melon", "cantaloupe"),
    "🍍" to setOf("pineapple"),
    "🥭" to setOf("mango", "mangos", "mangoes"),
    "🥑" to setOf("avocado", "avocados", "guacamole"),
    "🍅" to setOf("tomato", "tomatoes", "salsa"),
    "🥕" to setOf("carrot", "carrots"),
    "🥦" to setOf("broccoli", "cauliflower"),
    "🥬" to setOf("lettuce", "spinach", "kale", "cabbage", "arugula", "greens", "salad"),
    "🌽" to setOf("corn", "sweetcorn"),
    "🥔" to setOf("potato", "potatoes"),
    "🧅" to setOf("onion", "onions", "shallot", "shallots", "leek", "leeks"),
    "🧄" to setOf("garlic"),
    "🌶️" to setOf("pepper", "peppers", "chili", "chilli", "jalapeno"),
    "🥒" to setOf("cucumber", "cucumbers", "pickle", "pickles", "zucchini", "courgette"),
    "🍄" to setOf("mushroom", "mushrooms"),
    "🌿" to setOf("basil", "cilantro", "coriander", "parsley", "mint", "thyme", "rosemary", "herb", "herbs"),
    "🫚" to setOf("ginger"),

    // Dairy and eggs
    "🥛" to setOf("milk", "cream", "creamer", "buttermilk"),
    "🧀" to setOf("cheese", "cheddar", "parmesan", "mozzarella", "feta", "brie"),
    "🧈" to setOf("butter", "margarine"),
    "🥚" to setOf("egg", "eggs"),
    "🍦" to setOf("yogurt", "yoghurt", "icecream"),

    // Meat and protein
    "🍗" to setOf("chicken", "turkey", "poultry", "wings", "drumstick"),
    "🥩" to setOf("beef", "steak", "lamb", "mince", "ribs", "roast", "brisket"),
    "🥓" to setOf("bacon", "pork", "ham", "sausage", "sausages", "prosciutto", "salami"),
    "🐟" to setOf("fish", "salmon", "tuna", "cod", "tilapia", "halibut", "sardine", "sardines"),
    "🦐" to setOf("shrimp", "prawn", "prawns", "crab", "lobster", "scallop", "scallops"),
    "🫘" to setOf("bean", "beans", "lentil", "lentils", "chickpea", "chickpeas", "hummus", "tofu"),
    "🥜" to setOf("peanut", "peanuts", "almond", "almonds", "cashew", "cashews", "walnut", "walnuts", "nut", "nuts"),

    // Bakery and grains
    "🍞" to setOf("bread", "loaf", "baguette", "sourdough", "toast", "bun", "buns", "roll", "rolls"),
    "🥐" to setOf("croissant", "pastry", "pastries", "danish", "scone", "scones"),
    "🥯" to setOf("bagel", "bagels", "muffin", "muffins"),
    "🌮" to setOf("tortilla", "tortillas", "taco", "tacos", "wrap", "wraps", "pita"),
    "🍝" to setOf("pasta", "spaghetti", "penne", "noodle", "noodles", "ramen", "macaroni", "lasagna"),
    "🍚" to setOf("rice", "quinoa", "couscous", "barley"),
    "🥣" to setOf("cereal", "oat", "oats", "oatmeal", "granola", "muesli"),
    "🌾" to setOf("flour", "yeast"),

    // Pantry
    "🫒" to setOf("oil", "olive", "olives", "vinegar"),
    "🧂" to setOf("salt", "sugar", "spice", "spices", "seasoning", "cinnamon", "paprika"),
    "🍯" to setOf("honey", "syrup", "jam", "jelly", "marmalade", "nutella"),
    "🥫" to setOf("can", "canned", "soup", "sauce", "ketchup", "mustard", "mayo", "mayonnaise", "broth", "stock"),
    "🍫" to setOf("chocolate", "candy", "sweets", "cookie", "cookies", "biscuit", "biscuits", "cake", "dessert"),
    "🍿" to setOf("popcorn", "chip", "chips", "crisps", "crackers", "pretzel", "pretzels", "snack", "snacks"),

    // Drinks
    "☕" to setOf("coffee", "espresso", "latte"),
    "🍵" to setOf("tea", "matcha"),
    "🧃" to setOf("juice", "lemonade", "smoothie"),
    "🥤" to setOf("soda", "cola", "coke", "pop", "sprite", "seltzer", "sparkling"),
    "💧" to setOf("water"),
    "🍷" to setOf("wine", "prosecco", "champagne"),
    "🍺" to setOf("beer", "cider", "ale", "lager"),

    // Frozen and household
    "🧊" to setOf("ice", "frozen"),
    "🧻" to setOf("toilet", "paper", "towel", "towels", "tissue", "tissues", "napkin", "napkins"),
    "🧼" to setOf("soap", "shampoo", "detergent", "cleaner", "bleach", "sponge", "dish", "dishes", "laundry"),
    "🗑️" to setOf("trash", "bin", "bag", "bags", "garbage"),
    "🐕" to setOf("dog", "cat", "pet", "kibble", "litter"),
    "💊" to setOf("vitamin", "vitamins", "medicine", "advil", "tylenol", "ibuprofen"),
    "🪥" to setOf("toothpaste", "toothbrush", "floss", "deodorant"),
)

/**
 * The glyph for [itemName], or null when nothing matches.
 *
 * Matching is by whole word, so "buttermilk" is its own entry rather than half-matching
 * "butter". When a name matches more than one entry -- "orange juice" is both an orange
 * and a juice -- the earlier entry in [ITEM_GLYPHS] wins, which is why the list is
 * ordered specific-first.
 */
fun glyphForItem(itemName: String): String? {
    val words = itemName.lowercase()
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotBlank() }
        .toSet()
    if (words.isEmpty()) return null

    for ((glyph, keywords) in ITEM_GLYPHS) {
        if (words.any { it in keywords }) return glyph
    }
    return null
}
