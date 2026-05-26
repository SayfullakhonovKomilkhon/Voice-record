package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.SessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    var isPro by remember { mutableStateOf(sessionManager.isProActive) }
    
    var isAnnualSelected by remember { mutableStateOf(false) }
    var showGooglePlayDialog by remember { mutableStateOf(false) }
    var isPaying by remember { mutableStateOf(false) }
    var paymentSuccess by remember { mutableStateOf(false) }

    val monthlyPrice = "299 ₽"
    val annualPrice = "1 990 ₽"
    val currentPrice = if (isAnnualSelected) annualPrice else monthlyPrice
    val currentPeriod = if (isAnnualSelected) "в год" else "в месяц"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Тарифы и Подписка", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with gorgeous modern presentation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isPro) "Вам доступен тариф PRO!" else "Разблокируйте ИИ-возможности",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPro) "Безграничные расшифровки и экспорт активны" else "Записывайте переговоры без лимитов и делитесь результатами",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Current plan feedback
            Text(
                text = "Текущий тариф: " + if (isPro) "PRO (Премиум)" else "Базовый (Бесплатный)",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isPro) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Compare table plans
            Text(
                text = "Сравнение тарифов",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Comparisons Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureRow(title = "Лимит записи в месяц", free = "До 3 часов", pro = "Без ограничений", highlight = true)
                    FeatureRow(title = "Голосовые профили", free = "До 5 профилей", pro = "Без ограничений", highlight = true)
                    FeatureRow(title = "Экспорт в PDF и Word", free = "❌ Нет", pro = "✓ Есть", highlight = true)
                    FeatureRow(title = "Точная ИИ-сводка решений", free = "✓ Есть", pro = "✓ Есть")
                    FeatureRow(title = "Распознавание спикеров", free = "✓ Есть", pro = "✓ Есть")
                    FeatureRow(title = "Интерактивные теги и фильтры", free = "✓ Есть", pro = "✓ Есть")
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            if (!isPro) {
                // Subscription periods switch
                Text(
                    text = "Выберите вариант оплаты подписки:",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isAnnualSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { isAnnualSelected = false }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Пакет Месячный", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("299 ₽ / мес", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAnnualSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { isAnnualSelected = true }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Пакет Годовой", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("1 990 ₽ / год (скидка 40%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showGooglePlayDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Активировать PRO за $currentPrice", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Оплата защищена сервисом Google Play. Подписку можно отменить в любое время в профиле Google Play.",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                // If Pro is already active
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        sessionManager.isProActive = false
                        isPro = false
                        Toast.makeText(context, "Подписка отменена (симуляция)", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text("Управление / Отменить подписку", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Google Play Billing Simulator Dialog
    if (showGooglePlayDialog) {
        Dialog(onDismissRequest = { if (!isPaying) showGooglePlayDialog = false }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFCFC)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!paymentSuccess) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Shop,
                                    contentDescription = "Google Play",
                                    tint = Color(0xFF34A853),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text("Google Play", fontWeight = FontWeight.Bold, color = Color(0xFF5F6368), style = MaterialTheme.typography.titleMedium)
                            }
                            IconButton(onClick = { if (!isPaying) showGooglePlayDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Договоры и Сделки Регистратор Pro",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF202124),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Подписка: " + if (isAnnualSelected) "PRO 1 Год" else "PRO 1 Месяц",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5F6368)
                        )

                        Text(
                            text = "$currentPrice / $currentPeriod",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF202124),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        HorizontalDivider(color = Color(0xFFE8EAED))

                        Spacer(modifier = Modifier.height(16.dp))

                        // Selected payment card details
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F3F4))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.CreditCard, contentDescription = "Visa", tint = Color(0xFF1A73E8))
                                Column {
                                    Text("Visa •••• 8912", fontWeight = FontWeight.Bold, color = Color(0xFF202124), style = MaterialTheme.typography.bodyMedium)
                                    Text("Баланс подтвержден", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5F6368))
                                }
                            }
                            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = Color(0xFF5F6368))
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (isPaying) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Обработка транзакции...", style = MaterialTheme.typography.bodySmall, color = Color(0xFF5F6368))
                        } else {
                            Button(
                                onClick = {
                                    isPaying = true
                                    // Simulate network billing trip
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(1800)
                                        isPaying = false
                                        paymentSuccess = true
                                        sessionManager.isProActive = true
                                        isPro = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                            ) {
                                Text("Подписаться в 1 клик", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    } else {
                        // Success block
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF34A853),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Оплата прошла успешно!",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF202124)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Тариф PRO успешно активирован. Теперь все ограничения полностью сняты!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5F6368),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                showGooglePlayDialog = false
                                paymentSuccess = false
                                onNavigateBack()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Отлично")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureRow(title: String, free: String, pro: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.5f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = free,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
        Text(
            text = pro,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End
        )
    }
}
