package com.kumbarakala.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.SoundEffectConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PotteryItem(
    val id: Int,
    var name: String,
    var nameKn: String,
    var imageRes: Int,
    var quantity: Int,
    var price: String,
    var origin: String,
    var originKn: String,
    var category: String,
    var categoryKn: String,
    var shortDesc: String,
    var shortDescKn: String,
    val healthBenefits: List<String>,
    val healthBenefitsKn: List<String>,
    val ecoBenefits: List<String>,
    val ecoBenefitsKn: List<String>,
    val rating: Float = 4.8f,
    val reviewCount: Int = 124
)

data class ArtisanMember(
    val name: String,
    val village: String,
    val phone: String,
    val email: String,
    val specialization: String,
    val imageRes: Int
)

data class OrderRecord(
    val orderId: String,
    val customerName: String,
    val items: List<Pair<PotteryItem, Int>>,
    val total: Int,
    val status: String = "Paid"
)

data class ProductOption(
    val name: String,
    val nameKn: String,
    val imageRes: Int,
    val category: String,
    val categoryKn: String,
    val description: String,
    val descriptionKn: String
)

class ArtisanViewModel : ViewModel() {
    var isKannada by mutableStateOf(false)
    var isLoggedIn by mutableStateOf(false)
    var userRole by mutableStateOf("")
    var showAdminLoginDialog by mutableStateOf(false)

    var artisanName by mutableStateOf("Disha N")
    var artisanVillage by mutableStateOf("Channapatna Heritage Village")
    var artisanPhone by mutableStateOf("+91 91000 22233")
    var artisanEmail by mutableStateOf("manyashrikant@gmail.com")
    var tagline by mutableStateOf("Preserving the Earth's Soul through Clay")
    var heritageBackground by mutableStateOf("Third-generation artisan in the Channapatna tradition.")
    var craftBio by mutableStateOf("Specializing in high-fire terracotta and natural pit-firing glazing.")

    var customerName by mutableStateOf("Disha N (Buyer)")
    var customerEmail by mutableStateOf("disha.buyer@gmail.com")

    val savedDesigns = mutableStateListOf<PotteryItem>()
    val cartItems = mutableStateListOf<PotteryItem>()
    val orders = mutableStateListOf<OrderRecord>()

    var showPaymentQR by mutableStateOf(false)
    var selectedTab by mutableIntStateOf(0)
    var selectedArtifact by mutableStateOf<PotteryItem?>(null)
    var showProductDialog by mutableStateOf(false)
    var editingProduct by mutableStateOf<PotteryItem?>(null)

    fun text(en: String, kn: String): String = if (isKannada) kn else en

    fun appFont(size: Int): androidx.compose.ui.unit.TextUnit =
        if (isKannada) (size - 2).coerceAtLeast(10).sp else size.sp

    val categories = listOf("All", "Water Pots", "Cooking", "Lighting", "Fermentation")
    val categoriesKn = listOf("ಎಲ್ಲಾ", "ನೀರಿನ ಮಡಿಕೆ", "ಅಡುಗೆ", "ದೀಪಗಳು", "ಮೊಸರು ಮಡಿಕೆ")

    fun categoryLabel(category: String): String {
        val index = categories.indexOf(category)
        return if (isKannada && index >= 0) categoriesKn[index] else category
    }

    val productOptions = listOf(
        ProductOption("Heritage Matka", "ಪಾರಂಪರಿಕ ಮಡಿಕೆ", R.drawable.matka, "Water Pots", "ನೀರಿನ ಮಡಿಕೆ", "Traditional clay pot that naturally cools water.", "ನೀರನ್ನು ಸಹಜವಾಗಿ ತಂಪಾಗಿಡುವ ಪಾರಂಪರಿಕ ಮಣ್ಣಿನ ಮಡಿಕೆ."),
        ProductOption("Clay Handi", "ಮಣ್ಣಿನ ಹಂಡಿ", R.drawable.handi, "Cooking", "ಅಡುಗೆ", "Handcrafted slow-cooking pot for authentic flavor preservation.", "ಸಾಂಪ್ರದಾಯಿಕ ರುಚಿಯನ್ನು ಉಳಿಸುವ ಕೈಯಿಂದ ಮಾಡಿದ ಅಡುಗೆ ಹಂಡಿ."),
        ProductOption("Deepak Lamp", "ಮಣ್ಣಿನ ದೀಪ", R.drawable.lamp, "Lighting", "ದೀಪಗಳು", "Artisanal clay lamps for sacred and festive lighting.", "ಪೂಜೆ ಮತ್ತು ಹಬ್ಬಗಳಿಗಾಗಿ ಕೈಯಿಂದ ಮಾಡಿದ ಮಣ್ಣಿನ ದೀಪ."),
        ProductOption("Authentic Curd Pot", "ಮೊಸರು ಮಡಿಕೆ", R.drawable.curd_pot, "Fermentation", "ಮೊಸರು ಮಡಿಕೆ", "Porous clay walls perfect for natural curd fermentation.", "ಸಹಜ ಮೊಸರು ತಯಾರಿಕೆಗೆ ಸೂಕ್ತವಾದ ಮಣ್ಣಿನ ಮಡಿಕೆ.")
    )

    val myProducts = mutableStateListOf(
        PotteryItem(1, "Heritage Matka", "ಪಾರಂಪರಿಕ ಮಡಿಕೆ", R.drawable.matka, 15, "₹250", "Mysuru", "ಮೈಸೂರು", "Water Pots", "ನೀರಿನ ಮಡಿಕೆ", "Traditional clay pot that naturally cools water.", "ನೀರನ್ನು ಸಹಜವಾಗಿ ತಂಪಾಗಿಡುವ ಪಾರಂಪರಿಕ ಮಣ್ಣಿನ ಮಡಿಕೆ.", listOf("Maintains pH balance", "Natural alkaline cooling"), listOf("pH ಸಮತೋಲನ ಕಾಪಾಡುತ್ತದೆ", "ಸಹಜ ತಂಪು ನೀಡುತ್ತದೆ"), listOf("100% Biodegradable", "Plastic-free alternative"), listOf("100% ಜೈವಿಕವಾಗಿ ಕರಗುತ್ತದೆ", "ಪ್ಲಾಸ್ಟಿಕ್‌ಗೆ ಉತ್ತಮ ಪರ್ಯಾಯ")),
        PotteryItem(2, "Clay Handi", "ಮಣ್ಣಿನ ಹಂಡಿ", R.drawable.handi, 8, "₹450", "Channapatna", "ಚನ್ನಪಟ್ಟಣ", "Cooking", "ಅಡುಗೆ", "Handcrafted slow-cooking pot for authentic flavor preservation.", "ಸಾಂಪ್ರದಾಯಿಕ ರುಚಿಯನ್ನು ಉಳಿಸುವ ಕೈಯಿಂದ ಮಾಡಿದ ಅಡುಗೆ ಹಂಡಿ.", listOf("Retains vitamins", "Chemical-free cooking"), listOf("ಪೋಷಕಾಂಶ ಉಳಿಸುತ್ತದೆ", "ರಾಸಾಯನಿಕರಹಿತ ಅಡುಗೆ"), listOf("Earth-friendly clay", "Sustainable craft"), listOf("ಪ್ರಕೃತಿ ಸ್ನೇಹಿ ಮಣ್ಣು", "ಸ್ಥಿರವಾದ ಕೈಗಾರಿಕೆ")),
        PotteryItem(3, "Deepak Lamp", "ಮಣ್ಣಿನ ದೀಪ", R.drawable.lamp, 12, "₹50", "Bengaluru", "ಬೆಂಗಳೂರು", "Lighting", "ದೀಪಗಳು", "Artisanal clay lamps for sacred and festive lighting.", "ಪೂಜೆ ಮತ್ತು ಹಬ್ಬಗಳಿಗಾಗಿ ಕೈಯಿಂದ ಮಾಡಿದ ಮಣ್ಣಿನ ದೀಪ.", listOf("Natural glow", "Non-toxic materials"), listOf("ಸಹಜ ಬೆಳಕು", "ವಿಷರಹಿತ ವಸ್ತುಗಳು"), listOf("Zero waste", "Hand-molded"), listOf("ಶೂನ್ಯ ತ್ಯಾಜ್ಯ", "ಕೈಯಿಂದ ರೂಪಿಸಲಾಗಿದೆ")),
        PotteryItem(4, "Authentic Curd Pot", "ಮೊಸರು ಮಡಿಕೆ", R.drawable.curd_pot, 5, "₹180", "Mysuru", "ಮೈಸೂರು", "Fermentation", "ಮೊಸರು ಮಡಿಕೆ", "Porous clay walls perfect for natural curd fermentation.", "ಸಹಜ ಮೊಸರು ತಯಾರಿಕೆಗೆ ಸೂಕ್ತವಾದ ಮಣ್ಣಿನ ಮಡಿಕೆ.", listOf("Natural probiotics", "Enhanced creamy texture"), listOf("ಸಹಜ ಪ್ರೊಬೈಯಾಟಿಕ್ಸ್", "ಹೆಚ್ಚು ಮೃದುವಾದ ಗುಣ"), listOf("Micro-plastic free", "Eco-conscious"), listOf("ಮೈಕ್ರೋ ಪ್ಲಾಸ್ಟಿಕ್ ಇಲ್ಲ", "ಪರಿಸರ ಸ್ನೇಹಿ"))
    )

    val artisanList = mutableStateListOf(
        ArtisanMember(
            "Ramesh Kumbara",
            "Kumbalgodu, Karnataka",
            "+91 92000 11122",
            "ramesh@kumbara.com",
            "Water pots, cooking pots\nExperience: 45+ years family tradition\nBio: I am a third-generation potter from Kumbalgodu. My family has been crafting clay pots for over 45 years using traditional techniques.\nMessage: Made with natural clay, safe for your family",
            R.drawable.handi
        ),
        ArtisanMember(
            "Lakshmi Amma",
            "Channapatna, Karnataka",
            "+91 91000 22233",
            "lakshmi@kumbara.com",
            "Diyas, decorative lamps\nExperience: 30+ years\nBio: I have been making clay diyas and decorative items for festivals for over 30 years. Each piece is handmade with care.\nMessage: Light your home with eco-friendly tradition",
            R.drawable.lamp
        ),
        ArtisanMember(
            "Manjunath Gowda",
            "Ramanagara, Karnataka",
            "+91 98765 43210",
            "manjunath@kumbara.com",
            "Designer pots, planters\nExperience: 15 years\nBio: I learned pottery from my father and now combine traditional skills with modern designs for everyday use.\nMessage: Tradition meets modern living",
            R.drawable.matka
        ),
        ArtisanMember(
            "Savithri Bai",
            "Maddur, Karnataka",
            "+91 86189 33445",
            "savithri@kumbara.com",
            "Curd pots, kitchen utensils\nExperience: 25 years\nBio: I specialize in making curd pots and kitchen items that help maintain natural taste and health.\nMessage: Healthy living starts with natural materials",
            R.drawable.curd_pot
        ),
        ArtisanMember(
            "Shankar Kumbara",
            "Tumakuru, Karnataka",
            "+91 94810 77889",
            "shankar@kumbara.com",
            "Water storage pots, garden items\nExperience: 35 years\nBio: I create large water storage pots using eco-friendly clay. My work supports sustainable and plastic-free living.\nMessage: Choose clay, choose sustainability",
            R.drawable.matka
        )
    )

    fun itemName(item: PotteryItem) = if (isKannada) item.nameKn else item.name
    fun itemOrigin(item: PotteryItem) = if (isKannada) item.originKn else item.origin
    fun itemCategory(item: PotteryItem) = if (isKannada) item.categoryKn else item.category
    fun itemDesc(item: PotteryItem) = if (isKannada) item.shortDescKn else item.shortDesc
    fun itemHealth(item: PotteryItem) = if (isKannada) item.healthBenefitsKn else item.healthBenefits
    fun itemEco(item: PotteryItem) = if (isKannada) item.ecoBenefitsKn else item.ecoBenefits
    fun optionName(option: ProductOption) = if (isKannada) option.nameKn else option.name
    fun optionCategory(option: ProductOption) = if (isKannada) option.categoryKn else option.category
    fun optionDesc(option: ProductOption) = if (isKannada) option.descriptionKn else option.description

    fun logout() {
        selectedTab = 0
        selectedArtifact = null
        showPaymentQR = false
        isLoggedIn = false
    }

    fun addToCart(item: PotteryItem): Boolean {
        if (item.quantity <= cartItems.count { it.id == item.id }) return false
        cartItems.add(item)
        return true
    }

    fun cartQuantity(item: PotteryItem): Int = cartItems.count { it.id == item.id }

    fun removeOneFromCart(item: PotteryItem) {
        cartItems.firstOrNull { it.id == item.id }?.let { cartItems.remove(it) }
    }

    fun removeAllFromCart(item: PotteryItem) {
        cartItems.removeAll { it.id == item.id }
    }

    fun cartGroups(): List<Pair<PotteryItem, Int>> =
        cartItems.groupBy { it.id }.map { (_, list) -> list.first() to list.size }

    fun cartSubtotal(): Int = cartGroups().sumOf { (item, qty) -> item.priceValue() * qty }

    fun deliveryFee(): Int = if (cartItems.isEmpty()) 0 else 40

    fun cartTotal(): Int = cartSubtotal() + deliveryFee()

    fun completePayment() {
        val groupedItems = cartGroups()
        if (groupedItems.isEmpty()) return

        groupedItems.forEach { (item, qty) ->
            item.quantity = (item.quantity - qty).coerceAtLeast(0)
        }

        orders.add(
            0,
            OrderRecord(
                "KK-${System.currentTimeMillis().toString().takeLast(6)}",
                customerName,
                groupedItems,
                cartTotal()
            )
        )

        cartItems.clear()
        showPaymentQR = false
        selectedTab = 2
    }

    fun saveOrUpdateProduct(selectedOption: ProductOption, price: String, stock: String, origin: String) {
        val cleanStock = stock.toIntOrNull() ?: 0

        editingProduct?.let {
            it.name = selectedOption.name
            it.nameKn = selectedOption.nameKn
            it.imageRes = selectedOption.imageRes
            it.price = price
            it.quantity = cleanStock
            it.origin = origin
            it.originKn = origin
            it.category = selectedOption.category
            it.categoryKn = selectedOption.categoryKn
            it.shortDesc = selectedOption.description
            it.shortDescKn = selectedOption.descriptionKn
        } ?: run {
            val newId = (myProducts.maxOfOrNull { it.id } ?: 0) + 1

            myProducts.add(
                PotteryItem(
                    newId,
                    selectedOption.name,
                    selectedOption.nameKn,
                    selectedOption.imageRes,
                    cleanStock,
                    price,
                    origin,
                    origin,
                    selectedOption.category,
                    selectedOption.categoryKn,
                    selectedOption.description,
                    selectedOption.descriptionKn,
                    listOf("Handmade with natural clay", "Chemical-free craft"),
                    listOf("ಸಹಜ ಮಣ್ಣಿನಿಂದ ಕೈಯಿಂದ ಮಾಡಲಾಗಿದೆ", "ರಾಸಾಯನಿಕರಹಿತ ಕಲೆ"),
                    listOf("Biodegradable", "Supports local artisans"),
                    listOf("ಜೈವಿಕವಾಗಿ ಕರಗುತ್ತದೆ", "ಸ್ಥಳೀಯ ಕುಶಲಕರ್ಮಿಗಳಿಗೆ ಬೆಂಬಲ")
                )
            )
        }

        editingProduct = null
        showProductDialog = false
    }
}

fun PotteryItem.priceValue(): Int =
    price.filter { it.isDigit() }.toIntOrNull() ?: 0
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KumbaraTheme {
                val vm: ArtisanViewModel = viewModel()
                Surface(Modifier.fillMaxSize(), color = Color(0xFFFDFCF9)) {
                    AnimatedContent(targetState = vm.isLoggedIn, label = "auth_flow") { loggedIn ->
                        if (!loggedIn) OriginalRoleLogin(vm) else MainContentRouter(vm)
                    }
                }
            }
        }
    }
}

@Composable
fun OriginalRoleLogin(vm: ArtisanViewModel) {
    var adminCode by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    if (vm.showAdminLoginDialog) {
        AlertDialog(
            onDismissRequest = { vm.showAdminLoginDialog = false },
            title = {
                Text(
                    vm.text("Admin Verification", "ನಿರ್ವಾಹಕ ಪರಿಶೀಲನೆ"),
                    fontWeight = FontWeight.Black,
                    fontSize = vm.appFont(20)
                )
            },
            text = {
                Column {
                    Text(
                        vm.text("Enter secure code to access dashboard", "ಡ್ಯಾಶ್‌ಬೋರ್ಡ್ ತೆರೆಯಲು ಸುರಕ್ಷತಾ ಕೋಡ್ ನಮೂದಿಸಿ"),
                        color = Color.Gray,
                        fontSize = vm.appFont(14)
                    )
                    OutlinedTextField(
                        value = adminCode,
                        onValueChange = {
                            adminCode = it
                            errorText = ""
                        },
                        label = {
                            Text(vm.text("Admin Code", "ನಿರ್ವಾಹಕ ಕೋಡ್"), fontSize = vm.appFont(13))
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.padding(top = 10.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    if (errorText.isNotEmpty()) {
                        Text(
                            errorText,
                            color = Color.Red,
                            fontSize = vm.appFont(12),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (adminCode == "1234") {
                            vm.userRole = "Admin"
                            vm.selectedTab = 0
                            vm.isLoggedIn = true
                            vm.showAdminLoginDialog = false
                        } else {
                            errorText = vm.text("Incorrect code. Access denied.", "ತಪ್ಪಾದ ಕೋಡ್. ಪ್ರವೇಶ ನಿರಾಕರಿಸಲಾಗಿದೆ.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB35A2C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(vm.text("Enter", "ಪ್ರವೇಶಿಸಿ"), fontSize = vm.appFont(14))
                }
            },
            containerColor = Color(0xFFFFFBF7)
        )
    }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.hero_image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF2B160D).copy(alpha = 0.30f))
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2B160D).copy(alpha = 0.18f),
                            Color(0xFF2B160D).copy(alpha = 0.08f),
                            Color(0xFF2B160D).copy(alpha = 0.88f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 116.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    vm.text("KUMBARA-KALA", "ಕುಂಬಾರ-ಕಲಾ"),
                    fontSize = vm.appFont(36),
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1
                )

                Text(
                    vm.text(
                        "HANDMADE CLAY • HERITAGE CRAFT • EARTH FIRST",
                        "ಕೈಯಿಂದ ಮಾಡಿದ ಮಣ್ಣು • ಪರಂಪರೆ • ಪ್ರಕೃತಿ ಸ್ನೇಹಿ"
                    ),
                    fontSize = vm.appFont(11),
                    color = Color(0xFFFFE0B2),
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    vm.text(
                        "A warm marketplace for artisans, pottery lovers, and heritage craft.",
                        "ಕುಶಲಕರ್ಮಿಗಳು, ಮಣ್ಣಿನ ಕಲಾ ಪ್ರಿಯರು ಮತ್ತು ಪರಂಪರೆಯಿಗಾಗಿ ಆತ್ಮೀಯ ಮಾರುಕಟ್ಟೆ."
                    ),
                    fontSize = vm.appFont(14),
                    color = Color.White.copy(alpha = 0.92f),
                    lineHeight = vm.appFont(19)
                )
            }

            Spacer(Modifier.height(22.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFFFFFBF7).copy(alpha = 0.94f),
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    LoginRoleCard(
                        title = vm.text("Artisan Login", "ಕುಶಲಕರ್ಮಿ ಲಾಗಿನ್"),
                        subtitle = vm.text("Manage products, stock, and orders", "ಉತ್ಪನ್ನ, ಸಂಗ್ರಹ ಮತ್ತು ಆರ್ಡರ್ ನಿರ್ವಹಿಸಿ"),
                        icon = Icons.Default.Edit,
                        accent = Color(0xFFB35A2C),
                        vm = vm
                    ) {
                        vm.userRole = "Artisan"
                        vm.selectedTab = 0
                        vm.isLoggedIn = true
                    }

                    Spacer(Modifier.height(12.dp))

                    LoginRoleCard(
                        title = vm.text("Customer Entry", "ಗ್ರಾಹಕ ಪ್ರವೇಶ"),
                        subtitle = vm.text("Explore, save, and buy pottery", "ಮಣ್ಣಿನ ಕಲೆ ನೋಡಿ, ಉಳಿಸಿ, ಖರೀದಿಸಿ"),
                        icon = Icons.Default.ShoppingCart,
                        accent = Color(0xFF2E7D32),
                        vm = vm
                    ) {
                        vm.userRole = "Customer"
                        vm.selectedTab = 0
                        vm.isLoggedIn = true
                    }

                    Spacer(Modifier.height(12.dp))

                    LoginRoleCard(
                        title = vm.text("Admin Portal", "ನಿರ್ವಾಹಕ ಪೋರ್ಟಲ್"),
                        subtitle = vm.text("View artisans and purchase stats", "ಕುಶಲಕರ್ಮಿಗಳು ಮತ್ತು ಖರೀದಿ ಅಂಕಿಅಂಶಗಳು"),
                        icon = Icons.Default.Settings,
                        accent = Color(0xFF5D4037),
                        vm = vm
                    ) {
                        vm.showAdminLoginDialog = true
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 20.dp, end = 20.dp, bottom = 18.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFFFBF7).copy(alpha = 0.93f),
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color(0xFF5D4037),
                    modifier = Modifier.size(22.dp)
                )

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        vm.text("Language", "ಭಾಷೆ"),
                        fontSize = vm.appFont(12),
                        color = Color(0xFF3E2723),
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        vm.text("Tap to switch the whole app", "ಪೂರ್ಣ ಆಪ್ ಭಾಷೆ ಬದಲಿಸಿ"),
                        fontSize = vm.appFont(10),
                        color = Color(0xFF795548),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                LoginLanguageButton("EN", !vm.isKannada) {
                    vm.isKannada = false
                }

                Spacer(Modifier.width(8.dp))

                LoginLanguageButton("ಕನ್ನಡ", vm.isKannada) {
                    vm.isKannada = true
                }
            }
        }
    }
}

@Composable
fun LoginLanguageButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color(0xFFB35A2C) else Color(0xFFFBE9E7),
        border = BorderStroke(1.dp, if (selected) Color(0xFFB35A2C) else Color(0xFFE0B49A))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            color = if (selected) Color.White else Color(0xFF5D4037),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun LoginRoleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    vm: ArtisanViewModel,
    onClick: () -> Unit
) {
    Card(
        Modifier
            .fillMaxWidth()
            .height(106.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF7).copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(5.dp)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                Modifier.size(56.dp),
                shape = CircleShape,
                color = accent.copy(alpha = 0.13f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Black,
                    fontSize = vm.appFont(17),
                    color = Color(0xFF3E2723),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    fontSize = vm.appFont(12),
                    color = Color(0xFF8D6E63),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(Icons.Default.KeyboardArrowRight, null, tint = accent)
        }
    }
}
@Composable
fun MainContentRouter(vm: ArtisanViewModel) {
    Scaffold(
        bottomBar = {
            if (vm.selectedArtifact == null && !vm.showPaymentQR) {
                NavigationBar(containerColor = Color.White) {
                    when (vm.userRole) {
                        "Artisan" -> {
                            NavigationBarItem(vm.selectedTab == 0, { vm.selectedTab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text(vm.text("Workshop", "ಕಾರ್ಯಾಗಾರ"), fontSize = vm.appFont(12)) })
                            NavigationBarItem(vm.selectedTab == 1, { vm.selectedTab = 1 }, icon = { Icon(Icons.Default.List, null) }, label = { Text(vm.text("Orders", "ಆರ್ಡರ್"), fontSize = vm.appFont(12)) })
                            NavigationBarItem(vm.selectedTab == 3, { vm.selectedTab = 3 }, icon = { Icon(Icons.Default.AccountCircle, null) }, label = { Text(vm.text("Profile", "ಪ್ರೊಫೈಲ್"), fontSize = vm.appFont(12)) })
                        }
                        "Customer" -> {
                            NavigationBarItem(vm.selectedTab == 0, { vm.selectedTab = 0 }, icon = { Icon(Icons.Default.Search, null) }, label = { Text(vm.text("Gallery", "ಗ್ಯಾಲರಿ"), fontSize = vm.appFont(12)) })
                            NavigationBarItem(vm.selectedTab == 1, { vm.selectedTab = 1 }, icon = { Icon(Icons.Default.Favorite, null) }, label = { Text(vm.text("Saved", "ಉಳಿಸಿದವು"), fontSize = vm.appFont(12)) })
                            NavigationBarItem(vm.selectedTab == 2, { vm.selectedTab = 2 }, icon = { Icon(Icons.Default.ShoppingCart, null) }, label = { Text("${vm.text("Cart", "ಕಾರ್ಟ್")} ${vm.cartItems.size}", fontSize = vm.appFont(12)) })
                        }
                        "Admin" -> {
                            NavigationBarItem(vm.selectedTab == 0, { vm.selectedTab = 0 }, icon = { Icon(Icons.Default.Person, null) }, label = { Text(vm.text("Artisans", "ಕುಶಲಕರ್ಮಿಗಳು"), fontSize = vm.appFont(12)) })
                            NavigationBarItem(vm.selectedTab == 1, { vm.selectedTab = 1 }, icon = { Icon(Icons.Default.List, null) }, label = { Text(vm.text("Stats", "ಅಂಕಿಅಂಶ"), fontSize = vm.appFont(12)) })
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (vm.userRole) {
                "Artisan" -> when (vm.selectedTab) {
                    0 -> if (vm.selectedArtifact == null) RegulatedWorkshop(vm) else ArtifactDetailScreen(vm.selectedArtifact!!, vm)
                    1 -> ArtisanOrdersScreen(vm)
                    else -> ProfileScreen(vm)
                }
                "Customer" -> when (vm.selectedTab) {
                    0 -> if (vm.selectedArtifact == null) CustomerGallery(vm) else ArtifactDetailScreen(vm.selectedArtifact!!, vm)
                    1 -> SavedDesignsScreen(vm)
                    2 -> if (vm.showPaymentQR) PaymentQRScreen(vm) else CartAndPaymentScreen(vm)
                }
                "Admin" -> when (vm.selectedTab) {
                    0 -> AdminArtisanTab(vm)
                    1 -> AdminStatsTab(vm)
                }
            }
        }
    }
}

@Composable
fun RegulatedWorkshop(vm: ArtisanViewModel) {
    val sortedProducts = vm.myProducts.sortedByDescending { it.quantity }
    var openedItem by remember { mutableStateOf<PotteryItem?>(null) }

    if (vm.showProductDialog) ProductFormDialog(vm)

    openedItem?.let { item ->
        WorkshopProductDialog(
            item = item,
            vm = vm,
            onClose = { openedItem = null },
            onEdit = {
                openedItem = null
                vm.editingProduct = item
                vm.showProductDialog = true
            }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.logout() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = vm.text("Back to Login", "ಲಾಗಿನ್‌ಗೆ ಹಿಂತಿರುಗಿ"), tint = Color(0xFF3E2723))
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    vm.editingProduct = null
                    vm.showProductDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB35A2C)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text(vm.text("Add", "ಸೇರಿಸಿ"), fontSize = vm.appFont(14))
            }
        }

        Text(vm.text("WORKSHOP GALLERY", "ಕಾರ್ಯಾಗಾರ ಗ್ಯಾಲರಿ"), fontSize = vm.appFont(24), fontWeight = FontWeight.Black, color = Color(0xFF3E2723))
        Text(vm.text("Tap a product to manage stock or edit details", "ಸಂಗ್ರಹ ಬದಲಿಸಲು ಅಥವಾ ವಿವರ ತಿದ್ದಲು ಉತ್ಪನ್ನ ಒತ್ತಿರಿ"), fontSize = vm.appFont(12), color = Color.Gray)
        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(sortedProducts, key = { it.id }) { item ->
                PictureRegulatedCard(item, vm) { openedItem = item }
            }
        }
    }
}

@Composable
fun PictureRegulatedCard(item: PotteryItem, vm: ArtisanViewModel, onCardClick: () -> Unit) {
    val isOutOfStock = item.quantity <= 0
    val isLowStock = item.quantity in 1..3
    val grayscaleMatrix = ColorMatrix(floatArrayOf(0.33f,0.33f,0.33f,0f,0f, 0.33f,0.33f,0.33f,0f,0f, 0.33f,0.33f,0.33f,0f,0f, 0f,0f,0f,1f,0f))

    Card(Modifier.fillMaxWidth().height(245.dp).clickable { onCardClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
        Column {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Image(painterResource(item.imageRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, colorFilter = if (isOutOfStock) ColorFilter.colorMatrix(grayscaleMatrix) else null)
                if (isOutOfStock || isLowStock) {
                    Surface(Modifier.align(Alignment.TopStart).padding(8.dp), color = if (isOutOfStock) Color.Black.copy(0.7f) else Color(0xFFFFA000), shape = RoundedCornerShape(6.dp)) {
                        Text(if (isOutOfStock) vm.text("OUT", "ಖಾಲಿ") else vm.text("LOW", "ಕಡಿಮೆ"), color = Color.White, fontSize = vm.appFont(10), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
            Column(Modifier.padding(10.dp)) {
                Text(vm.itemName(item), fontWeight = FontWeight.Bold, fontSize = vm.appFont(14), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.price} • ${vm.text("Stock", "ಸಂಗ್ರಹ")} ${item.quantity}", fontSize = vm.appFont(11), color = Color.Gray)
            }
        }
    }
}

@Composable
fun WorkshopProductDialog(item: PotteryItem, vm: ArtisanViewModel, onClose: () -> Unit, onEdit: () -> Unit) {
    var stock by remember(item.id, item.quantity) { mutableIntStateOf(item.quantity) }

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            Button(onClick = onEdit, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037))) {
                Icon(Icons.Default.Edit, null)
                Spacer(Modifier.width(6.dp))
                Text(vm.text("Edit", "ತಿದ್ದು"), fontSize = vm.appFont(14))
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text(vm.text("Close", "ಮುಚ್ಚಿ"), fontSize = vm.appFont(14))
            }
        },
        title = {
            Text(vm.itemName(item), fontWeight = FontWeight.Black, color = Color(0xFF3E2723), fontSize = vm.appFont(20))
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Card(Modifier.fillMaxWidth().height(230.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Image(painterResource(item.imageRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Spacer(Modifier.height(14.dp))
                Text(vm.itemCategory(item), color = Color(0xFFB35A2C), fontWeight = FontWeight.Bold, fontSize = vm.appFont(14))
                Text(item.price, color = Color(0xFF2E7D32), fontWeight = FontWeight.Black, fontSize = vm.appFont(20))
                Spacer(Modifier.height(16.dp))
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFFBE9E7)) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (stock > 0) { stock--; item.quantity = stock } }) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF5D4037))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(vm.text("Stock", "ಸಂಗ್ರಹ"), fontSize = vm.appFont(11), color = Color.Gray)
                            Text("$stock", fontSize = vm.appFont(24), fontWeight = FontWeight.Black, color = Color(0xFF3E2723))
                        }
                        IconButton(onClick = { stock++; item.quantity = stock }) {
                            Icon(Icons.Default.KeyboardArrowUp, null, tint = Color(0xFFB35A2C))
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFFFFBF7)
    )
}

@Composable
fun ProductFormDialog(vm: ArtisanViewModel) {
    val editing = vm.editingProduct
    var selectedOption by remember(editing) {
        mutableStateOf(vm.productOptions.firstOrNull { it.name == editing?.name } ?: vm.productOptions.first())
    }
    var menuOpen by remember { mutableStateOf(false) }
    var price by remember(editing) { mutableStateOf(editing?.price ?: "₹") }
    var stock by remember(editing) { mutableStateOf(editing?.quantity?.toString() ?: "1") }
    var origin by remember(editing) { mutableStateOf(editing?.origin ?: "") }

    AlertDialog(
        onDismissRequest = {
            vm.showProductDialog = false
            vm.editingProduct = null
        },
        title = {
            Text(
                if (editing == null) vm.text("Add Product", "ಉತ್ಪನ್ನ ಸೇರಿಸಿ") else vm.text("Edit Product", "ಉತ್ಪನ್ನ ತಿದ್ದು"),
                fontWeight = FontWeight.Black,
                fontSize = vm.appFont(20)
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(vm.text("Choose Product", "ಉತ್ಪನ್ನ ಆಯ್ಕೆಮಾಡಿ"), fontWeight = FontWeight.Bold, color = Color(0xFF5D4037), fontSize = vm.appFont(14))

                Box {
                    OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                        Image(painterResource(selectedOption.imageRes), null, Modifier.size(42.dp), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                            Text(vm.optionName(selectedOption), fontWeight = FontWeight.Bold, fontSize = vm.appFont(14), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(vm.optionCategory(selectedOption), fontSize = vm.appFont(11), color = Color.Gray)
                        }
                        Icon(Icons.Default.KeyboardArrowDown, null)
                    }

                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        vm.productOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(painterResource(option.imageRes), null, Modifier.size(40.dp), contentScale = ContentScale.Crop)
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(vm.optionName(option), fontWeight = FontWeight.Bold, fontSize = vm.appFont(14))
                                            Text(vm.optionCategory(option), fontSize = vm.appFont(11), color = Color.Gray)
                                        }
                                    }
                                },
                                onClick = {
                                    selectedOption = option
                                    menuOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(vm.optionDesc(selectedOption), fontSize = vm.appFont(12), color = Color.Gray, lineHeight = vm.appFont(16))
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(price, { price = it }, label = { Text(vm.text("Price", "ಬೆಲೆ"), fontSize = vm.appFont(13)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(stock, { stock = it.filter { ch -> ch.isDigit() } }, label = { Text(vm.text("Stock", "ಸಂಗ್ರಹ"), fontSize = vm.appFont(13)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(origin, { origin = it }, label = { Text(vm.text("Origin", "ಮೂಲ ಸ್ಥಳ"), fontSize = vm.appFont(13)) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { vm.saveOrUpdateProduct(selectedOption, price, stock, origin) },
                enabled = price.filter { it.isDigit() }.isNotBlank() && origin.isNotBlank()
            ) {
                Text(vm.text("Save", "ಉಳಿಸಿ"), fontSize = vm.appFont(14))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                vm.showProductDialog = false
                vm.editingProduct = null
            }) {
                Text(vm.text("Cancel", "ರದ್ದು"), fontSize = vm.appFont(14))
            }
        },
        containerColor = Color.White
    )
}
@Composable
fun CustomerGallery(vm: ArtisanViewModel) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val filtered = vm.myProducts.filter {
        val matchesText = it.name.contains(query, true) ||
                it.nameKn.contains(query, true) ||
                it.origin.contains(query, true) ||
                it.originKn.contains(query, true)
        val matchesCategory = selectedCategory == "All" || it.category == selectedCategory
        matchesText && matchesCategory
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        IconButton(onClick = { vm.logout() }) {
            Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF3E2723))
        }

        Text(
            vm.text("EXPLORE COLLECTION", "ಸಂಗ್ರಹ ವೀಕ್ಷಿಸಿ"),
            fontSize = vm.appFont(24),
            fontWeight = FontWeight.Black,
            color = Color(0xFF3E2723)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            label = {
                Text(
                    vm.text("Search by product or origin", "ಉತ್ಪನ್ನ ಅಥವಾ ಸ್ಥಳದಿಂದ ಹುಡುಕಿ"),
                    fontSize = vm.appFont(13)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            Modifier
                .padding(vertical = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            vm.categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(vm.categoryLabel(category), fontSize = vm.appFont(12))
                    }
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(vm.text("No products found.", "ಉತ್ಪನ್ನಗಳು ಸಿಗಲಿಲ್ಲ."), fontSize = vm.appFont(14))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    CustomerPotteryCard(item, vm) {
                        vm.selectedArtifact = item
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerPotteryCard(item: PotteryItem, vm: ArtisanViewModel, onCardClick: () -> Unit) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var feedback by remember { mutableStateOf("") }
    val inCart = vm.cartQuantity(item)
    val canAdd = item.quantity > inCart

    Card(
        Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Image(
                    painterResource(item.imageRes),
                    null,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                IconButton(
                    onClick = {
                        if (vm.savedDesigns.contains(item)) {
                            vm.savedDesigns.remove(item)
                        } else {
                            vm.savedDesigns.add(item)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.White.copy(0.75f), CircleShape)
                ) {
                    Icon(
                        if (vm.savedDesigns.contains(item)) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        null,
                        tint = Color(0xFFB35A2C)
                    )
                }
            }

            Column(Modifier.padding(8.dp)) {
                Text(
                    vm.itemName(item),
                    fontWeight = FontWeight.Bold,
                    fontSize = vm.appFont(14),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    "${vm.itemCategory(item)} • ${vm.text("Stock", "ಸಂಗ್ರಹ")}: ${item.quantity}",
                    fontSize = vm.appFont(10),
                    color = Color.Gray
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.price,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF2E7D32),
                        fontSize = vm.appFont(14)
                    )

                    Button(
                        onClick = {
                            val added = vm.addToCart(item)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            feedback = if (added) vm.text("Added", "ಸೇರಿತು") else vm.text("Full", "ಪೂರ್ಣ")
                            scope.launch {
                                delay(1000)
                                feedback = ""
                            }
                        },
                        enabled = canAdd,
                        modifier = Modifier
                            .height(32.dp)
                            .width(80.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (feedback.isNotEmpty()) Color(0xFF2E7D32) else Color(0xFF3E2723),
                            disabledContainerColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            when {
                                feedback.isNotEmpty() -> feedback
                                !canAdd -> vm.text("Limit", "ಮಿತಿ")
                                else -> vm.text("Add", "ಸೇರಿಸಿ")
                            },
                            fontSize = vm.appFont(11),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedDesignsScreen(vm: ArtisanViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        IconButton(onClick = { vm.logout() }) {
            Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF3E2723))
        }

        Text(
            vm.text("SAVED DESIGNS", "ಉಳಿಸಿದ ವಿನ್ಯಾಸಗಳು"),
            fontSize = vm.appFont(24),
            fontWeight = FontWeight.Black,
            color = Color(0xFF3E2723)
        )

        Spacer(Modifier.height(16.dp))

        if (vm.savedDesigns.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(vm.text("No bookmarks yet.", "ಇನ್ನೂ ಉಳಿಸಿದವುಗಳಿಲ್ಲ."), fontSize = vm.appFont(14))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(vm.savedDesigns, key = { it.id }) { item ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painterResource(item.imageRes),
                                null,
                                Modifier.size(100.dp),
                                contentScale = ContentScale.Crop
                            )

                            Column(
                                Modifier
                                    .padding(12.dp)
                                    .weight(1f)
                            ) {
                                Text(
                                    vm.itemName(item),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = vm.appFont(14),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(item.price, color = Color(0xFF2E7D32), fontSize = vm.appFont(13))
                            }

                            IconButton(onClick = { vm.addToCart(item) }) {
                                Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFFB35A2C))
                            }

                            IconButton(onClick = { vm.savedDesigns.remove(item) }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartAndPaymentScreen(vm: ArtisanViewModel) {
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        IconButton(onClick = { vm.logout() }) {
            Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF3E2723))
        }

        Text(
            vm.text("CHECKOUT & PROFILE", "ಪಾವತಿ ಮತ್ತು ಪ್ರೊಫೈಲ್"),
            fontSize = vm.appFont(24),
            fontWeight = FontWeight.Black,
            color = Color(0xFF3E2723)
        )

        Card(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFBE9E7))
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(50.dp), shape = CircleShape, color = Color.White) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.padding(10.dp))
                }

                Column(Modifier.padding(start = 12.dp)) {
                    Text(vm.customerName, fontWeight = FontWeight.Bold, fontSize = vm.appFont(14))
                    Text(vm.customerEmail, fontSize = vm.appFont(11), color = Color.Gray)
                }
            }
        }

        Box(Modifier.weight(1f)) {
            if (vm.cartItems.isEmpty()) {
                Text(
                    vm.text("Cart is empty", "ಕಾರ್ಟ್ ಖಾಲಿಯಾಗಿದೆ"),
                    Modifier.align(Alignment.Center),
                    fontSize = vm.appFont(14)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vm.cartGroups(), key = { it.first.id }) { pair ->
                        CartRow(pair.first, pair.second, vm)
                    }
                }
            }
        }

        if (vm.cartItems.isNotEmpty()) {
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    PriceLine(vm.text("Subtotal", "ಉಪ ಮೊತ್ತ"), vm.cartSubtotal(), vm)
                    PriceLine(vm.text("Delivery", "ವಿತರಣೆ"), vm.deliveryFee(), vm)
                    Divider(Modifier.padding(vertical = 8.dp))
                    PriceLine(vm.text("Total", "ಒಟ್ಟು"), vm.cartTotal(), vm, bold = true)
                }
            }
        }

        Button(
            onClick = {
                safeStartActivity(
                    context,
                    Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:manyshrikant@gmail.com")
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037))
        ) {
            Icon(Icons.Default.Email, null)
            Spacer(Modifier.width(8.dp))
            Text(vm.text("Contact Customer Care", "ಗ್ರಾಹಕ ಸಹಾಯ ಸಂಪರ್ಕಿಸಿ"), fontSize = vm.appFont(14))
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { vm.showPaymentQR = true },
            enabled = vm.cartItems.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB35A2C))
        ) {
            Text(
                vm.text("Proceed to Payment", "ಪಾವತಿಗೆ ಮುಂದುವರಿಸಿ"),
                fontWeight = FontWeight.Bold,
                fontSize = vm.appFont(14)
            )
        }

        if (vm.orders.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "${vm.text("Last order", "ಕೊನೆಯ ಆರ್ಡರ್")}: ${vm.orders.first().orderId} • ₹${vm.orders.first().total}",
                fontSize = vm.appFont(12),
                color = Color.Gray
            )
        }
    }
}
@Composable
fun CartRow(item: PotteryItem, qty: Int, vm: ArtisanViewModel) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(item.imageRes), null, Modifier.size(72.dp), contentScale = ContentScale.Crop)

            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(vm.itemName(item), fontWeight = FontWeight.Bold, fontSize = vm.appFont(14), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("₹${item.priceValue() * qty}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = vm.appFont(13))
            }

            IconButton(onClick = { vm.removeOneFromCart(item) }) {
                Icon(Icons.Default.KeyboardArrowDown, null)
            }

            Text("$qty", fontWeight = FontWeight.Bold, fontSize = vm.appFont(14))

            IconButton(onClick = { vm.addToCart(item) }) {
                Icon(Icons.Default.Add, null)
            }

            IconButton(onClick = { vm.removeAllFromCart(item) }) {
                Icon(Icons.Default.Delete, null, tint = Color.Red)
            }
        }
    }
}

@Composable
fun PriceLine(label: String, amount: Int, vm: ArtisanViewModel, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Black else FontWeight.Normal, fontSize = vm.appFont(14))
        Text("₹$amount", fontWeight = if (bold) FontWeight.Black else FontWeight.Normal, fontSize = vm.appFont(14))
    }
}

@Composable
fun PaymentQRScreen(vm: ArtisanViewModel) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = { vm.showPaymentQR = false }, Modifier.align(Alignment.Start)) {
            Icon(Icons.Default.ArrowBack, null)
        }

        Text(vm.text("HERITAGE PAYMENT", "ಪಾರಂಪರಿಕ ಪಾವತಿ"), fontSize = vm.appFont(24), fontWeight = FontWeight.Black, color = Color(0xFF3E2723))
        Text("${vm.text("Total", "ಒಟ್ಟು")}: ₹${vm.cartTotal()}", fontSize = vm.appFont(18), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(32.dp))

        Surface(
            modifier = Modifier.size(300.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, Color(0xFFB35A2C)),
            color = Color.White
        ) {
            Image(painterResource(R.drawable.qr), "QR", Modifier.padding(16.dp), contentScale = ContentScale.Fit)
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { vm.completePayment() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            Icon(Icons.Default.CheckCircle, null)
            Spacer(Modifier.width(8.dp))
            Text(vm.text("I Have Paid - Confirm Order", "ನಾನು ಪಾವತಿಸಿದ್ದೇನೆ - ಆರ್ಡರ್ ದೃಢೀಕರಿಸಿ"), fontWeight = FontWeight.Bold, fontSize = vm.appFont(14))
        }
    }
}

@Composable
fun ArtisanOrdersScreen(vm: ArtisanViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        IconButton(onClick = { vm.logout() }) {
            Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF3E2723))
        }

        Text(vm.text("ORDER MANAGEMENT", "ಆರ್ಡರ್ ನಿರ್ವಹಣೆ"), fontSize = vm.appFont(24), fontWeight = FontWeight.Black, color = Color(0xFF3E2723))
        Spacer(Modifier.height(16.dp))

        if (vm.orders.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(vm.text("No paid orders yet.", "ಇನ್ನೂ ಪಾವತಿಸಿದ ಆರ್ಡರ್ ಇಲ್ಲ."), fontSize = vm.appFont(14))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(vm.orders, key = { it.orderId }) { order ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(order.orderId, fontWeight = FontWeight.Black, fontSize = vm.appFont(14))
                                Text(vm.text(order.status, "ಪಾವತಿಸಲಾಗಿದೆ"), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = vm.appFont(13))
                            }

                            Text(order.customerName, color = Color.Gray, fontSize = vm.appFont(13))
                            Spacer(Modifier.height(8.dp))

                            order.items.forEach { (item, qty) ->
                                Text("${vm.itemName(item)} x $qty", fontSize = vm.appFont(13))
                            }

                            Spacer(Modifier.height(8.dp))
                            Text("${vm.text("Total", "ಒಟ್ಟು")}: ₹${order.total}", fontWeight = FontWeight.Black, fontSize = vm.appFont(14))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminArtisanTab(vm: ArtisanViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        IconButton(onClick = { vm.logout() }) {
            Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF3E2723))
        }

        Text(vm.text("ARTISAN MANAGEMENT", "ಕುಶಲಕರ್ಮಿ ನಿರ್ವಹಣೆ"), fontSize = vm.appFont(24), fontWeight = FontWeight.Black, color = Color(0xFF3E2723))
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(vm.artisanList, key = { it.email }) { artisan ->
                var expanded by remember { mutableStateOf(false) }

                Card(
                    Modifier.fillMaxWidth().clickable { expanded = !expanded },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(60.dp), CircleShape) {
                                Image(painterResource(artisan.imageRes), null, contentScale = ContentScale.Crop)
                            }

                            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(artisan.name, fontWeight = FontWeight.Bold, fontSize = vm.appFont(14))
                                Text(artisan.village, color = Color.Gray, fontSize = vm.appFont(13))
                            }

                            Icon(
                                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                null,
                                tint = Color(0xFF5D4037)
                            )
                        }

                        if (expanded) {
                            Spacer(Modifier.height(16.dp))
                            Text("${vm.text("Phone", "ಫೋನ್")}: ${artisan.phone}", fontSize = vm.appFont(13))
                            Text("${vm.text("Email", "ಇಮೇಲ್")}: ${artisan.email}", fontSize = vm.appFont(13))
                            Spacer(Modifier.height(8.dp))
                            Text(artisan.specialization, fontSize = vm.appFont(13), color = Color.DarkGray, lineHeight = vm.appFont(18))

                            Row(Modifier.fillMaxWidth(), Arrangement.End) {
                                TextButton(onClick = { vm.artisanList.remove(artisan) }) {
                                    Text(vm.text("Remove Artisan", "ಕುಶಲಕರ್ಮಿ ತೆಗೆದುಹಾಕಿ"), color = Color.Red, fontSize = vm.appFont(13))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStatsTab(vm: ArtisanViewModel) {
    val totalStock = vm.myProducts.sumOf { it.quantity }
    val revenue = vm.orders.sumOf { it.total }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        IconButton(onClick = { vm.logout() }) {
            Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF3E2723))
        }

        Text(vm.text("PURCHASE STATISTICS", "ಖರೀದಿ ಅಂಕಿಅಂಶಗಳು"), fontSize = vm.appFont(24), fontWeight = FontWeight.Black, color = Color(0xFF3E2723))
        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            StatCard(vm.text("Orders", "ಆರ್ಡರ್"), "${vm.orders.size}", vm, Modifier.weight(1f))
            StatCard(vm.text("Revenue", "ಆದಾಯ"), "₹$revenue", vm, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            StatCard(vm.text("Products", "ಉತ್ಪನ್ನಗಳು"), "${vm.myProducts.size}", vm, Modifier.weight(1f))
            StatCard(vm.text("Stock", "ಸಂಗ್ರಹ"), "$totalStock", vm, Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))
        Text(vm.text("Popular Products", "ಜನಪ್ರಿಯ ಉತ್ಪನ್ನಗಳು"), fontWeight = FontWeight.Bold, fontSize = vm.appFont(14))

        Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp)) {
                vm.myProducts.sortedByDescending { it.rating }.take(3).forEachIndexed { index, item ->
                    Text("${index + 1}. ${vm.itemName(item)} (${item.rating})", fontSize = vm.appFont(13))
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, vm: ArtisanViewModel, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFFFBE9E7))) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontSize = vm.appFont(12))
            Text(value, fontSize = vm.appFont(20), fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ProfileScreen(vm: ArtisanViewModel) {
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Box(Modifier.fillMaxWidth().height(200.dp).background(Color(0xFFB35A2C))) {
            IconButton(onClick = { vm.logout() }, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }

            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(Modifier.size(100.dp).shadow(8.dp, CircleShape), shape = CircleShape, color = Color.White) {
                    Icon(Icons.Default.Person, null, Modifier.size(60.dp).padding(16.dp), tint = Color(0xFF5D4037))
                }

                Spacer(Modifier.height(8.dp))
                Text(vm.tagline, fontStyle = FontStyle.Italic, color = Color.White, fontSize = vm.appFont(14), textAlign = TextAlign.Center)
            }
        }

        Column(Modifier.padding(20.dp)) {
            Text(vm.text("Artisan Identity", "ಕುಶಲಕರ್ಮಿ ಪರಿಚಯ"), fontWeight = FontWeight.Black, color = Color(0xFF3E2723), fontSize = vm.appFont(18))

            ProfileEditField(vm.text("Full Name", "ಪೂರ್ಣ ಹೆಸರು"), vm.artisanName, vm) { vm.artisanName = it }
            ProfileEditField(vm.text("Village", "ಗ್ರಾಮ"), vm.artisanVillage, vm) { vm.artisanVillage = it }
            ProfileEditField(vm.text("Phone", "ಫೋನ್"), vm.artisanPhone, vm) { vm.artisanPhone = it }
            ProfileEditField(vm.text("Email", "ಇಮೇಲ್"), vm.artisanEmail, vm) { vm.artisanEmail = it }
            ProfileEditField(vm.text("Heritage", "ಪರಂಪರೆ"), vm.heritageBackground, vm) { vm.heritageBackground = it }
            ProfileEditField(vm.text("Bio", "ಜೀವನಚರಿತ್ರೆ"), vm.craftBio, vm) { vm.craftBio = it }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    safeStartActivity(context, Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:manyshrikant@gmail.com")
                    })
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E2723)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Email, null)
                Spacer(Modifier.width(8.dp))
                Text(vm.text("Contact Admin", "ನಿರ್ವಾಹಕರನ್ನು ಸಂಪರ್ಕಿಸಿ"), fontSize = vm.appFont(14))
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(onClick = { vm.logout() }, modifier = Modifier.fillMaxWidth()) {
                Text(vm.text("Logout", "ಲಾಗ್ ಔಟ್"), color = Color.Red, fontSize = vm.appFont(14))
            }
        }
    }
}

@Composable
fun ProfileEditField(label: String, value: String, vm: ArtisanViewModel, onValueChange: (String) -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(label, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037), fontSize = vm.appFont(12))
        OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
    }
}

@Composable
fun ArtifactDetailScreen(item: PotteryItem, vm: ArtisanViewModel) {
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box {
            Image(painterResource(item.imageRes), null, Modifier.fillMaxWidth().height(320.dp), contentScale = ContentScale.Crop)

            IconButton(
                onClick = { vm.selectedArtifact = null },
                Modifier.padding(16.dp).background(Color.White.copy(0.8f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, null)
            }
        }

        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("${item.rating}", fontWeight = FontWeight.Bold, fontSize = vm.appFont(14))
                Text(" (${item.reviewCount} ${vm.text("Reviews", "ವಿಮರ್ಶೆಗಳು")})", color = Color.Gray, fontSize = vm.appFont(14))
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(vm.itemName(item), fontSize = vm.appFont(28), fontWeight = FontWeight.Black, color = Color(0xFF3E2723), modifier = Modifier.weight(1f))
                Text(item.price, fontSize = vm.appFont(24), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }

            Text("${vm.text("Origin", "ಮೂಲ ಸ್ಥಳ")}: ${vm.itemOrigin(item)} • ${vm.itemCategory(item)}", color = Color(0xFFB35A2C), fontWeight = FontWeight.Bold, fontSize = vm.appFont(14))
            Text("${vm.text("Available stock", "ಲಭ್ಯ ಸಂಗ್ರಹ")}: ${item.quantity}", color = Color.Gray, fontSize = vm.appFont(13))

            Spacer(Modifier.height(16.dp))
            Text(vm.itemDesc(item), fontSize = vm.appFont(16), lineHeight = vm.appFont(22), color = Color.DarkGray)

            Spacer(Modifier.height(24.dp))
            DetailSection(vm.text("Health Benefits", "ಆರೋಗ್ಯ ಲಾಭಗಳು"), vm.itemHealth(item), vm)
            DetailSection(vm.text("Eco Value", "ಪರಿಸರ ಮೌಲ್ಯ"), vm.itemEco(item), vm)

            Spacer(Modifier.height(24.dp))

            if (vm.userRole == "Customer") {
                Button(
                    onClick = { vm.addToCart(item) },
                    enabled = item.quantity > vm.cartQuantity(item),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB35A2C))
                ) {
                    Icon(Icons.Default.ShoppingCart, null)
                    Spacer(Modifier.width(8.dp))
                    Text(vm.text("Add to Cart", "ಕಾರ್ಟ್‌ಗೆ ಸೇರಿಸಿ"), fontSize = vm.appFont(14))
                }

                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Check out ${item.name} on Kumbara-Kala!")
                        setPackage("com.whatsapp")
                    }

                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        safeStartActivity(context, Intent.createChooser(intent, vm.text("Share", "ಹಂಚಿಕೊಳ್ಳಿ")))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(8.dp))
                Text(vm.text("Share on WhatsApp", "WhatsApp ನಲ್ಲಿ ಹಂಚಿಕೊಳ್ಳಿ"), fontSize = vm.appFont(14))
            }
        }
    }
}

@Composable
fun DetailSection(title: String, points: List<String>, vm: ArtisanViewModel) {
    Column(Modifier.padding(bottom = 16.dp)) {
        Text(title, fontWeight = FontWeight.Black, fontSize = vm.appFont(16), color = Color(0xFF3E2723))
        points.forEach {
            Text("• $it", fontSize = vm.appFont(14), color = Color.Gray, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
        }
    }
}

fun safeStartActivity(context: android.content.Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (_: Exception) {
        }
    } catch (_: Exception) {
    }
}

@Composable
fun KumbaraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF5D4037),
            secondary = Color(0xFFB35A2C),
            background = Color(0xFFFDFCF9),
            surface = Color.White
        ),
        content = content
    )
}
