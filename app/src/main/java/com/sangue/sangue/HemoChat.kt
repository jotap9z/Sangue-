package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Conversa(
    val nome: String,
    val ultimaMensagem: String,
    val horario: String,
    val nova: Boolean
)

data class Mensagem(
    val texto: String,
    val deHemocentro: Boolean
)

data class NotificacaoHemo(
    val titulo: String,
    val descricao: String,
    val tempo: String,
    val lida: Boolean
)

enum class ChatStep {
    LISTA,
    CONVERSA
}

@Composable
fun HemoChatScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToInicio: () -> Unit = {},
    onNavigateToAgenda: () -> Unit = {},
    onNavigateToEstoque: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToMais: () -> Unit = {}
) {
    var currentStep by remember {
        mutableStateOf(ChatStep.LISTA)
    }
    var conversaSelecionada by remember {
        mutableStateOf("Ana Silva")
    }

    BackHandler {
        if (currentStep == ChatStep.CONVERSA) {
            currentStep = ChatStep.LISTA
        } else {
            onNavigateBack()
        }
    }

    when (currentStep) {
        ChatStep.LISTA -> ChatListView(
            onConversaClick = { nome ->
                conversaSelecionada = nome
                currentStep = ChatStep.CONVERSA
            },
            onInicio = onNavigateToInicio,
            onAgenda = onNavigateToAgenda,
            onEstoque = onNavigateToEstoque,
            onChat = onNavigateToChat,
            onMais = onNavigateToMais
        )

        ChatStep.CONVERSA -> ConversaDetailView(
            nomeDoador = conversaSelecionada,
            onBack = {
                currentStep = ChatStep.LISTA
            },
            onInicio = onNavigateToInicio,
            onAgenda = onNavigateToAgenda,
            onEstoque = onNavigateToEstoque,
            onChat = onNavigateToChat,
            onMais = onNavigateToMais
        )
    }
}

@Composable
private fun BarraChatHemo(
    onInicio: () -> Unit,
    onAgenda: () -> Unit,
    onEstoque: () -> Unit,
    onChat: () -> Unit,
    onMais: () -> Unit
) {
    val acesso = LocalAcessoHemocentro.current
    val vermelho = Color(0xFFE21C2C)
    val cinza = Color(0xFF64748B)

    NavigationBar(
        containerColor = Color(0xFFFDFBFC),
        tonalElevation = 8.dp
    ) {
        val destinos = listOf(
            Triple("Início", Icons.Outlined.Home, onInicio),
            Triple("Agenda", Icons.Outlined.CalendarToday, onAgenda),
            Triple("Estoque", Icons.Outlined.WaterDrop, onEstoque),
            Triple("Chat", Icons.Outlined.ChatBubbleOutline, onChat),
            Triple("Mais", Icons.Outlined.MoreHoriz, onMais)
        ).filter {
            when (it.first) {
                "Agenda" -> acesso.agenda
                "Estoque" -> acesso.estoque
                else -> true
            }
        }

        destinos.forEach { (titulo, icone, acao) ->
            NavigationBarItem(
                selected = titulo == "Chat",
                onClick = acao,
                icon = {
                    Icon(icone, contentDescription = titulo)
                },
                label = {
                    Text(titulo, fontSize = 10.sp)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = vermelho,
                    selectedTextColor = vermelho,
                    unselectedIconColor = cinza,
                    unselectedTextColor = cinza,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListView(
    onConversaClick: (String) -> Unit,
    onInicio: () -> Unit,
    onAgenda: () -> Unit,
    onEstoque: () -> Unit,
    onChat: () -> Unit,
    onMais: () -> Unit
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val borderGray = Color(0xFFE2E8F0)

    var searchQuery by remember { mutableStateOf("") }
    var showNotificacoesSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val notificacoesList = remember {
        mutableStateListOf(
            NotificacaoHemo(
                "Novo check-in realizado",
                "Ana Silva realizou check-in via QR Code.",
                "Há 5 min",
                false
            ),
            NotificacaoHemo(
                "Estoque Crítico",
                "O tipo sanguíneo O- atingiu nível crítico.",
                "Há 30 min",
                false
            ),
            NotificacaoHemo(
                "Mensagem de doador",
                "Carlos Mendes enviou uma nova pergunta no chat.",
                "Há 1 hora",
                true
            )
        )
    }

    val conversas = listOf(
        Conversa(
            "Ana Silva",
            "Preciso levar documento? · 09:30",
            "09:30",
            true
        ),
        Conversa(
            "Carlos Mendes",
            "Consigo remarcar meu horário? · 09:12",
            "09:12",
            false
        ),
        Conversa(
            "Juliana Souza",
            "Obrigada pelo retorno! · ontem",
            "ontem",
            false
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        top = 24.dp,
                        bottom = 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onInicio) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Voltar",
                            tint = textDark
                        )
                    }

                    Text(
                        "Chats com doadores",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textDark
                    )

                    IconButton(
                        onClick = { showNotificacoesSheet = true }
                    ) {
                        Box {
                            Icon(
                                Icons.Outlined.Notifications,
                                "Notificações",
                                tint = textDark
                            )
                            if (notificacoesList.any { !it.lida }) {
                                Box(
                                    modifier = Modifier.size(8.dp)
                                        .background(sangueRed, CircleShape)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                BarraChatHemo(
                    onInicio,
                    onAgenda,
                    onEstoque,
                    onChat,
                    onMais
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
            ) {
                item {
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Buscar conversa",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = borderGray,
                            focusedBorderColor = sangueRed,
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(Modifier.height(24.dp))
                }

                items(conversas) { conversa ->
                    CardConversa(conversa) {
                        onConversaClick(conversa.nome)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                item {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        if (showNotificacoesSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showNotificacoesSheet = false
                },
                sheetState = sheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Notificações do Hemocentro",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textDark,
                            modifier = Modifier.weight(1f)
                        )

                        TextButton(
                            onClick = {
                                for (i in notificacoesList.indices) {
                                    notificacoesList[i] =
                                        notificacoesList[i].copy(lida = true)
                                }
                            },
                            contentPadding = PaddingValues(
                                horizontal = 4.dp,
                                vertical = 2.dp
                            )
                        ) {
                            Text(
                                "Marcar lidas",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = sangueRed,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (notificacoesList.isEmpty()) {
                        Text(
                            "Nenhuma nova notificação.",
                            fontSize = 14.sp,
                            color = textGray,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    } else {
                        notificacoesList.forEach { notif ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (notif.lida) {
                                        Color(0xFFF8FAFC)
                                    } else {
                                        Color(0xFFFEF2F2)
                                    }
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (notif.lida) {
                                        borderGray
                                    } else {
                                        Color(0xFFFECACA)
                                    }
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement =
                                            Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            notif.titulo,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textDark
                                        )
                                        Text(
                                            notif.tempo,
                                            fontSize = 10.sp,
                                            color = textGray
                                        )
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        notif.descricao,
                                        fontSize = 12.sp,
                                        color = textGray,
                                        lineHeight = 16.sp
                                    )
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
fun CardConversa(
    conversa: Conversa,
    onClick: () -> Unit
) {
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val borderGray = Color(0xFFE2E8F0)
    val sangueRed = Color(0xFFE21C2C)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, borderGray),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    conversa.nome,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    conversa.ultimaMensagem,
                    fontSize = 14.sp,
                    color = textGray,
                    maxLines = 1
                )
            }

            if (conversa.nova) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.background(
                        Color(0xFFFEF2F2),
                        RoundedCornerShape(50)
                    ).padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Nova",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = sangueRed
                    )
                }
            }
        }
    }
}

@Composable
fun ConversaDetailView(
    nomeDoador: String,
    onBack: () -> Unit,
    onInicio: () -> Unit,
    onAgenda: () -> Unit,
    onEstoque: () -> Unit,
    onChat: () -> Unit,
    onMais: () -> Unit
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val borderGray = Color(0xFFE2E8F0)

    var mensagemTexto by remember {
        mutableStateOf("")
    }

    val listaMensagens = remember {
        mutableStateListOf(
            Mensagem(
                "Olá! Preciso levar algum documento para doar?",
                false
            ),
            Mensagem(
                "Sim. Leve um documento oficial com foto, como RG ou CNH.",
                true
            )
        )
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column(
                modifier = Modifier.background(Color.White)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        top = 24.dp,
                        bottom = 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Voltar",
                            tint = textDark
                        )
                    }

                    Text(
                        "Chat com $nomeDoador",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textDark,
                        modifier = Modifier.weight(1f).offset(x = (-24).dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier.background(Color.White)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = mensagemTexto,
                        onValueChange = {
                            mensagemTexto = it
                        },
                        placeholder = {
                            Text(
                                "Digite uma resposta...",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = borderGray,
                            focusedBorderColor = sangueRed,
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier = Modifier.size(48.dp)
                            .background(sangueRed, CircleShape)
                            .clip(CircleShape)
                            .clickable {
                                if (mensagemTexto.isNotBlank()) {
                                    listaMensagens.add(
                                        Mensagem(mensagemTexto, true)
                                    )
                                    mensagemTexto = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                BarraChatHemo(
                    onInicio,
                    onAgenda,
                    onEstoque,
                    onChat,
                    onMais
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8FAFC)
                ),
                border = BorderStroke(1.dp, borderGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "$nomeDoador · Online",
                    fontSize = 12.sp,
                    color = textGray,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            listaMensagens.forEach { msg ->
                MessageBubble(msg)
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun MessageBubble(mensagem: Mensagem) {
    val isHemocentro = mensagem.deHemocentro
    val bubbleColor = if (isHemocentro) {
        Color(0xFFE21C2C)
    } else {
        Color(0xFFF1F5F9)
    }
    val textColor = if (isHemocentro) {
        Color.White
    } else {
        Color(0xFF1E293B)
    }
    val alignment = if (isHemocentro) {
        Alignment.End
    } else {
        Alignment.Start
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier.widthIn(max = 280.dp)
                .background(
                    color = bubbleColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isHemocentro) 16.dp else 4.dp,
                        bottomEnd = if (isHemocentro) 4.dp else 16.dp
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                mensagem.texto,
                fontSize = 14.sp,
                color = textColor,
                lineHeight = 20.sp
            )
        }
    }
}