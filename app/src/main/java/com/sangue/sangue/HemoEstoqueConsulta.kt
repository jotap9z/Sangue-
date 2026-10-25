package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URLEncoder

private val VermelhoEstoqueConsulta = Color(0xFFE21C2C)
private val CinzaEstoqueConsulta = Color(0xFF64748B)

private val TiposEstoqueConsulta = setOf(
    "O+", "O-",
    "A+", "A-",
    "B+", "B-",
    "AB+", "AB-"
)

private data class ItemEstoqueConsulta(
    val tipo: String,
    val quantidade: Int?,
    val minimo: Int?,
    val ideal: Int?,
    val situacao: String,
    val versao: Long?
)

private data class EnvioEstoqueConsulta(
    val base: ItemEstoqueConsulta,
    val quantidade: Int,
    val minimo: Int,
    val ideal: Int
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
    val acesso = LocalAcessoHemocentro.current
    val recarregarAcesso = LocalRecarregarAcessoHemo.current

    val podeEditar = acesso.estoque && acesso.escrita

    val id = SangueApi.usuario?.optLong("id")
    val centro = SangueApi.usuario?.optLong("hemocentroId")
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var itens by remember(id, centro) {
        mutableStateOf<List<ItemEstoqueConsulta>>(emptyList())
    }
    var selecionado by remember(id, centro) {
        mutableStateOf<ItemEstoqueConsulta?>(null)
    }
    var quantidade by remember(id, centro) {
        mutableStateOf("")
    }
    var minimo by remember(id, centro) {
        mutableStateOf("")
    }
    var ideal by remember(id, centro) {
        mutableStateOf("")
    }
    var carregando by remember(id, centro) {
        mutableStateOf(true)
    }
    var salvando by remember(id, centro) {
        mutableStateOf(false)
    }
    var consultaValida by remember(id, centro) {
        mutableStateOf(false)
    }
    var exigeConferencia by remember(id, centro) {
        mutableStateOf(false)
    }
    var recarregarFormulario by remember(id, centro) {
        mutableStateOf(false)
    }
    var erroConsulta by remember(id, centro) {
        mutableStateOf<String?>(null)
    }
    var erroFormulario by remember(id, centro) {
        mutableStateOf<String?>(null)
    }
    var mensagem by remember(id, centro) {
        mutableStateOf<String?>(null)
    }
    var atualizar by remember(id, centro) {
        mutableStateOf(0)
    }
    var confirmacao by remember(id, centro) {
        mutableStateOf<EnvioEstoqueConsulta?>(null)
    }

    val salvandoAtual by rememberUpdatedState(salvando)

    fun preencher(item: ItemEstoqueConsulta) {
        selecionado = item
        quantidade = item.quantidade?.toString().orEmpty()
        minimo = item.minimo?.toString().orEmpty()
        ideal = item.ideal?.toString().orEmpty()
        exigeConferencia = false
        erroFormulario = null
    }

    fun voltar() {
        if (salvando) return

        if (selecionado != null) {
            selecionado = null
            confirmacao = null
            quantidade = ""
            minimo = ""
            ideal = ""
            erroFormulario = null
            exigeConferencia = false
        } else {
            onNavigateBack()
        }
    }

    suspend fun consultar(): List<ItemEstoqueConsulta> {
        val resposta = SangueApi.objeto(
            metodo = "GET",
            caminho = "/api/hemocentros/me/estoque"
        )

        check(resposta.getLong("hemocentroId") == centro)

        val lista = resposta.getJSONArray("itens")
        val recebidos = (0 until lista.length()).map {
            lerItemEstoqueConsulta(lista.getJSONObject(it))
        }

        check(
            recebidos.map { it.tipo }.toSet() ==
                    TiposEstoqueConsulta
        )
        check(recebidos.size == TiposEstoqueConsulta.size)

        return recebidos
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, evento ->
            if (
                evento == Lifecycle.Event.ON_RESUME &&
                !salvandoAtual
            ) {
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
        consultaValida = false
        erroConsulta = null

        try {
            val recebidos = consultar()
            itens = recebidos
            consultaValida = true

            val base = selecionado

            if (base != null) {
                val atual = recebidos.first {
                    it.tipo == base.tipo
                }

                if (recarregarFormulario) {
                    preencher(atual)
                } else if (atual != base) {
                    exigeConferencia = true
                    erroFormulario =
                        "Este estoque mudou desde a abertura do formulário. " +
                                "Recarregue os dados antes de salvar."
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ApiException) {
            erroConsulta = exception.message
                ?: "Não foi possível consultar o estoque."
        } catch (_: Exception) {
            erroConsulta =
                "Não foi possível consultar o estoque."
        } finally {
            recarregarFormulario = false
            carregando = false
        }
    }

    fun prepararConfirmacao() {
        if (
            !podeEditar ||
            carregando ||
            salvando ||
            !consultaValida ||
            exigeConferencia
        ) {
            return
        }

        val base = selecionado ?: return

        val qtd = quantidade.trim().toIntOrNull()
        val min = minimo.trim().toIntOrNull()
        val ide = ideal.trim().toIntOrNull()

        erroFormulario = null

        if (qtd == null || qtd !in 0..1_000_000) {
            erroFormulario =
                "Informe uma quantidade inteira entre 0 e 1000000."
            return
        }

        if (min == null || min !in 1..1_000_000) {
            erroFormulario =
                "Informe um nível mínimo inteiro entre 1 e 1000000."
            return
        }

        if (
            ide == null ||
            ide !in 1..1_000_000 ||
            ide <= min
        ) {
            erroFormulario =
                "O nível ideal deve ser maior que o mínimo " +
                        "e não pode ultrapassar 1000000."
            return
        }

        if (
            qtd == base.quantidade &&
            min == base.minimo &&
            ide == base.ideal
        ) {
            erroFormulario = "Não há alterações para salvar."
            return
        }

        confirmacao = EnvioEstoqueConsulta(
            base = base,
            quantidade = qtd,
            minimo = min,
            ideal = ide
        )
    }

    fun salvar(envio: EnvioEstoqueConsulta) {
        if (
            !podeEditar ||
            carregando ||
            salvando ||
            !consultaValida ||
            exigeConferencia ||
            selecionado != envio.base ||
            !SangueApi.autenticado
        ) {
            return
        }

        confirmacao = null
        salvando = true
        exigeConferencia = true
        erroFormulario = null
        erroConsulta = null
        mensagem = null

        scope.launch {
            var aceita = false
            var falha: String? = null

            try {
                try {
                    val tipoCodificado = URLEncoder.encode(
                        envio.base.tipo,
                        "UTF-8"
                    )

                    val resposta = SangueApi.objeto(
                        metodo = "PUT",
                        caminho = "/api/hemocentros/me/estoque/" +
                                tipoCodificado,
                        corpo = JSONObject()
                            .put(
                                "quantidadeDisponivel",
                                envio.quantidade
                            )
                            .put("nivelMinimo", envio.minimo)
                            .put("nivelIdeal", envio.ideal)
                            .put(
                                "versao",
                                envio.base.versao
                                    ?: JSONObject.NULL
                            )
                    )

                    val salvo = lerItemEstoqueConsulta(resposta)

                    check(salvo.tipo == envio.base.tipo)
                    check(
                        salvo.quantidade == envio.quantidade &&
                                salvo.minimo == envio.minimo &&
                                salvo.ideal == envio.ideal
                    )

                    aceita = true
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: ApiException) {
                    falha = when (exception.status) {
                        409 ->
                            "O estoque mudou em outra sessão. " +
                                    "Recarregue os dados antes de editar novamente."

                        0 ->
                            "Não foi possível confirmar o salvamento. " +
                                    "A atualização pode ter sido realizada."

                        in 500..599 ->
                            "Não foi possível confirmar o resultado " +
                                    "da atualização."

                        else ->
                            exception.message
                                ?: "Não foi possível salvar o estoque."
                    }

                    if (exception.status == 403) {
                        recarregarAcesso()
                    }
                } catch (_: Exception) {
                    falha =
                        "Não foi possível conferir a resposta do salvamento."
                }

                // Consulta novamente mesmo se a resposta do PUT
                // tiver sido perdida durante a conexão.
                try {
                    val recebidos = consultar()
                    itens = recebidos
                    consultaValida = true
                    erroConsulta = null

                    if (aceita) {
                        val atual = recebidos.first {
                            it.tipo == envio.base.tipo
                        }

                        mensagem = if (
                            atual.quantidade == envio.quantidade &&
                            atual.minimo == envio.minimo &&
                            atual.ideal == envio.ideal
                        ) {
                            "Estoque ${envio.base.tipo} atualizado e conferido."
                        } else {
                            "Sua atualização foi aceita. " +
                                    "Houve outra alteração em seguida; " +
                                    "a lista mostra os dados mais recentes."
                        }

                        selecionado = null
                        quantidade = ""
                        minimo = ""
                        ideal = ""
                        exigeConferencia = false
                        erroFormulario = null
                    } else {
                        erroFormulario = (
                                falha
                                    ?: "Não foi possível confirmar o salvamento."
                                ) +
                                " Confira a última consulta abaixo " +
                                "e toque em Recarregar dados."
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    consultaValida = false
                    erroConsulta =
                        "Não foi possível consultar o estoque após a operação."

                    erroFormulario = if (aceita) {
                        "A atualização foi aceita, mas não foi possível " +
                                "conferir os dados. Toque em Recarregar dados " +
                                "antes de realizar outra alteração."
                    } else {
                        (
                                falha
                                    ?: "Não foi possível confirmar o salvamento."
                                ) +
                                " Recarregue os dados antes de tentar novamente."
                    }
                }
            } finally {
                salvando = false
            }
        }
    }

    BackHandler {
        voltar()
    }

    val emEdicao = selecionado != null
    val ocupado = carregando || salvando

    val camposHabilitados = podeEditar &&
            !ocupado &&
            consultaValida &&
            !exigeConferencia

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
                IconButton(
                    onClick = { voltar() },
                    enabled = !salvando
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar"
                    )
                }

                Text(
                    text = if (emEdicao) {
                        "Atualizar estoque"
                    } else {
                        "Estoque de sangue"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { atualizar++ },
                    enabled = !ocupado
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Atualizar consulta",
                        tint = VermelhoEstoqueConsulta
                    )
                }
            }
        },
        bottomBar = {
            if (!emEdicao) {
                BarraNavegacaoHemo(
                    selecionado = "Estoque",
                    onInicio = onNavigateToInicio,
                    onAgenda = onNavigateToAgenda,
                    onEstoque = onNavigateToEstoque,
                    onChat = onNavigateToChat,
                    onMais = onNavigateToMais
                )
            }
        }
    ) { padding ->
        val base = selecionado

        if (base == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = if (podeEditar) {
                        "Consulte ou atualize as quantidades " +
                                "registradas na instituição."
                    } else {
                        "Consulta das quantidades registradas na instituição."
                    },
                    fontSize = 13.sp,
                    color = CinzaEstoqueConsulta
                )

                Spacer(Modifier.height(16.dp))

                if (carregando) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = VermelhoEstoqueConsulta
                    )
                }

                mensagem?.let {
                    Text(
                        it,
                        color = Color(0xFF166534)
                    )
                }

                erroConsulta?.let {
                    Text(
                        it,
                        color = VermelhoEstoqueConsulta
                    )

                    TextButton(
                        onClick = { atualizar++ },
                        enabled = !ocupado
                    ) {
                        Text("Tentar novamente")
                    }
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
                                verticalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    item.tipo,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = VermelhoEstoqueConsulta
                                )

                                Text(
                                    rotuloEstoqueConsulta(item.situacao),
                                    color = corEstoqueConsulta(
                                        item.situacao
                                    ),
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    item.quantidade?.let {
                                        "$it unidades"
                                    } ?: "Quantidade não informada",
                                    fontSize = 12.sp,
                                    color = CinzaEstoqueConsulta
                                )

                                if (
                                    item.minimo != null &&
                                    item.ideal != null
                                ) {
                                    Text(
                                        "Mínimo: ${item.minimo}\n" +
                                                "Ideal: ${item.ideal}",
                                        fontSize = 12.sp,
                                        color = CinzaEstoqueConsulta
                                    )
                                }

                                if (podeEditar) {
                                    TextButton(
                                        enabled = !ocupado &&
                                                consultaValida,
                                        onClick = {
                                            mensagem = null
                                            preencher(item)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (
                                                item.versao == null
                                            ) {
                                                "Cadastrar"
                                            } else {
                                                "Editar"
                                            },
                                            color = VermelhoEstoqueConsulta
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (ocupado) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = VermelhoEstoqueConsulta
                    )
                }

                Text(
                    "Tipo sanguíneo ${base.tipo}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = VermelhoEstoqueConsulta
                )

                Text(
                    "Informe a quantidade total disponível. " +
                            "O valor substituirá a quantidade registrada.",
                    fontSize = 13.sp,
                    color = CinzaEstoqueConsulta
                )

                if (!podeEditar && !salvando) {
                    Text(
                        "A edição está indisponível para o acesso atual.",
                        color = VermelhoEstoqueConsulta
                    )
                }

                itens.firstOrNull {
                    it.tipo == base.tipo
                }?.let { atual ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF8FAFC)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Última consulta",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                atual.quantidade?.let {
                                    "$it unidades"
                                } ?: "Quantidade não informada"
                            )

                            Text(
                                "Mínimo: ${
                                    atual.minimo ?: "Não informado"
                                }"
                            )

                            Text(
                                "Ideal: ${
                                    atual.ideal ?: "Não informado"
                                }"
                            )

                            Text(
                                rotuloEstoqueConsulta(atual.situacao),
                                color = corEstoqueConsulta(
                                    atual.situacao
                                )
                            )
                        }
                    }
                }

                CampoNumeroEstoque(
                    titulo = "Quantidade total disponível",
                    valor = quantidade,
                    enabled = camposHabilitados
                ) {
                    quantidade = it
                    erroFormulario = null
                }

                CampoNumeroEstoque(
                    titulo = "Nível mínimo",
                    valor = minimo,
                    enabled = camposHabilitados
                ) {
                    minimo = it
                    erroFormulario = null
                }

                CampoNumeroEstoque(
                    titulo = "Nível ideal",
                    valor = ideal,
                    enabled = camposHabilitados
                ) {
                    ideal = it
                    erroFormulario = null
                }

                Text(
                    "Crítico: abaixo do mínimo.\n" +
                            "Baixo: a partir do mínimo e abaixo do ideal.\n" +
                            "Normal: a partir do ideal.",
                    fontSize = 12.sp,
                    color = CinzaEstoqueConsulta
                )

                erroConsulta?.let {
                    Text(
                        it,
                        color = VermelhoEstoqueConsulta
                    )
                }

                erroFormulario?.let {
                    Text(
                        it,
                        color = VermelhoEstoqueConsulta
                    )
                }

                if (exigeConferencia || !consultaValida) {
                    Text(
                        "Recarregue os dados para conferir " +
                                "o estoque antes de salvar.",
                        fontSize = 13.sp,
                        color = CinzaEstoqueConsulta
                    )
                }

                OutlinedButton(
                    enabled = !ocupado,
                    onClick = {
                        recarregarFormulario = true
                        atualizar++
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Recarregar dados")
                }

                Button(
                    enabled = camposHabilitados,
                    onClick = { prepararConfirmacao() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VermelhoEstoqueConsulta
                    )
                ) {
                    Text(
                        if (salvando) {
                            "Salvando..."
                        } else {
                            "Salvar estoque"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    enabled = !salvando,
                    onClick = { voltar() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Cancelar",
                        color = CinzaEstoqueConsulta
                    )
                }
            }
        }
    }

    confirmacao?.let { envio ->
        AlertDialog(
            onDismissRequest = {
                confirmacao = null
            },
            title = {
                Text("Confirmar atualização?")
            },
            text = {
                Text(
                    "Tipo: ${envio.base.tipo}\n\n" +
                            "Quantidade total: ${envio.quantidade}\n" +
                            "Nível mínimo: ${envio.minimo}\n" +
                            "Nível ideal: ${envio.ideal}\n\n" +
                            "A quantidade informada substituirá " +
                            "o total registrado."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = camposHabilitados &&
                            selecionado == envio.base,
                    onClick = {
                        salvar(envio)
                    }
                ) {
                    Text(
                        "Confirmar",
                        color = VermelhoEstoqueConsulta
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmacao = null
                    }
                ) {
                    Text("Voltar")
                }
            }
        )
    }
}

@Composable
private fun CampoNumeroEstoque(
    titulo: String,
    valor: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValueChange,
        label = {
            Text(titulo)
        },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VermelhoEstoqueConsulta,
            focusedLabelColor = VermelhoEstoqueConsulta,
            unfocusedBorderColor = Color(0xFFE2E8F0)
        )
    )
}

private fun lerItemEstoqueConsulta(
    json: JSONObject
): ItemEstoqueConsulta {
    val item = ItemEstoqueConsulta(
        tipo = json.getString("tipoSanguineo"),
        quantidade = if (
            json.isNull("quantidadeDisponivel")
        ) {
            null
        } else {
            json.getInt("quantidadeDisponivel")
        },
        minimo = if (json.isNull("nivelMinimo")) {
            null
        } else {
            json.getInt("nivelMinimo")
        },
        ideal = if (json.isNull("nivelIdeal")) {
            null
        } else {
            json.getInt("nivelIdeal")
        },
        situacao = json.getString("situacao"),
        versao = if (json.isNull("versao")) {
            null
        } else {
            json.getLong("versao")
        }
    )

    check(item.tipo in TiposEstoqueConsulta)

    if (item.situacao == "NAO_INFORMADO") {
        check(
            item.quantidade == null &&
                    item.minimo == null &&
                    item.ideal == null &&
                    item.versao == null
        )
    } else {
        val qtd = requireNotNull(item.quantidade)
        val min = requireNotNull(item.minimo)
        val ideal = requireNotNull(item.ideal)

        check(
            qtd in 0..1_000_000 &&
                    min in 1..1_000_000 &&
                    ideal in 1..1_000_000 &&
                    ideal > min
        )

        check(requireNotNull(item.versao) >= 0)

        val situacaoEsperada = when {
            qtd < min -> "CRITICO"
            qtd < ideal -> "BAIXO"
            else -> "NORMAL"
        }

        check(item.situacao == situacaoEsperada)
    }

    return item
}

private fun rotuloEstoqueConsulta(
    situacao: String
): String {
    return when (situacao) {
        "NORMAL" -> "Normal"
        "BAIXO" -> "Baixo"
        "CRITICO" -> "Crítico"
        else -> "Não informado"
    }
}

private fun corEstoqueConsulta(
    situacao: String
): Color {
    return when (situacao) {
        "NORMAL" -> Color(0xFF16A34A)
        "BAIXO" -> Color(0xFFF59E0B)
        "CRITICO" -> VermelhoEstoqueConsulta
        else -> CinzaEstoqueConsulta
    }
}