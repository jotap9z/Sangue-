package com.sangue.sangue

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var centroAgendamentoId by remember { mutableStateOf<Long?>(null) }

    fun voltarHomeHemo() {
        navController.navigate("hemo_home") {
            popUpTo("hemo_home") { inclusive = true }
            launchSingleTop = true
        }
    }

    fun voltarLogin() {
        centroAgendamentoId = null
        navController.navigate("landing_page") {
            popUpTo(0)
        }
    }

    fun inicioHemo() {
        navController.navigate("hemo_home") {
            popUpTo("hemo_home")
        }
    }

    fun abrirNotificacao(destino: String, referencia: Long?) {
        val rota = when (destino) {
            "chat" -> referencia?.let { "conversa/$it" } ?: "conversas"
            "emergencia" -> referencia?.let { "emergencia/$it" } ?: "emergencia"
            "checkin" -> referencia?.let { "reserva/$it" } ?: "checkin"
            "historico" -> "historico"
            else -> "home"
        }
        navController.navigate(rota) { launchSingleTop = true }
    }

    NavHost(
        navController = navController,
        startDestination = "landing_page"
    ) {
        composable("auth") {
            AuthScreen(
                onLoginSuccess = { isHemocentro ->
                    navController.navigate(
                        if (isHemocentro) "hemo_home" else "home"
                    ) {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("landing_page") {
            LandPageScreen(
                onNavigateToLogin = {
                    navController.navigate("auth")
                },
                onNavigateToCheckin = {
                    navController.navigate("checkin")
                },
                onNavigateToEmergencia = {
                    navController.navigate("emergencia")
                },
                onNavigateToHistorico = {
                    navController.navigate("historico")
                },
                onNavigateToPerfil = {
                    navController.navigate("perfil")
                }
            )
        }

        composable("home") {
            HomeScreen(
                onAbrirNotificacao = { destino, ref -> abrirNotificacao(destino, ref) },
                onNavigateToInicio = {},
                onNavigateToVerificacao = {
                    centroAgendamentoId = null
                    navController.navigate("posso_doar")
                },
                onNavigateToCheckin = {
                    navController.navigate("checkin")
                },
                onNavigateToEmergencia = {
                    navController.navigate("emergencia")
                },
                onNavigateToHistorico = {
                    navController.navigate("historico")
                },
                onNavigateToPerfil = {
                    navController.navigate("perfil")
                },
                onNavigateToEstoque = {
                    navController.navigate("estoque")
                },
                onNavigateToHemocentros = {
                    navController.navigate("hemocentros")
                }
            )
        }

        composable("checkin") {
            CheckInScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("perfil") {
            PerfilScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = { voltarLogin() },
                onNavigateToTriagem = {
                    navController.navigate("posso_doar")
                }
            )
        }

        composable("posso_doar") {
            PossoDoarScreen(
                onNavigateToResultado = {
                    navController.navigate("resultado")
                }
            ) {
                navController.popBackStack()
            }
        }

        composable("resultado") {
            ResultadoScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAgendamento = {
                    navController.navigate("agendamento")
                }
            ) {
                navController.navigate("hemocentros")
            }
        }

        composable("resultado_negativo") {
            ResultadoNegativoScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("agendamento") {
            AgendamentoScreen(
                hemocentroInicial = centroAgendamentoId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateHome = {
                    centroAgendamentoId = null
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToCheckin = {
                    centroAgendamentoId = null
                    navController.navigate("checkin") {
                        popUpTo("home")
                    }
                }
            )
        }

        composable("emergencia") {
            EmergenciaScreen(
                onNavigateBack = { navController.popBackStack() },
                onAgendarCentro = { id -> centroAgendamentoId = id; navController.navigate("posso_doar") }
            )
        }

        composable("historico") {
            HistoricoScreen {
                navController.popBackStack()
            }
        }

        composable("estoque") {
            EstoqueScreen(
                onNavigateBack = { navController.popBackStack() },
                onAgendarCentro = { id -> centroAgendamentoId = id; navController.navigate("posso_doar") }
            )
        }
        composable("hemocentros") {
            HemocentrosScreen(
                onNavigateBack = { navController.popBackStack() },
                onAgendarCentro = { id -> centroAgendamentoId = id; navController.navigate("posso_doar") }
            )
        }
        composable("emergencia/{id}") { entrada ->
            EmergenciaScreen(
                onNavigateBack = { navController.popBackStack() },
                chamadoInicial = entrada.arguments?.getString("id")?.toLongOrNull(),
                onAgendarCentro = { id -> centroAgendamentoId = id; navController.navigate("posso_doar") }
            )
        }
        composable("conversas") { ConversasReaisScreen(onBack = { navController.popBackStack() }) }
        composable("conversa/{id}") { entrada ->
            val id = entrada.arguments?.getString("id")?.toLongOrNull()
            if (SangueApi.usuario?.optString("tipoUsuario") == "hemocentro") {
                AreaHemocentro("hemo_chat", { voltarHomeHemo() }, { voltarLogin() }) {
                    ConversasReaisScreen(onBack = { navController.popBackStack() }, conversaInicial = id)
                }
            } else ConversasReaisScreen(onBack = { navController.popBackStack() }, conversaInicial = id)
        }
        composable("reserva/{id}") { entrada ->
            CheckInScreen(onNavigateBack = { navController.popBackStack() }, onNavigateHome = { navController.navigate("home") }, agendamentoInicial = entrada.arguments?.getString("id")?.toLongOrNull())
        }
        composable("hemo_chamados") {
            AreaHemocentro("hemo_chamados", { voltarHomeHemo() }, { voltarLogin() }) {
                HemoChamadosReaisScreen { navController.popBackStack() }
            }
        }

        composable("hemo_home") {
            AreaHemocentro(
                destino = "hemo_home",
                onVoltar = { voltarHomeHemo() },
                onLogout = { voltarLogin() }
            ) {
                HemoHomeScreen(
                    onNavigateToInicio = {},
                    onNavigateToAgenda = {
                        navController.navigate("hemo_agenda")
                    },
                    onNavigateToEstoque = {
                        navController.navigate("hemo_estoque")
                    },
                    onNavigateToChat = {
                        navController.navigate("hemo_chat")
                    },
                    onNavigateToMais = {
                        navController.navigate("hemo_mais")
                    },
                    onNavigateToCriarChamado = {
                        navController.navigate("hemo_chamados")
                    }
                )
            }
        }

        composable("hemo_agenda") {
            AreaHemocentro(
                destino = "hemo_agenda",
                onVoltar = { voltarHomeHemo() },
                onLogout = { voltarLogin() }
            ) {
                HemoAgendaScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToInicio = { inicioHemo() },
                    onNavigateToAgenda = {},
                    onNavigateToEstoque = {
                        navController.navigate("hemo_estoque")
                    },
                    onNavigateToChat = {
                        navController.navigate("hemo_chat")
                    },
                    onNavigateToMais = {
                        navController.navigate("hemo_mais")
                    },
                    onNavigateToEscanear = {
                        navController.navigate("hemo_scanner")
                    }
                )
            }
        }

        composable("hemo_scanner") {
            AreaHemocentro(
                destino = "hemo_scanner",
                onVoltar = { voltarHomeHemo() },
                onLogout = { voltarLogin() }
            ) {
                HemoScannerScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable("hemo_estoque") {
            AreaHemocentro(
                destino = "hemo_estoque",
                onVoltar = { voltarHomeHemo() },
                onLogout = { voltarLogin() }
            ) {
                HemoEstoqueConsultaScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToInicio = { inicioHemo() },
                    onNavigateToAgenda = {
                        navController.navigate("hemo_agenda")
                    },
                    onNavigateToEstoque = {},
                    onNavigateToChat = {
                        navController.navigate("hemo_chat")
                    },
                    onNavigateToMais = {
                        navController.navigate("hemo_mais")
                    }
                )
            }
        }

        composable("hemo_chat") {
            AreaHemocentro(
                destino = "hemo_chat",
                onVoltar = { voltarHomeHemo() },
                onLogout = { voltarLogin() }
            ) {
                HemoChatScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToInicio = { inicioHemo() },
                    onNavigateToAgenda = {
                        navController.navigate("hemo_agenda")
                    },
                    onNavigateToEstoque = {
                        navController.navigate("hemo_estoque")
                    },
                    onNavigateToChat = {},
                    onNavigateToMais = {
                        navController.navigate("hemo_mais")
                    }
                )
            }
        }

        composable("hemo_mais") {
            AreaHemocentro(
                destino = "hemo_mais",
                onVoltar = { voltarHomeHemo() },
                onLogout = { voltarLogin() }
            ) {
                HemoMaisScreen(
                    onNavigateToInicio = { inicioHemo() },
                    onNavigateToAgenda = {
                        navController.navigate("hemo_agenda")
                    },
                    onNavigateToEstoque = {
                        navController.navigate("hemo_estoque")
                    },
                    onNavigateToChat = {
                        navController.navigate("hemo_chat")
                    },
                    onNavigateToMais = {},
                    onLogout = { voltarLogin() }
                )
            }
        }
    }
}