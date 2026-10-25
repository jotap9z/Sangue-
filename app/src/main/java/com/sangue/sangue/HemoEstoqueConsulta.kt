package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
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

private data class ItemEstoqueConsulta(
    val tipo: String,
    val quantidade: Int?,
    val situacao: String
)

@Composable
fun HemoEstoqueConsultaScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToInicio: () -> Unit = {},
    onNavigateToAgenda: () -> Unit = {},
    onNavigateToEstoque: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToMais: () -> Unit = {}
) {
    val vermelho = Color(0xFFE21C2C)
    val id = SangueApi.usuario?.optLong("id")
    val centro = SangueApi.usuario?.optLong("hemocentroId")
    val lifecycleOwner = LocalLifecycleOwner.current

    var itens by remember(id) {
        mutableStateOf<List<ItemEstoqueConsulta>>(emptyList())
    }
    var carregando by remember(id) {
        mutableStateOf(true)
    }
    var erro by remember(id) {
        mutableStateOf<String?>(null)
    }
    var atualizar by remember(id) {
        mutableStateOf(0)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                atualizar++
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(id, centro, atualizar) {
        carregando = true
        erro = null
        itens = emptyList()

        try {
            val resposta = SangueApi.objeto(
                metodo = "GET",
                caminho = "/api/hemocentros/me/estoque"
            )

            check(resposta.getLong("hemocentroId") == centro)

            val lista = resposta.getJSONArray("itens")

            val recebidos = (0 until lista.length()).map { indice ->
                val item = lista.getJSONObject(indice)

                ItemEstoqueConsulta(
                    tipo = item.getString("tipoSanguineo"),
                    quantidade = if (
                        item.isNull("quantidadeDisponivel")
                    ) {
                        null
                    } else {
                        item.getInt("quantidadeDisponivel")
                    },
                    situacao = item.getString("situacao")
                )
            }

            check(
                recebidos.map { it.tipo }.distinct().size ==
                        recebidos.size
            )
            check(
                recebidos.all {
                    it.tipo in setOf(
                        "A+", "A-", "B+", "B-",
                        "AB+", "AB-", "O+", "O-"
                    ) &&
                            it.situacao in setOf(
                        "NORMAL", "BAIXO",
                        "CRITICO", "NAO_INFORMADO"
                    ) &&
                            (it.quantidade == null ||
                                    it.quantidade >= 0)
                }
            )

            itens = recebidos
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ApiException) {
            erro = exception.message
                ?: "Não foi possível consultar o estoque."
        } catch (_: Exception) {
            erro = "Não foi possível consultar o estoque."
        } finally {
            carregando = false
        }
    }

    BackHandler(onBack = onNavigateBack)

    Scaffold(
        containerColor = Color.White,
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
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar"
                    )
                }

                Text(
                    "Estoque de sangue",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { atualizar++ },
                    enabled = !carregando
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Atualizar estoque",
                        tint = vermelho
                    )
                }
            }
        },
        bottomBar = {
            BarraNavegacaoHemo(
                selecionado = "Estoque",
                onInicio = onNavigateToInicio,
                onAgenda = onNavigateToAgenda,
                onEstoque = onNavigateToEstoque,
                onChat = onNavigateToChat,
                onMais = onNavigateToMais
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                "Consulte as quantidades registradas pela instituição. " +
                        "A edição de quantidades não está disponível nesta tela.",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )

            Spacer(Modifier.height(16.dp))

            if (carregando) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = vermelho
                )
            }

            erro?.let {
                Text(it, color = vermelho)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = itens,
                    key = { it.tipo }
                ) { item ->
                    val cor = when (item.situacao) {
                        "NORMAL" -> Color(0xFF16A34A)
                        "BAIXO" -> Color(0xFFF59E0B)
                        "CRITICO" -> vermelho
                        else -> Color(0xFF64748B)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            Color(0xFFE2E8F0)
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                item.tipo,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = vermelho
                            )

                            Text(
                                text = when (item.situacao) {
                                    "NORMAL" -> "Normal"
                                    "BAIXO" -> "Baixo"
                                    "CRITICO" -> "Crítico"
                                    else -> "Não informado"
                                },
                                color = cor,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                item.quantidade?.let {
                                    "$it unidades"
                                } ?: "Quantidade não informada",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}