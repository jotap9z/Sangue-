package com.sangue.sangue

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class NotificacaoItem(
    val titulo: String,
    val mensagem: String,
    val tempo: String,
    val lida: Boolean,
    val icone: ImageVector,
    val corIcone: Color
)

private const val MENSAGEM_CONVITE_HOME =
    "Ei! Venha salvar vidas comigo no Sangue+. " +
            "Encontre hemocentros e acompanhe suas doações!"

private const val MILISSEGUNDOS_POR_DIA_HOME = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToInicio: () -> Unit = {},
    onNavigateToVerificacao: () -> Unit = {},
    onNavigateToCheckin: () -> Unit = {},
    onNavigateToEmergencia: () -> Unit = {},
    onNavigateToHistorico: () -> Unit = {},
    onNavigateToPerfil: () -> Unit = {},
    onNavigateToEstoque: () -> Unit = {},
    onNavigateToHemocentros: () -> Unit = {},
    onAbrirNotificacao: (String, Long?) -> Unit = { _, _ -> }
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val lifecycleOwner = remember(context) {
        encontrarLifecycleOwnerHome(context)
    }

    val shareSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val notificationsSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var showShareSheet by remember {
        mutableStateOf(false)
    }

    var showNotificationsSheet by remember {
        mutableStateOf(false)
    }

    val usuario = SangueApi.usuario
    val usuarioId = usuario?.optLong("id")

    val primeiroNome = usuario
        ?.optString("nome")
        .orEmpty()
        .trim()
        .split(Regex("\\s+"))
        .firstOrNull()
        .orEmpty()

    var intervalo by remember(usuarioId) {
        mutableStateOf<JSONObject?>(null)
    }

    var carregando by remember(usuarioId) {
        mutableStateOf(true)
    }

    var erro by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }

    var referenciaLocal by remember {
        mutableStateOf(referenciaLocalHome())
    }

    // Acompanha a data e o fuso configurados no celular.
    // Atualiza ao entrar, ao retornar do segundo plano
    // e depois da virada do dia local.
    LaunchedEffect(usuarioId, lifecycleOwner) {
        suspend fun acompanharDados() {
            var ultimaReferenciaConsultada: String? = null

            while (currentCoroutineContext().isActive) {
                referenciaLocal = referenciaLocalHome()

                if (usuarioId == null || !SangueApi.autenticado) {
                    intervalo = null
                    carregando = false
                    erro = "Entre novamente para consultar seus dados."
                    return
                }

                val referenciaDaConsulta = referenciaLocal

                if (
                    ultimaReferenciaConsultada != referenciaDaConsulta ||
                    erro != null
                ) {
                    carregando = true
                    erro = null

                    try {
                        val resposta = SangueApi.objeto(
                            metodo = "GET",
                            caminho = "/api/doadores/me/intervalo"
                        )

                        intervalo = resposta
                        ultimaReferenciaConsultada = referenciaDaConsulta
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: ApiException) {
                        intervalo = null
                        erro = exception.message
                            ?: "Não foi possível carregar seus dados."
                    } catch (exception: Exception) {
                        intervalo = null
                        erro = "Não foi possível carregar seus dados."
                    } finally {
                        carregando = false
                    }
                }

                delay(
                    if (erro != null) {
                        60_000L
                    } else {
                        minOf(
                            milissegundosAteMeiaNoiteHome(),
                            60_000L
                        )
                    }
                )
            }
        }

        if (lifecycleOwner != null) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(
                Lifecycle.State.RESUMED
            ) {
                acompanharDados()
            }
        } else {
            acompanharDados()
        }
    }

    val situacao = intervalo
        ?.optString("situacao")
        .orEmpty()

    val proximaData = intervalo
        ?.optString("proximaDataPeloHistorico")
        ?.takeUnless {
            it.isBlank() || it == "null"
        }

    val dataFormatada = proximaData?.let {
        formatarDataHome(it)
    }

    // Usa a data prevista pela API e o dia local do celular.
    // A referência inclui o fuso para recalcular ao mudar de região.
    val diasRestantes = remember(
        proximaData,
        referenciaLocal
    ) {
        proximaData?.let {
            calcularDiasLocaisHome(it)
        }
    }

    val quantidadeDoacoes = intervalo
        ?.optLong("doacoesUltimosDozeMeses", -1L)
        ?.takeIf { it >= 0 }

    val possuiPrevisao = situacao in setOf(
        "AGUARDANDO_INTERVALO",
        "INTERVALO_CUMPRIDO"
    ) && diasRestantes != null

    val temContagem = !carregando &&
            erro == null &&
            possuiPrevisao

    val textoContagem = when {
        carregando -> "Carregando..."

        erro != null -> "Dados indisponíveis"

        possuiPrevisao -> {
            if (diasRestantes == 1L) {
                "1 dia"
            } else {
                "$diasRestantes dias"
            }
        }

        situacao == "PERFIL_PENDENTE" ->
            "Acompanhamento pendente"

        situacao == "SEM_DOACOES_REGISTRADAS" ->
            "Sem doações registradas"

        else -> "Previsão indisponível"
    }

    val coresNavegacao = NavigationBarItemDefaults.colors(
        selectedIconColor = sangueRed,
        selectedTextColor = sangueRed,
        unselectedIconColor = textGray,
        unselectedTextColor = textGray,
        indicatorColor = Color.Transparent
    )

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFFDFBFC),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = onNavigateToInicio,
                    icon = {
                        Icon(
                            painterResource(R.drawable.home),
                            "Início",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text("Início", fontSize = 10.sp)
                    },
                    colors = coresNavegacao
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToCheckin,
                    icon = {
                        Icon(
                            painterResource(R.drawable.agendar),
                            "Check-in",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text("Check-in", fontSize = 10.sp)
                    },
                    colors = coresNavegacao
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToEmergencia,
                    icon = {
                        Icon(
                            painterResource(R.drawable.emergencia),
                            "Emergência",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text("Emergência", fontSize = 10.sp)
                    },
                    colors = coresNavegacao
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHistorico,
                    icon = {
                        Icon(
                            painterResource(R.drawable.historico),
                            "Histórico",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text("Histórico", fontSize = 10.sp)
                    },
                    colors = coresNavegacao
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToPerfil,
                    icon = {
                        Icon(
                            painterResource(R.drawable.icone_perfil),
                            "Perfil",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text("Perfil", fontSize = 10.sp)
                    },
                    colors = coresNavegacao
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(sangueRed, Color.White)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = 40.dp,
                            bottom = 24.dp
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = (-8).dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(
                                    R.drawable.logobranca
                                ),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(70.dp)
                                    .scale(1.8f)
                                    .offset(x = 0.3.dp)
                            )

                            Text(
                                "SANGUE+",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.offset(x = 2.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                showNotificationsSheet = true
                            }
                        ) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = "Notificações",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = if (primeiroNome.isBlank()) {
                            "Olá! 👋"
                        } else {
                            "Olá, $primeiroNome! 👋"
                        },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        "Que bom ter você aqui!",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // PRÓXIMA DOAÇÃO

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            "Próxima doação",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textDark
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    textoContagem,
                                    fontSize = if (temContagem) {
                                        28.sp
                                    } else {
                                        20.sp
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = sangueRed
                                )

                                if (temContagem) {
                                    dataFormatada?.let { data ->
                                        Spacer(
                                            modifier = Modifier.height(4.dp)
                                        )

                                        Text(
                                            data,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textGray
                                        )
                                    }
                                }
                            }

                            Icon(
                                Icons.Outlined.DateRange,
                                contentDescription = "Calendário",
                                tint = sangueRed,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onNavigateToVerificacao,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = sangueRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Agendar doação",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AcaoRapidaItem(
                        icon = Icons.Outlined.LocationOn,
                        texto = "Hemocentros\npróximos",
                        onClick = onNavigateToHemocentros
                    )

                    AcaoRapidaItem(
                        iconRes = R.drawable.estoque,
                        texto = "Estoque de\nsangue",
                        onClick = onNavigateToEstoque
                    )

                    AcaoRapidaItem(
                        iconRes = R.drawable.convide,
                        texto = "Convide um\namigo",
                        onClick = {
                            showShareSheet = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // HISTÓRICO REAL

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = textGray,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(
                                    "Seu histórico",
                                    fontSize = 14.sp,
                                    color = textGray
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Doações registradas",
                                fontSize = 14.sp,
                                color = textDark
                            )

                            Text(
                                quantidadeDoacoes?.toString() ?: "—",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = sangueRed
                            )

                            Text(
                                "Nos últimos 12 meses",
                                fontSize = 12.sp,
                                color = textGray
                            )
                        }

                        Image(
                            painter = painterResource(
                                R.drawable.maocoracao
                            ),
                            contentDescription = "Coração",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(110.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // NOTIFICAÇÕES

        if (showNotificationsSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showNotificationsSheet = false
                },
                sheetState = notificationsSheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp
                )
            ) {
                Box(Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
                    NotificacoesReaisScreen(
                        onBack = { showNotificationsSheet = false },
                        onAbrir = { destino, ref -> showNotificationsSheet = false; onAbrirNotificacao(destino, ref) }
                    )
                }
            }
        }

        // COMPARTILHAMENTO

        if (showShareSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showShareSheet = false
                },
                sheetState = shareSheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 24.dp,
                            vertical = 8.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Convide um amigo",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textDark,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShareAppButtonHome(
                            name = "WhatsApp",
                            iconRes = R.drawable.whatsappicon,
                            outerColor = Color(0xFFDCFCE7)
                        ) {
                            shareToAppHome(
                                context,
                                "com.whatsapp"
                            )
                        }

                        ShareAppButtonHome(
                            name = "Instagram",
                            iconRes = R.drawable.instaicon,
                            outerColor = Color(0xFFFCE7F3)
                        ) {
                            shareToAppHome(
                                context,
                                "com.instagram.android"
                            )
                        }

                        ShareAppButtonHome(
                            name = "Mensagens",
                            imageVector = Icons.AutoMirrored.Outlined.Chat,
                            outerColor = Color(0xFFDBEAFE),
                            iconTint = Color(0xFF3B82F6)
                        ) {
                            sendSmsHome(context)
                        }

                        ShareAppButtonHome(
                            name = "Copiar texto",
                            imageVector = Icons.Outlined.ContentCopy,
                            outerColor = Color(0xFFF1F5F9),
                            iconTint = textGray
                        ) {
                            copyToClipboardHome(context)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEF2F2)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            MENSAGEM_CONVITE_HOME,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textDark,
                            modifier = Modifier.padding(20.dp),
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    TextButton(
                        onClick = {
                            scope.launch {
                                shareSheetState.hide()
                                showShareSheet = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            "Cancelar",
                            color = textGray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// DATA LOCAL E CICLO DE VIDA
// ==========================================

private fun encontrarLifecycleOwnerHome(
    context: Context
): LifecycleOwner? {
    var atual = context

    while (true) {
        if (atual is LifecycleOwner) {
            return atual
        }

        if (atual !is ContextWrapper) {
            return null
        }

        val proximo = atual.baseContext

        if (proximo === atual) {
            return null
        }

        atual = proximo
    }
}

private fun referenciaLocalHome(): String {
    val fuso = TimeZone.getDefault()
    val calendario = Calendar.getInstance(fuso)

    val ano = calendario.get(Calendar.YEAR)
    val diaDoAno = calendario.get(Calendar.DAY_OF_YEAR)

    return "${fuso.id}:$ano:$diaDoAno"
}

private fun milissegundosAteMeiaNoiteHome(): Long {
    val agora = System.currentTimeMillis()

    val proximaMeiaNoite = Calendar.getInstance(
        TimeZone.getDefault()
    ).apply {
        timeInMillis = agora
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return (
            proximaMeiaNoite.timeInMillis - agora + 1_000L
            ).coerceAtLeast(1_000L)
}

private fun calcularDiasLocaisHome(
    dataPrevista: String
): Long? {
    if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(dataPrevista)) {
        return null
    }

    return try {
        val utc = TimeZone.getTimeZone("UTC")

        val formato = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.ROOT
        ).apply {
            isLenient = false
            timeZone = utc
        }

        val destino = formato.parse(dataPrevista)
            ?: return null

        val hojeLocal = Calendar.getInstance(
            TimeZone.getDefault()
        )

        // Representa somente a data local em UTC.
        // Assim, mudanças de horário de verão não alteram
        // a quantidade de dias do calendário.
        val hojeComoData = Calendar.getInstance(utc).apply {
            clear()
            set(
                hojeLocal.get(Calendar.YEAR),
                hojeLocal.get(Calendar.MONTH),
                hojeLocal.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
        }

        (
                (destino.time - hojeComoData.timeInMillis) /
                        MILISSEGUNDOS_POR_DIA_HOME
                ).coerceAtLeast(0L)
    } catch (_: Exception) {
        null
    }
}

private fun formatarDataHome(
    data: String
): String {
    val partes = data.split("-")

    return if (partes.size == 3) {
        "${partes[2]}/${partes[1]}/${partes[0]}"
    } else {
        data
    }
}

// ==========================================
// COMPONENTES AUXILIARES
// ==========================================

@Composable
fun AcaoRapidaItem(
    icon: ImageVector? = null,
    iconRes: Int? = null,
    texto: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier
                .size(64.dp)
                .clickable {
                    onClick?.invoke()
                }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = texto,
                        tint = Color(0xFFE21C2C),
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    iconRes?.let { recurso ->
                        Icon(
                            painterResource(recurso),
                            contentDescription = texto,
                            tint = Color(0xFFE21C2C),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            texto,
            fontSize = 12.sp,
            color = Color(0xFF1E293B),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ShareAppButtonHome(
    name: String,
    iconRes: Int? = null,
    imageVector: ImageVector? = null,
    outerColor: Color,
    iconTint: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable {
            onClick()
        }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    outerColor,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = name,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                imageVector?.let { icone ->
                    Icon(
                        imageVector = icone,
                        contentDescription = name,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
    }
}

private fun shareToAppHome(
    context: Context,
    packageName: String
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            MENSAGEM_CONVITE_HOME
        )
        setPackage(packageName)
    }

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        val alternativa = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                MENSAGEM_CONVITE_HOME
            )
        }

        try {
            context.startActivity(
                Intent.createChooser(
                    alternativa,
                    "Compartilhar via"
                )
            )
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Nenhum aplicativo de compartilhamento disponível.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

private fun sendSmsHome(
    context: Context
) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "smsto:".toUri()
        putExtra(
            "sms_body",
            MENSAGEM_CONVITE_HOME
        )
    }

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            "Aplicativo de mensagens não encontrado.",
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun copyToClipboardHome(
    context: Context
) {
    val clipboard = context.getSystemService(
        Context.CLIPBOARD_SERVICE
    ) as ClipboardManager

    clipboard.setPrimaryClip(
        ClipData.newPlainText(
            "Convite Sangue+",
            MENSAGEM_CONVITE_HOME
        )
    )

    Toast.makeText(
        context,
        "Convite copiado!",
        Toast.LENGTH_SHORT
    ).show()
}