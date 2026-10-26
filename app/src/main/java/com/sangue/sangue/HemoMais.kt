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
    MEU_PERFIL,
    RELATORIOS,
    NOTIFICACOES,
    PREFERENCIAS,
    COORDENADAS,
    EXCLUSAO,
    CHAMADOS
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
        TelaMaisReal.RELATORIOS -> { RelatorioRealScreen { tela = TelaMaisReal.MENU }; return }
        TelaMaisReal.COORDENADAS -> { CoordenadasReaisScreen { tela = TelaMaisReal.MENU }; return }
        TelaMaisReal.PREFERENCIAS -> { PreferenciasReaisScreen({ tela = TelaMaisReal.MENU }, onLogout); return }
        TelaMaisReal.EXCLUSAO -> { PreferenciasReaisScreen({ tela = TelaMaisReal.MENU }, onLogout, true); return }
        TelaMaisReal.CHAMADOS -> { HemoChamadosReaisScreen { tela = TelaMaisReal.MENU }; return }
        TelaMaisReal.NOTIFICACOES -> {
            var conversa by remember { mutableStateOf<Long?>(null) }
            if (conversa != null) ConversasReaisScreen(onBack = { conversa = null }, conversaInicial = conversa)
            else NotificacoesReaisScreen(onBack = { tela = TelaMaisReal.MENU }, onAbrir = { destino, ref ->
                if (destino == "chat") conversa = ref
                if (destino == "estoque") onNavigateToEstoque()
            })
            return
        }

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
            PerfilInstituicaoScreen(onBack = { tela = TelaMaisReal.MENU; atualizar++ })
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
                if (acesso.administrador) "Consultar e editar os dados cadastrados" else "Consultar os dados cadastrados",
                !saindo
            ) {
                tela = TelaMaisReal.PERFIL
            }

            if (acesso.administrador) {
                ItemMaisReal("Relatórios", "Consultar indicadores reais", !saindo) { tela = TelaMaisReal.RELATORIOS }
                ItemMaisReal("Localização da instituição", "Cadastrar coordenadas verificadas", !saindo) { tela = TelaMaisReal.COORDENADAS }
            }
            if (acesso.administrador || acesso.funcao in setOf("ATENDIMENTO", "ESTOQUE")) {
                ItemMaisReal("Chamados emergenciais", "Consultar chamados e interesses", !saindo) { tela = TelaMaisReal.CHAMADOS }
            }
            ItemMaisReal("Notificações", "Consultar a caixa interna", !saindo) { tela = TelaMaisReal.NOTIFICACOES }
            ItemMaisReal("Preferências", "Configurar avisos internos", !saindo) { tela = TelaMaisReal.PREFERENCIAS }
            ItemMaisReal("Solicitar exclusão da conta", "Registrar solicitação e bloquear o acesso", !saindo) { tela = TelaMaisReal.EXCLUSAO }

            OutlinedButton(
                onClick = { confirmarSaida = true },
                enabled = !saindo,
                modifier = Modifier.fillMaxWidth()
                , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) {
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
            , shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), containerColor = androidx.compose.ui.graphics.Color.White)
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
            , shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), containerColor = androidx.compose.ui.graphics.Color.White)
    }
}

@Composable
private fun ItemMaisReal(
    titulo: String,
    descricao: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    AcessoOriginal(titulo, descricao, enabled, onClick)
}