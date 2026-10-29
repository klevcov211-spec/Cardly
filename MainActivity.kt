package com.example.cardly

// ============================================================
// ИМПОРТЫ (все нужные библиотеки для работы приложения)
// ============================================================
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

// ============================================================
// ЦВЕТА ДЛЯ ТЁМ (чтобы легко менять в одном месте)
// ============================================================
// Тёмная тема
val DarkBg = Color(0xFF131938)       // Основной фон
val DarkCardBg = Color(0xFF1E2545)   // Фон карточек
val DarkText = Color(0xFFFFFFFF)     // Основной текст
val DarkGrayText = Color(0xFF9AA0B5) // Второстепенный текст

// Светлая тема
val LightBg = Color(0xFFF4F5F9)      // Светлый фон
val LightCardBg = Color(0xFFFFFFFF)  // Белые карточки
val LightText = Color(0xFF131938)    // Тёмный текст
val LightGrayText = Color(0xFF6B7280) // Серый текст

// Акцентные цвета (не меняются)
val PurpleBtn = Color(0xFF7B51D3)    // Фиолетовые кнопки
val PurpleSoft = Color(0xFF5B3FAB)   // Тёмно-фиолетовый (для эффекта стопки)
val PurpleStroke = Color(0xFF9B7BFF) // Обводка карточек
val ErrorRed = Color(0xFFE5484D)     // Красный (ошибки)
val SuccessGreen = Color(0xFF3FB950) // Зелёный (правильный ответ)

// Шрифт приложения
val AppFont = FontFamily.SansSerif

// ============================================================
// КЛАСС ДЛЯ ХРАНЕНИЯ НАБОРА ЦВЕТОВ ТЕКУЩЕЙ ТЕМЫ
// ============================================================
data class AppTheme(
    val bg: Color,       // Фон экрана
    val cardBg: Color,   // Фон карточек
    val text: Color,     // Основной текст
    val grayText: Color, // Второстепенный текст
    val isDark: Boolean  // Флаг тёмной темы
)

// Функция возвращает нужный набор цветов в зависимости от флага
fun getTheme(isDark: Boolean): AppTheme {
    return if (isDark) AppTheme(DarkBg, DarkCardBg, DarkText, DarkGrayText, true)
    else AppTheme(LightBg, LightCardBg, LightText, LightGrayText, false)
}

// ============================================================
// ВАЛИДАЦИЯ ДАННЫХ ПРИ РЕГИСТРАЦИИ
// ============================================================
fun isValidEmail(email: String): Boolean {
    val trimmed = email.trim()
    val parts = trimmed.split("@")
    if (parts.size != 2) return false
    val (local, domain) = parts
    if (local.isBlank() || domain.isBlank()) return false
    if (!domain.contains(".")) return false
    val zone = domain.substringAfterLast(".")
    if (zone.length < 2) return false
    val allowedRegex = Regex("^[A-Za-zА-Яа-яЁё0-9._%+-]+$")
    if (!allowedRegex.matches(local)) return false
    if (!allowedRegex.matches(domain.replace(".", ""))) return false
    return true
}

fun isValidLogin(login: String): Boolean {
    val trimmed = login.trim()
    if (trimmed.length < 3) return false
    if (trimmed.contains(" ")) return false
    val allowedRegex = Regex("^[A-Za-zА-Яа-яЁё0-9_]+$")
    return allowedRegex.matches(trimmed)
}

// ============================================================
// МОДЕЛИ ДАННЫХ
// ============================================================
data class WordCard(val original: String, val translation: String)

data class Folder(
    val id: Int,
    val name: String,
    val theme: String,
    val words: List<WordCard>
)

// ============================================================
// ХРАНИЛИЩЕ (SharedPreferences) — сохранение аккаунтов и папок
// ============================================================
class AccountStorage(context: Context) {
    private val prefs = context.getSharedPreferences("cardly_accounts", Context.MODE_PRIVATE)

    fun getUsers(): List<String> {
        val raw = prefs.getString("users", "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(",")
    }

    fun addUser(login: String, password: String, email: String): Boolean {
        if (login.isBlank() || password.isBlank()) return false
        if (getUsers().contains(login)) return false
        val users = getUsers() + login
        prefs.edit().putString("users", users.joinToString(",")).apply()
        prefs.edit().putString("${login}_password", password).apply()
        prefs.edit().putString("${login}_email", email).apply()
        return true
    }

    fun checkLogin(login: String, password: String): Boolean {
        val savedPassword = prefs.getString("${login}_password", null) ?: return false
        return savedPassword == password
    }

    fun getEmail(login: String): String = prefs.getString("${login}_email", "") ?: ""
    fun isUserExists(login: String): Boolean = getUsers().contains(login)

    fun setCurrentUser(login: String) = prefs.edit().putString("current_user", login).apply()
    fun getCurrentUser(): String = prefs.getString("current_user", "") ?: ""
    fun clearCurrentUser() = prefs.edit().remove("current_user").apply()

    fun isDarkTheme(): Boolean = prefs.getBoolean("is_dark_theme", true)
    fun setDarkTheme(value: Boolean) = prefs.edit().putBoolean("is_dark_theme", value).apply()

    fun areNotificationsEnabled(): Boolean = prefs.getBoolean("notifications_enabled", true)
    fun setNotificationsEnabled(value: Boolean) = prefs.edit().putBoolean("notifications_enabled", value).apply()

    // Сохраняем папки в JSON-формате
    fun saveUserFolders(login: String, folders: List<Folder>) {
        val arr = JSONArray()
        folders.forEach { folder ->
            val folderObj = JSONObject()
            folderObj.put("id", folder.id)
            folderObj.put("name", folder.name)
            folderObj.put("theme", folder.theme)
            val wordsArr = JSONArray()
            folder.words.forEach { word ->
                val wordObj = JSONObject()
                wordObj.put("original", word.original)
                wordObj.put("translation", word.translation)
                wordsArr.put(wordObj)
            }
            folderObj.put("words", wordsArr)
            arr.put(folderObj)
        }
        prefs.edit().putString("${login}_folders", arr.toString()).apply()
    }

    // Загружаем папки из JSON
    fun loadUserFolders(login: String): MutableList<Folder> {
        val raw = prefs.getString("${login}_folders", null) ?: return mutableStateListOf()
        return try {
            val arr = JSONArray(raw)
            val result = mutableStateListOf<Folder>()
            for (i in 0 until arr.length()) {
                val folderObj = arr.getJSONObject(i)
                val wordsArr = folderObj.getJSONArray("words")
                val words = mutableListOf<WordCard>()
                for (j in 0 until wordsArr.length()) {
                    val wordObj = wordsArr.getJSONObject(j)
                    words.add(WordCard(wordObj.getString("original"), wordObj.getString("translation")))
                }
                result.add(
                    Folder(
                        id = folderObj.getInt("id"),
                        name = folderObj.getString("name"),
                        theme = folderObj.getString("theme"),
                        words = words
                    )
                )
            }
            result
        } catch (e: Exception) {
            mutableStateListOf()
        }
    }
}

// ============================================================
// ГОТОВЫЕ ТЕМЫ (для новых пользователей)
// ============================================================
val themeHome = listOf(
    WordCard("Home", "Дом"), WordCard("Bed", "Кровать"), WordCard("Bedroom", "Спальня"),
    WordCard("Window", "Окно"), WordCard("Carpet", "Коврик"), WordCard("TV", "Телевизор"),
    WordCard("Balcony", "Балкон"), WordCard("Door", "Дверь"), WordCard("Wall", "Стена"),
    WordCard("Floor", "Пол"), WordCard("Ceiling", "Потолок"), WordCard("Sofa", "Диван"),
    WordCard("Chair", "Стул"), WordCard("Table", "Стол"), WordCard("Lamp", "Лампа"),
    WordCard("Mirror", "Зеркало"), WordCard("Kitchen", "Кухня"), WordCard("Bathroom", "Ванная"),
    WordCard("Toilet", "Туалет"), WordCard("Fridge", "Холодильник"), WordCard("Oven", "Печь"),
    WordCard("Plate", "Тарелка"), WordCard("Cup", "Чашка"), WordCard("Fork", "Вилка"),
    WordCard("Spoon", "Ложка"), WordCard("Knife", "Нож"), WordCard("Pillow", "Подушка"),
    WordCard("Blanket", "Одеяло"), WordCard("Wardrobe", "Шкаф"), WordCard("Key", "Ключ")
)

val themeZoo = listOf(
    WordCard("Lion", "Лев"), WordCard("Tiger", "Тигр"), WordCard("Elephant", "Слон"),
    WordCard("Giraffe", "Жираф"), WordCard("Monkey", "Обезьяна"), WordCard("Bear", "Медведь"),
    WordCard("Wolf", "Волк"), WordCard("Fox", "Лиса"), WordCard("Rabbit", "Кролик"),
    WordCard("Zebra", "Зебра"), WordCard("Panda", "Панда"), WordCard("Kangaroo", "Кенгуру"),
    WordCard("Penguin", "Пингвин"), WordCard("Snake", "Змея"), WordCard("Crocodile", "Крокодил"),
    WordCard("Turtle", "Черепаха"), WordCard("Parrot", "Попугай"), WordCard("Owl", "Сова"),
    WordCard("Eagle", "Орёл"), WordCard("Shark", "Акула"), WordCard("Whale", "Кит"),
    WordCard("Dolphin", "Дельфин"), WordCard("Camel", "Верблюд"), WordCard("Horse", "Лошадь"),
    WordCard("Cow", "Корова"), WordCard("Pig", "Свинья"), WordCard("Sheep", "Овца"),
    WordCard("Chicken", "Курица"), WordCard("Duck", "Утка"), WordCard("Deer", "Олень")
)

val themeAirport = listOf(
    WordCard("Airport", "Аэропорт"), WordCard("Plane", "Самолёт"), WordCard("Ticket", "Билет"),
    WordCard("Passport", "Паспорт"), WordCard("Luggage", "Багаж"), WordCard("Suitcase", "Чемодан"),
    WordCard("Gate", "Выход"), WordCard("Flight", "Рейс"), WordCard("Pilot", "Пилот"),
    WordCard("Stewardess", "Стюардесса"), WordCard("Runway", "Взлётная полоса"), WordCard("Departure", "Отправление"),
    WordCard("Arrival", "Прибытие"), WordCard("Delay", "Задержка"), WordCard("Boarding", "Посадка"),
    WordCard("Seat", "Место"), WordCard("Window seat", "Место у окна"), WordCard("Aisle", "Проход"),
    WordCard("Customs", "Таможня"), WordCard("Visa", "Виза"), WordCard("Terminal", "Терминал"),
    WordCard("Crew", "Экипаж"), WordCard("Lounge", "Зал ожидания"), WordCard("Check-in", "Регистрация"),
    WordCard("Duty free", "Дьюти-фри"), WordCard("Layover", "Пересадка"), WordCard("Turbulence", "Турбулентность"),
    WordCard("Takeoff", "Взлёт"), WordCard("Landing", "Посадка"), WordCard("Luggage tag", "Бирка багажа")
)

val themeFood = listOf(
    WordCard("Bread", "Хлеб"), WordCard("Milk", "Молоко"), WordCard("Cheese", "Сыр"),
    WordCard("Meat", "Мясо"), WordCard("Fish", "Рыба"), WordCard("Egg", "Яйцо"),
    WordCard("Rice", "Рис"), WordCard("Soup", "Суп"), WordCard("Salad", "Салат"),
    WordCard("Apple", "Яблоко"), WordCard("Banana", "Банан"), WordCard("Orange", "Апельсин"),
    WordCard("Grape", "Виноград"), WordCard("Strawberry", "Клубника"), WordCard("Watermelon", "Арбуз"),
    WordCard("Lemon", "Лимон"), WordCard("Tomato", "Помидор"), WordCard("Potato", "Картошка"),
    WordCard("Carrot", "Морковь"), WordCard("Onion", "Лук"), WordCard("Cucumber", "Огурец"),
    WordCard("Sugar", "Сахар"), WordCard("Salt", "Соль"), WordCard("Pepper", "Перец"),
    WordCard("Butter", "Масло"), WordCard("Coffee", "Кофе"), WordCard("Tea", "Чай"),
    WordCard("Juice", "Сок"), WordCard("Water", "Вода"), WordCard("Chocolate", "Шоколад")
)

val themeColors = listOf(
    WordCard("Red", "Красный"), WordCard("Blue", "Синий"), WordCard("Green", "Зелёный"),
    WordCard("Yellow", "Жёлтый"), WordCard("Black", "Чёрный"), WordCard("White", "Белый"),
    WordCard("Orange", "Оранжевый"), WordCard("Purple", "Фиолетовый"), WordCard("Pink", "Розовый"),
    WordCard("Brown", "Коричневый"), WordCard("Gray", "Серый"), WordCard("Gold", "Золотой"),
    WordCard("Silver", "Серебряный"), WordCard("Beige", "Бежевый"), WordCard("Turquoise", "Бирюзовый"),
    WordCard("Violet", "Сиреневый"), WordCard("Crimson", "Малиновый"), WordCard("Navy", "Тёмно-синий"),
    WordCard("Lime", "Лаймовый"), WordCard("Coral", "Коралловый"), WordCard("Mint", "Мятный"),
    WordCard("Lavender", "Лавандовый"), WordCard("Maroon", "Бордовый"), WordCard("Olive", "Оливковый"),
    WordCard("Teal", "Изумрудный"), WordCard("Indigo", "Индиго"), WordCard("Peach", "Персиковый"),
    WordCard("Cherry", "Вишнёвый"), WordCard("Sky blue", "Голубой"), WordCard("Emerald", "Смарагдовый")
)

val themeProfessions = listOf(
    WordCard("Doctor", "Врач"), WordCard("Teacher", "Учитель"), WordCard("Engineer", "Инженер"),
    WordCard("Lawyer", "Юрист"), WordCard("Driver", "Водитель"), WordCard("Cook", "Повар"),
    WordCard("Waiter", "Официант"), WordCard("Pilot", "Пилот"), WordCard("Firefighter", "Пожарный"),
    WordCard("Police officer", "Полицейский"), WordCard("Soldier", "Солдат"), WordCard("Farmer", "Фермер"),
    WordCard("Builder", "Строитель"), WordCard("Painter", "Художник"), WordCard("Singer", "Певец"),
    WordCard("Actor", "Актёр"), WordCard("Writer", "Писатель"), WordCard("Journalist", "Журналист"),
    WordCard("Photographer", "Фотограф"), WordCard("Designer", "Дизайнер"), WordCard("Programmer", "Программист"),
    WordCard("Scientist", "Учёный"), WordCard("Nurse", "Медсестра"), WordCard("Dentist", "Стоматолог"),
    WordCard("Baker", "Пекарь"), WordCard("Butcher", "Мясник"), WordCard("Barber", "Парикмахер"),
    WordCard("Tailor", "Портной"), WordCard("Carpenter", "Плотник"), WordCard("Plumber", "Сантехник")
)

fun defaultFolders(): List<Folder> = listOf(
    Folder(1, "Папка №1", "Тема: дом", themeHome),
    Folder(2, "Папка №2", "Тема: зоопарк", themeZoo),
    Folder(3, "Папка №3", "Тема: Аэропорт", themeAirport),
    Folder(4, "Папка №4", "Тема: еда", themeFood),
    Folder(5, "Папка №5", "Тема: цвета", themeColors),
    Folder(6, "Папка №6", "Тема: профессии", themeProfessions)
)

// ============================================================
// СОВЕТЫ (показываются на главном экране по два)
// ============================================================
val allTips = listOf(
    "Интервальное повторение" to "Повторяй слова через 1 день, потом через 3, потом через 7. Так они запоминаются навсегда!",
    "Метод ассоциаций" to "Придумывай яркие картинки к словам. Чем абсурднее образ — тем лучше запомнится.",
    "Проговаривай вслух" to "Произноси каждое слово вслух 3 раза. Мозг запоминает через речь лучше, чем через глаза.",
    "Учи в контексте" to "Не зубри слова по отдельности. Составляй с ними предложения — так запоминается в 3 раза быстрее.",
    "15 минут каждый день" to "Лучше 15 минут ежедневно, чем 3 часа раз в неделю. Регулярность важнее интенсивности.",
    "Две стороны карточки" to "Сначала смотри на слово → вспоминай перевод. Потом наоборот. Так работают оба направления памяти.",
    "Повторяй перед сном" to "Просмотри карточки за 10 минут до сна. Во сне мозг закрепляет информацию.",
    "Не бойся ошибок" to "Ошибки — это часть обучения. Каждая ошибка делает твой мозг сильнее.",
    "Учи по темам" to "Слова одной темы (еда, животные, транспорт) запоминаются вместе — как сеть, а не по одному.",
    "Ставь цель на день" to "Не 'учить английский', а 'выучить 10 новых слов'. Конкретные цели работают лучше."
)

// ============================================================
// ГЛАВНЫЙ КЛАСС — ACTIVITY
// ============================================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Получаем контекст
            val context = LocalContext.current
            // Хранилище аккаунтов
            val storage = remember { AccountStorage(context) }

            // ==========================================
            // СОСТОЯНИЯ
            // ==========================================
            var currentUser by remember { mutableStateOf(storage.getCurrentUser()) }
            var screen by remember { mutableStateOf(if (currentUser.isNotBlank()) "home" else "login") }

            var isDarkTheme by remember { mutableStateOf(storage.isDarkTheme()) }
            val theme = getTheme(isDarkTheme)

            var notificationsEnabled by remember { mutableStateOf(storage.areNotificationsEnabled()) }

            // Папки текущего пользователя
            val folders = remember(currentUser) {
                if (currentUser.isBlank()) {
                    mutableStateListOf<Folder>()
                } else {
                    val saved = storage.loadUserFolders(currentUser)
                    if (saved.isEmpty()) {
                        val defaults = defaultFolders()
                        storage.saveUserFolders(currentUser, defaults)
                        mutableStateListOf<Folder>().also { it.addAll(defaults) }
                    } else {
                        saved
                    }
                }
            }

            // Автосохранение при изменении списка папок
            LaunchedEffect(folders.size, currentUser) {
                if (currentUser.isNotBlank()) {
                    storage.saveUserFolders(currentUser, folders.toList())
                }
            }

            // Прогресс
            var lastStudiedFolder by remember { mutableStateOf<Folder?>(null) }
            var learnedWords by remember { mutableStateOf(0) }
            val totalWords = lastStudiedFolder?.words?.size ?: 1
            val progress: Float = if (totalWords > 0) learnedWords.toFloat() / totalWords else 0f
            var currentFolder by remember { mutableStateOf<Folder?>(null) }

            MaterialTheme {
                // РОУТЕР
                when (screen) {
                    "login" -> LoginScreen(storage, theme,
                        onLogin = { login, password ->
                            if (storage.checkLogin(login, password)) {
                                storage.setCurrentUser(login); currentUser = login; screen = "home"
                            }
                        },
                        onGoToRegister = { screen = "register" }
                    )
                    "register" -> RegisterScreen(storage, theme,
                        onRegister = { login, password, email ->
                            storage.addUser(login, password, email)
                            storage.setCurrentUser(login)
                            currentUser = login
                            screen = "home"
                        },
                        onBack = { screen = "login" }
                    )
                    "home" -> HomeScreen(theme, progress, lastStudiedFolder?.theme ?: "нет темы",
                        onStartStudy = { screen = "folders" },
                        onCreateFolder = { screen = "create_folder" },
                        onOpenFolders = { screen = "folders" },
                        onOpenProfile = { screen = "profile" },
                        onUseReadyTheme = { screen = "folders" }
                    )
                    "create_folder" -> CreateFolderScreen(theme,
                        onBack = { screen = "home" },
                        onSave = { name, themeStr, words ->
                            folders.add(Folder(folders.size + 1, name, "Тема: $themeStr", words))
                            screen = "folders"
                        }
                    )
                    "folders" -> FoldersScreen(theme, folders,
                        onBack = { screen = "home" },
                        onOpenFolder = { folder ->
                            currentFolder = folder; lastStudiedFolder = folder; screen = "folder_detail"
                        },
                        onAddFolder = { screen = "create_folder" }
                    )
                    "folder_detail" -> {
                        currentFolder?.let { folder ->
                            FolderDetailScreen(theme, folder,
                                onBack = { screen = "folders" },
                                onStudy = { lastStudiedFolder = folder; screen = "study" },
                                onTestDirect = { lastStudiedFolder = folder; screen = "test" },
                                onAddWord = { screen = "add_word" }
                            )
                        }
                    }
                    "add_word" -> {
                        currentFolder?.let { folder ->
                            AddWordScreen(theme, folder,
                                onBack = { screen = "folder_detail" },
                                onAdd = { word, translation ->
                                    val idx = folders.indexOfFirst { it.id == folder.id }
                                    if (idx != -1) {
                                        val newWords = folder.words.toMutableList()
                                        newWords.add(WordCard(word, translation))
                                        folders[idx] = folder.copy(words = newWords)
                                        currentFolder = folders[idx]
                                    }
                                    screen = "folder_detail"
                                }
                            )
                        }
                    }
                    "study" -> {
                        val folder = currentFolder ?: lastStudiedFolder ?: folders.firstOrNull()
                        if (folder != null) {
                            StudyScreen(theme, folder,
                                onBack = { screen = "home" },
                                onFinish = { learnedWords = folder.words.size; screen = "test" }
                            )
                        }
                    }
                    "test" -> {
                        val folder = currentFolder ?: lastStudiedFolder ?: folders.firstOrNull()
                        if (folder != null) {
                            TestScreen(theme, folder,
                                onBack = { screen = "study" },
                                onFinish = { screen = "home" }
                            )
                        }
                    }
                    "profile" -> ProfileScreen(theme, currentUser, storage.getEmail(currentUser),
                        onBack = { screen = "home" },
                        onLogout = {
                            storage.clearCurrentUser()
                            currentUser = ""; folders.clear()
                            currentFolder = null; lastStudiedFolder = null; learnedWords = 0
                            screen = "login"
                        },
                        onSettings = { screen = "settings" }
                    )
                    "settings" -> SettingsScreen(theme, isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme; storage.setDarkTheme(isDarkTheme) },
                        notificationsEnabled = notificationsEnabled,
                        onToggleNotifications = { notificationsEnabled = !notificationsEnabled; storage.setNotificationsEnabled(notificationsEnabled) },
                        onBack = { screen = "profile" }
                    )
                }
            }
        }
    }
}

// ============================================================
// ОБЩИЕ КОМПОНЕНТЫ (Кнопка и Шапка)
// ============================================================
@Composable
fun AppButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = if (enabled) PurpleBtn else PurpleBtn.copy(alpha = 0.4f)),
        elevation = ButtonDefaults.elevation(0.dp)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = AppFont, textAlign = TextAlign.Center)
    }
}

@Composable
fun CardlyInput(
    value: String, onValueChange: (String) -> Unit, label: String = "",
    placeholder: String = "", isPassword: Boolean = false, theme: AppTheme, modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            Text(label, color = theme.text, fontSize = 13.sp, fontFamily = AppFont, modifier = Modifier.padding(bottom = 6.dp, start = 6.dp))
        }
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, color = theme.text.copy(alpha = 0.7f), fontFamily = AppFont) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = PurpleBtn, focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent, textColor = Color.White, cursorColor = Color.White
            )
        )
    }
}

@Composable
fun CardlyTopBar(theme: AppTheme, title: String = "", onBack: (() -> Unit)? = null, showLogo: Boolean = false, onProfileClick: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = theme.text)
            }
        }
        if (showLogo) {
            Text("Cardly", color = theme.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(23.dp))
                .background(if (theme.isDark) Color(0xFFA0A0A0) else Color(0xFFD0D0D0))
                .clickable { onProfileClick?.invoke() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = if (theme.isDark) Color.White else Color.Black, modifier = Modifier.size(28.dp))
            }
        } else {
            Spacer(modifier = Modifier.width(36.dp))
            Text(title, color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("Cardly", color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
        }
    }
}

// ============================================================
// 1. ЭКРАН ВХОДА
// ============================================================
@Composable
fun LoginScreen(storage: AccountStorage, theme: AppTheme, onLogin: (String, String) -> Unit, onGoToRegister: () -> Unit) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(theme.bg).padding(horizontal = 24.dp)) {
        Spacer(modifier = Modifier.height(60.dp))
        Text("Cardly", color = theme.text, fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
        Spacer(modifier = Modifier.height(60.dp))
        Text("Вход", color = theme.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
        Spacer(modifier = Modifier.height(40.dp))

        CardlyInput(value = login, onValueChange = { login = it; error = "" }, label = "Логин", theme = theme)
        Spacer(modifier = Modifier.height(18.dp))
        CardlyInput(value = password, onValueChange = { password = it; error = "" }, label = "Пароль", isPassword = true, theme = theme)

        if (error.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(error, color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
        }
        Spacer(modifier = Modifier.height(24.dp))
        AppButton(text = "Войти", onClick = {
            if (login.isBlank() || password.isBlank()) error = "Заполни все поля"
            else if (!storage.isUserExists(login.trim())) error = "Такого пользователя нет"
            else if (!storage.checkLogin(login.trim(), password)) error = "Неверный пароль"
            else onLogin(login.trim(), password)
        })
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(onClick = onGoToRegister, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = theme.cardBg), border = null) {
            Text("Зарегистрироваться", color = theme.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

// ============================================================
// 2. ЭКРАН РЕГИСТРАЦИИ
// ============================================================
@Composable
fun RegisterScreen(storage: AccountStorage, theme: AppTheme, onRegister: (String, String, String) -> Unit, onBack: () -> Unit) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(theme.bg).padding(horizontal = 24.dp)) {
        Spacer(modifier = Modifier.height(40.dp))
        Text("Cardly", color = theme.text, fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
        Spacer(modifier = Modifier.height(40.dp))
        Text("Регистрация", color = theme.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
        Spacer(modifier = Modifier.height(40.dp))

        CardlyInput(value = login, onValueChange = { login = it; error = "" }, label = "Логин", theme = theme)
        Spacer(modifier = Modifier.height(18.dp))
        CardlyInput(value = password, onValueChange = { password = it; error = "" }, label = "Пароль", isPassword = true, theme = theme)
        Spacer(modifier = Modifier.height(18.dp))
        CardlyInput(value = email, onValueChange = { email = it; error = "" }, label = "Почта (например, имя@mail.ru)", theme = theme)

        if (error.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(error, color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
        }
        Spacer(modifier = Modifier.height(24.dp))
        AppButton(text = "Зарегистрироваться", onClick = {
            val cleanLogin = login.trim()
            val cleanEmail = email.trim()
            when {
                cleanLogin.isBlank() -> error = "Введи логин"
                !isValidLogin(cleanLogin) -> error = "Логин: минимум 3 символа"
                storage.isUserExists(cleanLogin) -> error = "Такой логин уже занят"
                password.length < 3 -> error = "Пароль минимум 3 символа"
                cleanEmail.isBlank() -> error = "Введи почту"
                !isValidEmail(cleanEmail) -> error = "Неверная почта"
                else -> onRegister(cleanLogin, password, cleanEmail)
            }
        })
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = theme.cardBg), border = null) {
            Text("← Назад ко входу", color = theme.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

// ============================================================
// 3. ГЛАВНЫЙ ЭКРАН
// ============================================================
@Composable
fun HomeScreen(theme: AppTheme, progress: Float, lastFolderName: String, onStartStudy: () -> Unit,
               onCreateFolder: () -> Unit, onOpenFolders: () -> Unit, onOpenProfile: () -> Unit, onUseReadyTheme: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    var tipPairIndex by remember { mutableStateOf(0) }
    val tip1 = allTips[tipPairIndex]
    val tip2 = allTips[(tipPairIndex + 1) % allTips.size]

    Scaffold(
        backgroundColor = theme.bg,
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().background(theme.bg).padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                BottomNavItem(Icons.Default.Home, selectedTab == 0, theme) { selectedTab = 0 }
                BottomNavItem(Icons.Default.Add, selectedTab == 1, theme) { selectedTab = 1; onCreateFolder() }
                BottomNavItem(Icons.Default.List, selectedTab == 2, theme) { selectedTab = 2; onOpenFolders() }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            CardlyTopBar(theme = theme, showLogo = true, onProfileClick = onOpenProfile)
            Spacer(modifier = Modifier.height(12.dp))

            // Две карточки со стопкой
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(modifier = Modifier.weight(1f).height(110.dp)) {
                    Box(modifier = Modifier.fillMaxSize().offset(x = 10.dp).clip(RoundedCornerShape(20.dp)).background(PurpleSoft))
                    Box(modifier = Modifier.fillMaxSize().offset(x = 5.dp).clip(RoundedCornerShape(20.dp)).background(PurpleBtn))
                    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(theme.cardBg)
                        .clickable { onCreateFolder() }.padding(14.dp), contentAlignment = Alignment.CenterStart) {
                        Text("Создать тему", color = theme.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
                    }
                }
                Box(modifier = Modifier.weight(1f).height(110.dp)) {
                    Box(modifier = Modifier.fillMaxSize().offset(x = 10.dp).clip(RoundedCornerShape(20.dp)).background(PurpleSoft))
                    Box(modifier = Modifier.fillMaxSize().offset(x = 5.dp).clip(RoundedCornerShape(20.dp)).background(PurpleBtn))
                    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(theme.cardBg)
                        .clickable { onUseReadyTheme() }.padding(14.dp), contentAlignment = Alignment.CenterStart) {
                        Text("Использовать\nготовую тему", color = theme.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            AppButton(text = "Начать обучение", onClick = onStartStudy)
            Spacer(modifier = Modifier.height(20.dp))

            // Прогресс + Советы
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(theme.cardBg).padding(14.dp)) {
                Text("Ваш прогресс последней темы ($lastFolderName): ${(progress * 100).toInt()}%",
                    color = theme.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = PurpleBtn, backgroundColor = theme.grayText.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TipCardBig(tipPairIndex + 1, tip1.first, tip1.second, theme, Modifier.weight(1f))
                    TipCardBig(((tipPairIndex + 1) % allTips.size) + 1, tip2.first, tip2.second, theme, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PurpleBtn.copy(alpha = 0.3f))
                    .clickable { tipPairIndex = (tipPairIndex + 2) % allTips.size }.padding(10.dp), contentAlignment = Alignment.Center) {
                    Text("👆 Показать другие советы", color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
                }
            }
        }
    }
}

@Composable
fun TipCardBig(number: Int, title: String, text: String, theme: AppTheme, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(PurpleBtn).padding(14.dp)) {
        Text("Совет №$number", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppFont)
        Spacer(modifier = Modifier.height(10.dp))
        Text(text, color = Color.White, fontSize = 12.sp, fontFamily = AppFont, lineHeight = 17.sp)
    }
}

@Composable
fun BottomNavItem(icon: ImageVector, selected: Boolean, theme: AppTheme, onClick: () -> Unit) {
    Icon(icon, contentDescription = null, tint = if (selected) PurpleBtn else theme.text,
        modifier = Modifier.size(30.dp).clickable { onClick() })
}

// ============================================================
// 4. СОЗДАНИЕ ПАПКИ
// ============================================================
@Composable
fun CreateFolderScreen(theme: AppTheme, onBack: () -> Unit, onSave: (String, String, List<WordCard>) -> Unit) {
    var folderName by remember { mutableStateOf("") }
    var folderTheme by remember { mutableStateOf("") }
    var word by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    val cards = remember { mutableStateListOf<WordCard>() }

    Column(modifier = Modifier.fillMaxSize().background(theme.bg)) {
        CardlyTopBar(theme = theme, title = "Создание папки", onBack = onBack)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            CardlyInput(value = folderName, onValueChange = { folderName = it }, label = "Название папки", theme = theme)
            Spacer(modifier = Modifier.height(12.dp))
            CardlyInput(value = folderTheme, onValueChange = { folderTheme = it }, label = "Тема", theme = theme)
            Spacer(modifier = Modifier.height(18.dp))
            Divider(color = theme.grayText.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(14.dp))
            Text("Карточки (${cards.size})", color = theme.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
            Spacer(modifier = Modifier.height(12.dp))
            CardlyInput(value = word, onValueChange = { word = it }, label = "Слово", theme = theme)
            Spacer(modifier = Modifier.height(12.dp))
            CardlyInput(value = translation, onValueChange = { translation = it }, label = "Перевод", theme = theme)
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(text = "Добавить карточку", onClick = {
                if (word.isNotBlank() && translation.isNotBlank()) {
                    cards.add(WordCard(word, translation)); word = ""; translation = ""
                }
            })
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(cards) { card ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                        .background(theme.cardBg).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(card.original, color = theme.text, fontSize = 13.sp, fontFamily = AppFont)
                        Text(card.translation, color = theme.text, fontSize = 13.sp, fontFamily = AppFont)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(text = "Сохранить папку", onClick = {
                if (folderName.isNotBlank() && cards.isNotEmpty()) onSave(folderName, folderTheme.ifBlank { "Разное" }, cards.toList())
            })
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ============================================================
// 5. СПИСОК ПАПОК
// ============================================================
@Composable
fun FoldersScreen(theme: AppTheme, folders: List<Folder>, onBack: () -> Unit, onOpenFolder: (Folder) -> Unit, onAddFolder: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(theme.bg)) {
        CardlyTopBar(theme = theme, title = "Ваши папки", onBack = onBack)
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(folders) { folder ->
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(theme.cardBg)
                    .border(1.dp, PurpleStroke.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                    .clickable { onOpenFolder(folder) }.padding(16.dp)) {
                    Column {
                        Text(folder.name, color = theme.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(folder.theme, color = theme.grayText, fontSize = 13.sp, fontFamily = AppFont)
                        Text("Слов: ${folder.words.size}", color = theme.grayText, fontSize = 13.sp, fontFamily = AppFont)
                    }
                }
            }
            item {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(theme.cardBg)
                    .clickable { onAddFolder() }.padding(16.dp)) {
                    Text("+ Добавить папку", color = theme.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
                }
            }
        }
    }
}

// ============================================================
// 6. ВНУТРИ ПАПКИ
// ============================================================
@Composable
fun FolderDetailScreen(theme: AppTheme, folder: Folder, onBack: () -> Unit, onStudy: () -> Unit, onTestDirect: () -> Unit, onAddWord: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(theme.bg)) {
        CardlyTopBar(theme = theme, title = folder.theme, onBack = onBack)
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(folder.words) { word ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(theme.cardBg)
                    .padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(word.original, color = theme.text, fontSize = 14.sp, fontFamily = AppFont)
                    Text(word.translation, color = theme.text, fontSize = 14.sp, fontFamily = AppFont)
                }
            }
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppButton(text = "Добавить", onClick = onAddWord, modifier = Modifier.weight(1f))
                AppButton(text = "Учить", onClick = onStudy, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            AppButton(text = "Пройти тест", onClick = onTestDirect)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ============================================================
// 6.5 ЭКРАН ДОБАВЛЕНИЯ СЛОВА
// ============================================================
@Composable
fun AddWordScreen(theme: AppTheme, folder: Folder, onBack: () -> Unit, onAdd: (String, String) -> Unit) {
    var word by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().background(theme.bg)) {
        CardlyTopBar(theme = theme, title = "Добавить слово", onBack = onBack)
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Папка: ${folder.name}", color = theme.grayText, fontSize = 14.sp, fontFamily = AppFont)
            Spacer(modifier = Modifier.height(20.dp))
            CardlyInput(value = word, onValueChange = { word = it }, label = "Слово", theme = theme)
            Spacer(modifier = Modifier.height(14.dp))
            CardlyInput(value = translation, onValueChange = { translation = it }, label = "Перевод", theme = theme)
            Spacer(modifier = Modifier.height(24.dp))
            AppButton(text = "Сохранить", onClick = {
                if (word.isNotBlank() && translation.isNotBlank()) onAdd(word, translation)
            })
        }
    }
}

// ============================================================
// 7. ТРЕНИРОВКА (БЕЗ ВВОДА ТЕКСТА И ПРОВЕРКИ)
// ============================================================
@Composable
fun StudyScreen(theme: AppTheme, folder: Folder, onBack: () -> Unit, onFinish: () -> Unit) {
    var index by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    val currentWord = folder.words.getOrNull(index) ?: return

    Column(modifier = Modifier.fillMaxSize().background(theme.bg).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CardlyTopBar(theme = theme, title = folder.theme, onBack = onBack)
        Text("${index + 1}/${folder.words.size}", color = theme.grayText, fontFamily = AppFont, fontSize = 14.sp, modifier = Modifier.align(Alignment.End))
        Spacer(modifier = Modifier.weight(1f))

        // Карточка
        Box(modifier = Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(28.dp)).background(theme.cardBg)
            .border(1.dp, PurpleStroke.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
            .clickable { isFlipped = !isFlipped }, contentAlignment = Alignment.Center) {
            Text(text = if (isFlipped) currentWord.translation else currentWord.original,
                color = theme.text, fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Кнопка "Дальше"
        AppButton(text = if (index < folder.words.size - 1) "Следующее слово" else "К тесту", onClick = {
            if (index < folder.words.size - 1) {
                index++; isFlipped = false
            } else {
                onFinish()
            }
        })
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ============================================================
// 8. ТЕСТ (20 ВОПРОСОВ)
// ============================================================
@Composable
fun TestScreen(theme: AppTheme, folder: Folder, onBack: () -> Unit, onFinish: () -> Unit) {
    var currentQuestion by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf("") }
    var answerChecked by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }

    // 20 вопросов или все слова, если их меньше
    val totalQuestions = minOf(20, folder.words.size)
    val currentWord = folder.words[currentQuestion]

    // 4 варианта: 1 правильный + 3 случайных
    val options = remember(currentQuestion) {
        val wrong = folder.words.map { it.translation }.filter { it != currentWord.translation }.shuffled().take(3)
        (wrong + currentWord.translation).shuffled()
    }

    Column(modifier = Modifier.fillMaxSize().background(theme.bg).padding(horizontal = 16.dp)) {
        CardlyTopBar(theme = theme, title = "Вопрос ${currentQuestion + 1}", onBack = onBack)

        val progress = (currentQuestion + 1).toFloat() / totalQuestions
        Text("Прогресс по тесту: ${(progress * 100).toInt()}%", color = theme.grayText, fontSize = 11.sp, fontFamily = AppFont)
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
            color = PurpleBtn, backgroundColor = theme.grayText.copy(alpha = 0.3f))

        Spacer(modifier = Modifier.height(20.dp))
        Text("Вопрос ${currentQuestion + 1} из $totalQuestions", color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
        Spacer(modifier = Modifier.height(6.dp))
        Text("${currentWord.original} это...", color = theme.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
        Spacer(modifier = Modifier.height(24.dp))

        options.forEach { option ->
            val isSelected = selectedAnswer == option
            val isRight = option == currentWord.translation
            val borderColor = when {
                answerChecked && isRight && !isCorrect -> SuccessGreen
                answerChecked && isSelected && !isRight -> ErrorRed
                answerChecked && isSelected && isRight -> SuccessGreen
                !answerChecked && isSelected -> PurpleStroke
                else -> Color.Transparent
            }
            val borderWidth = when {
                answerChecked && (isSelected || (isRight && !isCorrect)) -> 2.dp
                !answerChecked && isSelected -> 2.dp
                else -> 0.dp
            }
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(28.dp))
                .background(theme.cardBg).border(borderWidth, borderColor, RoundedCornerShape(28.dp))
                .clickable { selectedAnswer = option; answerChecked = false; isCorrect = false }.padding(18.dp)) {
                Text(option, color = theme.text, fontSize = 16.sp, fontFamily = AppFont)
            }
        }

        if (answerChecked && !isCorrect) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Неверно! Выбери другой вариант или проверь снова.", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
        }

        Spacer(modifier = Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppButton(text = "Прошлый", onClick = {
                if (currentQuestion > 0) { currentQuestion--; selectedAnswer = ""; answerChecked = false; isCorrect = false }
            }, modifier = Modifier.weight(1f))
            AppButton(text = if (answerChecked && isCorrect) "Следующий" else "Проверить", onClick = {
                if (!answerChecked) {
                    if (selectedAnswer.isBlank()) return@AppButton
                    answerChecked = true
                    isCorrect = selectedAnswer == currentWord.translation
                } else {
                    if (isCorrect) {
                        if (currentQuestion < totalQuestions - 1) {
                            currentQuestion++; selectedAnswer = ""; answerChecked = false; isCorrect = false
                        } else {
                            onFinish()
                        }
                    }
                }
            }, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ============================================================
// 9. ПРОФИЛЬ
// ============================================================
@Composable
fun ProfileScreen(theme: AppTheme, login: String, email: String, onBack: () -> Unit, onLogout: () -> Unit, onSettings: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(theme.bg)) {
        CardlyTopBar(theme = theme, title = "Профиль", onBack = onBack)
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(110.dp).clip(RoundedCornerShape(55.dp)).background(PurpleBtn), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(login.ifBlank { "Пользователь" }, color = theme.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont)
            Text(email.ifBlank { "user@cardly.app" }, color = theme.grayText, fontSize = 14.sp, fontFamily = AppFont)
            Spacer(modifier = Modifier.height(40.dp))
            AppButton(text = "Настройки", onClick = onSettings)
            Spacer(modifier = Modifier.height(14.dp))
            AppButton(text = "Выйти из аккаунта", onClick = onLogout)
        }
    }
}

// ============================================================
// 10. НАСТРОЙКИ
// ============================================================
@Composable
fun SettingsScreen(
    theme: AppTheme, isDarkTheme: Boolean, onToggleTheme: () -> Unit,
    notificationsEnabled: Boolean, onToggleNotifications: () -> Unit, onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(theme.bg)) {
        CardlyTopBar(theme = theme, title = "Настройки", onBack = onBack)
        Column(modifier = Modifier.padding(16.dp)) {
            SettingItem("Язык интерфейса", "Русский", theme)
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onToggleNotifications() }) {
                SettingItem("Уведомления", if (notificationsEnabled) "Включены" else "Выключены", theme)
            }
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onToggleTheme() }) {
                SettingItem("Тема оформления", if (isDarkTheme) "Тёмная" else "Светлая", theme)
            }
        }
    }
}

@Composable
fun SettingItem(title: String, value: String, theme: AppTheme) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(theme.cardBg).padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = theme.text, fontSize = 15.sp, fontFamily = AppFont)
        Text(value, color = theme.grayText, fontSize = 15.sp, fontFamily = AppFont)
    }
}