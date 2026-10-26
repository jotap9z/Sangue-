package com.sangue.sangue

import androidx.compose.runtime.Composable

@Composable
fun HemoChatScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToInicio: () -> Unit = {},
    onNavigateToAgenda: () -> Unit = {},
    onNavigateToEstoque: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToMais: () -> Unit = {}
) {
    ConversasReaisScreen(
        onBack = onNavigateBack,
        rodape = { habilitado ->
            BarraNavegacaoHemo(
                selecionado = "Chat",
                onInicio = onNavigateToInicio,
                onAgenda = onNavigateToAgenda,
                onEstoque = onNavigateToEstoque,
                onChat = onNavigateToChat,
                onMais = onNavigateToMais,
                enabled = habilitado
            )
        },
        onOutroDestino = { destino ->
            when (destino) {
                "estoque" -> onNavigateToEstoque()
                else -> onNavigateToMais()
            }
        }
    )
}