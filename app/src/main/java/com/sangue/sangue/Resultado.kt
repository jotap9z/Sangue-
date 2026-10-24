package com.sangue.sangue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
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
import org.json.JSONObject

@Composable
fun ResultadoScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAgendamento: () -> Unit = {},
    onNavigateToHemocentros: () -> Unit = {}
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)

    val usuarioId = SangueApi.usuario?.optLong("id")

    var resultado by remember(usuarioId) {
        mutableStateOf<JSONObject?>(null)
    }

    var carregando by remember(usuarioId) {
        mutableStateOf(true)
    }

    var erro by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }

    var tentativa by remember(usuarioId) {
        mutableStateOf(0)
    }

    LaunchedEffect(usuarioId, tentativa) {
        resultado = null
        erro = null
        carregando = true

        try {
            if (!SangueApi.autenticado) {
                erro = "Entre novamente para consultar a pré-triagem."
            } else {
                resultado = SangueApi.objeto(
                    metodo = "GET",
                    caminho = "/api/doadores/me/pre-triagens/ultima"
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ApiException) {
            erro = exception.message
                ?: "Não foi possível consultar o resultado."
        } catch (exception: Exception) {
            erro = "Não foi possível consultar o resultado."
        } finally {
            carregando = false
        }
    }

    val classificacao = resultado
        ?.optString("resultado")
        .orEmpty()

    val semAlertas = classificacao == "SEM_ALERTAS_INICIAIS"
    val necessitaAvaliacao = classificacao == "NECESSITA_AVALIACAO"
    val resultadoReconhecido = semAlertas || necessitaAvaliacao

    val corResultado = if (semAlertas) {
        Color(0xFF15803D)
    } else {
        Color(0xFFB45309)
    }

    val motivos = resultado
        ?.optJSONArray("motivos")
        ?.let { array ->
            (0 until array.length()).map {
                array.optString(it)
            }.filter { it.isNotBlank() }
        }
        .orEmpty()

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
                        contentDescription = "Voltar",
                        tint = textDark
                    )
                }

                Text(
                    "Resultado da pré-triagem",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark,
                    modifier = Modifier
                        .weight(1f)
                        .offset(x = (-24).dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            when {
                carregando -> {
                    CircularProgressIndicator(color = sangueRed)

                    Text(
                        "Consultando resultado...",
                        color = textGray,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                erro != null -> {
                    Text(
                        erro.orEmpty(),
                        color = sangueRed,
                        textAlign = TextAlign.Center
                    )

                    TextButton(onClick = { tentativa++ }) {
                        Text("Tentar novamente", color = sangueRed)
                    }
                }

                !resultadoReconhecido -> {
                    Text(
                        "Resultado indisponível.",
                        color = textGray
                    )

                    TextButton(onClick = { tentativa++ }) {
                        Text("Consultar novamente", color = sangueRed)
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(
                                corResultado.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (semAlertas) {
                                Icons.Default.Check
                            } else {
                                Icons.Outlined.Info
                            },
                            contentDescription = null,
                            tint = corResultado,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        if (semAlertas) {
                            "Sem alertas iniciais"
                        } else {
                            "Converse com o hemocentro"
                        },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = corResultado,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        resultado?.optString("mensagem").orEmpty(),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = textGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (motivos.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFFBEB)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    "Pontos para avaliação",
                                    fontWeight = FontWeight.Bold,
                                    color = textDark
                                )

                                motivos.forEach {
                                    Text(
                                        "• $it",
                                        fontSize = 14.sp,
                                        lineHeight = 21.sp,
                                        color = textGray,
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        border = BorderStroke(
                            1.dp,
                            Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Importante",
                                fontWeight = FontWeight.Bold,
                                color = textDark
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                resultado?.optString("aviso").orEmpty(),
                                color = textGray,
                                fontSize = 14.sp,
                                lineHeight = 21.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Pré-triagem registrada na sua conta.",
                        fontSize = 13.sp,
                        color = textGray
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onNavigateToAgendamento,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = sangueRed
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "Consultar horários",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Voltar ao questionário",
                    color = sangueRed,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PassoItem(
    texto: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF1E293B),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            texto,
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            lineHeight = 20.sp
        )
    }
}