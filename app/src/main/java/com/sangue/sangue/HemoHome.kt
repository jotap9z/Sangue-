package com.sangue.sangue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val VermelhoPainelHemo = Color(0xFFE21C2C)
private val TextoPainelHemo = Color(0xFF1E293B)
private val CinzaPainelHemo = Color(0xFF64748B)

private data class ResumoAgendaPainel(
    val total: Int,
    val aguardandoChegada: Int,
    val atendimentosIniciados: Int,
    val doacoesRealizadas: Int
)

private data class ResumoEstoquePainel(
    val criticos: Int,
    val baixos: Int,
    val naoInformados: Int
)

@Composable
fun HemoHomeScreen(
    onNavigateToInicio: () -> Unit = {},
    onNavigateToAgenda: () -> Unit = {},
    onNavigateToEstoque: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToMais: () -> Unit = {},
    onNavigateToCriarChamado: () -> Unit = {}
) {
    val usuario = SangueApi.usuario
    val usuarioId = usuario?.optLong("id")
    val hemocentroId = usuario?.let {
        if (it.isNull("hemocentroId")) {
            null
        } else {
            it.optLong("hemocentroId")
        }
    }

    val nomeResponsavel = usuario
        ?.optString("nome")
        .orEmpty()
        .trim()
        .substringBefore(" ")

    val lifecycleOwner = LocalLifecycleOwner.current

    var nomeHemocentro by remember(usuarioId, hemocentroId) {
        mutableStateOf<String?>(null)
    }
    var agenda by remember(usuarioId, hemocentroId) {
        mutableStateOf<ResumoAgendaPainel?>(null)
    }
    var estoque by remember(usuarioId, hemocentroId) {
        mutableStateOf<ResumoEstoquePainel?>(null)
    }
    var erroInstituicao by remember(usuarioId, hemocentroId) {
        mutableStateOf<String?>(null)
    }
    var erroAgenda by remember(usuarioId, hemocentroId) {
        mutableStateOf<String?>(null)
    }
    var erroEstoque by remember(usuarioId, hemocentroId) {
        mutableStateOf<String?>(null)
    }
    var carregando by remember(usuarioId, hemocentroId) {
        mutableStateOf(true)
    }
    var atualizar by remember(usuarioId, hemocentroId) {
        mutableStateOf(0)
    }
    var dataConsulta by remember(usuarioId, hemocentroId) {
        mutableStateOf(hojePainelHemo())
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

    // Renova os indicadores na virada do dia usada pela agenda da API.
    LaunchedEffect(usuarioId, hemocentroId) {
        while (true) {
            val proximaVirada = Calendar.getInstance(
                TimeZone.getTimeZone("America/Sao_Paulo")
            ).apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            delay(
                (proximaVirada.timeInMillis - System.currentTimeMillis())
                    .coerceAtLeast(1000L)
            )

            atualizar++
        }
    }

    LaunchedEffect(usuarioId, hemocentroId, atualizar) {
        carregando = true
        erroInstituicao = null
        erroAgenda = null
        erroEstoque = null

        nomeHemocentro = null
        agenda = null
        estoque = null

        val dia = hojePainelHemo()
        dataConsulta = dia

        if (
            !SangueApi.autenticado ||
            usuario?.optString("tipoUsuario") != "hemocentro"
        ) {
            erroInstituicao =
                "Entre com uma conta de hemocentro para acessar o painel."
            carregando = false
            return@LaunchedEffect
        }

        try {
            coroutineScope {
                launch {
                    try {
                        val resposta = SangueApi.objeto(
                            metodo = "GET",
                            caminho = "/api/hemocentros/me"
                        )

                        if (hemocentroId != null) {
                            check(
                                resposta.getLong("id") == hemocentroId
                            )
                        }

                        nomeHemocentro = resposta.getString("nome")
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: ApiException) {
                        erroInstituicao = exception.message
                            ?: "Não foi possível consultar a instituição."
                    } catch (_: Exception) {
                        erroInstituicao =
                            "Não foi possível consultar a instituição."
                    }
                }

                launch {
                    try {
                        val reservas = SangueApi.lista(
                            caminho = "/api/hemocentros/me/agendamentos" +
                                    "?de=$dia&ate=$dia"
                        )

                        val estadosComChegada = setOf(
                            "EM_ATENDIMENTO",
                            "APROVADO_TRIAGEM",
                            "NAO_APTO_TRIAGEM",
                            "DOACAO_REALIZADA",
                            "ENCERRADO_SEM_COLETA"
                        )

                        agenda = ResumoAgendaPainel(
                            total = reservas.size,
                            aguardandoChegada = reservas.count {
                                it.getString("status") == "AGENDADO"
                            },
                            atendimentosIniciados = reservas.count {
                                it.getString("status") in estadosComChegada
                            },
                            doacoesRealizadas = reservas.count {
                                it.getString("status") == "DOACAO_REALIZADA"
                            }
                        )
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: ApiException) {
                        erroAgenda = exception.message
                            ?: "Não foi possível consultar a agenda."
                    } catch (_: Exception) {
                        erroAgenda =
                            "Não foi possível consultar a agenda."
                    }
                }

                launch {
                    try {
                        val resposta = SangueApi.objeto(
                            metodo = "GET",
                            caminho = "/api/hemocentros/me/estoque"
                        )

                        if (hemocentroId != null) {
                            check(
                                resposta.getLong("hemocentroId") ==
                                        hemocentroId
                            )
                        }

                        val itens = resposta.getJSONArray("itens")
                        val situacoes = (0 until itens.length()).map {
                            itens.getJSONObject(it).getString("situacao")
                        }

                        check(
                            situacoes.all {
                                it in setOf(
                                    "NAO_INFORMADO",
                                    "CRITICO",
                                    "BAIXO",
                                    "NORMAL"
                                )
                            }
                        )

                        estoque = ResumoEstoquePainel(
                            criticos = situacoes.count {
                                it == "CRITICO"
                            },
                            baixos = situacoes.count {
                                it == "BAIXO"
                            },
                            naoInformados = situacoes.count {
                                it == "NAO_INFORMADO"
                            }
                        )
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: ApiException) {
                        erroEstoque = exception.message
                            ?: "Não foi possível consultar o estoque."
                    } catch (_: Exception) {
                        erroEstoque =
                            "Não foi possível consultar o estoque."
                    }
                }
            }
        } finally {
            carregando = false
        }
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFFDFBFC)
            ) {
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
                        selected = indice == 0,
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
                            selectedIconColor = VermelhoPainelHemo,
                            selectedTextColor = VermelhoPainelHemo,
                            unselectedIconColor = CinzaPainelHemo,
                            unselectedTextColor = CinzaPainelHemo,
                            indicatorColor = Color(0xFFFEF2F2)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = nomeHemocentro
                    ?: if (carregando) {
                        "Carregando instituição..."
                    } else {
                        "Hemocentro"
                    },
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                color = TextoPainelHemo,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (nomeResponsavel.isBlank()) {
                            "Olá!"
                        } else {
                            "Olá, $nomeResponsavel!"
                        },
                        fontSize = 16.sp,
                        color = CinzaPainelHemo
                    )

                    Text(
                        "Painel do hemocentro",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextoPainelHemo
                    )
                }

                IconButton(
                    onClick = { atualizar++ },
                    enabled = !carregando
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Atualizar painel",
                        tint = VermelhoPainelHemo
                    )
                }
            }

            if (carregando) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = VermelhoPainelHemo
                )
            }

            erroInstituicao?.let {
                Text(
                    text = it,
                    color = VermelhoPainelHemo,
                    fontSize = 13.sp
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = VermelhoPainelHemo
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Agenda de ${dataPainelHemo(dataConsulta)}",
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    val resumo = agenda

                    Text(
                        text = if (resumo != null) {
                            "${resumo.total} agendamentos"
                        } else if (carregando) {
                            "Carregando..."
                        } else {
                            "Consulta indisponível"
                        },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    if (resumo != null) {
                        Text(
                            text = "${resumo.aguardandoChegada} " +
                                    "aguardando chegada",
                            color = Color.White
                        )

                        Text(
                            text = "O total inclui todos os status " +
                                    "dos agendamentos dessa data.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            erroAgenda?.let {
                Text(
                    text = it,
                    color = VermelhoPainelHemo,
                    fontSize = 13.sp
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8FAFC)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Atendimentos da data",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val resumo = agenda

                    if (resumo != null) {
                        Text(
                            "Chegadas registradas: " +
                                    resumo.atendimentosIniciados,
                            color = CinzaPainelHemo
                        )

                        Text(
                            "Doações realizadas: " +
                                    resumo.doacoesRealizadas,
                            color = CinzaPainelHemo
                        )
                    } else {
                        Text(
                            text = if (carregando) {
                                "Consultando atendimentos..."
                            } else {
                                "Não foi possível consultar os atendimentos."
                            },
                            color = CinzaPainelHemo
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8FAFC)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Situação do estoque",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val resumo = estoque

                    if (resumo != null) {
                        Text(
                            "Tipos em situação crítica: ${resumo.criticos}",
                            color = CinzaPainelHemo
                        )

                        Text(
                            "Tipos com estoque baixo: ${resumo.baixos}",
                            color = CinzaPainelHemo
                        )

                        Text(
                            "Tipos sem estoque informado: " +
                                    resumo.naoInformados,
                            color = CinzaPainelHemo
                        )
                    } else {
                        Text(
                            text = if (carregando) {
                                "Consultando estoque..."
                            } else {
                                "Não foi possível consultar o estoque."
                            },
                            color = CinzaPainelHemo
                        )
                    }
                }
            }

            erroEstoque?.let {
                Text(
                    text = it,
                    color = VermelhoPainelHemo,
                    fontSize = 13.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HemoAcaoQuadrada(
                    "Agenda",
                    Icons.Outlined.CalendarMonth,
                    onNavigateToAgenda
                )

                HemoAcaoQuadrada(
                    "Estoque",
                    Icons.Outlined.WaterDrop,
                    onNavigateToEstoque
                )

                HemoAcaoQuadrada(
                    "Emergência",
                    Icons.Outlined.WarningAmber,
                    onNavigateToCriarChamado
                )
            }

            Button(
                onClick = onNavigateToCriarChamado,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VermelhoPainelHemo
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Criar chamado emergencial",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun HemoAcaoQuadrada(
    texto: String,
    icone: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            1.dp,
            Color(0xFFF1F5F9)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icone,
                contentDescription = texto,
                tint = VermelhoPainelHemo,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                texto,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPainelHemo
            )
        }
    }
}

private fun hojePainelHemo(): String {
    return SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.ROOT
    ).apply {
        timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
    }.format(java.util.Date())
}

private fun dataPainelHemo(valor: String): String {
    val partes = valor.take(10).split("-")

    return if (partes.size == 3) {
        "${partes[2]}/${partes[1]}/${partes[0]}"
    } else {
        valor
    }
}