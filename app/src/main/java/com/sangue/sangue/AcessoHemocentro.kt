package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException

data class AcessoHemocentro(
    val funcao: String = "",
    val permissao: String = "",
    val inicializar: Boolean = false,
    val conferido: Boolean = true
) {
    val administrador: Boolean
        get() = permissao == "ADMINISTRADOR"

    val membro: Boolean
        get() = funcao.isNotEmpty() && permissao.isNotEmpty()

    val escrita: Boolean
        get() = conferido && membro &&
                permissao != "SOMENTE_VISUALIZACAO"

    val agenda: Boolean
        get() = administrador ||
                funcao in setOf("ATENDIMENTO", "TRIAGEM")

    val estoque: Boolean
        get() = administrador || funcao == "ESTOQUE"

    val horarios: Boolean
        get() = administrador || funcao == "ATENDIMENTO"

    val editarHorarios: Boolean
        get() = horarios && membro &&
                permissao != "SOMENTE_VISUALIZACAO"

    val checkin: Boolean
        get() = escrita &&
                (administrador || funcao == "ATENDIMENTO")

    val triagem: Boolean
        get() = escrita &&
                (administrador || funcao == "TRIAGEM")

    val encerrar: Boolean
        get() = escrita && agenda

    val emitirChamado: Boolean
        get() = escrita && estoque

    fun permite(destino: String): Boolean {
        return when (destino) {
            "hemo_home", "hemo_mais" -> membro || inicializar
            "hemo_agenda" -> agenda
            "hemo_scanner" -> checkin
            "hemo_estoque" -> estoque
            "hemo_chat" -> membro
            "hemo_chamados" -> administrador || funcao in setOf("ESTOQUE", "ATENDIMENTO")
            else -> false
        }
    }
}

val LocalAcessoHemocentro = staticCompositionLocalOf {
    AcessoHemocentro()
}

val LocalRecarregarAcessoHemo =
    staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun AreaHemocentro(
    destino: String,
    onVoltar: () -> Unit,
    onLogout: () -> Unit,
    conteudo: @Composable () -> Unit
) {
    val usuario = SangueApi.usuario
    val id = usuario?.optLong("id")
    val centro = usuario?.optLong("hemocentroId")
    val lifecycleOwner = LocalLifecycleOwner.current

    var acesso by remember(id, centro) {
        mutableStateOf<AcessoHemocentro?>(null)
    }
    var erro by remember(id, centro) {
        mutableStateOf<String?>(null)
    }
    var carregando by remember(id, centro) {
        mutableStateOf(true)
    }
    var tentativa by remember(id, centro) {
        mutableStateOf(0)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                tentativa++
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(id, centro, tentativa) {
        carregando = true
        erro = null

        try {
            check(
                SangueApi.autenticado &&
                        usuario?.optString("tipoUsuario") == "hemocentro"
            )

            val vinculo = SangueApi.objeto(
                metodo = "GET",
                caminho = "/api/hemocentros/me/equipe/me"
            )

            check(vinculo.getLong("usuarioId") == id)
            check(vinculo.getLong("hemocentroId") == centro)
            check(vinculo.getBoolean("ativo"))

            val funcao = vinculo.getString("funcao")
            val permissao = vinculo.getString("permissao")

            check(
                funcao in setOf(
                    "ADMINISTRADOR",
                    "ATENDIMENTO",
                    "TRIAGEM",
                    "ESTOQUE"
                )
            )
            check(
                permissao in setOf(
                    "ADMINISTRADOR",
                    "PADRAO",
                    "SOMENTE_VISUALIZACAO"
                )
            )

            acesso = AcessoHemocentro(
                funcao = funcao,
                permissao = permissao
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ApiException) {
            if (
                exception.status == 403 &&
                exception.message ==
                "Inicialize a equipe institucional antes de continuar."
            ) {
                acesso = AcessoHemocentro(inicializar = true)
            } else {
                acesso = null
                erro = exception.message
                    ?: "Não foi possível conferir seu acesso."
            }
        } catch (_: Exception) {
            acesso = null
            erro = "Não foi possível conferir seu acesso. " +
                    "Entre novamente ou tente atualizar."
        } finally {
            carregando = false
        }
    }

    val atual = acesso

    if (atual != null && atual.permite(destino)) {
        CompositionLocalProvider(
            LocalAcessoHemocentro provides atual.copy(
                conferido = !carregando
            ),
            LocalRecarregarAcessoHemo provides {
                tentativa++
            }
        ) {
            conteudo()
        }
        return
    }

    BackHandler(onBack = onVoltar)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (carregando) {
            CircularProgressIndicator(
                color = Color(0xFFE21C2C)
            )
            Spacer(Modifier.height(16.dp))
            Text("Conferindo seu acesso...")
        } else {
            Text(
                erro ?: "Sua função não permite acessar esta tela."
            )
            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { tentativa++ }
                , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) {
                Text("Atualizar acesso")
            }

            TextButton(onClick = onVoltar) {
                Text("Voltar")
            }

            TextButton(
                onClick = {
                    SangueApi.limparSessao()
                    onLogout()
                }
            ) {
                Text("Voltar ao login")
            }
        }
    }
}

@Composable
fun BarraNavegacaoHemo(
    selecionado: String,
    onInicio: () -> Unit,
    onAgenda: () -> Unit,
    onEstoque: () -> Unit,
    onChat: () -> Unit,
    onMais: () -> Unit,
    enabled: Boolean = true
) {
    val acesso = LocalAcessoHemocentro.current

    NavigationBar(
        containerColor = Color(0xFFFDFBFC)
    ) {
        val destinos = listOf(
            Triple("Início", Icons.Outlined.Home, onInicio),
            Triple("Agenda", Icons.Outlined.CalendarToday, onAgenda),
            Triple("Estoque", Icons.Outlined.WaterDrop, onEstoque),
            Triple("Chat", Icons.Outlined.ChatBubbleOutline, onChat),
            Triple("Mais", Icons.Outlined.MoreHoriz, onMais)
        ).filter {
            when (it.first) {
                "Agenda" -> acesso.agenda
                "Estoque" -> acesso.estoque
                "Chat" -> acesso.membro
                else -> true
            }
        }

        destinos.forEach { (titulo, icone, acao) ->
            NavigationBarItem(
                selected = titulo == selecionado,
                enabled = enabled,
                onClick = acao,
                icon = {
                    Icon(
                        icone,
                        contentDescription = titulo
                    )
                },
                label = {
                    Text(titulo, fontSize = 10.sp)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFE21C2C),
                    selectedTextColor = Color(0xFFE21C2C),
                    unselectedIconColor = Color(0xFF64748B),
                    unselectedTextColor = Color(0xFF64748B),
                    indicatorColor = Color(0xFFFEF2F2)
                )
            )
        }
    }
}