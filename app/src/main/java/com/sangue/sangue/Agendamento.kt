package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private data class CentroAgendamento(
    val id: Long,
    val nome: String,
    val endereco: String,
    val cidade: String,
    val estado: String
)

private data class HorarioAgendamento(
    val id: Long,
    val inicio: String,
    val vagas: Long
)

private val VermelhoAgendamento = Color(0xFFE21C2C)
private val TextoAgendamento = Color(0xFF1E293B)
private val CinzaAgendamento = Color(0xFF64748B)

@Composable
fun AgendamentoScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onNavigateToCheckin: () -> Unit = {},
    hemocentroInicial: Long? = null
) {
    val usuarioId = SangueApi.usuario?.optLong("id")
    val scope = rememberCoroutineScope()

    var etapa by remember(usuarioId) {
        mutableStateOf(1)
    }
    var centros by remember(usuarioId) {
        mutableStateOf<List<CentroAgendamento>>(emptyList())
    }
    var centroId by remember(usuarioId) {
        mutableStateOf<Long?>(hemocentroInicial)
    }
    var horarios by remember(usuarioId) {
        mutableStateOf<List<HorarioAgendamento>>(emptyList())
    }
    var horarioSelecionado by remember(usuarioId) {
        mutableStateOf<HorarioAgendamento?>(null)
    }
    var confirmado by remember(usuarioId) {
        mutableStateOf<JSONObject?>(null)
    }
    var carregandoCentros by remember(usuarioId) {
        mutableStateOf(true)
    }
    var carregandoHorarios by remember(usuarioId) {
        mutableStateOf(false)
    }
    var reservando by remember(usuarioId) {
        mutableStateOf(false)
    }
    var confirmacaoIncerta by remember(usuarioId) {
        mutableStateOf(false)
    }
    var erroCentros by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var erroHorarios by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var erroReserva by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var avisoTriagem by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var atualizarCentros by remember(usuarioId) {
        mutableStateOf(0)
    }
    var atualizarHorarios by remember(usuarioId) {
        mutableStateOf(0)
    }
    var janela by remember(usuarioId) {
        mutableStateOf(0)
    }

    val dataBase = remember(usuarioId) {
        hojeAgendamento()
    }

    val inicioPeriodo = remember(dataBase, janela) {
        somarDiasAgendamento(dataBase, janela * 31)
    }

    val fimPeriodo = remember(inicioPeriodo) {
        somarDiasAgendamento(inicioPeriodo, 30)
    }

    val centroSelecionado = centros.firstOrNull {
        it.id == centroId
    }

    LaunchedEffect(usuarioId, atualizarCentros) {
        carregandoCentros = true
        erroCentros = null
        avisoTriagem = null

        try {
            if (!SangueApi.autenticado) {
                erroCentros = "Entre na sua conta para agendar."
                centros = emptyList()
                centroId = null
            } else {
                val triagem = SangueApi.objeto(
                    metodo = "GET",
                    caminho = "/api/doadores/me/pre-triagens/ultima"
                )

                if (
                    triagem.optString("resultado") ==
                    "NECESSITA_AVALIACAO"
                ) {
                    avisoTriagem = triagem.optString("mensagem")
                }

                val resposta = SangueApi.lista(
                    caminho = "/api/hemocentros/buscar",
                    autenticado = false
                )

                centros = resposta.map {
                    CentroAgendamento(
                        id = it.getLong("id"),
                        nome = it.getString("nome"),
                        endereco = it.getString("endereco"),
                        cidade = it.getString("cidade"),
                        estado = it.getString("estado")
                    )
                }

                if (centros.none { it.id == centroId }) {
                    centroId = null
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ApiException) {
            centros = emptyList()
            centroId = null

            erroCentros = if (exception.status == 404) {
                "Responda à pré-triagem antes de agendar."
            } else {
                exception.message
                    ?: "Não foi possível carregar os hemocentros."
            }
        } catch (exception: Exception) {
            centros = emptyList()
            centroId = null
            erroCentros = "Não foi possível carregar os hemocentros."
        } finally {
            carregandoCentros = false
        }
    }

    LaunchedEffect(
        usuarioId,
        centroId,
        inicioPeriodo,
        atualizarHorarios
    ) {
        horarios = emptyList()
        horarioSelecionado = null
        erroHorarios = null

        val id = centroId

        if (id == null || !SangueApi.autenticado) {
            carregandoHorarios = false
        } else {
            carregandoHorarios = true

            try {
                val resposta = SangueApi.lista(
                    caminho = "/api/hemocentros/$id/horarios" +
                            "?de=$inicioPeriodo&ate=$fimPeriodo",
                    autenticado = false
                )

                horarios = resposta
                    .filter {
                        it.optBoolean("ativo") &&
                                it.optLong("vagasDisponiveis") > 0
                    }
                    .map {
                        HorarioAgendamento(
                            id = it.getLong("id"),
                            inicio = it.getString("inicio"),
                            vagas = it.getLong("vagasDisponiveis")
                        )
                    }
                    .sortedBy { it.inicio }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ApiException) {
                erroHorarios = exception.message
                    ?: "Não foi possível carregar os horários."
            } catch (exception: Exception) {
                erroHorarios = "Não foi possível carregar os horários."
            } finally {
                carregandoHorarios = false
            }
        }
    }

    val voltar: () -> Unit = {
        if (!reservando) {
            when {
                etapa == 3 -> onNavigateHome()
                confirmacaoIncerta -> onNavigateHome()
                etapa == 2 -> etapa = 1
                else -> onNavigateBack()
            }
        }
    }

    BackHandler {
        voltar()
    }

    suspend fun buscarReserva(
        horario: HorarioAgendamento
    ): JSONObject? {
        val data = horario.inicio.take(10)

        return SangueApi.lista(
            caminho = "/api/doadores/me/agendamentos" +
                    "?de=$data&ate=$data"
        ).firstOrNull {
            it.optLong("horarioId") == horario.id &&
                    it.optString("status") == "AGENDADO"
        }
    }

    fun confirmarOuVerificar() {
        if (reservando) return

        val horario = horarioSelecionado ?: return

        if (!SangueApi.autenticado) {
            erroReserva = "Sua sessão expirou. Entre novamente."
            return
        }

        val apenasVerificar = confirmacaoIncerta
        reservando = true
        erroReserva = null

        scope.launch {
            try {
                if (apenasVerificar) {
                    val existente = buscarReserva(horario)

                    if (existente != null) {
                        confirmado = existente
                        confirmacaoIncerta = false
                        etapa = 3
                    } else {
                        confirmacaoIncerta = false
                        erroReserva = "Nenhuma reserva ativa foi encontrada " +
                                "para esse horário. Você pode atualizar as vagas " +
                                "ou tentar confirmar novamente."
                    }
                } else {
                    val resposta = SangueApi.objeto(
                        metodo = "POST",
                        caminho = "/api/doadores/me/agendamentos",
                        corpo = JSONObject()
                            .put("horarioId", horario.id)
                    )

                    check(
                        resposta.getLong("id") > 0 &&
                                resposta.getString("status") == "AGENDADO"
                    )

                    confirmado = resposta
                    etapa = 3
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ApiException) {
                if (
                    apenasVerificar ||
                    exception.status == 0 ||
                    exception.status == 409 ||
                    exception.status >= 500
                ) {
                    confirmacaoIncerta = true
                    erroReserva = (
                            exception.message
                                ?: "Não foi possível confirmar a operação."
                            ) + "\nVerifique a reserva antes de tentar novamente."
                } else {
                    erroReserva = exception.message
                        ?: "Não foi possível reservar esse horário."
                }
            } catch (exception: Exception) {
                confirmacaoIncerta = true
                erroReserva = "Não foi possível confirmar o resultado " +
                        "da operação. Verifique se a reserva foi registrada."
            } finally {
                reservando = false
            }
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
                        bottom = 8.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = voltar,
                    enabled = !reservando
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = TextoAgendamento
                    )
                }

                Text(
                    "Agendar doação",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoAgendamento,
                    modifier = Modifier
                        .weight(1f)
                        .offset(x = (-24).dp),
                    textAlign = TextAlign.Center
                )
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    erroReserva?.let {
                        Text(
                            it,
                            color = VermelhoAgendamento,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    when (etapa) {
                        1 -> {
                            Button(
                                onClick = {
                                    erroReserva = null
                                    etapa = 2
                                },
                                enabled = horarioSelecionado != null &&
                                        !carregandoHorarios &&
                                        !carregandoCentros,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VermelhoAgendamento
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Revisar agendamento")
                            }
                        }

                        2 -> {
                            Button(
                                onClick = {
                                    confirmarOuVerificar()
                                },
                                enabled = !reservando,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VermelhoAgendamento
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    when {
                                        reservando -> "Aguarde..."
                                        confirmacaoIncerta -> "Verificar reserva"
                                        else -> "Confirmar agendamento"
                                    }
                                )
                            }

                            if (!confirmacaoIncerta) {
                                TextButton(
                                    onClick = {
                                        etapa = 1
                                        erroReserva = null
                                        atualizarHorarios++
                                    },
                                    enabled = !reservando,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Voltar e atualizar vagas",
                                        color = VermelhoAgendamento
                                    )
                                }
                            }
                        }

                        3 -> {
                            Button(
                                onClick = onNavigateHome,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VermelhoAgendamento
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Voltar ao início")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepCircle(1, etapa)
                    Spacer(modifier = Modifier.width(16.dp))
                    StepCircle(2, etapa)
                    Spacer(modifier = Modifier.width(16.dp))
                    StepCircle(3, etapa)
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            when (etapa) {
                1 -> {
                    item {
                        Text(
                            "Escolha o hemocentro",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoAgendamento
                        )

                        avisoTriagem?.let {
                            Text(
                                it,
                                color = Color(0xFFB45309),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    if (carregandoCentros) {
                        item {
                            CarregandoAgendamento(
                                "Carregando hemocentros..."
                            )
                        }
                    }

                    erroCentros?.let { mensagem ->
                        item {
                            Text(
                                mensagem,
                                color = VermelhoAgendamento
                            )

                            TextButton(
                                onClick = { atualizarCentros++ },
                                enabled = !carregandoCentros
                            ) {
                                Text(
                                    "Tentar novamente",
                                    color = VermelhoAgendamento
                                )
                            }
                        }
                    }

                    if (
                        !carregandoCentros &&
                        erroCentros == null &&
                        centros.isEmpty()
                    ) {
                        item {
                            Text(
                                "Nenhum hemocentro cadastrado.",
                                color = CinzaAgendamento
                            )
                        }
                    }

                    items(
                        items = centros,
                        key = { "centro-${it.id}" }
                    ) { centro ->
                        val selecionado = centro.id == centroId

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    centroId = centro.id
                                    janela = 0
                                    etapa = 1
                                    erroReserva = null
                                },
                            border = BorderStroke(
                                if (selecionado) 2.dp else 1.dp,
                                if (selecionado) {
                                    VermelhoAgendamento
                                } else {
                                    Color(0xFFE2E8F0)
                                }
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selecionado,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = VermelhoAgendamento
                                    )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        centro.nome,
                                        fontWeight = FontWeight.Bold,
                                        color = TextoAgendamento
                                    )

                                    Text(
                                        centro.endereco,
                                        fontSize = 13.sp,
                                        color = CinzaAgendamento
                                    )

                                    Text(
                                        "${centro.cidade} - ${centro.estado}",
                                        fontSize = 12.sp,
                                        color = CinzaAgendamento
                                    )
                                }
                            }
                        }
                    }

                    if (centroSelecionado != null) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Escolha a data e o horário",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextoAgendamento
                            )

                            Text(
                                "${dataAgendamento(inicioPeriodo)} até " +
                                        dataAgendamento(fimPeriodo),
                                color = CinzaAgendamento,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(
                                    onClick = { janela-- },
                                    enabled = janela > 0 &&
                                            !carregandoHorarios
                                ) {
                                    Text("Anterior")
                                }

                                TextButton(
                                    onClick = { atualizarHorarios++ },
                                    enabled = !carregandoHorarios
                                ) {
                                    Text("Atualizar")
                                }

                                TextButton(
                                    onClick = { janela++ },
                                    enabled = !carregandoHorarios
                                ) {
                                    Text("Próximo")
                                }
                            }
                        }

                        if (carregandoHorarios) {
                            item {
                                CarregandoAgendamento(
                                    "Consultando vagas..."
                                )
                            }
                        }

                        erroHorarios?.let { mensagem ->
                            item {
                                Text(
                                    mensagem,
                                    color = VermelhoAgendamento
                                )
                            }
                        }

                        if (
                            !carregandoHorarios &&
                            erroHorarios == null &&
                            horarios.isEmpty()
                        ) {
                            item {
                                Text(
                                    "Não há horários com vagas neste período. " +
                                            "Consulte o próximo período ou outro hemocentro.",
                                    color = CinzaAgendamento,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        items(
                            items = horarios,
                            key = { "horario-${it.id}" }
                        ) { horario ->
                            val selecionado =
                                horarioSelecionado?.id == horario.id

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        horarioSelecionado = horario
                                        erroReserva = null
                                    },
                                border = BorderStroke(
                                    if (selecionado) 2.dp else 1.dp,
                                    if (selecionado) {
                                        VermelhoAgendamento
                                    } else {
                                        Color(0xFFE2E8F0)
                                    }
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selecionado,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = VermelhoAgendamento
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column {
                                        Text(
                                            dataHoraAgendamento(horario.inicio),
                                            fontWeight = FontWeight.Bold,
                                            color = TextoAgendamento
                                        )

                                        Text(
                                            "${horario.vagas} vaga(s) disponível(is)",
                                            color = CinzaAgendamento,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        Text(
                            "Revise seu agendamento",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoAgendamento
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF8FAFC)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    centroSelecionado?.nome.orEmpty(),
                                    fontWeight = FontWeight.Bold,
                                    color = TextoAgendamento
                                )

                                Text(
                                    centroSelecionado?.endereco.orEmpty(),
                                    color = CinzaAgendamento
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                horarioSelecionado?.let {
                                    Text(
                                        dataHoraAgendamento(it.inicio),
                                        fontWeight = FontWeight.Bold,
                                        color = VermelhoAgendamento
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            "A vaga será confirmada pelo servidor ao concluir " +
                                    "a reserva. O agendamento não confirma aptidão " +
                                    "para doar; a avaliação presencial é obrigatória.",
                            color = CinzaAgendamento,
                            fontSize = 14.sp
                        )
                    }
                }

                3 -> {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(64.dp)
                            )

                            Text(
                                "Agendamento confirmado!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF15803D),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    item {
                        val reserva = confirmado

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF8FAFC)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    "Reserva #${reserva?.optLong("id")}",
                                    fontWeight = FontWeight.Bold,
                                    color = TextoAgendamento
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    reserva
                                        ?.optString("nomeHemocentro")
                                        .orEmpty(),
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    reserva
                                        ?.optString("enderecoHemocentro")
                                        .orEmpty(),
                                    color = CinzaAgendamento
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    dataHoraAgendamento(
                                        reserva
                                            ?.optString("inicio")
                                            .orEmpty()
                                    ),
                                    color = VermelhoAgendamento,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            confirmado?.optString("aviso").orEmpty(),
                            color = CinzaAgendamento,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CarregandoAgendamento(
    texto: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = VermelhoAgendamento
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            texto,
            color = CinzaAgendamento
        )
    }
}

private fun hojeAgendamento(): String {
    return SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.ROOT
    ).apply {
        timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
    }.format(java.util.Date())
}

private fun somarDiasAgendamento(
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

private fun dataAgendamento(
    valor: String
): String {
    val partes = valor.take(10).split("-")

    return if (partes.size == 3) {
        "${partes[2]}/${partes[1]}/${partes[0]}"
    } else {
        valor
    }
}

private fun dataHoraAgendamento(
    valor: String
): String {
    return if (valor.length >= 16) {
        "${dataAgendamento(valor)} às ${valor.substring(11, 16)}"
    } else {
        "Data e horário indisponíveis"
    }
}

@Composable
fun StepCircle(
    step: Int,
    currentStep: Int
) {
    val concluido = step < currentStep || currentStep == 3
    val ativo = step == currentStep

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                when {
                    currentStep == 3 -> Color(0xFF22C55E)
                    concluido || ativo -> VermelhoAgendamento
                    else -> Color(0xFFF1F5F9)
                },
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (concluido) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                step.toString(),
                color = if (ativo) {
                    Color.White
                } else {
                    CinzaAgendamento
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SectionTitle(
    title: String
) {
    Text(
        title,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = TextoAgendamento,
        modifier = Modifier.padding(
            start = 24.dp,
            top = 24.dp,
            bottom = 12.dp
        )
    )
}

@Composable
fun DateCard(
    diaNum: String,
    mes: String,
    diaSemana: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(65.dp)
            .height(90.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                VermelhoAgendamento
            } else {
                Color(0xFFF8FAFC)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val cor = if (isSelected) {
                Color.White
            } else {
                TextoAgendamento
            }

            Text(
                diaNum,
                color = cor,
                fontWeight = FontWeight.Bold
            )

            Text(
                mes,
                color = cor,
                fontSize = 12.sp
            )

            Text(
                diaSemana,
                color = cor,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun TimeCard(
    time: String,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(45.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                VermelhoAgendamento
            } else {
                Color(0xFFF8FAFC)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                time,
                color = if (isSelected) {
                    Color.White
                } else {
                    TextoAgendamento
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}