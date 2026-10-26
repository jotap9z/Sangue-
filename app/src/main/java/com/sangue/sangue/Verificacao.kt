package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigDecimal

data class PerguntaItem(
    val texto: String,
    val icone: ImageVector,
    val corIcone: Color
)

private data class PerguntaPreTriagem(
    val chave: String,
    val texto: String,
    val permiteNaoSeAplica: Boolean = false
)

private val perguntasPreTriagem = listOf(
    PerguntaPreTriagem(
        "bemDeSaude",
        "Você está bem de saúde?"
    ),
    PerguntaPreTriagem(
        "sintomasRecentes",
        "Você teve febre, gripe ou outros sintomas recentes?"
    ),
    PerguntaPreTriagem(
        "cirurgiaRecente",
        "Você realizou cirurgia nos últimos 12 meses?"
    ),
    PerguntaPreTriagem(
        "tatuagemOuPiercingRecente",
        "Você fez tatuagem ou colocou piercing nos últimos 12 meses?"
    ),
    PerguntaPreTriagem(
        "gravidaOuAmamentando",
        "Você está grávida ou amamentando?"
    ),
    PerguntaPreTriagem(
        "condicaoDeSaudeOuMedicamento",
        "Você possui alguma condição de saúde ou usa medicamento?"
    ),
    PerguntaPreTriagem(
        "descansadoEAlimentado",
        "Você está descansado(a) e alimentado(a)?"
    ),
    PerguntaPreTriagem(
        "autorizacaoResponsavel",
        "Se você tem menos de 18 anos, possui autorização formal " +
                "do responsável legal?",
        permiteNaoSeAplica = true
    )
)

@Composable
fun PossoDoarScreen(
    onNavigateToResultado: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)

    val usuarioId = SangueApi.usuario?.optLong("id")

    var perguntaEdicao by remember(usuarioId) { mutableStateOf<PerguntaPreTriagem?>(null) }
    var peso by remember(usuarioId) {
        mutableStateOf("")
    }

    var respostas by remember(usuarioId) {
        mutableStateOf<Map<String, Boolean?>>(emptyMap())
    }

    var observacoes by remember(usuarioId) {
        mutableStateOf("")
    }

    var enviando by remember(usuarioId) {
        mutableStateOf(false)
    }

    var erro by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }

    val scope = rememberCoroutineScope()

    val formularioCompleto = peso.isNotBlank() &&
            perguntasPreTriagem.all {
                respostas.containsKey(it.chave)
            }

    val voltar: () -> Unit = {
        if (!enviando) {
            onNavigateBack()
        }
    }

    BackHandler {
        voltar()
    }

    fun enviar() {
        if (enviando) return

        if (!SangueApi.autenticado) {
            erro = "Entre na sua conta para responder à pré-triagem."
            return
        }

        val pesoInformado = peso
            .trim()
            .replace(',', '.')
            .toBigDecimalOrNull()

        if (
            pesoInformado == null ||
            pesoInformado <= BigDecimal.ZERO ||
            pesoInformado > BigDecimal("500") ||
            pesoInformado.stripTrailingZeros().scale() > 2
        ) {
            erro = "Informe um peso válido, maior que zero e até " +
                    "500 kg, com no máximo duas casas decimais."
            return
        }

        val perguntaPendente = perguntasPreTriagem.firstOrNull {
            !respostas.containsKey(it.chave)
        }

        if (perguntaPendente != null) {
            erro = "Responda: ${perguntaPendente.texto}"
            return
        }

        val corpo = JSONObject()
            .put("pesoKg", pesoInformado)
            .put(
                "observacoes",
                observacoes.trim().ifBlank { null }
                    ?: JSONObject.NULL
            )

        perguntasPreTriagem.forEach { pergunta ->
            corpo.put(
                pergunta.chave,
                respostas[pergunta.chave] ?: JSONObject.NULL
            )
        }

        erro = null
        enviando = true

        scope.launch {
            try {
                SangueApi.objeto(
                    metodo = "POST",
                    caminho = "/api/doadores/me/pre-triagens",
                    corpo = corpo
                )

                onNavigateToResultado()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ApiException) {
                erro = exception.message
                    ?: "Não foi possível registrar a pré-triagem."
            } catch (exception: Exception) {
                erro = "Não foi possível confirmar o registro " +
                        "da pré-triagem. Tente novamente."
            } finally {
                enviando = false
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
                    enabled = !enviando
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = textDark
                    )
                }

                Text(
                    "Pré-triagem",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark,
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
                    erro?.let { mensagem ->
                        Text(
                            mensagem,
                            color = sangueRed,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Button(
                        onClick = { enviar() },
                        enabled = formularioCompleto && !enviando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = sangueRed
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            if (enviando) {
                                "Enviando..."
                            } else {
                                "Enviar e ver resultado"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Questionário inicial",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textDark
            )

            Text(
                "Responda com atenção. Esta pré-triagem não " +
                        "confirma aptidão para doar e não substitui " +
                        "a avaliação presencial do hemocentro.",
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = textGray,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = peso,
                onValueChange = {
                    peso = it
                    erro = null
                },
                enabled = !enviando,
                label = { Text("Peso atual em kg") },
                placeholder = { Text("Ex.: 70,5") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            perguntasPreTriagem.forEachIndexed { indice, pergunta ->
                val respondida = respostas.containsKey(pergunta.chave)
                val valor = respostas[pergunta.chave]
                val rotulo = if (!respondida) "Toque para responder" else if (valor == null) "Não se aplica" else if (valor) "Sim" else "Não"
                AcessoOriginal(pergunta.texto, rotulo, enabled = !enviando) { perguntaEdicao = pergunta }
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = observacoes,
                onValueChange = {
                    observacoes = it.take(500)
                },
                enabled = !enviando,
                label = {
                    Text("Observações — opcional")
                },
                supportingText = {
                    Text("${observacoes.length}/500")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    perguntaEdicao?.let { pergunta ->
        AlertDialog(
            onDismissRequest = { perguntaEdicao = null },
            title = { Text(pergunta.texto, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = { Column {
                Text("Selecione sua resposta. A avaliação presencial continua necessária.", color = textGray)
                Spacer(Modifier.height(16.dp))
                OpcaoRespostaPreTriagem("Sim", respostas.containsKey(pergunta.chave) && respostas[pergunta.chave] == true, !enviando) { respostas = respostas + (pergunta.chave to true); erro = null; perguntaEdicao = null }
                OpcaoRespostaPreTriagem("Não", respostas.containsKey(pergunta.chave) && respostas[pergunta.chave] == false, !enviando) { respostas = respostas + (pergunta.chave to false); erro = null; perguntaEdicao = null }
                if (pergunta.permiteNaoSeAplica) OpcaoRespostaPreTriagem("Não se aplica", respostas.containsKey(pergunta.chave) && respostas[pergunta.chave] == null, !enviando) { respostas = respostas + (pergunta.chave to null); erro = null; perguntaEdicao = null }
            } },
            confirmButton = { TextButton(onClick = { perguntaEdicao = null }) { Text("Voltar", color = sangueRed) } }
            , shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), containerColor = androidx.compose.ui.graphics.Color.White)
    }

}

@Composable
private fun OpcaoRespostaPreTriagem(
    texto: String,
    selecionada: Boolean,
    habilitada: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selecionada,
                enabled = habilitada,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selecionada,
            enabled = habilitada,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFFE21C2C)
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            texto,
            color = Color(0xFF1E293B),
            fontSize = 14.sp
        )
    }
}