package com.sangue.sangue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
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
import kotlinx.coroutines.CancellationException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private const val TAMANHO_PAGINA_HISTORICO = 20

private data class DoacaoHistorico(
    val id: Long,
    val nomeHemocentro: String,
    val realizadaEm: String,
    val tipoSanguineoConfirmado: String,
    val observacoes: String?
)

@Composable
fun HistoricoScreen(
    onNavigateBack: () -> Unit = {}
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val borderGray = Color(0xFFF1F5F9)

    val usuarioId = SangueApi.usuario?.optLong("id")

    var doacoes by remember(usuarioId) {
        mutableStateOf<List<DoacaoHistorico>>(emptyList())
    }

    var pagina by remember(usuarioId) {
        mutableStateOf(0)
    }

    var atualizacao by remember(usuarioId) {
        mutableStateOf(0)
    }

    var carregando by remember(usuarioId) {
        mutableStateOf(true)
    }

    var erro by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }

    var temMais by remember(usuarioId) {
        mutableStateOf(false)
    }

    var consultaConcluida by remember(usuarioId) {
        mutableStateOf(false)
    }

    LaunchedEffect(usuarioId, pagina, atualizacao) {
        carregando = true
        erro = null

        if (pagina == 0) {
            doacoes = emptyList()
            temMais = false
            consultaConcluida = false
        }

        try {
            if (usuarioId == null || !SangueApi.autenticado) {
                erro = "Entre na sua conta para consultar o histórico."
            } else {
                val resposta = SangueApi.lista(
                    caminho = "/api/doadores/me/doacoes" +
                            "?pagina=$pagina" +
                            "&tamanho=$TAMANHO_PAGINA_HISTORICO"
                )

                val novosRegistros = resposta.map { item ->
                    DoacaoHistorico(
                        id = item.getLong("id"),
                        nomeHemocentro = item.getString(
                            "nomeHemocentro"
                        ),
                        realizadaEm = item.getString(
                            "realizadaEm"
                        ),
                        tipoSanguineoConfirmado = item.getString(
                            "tipoSanguineoConfirmado"
                        ),
                        observacoes = if (
                            item.isNull("observacoes")
                        ) {
                            null
                        } else {
                            item.optString("observacoes")
                                .trim()
                                .takeIf { it.isNotEmpty() }
                        }
                    )
                }

                doacoes = if (pagina == 0) {
                    novosRegistros.distinctBy { it.id }
                } else {
                    (doacoes + novosRegistros)
                        .distinctBy { it.id }
                }

                temMais = resposta.size == TAMANHO_PAGINA_HISTORICO
                consultaConcluida = true
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ApiException) {
            erro = exception.message
                ?: "Não foi possível carregar o histórico."
        } catch (exception: Exception) {
            erro = "Não foi possível carregar o histórico."
        } finally {
            carregando = false
        }
    }

    fun atualizarHistorico() {
        if (carregando) return

        pagina = 0
        atualizacao++
    }

    val tituloResumo = when {
        !consultaConcluida -> "Doações registradas"
        temMais -> "Doações carregadas"
        else -> "Total de doações registradas"
    }

    val quantidadeResumo = when {
        !consultaConcluida -> "—"
        temMais -> "${doacoes.size}+"
        doacoes.size == 1 -> "1 doação"
        else -> "${doacoes.size} doações"
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 24.dp,
                        bottom = 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = textDark
                    )
                }

                Text(
                    "Histórico",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = { atualizarHistorico() },
                    enabled = !carregando
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Atualizar histórico",
                        tint = if (carregando) {
                            Color(0xFFCBD5E1)
                        } else {
                            sangueRed
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 16.dp,
                bottom = 40.dp
            )
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = sangueRed
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                tituloResumo,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 14.sp
                            )

                            Text(
                                quantidadeResumo,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            if (consultaConcluida) {
                                Text(
                                    if (temMais) {
                                        "Carregue mais para consultar o restante."
                                    } else {
                                        "Histórico registrado no Sangue+."
                                    },
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.DateRange,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Doações realizadas",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark
                )

                Text(
                    "Coletas registradas pelo hemocentro.",
                    fontSize = 13.sp,
                    color = textGray,
                    modifier = Modifier.padding(
                        top = 4.dp,
                        bottom = 16.dp
                    )
                )
            }

            if (
                consultaConcluida &&
                doacoes.isEmpty() &&
                erro == null &&
                !carregando
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                "Nenhuma doação registrada",
                                fontWeight = FontWeight.Bold,
                                color = textDark
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Suas doações aparecerão aqui após " +
                                        "o registro da coleta pelo hemocentro.",
                                color = textGray,
                                fontSize = 14.sp,
                                lineHeight = 21.sp
                            )
                        }
                    }
                }
            }

            items(
                items = doacoes,
                key = { it.id }
            ) { doacao ->
                HistoricoCard(
                    status = "Concluída",
                    corStatus = Color(0xFF22C55E),
                    iconeStatus = Icons.Default.Check,
                    data = formatarDataHistorico(
                        doacao.realizadaEm
                    ),
                    local = doacao.nomeHemocentro,
                    volume = "Tipo sanguíneo confirmado: " +
                            doacao.tipoSanguineoConfirmado,
                    bordaColor = borderGray,
                    observacoes = doacao.observacoes
                )
            }

            if (erro != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEF2F2)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                erro.orEmpty(),
                                color = sangueRed,
                                fontSize = 14.sp
                            )

                            if (doacoes.isNotEmpty()) {
                                Text(
                                    "Os registros já carregados " +
                                            "continuam disponíveis.",
                                    color = textGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            TextButton(
                                onClick = {
                                    atualizacao++
                                },
                                enabled = !carregando
                            ) {
                                Text(
                                    "Tentar novamente",
                                    color = sangueRed
                                )
                            }
                        }
                    }
                }
            }

            if (carregando) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = sangueRed
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            "Carregando histórico...",
                            color = textGray
                        )
                    }
                }
            }

            if (
                temMais &&
                !carregando &&
                erro == null
            ) {
                item {
                    OutlinedButton(
                        onClick = {
                            pagina++
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                        , colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) {
                        Text(
                            "Carregar mais",
                            color = sangueRed
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// CARD DE CADA DOAÇÃO
// ==========================================

@Composable
fun HistoricoCard(
    status: String,
    corStatus: Color,
    iconeStatus: ImageVector,
    data: String,
    local: String,
    volume: String,
    bordaColor: Color,
    observacoes: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            1.dp,
            bordaColor
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        corStatus.copy(alpha = 0.1f),
                        RoundedCornerShape(50.dp)
                    )
                    .padding(
                        horizontal = 10.dp,
                        vertical = 4.dp
                    )
            ) {
                Icon(
                    imageVector = iconeStatus,
                    contentDescription = null,
                    tint = corStatus,
                    modifier = Modifier.size(14.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    status,
                    color = corStatus,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                data,
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                color = bordaColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    local,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.WaterDrop,
                    contentDescription = null,
                    tint = Color(0xFFE21C2C),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    volume,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.weight(1f)
                )
            }

            observacoes?.takeIf {
                it.isNotBlank()
            }?.let { texto ->
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Observações do atendimento",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Text(
                    texto,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// ==========================================
// DATA NO FUSO LOCAL DO CELULAR
// ==========================================

private fun formatarDataHistorico(
    valor: String
): String {
    return try {
        // Instant pode incluir frações com até nove dígitos.
        // Elas são removidas apenas para a apresentação em minutos.
        val normalizado = valor.replace(
            Regex("(\\.\\d+)(?=Z|[+-]\\d{2}:?\\d{2}$)"),
            ""
        )

        val entrada = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.ROOT
        ).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val posicao = ParsePosition(0)
        val instante = entrada.parse(
            normalizado,
            posicao
        )

        if (
            instante == null ||
            posicao.index != normalizado.length
        ) {
            "Data indisponível"
        } else {
            SimpleDateFormat(
                "dd/MM/yyyy 'às' HH:mm",
                Locale("pt", "BR")
            ).apply {
                timeZone = TimeZone.getDefault()
            }.format(instante)
        }
    } catch (_: Exception) {
        "Data indisponível"
    }
}