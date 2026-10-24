package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val VermelhoHorarios = Color(0xFFE21C2C)
private val CinzaHorarios = Color(0xFF64748B)

private data class HorarioHemoReal(
    val id: Long,
    val inicio: String,
    val capacidade: Int,
    val vagasDisponiveis: Long,
    val ativo: Boolean
)

private data class OperacaoHorarioReal(
    val id: Long?,
    val inicio: String,
    val capacidade: Int,
    val ativo: Boolean
)

@Composable
fun HemoHorariosScreen(
    dataInicial: String,
    onNavigateBack: () -> Unit = {}
) {
    val usuarioId = SangueApi.usuario?.optLong("id")
    val scope = rememberCoroutineScope()

    var dia by remember(usuarioId, dataInicial) {
        mutableStateOf(dataInicial)
    }
    var dataDigitada by remember(usuarioId, dataInicial) {
        mutableStateOf(dataAgendaReal(dataInicial))
    }
    var hora by remember(usuarioId) {
        mutableStateOf("")
    }
    var capacidade by remember(usuarioId) {
        mutableStateOf("")
    }
    var horarios by remember(usuarioId) {
        mutableStateOf<List<HorarioHemoReal>>(emptyList())
    }
    var carregando by remember(usuarioId) {
        mutableStateOf(true)
    }
    var salvando by remember(usuarioId) {
        mutableStateOf(false)
    }
    var conferido by remember(usuarioId) {
        mutableStateOf(false)
    }
    var erro by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var mensagem by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var atualizar by remember(usuarioId) {
        mutableStateOf(0)
    }
    var pendente by remember(usuarioId) {
        mutableStateOf<OperacaoHorarioReal?>(null)
    }

    suspend fun consultarHorarios(): List<HorarioHemoReal> {
        return SangueApi.lista(
            caminho = "/api/hemocentros/me/horarios" +
                    "?de=$dia&ate=$dia"
        ).map { lerHorarioHemoReal(it) }
            .sortedBy { it.inicio }
    }

    LaunchedEffect(usuarioId, dia, atualizar) {
        carregando = true
        conferido = false
        erro = null
        horarios = emptyList()

        try {
            if (!SangueApi.autenticado) {
                erro = "Entre com a conta do hemocentro."
            } else {
                horarios = consultarHorarios()
                conferido = true
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ApiException) {
            erro = exception.message
                ?: "Não foi possível consultar os horários."
        } catch (_: Exception) {
            erro = "Não foi possível consultar os horários."
        } finally {
            carregando = false
        }
    }

    val ocupado = carregando || salvando
    val podeEditar = !ocupado &&
            conferido &&
            pendente == null &&
            SangueApi.autenticado

    fun selecionarDia(novoDia: String) {
        if (ocupado || pendente != null) return

        dia = novoDia
        dataDigitada = dataAgendaReal(novoDia)
        erro = null
        mensagem = null
    }

    fun prepararCadastro() {
        if (!podeEditar) return

        val horarioDigitado = hora.trim()
        val quantidade = capacidade.toIntOrNull()

        if (
            !Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
                .matches(horarioDigitado)
        ) {
            erro = "Informe o horário no formato HH:mm, como 09:30."
            return
        }

        if (quantidade == null || quantidade !in 1..100) {
            erro = "Informe uma capacidade entre 1 e 100 vagas."
            return
        }

        val inicio = "${dia}T$horarioDigitado:00"

        if (!horarioFuturoHemoReal(inicio)) {
            erro = "O horário deve estar no futuro."
            return
        }

        if (
            horarios.any {
                it.inicio.take(16) == inicio.take(16)
            }
        ) {
            erro = "Já existe um horário cadastrado para esse início."
            return
        }

        erro = null

        pendente = OperacaoHorarioReal(
            id = null,
            inicio = inicio,
            capacidade = quantidade,
            ativo = true
        )
    }

    fun executarOperacao(operacao: OperacaoHorarioReal) {
        if (
            ocupado ||
            !conferido ||
            !SangueApi.autenticado
        ) {
            return
        }

        if (!horarioFuturoHemoReal(operacao.inicio)) {
            erro = "Somente horários futuros podem ser alterados."
            return
        }

        salvando = true
        conferido = false
        erro = null
        mensagem = null

        scope.launch {
            try {
                val resposta = if (operacao.id == null) {
                    SangueApi.objeto(
                        metodo = "POST",
                        caminho = "/api/hemocentros/me/horarios",
                        corpo = JSONObject()
                            .put("inicio", operacao.inicio)
                            .put("capacidade", operacao.capacidade)
                    )
                } else {
                    SangueApi.objeto(
                        metodo = "PUT",
                        caminho = "/api/hemocentros/me/horarios/" +
                                "${operacao.id}/disponibilidade",
                        corpo = JSONObject().put(
                            "ativo",
                            operacao.ativo
                        )
                    )
                }

                val confirmado = lerHorarioHemoReal(resposta)

                if (operacao.id == null) {
                    check(
                        confirmado.inicio.take(16) ==
                                operacao.inicio.take(16) &&
                                confirmado.capacidade ==
                                operacao.capacidade &&
                                confirmado.ativo
                    )

                    hora = ""
                    capacidade = ""
                    mensagem = "Horário cadastrado com sucesso."
                } else {
                    check(
                        confirmado.id == operacao.id &&
                                confirmado.ativo == operacao.ativo
                    )

                    mensagem = if (operacao.ativo) {
                        "Horário ativado para novas reservas."
                    } else {
                        "Horário desativado para novas reservas."
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ApiException) {
                erro = exception.message
                    ?: "Não foi possível confirmar a operação."

                if (
                    exception.status == 0 ||
                    exception.status >= 500
                ) {
                    erro = "$erro\n\nConfira a lista de horários " +
                            "antes de tentar novamente."
                }
            } catch (_: Exception) {
                erro = "Não foi possível conferir a resposta. " +
                        "Consulte a lista antes de tentar novamente."
            }

            try {
                // Consulta novamente mesmo quando a resposta da
                // alteração pode ter sido perdida na conexão.
                horarios = consultarHorarios()
                conferido = true
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                conferido = false

                erro = listOfNotNull(
                    erro,
                    "Atualize a tela para conferir os horários " +
                            "antes de realizar outra alteração."
                ).joinToString("\n\n")
            } finally {
                salvando = false
            }
        }
    }

    BackHandler {
        if (!salvando) {
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
                    enabled = !salvando
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar"
                    )
                }

                Text(
                    text = "Horários e vagas",
                    modifier = Modifier.weight(1f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        mensagem = null
                        atualizar++
                    },
                    enabled = !ocupado && pendente == null
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Atualizar horários",
                        tint = VermelhoHorarios
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
            Text(
                text = "Data de atendimento",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = dataDigitada,
                onValueChange = {
                    dataDigitada = it.take(10)
                },
                enabled = !ocupado && pendente == null,
                label = { Text("Data — dd/MM/aaaa") },
                placeholder = { Text("Ex.: 25/10/2026") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = {
                    val convertida = converterDataHorarioReal(
                        dataDigitada.trim()
                    )

                    if (convertida == null) {
                        erro = "Informe uma data válida no formato " +
                                "dd/MM/aaaa."
                    } else {
                        selecionarDia(convertida)
                        atualizar++
                    }
                },
                enabled = !ocupado && pendente == null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Consultar data", color = VermelhoHorarios)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        selecionarDia(moverDiaHorarioReal(dia, -1))
                    },
                    enabled = !ocupado && pendente == null
                ) {
                    Text("Anterior", color = VermelhoHorarios)
                }

                Text(
                    text = dataAgendaReal(dia),
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = {
                        selecionarDia(moverDiaHorarioReal(dia, 1))
                    },
                    enabled = !ocupado && pendente == null
                ) {
                    Text("Próximo", color = VermelhoHorarios)
                }
            }

            if (ocupado) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = VermelhoHorarios
                    )
                }

                Text(
                    text = if (salvando) {
                        "Confirmando operação e atualizando horários..."
                    } else {
                        "Carregando horários..."
                    },
                    color = CinzaHorarios
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
                    color = VermelhoHorarios
                )
            }

            if (!conferido && !ocupado) {
                OutlinedButton(
                    onClick = { atualizar++ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Atualizar horários")
                }
            }

            HorizontalDivider()

            Text(
                text = "Cadastrar em ${dataAgendaReal(dia)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Cadastre cada horário com a quantidade de " +
                        "vagas que poderá receber.",
                color = CinzaHorarios
            )

            OutlinedTextField(
                value = hora,
                onValueChange = { valor ->
                    hora = valor.filter {
                        it in '0'..'9' || it == ':'
                    }.take(5)
                },
                enabled = podeEditar,
                label = { Text("Horário — HH:mm") },
                placeholder = { Text("Ex.: 09:30") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = capacidade,
                onValueChange = { valor ->
                    capacidade = valor.filter {
                        it in '0'..'9'
                    }.take(3)
                },
                enabled = podeEditar,
                label = { Text("Quantidade de vagas") },
                supportingText = {
                    Text("Entre 1 e 100 vagas por horário")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { prepararCadastro() },
                enabled = podeEditar &&
                        hora.isNotBlank() &&
                        capacidade.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VermelhoHorarios
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cadastrar horário")
            }

            HorizontalDivider()

            Text(
                text = "Horários de ${dataAgendaReal(dia)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            if (
                !ocupado &&
                conferido &&
                horarios.isEmpty()
            ) {
                Text(
                    text = "Nenhum horário cadastrado nesta data.",
                    color = CinzaHorarios
                )
            }

            horarios.forEach { horario ->
                val futuro = horarioFuturoHemoReal(horario.inicio)

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = dataHoraAgendaReal(horario.inicio),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Capacidade: ${horario.capacidade} vagas"
                        )

                        Text(
                            text = "Vagas disponíveis: " +
                                    horario.vagasDisponiveis,
                            color = CinzaHorarios
                        )

                        Text(
                            text = if (horario.ativo) {
                                "Ativo"
                            } else {
                                "Desativado"
                            },
                            color = if (horario.ativo) {
                                Color(0xFF15803D)
                            } else {
                                CinzaHorarios
                            },
                            fontWeight = FontWeight.SemiBold
                        )

                        if (!futuro) {
                            Text(
                                text = "Horário passado: disponibilidade " +
                                        "não pode ser alterada.",
                                color = CinzaHorarios,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                pendente = OperacaoHorarioReal(
                                    id = horario.id,
                                    inicio = horario.inicio,
                                    capacidade = horario.capacidade,
                                    ativo = !horario.ativo
                                )
                            },
                            enabled = podeEditar && futuro,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (horario.ativo) {
                                    "Desativar novas reservas"
                                } else {
                                    "Ativar novas reservas"
                                },
                                color = VermelhoHorarios
                            )
                        }
                    }
                }
            }

            Text(
                text = "Desativar um horário impede novas reservas. " +
                        "Os agendamentos já realizados continuam registrados.",
                color = CinzaHorarios,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    val operacao = pendente

    if (operacao != null) {
        val criando = operacao.id == null

        AlertDialog(
            onDismissRequest = { pendente = null },
            title = {
                Text(
                    text = if (criando) {
                        "Cadastrar horário?"
                    } else if (operacao.ativo) {
                        "Ativar horário?"
                    } else {
                        "Desativar novas reservas?"
                    }
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(dataHoraAgendaReal(operacao.inicio))

                    if (criando) {
                        Text(
                            text = "Capacidade: " +
                                    "${operacao.capacidade} vagas"
                        )
                    } else if (!operacao.ativo) {
                        Text(
                            text = "Os agendamentos existentes serão " +
                                    "mantidos. Apenas novas reservas " +
                                    "serão impedidas."
                        )
                    } else {
                        Text(
                            text = "O horário ficará disponível para " +
                                    "novas reservas conforme as vagas livres."
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendente = null
                        executarOperacao(operacao)
                    },
                    enabled = !ocupado && conferido
                ) {
                    Text(
                        text = "Confirmar",
                        color = VermelhoHorarios
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendente = null }
                ) {
                    Text("Voltar")
                }
            }
        )
    }
}

private fun lerHorarioHemoReal(
    objeto: JSONObject
): HorarioHemoReal {
    val id = objeto.getLong("id")
    val capacidade = objeto.getInt("capacidade")
    val vagas = objeto.getLong("vagasDisponiveis")

    check(id > 0)
    check(capacidade in 1..100)
    check(vagas in 0L..capacidade.toLong())

    return HorarioHemoReal(
        id = id,
        inicio = objeto.getString("inicio"),
        capacidade = capacidade,
        vagasDisponiveis = vagas,
        ativo = objeto.getBoolean("ativo")
    )
}

private fun converterDataHorarioReal(
    valor: String
): String? {
    if (!Regex("^\\d{2}/\\d{2}/\\d{4}$").matches(valor)) {
        return null
    }

    val formato = SimpleDateFormat(
        "dd/MM/yyyy",
        Locale.ROOT
    ).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }

    val posicao = ParsePosition(0)
    val data = formato.parse(valor, posicao) ?: return null

    if (posicao.index != valor.length) return null
    if (formato.format(data) != valor) return null

    return SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.ROOT
    ).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(data)
}

private fun moverDiaHorarioReal(
    valor: String,
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
        time = requireNotNull(formato.parse(valor))
        add(Calendar.DAY_OF_MONTH, dias)
    }

    return formato.format(calendario.time)
}

private fun horarioFuturoHemoReal(
    inicio: String
): Boolean {
    if (inicio.length < 16) return false

    val valor = inicio.take(16)

    val formato = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm",
        Locale.ROOT
    ).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
    }

    val posicao = ParsePosition(0)
    val data = formato.parse(valor, posicao) ?: return false

    return posicao.index == valor.length &&
            formato.format(data) == valor &&
            data.after(Date())
}