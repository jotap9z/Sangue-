package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

private val VermelhoAtendimentoReal = Color(0xFFE21C2C)
private val CinzaAtendimentoReal = Color(0xFF64748B)

private enum class AcaoAtendimentoReal {
    APROVAR,
    NAO_APTO,
    INTERVALO,
    DOACAO,
    SEM_COLETA
}

@Composable
fun HemoAtendimentoScreen(
    agendamentoId: Long,
    onNavigateBack: () -> Unit = {},
    onEscanear: () -> Unit = {}
) {
    val usuarioId = SangueApi.usuario?.optLong("id")
    val scope = rememberCoroutineScope()

    var detalhe by remember(usuarioId, agendamentoId) {
        mutableStateOf<JSONObject?>(null)
    }
    var intervalo by remember(usuarioId, agendamentoId) {
        mutableStateOf<JSONObject?>(null)
    }
    var carregando by remember(usuarioId, agendamentoId) {
        mutableStateOf(true)
    }
    var processando by remember(usuarioId, agendamentoId) {
        mutableStateOf(false)
    }
    var exigeConferencia by remember(usuarioId, agendamentoId) {
        mutableStateOf(true)
    }
    var erro by remember(usuarioId, agendamentoId) {
        mutableStateOf<String?>(null)
    }
    var mensagem by remember(usuarioId, agendamentoId) {
        mutableStateOf<String?>(null)
    }
    var atualizar by remember(usuarioId, agendamentoId) {
        mutableStateOf(0)
    }
    var observacaoTriagem by remember(usuarioId, agendamentoId) {
        mutableStateOf("")
    }
    var observacoesColeta by remember(usuarioId, agendamentoId) {
        mutableStateOf("")
    }
    var motivoEncerramento by remember(usuarioId, agendamentoId) {
        mutableStateOf("")
    }
    var regraSelecionada by remember(usuarioId, agendamentoId) {
        mutableStateOf("")
    }
    var tipoSelecionado by remember(usuarioId, agendamentoId) {
        mutableStateOf("")
    }
    var acaoPendente by remember(usuarioId, agendamentoId) {
        mutableStateOf<AcaoAtendimentoReal?>(null)
    }

    suspend fun buscarDetalhes(): JSONObject {
        val resposta = SangueApi.objeto(
            metodo = "GET",
            caminho = "/api/hemocentros/me/agendamentos/" +
                    "$agendamentoId/detalhes"
        )

        check(
            resposta.getJSONObject("agendamento")
                .getLong("id") == agendamentoId
        )

        resposta.getJSONObject("doador")
        resposta.getJSONObject("atendimento")
        resposta.getJSONObject("respostas")
        resposta.getJSONObject("preTriagem")

        return resposta
    }

    LaunchedEffect(usuarioId, agendamentoId, atualizar) {
        carregando = true
        erro = null

        try {
            detalhe = buscarDetalhes()
            exigeConferencia = false
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ApiException) {
            exigeConferencia = true
            erro = exception.message
                ?: "Não foi possível consultar o atendimento."
        } catch (_: Exception) {
            exigeConferencia = true
            erro = "Não foi possível consultar o atendimento."
        } finally {
            carregando = false
        }
    }

    fun executarAcao(acao: AcaoAtendimentoReal) {
        if (
            processando ||
            carregando ||
            exigeConferencia ||
            !SangueApi.autenticado
        ) {
            return
        }

        val statusAtual = detalhe
            ?.getJSONObject("agendamento")
            ?.getString("status")
            ?: return

        val permiteAcao = when (acao) {
            AcaoAtendimentoReal.APROVAR,
            AcaoAtendimentoReal.NAO_APTO ->
                statusAtual == "EM_ATENDIMENTO"

            AcaoAtendimentoReal.INTERVALO,
            AcaoAtendimentoReal.SEM_COLETA ->
                statusAtual in setOf(
                    "EM_ATENDIMENTO",
                    "APROVADO_TRIAGEM"
                )

            AcaoAtendimentoReal.DOACAO ->
                statusAtual == "APROVADO_TRIAGEM"
        }

        if (!permiteAcao) return

        val observacao = observacaoTriagem.trim()
        val observacoes = observacoesColeta.trim()
        val motivo = motivoEncerramento.trim()
        val regra = regraSelecionada
        val tipo = tipoSelecionado

        if (
            acao == AcaoAtendimentoReal.NAO_APTO &&
            observacao.isBlank()
        ) {
            erro = "Informe a orientação da avaliação presencial."
            return
        }

        if (
            acao == AcaoAtendimentoReal.SEM_COLETA &&
            motivo.length !in 3..500
        ) {
            erro = "Informe um motivo entre 3 e 500 caracteres."
            return
        }

        if (
            acao == AcaoAtendimentoReal.INTERVALO &&
            regra !in setOf(
                "DOIS_MESES_QUATRO_DOACOES",
                "TRES_MESES_TRES_DOACOES"
            )
        ) {
            erro = "Selecione a regra definida pela equipe."
            return
        }

        if (
            acao == AcaoAtendimentoReal.DOACAO &&
            (
                    tipo.isBlank() ||
                            intervalo?.optString("situacao") !in setOf(
                        "SEM_DOACOES_REGISTRADAS",
                        "INTERVALO_CUMPRIDO"
                    )
                    )
        ) {
            erro = "Confirme a regra de intervalo e o tipo sanguíneo."
            return
        }

        processando = true
        exigeConferencia = true
        erro = null
        mensagem = null

        if (acao == AcaoAtendimentoReal.INTERVALO) {
            intervalo = null
        }

        scope.launch {
            try {
                val base = "/api/hemocentros/me/agendamentos/" +
                        agendamentoId

                when (acao) {
                    AcaoAtendimentoReal.APROVAR,
                    AcaoAtendimentoReal.NAO_APTO -> {
                        val aprovado =
                            acao == AcaoAtendimentoReal.APROVAR

                        val resposta = SangueApi.objeto(
                            metodo = "POST",
                            caminho = "$base/triagem-presencial",
                            corpo = JSONObject()
                                .put("aprovado", aprovado)
                                .put(
                                    "observacao",
                                    observacao.ifBlank { null }
                                        ?: JSONObject.NULL
                                )
                        )

                        check(
                            resposta.getLong("agendamentoId") ==
                                    agendamentoId
                        )

                        check(
                            resposta.getString("status") ==
                                    if (aprovado) {
                                        "APROVADO_TRIAGEM"
                                    } else {
                                        "NAO_APTO_TRIAGEM"
                                    }
                        )

                        mensagem = "Avaliação presencial registrada."
                    }

                    AcaoAtendimentoReal.INTERVALO -> {
                        val resposta = SangueApi.objeto(
                            metodo = "PUT",
                            caminho = "$base/regra-intervalo",
                            corpo = JSONObject().put("regra", regra)
                        )

                        check(resposta.getString("regra") == regra)
                        resposta.getString("situacao")

                        intervalo = resposta
                        mensagem = "Regra de intervalo salva."
                    }

                    AcaoAtendimentoReal.DOACAO -> {
                        val resposta = SangueApi.objeto(
                            metodo = "POST",
                            caminho = "$base/doacao",
                            corpo = JSONObject()
                                .put("tipoSanguineoConfirmado", tipo)
                                .put(
                                    "observacoes",
                                    observacoes.ifBlank { null }
                                        ?: JSONObject.NULL
                                )
                        )

                        check(
                            resposta.getLong("agendamentoId") ==
                                    agendamentoId
                        )
                        check(
                            resposta.getString(
                                "tipoSanguineoConfirmado"
                            ) == tipo
                        )
                        check(resposta.getLong("id") > 0)

                        mensagem = "Doação registrada na API. " +
                                "O doador poderá consultar o histórico " +
                                "e a previsão da próxima doação."
                    }

                    AcaoAtendimentoReal.SEM_COLETA -> {
                        val resposta = SangueApi.objeto(
                            metodo = "POST",
                            caminho = "$base/sem-coleta",
                            corpo = JSONObject().put("motivo", motivo)
                        )

                        check(
                            resposta.getLong("id") == agendamentoId &&
                                    resposta.getString("status") ==
                                    "ENCERRADO_SEM_COLETA"
                        )

                        mensagem = "Atendimento encerrado sem coleta."
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ApiException) {
                erro = exception.message
                    ?: "Não foi possível confirmar a operação."
            } catch (_: Exception) {
                erro = "Não foi possível conferir a resposta da operação."
            }

            try {
                // Consulta o estado real mesmo se a resposta da
                // alteração tiver sido perdida durante a conexão.
                detalhe = buscarDetalhes()
                exigeConferencia = false
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                exigeConferencia = true
                erro = listOfNotNull(
                    erro,
                    "Atualize o atendimento para conferir a situação " +
                            "antes de realizar outra alteração."
                ).joinToString("\n\n")
            } finally {
                processando = false
            }
        }
    }

    val ocupado = carregando || processando

    BackHandler {
        if (!processando) {
            onNavigateBack()
        }
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
                IconButton(
                    onClick = onNavigateBack,
                    enabled = !processando
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar"
                    )
                }

                Text(
                    text = "Atendimento #$agendamentoId",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                IconButton(
                    onClick = { atualizar++ },
                    enabled = !ocupado && acaoPendente == null
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Atualizar atendimento",
                        tint = VermelhoAtendimentoReal
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (ocupado) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = VermelhoAtendimentoReal
                    )
                }

                Text(
                    text = if (processando) {
                        "Confirmando operação e atualizando atendimento..."
                    } else {
                        "Carregando atendimento..."
                    },
                    color = CinzaAtendimentoReal
                )
            }

            mensagem?.let {
                Text(
                    text = it,
                    color = Color(0xFF15803D),
                    fontWeight = FontWeight.SemiBold
                )
            }

            erro?.let {
                Text(
                    text = it,
                    color = VermelhoAtendimentoReal
                )
            }

            if (exigeConferencia && !ocupado) {
                OutlinedButton(
                    onClick = { atualizar++ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Atualizar atendimento")
                }
            }

            val dados = detalhe

            if (dados != null) {
                val reserva = dados.getJSONObject("agendamento")
                val doador = dados.getJSONObject("doador")
                val atendimento = dados.getJSONObject("atendimento")
                val respostas = dados.getJSONObject("respostas")
                val preTriagem = dados.getJSONObject("preTriagem")
                val status = reserva.getString("status")
                val podeAlterar = !ocupado &&
                        !exigeConferencia &&
                        SangueApi.autenticado

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8FAFC)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = doador.getString("nome"),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = dataHoraAgendaReal(
                                reserva.getString("inicio")
                            )
                        )

                        Text(
                            text = reserva.getString("nomeHemocentro"),
                            color = CinzaAtendimentoReal
                        )

                        Text(
                            text = statusHemoReal(status),
                            color = VermelhoAtendimentoReal,
                            fontWeight = FontWeight.Bold
                        )

                        textoOpcionalAtendimento(
                            atendimento,
                            "observacaoTriagem"
                        )?.let {
                            Text(
                                text = "Observação da triagem: $it",
                                color = CinzaAtendimentoReal
                            )
                        }

                        textoOpcionalAtendimento(
                            dados,
                            "motivoEncerramento"
                        )?.let {
                            Text(
                                text = "Motivo do encerramento: $it",
                                color = CinzaAtendimentoReal
                            )
                        }
                    }
                }

                SecaoAtendimentoReal("Pré-triagem vinculada à reserva")

                Text(
                    text = preTriagem.getString("mensagem"),
                    fontWeight = FontWeight.SemiBold
                )

                val motivos = preTriagem.optJSONArray("motivos")

                if (motivos != null) {
                    for (indice in 0 until motivos.length()) {
                        Text(
                            text = "• ${motivos.getString(indice)}",
                            color = CinzaAtendimentoReal
                        )
                    }
                }

                Text(
                    text = preTriagem.getString("aviso"),
                    color = CinzaAtendimentoReal,
                    fontSize = 13.sp
                )

                Text(
                    text = "Peso informado: ${
                        textoOpcionalAtendimento(respostas, "pesoKg")
                            ?: "Não informado"
                    } kg"
                )

                val perguntas = listOf(
                    "bemDeSaude" to "Declarou estar bem de saúde",
                    "sintomasRecentes" to "Relatou sintomas recentes",
                    "cirurgiaRecente" to "Relatou cirurgia recente",
                    "tatuagemOuPiercingRecente" to
                            "Relatou tatuagem ou piercing recente",
                    "gravidaOuAmamentando" to
                            "Declarou gravidez ou amamentação",
                    "condicaoDeSaudeOuMedicamento" to
                            "Relatou condição de saúde ou medicamento",
                    "descansadoEAlimentado" to
                            "Declarou estar descansado e alimentado",
                    "autorizacaoResponsavel" to
                            "Declarou autorização do responsável"
                )

                perguntas.forEach { (campo, titulo) ->
                    val resposta = if (
                        !respostas.has(campo) ||
                        respostas.isNull(campo)
                    ) {
                        "Não informado ou não aplicável"
                    } else if (respostas.getBoolean(campo)) {
                        "Sim"
                    } else {
                        "Não"
                    }

                    Text(
                        text = "$titulo: $resposta",
                        color = CinzaAtendimentoReal,
                        fontSize = 14.sp
                    )
                }

                textoOpcionalAtendimento(
                    respostas,
                    "observacoes"
                )?.let {
                    Text(
                        text = "Observações do doador: $it",
                        color = CinzaAtendimentoReal
                    )
                }

                if (status == "AGENDADO") {
                    SecaoAtendimentoReal("Check-in pendente")

                    Text(
                        text = "Registre a chegada pelo QR Code antes " +
                                "de realizar a triagem presencial.",
                        color = CinzaAtendimentoReal
                    )

                    Button(
                        onClick = onEscanear,
                        enabled = podeAlterar,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VermelhoAtendimentoReal
                        )
                    ) {
                        Text("Escanear check-in")
                    }
                }

                if (status == "EM_ATENDIMENTO") {
                    SecaoAtendimentoReal("Avaliação presencial")

                    Text(
                        text = "Registre o resultado definido pela equipe " +
                                "após a avaliação presencial.",
                        color = CinzaAtendimentoReal
                    )

                    OutlinedTextField(
                        value = observacaoTriagem,
                        onValueChange = {
                            observacaoTriagem = it.take(500)
                        },
                        enabled = podeAlterar,
                        label = { Text("Observação ou orientação") },
                        supportingText = {
                            Text("${observacaoTriagem.length}/500")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Button(
                        onClick = {
                            acaoPendente = AcaoAtendimentoReal.APROVAR
                        },
                        enabled = podeAlterar,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VermelhoAtendimentoReal
                        )
                    ) {
                        Text("Registrar aprovação na triagem")
                    }

                    OutlinedButton(
                        onClick = {
                            acaoPendente = AcaoAtendimentoReal.NAO_APTO
                        },
                        enabled = podeAlterar &&
                                observacaoTriagem.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Registrar resultado não apto",
                            color = VermelhoAtendimentoReal
                        )
                    }

                    Text(
                        text = "Para o resultado não apto, informe a " +
                                "orientação no campo acima.",
                        color = CinzaAtendimentoReal,
                        fontSize = 13.sp
                    )
                }

                if (
                    status in setOf(
                        "EM_ATENDIMENTO",
                        "APROVADO_TRIAGEM"
                    )
                ) {
                    SecaoAtendimentoReal("Regra de intervalo")

                    Text(
                        text = "Selecione a regra aplicável ao doador, " +
                                "conforme a avaliação da equipe. Não há " +
                                "seleção automática.",
                        color = CinzaAtendimentoReal
                    )

                    val regras = listOf(
                        "DOIS_MESES_QUATRO_DOACOES" to
                                "2 meses e até 4 doações em 12 meses",
                        "TRES_MESES_TRES_DOACOES" to
                                "3 meses e até 3 doações em 12 meses"
                    )

                    regras.forEach { (valor, titulo) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = regraSelecionada == valor,
                                onClick = {
                                    regraSelecionada = valor
                                    intervalo = null
                                },
                                enabled = podeAlterar,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = VermelhoAtendimentoReal
                                )
                            )

                            Text(
                                text = titulo,
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            acaoPendente = AcaoAtendimentoReal.INTERVALO
                        },
                        enabled = podeAlterar &&
                                regraSelecionada.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Salvar e conferir intervalo")
                    }

                    intervalo?.let { resultado ->
                        Text(
                            text = resultado.getString("mensagem"),
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = resultado.getString("aviso"),
                            color = CinzaAtendimentoReal,
                            fontSize = 13.sp
                        )
                    }
                }

                if (status == "APROVADO_TRIAGEM") {
                    SecaoAtendimentoReal("Registro da coleta")

                    Text(
                        text = "Registre somente após a realização da " +
                                "coleta e a confirmação do tipo sanguíneo " +
                                "pela equipe.",
                        color = CinzaAtendimentoReal
                    )

                    val tipos = listOf(
                        "A+", "A-", "B+", "B-",
                        "AB+", "AB-", "O+", "O-"
                    )

                    tipos.chunked(4).forEach { linha ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            linha.forEach { tipo ->
                                FilterChip(
                                    selected = tipoSelecionado == tipo,
                                    onClick = { tipoSelecionado = tipo },
                                    enabled = podeAlterar,
                                    label = { Text(tipo) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = observacoesColeta,
                        onValueChange = {
                            observacoesColeta = it.take(500)
                        },
                        enabled = podeAlterar,
                        label = { Text("Observações da coleta — opcional") },
                        supportingText = {
                            Text("${observacoesColeta.length}/500")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    val intervaloPermite = intervalo
                        ?.optString("situacao") in setOf(
                        "SEM_DOACOES_REGISTRADAS",
                        "INTERVALO_CUMPRIDO"
                    )

                    if (!intervaloPermite) {
                        Text(
                            text = "Salve a regra e confira o intervalo " +
                                    "antes de registrar a doação.",
                            color = CinzaAtendimentoReal,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            acaoPendente = AcaoAtendimentoReal.DOACAO
                        },
                        enabled = podeAlterar &&
                                tipoSelecionado.isNotBlank() &&
                                intervaloPermite,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VermelhoAtendimentoReal
                        )
                    ) {
                        Text("Registrar doação realizada")
                    }
                }

                if (
                    status in setOf(
                        "EM_ATENDIMENTO",
                        "APROVADO_TRIAGEM"
                    )
                ) {
                    SecaoAtendimentoReal("Encerrar sem coleta")

                    OutlinedTextField(
                        value = motivoEncerramento,
                        onValueChange = {
                            motivoEncerramento = it.take(500)
                        },
                        enabled = podeAlterar,
                        label = { Text("Motivo do encerramento") },
                        supportingText = {
                            Text("${motivoEncerramento.length}/500")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    OutlinedButton(
                        onClick = {
                            acaoPendente = AcaoAtendimentoReal.SEM_COLETA
                        },
                        enabled = podeAlterar &&
                                motivoEncerramento.trim().length >= 3,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Encerrar atendimento sem coleta",
                            color = VermelhoAtendimentoReal
                        )
                    }
                }

                if (
                    status !in setOf(
                        "AGENDADO",
                        "EM_ATENDIMENTO",
                        "APROVADO_TRIAGEM"
                    )
                ) {
                    Text(
                        text = "Este atendimento não possui etapas " +
                                "pendentes de triagem ou coleta.",
                        color = CinzaAtendimentoReal
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    val acao = acaoPendente

    if (acao != null) {
        val titulo = when (acao) {
            AcaoAtendimentoReal.APROVAR ->
                "Confirmar aprovação na triagem?"

            AcaoAtendimentoReal.NAO_APTO ->
                "Confirmar resultado não apto?"

            AcaoAtendimentoReal.INTERVALO ->
                "Salvar regra de intervalo?"

            AcaoAtendimentoReal.DOACAO ->
                "Confirmar doação realizada?"

            AcaoAtendimentoReal.SEM_COLETA ->
                "Encerrar sem coleta?"
        }

        val resumo = when (acao) {
            AcaoAtendimentoReal.APROVAR ->
                "A avaliação presencial será registrada como aprovada."

            AcaoAtendimentoReal.NAO_APTO ->
                "Orientação registrada:\n${observacaoTriagem.trim()}"

            AcaoAtendimentoReal.INTERVALO ->
                if (
                    regraSelecionada == "DOIS_MESES_QUATRO_DOACOES"
                ) {
                    "Regra selecionada: 2 meses e até 4 doações " +
                            "em 12 meses."
                } else {
                    "Regra selecionada: 3 meses e até 3 doações " +
                            "em 12 meses."
                }

            AcaoAtendimentoReal.DOACAO ->
                "Confirme que a coleta foi realizada.\n\n" +
                        "Tipo sanguíneo confirmado: $tipoSelecionado"

            AcaoAtendimentoReal.SEM_COLETA ->
                "Motivo:\n${motivoEncerramento.trim()}"
        }

        AlertDialog(
            onDismissRequest = { acaoPendente = null },
            title = { Text(titulo) },
            text = {
                Text("Reserva #$agendamentoId\n\n$resumo")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        acaoPendente = null
                        executarAcao(acao)
                    },
                    enabled = !ocupado && !exigeConferencia
                ) {
                    Text(
                        text = "Confirmar",
                        color = VermelhoAtendimentoReal
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { acaoPendente = null }
                ) {
                    Text("Voltar")
                }
            }
        )
    }
}

@Composable
private fun SecaoAtendimentoReal(titulo: String) {
    HorizontalDivider(
        modifier = Modifier.padding(top = 8.dp)
    )

    Text(
        text = titulo,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}

private fun textoOpcionalAtendimento(
    objeto: JSONObject,
    campo: String
): String? {
    return if (!objeto.has(campo) || objeto.isNull(campo)) {
        null
    } else {
        objeto.optString(campo).takeIf { it.isNotBlank() }
    }
}