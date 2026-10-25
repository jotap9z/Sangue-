package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private enum class TelaMaisReal {
    MENU,
    EQUIPE,
    HORARIOS,
    PERFIL,
    MEU_PERFIL
}

private val VermelhoMaisReal = Color(0xFFE21C2C)

@Composable
fun HemoMaisScreen(
    onNavigateToInicio: () -> Unit = {},
    onNavigateToAgenda: () -> Unit = {},
    onNavigateToEstoque: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToMais: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val acesso = LocalAcessoHemocentro.current
    val recarregarAcesso = LocalRecarregarAcessoHemo.current
    val usuarioId = SangueApi.usuario?.optLong("id")
    val scope = rememberCoroutineScope()

    var tela by remember(usuarioId) {
        mutableStateOf(TelaMaisReal.MENU)
    }
    var instituicao by remember(usuarioId) {
        mutableStateOf<JSONObject?>(null)
    }
    var erro by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var atualizar by remember(usuarioId) {
        mutableStateOf(0)
    }
    var saindo by remember(usuarioId) {
        mutableStateOf(false)
    }
    var confirmarSaida by remember {
        mutableStateOf(false)
    }
    var aviso by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(usuarioId, atualizar) {
        instituicao = null
        erro = null

        if (!SangueApi.autenticado) {
            erro = "Entre com uma conta de hemocentro."
            return@LaunchedEffect
        }

        try {
            instituicao = SangueApi.objeto(
                "GET",
                "/api/hemocentros/me"
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ApiException) {
            erro = exception.message
                ?: "Não foi possível consultar a instituição."
        } catch (_: Exception) {
            erro = "Não foi possível consultar a instituição."
        }
    }

    BackHandler(enabled = tela == TelaMaisReal.MENU) {
        if (!saindo) onNavigateToInicio()
    }

    when (tela) {
        TelaMaisReal.MEU_PERFIL -> {
            HemoMeuPerfilScreen(
                onNavigateBack = {
                    tela = TelaMaisReal.MENU
                    atualizar++
                },
                onLogout = onLogout
            )
            return
        }

        TelaMaisReal.EQUIPE -> {
            HemoEquipeScreen(
                onNavigateBack = {
                    tela = TelaMaisReal.MENU
                    atualizar++
                    recarregarAcesso()
                }
            )
            return
        }

        TelaMaisReal.HORARIOS -> {
            if (acesso.editarHorarios) {
                HemoHorariosScreen(
                    dataInicial = SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.ROOT
                    ).apply {
                        timeZone = TimeZone.getTimeZone(
                            "America/Sao_Paulo"
                        )
                    }.format(java.util.Date()),
                    onNavigateBack = {
                        tela = TelaMaisReal.MENU
                    }
                )
                return
            } else {
                LaunchedEffect(Unit) {
                    tela = TelaMaisReal.MENU
                }
            }
        }

        TelaMaisReal.PERFIL -> {
            BackHandler {
                tela = TelaMaisReal.MENU
            }

            Scaffold(
                containerColor = Color.White,
                topBar = {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(top = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                tela = TelaMaisReal.MENU
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Voltar"
                            )
                        }
                        Text(
                            "Perfil da instituição",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier.fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val dados = instituicao

                    if (dados != null) {
                        val campos = listOf(
                            "nome" to "Nome",
                            "endereco" to "Endereço",
                            "cidade" to "Cidade",
                            "estado" to "Estado",
                            "telefone" to "Telefone",
                            "cnpj" to "CNPJ",
                            "cnes" to "CNES",
                            "emailContato" to "E-mail de contato"
                        )

                        campos.forEach { (campo, titulo) ->
                            Text(
                                titulo,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (dados.isNull(campo)) {
                                    "Não informado"
                                } else {
                                    dados.optString(campo)
                                        .ifBlank { "Não informado" }
                                }
                            )
                        }
                    } else {
                        Text(
                            erro ?: "Carregando instituição...",
                            color = VermelhoMaisReal
                        )
                    }

                    OutlinedButton(
                        onClick = { atualizar++ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Atualizar dados")
                    }
                }
            }
            return
        }

        TelaMaisReal.MENU -> Unit
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            BarraNavegacaoHemo(
                selecionado = "Mais",
                onInicio = onNavigateToInicio,
                onAgenda = onNavigateToAgenda,
                onEstoque = onNavigateToEstoque,
                onChat = onNavigateToChat,
                onMais = onNavigateToMais,
                enabled = !saindo
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Mais",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFEF2F2)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = instituicao?.optString("nome")
                            ?.takeIf { it.isNotBlank() }
                            ?: "Seu hemocentro",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Conta conectada: " +
                                SangueApi.usuario?.optString("nome").orEmpty()
                    )
                    Text(
                        SangueApi.usuario?.optString("email").orEmpty(),
                        color = Color(0xFF64748B)
                    )
                }
            }

            erro?.let {
                Text(it, color = VermelhoMaisReal)
            }

            ItemMaisReal(
                "Meu perfil",
                "Editar seus dados pessoais e alterar sua senha",
                !saindo
            ) {
                tela = TelaMaisReal.MEU_PERFIL
            }

            ItemMaisReal(
                "Equipe e acessos",
                if (acesso.inicializar) {
                    "Inicializar a equipe da instituição"
                } else if (acesso.administrador) {
                    "Cadastrar funcionários e gerenciar acessos"
                } else {
                    "Consultar sua função e permissão"
                },
                !saindo
            ) {
                tela = TelaMaisReal.EQUIPE
            }

            if (acesso.administrador) {
                ItemMaisReal(
                    "Administrador principal",
                    "Consultar o administrador real e transferir a função",
                    !saindo
                ) {
                    tela = TelaMaisReal.EQUIPE
                }
            }

            if (acesso.editarHorarios) {
                ItemMaisReal(
                    "Horários e vagas",
                    "Cadastrar horários e controlar novas reservas",
                    !saindo
                ) {
                    tela = TelaMaisReal.HORARIOS
                }
            }

            ItemMaisReal(
                "Perfil da instituição",
                "Consultar os dados cadastrados",
                !saindo
            ) {
                tela = TelaMaisReal.PERFIL
            }

            ItemMaisReal(
                "Relatórios",
                "Indicadores e relatórios",
                !saindo
            ) {
                aviso = "Os relatórios desta seção ainda não estão " +
                        "integrados. Os indicadores disponíveis podem " +
                        "ser consultados na Home."
            }

            ItemMaisReal(
                "Notificações",
                "Preferências e alertas",
                !saindo
            ) {
                aviso = "As preferências e notificações desta seção " +
                        "ainda não estão integradas."
            }

            OutlinedButton(
                onClick = { confirmarSaida = true },
                enabled = !saindo,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (saindo) "Saindo..." else "Sair da conta",
                    color = VermelhoMaisReal
                )
            }
        }
    }

    if (confirmarSaida) {
        AlertDialog(
            onDismissRequest = { confirmarSaida = false },
            title = { Text("Sair da conta?") },
            text = {
                Text("Você poderá entrar novamente com seu e-mail e senha.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarSaida = false
                        saindo = true

                        scope.launch {
                            try {
                                SangueApi.sair()
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (_: Exception) {
                                SangueApi.limparSessao()
                            } finally {
                                saindo = false
                            }

                            onLogout()
                        }
                    }
                ) {
                    Text("Sair", color = VermelhoMaisReal)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmarSaida = false }
                ) {
                    Text("Voltar")
                }
            }
        )
    }

    aviso?.let { mensagem ->
        AlertDialog(
            onDismissRequest = { aviso = null },
            title = { Text("Informação") },
            text = { Text(mensagem) },
            confirmButton = {
                TextButton(onClick = { aviso = null }) {
                    Text("Entendi")
                }
            }
        )
    }
}

@Composable
private fun ItemMaisReal(
    titulo: String,
    descricao: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                titulo,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                descricao,
                color = Color(0xFF64748B),
                fontSize = 13.sp
            )
        }
    }
}