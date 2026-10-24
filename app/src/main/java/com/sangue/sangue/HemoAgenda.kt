package com.sangue.sangue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val VermelhoAgendaReal = Color(0xFFE21C2C)

private data class ReservaAgendaReal(
    val id: Long,
    val nome: String,
    val inicio: String,
    val status: String
)

@Composable
fun HemoAgendaScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToEscanear: () -> Unit = {},
    onNavigateToInicio: () -> Unit = {},
    onNavigateToAgenda: () -> Unit = {},
    onNavigateToEstoque: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToMais: () -> Unit = {}
) {
    val usuarioId = SangueApi.usuario?.optLong("id")
    val lifecycleOwner = LocalLifecycleOwner.current

    var dia by remember(usuarioId) {
        mutableStateOf(hojeAgendaReal())
    }
    var busca by remember(usuarioId) {
        mutableStateOf("")
    }
    var reservas by remember(usuarioId) {
        mutableStateOf<List<ReservaAgendaReal>>(emptyList())
    }
    var selecionada by remember(usuarioId) {
        mutableStateOf<Long?>(null)
    }
    var mostrarHorarios by remember(usuarioId) {
        mutableStateOf(false)
    }
    var carregando by remember(usuarioId) {
        mutableStateOf(false)
    }
    var erro by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var atualizar by remember(usuarioId) {
        mutableStateOf(0)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                atualizar++
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(usuarioId, dia, atualizar) {
        carregando = true
        erro = null
        reservas = emptyList()

        try {
            if (!SangueApi.autenticado) {
                erro = "Entre com a conta do hemocentro."
            } else {
                reservas = SangueApi.lista(
                    caminho = "/api/hemocentros/me/agendamentos" +
                            "?de=$dia&ate=$dia"
                ).map {
                    ReservaAgendaReal(
                        id = it.getLong("id"),
                        nome = it.getString("nomeDoador"),
                        inicio = it.getString("inicio"),
                        status = it.getString("status")
                    )
                }.sortedBy { it.inicio }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ApiException) {
            erro = exception.message
                ?: "Não foi possível carregar a agenda."
        } catch (_: Exception) {
            erro = "Não foi possível carregar a agenda."
        } finally {
            carregando = false
        }
    }

    if (mostrarHorarios) {
        HemoHorariosScreen(
            dataInicial = dia,
            onNavigateBack = {
                mostrarHorarios = false
                atualizar++
            }
        )
        return
    }

    val reservaId = selecionada

    if (reservaId != null) {
        HemoAtendimentoScreen(
            agendamentoId = reservaId,
            onNavigateBack = {
                selecionada = null
                atualizar++
            },
            onEscanear = {
                selecionada = null
                onNavigateToEscanear()
            }
        )
        return
    }

    val filtro = busca.trim()
    val visiveis = reservas.filter {
        filtro.isBlank() ||
                it.nome.contains(filtro, ignoreCase = true) ||
                it.id.toString().contains(filtro) ||
                dataHoraAgendaReal(it.inicio).contains(filtro)
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 24.dp,
                        bottom = 12.dp,
                        start = 12.dp,
                        end = 12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar"
                    )
                }

                Text(
                    text = "Agendamentos",
                    modifier = Modifier.weight(1f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { atualizar++ },
                    enabled = !carregando
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Atualizar agenda",
                        tint = VermelhoAgendaReal
                    )
                }

                IconButton(onClick = onNavigateToEscanear) {
                    Icon(
                        Icons.Outlined.CropFree,
                        contentDescription = "Escanear check-in",
                        tint = VermelhoAgendaReal
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFFFDFBFC)) {
                val destinos = listOf(
                    Triple(
                        "Início",
                        Icons.Outlined.Home,
                        onNavigateToInicio
                    ),
                    Triple(
                        "Agenda",
                        Icons.Outlined.CalendarToday,
                        onNavigateToAgenda
                    ),
                    Triple(
                        "Estoque",
                        Icons.Outlined.WaterDrop,
                        onNavigateToEstoque
                    ),
                    Triple(
                        "Chat",
                        Icons.Outlined.ChatBubbleOutline,
                        onNavigateToChat
                    ),
                    Triple(
                        "Mais",
                        Icons.Outlined.MoreHoriz,
                        onNavigateToMais
                    )
                )

                destinos.forEachIndexed { indice, destino ->
                    NavigationBarItem(
                        selected = indice == 1,
                        onClick = destino.third,
                        icon = {
                            Icon(
                                destino.second,
                                contentDescription = destino.first
                            )
                        },
                        label = {
                            Text(destino.first, fontSize = 10.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VermelhoAgendaReal,
                            selectedTextColor = VermelhoAgendaReal,
                            indicatorColor = Color(0xFFFEF2F2)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            dia = moverDiaAgendaReal(dia, -1)
                        },
                        enabled = !carregando
                    ) {
                        Text("Anterior", color = VermelhoAgendaReal)
                    }

                    Text(
                        text = dataAgendaReal(dia),
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = {
                            dia = moverDiaAgendaReal(dia, 1)
                        },
                        enabled = !carregando
                    ) {
                        Text("Próximo", color = VermelhoAgendaReal)
                    }
                }

                TextButton(
                    onClick = {
                        dia = hojeAgendaReal()
                        atualizar++
                    },
                    enabled = !carregando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ir para hoje", color = VermelhoAgendaReal)
                }
            }

            item {
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it },
                    label = {
                        Text("Buscar nome, reserva ou horário")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Button(
                    onClick = onNavigateToEscanear,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VermelhoAgendaReal
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.Outlined.CropFree,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Escanear check-in")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { mostrarHorarios = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = VermelhoAgendaReal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Horários e vagas",
                        color = VermelhoAgendaReal
                    )
                }
            }

            if (carregando) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = VermelhoAgendaReal
                        )
                    }
                }
            }

            erro?.let { mensagem ->
                item {
                    Text(
                        text = mensagem,
                        color = VermelhoAgendaReal
                    )
                }
            }

            if (
                !carregando &&
                erro == null &&
                visiveis.isEmpty()
            ) {
                item {
                    Text(
                        text = if (reservas.isEmpty()) {
                            "Nenhum agendamento nesta data."
                        } else {
                            "Nenhum agendamento corresponde à busca."
                        },
                        color = Color(0xFF64748B)
                    )
                }
            }

            items(
                items = visiveis,
                key = { it.id }
            ) { reserva ->
                OutlinedCard(
                    onClick = { selecionada = reserva.id },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = reserva.nome,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Reserva #${reserva.id}",
                            color = Color(0xFF64748B)
                        )

                        Text(
                            text = dataHoraAgendaReal(reserva.inicio)
                        )

                        Text(
                            text = statusHemoReal(reserva.status),
                            color = VermelhoAgendaReal,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Abrir atendimento",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

private fun hojeAgendaReal(): String {
    return SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.ROOT
    ).apply {
        timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
    }.format(java.util.Date())
}

private fun moverDiaAgendaReal(
    data: String,
    dias: Int
): String {
    val formato = SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.ROOT
    ).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }

    val calendario = Calendar.getInstance(
        TimeZone.getTimeZone("UTC")
    ).apply {
        time = requireNotNull(formato.parse(data))
        add(Calendar.DAY_OF_MONTH, dias)
    }

    return formato.format(calendario.time)
}

internal fun dataAgendaReal(valor: String): String {
    val partes = valor.take(10).split("-")

    return if (partes.size == 3) {
        "${partes[2]}/${partes[1]}/${partes[0]}"
    } else {
        valor
    }
}

internal fun dataHoraAgendaReal(valor: String): String {
    return if (valor.length >= 16) {
        "${dataAgendaReal(valor)} às ${valor.substring(11, 16)}"
    } else {
        "Data e horário indisponíveis"
    }
}

internal fun statusHemoReal(status: String): String {
    return when (status) {
        "AGENDADO" -> "Agendado"
        "EM_ATENDIMENTO" -> "Chegada registrada"
        "APROVADO_TRIAGEM" -> "Aprovado na triagem presencial"
        "NAO_APTO_TRIAGEM" -> "Não apto na triagem presencial"
        "DOACAO_REALIZADA" -> "Doação realizada"
        "FALTOU" -> "Não compareceu"
        "ENCERRADO_SEM_COLETA" -> "Encerrado sem coleta"
        "CANCELADO_DOADOR" -> "Cancelado pelo doador"
        "CANCELADO_HEMOCENTRO" -> "Cancelado pelo hemocentro"
        else -> "Situação não reconhecida"
    }
}